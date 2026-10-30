package mobile_test

import (
	"context"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// Regression source only; not executed by the coding agent.
func TestDailyDigestSharesProviderAcceptanceWithoutClearingUnread(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	cutoff := time.Date(2026, 10, 29, 11, 0, 0, 0, time.UTC)
	s.Now = func() time.Time { return cutoff }
	serverRecord(t, repo, mobile.Preferences, mobile.Record{OwnerID: 1, Key: "1", State: "active"},
		mobile.Preference{PushPermission: true, Frequency: "digest", DirectMessages: true, Interactions: true})
	if _, err := s.RegisterPushDevice(ctx, 1, mobile.PushDeviceInput{Platform: "android", Token: "synthetic-digest-device-token", Permission: "granted", Enabled: true}); err != nil {
		t.Fatal(err)
	}
	first := serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 1, State: "unread"}, mobile.Notification{Category: "interactions", PushState: "queued", DeliverAfter: &cutoff})
	second := serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 1, State: "unread"}, mobile.Notification{Category: "direct_messages", PushState: "queued", DeliverAfter: &cutoff})
	p := &fixturePush{}
	s.Push = p
	for _, row := range []mobile.Record{first, second} {
		work, err := s.PushWork(ctx, row.ID)
		if err != nil || len(work) != 1 || work[0].Payload.NotificationID != 0 {
			t.Fatalf("digest should open the inbox: work=%v err=%v", work, err)
		}
		if err := s.DispatchPush(ctx, "digest-fixture", row.ID); err != nil {
			t.Fatal(err)
		}
	}
	if p.sends != 1 {
		t.Fatalf("provider sends=%d, want one daily digest", p.sends)
	}
	if err := repo.View(ctx, func(tx mobile.Tx) error {
		for _, id := range []int64{first.ID, second.ID} {
			row, err := tx.Get(mobile.Notifications, id)
			if err != nil {
				return err
			}
			n, err := mobile.Decode[mobile.Notification](row)
			if err != nil {
				return err
			}
			if row.State != "unread" || n.Read || n.PushState != "accepted" {
				t.Fatalf("digest must preserve unread and record acceptance: %+v %+v", row, n)
			}
		}
		return nil
	}); err != nil {
		t.Fatal(err)
	}
}
