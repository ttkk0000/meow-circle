package mobile_test

import (
	"context"
	"errors"
	"fmt"
	"kitty-circle/internal/mobile"
	"testing"
	"time"
)

// Regression source only. No test execution was authorized for this task.
func sessionAndGrant(t *testing.T, repo mobile.Repository, actor int64, scope mobile.PurposeScope) (context.Context, string) {
	t.Helper()
	token, e := mobile.NewCapability()
	if e != nil {
		t.Fatal(e)
	}
	grant, e := mobile.NewCapability()
	if e != nil {
		t.Fatal(e)
	}
	if e = repo.Update(context.Background(), func(tx mobile.Tx) error {
		r := mobile.Record{OwnerID: actor, Key: mobile.Fingerprint(token), State: "active"}
		if e := mobile.SetBody(&r, struct {
			ExpiresAt time.Time `json:"expires_at"`
		}{time.Now().Add(time.Hour)}); e != nil {
			return e
		}
		if e := tx.Save(mobile.Sessions, &r); e != nil {
			return e
		}
		r = mobile.Record{OwnerID: actor, Key: mobile.Fingerprint(grant), State: "purpose_grant"}
		if e := mobile.SetBody(&r, mobile.PurposeGrant{Scope: scope, SessionKey: mobile.Fingerprint(token), ExpiresAt: time.Now().Add(time.Minute)}); e != nil {
			return e
		}
		return tx.Save(mobile.AuthChallenges, &r)
	}); e != nil {
		t.Fatal(e)
	}
	return mobile.WithSession(context.Background(), token), grant
}
func TestLegacyPhoneIsNotInteractionProof(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	if e := repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.Qualifications, "1")
		if e != nil {
			return e
		}
		q, e := mobile.Decode[mobile.Qualification](r)
		if e != nil {
			return e
		}
		q.PhoneVerifiedAt = nil
		if e = mobile.SetBody(&r, q); e != nil {
			return e
		}
		return tx.Save(mobile.Qualifications, &r)
	}); e != nil {
		t.Fatal(e)
	}
	q, e := s.Eligibility(ctx, 1)
	if e != nil || q.Interaction || q.PhoneVerified {
		t.Fatalf("legacy phone trusted: %+v %v", q, e)
	}
	_, e = s.SaveEntry(ctx, 1, 0, declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Space: "community", Text: "小满", Audience: mobile.Audience{Mode: "public"}}}))
	var rule mobile.RuleError
	if !errors.As(e, &rule) || rule.Code != "phone_verification_required" {
		t.Fatalf("outward gate: %v", e)
	}
	if _, e = s.Settings(ctx, 1, mobile.Drafts, mobile.SettingsInput{Key: "private", Draft: &mobile.Draft{ContextKind: "compose", Text: "私有草稿", Audience: mobile.Audience{Mode: "private"}}}); e != nil {
		t.Fatal("private draft was blocked", e)
	}
}
func TestPurposeGrantWrongScopeRollsBackWithoutBurningProof(t *testing.T) {
	_, repo := fixture(t)
	scope, e := mobile.ScopeFor("guardian.consent", "guardians", 42, 3, "guardian", []string{"interaction"})
	if e != nil {
		t.Fatal(e)
	}
	ctx, grant := sessionAndGrant(t, repo, 1, scope)
	wrong := scope
	wrong.ResourceVersion++
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		return mobile.ConsumePurpose(tx, 1, mobile.SessionKey(ctx), grant, "scope-request", wrong, time.Now())
	}); e == nil {
		t.Fatal("wrong object version accepted")
	}
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		return mobile.ConsumePurpose(tx, 1, mobile.SessionKey(ctx), grant, "scope-request", scope, time.Now())
	}); e != nil {
		t.Fatal("rollback burned proof", e)
	}
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		return mobile.ConsumePurpose(tx, 1, mobile.SessionKey(ctx), grant, "other-request", scope, time.Now())
	}); e == nil {
		t.Fatal("consumed grant accepted")
	}
}
func TestEntryOperationReplaysAndRejectsChangedPayload(t *testing.T) {
	s, _ := fixture(t)
	in := declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Space: "community", Text: "小满", Audience: mobile.Audience{Mode: "public"}}})
	ctx := context.Background()
	first, e := s.SaveEntry(ctx, 1, 0, in)
	if e != nil {
		t.Fatal(e)
	}
	again, e := s.SaveEntry(ctx, 1, 0, in)
	if e != nil || first.ID != again.ID {
		t.Fatal("duplicate entry", e)
	}
	changed := in
	changed.Entry.Text = "改变后的内容"
	_, e = s.SaveEntry(ctx, 1, 0, changed)
	var rule mobile.RuleError
	if !errors.As(e, &rule) || rule.Code != "idempotency_conflict" {
		t.Fatalf("changed request accepted: %v", e)
	}
	receipt, e := s.Operation(ctx, 1, in.ClientRequestID)
	if e != nil || receipt.ResultID != first.ID || receipt.State != "committed" {
		t.Fatal(receipt, e)
	}
	if _, e = s.Operation(ctx, 2, in.ClientRequestID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("another account read receipt", e)
	}
}
func TestGuardianWithdrawalImmediatelyRemovesUnder14Eligibility(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	if e := s.RecordAgeEvidence(ctx, "test-verifier", mobile.AgeEvidenceInput{ActorID: 2, AgeBand: "under14", ValidUntil: time.Now().Add(time.Hour), EvidenceRef: "child-age"}); e != nil {
		t.Fatal(e)
	}
	row, e := s.RecordGuardianEvidence(ctx, "test-verifier", mobile.GuardianEvidenceInput{GuardianID: 1, ChildID: 2, PolicyVersion: "fixture-policy", EvidenceRef: "verified-relationship", ValidUntil: time.Now().Add(time.Hour)})
	if e != nil {
		t.Fatal(e)
	}
	scope, _ := mobile.GuardianScope(row.ID, row.Version, "fixture-policy")
	proofCtx, grant := sessionAndGrant(t, repo, 1, scope)
	row, e = s.Guardian(proofCtx, 1, row.ID, mobile.GuardianDecision{CommandMeta: meta(), Version: row.Version, Consent: true, ReauthToken: grant})
	if e != nil {
		t.Fatal(e)
	}
	q, e := s.Eligibility(ctx, 2)
	if e != nil || !q.Interaction || q.Adult {
		t.Fatal(q, e)
	}
	if _, e = s.Guardian(ctx, 1, row.ID, mobile.GuardianDecision{CommandMeta: meta(), Version: row.Version, Consent: false}); e != nil {
		t.Fatal(e)
	}
	q, e = s.Eligibility(ctx, 2)
	if e != nil || q.Interaction {
		t.Fatal("withdrawal retained access", q, e)
	}
}
func TestGuestSafetyDoesNotRequireReadingTarget(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	access, e := s.NewSafetySession(ctx)
	if e != nil {
		t.Fatal(e)
	}
	in := mobile.RightsInput{CommandMeta: meta(), Type: "safety", TargetKind: mobile.Entries, TargetID: 999999, Reason: "安全线索"}
	receipt, e := s.SubmitRightsCase(ctx, 0, access.Token, in)
	if e != nil {
		t.Fatal(e)
	}
	if receipt.State != "submitted" {
		t.Fatal(receipt)
	}
	other, e := s.NewSafetySession(ctx)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.RightsStatus(ctx, other.Token, receipt.ID); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatal("other guest read ticket", e)
	}
}
func TestFieldErasureHoldsDoNotBlockUnrelatedFields(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	if e := repo.Update(ctx, func(tx mobile.Tx) error {
		u, e := tx.User(1)
		if e != nil {
			return e
		}
		u.Bio = "保全内容"
		u.AvatarURL = "/private-avatar"
		return tx.SaveUser(&u)
	}); e != nil {
		t.Fatal(e)
	}
	token, e := s.NewDataCapability(ctx, 1)
	if e != nil {
		t.Fatal(e)
	}
	in := mobile.DataRequestInput{CommandMeta: meta(), Type: "erase_fields", Fields: []string{"bio", "avatar"}, QueryToken: token}
	scope, e := mobile.DataScope(1, in)
	if e != nil {
		t.Fatal(e)
	}
	proofCtx, grant := sessionAndGrant(t, repo, 1, scope)
	in.ReauthToken = grant
	r, e := s.SubmitDataRequest(proofCtx, 1, in)
	if e != nil {
		t.Fatal(e)
	}
	if e = s.SetDataHold(ctx, "privacy-test", r.ID, mobile.DataHoldInput{Version: r.Version, Hold: mobile.DataHold{Kind: mobile.Accounts, ID: 1, Fields: []string{"bio"}, Basis: "specific pending matter", Responsible: "privacy-test", ReviewAt: time.Now().Add(time.Hour)}}); e != nil {
		t.Fatal(e)
	}
	status, e := s.DataRequestStatus(ctx, r.ID, token)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.ProcessDataRequest(ctx, "privacy-test", r.ID, status.Version); e != nil {
		t.Fatal(e)
	}
	status, e = s.DataRequestStatus(ctx, r.ID, token)
	if e != nil || status.State != "partially_held" || status.Held != 1 {
		t.Fatal(status, e)
	}
	if e := repo.View(ctx, func(tx mobile.Tx) error {
		u, e := tx.User(1)
		if e != nil {
			return e
		}
		if u.Bio != "保全内容" || u.AvatarURL != "" {
			t.Fatal("unrelated field erasure was blocked")
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
}
func TestGuestMediaQuotaCountsBeforeDiskWrite(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	access, e := s.NewSafetySession(ctx)
	if e != nil {
		t.Fatal(e)
	}
	file := mobile.MediaFile{Scope: "private", MIME: "image/png", SHA256: mobile.Fingerprint("example")}
	for n := 0; n < 36; n++ {
		if _, e = s.ReserveMedia(ctx, 0, access.Token, fmt.Sprintf("upload-key-%d", n), file); e != nil {
			t.Fatal(e)
		}
	}
	if _, e = s.ReserveMedia(ctx, 0, access.Token, "over-quota", file); !errors.Is(e, mobile.ErrRateLimited) {
		t.Fatal("guest quota did not include reservations", e)
	}
	if _, e = s.ReserveMedia(ctx, 0, access.Token, "upload-key-0", file); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("concurrent upload acquired the same live lease", e)
	}
}
