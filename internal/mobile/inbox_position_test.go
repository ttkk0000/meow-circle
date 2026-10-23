package mobile_test

import (
	"context"
	"testing"

	"kitty-circle/internal/mobile"
)

// 仅新增回归源码；本任务不运行 Go 测试。
func TestFirstUnreadPositionIgnoresOwnMessagesAndAdvancesWithReceipt(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	conversation, err := s.Conversation(ctx, 1, mobile.ConversationInput{CommandMeta: meta(), UserID: 2})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.ConversationAction(ctx, 2, conversation.ID, conversation.Version, "accept"); err != nil {
		t.Fatal(err)
	}
	send := func(actor int64, key string) mobile.Record {
		t.Helper()
		r, e := s.Message(ctx, actor, conversation.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: key}, IdempotencyKey: key})
		if e != nil {
			t.Fatal(e)
		}
		return r
	}
	send(2, "own-message-position")
	first := send(1, "first-unread-position")
	second := send(1, "second-unread-position")
	u, err := s.Unread(ctx, 2)
	if err != nil || u.Direct[conversation.ID] != 2 || u.DirectFirstUnread[conversation.ID] != first.ID {
		t.Fatalf("first unread: %+v %v", u, err)
	}
	// 读取汇总不核销未读。
	again, err := s.Unread(ctx, 2)
	if err != nil || again.DirectFirstUnread[conversation.ID] != first.ID {
		t.Fatalf("GET changed receipt: %+v %v", again, err)
	}
	if _, err = s.ReadMessages(ctx, 2, conversation.ID, false, first.ID); err != nil {
		t.Fatal(err)
	}
	u, err = s.Unread(ctx, 2)
	if err != nil || u.Direct[conversation.ID] != 1 || u.DirectFirstUnread[conversation.ID] != second.ID {
		t.Fatalf("receipt position: %+v %v", u, err)
	}
	if _, err = s.ReadMessages(ctx, 2, conversation.ID, false, second.ID); err != nil {
		t.Fatal(err)
	}
	u, err = s.Unread(ctx, 2)
	if err != nil || u.Direct[conversation.ID] != 0 || u.DirectFirstUnread[conversation.ID] != 0 {
		t.Fatalf("all read position retained: %+v %v", u, err)
	}
	outsider, err := s.Unread(ctx, 3)
	if err != nil || len(outsider.DirectFirstUnread) != 0 {
		t.Fatalf("position leaked to outsider: %+v %v", outsider, err)
	}
}

func TestGlobalUnreadExcludesMutedGroupsAndNeverIncludesPendingTasks(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	group, err := s.CreateGroup(ctx, 1, mobile.GroupInput{CommandMeta: meta(), Name: "免打扰回归"})
	if err != nil {
		t.Fatal(err)
	}
	invite, err := s.Invite(ctx, 1, mobile.InviteInput{CommandMeta: meta(), Scope: "group_member", TargetID: group.ID, UserID: 2})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Invitation(ctx, 2, invite.ID, invite.Version, "accept"); err != nil {
		t.Fatal(err)
	}
	group, err = s.Get(ctx, 2, mobile.Groups, group.ID)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Message(ctx, 1, group.ID, true, mobile.MessageInput{Content: mobile.RichContent{Text: "真实未读"}, IdempotencyKey: "muted-group-message"}); err != nil {
		t.Fatal(err)
	}
	before, err := s.Unread(ctx, 2)
	if err != nil || before.Groups[group.ID] != 1 {
		t.Fatal(before, err)
	}
	if _, err = s.Group(ctx, 2, group.ID, "mute", mobile.GroupAction{CommandMeta: meta(), Version: group.Version}); err != nil {
		t.Fatal(err)
	}
	after, err := s.Unread(ctx, 2)
	if err != nil || after.Groups[group.ID] != 1 || after.GlobalTotal != before.GlobalTotal-1 {
		t.Fatal("mute changed local unread or retained global badge", before, after, err)
	}
}
