package mobile_test

import (
	"context"
	"errors"
	"testing"

	"kitty-circle/internal/mobile"
)

// 仅回归源码；本轮未执行。
func TestLeadSubmissionReplaysReceiptWithoutDuplicateLeadOrNotice(t *testing.T) {
	s, repo, pet, parent, _, photo := leadFixture(t)
	ctx := context.Background()
	in := mobile.LeadSubmissionInput{CommandMeta: meta(), Version: parent.Version, Text: "今晚看到相似猫，仍需核对", MediaIDs: []int64{photo.ID}}
	first, e := s.SubmitLead(ctx, 2, parent.ID, in)
	if e != nil {
		t.Fatal(e)
	}
	again, e := s.SubmitLead(ctx, 2, parent.ID, in)
	if e != nil || again.ID != first.ID || again.Version != first.Version {
		t.Fatal("original lead request did not replay", e)
	}
	receipt, e := s.Operation(ctx, 2, in.ClientRequestID)
	if e != nil || receipt.ResultKind != mobile.Leads || receipt.ResultID != first.ID {
		t.Fatal("missing original lead receipt", e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error {
		count, err := tx.Count(mobile.Leads, mobile.Filter{RefID: parent.ID})
		if count != 2 {
			t.Fatal("receipt retry created a duplicate lead", count)
		}
		notices, err2 := tx.Count(mobile.Notifications, mobile.Filter{OwnerID: 1})
		if notices != 2 {
			t.Fatal("receipt retry created a duplicate notice", notices)
		}
		if err != nil {
			return err
		}
		return err2
	}); e != nil {
		t.Fatal(e)
	}
	current, e := s.Get(ctx, 1, mobile.Cases, parent.ID)
	if e != nil || current.Version != parent.Version || current.State != parent.State {
		t.Fatal("submission changed public case", e)
	}
	p, e := s.Get(ctx, 1, mobile.Pets, pet.ID)
	if e != nil || p.Version != pet.Version || !body[mobile.Pet](t, p).Missing {
		t.Fatal("submission claimed pet was found", e)
	}
}

func TestLeadSubmissionRejectsChangedOriginalRequest(t *testing.T) {
	s, _, _, parent, _, _ := leadFixture(t)
	ctx := context.Background()
	in := mobile.LeadSubmissionInput{CommandMeta: meta(), Version: parent.Version, Text: "原始目击描述"}
	if _, e := s.SubmitLead(ctx, 2, parent.ID, in); e != nil {
		t.Fatal(e)
	}
	in.Text = "重用原编号改写目击描述"
	if _, e := s.SubmitLead(ctx, 2, parent.ID, in); e == nil {
		t.Fatal("changed original payload accepted")
	}
	if _, e := s.Operation(ctx, 3, in.ClientRequestID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("other actor read submission receipt", e)
	}
}

func TestLeadSubmissionChecksVersionClosedCaseAndPrivateAttachments(t *testing.T) {
	s, _, _, parent, _, photo := leadFixture(t)
	ctx := context.Background()
	in := mobile.LeadSubmissionInput{CommandMeta: meta(), Version: parent.Version + 1, Text: "有待核对的目击", MediaIDs: []int64{photo.ID}}
	if _, e := s.SubmitLead(ctx, 2, parent.ID, in); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("stale case version accepted", e)
	}
	in.Version = parent.Version
	if _, e := s.SubmitLead(ctx, 3, parent.ID, in); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatal("foreign private photo attached", e)
	}
	closed, e := s.CloseCase(ctx, 1, parent.ID, mobile.StateInput{CommandMeta: meta(), Version: parent.Version, Note: "停止接收"})
	if e != nil {
		t.Fatal(e)
	}
	in.Version = closed.Version
	if _, e = s.SubmitLead(ctx, 2, parent.ID, in); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("closed case accepted a new lead", e)
	}
}

func TestLeadSubmissionKeepsRecordPrivateAndLegacyEndpointCompatible(t *testing.T) {
	s, _, _, parent, _, _ := leadFixture(t)
	ctx := context.Background()
	created, e := s.SubmitLead(ctx, 2, parent.ID, mobile.LeadSubmissionInput{CommandMeta: meta(), Version: parent.Version, Text: "私密目击，不公开地点"})
	if e != nil || created.Public || created.RefID != parent.ID || created.OwnerID != 2 {
		t.Fatal("lead became public or lost its source", e)
	}
	if _, e = s.Get(ctx, 3, mobile.Leads, created.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("outsider read private lead", e)
	}
	legacy, e := s.Lead(ctx, 2, parent.ID, "兼容旧版线索", nil)
	if e != nil {
		t.Fatal(e)
	}
	replay, e := s.Lead(ctx, 2, parent.ID, "兼容旧版线索", nil)
	if e != nil || replay.ID != legacy.ID {
		t.Fatal("legacy lead compatibility changed", e)
	}
}
