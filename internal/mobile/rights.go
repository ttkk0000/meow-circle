package mobile

import (
	"context"
	"strings"
	"time"
)

type SafetySession struct {
	ExpiresAt       time.Time `json:"expires_at"`
	Contact         string    `json:"contact,omitempty"`
	ContactVerified bool      `json:"contact_verified"`
}
type SafetyAccess struct {
	Token     string    `json:"token"`
	ExpiresAt time.Time `json:"expires_at"`
}

func (s *Service) NewSafetySession(ctx context.Context) (out SafetyAccess, err error) {
	token, e := NewCapability()
	if e != nil {
		return out, e
	}
	out = SafetyAccess{token, s.Now().Add(30 * 24 * time.Hour)}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r := Record{Key: Fingerprint(token), State: "safety_session"}
		if e := SetBody(&r, SafetySession{ExpiresAt: out.ExpiresAt}); e != nil {
			return e
		}
		return tx.Save(AuthChallenges, &r)
	})
	return
}
func safetySession(tx Tx, token string, now time.Time) (Record, SafetySession, error) {
	if len(token) < 40 || len(token) > 100 {
		return Record{}, SafetySession{}, ErrForbidden
	}
	r, e := tx.ByKey(AuthChallenges, Fingerprint(token))
	if e != nil {
		return r, SafetySession{}, ErrForbidden
	}
	v, e := Decode[SafetySession](r)
	if e != nil {
		return r, v, e
	}
	if r.State != "safety_session" || !v.ExpiresAt.After(now) {
		return r, v, ErrForbidden
	}
	return r, v, nil
}

type RightsInput struct {
	Confirmed bool `json:"confirmed"`
	CommandMeta
	Type                string  `json:"type"` // safety, privacy, rights_notice, counterstatement, supplement, data_assistance
	TargetKind          Kind    `json:"target_kind"`
	TargetID            int64   `json:"target_id"`
	RelatedCaseID       int64   `json:"related_case_id"`
	Version             int64   `json:"version"`
	Reason              string  `json:"reason"`
	RightType           string  `json:"right_type"`
	LegalName           string  `json:"legal_name"`
	Statement           string  `json:"statement"`
	EvidenceDescription string  `json:"evidence_description"`
	ReauthToken         string  `json:"reauth_token"`
	MediaIDs            []int64 `json:"media_ids"`
}
type RightsMessage struct {
	Type      string    `json:"type"`
	Text      string    `json:"text"`
	Evidence  string    `json:"evidence"`
	At        time.Time `json:"at"`
	ActorRole string    `json:"actor_role"`
}
type RightsCase struct {
	EvidenceErasedAt *time.Time      `json:"evidence_erased_at,omitempty"`
	TransferReceipt  string          `json:"transfer_receipt,omitempty"`
	EvidenceUntil    time.Time       `json:"evidence_until"`
	Type             string          `json:"type"`
	TargetKind       Kind            `json:"target_kind"`
	TargetID         int64           `json:"target_id"`
	RelatedCaseID    int64           `json:"related_case_id"`
	SessionHash      string          `json:"session_hash"`
	Contact          string          `json:"contact"`
	LegalName        string          `json:"legal_name"`
	RightType        string          `json:"right_type"`
	Statement        string          `json:"statement"`
	Messages         []RightsMessage `json:"messages"`
	ReviewDue        time.Time       `json:"review_due"`
	TransferState    string          `json:"transfer_state"`
	Operator         string          `json:"operator,omitempty"`
}
type RightsReceipt struct {
	EvidenceState string    `json:"evidence_state"`
	ID            int64     `json:"id"`
	Version       int64     `json:"version"`
	State         string    `json:"state"`
	Type          string    `json:"type"`
	ReviewDue     time.Time `json:"review_due"`
}

