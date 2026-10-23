package mobile

import (
	"context"
	"fmt"
	"time"
)

type ReviewInput struct {
	CommandMeta
	Check      string  `json:"check"`
	Type       string  `json:"type"`
	TargetKind Kind    `json:"target_kind"`
	TargetID   int64   `json:"target_id"`
	Reason     string  `json:"reason"`
	AppealOf   int64   `json:"appeal_of"`
	MediaIDs   []int64 `json:"media_ids"`
}

func (s *Service) SubmitReview(ctx context.Context, actor int64, in ReviewInput) (out Record, err error) {
	if in.Type == "verification" && !HasString([]string{"account", "publication_authority", "pet_recent", "location", "medical_document", "medical_source"}, in.Check) {
		return out, Invalid("choose a specific verification check")
	}
	if err = textValid(in.Reason, 3000, true); err != nil {
		return
	}
	if !HasString([]string{"report", "verification", "appeal", "recovery", "support"}, in.Type) {
		return out, Invalid("invalid review type")
	}
	err = s.command(ctx, actor, "review.submit", 0, in, Reviews, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if e := s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		if in.Type == "verification" {
			for _, id := range in.MediaIDs {
				r, e := tx.Get(Media, id)
				if e != nil {
					return e
				}
				file, e := Decode[MediaFile](r)
				if e != nil {
					return e
				}
				if file.KnownAI {
					return Denied("generated_media_is_not_factual_evidence")
				}
			}
		}
		targetVersion := int64(0)
		if in.Type == "appeal" {
			prior, e := tx.Get(Reviews, in.AppealOf)
			if e != nil {
				return e
			}
			if prior.OwnerID != actor || !HasString([]string{"rejected", "approved", "expired"}, prior.State) {
				return ErrForbidden
			}
		} else if in.TargetID > 0 {
			if in.TargetKind == Kind("users") {
				if in.Type != "report" {
					return Invalid("user targets only support the typed report path")
				}
				if _, e := tx.User(in.TargetID); e != nil {
					return e
				}
			} else {
				if !queryKind(in.TargetKind) || in.TargetKind == Reviews {
					return Invalid("invalid review target")
				}
				p, e := tx.Get(in.TargetKind, in.TargetID)
				if e != nil {
					return e
				}
				ok, e := s.canRead(tx, in.TargetKind, p, actor)
				if e != nil {
					return e
				}
				if !ok {
					return ErrForbidden
				}
				if in.Type == "verification" && p.OwnerID != actor {
					return ErrForbidden
				}
				targetVersion = p.Version
			}
		}
		until := s.Now().Add(30 * 24 * time.Hour)
		r := Record{OwnerID: actor, RefID: in.TargetID, State: "submitted", MediaIDs: in.MediaIDs}
		if e := save(tx, Reviews, &r, Review{Check: in.Check, TargetVersion: targetVersion, Type: in.Type, TargetKind: in.TargetKind, TargetID: in.TargetID, Reason: in.Reason, AppealOf: in.AppealOf, EvidenceExpiresAt: &until}, actor, "review.submitted"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type ReviewDecision struct {
	ValidUntil   *time.Time `json:"valid_until,omitempty"`
	Version      int64      `json:"version"`
	State        string     `json:"state"`
	Summary      string     `json:"summary"`
	InternalNote string     `json:"internal_note"`
}

// DecideReview is an operations boundary. HTTP must require the separate staff
// credential; consumer bearer tokens must never grant this method.
func (s *Service) DecideReview(ctx context.Context, id int64, in ReviewDecision) (out Record, err error) {
	if OperatorID(ctx) == "" {
		return out, ErrForbidden
	}
	if !HasString([]string{"in_review", "needs_info", "approved", "rejected", "expired"}, in.State) {
		return out, Invalid("invalid decision")
	}
	if err = textValid(in.Summary, 1000, true); err != nil {
		return
	}
	if err = textValid(in.InternalNote, 5000, false); err != nil {
		return
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Reviews, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		if !HasString([]string{"submitted", "in_review", "needs_info"}, r.State) {
			return ErrConflict
		}
		v, e := Decode[Review](r)
		if e != nil {
			return e
		}
		if v.EvidenceExpiresAt != nil && !v.EvidenceExpiresAt.After(s.Now()) && in.State != "expired" {
			return ErrConflict
		}
		if v.Type == "verification" && in.State == "approved" {
			if in.ValidUntil == nil || !in.ValidUntil.After(s.Now()) || in.ValidUntil.After(s.Now().Add(90*24*time.Hour)) {
				return Invalid("verification requires a validity of up to 90 days")
			}
			v.ValidUntil = in.ValidUntil
			now := s.Now()
			v.ReviewedAt = &now
		}
		r.State = in.State
		v.Summary = in.Summary
		v.InternalNote = in.InternalNote
		if in.State == "expired" {
			r.MediaIDs = nil
		}
		if e = save(tx, Reviews, &r, v, 0, "review.decided"); e != nil {
			return e
		}
		out = r
		if e = s.notify(tx, r.OwnerID, 0, "applications", Reviews, r.ID, "申请审核进度已更新"); e != nil {
			return e
		}
		return audit(tx, OperatorID(ctx), "review.decided", Reviews, r.ID)
	})
	return
}
func (s *Service) SupplementReview(ctx context.Context, actor, id int64, in StateInput, media []int64) (out Record, err error) {
	if err = textValid(in.Note, 3000, true); err != nil {
		return
	}
	err = s.command(ctx, actor, "review.supplement", id, struct {
		StateInput
		MediaIDs []int64 `json:"media_ids"`
	}{in, media}, Reviews, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, e := writable(tx, Reviews, id, actor, in.Version)
		if e != nil {
			return e
		}
		if r.State != "needs_info" {
			return ErrConflict
		}
		if e = s.attachments(tx, actor, media); e != nil {
			return e
		}
		v, e := Decode[Review](r)
		if e != nil {
			return e
		}
		v.Reason = in.Note
		expiry := s.Now().Add(30 * 24 * time.Hour)
		v.EvidenceExpiresAt = &expiry
		r.MediaIDs = media
		r.State = "submitted"
		if e = save(tx, Reviews, &r, v, actor, "review.supplemented"); e != nil {
			return e
		}
		out, e = project(Reviews, r, actor)
		return e
	})
	return
}

func (s *Service) StaffReviews(ctx context.Context, after int64) (out []Record, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error { var e error; out, e = tx.List(Reviews, Filter{After: after, Limit: 100}); return e })
	return
}

