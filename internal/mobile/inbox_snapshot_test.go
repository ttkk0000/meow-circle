package mobile_test

import (
	"context"
	"errors"
	"fmt"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// Regression source only; no build or test has been executed for this batch.
func inboxConversation(t *testing.T, s *mobile.Service, accepted bool) mobile.Record {
	t.Helper()
	r, e := s.Conversation(context.Background(), 1, mobile.ConversationInput{CommandMeta: meta(), UserID: 2})
	if e != nil {
		t.Fatal(e)
	}
	if accepted {
		r, e = s.ConversationAction(context.Background(), 2, r.ID, r.Version, "accept")
		if e != nil {
			t.Fatal(e)
		}
	}
	return r
}

func inboxMessage(t *testing.T, s *mobile.Service, parent int64, group bool) mobile.Record {
	t.Helper()
	r, e := s.Message(context.Background(), 1, parent, group, mobile.MessageInput{
		Content: mobile.RichContent{Text: "new message"}, IdempotencyKey: meta().ClientRequestID,
	})
	if e != nil {
		t.Fatal(e)
	}
	return r
}

func TestInboxReadAllKeepsArrivalsAfterSnapshotAndReplaysOriginal(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	c := inboxConversation(t, s, true)
	inboxMessage(t, s, c.ID, false)
	oldNotice := serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 2, State: "unread"}, mobile.Notification{Category: "system"})
	prepare := mobile.InboxSnapshotInput{CommandMeta: meta()}
	snapshot, e := s.PrepareInboxReadAll(ctx, 2, prepare)
	if e != nil || snapshot.Count != 2 {
		t.Fatal("snapshot must count the chat once, without its push copy", snapshot, e)
	}
	for i := 0; i < 2; i++ {
		u, e := s.Unread(ctx, 2)
		if e != nil || u.Direct[c.ID] != 1 || u.Notifications["system"] != 1 {
			t.Fatal("GET or prepare consumed unread", u, e)
		}
	}
	inboxMessage(t, s, c.ID, false)
	serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 2, State: "unread"}, mobile.Notification{Category: "system"})
	confirm := mobile.InboxReadAllInput{CommandMeta: meta(), SnapshotID: snapshot.SnapshotID, Version: snapshot.Version}
	result, e := s.ConfirmInboxReadAll(ctx, 2, confirm)
	if e != nil || result.AppliedCount != 2 {
		t.Fatal(result, e)
	}
	again, e := s.ConfirmInboxReadAll(ctx, 2, confirm)
	if e != nil || again != result {
		t.Fatal("retry changed the original result", again, e)
	}
	preparedAgain, e := s.PrepareInboxReadAll(ctx, 2, prepare)
	if e != nil || preparedAgain != snapshot {
		t.Fatal("prepare retry created a new scope", preparedAgain, e)
	}
	u, e := s.Unread(ctx, 2)
	if e != nil || u.Direct[c.ID] != 1 || u.Notifications["system"] != 1 {
		t.Fatal("new arrivals were consumed", u, e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.Notifications, oldNotice.ID)
		if e == nil && r.State != "read" {
			t.Fatal("old notification not read", r)
		}
		return e
	}); e != nil {
		t.Fatal(e)
	}
}

