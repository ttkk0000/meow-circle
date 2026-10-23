package mobile

import (
	"context"
	"errors"
	"fmt"
)

// 内容订阅只影响读取和提醒偏好，不授予好友、群聊或私密资料权限。
type ContentSubscriptions struct {
	FollowedUsers []int64 `json:"followed_users"`
	WatchedCases  []int64 `json:"watched_cases"`
}
type ContentSubscriptionInput struct {
	CommandMeta
	Version       int64   `json:"version"`
	FollowedUsers []int64 `json:"followed_users"`
	WatchedCases  []int64 `json:"watched_cases"`
}

func contentPreferences(tx Tx, actor int64) (Record, Preference, error) {
	r, e := tx.ByKey(Preferences, fmt.Sprint(actor))
	if errors.Is(e, ErrNotFound) {
		return Record{OwnerID: actor, Key: fmt.Sprint(actor), State: "active"}, Preference{DirectMessages: true, Groups: true, Applications: true, Interactions: true, Frequency: "instant"}, nil
	}
	if e != nil {
		return r, Preference{}, e
	}
	p, e := Decode[Preference](r)
	return r, p, e
}
func contentBody(p Preference) ContentSubscriptions {
	if p.Content == nil {
		return ContentSubscriptions{FollowedUsers: []int64{}, WatchedCases: []int64{}}
	}
	return ContentSubscriptions{FollowedUsers: append([]int64{}, p.Content.FollowedUsers...), WatchedCases: append([]int64{}, p.Content.WatchedCases...)}
}
func (s *Service) ContentSubscriptions(ctx context.Context, actor int64) (out Record, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, p, e := contentPreferences(tx, actor)
		if e != nil {
			return e
		}
		out = r
		return SetBody(&out, contentBody(p))
	})
	return
}
func (s *Service) SaveContentSubscriptions(ctx context.Context, actor int64, in ContentSubscriptionInput) (out Record, err error) {
	if len(in.FollowedUsers) > 200 || len(in.WatchedCases) > 200 || len(Unique(in.FollowedUsers)) != len(in.FollowedUsers) || len(Unique(in.WatchedCases)) != len(in.WatchedCases) {
		return out, Invalid("use up to 200 distinct subscriptions")
	}
	err = s.command(ctx, actor, "content_subscriptions.save", 0, in, Preferences, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, p, e := contentPreferences(tx, actor)
		if e != nil {
			return e
		}
		if r.Version != in.Version {
			return ErrConflict
		}
		previous := contentBody(p)
		addingUsers := false
		for _, id := range in.FollowedUsers {
			if id <= 0 || id == actor {
				return Invalid("invalid followed user")
			}
			if Has(previous.FollowedUsers, id) {
				continue
			}
			addingUsers = true
			if _, e = tx.User(id); e != nil {
				return e
			}
			ok, e := s.publicProcessingAllowed(tx, id)
			if e != nil {
				return e
			}
			if !ok {
				return ErrNotFound
			}
			blocked, e := blocked(tx, actor, id)
			if e != nil {
				return e
			}
			if blocked {
				return ErrForbidden
			}
		}
		if addingUsers {
			if e := s.interaction(tx, actor); e != nil {
				return e
			}
		}
		for _, id := range in.WatchedCases {
			if id <= 0 {
				return Invalid("invalid watched case")
			}
			if Has(previous.WatchedCases, id) {
				continue
			}
			c, e := tx.Get(Cases, id)
			if e != nil {
				return e
			}
			ok, e := s.canRead(tx, Cases, c, actor)
			if e != nil {
				return e
			}
			if !ok {
				return ErrNotFound
			}
		}
		p.Content = &ContentSubscriptions{FollowedUsers: append([]int64{}, in.FollowedUsers...), WatchedCases: append([]int64{}, in.WatchedCases...)}
		if e = save(tx, Preferences, &r, p, actor, "content_subscriptions.saved"); e != nil {
			return e
		}
		out = r
		return nil
	})
	if err == nil {
		p, e := Decode[Preference](out)
		if e != nil {
			return out, e
		}
		err = SetBody(&out, contentBody(p))
	}
	return
}

// 由受保护的case事件消费者逐页调用；每个版本每人至多一条，始终重核可读性。
func (s *Service) CaseWatchAlerts(ctx context.Context, caseID, after int64) (out AlertBatch, err error) {
	err = s.Repo.Update(ctx, func(tx Tx) error {
		c, e := tx.Get(Cases, caseID)
		if e != nil {
			return e
		}
		rows, e := tx.List(Preferences, Filter{After: after, Limit: 100})
		if e != nil {
			return e
		}
		for _, row := range rows {
			out.Next = row.ID
			p, e := Decode[Preference](row)
			if e != nil {
				return e
			}
			if p.Content == nil || !Has(p.Content.WatchedCases, caseID) || row.OwnerID == c.OwnerID {
				continue
			}
			if e = s.active(tx, row.OwnerID); errors.Is(e, ErrUnauthorized) || errors.Is(e, ErrNotFound) {
				continue
			} else if e != nil {
				return e
			}
			ok, e := s.canRead(tx, Cases, c, row.OwnerID)
			if e != nil {
				return e
			}
			if !ok {
				continue
			}
			key := fmt.Sprintf("case-watch:%d:%d:%d", caseID, c.Version, row.OwnerID)
			if _, e = tx.ByKey(Notifications, key); e == nil {
				continue
			} else if !errors.Is(e, ErrNotFound) {
				return e
			}
			notice := Record{OwnerID: row.OwnerID, Key: key, State: "unread"}
			body := Notification{Category: "interactions", Text: "关注的寻宠求助有了新的进展", TargetKind: Cases, TargetID: caseID, PushState: "suppressed"}
			if e = save(tx, Notifications, &notice, body, 0, "case.watch_updated"); e != nil {
				return e
			}
			out.Created++
		}
		return nil
	})
	return
}
