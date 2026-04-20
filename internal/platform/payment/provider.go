package payment

import (
	"crypto/rand"
	"encoding/hex"
	"errors"
	"strings"
	"sync"
	"time"

	"bestTry/internal/domain"
)

// Provider is the contract every payment channel must satisfy.
// Real providers (Alipay / WeChat / Stripe) can be added by implementing this interface.
type Provider interface {
	Method() domain.PaymentMethod
	Charge(order domain.Order) (ChargeResult, error)
	Refund(order domain.Order) (RefundResult, error)
}

type ChargeResult struct {
	TxID    string
	PaidAt  time.Time
	Raw     map[string]any
	Success bool
}

type RefundResult struct {
	TxID       string
	RefundedAt time.Time
	Success    bool
}

// Router dispatches payment actions to the matching provider.
type Router struct {
	mu        sync.RWMutex
	providers map[domain.PaymentMethod]Provider
}

func NewRouter(providers ...Provider) *Router {
	r := &Router{providers: make(map[domain.PaymentMethod]Provider)}
	for _, p := range providers {
		r.Register(p)
	}
	return r
}

func (r *Router) Register(p Provider) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.providers[p.Method()] = p
}

func (r *Router) Charge(method domain.PaymentMethod, order domain.Order) (ChargeResult, error) {
	r.mu.RLock()
	p, ok := r.providers[method]
	r.mu.RUnlock()
	if !ok {
		return ChargeResult{}, errors.New("unsupported payment method")
	}
	return p.Charge(order)
}

func (r *Router) Refund(method domain.PaymentMethod, order domain.Order) (RefundResult, error) {
	r.mu.RLock()
	p, ok := r.providers[method]
	r.mu.RUnlock()
	if !ok {
		return RefundResult{}, errors.New("unsupported payment method")
	}
	return p.Refund(order)
}

func (r *Router) SupportedMethods() []domain.PaymentMethod {
	r.mu.RLock()
	defer r.mu.RUnlock()
	methods := make([]domain.PaymentMethod, 0, len(r.providers))
	for m := range r.providers {
		methods = append(methods, m)
	}
	return methods
}

// MockProvider simulates a payment channel; always succeeds unless amount <= 0.
// Useful for local development and integration tests.
type MockProvider struct{}

func (MockProvider) Method() domain.PaymentMethod { return domain.PaymentMethodMock }

func (MockProvider) Charge(order domain.Order) (ChargeResult, error) {
	if order.AmountCents <= 0 {
		return ChargeResult{}, errors.New("invalid amount")
	}
	return ChargeResult{
		TxID:    "mock_" + randomHex(10),
		PaidAt:  time.Now().UTC(),
		Success: true,
	}, nil
}

func (MockProvider) Refund(order domain.Order) (RefundResult, error) {
	if strings.TrimSpace(order.PaymentTxID) == "" {
		return RefundResult{}, errors.New("no payment tx to refund")
	}
	return RefundResult{
		TxID:       "rfnd_" + randomHex(10),
		RefundedAt: time.Now().UTC(),
		Success:    true,
	}, nil
}

// ===== Production-ready stubs =====
//
// The stubs below record the domain contract for real payment channels.
// In production, replace the body with the vendor SDK (Alipay OpenSDK,
// WeChatPay SDK, Stripe Go SDK) and wire up webhooks to mutate the order.

// AlipayProvider is a stub demonstrating the Alipay open platform integration.
type AlipayProvider struct {
	AppID      string
	PrivateKey string
	Gateway    string
}

func (AlipayProvider) Method() domain.PaymentMethod { return domain.PaymentMethodAlipay }

func (p AlipayProvider) Charge(order domain.Order) (ChargeResult, error) {
	if p.AppID == "" || p.PrivateKey == "" {
		return ChargeResult{}, errors.New("alipay not configured: set APP_ID and PRIVATE_KEY")
	}
	// Real impl: alipay.TradePrecreate / TradePagePay and return the raw body for the client to render.
	return ChargeResult{
		TxID: "alipay_pending_" + randomHex(8),
		Raw:  map[string]any{"provider": "alipay", "gateway": p.Gateway},
	}, nil
}

func (p AlipayProvider) Refund(order domain.Order) (RefundResult, error) {
	if p.AppID == "" {
		return RefundResult{}, errors.New("alipay not configured")
	}
	return RefundResult{TxID: "alipay_refund_" + randomHex(8), Success: true, RefundedAt: time.Now().UTC()}, nil
}

// WechatPayProvider is a stub for WeChat Pay v3.
type WechatPayProvider struct {
	MerchantID string
	APIKey     string
}

func (WechatPayProvider) Method() domain.PaymentMethod { return domain.PaymentMethodWechat }

func (p WechatPayProvider) Charge(order domain.Order) (ChargeResult, error) {
	if p.MerchantID == "" || p.APIKey == "" {
		return ChargeResult{}, errors.New("wechat pay not configured")
	}
	return ChargeResult{
		TxID: "wxpay_pending_" + randomHex(8),
		Raw:  map[string]any{"provider": "wechat"},
	}, nil
}

func (p WechatPayProvider) Refund(order domain.Order) (RefundResult, error) {
	if p.MerchantID == "" {
		return RefundResult{}, errors.New("wechat pay not configured")
	}
	return RefundResult{TxID: "wxpay_refund_" + randomHex(8), Success: true, RefundedAt: time.Now().UTC()}, nil
}

// StripeProvider is a stub for Stripe.
type StripeProvider struct {
	SecretKey string
}

func (StripeProvider) Method() domain.PaymentMethod { return domain.PaymentMethodStripe }

func (p StripeProvider) Charge(order domain.Order) (ChargeResult, error) {
	if p.SecretKey == "" {
		return ChargeResult{}, errors.New("stripe not configured: set STRIPE_SECRET_KEY")
	}
	// Real impl: paymentintent.New(...) → return the client_secret for the front-end.
	return ChargeResult{
		TxID: "pi_pending_" + randomHex(12),
		Raw:  map[string]any{"provider": "stripe"},
	}, nil
}

func (p StripeProvider) Refund(order domain.Order) (RefundResult, error) {
	if p.SecretKey == "" {
		return RefundResult{}, errors.New("stripe not configured")
	}
	return RefundResult{TxID: "re_" + randomHex(12), Success: true, RefundedAt: time.Now().UTC()}, nil
}

func randomHex(n int) string {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		return "000000"
	}
	return hex.EncodeToString(b)
}
