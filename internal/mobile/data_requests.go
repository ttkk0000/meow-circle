package mobile

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"time"
)

type DataSelection struct {
	Kind Kind  `json:"kind"`
	ID   int64 `json:"id"`
}
type DataRequestInput struct {
	CommandMeta
	Type        string          `json:"type"` // erase_fields, close_account, matters_assistance
	Fields      []string        `json:"fields"`
	Selections  []DataSelection `json:"selections"`
	Note        string          `json:"note"`
	ReauthToken string          `json:"reauth_token"`
	QueryToken  string          `json:"query_token"` // client obtains a random capability before final confirmation
}
type DataItem struct {
	Kind  Kind   `json:"kind"`
	ID    int64  `json:"id"`
	State string `json:"state"`
}
type DataHold struct {
	Kind        Kind      `json:"kind"`
	ID          int64     `json:"id"`
	Fields      []string  `json:"fields"`
	Basis       string    `json:"basis"`
	Responsible string    `json:"responsible"`
	ReviewAt    time.Time `json:"review_at"`
	Released    bool      `json:"released"`
}
type DataRequest struct {
	ProgressVersion int                               `json:"progress_version,omitempty"`
	ErasedFields    []string                          `json:"erased_fields,omitempty"`
	Processors      map[string]ExternalErasureReceipt `json:"processors,omitempty"`
	Type            string                            `json:"type"`
	Fields          []string                          `json:"fields"`
	Items           []DataItem                        `json:"items"`
	Holds           []DataHold                        `json:"holds"`
	QueryHash       string                            `json:"query_hash"`
	QueryExpiresAt  time.Time                         `json:"query_expires_at"`
	Note            string                            `json:"note"`
	ExternalState   string                            `json:"external_state"`
}
type DataRequestStatus struct {
	ProgressVersion  int        `json:"progress_version"`
	Erased           int        `json:"erased"`
	PhysicalPending  int        `json:"physical_pending"`
	ProviderPending  int        `json:"provider_pending"`
	RestrictedCopies int        `json:"restricted_copies"`
	PendingFields    []string   `json:"pending_fields"`
	NextReviewAt     *time.Time `json:"next_review_at,omitempty"`
	ReviewRequired   bool       `json:"review_required"`
	ID               int64      `json:"id"`
	Version          int64      `json:"version"`
	Type             string     `json:"type"`
	State            string     `json:"state"`
	Pending          int        `json:"pending"`
	Held             int        `json:"held"`
	ExternalState    string     `json:"external_state"`
}

