package mobile

import (
	"context"
	"errors"
	"fmt"
	"time"
)

// Preparing a snapshot is an explicit POST. Reading the inbox never advances a
// receipt or creates a snapshot. Clients receive counts, never editable cursors.
type InboxSnapshotInput struct {
	CommandMeta
}

type InboxReadAllInput struct {
	CommandMeta
	SnapshotID int64 `json:"snapshot_id"`
	Version    int64 `json:"version"`
}

type InboxReadSnapshot struct {
	SnapshotID int64     `json:"snapshot_id"`
	Version    int64     `json:"version"`
	Count      int       `json:"count"`
	MutedCount int       `json:"muted_count"`
	ExpiresAt  time.Time `json:"expires_at"`
}

type InboxReadAllResult struct {
	SnapshotID   int64 `json:"snapshot_id"`
	AppliedCount int   `json:"applied_count"`
	SkippedCount int   `json:"skipped_count"`
}

type inboxChatPosition struct {
	Kind    Kind  `json:"kind"`
	ID      int64 `json:"id"`
	Through int64 `json:"through"`
	Count   int   `json:"count"`
}

type inboxNotificationPosition struct {
	ID      int64 `json:"id"`
	Counted bool  `json:"counted"`
}

type inboxSnapshotBody struct {
	Type          string                      `json:"type"`
	Count         int                         `json:"count"`
	MutedCount    int                         `json:"muted_count"`
	ExpiresAt     time.Time                   `json:"expires_at"`
	Chats         []inboxChatPosition         `json:"chats"`
	Notifications []inboxNotificationPosition `json:"notifications"`
	Result        *InboxReadAllResult         `json:"result,omitempty"`
}

