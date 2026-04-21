// Package cache provides a Redis-backed read-through caching decorator around
// any store.Store implementation. It targets the highest-QPS endpoints only
// (feed listings, unread notification counts) and aggressively invalidates on
// writes to keep reasoning simple.
//
// Enable by exporting REDIS_URL alongside DATABASE_URL.
package cache

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"strconv"
	"time"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/store"

	"github.com/redis/go-redis/v9"
)

// Store wraps an inner store.Store with Redis caching.
type Store struct {
	inner store.Store
	rdb   *redis.Client
	ttl   time.Duration
}

// New connects to the given Redis URL (e.g. redis://localhost:6379/0) and
// returns a caching decorator around inner. The caller is responsible for
// keeping `inner` alive.
func New(ctx context.Context, url string, inner store.Store) (*Store, error) {
	opts, err := redis.ParseURL(url)
	if err != nil {
		return nil, err
	}
	rdb := redis.NewClient(opts)
	pingCtx, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()
	if err := rdb.Ping(pingCtx).Err(); err != nil {
		return nil, err
	}
	return &Store{inner: inner, rdb: rdb, ttl: 30 * time.Second}, nil
}

// Close releases the underlying Redis client.
func (s *Store) Close() error { return s.rdb.Close() }

// ===== internal helpers =====

func (s *Store) bg() (context.Context, context.CancelFunc) {
	return context.WithTimeout(context.Background(), 500*time.Millisecond)
}

func (s *Store) getJSON(key string, dst any) bool {
	ctx, cancel := s.bg()
	defer cancel()
	raw, err := s.rdb.Get(ctx, key).Bytes()
	if err != nil {
		if err != redis.Nil {
			log.Printf("cache get %s: %v", key, err)
		}
		return false
	}
	if err := json.Unmarshal(raw, dst); err != nil {
		return false
	}
	return true
}

func (s *Store) setJSON(key string, value any, ttl time.Duration) {
	ctx, cancel := s.bg()
	defer cancel()
	raw, err := json.Marshal(value)
	if err != nil {
		return
	}
	if err := s.rdb.Set(ctx, key, raw, ttl).Err(); err != nil {
		log.Printf("cache set %s: %v", key, err)
	}
}

func (s *Store) del(keys ...string) {
	if len(keys) == 0 {
		return
	}
	ctx, cancel := s.bg()
	defer cancel()
	_ = s.rdb.Del(ctx, keys...).Err()
}

// keys

const (
	keyListPosts    = "posts:list"
	keyListListings = "listings:list"
)

func keyUnreadNotif(userID int64) string {
	return "notif:unread:" + strconv.FormatInt(userID, 10)
}

func keyPost(id int64) string {
	return "post:" + strconv.FormatInt(id, 10)
}

func keyListing(id int64) string {
	return "listing:" + strconv.FormatInt(id, 10)
}

// ===== Users (passthrough) =====

func (s *Store) CreateUser(u domain.User) (domain.User, bool) { return s.inner.CreateUser(u) }
func (s *Store) FindUserByUsername(n string) (domain.User, bool) {
	return s.inner.FindUserByUsername(n)
}
func (s *Store) GetUser(id int64) (domain.User, bool)       { return s.inner.GetUser(id) }
func (s *Store) CountUsers() int                            { return s.inner.CountUsers() }
func (s *Store) GetUsers(ids []int64) map[int64]domain.User { return s.inner.GetUsers(ids) }
func (s *Store) UpdateUserProfile(id int64, n, a, b string) (domain.User, bool) {
	return s.inner.UpdateUserProfile(id, n, a, b)
}

// ===== Posts =====

func (s *Store) CreatePost(in domain.Post) domain.Post {
	out := s.inner.CreatePost(in)
	s.del(keyListPosts)
	return out
}

