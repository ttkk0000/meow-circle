package mobile_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// Regression source only: no application test execution is authorized.
func retentionPhoto(t *testing.T, s *mobile.Service, actor int64, token string) mobile.Record {
	t.Helper()
	r, e := s.ReserveMedia(context.Background(), actor, token, meta().ClientRequestID,
		mobile.MediaFile{Scope: "private", MIME: "image/png", SHA256: mobile.Fingerprint("retained photo")})
	if e != nil {
		t.Fatal(e)
	}
	r, e = s.CompleteReservedMedia(context.Background(), actor, token, r.ID, r.Version)
	if e != nil {
		t.Fatal(e)
	}
	return r
}

func retentionRequest(t *testing.T, s *mobile.Service, repo mobile.Repository, fields []string, mediaIDs ...int64) (mobile.Record, string) {
	t.Helper()
	token, e := s.NewDataCapability(context.Background(), 1)
	if e != nil {
		t.Fatal(e)
	}
	in := mobile.DataRequestInput{CommandMeta: meta(), Type: "erase_fields", Fields: fields, QueryToken: token}
	for _, id := range mediaIDs {
		in.Selections = append(in.Selections, mobile.DataSelection{Kind: mobile.Media, ID: id})
	}
	scope, e := mobile.DataScope(1, in)
	if e != nil {
		t.Fatal(e)
	}
	ctx, proof := sessionAndGrant(t, repo, 1, scope)
	in.ReauthToken = proof
	r, e := s.SubmitDataRequest(ctx, 1, in)
	if e != nil {
		t.Fatal(e)
	}
	return r, token
}

func TestRightsEvidenceBlocksOwnedDeletionAndPhysicalErasure(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	photo := retentionPhoto(t, s, 1, "")
	file := body[mobile.MediaFile](t, photo)
	access, e := s.NewSafetySession(ctx)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.SubmitRightsCase(ctx, 1, access.Token, mobile.RightsInput{CommandMeta: meta(), Type: "safety", Reason: "私密证据", MediaIDs: []int64{photo.ID}}); e != nil {
		t.Fatal(e)
	}
	request, token := retentionRequest(t, s, repo, nil, photo.ID)
	request, e = s.ProcessDataRequest(ctx, "privacy-fixture", request.ID, request.Version)
	if e != nil {
		t.Fatal(e)
	}
	status, e := s.DataRequestStatus(ctx, request.ID, token)
	if e != nil || status.Held != 1 || status.Pending != 1 || status.State != "partially_held" || status.NextReviewAt == nil {
		t.Fatal(status, e)
	}
	used, e := s.UploadFilesInUse(ctx, []string{file.Filename})
	if e != nil || used[file.Filename] != "held" {
		t.Fatal(used, e)
	}
	// Simulate a legacy cleanup decision made before unified retention existed.
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.Media, photo.ID)
		if e != nil {
			return e
		}
		r.State = "erasure_pending"
		return tx.Save(mobile.Media, &r)
	}); e != nil {
		t.Fatal(e)
	}
	if _, e = s.PrepareMediaErasure(ctx, "privacy-fixture", photo.ID); e == nil {
		t.Fatal("physical erasure bypassed an active rights case")
	}
}

