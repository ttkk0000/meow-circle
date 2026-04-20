package api

import (
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"bestTry/internal/domain"
)

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
func (r *Router) notify(userID int64, kind domain.NotificationKind, title, body string, refID int64) {
	if userID <= 0 {
		return
	}
	r.store.CreateNotification(domain.Notification{
		UserID: userID,
		Kind:   kind,
		Title:  title,
		Body:   body,
		RefID:  refID,
	})
}

// notifyOrderStatus delivers a templated order update to both buyer and seller.
func (r *Router) notifyOrderStatus(order domain.Order, action string) {
	title := fmt.Sprintf("Order #%d %s", order.ID, action)
	body := fmt.Sprintf("%s · %.2f %s", order.ListingTitle, float64(order.AmountCents)/100, order.Currency)
	r.notify(order.BuyerID, domain.NotificationOrder, title, body, order.ID)
	if order.SellerID != order.BuyerID {
		r.notify(order.SellerID, domain.NotificationOrder, title, body, order.ID)
	}
}
