package identity

import (
	"context"
	"errors"
	"fmt"
	"kitty-circle/internal/mobile"
	"net/url"
	"strings"
	"time"
)

// 业务服务只接受结果、有效期与必要来源；原始证件和生物信息由已配置通道处理。
type AgeProvider interface {
	Info() AgeProviderInfo
	Create(context.Context, AgeProviderRequest) (AgeProviderResult, error)
	Query(context.Context, AgeProviderRequest) (AgeProviderResult, error)
}
type AgeProviderInfo struct {
	Available    bool   `json:"available"`
	Name         string `json:"name"`
	PrivacyURL   string `json:"privacy_url"`
	LaunchOrigin string `json:"-"`
}
type AgeProviderRequest struct {
	Purpose   string    `json:"purpose"`
	Reference string    `json:"reference"`
	Nonce     string    `json:"nonce"`
	ExpiresAt time.Time `json:"expires_at"`
}
type AgeProviderResult struct {
	Reference   string    `json:"reference"`
	Nonce       string    `json:"nonce"`
	State       string    `json:"state"`
	LaunchURL   string    `json:"launch_url"`
	ValidUntil  time.Time `json:"valid_until"`
	EvidenceRef string    `json:"evidence_ref"`
}
type AgeAssuranceInput struct {
	mobile.CommandMeta
	TaskKey   string `json:"task_key"`
	TaskLabel string `json:"task_label"`
	Accepted  bool   `json:"accepted"`
}
type AgeAssuranceStatus struct {
	ID         int64      `json:"id"`
	State      string     `json:"state"`
	TaskKey    string     `json:"task_key"`
	TaskLabel  string     `json:"task_label"`
	Provider   string     `json:"provider"`
	LaunchURL  string     `json:"launch_url"`
	ExpiresAt  time.Time  `json:"expires_at"`
	ValidUntil *time.Time `json:"valid_until"`
}
type ageAssurance struct {
	Request           AgeProviderRequest `json:"request"`
	Input             AgeAssuranceInput  `json:"input"`
	PayloadHash       string             `json:"payload_hash"`
	Provider          string             `json:"provider"`
	AgeFingerprint    string             `json:"age_fingerprint"`
	ResultFingerprint string             `json:"result_fingerprint"`
	LaunchURL         string             `json:"launch_url"`
	ValidUntil        *time.Time         `json:"valid_until"`
	ExpiresAt         time.Time          `json:"expires_at"`
	QueriedAt         time.Time          `json:"queried_at"`
}
type agePointer struct {
	ID        int64     `json:"id"`
	ExpiresAt time.Time `json:"expires_at"`
}

