package mobile

import (
	"context"
	"time"
)

type EventEnvelope struct {
	ID        int64     `json:"id"`
	Version   int64     `json:"version"`
	CreatedAt time.Time `json:"created_at"`
	Event     Event     `json:"event"`
}
type EventPage struct {
	Items []EventEnvelope `json:"items"`
	Next  int64           `json:"next_cursor"`
}

// PendingEvents is a port for a local scheduler today and a broker publisher
// after extraction. Credentials and provider-revocation jobs are not projected.
func (s *Service) PendingEvents(ctx context.Context, after int64) (out EventPage, err error) {
	out.Items = []EventEnvelope{}
	err = s.Repo.View(ctx, func(tx Tx) error {
		rows, e := tx.List(Outbox, Filter{After: after, State: "pending", Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			out.Next = r.ID
			event, e := Decode[Event](r)
			if e != nil {
				return e
			}
			if event.SchemaVersion != 1 || !event.AggregateKind.Valid() {
				continue
			}
			out.Items = append(out.Items, EventEnvelope{r.ID, r.Version, r.CreatedAt, event})
		}
		return nil
	})
	return
}
func (s *Service) AcknowledgeEvent(ctx context.Context, id, version int64) error {
	return s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Outbox, id)
		if e != nil {
			return e
		}
		if e = expected(r, version); e != nil {
			return e
		}
		event, e := Decode[Event](r)
		if e != nil {
			return e
		}
		if event.SchemaVersion != 1 || !event.AggregateKind.Valid() {
			return ErrForbidden
		}
		if r.State != "pending" {
			return ErrConflict
		}
		r.State = "acknowledged"
		return tx.Save(Outbox, &r)
	})
}
