package mobile_test

import (
	"context"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// Regression source only; no Go tests have been executed in this environment.
func closureApplication(t *testing.T, s *mobile.Service, repo mobile.Repository, petID, applicant int64) mobile.Record {
	t.Helper()
	r, e := s.ApplyAdoption(context.Background(), applicant, mobile.AdoptionInput{CommandMeta: meta(), PetID: petID, Message: "本轮申请摘要"})
	if e != nil {
		t.Fatal(e)
	}
	r, e = s.Adoption(context.Background(), 1, r.ID, "approve", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version})
	if e != nil {
		t.Fatal(e)
	}
	a := body[mobile.Adoption](t, r)
	a.EffectiveContract = 1
	a.Contracts = []mobile.ContractRevision{{Number: 1, Status: "effective", AcceptedBy: []int64{1, applicant}, Terms: mobile.ContractTerms{City: "武汉", Species: "cat"}}}
	return serverRecord(t, repo, mobile.Adoptions, r, a)
}

func TestAdoptionReservesPetAndClosesOtherApplications(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	a := closureApplication(t, s, repo, p.ID, 2)
	b := closureApplication(t, s, repo, p.ID, 3)
	var e error
	a, e = s.Adoption(ctx, 1, a.ID, "confirm_handover", mobile.AdoptionAction{CommandMeta: meta(), Version: a.Version})
	if e != nil {
		t.Fatal(e)
	}
	b, e = s.Get(ctx, 3, mobile.Adoptions, b.ID)
	if e != nil || b.State != "waitlisted" {
		t.Fatal("other candidate was not waitlisted", b, e)
	}
	if _, e = s.Adoption(ctx, 3, b.ID, "confirm_handover", mobile.AdoptionAction{CommandMeta: meta(), Version: b.Version}); e == nil {
		t.Fatal("second placement obtained the same pet")
	}
	a, e = s.Adoption(ctx, 2, a.ID, "confirm_handover", mobile.AdoptionAction{CommandMeta: meta(), Version: a.Version})
	if e != nil || a.State != "completed" {
		t.Fatal(a, e)
	}
	b, e = s.Get(ctx, 3, mobile.Adoptions, b.ID)
	if e != nil || b.State != "closed_other_placement" {
		t.Fatal("other application remained actionable", b, e)
	}
}

func TestAdoptionCycleRejectsStaleApplyAndKeepsHistory(t *testing.T) {
	s, _ := fixture(t)
	p := pet(t, s, true)
	ctx := context.Background()
	old, e := s.ApplyAdoption(ctx, 2, mobile.AdoptionInput{CommandMeta: meta(), PetID: p.ID, Message: "第一轮"})
	if e != nil {
		t.Fatal(e)
	}
	pb := body[mobile.Pet](t, p)
	pb.AdoptionAvailable = false
	p, e = s.SavePet(ctx, 1, p.ID, mobile.PetInput{CommandMeta: meta(), Version: p.Version, Pet: pb})
	if e != nil {
		t.Fatal(e)
	}
	pb = body[mobile.Pet](t, p)
	pb.AdoptionAvailable = true
	pb.AdoptionCycle = 999 // Client metadata must not choose the cycle.
	p, e = s.SavePet(ctx, 1, p.ID, mobile.PetInput{CommandMeta: meta(), Version: p.Version, Pet: pb})
	if e != nil || body[mobile.Pet](t, p).AdoptionCycle != 1 {
		t.Fatal("server did not own the cycle", p, e)
	}
	if _, e = s.ApplyAdoption(ctx, 2, mobile.AdoptionInput{CommandMeta: meta(), PetID: p.ID, Message: "过时入口"}); e == nil {
		t.Fatal("unknown cycle applied to a new round")
	}
	next, e := s.ApplyAdoption(ctx, 2, mobile.AdoptionInput{CommandMeta: meta(), PetID: p.ID, AdoptionCycle: 1, Message: "重新核对的新一轮"})
	if e != nil || next.ID == old.ID {
		t.Fatal("new cycle reused old application", next, e)
	}
	previous, e := s.Get(ctx, 2, mobile.Adoptions, old.ID)
	if e != nil || previous.State != "closed_other_placement" {
		t.Fatal("old round remained actionable", previous, e)
	}
}

func TestTrialConfirmationCannotReserveTwoApplications(t *testing.T) {
	s, repo := fixture(t)
	p := pet(t, s, true)
	ctx := context.Background()
	apps := []mobile.Record{closureApplication(t, s, repo, p.ID, 2), closureApplication(t, s, repo, p.ID, 3)}
	until := s.Now().Add(time.Hour)
	for i := range apps {
		a := body[mobile.Adoption](t, apps[i])
		a.TrialUntil = &until
		a.TrialConfirmations = []int64{1}
		a.Contracts[0].Terms.TrialUntil = &until
		apps[i].State = "trial_proposed"
		apps[i] = serverRecord(t, repo, mobile.Adoptions, apps[i], a)
	}
	first, e := s.Adoption(ctx, 2, apps[0].ID, "confirm_trial", mobile.AdoptionAction{CommandMeta: meta(), Version: apps[0].Version})
	if e != nil || first.State != "trial_active" {
		t.Fatal(first, e)
	}
	second, e := s.Get(ctx, 3, mobile.Adoptions, apps[1].ID)
	if e != nil || second.State != "waitlisted" {
		t.Fatal(second, e)
	}
	if _, e = s.Adoption(ctx, 3, second.ID, "confirm_trial", mobile.AdoptionAction{CommandMeta: meta(), Version: second.Version}); e == nil {
		t.Fatal("another application acquired an active trial")
	}
}

