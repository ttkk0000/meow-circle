package mobile

import (
	"context"
	"fmt"
)

type ShareCard struct {
	Kind          Kind   `json:"kind"`
	ID            int64  `json:"id"`
	Title         string `json:"title"`
	Summary       string `json:"summary"`
	ImageURL      string `json:"image_url"`
	ContentOrigin string `json:"content_origin"`
	RelativeURL   string `json:"relative_url"`
}

func (s *Service) PublicShare(ctx context.Context, kind Kind, id int64) (out ShareCard, err error) {
	if !HasKind([]Kind{Entries, Pets, Cases, Offers}, kind) {
		return out, ErrNotFound
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		r, e := tx.Get(kind, id)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, kind, r, 0)
		if e != nil {
			return e
		}
		if !ok {
			return ErrNotFound
		}
		out = ShareCard{Kind: kind, ID: id, RelativeURL: fmt.Sprintf("/api/v2/share/%s/%d", kind, id)}
		switch kind {
		case Entries:
			v, e := Decode[Entry](r)
			if e != nil {
				return e
			}
			out.Title, out.Summary, out.ContentOrigin = v.Title, v.Text, v.ContentOrigin
		case Pets:
			v, e := Decode[Pet](r)
			if e != nil {
				return e
			}
			out.Title, out.Summary = v.Name, v.Bio
		case Cases:
			v, e := Decode[Case](r)
			if e != nil {
				return e
			}
			out.Title, out.Summary = v.Title, v.Description
		case Offers:
			v, e := Decode[Offer](r)
			if e != nil {
				return e
			}
			out.Title = "M&D · 宠物信息"
			out.Summary = v.Nature + " · " + v.City + " · " + v.Area
		}
		if out.Title == "" {
			out.Title = "M&D · meow & doggie"
		}
		summary := []rune(out.Summary)
		if len(summary) > 200 {
			out.Summary = string(summary[:200])
		}
		if len(r.MediaIDs) > 0 {
			m, e := tx.Get(Media, r.MediaIDs[0])
			if e == nil {
				ok, e := s.mediaAccess(tx, m, 0)
				if e != nil {
					return e
				}
				if ok {
					out.ImageURL = fmt.Sprintf("/api/v2/media/%d/content", m.ID)
				}
			} else if e != ErrNotFound {
				return e
			}
		}
		return nil
	})
	return
}
