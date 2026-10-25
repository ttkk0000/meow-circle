package identity

import (
	"context"
	"crypto/rand"
	"errors"
	"fmt"
	"math/big"
	"net/mail"
	"strings"
	"time"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/auth"
)

type EmailSender interface {
	Send(context.Context, string, string, string) error
}

func normalizeEmail(value string) (string, error) {
	v := strings.ToLower(strings.TrimSpace(value))
	a, e := mail.ParseAddress(v)
	if e != nil || a.Address != v || len(v) > 254 || !strings.Contains(v, ".") {
		return "", mobile.Invalid("invalid email")
	}
	return v, nil
}
func username(value string) (string, error) {
	v := strings.ToLower(strings.TrimSpace(value))
	if len(v) < 3 || len(v) > 32 {
		return "", mobile.Invalid("username must be 3 to 32 characters")
	}
	letter := false
	for _, c := range v {
		if c >= 'a' && c <= 'z' {
			letter = true
		} else if !(c >= '0' && c <= '9' || c == '_') {
			return "", mobile.Invalid("username accepts letters, digits and underscore")
		}
	}
	if !letter {
		return "", mobile.Invalid("username must contain a letter")
	}
	return v, nil
}

type EmailOTPInput struct {
	SafetyToken string `json:"-"`
	Email       string `json:"email"`
	Purpose     string `json:"purpose"`
}

func (s *Service) IssueEmailOTP(ctx context.Context, actor int64, client string, in EmailOTPInput) (out OTPResult, err error) {
	if s.Email == nil {
		return out, mobile.ErrUnavailable
	}
	address, e := normalizeEmail(in.Email)
	if e != nil {
		return out, e
	}
	if !mobile.HasString([]string{"email_register", "email_login", "email_bind", "email_recover", "rights_contact"}, in.Purpose) {
		return out, mobile.Invalid("invalid email purpose")
	}
	if in.Purpose == "email_bind" && actor <= 0 {
		return out, mobile.ErrUnauthorized
	}
	if in.Purpose != "email_bind" {
		actor = 0
	}
	if e = s.budget(ctx, "email-send:"+digest(client)); e != nil {
		return out, e
	}
	n, e := rand.Int(rand.Reader, big.NewInt(1000000))
	if e != nil {
		return out, e
	}
	code := fmt.Sprintf("%06d", n.Int64())
	var challenge mobile.Record
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if in.Purpose == "rights_contact" {
			r, e := tx.ByKey(mobile.AuthChallenges, mobile.Fingerprint(in.SafetyToken))
			if e != nil {
				return mobile.ErrForbidden
			}
			v, e := mobile.Decode[mobile.SafetySession](r)
			if e != nil {
				return e
			}
			if r.State != "safety_session" || !v.ExpiresAt.After(s.Now()) {
				return mobile.ErrForbidden
			}
		}
		if actor > 0 {
			if e := active(tx, actor); e != nil {
				return e
			}
		}
		now := s.Now()
		r, e := tx.ByKey(mobile.OTPChallenges, in.Purpose+":"+address)
		v := OTP{Window: now}
		if errors.Is(e, mobile.ErrNotFound) {
			r = mobile.Record{Key: in.Purpose + ":" + address}
		} else if e != nil {
			return e
		} else {
			v, e = mobile.Decode[OTP](r)
			if e != nil {
				return e
			}
			if now.Sub(v.SentAt) < time.Minute {
				return mobile.ErrRateLimited
			}
			if now.Sub(v.Window) >= time.Hour {
				v.Count, v.Window = 0, now
			}
			if v.Count >= 5 {
				return mobile.ErrRateLimited
			}
		}
		v.Phone, v.Purpose, v.ActorID, v.Hash = address, in.Purpose, actor, s.codeHash(address, in.Purpose, code)
		if in.Purpose == "rights_contact" {
			v.SessionKey = mobile.Fingerprint(in.SafetyToken)
		}
		v.ExpiresAt, v.SentAt, v.Attempts, v.Count = now.Add(5*time.Minute), now, 0, v.Count+1
		r.State = "sending"
		if e = set(tx, mobile.OTPChallenges, &r, v); e != nil {
			return e
		}
		challenge, out = r, OTPResult{r.ID, v.ExpiresAt, 60}
		return nil
	})
	if err != nil {
		return
	}
	delivery := s.Email.Send(ctx, address, code, in.Purpose)
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.OTPChallenges, challenge.ID)
		if e != nil {
			return e
		}
		if r.Version != challenge.Version {
			return mobile.ErrConflict
		}
		r.State = "ready"
		if delivery != nil {
			r.State = "failed"
		}
		return tx.Save(mobile.OTPChallenges, &r)
	})
	if delivery != nil {
		err = mobile.ErrUnavailable
	}
	return
}

