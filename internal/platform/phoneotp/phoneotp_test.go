package phoneotp

import (
	"os"
	"testing"
	"time"
)

func TestNormalizeAndValid(t *testing.T) {
	if g, w := NormalizePhone("+86 138 0013 8000"), "8613800138000"; g != w {
		t.Fatalf("NormalizePhone = %q want %q", g, w)
	}
	if !ValidPhone("13800138000") {
		t.Fatal("ValidPhone")
	}
	if ValidPhone("12345") {
		t.Fatal("short should be invalid")
	}
}

func TestPeekDevBypass(t *testing.T) {
	t.Setenv("MEOW_DEV_SMS_CODE", "111111")
	s := NewStore(time.Minute)
	if !s.Peek("13800138000", "111111") {
		t.Fatal("dev bypass")
	}
}

func TestIssueAndPeek(t *testing.T) {
	os.Unsetenv("MEOW_DEV_SMS_CODE")
	s := NewStore(5 * time.Minute)
	phone := "13800138000"
	code := s.Issue(phone)
	if !s.Peek(phone, code) {
		t.Fatal("peek should match")
	}
	if !s.Peek(phone, code) {
		t.Fatal("peek is idempotent")
	}
	s.Forget(phone)
	if s.Peek(phone, code) {
		t.Fatal("after forget should fail")
	}
}
