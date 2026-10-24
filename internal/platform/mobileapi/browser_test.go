package mobileapi_test

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/mobileapi"
)

// 本轮仅新增回归源码；按仓库规则不执行测试。
func TestBrowserSessionKeepsTokensInSecureCookieAndRequiresExactCSRFOrigin(t *testing.T) {
	native, _ := apiFixture(t)
	h := native.(*mobileapi.Handler)
	browser, err := mobileapi.NewBrowser(h, "https://mnd.test", false)
	if err != nil {
		t.Fatal(err)
	}
	call := func(method, path, body, origin, csrf string, cookies ...*http.Cookie) *httptest.ResponseRecorder {
		r := httptest.NewRequest(method, "https://mnd.test/api/web/v2/"+path, strings.NewReader(body))
		if origin != "" {
			r.Header.Set("Origin", origin)
		}
		if csrf != "" {
			r.Header.Set("X-MND-CSRF", csrf)
		}
		for _, cookie := range cookies {
			r.AddCookie(cookie)
		}
		w := httptest.NewRecorder()
		browser.ServeHTTP(w, r)
		return w
	}
	session := call("GET", "session", "", "", "")
	if session.Code != 200 {
		t.Fatal(session.Code)
	}
	csrf := session.Result().Cookies()[0]
	if !csrf.HttpOnly || !csrf.Secure || csrf.SameSite != http.SameSiteStrictMode || csrf.Path != "/api/web/v2/" {
		t.Fatal("unsafe csrf cookie", csrf)
	}
	if _, err = h.Identity.IssueOTP(context.Background(), 0, "browser-fixture", identity.OTPInput{Phone: "13800138000", Purpose: "login"}); err != nil {
		t.Fatal(err)
	}
	code := h.Identity.SMS.(*gateway).code
	body := `{"phone":"13800138000","code":"` + code + `","device":"untrusted-native-device"}`
	if w := call("POST", "auth/phone", body, "https://evil.test", csrf.Value, csrf); w.Code != 403 {
		t.Fatal("cross origin login", w.Code)
	}
	if w := call("POST", "auth/phone", body, "https://mnd.test", "", csrf); w.Code != 403 {
		t.Fatal("csrf missing", w.Code)
	}
	login := call("POST", "auth/phone", body, "https://mnd.test", csrf.Value, csrf)
	if login.Code != 200 {
		t.Fatal(login.Code, login.Body.String())
	}
	var envelope struct {
		Data map[string]json.RawMessage `json:"data"`
	}
	if err = json.Unmarshal(login.Body.Bytes(), &envelope); err != nil {
		t.Fatal(err)
	}
	if envelope.Data["token"] != nil || envelope.Data["refresh_token"] != nil {
		t.Fatal("account token exposed to browser JS")
	}
	var account *http.Cookie
	for _, cookie := range login.Result().Cookies() {
		if cookie.Name == "mnd_web_session" {
			account = cookie
		}
	}
	if account == nil || !account.HttpOnly || !account.Secure || account.SameSite != http.SameSiteStrictMode {
		t.Fatal("unsafe account cookie")
	}
	if profile := call("GET", "profile", "", "", "", account, csrf); profile.Code != 200 {
		t.Fatal(profile.Code, profile.Body.String())
	}
	tampered := *account
	pos := len(tampered.Value) / 2
	if tampered.Value[pos] == 'A' {
		tampered.Value = tampered.Value[:pos] + "B" + tampered.Value[pos+1:]
	} else {
		tampered.Value = tampered.Value[:pos] + "A" + tampered.Value[pos+1:]
	}
	if result := call("GET", "profile", "", "", "", &tampered, csrf); result.Code != 401 {
		t.Fatal("tampered cookie accepted", result.Code)
	}
	if out := call("POST", "auth/logout", `{"all":false}`, "https://mnd.test", csrf.Value, account, csrf); out.Code != 200 {
		t.Fatal(out.Code, out.Body.String())
	}
	if result := call("GET", "profile", "", "", "", account, csrf); result.Code != 401 {
		t.Fatal("revoked cookie reused", result.Code)
	}
}

func TestBrowserConfigurationSPAFallbackAndPrivateShareStayClosed(t *testing.T) {
	native, _ := apiFixture(t)
	h := native.(*mobileapi.Handler)
	if _, err := mobileapi.NewBrowser(h, "http://mnd.test", false); err == nil {
		t.Fatal("insecure production origin accepted")
	}
	closed, _ := mobileapi.NewBrowser(h, "", false)
	w := httptest.NewRecorder()
	closed.ServeHTTP(w, httptest.NewRequest("GET", "/api/web/v2/session", nil))
	if w.Code != 503 {
		t.Fatal("unconfigured browser session opened", w.Code)
	}
	root := t.TempDir()
	if err := os.WriteFile(filepath.Join(root, "index.html"), []byte("<!doctype html><title>M&D fixture</title>"), 0600); err != nil {
		t.Fatal(err)
	}
	spa := mobileapi.SPAHandler(root)
	w = httptest.NewRecorder()
	spa.ServeHTTP(w, httptest.NewRequest("GET", "/app/entries/123", nil))
	if w.Code != 200 || !strings.Contains(w.Body.String(), "M&D fixture") {
		t.Fatal("deep link lost", w.Code)
	}
	w = httptest.NewRecorder()
	spa.ServeHTTP(w, httptest.NewRequest("GET", "/app/assets/missing.js", nil))
	if w.Code != 404 {
		t.Fatal("missing JS fell through to HTML", w.Code)
	}
	entry, err := h.Service.SaveEntry(context.Background(), 1, 0, mobile.EntryInput{CommandMeta: mobile.CommandMeta{ClientRequestID: "private-browser-share"}, Entry: mobile.Entry{Space: "circle", Text: "private-share-secret", Audience: mobile.Audience{Mode: "private"}}})
	if err != nil {
		t.Fatal(err)
	}
	w = httptest.NewRecorder()
	h.PublicSharePage(w, httptest.NewRequest("GET", "/share/entries/"+jsonInt(entry.ID), nil))
	if w.Code != 404 || strings.Contains(w.Body.String(), "private-share-secret") {
		t.Fatal("private share exposed", w.Code)
	}
}
func jsonInt(value int64) string { raw, _ := json.Marshal(value); return string(raw) }