// Both snapshot creation and confirmation use the existing durable operation
// journal. Replays check the current actor/session and owner, and return only a
// minimal count receipt; no private chat contents escape a later ACL change.
func (s *Service) inboxCommand(ctx context.Context, actor int64, name string, input any, apply func(Tx) (Record, error)) (out Record, err error) {
	key, hash, err := commandKey(input)
	if err != nil {
		return out, err
	}
	if header := CommandID(ctx); header != "" && header != key {
		return out, Denied("idempotency_conflict")
	}
	err = s.actorUpdate(ctx, actor, func(tx Tx) error {
		operationKey := fmt.Sprintf("%d:%s", actor, key)
		operation, e := tx.ByKey(Operations, operationKey)
		if e == nil {
			v, e := Decode[Operation](operation)
			if e != nil {
				return e
			}
			if v.Command != name || v.PayloadHash != hash || v.ResultKind != Receipts {
				return Denied("idempotency_conflict")
			}
			out, e = tx.Get(Receipts, v.ResultID)
			if e != nil {
				return e
			}
			if out.OwnerID != actor {
				return ErrForbidden
			}
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		out, e = apply(tx)
		if e != nil {
			return e
		}
		operation = Record{OwnerID: actor, Key: operationKey, State: "committed"}
		if e = SetBody(&operation, Operation{key, name, out.ID, hash, Receipts, out.ID, out.Version, "committed"}); e != nil {
			return e
		}
		return tx.Save(Operations, &operation)
	})
	return out, err
}

func inboxPosition(tx Tx, kind Kind, id, actor int64) (int64, error) {
	r, e := tx.ByKey(Receipts, fmt.Sprintf("%s:%d:%d", kind, id, actor))
	if errors.Is(e, ErrNotFound) {
		return 0, nil
	}
	if e != nil {
		return 0, e
	}
	v, e := Decode[Receipt](r)
	return v.ThroughMessageID, e
}

func (s *Service) PrepareInboxReadAll(ctx context.Context, actor int64, in InboxSnapshotInput) (out InboxReadSnapshot, err error) {
	r, err := s.inboxCommand(ctx, actor, "inbox.snapshot", in, func(tx Tx) (Record, error) {
		var result Record
		body := inboxSnapshotBody{Type: "inbox_read_snapshot", ExpiresAt: s.Now().Add(10 * time.Minute)}
		// Bound outstanding confirmation windows without changing GET behaviour.
		open, e := allRecords(tx, Receipts, Filter{OwnerID: actor, KeyPrefix: fmt.Sprintf("inbox-snapshot:%d:", actor), State: "prepared"})
		if e != nil {
			return result, e
		}
		active := 0
		for _, row := range open {
			previous, e := Decode[inboxSnapshotBody](row)
			if e != nil {
				return result, e
			}
			if previous.ExpiresAt.After(s.Now()) {
				active++
			}
		}
		if active >= 20 {
			return result, ErrRateLimited
		}
		for _, kind := range []Kind{Conversations, Groups} {
			parents, e := allRecords(tx, kind, Filter{ParticipantID: actor})
			if e != nil {
				return result, e
			}
			for _, parent := range parents {
				ok, e := s.canRead(tx, kind, parent, actor)
				if e != nil {
					return result, e
				}
				if !ok || kind == Conversations && parent.State == "declined" {
					continue
				}
				through, e := inboxPosition(tx, kind, parent.ID, actor)
				if e != nil {
					return result, e
				}
				child := Messages
				if kind == Groups {
					child = GroupMessages
				}
				messages, e := allRecords(tx, child, Filter{RefID: parent.ID, After: through})
				if e != nil {
					return result, e
				}
				position := inboxChatPosition{Kind: kind, ID: parent.ID}
				for _, message := range messages {
					if message.OwnerID != actor {
						position.Count++
						if message.ID > position.Through {
							position.Through = message.ID
						}
					}
				}
				if position.Count == 0 {
					continue
				}
				body.Count += position.Count
				body.Chats = append(body.Chats, position)
				if kind == Groups {
					group, e := Decode[Group](parent)
					if e != nil {
						return result, e
					}
					if Has(group.Muted, actor) {
						body.MutedCount += position.Count
					}
				}
			}
		}
		notifications, e := allRecords(tx, Notifications, Filter{OwnerID: actor, State: "unread"})
		if e != nil {
			return result, e
		}
		for _, row := range notifications {
			n, e := Decode[Notification](row)
			if e != nil {
				return result, e
			}
			// Push/onsite chat copies are consumed too, but do not count twice.
			category := notificationCategory(row, n)
			counted := category != "direct_messages" && category != "groups"
			body.Notifications = append(body.Notifications, inboxNotificationPosition{row.ID, counted})
			if counted {
				body.Count++
			}
		}
		result = Record{OwnerID: actor, Key: fmt.Sprintf("inbox-snapshot:%d:%s", actor, in.ClientRequestID), State: "prepared"}
		if e = SetBody(&result, body); e != nil {
			return result, e
		}
		e = tx.Save(Receipts, &result)
		return result, e
	})
	if err != nil {
		return out, err
	}
	body, err := Decode[inboxSnapshotBody](r)
	if err != nil {
		return out, err
	}
	// Snapshot version is immutable for confirmation even after it is consumed.
	return InboxReadSnapshot{r.ID, 1, body.Count, body.MutedCount, body.ExpiresAt}, nil
}

func (s *Service) ConfirmInboxReadAll(ctx context.Context, actor int64, in InboxReadAllInput) (out InboxReadAllResult, err error) {
	if in.SnapshotID <= 0 || in.Version != 1 {
		return out, Invalid("valid snapshot and version required")
	}
	r, err := s.inboxCommand(ctx, actor, "inbox.read_all", in, func(tx Tx) (Record, error) {
		snapshot, e := tx.Get(Receipts, in.SnapshotID)
		if e != nil {
			return snapshot, e
		}
		if snapshot.OwnerID != actor {
			return snapshot, ErrForbidden
		}
		body, e := Decode[inboxSnapshotBody](snapshot)
		if e != nil {
			return snapshot, e
		}
		if body.Type != "inbox_read_snapshot" {
			return snapshot, ErrForbidden
		}
		if body.Result != nil {
			return snapshot, nil
		}
		if !body.ExpiresAt.After(s.Now()) {
			return snapshot, Denied("inbox_snapshot_expired")
		}
		result := InboxReadAllResult{SnapshotID: snapshot.ID}
		for _, position := range body.Chats {
			applied, skipped, e := s.applyInboxPosition(tx, actor, position)
			if e != nil {
				return snapshot, e
			}
			result.AppliedCount += applied
			result.SkippedCount += skipped
		}
		for _, position := range body.Notifications {
			notification, e := tx.Get(Notifications, position.ID)
			if errors.Is(e, ErrNotFound) {
				if position.Counted {
					result.SkippedCount++
				}
				continue
			}
			if e != nil {
				return snapshot, e
			}
			if notification.OwnerID != actor {
				return snapshot, ErrForbidden
			}
			if notification.State == "unread" {
				n, e := Decode[Notification](notification)
				if e != nil {
					return snapshot, e
				}
				n.Read = true
				notification.State = "read"
				if e = save(tx, Notifications, &notification, n, actor, "notification.read"); e != nil {
					return snapshot, e
				}
				if position.Counted {
					result.AppliedCount++
				}
			}
		}
		body.Result = &result
		snapshot.State = "consumed"
		if e = SetBody(&snapshot, body); e != nil {
			return snapshot, e
		}
		e = tx.Save(Receipts, &snapshot)
		return snapshot, e
	})
	if err != nil {
		return out, err
	}
	body, err := Decode[inboxSnapshotBody](r)
	if err != nil {
		return out, err
	}
	if body.Result == nil {
		return out, ErrConflict
	}
	return *body.Result, nil
}

func (s *Service) applyInboxPosition(tx Tx, actor int64, position inboxChatPosition) (applied, skipped int, err error) {
	parent, err := tx.Get(position.Kind, position.ID)
	if errors.Is(err, ErrNotFound) {
		return 0, position.Count, nil
	}
	if err != nil {
		return 0, 0, err
	}
	ok, err := s.canRead(tx, position.Kind, parent, actor)
	if err != nil {
		return 0, 0, err
	}
	if !ok || position.Kind == Conversations && parent.State == "declined" {
		return 0, position.Count, nil
	}
	through, err := inboxPosition(tx, position.Kind, position.ID, actor)
	if err != nil || through >= position.Through {
		return 0, 0, err
	}
	child := Messages
	if position.Kind == Groups {
		child = GroupMessages
	}
	rows, err := allRecords(tx, child, Filter{RefID: position.ID, After: through})
	if err != nil {
		return 0, 0, err
	}
	for _, row := range rows {
		if row.ID <= position.Through && row.OwnerID != actor {
			applied++
		}
	}
	key := fmt.Sprintf("%s:%d:%d", position.Kind, position.ID, actor)
	r, err := tx.ByKey(Receipts, key)
	if errors.Is(err, ErrNotFound) {
		r = Record{OwnerID: actor, Key: key, RefID: position.ID, State: "active"}
	} else if err != nil {
		return 0, 0, err
	}
	err = save(tx, Receipts, &r, Receipt{position.Kind, position.Through}, actor, "messages.read")
	return applied, 0, err
}