// Resolution ends a disputed match; it never declares a new legal owner or
// completes a handover. A fresh match still needs both participants to confirm.
func (s *Service) ResolveClaimDispute(ctx context.Context, id int64, in StateInput) (out Record, err error) {
	if OperatorID(ctx) == "" {
		return out, ErrForbidden
	}
	if err = textValid(in.Note, 1000, true); err != nil {
		return
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Claims, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		if r.State != "disputed" {
			return ErrConflict
		}
		p, e := tx.Get(Cases, r.RefID)
		if e != nil {
			return e
		}
		c, e := Decode[Case](p)
		if e != nil {
			return e
		}
		if p.State != "disputed" || c.MatchedClaimID != r.ID {
			return ErrConflict
		}
		c.MatchedClaimID = 0
		p.State = "active"
		claim, e := Decode[Claim](r)
		if e != nil {
			return e
		}
		claim.Note = in.Note
		claim.OwnerConfirmed = false
		claim.ClaimantConfirmed = false
		r.State = "rejected"
		if e = save(tx, Claims, &r, claim, 0, "claim.dispute_resolved"); e != nil {
			return e
		}
		if e = save(tx, Cases, &p, c, 0, "case.reopened"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

func (s *Service) mediaAccess(tx Tx, r Record, actor int64) (bool, error) {
	if r.State != "ready" {
		return false, nil
	}
	if actor > 0 && r.OwnerID == actor {
		return true, nil
	}
	allowed, e := s.publicProcessingAllowed(tx, r.OwnerID)
	if e != nil || !allowed {
		return false, e
	}
	file, e := Decode[MediaFile](r)
	if e != nil {
		return false, e
	}
	if file.Scope == "public" {
		u, e := tx.User(r.OwnerID)
		if e != nil {
			return false, e
		}
		if u.AvatarURL == fmt.Sprintf("/api/v2/media/%d/content", r.ID) {
			return true, nil
		}
	}
	for _, k := range []Kind{Pets, Offers, Entries, Cases, Claims, Leads, CareUpdates, Messages, GroupMessages, Reviews} {
		f := Filter{MediaID: r.ID, Limit: 100}
		for {
			rows, e := tx.List(k, f)
			if e != nil {
				return false, e
			}
			for _, p := range rows {
				if k == Reviews {
					v, e := Decode[Review](p)
					if e != nil {
						return false, e
					}
					if v.EvidenceExpiresAt != nil && !v.EvidenceExpiresAt.After(s.Now()) {
						continue
					}
				}
				ok, e := s.canRead(tx, k, p, actor)
				if e != nil {
					return false, e
				}
				if ok {
					return true, nil
				}
			}
			if len(rows) < 100 {
				break
			}
			f.After = rows[len(rows)-1].ID
		}
	}
	return false, nil
}
func (s *Service) RegisterMedia(ctx context.Context, actor int64, key string, file MediaFile) (out Record, err error) {
	if len(key) < 8 || len(key) > 100 {
		return out, Invalid("idempotency key required")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		recordKey := fmt.Sprintf("%d:%s", actor, key)
		r, e := tx.ByKey(Media, recordKey)
		if e == nil {
			v, e := Decode[MediaFile](r)
			if e != nil {
				return e
			}
			if v.SHA256 != file.SHA256 || v.Scope != file.Scope {
				return ErrConflict
			}
			if r.State != "ready" {
				return ErrConflict
			}
			out = r
			return nil
		}
		if e != ErrNotFound {
			return e
		}
		r = Record{OwnerID: actor, Key: recordKey, State: "ready"}
		if e = save(tx, Media, &r, file, actor, "media.stored"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
func (s *Service) MediaFile(ctx context.Context, actor, id int64) (file MediaFile, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		if actor > 0 {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		}
		r, e := tx.Get(Media, id)
		if e != nil {
			return e
		}
		ok, e := s.mediaAccess(tx, r, actor)
		if e != nil {
			return e
		}
		if !ok {
			return ErrNotFound
		}
		file, e = Decode[MediaFile](r)
		return e
	})
	return
}