func (s *Store) ListPosts() []domain.Post {
	var cached []domain.Post
	if s.getJSON(keyListPosts, &cached) {
		return cached
	}
	fresh := s.inner.ListPosts()
	s.setJSON(keyListPosts, fresh, s.ttl)
	return fresh
}

func (s *Store) ListPostsByAuthor(authorID int64) []domain.Post {
	return s.inner.ListPostsByAuthor(authorID)
}

func (s *Store) GetPost(id int64) (domain.Post, bool) {
	var cached domain.Post
	if s.getJSON(keyPost(id), &cached) {
		return cached, true
	}
	p, ok := s.inner.GetPost(id)
	if ok {
		s.setJSON(keyPost(id), p, s.ttl)
	}
	return p, ok
}

func (s *Store) UpdatePost(p domain.Post) bool {
	ok := s.inner.UpdatePost(p)
	if ok {
		s.del(keyListPosts, keyPost(p.ID))
	}
	return ok
}

func (s *Store) DeletePost(id int64) bool {
	ok := s.inner.DeletePost(id)
	if ok {
		s.del(keyListPosts, keyPost(id))
	}
	return ok
}

func (s *Store) SearchPosts(q string) []domain.Post { return s.inner.SearchPosts(q) }

// ===== Comments =====

func (s *Store) AddComment(in domain.Comment) (domain.Comment, bool) {
	c, ok := s.inner.AddComment(in)
	if ok {
		s.del(keyListPosts, keyPost(in.PostID))
	}
	return c, ok
}
func (s *Store) ListCommentsByPost(id int64) []domain.Comment { return s.inner.ListCommentsByPost(id) }
func (s *Store) ListAllComments() []domain.Comment            { return s.inner.ListAllComments() }
func (s *Store) DeleteComment(id int64) bool {
	if ok := s.inner.DeleteComment(id); ok {
		s.del(keyListPosts)
		return true
	}
	return false
}

// ===== Listings =====

func (s *Store) CreateListing(in domain.Listing) domain.Listing {
	out := s.inner.CreateListing(in)
	s.del(keyListListings)
	return out
}
func (s *Store) ListListings() []domain.Listing {
	var cached []domain.Listing
	if s.getJSON(keyListListings, &cached) {
		return cached
	}
	fresh := s.inner.ListListings()
	s.setJSON(keyListListings, fresh, s.ttl)
	return fresh
}
func (s *Store) ListListingsBySeller(id int64) []domain.Listing {
	return s.inner.ListListingsBySeller(id)
}
func (s *Store) GetListing(id int64) (domain.Listing, bool) {
	var cached domain.Listing
	if s.getJSON(keyListing(id), &cached) {
		return cached, true
	}
	l, ok := s.inner.GetListing(id)
	if ok {
		s.setJSON(keyListing(id), l, s.ttl)
	}
	return l, ok
}
func (s *Store) UpdateListing(l domain.Listing) bool {
	if ok := s.inner.UpdateListing(l); ok {
		s.del(keyListListings, keyListing(l.ID))
		return true
	}
	return false
}
func (s *Store) DeleteListing(id int64) bool {
	if ok := s.inner.DeleteListing(id); ok {
		s.del(keyListListings, keyListing(id))
		return true
	}
	return false
}
func (s *Store) SearchListings(q string) []domain.Listing { return s.inner.SearchListings(q) }

// ===== Media (passthrough) =====

func (s *Store) CreateMedia(m domain.Media) domain.Media  { return s.inner.CreateMedia(m) }
func (s *Store) GetMedia(id int64) (domain.Media, bool)   { return s.inner.GetMedia(id) }
func (s *Store) GetMediaBatch(ids []int64) []domain.Media { return s.inner.GetMediaBatch(ids) }
func (s *Store) ListMediaByOwner(id int64) []domain.Media { return s.inner.ListMediaByOwner(id) }
func (s *Store) ListMediaByStatus(st domain.MediaStatus) []domain.Media {
	return s.inner.ListMediaByStatus(st)
}
func (s *Store) UpdateMediaStatus(id int64, st domain.MediaStatus) bool {
	return s.inner.UpdateMediaStatus(id, st)
}
func (s *Store) DeleteMedia(id int64) (domain.Media, bool) { return s.inner.DeleteMedia(id) }