func NewCapability() (string, error) {
	b := make([]byte, 32)
	if _, e := rand.Read(b); e != nil {
		return "", e
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}
func validDataInput(in DataRequestInput) error {
	if !HasString([]string{"erase_fields", "close_account", "matters_assistance"}, in.Type) || len(in.QueryToken) < 40 || len(in.QueryToken) > 100 || len(in.Fields) > 10 || len(in.Selections) > 200 || textValid(in.Note, 2000, false) != nil {
		return Invalid("invalid data request")
	}
	for _, f := range in.Fields {
		if !HasString([]string{"bio", "avatar", "nickname"}, f) {
			return Invalid("invalid profile field")
		}
	}
	for _, v := range in.Selections {
		if in.Type == "matters_assistance" && v.ID > 0 && queryKind(v.Kind) {
			continue
		}
		if v.ID <= 0 || !HasKind([]Kind{Entries, Pets, Cases, Offers, Drafts, Media, Subscriptions}, v.Kind) {
			return Invalid("invalid data selection")
		}
	}
	if in.Type == "erase_fields" && len(in.Fields) == 0 && len(in.Selections) == 0 {
		return Invalid("select data to erase")
	}
	return nil
}
func HasKind(kinds []Kind, k Kind) bool {
	for _, v := range kinds {
		if v == k {
			return true
		}
	}
	return false
}
func DataScope(actor int64, in DataRequestInput) (PurposeScope, error) {
	if e := validDataInput(in); e != nil {
		return PurposeScope{}, e
	}
	return ScopeFor("data_request.submit", string(Accounts), actor, 0, "owner", struct {
		Type       string          `json:"type"`
		Fields     []string        `json:"fields"`
		Selections []DataSelection `json:"selections"`
		QueryHash  string          `json:"query_hash"`
	}{in.Type, in.Fields, in.Selections, Fingerprint(in.QueryToken)})
}
func (s *Service) SubmitDataRequest(ctx context.Context, actor int64, in DataRequestInput) (out Record, err error) {
	if e := validDataInput(in); e != nil {
		return out, e
	}
	err = s.command(ctx, actor, "data_request.submit", 0, in, DataRequests, &out, func(tx Tx) error {
		capability, e := tx.ByKey(AuthChallenges, Fingerprint(in.QueryToken))
		if e != nil {
			return Denied("query_capability_required")
		}
		cap, e := Decode[SafetySession](capability)
		if e != nil {
			return e
		}
		if capability.OwnerID != actor || capability.State != "data_query_capability" || !cap.ExpiresAt.After(s.Now()) {
			return Denied("query_capability_required")
		}
		capability.State = "consumed"
		if e = tx.Save(AuthChallenges, &capability); e != nil {
			return e
		}
		if in.Type != "matters_assistance" {
			scope, e := DataScope(actor, in)
			if e != nil {
				return e
			}
			if e = ConsumePurpose(tx, actor, SessionKey(ctx), in.ReauthToken, in.ClientRequestID, scope, s.Now()); e != nil {
				return e
			}
		}
		d := DataRequest{ProgressVersion: 1, Type: in.Type, Fields: append([]string{}, in.Fields...), Items: []DataItem{}, Holds: []DataHold{}, QueryHash: Fingerprint(in.QueryToken), QueryExpiresAt: s.Now().Add(180 * 24 * time.Hour), Note: in.Note, ExternalState: "not_required"}
		selections := in.Selections
		if in.Type == "close_account" {
			if e = DisablePushDevices(tx, actor, "", true); e != nil {
				return e
			}
			d.Fields = []string{"bio", "avatar", "nickname"}
			d.ExternalState = "pending_inventory"
			for _, kind := range []Kind{Entries, Pets, Cases, Offers, Drafts, Media, Subscriptions, Qualifications, Guardians, Identities, Preferences, Expressions, PushDevices, Sessions, AuthChallenges} {
				rows, e := allRecords(tx, kind, Filter{OwnerID: actor})
				if e != nil {
					return e
				}
				for _, row := range rows {
					selections = append(selections, DataSelection{kind, row.ID})
				}
			}
			// Retain legal/transaction matters privately for staff assessment; no global deletion blockade.
			for _, kind := range []Kind{Adoptions, Appointments, Claims, Groups, Reviews, RightsCases} {
				rows, e := allRecords(tx, kind, Filter{ParticipantID: actor})
				if e != nil {
					return e
				}
				for _, row := range rows {
					d.Holds = append(d.Holds, DataHold{Kind: kind, ID: row.ID, Fields: []string{"minimum_matter_record"}, Basis: "pending_case_assessment", Responsible: "privacy", ReviewAt: s.Now().Add(7 * 24 * time.Hour)})
				}
			}
			u, e := tx.User(actor)
			if e != nil {
				return e
			}
			u.Phone, u.PasswordHash, u.PasswordSalt = "", "", ""
			if e = tx.SaveUser(&u); e != nil {
				return e
			}
			account, e := tx.ByKey(Accounts, fmt.Sprint(actor))
			if errors.Is(e, ErrNotFound) {
				account = Record{OwnerID: actor, Key: fmt.Sprint(actor)}
			} else if e != nil {
				return e
			}
			var a map[string]any
			if account.ID > 0 {
				if e = json.Unmarshal(account.Data, &a); e != nil {
					return e
				}
			} else {
				a = map[string]any{}
			}
			epoch, e := Decode[struct {
				Epoch int64 `json:"session_epoch"`
			}](account)
			if account.ID > 0 && e != nil {
				return e
			}
			a["session_epoch"] = epoch.Epoch + 1
			a["revoke_before"], a["deletion_requested_at"] = s.Now(), s.Now()
			account.State = "deletion_pending"
			if e = SetBody(&account, a); e != nil {
				return e
			}
			if e = tx.Save(Accounts, &account); e != nil {
				return e
			}
		}
		seen := map[string]bool{}
		for _, selection := range selections {
			key := fmt.Sprintf("%s:%d", selection.Kind, selection.ID)
			if seen[key] {
				continue
			}
			seen[key] = true
			r, e := tx.Get(selection.Kind, selection.ID)
			if e != nil {
				return e
			}
			if in.Type == "matters_assistance" {
				ok, e := s.canRead(tx, selection.Kind, r, actor)
				if e != nil {
					return e
				}
				if !ok {
					return ErrForbidden
				}
				continue
			}
			if e = owner(r, actor); e != nil {
				return e
			}
			d.Items = append(d.Items, DataItem{selection.Kind, selection.ID, "pending"})
			if HasKind([]Kind{Entries, Pets, Cases, Offers}, selection.Kind) {
				r.Public = false
				r.State = "removed"
				if e = tx.Save(selection.Kind, &r); e != nil {
					return e
				}
			}
		}
		state := "processing"
		if in.Type == "matters_assistance" {
			state = "needs_assistance"
		}
		r := Record{OwnerID: actor, State: state, Key: "request:" + Fingerprint(in.QueryToken) + ":" + in.ClientRequestID}
		if e := save(tx, DataRequests, &r, d, actor, "data_request.submitted"); e != nil {
			return e
		}
		out = r
		return nil
	})
	if err == nil {
		out = redactDataRequest(out)
	}
	return
}
func redactDataRequest(r Record) Record {
	d, e := Decode[DataRequest](r)
	if e == nil {
		d.QueryHash = ""
		_ = SetBody(&r, d)
	}
	return r
}
func (s *Service) DataRequestStatus(ctx context.Context, id int64, token string) (out DataRequestStatus, err error) {
	if len(token) < 40 || len(token) > 100 {
		return out, ErrForbidden
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		r, e := tx.Get(DataRequests, id)
		if e != nil {
			return ErrNotFound
		}
		d, e := Decode[DataRequest](r)
		if e != nil {
			return e
		}
		if d.QueryHash != Fingerprint(token) || !d.QueryExpiresAt.After(s.Now()) {
			return ErrForbidden
		}
		out = DataRequestStatus{ID: r.ID, Version: r.Version, Type: d.Type, State: r.State, ExternalState: d.ExternalState, PendingFields: []string{}, ProgressVersion: d.ProgressVersion}
		for _, receipt := range d.Processors {
			if receipt.State == "restricted_backup" {
				out.RestrictedCopies++
			}
		}
		heldTargets := map[string]bool{}
		for _, item := range d.Items {
			if item.State != "erased" {
				out.Pending++
			} else {
				out.Erased++
			}
			if item.State == "physical_pending" {
				out.PhysicalPending++
			}
			if item.State == "provider_pending" {
				out.ProviderPending++
			}
			if item.State != "erased" {
				held, due, e := selectedRetention(tx, item, s.Now())
				if e != nil {
					return e
				}
				if held {
					heldTargets[fmt.Sprintf("%s:%d", item.Kind, item.ID)] = true
					if due != nil {
						out.NextReviewAt = earlierReview(out.NextReviewAt, *due)
					}
				}
			}
		}
		for _, field := range d.Fields {
			if !HasString(d.ErasedFields, field) {
				out.PendingFields = append(out.PendingFields, field)
				held, due, e := fieldRetention(tx, r.OwnerID, field)
				if e != nil {
					return e
				}
				if held {
					heldTargets[fmt.Sprintf("%s:%d", Accounts, r.OwnerID)] = true
					if due != nil {
						out.NextReviewAt = earlierReview(out.NextReviewAt, *due)
					}
				}
			}
		}
		for _, h := range d.Holds {
			if !h.Released {
				heldTargets[fmt.Sprintf("%s:%d", h.Kind, h.ID)] = true
				out.NextReviewAt = earlierReview(out.NextReviewAt, h.ReviewAt)
			}
		}
		out.Held = len(heldTargets)
		out.ReviewRequired = out.NextReviewAt != nil && !out.NextReviewAt.After(s.Now())
		return nil
	})
	return
}

