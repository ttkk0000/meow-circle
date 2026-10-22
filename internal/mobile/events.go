package mobile

// Event is the extraction seam for future service boundaries. It is committed
// with the aggregate; delivery is at least once and consumers deduplicate by ID.
// A future broker adapter replaces delivery, not the domain transition.
type Event struct {
	SchemaVersion    int    `json:"schema_version"`
	Topic            string `json:"topic"`
	AggregateKind    Kind   `json:"aggregate_kind"`
	AggregateID      int64  `json:"aggregate_id"`
	AggregateVersion int64  `json:"aggregate_version"`
	ActorID          int64  `json:"actor_id"`
}

func emit(tx Tx, kind Kind, r Record, actor int64, topic string) error {
	e := Record{OwnerID: actor, RefID: r.ID, State: "pending"}
	if err := SetBody(&e, Event{SchemaVersion: 1, Topic: topic, AggregateKind: kind,
		AggregateID: r.ID, AggregateVersion: r.Version, ActorID: actor}); err != nil {
		return err
	}
	return tx.Save(Outbox, &e)
}
