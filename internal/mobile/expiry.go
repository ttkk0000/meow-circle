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
			cases, e := allRecords(tx, RightsCases, Filter{MediaID: r.ID})
			if e != nil {
				return e
			}
			retained := false
			for _, c := range cases {
				v, e := Decode[struct {
					Case RightsCase `json:"case"`
				}](c)
				if e != nil {
					return e
				}
				if v.Case.EvidenceUntil.After(s.Now()) {
					retained = true
				}
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
