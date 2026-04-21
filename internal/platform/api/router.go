package api

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"bestTry/internal/domain"
	"bestTry/internal/platform/audit"
	"bestTry/internal/platform/auth"
	"bestTry/internal/platform/payment"
	"bestTry/internal/store"
	"bestTry/internal/store/cache"
	"bestTry/internal/store/postgres"
)

type contextKey string

const contextKeyUser contextKey = "current_user"

const (
	defaultSeedUsername = "demo"
	defaultSeedPassword = "123456"
	defaultSeedNickname = "Demo User"
)

// maxRequestBody caps the JSON body size accepted by non-upload endpoints.
// Media upload endpoints (multipart) enforce their own per-kind limits
// (see handleMedia) and are exempted in the ServeHTTP wrapper.
const maxRequestBody = 1 << 20 // 1 MiB

type Router struct {
	store       store.Store
	storeKind   string
	mux         *http.ServeMux
	adminKey    string
	tokens      *auth.TokenService
	payments    *payment.Router
	filter      *audit.Filter
	loginLimit  *auth.LoginLimiter
	allowOrigin string
}

func NewRouter() http.Handler {
	secret := getEnv("JWT_SECRET", "change-me-in-production")
	st, kind := buildStore()
	ensureDefaultUser(st)
	r := &Router{
		store:       st,
		storeKind:   kind,
		mux:         http.NewServeMux(),
		adminKey:    getEnv("ADMIN_KEY", "admin123"),
		tokens:      auth.NewTokenService(secret, 72*time.Hour),
		payments:    buildPaymentRouter(),
		filter:      audit.NewFilter(audit.DefaultKeywords()),
		loginLimit:  auth.NewLoginLimiter(5, 10*time.Minute, 10*time.Minute),
		allowOrigin: getEnv("CORS_ALLOW_ORIGIN", "*"),
	}
	r.routes()
	return r
}

func ensureDefaultUser(st store.Store) {
	if _, exists := st.FindUserByUsername(defaultSeedUsername); exists {
		return
	}
	hash, salt, err := auth.HashPassword(defaultSeedPassword)
	if err != nil {
		log.Printf("seed: failed to hash default user password: %v", err)
		return
	}
	if _, ok := st.CreateUser(domain.User{
		Username:     defaultSeedUsername,
		Nickname:     defaultSeedNickname,
		PasswordHash: hash,
		PasswordSalt: salt,
	}); ok {
		log.Printf("seed: default user ready (username=%s)", defaultSeedUsername)
	}
}

// buildStore wires a persistence backend based on environment variables:
//   - DATABASE_URL set        → PostgreSQL (pgx pool)
//   - REDIS_URL set in addition → Postgres wrapped by a Redis read-through cache
//   - Neither set             → In-memory store (default, no external deps)
//
// The returned kind is one of "memory" / "postgres" / "postgres+redis"
// and is surfaced via /healthz for operators.
func buildStore() (store.Store, string) {
	dsn := strings.TrimSpace(os.Getenv("DATABASE_URL"))
	redisURL := strings.TrimSpace(os.Getenv("REDIS_URL"))

	if dsn == "" {
		log.Printf("store: using in-memory backend (set DATABASE_URL to enable PostgreSQL)")
		return store.NewMemoryStore(), "memory"
	}

	pgCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	pg, err := postgres.New(pgCtx, dsn)
	if err != nil {
		log.Printf("store: postgres unavailable (%v), falling back to in-memory", err)
		return store.NewMemoryStore(), "memory"
	}
	log.Printf("store: connected to PostgreSQL")

	if redisURL == "" {
		return pg, "postgres"
	}

	rCtx, rCancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer rCancel()
	cached, err := cache.New(rCtx, redisURL, pg)
	if err != nil {
		log.Printf("store: redis unavailable (%v), serving direct Postgres", err)
		return pg, "postgres"
	}
	log.Printf("store: Redis read-through cache enabled")
	return cached, "postgres+redis"
}

