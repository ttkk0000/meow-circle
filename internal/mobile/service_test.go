package mobile_test

import (
	"context"
	"errors"
	"fmt"
	"sync/atomic"
	"testing"
	"time"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/store"
)

func fixture(t *testing.T) (*mobile.Service, mobile.Repository) {
	t.Helper()
	memory := store.NewMemoryStore()
	for _, name := range []string{"publisher", "applicant", "outsider", "editor"} {
		if _, ok := memory.CreateUser(domain.User{Username: name}); !ok {
			t.Fatal("create user")
		}
	}
	repo := memory.MobileRepository()
	service := mobile.New(repo)
	if e := repo.Update(context.Background(), func(tx mobile.Tx) error {
		for id := int64(1); id <= 4; id++ {
			u, e := tx.User(id)
			if e != nil {
				return e
			}
			u.Phone = fmt.Sprintf("138001380%02d", id)
			if e = tx.SaveUser(&u); e != nil {
				return e
			}
			if e = mobile.RecordPhoneProof(tx, id, u.Phone, time.Now().UTC()); e != nil {
				return e
			}
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
	for id := int64(1); id <= 4; id++ {
		if e := service.RecordAgeEvidence(context.Background(), "test-verifier", mobile.AgeEvidenceInput{ActorID: id, AgeBand: "adult", ValidUntil: time.Now().Add(24 * time.Hour), EvidenceRef: "fixture-verified-age"}); e != nil {
			t.Fatal(e)
		}
	}
	for _, species := range []string{"cat", "dog"} {
		if _, e := service.RecordAuthority(context.Background(), "test-policy", mobile.AuthorityInput{Scope: "city_rule", City: "武汉", Species: species, RuleVersion: "fixture-rule", Actions: []string{"adoption", "commercial", "offline"}, EvidenceRef: "fixture-rule", ValidUntil: time.Now().Add(24 * time.Hour)}); e != nil {
			t.Fatal(e)
		}
	}
	return service, repo
}

var requestSequence atomic.Int64

func meta() mobile.CommandMeta {
	return mobile.CommandMeta{ClientRequestID: fmt.Sprintf("test-request-%d", requestSequence.Add(1))}
}
func declared(t *testing.T, s *mobile.Service, in mobile.EntryInput) mobile.EntryInput {
	t.Helper()
	in.CommandMeta = meta()
	in.Declaration.Origin = "unknown"
	if p, e := s.PreviewPublication(context.Background(), 1, in); e == nil {
		in.Declaration.ManifestHash = p.ManifestHash
		in.Declaration.Confirmed = true
	}
	return in
}

func pet(t *testing.T, s *mobile.Service, available bool) mobile.Record {
	t.Helper()
	in := mobile.PetInput{CommandMeta: meta(), Pet: mobile.Pet{Name: "小满", Species: "cat", Health: "仅本人可见", Audience: mobile.Audience{Mode: "public"}}}
	r, e := s.SavePet(context.Background(), 1, 0, in)
	if e != nil {
		t.Fatal(e)
	}
	if available {
		if _, e = s.RecordAuthority(context.Background(), "test-verifier", mobile.AuthorityInput{ActorID: 1, PetID: r.ID, Scope: "disposition", Species: "cat", EvidenceRef: "fixture-authority", ValidUntil: time.Now().Add(24 * time.Hour)}); e != nil {
			t.Fatal(e)
		}
		in.Version = r.Version
		in.Pet.AdoptionAvailable = true
		in.CommandMeta = meta()
		r, e = s.SavePet(context.Background(), 1, r.ID, in)
		if e != nil {
			t.Fatal(e)
		}
	}
	return r
}

func body[T any](t *testing.T, r mobile.Record) T {
	t.Helper()
	v, e := mobile.Decode[T](r)
	if e != nil {
		t.Fatal(e)
	}
	return v
}
func TestFriendsAudienceRevokesImmediately(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	request, e := s.Friend(ctx, 1, "request", mobile.FriendInput{CommandMeta: meta(), UserID: 2})
	if e != nil {
		t.Fatal(e)
	}
	friend, e := s.Friend(ctx, 2, "accept", mobile.FriendInput{CommandMeta: meta(), UserID: 1, Version: request.Version})
	if e != nil {
		t.Fatal(e)
	}
	entry, e := s.SaveEntry(ctx, 1, 0, declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Space: "circle", Text: "只给选中的朋友", Audience: mobile.Audience{Mode: "selected", UserIDs: []int64{2}}}}))
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Get(ctx, 2, mobile.Entries, entry.ID); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Get(ctx, 3, mobile.Entries, entry.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatalf("outsider: %v", e)
	}
	if _, e = s.Friend(ctx, 1, "remove", mobile.FriendInput{CommandMeta: meta(), UserID: 2, Version: friend.Version}); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Get(ctx, 2, mobile.Entries, entry.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatalf("removed friend retained access: %v", e)
	}
}
func TestAppointmentVersionsNeedTwoParticipants(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	p := pet(t, s, false)
	terms := mobile.AppointmentTerms{City: "武汉", Species: "cat", StartsAt: time.Now().Add(48 * time.Hour), Place: "武汉", Reception: "接待人", PetName: "小满", RequiredFees: "无", Payee: "接待人"}
	r, e := s.CreateAppointment(ctx, 2, mobile.AppointmentInput{CommandMeta: meta(), ContextKind: "visit", ContextID: p.ID, PeerID: 1, Terms: terms})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Appointment(ctx, 3, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version}); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatalf("outsider confirmation: %v", e)
	}
	r, e = s.Appointment(ctx, 1, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version})
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Appointment](t, r)
	if v.EffectiveVersion != 1 {
		t.Fatal("V1 not effective")
	}
	oldVersion := r.Version
	newTerms := terms
	newTerms.StartsAt = newTerms.StartsAt.Add(time.Hour)
	r, e = s.Appointment(ctx, 2, r.ID, "propose", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, Terms: &newTerms})
	if e != nil {
		t.Fatal(e)
	}
	v = body[mobile.Appointment](t, r)
	if v.EffectiveVersion != 1 || v.PendingVersion != 2 {
		t.Fatal("pending proposal replaced effective terms")
	}
	if _, e = s.Appointment(ctx, 1, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: oldVersion}); !errors.Is(e, mobile.ErrConflict) {
		t.Fatalf("stale confirmation: %v", e)
	}
	r, e = s.Appointment(ctx, 1, r.ID, "confirm", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version})
	if e != nil {
		t.Fatal(e)
	}
	r, e = s.Appointment(ctx, 2, r.ID, "restore", mobile.AppointmentAction{CommandMeta: meta(), Version: r.Version, RestoreVersion: 1})
	if e != nil {
		t.Fatal(e)
	}
	v = body[mobile.Appointment](t, r)
	if v.EffectiveVersion != 2 || v.PendingVersion != 3 || len(v.Versions) != 3 {
		t.Fatal("restore did not create V3")
	}
}
func TestGroupTransferDoesNotTransferUntilReceiverAccepts(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	g, e := s.CreateGroup(ctx, 1, mobile.GroupInput{CommandMeta: meta(), Name: "猫友群"})
	if e != nil {
		t.Fatal(e)
	}
	invite, e := s.Invite(ctx, 1, mobile.InviteInput{CommandMeta: meta(), Scope: "group_member", TargetID: g.ID, UserID: 2})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Invitation(ctx, 2, invite.ID, invite.Version, "accept"); e != nil {
		t.Fatal(e)
	}
	g, e = s.Get(ctx, 1, mobile.Groups, g.ID)
	if e != nil {
		t.Fatal(e)
	}
	g, e = s.Group(ctx, 1, g.ID, "transfer", mobile.GroupAction{CommandMeta: meta(), Version: g.Version, UserID: 2})
	if e != nil {
		t.Fatal(e)
	}
	if g.OwnerID != 1 {
		t.Fatal("owner changed on proposal")
	}
	if _, e = s.Group(ctx, 1, g.ID, "leave", mobile.GroupAction{CommandMeta: meta(), Version: g.Version}); e == nil {
		t.Fatal("owner left before acceptance")
	}
	g, e = s.Group(ctx, 2, g.ID, "accept_transfer", mobile.GroupAction{CommandMeta: meta(), Version: g.Version})
	if e != nil {
		t.Fatal(e)
	}
	if g.OwnerID != 2 {
		t.Fatal("receiver not owner")
	}
	if _, e = s.Group(ctx, 1, g.ID, "dissolve", mobile.GroupAction{CommandMeta: meta(), Version: g.Version}); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatalf("old owner dissolved group: %v", e)
	}
}
func TestClaimEvidenceAndDualHandover(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	c, e := s.SaveCase(ctx, 1, 0, mobile.CaseInput{CommandMeta: meta(), Case: mobile.Case{Type: "found", Title: "发现一只猫", Species: "cat", Description: "请提供私密特征", City: "武汉", Area: "武昌", Latitude: 30.123456, Longitude: 114.123456, PrivateLocation: "不公开的门牌", OccurredAt: time.Now().Add(-time.Hour)}})
	if e != nil {
		t.Fatal(e)
	}
	public, e := s.Get(ctx, 0, mobile.Cases, c.ID)
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Case](t, public)
	if v.PrivateLocation != "" || v.Latitude != 30.12 {
		t.Fatal("precise location leaked")
	}
	claim, e := s.Claim(ctx, 2, c.ID, mobile.ClaimInput{CommandMeta: meta(), PetName: "小满", Evidence: "私密花纹与就诊记录"})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Get(ctx, 3, mobile.Claims, claim.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatalf("claim leaked: %v", e)
	}
	claim, e = s.ClaimAction(ctx, 1, claim.ID, "match", mobile.StateInput{CommandMeta: meta(), Version: claim.Version})
	if e != nil {
		t.Fatal(e)
	}
	claim, e = s.ClaimAction(ctx, 1, claim.ID, "confirm_handover", mobile.StateInput{CommandMeta: meta(), Version: claim.Version})
	if e != nil {
		t.Fatal(e)
	}
	if claim.State == "completed" {
		t.Fatal("one-sided handover completed")
	}
	claim, e = s.ClaimAction(ctx, 2, claim.ID, "confirm_handover", mobile.StateInput{CommandMeta: meta(), Version: claim.Version})
	if e != nil {
		t.Fatal(e)
	}
	if claim.State != "completed" {
		t.Fatal("dual handover missing")
	}
	c, e = s.Get(ctx, 0, mobile.Cases, c.ID)
	if e != nil || c.State != "closed" {
		t.Fatalf("case not closed: %v", e)
	}
}
func TestEditorCannotMakeAdoptionDecisions(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	i, e := s.Invite(ctx, 1, mobile.InviteInput{CommandMeta: meta(), Scope: "adoption_editor", TargetID: p.ID, UserID: 4})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Invitation(ctx, 4, i.ID, i.Version, "accept"); e != nil {
		t.Fatal(e)
	}
	edited, e := s.Introduction(ctx, 4, p.ID, mobile.IntroductionInput{CommandMeta: meta(), Version: p.Version, Bio: "补充领养介绍"})
	if e != nil {
		t.Fatal(e)
	}
	if body[mobile.Pet](t, edited).Health != "" {
		t.Fatal("editor received private health notes")
	}
	a, e := s.ApplyAdoption(ctx, 2, mobile.AdoptionInput{CommandMeta: meta(), PetID: p.ID, Message: "希望领养"})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Adoption(ctx, 4, a.ID, "approve", mobile.AdoptionAction{CommandMeta: meta(), Version: a.Version}); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatalf("editor made decision: %v", e)
	}
}
func TestMessageRequestAndReadReceipt(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	c, e := s.Conversation(ctx, 1, mobile.ConversationInput{CommandMeta: meta(), UserID: 2})
	if e != nil {
		t.Fatal(e)
	}
	m, e := s.Message(ctx, 1, c.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "你好"}, IdempotencyKey: "first-message"})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Message(ctx, 1, c.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "第二条"}, IdempotencyKey: "second-message"}); e == nil {
		t.Fatal("unaccepted request sent two messages")
	}
	again, e := s.Message(ctx, 1, c.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "你好"}, IdempotencyKey: "first-message"})
	if e != nil || again.ID != m.ID {
		t.Fatal("retry duplicated message")
	}
	u, e := s.Unread(ctx, 2)
	if e != nil || u.Direct[c.ID] != 1 {
		t.Fatalf("unread: %+v %v", u, e)
	}
	if _, e = s.ReadMessages(ctx, 2, c.ID, false, m.ID); e != nil {
		t.Fatal(e)
	}
	u, e = s.Unread(ctx, 2)
	if e != nil || u.Direct[c.ID] != 0 {
		t.Fatalf("receipt not applied: %+v %v", u, e)
	}
}
func TestTransactionRollbackAndCAS(t *testing.T) {
	_, repo := fixture(t)
	ctx := context.Background()
	sentinel := errors.New("abort")
	e := repo.Update(ctx, func(tx mobile.Tx) error {
		r := mobile.Record{OwnerID: 1, State: "active", Key: "rollback", Data: []byte(`{}`)}
		if e := tx.Save(mobile.Pets, &r); e != nil {
			return e
		}
		return sentinel
	})
	if !errors.Is(e, sentinel) {
		t.Fatal(e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error { _, e := tx.ByKey(mobile.Pets, "rollback"); return e }); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("transaction did not roll back")
	}
	var r mobile.Record
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		r = mobile.Record{OwnerID: 1, State: "active", Data: []byte(`{}`)}
		return tx.Save(mobile.Pets, &r)
	}); e != nil {
		t.Fatal(e)
	}
	stale := r
	if e = repo.Update(ctx, func(tx mobile.Tx) error { return tx.Save(mobile.Pets, &r) }); e != nil {
		t.Fatal(e)
	}
	if e = repo.Update(ctx, func(tx mobile.Tx) error { return tx.Save(mobile.Pets, &stale) }); !errors.Is(e, mobile.ErrConflict) {
		t.Fatalf("stale write accepted: %v", e)
	}
}
func TestPrivateUploadCannotBecomePublicByAccident(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	m, e := s.RegisterMedia(ctx, 1, "private-evidence", mobile.MediaFile{Scope: "private", SHA256: "example", Filename: "private.jpg"})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.SaveEntry(ctx, 1, 0, declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Space: "community", Text: "图片", Audience: mobile.Audience{Mode: "public"}}, MediaIDs: []int64{m.ID}})); e == nil {
		t.Fatal("private evidence published")
	}
	if _, e = s.MediaFile(ctx, 2, m.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatalf("private media accessible: %v", e)
	}
}

