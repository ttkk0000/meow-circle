package mobile

import "fmt"

type pushBinding struct {
	PushDeviceInput
	SessionKey string `json:"session_key,omitempty"`
}

// Called in the same transaction as session revocation or account closure.
// Legacy unbound registrations are disabled rather than guessed to be another device.
func DisablePushDevices(tx Tx, actor int64, session string, all bool) error {
	rows, e := allRecords(tx, PushDevices, Filter{OwnerID: actor})
	if e != nil {
		return e
	}
	for _, r := range rows {
		v, e := Decode[pushBinding](r)
		if e != nil {
			return e
		}
		if !all && v.SessionKey != "" && v.SessionKey != session {
			continue
		}
		v.Token, v.Enabled, v.Permission, v.SessionKey = "", false, "denied", ""
		r.State = "disabled"
		if e = SetBody(&r, v); e != nil {
			return e
		}
		if e = tx.Save(PushDevices, &r); e != nil {
			return e
		}
	}
	return nil
}

func RotatePushSession(tx Tx, actor int64, previous, next string) error {
	rows, e := allRecords(tx, PushDevices, Filter{OwnerID: actor, State: "enabled"})
	if e != nil {
		return e
	}
	for _, r := range rows {
		v, e := Decode[pushBinding](r)
		if e != nil {
			return e
		}
		if v.SessionKey != previous {
			continue
		}
		v.SessionKey = next
		if e = SetBody(&r, v); e != nil {
			return e
		}
		if e = tx.Save(PushDevices, &r); e != nil {
			return e
		}
	}
	return nil
}

func (s *Service) pushAllowed(tx Tx, r Record, n Notification) (bool, error) {
	if e := s.active(tx, r.OwnerID); e == ErrUnauthorized || e == ErrNotFound {
		return false, nil
	} else if e != nil {
		return false, e
	}
	pref, e := tx.ByKey(Preferences, fmt.Sprint(r.OwnerID))
	if e == ErrNotFound {
		return false, nil
	}
	if e != nil {
		return false, e
	}
	p, e := Decode[Preference](pref)
	if e != nil {
		return false, e
	}
	category := map[string]bool{"direct_messages": p.DirectMessages, "groups": p.Groups, "applications": p.Applications, "interactions": p.Interactions, "nearby": true}[n.Category]
	if !p.PushPermission || p.Frequency == "off" || !category {
		return false, nil
	}
	if n.Category != "nearby" {
		return true, nil
	}
	subscription, e := tx.ByKey(Subscriptions, fmt.Sprint(r.OwnerID))
	if e == ErrNotFound {
		return false, nil
	}
	if e != nil {
		return false, e
	}
	v, e := Decode[Subscription](subscription)
	if e != nil {
		return false, e
	}
	if subscription.State != "active" || !v.Enabled || v.Frequency == "off" {
		return false, nil
	}
	ids := n.CaseIDs
	if len(ids) == 0 {
		ids = []int64{n.TargetID}
	}
	for _, id := range ids {
		c, e := tx.Get(Cases, id)
		if e == ErrNotFound {
			continue
		}
		if e != nil {
			return false, e
		}
		allowed, e := s.publicProcessingAllowed(tx, c.OwnerID)
		if e != nil {
			return false, e
		}
		if !allowed || !c.Public || !HasString([]string{"active", "matched"}, c.State) {
			continue
		}
		b, e := blocked(tx, c.OwnerID, r.OwnerID)
		if e != nil {
			return false, e
		}
		body, e := Decode[Case](c)
		if e != nil {
			return false, e
		}
		if !b && matchesSubscription(body, v) {
			return true, nil
		}
	}
	return false, nil
}