// buildPaymentRouter wires the Mock provider by default, and opportunistically
// registers Alipay / WeChat / Stripe when their credentials are supplied via
// environment variables. This lets operators flip on real channels without
// code changes.
func buildPaymentRouter() *payment.Router {
	providers := []payment.Provider{payment.MockProvider{}}
	if appID := os.Getenv("ALIPAY_APP_ID"); appID != "" {
		providers = append(providers, payment.AlipayProvider{
			AppID:      appID,
			PrivateKey: os.Getenv("ALIPAY_PRIVATE_KEY"),
			Gateway:    getEnv("ALIPAY_GATEWAY", "https://openapi.alipay.com/gateway.do"),
		})
	}
	if mch := os.Getenv("WECHAT_MCH_ID"); mch != "" {
		providers = append(providers, payment.WechatPayProvider{
			MerchantID: mch,
			APIKey:     os.Getenv("WECHAT_API_KEY"),
		})
	}
	if sk := os.Getenv("STRIPE_SECRET_KEY"); sk != "" {
		providers = append(providers, payment.StripeProvider{SecretKey: sk})
	}
	return payment.NewRouter(providers...)
}

func (r *Router) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	start := time.Now()
	r.setCORSHeaders(w)
	if req.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	// Cap JSON body size for every endpoint except media upload which
	// runs its own multipart size check.
	if !strings.HasPrefix(req.URL.Path, "/api/v1/media") && req.Body != nil {
		req.Body = http.MaxBytesReader(w, req.Body, maxRequestBody)
	}
	lw := &loggingResponseWriter{ResponseWriter: w, status: http.StatusOK}
	r.mux.ServeHTTP(lw, req)

	// Access log — only for API endpoints; static files would drown the log.
	if strings.HasPrefix(req.URL.Path, "/api/") ||
		strings.HasPrefix(req.URL.Path, "/healthz") ||
		strings.HasPrefix(req.URL.Path, "/readyz") {
		log.Printf("%s %s %d %dB %s ip=%s",
			req.Method, req.URL.Path, lw.status, lw.bytes,
			time.Since(start).Truncate(time.Microsecond), clientIP(req))
	}
}

// loggingResponseWriter wraps http.ResponseWriter to capture status + size.
type loggingResponseWriter struct {
	http.ResponseWriter
	status int
	bytes  int
}

func (l *loggingResponseWriter) WriteHeader(code int) {
	l.status = code
	l.ResponseWriter.WriteHeader(code)
}

func (l *loggingResponseWriter) Write(b []byte) (int, error) {
	n, err := l.ResponseWriter.Write(b)
	l.bytes += n
	return n, err
}

