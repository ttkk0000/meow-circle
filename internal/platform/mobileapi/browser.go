package mobileapi

import (
	"bytes"
	"context"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"

	"kitty-circle/internal/mobile"
)

const browserPrefix = "/api/web/v2/"
const sessionCookie = "mnd_web_session"
const csrfCookie = "mnd_web_csrf"

type browserPair struct {
	Access  string `json:"access"`
	Refresh string `json:"refresh"`
}
type browserRefreshContext struct{}

// BrowserHandler 只适配同源浏览器会话，权限／资格／版本仍由原 v2 Handler 校验。
type BrowserHandler struct {
	API         *Handler
	Origin      string
	Development bool
}

func NewBrowser(api *Handler, origin string, development bool) (*BrowserHandler, error) {
	origin = strings.TrimRight(origin, "/")
	if origin != "" {
		u, e := url.Parse(origin)
		local := u != nil && (u.Hostname() == "localhost" || u.Hostname() == "127.0.0.1" || u.Hostname() == "[::1]" || u.Hostname() == "::1")
		if e != nil || u.Host == "" || u.User != nil || u.RawQuery != "" || u.Fragment != "" || u.Path != "" || (u.Scheme != "https" && !(development && local && u.Scheme == "http")) {
			return nil, fmt.Errorf("MND_WEB_ORIGIN must be one exact HTTPS origin (development permits loopback HTTP)")
		}
	}
	return &BrowserHandler{API: api, Origin: origin, Development: development}, nil
}
func (h *BrowserHandler) cookie(w http.ResponseWriter, name, value string, age int) {
	http.SetCookie(w, &http.Cookie{Name: name, Value: value, Path: browserPrefix, MaxAge: age, HttpOnly: true, Secure: !strings.HasPrefix(h.Origin, "http://"), SameSite: http.SameSiteStrictMode})
}
func (h *BrowserHandler) cipher() (cipher.AEAD, error) {
	if len(h.API.Identity.Secret) < 32 {
		return nil, mobile.ErrUnavailable
	}
	key := sha256.Sum256(append([]byte("M&D browser session v1\x00"), h.API.Identity.Secret...))
	block, e := aes.NewCipher(key[:])
	if e != nil {
		return nil, e
	}
	return cipher.NewGCM(block)
}
func (h *BrowserHandler) seal(pair browserPair) (string, error) {
	a, e := h.cipher()
	if e != nil {
		return "", e
	}
	nonce := make([]byte, a.NonceSize())
	if _, e = rand.Read(nonce); e != nil {
		return "", e
	}
	plain, e := json.Marshal(pair)
	if e != nil {
		return "", e
	}
	return base64.RawURLEncoding.EncodeToString(a.Seal(nonce, nonce, plain, []byte(h.Origin))), nil
}
func (h *BrowserHandler) pair(r *http.Request) (browserPair, error) {
	c, e := r.Cookie(sessionCookie)
	if e != nil {
		return browserPair{}, nil
	}
	if len(c.Value) > 4096 {
		return browserPair{}, mobile.ErrUnauthorized
	}
	data, e := base64.RawURLEncoding.DecodeString(c.Value)
	if e != nil {
		return browserPair{}, mobile.ErrUnauthorized
	}
	a, e := h.cipher()
	if e != nil {
		return browserPair{}, e
	}
	if len(data) < a.NonceSize() {
		return browserPair{}, mobile.ErrUnauthorized
	}
	plain, e := a.Open(nil, data[:a.NonceSize()], data[a.NonceSize():], []byte(h.Origin))
	if e != nil {
		return browserPair{}, mobile.ErrUnauthorized
	}
	var pair browserPair
	e = json.Unmarshal(plain, &pair)
	return pair, e
}
func (h *BrowserHandler) csrf(w http.ResponseWriter, r *http.Request) (string, error) {
	c, e := r.Cookie(csrfCookie)
	if e == nil && len(c.Value) == 43 {
		return c.Value, nil
	}
	b := make([]byte, 32)
	if _, e = rand.Read(b); e != nil {
		return "", e
	}
	value := base64.RawURLEncoding.EncodeToString(b)
	h.cookie(w, csrfCookie, value, 24*60*60)
	return value, nil
}