// Database erasure commits per request. External copies and physical files have
// independent receipts; this method cannot claim they were erased.
func (s *Service) ProcessDataRequest(ctx context.Context, operator string, id, version int64) (out Record, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(DataRequests, id)
		if e != nil {
			return e
		}
		if e = expected(r, version); e != nil {
			return e
		}
		d, e := Decode[DataRequest](r)
		if e != nil {
			return e
		}
		if r.State != "processing" && r.State != "partially_held" {
			return ErrConflict
		}
		heldPending := false
		d.ProgressVersion = 1
		u, e := tx.User(r.OwnerID)
		if e != nil {
			return e
		}
		for _, f := range d.Fields {
			if HasString(d.ErasedFields, f) {
				continue
			}
			held, _, e := fieldRetention(tx, r.OwnerID, f)
			if e != nil {
				return e
			}
			if held {
				heldPending = true
				continue
			}
			switch f {
			case "bio":
				u.Bio = ""
			case "avatar":
				u.AvatarURL = ""
			case "nickname":
				u.Nickname = "已注销用户"
			}
			d.ErasedFields = append(d.ErasedFields, f)
		}
		if d.Type == "close_account" {
			u.Username = fmt.Sprintf("closed_%d", r.OwnerID)
		}
		if e = tx.SaveUser(&u); e != nil {
			return e
		}
		pending := false
		for _, field := range d.Fields {
			if !HasString(d.ErasedFields, field) {
				pending = true
			}
		}
		changedItems := 0
		for i := range d.Items {
			item := &d.Items[i]
			if item.State == "erased" {
				continue
			}
			held, _, e := selectedRetention(tx, *item, s.Now())
			if e != nil {
				return e
			}
			if held {
				heldPending = true
				item.State = "held"
				pending = true
				continue
			}
			target, e := tx.Get(item.Kind, item.ID)
			if e == ErrNotFound {
				item.State = "erased"
				continue
			}
			if e != nil {
				return e
			}
			if target.OwnerID != r.OwnerID {
				return ErrConflict
			}
			if target.State == "erased" {
				item.State = "erased"
				continue
			}
			if item.Kind == Media {
				if target.State == "deleting" || target.State == "erasure_pending" {
					item.State = "physical_pending"
					pending = true
					continue
				} // retain only the opaque filename until the physical cleanup receipt
				if changedItems >= 100 {
					item.State = "pending"
					pending = true
					continue
				}
				m, e := Decode[MediaFile](target)
				if e != nil {
					return e
				}
				m.SourceLabel = ""
				target.State = "erasure_pending"
				if e = SetBody(&target, m); e != nil {
					return e
				}
				if e = tx.Save(Media, &target); e != nil {
					return e
				}
				changedItems++
				item.State = "physical_pending"
				pending = true
				continue
			}
			if item.Kind == Identities {
				subject, e := Decode[struct {
					Secret string `json:"secret"`
				}](target)
				if e != nil {
					return e
				}
				if subject.Secret != "" {
					if target.State == "revoked" {
						item.State = "provider_pending"
						pending = true
						continue
					}
					if changedItems >= 100 {
						item.State = "pending"
						pending = true
						continue
					}
					target.State = "revoked"
					if e = tx.Save(Identities, &target); e != nil {
						return e
					}
					job := Record{OwnerID: r.OwnerID, RefID: target.ID, Key: fmt.Sprintf("revoke:%d:%d", target.ID, target.Version), State: "pending"}
					if e = SetBody(&job, struct {
						Topic   string          `json:"topic"`
						Subject json.RawMessage `json:"subject"`
					}{"identity.revocation_requested", target.Data}); e != nil {
						return e
					}
					if e = tx.Save(Outbox, &job); e != nil {
						return e
					}
					changedItems++
					item.State = "provider_pending"
					pending = true
					continue
				}
			}
			if changedItems >= 100 {
				item.State = "pending"
				pending = true
				continue
			}
			if e = tx.Delete(item.Kind, target.ID, target.Version); e != nil {
				return e
			}
			changedItems++
			item.State = "erased"
		}
		for _, hold := range d.Holds {
			if !hold.Released {
				heldPending = true
				pending = true
			}
		}
		r.State = "completed"
		if pending {
			r.State = "processing"
		}
		if heldPending {
			r.State = "partially_held"
		}
		if d.ExternalState != "not_required" && d.ExternalState != "completed" {
			r.State = "processing"
		}
		if r.State == "completed" && d.Type == "close_account" {
			account, e := tx.ByKey(Accounts, fmt.Sprint(r.OwnerID))
			if e != nil {
				return e
			}
			account.State = "closed"
			if e = tx.Save(Accounts, &account); e != nil {
				return e
			}
		}
		if e = save(tx, DataRequests, &r, d, r.OwnerID, "data_request.progress"); e != nil {
			return e
		}
		out = redactDataRequest(r)
		return audit(tx, operator, "data_request.process", DataRequests, id)
	})
	return
}

