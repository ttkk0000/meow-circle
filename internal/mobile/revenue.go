package mobile

import (
	"context"
	"fmt"
	"time"
)

type RevenueInput struct {
	CommandMeta
	OriginalID         int64     `json:"original_id"`
	Kind               string    `json:"kind"`
	PartyID            int64     `json:"party_id"`
	AppointmentID      int64     `json:"appointment_id"`
	AppointmentVersion int64     `json:"appointment_version"`
	AmountCents        int64     `json:"amount_cents"`
	Currency           string    `json:"currency"`
	OccurredAt         time.Time `json:"occurred_at"`
	SourceRef          string    `json:"source_ref"`
	Responsible        string    `json:"responsible"`
}

func (s *Service) RecordRevenue(ctx context.Context, operator string, in RevenueInput) (out Record, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	if !HasString([]string{"actual_received", "actual_refund"}, in.Kind) || in.AmountCents <= 0 || in.AmountCents > 100000000 || in.Currency != "CNY" || in.OccurredAt.After(s.Now()) || textValid(in.SourceRef, 160, true) != nil || textValid(in.Responsible, 120, true) != nil {
		return out, Invalid("actual settlement evidence required; a quote or visit is not revenue")
	}
	key, hash, e := commandKey(in)
	if e != nil {
		return out, e
	}
	key = Fingerprint(in.Kind + ":" + in.SourceRef)
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.ByKey(RevenueLedger, "settlement:"+key)
		if e == nil {
			v, e := Decode[struct {
				Hash string `json:"hash"`
			}](r)
			if e != nil {
				return e
			}
			if v.Hash != hash {
				return Denied("idempotency_conflict")
			}
			out = r
			return nil
		}
		if e != ErrNotFound {
			return e
		}
		appointment, e := tx.Get(Appointments, in.AppointmentID)
		if e != nil {
			return e
		}
		if !participant(appointment, in.PartyID) {
			return ErrForbidden
		}
		a, e := Decode[Appointment](appointment)
		if e != nil {
			return e
		}
		found := false
		for _, v := range a.Versions {
			if v.Number == in.AppointmentVersion && len(v.Confirmations) == 2 {
				found = true
			}
		}
		if !found {
			return ErrConflict
		}
		if in.Kind == "actual_refund" {
			original, e := tx.Get(RevenueLedger, in.OriginalID)
			if e != nil {
				return e
			}
			v, e := Decode[struct {
				Input RevenueInput `json:"input"`
			}](original)
			if e != nil {
				return e
			}
			if v.Input.Kind != "actual_received" || v.Input.PartyID != in.PartyID || v.Input.AppointmentID != in.AppointmentID {
				return Denied("refund_original_mismatch")
			}
			refunded := int64(0)
			rows, e := allRecords(tx, RevenueLedger, Filter{OwnerID: in.PartyID})
			if e != nil {
				return e
			}
			for _, row := range rows {
				prior, e := Decode[struct {
					Input RevenueInput `json:"input"`
				}](row)
				if e != nil {
					return e
				}
				if prior.Input.Kind == "actual_refund" && prior.Input.OriginalID == in.OriginalID {
					refunded += prior.Input.AmountCents
				}
			}
			if refunded+in.AmountCents > v.Input.AmountCents {
				return Denied("refund_exceeds_received_amount")
			}
		} else if in.OriginalID != 0 {
			return Invalid("original_id only applies to a refund")
		}
		r = Record{OwnerID: in.PartyID, RefID: in.AppointmentID, Key: "settlement:" + key, State: "recorded"}
		if e = SetBody(&r, struct {
			Hash       string       `json:"hash"`
			Input      RevenueInput `json:"input"`
			RecordedBy string       `json:"recorded_by"`
		}{hash, in, operator}); e != nil {
			return e
		}
		if e = tx.Save(RevenueLedger, &r); e != nil {
			return e
		}
		out = r
		return audit(tx, operator, fmt.Sprintf("settlement.%s", in.Kind), RevenueLedger, r.ID)
	})
	return
}
func (s *Service) RevenuePage(ctx context.Context, operator string, after int64) (out []Record, err error) {
	if operator == "" {
		return nil, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		var e error
		out, e = tx.List(RevenueLedger, Filter{After: after, Limit: 100})
		if e != nil {
			return e
		}
		return audit(tx, operator, "settlement.export_page", RevenueLedger, after)
	})
	return
}
