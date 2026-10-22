package identity

import (
	"context"
	"errors"
	"kitty-circle/internal/mobile"
	"time"
)

// Shared fixed-window budgets survive restarts and apply across replicas.
// The transport also caps bursts by client IP before expensive verification.
func (s *Service) budget(ctx context.Context, key string) error {
	return s.Repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.AuthChallenges, "budget:"+digest(key))
		v := OTP{Window: s.Now()}
		if errors.Is(e, mobile.ErrNotFound) {
			r = mobile.Record{Key: "budget:" + digest(key), State: "budget"}
		} else if e != nil {
			return e
		} else {
			v, e = mobile.Decode[OTP](r)
			if e != nil {
				return e
			}
		}
		if s.Now().Sub(v.Window) >= 10*time.Minute {
			v.Window = s.Now()
			v.Count = 0
		}
		if v.Count >= 10 {
			return mobile.ErrRateLimited
		}
		v.Count++
		return set(tx, mobile.AuthChallenges, &r, v)
	})
}
