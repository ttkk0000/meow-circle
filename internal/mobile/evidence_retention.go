package mobile

import (
	"context"
	"time"
)

type rightsCaseRecord struct {
	RequestHash string     `json:"request_hash"`
	Case        RightsCase `json:"case"`
}

func earlierReview(current *time.Time, candidate time.Time) *time.Time {
	if candidate.IsZero() || current != nil && !candidate.Before(*current) {
		return current
	}
	return &candidate
}

// A review date requests human review; it never releases a scoped hold.
func dataRetention(tx Tx, kind Kind, id int64) (held bool, reviewAt *time.Time, err error) {
	// Participant matters may be held by another participant's request.
	requests, err := allRecords(tx, DataRequests, Filter{})
	if err != nil {
		return false, nil, err
	}
	for _, request := range requests {
		d, e := Decode[DataRequest](request)
		if e != nil {
			return false, nil, e
		}
		for _, h := range d.Holds {
			if !h.Released && h.Kind == kind && h.ID == id {
				held = true
				reviewAt = earlierReview(reviewAt, h.ReviewAt)
			}
		}
	}
	return
}

func fieldRetention(tx Tx, ownerID int64, field string) (held bool, reviewAt *time.Time, err error) {
	requests, err := allRecords(tx, DataRequests, Filter{OwnerID: ownerID})
	if err != nil {
		return false, nil, err
	}
	for _, request := range requests {
		d, e := Decode[DataRequest](request)
		if e != nil {
			return false, nil, e
		}
		for _, h := range d.Holds {
			if !h.Released && h.Kind == Accounts && h.ID == ownerID && HasString(h.Fields, field) {
				held = true
				reviewAt = earlierReview(reviewAt, h.ReviewAt)
			}
		}
	}
	return
}

func selectedRetention(tx Tx, item DataItem, now time.Time) (bool, *time.Time, error) {
	if item.Kind != Media {
		return dataRetention(tx, item.Kind, item.ID)
	}
	r, e := tx.Get(Media, item.ID)
	if e == ErrNotFound {
		return false, nil, nil
	}
	if e != nil {
		return false, nil, e
	}
	return mediaRetention(tx, r, now)
}

func sameFields(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	for _, field := range a {
		if !HasString(b, field) {
			return false
		}
	}
	return true
}

func validateDataHold(d DataRequest, ownerID int64, in DataHold) error {
	if !in.Kind.Valid() || in.ID <= 0 || len(in.Fields) > 10 {
		return Invalid("invalid retention scope")
	}
	var existing *DataHold
	for i := range d.Holds {
		if d.Holds[i].Kind == in.Kind && d.Holds[i].ID == in.ID {
			existing = &d.Holds[i]
			break
		}
	}
	if in.Released {
		if existing == nil || existing.Released || !sameFields(existing.Fields, in.Fields) {
			return ErrConflict
		}
		return nil
	}
	if in.Kind == Accounts {
		if in.ID != ownerID || len(in.Fields) == 0 {
			return ErrForbidden
		}
		for _, field := range in.Fields {
			if !HasString(d.Fields, field) || HasString(d.ErasedFields, field) {
				return Denied("retention_outside_pending_selection")
			}
		}
		return nil
	}
	for _, item := range d.Items {
		if item.Kind == in.Kind && item.ID == in.ID {
			if item.State == "erased" || len(in.Fields) != 0 {
				return Denied("retention_outside_pending_selection")
			}
			return nil
		}
	}
	// Close-account assessment holds may refer to participant matters rather
	// than owned deletion selections. Only preserve their existing scope.
	if existing != nil && !existing.Released && sameFields(existing.Fields, in.Fields) {
		return nil
	}
	return Denied("retention_outside_pending_selection")
}

func rightsEvidenceClosed(r Record, c RightsCase) bool {
	return r.State == "closed" && HasString([]string{"not_required", "transferred"}, c.TransferState)
}

