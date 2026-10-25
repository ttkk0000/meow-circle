package mobileapi

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

// 全部为拒绝路径，不调用真实高德服务，也不使用实际Key。
func TestAmapProxyRejectsUntrustedRequests(t *testing.T) {
	for _, item := range []struct {
		name, method, path, referer, origin string
		status                              int
	}{
		{"missing-origin", "GET", "/maps/amap/_AMapService/v3/assistant/coordinate/convert?key=public-key", "", "", 403},
		{"foreign-origin", "GET", "/maps/amap/_AMapService/v3/assistant/coordinate/convert?key=public-key", "https://app.example/app/map", "https://foreign.example", 403},
		{"key-substitution", "GET", "/maps/amap/_AMapService/v3/assistant/coordinate/convert?key=other-key", "https://app.example/app/map", "", 400},
		{"not-general-proxy", "GET", "/maps/amap/_AMapService/v3/unknown?key=public-key", "https://app.example/app/map", "", 404},
		{"no-writes", "POST", "/maps/amap/_AMapService/v3/geocode/regeo?key=public-key", "https://app.example/app/map", "", 405},
	} {
		t.Run(item.name, func(t *testing.T) {
			r := httptest.NewRequest(item.method, item.path, nil)
			r.Header.Set("Referer", item.referer)
			r.Header.Set("Origin", item.origin)
			w := httptest.NewRecorder()
			AmapProxy("public-key", "example-security-code", "https://app.example").ServeHTTP(w, r)
			if w.Code != item.status {
				t.Fatalf("got %d, want %d", w.Code, item.status)
			}
			if strings.Contains(w.Body.String(), "example-security-code") {
				t.Fatal("security code leaked")
			}
		})
	}
	w := httptest.NewRecorder()
	AmapProxy("", "", "").ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/maps/amap/_AMapService/v3/geocode/regeo", nil))
	if w.Code != http.StatusServiceUnavailable {
		t.Fatal("missing configuration must fail closed")
	}
}
