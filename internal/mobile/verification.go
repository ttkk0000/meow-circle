package mobile

import (
	"context"
	"time"
)

type VerificationSummary struct {
	ID            int64      `json:"id"`
	Check         string     `json:"check"`
	State         string     `json:"state"`
	Summary       string     `json:"summary"`
	TargetVersion int64      `json:"target_version"`
	ReviewedAt    *time.Time `json:"reviewed_at,omitempty"`
	ValidUntil    *time.Time `json:"valid_until,omitempty"`
}

func (s *Service) Verifications(ctx context.Context, actor int64, k Kind, id int64) (out []VerificationSummary, err error) {
	out = []VerificationSummary{}
	if k != Offers && k != Pets {
		return out, ErrForbidden
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		p, e := tx.Get(k, id)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, k, p, actor)
		if e != nil {
			return e
		}
		if !ok {
			return ErrNotFound
		}
		reviews, e := allRecords(tx, Reviews, Filter{RefID: id})
		if e != nil {
			return e
		}
		for _, r := range reviews {
			v, e := Decode[Review](r)
			if e != nil {
				return e
			}
			if v.Type != "verification" || v.TargetKind != k || v.TargetVersion != p.Version {
				continue
			}
			state := r.State
			if state == "approved" && (v.ValidUntil == nil || !v.ValidUntil.After(s.Now())) {
				state = "expired"
			}
			out = append(out, VerificationSummary{r.ID, v.Check, state, v.Summary, v.TargetVersion, v.ReviewedAt, v.ValidUntil})
		}
		return nil
	})
	return
}
func (s *Service) StaffMediaFile(ctx context.Context, id int64) (file MediaFile, err error) {
	if OperatorID(ctx) == "" {
		return file, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Media, id)
		if e != nil {
			return e
		}
		if r.State != "ready" {
			return ErrForbidden
		}
		reviews, e := allRecords(tx, Reviews, Filter{MediaID: id})
		if e != nil {
			return e
		}
		for _, review := range reviews {
			v, e := Decode[Review](review)
			if e != nil {
				return e
			}
			if v.EvidenceExpiresAt != nil && v.EvidenceExpiresAt.After(s.Now()) {
				file, e = Decode[MediaFile](r)
				if e != nil {
					return e
				}
				return audit(tx, OperatorID(ctx), "evidence.read", Media, id)
			}
		}
		cases, e := allRecords(tx, RightsCases, Filter{MediaID: id})
		if e != nil {
			return e
		}
		for _, c := range cases {
			body, e := Decode[struct {
				Case RightsCase `json:"case"`
			}](c)
			if e != nil {
				return e
			}
			if body.Case.EvidenceUntil.After(s.Now()) {
				file, e = Decode[MediaFile](r)
				if e != nil {
					return e
				}
				return audit(tx, OperatorID(ctx), "rights_evidence.read", Media, id)
			}
		}
		return ErrForbidden
	})
	return
}
