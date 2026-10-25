package mobileauth

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strings"

	"kitty-circle/internal/mobile"
)

type ProjectionGateway struct{ Endpoint, Token string }

func ProjectionFromEnvironment(prefix string) (mobile.ProjectionSink, error) {
	endpoint, token := os.Getenv(prefix+"_URL"), os.Getenv(prefix+"_TOKEN")
	if endpoint == "" && token == "" {
		return nil, nil
	}
	u, err := url.Parse(endpoint)
	if err != nil || u.Scheme != "https" || u.Host == "" || u.User != nil || u.RawQuery != "" || u.Fragment != "" || len(token) < 32 {
		return nil, fmt.Errorf("invalid %s gateway configuration", prefix)
	}
	return ProjectionGateway{strings.TrimRight(endpoint, "/"), token}, nil
}
func (g ProjectionGateway) Publish(ctx context.Context, event mobile.ProjectionEvent) (out mobile.ProjectionAcceptance, err error) {
	data, err := json.Marshal(event)
	if err != nil {
		return out, err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, g.Endpoint+"/events", bytes.NewReader(data))
	if err != nil {
		return out, mobile.ErrUnavailable
	}
	req.Header.Set("Authorization", "Bearer "+g.Token)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Idempotency-Key", fmt.Sprintf("%s:%d", event.Consumer, event.EventID))
	response, err := client.Do(req)
	if err != nil {
		return out, mobile.ErrUnavailable
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return out, mobile.ErrUnavailable
	}
	data, err = io.ReadAll(io.LimitReader(response.Body, 8193))
	if err != nil || len(data) > 8192 {
		return out, mobile.ErrUnavailable
	}
	decoder := json.NewDecoder(bytes.NewReader(data))
	decoder.DisallowUnknownFields()
	if decoder.Decode(&out) != nil || decoder.Decode(new(any)) != io.EOF {
		return out, mobile.ErrUnavailable
	}
	return out, nil
}
