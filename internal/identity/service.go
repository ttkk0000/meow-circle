// Package identity owns login challenges, linked identities and revocable sessions.
// Provider verification and SMS delivery are ports; external calls occur outside
// repository transactions. No provider is enabled by the presence of a button.
package identity

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"math/big"
	"strings"
	"time"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/auth"
	"kitty-circle/internal/platform/phoneotp"
)

type SMS interface {
	Send(context.Context, string, string, string) error
}
type Provider interface {
	Verify(context.Context, Credential, string) (Subject, error)
}
type Credential struct {
	Code          string `json:"code"`
	IdentityToken string `json:"identity_token"`
	AccessToken   string `json:"access_token"`
}
type Subject struct {
	Provider string
	AppID    string
	ID       string
	Secret   string `json:"secret,omitempty"` // encrypted provider revocation token; never projected to clients
}
type Service struct {
	Repo      mobile.Repository
	SMS       SMS
	Providers map[string]Provider
	Secret    []byte
	Now       func() time.Time
	Email     EmailSender
}

func New(repo mobile.Repository, secret string, sms SMS, providers map[string]Provider) *Service {
	return &Service{Repo: repo, SMS: sms, Providers: providers, Secret: []byte(secret), Now: func() time.Time { return time.Now().UTC() }}
}
func randomToken() (string, error) {
	b := make([]byte, 32)
	if _, e := rand.Read(b); e != nil {
		return "", e
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}
func digest(v string) string { h := sha256.Sum256([]byte(v)); return hex.EncodeToString(h[:]) }
func (s *Service) codeHash(phone, purpose, code string) string {
	h := hmac.New(sha256.New, s.Secret)
	_, _ = h.Write([]byte(phone + ":" + purpose + ":" + code))
	return hex.EncodeToString(h.Sum(nil))
}
func phoneValid(phone string) (string, error) {
	p := phoneotp.NormalizePhone(phone)
	if !phoneotp.ValidPhone(p) {
		return "", mobile.Invalid("invalid phone")
	}
	return p, nil
}

type Account struct {
	SessionEpoch        int64      `json:"session_epoch"`
	Theme               string     `json:"theme,omitempty"`
	Background          string     `json:"background,omitempty"`
	RevokeBefore        time.Time  `json:"revoke_before"`
	DeletionRequestedAt *time.Time `json:"deletion_requested_at,omitempty"`
}
type Session struct {
	Epoch            int64     `json:"epoch"`
	ExpiresAt        time.Time `json:"expires_at"`
	RefreshHash      string    `json:"refresh_hash"`
	RefreshExpiresAt time.Time `json:"refresh_expires_at"`
	Device           string    `json:"device"`
}
type LoginResult struct {
	User         domain.User `json:"user"`
	Token        string      `json:"token"`
	RefreshToken string      `json:"refresh_token"`
	ExpiresAt    time.Time   `json:"expires_at"`
	Created      bool        `json:"created"`
}
type OTP struct {
	SessionKey string    `json:"session_key,omitempty"`
	Phone      string    `json:"phone"`
	Purpose    string    `json:"purpose"`
	Hash       string    `json:"hash"`
	ExpiresAt  time.Time `json:"expires_at"`
	Attempts   int       `json:"attempts"`
	SentAt     time.Time `json:"sent_at"`
	Window     time.Time `json:"window"`
	Count      int       `json:"count"`
	ActorID    int64     `json:"actor_id"`
}
type Challenge struct {
	Scope      *mobile.PurposeScope `json:"scope,omitempty"`
	SessionKey string               `json:"session_key,omitempty"`
	Provider   string               `json:"provider"`
	Nonce      string               `json:"nonce"`
	ExpiresAt  time.Time            `json:"expires_at"`
	Subject    *Subject             `json:"subject,omitempty"`
	Resume     string               `json:"resume,omitempty"`
}

func active(tx mobile.Tx, actor int64) error {
	if actor <= 0 {
		return mobile.ErrUnauthorized
	}
	if _, e := tx.User(actor); e != nil {
		return e
	}
	r, e := tx.ByKey(mobile.Accounts, fmt.Sprint(actor))
	if errors.Is(e, mobile.ErrNotFound) {
		return nil
	}
	if e != nil {
		return e
	}
	if r.State != "active" {
		return mobile.ErrUnauthorized
	}
	return nil
}
func set(tx mobile.Tx, k mobile.Kind, r *mobile.Record, v any) error {
	if e := mobile.SetBody(r, v); e != nil {
		return e
	}
	return tx.Save(k, r)
}
func (s *Service) account(tx mobile.Tx, user int64) (mobile.Record, Account, error) {
	r, e := tx.ByKey(mobile.Accounts, fmt.Sprint(user))
	if errors.Is(e, mobile.ErrNotFound) {
		r = mobile.Record{OwnerID: user, Key: fmt.Sprint(user), State: "active"}
		e = set(tx, mobile.Accounts, &r, Account{SessionEpoch: 1})
	}
	if e != nil {
		return r, Account{}, e
	}
	a, e := mobile.Decode[Account](r)
	return r, a, e
}
func (s *Service) issue(tx mobile.Tx, u domain.User, device string) (out LoginResult, err error) {
	if err = active(tx, u.ID); err != nil {
		return
	}
	account, a, e := s.account(tx, u.ID)
	if e != nil {
		return out, e
	}
	if a.SessionEpoch <= 0 {
		a.SessionEpoch = 1
		if e = mobile.DisablePushDevices(tx, u.ID, "", true); e != nil {
			return out, e
		}
		if e = set(tx, mobile.Accounts, &account, a); e != nil {
			return out, e
		}
	}
	if len(device) > 120 {
		return out, mobile.Invalid("device too long")
	}
	access, e := randomToken()
	if e != nil {
		return out, e
	}
	refresh, e := randomToken()
	if e != nil {
		return out, e
	}
	now := s.Now()
	r := mobile.Record{OwnerID: u.ID, Key: digest(access), State: "active"}
	expiry := now.Add(time.Hour)
	if e = set(tx, mobile.Sessions, &r, Session{Epoch: a.SessionEpoch, ExpiresAt: expiry, RefreshHash: digest(refresh), RefreshExpiresAt: now.Add(30 * 24 * time.Hour), Device: device}); e != nil {
		return out, e
	}
	return LoginResult{User: u, Token: access, RefreshToken: refresh, ExpiresAt: expiry}, nil
}
func (s *Service) Authenticate(ctx context.Context, token string) (user domain.User, err error) {
	if len(token) < 32 || len(token) > 256 {
		return user, mobile.ErrUnauthorized
	}
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.Sessions, digest(token))
		if errors.Is(e, mobile.ErrNotFound) {
			return mobile.ErrUnauthorized
		}
		if e != nil {
			return e
		}
		v, e := mobile.Decode[Session](r)
		if e != nil {
			return e
		}
		if r.State != "active" || !v.ExpiresAt.After(s.Now()) {
			return mobile.ErrUnauthorized
		}
		if e = active(tx, r.OwnerID); e != nil {
			return e
		}
		a, e := tx.ByKey(mobile.Accounts, fmt.Sprint(r.OwnerID))
		if e == nil {
			body, e := mobile.Decode[Account](a)
			if e != nil {
				return e
			}
			if v.Epoch != body.SessionEpoch || v.Epoch == 0 && !r.CreatedAt.After(body.RevokeBefore) {
				return mobile.ErrUnauthorized
			}
		} else if !errors.Is(e, mobile.ErrNotFound) {
			return e
		}
		user, e = tx.User(r.OwnerID)
		return e
	})
	return
}
func (s *Service) Refresh(ctx context.Context, access, refresh, device string) (out LoginResult, err error) {
	err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.Sessions, digest(access))
		if e != nil {
			return mobile.ErrUnauthorized
		}
		v, e := mobile.Decode[Session](r)
		if e != nil {
			return e
		}
		if r.State != "active" || !v.RefreshExpiresAt.After(s.Now()) || !hmac.Equal([]byte(v.RefreshHash), []byte(digest(refresh))) {
			return mobile.ErrUnauthorized
		}
		if e = active(tx, r.OwnerID); e != nil {
			return e
		}
		account, e := tx.ByKey(mobile.Accounts, fmt.Sprint(r.OwnerID))
		if e == nil {
			a, e := mobile.Decode[Account](account)
			if e != nil {
				return e
			}
			if v.Epoch != a.SessionEpoch || v.Epoch == 0 && !r.CreatedAt.After(a.RevokeBefore) {
				return mobile.ErrUnauthorized
			}
		} else if !errors.Is(e, mobile.ErrNotFound) {
			return e
		}
		u, e := tx.User(r.OwnerID)
		if e != nil {
			return e
		}
		r.State = "rotated"
		if e = tx.Save(mobile.Sessions, &r); e != nil {
			return e
		}
		out, e = s.issue(tx, u, device)
		if e != nil {
			return e
		}
		return mobile.RotatePushSession(tx, u.ID, digest(access), digest(out.Token))
	})
	return
}
func (s *Service) Logout(ctx context.Context, actor int64, token string, all bool) error {
	return s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		if all {
			r, a, e := s.account(tx, actor)
			if e != nil {
				return e
			}
			a.RevokeBefore = s.Now()
			a.SessionEpoch++
			if e := mobile.DisablePushDevices(tx, r.OwnerID, "", true); e != nil {
				return e
			}
			return set(tx, mobile.Accounts, &r, a)
		}
		r, e := tx.ByKey(mobile.Sessions, digest(token))
		if e != nil {
			return e
		}
		if r.OwnerID != actor {
			return mobile.ErrForbidden
		}
		if e = mobile.DisablePushDevices(tx, actor, digest(token), false); e != nil {
			return e
		}
		r.State = "revoked"
		return tx.Save(mobile.Sessions, &r)
	})
}

