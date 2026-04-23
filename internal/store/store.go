// Package store defines the Store interface that every persistence backend
// (memory, PostgreSQL, Redis cache decorator, …) must implement.
//
// Design notes:
//   - Signatures intentionally avoid returning error for the MVP. Backends that
//     can fail (e.g. Postgres) log the error and return a zero value / false.
//   - The interface is intentionally fat: splitting it up can be done later
//     when a single module needs a narrower slice.
package store

import "kitty-circle/internal/domain"

// Store is the abstraction over the persistence layer used by the API.
type Store interface {
	// Users
	CreateUser(user domain.User) (domain.User, bool)
	FindUserByUsername(username string) (domain.User, bool)
	FindUserByPhone(phoneNormalized string) (domain.User, bool)
	GetUser(id int64) (domain.User, bool)
	UpdateUserProfile(id int64, nickname, avatarURL, bio string) (domain.User, bool)
	CountUsers() int
	GetUsers(ids []int64) map[int64]domain.User

	// Posts
	CreatePost(input domain.Post) domain.Post
	ListPosts() []domain.Post
	ListPostsByAuthor(authorID int64) []domain.Post
	GetPost(postID int64) (domain.Post, bool)
	UpdatePost(post domain.Post) bool
	DeletePost(postID int64) bool
	SearchPosts(keyword string) []domain.Post

	// Comments
	AddComment(input domain.Comment) (domain.Comment, bool)
	ListCommentsByPost(postID int64) []domain.Comment
	ListAllComments() []domain.Comment
	DeleteComment(commentID int64) bool

	// Listings
	CreateListing(input domain.Listing) domain.Listing
	ListListings() []domain.Listing
	ListListingsBySeller(sellerID int64) []domain.Listing
	GetListing(listingID int64) (domain.Listing, bool)
	UpdateListing(listing domain.Listing) bool
	DeleteListing(listingID int64) bool
	SearchListings(keyword string) []domain.Listing

	// Media
	CreateMedia(media domain.Media) domain.Media
	GetMedia(id int64) (domain.Media, bool)
	GetMediaBatch(ids []int64) []domain.Media
	ListMediaByOwner(ownerID int64) []domain.Media
	ListMediaByStatus(status domain.MediaStatus) []domain.Media
	UpdateMediaStatus(id int64, status domain.MediaStatus) bool
	DeleteMedia(id int64) (domain.Media, bool)

	// Reports
	CreateReport(r domain.Report) domain.Report
	GetReport(id int64) (domain.Report, bool)
	ListReports(status domain.ReportStatus) []domain.Report
	UpdateReport(report domain.Report) bool

	// Orders
	CreateOrder(order domain.Order) domain.Order
	GetOrder(id int64) (domain.Order, bool)
	UpdateOrder(order domain.Order) bool
	ListOrdersByBuyer(buyerID int64) []domain.Order
	ListOrdersBySeller(sellerID int64) []domain.Order
	ListAllOrders() []domain.Order

	// Notifications
	CreateNotification(n domain.Notification) domain.Notification
	ListNotifications(userID int64, unreadOnly bool) []domain.Notification
	CountUnreadNotifications(userID int64) int
	MarkNotificationRead(id, userID int64) bool
	MarkAllNotificationsRead(userID int64) int

	// Messages
	CreateMessage(m domain.Message) domain.Message
	ListMessagesBetween(a, b int64) []domain.Message
	MarkConversationRead(self, peer int64) int
	ListConversations(userID int64) []domain.Conversation

	// Audit
	CreateAuditLog(log domain.AuditLog) domain.AuditLog
	ListAuditLogs(limit int) []domain.AuditLog

	// Social (feed UI: likes, follow filter)
	Follow(followerID, followingID int64) bool
	Unfollow(followerID, followingID int64) bool
	IsFollowing(followerID, followingID int64) bool
	ListFollowingIDs(followerID int64) []int64

	TogglePostLike(userID, postID int64) (liked bool, count int64, ok bool)
	BatchPostLikeCounts(postIDs []int64) map[int64]int64
	BatchUserLikedPosts(userID int64, postIDs []int64) map[int64]bool
}
