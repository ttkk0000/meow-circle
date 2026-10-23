package mobile

import (
	"context"
	"errors"
	"fmt"
	"math"
	"time"
)

func distanceKM(a, b, c, d float64) float64 {
	rad := math.Pi / 180
	lat, lon := (c-a)*rad, (d-b)*rad
	v := math.Sin(lat/2)*math.Sin(lat/2) + math.Cos(a*rad)*math.Cos(c*rad)*math.Sin(lon/2)*math.Sin(lon/2)
	v = math.Min(1, math.Max(0, v))
	return 6371 * 2 * math.Atan2(math.Sqrt(v), math.Sqrt(1-v))
}

type AlertBatch struct {
	Next    int64 `json:"next_cursor"`
	Created int   `json:"created"`
}

// NearbyAlerts is a bounded, idempotent local consumer of case publication.
// The protected operations scheduler calls it using case.saved outbox events.
// It rechecks closure and opt-in at dispatch time; no broker is required.
func (s *Service) NearbyAlerts(ctx context.Context, caseID, after int64) (out AlertBatch, err error) {
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Cases, caseID)
		if e != nil {
			return e
		}
		if r.State != "active" && r.State != "matched" {
			return ErrConflict
		}
		c, e := Decode[Case](r)
		if e != nil {
			return e
		}
		subscriptions, e := tx.List(Subscriptions, Filter{After: after, State: "active", Limit: 100})
		if e != nil {
			return e
		}
		for _, subscription := range subscriptions {
			out.Next = subscription.ID
			v, e := Decode[Subscription](subscription)
			if e != nil {
				return e
			}
			if !v.Enabled || v.Frequency == "off" || subscription.OwnerID == r.OwnerID || distanceKM(c.Latitude, c.Longitude, v.Latitude, v.Longitude) > v.RadiusKM {
				continue
			}
			if v.Species != "" && v.Species != c.Species {
				continue
			}
			if e = s.active(tx, subscription.OwnerID); errors.Is(e, ErrUnauthorized) || errors.Is(e, ErrNotFound) {
				continue
			} else if e != nil {
				return e
			}
			key := fmt.Sprintf("nearby:%d:%d", caseID, subscription.OwnerID)
			var deliver *time.Time
			if v.Frequency == "daily" {
				local := s.Now().In(time.FixedZone("Asia/Shanghai", 8*3600))
				next := time.Date(local.Year(), local.Month(), local.Day()+1, 8, 0, 0, 0, local.Location())
				deliver = &next
				key = fmt.Sprintf("nearby-daily:%d:%s", subscription.OwnerID, local.Format("2006-01-02"))
			}
			notice, e := tx.ByKey(Notifications, key)
			if e == nil {
				body, e := Decode[Notification](notice)
				if e != nil {
					return e
				}
				if !Has(body.CaseIDs, caseID) && len(body.CaseIDs) < 100 {
					body.CaseIDs = append(body.CaseIDs, caseID)
					if e = save(tx, Notifications, &notice, body, 0, "nearby.digest_updated"); e != nil {
						return e
					}
				}
				continue
			}
			if !errors.Is(e, ErrNotFound) {
				return e
			}
			push := "suppressed"
			pref, e := tx.ByKey(Preferences, fmt.Sprint(subscription.OwnerID))
			if e == nil {
				p, e := Decode[Preference](pref)
				if e != nil {
					return e
				}
				if p.PushPermission {
					push = "queued"
				}
			} else if !errors.Is(e, ErrNotFound) {
				return e
			}
			notice = Record{OwnerID: subscription.OwnerID, Key: key, State: "unread"}
			body := Notification{Category: "nearby", Text: "附近有新的寻宠或发现信息", TargetKind: Cases, TargetID: caseID, CaseIDs: []int64{caseID}, PushState: push, DeliverAfter: deliver}
			if e = save(tx, Notifications, &notice, body, 0, "nearby.alert_created"); e != nil {
				return e
			}
			out.Created++
		}
		return nil
	})
	return
}