func rightsEvidenceHeld(r Record, c RightsCase, now time.Time) bool {
	return c.EvidenceErasedAt == nil && (c.EvidenceUntil.After(now) || !rightsEvidenceClosed(r, c))
}

// Every route to media erasure must check the same evidence and data holds.
// This does not grant anyone permission to read retained private material.
func mediaRetention(tx Tx, r Record, now time.Time) (held bool, reviewAt *time.Time, err error) {
	held, reviewAt, err = dataRetention(tx, Media, r.ID)
	if err != nil {
		return
	}
	reviews, err := allRecords(tx, Reviews, Filter{MediaID: r.ID})
	if err != nil {
		return false, nil, err
	}
	for _, review := range reviews {
		v, e := Decode[Review](review)
		if e != nil {
			return false, nil, e
		}
		if v.EvidenceExpiresAt != nil && v.EvidenceExpiresAt.After(now) {
			held = true
			reviewAt = earlierReview(reviewAt, *v.EvidenceExpiresAt)
		}
	}
	cases, err := allRecords(tx, RightsCases, Filter{MediaID: r.ID})
	if err != nil {
		return false, nil, err
	}
	for _, c := range cases {
		v, e := Decode[rightsCaseRecord](c)
		if e != nil {
			return false, nil, e
		}
		caseHeld, due, e := dataRetention(tx, RightsCases, c.ID)
		if e != nil {
			return false, nil, e
		}
		if caseHeld || rightsEvidenceHeld(c, v.Case, now) {
			held = true
			if due != nil {
				reviewAt = earlierReview(reviewAt, *due)
			}
			reviewAt = earlierReview(reviewAt, v.Case.EvidenceUntil)
		}
	}
	return
}

type RetentionExpiryInput struct {
	Cursor int64 `json:"cursor"`
}

type RetentionExpiryResult struct {
	NextCursor int64 `json:"next_cursor"`
	Erased     int   `json:"erased"`
	Held       int   `json:"held"`
}

// Explicitly scheduled by a privacy operator. Closing a case does not erase
// evidence before its retention deadline, and an overdue open case is held.
func (s *Service) ExpireRightsEvidence(ctx context.Context, operator string, in RetentionExpiryInput) (out RetentionExpiryResult, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	if in.Cursor < 0 {
		return out, Invalid("invalid retention cursor")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		rows, e := tx.List(RightsCases, Filter{After: in.Cursor, Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			out.NextCursor = r.ID
			v, e := Decode[rightsCaseRecord](r)
			if e != nil {
				return e
			}
			if v.Case.EvidenceErasedAt != nil || v.Case.EvidenceUntil.After(s.Now()) {
				continue
			}
			held, _, e := dataRetention(tx, RightsCases, r.ID)
			if e != nil {
				return e
			}
			if held || !rightsEvidenceClosed(r, v.Case) {
				out.Held++
				continue
			}
			// Keep only the receipt and workflow metadata. This is not a receipt
			// for deleting physical media, backups or external processor copies.
			now := s.Now()
			v.Case.EvidenceErasedAt = &now
			v.Case.Contact, v.Case.LegalName, v.Case.Statement, v.Case.RightType = "", "", "", ""
			v.Case.Messages = nil
			v.Case.TargetKind, v.Case.TargetID = "", 0
			r.MediaIDs = nil
			if e = save(tx, RightsCases, &r, v, r.OwnerID, "rights_case.evidence_erased"); e != nil {
				return e
			}
			if e = audit(tx, operator, "rights_case.evidence_erased", RightsCases, r.ID); e != nil {
				return e
			}
			out.Erased++
		}
		if len(rows) < 100 {
			out.NextCursor = 0
		}
		return nil
	})
	return
}

