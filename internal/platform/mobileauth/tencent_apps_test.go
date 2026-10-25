package mobileauth

import (
	"context"
	"errors"
	"io"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"net/http"
	"strings"
	"testing"
)

type providerRoundTrip func(*http.Request) (*http.Response, error)

func (f providerRoundTrip) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }

func TestTencentApplicationSelectionKeepsWebRedirectAndDeniesUnknownApps(t *testing.T) {
	p := tencentApplications{apps: map[tencentApplicationKey]tencentApplication{
		{"web-fixture", "web"}: {client: Tencent{Provider: "qq", AppID: "web-fixture", Secret: "fixture-secret"}, redirectURI: "https://mnd.example/app/provider-callback"},
	}}
	original := client
	t.Cleanup(func() { client = original })
	calls := 0
	client = &http.Client{Transport: providerRoundTrip(func(r *http.Request) (*http.Response, error) {
		calls++
		var body string
		if r.URL.Host != "graph.qq.com" {
			t.Fatal("untrusted provider address")
		}
		switch r.URL.Path {
		case "/oauth2.0/token":
			q := r.URL.Query()
			if q.Get("client_id") != "web-fixture" || q.Get("redirect_uri") != "https://mnd.example/app/provider-callback" || q.Get("client_secret") != "fixture-secret" {
				t.Fatal("wrong configured application or redirect")
			}
			body = `{"access_token":"fixture-access","error":0}`
		case "/oauth2.0/me":
			body = `{"client_id":"web-fixture","openid":"fixture-subject","error":0}`
		default:
			t.Fatal("untrusted provider method")
		}
		return &http.Response{StatusCode: http.StatusOK, Body: io.NopCloser(strings.NewReader(body)), Header: make(http.Header)}, nil
	})}
	for _, credential := range []identity.Credential{{AppID: "other", Client: "web", Code: "fixture"}, {AppID: "web-fixture", Client: "android", Code: "fixture"}, {AppID: "web-fixture", Client: "other", Code: "fixture"}} {
		if _, err := p.Verify(context.Background(), credential, "nonce"); !errors.Is(err, mobile.ErrUnauthorized) {
			t.Fatal("unknown application/client accepted")
		}
	}
	if calls != 0 {
		t.Fatal("denied credentials reached a provider")
	}
	subject, err := p.Verify(context.Background(), identity.Credential{AppID: "web-fixture", Client: "web", Code: "fixture"}, "nonce")
	if err != nil || subject.Provider != "qq" || subject.AppID != "web-fixture" || subject.ID != "fixture-subject" || calls != 2 {
		t.Fatal("verified application identity was not preserved")
	}
}
