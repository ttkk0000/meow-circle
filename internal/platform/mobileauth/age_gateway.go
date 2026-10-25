package mobileauth

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"
)

type AgeGateway struct {
	endpoint string
	token    string
	info     identity.AgeProviderInfo
}

func configuredHTTPS(raw string, originOnly bool) bool {
	u, err := url.Parse(raw)
	return err == nil && u.Scheme == "https" && u.Host != "" && u.Hostname() != "example.invalid" && u.User == nil && u.RawQuery == "" && u.Fragment == "" && (!originOnly || u.Path == "")
}
func AgeGatewayFromEnvironment() identity.AgeProvider {
	endpoint := strings.TrimRight(os.Getenv("AGE_GATEWAY_URL"), "/")
	token := os.Getenv("AGE_GATEWAY_TOKEN")
	name := strings.TrimSpace(os.Getenv("AGE_PROVIDER_NAME"))
	privacy := os.Getenv("AGE_PROVIDER_PRIVACY_URL")
	origin := strings.TrimRight(os.Getenv("AGE_PROVIDER_LAUNCH_ORIGIN"), "/")
	if !configuredHTTPS(endpoint, false) || len(token) < 32 || name == "" || len(name) > 120 || !configuredHTTPS(privacy, false) || !configuredHTTPS(origin, true) {
		return nil
	}
	return AgeGateway{endpoint, token, identity.AgeProviderInfo{Available: true, Name: name, PrivacyURL: privacy, LaunchOrigin: origin}}
}
func (g AgeGateway) Info() identity.AgeProviderInfo { return g.info }
func (g AgeGateway) Create(ctx context.Context, in identity.AgeProviderRequest) (identity.AgeProviderResult, error) {
	return g.request(ctx, "/requests", in)
}
func (g AgeGateway) Query(ctx context.Context, in identity.AgeProviderRequest) (identity.AgeProviderResult, error) {
	return g.request(ctx, "/requests/"+url.PathEscape(in.Reference)+"/query", in)
}
func (g AgeGateway) request(ctx context.Context, path string, in identity.AgeProviderRequest) (out identity.AgeProviderResult, err error) {
	data, err := json.Marshal(in)
	if err != nil {
		return out, mobile.ErrUnavailable
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, g.endpoint+path, bytes.NewReader(data))
	if err != nil {
		return out, mobile.ErrUnavailable
	}
	req.Header.Set("Authorization", "Bearer "+g.token)
	req.Header.Set("Content-Type", "application/json")
	if path == "/requests" {
		req.Header.Set("Idempotency-Key", in.Reference)
	}
	transport := &http.Client{Timeout: 8 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	response, err := transport.Do(req)
	if err != nil {
		return out, mobile.ErrUnavailable
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return out, mobile.ErrUnavailable
	}
	// 拒收证件号、照片、生物信息等合同以外字段，不将原始响应写入错误或日志。
	decoder := json.NewDecoder(io.LimitReader(response.Body, 64*1024))
	decoder.DisallowUnknownFields()
	if decoder.Decode(&out) != nil {
		return out, mobile.ErrUnavailable
	}
	var extra any
	if decoder.Decode(&extra) != io.EOF {
		return out, mobile.ErrUnavailable
	}
	return out, nil
}
