package identity_test

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
)

// Source regressions; no test process was run for this batch.
func TestRefreshedSessionLogoutDisablesItsPushBinding(t *testing.T) {
	s, sms := identityFixture()
	ctx := context.Background()
	if _, e := s.IssueOTP(ctx, 0, "push-fixture", identity.OTPInput{Phone: "13800138000", Purpose: "login"}); e != nil {
		t.Fatal(e)
	}
	login, e := s.PhoneLogin(ctx, identity.PhoneLogin{Phone: "13800138000", Code: sms.Code, AcceptTerms: true, Device: "fixture-phone"})
	if e != nil {
		t.Fatal(e)
	}
	m := mobile.New(s.Repo)
	device, e := m.RegisterPushDevice(mobile.WithSession(ctx, login.Token), login.User.ID, mobile.PushDeviceInput{Platform: "ios", Token: "synthetic-session-push-token", Permission: "granted", Enabled: true})
	if e != nil {
		t.Fatal(e)
	}
	fresh, e := s.Refresh(ctx, login.Token, login.RefreshToken, "fixture-phone")
	if e != nil {
		t.Fatal(e)
	}
	if e = s.Logout(mobile.WithSession(ctx, fresh.Token), fresh.User.ID, fresh.Token, false); e != nil {
		t.Fatal(e)
	}
	if e = s.Repo.View(ctx, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.PushDevices, device.ID)
		if e != nil {
			return e
		}
		v, e := mobile.Decode[mobile.PushDeviceInput](r)
		if e != nil {
			return e
		}
		if r.State != "disabled" || v.Token != "" || v.Enabled {
			t.Fatal("refreshed logout retained push token", r.State)
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
	if _, e = s.SaveProfile(mobile.WithSession(ctx, fresh.Token), fresh.User.ID, identity.ProfileInput{Nickname: "fixture", Theme: "honey", Background: "picnic"}); !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("revoked session wrote profile", e)
	}
}

type revocationFixture struct{ calls int }

func (*revocationFixture) Verify(context.Context, identity.Credential, string) (identity.Subject, error) {
	return identity.Subject{}, mobile.ErrUnavailable
}
func (p *revocationFixture) Revoke(context.Context, identity.Subject) error { p.calls++; return nil }

func TestRevocationsAreNotStarvedByUnrelatedOutboxEvents(t *testing.T) {
	s, _ := identityFixture()
	ctx := mobile.WithOperator(context.Background(), "revocation-fixture")
	p := &revocationFixture{}
	s.Providers = map[string]identity.Provider{"apple": p}
	if e := s.Repo.Update(ctx, func(tx mobile.Tx) error {
		for i := 0; i < 150; i++ {
			r := mobile.Record{Key: fmt.Sprintf("ordinary:%d", i), State: "pending"}
			if e := mobile.SetBody(&r, mobile.Event{SchemaVersion: 1, Topic: "entry.saved", AggregateKind: mobile.Entries, AggregateID: 1}); e != nil {
				return e
			}
			if e := tx.Save(mobile.Outbox, &r); e != nil {
				return e
			}
		}
		subject := identity.Subject{Provider: "apple", ID: "fixture-subject", Secret: "fixture-encrypted-credential"}
		link := mobile.Record{OwnerID: 1, State: "revoked", Key: "fixture-link"}
		if e := mobile.SetBody(&link, subject); e != nil {
			return e
		}
		if e := tx.Save(mobile.Identities, &link); e != nil {
			return e
		}
		r := mobile.Record{OwnerID: 1, RefID: link.ID, Key: fmt.Sprintf("revoke:%d:%d", link.ID, link.Version), State: "pending"}
		if e := mobile.SetBody(&r, identity.Revocation{Topic: "identity.revocation_requested", Subject: subject}); e != nil {
			return e
		}
		return tx.Save(mobile.Outbox, &r)
	}); e != nil {
		t.Fatal(e)
	}
	next, n, e := s.ProcessRevocations(ctx, 0)
	if e != nil || n != 1 || next != 0 || p.calls != 1 {
		t.Fatal(next, n, p.calls, e)
	}
	if _, n, e = s.ProcessRevocations(ctx, 0); e != nil || n != 0 || p.calls != 1 {
		t.Fatal("revocation repeated", n, p.calls, e)
	}
}
