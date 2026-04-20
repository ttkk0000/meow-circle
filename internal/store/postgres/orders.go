package postgres

import (
	"errors"
	"time"

	"bestTry/internal/domain"

	"github.com/jackc/pgx/v5"
)

const orderCols = "id, buyer_id, seller_id, listing_id, listing_title, amount_cents, currency, status, payment_method, payment_tx_id, note, created_at, updated_at, paid_at, shipped_at, completed_at"

func scanOrder(row pgx.Row) (domain.Order, error) {
	var o domain.Order
	var status, method string
	var paidAt, shippedAt, completedAt *time.Time
	err := row.Scan(&o.ID, &o.BuyerID, &o.SellerID, &o.ListingID, &o.ListingTitle,
		&o.AmountCents, &o.Currency, &status, &method, &o.PaymentTxID, &o.Note,
		&o.CreatedAt, &o.UpdatedAt, &paidAt, &shippedAt, &completedAt)
	if err != nil {
		return domain.Order{}, err
	}
	o.Status = domain.OrderStatus(status)
	o.PaymentMethod = domain.PaymentMethod(method)
	o.PaidAt = paidAt
	o.ShippedAt = shippedAt
	o.CompletedAt = completedAt
	return o, nil
}

func (s *Store) CreateOrder(order domain.Order) domain.Order {
	ctx, cancel := bg()
	defer cancel()
	if order.Status == "" {
		order.Status = domain.OrderStatusPendingPayment
	}
	if order.Currency == "" {
		order.Currency = "CNY"
	}
	row := s.pool.QueryRow(ctx, `
		INSERT INTO orders (buyer_id, seller_id, listing_id, listing_title, amount_cents, currency, status, payment_method, payment_tx_id, note)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
		RETURNING `+orderCols,
		order.BuyerID, order.SellerID, order.ListingID, order.ListingTitle,
		order.AmountCents, order.Currency, string(order.Status),
		string(order.PaymentMethod), order.PaymentTxID, order.Note)
	o, err := scanOrder(row)
	if err != nil {
		logErr("CreateOrder", err)
		return domain.Order{}
	}
	return o
}

func (s *Store) GetOrder(id int64) (domain.Order, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `SELECT `+orderCols+` FROM orders WHERE id=$1`, id)
	o, err := scanOrder(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetOrder", err)
		}
		return domain.Order{}, false
	}
	return o, true
}

func (s *Store) UpdateOrder(order domain.Order) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `
		UPDATE orders SET
			status=$2, payment_method=$3, payment_tx_id=$4, note=$5,
			paid_at=$6, shipped_at=$7, completed_at=$8, updated_at=now()
		WHERE id=$1`,
		order.ID, string(order.Status), string(order.PaymentMethod), order.PaymentTxID, order.Note,
		order.PaidAt, order.ShippedAt, order.CompletedAt)
	if err != nil {
		logErr("UpdateOrder", err)
		return false
	}
	return tag.RowsAffected() > 0
}

func (s *Store) ListOrdersByBuyer(buyerID int64) []domain.Order {
	return s.listOrders(`SELECT `+orderCols+` FROM orders WHERE buyer_id=$1 ORDER BY created_at DESC`, []any{buyerID})
}

func (s *Store) ListOrdersBySeller(sellerID int64) []domain.Order {
	return s.listOrders(`SELECT `+orderCols+` FROM orders WHERE seller_id=$1 ORDER BY created_at DESC`, []any{sellerID})
}

func (s *Store) ListAllOrders() []domain.Order {
	return s.listOrders(`SELECT `+orderCols+` FROM orders ORDER BY created_at DESC LIMIT 500`, nil)
}

func (s *Store) listOrders(sql string, args []any) []domain.Order {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, sql, args...)
	if err != nil {
		logErr("listOrders", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Order, 0, 16)
	for rows.Next() {
		o, err := scanOrder(rows)
		if err != nil {
			continue
		}
		out = append(out, o)
	}
	return out
}