func (r *Router) routes() {
	webDir := filepath.Join(getWorkingDir(), "web")
	uploadsDir := filepath.Join(getWorkingDir(), "data", "uploads")
	r.mux.Handle("/", http.FileServer(http.Dir(webDir)))
	r.mux.Handle("/uploads/", http.StripPrefix("/uploads/", http.FileServer(http.Dir(uploadsDir))))
	r.mux.HandleFunc("/admin", func(w http.ResponseWriter, req *http.Request) {
		http.ServeFile(w, req, filepath.Join(webDir, "admin.html"))
	})
	r.mux.HandleFunc("/admin/", func(w http.ResponseWriter, req *http.Request) {
		http.ServeFile(w, req, filepath.Join(webDir, "admin.html"))
	})
	r.mux.HandleFunc("/dashboard", func(w http.ResponseWriter, req *http.Request) {
		http.ServeFile(w, req, filepath.Join(webDir, "dashboard.html"))
	})
	r.mux.HandleFunc("/dashboard/", func(w http.ResponseWriter, req *http.Request) {
		http.ServeFile(w, req, filepath.Join(webDir, "dashboard.html"))
	})
	r.mux.HandleFunc("/login", func(w http.ResponseWriter, req *http.Request) {
		http.ServeFile(w, req, filepath.Join(webDir, "login.html"))
	})
	r.mux.HandleFunc("/register", func(w http.ResponseWriter, req *http.Request) {
		http.ServeFile(w, req, filepath.Join(webDir, "register.html"))
	})

	r.mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeOK(w, map[string]any{
			"status": "ok",
			"store":  r.storeKind,
			"time":   time.Now().UTC().Format(time.RFC3339),
		})
	})
	r.mux.HandleFunc("/readyz", func(w http.ResponseWriter, _ *http.Request) {
		// Liveness + lightweight readiness: does the store respond?
		r.store.CountUsers()
		writeOK(w, map[string]any{"status": "ready", "store": r.storeKind})
	})

	r.mux.HandleFunc("/api/v1/auth/register", r.handleRegister)
	r.mux.HandleFunc("/api/v1/auth/login", r.handleLogin)
	r.mux.HandleFunc("/api/v1/auth/me", r.requireAuth(r.handleMe))
	r.mux.HandleFunc("/api/v1/me", r.requireAuth(r.handleUpdateMe))
	r.mux.HandleFunc("/api/v1/users/", r.handleUserPublic)

	r.mux.HandleFunc("/api/v1/search", r.handleSearch)

	r.mux.HandleFunc("/api/v1/notifications", r.requireAuth(r.handleNotifications))
	r.mux.HandleFunc("/api/v1/notifications/", r.requireAuth(r.handleNotificationChild))

	r.mux.HandleFunc("/api/v1/messages", r.requireAuth(r.handleSendMessage))
	r.mux.HandleFunc("/api/v1/me/conversations", r.requireAuth(r.handleMyConversations))
	r.mux.HandleFunc("/api/v1/me/conversations/", r.requireAuth(r.handleConversationWithPeer))

	r.mux.HandleFunc("/api/v1/posts", r.handlePosts)
	r.mux.HandleFunc("/api/v1/posts/", r.handlePostChildren)
	r.mux.HandleFunc("/api/v1/listings", r.handleListings)
	r.mux.HandleFunc("/api/v1/listings/", r.handleListingChild)

	r.mux.HandleFunc("/api/v1/media", r.requireAuth(r.handleMedia))
	r.mux.HandleFunc("/api/v1/media/", r.handleMediaChild)

	r.mux.HandleFunc("/api/v1/reports", r.requireAuth(r.handleCreateReport))

	r.mux.HandleFunc("/api/v1/orders", r.requireAuth(r.handleOrders))
	r.mux.HandleFunc("/api/v1/orders/", r.requireAuth(r.handleOrderChild))

	r.mux.HandleFunc("/api/v1/payments/methods", r.handlePaymentMethods)

	r.mux.HandleFunc("/api/v1/me/posts", r.requireAuth(r.handleMyPosts))
	r.mux.HandleFunc("/api/v1/me/listings", r.requireAuth(r.handleMyListings))
	r.mux.HandleFunc("/api/v1/me/orders", r.requireAuth(r.handleMyOrders))

	r.mux.HandleFunc("/api/v1/admin/summary", r.handleAdminSummary)
	r.mux.HandleFunc("/api/v1/admin/posts", r.handleAdminPosts)
	r.mux.HandleFunc("/api/v1/admin/posts/", r.handleAdminPostChild)
	r.mux.HandleFunc("/api/v1/admin/comments", r.handleAdminComments)
	r.mux.HandleFunc("/api/v1/admin/comments/", r.handleAdminCommentChild)
	r.mux.HandleFunc("/api/v1/admin/listings", r.handleAdminListings)
	r.mux.HandleFunc("/api/v1/admin/listings/", r.handleAdminListingChild)
	r.mux.HandleFunc("/api/v1/admin/media", r.handleAdminMedia)
	r.mux.HandleFunc("/api/v1/admin/media/", r.handleAdminMediaChild)
	r.mux.HandleFunc("/api/v1/admin/reports", r.handleAdminReports)
	r.mux.HandleFunc("/api/v1/admin/reports/", r.handleAdminReportChild)
	r.mux.HandleFunc("/api/v1/admin/orders", r.handleAdminOrders)
	r.mux.HandleFunc("/api/v1/admin/audit-logs", r.handleAdminAudit)
}

func (r *Router) handlePaymentMethods(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{"methods": r.payments.SupportedMethods()})
}

// ===== Auth handlers =====

