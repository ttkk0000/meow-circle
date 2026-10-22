package identity

import (
	"context"
	"errors"
	"fmt"
	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
	"strings"
	"unicode/utf8"
)

type Profile struct {
	User       domain.User `json:"user"`
	Version    int64       `json:"version"`
	Theme      string      `json:"theme"`
	Background string      `json:"background"`
}

func (s *Service) Capabilities() map[string]any {
	providers := []string{}
	for _, p := range []string{"wechat", "qq", "apple"} {
		if s.Providers[p] != nil {
			providers = append(providers, p)
		}
	}
	return map[string]any{"providers": providers, "sms": s.SMS != nil, "password": true, "email": s.Email != nil, "registration": s.Email != nil, "interaction_contract": 1, "scoped_reauth": true, "apple_platform": "ios"}
}
func (s *Service) Profile(ctx context.Context, actor int64) (out Profile, err error) {
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		u, e := tx.User(actor)
		if e != nil {
			return e
		}
		out = Profile{User: u, Theme: "honey", Background: "picnic"}
		r, e := tx.ByKey(mobile.Accounts, fmt.Sprint(actor))
		if errors.Is(e, mobile.ErrNotFound) {
			return nil
		}
		if e != nil {
			return e
		}
		a, e := mobile.Decode[Account](r)
		if e != nil {
			return e
		}
		out.Version = r.Version
		if a.Theme != "" {
			out.Theme = a.Theme
		}
		if a.Background != "" {
			out.Background = a.Background
		}
		return nil
	})
	return
}

type ProfileInput struct {
	Version       int64  `json:"version"`
	Nickname      string `json:"nickname"`
	Bio           string `json:"bio"`
	AvatarMediaID *int64 `json:"avatar_media_id"`
	Theme         string `json:"theme"`
	Background    string `json:"background"`
}

func (s *Service) SaveProfile(ctx context.Context, actor int64, in ProfileInput) (out Profile, err error) {
	if !utf8.ValidString(in.Nickname) || strings.TrimSpace(in.Nickname) == "" || utf8.RuneCountInString(in.Nickname) > 40 || !utf8.ValidString(in.Bio) || utf8.RuneCountInString(in.Bio) > 500 {
		return out, mobile.Invalid("invalid nickname or biography")
	}
	if !mobile.HasString([]string{"honey", "mint", "night", "neutral"}, in.Theme) || !mobile.HasString([]string{"picnic", "desk", "arcade", "garden"}, in.Background) {
		return out, mobile.Invalid("unknown theme or profile background")
	}
	err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		record, e := tx.ByKey(mobile.Accounts, fmt.Sprint(actor))
		a := Account{}
		if errors.Is(e, mobile.ErrNotFound) {
			if in.Version != 0 {
				return mobile.ErrConflict
			}
			record = mobile.Record{OwnerID: actor, Key: fmt.Sprint(actor), State: "active"}
		} else if e != nil {
			return e
		} else {
			if record.Version != in.Version {
				return mobile.ErrConflict
			}
			a, e = mobile.Decode[Account](record)
			if e != nil {
				return e
			}
		}
		u, e := tx.User(actor)
		if e != nil {
			return e
		}
		if u.Nickname != strings.TrimSpace(in.Nickname) || u.Bio != in.Bio || in.AvatarMediaID != nil {
			if e = mobile.RequireInteraction(tx, actor, s.Now()); e != nil {
				return e
			}
		}
		u.Nickname = strings.TrimSpace(in.Nickname)
		u.Bio = in.Bio
		if in.AvatarMediaID != nil {
			if *in.AvatarMediaID == 0 {
				u.AvatarURL = ""
			} else {
				m, e := tx.Get(mobile.Media, *in.AvatarMediaID)
				if e != nil {
					return e
				}
				file, e := mobile.Decode[mobile.MediaFile](m)
				if e != nil {
					return e
				}
				if m.OwnerID != actor || m.State != "ready" || file.Scope != "public" {
					return mobile.ErrForbidden
				}
				u.AvatarURL = fmt.Sprintf("/api/v2/media/%d/content", m.ID)
			}
		}
		if e = tx.SaveUser(&u); e != nil {
			return e
		}
		a.Theme = in.Theme
		a.Background = in.Background
		if e = set(tx, mobile.Accounts, &record, a); e != nil {
			return e
		}
		out = Profile{u, record.Version, a.Theme, a.Background}
		return nil
	})
	return
}
