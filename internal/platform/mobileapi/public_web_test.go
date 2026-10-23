package mobileapi

import (
	"context"
	"crypto/sha256"
	"encoding/base64"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/store"
)

// Regression source only. These handlers and tests have not been run.
func publicPageFixture(t *testing.T) (*Handler, mobile.Record) {
	t.Helper()
	memory := store.NewMemoryStore()
	if _, ok := memory.CreateUser(domain.User{Username: "public-publisher"}); !ok {
		t.Fatal("create publisher")
	}
	repo := memory.MobileRepository()
	r := mobile.Record{OwnerID: 1, State: "published", Public: true}
	if e := repo.Update(context.Background(), func(tx mobile.Tx) error {
		if e := mobile.SetBody(&r, mobile.Entry{Title: `小满 <script>alert("title")</script>`, Text: `公开摘要 " onload="alert(1) <img src=x>`, Audience: mobile.Audience{Mode: "public"}, ContentOrigin: "ai_generated"}); e != nil {
			return e
		}
		return tx.Save(mobile.Entries, &r)
	}); e != nil {
		t.Fatal(e)
	}
	return New(mobile.New(repo), nil, t.TempDir(), ""), r
}

func TestPublicSharePageEscapesMetadataAndIgnoresUntrustedOrigin(t *testing.T) {
	h, entry := publicPageFixture(t)
	path := "/share/entries/" + strconv.FormatInt(entry.ID, 10)
	r := httptest.NewRequest(http.MethodGet, path+"?access_token=never-copy-this", nil)
	r.Host = "spoofed.invalid"
	r.Header.Set("X-Forwarded-Host", "forwarded.invalid")
	r.Header.Set("X-Forwarded-Proto", "javascript")
	w := httptest.NewRecorder()
	h.PublicSharePage(w, r)
	body := w.Body.String()
	if w.Code != http.StatusOK || !strings.Contains(body, "&lt;script&gt;") || strings.Contains(body, "<script>") || strings.Contains(body, "<img src=x>") {
		t.Fatal("public HTML was not escaped", w.Code, body)
	}
	for _, unexpected := range []string{"spoofed.invalid", "forwarded.invalid", "access_token", "never-copy-this", "#ZgotmplZ"} {
		if strings.Contains(body, unexpected) {
			t.Fatalf("untrusted origin, query or unsafe URL entered the page: %s", unexpected)
		}
	}
	for _, expected := range []string{`href="` + path + `"`, `href="mnd://entries/` + strconv.FormatInt(entry.ID, 10) + `"`, `href="/safety.html"`, "含生成内容"} {
		if !strings.Contains(body, expected) {
			t.Fatalf("missing public page action or metadata: %s", expected)
		}
	}
	hash := sha256.Sum256([]byte(publicShareCSS))
	if !strings.Contains(w.Header().Get("Content-Security-Policy"), "'sha256-"+base64.StdEncoding.EncodeToString(hash[:])+"'") || w.Header().Get("Referrer-Policy") != "no-referrer" || !strings.Contains(w.Header().Get("Cache-Control"), "no-store") {
		t.Fatal("missing share response policy", w.Header())
	}
}

