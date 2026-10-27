package mobile

import (
	"context"
	"fmt"
	"kitty-circle/internal/region"
	"time"
)

type AuthorityInput struct {
	Version     int64     `json:"version"`
	ActorID     int64     `json:"actor_id"`
	PetID       int64     `json:"pet_id"`
	Scope       string    `json:"scope"`
	City        string    `json:"city"`
	Species     string    `json:"species"`
	RuleVersion string    `json:"rule_version"`
	Actions     []string  `json:"actions"`
	EvidenceRef string    `json:"evidence_ref"`
	ValidUntil  time.Time `json:"valid_until"`
}

// Configured local rules and disposition evidence are separate records. Care
// and a found-animal post never confer the right to transfer an animal.
func (s *Service) RecordAuthority(ctx context.Context, operator string, in AuthorityInput) (out Record, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	if !HasString([]string{"care", "disposition", "city_rule"}, in.Scope) || !in.ValidUntil.After(s.Now()) || textValid(in.EvidenceRef, 120, true) != nil {
		return out, Invalid("invalid authority evidence")
	}
	if in.Species != "cat" && in.Species != "dog" {
		return out, Invalid("invalid species")
	}
	key := fmt.Sprintf("pet:%d:%d:%s", in.ActorID, in.PetID, in.Scope)
	if in.Scope == "city_rule" {
		city, known := region.Lookup(in.City)
		if !known || in.RuleVersion == "" || len(in.Actions) == 0 {
			return out, Invalid("city rule and permitted actions required")
		}
		in.City = city.Value
		for _, a := range in.Actions {
			if !HasString([]string{"adoption", "commercial", "offline"}, a) {
				return out, Invalid("invalid city action")
			}
		}
		key = "city:" + in.City + ":" + in.Species
	} else if in.ActorID <= 0 || in.PetID <= 0 {
		return out, Invalid("actor and pet required")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.ByKey(Authorities, key)
		if e == ErrNotFound {
			if in.Version != 0 {
				return ErrConflict
			}
			r = Record{Key: key, State: "active"}
		} else if e != nil {
			return e
		} else if e = expected(r, in.Version); e != nil {
			return e
		}
		if in.Scope != "city_rule" {
			if e = s.active(tx, in.ActorID); e != nil {
				return e
			}
			if _, e = tx.Get(Pets, in.PetID); e != nil {
				return e
			}
			r.OwnerID = in.ActorID
			r.RefID = in.PetID
		}
		if e = save(tx, Authorities, &r, in, in.ActorID, "authority.updated"); e != nil {
			return e
		}
		out = r
		return audit(tx, operator, "authority.updated", Authorities, r.ID)
	})
	return
}
func (s *Service) disposition(tx Tx, actor, petID int64) error {
	r, e := tx.ByKey(Authorities, fmt.Sprintf("pet:%d:%d:disposition", actor, petID))
	if e == ErrNotFound {
		return Denied("disposition_authority_required")
	}
	if e != nil {
		return e
	}
	v, e := Decode[AuthorityInput](r)
	if e != nil {
		return e
	}
	if r.State != "active" || !v.ValidUntil.After(s.Now()) {
		return Denied("disposition_authority_required")
	}
	return nil
}
func (s *Service) cityAdmission(tx Tx, city, species, action string) error {
	r, e := tx.ByKey(Authorities, "city:"+region.Normalize(city)+":"+species)
	if e == ErrNotFound {
		return Denied("city_rule_unavailable")
	}
	if e != nil {
		return e
	}
	v, e := Decode[AuthorityInput](r)
	if e != nil {
		return e
	}
	if r.State != "active" || !v.ValidUntil.After(s.Now()) || !HasString(v.Actions, action) {
		return Denied("city_action_unavailable")
	}
	return nil
}