type OTPInput struct {
	Phone   string `json:"phone"`
	Purpose string `json:"purpose"`
}
type OTPResult struct {
	ID                int64     `json:"id"`
	ExpiresAt         time.Time `json:"expires_at"`
	RetryAfterSeconds int       `json:"retry_after_seconds"`
}

func (s *Service) IssueOTP(ctx context.Context, actor int64, client string, in OTPInput) (out OTPResult, err error) {
	if s.SMS == nil {
		return out, mobile.ErrUnavailable
	}
	phone, e := phoneValid(in.Phone)
	if e != nil {
		return out, e
	}
	if !mobile.HasString([]string{"login", "recover", "bind_phone", "verify_phone", "reauth"}, in.Purpose) {
		return out, mobile.Invalid("invalid OTP purpose")
	}
	if (in.Purpose == "bind_phone" || in.Purpose == "verify_phone" || in.Purpose == "reauth") && actor <= 0 {
		return out, mobile.ErrUnauthorized
	}
	number, e := rand.Int(rand.Reader, big.NewInt(1000000))
	if e != nil {
		return out, e
	}
	code := fmt.Sprintf("%06d", number.Int64())
	var challenge mobile.Record
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if actor > 0 {
			if e := active(tx, actor); e != nil {
				return e
			}
		}
		if in.Purpose == "reauth" || in.Purpose == "verify_phone" {
			u, e := tx.User(actor)
			if e != nil {
				return e
			}
			if u.Phone != phone {
				return mobile.ErrForbidden
			}
		}
		now := s.Now()
		ipKey := "ip:" + digest(client)
		ip, e := tx.ByKey(mobile.OTPChallenges, ipKey)
		if errors.Is(e, mobile.ErrNotFound) {
			ip = mobile.Record{Key: ipKey, State: "rate_limit"}
		} else if e != nil {
			return e
		}
		rate := OTP{Window: now}
		if ip.ID > 0 {
			rate, e = mobile.Decode[OTP](ip)
			if e != nil {
				return e
			}
		}
		if now.Sub(rate.Window) >= time.Hour {
			rate.Window = now
			rate.Count = 0
		}
		if rate.Count >= 20 {
			return mobile.ErrRateLimited
		}
		rate.Count++
		if e = set(tx, mobile.OTPChallenges, &ip, rate); e != nil {
			return e
		}
		key := in.Purpose + ":" + phone
		r, e := tx.ByKey(mobile.OTPChallenges, key)
		v := OTP{Window: now}
		if errors.Is(e, mobile.ErrNotFound) {
			r = mobile.Record{Key: key}
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
				v.Window = now
				v.Count = 0
			}
			if v.Count >= 5 {
				return mobile.ErrRateLimited
			}
		}
		v.Phone = phone
		v.Purpose = in.Purpose
		v.Hash = s.codeHash(phone, in.Purpose, code)
		v.ActorID = actor
		v.ExpiresAt = now.Add(5 * time.Minute)
		v.SentAt = now
		v.Attempts = 0
		v.Count++
		r.State = "sending"
		if e = set(tx, mobile.OTPChallenges, &r, v); e != nil {
			return e
		}
		challenge = r
		out = OTPResult{r.ID, v.ExpiresAt, 60}
		return nil
	})
	if err != nil {
		return
	}
	delivery := s.SMS.Send(ctx, phone, code, in.Purpose)
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

