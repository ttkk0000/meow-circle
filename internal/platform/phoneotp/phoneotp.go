// Package phoneotp holds in-memory SMS verification codes for registration.
// There is no real SMS provider; codes are logged when MEOW_LOG_SMS_CODE=1.
// For local/dev, set MEOW_DEV_SMS_CODE to accept a fixed code without sending.
package phoneotp

import (
	"crypto/rand"
	"errors"
	"log"
	"math/big"
	"os"
	"strings"
	"sync"
	"time"
)

const (
	minDigits = 10
	maxDigits = 15
)

// NormalizePhone keeps ASCII digits only.
func NormalizePhone(s string) string {
	var b strings.Builder
	b.Grow(len(s))
	for _, r := range s {
		if r >= '0' && r <= '9' {
			b.WriteRune(r)
		}
	}
	return b.String()
}

// ValidPhone reports whether the normalized number looks acceptable (E.164-ish length).
func ValidPhone(norm string) bool {
	n := len(norm)
	return n >= minDigits && n <= maxDigits
}

// LooksLikePhone is true when the string is only digits (after normalize) and long enough to be a phone login.
func LooksLikePhone(s string) bool {
	return ValidPhone(NormalizePhone(s))
}

// Store keeps short-lived verification codes per normalized phone.
type Store struct {
	mu  sync.Mutex
	m   map[string]entry
	ttl time.Duration
}

type entry struct {
	code  string
	until time.Time
}

// NewStore creates a code store; ttl is how long each issued code remains valid.
func NewStore(ttl time.Duration) *Store {
	return &Store{m: make(map[string]entry), ttl: ttl}
}

func (s *Store) random6() string {
	const digits = "0123456789"
	var b strings.Builder
	b.Grow(6)
	for i := 0; i < 6; i++ {
		n, err := rand.Int(rand.Reader, big.NewInt(int64(len(digits))))
		if err != nil {
			b.WriteByte('0')
			continue
		}
		b.WriteByte(digits[n.Int64()])
	}
	return b.String()
}

// Issue generates a new code for the normalized phone and stores it until expiry.
func (s *Store) Issue(phoneNorm string) string {
	code := s.random6()
	s.mu.Lock()
	s.m[phoneNorm] = entry{code: code, until: time.Now().Add(s.ttl)}
	s.mu.Unlock()
	if os.Getenv("MEOW_LOG_SMS_CODE") == "1" {
		log.Printf("phoneotp: issued code for %s (dev log only)", phoneNorm)
		log.Printf("phoneotp: code=%s", code)
	}
	return code
}

// Peek checks the code without consuming it (use Forget after successful registration).
func (s *Store) Peek(phoneNorm, code string) bool {
	code = strings.TrimSpace(code)
	if code == "" {
		return false
	}
	if dev := strings.TrimSpace(os.Getenv("MEOW_DEV_SMS_CODE")); dev != "" && code == dev {
		return true
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	e, ok := s.m[phoneNorm]
	if !ok || time.Now().After(e.until) {
		return false
	}
	return e.code == code
}

// Forget drops a pending code after registration succeeds.
func (s *Store) Forget(phoneNorm string) {
	s.mu.Lock()
	delete(s.m, phoneNorm)
	s.mu.Unlock()
}

// ErrInvalidPhone marks a rejected phone string in API handlers.
var ErrInvalidPhone = errors.New("invalid phone number")
