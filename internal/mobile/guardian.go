package mobile

import (
	"context"
	"time"
)

type GuardianEvidenceInput struct {
	PolicyVersion string    `json:"policy_version"`
	GuardianID    int64     `json:"guardian_id"`
	ChildID       int64     `json:"child_id"`
	EvidenceRef   string    `json:"evidence_ref"`
	ValidUntil    time.Time `json:"valid_until"`
}

func (s *Service) RecordGuardianEvidence(ctx context.Context, operator string, in GuardianEvidenceInput) (out Record, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	if in.GuardianID == in.ChildID || !in.ValidUntil.After(s.Now()) || in.ValidUntil.After(s.Now().Add(366*24*time.Hour)) || textValid(in.EvidenceRef, 120, true) != nil || textValid(in.PolicyVersion, 80, true) != nil {
		return out, Invalid("invalid guardian evidence")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		if e := s.adult(tx, in.GuardianID); e != nil {
			return e
		}
		if e := s.active(tx, in.ChildID); e != nil {
			return e
		}
		r := Record{OwnerID: in.GuardianID, RefID: in.ChildID, Participants: []int64{in.ChildID}, State: "verified_pending_consent"}
		if e := save(tx, Guardians, &r, GuardianConsent{ChildID: in.ChildID, Scopes: []string{}, ValidUntil: in.ValidUntil, EvidenceRef: in.EvidenceRef, PolicyVersion: in.PolicyVersion}, in.GuardianID, "guardian.relationship_verified"); e != nil {
			return e
		}
		out = r
		return audit(tx, operator, "guardian.relationship_verified", Guardians, r.ID)
	})
	return
}

type GuardianDecision struct {
	CommandMeta
	Version     int64  `json:"version"`
	Consent     bool   `json:"consent"`
	ReauthToken string `json:"reauth_token"`
}

func GuardianScope(id, version int64, policyVersion string) (PurposeScope, error) {
	return ScopeFor("guardian.consent", string(Guardians), id, version, "guardian", struct {
		PolicyVersion string   `json:"policy_version"`
		Scopes        []string `json:"scopes"`
	}{policyVersion, []string{"interaction", "personal_data"}})
}
func (s *Service) Guardian(ctx context.Context, actor, id int64, in GuardianDecision) (out Record, err error) {
	err = s.command(ctx, actor, "guardian.decision", id, in, Guardians, &out, func(tx Tx) error {
		r, e := writable(tx, Guardians, id, actor, in.Version)
		if e != nil {
			return e
		}
		g, e := Decode[GuardianConsent](r)
		if e != nil {
			return e
		}
		if in.Consent {
			if e = s.adult(tx, actor); e != nil {
				return e
			}
			if !g.ValidUntil.After(s.Now()) || r.State != "verified_pending_consent" {
				return ErrConflict
			}
			scope, e := GuardianScope(id, in.Version, g.PolicyVersion)
			if e != nil {
				return e
			}
			if e = ConsumePurpose(tx, actor, SessionKey(ctx), in.ReauthToken, in.ClientRequestID, scope, s.Now()); e != nil {
				return e
			}
			g.Scopes, r.State = []string{"interaction", "personal_data"}, "active"
		} else {
			g.Scopes, r.State = []string{}, "withdrawn"
		}
		if e = save(tx, Guardians, &r, g, actor, "guardian.decision"); e != nil {
			return e
		}
		out, e = project(Guardians, r, actor)
		if e != nil {
			return e
		}
		return nil
	})
	return
}
func (s *Service) Guardians(ctx context.Context, actor int64) (out []Record, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		var e error
		out, e = allRecords(tx, Guardians, Filter{ParticipantID: actor})
		if e != nil {
			return e
		}
		for i := range out {
			out[i], e = project(Guardians, out[i], actor)
			if e != nil {
				return e
			}
		}
		return e
	})
	return
}
