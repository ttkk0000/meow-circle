package mobile

import (
	"context"
	"time"
)

type Fee struct {
	Name        string `json:"name"`
	AmountCents int64  `json:"amount_cents"`
	Required    bool   `json:"required"`
	PaidBy      string `json:"paid_by"`
	When        string `json:"when"`
}
type Offer struct {
	PetID                 int64     `json:"pet_id"`
	PetVersion            int64     `json:"pet_version"`
	Nature                string    `json:"nature"`         // adoption, sale, paid_transfer
	PublisherType         string    `json:"publisher_type"` // person, business, rescue
	Relationship          string    `json:"relationship"`   // direct, employee, agent
	AuthoritySummary      string    `json:"authority_summary"`
	Custodian             string    `json:"custodian"`
	CustodianRelationship string    `json:"custodian_relationship"`
	Origin                string    `json:"origin"`
	City                  string    `json:"city"`
	Area                  string    `json:"area"`
	VenueType             string    `json:"venue_type"`
	Reception             string    `json:"reception"`
	ReceptionRelationship string    `json:"reception_relationship"`
	Payee                 string    `json:"payee"`
	ResponsibleParty      string    `json:"responsible_party"`
	KnownHealth           string    `json:"known_health"`
	Aftercare             string    `json:"aftercare"`
	PriceCents            int64     `json:"price_cents"`
	Currency              string    `json:"currency"`
	Fees                  []Fee     `json:"fees"`
	TotalCents            int64     `json:"total_cents"`
	DepositTerms          string    `json:"deposit_terms"`
	PrivateAddress        string    `json:"private_address,omitempty"`
	ValidUntil            time.Time `json:"valid_until"`
}
type OfferInput struct {
	CommandMeta
	Version  int64   `json:"version"`
	Offer    Offer   `json:"offer"`
	MediaIDs []int64 `json:"media_ids"`
}

func (s *Service) SaveOffer(ctx context.Context, actor, id int64, in OfferInput) (out Record, err error) {
	if id == 0 && in.Version != 0 {
		return out, ErrConflict
	}
	o := in.Offer
	if !HasString([]string{"adoption", "sale", "paid_transfer"}, o.Nature) || !HasString([]string{"person", "business", "rescue"}, o.PublisherType) || !HasString([]string{"direct", "employee", "agent"}, o.Relationship) {
		return out, Invalid("invalid nature, publisher or relationship")
	}
	for _, v := range []string{o.Custodian, o.CustodianRelationship, o.AuthoritySummary, o.Origin, o.City, o.Area, o.VenueType, o.Reception, o.ReceptionRelationship, o.Payee, o.ResponsibleParty, o.KnownHealth, o.Aftercare, o.DepositTerms} {
		if e := textValid(v, 1000, true); e != nil {
			return out, e
		}
	}
	if err = textValid(o.PrivateAddress, 500, false); err != nil {
		return
	}
	if o.Currency != "CNY" || o.PriceCents < 0 || o.PriceCents > 100000000 || len(o.Fees) > 30 || o.ValidUntil.Before(s.Now()) || o.ValidUntil.After(s.Now().Add(90*24*time.Hour)) {
		return out, Invalid("invalid price or validity")
	}
	total := o.PriceCents
	for _, fee := range o.Fees {
		if fee.AmountCents < 0 || fee.AmountCents > 100000000 {
			return out, Invalid("invalid fee")
		}
		for _, v := range []string{fee.Name, fee.PaidBy, fee.When} {
			if e := textValid(v, 200, true); e != nil {
				return out, e
			}
		}
		if fee.Required {
			total += fee.AmountCents
		}
	}
	if total > 100000000 || o.TotalCents != total {
		return out, Invalid("total must include the price and every required fee")
	}
	if o.Nature == "adoption" && (o.PriceCents > 0 || total > 0) {
		return out, Invalid("charged adoption expenses require manual review; use the private support path")
	}
	err = s.command(ctx, actor, "offer.save", id, in, Offers, &out, func(tx Tx) error {
		if e := s.adult(tx, actor); e != nil {
			return e
		}
		pet, e := tx.Get(Pets, o.PetID)
		if e != nil {
			return e
		}
		if e = owner(pet, actor); e != nil {
			return e
		}
		if pet.Version != o.PetVersion || pet.State != "active" {
			return ErrConflict
		}
		if e = s.disposition(tx, actor, pet.ID); e != nil {
			return e
		}
		p, e := Decode[Pet](pet)
		if e != nil {
			return e
		}
		action := "commercial"
		if o.Nature == "adoption" {
			action = "adoption"
		}
		if e = s.cityAdmission(tx, o.City, p.Species, action); e != nil {
			return e
		}
		if e = s.publicAttachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		if e = s.factualAttachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		r := Record{OwnerID: actor, RefID: o.PetID, State: "published", Public: true, MediaIDs: in.MediaIDs}
		if id > 0 {
			r, e = writable(tx, Offers, id, actor, in.Version)
			if e != nil {
				return e
			}
			if r.State != "published" {
				return ErrConflict
			}
		}
		r.RefID = o.PetID
		r.MediaIDs = in.MediaIDs
		if e = save(tx, Offers, &r, o, actor, "offer.saved"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type DisclosureSnapshot struct {
	OfferID      int64 `json:"offer_id"`
	OfferVersion int64 `json:"offer_version"`
	Disclosure   Offer `json:"disclosure"`
}
