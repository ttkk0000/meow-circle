package mobile

import (
	"context"
	"strings"
)

func (s *Service) ValidateSafetySession(ctx context.Context, token string) error {
	return s.Repo.View(ctx, func(tx Tx) error { _, _, e := safetySession(tx, token, s.Now()); return e })
}
func (s *Service) SafetyMediaFile(ctx context.Context, token string, id int64) (out MediaFile, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		if _, _, e := safetySession(tx, token, s.Now()); e != nil {
			return e
		}
		r, e := tx.Get(Media, id)
		if e != nil {
			return e
		}
		if r.State != "ready" || !strings.HasPrefix(r.Key, "safety:"+Fingerprint(token)+":") {
			return ErrForbidden
		}
		out, e = Decode[MediaFile](r)
		return e
	})
	return
}
