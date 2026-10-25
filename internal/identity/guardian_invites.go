package identity

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"kitty-circle/internal/mobile"
	"net/url"
	"strings"
	"time"
)

type GuardianInviteInfo struct {
	Available     bool   `json:"available"`
	PolicyVersion string `json:"policy_version"`
	PolicyLabel   string `json:"policy_label"`
	PolicyKey     string `json:"policy_key"`
}
type GuardianInviteStart struct {
	mobile.CommandMeta
	PolicyVersion string `json:"policy_version"`
	Accepted      bool   `json:"accepted"`
}
type GuardianInviteClaim struct {
	mobile.CommandMeta
	Token         string `json:"token"`
	PolicyVersion string `json:"policy_version"`
	Accepted      bool   `json:"accepted"`
}
type GuardianInviteStatus struct {
	ID               int64     `json:"id"`
	Version          int64     `json:"version"`
	State            string    `json:"state"`
	PolicyVersion    string    `json:"policy_version"`
	ExpiresAt        time.Time `json:"expires_at"`
	GuardianRecordID int64     `json:"guardian_record_id"`
	ShareURL         string    `json:"share_url"`
	Mine             bool      `json:"mine"`
	CanCancel        bool      `json:"can_cancel"`
}
type guardianInvitePointer struct {
	ID        int64     `json:"id"`
	ExpiresAt time.Time `json:"expires_at"`
}
type GuardianInvitePage struct {
	Items      []GuardianInviteStatus `json:"items"`
	NextCursor int64                  `json:"next_cursor"`
}

