package mobile

import (
	"errors"
	"fmt"
	"strings"
)

// Apply the current inbox policy without rewriting historical IDs, read state or delivery receipts.
func notificationCategory(record Record, body Notification) string {
	if body.Category == "interactions" && (body.TargetKind == Friendships ||
		body.TargetKind == Cases && strings.HasPrefix(record.Key, fmt.Sprintf("case-watch:%d:", body.TargetID))) {
		return "applications"
	}
	return body.Category
}

// A notification can remain readable after its target is gone; a new push must not reveal that target.
func (s *Service) notificationAnchorReadable(tx Tx, actor int64, kind Kind, id int64, anchor *NotificationReplyAnchor) (bool, error) {
	if anchor == nil {
		return true, nil
	}
	if anchor.ID <= 0 || anchor.Version <= 0 || !(kind == Entries && anchor.Kind == Comments || kind == Pets && anchor.Kind == PetComments) {
		return false, nil
	}
	comment, e := tx.Get(anchor.Kind, anchor.ID)
	if errors.Is(e, ErrNotFound) {
		return false, nil
	}
	if e != nil {
		return false, e
	}
	if comment.RefID != id {
		return false, nil
	}
	allowed, e := s.canRead(tx, anchor.Kind, comment, actor)
	if errors.Is(e, ErrNotFound) || errors.Is(e, ErrForbidden) {
		return false, nil
	}
	return allowed, e
}

func (s *Service) notificationTargetReadable(tx Tx, actor int64, body Notification) (bool, error) {
	if body.TargetID <= 0 || !queryKind(body.TargetKind) || body.TargetKind == Notifications {
		return false, nil
	}
	target, e := tx.Get(body.TargetKind, body.TargetID)
	if errors.Is(e, ErrNotFound) {
		return false, nil
	}
	if e != nil {
		return false, e
	}
	allowed, e := s.canRead(tx, body.TargetKind, target, actor)
	if errors.Is(e, ErrNotFound) || errors.Is(e, ErrForbidden) {
		return false, nil
	}
	if e != nil || !allowed {
		return false, e
	}
	return s.notificationAnchorReadable(tx, actor, body.TargetKind, body.TargetID, body.ReplyAnchor)
}
