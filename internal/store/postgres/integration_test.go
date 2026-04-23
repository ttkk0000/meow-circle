//go:build integration

// Integration tests for the PostgreSQL-backed store. Gated behind the
// `integration` build tag so plain `go test ./...` never spins up a database.
//
// Requirements:
//   - DATABASE_URL pointing at a fresh Postgres (the test truncates tables).
//   - migrations/001_init.sql (+ 002_social.sql, 003_user_phone.sql) applied.
//   - REDIS_URL is optional; when set, the test also exercises the Redis
//     read-through cache decorator end-to-end.
//
// Run locally:
//   DATABASE_URL=postgres://meow:meowpass@localhost:5432/meow?sslmode=disable \
//   REDIS_URL=redis://localhost:6379/0 \
//   go test -tags=integration -count=1 ./internal/store/postgres/...

package postgres_test

import (
	"context"
	"os"
	"testing"
	"time"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/store"
	"kitty-circle/internal/store/cache"
	"kitty-circle/internal/store/postgres"
)

func mustDSN(t *testing.T) string {
	t.Helper()
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		t.Skip("DATABASE_URL not set — skipping integration tests")
	}
	return dsn
}

func freshStore(t *testing.T) *postgres.Store {
	t.Helper()
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	pg, err := postgres.New(ctx, mustDSN(t))
	if err != nil {
		t.Fatalf("connect postgres: %v", err)
	}
	t.Cleanup(pg.Close)
	if err := pg.TruncateAllForTest(context.Background()); err != nil {
		t.Fatalf("truncate: %v", err)
	}
	return pg
}

func TestPostgresHappyPath(t *testing.T) {
	pg := freshStore(t)
	runHappyPath(t, pg)
}

func TestPostgresWithRedisCache(t *testing.T) {
	redisURL := os.Getenv("REDIS_URL")
	if redisURL == "" {
		t.Skip("REDIS_URL not set — skipping cache integration")
	}
	pg := freshStore(t)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	cached, err := cache.New(ctx, redisURL, pg)
	if err != nil {
		t.Fatalf("connect redis: %v", err)
	}
	t.Cleanup(func() { _ = cached.Close() })

	runHappyPath(t, cached)
}

// runHappyPath exercises the most common write + read paths that the HTTP
// layer relies on. It is intentionally conservative: we only assert shape,
// not exhaustive field-by-field equality, because the store layer is allowed
// to fill in defaults (created_at, IDs, …).
func runHappyPath(t *testing.T, s store.Store) {
	t.Helper()

	// --- users -------------------------------------------------------------
	alice, ok := s.CreateUser(domain.User{
		Username: "alice", Nickname: "Alice",
		PasswordHash: "hash", PasswordSalt: "salt",
	})
	if !ok || alice.ID == 0 {
		t.Fatalf("CreateUser(alice): ok=%v id=%d", ok, alice.ID)
	}
	bob, ok := s.CreateUser(domain.User{
		Username: "bob", Nickname: "Bob",
		PasswordHash: "hash", PasswordSalt: "salt",
	})
	if !ok || bob.ID == 0 {
		t.Fatalf("CreateUser(bob): ok=%v id=%d", ok, bob.ID)
	}
	if n := s.CountUsers(); n != 2 {
		t.Errorf("CountUsers: got %d, want 2", n)
	}
	if got, ok := s.FindUserByUsername("alice"); !ok || got.ID != alice.ID {
		t.Errorf("FindUserByUsername(alice): ok=%v id=%d, want id=%d", ok, got.ID, alice.ID)
	}

	// --- posts + comments --------------------------------------------------
	post := s.CreatePost(domain.Post{
		AuthorID: alice.ID,
		Title:    "My first cat post",
		Content:  "meow",
		Category: domain.CategoryDailyShare,
		Tags:     []string{"cat", "orange"},
	})
	if post.ID == 0 {
		t.Fatal("CreatePost returned zero ID")
	}
	if list := s.ListPosts(); len(list) != 1 || list[0].ID != post.ID {
		t.Errorf("ListPosts: got %+v", list)
	}
	if _, ok := s.AddComment(domain.Comment{
		PostID: post.ID, AuthorID: bob.ID, Content: "cute!",
	}); !ok {
		t.Fatal("AddComment: expected ok=true")
	}
	if cs := s.ListCommentsByPost(post.ID); len(cs) != 1 {
		t.Errorf("ListCommentsByPost: got %d, want 1", len(cs))
	}

	// --- listings ----------------------------------------------------------
	listing := s.CreateListing(domain.Listing{
		SellerID:   bob.ID,
		Type:       domain.ListingTypeProduct,
		Title:      "Cat tree",
		PriceCents: 9900,
		Currency:   "CNY",
	})
	if listing.ID == 0 {
		t.Fatal("CreateListing returned zero ID")
	}
	if ls := s.ListListings(); len(ls) != 1 {
		t.Errorf("ListListings: got %d, want 1", len(ls))
	}

	// --- notifications -----------------------------------------------------
	_ = s.CreateNotification(domain.Notification{
		UserID: alice.ID, Kind: "system", Title: "welcome",
	})
	if n := s.CountUnreadNotifications(alice.ID); n != 1 {
		t.Errorf("CountUnreadNotifications(alice): got %d, want 1", n)
	}
	if n := s.MarkAllNotificationsRead(alice.ID); n != 1 {
		t.Errorf("MarkAllNotificationsRead: got %d, want 1", n)
	}
	if n := s.CountUnreadNotifications(alice.ID); n != 0 {
		t.Errorf("CountUnreadNotifications after mark-all-read: got %d, want 0", n)
	}

	// --- messages ----------------------------------------------------------
	_ = s.CreateMessage(domain.Message{
		SenderID: alice.ID, RecipientID: bob.ID, Content: "hi bob",
	})
	_ = s.CreateMessage(domain.Message{
		SenderID: bob.ID, RecipientID: alice.ID, Content: "hi alice",
	})
	if msgs := s.ListMessagesBetween(alice.ID, bob.ID); len(msgs) != 2 {
		t.Errorf("ListMessagesBetween: got %d, want 2", len(msgs))
	}

	// --- audit -------------------------------------------------------------
	_ = s.CreateAuditLog(domain.AuditLog{Actor: "admin", Action: "test.ran"})
	if logs := s.ListAuditLogs(10); len(logs) != 1 {
		t.Errorf("ListAuditLogs: got %d, want 1", len(logs))
	}
}
