package mobileauth

import (
	"context"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/base64"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"net/http"
	"net/url"
	"strings"
)

func (a Apple) seal(value, subject string) (string, error) {
	block, e := aes.NewCipher(a.EncryptionKey)
	if e != nil {
		return "", mobile.ErrUnavailable
	}
	gcm, e := cipher.NewGCM(block)
	if e != nil {
		return "", e
	}
	nonce := make([]byte, gcm.NonceSize())
	if _, e = rand.Read(nonce); e != nil {
		return "", e
	}
	encrypted := gcm.Seal(nonce, nonce, []byte(value), []byte(subject))
	return base64.RawURLEncoding.EncodeToString(encrypted), nil
}
func (a Apple) Revoke(ctx context.Context, subject identity.Subject) error {
	if subject.Provider != "apple" || subject.AppID != a.ClientID {
		return mobile.ErrUnauthorized
	}
	block, e := aes.NewCipher(a.EncryptionKey)
	if e != nil {
		return mobile.ErrUnavailable
	}
	gcm, e := cipher.NewGCM(block)
	if e != nil {
		return e
	}
	data, e := base64.RawURLEncoding.DecodeString(subject.Secret)
	if e != nil || len(data) < gcm.NonceSize() {
		return mobile.ErrUnauthorized
	}
	raw, e := gcm.Open(nil, data[:gcm.NonceSize()], data[gcm.NonceSize():], []byte(subject.ID))
	if e != nil {
		return mobile.ErrUnauthorized
	}
	form := url.Values{"client_id": {a.ClientID}, "client_secret": {a.ClientSecret}, "token": {string(raw)}, "token_type_hint": {"refresh_token"}}
	req, e := http.NewRequestWithContext(ctx, http.MethodPost, "https://appleid.apple.com/auth/revoke", strings.NewReader(form.Encode()))
	if e != nil {
		return mobile.ErrUnavailable
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, e := client.Do(req)
	if e != nil {
		return mobile.ErrUnavailable
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		return mobile.ErrUnavailable
	}
	return nil
}
