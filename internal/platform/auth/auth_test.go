package auth

import (
	"testing"
	"time"
)

func TestHashAndVerifyPassword(t *testing.T) {
	hash, salt, err := HashPassword("correct-horse-battery-staple")
	if err != nil {
		t.Fatalf("hash returned error: %v", err)
	}
	if hash == "" || salt == "" {
		t.Fatal("hash/salt must not be empty")
	}
	if !VerifyPassword("correct-horse-battery-staple", hash, salt) {
		t.Error("expected correct password to verify")
	}
	if VerifyPassword("wrong-password", hash, salt) {
		t.Error("expected wrong password to fail")
	}
	// Tampered salt should produce a failed verification, not a panic.
	if VerifyPassword("correct-horse-battery-staple", hash, "deadbeef") {
		t.Error("expected tampered salt to fail verification")
	}
}

func TestTokenServiceRoundTrip(t *testing.T) {
	svc := NewTokenService("unit-test-secret", time.Hour)
	tok, err := svc.Issue(42, "alice")
	if err != nil {
		t.Fatalf("issue token: %v", err)
	}
	claims, err := svc.Parse(tok)
	if err != nil {
		t.Fatalf("parse token: %v", err)
	}
	if claims.UserID != 42 || claims.Username != "alice" {
		t.Errorf("unexpected claims: %+v", claims)
	}
}

func TestTokenServiceExpiredToken(t *testing.T) {
	svc := NewTokenService("unit-test-secret", -time.Second)
	tok, err := svc.Issue(1, "alice")
	if err != nil {
		t.Fatalf("issue token: %v", err)
	}
	if _, err := svc.Parse(tok); err == nil {
		t.Error("expected expired token to fail parsing")
	}
}

func TestTokenServiceTamperedSecret(t *testing.T) {
	a := NewTokenService("secret-a", time.Hour)
	b := NewTokenService("secret-b", time.Hour)
	tok, err := a.Issue(1, "alice")
	if err != nil {
		t.Fatalf("issue token: %v", err)
	}
	if _, err := b.Parse(tok); err == nil {
		t.Error("expected token signed with different secret to fail")
	}
}

func TestLoginLimiterBlocksAfterThreshold(t *testing.T) {
	limit := NewLoginLimiter(3, time.Minute, 100*time.Millisecond)
	key := "user@ip"

	for i := 0; i < 2; i++ {
		if ok, _ := limit.Allow(key); !ok {
			t.Fatalf("attempt %d should be allowed", i+1)
		}
		limit.RecordFailure(key)
	}
	// Third failure should trip the block.
	limit.RecordFailure(key)
	if ok, wait := limit.Allow(key); ok || wait <= 0 {
		t.Errorf("expected limiter to block after 3 failures, got ok=%v wait=%s", ok, wait)
	}

	// After block window elapses, should be allowed again.
	time.Sleep(120 * time.Millisecond)
	if ok, _ := limit.Allow(key); !ok {
		t.Error("limiter should allow after block window")
	}
}

func TestLoginLimiterSuccessResets(t *testing.T) {
	limit := NewLoginLimiter(2, time.Minute, time.Minute)
	key := "user@ip"

	limit.RecordFailure(key)
	limit.RecordSuccess(key)
	// After success, 1st failure should not immediately trip the next
	// allow (threshold is 2).
	limit.RecordFailure(key)
	if ok, _ := limit.Allow(key); !ok {
		t.Error("limiter should still allow after success reset + single failure")
	}
}
