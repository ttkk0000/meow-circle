package mobile

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"time"
)

type RuleError struct{ Code, Message string }

func (e RuleError) Error() string { return e.Message }
func Denied(code string) error {
	return RuleError{code, "action eligibility requirements are not satisfied"}
}
func Fingerprint(value string) string {
	h := sha256.Sum256([]byte(value))
	return hex.EncodeToString(h[:])
}

type Qualification struct {
	PhoneHash       string     `json:"phone_hash,omitempty"`
	PhoneVerifiedAt *time.Time `json:"phone_verified_at,omitempty"`
	AgeBand         string     `json:"age_band,omitempty"`
	AgeValidUntil   time.Time  `json:"age_valid_until"`
	EvidenceRef     string     `json:"evidence_ref,omitempty"`
	VerifiedBy      string     `json:"verified_by,omitempty"`
}
type GuardianConsent struct {
	PolicyVersion string    `json:"policy_version"`
	ChildID       int64     `json:"child_id"`
	Scopes        []string  `json:"scopes"`
	ValidUntil    time.Time `json:"valid_until"`
	EvidenceRef   string    `json:"evidence_ref"`
}
type Eligibility struct {
	Interaction     bool     `json:"interaction"`
	Adult           bool     `json:"adult"`
	PhoneVerified   bool     `json:"phone_verified"`
	AgeBand         string   `json:"age_band"`
	Reasons         []string `json:"reasons"`
	ContractVersion int      `json:"contract_version"`
}

func (s *Service) eligibility(tx Tx, actor int64) (out Eligibility, err error) {
	out.ContractVersion, out.Reasons = 1, []string{}
	if err = s.active(tx, actor); err != nil {
		return
	}
	u, err := tx.User(actor)
	if err != nil {
		return out, err
	}
	r, err := tx.ByKey(Qualifications, fmt.Sprint(actor))
	q := Qualification{}
	if err == nil {
		q, err = Decode[Qualification](r)
	}
	if err != nil && !errors.Is(err, ErrNotFound) {
		return out, err
	}
	out.PhoneVerified = u.Phone != "" && q.PhoneVerifiedAt != nil && q.PhoneHash == Fingerprint(u.Phone)
	if !out.PhoneVerified {
		out.Reasons = append(out.Reasons, "phone_verification_required")
	}
	out.AgeBand = q.AgeBand
	if !q.AgeValidUntil.After(s.Now()) {
		out.AgeBand = "unknown"
	}
	allowed := out.AgeBand == "adult" || out.AgeBand == "14_17"
	if out.AgeBand == "under14" {
		rows, e := allRecords(tx, Guardians, Filter{RefID: actor, State: "active"})
		if e != nil {
			return out, e
		}
		for _, row := range rows {
			g, e := Decode[GuardianConsent](row)
			if e != nil {
				return out, e
			}
			if g.ValidUntil.After(s.Now()) && HasString(g.Scopes, "interaction") && HasString(g.Scopes, "personal_data") {
				adult, e := tx.ByKey(Qualifications, fmt.Sprint(row.OwnerID))
				if errors.Is(e, ErrNotFound) {
					continue
				}
				if e != nil {
					return out, e
				}
				v, e := Decode[Qualification](adult)
				if e != nil {
					return out, e
				}
				if e = s.active(tx, row.OwnerID); errors.Is(e, ErrUnauthorized) {
					continue
				}
				if e != nil {
					return out, e
				}
				allowed = v.AgeBand == "adult" && v.AgeValidUntil.After(s.Now())
				if allowed {
					break
				}
			}
		}
		if !allowed {
			out.Reasons = append(out.Reasons, "guardian_consent_required")
		}
	} else if !allowed {
		out.Reasons = append(out.Reasons, "age_verification_required")
	}
	out.Interaction = out.PhoneVerified && allowed
	out.Adult = out.PhoneVerified && out.AgeBand == "adult"
	return out, nil
}
func (s *Service) Eligibility(ctx context.Context, actor int64) (out Eligibility, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error { var e error; out, e = s.eligibility(tx, actor); return e })
	return
}
func (s *Service) interaction(tx Tx, actor int64) error {
	v, e := s.eligibility(tx, actor)
	if e != nil {
		return e
	}
	if !v.Interaction {
		return Denied(v.Reasons[0])
	}
	return nil
}
func RequireInteraction(tx Tx, actor int64, now time.Time) error {
	return (&Service{Now: func() time.Time { return now }}).interaction(tx, actor)
}
func (s *Service) publicProcessingAllowed(tx Tx, actor int64) (bool, error) {
	if e := s.active(tx, actor); e == ErrUnauthorized || e == ErrNotFound {
		return false, nil
	} else if e != nil {
		return false, e
	}
	r, e := tx.ByKey(Qualifications, fmt.Sprint(actor))
	if e == ErrNotFound {
		return true, nil
	}
	if e != nil {
		return false, e
	}
	q, e := Decode[Qualification](r)
	if e != nil {
		return false, e
	}
	if q.AgeBand != "under14" {
		return true, nil
	}
	eligibility, e := s.eligibility(tx, actor)
	return eligibility.Interaction, e
}
func (s *Service) adult(tx Tx, actor int64) error {
	if e := s.interaction(tx, actor); e != nil {
		return e
	}
	v, e := s.eligibility(tx, actor)
	if e != nil {
		return e
	}
	if !v.Adult {
		return Denied("adult_required")
	}
	return nil
}

