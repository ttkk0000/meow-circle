package api

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// setupTestServer locates the repo web/ directory via walking up from the
// test's working directory and cd's the process into the repo root so that
// api.NewRouter() can serve static files correctly.
func setupTestServer(t *testing.T) http.Handler {
	t.Helper()

	dir, err := os.Getwd()
	if err != nil {
		t.Fatalf("getwd: %v", err)
	}
	// Walk up until we find go.mod — that's the repo root.
	root := dir
	for {
		if _, err := os.Stat(filepath.Join(root, "go.mod")); err == nil {
			break
		}
		parent := filepath.Dir(root)
		if parent == root {
			t.Fatalf("could not locate repo root from %s", dir)
		}
		root = parent
	}
	if err := os.Chdir(root); err != nil {
		t.Fatalf("chdir: %v", err)
	}
	t.Cleanup(func() { _ = os.Chdir(dir) })

	// Deterministic JWT secret + admin key.
	t.Setenv("JWT_SECRET", "test-secret")
	t.Setenv("ADMIN_KEY", "test-admin-key")
	t.Setenv("DATABASE_URL", "") // force in-memory
	t.Setenv("REDIS_URL", "")

	return NewRouter()
}

type apiEnvelope struct {
	Code    int             `json:"code"`
	Message string          `json:"message"`
	Data    json.RawMessage `json:"data"`
}

func decodeEnvelope(t *testing.T, body []byte) apiEnvelope {
	t.Helper()
	var env apiEnvelope
	if err := json.Unmarshal(body, &env); err != nil {
		t.Fatalf("decode envelope: %v | body=%s", err, string(body))
	}
	return env
}

func TestHealthz(t *testing.T) {
	h := setupTestServer(t)
	req := httptest.NewRequest(http.MethodGet, "/healthz", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
	env := decodeEnvelope(t, rec.Body.Bytes())
	if env.Code != 0 {
		t.Fatalf("envelope code = %d, want 0", env.Code)
	}
	var data map[string]any
	if err := json.Unmarshal(env.Data, &data); err != nil {
		t.Fatalf("decode data: %v", err)
	}
	if data["store"] != "memory" {
		t.Errorf("store = %v, want memory", data["store"])
	}
}

func TestLoginPageIsServed(t *testing.T) {
	h := setupTestServer(t)
	for _, path := range []string{"/login", "/register"} {
		req := httptest.NewRequest(http.MethodGet, path, nil)
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("%s status = %d, want 200", path, rec.Code)
		}
		body := rec.Body.String()
		if !strings.Contains(body, "auth-card") {
			t.Errorf("%s does not look like an auth page (no auth-card class)", path)
		}
	}
}

func TestRegisterThenLoginFlow(t *testing.T) {
	h := setupTestServer(t)

	// Register.
	regBody := `{"username":"alice","password":"hunter22","nickname":"Alice"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", strings.NewReader(regBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("register status = %d, body=%s", rec.Code, rec.Body.String())
	}
	env := decodeEnvelope(t, rec.Body.Bytes())
	var data struct {
		Token string `json:"token"`
		User  struct {
			ID       int64  `json:"id"`
			Username string `json:"username"`
		} `json:"user"`
	}
	if err := json.Unmarshal(env.Data, &data); err != nil {
		t.Fatalf("decode register data: %v", err)
	}
	if data.Token == "" || data.User.Username != "alice" {
		t.Fatalf("unexpected register data: %+v", data)
	}

	// Login with correct password.
	loginBody := `{"username":"alice","password":"hunter22"}`
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", strings.NewReader(loginBody))
	req.Header.Set("Content-Type", "application/json")
	rec = httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("login status = %d, body=%s", rec.Code, rec.Body.String())
	}

	// Duplicate username should 409.
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", strings.NewReader(regBody))
	req.Header.Set("Content-Type", "application/json")
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusConflict {
		t.Fatalf("duplicate register status = %d, want 409", rec.Code)
	}

	// Short password should 400.
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/register",
		strings.NewReader(`{"username":"bob","password":"123"}`))
	req.Header.Set("Content-Type", "application/json")
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("short password status = %d, want 400", rec.Code)
	}
}

func TestLoginWrongPasswordTriggersLimiter(t *testing.T) {
	h := setupTestServer(t)

	// First register a user.
	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/register",
		strings.NewReader(`{"username":"carol","password":"hunter22"}`))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("register carol: %d %s", rec.Code, rec.Body.String())
	}

	bad := `{"username":"carol","password":"wrong"}`
	// Limiter config: 5 fails → block. After 5 failed attempts, the 6th
	// should return 429.
	for i := 0; i < 5; i++ {
		req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", strings.NewReader(bad))
		req.Header.Set("Content-Type", "application/json")
		rec = httptest.NewRecorder()
		h.ServeHTTP(rec, req)
		if rec.Code != http.StatusUnauthorized {
			t.Fatalf("attempt %d status = %d, want 401", i+1, rec.Code)
		}
	}
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", strings.NewReader(bad))
	req.Header.Set("Content-Type", "application/json")
	rec = httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusTooManyRequests {
		t.Fatalf("6th attempt status = %d, want 429", rec.Code)
	}
	if rec.Header().Get("Retry-After") == "" {
		t.Error("expected Retry-After header on 429")
	}
}

func TestProtectedRouteRequiresBearer(t *testing.T) {
	h := setupTestServer(t)
	req := httptest.NewRequest(http.MethodGet, "/api/v1/auth/me", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("unauth status = %d, want 401", rec.Code)
	}
}

func TestMaxBodySizeEnforced(t *testing.T) {
	h := setupTestServer(t)
	big := strings.Repeat("a", 2*1024*1024) // 2 MiB > 1 MiB cap
	body := `{"username":"test","password":"` + big + `"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("oversized body status = %d, want 400", rec.Code)
	}
}
