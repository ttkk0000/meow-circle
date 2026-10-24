package mobile

import (
	"context"
	"fmt"
	"time"
)

type MediaErasure struct {
	ID             int64  `json:"id"`
	Version        int64  `json:"version"`
	Filename       string `json:"-"`
	DeletePhysical bool   `json:"delete_physical"`
}

func (s *Service) NewDataCapability(ctx context.Context, actor int64) (token string, err error) {
	token, err = NewCapability()
	if err != nil {
		return
	}
	err = s.actorUpdate(ctx, actor, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r := Record{OwnerID: actor, Key: Fingerprint(token), State: "data_query_capability"}
		if e := SetBody(&r, SafetySession{ExpiresAt: s.Now().Add(180 * 24 * time.Hour)}); e != nil {
			return e
		}
		return tx.Save(AuthChallenges, &r)
	})
	return
}
func (s *Service) DataRequestByKey(ctx context.Context, key, token string) (out DataRequestStatus, err error) {
	if len(token) < 40 || len(token) > 100 || len(key) < 8 || len(key) > 100 {
		return out, ErrForbidden
	}
	var id int64
	err = s.Repo.View(ctx, func(tx Tx) error {
		r, e := tx.ByKey(DataRequests, "request:"+Fingerprint(token)+":"+key)
		id = r.ID
		return e
	})
	if err != nil {
		return
	}
	return s.DataRequestStatus(ctx, id, token)
}
func (s *Service) PrepareMediaErasure(ctx context.Context, operator string, id int64) (out MediaErasure, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Media, id)
		if e != nil {
			return e
		}
		if r.State != "erasure_pending" && r.State != "deleting" {
			return ErrConflict
		}
		held, _, e := mediaRetention(tx, r, s.Now())
		if e != nil {
			return e
		}
		if held {
			return Denied("media_under_scoped_hold")
		}
		file, e := Decode[MediaFile](r)
		if e != nil {
			return e
		}
		remove := true
		rows, e := allRecords(tx, Media, Filter{})
		if e != nil {
			return e
		}
		for _, other := range rows {
			if other.ID == id {
				continue
			}
			m, e := Decode[MediaFile](other)
			if e != nil {
				return e
			}
			if m.Filename == file.Filename && m.Filename != "" && other.State != "erased" {
				remove = false
			}
		}
		// New uploads use owner/request-specific physical names. Legacy shared
		// files survive until the last legitimate reference is removed.
		r.State = "deleting"
		if e = tx.Save(Media, &r); e != nil {
			return e
		}
		out = MediaErasure{r.ID, r.Version, file.Filename, remove}
		return audit(tx, operator, "media.erasure_prepared", Media, id)
	})
	return
}
func (s *Service) FinishMediaErasure(ctx context.Context, operator string, in MediaErasure) error {
	if operator == "" {
		return ErrForbidden
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Media, in.ID)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		if r.State != "deleting" {
			return ErrConflict
		}
		r.State = "erased"
		r.Data = []byte(`{}`)
		if e = tx.Save(Media, &r); e != nil {
			return e
		}
		return audit(tx, operator, "media.erased", Media, r.ID)
	})
}

type ExternalErasureReceipt struct {
	System      string `json:"system"`
	State       string `json:"state"`
	ResultRef   string `json:"result_ref"`
	Basis       string `json:"basis"`
	Responsible string `json:"responsible"`
}
type ExternalErasureInput struct {
	Version   int64                   `json:"version"`
	Inventory []string                `json:"inventory"`
	Receipt   *ExternalErasureReceipt `json:"receipt,omitempty"`
}

