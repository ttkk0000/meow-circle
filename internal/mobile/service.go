package mobile

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"strings"
	"time"
	"unicode/utf8"
)

type Service struct {
	Repo       Repository
	Now        func() time.Time
	Propagator string
	Push       PushSender
}

func New(repo Repository) *Service {
	return &Service{Repo: repo, Now: func() time.Time { return time.Now().UTC() }}
}

func textValid(value string, max int, required bool) error {
	if !utf8.ValidString(value) || utf8.RuneCountInString(value) > max || (required && strings.TrimSpace(value) == "") {
		return Invalid("invalid text (maximum %d characters)", max)
	}
	return nil
}
func pair(a, b int64) string {
	if a > b {
		a, b = b, a
	}
	return fmt.Sprintf("%d:%d", a, b)
}
func owner(r Record, actor int64) error {
	if actor <= 0 || r.OwnerID != actor {
		return ErrForbidden
	}
	return nil
}
func participant(r Record, actor int64) bool {
	return actor > 0 && (r.OwnerID == actor || Has(r.Participants, actor))
}
func expected(r Record, version int64) error {
	if version < 1 || version != r.Version {
		return ErrConflict
	}
	return nil
}
func writable(tx Tx, k Kind, id, actor, version int64) (Record, error) {
	r, err := tx.Get(k, id)
	if err != nil {
		return r, err
	}
	if err = owner(r, actor); err != nil {
		return r, err
	}
	return r, expected(r, version)
}
func (s *Service) active(tx Tx, actor int64) error {
	if actor <= 0 {
		return ErrUnauthorized
	}
	if _, err := tx.User(actor); err != nil {
		return err
	}
	r, err := tx.ByKey(Accounts, fmt.Sprint(actor))
	if errors.Is(err, ErrNotFound) {
		return nil
	}
	if err != nil {
		return err
	}
	if r.State != "active" {
		return ErrUnauthorized
	}
	return nil
}
func blocked(tx Tx, a, b int64) (bool, error) {
	r, err := tx.ByKey(Friendships, pair(a, b))
	if errors.Is(err, ErrNotFound) {
		return false, nil
	}
	if err != nil {
		return false, err
	}
	return r.State == "blocked", nil
}
func friends(tx Tx, a, b int64) (bool, error) {
	if a == b && a > 0 {
		return true, nil
	}
	r, err := tx.ByKey(Friendships, pair(a, b))
	if errors.Is(err, ErrNotFound) {
		return false, nil
	}
	if err != nil {
		return false, err
	}
	return r.State == "accepted", nil
}
func audienceValid(a Audience) error {
	switch a.Mode {
	case "private", "public", "friends":
		if len(a.UserIDs) > 0 {
			return Invalid("audience does not accept user IDs")
		}
	case "selected", "excluded":
		if len(a.UserIDs) > 200 {
			return Invalid("too many audience users")
		}
	default:
		return Invalid("invalid audience")
	}
	return nil
}
func visible(tx Tx, r Record, a Audience, actor int64) (bool, error) {
	if r.OwnerID == actor && actor > 0 {
		return true, nil
	}
	if actor > 0 {
		b, err := blocked(tx, r.OwnerID, actor)
		if err != nil || b {
			return false, err
		}
	}
	switch a.Mode {
	case "public":
		return true, nil
	case "selected":
		if actor <= 0 || !Has(a.UserIDs, actor) {
			return false, nil
		}
		return friends(tx, r.OwnerID, actor)
	case "friends":
		return friends(tx, r.OwnerID, actor)
	case "excluded":
		if Has(a.UserIDs, actor) {
			return false, nil
		}
		return friends(tx, r.OwnerID, actor)
	}
	return false, nil
}
func (s *Service) attachments(tx Tx, actor int64, ids []int64) error {
	if len(ids) > 9 || len(Unique(ids)) != len(ids) {
		return Invalid("use up to 9 distinct images")
	}
	for _, id := range ids {
		r, err := tx.Get(Media, id)
		if err != nil {
			return err
		}
		if r.OwnerID != actor || r.State != "ready" {
			return ErrForbidden
		}
	}
	return nil
}

