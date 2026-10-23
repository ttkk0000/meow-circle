package mobile

import (
	"context"
	"fmt"
	"time"
)

type Delivery struct {
	Cursor     int64     `json:"cursor"`
	EventID    int64     `json:"event_id"`
	Consumer   string    `json:"consumer"`
	LeaseOwner string    `json:"lease_owner"`
	LeaseUntil time.Time `json:"lease_until"`
	Attempts   int       `json:"attempts"`
	NextRetry  time.Time `json:"next_retry"`
	ResultRef  string    `json:"result_ref,omitempty"`
}
type DeliveryClaim struct {
	Consumer string `json:"consumer"`
	Limit    int    `json:"limit"`
}
type DeliveryWork struct {
	Delivery Record        `json:"delivery"`
	Event    EventEnvelope `json:"event"`
}
type DeliveryResult struct {
	Cursor            int64  `json:"cursor"`
	Consumer          string `json:"consumer"`
	Version           int64  `json:"version"`
	Success           bool   `json:"success"`
	ResultRef         string `json:"result_ref"`
	RetryAfterSeconds int    `json:"retry_after_seconds"`
}

func ValidConsumer(name string) bool {
	return HasString([]string{"push", "nearby_alerts", "privacy_cleanup", "search_projection", "audit_projection", "provider_revocations"}, name)
}
func consumerEvent(consumer string, event Event) bool {
	switch consumer {
	case "push":
		return event.AggregateKind == Notifications && (event.Topic == "notification.created" || event.Topic == "nearby.alert_created")
	case "nearby_alerts":
		return event.AggregateKind == Cases && event.Topic == "case.saved"
	case "privacy_cleanup":
		return event.AggregateKind == DataRequests && HasString([]string{"data_request.submitted", "data_request.hold_updated", "data_request.processor_receipt", "data_request.legacy_adapted"}, event.Topic)
	case "search_projection":
		return publicKind(event.AggregateKind)
	case "audit_projection":
		return true
	default:
		return false
	}
}

func (s *Service) ClaimDeliveries(ctx context.Context, worker string, in DeliveryClaim) (out []DeliveryWork, err error) {
	if worker == "" || !ValidConsumer(in.Consumer) || in.Limit < 1 || in.Limit > 50 {
		return nil, Invalid("invalid delivery claim")
	}
	out = []DeliveryWork{}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		// Restart from zero so an expired lease is revisited. Read at most
		// 100 envelopes at a time instead of retaining the full event history.
		after := int64(0)
		for {
			if e := ctx.Err(); e != nil {
				return e
			}
			events, e := tx.List(Outbox, Filter{State: "pending", After: after, Limit: 100})
			if e != nil {
				return e
			}
			for _, event := range events {
				after = event.ID
				v, e := Decode[Event](event)
				if e != nil {
					return e
				}
				if v.SchemaVersion != 1 || !v.AggregateKind.Valid() {
					continue
				}
				if !consumerEvent(in.Consumer, v) {
					continue
				}
				if in.Consumer == "push" {
					notice, e := tx.Get(Notifications, v.AggregateID)
					if e != nil && e != ErrNotFound {
						return e
					}
					if e == nil {
						n, e := Decode[Notification](notice)
						if e != nil {
							return e
						}
						if n.PushState == "queued" && n.DeliverAfter != nil && n.DeliverAfter.After(s.Now()) {
							continue
						}
					}
				}
				key := fmt.Sprintf("%d:%s", event.ID, in.Consumer)
				r, e := tx.ByKey(Deliveries, key)
				d := Delivery{EventID: event.ID, Consumer: in.Consumer}
				if e == ErrNotFound {
					r = Record{Key: key, RefID: event.ID, State: "pending"}
				} else if e != nil {
					return e
				} else {
					d, e = Decode[Delivery](r)
					if e != nil {
						return e
					}
					if r.State == "acknowledged" || d.LeaseUntil.After(s.Now()) || d.NextRetry.After(s.Now()) {
						continue
					}
				}
				d.LeaseOwner, d.LeaseUntil, d.Attempts = worker, s.Now().Add(time.Minute), d.Attempts+1
				r.State = "leased"
				if e = SetBody(&r, d); e != nil {
					return e
				}
				if e = tx.Save(Deliveries, &r); e != nil {
					return e
				}
				out = append(out, DeliveryWork{r, EventEnvelope{event.ID, event.Version, event.CreatedAt, v}})
				if len(out) >= in.Limit {
					return nil
				}
			}
			if len(events) < 100 {
				return nil
			}
		}
	})
	return
}
func (s *Service) CompleteDelivery(ctx context.Context, worker string, id int64, in DeliveryResult) error {
	if worker == "" || in.Cursor < 0 || in.Cursor > 0 && (in.Consumer != "nearby_alerts" || in.Success) || in.RetryAfterSeconds < 0 || in.RetryAfterSeconds > 86400 || textValid(in.ResultRef, 160, in.Success) != nil {
		return Invalid("invalid delivery result")
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Deliveries, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		d, e := Decode[Delivery](r)
		if e != nil {
			return e
		}
		if r.State != "leased" || d.Consumer != in.Consumer || d.LeaseOwner != worker || !d.LeaseUntil.After(s.Now()) {
			return ErrConflict
		}
		d.LeaseUntil = time.Time{}
		d.Cursor = in.Cursor
		d.ResultRef = in.ResultRef
		r.State = "acknowledged"
		if !in.Success {
			r.State = "pending"
			wait := in.RetryAfterSeconds
			if wait < 5 {
				wait = 5
			}
			d.NextRetry = s.Now().Add(time.Duration(wait) * time.Second)
		}
		if e = SetBody(&r, d); e != nil {
			return e
		}
		if e = tx.Save(Deliveries, &r); e != nil {
			return e
		}
		return audit(tx, worker, "delivery."+r.State, Deliveries, r.ID)
	})
}
