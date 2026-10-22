package identity

import (
	"context"
	"errors"
	"fmt"
	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
	"time"
)

// Legacy JWTs are accepted only behind the explicit development compatibility
// switch in the transport. Account revocation still applies to that switch.
func (s *Service) LegacyUser(ctx context.Context, id int64, issued time.Time) (u domain.User, err error) {
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if e := active(tx, id); e != nil {
			return e
		}
		r, e := tx.ByKey(mobile.Accounts, fmt.Sprint(id))
		if e == nil {
			a, e := mobile.Decode[Account](r)
			if e != nil {
				return e
			}
			if !issued.After(a.RevokeBefore) {
				return mobile.ErrUnauthorized
			}
		} else if !errors.Is(e, mobile.ErrNotFound) {
			return e
		}
		u, e = tx.User(id)
		return e
	})
	return
}