func TestPublicSharePagePrivateWithdrawnAndUnknownAreIndistinguishable(t *testing.T) {
	h, entry := publicPageFixture(t)
	if e := h.Service.Repo.Update(context.Background(), func(tx mobile.Tx) error {
		v := mobile.Entry{Title: "private-title-must-not-leak", Text: "private-body-must-not-leak", Audience: mobile.Audience{Mode: "private"}}
		if e := mobile.SetBody(&entry, v); e != nil {
			return e
		}
		return tx.Save(mobile.Entries, &entry)
	}); e != nil {
		t.Fatal(e)
	}
	unavailable := ""
	for _, path := range []string{"/share/entries/" + strconv.FormatInt(entry.ID, 10), "/share/entries/9223372036854775807", "/share/reviews/1", "/share/entries/01", "/share/entries/9223372036854775808"} {
		w := httptest.NewRecorder()
		h.PublicSharePage(w, httptest.NewRequest(http.MethodGet, path, nil))
		if w.Code != http.StatusNotFound || strings.Contains(w.Body.String(), "must-not-leak") || strings.Contains(w.Body.String(), "mnd://") {
			t.Fatal("unavailable resource leaked data", w.Code, w.Body.String())
		}
		if unavailable == "" {
			unavailable = w.Body.String()
		} else if unavailable != w.Body.String() {
			t.Fatal("unavailable resources exposed different existence information")
		}
	}
	entry.State = "withdrawn"
	if e := h.Service.Repo.Update(context.Background(), func(tx mobile.Tx) error {
		if e := mobile.SetBody(&entry, mobile.Entry{Title: "withdrawn-must-not-leak", Audience: mobile.Audience{Mode: "public"}}); e != nil {
			return e
		}
		return tx.Save(mobile.Entries, &entry)
	}); e != nil {
		t.Fatal(e)
	}
	w := httptest.NewRecorder()
	h.PublicSharePage(w, httptest.NewRequest(http.MethodGet, "/share/entries/"+strconv.FormatInt(entry.ID, 10), nil))
	if w.Code != http.StatusNotFound || w.Body.String() != unavailable {
		t.Fatal("withdrawn record remained available", w.Code, w.Body.String())
	}
}

func TestPublicSharePathPreservesInt64AndRejectsNoncanonicalIDs(t *testing.T) {
	for _, raw := range []string{"9007199254740993", "9223372036854775807", "1"} {
		_, got, ok := publicSharePath("/share/pets/" + raw)
		if !ok || strconv.FormatInt(got, 10) != raw {
			t.Fatal("int64 lost precision", raw, got, ok)
		}
	}
	for _, raw := range []string{"0", "-1", "+1", "01", "1.0", "1e2", "9223372036854775808", "1/extra", "1/"} {
		if _, _, ok := publicSharePath("/share/pets/" + raw); ok {
			t.Fatal("invalid share ID accepted", raw)
		}
	}
	for _, path := range []string{"/share/claims/1", "/share/accounts/1", "/share/entries", "/share//1", "/api/v2/share/entries/1"} {
		if _, _, ok := publicSharePath(path); ok {
			t.Fatal("unapproved share path accepted", path)
		}
	}
}

func TestPublicShareImageOnlyAllowsSameOriginMediaPath(t *testing.T) {
	valid := "/api/v2/media/9223372036854775807/content"
	if publicShareImage(valid) != valid {
		t.Fatal("canonical local media was rejected")
	}
	for _, path := range []string{"https://external.invalid/image.png", "//external.invalid/image.png", "/uploads/original.jpg", "/api/v2/media/01/content", "/api/v2/media/1/content?token=secret", "/api/v2/media/1/../content", "/api/v2/media/1/content#private"} {
		if publicShareImage(path) != "" {
			t.Fatal("unapproved public image URL accepted", path)
		}
	}
}

func TestPublicSharePageHeadAndUnsupportedMethods(t *testing.T) {
	h, entry := publicPageFixture(t)
	path := "/share/entries/" + strconv.FormatInt(entry.ID, 10)
	for _, method := range []string{http.MethodHead, http.MethodPost} {
		w := httptest.NewRecorder()
		h.PublicSharePage(w, httptest.NewRequest(method, path, nil))
		if method == http.MethodHead && (w.Code != http.StatusOK || w.Body.Len() != 0) {
			t.Fatal("HEAD returned a body or changed visibility", w.Code, w.Body.String())
		}
		if method == http.MethodPost && (w.Code != http.StatusMethodNotAllowed || w.Header().Get("Allow") != "GET, HEAD") {
			t.Fatal("unexpected method accepted", w.Code, w.Header())
		}
	}
}