type RightsQueueItem struct {
	ID            int64     `json:"id"`
	Version       int64     `json:"version"`
	State         string    `json:"state"`
	Type          string    `json:"type"`
	ReviewDue     time.Time `json:"review_due"`
	EvidenceUntil time.Time `json:"evidence_until"`
	EvidenceState string    `json:"evidence_state"`
	TransferState string    `json:"transfer_state"`
}

type RightsQueuePage struct {
	Items      []RightsQueueItem `json:"items"`
	NextCursor int64             `json:"next_cursor"`
}

type RightsCaseDetails struct {
	Receipt           RightsQueueItem `json:"receipt"`
	EvidenceAvailable bool            `json:"evidence_available"`
	TargetKind        Kind            `json:"target_kind,omitempty"`
	TargetID          int64           `json:"target_id,omitempty"`
	RelatedCaseID     int64           `json:"related_case_id,omitempty"`
	Contact           string          `json:"contact,omitempty"`
	LegalName         string          `json:"legal_name,omitempty"`
	RightType         string          `json:"right_type,omitempty"`
	Statement         string          `json:"statement,omitempty"`
	Messages          []RightsMessage `json:"messages"`
	MediaIDs          []int64         `json:"media_ids"`
}

func rightsQueueItem(r Record, c RightsCase, now time.Time) RightsQueueItem {
	return RightsQueueItem{r.ID, r.Version, r.State, c.Type, c.ReviewDue, c.EvidenceUntil, rightsEvidenceState(c, now), c.TransferState}
}

// Overdue retention does not extend private evidence reading permission. A
// moderator must first record a justified extension on the exact case version.
func (s *Service) RightsDetails(ctx context.Context, operator string, id int64) (out RightsCaseDetails, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(RightsCases, id)
		if e != nil {
			return e
		}
		v, e := Decode[rightsCaseRecord](r)
		if e != nil {
			return e
		}
		out = RightsCaseDetails{Receipt: rightsQueueItem(r, v.Case, s.Now()), Messages: []RightsMessage{}, MediaIDs: []int64{}}
		if v.Case.EvidenceErasedAt == nil && v.Case.EvidenceUntil.After(s.Now()) {
			out.EvidenceAvailable = true
			out.TargetKind, out.TargetID, out.RelatedCaseID = v.Case.TargetKind, v.Case.TargetID, v.Case.RelatedCaseID
			out.Contact, out.LegalName, out.RightType, out.Statement = v.Case.Contact, v.Case.LegalName, v.Case.RightType, v.Case.Statement
			out.Messages = append(out.Messages, v.Case.Messages...)
			out.MediaIDs = append(out.MediaIDs, r.MediaIDs...)
		}
		return audit(tx, operator, "rights_case.details_read", RightsCases, id)
	})
	return
}

func rightsEvidenceState(c RightsCase, now time.Time) string {
	if c.EvidenceErasedAt != nil {
		return "erased"
	}
	if !c.EvidenceUntil.After(now) {
		return "review_due"
	}
	return "retained"
}

func rightsReceipt(r Record, c RightsCase, now time.Time) RightsReceipt {
	return RightsReceipt{ID: r.ID, Version: r.Version, State: r.State, Type: c.Type,
		ReviewDue: c.ReviewDue, EvidenceState: rightsEvidenceState(c, now)}
}

func (s *Service) RightsQueue(ctx context.Context, operator string, after int64) (out RightsQueuePage, err error) {
	out.Items = []RightsQueueItem{}
	if operator == "" {
		return out, ErrForbidden
	}
	if after < 0 {
		return out, Invalid("invalid rights queue cursor")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		rows, e := tx.List(RightsCases, Filter{After: after, Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			v, e := Decode[rightsCaseRecord](r)
			if e != nil {
				return e
			}
			out.Items = append(out.Items, rightsQueueItem(r, v.Case, s.Now()))
			out.NextCursor = r.ID
		}
		if len(rows) < 100 {
			out.NextCursor = 0
		}
		return audit(tx, operator, "rights_case.queue_read", RightsCases, after)
	})
	return
}
