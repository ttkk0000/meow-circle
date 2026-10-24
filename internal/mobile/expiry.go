package mobile

import (
	"context"
	"strings"
)

func (s *Service) PurgeExpiredSafety(ctx context.Context, operator string, after int64) (next int64, count int, err error) {
	if operator == "" {
		return 0, 0, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		rows, e := tx.List(Media, Filter{After: after, Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			next = r.ID
			if !strings.HasPrefix(r.Key, "safety:") || r.State != "ready" {
				continue
			}
			session, e := tx.Get(AuthChallenges, r.RefID)
			if e != nil && e != ErrNotFound {
				return e
			}
			active := false
			if e == nil {
				v, e := Decode[SafetySession](session)
				if e != nil {
					return e
				}
				active = session.State == "safety_session" && v.ExpiresAt.After(s.Now())
			}
			if active {
				continue
			}
			retained, _, e := mediaRetention(tx, r, s.Now())
			if e != nil {
				return e
			}
			if retained {
				continue
			}
			r.State = "erasure_pending"
			if e = tx.Save(Media, &r); e != nil {
				return e
			}
			count++
		}
		return audit(tx, operator, "safety.expiry_processed", Media, after)
	})
	return
}
