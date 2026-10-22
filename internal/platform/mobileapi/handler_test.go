package mobileapi_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/mobileapi"
	"kitty-circle/internal/store"
	"time"
)

type gateway struct{ code string }

func (g *gateway) Send(_ context.Context, _, code, _ string) error { g.code = code; return nil }
func apiFixture(t *testing.T) (http.Handler, string) {
	t.Helper()
	memory := store.NewMemoryStore()
	memory.CreateUser(domain.User{Username: "fixture", Phone: "13800138000"})
	repo := memory.MobileRepository()
	sms := &gateway{}
	auth := identity.New(repo, "test-key-longer-than-thirty-two-bytes", sms, nil)
	if _, e := auth.IssueOTP(context.Background(), 0, "client", identity.OTPInput{Phone: "13800138000", Purpose: "login"}); e != nil {
		t.Fatal(e)
	}
	login, e := auth.PhoneLogin(context.Background(), identity.PhoneLogin{Phone: "13800138000", Code: sms.code, AcceptTerms: true})
	if e != nil {
		t.Fatal(e)
	}
	service := mobile.New(repo)
	if e := service.RecordAgeEvidence(context.Background(), "test-verifier", mobile.AgeEvidenceInput{ActorID: login.User.ID, AgeBand: "adult", EvidenceRef: "fixture", ValidUntil: time.Now().Add(24 * time.Hour)}); e != nil {
		t.Fatal(e)
	}
	return mobileapi.New(service, auth, t.TempDir(), "separate-staff-key"), login.Token
}
func request(t *testing.T, h http.Handler, method, path, token, body string) *httptest.ResponseRecorder {
	t.Helper()
	r := httptest.NewRequest(method, path, strings.NewReader(body))
	if token != "" {
		r.Header.Set("Authorization", "Bearer "+token)
	}
	w := httptest.NewRecorder()
	h.ServeHTTP(w, r)
	return w
}
func TestPublicBoundariesAndFriendActionRouting(t *testing.T) {
	h, token := apiFixture(t)
	if got := request(t, h, "GET", "/api/v2/accounts", "", "").Code; got != http.StatusForbidden {
		t.Fatalf("internal records readable: %d", got)
	}
	if got := request(t, h, "POST", "/api/v2/groups", "", `{"name":"群聊"}`).Code; got != http.StatusUnauthorized {
		t.Fatalf("unauthenticated command: %d", got)
	}
	w := request(t, h, "POST", "/api/v2/friendships/request", token, `{"user_id":1}`)
	if w.Code != 400 || !bytes.Contains(w.Body.Bytes(), []byte("invalid friend")) {
		t.Fatalf("friend action parsed as numeric id: %d %s", w.Code, w.Body.String())
	}
	if got := request(t, h, "GET", "/api/v2/ops/reviews", token, "").Code; got != 403 {
		t.Fatalf("consumer token granted staff access: %d", got)
	}
}
func TestStrictJSONAndPublicCatalogue(t *testing.T) {
	h, token := apiFixture(t)
	if got := request(t, h, "POST", "/api/v2/groups", token, `{"name":"群聊","owner_id":999}`).Code; got != 400 {
		t.Fatalf("unknown ownership field accepted: %d", got)
	}
	if got := request(t, h, "POST", "/api/v2/groups", token, `{"name":"群聊"} {}`).Code; got != 400 {
		t.Fatalf("two JSON objects accepted: %d", got)
	}
	w := request(t, h, "GET", "/api/v2/expression-catalog", "", "")
	if w.Code != 200 {
		t.Fatal(w.Code)
	}
	var result struct {
		Data struct {
			Emoji    []json.RawMessage `json:"emoji"`
			Stickers []json.RawMessage `json:"stickers"`
		} `json:"data"`
	}
	if e := json.Unmarshal(w.Body.Bytes(), &result); e != nil {
		t.Fatal(e)
	}
	if len(result.Data.Emoji) != 128 || len(result.Data.Stickers) != 24 {
		t.Fatal("design catalogue does not match")
	}
	w = request(t, h, "GET", "/api/v2/expression-assets/v1/emoji_u1f600.svg", "", "")
	if w.Code != 200 || w.Header().Get("Content-Type") != "image/svg+xml" {
		t.Fatalf("embedded asset unavailable: %d", w.Code)
	}
}