func TestGuestExpiryWaitsForClosureThenScrubsOnlyCaseEvidence(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	now := time.Now().UTC()
	s.Now = func() time.Time { return now }
	access, e := s.NewSafetySession(ctx)
	if e != nil {
		t.Fatal(e)
	}
	photo := retentionPhoto(t, s, 0, access.Token)
	ticket, e := s.SubmitRightsCase(ctx, 0, access.Token, mobile.RightsInput{CommandMeta: meta(), Type: "safety", Reason: "private report", EvidenceDescription: "private details", MediaIDs: []int64{photo.ID}})
	if e != nil {
		t.Fatal(e)
	}
	details, e := s.RightsDetails(ctx, "moderator-fixture", ticket.ID)
	if e != nil || !details.EvidenceAvailable || len(details.Messages) != 1 || len(details.MediaIDs) != 1 {
		t.Fatal(details, e)
	}
	now = now.Add(31 * 24 * time.Hour)
	details, e = s.RightsDetails(ctx, "moderator-fixture", ticket.ID)
	if e != nil || details.EvidenceAvailable || len(details.Messages) != 0 || len(details.MediaIDs) != 0 {
		t.Fatal("an overdue hold widened evidence reading permission", details, e)
	}
	if _, count, e := s.PurgeExpiredSafety(ctx, "privacy-fixture", 0); e != nil || count != 0 {
		t.Fatal("expired session erased an unresolved case attachment", count, e)
	}
	result, e := s.ExpireRightsEvidence(ctx, "privacy-fixture", mobile.RetentionExpiryInput{})
	if e != nil || result.Held != 1 || result.Erased != 0 {
		t.Fatal(result, e)
	}
	ticket, e = s.DecideRightsCase(ctx, "moderator-fixture", ticket.ID, mobile.RightsDecision{Version: ticket.Version, State: "closed", Summary: "处理结束", ReviewDue: now.Add(time.Hour), TransferState: "not_required"})
	if e != nil {
		t.Fatal(e)
	}
	result, e = s.ExpireRightsEvidence(ctx, "privacy-fixture", mobile.RetentionExpiryInput{})
	if e != nil || result.Erased != 1 || result.NextCursor != 0 {
		t.Fatal(result, e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.RightsCases, ticket.ID)
		if e != nil {
			return e
		}
		v, e := mobile.Decode[struct {
			Case mobile.RightsCase `json:"case"`
		}](r)
		if e != nil {
			return e
		}
		if len(r.MediaIDs) != 0 || len(v.Case.Messages) != 0 || v.Case.Contact != "" || v.Case.EvidenceErasedAt == nil {
			t.Fatal("private case evidence survived expiry", r.ID)
		}
		media, e := tx.Get(mobile.Media, photo.ID)
		if e == nil && media.State != "ready" {
			t.Fatal("case scrub claimed physical media was erased")
		}
		return e
	}); e != nil {
		t.Fatal(e)
	}
	if _, count, e := s.PurgeExpiredSafety(ctx, "privacy-fixture", 0); e != nil || count != 1 {
		t.Fatal(count, e)
	}
	work, e := s.PrepareMediaErasure(ctx, "privacy-fixture", photo.ID)
	if e != nil || !work.DeletePhysical {
		t.Fatal(work, e)
	}
	result, e = s.ExpireRightsEvidence(ctx, "privacy-fixture", mobile.RetentionExpiryInput{})
	if e != nil || result.Erased != 0 {
		t.Fatal("expiry was not idempotent", result, e)
	}
}

func TestFieldHoldCannotBeBypassedByAnotherRequest(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	now := time.Now().UTC()
	s.Now = func() time.Time { return now }
	first, firstToken := retentionRequest(t, s, repo, []string{"bio", "avatar"})
	hold := mobile.DataHold{Kind: mobile.Accounts, ID: 1, Fields: []string{"bio"}, Basis: "specific pending matter", Responsible: "privacy-fixture", ReviewAt: now.Add(time.Hour)}
	if e := s.SetDataHold(ctx, "privacy-fixture", first.ID, mobile.DataHoldInput{Version: first.Version, Hold: hold}); e != nil {
		t.Fatal(e)
	}
	second, secondToken := retentionRequest(t, s, repo, []string{"bio"})
	if _, e := s.ProcessDataRequest(ctx, "privacy-fixture", second.ID, second.Version); e != nil {
		t.Fatal(e)
	}
	now = now.Add(2 * time.Hour)
	status, e := s.DataRequestStatus(ctx, second.ID, secondToken)
	if e != nil || status.Held != 1 || !status.ReviewRequired || len(status.PendingFields) != 1 || status.State == "completed" {
		t.Fatal("overdue hold was released or hidden", status, e)
	}
	firstStatus, e := s.DataRequestStatus(ctx, first.ID, firstToken)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.ProcessDataRequest(ctx, "privacy-fixture", first.ID, firstStatus.Version); e != nil {
		t.Fatal(e)
	}
	firstStatus, e = s.DataRequestStatus(ctx, first.ID, firstToken)
	if e != nil || len(firstStatus.PendingFields) != 1 || firstStatus.PendingFields[0] != "bio" {
		t.Fatal("unrelated profile field was blocked", firstStatus, e)
	}
	hold.Released, hold.ReviewAt = true, now.Add(time.Hour)
	if e = s.SetDataHold(ctx, "privacy-fixture", first.ID, mobile.DataHoldInput{Version: firstStatus.Version, Hold: hold}); e != nil {
		t.Fatal(e)
	}
	if _, e = s.ProcessDataRequest(ctx, "privacy-fixture", second.ID, status.Version); e != nil {
		t.Fatal(e)
	}
	status, e = s.DataRequestStatus(ctx, second.ID, secondToken)
	if e != nil || status.State != "completed" || len(status.PendingFields) != 0 || status.Held != 0 {
		t.Fatal(status, e)
	}
}