// consumeOTP commits failed attempt counters. Callers return authentication
// failure after committing this transaction, rather than rolling counters back.
func (s *Service) consumeOTP(tx mobile.Tx, actor int64, phone, purpose, code string) (bool, error) {
	r, e := tx.ByKey(mobile.OTPChallenges, purpose+":"+phone)
	if errors.Is(e, mobile.ErrNotFound) {
		return false, nil
	}
	if e != nil {
		return false, e
	}
	v, e := mobile.Decode[OTP](r)
	if e != nil {
		return false, e
	}
	if r.State != "ready" || !v.ExpiresAt.After(s.Now()) || v.Attempts >= 5 || v.ActorID != actor {
		return false, nil
	}
	v.Attempts++
	ok := hmac.Equal([]byte(v.Hash), []byte(s.codeHash(phone, purpose, code)))
	if ok {
		r.State = "consumed"
	} else if v.Attempts >= 5 {
		r.State = "locked"
	}
	return ok, set(tx, mobile.OTPChallenges, &r, v)
}

type PhoneLogin struct {
	Phone       string `json:"phone"`
	Code        string `json:"code"`
	Device      string `json:"device"`
	AcceptTerms bool   `json:"accept_terms"`
}

func (s *Service) PhoneLogin(ctx context.Context, in PhoneLogin) (out LoginResult, err error) {
	phone, e := phoneValid(in.Phone)
	if e != nil {
		return out, e
	}
	valid := false
	err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		ok, e := s.consumeOTP(tx, 0, phone, "login", in.Code)
		if e != nil || !ok {
			return e
		}
		valid = true
		u, e := tx.UserByPhone(phone)
		created := false
		if errors.Is(e, mobile.ErrNotFound) {
			return mobile.ErrUnauthorized
		} else if e != nil {
			return e
		}
		if _, _, e = s.account(tx, u.ID); e != nil {
			return e
		}
		if e = mobile.RecordPhoneProof(tx, u.ID, phone, s.Now()); e != nil {
			return e
		}
		out, e = s.issue(tx, u, in.Device)
		out.Created = created
		return e
	})
	if err == nil && !valid {
		err = mobile.ErrUnauthorized
	}
	return
}

