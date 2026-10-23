package mobile

import (
	"context"
	"encoding/json"
	"time"
)

type ContractTerms struct {
	City             string     `json:"city"`
	Species          string     `json:"species"`
	Nature           string     `json:"nature"`
	Responsibilities string     `json:"responsibilities"`
	HealthDisclosure string     `json:"health_disclosure"`
	ReturnTerms      string     `json:"return_terms"`
	AmountCents      int64      `json:"amount_cents"`
	Fees             []Fee      `json:"fees"`
	Payee            string     `json:"payee"`
	TrialUntil       *time.Time `json:"trial_until,omitempty"`
	OfferID          int64      `json:"offer_id"`
	OfferVersion     int64      `json:"offer_version"`
}
type ContractRevision struct {
	Number     int64         `json:"number"`
	Terms      ContractTerms `json:"terms"`
	Hash       string        `json:"hash"`
	ProposedBy int64         `json:"proposed_by"`
	AcceptedBy []int64       `json:"accepted_by"`
	Status     string        `json:"status"`
	CreatedAt  time.Time     `json:"created_at"`
}
type ContractInput struct {
	CommandMeta
	Version     int64         `json:"version"`
	Terms       ContractTerms `json:"terms"`
	Revision    int64         `json:"revision"`
	ReauthToken string        `json:"reauth_token"`
}

