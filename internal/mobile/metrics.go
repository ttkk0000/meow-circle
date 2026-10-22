package mobile

import (
	"context"
	"errors"
	"fmt"
	"kitty-circle/internal/domain"
)

type Metrics struct {
	EntryID     int64 `json:"entry_id"`
	Likes       int64 `json:"likes"`
	Comments    int64 `json:"comments"`
	ViewerLiked bool  `json:"viewer_liked"`
}

func (s *Service) Metrics(ctx context.Context, actor int64, ids []int64) (out []Metrics, err error) {
	out = []Metrics{}
	if len(ids) > 50 || len(Unique(ids)) != len(ids) {
		return out, Invalid("use up to 50 distinct entries")
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if actor > 0 {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		}
		for _, id := range ids {
			r, e := tx.Get(Entries, id)
			if errors.Is(e, ErrNotFound) {
				continue
			}
			if e != nil {
				return e
			}
			ok, e := s.canRead(tx, Entries, r, actor)
			if e != nil {
				return e
			}
			if !ok || r.State != "published" {
				continue
			}
			m := Metrics{EntryID: id}
			m.Likes, e = tx.Count(Reactions, Filter{RefID: id, State: "active"})
			if e != nil {
				return e
			}
			m.Comments, e = tx.Count(Comments, Filter{RefID: id, State: "published"})
			if e != nil {
				return e
			}
			if actor > 0 {
				reaction, e := tx.ByKey(Reactions, fmt.Sprintf("%d:%d", id, actor))
				if e == nil {
					m.ViewerLiked = reaction.State == "active"
				} else if !errors.Is(e, ErrNotFound) {
					return e
				}
			}
			out = append(out, m)
		}
		return nil
	})
	return
}
func (s *Service) PublicUser(ctx context.Context, actor, id int64) (out domain.User, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		var e error
		out, e = tx.User(id)
		if e != nil {
			return e
		}
		out.Stats = nil
		if actor != id {
			allowed, e := s.publicProcessingAllowed(tx, id)
			if e != nil {
				return e
			}
			if !allowed {
				out.Username = "unavailable"
				out.Nickname = "暂不可见"
				out.Bio = ""
				out.AvatarURL = ""
			}
		}
		return nil
	})
	return
}