func (r *Router) handleRegister(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	var payload struct {
		Username string `json:"username"`
		Password string `json:"password"`
		Nickname string `json:"nickname"`
	}
	if err := decodeJSON(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	payload.Username = strings.TrimSpace(payload.Username)
	payload.Nickname = strings.TrimSpace(payload.Nickname)
	if len(payload.Username) < 3 || len(payload.Password) < 6 {
		writeError(w, http.StatusBadRequest, "username>=3 and password>=6 required")
		return
	}
	if payload.Nickname == "" {
		payload.Nickname = payload.Username
	}

	hash, salt, err := auth.HashPassword(payload.Password)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to hash password")
		return
	}

	user, ok := r.store.CreateUser(domain.User{
		Username:     payload.Username,
		Nickname:     payload.Nickname,
		PasswordHash: hash,
		PasswordSalt: salt,
	})
	if !ok {
		writeError(w, http.StatusConflict, "username already exists")
		return
	}

	token, err := r.tokens.Issue(user.ID, user.Username)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to issue token")
		return
	}

	writeCreated(w, map[string]any{
		"token": token,
		"user":  user,
	})
}

func (r *Router) handleLogin(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	var payload struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := decodeJSON(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	username := strings.TrimSpace(payload.Username)
	limiterKey := strings.ToLower(username) + "|" + clientIP(req)
	if ok, wait := r.loginLimit.Allow(limiterKey); !ok {
		w.Header().Set("Retry-After", strconv.Itoa(int(wait.Seconds())+1))
		writeError(w, http.StatusTooManyRequests, "too many failed attempts, try again later")
		return
	}

	user, ok := r.store.FindUserByUsername(username)
	if !ok {
		r.loginLimit.RecordFailure(limiterKey)
		writeError(w, http.StatusUnauthorized, "invalid username or password")
		return
	}
	if !auth.VerifyPassword(payload.Password, user.PasswordHash, user.PasswordSalt) {
		r.loginLimit.RecordFailure(limiterKey)
		writeError(w, http.StatusUnauthorized, "invalid username or password")
		return
	}

	token, err := r.tokens.Issue(user.ID, user.Username)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to issue token")
		return
	}

	r.loginLimit.RecordSuccess(limiterKey)
	writeOK(w, map[string]any{
		"token": token,
		"user":  user,
	})
}

func (r *Router) handleMe(w http.ResponseWriter, req *http.Request) {
	user, ok := currentUser(req)
	if !ok {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}
	writeOK(w, user)
}

// ===== Posts =====