func TestDataHoldRejectsUnselectedTargetsAndInventedRelease(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	r, _ := retentionRequest(t, s, repo, []string{"bio"})
	for _, hold := range []mobile.DataHold{
		{Kind: mobile.Accounts, ID: 2, Fields: []string{"bio"}},
		{Kind: mobile.Accounts, ID: 1, Fields: []string{"avatar"}},
		{Kind: mobile.Media, ID: 999},
		{Kind: mobile.Accounts, ID: 1, Fields: []string{"bio"}, Released: true},
	} {
		hold.Basis, hold.Responsible, hold.ReviewAt = "scope check", "privacy-fixture", time.Now().Add(time.Hour)
		if e := s.SetDataHold(ctx, "privacy-fixture", r.ID, mobile.DataHoldInput{Version: r.Version, Hold: hold}); e == nil {
			t.Fatal("accepted unrelated target or invented hold release", hold.Kind, hold.ID)
		}
	}
	if _, e := s.ProcessDataRequest(ctx, "privacy-fixture", r.ID, r.Version+1); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("failed hold unexpectedly changed the request version", e)
	}
}

func TestRightsScopeBindsIdentityAndAttachmentSelection(t *testing.T) {
	in := mobile.RightsInput{Type: "rights_notice", LegalName: "A", RightType: "copyright", MediaIDs: []int64{1}, Reason: "notice"}
	baseline, e := mobile.RightsScope(1, in)
	if e != nil {
		t.Fatal(e)
	}
	for _, change := range []func(*mobile.RightsInput){
		func(v *mobile.RightsInput) { v.LegalName = "B" },
		func(v *mobile.RightsInput) { v.RightType = "other" },
		func(v *mobile.RightsInput) { v.MediaIDs = []int64{2} },
	} {
		changed := in
		change(&changed)
		scope, e := mobile.RightsScope(1, changed)
		if e != nil || scope.ScopeHash == baseline.ScopeHash {
			t.Fatal("identity or evidence change reused the same authorization scope", e)
		}
	}
}

func TestVerificationEvidenceAlsoBlocksOwnerErasure(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	photo := retentionPhoto(t, s, 1, "")
	if _, e := s.SubmitReview(ctx, 1, mobile.ReviewInput{CommandMeta: meta(), Type: "support", Reason: "附件核对", MediaIDs: []int64{photo.ID}}); e != nil {
		t.Fatal(e)
	}
	request, token := retentionRequest(t, s, repo, nil, photo.ID)
	if _, e := s.ProcessDataRequest(ctx, "privacy-fixture", request.ID, request.Version); e != nil {
		t.Fatal(e)
	}
	status, e := s.DataRequestStatus(ctx, request.ID, token)
	if e != nil || status.Held != 1 || status.PhysicalPending != 0 {
		t.Fatal("review evidence was not preserved", status, e)
	}
}

func TestCaseHoldFromAnotherParticipantPreventsExpiry(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	now := time.Now().UTC()
	s.Now = func() time.Time { return now }
	access, e := s.NewSafetySession(ctx)
	if e != nil {
		t.Fatal(e)
	}
	ticket, e := s.SubmitRightsCase(ctx, 1, access.Token, mobile.RightsInput{CommandMeta: meta(), Type: "safety", Reason: "another participant matter"})
	if e != nil {
		t.Fatal(e)
	}
	now = now.Add(31 * 24 * time.Hour)
	ticket, e = s.DecideRightsCase(ctx, "moderator-fixture", ticket.ID, mobile.RightsDecision{Version: ticket.Version, State: "closed", Summary: "处理结束", ReviewDue: now.Add(time.Hour), TransferState: "not_required"})
	if e != nil {
		t.Fatal(e)
	}
	// An account-closure assessment hold belongs to the other participant.
	request := mobile.Record{OwnerID: 2, State: "partially_held"}
	hold := mobile.DataHold{Kind: mobile.RightsCases, ID: ticket.ID, Fields: []string{"minimum_matter_record"}, Basis: "specific joint matter", Responsible: "privacy-fixture", ReviewAt: now.Add(-time.Hour)}
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		if e := mobile.SetBody(&request, mobile.DataRequest{Holds: []mobile.DataHold{hold}}); e != nil {
			return e
		}
		return tx.Save(mobile.DataRequests, &request)
	}); e != nil {
		t.Fatal(e)
	}
	result, e := s.ExpireRightsEvidence(ctx, "privacy-fixture", mobile.RetentionExpiryInput{})
	if e != nil || result.Held != 1 || result.Erased != 0 {
		t.Fatal("participant hold was bypassed", result, e)
	}
	hold.Released, hold.ReviewAt = true, now.Add(time.Hour)
	if e = s.SetDataHold(ctx, "privacy-fixture", request.ID, mobile.DataHoldInput{Version: request.Version, Hold: hold}); e != nil {
		t.Fatal(e)
	}
	result, e = s.ExpireRightsEvidence(ctx, "privacy-fixture", mobile.RetentionExpiryInput{})
	if e != nil || result.Erased != 1 {
		t.Fatal("explicit release did not unblock case expiry", result, e)
	}
}
