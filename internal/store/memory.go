package store

import (
	"sort"
	"strings"
	"sync"
	"time"

	"bestTry/internal/domain"
)

// Compile-time check: MemoryStore implements Store.
var _ Store = (*MemoryStore)(nil)

type MemoryStore struct {
	mu sync.RWMutex

	userSeq         int64
	postSeq         int64
	commentSeq      int64
	listingSeq      int64
	mediaSeq        int64
	reportSeq       int64
	orderSeq        int64
	notificationSeq int64
	messageSeq      int64
	auditSeq        int64

	users         map[int64]domain.User
	usersByName   map[string]int64
	posts         map[int64]domain.Post
	comments      map[int64][]domain.Comment
	listings      map[int64]domain.Listing
	media         map[int64]domain.Media
	reports       map[int64]domain.Report
	orders        map[int64]domain.Order
	notifications map[int64]domain.Notification
	messages      map[int64]domain.Message
	auditLogs     map[int64]domain.AuditLog
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{
		users:         make(map[int64]domain.User),
		usersByName:   make(map[string]int64),
		posts:         make(map[int64]domain.Post),
		comments:      make(map[int64][]domain.Comment),
		listings:      make(map[int64]domain.Listing),
		media:         make(map[int64]domain.Media),
		reports:       make(map[int64]domain.Report),
		orders:        make(map[int64]domain.Order),
		notifications: make(map[int64]domain.Notification),
		messages:      make(map[int64]domain.Message),
		auditLogs:     make(map[int64]domain.AuditLog),
	}
}

func (s *MemoryStore) CreateUser(user domain.User) (domain.User, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, exists := s.usersByName[user.Username]; exists {
		return domain.User{}, false
	}
	s.userSeq++
	user.ID = s.userSeq
	user.CreatedAt = time.Now().UTC()
	s.users[user.ID] = user
	s.usersByName[user.Username] = user.ID
	return user, true
}

func (s *MemoryStore) FindUserByUsername(username string) (domain.User, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	id, ok := s.usersByName[username]
	if !ok {
		return domain.User{}, false
	}
	user, ok := s.users[id]
	return user, ok
}

func (s *MemoryStore) GetUser(id int64) (domain.User, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	user, ok := s.users[id]
	return user, ok
}

func (s *MemoryStore) UpdateUserProfile(id int64, nickname, avatarURL, bio string) (domain.User, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	u, ok := s.users[id]
	if !ok {
		return domain.User{}, false
	}
	u.Nickname = nickname
	u.AvatarURL = avatarURL
	u.Bio = bio
	s.users[id] = u
	return u, true
}

func (s *MemoryStore) CountUsers() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return len(s.users)
}

func (s *MemoryStore) GetUsers(ids []int64) map[int64]domain.User {
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make(map[int64]domain.User, len(ids))
	for _, id := range ids {
		if u, ok := s.users[id]; ok {
			out[id] = u
		}
	}
	return out
}

func (s *MemoryStore) CreatePost(input domain.Post) domain.Post {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.postSeq++
	now := time.Now().UTC()
	input.ID = s.postSeq
	input.CreatedAt = now
	input.LastReplyAt = now
	s.posts[input.ID] = input
	return input
}

func (s *MemoryStore) ListPosts() []domain.Post {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Post, 0, len(s.posts))
	for _, p := range s.posts {
		out = append(out, p)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].LastReplyAt.After(out[j].LastReplyAt)
	})
	return out
}

func (s *MemoryStore) GetPost(postID int64) (domain.Post, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	post, ok := s.posts[postID]
	return post, ok
}

func (s *MemoryStore) AddComment(input domain.Comment) (domain.Comment, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	post, ok := s.posts[input.PostID]
	if !ok {
		return domain.Comment{}, false
	}

	s.commentSeq++
	now := time.Now().UTC()
	input.ID = s.commentSeq
	input.CreatedAt = now

	s.comments[input.PostID] = append(s.comments[input.PostID], input)
	post.LastReplyAt = now
	s.posts[input.PostID] = post
	return input, true
}

func (s *MemoryStore) ListCommentsByPost(postID int64) []domain.Comment {
	s.mu.RLock()
	defer s.mu.RUnlock()

	src := s.comments[postID]
	out := make([]domain.Comment, len(src))
	copy(out, src)
	return out
}