func (r *Router) handlePosts(w http.ResponseWriter, req *http.Request) {
	switch req.Method {
	case http.MethodGet:
		page, size := parsePagination(req)
		all := r.store.ListPosts()
		items, total := paginate(all, page, size)
		writeOK(w, map[string]any{
			"items":     items,
			"total":     total,
			"page":      page,
			"page_size": size,
		})
	case http.MethodPost:
		claims, ok := r.parseAuth(req)
		if !ok {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		var payload struct {
			Title    string              `json:"title"`
			Content  string              `json:"content"`
			Category domain.PostCategory `json:"category"`
			Tags     []string            `json:"tags"`
			MediaIDs []int64             `json:"media_ids"`
		}
		if err := decodeJSON(req, &payload); err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}
		if strings.TrimSpace(payload.Title) == "" || strings.TrimSpace(payload.Content) == "" {
			writeError(w, http.StatusBadRequest, "title and content are required")
			return
		}
		if hit, blocked := r.filter.Check(payload.Title + " " + payload.Content); blocked {
			writeError(w, http.StatusBadRequest, "content rejected by moderation: "+hit)
			return
		}
		if payload.Category == "" {
			payload.Category = domain.CategoryDailyShare
		}
		mediaIDs, err := r.validateMediaOwnership(payload.MediaIDs, claims.UserID)
		if err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}
		post := r.store.CreatePost(domain.Post{
			AuthorID: claims.UserID,
			Title:    strings.TrimSpace(payload.Title),
			Content:  strings.TrimSpace(payload.Content),
			Category: payload.Category,
			Tags:     payload.Tags,
			MediaIDs: mediaIDs,
		})
		writeCreated(w, post)
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func (r *Router) handlePostChildren(w http.ResponseWriter, req *http.Request) {
	path := strings.TrimPrefix(req.URL.Path, "/api/v1/posts/")
	parts := strings.Split(path, "/")
	if len(parts) == 0 || parts[0] == "" {
		writeError(w, http.StatusNotFound, "not found")
		return
	}

	postID, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid post id")
		return
	}

	if len(parts) == 1 {
		switch req.Method {
		case http.MethodGet:
			post, ok := r.store.GetPost(postID)
			if !ok {
				writeError(w, http.StatusNotFound, "post not found")
				return
			}
			writeOK(w, map[string]any{
				"post":     post,
				"media":    r.store.GetMediaBatch(post.MediaIDs),
				"comments": r.store.ListCommentsByPost(postID),
			})
			return
		case http.MethodDelete:
			claims, ok := r.parseAuth(req)
			if !ok {
				writeError(w, http.StatusUnauthorized, "unauthorized")
				return
			}
			post, ok := r.store.GetPost(postID)
			if !ok {
				writeError(w, http.StatusNotFound, "post not found")
				return
			}
			if post.AuthorID != claims.UserID {
				writeError(w, http.StatusForbidden, "not post owner")
				return
			}
			r.store.DeletePost(postID)
			writeOK(w, map[string]any{"deleted": true, "id": postID})
			return
		default:
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
	}

	if len(parts) == 2 && parts[1] == "comments" {
		if req.Method != http.MethodPost {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		claims, ok := r.parseAuth(req)
		if !ok {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		var payload struct {
			Content string `json:"content"`
		}
		if err := decodeJSON(req, &payload); err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}
		if strings.TrimSpace(payload.Content) == "" {
			writeError(w, http.StatusBadRequest, "content is required")
			return
		}
		if hit, blocked := r.filter.Check(payload.Content); blocked {
			writeError(w, http.StatusBadRequest, "content rejected by moderation: "+hit)
			return
		}

		comment, ok := r.store.AddComment(domain.Comment{
			PostID:   postID,
			AuthorID: claims.UserID,
			Content:  strings.TrimSpace(payload.Content),
		})
		if !ok {
			writeError(w, http.StatusNotFound, "post not found")
			return
		}
		if post, exists := r.store.GetPost(postID); exists && post.AuthorID != claims.UserID {
			r.notify(post.AuthorID, domain.NotificationComment, "New comment on your post", comment.Content, postID)
		}
		writeCreated(w, comment)
		return
	}

	writeError(w, http.StatusNotFound, "not found")
}

// ===== Listings =====

func (r *Router) handleListings(w http.ResponseWriter, req *http.Request) {
	switch req.Method {
	case http.MethodGet:
		page, size := parsePagination(req)
		all := r.store.ListListings()
		items, total := paginateListings(all, page, size)
		writeOK(w, map[string]any{
			"items":     items,
			"total":     total,
			"page":      page,
			"page_size": size,
		})
	case http.MethodPost:
		claims, ok := r.parseAuth(req)
		if !ok {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		var payload struct {
			Type        domain.ListingType `json:"type"`
			Title       string             `json:"title"`
			Description string             `json:"description"`
			PriceCents  int64              `json:"price_cents"`
			Currency    string             `json:"currency"`
			MediaIDs    []int64            `json:"media_ids"`
		}
		if err := decodeJSON(req, &payload); err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}
		if strings.TrimSpace(payload.Title) == "" {
			writeError(w, http.StatusBadRequest, "title is required")
			return
		}
		if hit, blocked := r.filter.Check(payload.Title + " " + payload.Description); blocked {
			writeError(w, http.StatusBadRequest, "content rejected by moderation: "+hit)
			return
		}
		if payload.Type == "" {
			payload.Type = domain.ListingTypeProduct
		}
		mediaIDs, err := r.validateMediaOwnership(payload.MediaIDs, claims.UserID)
		if err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}
		listing := r.store.CreateListing(domain.Listing{
			SellerID:    claims.UserID,
			Type:        payload.Type,
			Title:       strings.TrimSpace(payload.Title),
			Description: strings.TrimSpace(payload.Description),
			PriceCents:  payload.PriceCents,
			Currency:    strings.ToUpper(strings.TrimSpace(payload.Currency)),
			MediaIDs:    mediaIDs,
		})
		writeCreated(w, listing)
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func (r *Router) handleListingChild(w http.ResponseWriter, req *http.Request) {
	listingID, err := parseID(req.URL.Path, "/api/v1/listings/")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid listing id")
		return
	}

	switch req.Method {
	case http.MethodGet:
		listing, ok := r.store.GetListing(listingID)
		if !ok {
			writeError(w, http.StatusNotFound, "listing not found")
			return
		}
		writeOK(w, map[string]any{
			"listing": listing,
			"media":   r.store.GetMediaBatch(listing.MediaIDs),
		})
		return
	case http.MethodDelete:
		claims, ok := r.parseAuth(req)
		if !ok {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		listing, ok := r.store.GetListing(listingID)
		if !ok {
			writeError(w, http.StatusNotFound, "listing not found")
			return
		}
		if listing.SellerID != claims.UserID {
			writeError(w, http.StatusForbidden, "not listing owner")
			return
		}
		r.store.DeleteListing(listingID)
		writeOK(w, map[string]any{"deleted": true, "id": listingID})
		return
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func (r *Router) handleMyPosts(w http.ResponseWriter, req *http.Request) {
	user, _ := currentUser(req)
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{"items": r.store.ListPostsByAuthor(user.ID)})
}

func (r *Router) handleMyListings(w http.ResponseWriter, req *http.Request) {
	user, _ := currentUser(req)
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{"items": r.store.ListListingsBySeller(user.ID)})
}

// ===== Admin =====

func (r *Router) handleAdminSummary(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{
		"posts":    len(r.store.ListPosts()),
		"comments": len(r.store.ListAllComments()),
		"listings": len(r.store.ListListings()),
		"users":    r.store.CountUsers(),
		"orders":   len(r.store.ListAllOrders()),
		"media":    len(r.store.ListMediaByStatus(domain.MediaStatus(""))),
		"reports":  len(r.store.ListReports(domain.ReportStatus(""))),
	})
}

func (r *Router) handleAdminPosts(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{"items": r.store.ListPosts()})
}

func (r *Router) handleAdminPostChild(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodDelete {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	postID, err := parseID(req.URL.Path, "/api/v1/admin/posts/")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid post id")
		return
	}
	if !r.store.DeletePost(postID) {
		writeError(w, http.StatusNotFound, "post not found")
		return
	}
	r.audit(req, "delete_post", "post", postID, "")
	writeOK(w, map[string]any{"deleted": true, "id": postID})
}

func (r *Router) handleAdminComments(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{"items": r.store.ListAllComments()})
}

