package mobileauth

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"kitty-circle/internal/mobile"
	"net/http"
	"strings"
)

// The configured gateway must support lookup and persistent idempotency keys.
// Acceptance receipts do not assert OS display or end-device delivery.
type GatewayPush struct{ Endpoint, Token string }

func (g GatewayPush) call(ctx context.Context, path string, input, out any) error {
	if !strings.HasPrefix(g.Endpoint, "https://") || g.Token == "" {
		return mobile.ErrUnavailable
	}
	b, e := json.Marshal(input)
	if e != nil {
		return e
	}
	req, e := http.NewRequestWithContext(ctx, http.MethodPost, strings.TrimRight(g.Endpoint, "/")+path, bytes.NewReader(b))
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
	if json.NewDecoder(io.LimitReader(resp.Body, 8192)).Decode(out) != nil {
		return mobile.ErrUnavailable
	}
	return nil
}
func (g GatewayPush) Lookup(ctx context.Context, key string) (out mobile.PushLookup, err error) {
	err = g.call(ctx, "/lookup", map[string]string{"idempotency_key": key}, &out)
	return
}
func (g GatewayPush) Send(ctx context.Context, in mobile.PushPayload) (out mobile.PushAcceptance, err error) {
	err = g.call(ctx, "/send", in, &out)
	return
}
