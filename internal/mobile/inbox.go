package mobile

import (
	"context"
	"errors"
	"fmt"
)

type Receipt struct {
	TargetKind       Kind  `json:"target_kind"`
	ThroughMessageID int64 `json:"through_message_id"`
}

func (s *Service) ReadMessages(ctx context.Context, actor, id int64, group bool, through int64) (out Record, err error) {
	err = s.mutate(ctx, actor, "messages.read", id, struct {
		Group   bool  `json:"group"`
		Through int64 `json:"through_message_id"`
	}{group, through}, Receipts, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		k, child := Conversations, Messages
		if group {
			k, child = Groups, GroupMessages
		}
		parent, e := tx.Get(k, id)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, k, parent, actor)
		if e != nil {
			return e
		}
		if !ok {
			return ErrForbidden
		}
		message, e := tx.Get(child, through)
		if e != nil {
			return e
		}
		if message.RefID != id {
			return Invalid("message belongs to another conversation")
		}
		key := fmt.Sprintf("%s:%d:%d", k, id, actor)
		r, e := tx.ByKey(Receipts, key)
		if errors.Is(e, ErrNotFound) {
			r = Record{OwnerID: actor, Key: key, RefID: id, State: "active"}
		} else if e != nil {
			return e
		} else {
			v, e := Decode[Receipt](r)
			if e != nil {
				return e
			}
			if v.ThroughMessageID >= through {
				out = r
				return nil
			}
		}
		if e = save(tx, Receipts, &r, Receipt{k, through}, actor, "messages.read"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type Unread struct {
	GlobalTotal       int             `json:"global_total"`
	Pending           map[string]int  `json:"pending"`
	Direct            map[int64]int   `json:"direct"`
	Groups            map[int64]int   `json:"groups"`
	MutedGroups       map[int64]int   `json:"muted_groups"`
	DirectFirstUnread map[int64]int64 `json:"direct_first_unread"`
	GroupFirstUnread  map[int64]int64 `json:"group_first_unread"`
	Notifications     map[string]int  `json:"notifications"`
	MessageRequests   int             `json:"message_requests"`
}

func (s *Service) Unread(ctx context.Context, actor int64) (out Unread, err error) {
	out = Unread{Direct: map[int64]int{}, Groups: map[int64]int{}, MutedGroups: map[int64]int{}, Pending: map[string]int{}, DirectFirstUnread: map[int64]int64{}, GroupFirstUnread: map[int64]int64{}, Notifications: map[string]int{}}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if e := s.checkActor(ctx, tx, actor); e != nil {
			return e
		}
		for _, k := range []Kind{Conversations, Groups} {
			parents, e := allRecords(tx, k, Filter{ParticipantID: actor})
			if e != nil {
				return e
			}
			for _, p := range parents {
				ok, e := s.canRead(tx, k, p, actor)
				if e != nil {
					return e
				}
				if !ok {
					continue
				}
				if k == Conversations && p.State == "declined" {
					continue
				}
				through := int64(0)
				receipt, e := tx.ByKey(Receipts, fmt.Sprintf("%s:%d:%d", k, p.ID, actor))
				if e == nil {
					v, e := Decode[Receipt](receipt)
					if e != nil {
						return e
					}
					through = v.ThroughMessageID
				} else if !errors.Is(e, ErrNotFound) {
					return e
				}
				child := Messages
				if k == Groups {
					child = GroupMessages
				}
				messages, e := allRecords(tx, child, Filter{RefID: p.ID, After: through})
				if e != nil {
					return e
				}
				count := 0
				first := int64(0)
				for _, m := range messages {
					if m.OwnerID != actor {
						count++
						if first == 0 || m.ID < first {
							first = m.ID
						}
					}
				}
				if k == Groups {
					out.Groups[p.ID] = count
					group, e := Decode[Group](p)
					if e != nil {
						return e
					}
					if Has(group.Muted, actor) {
						out.MutedGroups[p.ID] = count
					}
					if group.PendingTransferTo == actor && group.TransferExpiresAt != nil && group.TransferExpiresAt.After(s.Now()) {
						out.Pending["group_transfers"]++
					}
					// 免打扰排除全局数仍是设计建议，保持现行提醒口径。
					out.GlobalTotal += count
					if first > 0 {
						out.GroupFirstUnread[p.ID] = first
					}
				} else {
					out.Direct[p.ID] = count
					out.GlobalTotal += count
					if first > 0 {
						out.DirectFirstUnread[p.ID] = first
					}
					if p.State == "pending" && p.OwnerID != actor {
						out.MessageRequests += count
						out.Pending["message_requests"]++
					}
				}
			}
		}
		notifications, e := allRecords(tx, Notifications, Filter{OwnerID: actor, State: "unread"})
		if e != nil {
			return e
		}
		for _, r := range notifications {
			v, e := Decode[Notification](r)
			if e != nil {
				return e
			}
			category := notificationCategory(r, v)
			out.Notifications[category]++
			if category != "direct_messages" && category != "groups" {
				out.GlobalTotal++
			}
		}
		for _, kind := range []Kind{Adoptions, Appointments, Invitations, Claims, Reviews, Guardians, Friendships} {
			rows, e := allRecords(tx, kind, Filter{ParticipantID: actor})
			if e != nil {
				return e
			}
			for _, row := range rows {
				actionable := false
				switch kind {
				case Friendships:
					friend, e := Decode[Friend](row)
					if e != nil {
						return e
					}
					actionable = row.State == "pending" && friend.RecipientID == actor
					if actionable {
						if e = s.active(tx, friend.RequesterID); invitationAccessEnded(e) {
							actionable = false
						} else if e != nil {
							return e
						}
					}
				case Adoptions:
					a, e := Decode[Adoption](row)
					if e != nil {
						return e
					}
					actionable = (a.PublisherID == actor && HasString([]string{"submitted", "supplemented", "return_requested"}, row.State)) || (a.ApplicantID == actor && row.State == "needs_info")
					for _, v := range a.Contracts {
						if v.Status == "pending" && !Has(v.AcceptedBy, actor) {
							actionable = true
						}
					}
				case Appointments:
					a, e := Decode[Appointment](row)
					if e != nil {
						return e
					}
					for _, v := range a.Versions {
						if v.Number == a.PendingVersion && v.Status == "pending" && !Has(v.Confirmations, actor) {
							actionable = true
						}
					}
				case Invitations:
					invitation, e := Decode[Invitation](row)
					if e != nil {
						return e
					}
					actionable = row.State == "pending" && invitation.RecipientID == actor && invitation.ExpiresAt.After(s.Now())
				case Claims:
					actionable = (row.OwnerID == actor && row.State == "needs_info") || (row.OwnerID != actor && row.State == "submitted")
				case Reviews:
					actionable = row.OwnerID == actor && row.State == "needs_info"
				case Guardians:
					actionable = row.OwnerID == actor && row.State == "verified_pending_consent"
				}
				if actionable {
					out.Pending[string(kind)]++
				}
			}
		}
		return nil
	})
	return
}
func allRecords(tx Tx, k Kind, f Filter) ([]Record, error) {
	out := []Record{}
	f.Limit = 500
	for {
		rows, e := tx.List(k, f)
		if e != nil {
			return nil, e
		}
		out = append(out, rows...)
		if len(rows) < 500 {
			return out, nil
		}
		f.After = rows[len(rows)-1].ID
	}
}