func (r *Router) handleAdminCommentChild(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodDelete {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	commentID, err := parseID(req.URL.Path, "/api/v1/admin/comments/")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid comment id")
		return
	}
	if !r.store.DeleteComment(commentID) {
		writeError(w, http.StatusNotFound, "comment not found")
		return
	}
	r.audit(req, "delete_comment", "comment", commentID, "")
	writeOK(w, map[string]any{"deleted": true, "id": commentID})
}

func (r *Router) handleAdminListings(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{"items": r.store.ListListings()})
}

func (r *Router) handleAdminListingChild(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodDelete {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	listingID, err := parseID(req.URL.Path, "/api/v1/admin/listings/")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid listing id")
		return
	}
	if !r.store.DeleteListing(listingID) {
		writeError(w, http.StatusNotFound, "listing not found")
		return
	}
	r.audit(req, "delete_listing", "listing", listingID, "")
	writeOK(w, map[string]any{"deleted": true, "id": listingID})
}

// ===== Middleware =====

func (r *Router) requireAuth(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, req *http.Request) {
		header := req.Header.Get("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			writeError(w, http.StatusUnauthorized, "missing bearer token")
			return
		}
		token := strings.TrimSpace(strings.TrimPrefix(header, "Bearer "))
		claims, err := r.tokens.Parse(token)
		if err != nil {
			writeError(w, http.StatusUnauthorized, "invalid or expired token")
			return
		}
		user, ok := r.store.GetUser(claims.UserID)
		if !ok {
			writeError(w, http.StatusUnauthorized, "user no longer exists")
			return
		}

		ctx := context.WithValue(req.Context(), contextKeyUser, userContext{claims: claims, user: user})
		next.ServeHTTP(w, req.WithContext(ctx))
	}
}

type userContext struct {
	claims auth.Claims
	user   domain.User
}

func currentUser(req *http.Request) (domain.User, bool) {
	v, ok := req.Context().Value(contextKeyUser).(userContext)
	if !ok {
		return domain.User{}, false
	}
	return v.user, true
}