func (s *Service) publicAttachments(tx Tx, actor int64, ids []int64) error {
	if e := s.attachments(tx, actor, ids); e != nil {
		return e
	}
	for _, id := range ids {
		r, e := tx.Get(Media, id)
		if e != nil {
			return e
		}
		m, e := Decode[MediaFile](r)
		if e != nil {
			return e
		}
		if m.Scope != "public" {
			return Invalid("private evidence cannot be attached to a public publication; upload explicitly for public use")
		}
	}
	return nil
}
func (s *Service) factualAttachments(tx Tx, actor int64, ids []int64) error {
	if e := s.attachments(tx, actor, ids); e != nil {
		return e
	}
	for _, id := range ids {
		r, e := tx.Get(Media, id)
		if e != nil {
			return e
		}
		v, e := Decode[MediaFile](r)
		if e != nil {
			return e
		}
		if v.KnownAI {
			return Denied("generated_media_is_not_factual_evidence")
		}
	}
	return nil
}
func save(tx Tx, k Kind, r *Record, body any, actor int64, topic string) error {
	if err := SetBody(r, body); err != nil {
		return err
	}
	if err := tx.Save(k, r); err != nil {
		return err
	}
	return emit(tx, k, *r, actor, topic)
}
func (s *Service) notify(tx Tx, recipient, actor int64, category string, k Kind, id int64, message string) error {
	if recipient <= 0 || recipient == actor {
		return nil
	}
	push := "suppressed" // Device delivery is a separate outbox consumer, never a fake success.
	var deliverAfter *time.Time
	r, err := tx.ByKey(Preferences, fmt.Sprint(recipient))
	if err != nil && !errors.Is(err, ErrNotFound) {
		return err
	}
	if err == nil {
		p, e := Decode[Preference](r)
		if e != nil {
			return e
		}
		enabled := map[string]bool{"direct_messages": p.DirectMessages, "groups": p.Groups, "applications": p.Applications, "interactions": p.Interactions}[category]
		if enabled && p.PushPermission && p.Frequency != "off" {
			push = "queued"
			if p.Frequency == "digest" {
				next := s.Now().Truncate(24 * time.Hour).Add(24 * time.Hour)
				deliverAfter = &next
			}
		}
	}
	n := Record{OwnerID: recipient, State: "unread"}
	return save(tx, Notifications, &n, Notification{Category: category, Text: message, TargetKind: k, TargetID: id, PushState: push, DeliverAfter: deliverAfter}, actor, "notification.created")
}
func publicKind(k Kind) bool {
	switch k {
	case Pets, Offers, Entries, Cases, Comments, PetComments, Reactions:
		return true
	}
	return false
}
func queryKind(k Kind) bool {
	switch k {
	case Pets, Offers, Entries, Comments, PetComments, Reactions, Friendships, Cases, Claims, Leads, Subscriptions, Appointments, Adoptions, CareUpdates, Groups, GroupMessages, Conversations, Messages, Invitations, Notifications, Preferences, Expressions, Drafts, Media, Reviews, VisitFeedback:
		return true
	}
	return false
}