type PasswordInput struct {
	Email    string `json:"email"`
	Username string `json:"username"`
	Phone    string `json:"phone"`
	Password string `json:"password"`
	Device   string `json:"device"`
}

func (s *Service) Password(ctx context.Context, in PasswordInput) (out LoginResult, err error) {
	identifiers := 0
	for _, v := range []string{in.Username, in.Phone, in.Email} {
		if strings.TrimSpace(v) != "" {
			identifiers++
		}
	}
	if identifiers != 1 {
		return out, mobile.Invalid("provide exactly one account identifier")
	}
	if len(in.Username) > 120 || len(in.Phone) > 40 || len(in.Email) > 254 {
		return out, mobile.ErrUnauthorized
	}
	if e := s.budget(ctx, "password:"+strings.ToLower(strings.TrimSpace(in.Username))+":"+phoneotp.NormalizePhone(in.Phone)+":"+strings.ToLower(in.Email)); e != nil {
		return out, e
	}
	if len(in.Password) > 1024 {
		return out, mobile.ErrUnauthorized
	}
	var u domain.User
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		var e error
		if in.Email != "" {
			var address string
			address, e = normalizeEmail(in.Email)
			if e != nil {
				return mobile.ErrUnauthorized
			}
			var link mobile.Record
			link, e = tx.ByKey(mobile.Identities, "email:"+address)
			if e == nil && link.State == "active" {
				u, e = tx.User(link.OwnerID)
			} else if e == nil {
				e = mobile.ErrNotFound
			}
		} else if in.Phone != "" {
			p, normalizeErr := phoneValid(in.Phone)
			if normalizeErr != nil {
				return mobile.ErrUnauthorized
			}
			u, e = tx.UserByPhone(p)
		} else {
			u, e = tx.UserByName(strings.TrimSpace(in.Username))
		}
		if errors.Is(e, mobile.ErrNotFound) {
			return mobile.ErrUnauthorized
		}
		return e
	})
	if err != nil {
		return
	}
	if !auth.VerifyPassword(in.Password, u.PasswordHash, u.PasswordSalt) {
		return out, mobile.ErrUnauthorized
	}
	err = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		fresh, e := tx.User(u.ID)
		if e != nil {
			return e
		}
		if fresh.PasswordHash != u.PasswordHash || fresh.PasswordSalt != u.PasswordSalt {
			return mobile.ErrConflict
		}
		out, e = s.issue(tx, fresh, in.Device)
		return e
	})
	return
}
func (s *Service) Recover(ctx context.Context, phone, code, password string) error {
	p, e := phoneValid(phone)
	if e != nil {
		return e
	}
	if len(password) < 10 || len(password) > 128 {
		return mobile.Invalid("password must be 10 to 128 bytes")
	}
	hash, salt, e := auth.HashPassword(password)
	if e != nil {
		return e
	}
	valid := false
	e = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		ok, e := s.consumeOTP(tx, 0, p, "recover", code)
		if e != nil || !ok {
			return e
		}
		u, e := tx.UserByPhone(p)
		if errors.Is(e, mobile.ErrNotFound) {
			return nil
		}
		if e != nil {
			return e
		}
		if e = active(tx, u.ID); e != nil {
			return e
		}
		u.PasswordHash = hash
		u.PasswordSalt = salt
		if e = tx.SaveUser(&u); e != nil {
			return e
		}
		r, a, e := s.account(tx, u.ID)
		if e != nil {
			return e
		}
		a.RevokeBefore = s.Now()
		a.SessionEpoch++
		if e := mobile.DisablePushDevices(tx, r.OwnerID, "", true); e != nil {
			return e
		}
		valid = true
		return set(tx, mobile.Accounts, &r, a)
	})
	if e == nil && !valid {
		return mobile.ErrUnauthorized
	}
	return e
}

