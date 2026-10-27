package mobile_test

import (
	"context"
	"encoding/json"
	"errors"
	"strings"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// 本轮仅补回归源码，不执行测试。
func TestOperatorReviewQueueIsMinimalAndDetailsRequireNamedIdentity(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	var row mobile.Record
	if err := repo.Update(ctx, func(tx mobile.Tx) error {
		row = mobile.Record{OwnerID: 1, State: "submitted", MediaIDs: []int64{91}}
		if e := mobile.SetBody(&row, mobile.Review{Type: "report", Reason: "private statement", InternalNote: "private staff note"}); e != nil {
			return e
		}
		return tx.Save(mobile.Reviews, &row)
	}); err != nil {
		t.Fatal(err)
	}
	if _, err := s.StaffReviewQueue(ctx, 0); !errors.Is(err, mobile.ErrForbidden) {
		t.Fatal("unnamed queue read", err)
	}
	if _, err := s.StaffReviewDetails(ctx, row.ID); !errors.Is(err, mobile.ErrForbidden) {
		t.Fatal("unnamed detail read", err)
	}
	staff := mobile.WithOperator(ctx, "moderator-fixture")
	queue, err := s.StaffReviewQueue(staff, 0)
	if err != nil || len(queue.Items) != 1 || queue.Items[0].ID != row.ID {
		t.Fatal(queue, err)
	}
	raw, err := json.Marshal(queue)
	if err != nil || strings.Contains(string(raw), "private") || strings.Contains(string(raw), "media_ids") || strings.Contains(string(raw), "owner_id") {
		t.Fatal("queue exposed detail", string(raw), err)
	}
	detail, err := s.StaffReviewDetails(staff, row.ID)
	if err != nil || detail.ID != row.ID || len(detail.MediaIDs) != 1 {
		t.Fatal(detail, err)
	}
	if _, err = s.StaffReviewQueue(staff, -1); err == nil {
		t.Fatal("negative cursor accepted")
	}
}

func TestOperatorReviewDetailsDoNotRestoreExpiredEvidenceReferences(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	var row mobile.Record
	past := time.Now().Add(-time.Hour)
	if err := repo.Update(ctx, func(tx mobile.Tx) error {
		row = mobile.Record{OwnerID: 1, State: "submitted", MediaIDs: []int64{92}}
		if e := mobile.SetBody(&row, mobile.Review{Type: "verification", Reason: "original statement", EvidenceExpiresAt: &past}); e != nil {
			return e
		}
		return tx.Save(mobile.Reviews, &row)
	}); err != nil {
		t.Fatal(err)
	}
	detail, err := s.StaffReviewDetails(mobile.WithOperator(ctx, "moderator-fixture"), row.ID)
	if err != nil || len(detail.MediaIDs) != 0 || detail.Version != row.Version {
		t.Fatal(detail, err)
	}
	if err = repo.View(ctx, func(tx mobile.Tx) error {
		stored, e := tx.Get(mobile.Reviews, row.ID)
		if e == nil && len(stored.MediaIDs) != 1 {
			t.Fatal("read altered stored references")
		}
		return e
	}); err != nil {
		t.Fatal(err)
	}
}

func TestOperatorGuardianRereadIncludesTerminalStateWithoutInviteSecret(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	var row mobile.Record
	if err := repo.Update(ctx, func(tx mobile.Tx) error {
		row = mobile.Record{OwnerID: 2, Key: "guardian-invite:fixture", State: "guardian_relation_verified"}
		if e := mobile.SetBody(&row, mobile.GuardianInviteBody{Nonce: "never-expose-this", ClaimedBy: 1, PolicyVersion: "fixture-policy", GuardianRecordID: 81, ExpiresAt: time.Now().Add(time.Hour)}); e != nil {
			return e
		}
		return tx.Save(mobile.AuthChallenges, &row)
	}); err != nil {
		t.Fatal(err)
	}
	if _, err := s.StaffGuardianInviteDetails(ctx, row.ID); !errors.Is(err, mobile.ErrForbidden) {
		t.Fatal(err)
	}
	detail, err := s.StaffGuardianInviteDetails(mobile.WithOperator(ctx, "verifier-fixture"), row.ID)
	if err != nil || detail.State != "guardian_relation_verified" || detail.GuardianRecordID != 81 || detail.ChildID != 2 {
		t.Fatal(detail, err)
	}
	raw, err := json.Marshal(detail)
	if err != nil || strings.Contains(string(raw), "nonce") || strings.Contains(string(raw), "never-expose") {
		t.Fatal(string(raw), err)
	}
}
