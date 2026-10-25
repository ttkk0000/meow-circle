package identity_test

import (
	"context"
	"errors"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"net/url"
	"strings"
	"testing"
	"time"
)

// 仅回归源码；当前任务未执行测试、服务或真实关系核实。
func guardianFixture(t *testing.T) (*identity.Service, *mobile.Service, identity.GuardianInviteInfo) {
	t.Helper()
	s, _ := identityFixture()
	s.PublicAppOrigin = "https://mnd.example.test"
	business := mobile.New(s.Repo)
	business.Now = s.Now
	ctx := context.Background()
	if err := s.Repo.Update(ctx, func(tx mobile.Tx) error {
		user, err := tx.User(2)
		if err != nil {
			return err
		}
		return mobile.RecordPhoneProof(tx, 2, user.Phone, s.Now())
	}); err != nil {
		t.Fatal(err)
	}
	for id, band := range map[int64]string{1: "under14", 2: "adult"} {
		if err := business.RecordAgeEvidence(ctx, "fixture-verifier", mobile.AgeEvidenceInput{ActorID: id, AgeBand: band, ValidUntil: s.Now().Add(time.Hour), EvidenceRef: "fixture-proof"}); err != nil {
			t.Fatal(err)
		}
	}
	return s, business, identity.GuardianInviteInfo{PolicyVersion: strings.Repeat("a", 64), PolicyLabel: "fixture", PolicyKey: "teen-fixture"}
}
func inviteInput(key string, info identity.GuardianInviteInfo) identity.GuardianInviteStart {
	return identity.GuardianInviteStart{CommandMeta: mobile.CommandMeta{ClientRequestID: key}, PolicyVersion: info.PolicyVersion, Accepted: true}
}
func inviteToken(t *testing.T, value identity.GuardianInviteStatus) string {
	t.Helper()
	u, err := url.Parse(value.ShareURL)
	if err != nil {
		t.Fatal(err)
	}
	fragment, err := url.ParseQuery(u.Fragment)
	if err != nil || len(fragment.Get("code")) != 43 || u.RawQuery != "" {
		t.Fatal("invitation exposed capability in HTTP query", err)
	}
	return fragment.Get("code")
}
func TestGuardianInvitationSingleUseOwnershipAndOriginalKeys(t *testing.T) {
	s, business, info := guardianFixture(t)
	ctx := context.Background()
	in := inviteInput("guardian-create-fixture-001", info)
	unaccepted := in
	unaccepted.Accepted = false
	if _, err := s.CreateGuardianInvite(ctx, 1, info, unaccepted); err == nil {
		t.Fatal("invitation created without reading acknowledgement")
	}
	first, err := s.CreateGuardianInvite(ctx, 1, info, in)
	if err != nil || first.State != "invite_open" {
		t.Fatal("missing initial invitation", err)
	}
	token := inviteToken(t, first)
	alias, err := s.CreateGuardianInvite(ctx, 1, info, inviteInput("guardian-create-fixture-002", info))
	if err != nil || alias.ID != first.ID {
		t.Fatal("another key duplicated active invitation", err)
	}
	if _, err = s.GuardianInvite(ctx, 2, first.ID, info); !errors.Is(err, mobile.ErrNotFound) {
		t.Fatal("unclaimed invitation visible to another account", err)
	}
	claim := identity.GuardianInviteClaim{CommandMeta: mobile.CommandMeta{ClientRequestID: "guardian-claim-fixture-001"}, Token: token, PolicyVersion: info.PolicyVersion, Accepted: true}
	if _, err = s.ClaimGuardianInvite(ctx, 1, info, claim); err == nil {
		t.Fatal("child claimed own invitation")
	}
	claimed, err := s.ClaimGuardianInvite(ctx, 2, info, claim)
	if err != nil || claimed.State != "relation_pending" || claimed.ShareURL != "" {
		t.Fatal("claim skipped independent relationship review", err)
	}
	rows, err := business.Guardians(ctx, 1)
	if err != nil || len(rows) != 0 {
		t.Fatal("claim created a guardian authorization", err)
	}
	replay, err := s.ClaimGuardianInvite(ctx, 2, info, claim)
	if err != nil || replay.ID != claimed.ID {
		t.Fatal("same claim key not replayable", err)
	}
	claim.ClientRequestID = "guardian-claim-fixture-002"
	if _, err = s.ClaimGuardianInvite(ctx, 2, info, claim); err == nil {
		t.Fatal("single-use token claimed twice")
	}
	child, err := s.GuardianInvite(ctx, 1, first.ID, info)
	if err != nil || child.ShareURL != "" {
		t.Fatal("consumed token still shareable", err)
	}
	cancel := mobile.StateInput{CommandMeta: mobile.CommandMeta{ClientRequestID: "guardian-cancel-fixture-001"}, Version: claimed.Version}
	if _, err = s.CancelGuardianInvite(ctx, 2, claimed.ID, cancel, info); err != nil {
		t.Fatal(err)
	}
	if _, err = s.CancelGuardianInvite(ctx, 2, claimed.ID+1, cancel, info); err == nil {
		t.Fatal("cancel key replay changed target")
	}
	latest, err := s.CreateGuardianInvite(ctx, 1, info, inviteInput("guardian-create-fixture-003", info))
	if err != nil || latest.ID == first.ID {
		t.Fatal("cancelled invitation reused as new", err)
	}
	replay, err = s.CreateGuardianInvite(ctx, 1, info, in)
	if err != nil || replay.ID != first.ID || replay.State != "invite_cancelled" {
		t.Fatal("old key created another invitation", err)
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
	replay, err = s.CreateGuardianInvite(ctx, 1, info, in)
	if err != nil || replay.ID != first.ID || replay.State != "expired" {
		t.Fatal("purged original key created another invitation", err)
	}
}
func TestGuardianRelationshipEvidenceRequiresOriginalInvitationAndConsent(t *testing.T) {
	s, business, info := guardianFixture(t)
	ctx := context.Background()
	first, err := s.CreateGuardianInvite(ctx, 1, info, inviteInput("guardian-review-create-001", info))
	if err != nil {
		t.Fatal(err)
	}
	claimed, err := s.ClaimGuardianInvite(ctx, 2, info, identity.GuardianInviteClaim{CommandMeta: mobile.CommandMeta{ClientRequestID: "guardian-review-claim-001"}, Token: inviteToken(t, first), PolicyVersion: info.PolicyVersion, Accepted: true})
	if err != nil {
		t.Fatal(err)
	}
	input := mobile.GuardianEvidenceInput{InviteID: claimed.ID, InviteVersion: claimed.Version, GuardianID: 2, ChildID: 1, PolicyVersion: info.PolicyVersion, EvidenceRef: "fixture-relationship-proof", ValidUntil: s.Now().Add(time.Hour)}
	wrong := input
	wrong.InviteVersion++
	if _, err = business.RecordGuardianEvidence(ctx, "fixture-verifier", wrong); err == nil {
		t.Fatal("stale invitation version accepted")
	}
	wrong = input
	wrong.PolicyVersion = strings.Repeat("b", 64)
	if _, err = business.RecordGuardianEvidence(ctx, "fixture-verifier", wrong); err == nil {
		t.Fatal("different rule accepted")
	}
	if _, err = business.RecordGuardianEvidence(ctx, "", input); err == nil {
		t.Fatal("anonymous relationship evidence accepted")
	}
	verified, err := business.RecordGuardianEvidence(ctx, "fixture-verifier", input)
	if err != nil || verified.State != "verified_pending_consent" {
		t.Fatal("relationship review produced active consent", err)
	}
	if _, err = business.RecordGuardianEvidence(ctx, "fixture-verifier", input); err == nil {
		t.Fatal("consumed invitation created duplicate relationship")
	}
	public, err := s.GuardianInvite(ctx, 1, first.ID, info)
	if err != nil || public.GuardianRecordID != verified.ID || public.State != "relation_verified" {
		t.Fatal("review not linked to original invitation", err)
	}
	rows, err := business.Guardians(ctx, 1)
	if err != nil || len(rows) != 1 {
		t.Fatal(err)
	}
	body, err := mobile.Decode[mobile.GuardianConsent](rows[0])
	if err != nil || body.EvidenceRef != "" || len(body.Scopes) != 0 {
		t.Fatal("raw evidence or premature scopes projected", err)
	}
	decision := mobile.GuardianDecision{CommandMeta: mobile.CommandMeta{ClientRequestID: "guardian-consent-fixture-001"}, Version: verified.Version, Consent: true}
	if _, err = business.Guardian(ctx, 1, verified.ID, decision); err == nil {
		t.Fatal("child supplied guardian consent")
	}
	if _, err = business.Guardian(ctx, 2, verified.ID, decision); err == nil {
		t.Fatal("checkbox consent bypassed scoped reauthentication")
	}
}
