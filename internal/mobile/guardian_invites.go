package mobile

import (
	"context"
	"strings"
	"time"
)

// 只在私有auth_challenges保存。领取邀请不证明监护关系，不产生授权范围。
type GuardianInviteBody struct {
	Nonce            string    `json:"nonce"`
	ClaimedBy        int64     `json:"claimed_by"`
	PolicyVersion    string    `json:"policy_version"`
	GuardianRecordID int64     `json:"guardian_record_id"`
	ExpiresAt        time.Time `json:"expires_at"`
}
type StaffGuardianInvite struct {
	ID            int64     `json:"id"`
	Version       int64     `json:"version"`
	ChildID       int64     `json:"child_id"`
	GuardianID    int64     `json:"guardian_id"`
	PolicyVersion string    `json:"policy_version"`
	ExpiresAt     time.Time `json:"expires_at"`
}
type StaffGuardianInvitePage struct {
	Items      []StaffGuardianInvite `json:"items"`
	NextCursor int64                 `json:"next_cursor"`
}

// 身份模块使用与业务命令相同的事务内资格判断，不能复制一份本地成年开关。
func (s *Service) RequireAdult(tx Tx, actor int64) error { return s.adult(tx, actor) }
func (s *Service) GuardianInviteQueue(ctx context.Context, operator string, after int64) (out StaffGuardianInvitePage, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	out.Items = []StaffGuardianInvite{}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		rows, err := tx.List(AuthChallenges, Filter{After: after, Limit: 100, KeyPrefix: "guardian-invite:", State: "guardian_relation_pending"})
		if err != nil {
			return err
		}
		for _, r := range rows {
			v, err := Decode[GuardianInviteBody](r)
			if err != nil {
				return err
			}
			if !v.ExpiresAt.After(s.Now()) {
				continue
			}
			out.Items = append(out.Items, StaffGuardianInvite{r.ID, r.Version, r.OwnerID, v.ClaimedBy, v.PolicyVersion, v.ExpiresAt})
			if err = audit(tx, operator, "guardian.invite_read", AuthChallenges, r.ID); err != nil {
				return err
			}
		}
		if len(rows) == 100 {
			out.NextCursor = rows[len(rows)-1].ID
		}
		return nil
	})
	return
}
func pendingGuardianInvite(tx Tx, in GuardianEvidenceInput, now time.Time) (Record, GuardianInviteBody, error) {
	r, err := tx.Get(AuthChallenges, in.InviteID)
	if err != nil {
		return r, GuardianInviteBody{}, err
	}
	if !strings.HasPrefix(r.Key, "guardian-invite:") || r.State != "guardian_relation_pending" || r.OwnerID != in.ChildID {
		return r, GuardianInviteBody{}, ErrForbidden
	}
	if err = expected(r, in.InviteVersion); err != nil {
		return r, GuardianInviteBody{}, err
	}
	v, err := Decode[GuardianInviteBody](r)
	if err != nil {
		return r, v, err
	}
	if v.ClaimedBy != in.GuardianID || v.PolicyVersion != in.PolicyVersion || !v.ExpiresAt.After(now) {
		return r, v, ErrConflict
	}
	return r, v, nil
}