type browserResponse struct {
	header http.Header
	status int
	body   bytes.Buffer
}

func (b *browserResponse) Header() http.Header { return b.header }
func (b *browserResponse) WriteHeader(status int) {
	if b.status == 0 {
		b.status = status
	}
}
func (b *browserResponse) Write(data []byte) (int, error) {
	if b.status == 0 {
		b.status = 200
	}
	return b.body.Write(data)
}
func (h *BrowserHandler) forward(r *http.Request, path string, pair browserPair, body []byte) *browserResponse {
	ctx := r.Context()
	if path == "auth/refresh" {
		ctx = context.WithValue(ctx, browserRefreshContext{}, true)
	}
	clone := r.Clone(ctx)
	u := *r.URL
	u.Path = "/api/v2/" + path
	u.RawPath = ""
	clone.URL = &u
	clone.RequestURI = u.RequestURI()
	clone.Header = r.Header.Clone()
	clone.Header.Del("Cookie")
	clone.Header.Del("Authorization")
	clone.Header.Del("X-MND-CSRF")
	if pair.Access != "" {
		clone.Header.Set("Authorization", "Bearer "+pair.Access)
	}
	if body != nil {
		clone.Body = io.NopCloser(bytes.NewReader(body))
		clone.ContentLength = int64(len(body))
	}
	response := &browserResponse{header: make(http.Header)}
	h.API.ServeHTTP(response, clone)
	return response
}
func (h *BrowserHandler) relay(w http.ResponseWriter, response *browserResponse) {
	for key, values := range response.header {
		if strings.EqualFold(key, "Set-Cookie") {
			continue
		}
		w.Header()[key] = append([]string(nil), values...)
	}
	w.WriteHeader(response.status)
	_, _ = w.Write(response.body.Bytes())
}
func browserLogin(path string) bool {
	return path == "auth/password" || path == "auth/email" || path == "auth/register" || path == "auth/phone" || path == "auth/oauth-resolve"
}
func (h *BrowserHandler) takeLogin(w http.ResponseWriter, response *browserResponse) (browserPair, error) {
	var envelope map[string]json.RawMessage
	if e := json.Unmarshal(response.body.Bytes(), &envelope); e != nil {
		return browserPair{}, e
	}
	var data map[string]json.RawMessage
	if e := json.Unmarshal(envelope["data"], &data); e != nil {
		return browserPair{}, e
	}
	var pair browserPair
	_ = json.Unmarshal(data["token"], &pair.Access)
	_ = json.Unmarshal(data["refresh_token"], &pair.Refresh)
	if pair.Access == "" || pair.Refresh == "" {
		return pair, mobile.ErrUnavailable
	}
	sealed, e := h.seal(pair)
	if e != nil {
		return pair, e
	}
	h.cookie(w, sessionCookie, sealed, 30*24*60*60)
	delete(data, "token")
	delete(data, "refresh_token")
	safe, e := json.Marshal(data)
	if e != nil {
		return pair, e
	}
	envelope["data"] = safe
	raw, e := json.Marshal(envelope)
	if e != nil {
		return pair, e
	}
	response.body.Reset()
	_, _ = response.body.Write(raw)
	return pair, nil
}
func (h *BrowserHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "private, no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	path := strings.TrimPrefix(r.URL.Path, browserPrefix)
	if path == r.URL.Path || path == "" || strings.HasPrefix(path, "ops") || strings.Contains(path, "..") || r.Header.Get("Authorization") != "" {
		failure(w, mobile.ErrForbidden)
		return
	}
	if h.Origin == "" {
		failure(w, mobile.ErrUnavailable)
		return
	}
	expected, _ := url.Parse(h.Origin)
	if !strings.EqualFold(r.Host, expected.Host) {
		failure(w, mobile.ErrForbidden)
		return
	}
	if origin := r.Header.Get("Origin"); origin != "" && origin != h.Origin {
		failure(w, mobile.ErrForbidden)
		return
	}
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		c, e := r.Cookie(csrfCookie)
		if e != nil || r.Header.Get("Origin") != h.Origin || len(c.Value) != 43 || subtle.ConstantTimeCompare([]byte(c.Value), []byte(r.Header.Get("X-MND-CSRF"))) != 1 {
			failure(w, mobile.ErrForbidden)
			return
		}
	}
	pair, e := h.pair(r)
	if e != nil {
		h.cookie(w, sessionCookie, "", -1)
		failure(w, e)
		return
	}
	if path == "session" && r.Method == http.MethodGet {
		csrf, e := h.csrf(w, r)
		if e != nil {
			failure(w, e)
			return
		}
		var user json.RawMessage
		authenticated := false
		if pair.Access != "" {
			response := h.forward(r, "profile", pair, nil)
			if response.status == http.StatusOK {
				var env struct {
					Data struct {
						User json.RawMessage `json:"user"`
					} `json:"data"`
				}
				if json.Unmarshal(response.body.Bytes(), &env) == nil {
					user = env.Data.User
					authenticated = true
				}
			}
		}
		_ = json.NewEncoder(w).Encode(map[string]any{"ok": true, "data": map[string]any{"authenticated": authenticated, "refreshable": pair.Refresh != "", "user": user, "csrf": csrf}})
		return
	}
	if path == "auth/refresh" {
		if r.Method != http.MethodPost || pair.Access == "" || pair.Refresh == "" {
			failure(w, mobile.ErrUnauthorized)
			return
		}
		body, _ := json.Marshal(map[string]string{"access_token": pair.Access, "refresh_token": pair.Refresh, "device": "web-v2"})
		response := h.forward(r, path, browserPair{}, body)
		if response.status == 200 {
			_, e := h.takeLogin(w, response)
			if e != nil {
				failure(w, e)
				return
			}

		} else if response.status == 401 {
			h.cookie(w, sessionCookie, "", -1)
		}
		h.relay(w, response)
		return
	}
	var body []byte
	if strings.HasPrefix(path, "auth/oauth-") {
		body, e = h.webProviderBody(w, r, path)
		if e != nil {
			failure(w, e)
			return
		}
	}
	if browserLogin(path) {
		if body == nil {
			body, e = io.ReadAll(http.MaxBytesReader(w, r.Body, 1<<20))
		}
		if e != nil {
			failure(w, mobile.Invalid("request too large"))
			return
		}
		var fields map[string]json.RawMessage
		if json.Unmarshal(body, &fields) != nil {
			failure(w, mobile.Invalid("invalid JSON"))
			return
		}
		fields["device"] = json.RawMessage(`"web-v2"`)
		body, e = json.Marshal(fields)
		if e != nil {
			failure(w, e)
			return
		}
	}
	forwardPair := pair
	if browserLogin(path) && path != "auth/oauth-resolve" || path == "auth/capabilities" {
		forwardPair = browserPair{}
	}
	response := h.forward(r, path, forwardPair, body)
	if path == "auth/oauth-finish" && response.status == http.StatusOK {
		if e := h.takeProviderLogin(w, response); e != nil {
			failure(w, e)
			return
		}
	}
	if browserLogin(path) && response.status == 200 {
		if _, e := h.takeLogin(w, response); e != nil {
			failure(w, e)
			return
		}
	}
	if response.status == 200 && (path == "auth/logout" || strings.HasPrefix(path, "auth/account/") && !strings.HasSuffix(path, "/scope")) {
		h.cookie(w, sessionCookie, "", -1)
	}
	if response.status == 200 && path == "data-requests" && r.Method == http.MethodPost {
		var result struct {
			Data mobile.Record `json:"data"`
		}
		if json.Unmarshal(response.body.Bytes(), &result) == nil {
			if data, e := mobile.Decode[mobile.DataRequest](result.Data); e == nil && data.Type == "close_account" {
				h.cookie(w, sessionCookie, "", -1)
			}
		}
	}
	h.relay(w, response)
}