type ReauthInput struct {
	Password string              `json:"password"`
	Code     string              `json:"code"`
	Scope    mobile.PurposeScope `json:"scope"`
}

func (s *Service) Reauth(ctx context.Context, actor int64, in ReauthInput) (grant string, err error) {
	if !in.Scope.Valid() || mobile.SessionKey(ctx) == "" {
		return "", mobile.Invalid("a valid purpose scope and current session are required")
	}
	if e := s.budget(ctx, fmt.Sprintf("reauth:%d", actor)); e != nil {
		return "", e
	}
	valid := false
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		u, e := tx.User(actor)
		if e != nil {
			return e
		}
		if in.Password != "" && len(in.Password) <= 128 {
			valid = auth.VerifyPassword(in.Password, u.PasswordHash, u.PasswordSalt)
		} else if u.Phone != "" {
			valid, e = s.consumeOTP(tx, actor, u.Phone, "reauth", in.Code)
			if e != nil {
				return e
			}
		}
		if !valid {
			return nil
		}
		grant, e = randomToken()
		if e != nil {
			return e
		}
		r := mobile.Record{OwnerID: actor, Key: digest(grant), State: "purpose_grant"}
		return set(tx, mobile.AuthChallenges, &r, mobile.PurposeGrant{Scope: in.Scope, SessionKey: mobile.SessionKey(ctx), ExpiresAt: s.Now().Add(5 * time.Minute)})
	})
	if err == nil && !valid {
		err = mobile.ErrUnauthorized
	}
	return
}

type OAuthStart struct {
	Provider string               `json:"provider"`
	Resume   string               `json:"resume"`
	Scope    *mobile.PurposeScope `json:"scope,omitempty"`
}
type OAuthChallenge struct {
	State     string    `json:"state"`
	Nonce     string    `json:"nonce"`
	ExpiresAt time.Time `json:"expires_at"`
}

func (s *Service) Start(ctx context.Context, actor int64, in OAuthStart) (out OAuthChallenge, err error) {
	if in.Scope != nil && (actor <= 0 || !in.Scope.Valid() || mobile.SessionKey(ctx) == "") {
		return out, mobile.Invalid("scoped provider verification requires a current session")
	}
	if s.Providers[in.Provider] == nil {
		return out, mobile.ErrUnavailable
	}
	if len(in.Resume) > 120 || strings.Contains(in.Resume, "://") {
		return out, mobile.Invalid("resume must be an internal task key")
	}
	state, e := randomToken()
	if e != nil {
		return out, e
	}
	nonce, e := randomToken()
	if e != nil {
		return out, e
	}
	out = OAuthChallenge{state, nonce, s.Now().Add(5 * time.Minute)}
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if actor > 0 {
			if e := active(tx, actor); e != nil {
				return e
			}
		}
		r := mobile.Record{OwnerID: actor, Key: digest(state), State: "pending"}
		return set(tx, mobile.AuthChallenges, &r, Challenge{Provider: in.Provider, Nonce: nonce, ExpiresAt: out.ExpiresAt, Resume: in.Resume, Scope: in.Scope, SessionKey: mobile.SessionKey(ctx)})
	})
	return
}

type OAuthFinish struct {
	State      string     `json:"state"`
	Credential Credential `json:"credential"`
	Cancelled  bool       `json:"cancelled"`
}
type OAuthResult struct {
	Login              *LoginResult `json:"login,omitempty"`
	ResolutionToken    string       `json:"resolution_token,omitempty"`
	Resume             string       `json:"resume,omitempty"`
	NeedsAccountChoice bool         `json:"needs_account_choice"`
	ReauthToken        string       `json:"reauth_token,omitempty"`
}