func RightsScope(actor int64, in RightsInput) (PurposeScope, error) {
	return ScopeFor("rights_notice.submit", string(RightsCases), in.RelatedCaseID, in.Version, "submitter", struct {
		Actor     int64   `json:"actor"`
		Type      string  `json:"type"`
		Kind      Kind    `json:"kind"`
		ID        int64   `json:"id"`
		Reason    string  `json:"reason"`
		Statement string  `json:"statement"`
		Evidence  string  `json:"evidence"`
		LegalName string  `json:"legal_name"`
		RightType string  `json:"right_type"`
		MediaIDs  []int64 `json:"media_ids"`
	}{actor, in.Type, in.TargetKind, in.TargetID, in.Reason, in.Statement, in.EvidenceDescription, in.LegalName, in.RightType, in.MediaIDs})
}
func (s *Service) SubmitRightsCase(ctx context.Context, actor int64, token string, in RightsInput) (out RightsReceipt, err error) {
	if !HasString([]string{"safety", "privacy", "rights_notice", "counterstatement", "supplement", "data_assistance"}, in.Type) || textValid(in.Reason, 3000, true) != nil || textValid(in.EvidenceDescription, 4000, false) != nil || len(in.MediaIDs) > 9 {
		return out, Invalid("invalid safety or rights request")
	}
	if in.TargetID < 0 || in.TargetID > 0 && (!queryKind(in.TargetKind) || in.TargetKind == Reviews) {
		return out, Invalid("invalid target reference")
	}
	key, hash, e := commandKey(in)
	if e != nil {
		return out, e
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		_, session, e := safetySession(tx, token, s.Now())
		if e != nil {
			return e
		}
		sessionHash := Fingerprint(token)
		if actor > 0 {
			if e = s.checkActor(ctx, tx, actor); e != nil {
				return e
			}
		}
		r, e := tx.ByKey(RightsCases, "ticket:"+sessionHash+":"+key)
		if e == nil {
			v, e := Decode[struct {
				RequestHash string     `json:"request_hash"`
				Case        RightsCase `json:"case"`
			}](r)
			if e != nil {
				return e
			}
			if v.RequestHash != hash {
				return Denied("idempotency_conflict")
			}
			out = rightsReceipt(r, v.Case, s.Now())
			return nil
		}
		if e != ErrNotFound {
			return e
		}
		for _, id := range in.MediaIDs {
			m, e := tx.Get(Media, id)
			if e != nil {
				return e
			}
			if m.State != "ready" || (actor <= 0 || m.OwnerID != actor) && !strings.HasPrefix(m.Key, "safety:"+sessionHash+":") {
				return ErrForbidden
			}
		}
		formal := in.Type == "rights_notice" || in.Type == "counterstatement"
		if formal {
			if !in.Confirmed || !session.ContactVerified || textValid(in.LegalName, 120, true) != nil || textValid(in.Statement, 2000, true) != nil || textValid(in.RightType, 100, true) != nil || in.EvidenceDescription == "" {
				return Denied("formal_notice_identity_required")
			}
			// Guest identity is the verified contact session. Logged-in final submissions additionally use scoped reauth.
			if actor > 0 {
				scope, e := RightsScope(actor, in)
				if e != nil {
					return e
				}
				if e = ConsumePurpose(tx, actor, SessionKey(ctx), in.ReauthToken, in.ClientRequestID, scope, s.Now()); e != nil {
					return e
				}
			}
		}
		if in.Type == "counterstatement" || in.Type == "supplement" {
			prior, e := tx.Get(RightsCases, in.RelatedCaseID)
			if e != nil {
				return e
			}
			v, e := Decode[struct {
				Case RightsCase `json:"case"`
			}](prior)
			if e != nil {
				return e
			}
			if prior.Version != in.Version {
				return ErrConflict
			}
			if in.Type == "supplement" && v.Case.SessionHash != sessionHash {
				return ErrForbidden
			}
			if in.Type == "counterstatement" {
				target, e := tx.Get(v.Case.TargetKind, v.Case.TargetID)
				if e != nil {
					return e
				}
				if actor <= 0 || target.OwnerID != actor {
					return ErrForbidden
				}
			}
		}
		role := "reporter"
		if formal {
			role = "rights_submitter"
		}
		v := RightsCase{EvidenceUntil: s.Now().Add(30 * 24 * time.Hour), Type: in.Type, TargetKind: in.TargetKind, TargetID: in.TargetID, RelatedCaseID: in.RelatedCaseID, SessionHash: sessionHash, Contact: session.Contact, LegalName: in.LegalName, RightType: in.RightType, Statement: in.Statement, Messages: []RightsMessage{{in.Type, in.Reason, in.EvidenceDescription, s.Now(), role}}, ReviewDue: s.Now().Add(24 * time.Hour), TransferState: "pending_review"}
		r = Record{OwnerID: actor, Key: "ticket:" + sessionHash + ":" + key, RefID: in.RelatedCaseID, MediaIDs: in.MediaIDs, State: "submitted"}
		if e = save(tx, RightsCases, &r, struct {
			RequestHash string     `json:"request_hash"`
			Case        RightsCase `json:"case"`
		}{hash, v}, actor, "rights_case.submitted"); e != nil {
			return e
		}
		out = rightsReceipt(r, v, s.Now())
		return nil
	})
	return
}
func (s *Service) RightsStatus(ctx context.Context, token string, id int64) (out RightsReceipt, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		if _, _, e := safetySession(tx, token, s.Now()); e != nil {
			return e
		}
		r, e := tx.Get(RightsCases, id)
		if e != nil {
			return e
		}
		v, e := Decode[struct {
			Case RightsCase `json:"case"`
		}](r)
		if e != nil {
			return e
		}
		if v.Case.SessionHash != Fingerprint(token) {
			return ErrForbidden
		}
		out = rightsReceipt(r, v.Case, s.Now())
		return nil
	})
	return
}
func (s *Service) RightsByKey(ctx context.Context, token, key string) (out RightsReceipt, err error) {
	if len(token) < 40 || len(token) > 100 || len(key) < 8 || len(key) > 100 {
		return out, ErrForbidden
	}
	var id int64
	err = s.Repo.View(ctx, func(tx Tx) error {
		r, e := tx.ByKey(RightsCases, "ticket:"+Fingerprint(token)+":"+key)
		id = r.ID
		return e
	})
	if err != nil {
		return
	}
	return s.RightsStatus(ctx, token, id)
}

