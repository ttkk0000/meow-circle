package mobile

import "context"

// 仅给照护界面当前阶段与写入能力，不返回申请、协议、双方身份或健康材料。
type CareContext struct {
	AdoptionID int64  `json:"adoption_id"`
	State      string `json:"state"`
	CanPost    bool   `json:"can_post"`
	Reason     string `json:"reason"`
}

func (s *Service) CareContext(ctx context.Context, actor, adoptionID int64) (out CareContext, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		if e := s.checkActor(ctx, tx, actor); e != nil {
			return e
		}
		row, e := tx.Get(Adoptions, adoptionID)
		if e != nil {
			return e
		}
		adoption, e := Decode[Adoption](row)
		if e != nil {
			return e
		}
		collaborator := false
		if actor != adoption.ApplicantID && actor != adoption.PublisherID {
			collaborator, e = s.collaborator(tx, "caretaker", adoptionID, actor)
			if e != nil {
				return e
			}
			if !collaborator {
				return ErrNotFound
			}
		}
		out = CareContext{AdoptionID: adoptionID, State: row.State}
		if !carePostingOpen(row, adoption, s.Now()) {
			out.Reason = "care_stage_closed"
			return nil
		}
		if actor != adoption.ApplicantID && !collaborator {
			out.Reason = "care_recipient_required"
			return nil
		}
		eligibility, e := s.eligibility(tx, actor)
		if e != nil {
			return e
		}
		out.CanPost = eligibility.Interaction
		if !out.CanPost {
			out.Reason = "interaction_required"
		}
		return nil
	})
	return
}
