package mobile_test

import (
	"context"
	"errors"
	"reflect"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// 本批只补回归源码；未授权执行测试。
func leadFixture(t *testing.T) (*mobile.Service, mobile.Repository, mobile.Record, mobile.Record, mobile.Record, mobile.Record) {
	t.Helper()
	s, repo := fixture(t)
	p := serverRecord(t, repo, mobile.Pets, mobile.Record{OwnerID: 1, State: "active"}, mobile.Pet{Name: "小满", Species: "cat", Missing: true, Audience: mobile.Audience{Mode: "public"}})
	c := serverRecord(t, repo, mobile.Cases, mobile.Record{OwnerID: 1, Public: true, State: "active"}, mobile.Case{Type: "lost", PetID: p.ID, Title: "小满走失", Species: "cat", City: "武汉", Area: "江岸区"})
	photo := serverRecord(t, repo, mobile.Media, mobile.Record{OwnerID: 2, State: "ready"}, mobile.MediaFile{Scope: "private", MIME: "image/jpeg", Size: 10})
	lead, e := s.Lead(context.Background(), 2, c.ID, "傍晚看见一只相似的猫，仍需核对", []int64{photo.ID})
	if e != nil {
		t.Fatal(e)
	}
	return s, repo, p, c, lead, photo
}

func TestLeadAssessmentPreservesEvidenceAndCaseState(t *testing.T) {
	s, _, p, c, lead, _ := leadFixture(t)
	ctx := context.Background()
	in := mobile.LeadAssessmentInput{CommandMeta: meta(), Version: lead.Version, Assessment: "helpful"}
	for _, actor := range []int64{2, 3} {
		if _, e := s.AssessLead(ctx, actor, lead.ID, in); !errors.Is(e, mobile.ErrForbidden) {
			t.Fatal("non-publisher assessed a lead", actor, e)
		}
	}
	r, e := s.AssessLead(ctx, 1, lead.ID, in)
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Lead](t, r)
	if v.Assessment != "helpful" || v.ReviewedBy != 1 || v.ReviewedAt == nil || v.Text != body[mobile.Lead](t, lead).Text || !reflect.DeepEqual(r.MediaIDs, lead.MediaIDs) {
		t.Fatal("assessment changed evidence or omitted author/time")
	}
	replay, e := s.AssessLead(ctx, 1, lead.ID, in)
	if e != nil || replay.ID != r.ID || replay.Version != r.Version {
		t.Fatal("original request did not replay", e)
	}
	stale := in
	stale.CommandMeta = meta()
	stale.Assessment = "not_match"
	if _, e = s.AssessLead(ctx, 1, lead.ID, stale); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("stale assessment accepted", e)
	}
	stale.Version = r.Version
	if _, e = s.AssessLead(ctx, 1, lead.ID, stale); e != nil {
		t.Fatal(e)
	}
	currentCase, e := s.Get(ctx, 1, mobile.Cases, c.ID)
	if e != nil || currentCase.State != c.State || currentCase.Version != c.Version {
		t.Fatal("assessment changed case", e)
	}
	currentPet, e := s.Get(ctx, 1, mobile.Pets, p.ID)
	if e != nil || !body[mobile.Pet](t, currentPet).Missing || currentPet.Version != p.Version {
		t.Fatal("assessment claimed a pet was found", e)
	}
}

func TestLeadConversationIsIsolatedAndDoesNotGrantConsent(t *testing.T) {
	s, _, _, c, lead, _ := leadFixture(t)
	ctx := context.Background()
	plain, e := s.Conversation(ctx, 1, mobile.ConversationInput{CommandMeta: meta(), UserID: 2})
	if e != nil {
		t.Fatal(e)
	}
	in := mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version}
	conversation, e := s.LeadConversation(ctx, 1, lead.ID, in)
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Conversation](t, conversation)
	if conversation.ID == plain.ID || conversation.RefID != lead.ID || conversation.State != "pending" || v.ContextKind != "lead" || v.ContextID != lead.ID || v.RequesterID != 1 || v.RecipientID != 2 {
		t.Fatal("lead contact mixed with general chat or invented consent")
	}
	in.CommandMeta = meta()
	fromWitness, e := s.LeadConversation(ctx, 2, lead.ID, in)
	if e != nil || fromWitness.ID != conversation.ID {
		t.Fatal("witness opened a duplicate conversation", e)
	}
	other, e := s.Lead(ctx, 2, c.ID, "另一条需要单独核实的线索", nil)
	if e != nil {
		t.Fatal(e)
	}
	separate, e := s.LeadConversation(ctx, 1, other.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: other.Version})
	if e != nil || separate.ID == conversation.ID {
		t.Fatal("different leads shared a thread", e)
	}
	if _, e = s.LeadConversation(ctx, 3, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version}); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatal("outsider contacted a witness", e)
	}
	message, e := s.Message(ctx, 1, conversation.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "方便核对看到的时间吗"}, IdempotencyKey: "lead-first-message"})
	if e != nil || message.ID <= 0 {
		t.Fatal(e)
	}
	if _, e = s.Message(ctx, 1, conversation.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "第二条"}, IdempotencyKey: "lead-second-message"}); e == nil {
		t.Fatal("pending contact bypassed one-text limit")
	}
	if _, e = s.Message(ctx, 2, conversation.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "未接受先回复"}, IdempotencyKey: "lead-recipient-message"}); e == nil {
		t.Fatal("recipient bypassed explicit acceptance")
	}
}

