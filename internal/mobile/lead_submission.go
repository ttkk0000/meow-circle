package mobile

import (
	"context"
	"errors"
	"fmt"
)

type LeadSubmissionInput struct {
	CommandMeta
	Version  int64   `json:"version"`
	Text     string  `json:"text"`
	MediaIDs []int64 `json:"media_ids"`
}

// 新入口绑定本次明确确认的求助版本与原请求；保留旧Lead入口的兼容行为。
func (s *Service) SubmitLead(ctx context.Context, actor, caseID int64, in LeadSubmissionInput) (out Record, err error) {
	if err = textValid(in.Text, 2000, true); err != nil {
		return
	}
	if in.Version <= 0 {
		return out, Invalid("case version required")
	}
	err = s.command(ctx, actor, "lead.submit", caseID, in, Leads, &out, func(tx Tx) error {
		var e error
		out, e = s.privateLead(tx, actor, caseID, in.Version, fmt.Sprintf("lead-request:%d:%s", actor, in.ClientRequestID), in.Text, in.MediaIDs)
		return e
	})
	return
}

func (s *Service) privateLead(tx Tx, actor, caseID, version int64, key, text string, media []int64) (Record, error) {
	if e := s.interaction(tx, actor); e != nil {
		return Record{}, e
	}
	parent, e := tx.Get(Cases, caseID)
	if e != nil {
		return Record{}, e
	}
	ok, e := s.canRead(tx, Cases, parent, actor)
	if e != nil {
		return Record{}, e
	}
	if !ok {
		return Record{}, ErrForbidden
	}
	if version > 0 {
		if e = expected(parent, version); e != nil {
			return Record{}, e
		}
	}
	if parent.State != "active" && parent.State != "matched" {
		return Record{}, ErrConflict
	}
	if e = s.attachments(tx, actor, media); e != nil {
		return Record{}, e
	}
	old, e := tx.ByKey(Leads, key)
	if e == nil {
		return old, nil
	}
	if !errors.Is(e, ErrNotFound) {
		return Record{}, e
	}
	row := Record{OwnerID: actor, Key: key, Participants: []int64{parent.OwnerID}, RefID: caseID, MediaIDs: media, State: "submitted"}
	if e = save(tx, Leads, &row, Lead{Text: text}, actor, "lead.submitted"); e != nil {
		return Record{}, e
	}
	if e = s.notify(tx, parent.OwnerID, actor, "applications", Cases, caseID, "收到一条私密寻宠线索"); e != nil {
		return Record{}, e
	}
	return row, nil
}