// ===== Reports (passthrough) =====

func (s *Store) CreateReport(r domain.Report) domain.Report         { return s.inner.CreateReport(r) }
func (s *Store) GetReport(id int64) (domain.Report, bool)           { return s.inner.GetReport(id) }
func (s *Store) ListReports(st domain.ReportStatus) []domain.Report { return s.inner.ListReports(st) }
func (s *Store) UpdateReport(r domain.Report) bool                  { return s.inner.UpdateReport(r) }

// ===== Orders (passthrough) =====

func (s *Store) CreateOrder(o domain.Order) domain.Order    { return s.inner.CreateOrder(o) }
func (s *Store) GetOrder(id int64) (domain.Order, bool)     { return s.inner.GetOrder(id) }
func (s *Store) UpdateOrder(o domain.Order) bool            { return s.inner.UpdateOrder(o) }
func (s *Store) ListOrdersByBuyer(id int64) []domain.Order  { return s.inner.ListOrdersByBuyer(id) }
func (s *Store) ListOrdersBySeller(id int64) []domain.Order { return s.inner.ListOrdersBySeller(id) }
func (s *Store) ListAllOrders() []domain.Order              { return s.inner.ListAllOrders() }

// ===== Notifications =====

func (s *Store) CreateNotification(n domain.Notification) domain.Notification {
	out := s.inner.CreateNotification(n)
	s.del(keyUnreadNotif(n.UserID))
	return out
}
func (s *Store) ListNotifications(userID int64, unread bool) []domain.Notification {
	return s.inner.ListNotifications(userID, unread)
}
func (s *Store) CountUnreadNotifications(userID int64) int {
	key := keyUnreadNotif(userID)
	ctx, cancel := s.bg()
	defer cancel()
	if v, err := s.rdb.Get(ctx, key).Int(); err == nil {
		return v
	}
	fresh := s.inner.CountUnreadNotifications(userID)
	_ = s.rdb.Set(ctx, key, fresh, s.ttl).Err()
	return fresh
}
func (s *Store) MarkNotificationRead(id, userID int64) bool {
	if ok := s.inner.MarkNotificationRead(id, userID); ok {
		s.del(keyUnreadNotif(userID))
		return true
	}
	return false
}
func (s *Store) MarkAllNotificationsRead(userID int64) int {
	n := s.inner.MarkAllNotificationsRead(userID)
	if n > 0 {
		s.del(keyUnreadNotif(userID))
	}
	return n
}

// ===== Messages (passthrough with unread invalidation) =====

func (s *Store) CreateMessage(m domain.Message) domain.Message {
	out := s.inner.CreateMessage(m)
	s.del(keyUnreadNotif(m.RecipientID))
	return out
}
func (s *Store) ListMessagesBetween(a, b int64) []domain.Message {
	return s.inner.ListMessagesBetween(a, b)
}
func (s *Store) MarkConversationRead(self, peer int64) int {
	return s.inner.MarkConversationRead(self, peer)
}
func (s *Store) ListConversations(id int64) []domain.Conversation {
	return s.inner.ListConversations(id)
}

// ===== Audit (passthrough) =====

func (s *Store) CreateAuditLog(l domain.AuditLog) domain.AuditLog { return s.inner.CreateAuditLog(l) }
func (s *Store) ListAuditLogs(limit int) []domain.AuditLog        { return s.inner.ListAuditLogs(limit) }

// Stats returns a snapshot string for diagnostics.
func (s *Store) Stats() string {
	return fmt.Sprintf("cache:redis ttl=%s", s.ttl)
}