func (s *MemoryStore) ListPostsByAuthor(authorID int64) []domain.Post {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Post, 0)
	for _, p := range s.posts {
		if p.AuthorID == authorID {
			out = append(out, p)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) ListListingsBySeller(sellerID int64) []domain.Listing {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Listing, 0)
	for _, l := range s.listings {
		if l.SellerID == sellerID {
			out = append(out, l)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) GetListing(listingID int64) (domain.Listing, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	listing, ok := s.listings[listingID]
	return listing, ok
}

func (s *MemoryStore) ListAllComments() []domain.Comment {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Comment, 0)
	for _, comments := range s.comments {
		out = append(out, comments...)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) CreateListing(input domain.Listing) domain.Listing {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.listingSeq++
	input.ID = s.listingSeq
	input.CreatedAt = time.Now().UTC()
	if input.Currency == "" {
		input.Currency = "CNY"
	}
	s.listings[input.ID] = input
	return input
}

func (s *MemoryStore) ListListings() []domain.Listing {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Listing, 0, len(s.listings))
	for _, listing := range s.listings {
		out = append(out, listing)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) DeletePost(postID int64) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, ok := s.posts[postID]; !ok {
		return false
	}
	delete(s.posts, postID)
	delete(s.comments, postID)
	return true
}

func (s *MemoryStore) DeleteComment(commentID int64) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	for postID, comments := range s.comments {
		for i, comment := range comments {
			if comment.ID != commentID {
				continue
			}

			updated := append(comments[:i], comments[i+1:]...)
			s.comments[postID] = updated
			return true
		}
	}
	return false
}

func (s *MemoryStore) DeleteListing(listingID int64) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, ok := s.listings[listingID]; !ok {
		return false
	}
	delete(s.listings, listingID)
	return true
}

func (s *MemoryStore) UpdatePost(post domain.Post) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, ok := s.posts[post.ID]; !ok {
		return false
	}
	s.posts[post.ID] = post
	return true
}

func (s *MemoryStore) UpdateListing(listing domain.Listing) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, ok := s.listings[listing.ID]; !ok {
		return false
	}
	s.listings[listing.ID] = listing
	return true
}

// ===== Media =====

func (s *MemoryStore) CreateMedia(media domain.Media) domain.Media {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.mediaSeq++
	media.ID = s.mediaSeq
	media.CreatedAt = time.Now().UTC()
	s.media[media.ID] = media
	return media
}

func (s *MemoryStore) GetMedia(id int64) (domain.Media, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	m, ok := s.media[id]
	return m, ok
}

func (s *MemoryStore) GetMediaBatch(ids []int64) []domain.Media {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Media, 0, len(ids))
	for _, id := range ids {
		if m, ok := s.media[id]; ok {
			out = append(out, m)
		}
	}
	return out
}

func (s *MemoryStore) ListMediaByOwner(ownerID int64) []domain.Media {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Media, 0)
	for _, m := range s.media {
		if m.OwnerID == ownerID {
			out = append(out, m)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) ListMediaByStatus(status domain.MediaStatus) []domain.Media {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Media, 0)
	for _, m := range s.media {
		if status == "" || m.Status == status {
			out = append(out, m)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) UpdateMediaStatus(id int64, status domain.MediaStatus) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	m, ok := s.media[id]
	if !ok {
		return false
	}
	m.Status = status
	s.media[id] = m
	return true
}

func (s *MemoryStore) DeleteMedia(id int64) (domain.Media, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	m, ok := s.media[id]
	if !ok {
		return domain.Media{}, false
	}
	delete(s.media, id)
	return m, true
}

// ===== Reports =====

func (s *MemoryStore) CreateReport(r domain.Report) domain.Report {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.reportSeq++
	now := time.Now().UTC()
	r.ID = s.reportSeq
	r.Status = domain.ReportStatusOpen
	r.CreatedAt = now
	r.UpdatedAt = now
	s.reports[r.ID] = r
	return r
}

func (s *MemoryStore) GetReport(id int64) (domain.Report, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	r, ok := s.reports[id]
	return r, ok
}

func (s *MemoryStore) ListReports(status domain.ReportStatus) []domain.Report {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Report, 0)
	for _, r := range s.reports {
		if status == "" || r.Status == status {
			out = append(out, r)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) UpdateReport(report domain.Report) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, ok := s.reports[report.ID]; !ok {
		return false
	}
	report.UpdatedAt = time.Now().UTC()
	s.reports[report.ID] = report
	return true
}

// ===== Orders =====

func (s *MemoryStore) CreateOrder(order domain.Order) domain.Order {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.orderSeq++
	now := time.Now().UTC()
	order.ID = s.orderSeq
	order.Status = domain.OrderStatusPendingPayment
	order.CreatedAt = now
	order.UpdatedAt = now
	s.orders[order.ID] = order
	return order
}

func (s *MemoryStore) GetOrder(id int64) (domain.Order, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	o, ok := s.orders[id]
	return o, ok
}

func (s *MemoryStore) UpdateOrder(order domain.Order) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, ok := s.orders[order.ID]; !ok {
		return false
	}
	order.UpdatedAt = time.Now().UTC()
	s.orders[order.ID] = order
	return true
}