func subjectKey(v Subject) string { return v.Provider + ":" + v.AppID + ":" + v.ID }
func (s *Service) Finish(ctx context.Context, actor int64, in OAuthFinish) (out OAuthResult, err error) {
	var challenge mobile.Record
	var c Challenge
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.AuthChallenges, digest(in.State))
		if e != nil {
			return mobile.ErrUnauthorized
		}
		c, e = mobile.Decode[Challenge](r)
		if e != nil {
			return e
		}
		if r.OwnerID != actor || r.State != "pending" || !c.ExpiresAt.After(s.Now()) {
			return mobile.ErrUnauthorized
		}
		r.State = "verifying"
		if in.Cancelled {
			r.State = "cancelled"
		}
		if e = tx.Save(mobile.AuthChallenges, &r); e != nil {
			return e
		}
		challenge = r
		return nil
	})
	if err != nil || in.Cancelled {
		return
	}
	p := s.Providers[c.Provider]
	if p == nil {
		return out, mobile.ErrUnavailable
	}
	subject, e := p.Verify(ctx, in.Credential, c.Nonce)
	if e != nil {
		return out, e
	}
	if subject.Provider != c.Provider || subject.ID == "" || subject.AppID == "" {
		return out, mobile.ErrUnauthorized
	}
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.AuthChallenges, challenge.ID)
		if e != nil {
			return e
		}
		if r.Version != challenge.Version || r.State != "verifying" || !c.ExpiresAt.After(s.Now()) {
			return mobile.ErrConflict
		}
		out.Resume = c.Resume
		linked, e := tx.ByKey(mobile.Identities, subjectKey(subject))
		if e == nil && linked.State == "active" {
			if actor > 0 && linked.OwnerID != actor {
				return mobile.ErrConflict
			}
			u, e := tx.User(linked.OwnerID)
			if e != nil {
				return e
			}
			if c.Scope == nil {
				login, e := s.issue(tx, u, "")
				if e != nil {
					return e
				}
				out.Login = &login
			}
			if subject.Secret != "" {
				if e = set(tx, mobile.Identities, &linked, subject); e != nil {
					return e
				}
			}
			if actor > 0 && c.Scope != nil {
				if c.SessionKey != mobile.SessionKey(ctx) {
					return mobile.ErrUnauthorized
				}
				grant, e := randomToken()
				if e != nil {
					return e
				}
				proof := mobile.Record{OwnerID: actor, Key: digest(grant), State: "purpose_grant"}
				if e = set(tx, mobile.AuthChallenges, &proof, mobile.PurposeGrant{Scope: *c.Scope, SessionKey: c.SessionKey, ExpiresAt: s.Now().Add(5 * time.Minute)}); e != nil {
					return e
				}
				out.ReauthToken = grant
			}
			r.State = "consumed"
			return tx.Save(mobile.AuthChallenges, &r)
		}
		if e != nil && !errors.Is(e, mobile.ErrNotFound) {
			return e
		}
		resolution, e := randomToken()
		if e != nil {
			return e
		}
		r.Key = digest(resolution)
		r.State = "verified"
		c.Subject = &subject
		c.ExpiresAt = s.Now().Add(5 * time.Minute)
		if e = set(tx, mobile.AuthChallenges, &r, c); e != nil {
			return e
		}
		out.ResolutionToken = resolution
		out.NeedsAccountChoice = true
		return nil
	})
	return
}

type ResolveInput struct {
	Token       string `json:"token"`
	Create      bool   `json:"create"`
	AcceptTerms bool   `json:"accept_terms"`
	ReauthToken string `json:"reauth_token"`
	Device      string `json:"device"`
}

func (s *Service) Resolve(ctx context.Context, actor int64, in ResolveInput) (out LoginResult, err error) {
	err = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.AuthChallenges, digest(in.Token))
		if e != nil {
			return mobile.ErrUnauthorized
		}
		v, e := mobile.Decode[Challenge](r)
		if e != nil {
			return e
		}
		if r.State != "verified" || v.Subject == nil || !v.ExpiresAt.After(s.Now()) {
			return mobile.ErrUnauthorized
		}
		var u domain.User
		created := false
		if in.Create {
			if actor > 0 || r.OwnerID > 0 || !in.AcceptTerms {
				return mobile.ErrForbidden
			}
			suffix, e := randomToken()
			if e != nil {
				return e
			}
			u = domain.User{Username: "mnd_" + suffix[:16], Nickname: "新朋友"}
			if e = tx.SaveUser(&u); e != nil {
				return e
			}
			created = true
		} else {
			if actor <= 0 || (r.OwnerID > 0 && r.OwnerID != actor) {
				return mobile.ErrUnauthorized
			}
			scope, scopeErr := mobile.ScopeFor("identity.link", string(mobile.AuthChallenges), r.ID, r.Version, "owner", subjectKey(*v.Subject))
			if scopeErr != nil {
				return scopeErr
			}
			if e = mobile.ConsumePurpose(tx, actor, mobile.SessionKey(ctx), in.ReauthToken, digest(in.ReauthToken), scope, s.Now()); e != nil {
				return e
			}
			u, e = tx.User(actor)
			if e != nil {
				return e
			}
		}
		key := subjectKey(*v.Subject)
		link, e := tx.ByKey(mobile.Identities, key)
		if e == nil {
			previous, e := mobile.Decode[Subject](link)
			if e != nil {
				return e
			}
			if link.State == "revoked" && previous.Secret != "" {
				return mobile.ErrConflict
			}
			if link.State == "active" && link.OwnerID != u.ID {
				return mobile.ErrConflict
			}
			link.OwnerID = u.ID
		} else if errors.Is(e, mobile.ErrNotFound) {
			link = mobile.Record{OwnerID: u.ID, Key: key}
		} else {
			return e
		}
		link.State = "active"
		if e = set(tx, mobile.Identities, &link, v.Subject); e != nil {
			return e
		}
		r.State = "consumed"
		if e = tx.Save(mobile.AuthChallenges, &r); e != nil {
			return e
		}
		if _, _, e = s.account(tx, u.ID); e != nil {
			return e
		}
		out, e = s.issue(tx, u, in.Device)
		out.Created = created
		return e
	})
	return
}