func (s *Service) AgeCapability() AgeProviderInfo {
	if s.Age == nil {
		return AgeProviderInfo{}
	}
	return s.Age.Info()
}
func ageActive(state string) bool {
	return mobile.HasString([]string{"age_preparing", "age_pending", "age_unknown"}, state)
}
func (s *Service) ageRecord(tx mobile.Tx, actor, id int64) (mobile.Record, ageAssurance, error) {
	r, err := tx.Get(mobile.AuthChallenges, id)
	if err != nil {
		return r, ageAssurance{}, err
	}
	if r.OwnerID != actor || !strings.HasPrefix(r.Key, fmt.Sprintf("age:%d:", actor)) {
		return r, ageAssurance{}, mobile.ErrNotFound
	}
	v, err := mobile.Decode[ageAssurance](r)
	return r, v, err
}
func ageView(r mobile.Record, v ageAssurance, now time.Time) AgeAssuranceStatus {
	state := strings.TrimPrefix(r.State, "age_")
	if ageActive(r.State) && !v.Request.ExpiresAt.After(now) {
		state = "expired"
	}
	if state == "passed" && (v.ValidUntil == nil || !v.ValidUntil.After(now)) {
		state = "expired"
	}
	launch := ""
	if state == "pending" {
		launch = v.LaunchURL
	}
	return AgeAssuranceStatus{r.ID, state, v.Input.TaskKey, v.Input.TaskLabel, v.Provider, launch, v.Request.ExpiresAt, v.ValidUntil}
}
func (s *Service) ageStatus(tx mobile.Tx, r mobile.Record, v ageAssurance) (AgeAssuranceStatus, error) {
	if r.State == "age_passed" {
		current, err := ageFingerprint(tx, r.OwnerID)
		if err != nil {
			return AgeAssuranceStatus{}, err
		}
		if v.ResultFingerprint == "" || current != v.ResultFingerprint {
			r.State = "age_superseded"
		}
	}
	return ageView(r, v, s.Now()), nil
}
func ageFingerprint(tx mobile.Tx, actor int64) (string, error) {
	r, err := tx.ByKey(mobile.Qualifications, fmt.Sprint(actor))
	if errors.Is(err, mobile.ErrNotFound) {
		return "", nil
	}
	if err != nil {
		return "", err
	}
	q, err := mobile.Decode[mobile.Qualification](r)
	if err != nil {
		return "", err
	}
	return mobile.Fingerprint(fmt.Sprintf("%s:%s:%s:%s", q.AgeBand, q.AgeValidUntil.UTC().Format(time.RFC3339Nano), q.EvidenceRef, q.VerifiedBy)), nil
}
func recordAgeOperation(tx mobile.Tx, actor int64, key, hash string, r mobile.Record) error {
	operation := mobile.Record{OwnerID: actor, Key: fmt.Sprintf("%d:%s", actor, key), State: "committed"}
	return set(tx, mobile.Operations, &operation, mobile.Operation{ClientRequestID: key, Command: "age.start", ObjectID: actor, PayloadHash: hash, ResultKind: mobile.AuthChallenges, ResultID: r.ID, ResultVersion: r.Version, State: "committed"})
}
func (s *Service) CurrentAgeAssurance(ctx context.Context, actor int64) (out *AgeAssuranceStatus, err error) {
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if err := active(tx, actor); err != nil {
			return err
		}
		r, err := tx.ByKey(mobile.AuthChallenges, fmt.Sprintf("age-current:%d", actor))
		if errors.Is(err, mobile.ErrNotFound) {
			return nil
		}
		if err != nil {
			return err
		}
		pointer, err := mobile.Decode[agePointer](r)
		if err != nil {
			return err
		}
		record, v, err := s.ageRecord(tx, actor, pointer.ID)
		if errors.Is(err, mobile.ErrNotFound) {
			return nil
		}
		if err != nil {
			return err
		}
		value, err := s.ageStatus(tx, record, v)
		if err != nil {
			return err
		}
		out = &value
		return nil
	})
	return
}
func (s *Service) BeginAgeAssurance(ctx context.Context, actor int64, in AgeAssuranceInput) (out AgeAssuranceStatus, err error) {
	if actor <= 0 {
		return out, mobile.ErrUnauthorized
	}
	if !in.Accepted || in.TaskKey == "" || len(in.TaskKey) > 120 || strings.Contains(in.TaskKey, "://") || len(in.TaskLabel) > 240 || in.TaskLabel == "" {
		return out, mobile.Invalid("confirm a bounded internal task before age assurance")
	}
	if s.Age == nil || !s.Age.Info().Available {
		return out, mobile.ErrUnavailable
	}
	key, hash, err := mobile.CommandIdentity(in)
	if err != nil {
		return out, err
	}
	if header := mobile.CommandID(ctx); header != "" && header != key {
		return out, mobile.Denied("idempotency_conflict")
	}
	reference, err := randomToken()
	if err != nil {
		return out, err
	}
	nonce, err := randomToken()
	if err != nil {
		return out, err
	}
	var record mobile.Record
	var value ageAssurance
	created := false
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if err := active(tx, actor); err != nil {
			return err
		}
		operation, priorErr := tx.ByKey(mobile.Operations, fmt.Sprintf("%d:%s", actor, key))
		if priorErr == nil {
			prior, err := mobile.Decode[mobile.Operation](operation)
			if err != nil {
				return err
			}
			if prior.Command != "age.start" || prior.PayloadHash != hash {
				return mobile.Denied("idempotency_conflict")
			}
			r, v, err := s.ageRecord(tx, actor, prior.ResultID)
			if errors.Is(err, mobile.ErrNotFound) {
				out = AgeAssuranceStatus{ID: prior.ResultID, State: "expired", TaskKey: in.TaskKey, TaskLabel: in.TaskLabel, ExpiresAt: s.Now()}
				return nil
			}
			if err != nil {
				return err
			}
			out, err = s.ageStatus(tx, r, v)
			return err
		}
		if !errors.Is(priorErr, mobile.ErrNotFound) {
			return priorErr
		}
		existing, err := tx.ByKey(mobile.AuthChallenges, fmt.Sprintf("age:%d:%s", actor, key))
		if err == nil {
			value, err = mobile.Decode[ageAssurance](existing)
			if err != nil {
				return err
			}
			if value.PayloadHash != hash {
				return mobile.Denied("idempotency_conflict")
			}
			record = existing
			out, err = s.ageStatus(tx, record, value)
			if err != nil {
				return err
			}
			return recordAgeOperation(tx, actor, key, hash, record)
		}
		if !errors.Is(err, mobile.ErrNotFound) {
			return err
		}
		pointerRecord, err := tx.ByKey(mobile.AuthChallenges, fmt.Sprintf("age-current:%d", actor))
		if err == nil {
			pointer, err := mobile.Decode[agePointer](pointerRecord)
			if err != nil {
				return err
			}
			prior, v, err := s.ageRecord(tx, actor, pointer.ID)
			if err == nil && ageActive(prior.State) && v.Request.ExpiresAt.After(s.Now()) {
				record, value = prior, v
				out = ageView(record, value, s.Now())
				return recordAgeOperation(tx, actor, key, hash, record)
			}
			if err != nil && !errors.Is(err, mobile.ErrNotFound) {
				return err
			}
		} else if !errors.Is(err, mobile.ErrNotFound) {
			return err
		} else {
			pointerRecord = mobile.Record{OwnerID: actor, Key: fmt.Sprintf("age-current:%d", actor), State: "age_pointer"}
		}
		fingerprint, err := ageFingerprint(tx, actor)
		if err != nil {
			return err
		}
		until := s.Now().Add(15 * time.Minute)
		value = ageAssurance{Request: AgeProviderRequest{Purpose: "adult_business", Reference: reference, Nonce: nonce, ExpiresAt: until}, Input: in, PayloadHash: hash, Provider: s.Age.Info().Name, AgeFingerprint: fingerprint, ExpiresAt: until.Add(24 * time.Hour)}
		record = mobile.Record{OwnerID: actor, Key: fmt.Sprintf("age:%d:%s", actor, key), State: "age_preparing"}
		if err = set(tx, mobile.AuthChallenges, &record, value); err != nil {
			return err
		}
		if err = set(tx, mobile.AuthChallenges, &pointerRecord, agePointer{record.ID, until.Add(24 * time.Hour)}); err != nil {
			return err
		}
		created = true
		return recordAgeOperation(tx, actor, key, hash, record)
	})
	if err != nil {
		return out, err
	}
	if !created {
		return out, nil
	}
	// 先保留原请求，再调用供应商。丢失响应只查询同一reference，不能重新Create。
	result, providerErr := s.Age.Create(ctx, value.Request)
	return s.applyAgeResult(ctx, actor, record.ID, result, providerErr)
}
func (s *Service) QueryAgeAssurance(ctx context.Context, actor, id int64) (out AgeAssuranceStatus, err error) {
	if s.Age == nil {
		return out, mobile.ErrUnavailable
	}
	var record mobile.Record
	var value ageAssurance
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if err := active(tx, actor); err != nil {
			return err
		}
		var err error
		record, value, err = s.ageRecord(tx, actor, id)
		if err != nil {
			return err
		}
		if !ageActive(record.State) || !value.Request.ExpiresAt.After(s.Now()) {
			out, err = s.ageStatus(tx, record, value)
			return err
		}
		if value.QueriedAt.Add(3 * time.Second).After(s.Now()) {
			return mobile.ErrRateLimited
		}
		value.QueriedAt = s.Now()
		return set(tx, mobile.AuthChallenges, &record, value)
	})
	if err != nil {
		return out, err
	}
	if !ageActive(record.State) || !value.Request.ExpiresAt.After(s.Now()) {
		return out, nil
	}
	result, providerErr := s.Age.Query(ctx, value.Request)
	return s.applyAgeResult(ctx, actor, id, result, providerErr)
}
func (s *Service) applyAgeResult(ctx context.Context, actor, id int64, result AgeProviderResult, providerErr error) (out AgeAssuranceStatus, err error) {
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if err := active(tx, actor); err != nil {
			return err
		}
		r, v, err := s.ageRecord(tx, actor, id)
		if err != nil {
			return err
		}
		if !ageActive(r.State) || !v.Request.ExpiresAt.After(s.Now()) {
			out, err = s.ageStatus(tx, r, v)
			return err
		}
		if providerErr != nil {
			r.State = "age_unknown"
		} else {
			if result.Reference != v.Request.Reference || result.Nonce != v.Request.Nonce || !mobile.HasString([]string{"pending", "passed", "not_eligible", "cancelled", "failed", "expired"}, result.State) {
				return mobile.ErrUnavailable
			}
			if result.State == "pending" {
				launch, err := url.Parse(result.LaunchURL)
				if err != nil || len(result.LaunchURL) > 4096 || launch.Scheme != "https" || launch.User != nil || launch.Scheme+"://"+launch.Host != s.Age.Info().LaunchOrigin || launch.Fragment != "" {
					return mobile.ErrUnavailable
				}
				v.LaunchURL = result.LaunchURL
			}
			if result.State == "passed" {
				if !result.ValidUntil.After(s.Now()) || result.ValidUntil.After(s.Now().Add(366*24*time.Hour)) || result.EvidenceRef == "" || len(result.EvidenceRef) > 120 {
					return mobile.ErrUnavailable
				}
				fingerprint, err := ageFingerprint(tx, actor)
				if err != nil {
					return err
				}
				if fingerprint != v.AgeFingerprint {
					result.State = "superseded"
				} else {
					if err = mobile.RecordTrustedAgeProof(tx, actor, "adult", result.ValidUntil, result.EvidenceRef, "age-provider:"+v.Provider, s.Now()); err != nil {
						return err
					}
					v.ValidUntil = &result.ValidUntil
					v.ResultFingerprint, err = ageFingerprint(tx, actor)
					if err != nil {
						return err
					}
				}
			}
			r.State = "age_" + result.State
		}
		if !ageActive(r.State) {
			v.LaunchURL = ""
			v.Request.Reference = ""
			v.Request.Nonce = ""
		}
		if err = set(tx, mobile.AuthChallenges, &r, v); err != nil {
			return err
		}
		out = ageView(r, v, s.Now())
		return nil
	})
	return
}
