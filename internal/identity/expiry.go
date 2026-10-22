package identity

import (
	"context"
	"kitty-circle/internal/mobile"
	"time"
)

// The deployment scheduler calls this bounded command. It is never started by
// documentation or by a client. Failed-attempt rate windows survive one hour.
func (s *Service) PurgeExpiredChallenges(ctx context.Context, after int64) (next int64, count int, err error) {
	if mobile.OperatorID(ctx) == "" {
		return 0, 0, mobile.ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		rows, e := tx.List(mobile.OTPChallenges, mobile.Filter{After: after, Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			next = r.ID
			v, e := mobile.Decode[OTP](r)
			if e != nil {
				return e
			}
			if !v.ExpiresAt.IsZero() && !v.ExpiresAt.After(s.Now()) && !v.Window.Add(time.Hour).After(s.Now()) {
				if e = tx.Delete(mobile.OTPChallenges, r.ID, r.Version); e != nil {
					return e
				}
				count++
			}
		}
		return nil
	})
	return
}
func (s *Service) PurgeExpiredTokens(ctx context.Context, after int64) (next int64, count int, err error) {
	if mobile.OperatorID(ctx) == "" {
		return 0, 0, mobile.ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		rows, e := tx.List(mobile.AuthChallenges, mobile.Filter{After: after, Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			next = r.ID
			v, e := mobile.Decode[struct {
				ExpiresAt time.Time `json:"expires_at"`
			}](r)
			if e != nil {
				return e
			}
			if !v.ExpiresAt.IsZero() && !v.ExpiresAt.After(s.Now()) {
				if e = tx.Delete(mobile.AuthChallenges, r.ID, r.Version); e != nil {
					return e
				}
				count++
			}
		}
		return nil
	})
	return
}