func TestCountsAreAuthoritativeAndRetrySafe(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	entry, e := s.SaveEntry(ctx, 1, 0, declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Space: "community", Text: "小满", Audience: mobile.Audience{Mode: "public"}}}))
	if e != nil {
		t.Fatal(e)
	}
	for n := 0; n < 2; n++ {
		if _, e = s.React(ctx, 2, entry.ID, true); e != nil {
			t.Fatal(e)
		}
	}
	comment, e := s.Comment(ctx, 2, entry.ID, mobile.CommentInput{Content: mobile.RichContent{Text: "可爱"}, IdempotencyKey: "comment-retry"})
	if e != nil {
		t.Fatal(e)
	}
	metrics, e := s.Metrics(ctx, 2, []int64{entry.ID})
	if e != nil || len(metrics) != 1 || metrics[0].Likes != 1 || metrics[0].Comments != 1 || !metrics[0].ViewerLiked {
		t.Fatalf("counts: %+v %v", metrics, e)
	}
	if e = s.Remove(ctx, 2, mobile.Comments, comment.ID, comment.Version); e != nil {
		t.Fatal(e)
	}
	metrics, e = s.Metrics(ctx, 0, []int64{entry.ID})
	if e != nil || metrics[0].Comments != 0 || metrics[0].ViewerLiked {
		t.Fatalf("removed comment or anonymous state: %+v %v", metrics, e)
	}
}
