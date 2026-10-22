package mobile

import (
	"context"
	"errors"
	"fmt"
	"math"
	"time"
)

type CaseInput struct {
	CommandMeta
	Version  int64   `json:"version"`
	Case     Case    `json:"case"`
	MediaIDs []int64 `json:"media_ids"`
}

func coordinates(lat, lon float64) bool {
	return !math.IsNaN(lat) && !math.IsInf(lat, 0) && !math.IsNaN(lon) && !math.IsInf(lon, 0) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
}
func (s *Service) SaveCase(ctx context.Context, actor, id int64, in CaseInput) (out Record, err error) {
	if id == 0 && in.Version != 0 {
		return out, ErrConflict
	}
	c := in.Case
	if c.Type != "lost" && c.Type != "found" {
		return out, Invalid("type must be lost or found")
	}
	if err = textValid(c.Title, 80, true); err != nil {
		return
	}
	if err = textValid(c.Description, 5000, true); err != nil {
		return
	}
	if err = textValid(c.PrivateLocation, 500, false); err != nil {
		return
	}
	if !coordinates(c.Latitude, c.Longitude) || c.City == "" || len(c.City) > 120 || len(c.Area) > 200 || c.OccurredAt.IsZero() || c.OccurredAt.After(s.Now()) {
		return out, Invalid("invalid location or occurrence date")
	}
	if c.Species != "cat" && c.Species != "dog" {
		return out, Invalid("invalid species")
	}
	c.Latitude = math.Round(c.Latitude*100) / 100
	c.Longitude = math.Round(c.Longitude*100) / 100
	c.ClosureReason = ""
	c.MatchedClaimID = 0
	err = s.command(ctx, actor, "case.save", id, in, Cases, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		if e := s.publicAttachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		if c.PetID > 0 {
			p, e := tx.Get(Pets, c.PetID)
			if e != nil {
				return e
			}
			if e = owner(p, actor); e != nil {
				return e
			}
		}
		r := Record{OwnerID: actor, State: "active", Public: true}
		if id > 0 {
			var e error
			r, e = writable(tx, Cases, id, actor, in.Version)
			if e != nil {
				return e
			}
			if r.State != "active" {
				return ErrConflict
			}
			old, e := Decode[Case](r)
			if e != nil {
				return e
			}
			if old.Type != c.Type {
				return Invalid("cannot change lost/found type")
			}
		}
		r.MediaIDs = in.MediaIDs
		if e := save(tx, Cases, &r, c, actor, "case.saved"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type ClaimInput struct {
	CommandMeta
	PetName  string  `json:"pet_name"`
	Evidence string  `json:"evidence"`
	MediaIDs []int64 `json:"media_ids"`
}

func (s *Service) Claim(ctx context.Context, actor, caseID int64, in ClaimInput) (out Record, err error) {
	if err = textValid(in.Evidence, 5000, true); err != nil {
		return
	}
	if err = textValid(in.PetName, 40, true); err != nil {
		return
	}
	err = s.command(ctx, actor, "claim.create", caseID, in, Claims, &out, func(tx Tx) error {
		if e := s.factualAttachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		p, e := tx.Get(Cases, caseID)
		if e != nil {
			return e
		}
		if p.OwnerID == actor || p.State != "active" {
			return ErrConflict
		}
		if e = s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		key := fmt.Sprintf("%d:%d", caseID, actor)
		old, e := tx.ByKey(Claims, key)
		if e == nil {
			out = old
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		r := Record{OwnerID: actor, Participants: []int64{p.OwnerID}, RefID: caseID, MediaIDs: in.MediaIDs, Key: key, State: "submitted"}
		if e = save(tx, Claims, &r, Claim{PetName: in.PetName, Evidence: in.Evidence}, actor, "claim.submitted"); e != nil {
			return e
		}
		out = r
		return s.notify(tx, p.OwnerID, actor, "applications", Claims, r.ID, "收到认领材料，请核验")
	})
	return
}

type StateInput struct {
	CommandMeta
	Version  int64   `json:"version"`
	Note     string  `json:"note"`
	MediaIDs []int64 `json:"media_ids,omitempty"`
}

func (s *Service) ClaimAction(ctx context.Context, actor, id int64, action string, in StateInput) (out Record, err error) {
	if err = textValid(in.Note, 1000, false); err != nil {
		return
	}
	err = s.command(ctx, actor, "claim."+action, id, in, Claims, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if action != "withdraw" && action != "dispute" {
			if e := s.interaction(tx, actor); e != nil {
				return e
			}
		}
		r, e := tx.Get(Claims, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		p, e := tx.Get(Cases, r.RefID)
		if e != nil {
			return e
		}
		if !participant(r, actor) {
			return ErrForbidden
		}
		c, e := Decode[Claim](r)
		if e != nil {
			return e
		}
		notice := true
		switch action {
		case "match":
			if actor != p.OwnerID || p.State != "active" || r.State != "submitted" {
				return ErrConflict
			}
			body, e := Decode[Case](p)
			if e != nil {
				return e
			}
			body.MatchedClaimID = r.ID
			p.State = "matched"
			r.State = "matched"
			if e = save(tx, Cases, &p, body, actor, "case.matched"); e != nil {
				return e
			}
		case "reject":
			if actor != p.OwnerID || (r.State != "submitted" && r.State != "needs_info") {
				return ErrForbidden
			}
			r.State = "rejected"
		case "request_supplement":
			if actor != p.OwnerID || r.State != "submitted" || in.Note == "" {
				return ErrForbidden
			}
			r.State = "needs_info"
		case "supplement", "resubmit":
			if actor != r.OwnerID || p.State != "active" || in.Note == "" {
				return ErrForbidden
			}
			if action == "supplement" && r.State != "needs_info" || action == "resubmit" && !HasString([]string{"rejected", "withdrawn"}, r.State) {
				return ErrConflict
			}
			if e = s.attachments(tx, actor, in.MediaIDs); e != nil {
				return e
			}
			if len(in.MediaIDs) > 0 {
				r.MediaIDs = in.MediaIDs
			}
			if e = textValid(c.Evidence+"\n"+in.Note, 5000, true); e != nil {
				return e
			}
			c.Evidence += "\n" + in.Note
			r.State = "submitted"
		case "withdraw":
			if actor != r.OwnerID || (r.State != "submitted" && r.State != "needs_info") {
				return ErrForbidden
			}
			r.State = "withdrawn"
		case "dispute":
			if r.State != "matched" && r.State != "handover_pending" {
				return ErrConflict
			}
			r.State = "disputed"
			p.State = "disputed"
			if e = save(tx, Cases, &p, mustBody(p), actor, "case.disputed"); e != nil {
				return e
			}
		case "confirm_handover":
			if e = s.adult(tx, actor); e != nil {
				return e
			}
			context, e := Decode[Case](p)
			if e != nil {
				return e
			}
			if e = s.cityAdmission(tx, context.City, context.Species, "offline"); e != nil {
				return e
			}
			if p.State != "matched" || (r.State != "matched" && r.State != "handover_pending") {
				return ErrConflict
			}
			if actor == p.OwnerID {
				c.OwnerConfirmed = true
			} else {
				c.ClaimantConfirmed = true
			}
			r.State = "handover_pending"
			if c.OwnerConfirmed && c.ClaimantConfirmed {
				r.State = "completed"
				p.State = "closed"
				body, e := Decode[Case](p)
				if e != nil {
					return e
				}
				body.ClosureReason = "双方已确认交接"
				p.MediaIDs = nil
				if e = save(tx, Cases, &p, body, actor, "case.closed"); e != nil {
					return e
				}
			}
		default:
			notice = false
			return Invalid("invalid claim action")
		}
		c.Note = in.Note
		if e = save(tx, Claims, &r, c, actor, "claim."+action); e != nil {
			return e
		}
		out = r
		if notice {
			other := p.OwnerID
			if actor == other {
				other = r.OwnerID
			}
			return s.notify(tx, other, actor, "applications", Claims, r.ID, "认领进度已更新")
		}
		return nil
	})
	return
}
func mustBody(r Record) any { return r.Data }
func (s *Service) CloseCase(ctx context.Context, actor, id int64, in StateInput) (out Record, err error) {
	if err = textValid(in.Note, 300, true); err != nil {
		return
	}
	err = s.command(ctx, actor, "case.close", id, in, Cases, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, e := writable(tx, Cases, id, actor, in.Version)
		if e != nil {
			return e
		}
		if r.State != "active" {
			return ErrConflict
		}
		c, e := Decode[Case](r)
		if e != nil {
			return e
		}
		c.ClosureReason = in.Note
		r.State = "closed"
		r.MediaIDs = nil
		if e = save(tx, Cases, &r, c, actor, "case.closed"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
func (s *Service) Lead(ctx context.Context, actor, caseID int64, text string, media []int64) (out Record, err error) {
	if err = textValid(text, 2000, true); err != nil {
		return
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		p, e := tx.Get(Cases, caseID)
		if e != nil {
			return e
		}
		if p.State != "active" && p.State != "matched" {
			return ErrConflict
		}
		if e = s.attachments(tx, actor, media); e != nil {
			return e
		}
		r := Record{OwnerID: actor, Participants: []int64{p.OwnerID}, RefID: caseID, MediaIDs: media, State: "submitted"}
		if e = save(tx, Leads, &r, Lead{text}, actor, "lead.submitted"); e != nil {
			return e
		}
		out = r
		return s.notify(tx, p.OwnerID, actor, "applications", Cases, caseID, "收到一条私密寻宠线索")
	})
	return
}

type SubscriptionInput struct {
	Version      int64        `json:"version"`
	Subscription Subscription `json:"subscription"`
}

func (s *Service) Subscribe(ctx context.Context, actor int64, in SubscriptionInput) (out Record, err error) {
	v := in.Subscription
	if !coordinates(v.Latitude, v.Longitude) || v.RadiusKM < 5 || v.RadiusKM > 100 || len(v.City) > 120 || !HasString([]string{"instant", "daily", "off"}, v.Frequency) {
		return out, Invalid("invalid nearby subscription")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, e := tx.ByKey(Subscriptions, fmt.Sprint(actor))
		if errors.Is(e, ErrNotFound) {
			if in.Version != 0 {
				return ErrConflict
			}
			r = Record{OwnerID: actor, Key: fmt.Sprint(actor)}
		} else if e != nil {
			return e
		} else if e = expected(r, in.Version); e != nil {
			return e
		}
		r.State = "disabled"
		if v.Enabled {
			r.State = "active"
		}
		if e = save(tx, Subscriptions, &r, v, actor, "nearby.subscription_changed"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type AdoptionInput struct {
	CommandMeta
	PetID   int64  `json:"pet_id"`
	Message string `json:"message"`
}

func (s *Service) ApplyAdoption(ctx context.Context, actor int64, in AdoptionInput) (out Record, err error) {
	if err = textValid(in.Message, 2000, true); err != nil {
		return
	}
	err = s.command(ctx, actor, "adoption.apply", 0, in, Adoptions, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		pet, e := tx.Get(Pets, in.PetID)
		if e != nil {
			return e
		}
		p, e := Decode[Pet](pet)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, Pets, pet, actor)
		if e != nil {
			return e
		}
		if !ok || !p.AdoptionAvailable || pet.OwnerID == actor || pet.State != "active" {
			return ErrForbidden
		}
		b, e := blocked(tx, pet.OwnerID, actor)
		if e != nil {
			return e
		}
		if b {
			return ErrForbidden
		}
		key := fmt.Sprintf("%d:%d", pet.ID, actor)
		old, e := tx.ByKey(Adoptions, key)
		if e == nil {
			out = old
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		r := Record{OwnerID: pet.OwnerID, Participants: []int64{actor}, RefID: pet.ID, Key: key, State: "submitted"}
		if e = save(tx, Adoptions, &r, Adoption{PetID: pet.ID, ApplicantID: actor, PublisherID: pet.OwnerID, Message: in.Message, TrialConfirmations: []int64{}, HandoverConfirmations: []int64{}}, actor, "adoption.applied"); e != nil {
			return e
		}
		out = r
		return s.notify(tx, pet.OwnerID, actor, "applications", Adoptions, r.ID, "收到领养申请")
	})
	return
}

type AdoptionAction struct {
	CommandMeta
	Version    int64      `json:"version"`
	Note       string     `json:"note"`
	TrialUntil *time.Time `json:"trial_until,omitempty"`
}

func (s *Service) Adoption(ctx context.Context, actor, id int64, action string, in AdoptionAction) (out Record, err error) {
	if err = textValid(in.Note, 3000, false); err != nil {
		return
	}
	err = s.command(ctx, actor, "adoption."+action, id, in, Adoptions, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if action != "support" && action != "request_return" && action != "withdraw" {
			if e := s.interaction(tx, actor); e != nil {
				return e
			}
		}
		r, e := tx.Get(Adoptions, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		if !participant(r, actor) {
			return ErrForbidden
		}
		a, e := Decode[Adoption](r)
		if e != nil {
			return e
		}
		publisher := actor == a.PublisherID
		if HasString([]string{"propose_trial", "confirm_trial", "confirm_handover"}, action) {
			if e = s.adult(tx, actor); e != nil {
				return e
			}
			var contract *ContractRevision
			for i := range a.Contracts {
				if a.Contracts[i].Status == "pending" {
					return Denied("contract_change_pending")
				}
				if a.Contracts[i].Number == a.EffectiveContract && a.Contracts[i].Status == "effective" {
					contract = &a.Contracts[i]
				}
			}
			if contract == nil || len(contract.AcceptedBy) != 2 {
				return Denied("effective_contract_required")
			}
			if e = s.disposition(tx, a.PublisherID, a.PetID); e != nil {
				return e
			}
			if e = s.cityAdmission(tx, contract.Terms.City, contract.Terms.Species, "offline"); e != nil {
				return e
			}
			if action == "propose_trial" && (in.TrialUntil == nil || contract.Terms.TrialUntil == nil || !in.TrialUntil.Equal(*contract.Terms.TrialUntil) || in.Note != contract.Terms.Responsibilities) {
				return Denied("trial_requires_contract_revision")
			}
		}
		switch action {
		case "request_supplement":
			if !publisher || (r.State != "submitted" && r.State != "supplemented") || in.Note == "" {
				return ErrForbidden
			}
			r.State = "needs_info"
		case "supplement":
			if publisher || r.State != "needs_info" || in.Note == "" {
				return ErrForbidden
			}
			a.Supplement = in.Note
			r.State = "supplemented"
		case "approve", "reject":
			if !publisher || (r.State != "submitted" && r.State != "supplemented") {
				return ErrForbidden
			}
			r.State = "approved"
			if action == "reject" {
				r.State = "rejected"
			}
		case "withdraw":
			if publisher || !HasString([]string{"submitted", "needs_info", "supplemented", "approved", "trial_proposed"}, r.State) {
				return ErrForbidden
			}
			r.State = "withdrawn"
		case "propose_trial":
			if r.State != "approved" && r.State != "trial_proposed" {
				return ErrConflict
			}
			if in.TrialUntil == nil || !in.TrialUntil.After(s.Now()) || in.TrialUntil.After(s.Now().Add(30*24*time.Hour)) || in.Note == "" {
				return Invalid("trial terms and a date within 30 days required")
			}
			a.TrialTerms = in.Note
			a.TrialUntil = in.TrialUntil
			a.TrialConfirmations = []int64{actor}
			r.State = "trial_proposed"
		case "confirm_trial":
			if r.State != "trial_proposed" || a.TrialUntil == nil || !a.TrialUntil.After(s.Now()) {
				return ErrConflict
			}
			a.TrialConfirmations = Unique(append(a.TrialConfirmations, actor))
			if len(a.TrialConfirmations) == 2 {
				r.State = "trial_active"
			}
		case "decline_trial":
			if r.State != "trial_proposed" {
				return ErrConflict
			}
			r.State = "approved"
			a.TrialConfirmations = []int64{}
		case "confirm_handover":
			if !HasString([]string{"approved", "trial_active", "handover_pending"}, r.State) {
				return ErrConflict
			}
			a.HandoverConfirmations = Unique(append(a.HandoverConfirmations, actor))
			r.State = "handover_pending"
			if len(a.HandoverConfirmations) == 2 {
				pet, e := tx.Get(Pets, a.PetID)
				if e != nil {
					return e
				}
				p, e := Decode[Pet](pet)
				if e != nil {
					return e
				}
				if pet.OwnerID != a.PublisherID || !p.AdoptionAvailable {
					return ErrConflict
				}
				now := s.Now()
				a.CompletedAt = &now
				r.State = "completed"
				p.AdoptionAvailable = false
				p.Audience = Audience{Mode: "private"}
				pet.OwnerID = a.ApplicantID
				pet.Public = false
				pet.MediaIDs = nil
				if e = save(tx, Pets, &pet, p, actor, "pet.custody_transferred"); e != nil {
					return e
				}
				offers, e := allRecords(tx, Offers, Filter{RefID: pet.ID})
				if e != nil {
					return e
				}
				for _, offer := range offers {
					offer.State = "closed"
					offer.Public = false
					offer.MediaIDs = nil
					if e = save(tx, Offers, &offer, mustBody(offer), actor, "offer.closed"); e != nil {
						return e
					}
				}
			}
		case "support", "request_return":
			if !HasString([]string{"trial_active", "handover_pending", "completed", "return_requested"}, r.State) || in.Note == "" {
				return ErrConflict
			}
			if action == "request_return" {
				r.State = "return_requested"
			}
			review := Record{OwnerID: actor, State: "submitted", RefID: r.ID}
			if e = save(tx, Reviews, &review, Review{Type: "support", TargetKind: Adoptions, TargetID: r.ID, Reason: in.Note}, actor, "support.requested"); e != nil {
				return e
			}
		default:
			return Invalid("invalid adoption action")
		}
		a.Note = in.Note
		if e = save(tx, Adoptions, &r, a, actor, "adoption."+action); e != nil {
			return e
		}
		out = r
		other := a.PublisherID
		if actor == other {
			other = a.ApplicantID
		}
		return s.notify(tx, other, actor, "applications", Adoptions, r.ID, "领养进度已更新")
	})
	return
}

type CareInput struct {
	CommandMeta
	Text      string  `json:"text"`
	NeedsHelp bool    `json:"needs_help"`
	MediaIDs  []int64 `json:"media_ids"`
}

func (s *Service) Care(ctx context.Context, actor, adoptionID int64, in CareInput) (out Record, err error) {
	if err = textValid(in.Text, 3000, len(in.MediaIDs) == 0); err != nil {
		return
	}
	err = s.command(ctx, actor, "care.create", adoptionID, in, CareUpdates, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		p, e := tx.Get(Adoptions, adoptionID)
		if e != nil {
			return e
		}
		a, e := Decode[Adoption](p)
		if e != nil {
			return e
		}
		if (p.State != "completed" && p.State != "return_requested") || a.CompletedAt == nil || s.Now().After(a.CompletedAt.Add(30*24*time.Hour)) {
			return ErrConflict
		}
		allowed := actor == a.ApplicantID
		if !allowed {
			allowed, e = s.collaborator(tx, "caretaker", adoptionID, actor)
			if e != nil {
				return e
			}
		}
		if !allowed {
			return ErrForbidden
		}
		if e = s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		r := Record{OwnerID: actor, RefID: adoptionID, MediaIDs: in.MediaIDs, State: "published"}
		if e = save(tx, CareUpdates, &r, CareUpdate{in.Text, in.NeedsHelp}, actor, "care.updated"); e != nil {
			return e
		}
		out = r
		return s.notify(tx, a.PublisherID, actor, "applications", Adoptions, adoptionID, "照护近况已更新")
	})
	return
}

type IntroductionInput struct {
	CommandMeta
	Version  int64   `json:"version"`
	Bio      string  `json:"bio"`
	MediaIDs []int64 `json:"media_ids"`
}

func (s *Service) Introduction(ctx context.Context, actor, petID int64, in IntroductionInput) (out Record, err error) {
	if err = textValid(in.Bio, 5000, true); err != nil {
		return
	}
	err = s.command(ctx, actor, "adoption.introduction", petID, in, Pets, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		r, e := tx.Get(Pets, petID)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		p, e := Decode[Pet](r)
		if e != nil {
			return e
		}
		if !p.AdoptionAvailable {
			return ErrConflict
		}
		if actor != r.OwnerID {
			ok, e := s.collaborator(tx, "adoption_editor", petID, actor)
			if e != nil {
				return e
			}
			if !ok {
				return ErrForbidden
			}
		}
		if e = s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		p.Bio = in.Bio
		if p.Audience.Mode == "public" {
			if e = s.publicAttachments(tx, actor, in.MediaIDs); e != nil {
				return e
			}
		}
		r.MediaIDs = in.MediaIDs
		if e = save(tx, Pets, &r, p, actor, "adoption.introduction_changed"); e != nil {
			return e
		}
		out, e = project(Pets, r, actor)
		return e
	})
	return
}
