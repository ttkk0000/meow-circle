package mobile_test

import (
	"context"
	"strings"
	"testing"

	"kitty-circle/internal/mobile"
)

// Regression source only: builds and tests are not executed by this task.
func TestReplyNotificationKeepsParentAndPreciseCommentWithoutDuplicateDelivery(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	first, err := s.PetComment(ctx, 2, p.ID, mobile.CommentInput{Content: mobile.RichContent{Text: "原评论"}, IdempotencyKey: "anchor-original"})
	if err != nil {
		t.Fatal(err)
	}
	in := mobile.CommentInput{Content: mobile.RichContent{Text: "回复原评论"}, ReplyTo: first.ID, IdempotencyKey: "anchor-reply-original"}
	reply, err := s.PetComment(ctx, 3, p.ID, in)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.PetComment(ctx, 3, p.ID, in); err != nil {
		t.Fatal(err)
	}
	notices, err := s.List(ctx, 2, mobile.Notifications, 0, 0, 30)
	if err != nil || len(notices.Items) != 1 {
		t.Fatal("reply author receives one notice even when the original request replays", notices, err)
	}
	row := notices.Items[0]
	body, err := mobile.Decode[mobile.NotificationView](row)
	if err != nil || body.EventType != "pet.comment.replied" || body.TargetKind != mobile.Pets || body.TargetID != p.ID || body.TargetVersion == nil || *body.TargetVersion != p.Version {
		t.Fatal("notification must preserve the parent and transaction version", body, err)
	}
	if body.ReplyAnchor == nil || body.ReplyAnchor.Kind != mobile.PetComments || body.ReplyAnchor.ID != reply.ID || body.ReplyAnchor.Version != reply.Version {
		t.Fatal("anchor must point to the new reply, not the parent or an arbitrary list row", body)
	}
	if strings.Contains(string(row.Data), "push_state") || strings.Contains(string(row.Data), "deliver_after") || strings.Contains(string(row.Data), "push_receipts") {
		t.Fatal("public notification leaked delivery internals", string(row.Data))
	}
	if _, err = s.ReactComment(ctx, 3, mobile.PetComments, first.ID, true); err != nil {
		t.Fatal(err)
	}
	if _, err = s.ReactComment(ctx, 3, mobile.PetComments, first.ID, true); err != nil {
		t.Fatal(err)
	}
	notices, err = s.List(ctx, 2, mobile.Notifications, 0, 0, 30)
	if err != nil || len(notices.Items) != 2 {
		t.Fatal("same desired-state like must not create two notifications", notices, err)
	}
}

func TestDeletedCommentDoesNotPreventReadingOwnNotification(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	serverRecord(t, repo, mobile.Preferences, mobile.Record{OwnerID: 1, Key: "1", State: "active"}, mobile.Preference{PushPermission: true, Frequency: "realtime", Interactions: true})
	if _, err := s.RegisterPushDevice(ctx, 1, mobile.PushDeviceInput{Platform: "android", Token: "synthetic-anchor-device-token", Permission: "granted", Enabled: true}); err != nil {
		t.Fatal(err)
	}
	p := pet(t, s, true)
	comment, err := s.PetComment(ctx, 2, p.ID, mobile.CommentInput{Content: mobile.RichContent{Text: "可撤回的评论"}, IdempotencyKey: "anchor-deleted-comment"})
	if err != nil {
		t.Fatal(err)
	}
	notices, err := s.List(ctx, 1, mobile.Notifications, 0, 0, 30)
	if err != nil || len(notices.Items) != 1 {
		t.Fatal(notices, err)
	}
	if work, err := s.PushWork(ctx, notices.Items[0].ID); err != nil || len(work) != 1 {
		t.Fatal("visible comment should be eligible before deletion", work, err)
	}
	if err = s.Remove(ctx, 2, mobile.PetComments, comment.ID, comment.Version); err != nil {
		t.Fatal(err)
	}
	if _, err = s.Get(ctx, 1, mobile.PetComments, comment.ID); err == nil {
		t.Fatal("deleted comment must not remain readable through its anchor")
	}
	row := notices.Items[0]
	if work, err := s.PushWork(ctx, row.ID); err == nil || len(work) != 0 {
		t.Fatal("deleted anchor must close pending push even while parent remains visible", work, err)
	}
	if _, err = s.ReadNotification(ctx, 1, row.ID, row.Version); err != nil {
		t.Fatal("marking own notice read must not require access to the deleted comment", err)
	}
}

func TestLegacyNotificationDoesNotInventEventVersionOrAnchor(t *testing.T) {
	s, repo := fixture(t)
	row := serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 2, State: "unread"}, mobile.Notification{Category: "interactions", TargetKind: mobile.Entries, TargetID: 99, PushState: "delivered"})
	visible, err := s.Get(context.Background(), 2, mobile.Notifications, row.ID)
	if err != nil {
		t.Fatal(err)
	}
	body, err := mobile.Decode[mobile.NotificationView](visible)
	if err != nil || body.EventType != "" || body.TargetVersion != nil || body.ReplyAnchor != nil || visible.ID != row.ID || visible.Version != row.Version {
		t.Fatal("legacy event metadata must remain absent without changing identity", body, err)
	}
}