func (s *Service) RecordExternalErasure(ctx context.Context, operator string, id int64, in ExternalErasureInput) error {
	if operator == "" || len(in.Inventory) > 30 {
		return ErrForbidden
	}
	for _, system := range in.Inventory {
		if textValid(system, 80, true) != nil {
			return Invalid("invalid processor inventory")
		}
	}
	if in.Receipt != nil {
		v := in.Receipt
		if !HasString([]string{"pending", "completed", "restricted_backup", "not_applicable"}, v.State) || textValid(v.System, 80, true) != nil || textValid(v.ResultRef, 160, true) != nil || textValid(v.Basis, 1000, true) != nil || textValid(v.Responsible, 120, true) != nil {
			return Invalid("specific processor evidence required")
		}
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
		if d.ExternalState == "pending_inventory" {
			if len(in.Inventory) == 0 {
				return Invalid("inventory and a local-storage receipt are required")
			}
			d.Processors = map[string]ExternalErasureReceipt{}
			for _, system := range in.Inventory {
				d.Processors[system] = ExternalErasureReceipt{System: system, State: "pending"}
			}
			d.ExternalState = "pending"
		}
		if in.Receipt != nil {
			v := *in.Receipt
			if _, ok := d.Processors[v.System]; !ok {
				return Invalid("processor is not in the reviewed inventory")
			}
			d.Processors[v.System] = v
		}
		complete := len(d.Processors) > 0
		for _, v := range d.Processors {
			if v.State != "completed" && v.State != "restricted_backup" && v.State != "not_applicable" {
				complete = false
			}
		}
		if complete {
			d.ExternalState = "completed"
		}
		r.State = "processing"
		if e = save(tx, DataRequests, &r, d, r.OwnerID, "data_request.processor_receipt"); e != nil {
			return e
		}
		return audit(tx, operator, "data_request.processor_receipt", DataRequests, id)
	})
}

// Converts only historical erasure events. They become ordinary actionable
// events; conversion does not acknowledge cleanup or create new user consent.
func (s *Service) AdaptLegacyErasure(ctx context.Context, operator string, after int64) (next int64, count int, err error) {
	if operator == "" {
		return 0, 0, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		rows, e := tx.List(Outbox, Filter{After: after, State: "pending", Limit: 100})
		if e != nil {
			return e
		}
		for _, event := range rows {
			next = event.ID
			v, e := Decode[struct {
				Topic  string `json:"topic"`
				UserID int64  `json:"user_id"`
				Schema int    `json:"schema_version"`
			}](event)
			if e != nil {
				return e
			}
			if v.Topic != "account.erasure_requested" || v.Schema != 0 || v.UserID <= 0 {
				continue
			}
			key := fmt.Sprintf("legacy-erasure:%d", event.ID)
			request, e := tx.ByKey(DataRequests, key)
			if e == ErrNotFound {
				d := DataRequest{Type: "close_account", Fields: []string{"bio", "avatar", "nickname"}, Items: []DataItem{}, Holds: []DataHold{}, ExternalState: "pending_inventory", QueryExpiresAt: s.Now()}
				for _, kind := range []Kind{Entries, Pets, Cases, Offers, Drafts, Media, Subscriptions, Qualifications, Guardians, Identities, Preferences, Expressions, PushDevices, Sessions, AuthChallenges} {
					items, e := allRecords(tx, kind, Filter{OwnerID: v.UserID})
					if e != nil {
						return e
					}
					for _, item := range items {
						d.Items = append(d.Items, DataItem{kind, item.ID, "pending"})
					}
				}
				request = Record{OwnerID: v.UserID, Key: key, State: "processing"}
				if e = SetBody(&request, d); e != nil {
					return e
				}
				if e = tx.Save(DataRequests, &request); e != nil {
					return e
				}
			} else if e != nil {
				return e
			}
			if e = SetBody(&event, Event{1, "data_request.submitted", DataRequests, request.ID, request.Version, v.UserID}); e != nil {
				return e
			}
			if e = tx.Save(Outbox, &event); e != nil {
				return e
			}
			count++
			if e = audit(tx, operator, "legacy_erasure.adapted", DataRequests, request.ID); e != nil {
				return e
			}
		}
		return nil
	})
	return
}

func (s *Service) RevokeDataRequestAccess(ctx context.Context, id int64, token string) error {
	if len(token) < 40 || len(token) > 100 {
		return ErrForbidden
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(DataRequests, id)
		if e != nil {
			return e
		}
		d, e := Decode[DataRequest](r)
		if e != nil {
			return e
		}
		if d.QueryHash != Fingerprint(token) || !d.QueryExpiresAt.After(s.Now()) {
			return ErrForbidden
		}
		d.QueryHash = ""
		d.QueryExpiresAt = time.Time{}
		if e = SetBody(&r, d); e != nil {
			return e
		}
		return tx.Save(DataRequests, &r)
	})
}
