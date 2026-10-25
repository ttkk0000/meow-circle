package identity_test

import (
	"context"
	"errors"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"testing"
	"time"
)

type ageGatewayFixture struct {
	now                       *time.Time
	creates, queries          int
	unknownCreate, wrongNonce bool
}

func (g *ageGatewayFixture) Info() identity.AgeProviderInfo {
	return identity.AgeProviderInfo{Available: true, Name: "fixture", PrivacyURL: "https://age.example.test/privacy", LaunchOrigin: "https://age.example.test"}
}
func (g *ageGatewayFixture) Create(_ context.Context, in identity.AgeProviderRequest) (identity.AgeProviderResult, error) {
	g.creates++
	if g.unknownCreate {
		return identity.AgeProviderResult{}, mobile.ErrUnavailable
	}
	return identity.AgeProviderResult{Reference: in.Reference, Nonce: in.Nonce, State: "pending", LaunchURL: "https://age.example.test/verify?reference=" + in.Reference}, nil
}
func (g *ageGatewayFixture) Query(_ context.Context, in identity.AgeProviderRequest) (identity.AgeProviderResult, error) {
	g.queries++
	nonce := in.Nonce
	if g.wrongNonce {
		nonce = "wrong-original-challenge"
	}
	return identity.AgeProviderResult{Reference: in.Reference, Nonce: nonce, State: "passed", ValidUntil: g.now.Add(24 * time.Hour), EvidenceRef: "fixture-private-proof"}, nil
}

// 以下仅回归源码，当前任务未执行测试或核验网关。
func TestAgeAssuranceConsentOriginalKeyAndPrivateTrustedResult(t *testing.T) {
	s, _ := identityFixture()
	now := s.Now()
	s.Now = func() time.Time { return now }
	gateway := &ageGatewayFixture{now: &now}
	s.Age = gateway
	ctx := context.Background()
	in := identity.AgeAssuranceInput{CommandMeta: mobile.CommandMeta{ClientRequestID: "fixture-age-001"}, TaskKey: "appointment:1:v2", TaskLabel: "原预约"}
	if _, err := s.BeginAgeAssurance(ctx, 1, in); err == nil || gateway.creates != 0 {
		t.Fatal("age check started without explicit consent")
	}
	in.Accepted = true
	first, err := s.BeginAgeAssurance(ctx, 1, in)
	if err != nil || first.State != "pending" || first.ID <= 0 {
		t.Fatal("missing original request", err)
	}
	again, err := s.BeginAgeAssurance(ctx, 1, in)
	if err != nil || again.ID != first.ID || gateway.creates != 1 {
		t.Fatal("replay created another provider request", err)
	}
	changed := in
	changed.TaskLabel = "changed original task"
	if _, err = s.BeginAgeAssurance(ctx, 1, changed); err == nil {
		t.Fatal("same key accepted a changed task")
	}
	if _, err = s.QueryAgeAssurance(ctx, 2, first.ID); !errors.Is(err, mobile.ErrNotFound) || gateway.queries != 0 {
		t.Fatal("another account queried a private request", err)
	}
	gateway.wrongNonce = true
	if _, err = s.QueryAgeAssurance(ctx, 1, first.ID); err == nil {
		t.Fatal("unmatched provider challenge was accepted")
	}
	now = now.Add(4 * time.Second)
	gateway.wrongNonce = false
	passed, err := s.QueryAgeAssurance(ctx, 1, first.ID)
	if err != nil || passed.State != "passed" {
		t.Fatal("trusted result was not applied", err)
	}
	eligible, err := mobile.New(s.Repo).Eligibility(ctx, 1)
	if err != nil || eligible.AgeBand != "adult" {
		t.Fatal("trusted proof missing", err)
	}
	if err = mobile.New(s.Repo).RecordAgeEvidence(ctx, "fixture-reviewer", mobile.AgeEvidenceInput{ActorID: 1, AgeBand: "14_17", ValidUntil: now.Add(time.Hour), EvidenceRef: "new-reviewed-proof"}); err != nil {
		t.Fatal(err)
	}
	latest, err := s.CurrentAgeAssurance(ctx, 1)
	if err != nil || latest == nil || latest.State != "superseded" {
		t.Fatal("replaced proof still displayed as current", err)
	}
}
func TestAgeUnknownCreationIsQueriedAndExpiredKeysNeverCreateAgain(t *testing.T) {
	s, _ := identityFixture()
	now := s.Now()
	s.Now = func() time.Time { return now }
	gateway := &ageGatewayFixture{now: &now, unknownCreate: true}
	s.Age = gateway
	ctx := context.Background()
	in := identity.AgeAssuranceInput{CommandMeta: mobile.CommandMeta{ClientRequestID: "fixture-age-002"}, TaskKey: "contract:1:v2", TaskLabel: "原协议", Accepted: true}
	first, err := s.BeginAgeAssurance(ctx, 1, in)
	if err != nil || first.State != "unknown" {
		t.Fatal("lost response did not preserve unknown state", err)
	}
	alias := in
	alias.ClientRequestID = "fixture-age-003"
	replay, err := s.BeginAgeAssurance(ctx, 1, alias)
	if err != nil || replay.ID != first.ID || gateway.creates != 1 {
		t.Fatal("pending check was duplicated", err)
	}
	now = now.Add(16 * time.Minute)
	expired, err := s.QueryAgeAssurance(ctx, 1, first.ID)
	if err != nil || expired.State != "expired" || gateway.queries != 0 {
		t.Fatal("expired request reached the provider", err)
	}
	if err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.AuthChallenges, first.ID)
		if e != nil {
			return e
		}
		return tx.Delete(mobile.AuthChallenges, r.ID, r.Version)
	}); err != nil {
		t.Fatal(err)
	}
	replay, err = s.BeginAgeAssurance(ctx, 1, alias)
	if err != nil || replay.State != "expired" || gateway.creates != 1 {
		t.Fatal("expired or purged original key recreated a provider request", err)
	}
}
