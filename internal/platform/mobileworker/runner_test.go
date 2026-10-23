package mobileworker

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"strings"
	"testing"

	"kitty-circle/internal/mobile"
)

// Test source only. These HTTP fixtures have not been started in this session.
func TestWorkerRejectsUnsafeTargets(t *testing.T) {
	for _, url := range []string{"http://example.com/api/v2", "https://user:password@example.com/api/v2", "https://example.com/api/v1", "https://example.com/api/v2?token=secret"} {
		if _, e := New(url, strings.Repeat("t", 32), "push", true); e == nil {
			t.Fatal("unsafe target accepted", url)
		}
	}
	if _, e := New("http://127.0.0.1/api/v2", strings.Repeat("t", 32), "push", false); e == nil {
		t.Fatal("implicit cleartext accepted")
	}
}

func TestWorkerCheckpointRoundtripAndTargetIsolation(t *testing.T) {
	r, e := New("https://example.com/api/v2", strings.Repeat("t", 32), "maintenance", false)
	if e != nil {
		t.Fatal(e)
	}
	path := filepath.Join(t.TempDir(), "state.json")
	initial, e := r.Load(path)
	if e != nil || initial.Schema != 1 {
		t.Fatal(initial, e)
	}
	result := mobile.JobResult{State: "batch_complete", Positions: map[string]mobile.JobPosition{"uploads": {Cursor: 42, After: "mnd-fixture.png"}}}
	if e = r.Save(path, result); e != nil {
		t.Fatal(e)
	}
	loaded, e := r.Load(path)
	if e != nil || loaded.Positions["uploads"].Cursor != 42 {
		t.Fatal(loaded, e)
	}
	other, e := New("https://example.com/api/v2", strings.Repeat("u", 32), "push", false)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = other.Load(path); e == nil {
		t.Fatal("checkpoint reused across jobs")
	}
}

func TestWorkerSendsNamedJobAndRestoresContinuation(t *testing.T) {
	token := strings.Repeat("t", 32)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != "POST" || r.URL.Path != "/api/v2/ops/jobs/maintenance" || r.Header.Get("X-Operator-Token") != token {
			t.Error("incorrect job request")
		}
		var in mobile.JobInput
		if e := json.NewDecoder(r.Body).Decode(&in); e != nil || in.Positions["evidence"].Cursor != 123 {
			t.Error("continuation lost", e)
		}
		_ = json.NewEncoder(w).Encode(map[string]any{"ok": true, "data": mobile.JobResult{State: "batch_complete", Positions: map[string]mobile.JobPosition{"evidence": {Cursor: 456}}}})
	}))
	defer server.Close()
	r, e := New(server.URL+"/api/v2", token, "maintenance", true)
	if e != nil {
		t.Fatal(e)
	}
	checkpoint, e := r.Load(filepath.Join(t.TempDir(), "missing.json"))
	if e != nil {
		t.Fatal(e)
	}
	checkpoint.Positions["evidence"] = mobile.JobPosition{Cursor: 123}
	result, e := r.Tick(context.Background(), checkpoint)
	if e != nil || result.Positions["evidence"].Cursor != 456 {
		t.Fatal(result, e)
	}
}

func TestWorkerRejectsRedirectAndDoesNotEchoUpstreamSecrets(t *testing.T) {
	for _, code := range []int{http.StatusFound, http.StatusServiceUnavailable} {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Location", "https://example.com/secret-destination")
			w.WriteHeader(code)
			_, _ = w.Write([]byte("private-upstream-provider-token"))
		}))
		r, e := New(server.URL+"/api/v2", strings.Repeat("t", 32), "push", true)
		if e != nil {
			t.Fatal(e)
		}
		checkpoint, e := r.Load(filepath.Join(t.TempDir(), "missing.json"))
		if e != nil {
			t.Fatal(e)
		}
		_, e = r.Tick(context.Background(), checkpoint)
		server.Close()
		if e == nil || strings.Contains(e.Error(), "private-upstream") || strings.Contains(e.Error(), "secret-destination") {
			t.Fatal("unsafe failure", e)
		}
	}
}

func TestWorkerRejectsMalformedSuccessfulResponses(t *testing.T) {
	for _, body := range []string{
		`{"ok":true,"data":{"state":"batch_complete","positions":{}}} {}`,
		`{"ok":true,"data":{"state":"batch_complete","positions":{"evidence":{"cursor":-1}}}}`,
		`{"ok":true,"data":{"state":"batch_complete"}}`,
		`{"ok":false,"data":{"state":"batch_complete","positions":{}}}`,
	} {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) { _, _ = w.Write([]byte(body)) }))
		r, e := New(server.URL+"/api/v2", strings.Repeat("t", 32), "maintenance", true)
		if e != nil {
			t.Fatal(e)
		}
		checkpoint, e := r.Load(filepath.Join(t.TempDir(), "missing.json"))
		if e != nil {
			t.Fatal(e)
		}
		_, e = r.Tick(context.Background(), checkpoint)
		server.Close()
		if e == nil {
			t.Fatal("malformed result was allowed to advance checkpoint", body)
		}
	}
}
