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

	"kitty-circle/internal/domain"
	"kitty-circle/internal/platform/audit"
	"kitty-circle/internal/platform/auth"
	"kitty-circle/internal/platform/payment"
	"kitty-circle/internal/platform/phoneotp"
	"kitty-circle/internal/store"
	"kitty-circle/internal/store/cache"
	"kitty-circle/internal/store/postgres"
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
	phoneOTP    *phoneotp.Store
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
		phoneOTP:    phoneotp.NewStore(10 * time.Minute),
		allowOrigin: getEnv("CORS_ALLOW_ORIGIN", "*"),
	}
	r.routes()
	return r
}

func ensureDefaultUser(st store.Store) {
	// 1. Seed Users
	usersToSeed := []struct {
		username string
		nickname string
		bio      string
		avatar   string
	}{
		{"demo", "Demo User", "Documenting the daily life of two cats.", ""},
		{"peachlatte", "桃子和拿铁", "两只猫 of the daily life of two cats.", ""},
		{"puff_bakery", "泡芙小店", "提供猫罐头和零食", ""},
		{"sunday_walk", "周日散步社", "组织小型宠物活动", ""},
		{"clean_corner", "干净角落", "二手猫用品整理中", ""},
	}

	createdUsers := make(map[string]domain.User)
	hash, salt, err := auth.HashPassword(defaultSeedPassword)
	if err != nil {
		log.Printf("seed: failed to hash default user password: %v", err)
		return
	}

	for _, u := range usersToSeed {
		existing, exists := st.FindUserByUsername(u.username)
		if exists {
			createdUsers[u.username] = existing
			continue
		}
		created, ok := st.CreateUser(domain.User{
			Username:     u.username,
			Nickname:     u.nickname,
			Bio:          u.bio,
			AvatarURL:    u.avatar,
			PasswordHash: hash,
			PasswordSalt: salt,
		})
		if ok {
			createdUsers[u.username] = created
			log.Printf("seed: user ready (username=%s)", u.username)
		}
	}

	// 2. If it's a fresh database/memory store (e.g. no posts exist), seed posts, comments, listings, and follows
	if len(st.ListPosts()) == 0 {
		// Seed Posts
		postsToSeed := []struct {
			username string
			title    string
			content  string
			category domain.PostCategory
			tags     []string
		}{
			{"peachlatte", "猫猫第一次学会开门，家里从此没有秘密", "给门把手加了保护套，顺便记录一下这个聪明小脑袋。它先观察了我们两天，然后第三天就开始自己尝试。", domain.CategoryDailyShare, []string{"日常", "聪明猫"}},
			{"puff_bakery", "猫猫新手村：接猫回家第一周需要准备什么？", "接猫回家前，猫砂盆、航空箱、幼猫粮和水碗必不可少。最重要的是给主子一个安静的角落适应新环境。", domain.CategoryHelp, []string{"新手", "养猫技巧"}},
			{"sunday_walk", "周末猫猫摄影散步局，doggie 也可以来当气氛组", "小区花园集合，拍照为主，不强社交。胆小猫可以只坐航空箱里观察。", domain.CategoryActivity, []string{"活动", "摄影"}},
			{"clean_corner", "出一个 9 成新的开放式猫砂盆，适合小户型", "已彻底清洁消毒，同城可自提，附送未拆封猫砂铲。", domain.CategoryTrade, []string{"二手", "猫砂盆"}},
		}

		createdPosts := make([]domain.Post, 0)
		postMediaMap := map[string]string{
			"猫猫第一次学会开门，家里从此没有秘密":        "mock_image_4.png",
			"猫猫新手村：接猫回家第一周需要准备什么？":      "mock_image_5.png",
			"周末猫猫摄影散步局，doggie 也可以来当气氛组": "mock_image_6.png",
			"出一个 9 成新的开放式猫砂盆，适合小户型":     "mock_image_3.png",
		}

		for _, p := range postsToSeed {
			user, ok := createdUsers[p.username]
			if !ok {
				continue
			}
			var mediaIDs []int64
			if img, exists := postMediaMap[p.title]; exists {
				med := st.CreateMedia(domain.Media{
					OwnerID:   user.ID,
					Kind:      domain.MediaKindImage,
					MIME:      "image/png",
					Filename:  img,
					URL:       "/mock-images/" + img,
					Status:    domain.MediaStatusApproved,
					CreatedAt: time.Now().UTC(),
				})
				mediaIDs = []int64{med.ID}
			}

			createdPost := st.CreatePost(domain.Post{
				AuthorID:  user.ID,
				Title:     p.title,
				Content:   p.content,
				Category:  p.category,
				Tags:      p.tags,
				MediaIDs:  mediaIDs,
				CreatedAt: time.Now().UTC(),
			})
			createdPosts = append(createdPosts, createdPost)
			log.Printf("seed: created post: %s", p.title)
		}

		// Seed Comments
		if len(createdPosts) >= 1 {
			if u, ok := createdUsers["demo"]; ok {
				st.AddComment(domain.Comment{
					PostID:    createdPosts[0].ID,
					AuthorID:  u.ID,
					Content:   "好聪明的猫猫，我家猫只会开柜门翻零食。",
					CreatedAt: time.Now().UTC(),
				})
			}
			if u, ok := createdUsers["sunday_walk"]; ok {
				st.AddComment(domain.Comment{
					PostID:    createdPosts[0].ID,
					AuthorID:  u.ID,
					Content:   "可以给它准备些益智玩具，消耗精力。",
					CreatedAt: time.Now().UTC(),
				})
			}
		}
		if len(createdPosts) >= 2 {
			if u, ok := createdUsers["peachlatte"]; ok {
				st.AddComment(domain.Comment{
					PostID:    createdPosts[1].ID,
					AuthorID:  u.ID,
					Content:   "支持，当时接我家猫的时候也是手忙脚乱，有这个指南方便多了。",
					CreatedAt: time.Now().UTC(),
				})
			}
		}

		// Seed Listings
		listingsToSeed := []struct {
			username    string
			title       string
			description string
			priceCents  int64
			listType    domain.ListingType
			category    string
		}{
			{"puff_bakery", "手工皮革狗项圈", "Verified Seller · 可发货，也支持同城自提。", 12800, domain.ListingTypeProduct, "toys"},
			{"sunday_walk", "陶瓷高脚食碗套装", "给猫猫和 doggie 都友好的稳定食碗。", 6800, domain.ListingTypeProduct, "food"},
			{"clean_corner", "高级人体工学狗胸背", "轻量透气，适合日常散步和训练。", 4500, domain.ListingTypeProduct, "apparel"},
		}

		createdListings := make(map[string]domain.Listing)
		for _, l := range listingsToSeed {
			user, ok := createdUsers[l.username]
			if !ok {
				continue
			}
			list := st.CreateListing(domain.Listing{
				SellerID:    user.ID,
				Title:       l.title,
				Description: l.description,
				PriceCents:  l.priceCents,
				Currency:    "CNY",
				Type:        l.listType,
				Category:    l.category,
				CreatedAt:   time.Now().UTC(),
			})
			createdListings[l.title] = list
			log.Printf("seed: created listing: %s", l.title)
		}

		// Seed Follows
		demoUser, demoOk := createdUsers["demo"]
		peachlatteUser, peachOk := createdUsers["peachlatte"]
		puffUser, puffOk := createdUsers["puff_bakery"]
		sundayUser, sundayOk := createdUsers["sunday_walk"]

		if demoOk && peachOk {
			st.Follow(demoUser.ID, peachlatteUser.ID)
			st.Follow(peachlatteUser.ID, demoUser.ID)
		}
		if demoOk && puffOk {
			st.Follow(demoUser.ID, puffUser.ID)
			st.Follow(puffUser.ID, demoUser.ID)
		}
		if demoOk && sundayOk {
			st.Follow(demoUser.ID, sundayUser.ID)
		}

		// Seed Orders for demo user (so that the Android app's real orders page isn't empty)
		if demoOk {
			// Order 1: Leather collar order (shipped)
			if puffUserOk, puffOk := createdUsers["puff_bakery"]; puffOk {
				listing, listingOk := createdListings["手工皮革狗项圈"]
				if listingOk {
					paidTime := time.Now().Add(-24 * time.Hour).UTC()
					st.CreateOrder(domain.Order{
						BuyerID:      demoUser.ID,
						SellerID:     puffUserOk.ID,
						ListingID:    listing.ID,
						ListingTitle: listing.Title,
						AmountCents:  listing.PriceCents,
						Currency:     "CNY",
						Status:       domain.OrderStatusShipped,
						CreatedAt:    time.Now().Add(-48 * time.Hour).UTC(),
						UpdatedAt:    time.Now().Add(-24 * time.Hour).UTC(),
						PaidAt:       &paidTime,
						ShippedAt:    &paidTime,
					})
				}
			}
			// Order 2: Ceramic bowl set (paid)
			if sundayUserOk, sundayOk := createdUsers["sunday_walk"]; sundayOk {
				listing, listingOk := createdListings["陶瓷高脚食碗套装"]
				if listingOk {
					paidTime := time.Now().Add(-4 * 24 * time.Hour).UTC()
					st.CreateOrder(domain.Order{
						BuyerID:      demoUser.ID,
						SellerID:     sundayUserOk.ID,
						ListingID:    listing.ID,
						ListingTitle: listing.Title,
						AmountCents:  listing.PriceCents,
						Currency:     "CNY",
						Status:       domain.OrderStatusPaid,
						CreatedAt:    time.Now().Add(-5 * 24 * time.Hour).UTC(),
						UpdatedAt:    time.Now().Add(-4 * 24 * time.Hour).UTC(),
						PaidAt:       &paidTime,
					})
				}
			}
			// Order 3: Ergonomic dog harness (paid)
			if cleanUserOk, cleanOk := createdUsers["clean_corner"]; cleanOk {
				listing, listingOk := createdListings["高级人体工学狗胸背"]
				if listingOk {
					paidTime := time.Now().Add(-2 * 24 * time.Hour).UTC()
					st.CreateOrder(domain.Order{
						BuyerID:      demoUser.ID,
						SellerID:     cleanUserOk.ID,
						ListingID:    listing.ID,
						ListingTitle: listing.Title,
						AmountCents:  listing.PriceCents,
						Currency:     "CNY",
						Status:       domain.OrderStatusPaid,
						CreatedAt:    time.Now().Add(-3 * 24 * time.Hour).UTC(),
						UpdatedAt:    time.Now().Add(-2 * 24 * time.Hour).UTC(),
						PaidAt:       &paidTime,
					})
				}
			}

			if puffOk {
				st.CreateMessage(domain.Message{
					SenderID:    demoUser.ID,
					RecipientID: puffUser.ID,
					Content:     "你好！我刚付了订单 #4829，已经开始期待新的喂食角落了。",
					Read:        true,
				})
				st.CreateMessage(domain.Message{
					SenderID:    puffUser.ID,
					RecipientID: demoUser.ID,
					Content:     "谢谢下单，我们已经收到付款。",
					Read:        false,
				})
				st.CreateMessage(domain.Message{
					SenderID:    puffUser.ID,
					RecipientID: demoUser.ID,
					Content:     "我会安全打包，明天上午发出。",
					Read:        false,
				})
			}
		}
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
	r.mux.HandleFunc("/api/v1/auth/send-verification-code", r.handleSendVerificationCode)
	r.mux.HandleFunc("/api/v1/auth/login", r.handleLogin)
	r.mux.HandleFunc("/api/v1/auth/me", r.requireAuth(r.handleMe))
	r.mux.HandleFunc("/api/v1/me", r.requireAuth(r.handleUpdateMe))
	r.mux.HandleFunc("/api/v1/users/", func(w http.ResponseWriter, req *http.Request) {
		path := req.URL.Path
		if strings.HasSuffix(path, "/pets") {
			r.handleUserPets(w, req)
		} else {
			r.handleUserPublic(w, req)
		}
	})

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
	r.mux.HandleFunc("/api/v1/me/follow/", r.requireAuth(r.handleMeFollow))
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

func (r *Router) handleSendVerificationCode(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	var payload struct {
		Phone string `json:"phone"`
	}
	if err := decodeJSON(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	norm := phoneotp.NormalizePhone(payload.Phone)
	if !phoneotp.ValidPhone(norm) {
		writeError(w, http.StatusBadRequest, "invalid phone number")
		return
	}
	r.phoneOTP.Issue(norm)
	writeOK(w, map[string]any{"ok": true})
}

func (r *Router) handleRegister(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	var payload struct {
		Username string `json:"username"`
		Password string `json:"password"`
		Nickname string `json:"nickname"`
		Phone    string `json:"phone"`
		SMSCode  string `json:"sms_code"`
	}
	if err := decodeJSON(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	payload.Username = strings.TrimSpace(payload.Username)
	payload.Nickname = strings.TrimSpace(payload.Nickname)
	phoneNorm := phoneotp.NormalizePhone(payload.Phone)
	if len(payload.Username) < 3 || len(payload.Password) < 6 {
		writeError(w, http.StatusBadRequest, "username>=3 and password>=6 required")
		return
	}
	if payload.Nickname == "" {
		payload.Nickname = payload.Username
	}

	if phoneNorm != "" {
		if !phoneotp.ValidPhone(phoneNorm) {
			writeError(w, http.StatusBadRequest, "invalid phone number")
			return
		}
		if _, taken := r.store.FindUserByPhone(phoneNorm); taken {
			writeError(w, http.StatusConflict, "phone already registered")
			return
		}
		if !r.phoneOTP.Peek(phoneNorm, payload.SMSCode) {
			writeError(w, http.StatusBadRequest, "invalid or expired verification code")
			return
		}
	}

	hash, salt, err := auth.HashPassword(payload.Password)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to hash password")
		return
	}

	user, ok := r.store.CreateUser(domain.User{
		Username:     payload.Username,
		Nickname:     payload.Nickname,
		Phone:        phoneNorm,
		PasswordHash: hash,
		PasswordSalt: salt,
	})
	if !ok {
		writeError(w, http.StatusConflict, "username already exists")
		return
	}

	if phoneNorm != "" {
		r.phoneOTP.Forget(phoneNorm)
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

	ident := strings.TrimSpace(payload.Username)
	phoneNorm := phoneotp.NormalizePhone(ident)
	var limiterKey string
	var user domain.User
	var found bool
	if phoneotp.ValidPhone(phoneNorm) {
		limiterKey = phoneNorm + "|" + clientIP(req)
		user, found = r.store.FindUserByPhone(phoneNorm)
	} else {
		limiterKey = strings.ToLower(ident) + "|" + clientIP(req)
		user, found = r.store.FindUserByUsername(ident)
	}

	if allow, wait := r.loginLimit.Allow(limiterKey); !allow {
		w.Header().Set("Retry-After", strconv.Itoa(int(wait.Seconds())+1))
		writeError(w, http.StatusTooManyRequests, "too many failed attempts, try again later")
		return
	}

	if !found {
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
		filter := normalizeFeedFilter(req.URL.Query().Get("filter"))
		var viewerID int64
		if claims, ok := r.parseAuth(req); ok {
			viewerID = claims.UserID
		}
		if filter == "follow" && viewerID == 0 {
			writeError(w, http.StatusUnauthorized, "login required for follow feed")
			return
		}
		all := r.store.ListPosts()
		prepared := r.prepareFeedPosts(all, filter, viewerID)
		page, size := parsePagination(req)
		slice, total := paginate(prepared, page, size)
		items := r.buildPostFeedItems(slice, viewerID)
		writeOK(w, map[string]any{
			"items":     items,
			"total":     total,
			"page":      page,
			"page_size": size,
			"filter":    filter,
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
			counts := r.store.BatchPostLikeCounts([]int64{postID})
			var viewerID int64
			if claims, ok := r.parseAuth(req); ok {
				viewerID = claims.UserID
			}
			liked := false
			if viewerID != 0 {
				lm := r.store.BatchUserLikedPosts(viewerID, []int64{postID})
				liked = lm[postID]
			}
			author, _ := r.store.GetUser(post.AuthorID)
			followingAuthor := false
			if viewerID != 0 && post.AuthorID != viewerID {
				followingAuthor = r.store.IsFollowing(viewerID, post.AuthorID)
			}
			rawComments := r.store.ListCommentsByPost(postID)
			commentViews := make([]map[string]any, 0, len(rawComments))
			for _, c := range rawComments {
				row := map[string]any{
					"id": c.ID, "post_id": c.PostID, "author_id": c.AuthorID,
					"content": c.Content, "created_at": c.CreatedAt,
				}
				if u, ok := r.store.GetUser(c.AuthorID); ok {
					row["author"] = u
				}
				commentViews = append(commentViews, row)
			}
			writeOK(w, map[string]any{
				"post":             post,
				"media":            r.store.GetMediaBatch(post.MediaIDs),
				"comments":         commentViews,
				"like_count":       counts[postID],
				"liked":            liked,
				"author":           author,
				"following_author": followingAuthor,
			})
			return
		case http.MethodPatch:
			claims, ok := r.parseAuth(req)
			if !ok {
				writeError(w, http.StatusUnauthorized, "unauthorized")
				return
			}
			existing, ok := r.store.GetPost(postID)
			if !ok {
				writeError(w, http.StatusNotFound, "post not found")
				return
			}
			if existing.AuthorID != claims.UserID {
				writeError(w, http.StatusForbidden, "not post owner")
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
				payload.Category = existing.Category
			}
			mediaIDs, err := r.validateMediaOwnership(payload.MediaIDs, claims.UserID)
			if err != nil {
				writeError(w, http.StatusBadRequest, err.Error())
				return
			}
			updated := existing
			updated.Title = strings.TrimSpace(payload.Title)
			updated.Content = strings.TrimSpace(payload.Content)
			updated.Category = payload.Category
			updated.Tags = payload.Tags
			updated.MediaIDs = mediaIDs
			if !r.store.UpdatePost(updated) {
				writeError(w, http.StatusInternalServerError, "update failed")
				return
			}
			if fresh, ok := r.store.GetPost(postID); ok {
				writeOK(w, fresh)
				return
			}
			writeError(w, http.StatusInternalServerError, "post not found after update")
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
			img := r.firstMediaURL(post.MediaIDs)
			var opts []NotifyOption
			if img != "" {
				opts = append(opts, notifyImageURL(img))
			}
			if actor, ok := r.store.GetUser(claims.UserID); ok {
				opts = append(opts, notifyActor(actor))
			}
			r.notify(post.AuthorID, domain.NotificationComment, "New comment on your post", comment.Content, postID, opts...)
		}
		writeCreated(w, comment)
		return
	}

	if len(parts) == 2 && parts[1] == "like" {
		if req.Method != http.MethodPost {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		claims, ok := r.parseAuth(req)
		if !ok {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		liked, count, ok := r.store.TogglePostLike(claims.UserID, postID)
		if !ok {
			writeError(w, http.StatusNotFound, "post not found")
			return
		}
		writeOK(w, map[string]any{"liked": liked, "like_count": count})
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
	case http.MethodPatch:
		claims, ok := r.parseAuth(req)
		if !ok {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		existing, ok := r.store.GetListing(listingID)
		if !ok {
			writeError(w, http.StatusNotFound, "listing not found")
			return
		}
		if existing.SellerID != claims.UserID {
			writeError(w, http.StatusForbidden, "not listing owner")
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
			payload.Type = existing.Type
		}
		if payload.Currency == "" {
			payload.Currency = existing.Currency
		} else {
			payload.Currency = strings.ToUpper(strings.TrimSpace(payload.Currency))
		}
		mediaIDs, err := r.validateMediaOwnership(payload.MediaIDs, claims.UserID)
		if err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}
		updated := existing
		updated.Type = payload.Type
		updated.Title = strings.TrimSpace(payload.Title)
		updated.Description = strings.TrimSpace(payload.Description)
		updated.PriceCents = payload.PriceCents
		updated.Currency = payload.Currency
		updated.MediaIDs = mediaIDs
		if !r.store.UpdateListing(updated) {
			writeError(w, http.StatusInternalServerError, "update failed")
			return
		}
		if fresh, ok := r.store.GetListing(listingID); ok {
			writeOK(w, fresh)
			return
		}
		writeError(w, http.StatusInternalServerError, "listing not found after update")
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
