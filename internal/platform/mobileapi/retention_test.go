package mobileapi_test

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"kitty-circle/internal/platform/mobileapi"
)

// Source only; these role-boundary regressions have not been executed.
func TestRetentionRoutesRequireTheirOwnNamedRoles(t *testing.T) {
	handler, _ := apiFixture(t)
	h := handler.(*mobileapi.Handler)
	privacyToken, moderatorToken := strings.Repeat("p", 32), strings.Repeat("m", 32)
	h.Operators = []mobileapi.Operator{
		{ID: "privacy-fixture", Token: privacyToken, Roles: []string{"privacy"}},
		{ID: "moderator-fixture", Token: moderatorToken, Roles: []string{"moderator"}},
	}
	for _, tc := range []struct {
		method, path, token, body string
		want                      int
	}{
		{"POST", "/api/v2/ops/retention-expiry", privacyToken, `{"cursor":0}`, http.StatusOK},
		{"POST", "/api/v2/ops/retention-expiry", moderatorToken, `{"cursor":0}`, http.StatusForbidden},
		{"GET", "/api/v2/ops/rights-cases", moderatorToken, "", http.StatusOK},
		{"GET", "/api/v2/ops/rights-cases", privacyToken, "", http.StatusForbidden},
		{"GET", "/api/v2/ops/rights-cases/1", privacyToken, "", http.StatusForbidden},
		{"GET", "/api/v2/ops/rights-cases?cursor=invalid", moderatorToken, "", http.StatusBadRequest},
		{"POST", "/api/v2/ops/retention-expiry", privacyToken, `{"cursor":-1}`, http.StatusBadRequest},
	} {
		r := httptest.NewRequest(tc.method, tc.path, strings.NewReader(tc.body))
		r.Header.Set("X-Operator-Token", tc.token)
		w := httptest.NewRecorder()
		h.ServeHTTP(w, r)
		if w.Code != tc.want {
			t.Fatalf("%s %s: got %d, want %d", tc.method, tc.path, w.Code, tc.want)
		}
	}
}
