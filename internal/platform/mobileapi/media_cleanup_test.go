package mobileapi_test

import (
	"context"
	"fmt"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/mobileapi"
)

func TestUploadSweepProtectsReadyFreshLegacyAndRequiresPrivacyRole(t *testing.T) {
	handler, _ := apiFixture(t)
	h := handler.(*mobileapi.Handler)
	now := time.Now().UTC()
	h.Service.Now = func() time.Time { return now }
	operatorToken := strings.Repeat("p", 32)
	h.Operators = []mobileapi.Operator{{ID: "privacy-fixture", Token: operatorToken, Roles: []string{"privacy"}}}
	file := mobile.MediaFile{Scope: "public", MIME: "image/png", SHA256: mobile.Fingerprint("fixture")}
	ready, e := h.Service.ReserveMedia(context.Background(), 1, "", "ready-upload", file)
	if e != nil {
		t.Fatal(e)
	}
	metadata, _ := mobile.Decode[mobile.MediaFile](ready)
	if _, e = h.Service.CompleteReservedMedia(context.Background(), 1, "", ready.ID, ready.Version); e != nil {
		t.Fatal(e)
	}
	expired, e := h.Service.ReserveMedia(context.Background(), 1, "", "expired-upload", file)
	if e != nil {
		t.Fatal(e)
	}
	old, _ := mobile.Decode[mobile.MediaFile](expired)
	fresh := fmt.Sprintf("mnd-999-1-%s.png", file.SHA256)
	now = now.Add(20 * time.Minute)
	for _, name := range []string{metadata.Filename, old.Filename, ".upload-" + old.Filename + "-123", ".upload-" + metadata.Filename + "-123", fresh, "legacy.png"} {
		path := filepath.Join(h.MediaDir, name)
		if e = os.WriteFile(path, []byte("fixture"), 0600); e != nil {
			t.Fatal(e)
		}
		modified := now.Add(-time.Hour)
		if name == fresh {
			modified = now
		}
		if e = os.Chtimes(path, modified, modified); e != nil {
			t.Fatal(e)
		}
	}
	call := func(token string) *httptest.ResponseRecorder {
		r := httptest.NewRequest("POST", "/api/v2/ops/upload-expiry", strings.NewReader(`{"cursor":0,"after":""}`))
		r.Header.Set("X-Operator-Token", token)
		w := httptest.NewRecorder()
		h.ServeHTTP(w, r)
		return w
	}
	if w := call(strings.Repeat("x", 32)); w.Code != 403 {
		t.Fatal("unprivileged cleanup", w.Code)
	}
	if w := call(operatorToken); w.Code != 200 {
		t.Fatal(w.Code, w.Body.String())
	}
	for _, name := range []string{metadata.Filename, fresh, "legacy.png"} {
		if _, e = os.Stat(filepath.Join(h.MediaDir, name)); e != nil {
			t.Fatal("protected file lost", name, e)
		}
	}
	for _, name := range []string{old.Filename, ".upload-" + old.Filename + "-123", ".upload-" + metadata.Filename + "-123"} {
		if _, e = os.Stat(filepath.Join(h.MediaDir, name)); !os.IsNotExist(e) {
			t.Fatal("stale file retained", name, e)
		}
	}
	if w := call(operatorToken); w.Code != 200 {
		t.Fatal("cleanup retry was not idempotent", w.Code, w.Body.String())
	}
}