func TestWithdrawnAdoptionRequiresExplicitResubmit(t *testing.T) {
	s, repo := fixture(t)
	p := pet(t, s, true)
	ctx := context.Background()
	r := closureApplication(t, s, repo, p.ID, 2)
	r, e := s.Adoption(ctx, 2, r.ID, "withdraw", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version})
	if e != nil {
		t.Fatal(e)
	}
	again, e := s.ApplyAdoption(ctx, 2, mobile.AdoptionInput{CommandMeta: meta(), PetID: p.ID, Message: "不能自动重提"})
	if e != nil || again.State != "withdrawn" || again.ID != r.ID {
		t.Fatal(again, e)
	}
	r, e = s.Adoption(ctx, 2, r.ID, "resubmit", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version, Note: "本人再次核对的摘要"})
	if e != nil || r.State != "submitted" {
		t.Fatal(r, e)
	}
	a := body[mobile.Adoption](t, r)
	if a.EffectiveContract != 0 || len(a.Contracts) != 1 || a.Contracts[0].Status != "replaced" {
		t.Fatal("resubmit erased history or reused earlier consent", a)
	}
}

func TestReturnWithdrawAndDeadlinePreserveEffectiveArrangement(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	a := body[mobile.Adoption](t, r)
	now := s.Now()
	a.EffectiveReturn = 1
	a.ReturnPlans = []mobile.ReturnPlan{
		{Number: 1, State: "effective", ProposedBy: 1, Terms: mobile.ReturnTerms{StartsAt: now.Add(time.Hour)}, Confirmations: []int64{1, 2}},
		{Number: 2, State: "pending", ProposedBy: 2, Terms: mobile.ReturnTerms{StartsAt: now.Add(-time.Minute)}, Confirmations: []int64{2}},
	}
	r.State = "return_agreed"
	r = serverRecord(t, repo, mobile.Adoptions, r, a)
	if _, e := s.Return(ctx, 1, r.ID, "accept", mobile.ReturnInput{CommandMeta: meta(), Version: r.Version, Revision: 2}); e == nil {
		t.Fatal("expired return proposal was accepted")
	}
	if _, e := s.Return(ctx, 1, r.ID, "withdraw", mobile.ReturnInput{CommandMeta: meta(), Version: r.Version, Revision: 2}); e == nil {
		t.Fatal("receiver withdrew another person's proposal")
	}
	r, e := s.Return(ctx, 2, r.ID, "withdraw", mobile.ReturnInput{CommandMeta: meta(), Version: r.Version, Revision: 2})
	if e != nil {
		t.Fatal(e)
	}
	a = body[mobile.Adoption](t, r)
	if a.EffectiveReturn != 1 || a.ReturnPlans[0].State != "effective" || a.ReturnPlans[1].State != "withdrawn" || r.State != "return_agreed" {
		t.Fatal("withdraw erased the prior arrangement", a)
	}
}

func TestHandoverContinuesAfterPublisherClosesListing(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	r, e := s.Adoption(ctx, 1, r.ID, "confirm_handover", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version})
	if e != nil {
		t.Fatal(e)
	}
	p, e = s.Get(ctx, 1, mobile.Pets, p.ID)
	if e != nil {
		t.Fatal(e)
	}
	pb := body[mobile.Pet](t, p)
	pb.AdoptionAvailable = false
	if _, e = s.SavePet(ctx, 1, p.ID, mobile.PetInput{CommandMeta: meta(), Version: p.Version, Pet: pb}); e != nil {
		t.Fatal(e)
	}
	r, e = s.Adoption(ctx, 2, r.ID, "confirm_handover", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version})
	if e != nil || r.State != "completed" {
		t.Fatal("closing the listing interrupted its reserved handover", r, e)
	}
}

func TestResumedWaitlistRequiresFreshContractAndPreservesHistory(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	r.State = "waitlisted"
	r = serverRecord(t, repo, mobile.Adoptions, r, body[mobile.Adoption](t, r))
	r, e := s.Adoption(ctx, 1, r.ID, "resume_candidate", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version})
	if e != nil {
		t.Fatal(e)
	}
	a := body[mobile.Adoption](t, r)
	if a.EffectiveContract != 0 || len(a.Contracts) != 1 || a.Contracts[0].Status != "replaced" {
		t.Fatal("resumed consent reused or erased the old agreement", a)
	}
	if _, e = s.Adoption(ctx, 2, r.ID, "confirm_handover", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version}); e == nil {
		t.Fatal("resumed candidate entered a handover without fresh terms")
	}
}

