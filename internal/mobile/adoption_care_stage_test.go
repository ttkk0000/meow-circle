package mobile_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// 仅回归源码；未执行。
func TestTrialCareIsPrivateAndRequiresConfirmedCareStage(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	a := body[mobile.Adoption](t, r)
	a.TrialConfirmations = []int64{1, 2}
	r.State = "trial_active"
	r = serverRecord(t, repo, mobile.Adoptions, r, a)
	update, err := s.Care(ctx, 2, r.ID, mobile.CareInput{CommandMeta: meta(), Text: "试养中的实际照护近况"})
	if err != nil || update.RefID != r.ID || update.Public {
		t.Fatal(update, err)
	}
	for _, actor := range []int64{1, 3} {
		if _, err = s.Care(ctx, actor, r.ID, mobile.CareInput{CommandMeta: meta(), Text: "不应代替当前照护者记录"}); !errors.Is(err, mobile.ErrForbidden) {
			t.Fatal(actor, err)
		}
	}
	a.TrialConfirmations = []int64{2}
	r = serverRecord(t, repo, mobile.Adoptions, r, a)
	if _, err = s.Care(ctx, 2, r.ID, mobile.CareInput{CommandMeta: meta(), Text: "仅单方确认，不应开始照护阶段"}); !errors.Is(err, mobile.ErrConflict) {
		t.Fatal(err)
	}
}

func TestPrecompletionReturnKeepsConfirmedTrialCareWithoutTransfer(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	a := body[mobile.Adoption](t, r)
	a.TrialConfirmations = []int64{1, 2}
	r.State = "return_requested"
	r = serverRecord(t, repo, mobile.Adoptions, r, a)
	if _, err := s.Care(ctx, 2, r.ID, mobile.CareInput{CommandMeta: meta(), Text: "协商安全接回前仍在照护"}); err != nil {
		t.Fatal(err)
	}
	if err := repo.View(ctx, func(tx mobile.Tx) error {
		current, e := tx.Get(mobile.Pets, p.ID)
		if e != nil {
			return e
		}
		if current.OwnerID != 1 {
			t.Fatal("care update transferred pet ownership")
		}
		return nil
	}); err != nil {
		t.Fatal(err)
	}
}

func TestHistoricalTrialProofDoesNotExtendPostcompletionCareWindow(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	r := closureApplication(t, s, repo, p.ID, 2)
	a := body[mobile.Adoption](t, r)
	a.TrialConfirmations = []int64{1, 2}
	past := s.Now().Add(-31 * 24 * time.Hour)
	a.CompletedAt = &past
	r.State = "return_agreed"
	r = serverRecord(t, repo, mobile.Adoptions, r, a)
	if _, err := s.Care(ctx, 2, r.ID, mobile.CareInput{CommandMeta: meta(), Text: "已过完成后的30天窗口"}); !errors.Is(err, mobile.ErrConflict) {
		t.Fatal(err)
	}
}