func TestClosedCaseKeepsLeadHistoryButStopsNewContact(t *testing.T) {
	s, _, _, c, lead, _ := leadFixture(t)
	ctx := context.Background()
	conversation, e := s.LeadConversation(ctx, 1, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.CloseCase(ctx, 1, c.ID, mobile.StateInput{CommandMeta: meta(), Version: c.Version, Note: "本次停止接收"}); e != nil {
		t.Fatal(e)
	}
	for _, object := range []struct {
		kind mobile.Kind
		id   int64
	}{{mobile.Leads, lead.ID}, {mobile.Conversations, conversation.ID}} {
		if _, e = s.Get(ctx, 2, object.kind, object.id); e != nil {
			t.Fatal("history lost after closure", e)
		}
	}
	if _, e = s.AssessLead(ctx, 1, lead.ID, mobile.LeadAssessmentInput{CommandMeta: meta(), Version: lead.Version, Assessment: "helpful"}); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("closed case accepted assessment", e)
	}
	if _, e = s.LeadConversation(ctx, 2, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version}); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("closed case started contact", e)
	}
	if _, e = s.ConversationAction(ctx, 2, conversation.ID, conversation.Version, "accept"); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("closed case accepted a contact request", e)
	}
	if _, e = s.Message(ctx, 1, conversation.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "已关闭仍发消息"}, IdempotencyKey: "closed-lead-message"}); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("closed case accepted a new message", e)
	}
}

func TestRemovedCaseRevokesLeadConversationMessageAndMedia(t *testing.T) {
	s, repo, _, c, lead, photo := leadFixture(t)
	ctx := context.Background()
	conversation, e := s.LeadConversation(ctx, 1, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version})
	if e != nil {
		t.Fatal(e)
	}
	message, e := s.Message(ctx, 1, conversation.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "核对线索"}, IdempotencyKey: "removed-lead-message"})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Get(ctx, 1, mobile.Media, photo.ID); e != nil {
		t.Fatal("publisher could not read permitted private evidence", e)
	}
	c.State = "removed"
	serverRecord(t, repo, mobile.Cases, c, body[mobile.Case](t, c))
	for _, object := range []struct {
		kind mobile.Kind
		id   int64
	}{{mobile.Leads, lead.ID}, {mobile.Conversations, conversation.ID}, {mobile.Messages, message.ID}, {mobile.Media, photo.ID}} {
		if _, e = s.Get(ctx, 1, object.kind, object.id); !errors.Is(e, mobile.ErrNotFound) {
			t.Fatal("removed parent leaked private record", object.kind, e)
		}
	}
	if _, e = s.Get(ctx, 2, mobile.Media, photo.ID); e != nil {
		t.Fatal("original uploader lost own file", e)
	}
}

func TestLeadContactRechecksBlockAndVersion(t *testing.T) {
	s, _, _, _, lead, _ := leadFixture(t)
	ctx := context.Background()
	if _, e := s.LeadConversation(ctx, 1, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version + 1}); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("stale contact accepted", e)
	}
	if _, e := s.Friend(ctx, 2, "block", mobile.FriendInput{CommandMeta: meta(), UserID: 1}); e != nil {
		t.Fatal(e)
	}
	if _, e := s.LeadConversation(ctx, 1, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version}); e == nil {
		t.Fatal("blocked actor contacted witness")
	}
}

func TestLeadContactCannotBypassAdultContactPolicy(t *testing.T) {
	s, _, _, _, lead, _ := leadFixture(t)
	ctx := context.Background()
	if e := s.RecordAgeEvidence(ctx, "test-verifier", mobile.AgeEvidenceInput{ActorID: 2, AgeBand: "14_17", ValidUntil: s.Now().Add(time.Hour), EvidenceRef: "corrected-witness-age"}); e != nil {
		t.Fatal(e)
	}
	if _, e := s.LeadConversation(ctx, 1, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version}); e == nil {
		t.Fatal("lead contact bypassed adult stranger policy")
	}
}

func TestLeadAccessUsesCurrentPublisherInsteadOfParticipantSnapshot(t *testing.T) {
	s, repo, _, c, lead, _ := leadFixture(t)
	ctx := context.Background()
	previous, e := s.LeadConversation(ctx, 1, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version})
	if e != nil {
		t.Fatal(e)
	}
	// 模拟可信后台移交；线索上的旧Participants不构成永久读取权。
	c.OwnerID = 3
	serverRecord(t, repo, mobile.Cases, c, body[mobile.Case](t, c))
	if _, e = s.Get(ctx, 1, mobile.Leads, lead.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("previous publisher retained private access", e)
	}
	if _, e = s.Get(ctx, 2, mobile.Conversations, previous.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("old conversation survived a participant change", e)
	}
	if _, e = s.Get(ctx, 3, mobile.Leads, lead.ID); e != nil {
		t.Fatal("current publisher lost lead access", e)
	}
	current, e := s.LeadConversation(ctx, 3, lead.ID, mobile.LeadContactInput{CommandMeta: meta(), Version: lead.Version})
	if e != nil || current.ID == previous.ID || body[mobile.Conversation](t, current).RecipientID != 2 {
		t.Fatal("new publisher inherited old private thread", e)
	}
}