type RightsDecision struct {
	TransferReceipt string     `json:"transfer_receipt"`
	EvidenceUntil   *time.Time `json:"evidence_until,omitempty"`
	RetentionBasis  string     `json:"retention_basis"`
	Responsible     string     `json:"responsible"`
	Version         int64      `json:"version"`
	State           string     `json:"state"`
	Summary         string     `json:"summary"`
	ReviewDue       time.Time  `json:"review_due"`
	TransferState   string     `json:"transfer_state"`
}

func (s *Service) DecideRightsCase(ctx context.Context, operator string, id int64, in RightsDecision) (out RightsReceipt, err error) {
	if in.TransferState == "transferred" && textValid(in.TransferReceipt, 160, true) != nil {
		return out, Invalid("actual transfer receipt required")
	}
	if in.EvidenceUntil != nil && (!in.EvidenceUntil.After(s.Now()) || in.EvidenceUntil.After(s.Now().Add(90*24*time.Hour)) || textValid(in.RetentionBasis, 1000, true) != nil || textValid(in.Responsible, 120, true) != nil) {
		return out, Invalid("specific evidence retention basis and review period required")
	}
	if operator == "" || !HasString([]string{"in_review", "needs_info", "accepted", "rejected", "closed"}, in.State) || textValid(in.Summary, 2000, true) != nil || !in.ReviewDue.After(s.Now()) || !HasString([]string{"pending_review", "not_required", "pending_transfer", "transferred"}, in.TransferState) {
		return out, Invalid("invalid rights decision")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(RightsCases, id)
		if e != nil {
			return e
		}
		if e = expected(r, in.Version); e != nil {
			return e
		}
		v, e := Decode[struct {
			RequestHash string     `json:"request_hash"`
			Case        RightsCase `json:"case"`
		}](r)
		if e != nil {
			return e
		}
		if v.Case.EvidenceErasedAt != nil {
			return Denied("evidence_already_erased")
		}
		if in.EvidenceUntil != nil {
			if in.EvidenceUntil.Before(v.Case.EvidenceUntil) {
				return Denied("evidence_retention_cannot_be_shortened_or_restored")
			}
			for _, mediaID := range r.MediaIDs {
				media, e := tx.Get(Media, mediaID)
				if e != nil {
					return e
				}
				if media.State != "ready" {
					return Denied("media_retention_target_unavailable")
				}
			}
			v.Case.EvidenceUntil = *in.EvidenceUntil
			v.Case.Messages = append(v.Case.Messages, RightsMessage{"retention", in.RetentionBasis, in.Responsible, s.Now(), "operator"})
		}
		v.Case.Operator, v.Case.ReviewDue, v.Case.TransferState = operator, in.ReviewDue, in.TransferState
		if in.TransferState == "transferred" {
			v.Case.TransferReceipt = in.TransferReceipt
		}
		v.Case.Messages = append(v.Case.Messages, RightsMessage{"decision", in.Summary, "", s.Now(), "operator"})
		r.State = in.State
		if e = save(tx, RightsCases, &r, v, r.OwnerID, "rights_case.decided"); e != nil {
			return e
		}
		out = rightsReceipt(r, v.Case, s.Now())
		return audit(tx, operator, "rights_case.decided", RightsCases, id)
	})
	return
}
