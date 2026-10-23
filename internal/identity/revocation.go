package identity

import (
	"context"
	"fmt"
	"kitty-circle/internal/mobile"
	"time"
)

type Revoker interface {
	Revoke(context.Context, Subject) error
}
type Revocation struct {
	Topic   string  `json:"topic"`
	Subject Subject `json:"subject"`
}

func queueRevocation(tx mobile.Tx, link mobile.Record) error {
	v, e := mobile.Decode[Subject](link)
	if e != nil {
		return e
	}
	if v.Secret == "" {
		return nil
	}
	r := mobile.Record{OwnerID: link.OwnerID, RefID: link.ID, State: "pending", Key: fmt.Sprintf("revoke:%d:%d", link.ID, link.Version)}
	return set(tx, mobile.Outbox, &r, Revocation{Topic: "identity.revocation_requested", Subject: v})
}

// ProcessRevocations is called by the protected operations endpoint or an
// operator scheduler. Provider revocation is idempotent and outside SQL locks;
// a failed call remains pending and a concurrent acknowledgement uses CAS.
func (s *Service) ProcessRevocations(ctx context.Context, after int64) (next int64, processed int, err error) {
	worker := mobile.OperatorID(ctx)
	if worker == "" {
		return 0, 0, mobile.ErrForbidden
	}
	var rows []mobile.Record
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		var e error
		rows, e = tx.List(mobile.Outbox, mobile.Filter{KeyPrefix: "revoke:", After: after, State: "pending", Limit: 100})
		return e
	})
	if err != nil {
		return
	}
	for _, r := range rows {
		next = r.ID
		v, e := mobile.Decode[Revocation](r)
		if e != nil {
			err = e
			return
		}
		if v.Topic != "identity.revocation_requested" {
			continue
		}
		var lease mobile.Record
		claimed := false
		err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
			current, e := tx.Get(mobile.Outbox, r.ID)
			if e != nil {
				return e
			}
			if current.State != "pending" || current.Version != r.Version {
				return nil
			}
			key := fmt.Sprintf("%d:provider_revocations", r.ID)
			lease, e = tx.ByKey(mobile.Deliveries, key)
			d := mobile.Delivery{EventID: r.ID, Consumer: "provider_revocations"}
			if e == mobile.ErrNotFound {
				lease = mobile.Record{Key: key, RefID: r.ID, State: "pending"}
			} else if e != nil {
				return e
			} else {
				d, e = mobile.Decode[mobile.Delivery](lease)
				if e != nil {
					return e
				}
				if lease.State == "acknowledged" || d.LeaseUntil.After(s.Now()) || d.NextRetry.After(s.Now()) {
					return nil
				}
			}
			d.LeaseOwner, d.LeaseUntil, d.Attempts = worker, s.Now().Add(time.Minute), d.Attempts+1
			lease.State = "leased"
			if e = set(tx, mobile.Deliveries, &lease, d); e != nil {
				return e
			}
			claimed = true
			return nil
		})
		if err != nil {
			return
		}
		if !claimed {
			continue
		}
		provider, ok := s.Providers[v.Subject.Provider].(Revoker)
		if !ok {
			err = mobile.ErrUnavailable
			return
		}
		if e = provider.Revoke(ctx, v.Subject); e != nil {
			err = e
			return
		}
		err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
			currentLease, e := tx.Get(mobile.Deliveries, lease.ID)
			if e != nil {
				return e
			}
			d, e := mobile.Decode[mobile.Delivery](currentLease)
			if e != nil {
				return e
			}
			if currentLease.Version != lease.Version || d.LeaseOwner != worker || !d.LeaseUntil.After(s.Now()) {
				return mobile.ErrConflict
			}
			current, e := tx.Get(mobile.Outbox, r.ID)
			if e != nil {
				return e
			}
			if current.State != "pending" {
				return nil
			}
			if current.Version != r.Version {
				return mobile.ErrConflict
			}
			current.State = "delivered"
			current.Data = []byte(`{"topic":"identity.revocation_completed"}`)
			link, e := tx.Get(mobile.Identities, r.RefID)
			if e != nil {
				return e
			}
			subject, e := mobile.Decode[Subject](link)
			if e != nil {
				return e
			}
			if link.State == "revoked" && subject.Secret == v.Subject.Secret {
				subject.Secret = ""
				if e = set(tx, mobile.Identities, &link, subject); e != nil {
					return e
				}
			}
			if e = tx.Save(mobile.Outbox, &current); e != nil {
				return e
			}
			currentLease.State = "acknowledged"
			d.ResultRef = "provider_revocation_accepted"
			d.LeaseUntil = time.Time{}
			return set(tx, mobile.Deliveries, &currentLease, d)
		})
		if err != nil {
			return
		}
		processed++
	}
	if len(rows) < 100 {
		next = 0
	}
	return
}