func (s *Service) guardianToken(nonce string) string {
	h := hmac.New(sha256.New, s.Secret)
	_, _ = h.Write([]byte("M&D guardian invitation v1\x00" + nonce))
	return base64.RawURLEncoding.EncodeToString(h.Sum(nil))
}
func (s *Service) GuardianInviteCapability(info GuardianInviteInfo) GuardianInviteInfo {
	origin, err := url.Parse(s.PublicAppOrigin)
	info.Available = err == nil && origin.Scheme == "https" && origin.Host != "" && origin.User == nil && origin.Path == "" && origin.RawQuery == "" && origin.Fragment == "" && len(s.Secret) >= 32 && info.PolicyKey != "" && len(info.PolicyVersion) == 64
	return info
}
func (s *Service) guardianInviteView(r mobile.Record, actor int64, info GuardianInviteInfo) (out GuardianInviteStatus, err error) {
	if !strings.HasPrefix(r.Key, "guardian-invite:") || (r.OwnerID != actor && !mobile.Has(r.Participants, actor)) {
		return out, mobile.ErrNotFound
	}
	v, err := mobile.Decode[mobile.GuardianInviteBody](r)
	if err != nil {
		return out, err
	}
	state := strings.TrimPrefix(r.State, "guardian_")
	if !v.ExpiresAt.After(s.Now()) {
		state = "expired"
	} else if info.PolicyVersion != "" && v.PolicyVersion != info.PolicyVersion && state != "invite_cancelled" && state != "relation_verified" {
		state = "policy_changed"
	}
	link := ""
	if state == "invite_open" && r.OwnerID == actor && v.Nonce != "" && info.Available {
		link = s.PublicAppOrigin + "/app/guardian-invite#code=" + url.QueryEscape(s.guardianToken(v.Nonce))
	}
	return GuardianInviteStatus{r.ID, r.Version, state, v.PolicyVersion, v.ExpiresAt, v.GuardianRecordID, link, r.OwnerID == actor, mobile.HasString([]string{"guardian_invite_open", "guardian_relation_pending"}, r.State)}, nil
}
func guardianCommand(ctx context.Context, in any) (key, hash string, err error) {
	key, hash, err = mobile.CommandIdentity(in)
	if err == nil && mobile.CommandID(ctx) != "" && mobile.CommandID(ctx) != key {
		err = mobile.Denied("idempotency_conflict")
	}
	return
}
func (s *Service) GuardianInvites(ctx context.Context, actor, after int64, info GuardianInviteInfo) (out GuardianInvitePage, err error) {
	out.Items = []GuardianInviteStatus{}
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		rows, e := tx.List(mobile.AuthChallenges, mobile.Filter{ParticipantID: actor, After: after, Limit: 100, KeyPrefix: "guardian-invite:"})
		if e != nil {
			return e
		}
		for _, r := range rows {
			v, e := s.guardianInviteView(r, actor, info)
			if e != nil {
				return e
			}
			out.Items = append(out.Items, v)
		}
		if len(rows) == 100 {
			out.NextCursor = rows[len(rows)-1].ID
		}
		return nil
	})
	return
}
func guardianOperation(tx mobile.Tx, actor int64, key, hash, command string, r mobile.Record) error {
	op := mobile.Record{OwnerID: actor, Key: fmt.Sprintf("%d:%s", actor, key), State: "committed"}
	return set(tx, mobile.Operations, &op, mobile.Operation{ClientRequestID: key, Command: command, ObjectID: actor, PayloadHash: hash, ResultKind: mobile.AuthChallenges, ResultID: r.ID, ResultVersion: r.Version, State: "committed"})
}
func (s *Service) guardianReplay(tx mobile.Tx, actor int64, key, hash, command string, info GuardianInviteInfo) (*GuardianInviteStatus, error) {
	r, err := tx.ByKey(mobile.Operations, fmt.Sprintf("%d:%s", actor, key))
	if errors.Is(err, mobile.ErrNotFound) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	op, err := mobile.Decode[mobile.Operation](r)
	if err != nil {
		return nil, err
	}
	if op.Command != command || op.PayloadHash != hash {
		return nil, mobile.Denied("idempotency_conflict")
	}
	row, err := tx.Get(mobile.AuthChallenges, op.ResultID)
	if errors.Is(err, mobile.ErrNotFound) {
		return &GuardianInviteStatus{ID: op.ResultID, State: "expired", ExpiresAt: s.Now()}, nil
	}
	if err != nil {
		return nil, err
	}
	result, err := s.guardianInviteView(row, actor, info)
	return &result, err
}
func (s *Service) CreateGuardianInvite(ctx context.Context, actor int64, info GuardianInviteInfo, in GuardianInviteStart) (out GuardianInviteStatus, err error) {
	info = s.GuardianInviteCapability(info)
	if !info.Available || !in.Accepted || in.PolicyVersion != info.PolicyVersion {
		return out, mobile.ErrUnavailable
	}
	key, hash, err := guardianCommand(ctx, in)
	if err != nil {
		return out, err
	}
	nonce, err := randomToken()
	if err != nil {
		return out, err
	}
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		replay, e := s.guardianReplay(tx, actor, key, hash, "guardian.invite.create", info)
		if e != nil {
			return e
		}
		if replay != nil {
			out = *replay
			return nil
		}
		pointerRecord, e := tx.ByKey(mobile.AuthChallenges, fmt.Sprintf("guardian-current:%d", actor))
		if e == nil {
			pointer, e := mobile.Decode[guardianInvitePointer](pointerRecord)
			if e != nil {
				return e
			}
			prior, e := tx.Get(mobile.AuthChallenges, pointer.ID)
			if e == nil {
				v, e := mobile.Decode[mobile.GuardianInviteBody](prior)
				if e != nil {
					return e
				}
				if prior.OwnerID == actor && mobile.HasString([]string{"guardian_invite_open", "guardian_relation_pending"}, prior.State) && v.ExpiresAt.After(s.Now()) {
					if e = guardianOperation(tx, actor, key, hash, "guardian.invite.create", prior); e != nil {
						return e
					}
					out, e = s.guardianInviteView(prior, actor, info)
					return e
				}
			} else if !errors.Is(e, mobile.ErrNotFound) {
				return e
			}
		} else if !errors.Is(e, mobile.ErrNotFound) {
			return e
		} else {
			pointerRecord = mobile.Record{OwnerID: actor, Key: fmt.Sprintf("guardian-current:%d", actor), State: "guardian_pointer"}
		}
		r := mobile.Record{OwnerID: actor, Key: "guardian-invite:" + mobile.Fingerprint(s.guardianToken(nonce)), State: "guardian_invite_open"}
		if e = set(tx, mobile.AuthChallenges, &r, mobile.GuardianInviteBody{Nonce: nonce, PolicyVersion: info.PolicyVersion, ExpiresAt: s.Now().Add(15 * time.Minute)}); e != nil {
			return e
		}
		if e = guardianOperation(tx, actor, key, hash, "guardian.invite.create", r); e != nil {
			return e
		}
		if e = set(tx, mobile.AuthChallenges, &pointerRecord, guardianInvitePointer{r.ID, s.Now().Add(15 * time.Minute)}); e != nil {
			return e
		}
		out, e = s.guardianInviteView(r, actor, info)
		return e
	})
	return
}
func (s *Service) ClaimGuardianInvite(ctx context.Context, actor int64, info GuardianInviteInfo, in GuardianInviteClaim) (out GuardianInviteStatus, err error) {
	info = s.GuardianInviteCapability(info)
	if !info.Available || !in.Accepted || in.PolicyVersion != info.PolicyVersion || len(in.Token) != 43 {
		return out, mobile.ErrForbidden
	}
	key, hash, err := guardianCommand(ctx, in)
	if err != nil {
		return out, err
	}
	eligibility := mobile.New(s.Repo)
	eligibility.Now = s.Now
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		replay, e := s.guardianReplay(tx, actor, key, hash, "guardian.invite.claim", info)
		if e != nil {
			return e
		}
		if replay != nil {
			out = *replay
			return nil
		}
		if e = eligibility.RequireAdult(tx, actor); e != nil {
			return e
		}
		r, e := tx.ByKey(mobile.AuthChallenges, "guardian-invite:"+mobile.Fingerprint(in.Token))
		if e != nil {
			return mobile.ErrNotFound
		}
		v, e := mobile.Decode[mobile.GuardianInviteBody](r)
		if e != nil {
			return e
		}
		if r.OwnerID == actor || r.State != "guardian_invite_open" || !v.ExpiresAt.After(s.Now()) || v.PolicyVersion != info.PolicyVersion {
			return mobile.ErrConflict
		}
		r.State = "guardian_relation_pending"
		r.Participants = []int64{actor}
		v.ClaimedBy = actor
		v.Nonce = ""
		v.ExpiresAt = s.Now().Add(7 * 24 * time.Hour)
		if e = set(tx, mobile.AuthChallenges, &r, v); e != nil {
			return e
		}
		pointerRecord, e := tx.ByKey(mobile.AuthChallenges, fmt.Sprintf("guardian-current:%d", r.OwnerID))
		if e != nil {
			return e
		}
		if e = set(tx, mobile.AuthChallenges, &pointerRecord, guardianInvitePointer{r.ID, v.ExpiresAt}); e != nil {
			return e
		}
		if e = guardianOperation(tx, actor, key, hash, "guardian.invite.claim", r); e != nil {
			return e
		}
		out, e = s.guardianInviteView(r, actor, info)
		return e
	})
	return
}
func (s *Service) CancelGuardianInvite(ctx context.Context, actor, id int64, in mobile.StateInput, info GuardianInviteInfo) (out GuardianInviteStatus, err error) {
	key, hash, err := guardianCommand(ctx, struct {
		mobile.StateInput
		ID int64 `json:"id"`
	}{in, id})
	if err != nil {
		return out, err
	}
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		replay, e := s.guardianReplay(tx, actor, key, hash, "guardian.invite.cancel", info)
		if e != nil {
			return e
		}
		if replay != nil {
			out = *replay
			return nil
		}
		r, e := tx.Get(mobile.AuthChallenges, id)
		if e != nil {
			return e
		}
		if !strings.HasPrefix(r.Key, "guardian-invite:") || r.OwnerID != actor && !mobile.Has(r.Participants, actor) {
			return mobile.ErrNotFound
		}
		if r.Version != in.Version || !mobile.HasString([]string{"guardian_invite_open", "guardian_relation_pending"}, r.State) {
			return mobile.ErrConflict
		}
		v, e := mobile.Decode[mobile.GuardianInviteBody](r)
		if e != nil {
			return e
		}
		r.State = "guardian_invite_cancelled"
		v.Nonce = ""
		v.ExpiresAt = s.Now().Add(24 * time.Hour)
		if e = set(tx, mobile.AuthChallenges, &r, v); e != nil {
			return e
		}
		if e = guardianOperation(tx, actor, key, hash, "guardian.invite.cancel", r); e != nil {
			return e
		}
		out, e = s.guardianInviteView(r, actor, info)
		return e
	})
	return
}
func (s *Service) GuardianInvite(ctx context.Context, actor, id int64, info GuardianInviteInfo) (out GuardianInviteStatus, err error) {
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		r, e := tx.Get(mobile.AuthChallenges, id)
		if e != nil {
			return e
		}
		out, e = s.guardianInviteView(r, actor, info)
		return e
	})
	return
}