func (s *Service) canRead(tx Tx, k Kind, r Record, actor int64) (bool, error) {
	if actor != r.OwnerID && (publicKind(k) || k == Media) {
		ok, e := s.publicProcessingAllowed(tx, r.OwnerID)
		if e != nil || !ok {
			return false, e
		}
	}
	if k == Guardians || k == DataRequests || k == RightsCases {
		return participant(r, actor), nil
	}
	if !queryKind(k) {
		return false, ErrForbidden
	}
	if k == Pets {
		p, e := Decode[Pet](r)
		if e != nil {
			return false, e
		}
		if r.State != "active" {
			return false, nil
		}
		ok, e := visible(tx, r, p.Audience, actor)
		if e != nil || ok {
			return ok, e
		}
		if p.AdoptionAvailable {
			return s.collaborator(tx, "adoption_editor", r.ID, actor)
		}
		return false, nil
	}
	if k == Entries {
		p, e := Decode[Entry](r)
		if e != nil {
			return false, e
		}
		if r.State != "published" {
			return r.OwnerID == actor && actor > 0, nil
		}
		return visible(tx, r, p.Audience, actor)
	}
	if k == Cases {
		return r.State != "removed", nil
	}
	if k == Offers {
		o, e := Decode[Offer](r)
		if e != nil {
			return false, e
		}
		return r.OwnerID == actor && actor > 0 || r.State == "published" && o.ValidUntil.After(s.Now()), nil
	}
	if k == Comments || k == PetComments {
		if r.State != "published" {
			return false, nil
		}
		if actor > 0 && actor != r.OwnerID {
			denied, e := blocked(tx, r.OwnerID, actor)
			if e != nil || denied {
				return false, e
			}
		}
		parentKind := Entries
		if k == PetComments {
			parentKind = Pets
		}
		parent, e := tx.Get(parentKind, r.RefID)
		if e != nil {
			return false, e
		}
		return s.canRead(tx, parentKind, parent, actor)
	}
	if k == Reactions {
		p, e := tx.Get(Entries, r.RefID)
		if e != nil {
			return false, e
		}
		return s.canRead(tx, Entries, p, actor)
	}
	if k == Messages || k == GroupMessages {
		parent := Conversations
		if k == GroupMessages {
			parent = Groups
		}
		p, e := tx.Get(parent, r.RefID)
		if e != nil {
			return false, e
		}
		return s.canRead(tx, parent, p, actor)
	}
	if k == CareUpdates {
		p, e := tx.Get(Adoptions, r.RefID)
		if e != nil {
			return false, e
		}
		if participant(p, actor) {
			return true, nil
		}
		return s.collaborator(tx, "caretaker", p.ID, actor)
	}
	if k == VisitFeedback {
		p, e := tx.Get(Appointments, r.RefID)
		if e != nil {
			return false, e
		}
		return participant(p, actor), nil
	}
	if k == Media {
		return s.mediaAccess(tx, r, actor)
	}
	if k == Groups {
		g, e := Decode[Group](r)
		return e == nil && r.State == "active" && Has(g.Members, actor), e
	}
	if k == Conversations {
		if !participant(r, actor) {
			return false, nil
		}
		c, e := Decode[Conversation](r)
		if e != nil {
			return false, e
		}
		b, e := blocked(tx, c.RequesterID, c.RecipientID)
		return !b, e
	}
	return participant(r, actor), nil
}
func (s *Service) collaborator(tx Tx, scope string, target, actor int64) (bool, error) {
	if actor <= 0 {
		return false, nil
	}
	r, e := tx.ByKey(Invitations, fmt.Sprintf("%s:%d:%d", scope, target, actor))
	if errors.Is(e, ErrNotFound) {
		return false, nil
	}
	if e != nil {
		return false, e
	}
	return r.State == "accepted", nil
}
func project(k Kind, r Record, actor int64) (Record, error) {
	if k == Guardians {
		v, e := Decode[GuardianConsent](r)
		if e != nil {
			return r, e
		}
		v.EvidenceRef = ""
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	if k == DataRequests {
		return redactDataRequest(r), nil
	}
	if k == Notifications {
		v, e := Decode[Notification](r)
		if e != nil {
			return r, e
		}
		v.PushReceipts = nil
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	if k == Offers && r.OwnerID != actor {
		v, e := Decode[Offer](r)
		if e != nil {
			return r, e
		}
		v.PrivateAddress = ""
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	if k == Cases && r.OwnerID != actor {
		v, e := Decode[Case](r)
		if e != nil {
			return r, e
		}
		v.PrivateLocation = ""
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	if k == Pets && r.OwnerID != actor {
		v, e := Decode[Pet](r)
		if e != nil {
			return r, e
		}
		v.Health = ""
		v.Audience.UserIDs = nil
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	if k == Entries && r.OwnerID != actor {
		v, e := Decode[Entry](r)
		if e != nil {
			return r, e
		}
		v.Audience.UserIDs = nil
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	if k == Media {
		v, e := Decode[MediaFile](r)
		if e != nil {
			return r, e
		}
		v.Filename = ""
		v.UploadLeaseUntil = nil
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	if k == Reviews {
		v, e := Decode[Review](r)
		if e != nil {
			return r, e
		}
		v.InternalNote = ""
		if e = SetBody(&r, v); e != nil {
			return r, e
		}
	}
	return r, nil
}
func (s *Service) Get(ctx context.Context, actor int64, k Kind, id int64) (out Record, err error) {
	if !queryKind(k) {
		return out, ErrForbidden
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if actor > 0 {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		} else if !publicKind(k) {
			return ErrUnauthorized
		}
		r, e := tx.Get(k, id)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, k, r, actor)
		if e != nil {
			return e
		}
		if !ok {
			return ErrNotFound
		}
		out, e = project(k, r, actor)
		return e
	})
	return
}

type Page struct {
	Items []Record `json:"items"`
	Next  int64    `json:"next_cursor"`
}

func (s *Service) List(ctx context.Context, actor int64, k Kind, after, ref int64, limit int) (out Page, err error) {
	return s.ListFiltered(ctx, actor, k, after, ref, limit, Query{})
}

func (s *Service) ListFiltered(ctx context.Context, actor int64, k Kind, after, ref int64, limit int, q Query) (out Page, err error) {
	if len(q.Keyword) > 120 || len(q.City) > 120 || math.IsNaN(q.RadiusKM) || math.IsInf(q.RadiusKM, 0) || q.RadiusKM < 0 || q.RadiusKM > 100 || (q.RadiusKM > 0 && !coordinates(q.Latitude, q.Longitude)) {
		return out, Invalid("invalid search filters")
	}
	out.Items = make([]Record, 0)
	if !queryKind(k) {
		return out, ErrForbidden
	}
	if limit < 1 || limit > 100 {
		limit = 30
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if actor > 0 {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		} else if !publicKind(k) {
			return ErrUnauthorized
		}
		f := Filter{After: after, RefID: ref, Limit: 500}
		var content ContentSubscriptions
		if q.Following || q.Watched {
			if actor <= 0 {
				return ErrUnauthorized
			}
			if q.Following && k != Entries || q.Watched && k != Cases {
				return Invalid("invalid subscription filter")
			}
			_, p, e := contentPreferences(tx, actor)
			if e != nil {
				return e
			}
			content = contentBody(p)
		}
		if !publicKind(k) && k != Comments && k != Reactions && k != CareUpdates && k != Messages && k != GroupMessages && k != VisitFeedback {
			f.ParticipantID = actor
		}
		records, e := tx.List(k, f)
		if e != nil {
			return e
		}
		for _, r := range records {
			out.Next = r.ID
			if q.Following && !Has(content.FollowedUsers, r.OwnerID) || q.Watched && !Has(content.WatchedCases, r.ID) {
				continue
			}
			ok, e := s.canRead(tx, k, r, actor)
			if e != nil {
				return e
			}
			if !ok {
				continue
			}
			match, e := matches(k, r, q)
			if e != nil {
				return e
			}
			if !match {
				continue
			}
			p, e := project(k, r, actor)
			if e != nil {
				return e
			}
			out.Items = append(out.Items, p)
			if len(out.Items) == limit {
				break
			}
		}
		return nil
	})
	return
}

// Payload rejects misspelled fields, including fields belonging to other actions.
func Payload[T any](data json.RawMessage) (T, error) {
	var v T
	d := json.NewDecoder(strings.NewReader(string(data)))
	d.DisallowUnknownFields()
	err := d.Decode(&v)
	return v, err
}
