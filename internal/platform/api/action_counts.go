package api

import (
	"net/http"
	"time"

	"kitty-circle/internal/domain"
)

type actionCountsResponse struct {
	UnreadNotifications int    `json:"unread_notifications"`
	UnreadMessages      int    `json:"unread_messages"`
	BuyerOrderActions   int    `json:"buyer_order_actions"`
	SellerOrderActions  int    `json:"seller_order_actions"`
	OrderActions        int    `json:"order_actions"`
	Total               int    `json:"total"`
	GeneratedAt         string `json:"generated_at"`
}

// GET /api/v1/me/action-counts
//
// Compact badge source for every client shell. It keeps notification, message,
// and order action counts in one user-scoped response so mobile/desktop/web
// navigation does not have to fan out across multiple endpoints.
func (r *Router) handleMyActionCounts(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	user, _ := currentUser(req)

	unreadNotifications := r.store.CountUnreadNotifications(user.ID)
	unreadMessages := 0
	for _, c := range r.store.ListConversations(user.ID) {
		if c.UnreadCount > 0 {
			unreadMessages += c.UnreadCount
		}
	}

	buyerActions := countBuyerOrderActions(r.store.ListOrdersByBuyer(user.ID))
	sellerActions := countSellerOrderActions(r.store.ListOrdersBySeller(user.ID))
	orderActions := buyerActions + sellerActions

	writeOK(w, actionCountsResponse{
		UnreadNotifications: unreadNotifications,
		UnreadMessages:      unreadMessages,
		BuyerOrderActions:   buyerActions,
		SellerOrderActions:  sellerActions,
		OrderActions:        orderActions,
		Total:               unreadNotifications + unreadMessages + orderActions,
		GeneratedAt:         time.Now().UTC().Format(time.RFC3339),
	})
}

func countBuyerOrderActions(items []domain.Order) int {
	count := 0
	for _, o := range items {
		switch o.Status {
		case domain.OrderStatusPendingPayment, domain.OrderStatusShipped:
			count++
		}
	}
	return count
}

func countSellerOrderActions(items []domain.Order) int {
	count := 0
	for _, o := range items {
		if o.Status == domain.OrderStatusPaid {
			count++
		}
	}
	return count
}
