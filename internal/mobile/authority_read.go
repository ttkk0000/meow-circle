package mobile

import (
	"context"
	"errors"
	"fmt"
	"kitty-circle/internal/region"
)

type AuthorityLookup struct {
	ActorID int64  `json:"actor_id"`
	PetID   int64  `json:"pet_id"`
	Scope   string `json:"scope"`
	City    string `json:"city"`
	Species string `json:"species"`
}
type AuthoritySnapshot struct {
	Exists bool    `json:"exists"`
	Record *Record `json:"record"`
}

// 按已选自然键回读当前版本；不提供全用户或全授权目录。
func (s *Service) AuthoritySnapshot(ctx context.Context, operator string, in AuthorityLookup) (out AuthoritySnapshot, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	if !HasString([]string{"care", "disposition", "city_rule"}, in.Scope) || !HasString([]string{"cat", "dog"}, in.Species) {
		return out, Invalid("invalid authority lookup")
	}
	key := fmt.Sprintf("pet:%d:%d:%s", in.ActorID, in.PetID, in.Scope)
	if in.Scope == "city_rule" {
		city, known := region.Lookup(in.City)
		if !known {
			return out, Invalid("known city required")
		}
		key = "city:" + city.Value + ":" + in.Species
	} else if in.ActorID <= 0 || in.PetID <= 0 {
		return out, Invalid("actor and pet required")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.ByKey(Authorities, key)
		if errors.Is(e, ErrNotFound) {
			return nil
		}
		if e != nil {
			return e
		}
		out.Exists, out.Record = true, &r
		return audit(tx, operator, "authority.read", Authorities, r.ID)
	})
	return
}