type DataHoldInput struct {
	Version int64    `json:"version"`
	Hold    DataHold `json:"hold"`
}

func (s *Service) SetDataHold(ctx context.Context, operator string, id int64, in DataHoldInput) error {
	if operator == "" || textValid(in.Hold.Responsible, 120, true) != nil || textValid(in.Hold.Basis, 1000, true) != nil || !in.Hold.ReviewAt.After(s.Now()) || in.Hold.ReviewAt.After(s.Now().Add(90*24*time.Hour)) {
		return Invalid("specific retention basis, responsible party and review date required")
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(DataRequests, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		d, e := Decode[DataRequest](r)
		if e != nil {
			return e
		}
		if r.State != "processing" && r.State != "partially_held" {
			return ErrConflict
		}
		if e = validateDataHold(d, r.OwnerID, in.Hold); e != nil {
			return e
		}
		if in.Hold.Kind == Media && !in.Hold.Released {
			media, e := tx.Get(Media, in.Hold.ID)
			if e != nil {
				return e
			}
			if media.OwnerID != r.OwnerID {
				return ErrForbidden
			}
			file, e := Decode[MediaFile](media)
			if e != nil {
				return e
			}
			if HasString([]string{"erased", "deleting", "upload_expired"}, media.State) ||
				media.State == "uploading" && (file.UploadLeaseUntil == nil || !file.UploadLeaseUntil.After(s.Now())) {
				return Denied("media_retention_target_unavailable")
			}
		}
		found := false
		for i, h := range d.Holds {
			if h.Kind == in.Hold.Kind && h.ID == in.Hold.ID {
				d.Holds[i] = in.Hold
				found = true
			}
		}
		if !found {
			d.Holds = append(d.Holds, in.Hold)
		}
		r.State = "processing"
		if e = save(tx, DataRequests, &r, d, r.OwnerID, "data_request.hold_updated"); e != nil {
			return e
		}
		return audit(tx, operator, "data_request.hold_updated", DataRequests, id)
	})
}
