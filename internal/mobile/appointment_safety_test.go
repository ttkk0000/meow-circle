package mobile_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// 仅回归源码；未在本轮执行。
func heldAppointment(t *testing.T, s *mobile.Service) (mobile.Record, mobile.AppointmentTerms) {
	t.Helper()
	p := pet(t, s, false)
	terms := mobile.AppointmentTerms{City: "武汉", Species: "cat", StartsAt: s.Now().Add(3 * time.Hour), Place: "公共会面点", Reception: "发布人", PetName: "小满", RequiredFees: "无费用", Payee: "不收款"}
	ctx := context.Background()
	r, err := s.CreateAppointment(ctx, 2, mobile.AppointmentInput{CommandMeta: meta(), ContextKind: "visit", ContextID: p.ID, PeerID: 1, Terms: terms})
	if err != nil {
		t.Fatal(err)
	}
	r, err = s.Appointment(ctx, 1, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version})
	if err != nil {
		t.Fatal(err)
	}
	r, err = s.Appointment(ctx, 2, r.ID, "safety_hold", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Note: "原会面地点已不可用"})
	if err != nil {
		t.Fatal(err)
	}
	return r, terms
}

func TestSafetyReproposalRequiresFreshTermsSpecificReviewsAndBothParticipants(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	r, terms := heldAppointment(t, s)
	for _, action := range []string{"propose", "restore", "confirm"} {
		if _, err := s.Appointment(ctx, 2, r.ID, action, mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Terms: &terms, RestoreVersion: 1}); err == nil {
			t.Fatal("ordinary action released hold", action)
		}
	}
	if _, err := s.Appointment(ctx, 2, r.ID, "safety_repropose", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Terms: &terms}); err == nil {
		t.Fatal("missing resolution accepted")
	}
	terms.Place = "另一个已核对的公共会面点"
	in := mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Terms: &terms, Note: "核对了新地点、接待方及全部费用"}
	r, err := s.Appointment(ctx, 2, r.ID, "safety_repropose", in)
	if err != nil {
		t.Fatal(err)
	}
	a := body[mobile.Appointment](t, r)
	if r.State != "safety_hold" || a.EffectiveVersion != 1 || a.PendingVersion != 2 || len(a.Versions[1].Confirmations) != 1 || a.Versions[1].SafetyHoldVersion != a.SafetyHoldVersion {
		t.Fatal("single party released hold", r, a)
	}
	if _, err = s.Appointment(ctx, 1, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version}); err == nil {
		t.Fatal("no personal safety review")
	}
	if _, err = s.Appointment(ctx, 3, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Note: "outsider"}); !errors.Is(err, mobile.ErrForbidden) {
		t.Fatal(err)
	}
	r, err = s.Appointment(ctx, 1, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Note: "本人已核对具体更正及本次新安排"})
	if err != nil {
		t.Fatal(err)
	}
	a = body[mobile.Appointment](t, r)
	if r.State != "confirmed" || a.EffectiveVersion != 2 || a.PendingVersion != 0 || a.Versions[0].Status != "replaced" || len(a.Versions[1].SafetyChecks) != 2 || a.SafetyReason != "原会面地点已不可用" {
		t.Fatal(r, a)
	}
	// 同一原请求重放返回当前授权记录，不创建第三个提案或复用旧确认。
	r, err = s.Appointment(ctx, 2, r.ID, "safety_repropose", in)
	if err != nil || len(body[mobile.Appointment](t, r).Versions) != 2 {
		t.Fatal(r, err)
	}
}

func TestSafetyDeclineWithdrawalAndExpiryKeepHold(t *testing.T) {
	for _, action := range []string{"decline", "withdraw_change", "expire"} {
		s, _ := fixture(t)
		ctx := context.Background()
		r, terms := heldAppointment(t, s)
		r, err := s.Appointment(ctx, 2, r.ID, "safety_repropose", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Terms: &terms, Note: "已核对本次新安排"})
		if err != nil {
			t.Fatal(err)
		}
		actor := int64(2)
		if action == "decline" {
			actor = 1
		}
		if action == "expire" {
			now := s.Now().Add(4 * time.Hour)
			s.Now = func() time.Time { return now }
		}
		r, err = s.Appointment(ctx, actor, r.ID, action, mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version})
		if err != nil || r.State != "safety_hold" || body[mobile.Appointment](t, r).PendingVersion != 0 {
			t.Fatal(action, r, err)
		}
	}
}

func TestLegacyHoldGetsVersionBindingWithoutReusingOldConsent(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	r, terms := heldAppointment(t, s)
	a := body[mobile.Appointment](t, r)
	a.SafetyHoldVersion = 0
	a.SafetyReason = ""
	r = serverRecord(t, repo, mobile.Appointments, r, a)
	r, err := s.Appointment(ctx, 2, r.ID, "safety_repropose", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Terms: &terms, Note: "重新核对旧暂停事项后提出新安排"})
	if err != nil {
		t.Fatal(err)
	}
	a = body[mobile.Appointment](t, r)
	if r.State != "safety_hold" || a.SafetyHoldVersion <= 0 || len(a.Versions[1].Confirmations) != 1 || len(a.Versions[1].SafetyChecks) != 1 {
		t.Fatal(r, a)
	}
}

func TestRestoreRejectsNeverEffectivePendingVersion(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	p := pet(t, s, false)
	terms := mobile.AppointmentTerms{City: "武汉", Species: "cat", StartsAt: s.Now().Add(3 * time.Hour), Place: "公共会面点", Reception: "发布人", PetName: "小满", RequiredFees: "无费用", Payee: "不收款"}
	r, err := s.CreateAppointment(ctx, 2, mobile.AppointmentInput{CommandMeta: meta(), ContextKind: "visit", ContextID: p.ID, PeerID: 1, Terms: terms})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Appointment(ctx, 2, r.ID, "restore", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, RestoreVersion: 1}); err == nil {
		t.Fatal("pending version restored as history")
	}
}