func TestReturnAgreedKeepsCareAndSupportAvailable(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	a := body[mobile.Adoption](t, r)
	now := s.Now()
	a.CompletedAt = &now
	a.EffectiveReturn = 1
	a.ReturnPlans = []mobile.ReturnPlan{{Number: 1, State: "effective", ProposedBy: 1, Confirmations: []int64{1, 2}, Terms: mobile.ReturnTerms{StartsAt: now.Add(time.Hour)}}}
	r.State = "return_agreed"
	r = serverRecord(t, repo, mobile.Adoptions, r, a)
	if _, e := s.Care(ctx, 2, r.ID, mobile.CareInput{CommandMeta: meta(), Text: "接回前继续照护"}); e != nil {
		t.Fatal("agreed return blocked ongoing care", e)
	}
	r, e := s.Adoption(ctx, 2, r.ID, "support", mobile.AdoptionAction{CommandMeta: meta(), Version: r.Version, Note: "需要协助核对接回安排"})
	if e != nil || r.State != "return_agreed" || body[mobile.Adoption](t, r).EffectiveReturn != 1 {
		t.Fatal("support removed the effective return arrangement", r, e)
	}
}

func TestOfferAppointmentCannotConfirmOrRestoreAfterPlacement(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	app := closureApplication(t, s, repo, p.ID, 2)
	offer := serverRecord(t, repo, mobile.Offers, mobile.Record{OwnerID: 1, RefID: p.ID, Public: true, State: "published"}, mobile.Offer{
		PetID: p.ID, PetVersion: p.Version, City: "武汉", Area: "江岸区", VenueType: "公共场所", Reception: "送养方", Payee: "无费用", ValidUntil: s.Now().Add(24 * time.Hour),
	})
	appointment, e := s.CreateAppointment(ctx, 3, mobile.AppointmentInput{CommandMeta: meta(), ContextKind: "offer", ContextID: offer.ID, PeerID: 1, Terms: mobile.AppointmentTerms{
		City: "武汉", Species: "cat", StartsAt: s.Now().Add(time.Hour), Place: "公共场所", Reception: "送养方", PetName: "小满", RequiredFees: "无", Payee: "无费用",
	}})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Adoption(ctx, 1, app.ID, "confirm_handover", mobile.AdoptionAction{CommandMeta: meta(), Version: app.Version}); e != nil {
		t.Fatal(e)
	}
	for _, action := range []string{"confirm", "restore"} {
		if _, e = s.Appointment(ctx, 1, appointment.ID, action, mobile.AppointmentAction{CommandMeta: meta(), Version: appointment.Version, RestoreVersion: 1}); e == nil {
			t.Fatalf("%s reused an offer while the pet had another arrangement", action)
		}
	}
}

func TestReturnReconfirmationChecksBothAdultsAndReplacesOldPlan(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	a := body[mobile.Adoption](t, r)
	a.EffectiveReturn = 1
	a.ReturnPlans = []mobile.ReturnPlan{
		{Number: 1, State: "effective", ProposedBy: 1, Confirmations: []int64{1, 2}, Terms: mobile.ReturnTerms{StartsAt: s.Now().Add(time.Hour)}},
		{Number: 2, State: "pending", ProposedBy: 1, Confirmations: []int64{1}, Terms: mobile.ReturnTerms{StartsAt: s.Now().Add(2 * time.Hour)}},
	}
	r.State = "return_agreed"
	r = serverRecord(t, repo, mobile.Adoptions, r, a)
	if e := s.RecordAgeEvidence(ctx, "test-verifier", mobile.AgeEvidenceInput{ActorID: 1, AgeBand: "14_17", ValidUntil: s.Now().Add(time.Hour), EvidenceRef: "corrected-evidence"}); e != nil {
		t.Fatal(e)
	}
	if _, e := s.Return(ctx, 2, r.ID, "accept", mobile.ReturnInput{CommandMeta: meta(), Version: r.Version, Revision: 2}); e == nil {
		t.Fatal("stale proposer eligibility became an effective offline arrangement")
	}
	if e := s.RecordAgeEvidence(ctx, "test-verifier", mobile.AgeEvidenceInput{ActorID: 1, AgeBand: "adult", ValidUntil: s.Now().Add(time.Hour), EvidenceRef: "verified-evidence"}); e != nil {
		t.Fatal(e)
	}
	r, e := s.Return(ctx, 2, r.ID, "accept", mobile.ReturnInput{CommandMeta: meta(), Version: r.Version, Revision: 2})
	if e != nil {
		t.Fatal(e)
	}
	a = body[mobile.Adoption](t, r)
	if a.EffectiveReturn != 2 || a.ReturnPlans[0].State != "replaced" || a.ReturnPlans[1].State != "effective" {
		t.Fatal("two return plans remained effective", a)
	}
}
