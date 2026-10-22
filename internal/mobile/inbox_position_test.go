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
