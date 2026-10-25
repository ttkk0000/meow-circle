package mobile

import (
	"context"
	"errors"
	"fmt"
	"time"
)

type PetInput struct {
	CommandMeta
	Version  int64   `json:"version"`
	Pet      Pet     `json:"pet"`
	MediaIDs []int64 `json:"media_ids"`
}

func (s *Service) SavePet(ctx context.Context, actor, id int64, in PetInput) (out Record, err error) {
	if id == 0 && in.Version != 0 {
		return out, ErrConflict
	}
	if err = textValid(in.Pet.Name, 40, true); err != nil {
		return
	}
	if err = textValid(in.Pet.City, 120, false); err != nil {
		return
	}
	if !HasString([]string{"cat", "dog", "rabbit", "other"}, in.Pet.Species) {
		return out, Invalid("choose a supported profile species")
	}
	if in.Pet.Species != "cat" && in.Pet.Species != "dog" && in.Pet.AdoptionAvailable {
		return out, Denied("species_not_admitted_for_adoption")
	}
	if err = audienceValid(in.Pet.Audience); err != nil {
		return
	}
	if len(in.Pet.Bio) > 8000 || len(in.Pet.Health) > 8000 || len(in.Pet.Breed) > 120 {
		return out, Invalid("pet details too long")
	}
	err = s.command(ctx, actor, "pet.save", id, in, Pets, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if in.Pet.Audience.Mode != "private" || in.Pet.AdoptionAvailable {
			if e := s.interaction(tx, actor); e != nil {
				return e
			}
		}
		if in.Pet.AdoptionAvailable {
			if e := s.factualAttachments(tx, actor, in.MediaIDs); e != nil {
				return e
			}
			if e := s.adult(tx, actor); e != nil {
				return e
			}
			if e := s.disposition(tx, actor, id); e != nil {
				return e
			}
		}
		if e := s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		if in.Pet.Audience.Mode == "public" {
			if e := s.publicAttachments(tx, actor, in.MediaIDs); e != nil {
				return e
			}
		}
		r := Record{OwnerID: actor, State: "active"}
		in.Pet.AdoptionCycle = 0
		in.Pet.PlacementState = ""
		in.Pet.AdoptionOpened = in.Pet.AdoptionAvailable
		if id > 0 {
			var e error
			r, e = writable(tx, Pets, id, actor, in.Version)
			if e != nil {
				return e
			}
			old, e := Decode[Pet](r)
			if e != nil {
				return e
			}
			in.Pet.AdoptionCycle = old.AdoptionCycle
			in.Pet.PlacementState = old.PlacementState
			in.Pet.AdoptionOpened = old.AdoptionOpened || old.AdoptionAvailable || in.Pet.AdoptionAvailable
			if in.Pet.AdoptionAvailable && !old.AdoptionAvailable {
				occupied, e := otherPlacement(tx, id, 0)
				if e != nil {
					return e
				}
				if occupied {
					return Denied("pet_placement_conflict")
				}
				previous, e := tx.Count(Adoptions, Filter{RefID: id})
				if e != nil {
					return e
				}
				// The first opening stays at legacy cycle 0. Only a reopening
				// advances it; historical applications also identify legacy rounds.
				if old.AdoptionOpened || previous > 0 {
					in.Pet.AdoptionCycle++
				}
				in.Pet.PlacementState = ""
				if e = s.closeOtherAdoptions(tx, actor, Record{RefID: id}, "上一轮领养已结束，历史申请仍可查看；新一轮需重新核对提交"); e != nil {
					return e
				}
			}
		}
		r.MediaIDs = in.MediaIDs
		r.Public = in.Pet.Audience.Mode == "public"
		if e := save(tx, Pets, &r, in.Pet, actor, "pet.saved"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type FriendInput struct {
	CommandMeta
	Version int64  `json:"version"`
	UserID  int64  `json:"user_id"`
	Reason  string `json:"reason"`
}

func (s *Service) Friend(ctx context.Context, actor int64, action string, in FriendInput) (out Record, err error) {
	if in.UserID <= 0 || in.UserID == actor {
		return out, Invalid("invalid friend")
	}
	if err = textValid(in.Reason, 200, false); err != nil {
		return
	}
	err = s.command(ctx, actor, "friend."+action, 0, in, Friendships, &out, func(tx Tx) error {
		if HasString([]string{"block", "unblock", "decline", "withdraw", "remove"}, action) {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		} else if e := s.interaction(tx, actor); e != nil {
			return e
		}
		if e := s.active(tx, in.UserID); e != nil {
			return ErrNotFound
		}
		r, e := tx.ByKey(Friendships, pair(actor, in.UserID))
		fresh := errors.Is(e, ErrNotFound)
		if e != nil && !fresh {
			return e
		}
		f := Friend{RequesterID: actor, RecipientID: in.UserID, Reason: in.Reason}
		if fresh {
			r = Record{OwnerID: actor, Participants: []int64{in.UserID}, Key: pair(actor, in.UserID)}
		} else {
			if e = expected(r, in.Version); e != nil {
				return e
			}
			f, e = Decode[Friend](r)
			if e != nil {
				return e
			}
		}
		switch action {
		case "request":
			if !fresh && r.State != "declined" && r.State != "withdrawn" && r.State != "removed" {
				return ErrConflict
			}
			f = Friend{RequesterID: actor, RecipientID: in.UserID, Reason: in.Reason}
			r.OwnerID = actor
			r.Participants = []int64{in.UserID}
			r.State = "pending"
		case "accept", "decline":
			if r.State != "pending" || actor != f.RecipientID {
				return ErrForbidden
			}
			r.State = "declined"
			if action == "accept" {
				r.State = "accepted"
				now := s.Now()
				f.AcceptedAt = &now
			}
		case "withdraw":
			if r.State != "pending" || actor != f.RequesterID {
				return ErrForbidden
			}
			r.State = "withdrawn"
		case "remove":
			if r.State != "accepted" {
				return ErrConflict
			}
			r.State = "removed"
		case "block":
			if r.State == "blocked" && f.BlockedBy != actor {
				return ErrForbidden
			}
			r.State = "blocked"
			f.BlockedBy = actor
		case "unblock":
			if r.State != "blocked" || f.BlockedBy != actor {
				return ErrForbidden
			}
			r.State = "removed"
			f.BlockedBy = 0
		default:
			return Invalid("invalid friend action")
		}
		if e = save(tx, Friendships, &r, f, actor, "friend."+action); e != nil {
			return e
		}
		if action == "request" || action == "accept" {
			if e = s.notify(tx, in.UserID, actor, "interactions", Friendships, r.ID, "好友关系有更新"); e != nil {
				return e
			}
		}
		out = r
		return nil
	})
	return
}

type EntryInput struct {
	CommandMeta
	Declaration PublicationDeclaration `json:"declaration"`
	Version     int64                  `json:"version"`
	Entry       Entry                  `json:"entry"`
	MediaIDs    []int64                `json:"media_ids"`
}

func (s *Service) SaveEntry(ctx context.Context, actor, id int64, in EntryInput) (out Record, err error) {
	if e := textValid(in.Entry.Title, 80, false); e != nil {
		return out, e
	}
	if len(in.Entry.Tags) > 10 {
		return out, Invalid("too many tags")
	}
	for _, tag := range in.Entry.Tags {
		if e := textValid(tag, 30, true); e != nil {
			return out, e
		}
	}
	if in.Entry.Category != "" && !HasString([]string{"daily_share", "help", "activity", "trade"}, in.Entry.Category) {
		return out, Invalid("invalid category")
	}
	if id == 0 && in.Version != 0 {
		return out, ErrConflict
	}
	if err = textValid(in.Entry.Text, 5000, len(in.MediaIDs) == 0); err != nil {
		return
	}
	if err = textValid(in.Entry.City, 120, false); err != nil {
		return
	}
	if err = audienceValid(in.Entry.Audience); err != nil {
		return
	}
	switch in.Entry.Space {
	case "community":
		if in.Entry.Audience.Mode != "public" {
			return out, Invalid("community entries are public")
		}
	case "circle":
		if in.Entry.Audience.Mode == "public" {
			return out, Invalid("circle requires a private audience")
		}
	case "diary":
		if in.Entry.PetID <= 0 {
			return out, Invalid("diary requires a pet")
		}
	default:
		return out, Invalid("invalid space")
	}
	err = s.command(ctx, actor, "entry.save", id, in, Entries, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if in.Entry.Audience.Mode != "private" {
			if e := s.interaction(tx, actor); e != nil {
				return e
			}
		}
		if e := s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		if in.Entry.PetID > 0 {
			p, e := tx.Get(Pets, in.Entry.PetID)
			if e != nil {
				return e
			}
			if e = owner(p, actor); e != nil {
				return e
			}
		}
		if in.Entry.Audience.Mode == "public" {
			if e := s.publicAttachments(tx, actor, in.MediaIDs); e != nil {
				return e
			}
		}
		if in.Entry.Audience.Mode != "private" {
			in.Entry.ContentOrigin = ""
			preview, e := s.publicationManifest(tx, actor, in)
			if e != nil {
				return e
			}
			if !in.Declaration.Confirmed || preview.ManifestHash != in.Declaration.ManifestHash {
				return Denied("publication_declaration_required")
			}
			if e = s.requireSourceRecord(tx, actor, in.Declaration); e != nil {
				return e
			}
			in.Entry.ContentOrigin = in.Declaration.Origin
			if in.Declaration.Origin == "ai_generated" || in.Declaration.Origin == "ai_modified" {
				for _, mediaID := range in.MediaIDs {
					m, e := tx.Get(Media, mediaID)
					if e != nil {
						return e
					}
					file, e := Decode[MediaFile](m)
					if e != nil {
						return e
					}
					file.SourceLabel, e = PropagationLabel(file.SourceLabel, s.Propagator, file.SHA256, "1")
					if e != nil {
						return e
					}
					file.KnownAI = true
					if e = SetBody(&m, file); e != nil {
						return e
					}
					if e = tx.Save(Media, &m); e != nil {
						return e
					}
				}
			}
		}
		r := Record{OwnerID: actor, State: "published"}
		if id > 0 {
			var e error
			r, e = writable(tx, Entries, id, actor, in.Version)
			if e != nil {
				return e
			}
			if r.State == "deleted" {
				return ErrConflict
			}
		}
		r.RefID = in.Entry.PetID
		r.State = "published"
		r.MediaIDs = in.MediaIDs
		r.Public = in.Entry.Audience.Mode == "public"
		if e := save(tx, Entries, &r, in.Entry, actor, "entry.saved"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type CommentInput struct {
	Content        RichContent `json:"content"`
	ReplyTo        int64       `json:"reply_to"`
	IdempotencyKey string      `json:"idempotency_key"`
}

func (s *Service) Comment(ctx context.Context, actor, entry int64, in CommentInput) (Record, error) {
	return s.commentOn(ctx, actor, Entries, Comments, entry, in)
}
func (s *Service) PetComment(ctx context.Context, actor, pet int64, in CommentInput) (Record, error) {
	return s.commentOn(ctx, actor, Pets, PetComments, pet, in)
}

// 动态和宠物交流共用资格、原键、父资源权限及回复归属校验，使用各自独立的评论表。
func (s *Service) commentOn(ctx context.Context, actor int64, parentKind, commentKind Kind, entry int64, in CommentInput) (out Record, err error) {
	if err = contentValid(in.Content); err != nil {
		return
	}
	if len(in.IdempotencyKey) < 8 || len(in.IdempotencyKey) > 100 {
		return out, Invalid("idempotency key required")
	}
	command, message := "entry.comment", "动态有新评论"
	if parentKind == Pets {
		command, message = "pet.comment", "宠物领养交流有新评论"
	}
	err = s.command(ctx, actor, command, entry, in, commentKind, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		p, e := tx.Get(parentKind, entry)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, parentKind, p, actor)
		if e != nil {
			return e
		}
		if parentKind == Pets {
			pet, e := Decode[Pet](p)
			if e != nil {
				return e
			}
			if !pet.AdoptionAvailable && !pet.AdoptionOpened || !HasString([]string{"cat", "dog"}, pet.Species) {
				return ErrForbidden
			}
		}
		if !ok || parentKind == Entries && p.State != "published" || parentKind == Pets && p.State != "active" {
			return ErrForbidden
		}
		key := fmt.Sprintf("%d:%d:%s", actor, entry, in.IdempotencyKey)
		old, e := tx.ByKey(commentKind, key)
		if e == nil {
			body, e := Decode[Comment](old)
			if e != nil {
				return e
			}
			if body.RichContent != in.Content || body.ReplyTo != in.ReplyTo {
				return Denied("idempotency_conflict")
			}
			out = old
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		if in.ReplyTo > 0 {
			reply, e := tx.Get(commentKind, in.ReplyTo)
			if e != nil {
				return e
			}
			replyVisible, readError := s.canRead(tx, commentKind, reply, actor)
			if readError != nil {
				return readError
			}
			if reply.RefID != entry || !replyVisible {
				return Invalid("reply belongs to another entry")
			}
		}
		r := Record{OwnerID: actor, RefID: entry, Key: key, State: "published"}
		if e = save(tx, commentKind, &r, Comment{in.Content, in.ReplyTo}, actor, command+"ed"); e != nil {
			return e
		}
		out = r
		return s.notify(tx, p.OwnerID, actor, "interactions", parentKind, entry, message)
	})
	return
}
func (s *Service) React(ctx context.Context, actor, entry int64, liked bool) (out Record, err error) {
	err = s.mutate(ctx, actor, "entry.react", entry, struct {
		Liked bool `json:"liked"`
	}{liked}, Reactions, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		p, e := tx.Get(Entries, entry)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, Entries, p, actor)
		if e != nil {
			return e
		}
		if !ok || p.State != "published" {
			return ErrForbidden
		}
		r, e := tx.ByKey(Reactions, fmt.Sprintf("%d:%d", entry, actor))
		if errors.Is(e, ErrNotFound) {
			r = Record{OwnerID: actor, RefID: entry, Key: fmt.Sprintf("%d:%d", entry, actor)}
		} else if e != nil {
			return e
		}
		state := "removed"
		if liked {
			state = "active"
		}
		if r.ID > 0 && r.State == state {
			out = r
			return nil
		}
		r.State = state
		if e = save(tx, Reactions, &r, struct {
			Liked bool `json:"liked"`
		}{liked}, actor, "entry.reacted"); e != nil {
			return e
		}
		out = r
		if liked {
			return s.notify(tx, p.OwnerID, actor, "interactions", Entries, entry, "动态收到点赞")
		}
		return nil
	})
	return
}
func (s *Service) Remove(ctx context.Context, actor int64, k Kind, id, version int64) error {
	if k != Pets && k != Offers && k != Entries && k != Comments && k != PetComments && k != Drafts {
		return ErrForbidden
	}
	var out Record
	return s.mutate(ctx, actor, "resource.remove."+string(k), id, struct {
		Version int64 `json:"version"`
	}{version}, k, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, e := writable(tx, k, id, actor, version)
		if e != nil {
			return e
		}
		if k == Pets {
			occupied, e := otherPlacement(tx, id, 0)
			if e != nil {
				return e
			}
			if occupied {
				return Denied("pet_placement_conflict")
			}
		}
		r.State = "deleted"
		r.Public = false
		r.MediaIDs = nil
		if e = save(tx, k, &r, struct{}{}, actor, string(k)+".deleted"); e != nil {
			return e
		}
		out = r
		return nil
	})
}
func contentValid(c RichContent) error {
	if e := textValid(c.Text, 2000, c.StickerID == ""); e != nil {
		return e
	}
	if c.StickerID != "" && !validExpression(c.StickerID) {
		return Invalid("unknown sticker")
	}
	return nil
}

// The stable sticker IDs are shared with the mobile asset catalogue. Emoji are
// plain Unicode text; no image URLs or client-supplied HTML are accepted.
func validExpression(id string) bool {
	for n := 1; n <= 24; n++ {
		if id == fmt.Sprintf("sticker-%02d", n) {
			return true
		}
	}
	return false
}

type GroupInput struct {
	CommandMeta
	Name         string `json:"name"`
	Discoverable bool   `json:"discoverable"`
	City         string `json:"city"`
	Summary      string `json:"summary"`
}

func (s *Service) CreateGroup(ctx context.Context, actor int64, in GroupInput) (out Record, err error) {
	if err = textValid(in.Name, 60, true); err != nil {
		return
	}
	if err = textValid(in.City, 120, in.Discoverable); err != nil {
		return
	}
	if err = textValid(in.Summary, 1000, false); err != nil {
		return
	}
	err = s.command(ctx, actor, "group.create", 0, in, Groups, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		r := Record{OwnerID: actor, Participants: []int64{actor}, State: "active", Public: in.Discoverable}
		if e := save(tx, Groups, &r, Group{Name: in.Name, Members: []int64{actor}, Muted: []int64{}, Discoverable: in.Discoverable, City: in.City, Summary: in.Summary}, actor, "group.created"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type GroupAction struct {
	CommandMeta
	Version      int64  `json:"version"`
	UserID       int64  `json:"user_id"`
	Notice       string `json:"notice"`
	Discoverable bool   `json:"discoverable"`
	City         string `json:"city"`
	Summary      string `json:"summary"`
}

func (s *Service) Group(ctx context.Context, actor, id int64, action string, in GroupAction) (out Record, err error) {
	err = s.command(ctx, actor, "group."+action, id, in, Groups, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if action != "leave" && action != "dissolve" {
			if e := s.interaction(tx, actor); e != nil {
				return e
			}
		}
		r, e := tx.Get(Groups, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		g, e := Decode[Group](r)
		if e != nil {
			return e
		}
		if r.State != "active" || !Has(g.Members, actor) {
			return ErrForbidden
		}
		isOwner := actor == r.OwnerID
		switch action {
		case "directory":
			if !isOwner {
				return ErrForbidden
			}
			if e = textValid(in.City, 120, in.Discoverable); e != nil {
				return e
			}
			if e = textValid(in.Summary, 1000, false); e != nil {
				return e
			}
			g.Discoverable = in.Discoverable
			g.City = in.City
			g.Summary = in.Summary
			r.Public = in.Discoverable
		case "notice":
			if !isOwner {
				return ErrForbidden
			}
			if e = textValid(in.Notice, 2000, false); e != nil {
				return e
			}
			g.Notice = in.Notice
		case "transfer":
			if !isOwner || in.UserID == actor || !Has(g.Members, in.UserID) {
				return ErrForbidden
			}
			if g.PendingTransferTo > 0 && g.TransferExpiresAt != nil && g.TransferExpiresAt.After(s.Now()) {
				return ErrConflict
			}
			g.PendingTransferTo = in.UserID
			until := s.Now().Add(48 * time.Hour)
			g.TransferExpiresAt = &until
		case "accept_transfer", "decline_transfer":
			if actor != g.PendingTransferTo || g.TransferExpiresAt == nil || !g.TransferExpiresAt.After(s.Now()) {
				return ErrConflict
			}
			if action == "accept_transfer" {
				r.OwnerID = actor
				if e = s.transferGroupRequests(tx, id, actor); e != nil {
					return e
				}
			}
			g.PendingTransferTo = 0
			g.TransferExpiresAt = nil
		case "revoke_transfer":
			if !isOwner {
				return ErrForbidden
			}
			g.PendingTransferTo = 0
			g.TransferExpiresAt = nil
		case "dissolve":
			if !isOwner {
				return ErrForbidden
			}
			r.State = "dissolved"
			r.Public = false
			g.PendingTransferTo = 0
			g.TransferExpiresAt = nil
		case "leave", "remove_member":
			target := actor
			if action == "remove_member" {
				if !isOwner {
					return ErrForbidden
				}
				target = in.UserID
			}
			if target == r.OwnerID {
				return Invalid("transfer ownership before leaving")
			}
			g.Members = without(g.Members, target)
			g.Muted = without(g.Muted, target)
			if g.PendingTransferTo == target {
				g.PendingTransferTo = 0
				g.TransferExpiresAt = nil
			}
		case "mute":
			if Has(g.Muted, actor) {
				g.Muted = without(g.Muted, actor)
			} else {
				g.Muted = append(g.Muted, actor)
			}
		default:
			return Invalid("invalid group action")
		}
		r.Participants = g.Members
		if e = save(tx, Groups, &r, g, actor, "group."+action); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
func without(ids []int64, id int64) []int64 {
	out := make([]int64, 0, len(ids))
	for _, v := range ids {
		if v != id {
			out = append(out, v)
		}
	}
	return out
}

type InviteInput struct {
	CommandMeta
	Scope    string `json:"scope"`
	TargetID int64  `json:"target_id"`
	UserID   int64  `json:"user_id"`
}

func (s *Service) Invite(ctx context.Context, actor int64, in InviteInput) (out Record, err error) {
	err = s.command(ctx, actor, "invitation.create", 0, in, Invitations, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		if in.UserID == actor {
			return Invalid("cannot invite yourself")
		}
		if e := s.active(tx, in.UserID); e != nil {
			return e
		}
		b, e := blocked(tx, actor, in.UserID)
		if e != nil {
			return e
		}
		if b {
			return ErrForbidden
		}
		var k Kind
		switch in.Scope {
		case "group_member":
			k = Groups
		case "adoption_editor":
			k = Pets
		case "caretaker":
			k = Adoptions
		default:
			return Invalid("invalid invitation scope")
		}
		target, e := tx.Get(k, in.TargetID)
		if e != nil {
			return e
		}
		if k == Adoptions {
			a, e := Decode[Adoption](target)
			if e != nil {
				return e
			}
			if actor != a.ApplicantID {
				return ErrForbidden
			}
			if a.CompletedAt == nil || s.Now().After(a.CompletedAt.Add(30*24*time.Hour)) {
				return ErrConflict
			}
		} else if e = owner(target, actor); e != nil {
			return e
		}
		if k == Groups && target.State != "active" {
			return ErrConflict
		}
		if k == Pets {
			p, e := Decode[Pet](target)
			if e != nil {
				return e
			}
			if !p.AdoptionAvailable {
				return ErrConflict
			}
		}
		if k == Adoptions && target.State != "completed" {
			return ErrConflict
		}
		key := fmt.Sprintf("%s:%d:%d", in.Scope, in.TargetID, in.UserID)
		r, e := tx.ByKey(Invitations, key)
		accepted := e == nil && r.State == "accepted"
		if accepted && k == Groups {
			g, err := Decode[Group](target)
			if err != nil {
				return err
			}
			accepted = Has(g.Members, in.UserID)
		}
		if e == nil && (accepted || r.OwnerID == actor && r.State == "pending" && s.Now().Before(mustInviteExpiry(r))) {
			return ErrConflict
		}
		if e != nil && !errors.Is(e, ErrNotFound) {
			return e
		}
		if r.ID == 0 {
			r = Record{OwnerID: actor, Participants: []int64{in.UserID}, RefID: in.TargetID, Key: key}
		}
		r.OwnerID = actor
		r.Participants = []int64{in.UserID}
		r.State = "pending"
		i := Invitation{in.Scope, in.TargetID, in.UserID, s.Now().Add(72 * time.Hour)}
		if e = save(tx, Invitations, &r, i, actor, "invitation.created"); e != nil {
			return e
		}
		out = r
		return s.notify(tx, in.UserID, actor, "applications", Invitations, r.ID, "收到新邀请")
	})
	return
}
func mustInviteExpiry(r Record) time.Time { i, _ := Decode[Invitation](r); return i.ExpiresAt }
func (s *Service) Invitation(ctx context.Context, actor, id, version int64, action string) (out Record, err error) {
	err = s.mutate(ctx, actor, "invitation."+action, id, struct {
		Version int64 `json:"version"`
	}{version}, Invitations, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		r, e := tx.Get(Invitations, id)
		if e != nil {
			return e
		}
		if e = expected(r, version); e != nil {
			return e
		}
		i, e := Decode[Invitation](r)
		if e != nil {
			return e
		}
		switch action {
		case "accept", "decline":
			if actor != i.RecipientID || r.State != "pending" || !i.ExpiresAt.After(s.Now()) {
				return ErrConflict
			}
			r.State = "declined"
			if action == "accept" {
				r.State = "accepted"
				if i.Scope == "group_request" {
					gRecord, e := tx.Get(Groups, i.TargetID)
					if e != nil {
						return e
					}
					g, e := Decode[Group](gRecord)
					if e != nil {
						return e
					}
					if gRecord.OwnerID != actor || gRecord.State != "active" || !g.Discoverable || len(g.Members) >= 200 {
						return ErrConflict
					}
					if e = s.interaction(tx, r.OwnerID); e != nil {
						return e
					}
					b, e := blocked(tx, actor, r.OwnerID)
					if e != nil {
						return e
					}
					if b {
						return ErrForbidden
					}
					g.Members = Unique(append(g.Members, r.OwnerID))
					gRecord.Participants = g.Members
					if e = save(tx, Groups, &gRecord, g, actor, "group.member_joined"); e != nil {
						return e
					}
					if e = s.notify(tx, r.OwnerID, actor, "applications", Groups, gRecord.ID, "入群申请已被接受"); e != nil {
						return e
					}
				}
				if i.Scope == "group_member" {
					gRecord, e := tx.Get(Groups, i.TargetID)
					if e != nil {
						return e
					}
					g, e := Decode[Group](gRecord)
					if e != nil {
						return e
					}
					if gRecord.State != "active" || gRecord.OwnerID != r.OwnerID || len(g.Members) >= 200 {
						return ErrConflict
					}
					g.Members = Unique(append(g.Members, actor))
					gRecord.Participants = g.Members
					if e = save(tx, Groups, &gRecord, g, actor, "group.member_joined"); e != nil {
						return e
					}
				}
			}
		case "revoke":
			if e = owner(r, actor); e != nil {
				return e
			}
			if r.State != "pending" && r.State != "accepted" {
				return ErrConflict
			}
			if i.Scope == "group_request" && r.State == "accepted" {
				return Invalid("use the explicit group leave action")
			}
			r.State = "revoked"
			if i.Scope == "group_member" {
				gRecord, e := tx.Get(Groups, i.TargetID)
				if e != nil {
					return e
				}
				g, e := Decode[Group](gRecord)
				if e != nil {
					return e
				}
				if gRecord.OwnerID != actor {
					return ErrForbidden
				}
				if i.RecipientID == gRecord.OwnerID {
					return ErrConflict
				}
				g.Members = without(g.Members, i.RecipientID)
				gRecord.Participants = g.Members
				if e = save(tx, Groups, &gRecord, g, actor, "group.member_removed"); e != nil {
					return e
				}
			}
		default:
			return Invalid("invalid invitation action")
		}
		if e = save(tx, Invitations, &r, i, actor, "invitation."+action); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type ConversationInput struct {
	CommandMeta
	UserID int64 `json:"user_id"`
}

func (s *Service) Conversation(ctx context.Context, actor int64, in ConversationInput) (out Record, err error) {
	err = s.command(ctx, actor, "conversation.create", 0, in, Conversations, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		if in.UserID == actor {
			return Invalid("invalid recipient")
		}
		if e := s.active(tx, in.UserID); e != nil {
			return e
		}
		if e := s.directContact(tx, actor, in.UserID); e != nil {
			return e
		}
		r, e := tx.ByKey(Conversations, pair(actor, in.UserID))
		if e == nil {
			if r.State != "accepted" {
				isFriend, err := friends(tx, actor, in.UserID)
				if err != nil {
					return err
				}
				if isFriend {
					r.State = "accepted"
					if e = save(tx, Conversations, &r, mustBody(r), actor, "conversation.friend_accepted"); e != nil {
						return e
					}
				}
			}
			out = r
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		state := "pending"
		f, e := friends(tx, actor, in.UserID)
		if e != nil {
			return e
		}
		if f {
			state = "accepted"
		}
		r = Record{OwnerID: actor, Participants: []int64{in.UserID}, Key: pair(actor, in.UserID), State: state}
		if e = save(tx, Conversations, &r, Conversation{RequesterID: actor, RecipientID: in.UserID}, actor, "conversation.created"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
func (s *Service) ConversationAction(ctx context.Context, actor, id, version int64, action string) (out Record, err error) {
	err = s.mutate(ctx, actor, "conversation."+action, id, struct {
		Version int64 `json:"version"`
	}{version}, Conversations, &out, func(tx Tx) error {
		if action == "decline" {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		} else if e := s.interaction(tx, actor); e != nil {
			return e
		}
		r, e := tx.Get(Conversations, id)
		if e != nil {
			return e
		}
		if e = expected(r, version); e != nil {
			return e
		}
		c, e := Decode[Conversation](r)
		if e != nil {
			return e
		}
		if c.RecipientID != actor || r.State != "pending" {
			return ErrForbidden
		}
		if e = s.leadConversationContext(tx, r, c, actor, false); e != nil {
			return e
		}
		if action != "accept" && action != "decline" {
			return Invalid("invalid request action")
		}
		r.State = "declined"
		if action == "accept" {
			if e = s.leadConversationContext(tx, r, c, actor, true); e != nil {
				return e
			}
			if e := s.directContact(tx, c.RequesterID, c.RecipientID); e != nil {
				return e
			}
			r.State = "accepted"
		}
		if e = save(tx, Conversations, &r, c, actor, "conversation."+action); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}

type MessageInput struct {
	Content        RichContent `json:"content"`
	MediaIDs       []int64     `json:"media_ids"`
	IdempotencyKey string      `json:"idempotency_key"`
}

func (s *Service) Message(ctx context.Context, actor, parentID int64, group bool, in MessageInput) (out Record, err error) {
	if len(in.MediaIDs) == 0 {
		if err = contentValid(in.Content); err != nil {
			return
		}
	} else if err = textValid(in.Content.Text, 2000, false); err != nil {
		return
	}
	if in.Content.StickerID != "" && !validExpression(in.Content.StickerID) {
		return out, Invalid("unknown sticker")
	}
	if len(in.IdempotencyKey) < 8 || len(in.IdempotencyKey) > 100 {
		return out, Invalid("idempotency key required")
	}
	err = s.command(ctx, actor, "message.send", parentID, in, func() Kind {
		if group {
			return GroupMessages
		}
		return Messages
	}(), &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		parent, k := Conversations, Messages
		if group {
			parent, k = Groups, GroupMessages
		}
		p, e := tx.Get(parent, parentID)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, parent, p, actor)
		if e != nil {
			return e
		}
		if !ok {
			return ErrForbidden
		}
		key := fmt.Sprintf("%d:%d:%s", actor, parentID, in.IdempotencyKey)
		old, e := tx.ByKey(k, key)
		if e == nil {
			body, e := Decode[Message](old)
			if e != nil {
				return e
			}
			if body.RichContent != in.Content || !sameIDs(old.MediaIDs, in.MediaIDs) {
				return Denied("idempotency_conflict")
			}
			out = old
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		if !group {
			c, e := Decode[Conversation](p)
			if e != nil {
				return e
			}
			if e = s.leadConversationContext(tx, p, c, actor, true); e != nil {
				return e
			}
			if e := s.directContact(tx, c.RequesterID, c.RecipientID); e != nil {
				return e
			}
			if p.State == "pending" {
				if actor != c.RequesterID || len(in.MediaIDs) > 0 || in.Content.StickerID != "" {
					return ErrForbidden
				}
				messages, e := tx.List(Messages, Filter{RefID: parentID, Limit: 1})
				if e != nil {
					return e
				}
				if len(messages) > 0 {
					return Invalid("wait for the recipient to accept")
				}
			} else if p.State != "accepted" {
				return ErrForbidden
			}
		}
		if e = s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		r := Record{OwnerID: actor, RefID: parentID, Key: key, MediaIDs: in.MediaIDs, State: "sent"}
		if e = save(tx, k, &r, Message{in.Content}, actor, "message.sent"); e != nil {
			return e
		}
		out = r
		recipients := p.Participants
		category := "direct_messages"
		if group {
			category = "groups"
		}
		recipients = Unique(append(append([]int64{}, recipients...), p.OwnerID))
		for _, recipient := range recipients {
			if group {
				g, e := Decode[Group](p)
				if e != nil {
					return e
				}
				if Has(g.Muted, recipient) {
					continue
				}
			}
			if e = s.notify(tx, recipient, actor, category, parent, parentID, "收到新消息"); e != nil {
				return e
			}
		}
		return nil
	})
	return
}

type SettingsInput struct {
	Version     int64            `json:"version"`
	Preference  *Preference      `json:"preference,omitempty"`
	Expressions *ExpressionState `json:"expressions,omitempty"`
	Draft       *Draft           `json:"draft,omitempty"`
	MediaIDs    []int64          `json:"media_ids"`
	Key         string           `json:"key"`
}

func (s *Service) Settings(ctx context.Context, actor int64, k Kind, in SettingsInput) (out Record, err error) {
	var body any
	key := fmt.Sprint(actor)
	switch k {
	case Preferences:
		if in.Preference == nil {
			return out, Invalid("preference required")
		}
		if !HasString([]string{"instant", "digest", "off"}, in.Preference.Frequency) {
			return out, Invalid("invalid frequency")
		}
		body = in.Preference
	case Expressions:
		if in.Expressions == nil {
			return out, Invalid("expressions required")
		}
		if len(in.Expressions.Favorites) > 200 || len(in.Expressions.Recent) > 50 {
			return out, Invalid("too many expressions")
		}
		for _, id := range append(append([]string{}, in.Expressions.Favorites...), in.Expressions.Recent...) {
			if !validFavorite(id) {
				return out, Invalid("invalid expression")
			}
		}
		body = in.Expressions
	case Drafts:
		if in.Draft == nil || len(in.Key) < 1 || len(in.Key) > 80 {
			return out, Invalid("draft and key required")
		}
		if e := textValid(in.Draft.Text, 10000, false); e != nil {
			return out, e
		}
		if e := audienceValid(in.Draft.Audience); e != nil {
			return out, e
		}
		if in.Draft.Cursor < 0 {
			return out, Invalid("invalid cursor")
		}
		key += ":" + in.Key
		body = in.Draft
	default:
		return out, ErrForbidden
	}
	err = s.mutate(ctx, actor, string(k)+".settings", 0, in, k, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if e := s.attachments(tx, actor, in.MediaIDs); e != nil {
			return e
		}
		r, e := tx.ByKey(k, key)
		if errors.Is(e, ErrNotFound) {
			if in.Version != 0 {
				return ErrConflict
			}
			r = Record{OwnerID: actor, Key: key}
		} else if e != nil {
			return e
		} else if e = expected(r, in.Version); e != nil {
			return e
		}
		r.State = "active"
		if k == Preferences {
			preference := *in.Preference
			if !preference.PushPermission || preference.Frequency == "off" {
				if e = DisablePushDevices(tx, actor, "", true); e != nil {
					return e
				}
			}
			preference.Content = nil
			if r.Version > 0 {
				previous, e := Decode[Preference](r)
				if e != nil {
					return e
				}
				preference.Content = previous.Content
			}
			body = preference
		}
		if k == Drafts {
			r.MediaIDs = in.MediaIDs
		}
		if e = save(tx, k, &r, body, actor, string(k)+".saved"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
func HasString(values []string, v string) bool {
	for _, s := range values {
		if s == v {
			return true
		}
	}
	return false
}
func sameIDs(a, b []int64) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}
func (s *Service) ReadNotification(ctx context.Context, actor, id, version int64) (out Record, err error) {
	err = s.mutate(ctx, actor, "notification.read", id, struct {
		Version int64 `json:"version"`
	}{version}, Notifications, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, e := writable(tx, Notifications, id, actor, version)
		if e != nil {
			return e
		}
		n, e := Decode[Notification](r)
		if e != nil {
			return e
		}
		n.Read = true
		r.State = "read"
		if e = save(tx, Notifications, &r, n, actor, "notification.read"); e != nil {
			return e
		}
		out, e = project(Notifications, r, actor)
		return e
	})
	return
}
