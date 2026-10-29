package mobile

import (
	"context"
	"fmt"
	"time"
)

type PushDeviceInput struct {
	Version    int64  `json:"version"`
	Platform   string `json:"platform"`
	Token      string `json:"token"`
	Permission string `json:"permission"`
	Enabled    bool   `json:"enabled"`
}
type PushDeviceReceipt struct {
	ID      int64  `json:"id"`
	Version int64  `json:"version"`
	State   string `json:"state"`
}

type PushDeviceLookup struct {
	Platform string `json:"platform"`
	Token    string `json:"token"`
}

// 只返回当前账号绑定的版本，不回显Token；用于进程重启和结果不确定后的核对。
func (s *Service) PushDeviceStatus(ctx context.Context, actor int64, in PushDeviceLookup) (out PushDeviceReceipt, err error) {
	if !HasString([]string{"android", "ios"}, in.Platform) || len(in.Token) < 16 || len(in.Token) > 4096 {
		return out, Invalid("invalid push device")
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, e := tx.ByKey(PushDevices, in.Platform+":"+Fingerprint(in.Token))
		if e == ErrNotFound {
			return nil
		}
		if e != nil {
			return e
		}
		if r.OwnerID != actor {
			if r.State == "disabled" {
				return nil
			}
			return Denied("push_device_owned_by_another_account")
		}
		out = PushDeviceReceipt{r.ID, r.Version, r.State}
		return nil
	})
	return
}

func (s *Service) RegisterPushDevice(ctx context.Context, actor int64, in PushDeviceInput) (out PushDeviceReceipt, err error) {
	if !HasString([]string{"android", "ios"}, in.Platform) || len(in.Token) < 16 || len(in.Token) > 4096 || !HasString([]string{"granted", "denied", "unknown"}, in.Permission) {
		return out, Invalid("invalid push device")
	}
	err = s.actorUpdate(ctx, actor, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		key := in.Platform + ":" + Fingerprint(in.Token)
		r, e := tx.ByKey(PushDevices, key)
		if e == nil && in.Version == 0 && r.State == "disabled" {
			// Retain an opaque disabled record for old cleanup requests. A new
			// registration gets its own ID and cannot change that record's owner.
			r.Key = fmt.Sprintf("%s:disabled:%d", key, r.ID)
			if e = SetBody(&r, pushBinding{}); e != nil {
				return e
			}
			if e = tx.Save(PushDevices, &r); e != nil {
				return e
			}
			e = ErrNotFound
		}
		if e == ErrNotFound {
			if in.Version != 0 {
				return ErrConflict
			}
			r = Record{OwnerID: actor, Key: key}
		} else if e != nil {
			return e
		} else {
			if r.OwnerID != actor {
				return Denied("push_device_owned_by_another_account")
			}
			current, e := Decode[PushDeviceInput](r)
			if e != nil {
				return e
			}
			matchingRegistration := in.Version == 0 && current.Token == in.Token && current.Platform == in.Platform && current.Permission == in.Permission && current.Enabled == in.Enabled
			if !matchingRegistration {
				if e = expected(r, in.Version); e != nil {
					return e
				}
			}
		}
		r.State = "disabled"
		if in.Enabled && in.Permission == "granted" {
			r.State = "enabled"
		}
		stored := in
		if r.State == "disabled" {
			stored.Token = ""
		}
		if e = SetBody(&r, pushBinding{stored, SessionKey(ctx)}); e != nil {
			return e
		}
		if e = tx.Save(PushDevices, &r); e != nil {
			return e
		}
		if r.State == "enabled" && SessionKey(ctx) != "" {
			// 同一会话的Token轮换原子撤销旧通道，避免重复发送；不影响其它设备。
			rows, err := allRecords(tx, PushDevices, Filter{OwnerID: actor, State: "enabled"})
			if err != nil {
				return err
			}
			for _, previous := range rows {
				if previous.ID == r.ID {
					continue
				}
				binding, err := Decode[pushBinding](previous)
				if err != nil {
					return err
				}
				if binding.Platform != in.Platform || binding.SessionKey != SessionKey(ctx) {
					continue
				}
				binding.Token, binding.Enabled = "", false
				previous.State = "disabled"
				if err = SetBody(&previous, binding); err != nil {
					return err
				}
				if err = tx.Save(PushDevices, &previous); err != nil {
					return err
				}
			}
		}
		out = PushDeviceReceipt{r.ID, r.Version, r.State}
		return nil
	})
	return
}

type PushPayload struct {
	NotificationID int64  `json:"notification_id"`
	Platform       string `json:"platform"`
	Token          string `json:"token"`
	Text           string `json:"text"`
	IdempotencyKey string `json:"idempotency_key"`
}
type PushAcceptance struct {
	MessageID string `json:"message_id"`
	Accepted  bool   `json:"accepted"`
}
type PushLookup struct {
	State   string         `json:"state"`
	Receipt PushAcceptance `json:"receipt"`
}
type PushSender interface {
	Send(context.Context, PushPayload) (PushAcceptance, error)
	Lookup(context.Context, string) (PushLookup, error)
}
type PushWork struct {
	NotificationID int64
	Version        int64
	Payload        PushPayload
}

