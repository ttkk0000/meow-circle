package identity_test

import (
	"context"
	"errors"
	"fmt"
	"kitty-circle/internal/domain"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/store"
	"testing"
)

type sms struct {
	Code string
	Fail bool
}

func (s *sms) Send(_ context.Context, _, code, _ string) error {
	s.Code = code
	if s.Fail {
		return errors.New("offline")
	}
	return nil
}
func identityFixture() (*identity.Service, *sms) {
	gateway := &sms{}
	memory := store.NewMemoryStore()
	for i, phone := range []string{"13800138000", "13800138001"} {
		memory.CreateUser(domain.User{Username: fmt.Sprintf("fixture%d", i), Phone: phone})
	}
	return identity.New(memory.MobileRepository(), "test-secret-longer-than-thirty-two-bytes", gateway, nil), gateway
}
func TestOTPAttemptsPersistAndCodeIsSingleUse(t *testing.T) {
	s, gateway := identityFixture()
	ctx := context.Background()
	phone := "13800138000"
	if _, e := s.IssueOTP(ctx, 0, "127.0.0.1", identity.OTPInput{Phone: phone, Purpose: "login"}); e != nil {
		t.Fatal(e)
	}
	wrong := "000000"
	if wrong == gateway.Code {
		wrong = "999999"
	}
	for n := 0; n < 5; n++ {
		if _, e := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: phone, Code: wrong, AcceptTerms: true}); !errors.Is(e, mobile.ErrUnauthorized) {
			t.Fatalf("wrong code: %v", e)
		}
	}
	if _, e := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: phone, Code: gateway.Code, AcceptTerms: true}); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("locked code succeeded")
	}
	other := "13800138001"
	if _, e := s.IssueOTP(ctx, 0, "127.0.0.1", identity.OTPInput{Phone: other, Purpose: "login"}); e != nil {
		t.Fatal(e)
	}
	in := identity.PhoneLogin{Phone: other, Code: gateway.Code, AcceptTerms: true}
	login, e := s.PhoneLogin(ctx, in)
	if e != nil || login.Created {
		t.Fatalf("login: %v", e)
	}
	if _, e = s.PhoneLogin(ctx, in); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("OTP replay succeeded")
	}
}
func TestRefreshRotationAndLogout(t *testing.T) {
	s, gateway := identityFixture()
	ctx := context.Background()
	phone := "13800138000"
	if _, e := s.IssueOTP(ctx, 0, "client", identity.OTPInput{Phone: phone, Purpose: "login"}); e != nil {
		t.Fatal(e)
	}
	login, e := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: phone, Code: gateway.Code, AcceptTerms: true})
	if e != nil {
		t.Fatal(e)
	}
	fresh, e := s.Refresh(ctx, login.Token, login.RefreshToken, "phone")
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Authenticate(ctx, login.Token); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("rotated access token remained valid")
	}
	if _, e = s.Refresh(ctx, login.Token, login.RefreshToken, "phone"); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("refresh replay succeeded")
	}
	if e = s.Logout(ctx, fresh.User.ID, fresh.Token, false); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Authenticate(ctx, fresh.Token); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("logout did not revoke")
	}
}
func TestFailedSMSDoesNotCreateUsableChallenge(t *testing.T) {
	s, gateway := identityFixture()
	gateway.Fail = true
	ctx := context.Background()
	phone := "13800138000"
	if _, e := s.IssueOTP(ctx, 0, "client", identity.OTPInput{Phone: phone, Purpose: "login"}); !errors.Is(e, mobile.ErrUnavailable) {
		t.Fatal("delivery failure reported success")
	}
	if _, e := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: phone, Code: gateway.Code, AcceptTerms: true}); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("failed delivery challenge usable")
	}
}
func TestUnconfiguredProviderFailsClosed(t *testing.T) {
	s, _ := identityFixture()
	if _, e := s.Start(context.Background(), 0, identity.OAuthStart{Provider: "wechat"}); !errors.Is(e, mobile.ErrUnavailable) {
		t.Fatal("unconfigured provider did not fail closed")
	}
}
func TestUnknownPhoneDoesNotCreateAnAccount(t *testing.T) {
	s, gateway := identityFixture()
	ctx := context.Background()
	phone := "13800138009"
	if _, e := s.IssueOTP(ctx, 0, "new-client", identity.OTPInput{Phone: phone, Purpose: "login"}); e != nil {
		t.Fatal(e)
	}
	if _, e := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: phone, Code: gateway.Code, AcceptTerms: true}); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("implicit signup accepted", e)
	}
	if e := s.Repo.View(ctx, func(tx mobile.Tx) error { _, e := tx.UserByPhone(phone); return e }); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("unknown phone created a user", e)
	}
}
func TestScopedPasswordChangeRevokesOldSessionByEpoch(t *testing.T) {
	s, gateway := identityFixture()
	ctx := context.Background()
	phone := "13800138000"
	if _, e := s.IssueOTP(ctx, 0, "client", identity.OTPInput{Phone: phone, Purpose: "login"}); e != nil {
		t.Fatal(e)
	}
	login, e := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: phone, Code: gateway.Code})
	if e != nil {
		t.Fatal(e)
	}
	ctx = mobile.WithSession(ctx, login.Token)
	profile, e := s.Profile(ctx, login.User.ID)
	if e != nil {
		t.Fatal(e)
	}
	in := identity.AccountInput{CommandMeta: mobile.CommandMeta{ClientRequestID: "change-password-once"}, Version: profile.Version, Password: "updated-password-123"}
	scope, e := s.AccountScope(ctx, login.User.ID, "password", in)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.IssueOTP(ctx, login.User.ID, "client", identity.OTPInput{Phone: phone, Purpose: "reauth"}); e != nil {
		t.Fatal(e)
	}
	grant, e := s.Reauth(ctx, login.User.ID, identity.ReauthInput{Scope: scope, Code: gateway.Code})
	if e != nil {
		t.Fatal(e)
	}
	in.ReauthToken = grant
	if e = s.Account(ctx, login.User.ID, "password", in); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Authenticate(ctx, login.Token); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("old epoch still active", e)
	}
	receipt, e := mobile.New(s.Repo).Operation(context.Background(), login.User.ID, in.ClientRequestID)
	if e != nil || receipt.State != "committed" {
		t.Fatal(receipt, e)
	}
}