func TestInboxReadAllDoesNotAcceptRequestsOrClearPendingWork(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	c := inboxConversation(t, s, false)
	inboxMessage(t, s, c.ID, false)
	invitation := serverRecord(t, repo, mobile.Invitations, mobile.Record{OwnerID: 1, Participants: []int64{2}, State: "pending"}, mobile.Invitation{Scope: "group_member", RecipientID: 2, ExpiresAt: time.Now().Add(time.Hour)})
	before, e := s.Unread(ctx, 2)
	if e != nil || before.MessageRequests != 1 || before.Pending["message_requests"] != 1 || before.Pending["invitations"] != 1 {
		t.Fatal(before, e)
	}
	snapshot, e := s.PrepareInboxReadAll(ctx, 2, mobile.InboxSnapshotInput{CommandMeta: meta()})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.ConfirmInboxReadAll(ctx, 2, mobile.InboxReadAllInput{CommandMeta: meta(), SnapshotID: snapshot.SnapshotID, Version: snapshot.Version}); e != nil {
		t.Fatal(e)
	}
	after, e := s.Unread(ctx, 2)
	if e != nil || after.MessageRequests != 0 || after.Pending["message_requests"] != 1 || after.Pending["invitations"] != 1 {
		t.Fatal("read and actionable states were conflated", after, e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error {
		for kind, id := range map[mobile.Kind]int64{mobile.Conversations: c.ID, mobile.Invitations: invitation.ID} {
			r, e := tx.Get(kind, id)
			if e != nil {
				return e
			}
			if r.State != "pending" {
				t.Fatal("reading performed a business action", kind, r.State)
			}
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
}

func TestInboxReadAllIncludesMutedGroupAndRechecksMembership(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	makeGroup := func(name string) mobile.Record {
		return serverRecord(t, repo, mobile.Groups, mobile.Record{OwnerID: 1, Participants: []int64{1, 2}, State: "active"},
			mobile.Group{Name: name, Members: []int64{1, 2}, Muted: []int64{2}})
	}
	kept, removed := makeGroup("kept"), makeGroup("removed")
	inboxMessage(t, s, kept.ID, true)
	inboxMessage(t, s, removed.ID, true)
	snapshot, e := s.PrepareInboxReadAll(ctx, 2, mobile.InboxSnapshotInput{CommandMeta: meta()})
	if e != nil || snapshot.Count != 2 || snapshot.MutedCount != 2 {
		t.Fatal("muted messages missing from confirmation", snapshot, e)
	}
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.Groups, removed.ID)
		if e != nil {
			return e
		}
		r.Participants = []int64{1}
		if e = mobile.SetBody(&r, mobile.Group{Name: "removed", Members: []int64{1}}); e != nil {
			return e
		}
		return tx.Save(mobile.Groups, &r)
	}); e != nil {
		t.Fatal(e)
	}
	result, e := s.ConfirmInboxReadAll(ctx, 2, mobile.InboxReadAllInput{CommandMeta: meta(), SnapshotID: snapshot.SnapshotID, Version: snapshot.Version})
	if e != nil || result.AppliedCount != 1 || result.SkippedCount != 1 {
		t.Fatal("membership was not rechecked", result, e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error {
		_, e := tx.ByKey(mobile.Receipts, fmt.Sprintf("groups:%d:2", removed.ID))
		if !errors.Is(e, mobile.ErrNotFound) {
			t.Fatal("read marker written after removal", e)
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
}

func TestInboxReadAllRejectsExpiredForeignAndChangedRequests(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	c := inboxConversation(t, s, true)
	inboxMessage(t, s, c.ID, false)
	snapshot, e := s.PrepareInboxReadAll(ctx, 2, mobile.InboxSnapshotInput{CommandMeta: meta()})
	if e != nil {
		t.Fatal(e)
	}
	in := mobile.InboxReadAllInput{CommandMeta: meta(), SnapshotID: snapshot.SnapshotID, Version: snapshot.Version}
	if _, e = s.ConfirmInboxReadAll(ctx, 3, in); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatal("foreign snapshot accepted", e)
	}
	now := s.Now()
	s.Now = func() time.Time { return now.Add(11 * time.Minute) }
	if _, e = s.ConfirmInboxReadAll(ctx, 2, in); e == nil {
		t.Fatal("expired snapshot accepted without an expiry worker")
	}
	u, e := s.Unread(ctx, 2)
	if e != nil || u.Direct[c.ID] != 1 {
		t.Fatal("rejection consumed unread", u, e)
	}
	s.Now = func() time.Time { return now }
	if _, e = s.ConfirmInboxReadAll(ctx, 2, in); e != nil {
		t.Fatal(e)
	}
	in.SnapshotID++
	var rule mobile.RuleError
	if _, e = s.ConfirmInboxReadAll(ctx, 2, in); !errors.As(e, &rule) || rule.Code != "idempotency_conflict" {
		t.Fatal("same key changed scope", e)
	}
}

func TestInboxReadAllCannotRegressAnotherDeviceReadPosition(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	c := inboxConversation(t, s, true)
	inboxMessage(t, s, c.ID, false)
	snapshot, e := s.PrepareInboxReadAll(ctx, 2, mobile.InboxSnapshotInput{CommandMeta: meta()})
	if e != nil {
		t.Fatal(e)
	}
	second := inboxMessage(t, s, c.ID, false)
	if _, e = s.ReadMessages(ctx, 2, c.ID, false, second.ID); e != nil {
		t.Fatal(e)
	}
	if _, e = s.ConfirmInboxReadAll(ctx, 2, mobile.InboxReadAllInput{CommandMeta: meta(), SnapshotID: snapshot.SnapshotID, Version: snapshot.Version}); e != nil {
		t.Fatal(e)
	}
	third := inboxMessage(t, s, c.ID, false)
	u, e := s.Unread(ctx, 2)
	if e != nil || u.Direct[c.ID] != 1 || u.DirectFirstUnread[c.ID] != third.ID {
		t.Fatal("older snapshot regressed the cursor", u, e)
	}
}