func (s *MemoryStore) ListOrdersByBuyer(buyerID int64) []domain.Order {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Order, 0)
	for _, o := range s.orders {
		if o.BuyerID == buyerID {
			out = append(out, o)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) ListOrdersBySeller(sellerID int64) []domain.Order {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Order, 0)
	for _, o := range s.orders {
		if o.SellerID == sellerID {
			out = append(out, o)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) ListAllOrders() []domain.Order {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]domain.Order, 0, len(s.orders))
	for _, o := range s.orders {
		out = append(out, o)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

// ===== Notifications =====

func (s *MemoryStore) CreateNotification(n domain.Notification) domain.Notification {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.notificationSeq++
	n.ID = s.notificationSeq
	n.CreatedAt = time.Now().UTC()
	s.notifications[n.ID] = n
	return n
}

func (s *MemoryStore) ListNotifications(userID int64, unreadOnly bool) []domain.Notification {
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]domain.Notification, 0)
	for _, n := range s.notifications {
		if n.UserID != userID {
			continue
		}
		if unreadOnly && n.Read {
			continue
		}
		out = append(out, n)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) CountUnreadNotifications(userID int64) int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	count := 0
	for _, n := range s.notifications {
		if n.UserID == userID && !n.Read {
			count++
		}
	}
	return count
}

func (s *MemoryStore) MarkNotificationRead(id, userID int64) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	n, ok := s.notifications[id]
	if !ok || n.UserID != userID {
		return false
	}
	n.Read = true
	s.notifications[id] = n
	return true
}

func (s *MemoryStore) MarkAllNotificationsRead(userID int64) int {
	s.mu.Lock()
	defer s.mu.Unlock()
	count := 0
	for id, n := range s.notifications {
		if n.UserID == userID && !n.Read {
			n.Read = true
			s.notifications[id] = n
			count++
		}
	}
	return count
}

// ===== Messages =====

func (s *MemoryStore) CreateMessage(m domain.Message) domain.Message {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.messageSeq++
	m.ID = s.messageSeq
	m.CreatedAt = time.Now().UTC()
	s.messages[m.ID] = m
	return m
}

func (s *MemoryStore) ListMessagesBetween(a, b int64) []domain.Message {
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]domain.Message, 0)
	for _, m := range s.messages {
		if (m.SenderID == a && m.RecipientID == b) || (m.SenderID == b && m.RecipientID == a) {
			out = append(out, m)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.Before(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) MarkConversationRead(self, peer int64) int {
	s.mu.Lock()
	defer s.mu.Unlock()
	count := 0
	for id, m := range s.messages {
		if m.RecipientID == self && m.SenderID == peer && !m.Read {
			m.Read = true
			s.messages[id] = m
			count++
		}
	}
	return count
}

func (s *MemoryStore) ListConversations(userID int64) []domain.Conversation {
	s.mu.RLock()
	msgs := make([]domain.Message, 0, len(s.messages))
	for _, m := range s.messages {
		if m.SenderID == userID || m.RecipientID == userID {
			msgs = append(msgs, m)
		}
	}
	s.mu.RUnlock()

	sort.Slice(msgs, func(i, j int) bool {
		return msgs[i].CreatedAt.Before(msgs[j].CreatedAt)
	})

	agg := make(map[int64]*domain.Conversation)
	for _, m := range msgs {
		peerID := m.SenderID
		if peerID == userID {
			peerID = m.RecipientID
		}
		conv, ok := agg[peerID]
		if !ok {
			conv = &domain.Conversation{}
			agg[peerID] = conv
		}
		conv.LastMessage = m.Content
		conv.LastSenderID = m.SenderID
		conv.UpdatedAt = m.CreatedAt
		if m.RecipientID == userID && !m.Read {
			conv.UnreadCount++
		}
	}

	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]domain.Conversation, 0, len(agg))
	for peerID, conv := range agg {
		if peer, ok := s.users[peerID]; ok {
			conv.Peer = peer
		} else {
			conv.Peer = domain.User{ID: peerID, Username: "deleted"}
		}
		out = append(out, *conv)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].UpdatedAt.After(out[j].UpdatedAt)
	})
	return out
}

// ===== Search =====

func (s *MemoryStore) SearchPosts(keyword string) []domain.Post {
	keyword = strings.ToLower(strings.TrimSpace(keyword))
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]domain.Post, 0)
	for _, p := range s.posts {
		hay := strings.ToLower(p.Title + " " + p.Content)
		if keyword == "" || strings.Contains(hay, keyword) {
			out = append(out, p)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

func (s *MemoryStore) SearchListings(keyword string) []domain.Listing {
	keyword = strings.ToLower(strings.TrimSpace(keyword))
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]domain.Listing, 0)
	for _, l := range s.listings {
		hay := strings.ToLower(l.Title + " " + l.Description)
		if keyword == "" || strings.Contains(hay, keyword) {
			out = append(out, l)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	return out
}

// ===== Audit Log =====

func (s *MemoryStore) CreateAuditLog(log domain.AuditLog) domain.AuditLog {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.auditSeq++
	log.ID = s.auditSeq
	log.CreatedAt = time.Now().UTC()
	s.auditLogs[log.ID] = log
	return log
}

func (s *MemoryStore) ListAuditLogs(limit int) []domain.AuditLog {
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]domain.AuditLog, 0, len(s.auditLogs))
	for _, l := range s.auditLogs {
		out = append(out, l)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAt.After(out[j].CreatedAt)
	})
	if limit > 0 && len(out) > limit {
		out = out[:limit]
	}
	return out
}
