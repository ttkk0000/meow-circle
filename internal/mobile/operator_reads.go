package mobile

import (
	"context"
	"strings"
	"time"
)

// 队列只提供定位和筛选所需的摘要，原陈述和证据仅在具名详情访问中读取。
type StaffReviewItem struct {
	ID        int64     `json:"id"`
	Version   int64     `json:"version"`
	State     string    `json:"state"`
	Type      string    `json:"type"`
	CreatedAt time.Time `json:"created_at"`
}
type StaffReviewPage struct {
	Items      []StaffReviewItem `json:"items"`
	NextCursor int64             `json:"next_cursor"`
}

func (s *Service) StaffReviewQueue(ctx context.Context, after int64) (out StaffReviewPage, err error) {
	if OperatorID(ctx) == "" {
		return out, ErrForbidden
	}
	if after < 0 {
		return out, Invalid("invalid review cursor")
	}
	out.Items = []StaffReviewItem{}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		rows, e := tx.List(Reviews, Filter{After: after, Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			v, e := Decode[Review](r)
			if e != nil {
				return e
			}
			out.Items = append(out.Items, StaffReviewItem{r.ID, r.Version, r.State, v.Type, r.CreatedAt})
		}
		if len(rows) == 100 {
			out.NextCursor = rows[len(rows)-1].ID
		}
		return audit(tx, OperatorID(ctx), "review.queue_read", Reviews, 0)
	})
	return
}

func (s *Service) StaffReviewDetails(ctx context.Context, id int64) (out Record, err error) {
	if OperatorID(ctx) == "" {
		return out, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Reviews, id)
		if e != nil {
			return e
		}
		v, e := Decode[Review](r)
		if e != nil {
			return e
		}
		if v.EvidenceExpiresAt != nil && !v.EvidenceExpiresAt.After(s.Now()) {
			r.MediaIDs = nil
		}
		out = r
		return audit(tx, OperatorID(ctx), "review.detail_read", Reviews, id)
	})
	return
}

type StaffGuardianInviteDetails struct {
	ID               int64     `json:"id"`
	Version          int64     `json:"version"`
	ChildID          int64     `json:"child_id"`
	GuardianID       int64     `json:"guardian_id"`
	PolicyVersion    string    `json:"policy_version"`
	ExpiresAt        time.Time `json:"expires_at"`
	State            string    `json:"state"`
	GuardianRecordID int64     `json:"guardian_record_id"`
}

// 可重新读取已核实或过期的邀请，用于核对未知写结果；不返回邀请nonce或原始身份材料。
func (s *Service) StaffGuardianInviteDetails(ctx context.Context, id int64) (out StaffGuardianInviteDetails, err error) {
	if OperatorID(ctx) == "" {
		return out, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(AuthChallenges, id)
		if e != nil {
			return e
		}
		if !strings.HasPrefix(r.Key, "guardian-invite:") {
			return ErrNotFound
		}
		v, e := Decode[GuardianInviteBody](r)
		if e != nil {
			return e
		}
		out = StaffGuardianInviteDetails{r.ID, r.Version, r.OwnerID, v.ClaimedBy, v.PolicyVersion, v.ExpiresAt, r.State, v.GuardianRecordID}
		return audit(tx, OperatorID(ctx), "guardian.invite_detail_read", AuthChallenges, id)
	})
	return
}
