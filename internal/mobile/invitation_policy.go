package mobile

import "errors"

// 接受或使用协作授权时重新核对资源归属；邀请快照不授予永久权限。
func (s *Service) invitationAuthority(tx Tx, invitation Record, body Invitation, accepting bool) error {
	if invitation.RefID != body.TargetID || body.TargetID <= 0 || body.RecipientID <= 0 || body.RecipientID == invitation.OwnerID {
		return ErrForbidden
	}
	if e := s.active(tx, invitation.OwnerID); e != nil {
		return e
	}
	denied, e := blocked(tx, invitation.OwnerID, body.RecipientID)
	if e != nil {
		return e
	}
	if denied {
		return ErrForbidden
	}
	var kind Kind
	switch body.Scope {
	case "adoption_editor":
		kind = Pets
	case "caretaker":
		kind = Adoptions
	case "group_member":
		kind = Groups
	default:
		return ErrForbidden
	}
	target, e := tx.Get(kind, body.TargetID)
	if e != nil {
		return e
	}
	switch kind {
	case Pets:
		pet, e := Decode[Pet](target)
		if e != nil {
			return e
		}
		if target.OwnerID != invitation.OwnerID || target.State != "active" || !pet.AdoptionAvailable {
			return ErrForbidden
		}
	case Adoptions:
		adoption, e := Decode[Adoption](target)
		if e != nil {
			return e
		}
		if adoption.ApplicantID != invitation.OwnerID || target.State == "deleted" || accepting && !carePostingOpen(target, adoption, s.Now()) {
			return ErrForbidden
		}
	case Groups:
		if target.OwnerID != invitation.OwnerID || target.State != "active" {
			return ErrForbidden
		}
	}
	if accepting {
		return s.interaction(tx, invitation.OwnerID)
	}
	return nil
}

func invitationAccessEnded(err error) bool {
	return errors.Is(err, ErrNotFound) || errors.Is(err, ErrForbidden) || errors.Is(err, ErrUnauthorized)
}
