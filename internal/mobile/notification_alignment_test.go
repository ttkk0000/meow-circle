package mobile_test

import (
	"context"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// Regression source only. This batch does not execute Go builds or tests.
func TestHistoricalNotificationClassificationPreservesDeliveryAndReadState(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	old := serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 2, State: "unread"}, mobile.Notification{
		Category: "interactions", TargetKind: mobile.Friendships, TargetID: 91, PushState: "delivered",
		PushReceipts: []mobile.PushAcceptance{{Accepted: true, MessageID: "already-delivered"}},
	})
	watch := serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 2, Key: "case-watch:42:3:2", State: "unread"}, mobile.Notification{
		Category: "interactions", TargetKind: mobile.Cases, TargetID: 42, PushState: "suppressed",
	})
	serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 2, State: "unread"}, mobile.Notification{Category: "interactions", TargetKind: mobile.Cases, TargetID: 42})
	serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 2, State: "unread"}, mobile.Notification{Category: "interactions", TargetKind: mobile.Entries, TargetID: 9})
	for _, row := range []mobile.Record{old, watch} {
		projected, err := s.Get(ctx, 2, mobile.Notifications, row.ID)
		if err != nil {
			t.Fatal(err)
		}
		body, err := mobile.Decode[mobile.Notification](projected)
		if err != nil || body.Category != "applications" || body.Read || projected.ID != row.ID || projected.Version != row.Version {
			t.Fatal("historical category must change only in projection", projected, body, err)
		}
	}
	u, err := s.Unread(ctx, 2)
	if err != nil || u.Notifications["applications"] != 2 || u.Notifications["interactions"] != 2 || u.GlobalTotal != 4 {
		t.Fatal("list and summary must agree without counting a new event", u, err)
	}
	if err = repo.View(ctx, func(tx mobile.Tx) error {
		stored, e := tx.Get(mobile.Notifications, old.ID)
		if e != nil {
			return e
		}
		body, e := mobile.Decode[mobile.Notification](stored)
		if e != nil || body.Category != "interactions" || body.PushState != "delivered" || len(body.PushReceipts) != 1 || stored.Version != old.Version {
			t.Fatal("reading must preserve stored category and delivery", body, e)
		}
		return nil
	}); err != nil {
		t.Fatal(err)
	}
	if _, err = s.ReadNotification(ctx, 2, old.ID, old.Version); err != nil {
		t.Fatal(err)
	}
	u, err = s.Unread(ctx, 2)
	if err != nil || u.Notifications["applications"] != 1 || u.GlobalTotal != 3 {
		t.Fatal("reading one historical notification must decrement once", u, err)
	}
}

func TestRelationshipPendingIsIndependentOfReadAndExpiresWithCurrentGroup(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	input := mobile.FriendInput{CommandMeta: meta(), UserID: 2, Reason: "cat friends"}
	request, err := s.Friend(ctx, 1, "request", input)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Friend(ctx, 1, "request", input); err != nil {
		t.Fatal("original request must replay without another notification", err)
	}
	future, past := time.Now().Add(time.Hour), time.Now().Add(-time.Hour)
	serverRecord(t, repo, mobile.Groups, mobile.Record{OwnerID: 1, State: "active", Participants: []int64{2}}, mobile.Group{Name: "current", Members: []int64{1, 2}, PendingTransferTo: 2, TransferExpiresAt: &future})
	serverRecord(t, repo, mobile.Groups, mobile.Record{OwnerID: 1, State: "active", Participants: []int64{2}}, mobile.Group{Name: "expired", Members: []int64{1, 2}, PendingTransferTo: 2, TransferExpiresAt: &past})
	serverRecord(t, repo, mobile.Groups, mobile.Record{OwnerID: 1, State: "dissolved", Participants: []int64{2}}, mobile.Group{Name: "closed", Members: []int64{1, 2}, PendingTransferTo: 2, TransferExpiresAt: &future})
	u, err := s.Unread(ctx, 2)
	if err != nil || u.Pending["friendships"] != 1 || u.Pending["group_transfers"] != 1 || u.Notifications["applications"] != 1 {
		t.Fatal("only current recipient tasks count", u, err)
	}
	notices, err := s.List(ctx, 2, mobile.Notifications, 0, 0, 30)
	if err != nil || len(notices.Items) != 1 {
		t.Fatal("idempotent friend request must create one notification", notices, err)
	}
	if _, err = s.ReadNotification(ctx, 2, notices.Items[0].ID, notices.Items[0].Version); err != nil {
		t.Fatal(err)
	}
	u, err = s.Unread(ctx, 2)
	if err != nil || u.Pending["friendships"] != 1 || u.Notifications["applications"] != 0 {
		t.Fatal("read notification must not accept the friend request", u, err)
	}
	if _, err = s.Friend(ctx, 2, "accept", mobile.FriendInput{CommandMeta: meta(), UserID: 1, Version: request.Version}); err != nil {
		t.Fatal(err)
	}
	u, err = s.Unread(ctx, 2)
	if err != nil || u.Pending["friendships"] != 0 {
		t.Fatal("accepted request must leave the pending count", u, err)
	}
}