type AccountInput struct {
	mobile.CommandMeta
	Version     int64  `json:"version"`
	Email       string `json:"email"`
	ReauthToken string `json:"reauth_token"`
	Phone       string `json:"phone"`
	Code        string `json:"code"`
	Password    string `json:"password"`
	IdentityID  int64  `json:"identity_id"`
}

func (s *Service) Methods(ctx context.Context, actor int64) (out map[string]any, err error) {
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		u, e := tx.User(actor)
		if e != nil {
			return e
		}
		links, e := all(tx, mobile.Identities, mobile.Filter{OwnerID: actor, State: "active"})
		if e != nil {
			return e
		}
		providers := []map[string]any{}
		for _, r := range links {
			v, e := mobile.Decode[Subject](r)
			if e != nil {
				return e
			}
			providers = append(providers, map[string]any{"id": r.ID, "provider": v.Provider})
		}
		phone := ""
		if len(u.Phone) >= 7 {
			phone = u.Phone[:3] + "****" + u.Phone[len(u.Phone)-4:]
		}
		enabled := []string{}
		for _, name := range []string{"wechat", "qq", "apple"} {
			if s.Providers[name] != nil {
				enabled = append(enabled, name)
			}
		}
		out = map[string]any{"phone": phone, "password_set": u.PasswordHash != "", "linked": providers, "available_providers": enabled, "sms_available": s.SMS != nil}
		return nil
	})
	return
}
func (s *Service) Account(ctx context.Context, actor int64, action string, in AccountInput) error {
	if e := s.budget(ctx, fmt.Sprintf("account:%d", actor)); e != nil {
		return e
	}
	key, payloadHash, e := mobile.CommandIdentity(in)
	scopeForHash, scopeErr := s.accountScope(actor, action, in)
	if scopeErr != nil {
		return scopeErr
	}
	_, payloadHash, e = mobile.CommandIdentity(struct {
		mobile.CommandMeta
		Scope mobile.PurposeScope `json:"scope"`
	}{in.CommandMeta, scopeForHash})
	if e != nil {
		return e
	}
	if !mobile.HasString([]string{"phone", "password", "unlink", "email"}, action) {
		return mobile.Invalid("use a typed data request for account closure")
	}
	var hash, salt string
	if action == "password" {
		if len(in.Password) < 10 || len(in.Password) > 128 {
			return mobile.Invalid("password must be 10 to 128 bytes")
		}
		hash, salt, e = auth.HashPassword(in.Password)
		if e != nil {
			return e
		}
	}
	invalidOTP := false
	e = s.actorUpdate(ctx, actor, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		operationKey := fmt.Sprintf("%d:%s", actor, key)
		previous, e := tx.ByKey(mobile.Operations, operationKey)
		if e == nil {
			v, e := mobile.Decode[mobile.Operation](previous)
			if e != nil {
				return e
			}
			if v.Command != "account."+action || v.PayloadHash != payloadHash {
				return mobile.Denied("idempotency_conflict")
			}
			return nil
		}
		if !errors.Is(e, mobile.ErrNotFound) {
			return e
		}
		u, e := tx.User(actor)
		if e != nil {
			return e
		}
		r, a, e := s.account(tx, actor)
		if e != nil {
			return e
		}
		if r.Version != in.Version {
			return mobile.ErrConflict
		}
		scope, e := s.accountScope(actor, action, in)
		if e != nil {
			return e
		}
		if e = mobile.ConsumePurpose(tx, actor, mobile.SessionKey(ctx), in.ReauthToken, in.ClientRequestID, scope, s.Now()); e != nil {
			return e
		}
		switch action {
		case "phone":
			phone, e := phoneValid(in.Phone)
			if e != nil {
				return e
			}
			ok, e := s.consumeOTP(tx, actor, phone, "bind_phone", in.Code)
			if e != nil {
				return e
			}
			if !ok {
				invalidOTP = true
				return mobile.ErrUnauthorized
			}
			u.Phone = phone
			if e = tx.SaveUser(&u); e != nil {
				return e
			}
			if e = mobile.RecordPhoneProof(tx, actor, phone, s.Now()); e != nil {
				return e
			}
		case "password":
			u.PasswordHash, u.PasswordSalt = hash, salt
			if e = tx.SaveUser(&u); e != nil {
				return e
			}
		case "email":
			address, e := normalizeEmail(in.Email)
			if e != nil {
				return e
			}
			ok, e := s.consumeOTP(tx, actor, address, "email_bind", in.Code)
			if e != nil {
				return e
			}
			if !ok {
				invalidOTP = true
				return mobile.ErrUnauthorized
			}
			if _, e = tx.ByKey(mobile.Identities, "email:"+address); e == nil {
				return mobile.ErrConflict
			} else if !errors.Is(e, mobile.ErrNotFound) {
				return e
			}
			links, e := all(tx, mobile.Identities, mobile.Filter{OwnerID: actor, State: "active"})
			if e != nil {
				return e
			}
			for _, link := range links {
				v, e := mobile.Decode[Subject](link)
				if e != nil {
					return e
				}
				if v.Provider == "email" {
					link.State = "revoked"
					if e = tx.Save(mobile.Identities, &link); e != nil {
						return e
					}
				}
			}
			link := mobile.Record{OwnerID: actor, Key: "email:" + address, State: "active"}
			if e = set(tx, mobile.Identities, &link, Subject{Provider: "email", ID: address}); e != nil {
				return e
			}
		case "unlink":
			link, e := tx.Get(mobile.Identities, in.IdentityID)
			if e != nil {
				return e
			}
			if link.OwnerID != actor || link.State != "active" {
				return mobile.ErrForbidden
			}
			links, e := all(tx, mobile.Identities, mobile.Filter{OwnerID: actor, State: "active"})
			if e != nil {
				return e
			}
			usable := u.PasswordHash != "" && u.PasswordSalt != "" || u.Phone != "" && s.SMS != nil
			for _, candidate := range links {
				if candidate.ID == link.ID {
					continue
				}
				subject, e := mobile.Decode[Subject](candidate)
				if e != nil {
					return e
				}
				if subject.Provider == "email" && s.Email != nil || s.Providers[subject.Provider] != nil {
					usable = true
				}
			}
			if !usable {
				return mobile.Invalid("retain another login method")
			}
			link.State = "revoked"
			if e = tx.Save(mobile.Identities, &link); e != nil {
				return e
			}
			subject, e := mobile.Decode[Subject](link)
			if e != nil {
				return e
			}
			if subject.Provider != "email" {
				if e = queueRevocation(tx, link); e != nil {
					return e
				}
			}
		}
		a.RevokeBefore = s.Now()
		a.SessionEpoch++
		if e := mobile.DisablePushDevices(tx, r.OwnerID, "", true); e != nil {
			return e
		}
		if e = set(tx, mobile.Accounts, &r, a); e != nil {
			return e
		}
		operation := mobile.Record{OwnerID: actor, Key: operationKey, State: "committed"}
		return set(tx, mobile.Operations, &operation, mobile.Operation{ClientRequestID: key, Command: "account." + action, ObjectID: actor, PayloadHash: payloadHash, ResultKind: mobile.Accounts, ResultID: r.ID, ResultVersion: r.Version, State: "committed"})
	})
	if e == nil && invalidOTP {
		return mobile.ErrUnauthorized
	}
	return e
}
func (s *Service) accountScope(actor int64, action string, in AccountInput) (mobile.PurposeScope, error) {
	if !mobile.HasString([]string{"phone", "password", "unlink", "email"}, action) {
		return mobile.PurposeScope{}, mobile.Invalid("invalid account action")
	}
	value := struct {
		Phone        string `json:"phone"`
		Email        string `json:"email"`
		PasswordHash string `json:"password_hash"`
		IdentityID   int64  `json:"identity_id"`
	}{in.Phone, in.Email, s.codeHash(fmt.Sprint(actor), "account.password.scope", in.Password), in.IdentityID}
	return mobile.ScopeFor("account."+action, string(mobile.Accounts), actor, in.Version, "owner", value)
}
func (s *Service) AccountScope(ctx context.Context, actor int64, action string, in AccountInput) (out mobile.PurposeScope, err error) {
	err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		if e := active(tx, actor); e != nil {
			return e
		}
		r, e := tx.ByKey(mobile.Accounts, fmt.Sprint(actor))
		if e != nil {
			return e
		}
		if r.Version != in.Version {
			return mobile.ErrConflict
		}
		out, e = s.accountScope(actor, action, in)
		return e
	})
	return
}

func all(tx mobile.Tx, k mobile.Kind, f mobile.Filter) ([]mobile.Record, error) {
	out := []mobile.Record{}
	f.Limit = 500
	for {
		rows, e := tx.List(k, f)
		if e != nil {
			return nil, e
		}
		out = append(out, rows...)
		if len(rows) < 500 {
			return out, nil
		}
		f.After = rows[len(rows)-1].ID
	}
}