// Provider acceptance is not device delivery. Providers deduplicate this key.
func (s *Service) PushWork(ctx context.Context, id int64) (out []PushWork, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		r, e := tx.Get(Notifications, id)
		if e != nil {
			return e
		}
		n, e := Decode[Notification](r)
		if e != nil {
			return e
		}
		if n.PushState != "queued" || n.DeliverAfter != nil && n.DeliverAfter.After(s.Now()) {
			return ErrConflict
		}
		allowed, e := s.pushAllowed(tx, r, n)
		if e != nil {
			return e
		}
		if !allowed {
			return Denied("push_permission_withdrawn")
		}
		rows, e := allRecords(tx, PushDevices, Filter{OwnerID: r.OwnerID, State: "enabled"})
		if e != nil {
			return e
		}
		for _, device := range rows {
			v, e := Decode[PushDeviceInput](device)
			if e != nil {
				return e
			}
			out = append(out, PushWork{id, r.Version, PushPayload{id, v.Platform, v.Token, "M&D有新的动态，请打开应用查看", fmt.Sprintf("push:%d:%d", id, device.ID)}})
		}
		return nil
	})
	return
}
func (s *Service) RecordPushAcceptance(ctx context.Context, operator string, id, version int64, receipts []PushAcceptance) error {
	if operator == "" || len(receipts) == 0 {
		return ErrForbidden
	}
	for _, v := range receipts {
		if !v.Accepted || textValid(v.MessageID, 160, true) != nil {
			return ErrUnavailable
		}
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Notifications, id)
		if e != nil {
			return e
		}
		if e = expected(r, version); e != nil {
			return e
		}
		n, e := Decode[Notification](r)
		if e != nil {
			return e
		}
		if n.PushState != "queued" {
			return ErrConflict
		}
		n.PushState = "accepted"
		n.PushReceipts = receipts
		if e = save(tx, Notifications, &r, n, r.OwnerID, "notification.push_accepted"); e != nil {
			return e
		}
		return audit(tx, operator, "notification.push_accepted", Notifications, id)
	})
}
func (s *Service) DispatchPush(ctx context.Context, operator string, id int64) error {
	if operator == "" {
		return ErrForbidden
	}
	suppressed := false
	if e := s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Notifications, id)
		if e != nil {
			return e
		}
		n, e := Decode[Notification](r)
		if e != nil {
			return e
		}
		if HasString([]string{"accepted", "delivered", "suppressed"}, n.PushState) {
			suppressed = true
			return nil
		}
		if n.PushState != "queued" {
			return ErrConflict
		}
		pref, e := tx.ByKey(Preferences, fmt.Sprint(r.OwnerID))
		allowed := false
		if e == nil {
			p, e := Decode[Preference](pref)
			if e != nil {
				return e
			}
			category := map[string]bool{"direct_messages": p.DirectMessages, "groups": p.Groups, "applications": p.Applications, "interactions": p.Interactions, "nearby": true}[notificationCategory(r, n)]
			allowed = p.PushPermission && p.Frequency != "off" && category
			if allowed && p.Frequency == "digest" && n.DeliverAfter == nil {
				next := s.Now().Truncate(24 * time.Hour).Add(24 * time.Hour)
				n.DeliverAfter = &next
				if e = SetBody(&r, n); e != nil {
					return e
				}
				if e = tx.Save(Notifications, &r); e != nil {
					return e
				}
			}
		} else if e != ErrNotFound {
			return e
		}
		allowed, e = s.pushAllowed(tx, r, n)
		if e != nil {
			return e
		}
		if !allowed {
			suppressed = true
			n.PushState = "suppressed"
			if e = SetBody(&r, n); e != nil {
				return e
			}
			if e = tx.Save(Notifications, &r); e != nil {
				return e
			}
			return audit(tx, operator, "notification.push_suppressed", Notifications, id)
		}
		return nil
	}); e != nil {
		return e
	}
	if suppressed {
		return nil
	}
	if s.Push == nil {
		return ErrUnavailable
	}
	work, e := s.PushWork(ctx, id)
	if e != nil {
		return e
	}
	if len(work) == 0 {
		return Denied("no_enabled_push_device")
	}
	receipts := []PushAcceptance{}
	for _, item := range work {
		if e := s.validatePushWork(ctx, item); e != nil {
			return e
		}
		lookup, e := s.Push.Lookup(ctx, item.Payload.IdempotencyKey)
		if e != nil {
			return ErrUnavailable
		}
		receipt := lookup.Receipt
		switch lookup.State {
		case "accepted":
		case "not_found":
			if e := s.validatePushWork(ctx, item); e != nil {
				return e
			}
			receipt, e = s.Push.Send(ctx, item.Payload)
			if e != nil {
				return ErrUnavailable
			}
		default:
			return ErrUnavailable
		}
		if !receipt.Accepted || receipt.MessageID == "" {
			return ErrUnavailable
		}
		receipts = append(receipts, receipt)
	}
	return s.RecordPushAcceptance(ctx, operator, id, work[0].Version, receipts)
}

func (s *Service) validatePushWork(ctx context.Context, item PushWork) error {
	current, e := s.PushWork(ctx, item.NotificationID)
	if e != nil {
		return e
	}
	for _, candidate := range current {
		if candidate.Payload.IdempotencyKey == item.Payload.IdempotencyKey && candidate.Payload.Token == item.Payload.Token {
			return nil
		}
	}
	return Denied("push_permission_withdrawn")
}