func ContractScope(id, version int64, revision ContractRevision, actor int64) (PurposeScope, error) {
	return ScopeFor("contract.accept", string(Adoptions), id, version, "party", struct {
		Actor  int64  `json:"actor"`
		Number int64  `json:"number"`
		Hash   string `json:"hash"`
	}{actor, revision.Number, revision.Hash})
}
func (s *Service) Contract(ctx context.Context, actor, id int64, action string, in ContractInput) (out Record, err error) {
	err = s.command(ctx, actor, "contract."+action, id, in, Adoptions, &out, func(tx Tx) error {
		if e := s.adult(tx, actor); e != nil {
			return e
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
		if !HasString([]string{"approved", "trial_proposed", "trial_active", "handover_pending"}, r.State) {
			return ErrConflict
		}
		a, e := Decode[Adoption](r)
		if e != nil {
			return e
		}
		pet, p, e := adoptionPet(tx, a)
		if e != nil {
			return e
		}
		if pet.State != "active" || pet.OwnerID != a.PublisherID {
			return ErrConflict
		}
		if e = s.disposition(tx, a.PublisherID, a.PetID); e != nil {
			return e
		}
		switch action {
		case "propose":
			t := in.Terms
			if t.Species != p.Species || !HasString([]string{"adoption", "sale", "paid_transfer"}, t.Nature) || t.AmountCents < 0 || t.AmountCents > 100000000 || len(t.Fees) > 30 {
				return Invalid("invalid contract terms")
			}
			for _, v := range []string{t.Responsibilities, t.HealthDisclosure, t.ReturnTerms, t.Payee} {
				if e = textValid(v, 3000, true); e != nil {
					return e
				}
			}
			if t.TrialUntil != nil && (!t.TrialUntil.After(s.Now()) || t.TrialUntil.After(s.Now().Add(30*24*time.Hour))) {
				return Invalid("invalid trial period")
			}
			if e = s.cityAdmission(tx, t.City, t.Species, "adoption"); e != nil {
				return e
			}
			if t.Nature == "adoption" {
				if t.AmountCents != 0 || len(t.Fees) != 0 {
					return Denied("charged_adoption_requires_review")
				}
			} else {
				offer, e := tx.Get(Offers, t.OfferID)
				if e != nil {
					return e
				}
				o, e := Decode[Offer](offer)
				if e != nil {
					return e
				}
				if offer.OwnerID != a.PublisherID || offer.RefID != a.PetID || offer.Version != t.OfferVersion || offer.State != "published" || !o.ValidUntil.After(s.Now()) {
					return ErrConflict
				}
				if o.TotalCents != t.AmountCents || o.Nature != t.Nature || o.City != t.City || o.Payee != t.Payee {
					return Denied("contract_disclosure_mismatch")
				}
				left, _ := json.Marshal(o.Fees)
				right, _ := json.Marshal(t.Fees)
				if string(left) != string(right) {
					return Denied("contract_disclosure_mismatch")
				}
				if e = s.cityAdmission(tx, t.City, t.Species, "commercial"); e != nil {
					return e
				}
			}
			for i := range a.Contracts {
				if a.Contracts[i].Status == "pending" {
					a.Contracts[i].Status = "superseded"
				}
			}
			b, e := json.Marshal(t)
			if e != nil {
				return e
			}
			a.Contracts = append(a.Contracts, ContractRevision{int64(len(a.Contracts) + 1), t, Fingerprint(string(b)), actor, []int64{}, "pending", s.Now()})
			a.HandoverConfirmations = []int64{}
		case "accept":
			i := -1
			for j, v := range a.Contracts {
				if v.Number == in.Revision {
					i = j
				}
			}
			if i < 0 || a.Contracts[i].Status != "pending" {
				return ErrConflict
			}
			v := &a.Contracts[i]
			if v.Terms.Species != p.Species || v.Terms.TrialUntil != nil && !v.Terms.TrialUntil.After(s.Now()) {
				return ErrConflict
			}
			if Has(v.AcceptedBy, actor) {
				return ErrConflict
			}
			if e = s.cityAdmission(tx, v.Terms.City, v.Terms.Species, "offline"); e != nil {
				return e
			}
			scope, e := ContractScope(id, in.Version, *v, actor)
			if e != nil {
				return e
			}
			if e = ConsumePurpose(tx, actor, SessionKey(ctx), in.ReauthToken, in.ClientRequestID, scope, s.Now()); e != nil {
				return e
			}
			v.AcceptedBy = append(v.AcceptedBy, actor)
			if len(v.AcceptedBy) == 2 {
				for j := range a.Contracts {
					if a.Contracts[j].Status == "effective" {
						a.Contracts[j].Status = "replaced"
					}
				}
				v.Status = "effective"
				a.EffectiveContract = v.Number
				a.TrialUntil = v.Terms.TrialUntil
				a.TrialTerms = v.Terms.Responsibilities
			}
		case "decline":
			found := false
			for i := range a.Contracts {
				if a.Contracts[i].Number == in.Revision && a.Contracts[i].Status == "pending" {
					a.Contracts[i].Status = "declined"
					found = true
				}
			}
			if !found {
				return ErrConflict
			}
		default:
			return Invalid("invalid contract action")
		}
		if e = save(tx, Adoptions, &r, a, actor, "contract."+action); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
func (s *Service) ContractScope(ctx context.Context, actor, id, version, revision int64) (out PurposeScope, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		r, e := tx.Get(Adoptions, id)
		if e != nil {
			return e
		}
		if !participant(r, actor) {
			return ErrForbidden
		}
		if e = expected(r, version); e != nil {
			return e
		}
		a, e := Decode[Adoption](r)
		if e != nil {
			return e
		}
		for _, v := range a.Contracts {
			if v.Number == revision && v.Status == "pending" {
				out, e = ContractScope(id, version, v, actor)
				return e
			}
		}
		return ErrConflict
	})
	return
}

type ReturnTerms struct {
	StartsAt  time.Time `json:"starts_at"`
	Place     string    `json:"place"`
	Reception string    `json:"reception"`
	CareUntil string    `json:"care_until"`
	Costs     string    `json:"costs"`
	Safety    string    `json:"safety"`
}
type ReturnPlan struct {
	Number        int64       `json:"number"`
	Terms         ReturnTerms `json:"terms"`
	ProposedBy    int64       `json:"proposed_by"`
	Confirmations []int64     `json:"confirmations"`
	Handover      []int64     `json:"handover"`
	State         string      `json:"state"`
}
type ReturnInput struct {
	CommandMeta
	Version  int64       `json:"version"`
	Revision int64       `json:"revision"`
	Terms    ReturnTerms `json:"terms"`
}

func (s *Service) Return(ctx context.Context, actor, id int64, action string, in ReturnInput) (out Record, err error) {
	err = s.command(ctx, actor, "return."+action, id, in, Adoptions, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
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
		if r.State != "return_requested" && r.State != "return_agreed" {
			return ErrConflict
		}
		a, e := Decode[Adoption](r)
		if e != nil {
			return e
		}
		petRecord, _, e := adoptionPet(tx, a)
		if e != nil {
			return e
		}
		if petRecord.OwnerID != a.PublisherID && petRecord.OwnerID != a.ApplicantID {
			return ErrConflict
		}
		if action == "propose" {
			if e = s.adult(tx, actor); e != nil {
				return e
			}
			if !in.Terms.StartsAt.After(s.Now()) || in.Terms.StartsAt.After(s.Now().Add(30*24*time.Hour)) {
				return Invalid("invalid safe return time")
			}
			for _, v := range []string{in.Terms.Place, in.Terms.Reception, in.Terms.CareUntil, in.Terms.Costs, in.Terms.Safety} {
				if e = textValid(v, 1500, true); e != nil {
					return e
				}
			}
			for i := range a.ReturnPlans {
				if a.ReturnPlans[i].State == "pending" {
					a.ReturnPlans[i].State = "superseded"
				}
			}
			a.ReturnPlans = append(a.ReturnPlans, ReturnPlan{int64(len(a.ReturnPlans) + 1), in.Terms, actor, []int64{actor}, []int64{}, "pending"})
		} else {
			i := -1
			for j, v := range a.ReturnPlans {
				if v.Number == in.Revision {
					i = j
				}
			}
			if i < 0 {
				return ErrConflict
			}
			v := &a.ReturnPlans[i]
			switch action {
			case "accept":
				if v.State != "pending" || !v.Terms.StartsAt.After(s.Now()) {
					return ErrConflict
				}
				if Has(v.Confirmations, actor) {
					return ErrConflict
				}
				if e = s.adult(tx, actor); e != nil {
					return e
				}
				v.Confirmations = Unique(append(v.Confirmations, actor))
				if len(v.Confirmations) == 2 {
					for _, party := range []int64{a.PublisherID, a.ApplicantID} {
						if e = s.adult(tx, party); e != nil {
							return e
						}
					}
					for j := range a.ReturnPlans {
						if a.ReturnPlans[j].State == "effective" {
							a.ReturnPlans[j].State = "replaced"
						}
					}
					v.State = "effective"
					a.EffectiveReturn = v.Number
					r.State = "return_agreed"
				}
			case "decline":
				if v.State != "pending" || v.ProposedBy == actor || !v.Terms.StartsAt.After(s.Now()) {
					return ErrConflict
				}
				v.State = "declined"
			case "withdraw":
				if v.State != "pending" || v.ProposedBy != actor {
					return ErrForbidden
				}
				v.State = "withdrawn"
			case "handover":
				if v.State != "effective" || a.EffectiveReturn != v.Number {
					return ErrConflict
				}
				if e = s.adult(tx, actor); e != nil {
					return e
				}
				v.Handover = Unique(append(v.Handover, actor))
				if len(v.Handover) == 2 {
					for _, party := range []int64{a.PublisherID, a.ApplicantID} {
						if e = s.adult(tx, party); e != nil {
							return e
						}
					}
					v.State = "completed"
					r.State = "returned"
					for j := range a.ReturnPlans {
						if a.ReturnPlans[j].State == "pending" {
							a.ReturnPlans[j].State = "superseded"
						}
					}
					pet, e := tx.Get(Pets, a.PetID)
					if e != nil {
						return e
					}
					if pet.OwnerID == a.ApplicantID {
						pet.OwnerID = a.PublisherID
					}
					p, e := Decode[Pet](pet)
					if e != nil {
						return e
					}
					if pet.OwnerID != a.PublisherID {
						return ErrConflict
					}
					p.AdoptionAvailable = false
					p.PlacementState = ""
					if e = save(tx, Pets, &pet, p, actor, "pet.safely_returned"); e != nil {
						return e
					}
				}
			default:
				return Invalid("invalid return action")
			}
		}
		if e = save(tx, Adoptions, &r, a, actor, "return."+action); e != nil {
			return e
		}
		out = r
		other := a.PublisherID
		if actor == other {
			other = a.ApplicantID
		}
		return s.notify(tx, other, actor, "applications", Adoptions, r.ID, "安全接回进度已更新")
	})
	return
}
