package api

import (
	"encoding/json"
	"fmt"
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

func TestRegisterPhoneDevCodeAndLoginByPhone(t *testing.T) {
	t.Setenv("MEOW_DEV_SMS_CODE", "999999")
	h := setupTestServer(t)

	regBody := `{"username":"phoneuser","password":"hunter22","phone":"13900001111","sms_code":"999999"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", strings.NewReader(regBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("register with phone: status=%d body=%s", rec.Code, rec.Body.String())
	}

	dup := `{"username":"other","password":"hunter22","phone":"13900001111","sms_code":"999999"}`
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", strings.NewReader(dup))
	req.Header.Set("Content-Type", "application/json")
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusConflict {
		t.Fatalf("duplicate phone register: status=%d want 409", rec.Code)
	}

	loginBody := `{"username":"13900001111","password":"hunter22"}`
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", strings.NewReader(loginBody))
	req.Header.Set("Content-Type", "application/json")
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("login by phone: status=%d body=%s", rec.Code, rec.Body.String())
	}
}

func TestSendVerificationCodeEndpoint(t *testing.T) {
	h := setupTestServer(t)
	body := `{"phone":"13800138000"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/send-verification-code", strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("send code: status=%d body=%s", rec.Code, rec.Body.String())
	}
}

func TestRegisterPhoneRequiresValidSMS(t *testing.T) {
	t.Setenv("MEOW_DEV_SMS_CODE", "")
	h := setupTestServer(t)

	send := httptest.NewRequest(http.MethodPost, "/api/v1/auth/send-verification-code",
		strings.NewReader(`{"phone":"13700137000"}`))
	send.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, send)
	if rec.Code != http.StatusOK {
		t.Fatalf("send: %d", rec.Code)
	}

	bad := `{"username":"smsbad","password":"hunter22","phone":"13700137000","sms_code":"000000"}`
	rec = httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", strings.NewReader(bad))
	req.Header.Set("Content-Type", "application/json")
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("register bad sms: status=%d want 400 body=%s", rec.Code, rec.Body.String())
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

func TestActionCountsAggregatesUnreadAndOrders(t *testing.T) {
	h := setupTestServer(t)

	register := func(username string) (string, int64) {
		t.Helper()
		body := fmt.Sprintf(`{"username":%q,"password":"hunter22","nickname":%q}`, username, username)
		req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", strings.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, req)
		if rec.Code != http.StatusCreated {
			t.Fatalf("register %s: status=%d body=%s", username, rec.Code, rec.Body.String())
		}
		env := decodeEnvelope(t, rec.Body.Bytes())
		var data struct {
			Token string `json:"token"`
			User  struct {
				ID int64 `json:"id"`
			} `json:"user"`
		}
		if err := json.Unmarshal(env.Data, &data); err != nil {
			t.Fatalf("decode register data: %v", err)
		}
		return data.Token, data.User.ID
	}

	doJSON := func(method, path, token, body string) apiEnvelope {
		t.Helper()
		req := httptest.NewRequest(method, path, strings.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		if token != "" {
			req.Header.Set("Authorization", "Bearer "+token)
		}
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, req)
		if rec.Code < 200 || rec.Code >= 300 {
			t.Fatalf("%s %s: status=%d body=%s", method, path, rec.Code, rec.Body.String())
		}
		return decodeEnvelope(t, rec.Body.Bytes())
	}

	sellerToken, sellerID := register("seller_action_counts")
	buyerToken, buyerID := register("buyer_action_counts")

	env := doJSON(
		http.MethodPost,
		"/api/v1/listings",
		sellerToken,
		`{"type":"product","title":"Action count cat toy","description":"safe toy","price_cents":6800,"currency":"CNY"}`,
	)
	var listing struct {
		ID int64 `json:"id"`
	}
	if err := json.Unmarshal(env.Data, &listing); err != nil {
		t.Fatalf("decode listing: %v", err)
	}

	env = doJSON(
		http.MethodPost,
		"/api/v1/orders",
		buyerToken,
		fmt.Sprintf(`{"listing_id":%d,"note":"badge test"}`, listing.ID),
	)
	var order struct {
		ID int64 `json:"id"`
	}
	if err := json.Unmarshal(env.Data, &order); err != nil {
		t.Fatalf("decode order: %v", err)
	}

	doJSON(
		http.MethodPost,
		"/api/v1/messages",
		sellerToken,
		fmt.Sprintf(`{"recipient_id":%d,"content":"Order is ready"}`, buyerID),
	)

	env = doJSON(http.MethodGet, "/api/v1/me/action-counts", buyerToken, "")
	var buyerCounts actionCountsResponse
	if err := json.Unmarshal(env.Data, &buyerCounts); err != nil {
		t.Fatalf("decode buyer counts: %v", err)
	}
	if buyerCounts.BuyerOrderActions != 1 || buyerCounts.OrderActions != 1 {
		t.Fatalf("buyer order actions = %+v, want buyer=1 total=1", buyerCounts)
	}
	if buyerCounts.UnreadMessages != 1 {
		t.Fatalf("buyer unread messages = %d, want 1", buyerCounts.UnreadMessages)
	}
	if buyerCounts.UnreadNotifications == 0 || buyerCounts.Total < 3 {
		t.Fatalf("buyer aggregate counts look wrong: %+v", buyerCounts)
	}

	doJSON(http.MethodPost, fmt.Sprintf("/api/v1/orders/%d/pay", order.ID), buyerToken, `{"method":"mock"}`)

	env = doJSON(http.MethodGet, "/api/v1/me/action-counts", sellerToken, "")
	var sellerCounts actionCountsResponse
	if err := json.Unmarshal(env.Data, &sellerCounts); err != nil {
		t.Fatalf("decode seller counts: %v", err)
	}
	if sellerCounts.SellerOrderActions != 1 || sellerCounts.OrderActions != 1 {
		t.Fatalf("seller order actions = %+v, want seller=1 total=1", sellerCounts)
	}
	if sellerCounts.UnreadMessages != 0 {
		t.Fatalf("seller unread messages = %d, want 0", sellerCounts.UnreadMessages)
	}
	if sellerID == buyerID {
		t.Fatal("test users should be distinct")
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
