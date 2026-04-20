package api

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"bestTry/internal/domain"
)

// POST /api/v1/orders              create order (buyer)
// GET  /api/v1/orders/{id}         view order (buyer or seller)
// POST /api/v1/orders/{id}/pay     pay (buyer)
// POST /api/v1/orders/{id}/cancel  cancel (buyer, only when pending_payment)
// POST /api/v1/orders/{id}/ship    ship (seller, only when paid)
// POST /api/v1/orders/{id}/complete   confirm received (buyer, only when shipped)
// POST /api/v1/orders/{id}/refund  refund (seller, when paid or shipped)

func (r *Router) handleOrders(w http.ResponseWriter, req *http.Request) {
	switch req.Method {
	case http.MethodPost:
		r.createOrder(w, req)
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func (r *Router) createOrder(w http.ResponseWriter, req *http.Request) {
	user, _ := currentUser(req)
	var payload struct {
		ListingID int64  `json:"listing_id"`
		Note      string `json:"note"`
	}
	if err := decodeJSON(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	if payload.ListingID <= 0 {
		writeError(w, http.StatusBadRequest, "listing_id is required")
		return
	}
	listing, ok := r.store.GetListing(payload.ListingID)
	if !ok {
		writeError(w, http.StatusNotFound, "listing not found")
		return
	}
	if listing.SellerID == user.ID {
		writeError(w, http.StatusBadRequest, "cannot buy your own listing")
		return
	}
	if listing.PriceCents <= 0 {
		writeError(w, http.StatusBadRequest, "listing has no price, cannot create order")
		return
	}
	currency := listing.Currency
	if currency == "" {
		currency = "CNY"
	}

	order := r.store.CreateOrder(domain.Order{
		BuyerID:      user.ID,
		SellerID:     listing.SellerID,
		ListingID:    listing.ID,
		ListingTitle: listing.Title,
		AmountCents:  listing.PriceCents,
		Currency:     currency,
		Note:         strings.TrimSpace(payload.Note),
	})
	r.notifyOrderStatus(order, "created")
	writeCreated(w, order)
}

func (r *Router) handleOrderChild(w http.ResponseWriter, req *http.Request) {
	user, _ := currentUser(req)
	path := strings.TrimPrefix(req.URL.Path, "/api/v1/orders/")
	parts := strings.Split(path, "/")
	if len(parts) == 0 || parts[0] == "" {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	id, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid order id")
		return
	}
	order, ok := r.store.GetOrder(id)
	if !ok {
		writeError(w, http.StatusNotFound, "order not found")
		return
	}
	if order.BuyerID != user.ID && order.SellerID != user.ID {
		writeError(w, http.StatusForbidden, "not order participant")
		return
	}

	if len(parts) == 1 {
		if req.Method != http.MethodGet {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		writeOK(w, order)
		return
	}

	if len(parts) != 2 {
		writeError(w, http.StatusNotFound, "not found")
		return
	}

	if req.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}

	switch parts[1] {
	case "pay":
		r.payOrder(w, req, order)
	case "cancel":
		r.cancelOrder(w, order)
	case "ship":
		r.shipOrder(w, order)
	case "complete":
		r.completeOrder(w, order)
	case "refund":
		r.refundOrder(w, order)
	default:
		writeError(w, http.StatusNotFound, "unknown action")
	}
}

func (r *Router) payOrder(w http.ResponseWriter, req *http.Request, order domain.Order) {
	user, _ := currentUser(req)
	if order.BuyerID != user.ID {
		writeError(w, http.StatusForbidden, "only buyer can pay")
		return
	}
	if order.Status != domain.OrderStatusPendingPayment {
		writeError(w, http.StatusConflict, "order is not awaiting payment")
		return
	}
	var payload struct {
		Method domain.PaymentMethod `json:"method"`
	}
	_ = decodeJSONOptional(req, &payload)
	if payload.Method == "" {
		payload.Method = domain.PaymentMethodMock
	}

	result, err := r.payments.Charge(payload.Method, order)
	if err != nil {
		writeError(w, http.StatusBadGateway, "payment failed: "+err.Error())
		return
	}
	if !result.Success {
		writeError(w, http.StatusBadGateway, "payment failed")
		return
	}

	now := result.PaidAt
	if now.IsZero() {
		now = time.Now().UTC()
	}
	order.Status = domain.OrderStatusPaid
	order.PaymentMethod = payload.Method
	order.PaymentTxID = result.TxID
	order.PaidAt = &now
	r.store.UpdateOrder(order)
	r.notifyOrderStatus(order, "paid")
	writeOK(w, order)
}

func (r *Router) cancelOrder(w http.ResponseWriter, order domain.Order) {
	if order.Status != domain.OrderStatusPendingPayment {
		writeError(w, http.StatusConflict, "only pending_payment orders can be cancelled")
		return
	}
	order.Status = domain.OrderStatusCancelled
	r.store.UpdateOrder(order)
	r.notifyOrderStatus(order, "cancelled")
	writeOK(w, order)
}

func (r *Router) shipOrder(w http.ResponseWriter, order domain.Order) {
	if order.Status != domain.OrderStatusPaid {
		writeError(w, http.StatusConflict, "only paid orders can be shipped")
		return
	}
	now := time.Now().UTC()
	order.Status = domain.OrderStatusShipped
	order.ShippedAt = &now
	r.store.UpdateOrder(order)
	r.notifyOrderStatus(order, "shipped")
	writeOK(w, order)
}

func (r *Router) completeOrder(w http.ResponseWriter, order domain.Order) {
	if order.Status != domain.OrderStatusShipped {
		writeError(w, http.StatusConflict, "only shipped orders can be completed")
		return
	}
	now := time.Now().UTC()
	order.Status = domain.OrderStatusCompleted
	order.CompletedAt = &now
	r.store.UpdateOrder(order)
	r.notifyOrderStatus(order, "completed")
	writeOK(w, order)
}

func (r *Router) refundOrder(w http.ResponseWriter, order domain.Order) {
	switch order.Status {
	case domain.OrderStatusPaid, domain.OrderStatusShipped:
	default:
		writeError(w, http.StatusConflict, "only paid/shipped orders can be refunded")
		return
	}
	_, err := r.payments.Refund(order.PaymentMethod, order)
	if err != nil {
		writeError(w, http.StatusBadGateway, "refund failed: "+err.Error())
		return
	}
	order.Status = domain.OrderStatusRefunded
	r.store.UpdateOrder(order)
	r.notifyOrderStatus(order, "refunded")
	writeOK(w, order)
}

// GET /api/v1/me/orders?role=buyer|seller
func (r *Router) handleMyOrders(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	user, _ := currentUser(req)
	role := strings.ToLower(strings.TrimSpace(req.URL.Query().Get("role")))
	var items []domain.Order
	switch role {
	case "seller":
		items = r.store.ListOrdersBySeller(user.ID)
	default:
		items = r.store.ListOrdersByBuyer(user.ID)
	}
	writeOK(w, map[string]any{"items": items, "role": role})
}

// Admin: list all orders
func (r *Router) handleAdminOrders(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	writeOK(w, map[string]any{"items": r.store.ListAllOrders()})
}
