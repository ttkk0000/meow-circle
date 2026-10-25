package identity_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
)

// 回归源码，当前任务未授权执行测试。
func browserLogin(t *testing.T) (*identity.Service, identity.LoginResult) {
	t.Helper()
	s, sms := identityFixture()
	ctx := context.Background()
	if _, err := s.IssueOTP(ctx, 0, "browser", identity.OTPInput{Phone: "13800138000", Purpose: "login"}); err != nil {
		t.Fatal(err)
	}
	login, err := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: "13800138000", Code: sms.Code, AcceptTerms: true})
	if err != nil {
		t.Fatal(err)
	}
	return s, login
}
func TestBrowserRefreshAcrossServicesReplaysOnlyEncryptedActiveSuccessor(t *testing.T) {
	s, login := browserLogin(t)
	ctx := context.Background()
	first, err := s.BrowserRefresh(ctx, login.Token, login.RefreshToken)
	if err != nil {
		t.Fatal(err)
	}
	other := identity.New(s.Repo, string(s.Secret), nil, nil)
	replay, err := other.BrowserRefresh(ctx, login.Token, login.RefreshToken)
	if err != nil || replay.Token != first.Token || replay.RefreshToken != first.RefreshToken {
		t.Fatal("replicas issued different successors", err)
	}
	if err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		rows, e := tx.List(mobile.Sessions, mobile.Filter{})
		if e != nil {
			return e
		}
		for _, row := range rows {
			if strings.Contains(string(row.Data), first.Token) || strings.Contains(string(row.Data), first.RefreshToken) {
				t.Fatal("plaintext credentials in replay storage")
			}
		}
		return nil
	}); err != nil {
		t.Fatal(err)
	}
	if err = s.Logout(ctx, first.User.ID, first.Token, false); err != nil {
		t.Fatal(err)
	}
	if _, err = other.BrowserRefresh(ctx, login.Token, login.RefreshToken); !errors.Is(err, mobile.ErrUnauthorized) {
		t.Fatal("logout successor was replayed", err)
	}
}
func TestBrowserReplayExpiryDoesNotRelaxNativeRefresh(t *testing.T) {
	s, login := browserLogin(t)
	ctx := context.Background()
	if _, err := s.BrowserRefresh(ctx, login.Token, login.RefreshToken); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Refresh(ctx, login.Token, login.RefreshToken, "android"); !errors.Is(err, mobile.ErrUnauthorized) {
		t.Fatal("native refresh replayed browser receipt", err)
	}
	now := s.Now()
	s.Now = func() time.Time { return now.Add(31 * time.Second) }
	if _, err := s.BrowserRefresh(ctx, login.Token, login.RefreshToken); !errors.Is(err, mobile.ErrUnauthorized) {
		t.Fatal("expired browser receipt was replayed", err)
	}
}
