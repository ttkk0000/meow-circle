package identity

import (
	"context"
	"kitty-circle/internal/mobile"
)

func (s *Service) LinkScope(ctx context.Context, actor int64, token string) (out mobile.PurposeScope, err error) {
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		r, e := tx.ByKey(mobile.AuthChallenges, digest(token))
		if e != nil {
			return mobile.ErrUnauthorized
		}
		v, e := mobile.Decode[Challenge](r)
		if e != nil {
			return e
		}
		if r.State != "verified" || v.Subject == nil || !v.ExpiresAt.After(s.Now()) || r.OwnerID > 0 && r.OwnerID != actor {
			return mobile.ErrForbidden
		}
		out, e = mobile.ScopeFor("identity.link", string(mobile.AuthChallenges), r.ID, r.Version, "owner", subjectKey(*v.Subject))
		return e
	})
	return
}
