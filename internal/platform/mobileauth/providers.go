package mobileauth

import (
	"bytes"
	"context"
	"crypto"
	"crypto/rsa"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"io"
	"math/big"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"

	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
)

var client = &http.Client{Timeout: 8 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}

func requestJSON(ctx context.Context, method, endpoint string, form url.Values, out any) error {
	var body io.Reader
	if method == http.MethodGet && form != nil {
		endpoint += "?" + form.Encode()
	} else if form != nil {
		body = strings.NewReader(form.Encode())
	}
	req, e := http.NewRequestWithContext(ctx, method, endpoint, body)
	if e != nil {
		return mobile.ErrUnavailable
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	}
	resp, e := client.Do(req)
	if e != nil {
		return mobile.ErrUnavailable
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		return mobile.ErrUnavailable
	}
	if e = json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(out); e != nil {
		return mobile.ErrUnauthorized
	}
	return nil
}

type Tencent struct{ Provider, AppID, Secret string }

func (t Tencent) Verify(ctx context.Context, c identity.Credential, nonce string) (identity.Subject, error) {
	if t.AppID == "" || t.Secret == "" {
		return identity.Subject{}, mobile.ErrUnavailable
	}
	if t.Provider == "wechat" {
		if c.Code == "" || len(c.Code) > 2048 {
			return identity.Subject{}, mobile.ErrUnauthorized
		}
		var response struct {
			AccessToken string `json:"access_token"`
			OpenID      string `json:"openid"`
			Scope       string `json:"scope"`
			ErrCode     int    `json:"errcode"`
		}
		e := requestJSON(ctx, http.MethodGet, "https://api.weixin.qq.com/sns/oauth2/access_token", url.Values{"appid": {t.AppID}, "secret": {t.Secret}, "code": {c.Code}, "grant_type": {"authorization_code"}}, &response)
		if e != nil {
			return identity.Subject{}, e
		}
		if response.ErrCode != 0 || response.OpenID == "" || response.AccessToken == "" {
			return identity.Subject{}, mobile.ErrUnauthorized
		}
		return identity.Subject{Provider: t.Provider, AppID: t.AppID, ID: response.OpenID}, nil
	}
	if t.Provider != "qq" {
		return identity.Subject{}, mobile.ErrUnavailable
	}
	access := c.AccessToken
	if c.Code != "" {
		var response struct {
			AccessToken string `json:"access_token"`
			Error       int    `json:"error"`
		}
		e := requestJSON(ctx, http.MethodGet, "https://graph.qq.com/oauth2.0/token", url.Values{"client_id": {t.AppID}, "client_secret": {t.Secret}, "code": {c.Code}, "grant_type": {"authorization_code"}, "fmt": {"json"}}, &response)
		if e != nil {
			return identity.Subject{}, e
		}
		if response.Error != 0 {
			return identity.Subject{}, mobile.ErrUnauthorized
		}
		access = response.AccessToken
	}
	if access == "" || len(access) > 4096 {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	var me struct {
		ClientID string `json:"client_id"`
		OpenID   string `json:"openid"`
		Error    int    `json:"error"`
	}
	e := requestJSON(ctx, http.MethodGet, "https://graph.qq.com/oauth2.0/me", url.Values{"access_token": {access}, "fmt": {"json"}}, &me)
	if e != nil {
		return identity.Subject{}, e
	}
	if me.ClientID != t.AppID || me.OpenID == "" || me.Error != 0 {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	return identity.Subject{Provider: "qq", AppID: t.AppID, ID: me.OpenID}, nil
}

type Apple struct {
	ClientID, ClientSecret string
	EncryptionKey          []byte
}

func (a Apple) Verify(ctx context.Context, c identity.Credential, nonce string) (identity.Subject, error) {
	if a.ClientID == "" || a.ClientSecret == "" {
		return identity.Subject{}, mobile.ErrUnavailable
	}
	if c.Code == "" || len(c.Code) > 4096 {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	var response struct {
		IDToken      string `json:"id_token"`
		RefreshToken string `json:"refresh_token"`
		Error        string `json:"error"`
	}
	if e := requestJSON(ctx, http.MethodPost, "https://appleid.apple.com/auth/token", url.Values{"client_id": {a.ClientID}, "client_secret": {a.ClientSecret}, "code": {c.Code}, "grant_type": {"authorization_code"}}, &response); e != nil {
		return identity.Subject{}, e
	}
	if response.Error != "" || response.IDToken == "" {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	subject, e := a.verifyToken(ctx, response.IDToken, nonce)
	if e != nil {
		return subject, e
	}
	if response.RefreshToken == "" {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	subject.Secret, e = a.seal(response.RefreshToken, subject.ID)
	return subject, e
}
func (a Apple) verifyToken(ctx context.Context, token, nonce string) (identity.Subject, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	var header struct {
		Algorithm string `json:"alg"`
		KeyID     string `json:"kid"`
	}
	b, e := base64.RawURLEncoding.DecodeString(parts[0])
	if e != nil || json.Unmarshal(b, &header) != nil || header.Algorithm != "RS256" {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	var keys struct {
		Keys []struct {
			KID string `json:"kid"`
			KTY string `json:"kty"`
			Use string `json:"use"`
			N   string `json:"n"`
			E   string `json:"e"`
		} `json:"keys"`
	}
	if e = requestJSON(ctx, http.MethodGet, "https://appleid.apple.com/auth/keys", nil, &keys); e != nil {
		return identity.Subject{}, e
	}
	var public *rsa.PublicKey
	for _, key := range keys.Keys {
		if key.KID != header.KeyID || key.KTY != "RSA" || key.Use != "sig" {
			continue
		}
		n, e := base64.RawURLEncoding.DecodeString(key.N)
		if e != nil {
			return identity.Subject{}, mobile.ErrUnauthorized
		}
		ex, e := base64.RawURLEncoding.DecodeString(key.E)
		if e != nil || len(ex) > 4 {
			return identity.Subject{}, mobile.ErrUnauthorized
		}
		exponent := 0
		for _, v := range ex {
			exponent = exponent*256 + int(v)
		}
		if len(n) < 256 || exponent < 3 {
			return identity.Subject{}, mobile.ErrUnauthorized
		}
		public = &rsa.PublicKey{N: new(big.Int).SetBytes(n), E: exponent}
		break
	}
	if public == nil {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	signature, e := base64.RawURLEncoding.DecodeString(parts[2])
	if e != nil {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	hash := sha256.Sum256([]byte(parts[0] + "." + parts[1]))
	if rsa.VerifyPKCS1v15(public, crypto.SHA256, hash[:], signature) != nil {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	b, e = base64.RawURLEncoding.DecodeString(parts[1])
	if e != nil {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	var claims struct {
		Issuer   string `json:"iss"`
		Audience string `json:"aud"`
		Subject  string `json:"sub"`
		Nonce    string `json:"nonce"`
		Expires  int64  `json:"exp"`
		Issued   int64  `json:"iat"`
	}
	if json.Unmarshal(b, &claims) != nil {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	now := time.Now().Unix()
	if claims.Issuer != "https://appleid.apple.com" || claims.Audience != a.ClientID || claims.Subject == "" || claims.Nonce != nonce || nonce == "" || claims.Expires <= now || claims.Issued > now+60 || claims.Issued < now-600 {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	return identity.Subject{Provider: "apple", AppID: a.ClientID, ID: claims.Subject}, nil
}

// GatewaySMS uses an operator-owned HTTPS gateway that must report a durable
// provider acceptance. It does not return codes or log phone numbers/secrets.
type GatewaySMS struct{ Endpoint, Token string }

func (g GatewaySMS) Send(ctx context.Context, phone, code, purpose string) error {
	u, e := url.Parse(g.Endpoint)
	if e != nil || u.Scheme != "https" || u.Host == "" || u.User != nil || g.Token == "" {
		return mobile.ErrUnavailable
	}
	b, e := json.Marshal(map[string]string{"phone": phone, "code": code, "purpose": purpose})
	if e != nil {
		return e
	}
	req, e := http.NewRequestWithContext(ctx, http.MethodPost, g.Endpoint, bytes.NewReader(b))
	if e != nil {
		return mobile.ErrUnavailable
	}
	req.Header.Set("Authorization", "Bearer "+g.Token)
	req.Header.Set("Content-Type", "application/json")
	resp, e := client.Do(req)
	if e != nil {
		return mobile.ErrUnavailable
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return mobile.ErrUnavailable
	}
	var receipt struct {
		Accepted bool   `json:"accepted"`
		ID       string `json:"message_id"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 4096)).Decode(&receipt) != nil || !receipt.Accepted || receipt.ID == "" {
		return mobile.ErrUnavailable
	}
	return nil
}
func FromEnvironment(repo mobile.Repository, secret string) *identity.Service {
	providers := map[string]identity.Provider{}
	for _, name := range []string{"wechat", "qq"} {
		prefix := strings.ToUpper(name)
		id, key := os.Getenv(prefix+"_LOGIN_APP_ID"), os.Getenv(prefix+"_LOGIN_APP_SECRET")
		if id != "" && key != "" {
			providers[name] = Tencent{name, id, key}
		}
	}
	if id, key := os.Getenv("APPLE_LOGIN_CLIENT_ID"), os.Getenv("APPLE_LOGIN_CLIENT_SECRET"); id != "" && key != "" {
		encryptionKey, e := base64.StdEncoding.DecodeString(os.Getenv("APPLE_TOKEN_ENCRYPTION_KEY"))
		if e == nil && len(encryptionKey) == 32 {
			providers["apple"] = Apple{ClientID: id, ClientSecret: key, EncryptionKey: encryptionKey}
		}
	}
	var sms identity.SMS
	if endpoint, token := os.Getenv("SMS_GATEWAY_URL"), os.Getenv("SMS_GATEWAY_TOKEN"); endpoint != "" && token != "" {
		sms = GatewaySMS{endpoint, token}
	}
	s := identity.New(repo, secret, sms, providers)
	if endpoint, token := os.Getenv("EMAIL_GATEWAY_URL"), os.Getenv("EMAIL_GATEWAY_TOKEN"); strings.HasPrefix(endpoint, "https://") && token != "" {
		s.Email = GatewayEmail{endpoint, token}
	}
	return s
}

type GatewayEmail struct{ Endpoint, Token string }

func (g GatewayEmail) Send(ctx context.Context, address, code, purpose string) error {
	b, e := json.Marshal(map[string]string{"email": address, "code": code, "purpose": purpose})
	if e != nil {
		return e
	}
	req, e := http.NewRequestWithContext(ctx, http.MethodPost, g.Endpoint, bytes.NewReader(b))
	if e != nil {
		return mobile.ErrUnavailable
	}
	req.Header.Set("Authorization", "Bearer "+g.Token)
	req.Header.Set("Content-Type", "application/json")
	resp, e := client.Do(req)
	if e != nil {
		return mobile.ErrUnavailable
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return mobile.ErrUnavailable
	}
	var receipt struct {
		Accepted bool   `json:"accepted"`
		ID       string `json:"message_id"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 4096)).Decode(&receipt) != nil || !receipt.Accepted || receipt.ID == "" {
		return mobile.ErrUnavailable
	}
	return nil
}
