package identity

import (
	"context"

	"kitty-circle/internal/mobile"
)

// HTTP authentication precedes repository locking. Recheck the same session
// inside every authenticated mutation, including both OAuth callback stages.
func (s *Service) actorUpdate(ctx context.Context, actor int64, fn func(mobile.Tx) error) error {
	return s.Repo.Update(ctx, func(tx mobile.Tx) error {
		if key := mobile.SessionKey(ctx); actor > 0 && key != "" {
			if e := mobile.CheckSession(tx, actor, key, s.Now()); e != nil {
				return e
			}
		}
		return fn(tx)
	})
}
