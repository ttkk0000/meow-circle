package auth

import (
	"sync"
	"time"
)

// LoginLimiter tracks failed login attempts per identity and blocks further
// attempts once the threshold is exceeded within the sliding window.
//
// This is a minimal in-memory implementation suitable for a single-node MVP.
// Swap for Redis in a clustered deployment.
type LoginLimiter struct {
	mu       sync.Mutex
	records  map[string]*attemptRecord
	maxFails int
	window   time.Duration
	blockFor time.Duration
}

type attemptRecord struct {
	fails        int
	firstAt      time.Time
	blockedUntil time.Time
}

func NewLoginLimiter(maxFails int, window, blockFor time.Duration) *LoginLimiter {
	return &LoginLimiter{
		records:  make(map[string]*attemptRecord),
		maxFails: maxFails,
		window:   window,
		blockFor: blockFor,
	}
}

// Allow reports whether an identity is currently allowed to attempt login,
// and if not, how long it must wait.
func (l *LoginLimiter) Allow(key string) (bool, time.Duration) {
	l.mu.Lock()
	defer l.mu.Unlock()
	rec, ok := l.records[key]
	if !ok {
		return true, 0
	}
	if !rec.blockedUntil.IsZero() && time.Now().Before(rec.blockedUntil) {
		return false, time.Until(rec.blockedUntil)
	}
	return true, 0
}

// RecordFailure registers a failed attempt and trips the block if the
// threshold is reached within the window.
func (l *LoginLimiter) RecordFailure(key string) {
	l.mu.Lock()
	defer l.mu.Unlock()
	now := time.Now()
	rec, ok := l.records[key]
	if !ok || now.Sub(rec.firstAt) > l.window {
		l.records[key] = &attemptRecord{fails: 1, firstAt: now}
		return
	}
	rec.fails++
	if rec.fails >= l.maxFails {
		rec.blockedUntil = now.Add(l.blockFor)
		rec.fails = 0
		rec.firstAt = now
	}
}

// RecordSuccess clears the record for the identity.
func (l *LoginLimiter) RecordSuccess(key string) {
	l.mu.Lock()
	defer l.mu.Unlock()
	delete(l.records, key)
}
