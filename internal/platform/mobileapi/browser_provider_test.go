package mobileapi

import (
	"bytes"
	"kitty-circle/internal/identity"
	"net/http"
	"net/http/httptest"
	"testing"
)

// 仅回归源码，当前任务不执行测试。
func TestProviderNestedLoginTokensOnlyEnterHttpOnlyCookie(t *testing.T) {
	h := &BrowserHandler{API: &Handler{Identity: &identity.Service{Secret: "fixture-secret-longer-than-thirty-two-bytes"}}, Origin: "https://mnd.example"}
	response := &browserResponse{status: http.StatusOK, header: make(http.Header)}
	_, _ = response.body.WriteString(`{"ok":true,"data":{"login":{"token":"fixture-access","refresh_token":"fixture-refresh","user":{"id":123}},"needs_account_choice":false}}`)
	w := httptest.NewRecorder()
	if err := h.takeProviderLogin(w, response); err != nil {
		t.Fatal(err)
	}
	for _, forbidden := range []string{"fixture-access", "fixture-refresh", `"token"`, `"refresh_token"`} {
		if bytes.Contains(response.body.Bytes(), []byte(forbidden)) {
			t.Fatal("provider session leaked into JavaScript response")
		}
	}
	if !bytes.Contains(response.body.Bytes(), []byte(`"id":123`)) {
		t.Fatal("safe user projection lost")
	}
	cookies := w.Result().Cookies()
	if len(cookies) != 1 || !cookies[0].HttpOnly || !cookies[0].Secure || cookies[0].SameSite != http.SameSiteStrictMode || cookies[0].Path != browserPrefix {
		t.Fatal("provider cookie policy changed")
	}
	r := httptest.NewRequest(http.MethodGet, "https://mnd.example/api/web/v2/session", nil)
	r.AddCookie(cookies[0])
	pair, err := h.pair(r)
	if err != nil || pair.Access != "fixture-access" || pair.Refresh != "fixture-refresh" {
		t.Fatal("provider cookie did not retain the sealed session")
	}
}
