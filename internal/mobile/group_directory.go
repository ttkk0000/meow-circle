package mobile

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"
)

type GroupDirectoryBody struct {
	Name        string `json:"name"`
	City        string `json:"city"`
	Summary     string `json:"summary"`
	MemberCount int    `json:"member_count"`
	Member      bool   `json:"member"`
}
type GroupJoinInput struct {
	CommandMeta
	Note string `json:"note"`
}

// 同一事务移交待审申请；旧群主不再保有申请资料或批准权。
func (s *Service) transferGroupRequests(tx Tx, groupID, owner int64) error {
	rows, e := tx.List(Invitations, Filter{RefID: groupID, State: "pending", Limit: 100})
	if e != nil {
		return e
	}
	for _, row := range rows {
		v, e := Decode[Invitation](row)
		if e != nil {
			return e
		}
		if v.Scope != "group_request" || !v.ExpiresAt.After(s.Now()) {
			continue
		}
		v.RecipientID = owner
		row.Participants = Unique([]int64{row.OwnerID, owner})
		if e = save(tx, Invitations, &row, v, owner, "group.request_transferred"); e != nil {
			return e
		}
		if e = s.notify(tx, owner, row.OwnerID, "applications", Invitations, row.ID, "群主变更后待审核的入群申请"); e != nil {
			return e
		}
	}
	return nil
}

func (s *Service) directoryRecord(tx Tx, r Record, actor int64) (Record, error) {
	g, e := Decode[Group](r)
	if e != nil {
		return Record{}, e
	}
	if r.State != "active" || !g.Discoverable {
		return Record{}, ErrNotFound
	}
	ok, e := s.publicProcessingAllowed(tx, r.OwnerID)
	if e != nil {
		return Record{}, e
	}
	if !ok {
		return Record{}, ErrNotFound
	}
	if actor > 0 {
		b, e := blocked(tx, actor, r.OwnerID)
		if e != nil {
			return Record{}, e
		}
		if b {
			return Record{}, ErrNotFound
		}
	}
	r.Participants = nil
	r.OwnerID = 0
	r.MediaIDs = nil
	r.Key = ""
	e = SetBody(&r, GroupDirectoryBody{Name: g.Name, City: g.City, Summary: g.Summary, MemberCount: len(g.Members), Member: Has(g.Members, actor)})
	return r, e
}

// 群目录始终是独立公开投影，不修改Groups／GroupMessages的成员ACL。
func (s *Service) GroupDirectory(ctx context.Context, actor, after, id int64, keyword, city string) (out Page, err error) {
	out.Items = []Record{}
	if len(keyword) > 120 || len(city) > 120 {
		return out, Invalid("invalid directory filters")
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if actor > 0 {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		}
		if id > 0 {
			r, e := tx.Get(Groups, id)
			if e != nil {
				return e
			}
			r, e = s.directoryRecord(tx, r, actor)
			if e != nil {
				return e
			}
			out.Items = append(out.Items, r)
			return nil
		}
		rows, e := tx.List(Groups, Filter{After: after, PublicOnly: true, State: "active", Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			out.Next = r.ID
			entry, e := s.directoryRecord(tx, r, actor)
			if errors.Is(e, ErrNotFound) {
				continue
			}
			if e != nil {
				return e
			}
			body, e := Decode[GroupDirectoryBody](entry)
			if e != nil {
				return e
			}
			if city != "" && body.City != city || keyword != "" && !strings.Contains(strings.ToLower(body.Name+body.Summary), strings.ToLower(keyword)) {
				continue
			}
			out.Items = append(out.Items, entry)
			if len(out.Items) == 30 {
				break
			}
		}
		return nil
	})
	return
}
func (s *Service) RequestGroupJoin(ctx context.Context, actor, id int64, in GroupJoinInput) (out Record, err error) {
	if e := textValid(in.Note, 1000, false); e != nil {
		return out, e
	}
	err = s.command(ctx, actor, "group.join_request", id, in, Invitations, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		group, e := tx.Get(Groups, id)
		if e != nil {
			return e
		}
		if _, e = s.directoryRecord(tx, group, actor); e != nil {
			return e
		}
		g, e := Decode[Group](group)
		if e != nil {
			return e
		}
		if Has(g.Members, actor) {
			return ErrConflict
		}
		pending, e := tx.Count(Invitations, Filter{RefID: id, State: "pending"})
		if e != nil {
			return e
		}
		if pending >= 100 {
			return ErrRateLimited
		}
		key := fmt.Sprintf("group_request:%d:%d", id, actor)
		r, e := tx.ByKey(Invitations, key)
		if errors.Is(e, ErrNotFound) {
			r = Record{OwnerID: actor, Key: key, RefID: id, Participants: []int64{actor, group.OwnerID}}
		} else if e != nil {
			return e
		} else {
			previous, e := Decode[Invitation](r)
			if e != nil {
				return e
			}
			if r.State == "pending" && previous.ExpiresAt.After(s.Now()) {
				return ErrConflict
			}
		}
		r.State = "pending"
		r.Participants = Unique([]int64{actor, group.OwnerID})
		body := Invitation{Scope: "group_request", TargetID: id, RecipientID: group.OwnerID, ExpiresAt: s.Now().Add(48 * time.Hour), Note: in.Note}
		if e = save(tx, Invitations, &r, body, actor, "group.join_requested"); e != nil {
			return e
		}
		out = r
		return s.notify(tx, group.OwnerID, actor, "applications", Invitations, r.ID, "收到新的入群申请")
	})
	return
}