func (r *Router) parseAuth(req *http.Request) (auth.Claims, bool) {
	header := req.Header.Get("Authorization")
	if !strings.HasPrefix(header, "Bearer ") {
		return auth.Claims{}, false
	}
	token := strings.TrimSpace(strings.TrimPrefix(header, "Bearer "))
	claims, err := r.tokens.Parse(token)
	if err != nil {
		return auth.Claims{}, false
	}
	return claims, true
}

// ===== Helpers =====

func (r *Router) checkAdmin(w http.ResponseWriter, req *http.Request) bool {
	if req.Header.Get("X-Admin-Key") == r.adminKey {
		return true
	}
	writeError(w, http.StatusUnauthorized, "unauthorized")
	return false
}

func parseID(path, prefix string) (int64, error) {
	raw := strings.TrimPrefix(path, prefix)
	raw = strings.Trim(raw, "/")
	return strconv.ParseInt(raw, 10, 64)
}

func parsePagination(req *http.Request) (int, int) {
	page, _ := strconv.Atoi(req.URL.Query().Get("page"))
	size, _ := strconv.Atoi(req.URL.Query().Get("page_size"))
	if page <= 0 {
		page = 1
	}
	if size <= 0 || size > 100 {
		size = 20
	}
	return page, size
}

func paginate(all []domain.Post, page, size int) ([]domain.Post, int) {
	total := len(all)
	start := (page - 1) * size
	if start >= total {
		return []domain.Post{}, total
	}
	end := start + size
	if end > total {
		end = total
	}
	return all[start:end], total
}

func paginateListings(all []domain.Listing, page, size int) ([]domain.Listing, int) {
	total := len(all)
	start := (page - 1) * size
	if start >= total {
		return []domain.Listing{}, total
	}
	end := start + size
	if end > total {
		end = total
	}
	return all[start:end], total
}

func decodeJSON(req *http.Request, out any) error {
	defer func() { _ = req.Body.Close() }()
	decoder := json.NewDecoder(req.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(out); err != nil {
		return errors.New("invalid json payload")
	}
	return nil
}

// decodeJSONOptional accepts empty body as success.
func decodeJSONOptional(req *http.Request, out any) error {
	defer func() { _ = req.Body.Close() }()
	decoder := json.NewDecoder(req.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(out); err != nil {
		if errors.Is(err, io.EOF) {
			return nil
		}
		return errors.New("invalid json payload")
	}
	return nil
}

func (r *Router) validateMediaOwnership(ids []int64, ownerID int64) ([]int64, error) {
	if len(ids) == 0 {
		return nil, nil
	}
	seen := make(map[int64]struct{}, len(ids))
	valid := make([]int64, 0, len(ids))
	for _, id := range ids {
		if id <= 0 {
			continue
		}
		if _, ok := seen[id]; ok {
			continue
		}
		seen[id] = struct{}{}
		m, ok := r.store.GetMedia(id)
		if !ok {
			return nil, errors.New("media not found")
		}
		if m.OwnerID != ownerID {
			return nil, errors.New("not media owner")
		}
		if m.Status == domain.MediaStatusRejected {
			return nil, errors.New("media rejected by moderation")
		}
		valid = append(valid, id)
	}
	return valid, nil
}

// ===== Unified Response Envelope =====

type envelope struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    any    `json:"data,omitempty"`
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}

func writeOK(w http.ResponseWriter, data any) {
	writeJSON(w, http.StatusOK, envelope{Code: 0, Message: "ok", Data: data})
}

func writeCreated(w http.ResponseWriter, data any) {
	writeJSON(w, http.StatusCreated, envelope{Code: 0, Message: "created", Data: data})
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, envelope{Code: status, Message: message})
}

func (r *Router) setCORSHeaders(w http.ResponseWriter) {
	origin := r.allowOrigin
	if origin == "" {
		origin = "*"
	}
	w.Header().Set("Access-Control-Allow-Origin", origin)
	w.Header().Set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Admin-Key")
	if origin != "*" {
		w.Header().Set("Vary", "Origin")
	}
}

func getWorkingDir() string {
	dir, err := os.Getwd()
	if err != nil || dir == "" {
		return "."
	}
	return dir
}

func getEnv(key, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	return value
}
