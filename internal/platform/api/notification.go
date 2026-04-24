package api

import (
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"kitty-circle/internal/domain"
)

// NotifyOption augments an in-app notification before persistence.
type NotifyOption func(*domain.Notification)

func notifyActor(u domain.User) NotifyOption {
	return func(n *domain.Notification) {
		if u.ID <= 0 {
			return
		}
		n.ActorID = u.ID
		n.ActorUsername = u.Username
		n.ActorNickname = u.Nickname
		n.ActorAvatarURL = u.AvatarURL
	}
}

func notifyImageURL(url string) NotifyOption {
	return func(n *domain.Notification) {
		n.ImageURL = strings.TrimSpace(url)
	}
}

func (r *Router) firstMediaURL(ids []int64) string {
	for _, id := range ids {
		if id <= 0 {
			continue
		}
		if m, ok := r.store.GetMedia(id); ok && strings.TrimSpace(m.URL) != "" {
			return m.URL
		}
	}
	return ""
}

func (r *Router) handleNotifications(w http.ResponseWriter, req *http.Request) {
	user, _ := currentUser(req)
	switch req.Method {
	case http.MethodGet:
		unread := req.URL.Query().Get("unread") == "true"
		items := r.store.ListNotifications(user.ID, unread)
		writeOK(w, map[string]any{
			"items":        items,
			"unread_count": r.store.CountUnreadNotifications(user.ID),
		})
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func (r *Router) handleNotificationChild(w http.ResponseWriter, req *http.Request) {
	user, _ := currentUser(req)
	raw := strings.TrimPrefix(req.URL.Path, "/api/v1/notifications/")
	parts := strings.Split(raw, "/")
	if len(parts) == 0 || parts[0] == "" {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	if parts[0] == "read-all" {
		if req.Method != http.MethodPost {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		count := r.store.MarkAllNotificationsRead(user.ID)
		writeOK(w, map[string]any{"updated": count})
		return
	}
	id, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid notification id")
		return
	}
	if len(parts) == 2 && parts[1] == "read" {
		if req.Method != http.MethodPost {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		if !r.store.MarkNotificationRead(id, user.ID) {
			writeError(w, http.StatusNotFound, "notification not found")
			return
		}
		writeOK(w, map[string]any{"id": id, "read": true})
		return
	}
	writeError(w, http.StatusNotFound, "not found")
}

// notify is a helper wrapper to fan out an in-app notification.
// It silently ignores errors so callers can use fire-and-forget semantics.
func (r *Router) notify(userID int64, kind domain.NotificationKind, title, body string, refID int64, opts ...NotifyOption) {
	if userID <= 0 {
		return
	}
	n := domain.Notification{
		UserID: userID,
		Kind:   kind,
		Title:  title,
		Body:   body,
		RefID:  refID,
	}
	for _, o := range opts {
		o(&n)
	}
	r.store.CreateNotification(n)
}

// notifyOrderStatus delivers a templated order update to both buyer and seller.
func (r *Router) notifyOrderStatus(order domain.Order, action string) {
	title := fmt.Sprintf("Order #%d %s", order.ID, action)
	body := fmt.Sprintf("%s · %.2f %s", order.ListingTitle, float64(order.AmountCents)/100, order.Currency)
	img := ""
	if listing, ok := r.store.GetListing(order.ListingID); ok {
		img = r.firstMediaURL(listing.MediaIDs)
	}
	var opts []NotifyOption
	if img != "" {
		opts = append(opts, notifyImageURL(img))
	}
	r.notify(order.BuyerID, domain.NotificationOrder, title, body, order.ID, opts...)
	if order.SellerID != order.BuyerID {
		r.notify(order.SellerID, domain.NotificationOrder, title, body, order.ID, opts...)
	}
}