type EmailInput struct {
	Email       string `json:"email"`
	Code        string `json:"code"`
	Username    string `json:"username"`
	Password    string `json:"password"`
	Device      string `json:"device"`
	AcceptTerms bool   `json:"accept_terms"`
}

// EmailLogin never merges an existing account or registers implicitly.
func (s *Service) EmailLogin(ctx context.Context, in EmailInput, register bool) (out LoginResult, err error) {
	address, e := normalizeEmail(in.Email)
	if e != nil {
		return out, e
	}
	purpose, name, hash, salt := "email_login", "", "", ""
	if register {
		purpose = "email_register"
		if !in.AcceptTerms || len(in.Password) < 10 || len(in.Password) > 128 {
			return out, mobile.Invalid("accept terms and use a 10 to 128 byte password")
		}
		name, e = username(in.Username)
		if e != nil {
			return out, e
		}
		hash, salt, e = auth.HashPassword(in.Password)
		if e != nil {
			return out, e
		}
	}
	valid := false
	err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		ok, e := s.consumeOTP(tx, 0, address, purpose, in.Code)
		if e != nil || !ok {
			return e
		}
		valid = true
		link, e := tx.ByKey(mobile.Identities, "email:"+address)
		var u domain.User
		if register {
			if e == nil {
				return identifierInUse()
			}
			if !errors.Is(e, mobile.ErrNotFound) {
				return e
			}
			u = domain.User{Username: name, Nickname: "新朋友", PasswordHash: hash, PasswordSalt: salt}
			if e = tx.SaveUser(&u); e != nil {
				return e
			}
			link = mobile.Record{OwnerID: u.ID, Key: "email:" + address, State: "active"}
			if e = set(tx, mobile.Identities, &link, Subject{Provider: "email", ID: address}); e != nil {
				return e
			}
		} else {
			if errors.Is(e, mobile.ErrNotFound) {
				return mobile.ErrUnauthorized
			}
			if e != nil {
				return e
			}
			if link.State != "active" {
				return mobile.ErrUnauthorized
			}
			u, e = tx.User(link.OwnerID)
			if e != nil {
				return e
			}
		}
		if _, _, e = s.account(tx, u.ID); e != nil {
			return e
		}
		out, e = s.issue(tx, u, in.Device)
		out.Created = register
		return e
	})
	if err == nil && !valid {
		err = mobile.ErrUnauthorized
	}
	return
}
func (s *Service) RecoverEmail(ctx context.Context, in EmailInput) error {
	address, e := normalizeEmail(in.Email)
	if e != nil {
		return e
	}
	if len(in.Password) < 10 || len(in.Password) > 128 {
		return mobile.Invalid("password must be 10 to 128 bytes")
	}
	hash, salt, e := auth.HashPassword(in.Password)
	if e != nil {
		return e
	}
	valid := false
	e = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		ok, e := s.consumeOTP(tx, 0, address, "email_recover", in.Code)
		if e != nil || !ok {
			return e
		}
		link, e := tx.ByKey(mobile.Identities, "email:"+address)
		if e != nil || link.State != "active" {
			return mobile.ErrUnauthorized
		}
		if e = active(tx, link.OwnerID); e != nil {
			return e
		}
		u, e := tx.User(link.OwnerID)
		if e != nil {
			return e
		}
		u.PasswordHash, u.PasswordSalt = hash, salt
		if e = tx.SaveUser(&u); e != nil {
			return e
		}
		r, a, e := s.account(tx, u.ID)
		if e != nil {
			return e
		}
		a.RevokeBefore, valid = s.Now(), true
		a.SessionEpoch++
		if e := mobile.DisablePushDevices(tx, r.OwnerID, "", true); e != nil {
			return e
		}
		return set(tx, mobile.Accounts, &r, a)
	})
	if e == nil && !valid {
		return mobile.ErrUnauthorized
	}
	return e
}
func (s *Service) VerifyPhone(ctx context.Context, actor int64, code string) error {
	valid := false
	e := s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		u, e := tx.User(actor)
		if e != nil {
			return e
		}
		ok, e := s.consumeOTP(tx, actor, u.Phone, "verify_phone", code)
		if e != nil || !ok {
			return e
		}
		valid = true
		return mobile.RecordPhoneProof(tx, actor, u.Phone, s.Now())
	})
	if e == nil && !valid {
		return mobile.ErrUnauthorized
	}
	return e
}
