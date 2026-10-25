package mobile

import (
	"context"
	"errors"
	"fmt"
)

type LeadAssessmentInput struct {
	CommandMeta
	Version    int64  `json:"version"`
	Assessment string `json:"assessment"`
}
type LeadContactInput struct {
	CommandMeta
	Version int64 `json:"version"`
}

// 私密线索始终按当前求助与原目击者核对，不将快照参与人当作永久授权。
func (s *Service) leadContext(tx Tx, lead Record, actor int64) (Record, error) {
	if actor <= 0 || lead.State == "deleted" || lead.State == "removed" {
		return Record{}, ErrForbidden
	}
	parent, e := tx.Get(Cases, lead.RefID)
	if e != nil {
		return Record{}, e
	}
	if parent.State == "removed" || parent.State == "deleted" || actor != parent.OwnerID && actor != lead.OwnerID {
		return Record{}, ErrForbidden
	}
	return parent, nil
}

func (s *Service) AssessLead(ctx context.Context, actor, id int64, in LeadAssessmentInput) (out Record, err error) {
	if !HasString([]string{"helpful", "not_match"}, in.Assessment) {
		return out, Invalid("choose a lead assessment")
	}
	err = s.command(ctx, actor, "lead.assess", id, in, Leads, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		lead, e := tx.Get(Leads, id)
		if e != nil {
			return e
		}
		if e = expected(lead, in.Version); e != nil {
			return e
		}
		parent, e := s.leadContext(tx, lead, actor)
		if e != nil {
			return e
		}
		if parent.OwnerID != actor {
			return ErrForbidden
		}
		if parent.State != "active" && parent.State != "matched" {
			return ErrConflict
		}
		body, e := Decode[Lead](lead)
		if e != nil {
			return e
		}
		if !HasString([]string{"", "pending", "helpful", "not_match"}, body.Assessment) {
			return ErrConflict
		}
		now := s.Now()
		body.Assessment, body.ReviewedBy, body.ReviewedAt = in.Assessment, actor, &now
		// 只改变处理标记，原话、照片、求助状态和宠物走失状态均不改动。
		if e = save(tx, Leads, &lead, body, actor, "lead.assessed"); e != nil {
			return e
		}
		out = lead
		return nil
	})
	return
}

func (s *Service) LeadConversation(ctx context.Context, actor, id int64, in LeadContactInput) (out Record, err error) {
	err = s.command(ctx, actor, "lead.contact", id, in, Conversations, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		lead, e := tx.Get(Leads, id)
		if e != nil {
			return e
		}
		if e = expected(lead, in.Version); e != nil {
			return e
		}
		parent, e := s.leadContext(tx, lead, actor)
		if e != nil {
			return e
		}
		if parent.State != "active" && parent.State != "matched" {
			return ErrConflict
		}
		peer := lead.OwnerID
		if actor == lead.OwnerID {
			peer = parent.OwnerID
		}
		if actor == peer {
			return Invalid("a witness conversation needs two people")
		}
		if e = s.active(tx, peer); e != nil {
			return e
		}
		if e = s.directContact(tx, actor, peer); e != nil {
			return e
		}
		key := fmt.Sprintf("lead:%d:%s", id, pair(actor, peer))
		old, e := tx.ByKey(Conversations, key)
		if e == nil {
			body, decodeErr := Decode[Conversation](old)
			if decodeErr != nil {
				return decodeErr
			}
			if body.ContextKind != "lead" || body.ContextID != id {
				return ErrForbidden
			}
			if e = s.leadConversationContext(tx, old, body, actor, false); e != nil {
				return e
			}
			out = old
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		state := "pending"
		connected, e := friends(tx, actor, peer)
		if e != nil {
			return e
		}
		if connected {
			state = "accepted"
		}
		out = Record{OwnerID: actor, Participants: []int64{peer}, RefID: id, Key: key, State: state}
		return save(tx, Conversations, &out, Conversation{RequesterID: actor, RecipientID: peer, ContextKind: "lead", ContextID: id}, actor, "lead.conversation_created")
	})
	return
}

// 读取保留双方的私密历史；新增消息／接受请求还要求求助仍在处理中。
func (s *Service) leadConversationContext(tx Tx, conversation Record, body Conversation, actor int64, writing bool) error {
	if body.ContextKind == "" {
		return nil
	}
	if body.ContextKind != "lead" || body.ContextID <= 0 || conversation.RefID != body.ContextID || !participant(conversation, actor) {
		return ErrForbidden
	}
	lead, e := tx.Get(Leads, body.ContextID)
	if e != nil {
		return e
	}
	parent, e := s.leadContext(tx, lead, actor)
	if e != nil {
		return e
	}
	if pair(body.RequesterID, body.RecipientID) != pair(parent.OwnerID, lead.OwnerID) {
		return ErrForbidden
	}
	if writing && parent.State != "active" && parent.State != "matched" {
		return ErrConflict
	}
	return nil
}