// 陌生人私信只向双方均有有效成年结果的账号开放消息请求。
// 已双向确认的好友可继续受保护的日常交流；不向调用方透露对方年龄。
func (s *Service) directContact(tx Tx, actor, peer int64) error {
	b, err := blocked(tx, actor, peer)
	if err != nil {
		return err
	}
	if b {
		return ErrForbidden
	}
	connected, err := friends(tx, actor, peer)
	if err != nil {
		return err
	}
	if connected {
		return nil
	}
	for _, id := range []int64{actor, peer} {
		r, err := tx.ByKey(Qualifications, fmt.Sprint(id))
		if errors.Is(err, ErrNotFound) {
			return Denied("contact_restricted")
		}
		if err != nil {
			return err
		}
		proof, err := Decode[Qualification](r)
		if err != nil {
			return err
		}
		if proof.AgeBand != "adult" || !proof.AgeValidUntil.After(s.Now()) {
			return Denied("contact_restricted")
		}
	}
	return nil
}

// The identity module calls this only after consuming a real OTP in the same transaction.
func RecordPhoneProof(tx Tx, actor int64, phone string, now time.Time) error {
	r, e := tx.ByKey(Qualifications, fmt.Sprint(actor))
	q := Qualification{}
	if errors.Is(e, ErrNotFound) {
		r = Record{OwnerID: actor, Key: fmt.Sprint(actor), State: "active"}
	} else if e != nil {
		return e
	} else {
		q, e = Decode[Qualification](r)
		if e != nil {
			return e
		}
	}
	q.PhoneHash, q.PhoneVerifiedAt = Fingerprint(phone), &now
	if e = SetBody(&r, q); e != nil {
		return e
	}
	return tx.Save(Qualifications, &r)
}

type AgeEvidenceInput struct {
	ActorID     int64     `json:"actor_id"`
	AgeBand     string    `json:"age_band"`
	ValidUntil  time.Time `json:"valid_until"`
	EvidenceRef string    `json:"evidence_ref"`
}

// Trusted verifier boundary. End users cannot assert an adult flag.
func (s *Service) RecordAgeEvidence(ctx context.Context, operator string, in AgeEvidenceInput) error {
	if operator == "" {
		return ErrForbidden
	}
	if !HasString([]string{"under14", "14_17", "adult"}, in.AgeBand) || !in.ValidUntil.After(s.Now()) || in.ValidUntil.After(s.Now().Add(366*24*time.Hour)) || textValid(in.EvidenceRef, 120, true) != nil {
		return Invalid("invalid age evidence")
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		if e := s.active(tx, in.ActorID); e != nil {
			return e
		}
		return RecordTrustedAgeProof(tx, in.ActorID, in.AgeBand, in.ValidUntil, in.EvidenceRef, operator, s.Now())
	})
}

// 仅供具名运营和服务端核验通道调用，不开放用户自报资格入口。
func RecordTrustedAgeProof(tx Tx, actor int64, band string, until time.Time, evidence, verifier string, now time.Time) error {
	if actor <= 0 || verifier == "" || !HasString([]string{"under14", "14_17", "adult"}, band) || !until.After(now) || until.After(now.Add(366*24*time.Hour)) || textValid(evidence, 120, true) != nil {
		return Invalid("invalid trusted age proof")
	}
	r, e := tx.ByKey(Qualifications, fmt.Sprint(actor))
	q := Qualification{}
	if errors.Is(e, ErrNotFound) {
		r = Record{OwnerID: actor, Key: fmt.Sprint(actor), State: "active"}
	} else if e != nil {
		return e
	} else {
		q, e = Decode[Qualification](r)
		if e != nil {
			return e
		}
	}
	q.AgeBand, q.AgeValidUntil, q.EvidenceRef, q.VerifiedBy = band, until, evidence, verifier
	if e = save(tx, Qualifications, &r, q, actor, "qualification.age_recorded"); e != nil {
		return e
	}
	return audit(tx, verifier, "qualification.age_recorded", Qualifications, r.ID)
}

func audit(tx Tx, operator, action string, kind Kind, id int64) error {
	r := Record{RefID: id, State: "recorded"}
	if e := SetBody(&r, struct {
		Operator string `json:"operator"`
		Action   string `json:"action"`
		Kind     Kind   `json:"kind"`
	}{operator, action, kind}); e != nil {
		return e
	}
	return tx.Save(AuditEntries, &r)
}
