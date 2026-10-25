package opsapi

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"kitty-circle/internal/operations"
	"kitty-circle/internal/platform/mobileapi"
)

func TestNamedRoleAndEnvironmentRequired(t *testing.T) {
	const token = "0123456789abcdef0123456789abcdef"
	for _, tt := range []struct {
		name                     string
		roles, envs              []string
		path, method, credential string
		want                     int
	}{
		{"consumer-cookie", nil, nil, "context", "GET", "", 401},
		{"worker-not-viewer", []string{"worker"}, []string{"staging"}, "context", "GET", token, 403},
		{"other-environment", []string{"ops_viewer"}, []string{"production"}, "context", "GET", token, 403},
		{"explicit-reader", []string{"ops_viewer"}, []string{"staging"}, "context", "GET", token, 200},
		{"reader-cannot-write", []string{"ops_viewer"}, []string{"staging"}, "incidents", "POST", token, 403},
		{"admin-not-release", []string{"ops_admin"}, []string{"staging"}, "releases", "POST", token, 403},
		{"no-execution-route", []string{"ops_release"}, []string{"staging"}, "releases/abc/execute", "POST", token, 404},
	} {
		t.Run(tt.name, func(t *testing.T) {
			h := &Handler{Service: operations.New(operations.NewMemory(), "staging"), Operators: []mobileapi.Operator{{ID: "named", Token: token, Roles: tt.roles, Environments: tt.envs}}}
			r := httptest.NewRequest(tt.method, "/api/ops/v1/"+tt.path, strings.NewReader(`{}`))
			r.Header.Set("X-Operator-Token", tt.credential)
			r.Header.Set("Cookie", "meow_token=consumer")
			w := httptest.NewRecorder()
			h.ServeHTTP(w, r)
			if w.Code != tt.want {
				t.Fatalf("got %d want %d", w.Code, tt.want)
			}
			if strings.Contains(w.Body.String(), token) {
				t.Fatal("credential leaked")
			}
		})
	}
}
func TestPayloadLimitDoesNotHideTrailingObject(t *testing.T) {
	r := httptest.NewRequest(http.MethodPost, "/", strings.NewReader(`{}`+strings.Repeat(" ", 33*1024)+`{}`))
	if _, e := payload[struct{}](r); e == nil {
		t.Fatal("oversized trailing body accepted")
	}
}
