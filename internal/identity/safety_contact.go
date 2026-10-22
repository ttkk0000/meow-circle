package identity

import (
	"context"
	"kitty-circle/internal/mobile"
)

func (s *Service) VerifySafetyContact(ctx context.Context, token string, in EmailInput) error {
	if len(token) < 40 || len(token) > 100 {
		return mobile.ErrForbidden
	}
	address, e := normalizeEmail(in.Email)
	if e != nil {
		return e
	}
	valid := false
	e = s.Repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.AuthChallenges, mobile.Fingerprint(token))
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
		challenge, e := tx.ByKey(mobile.OTPChallenges, "rights_contact:"+address)
		if e != nil {
			return mobile.ErrUnauthorized
		}
		otp, e := mobile.Decode[OTP](challenge)
		if e != nil {
			return e
		}
		if otp.SessionKey != mobile.Fingerprint(token) {
			return mobile.ErrForbidden
		}
		ok, e := s.consumeOTP(tx, 0, address, "rights_contact", in.Code)
		if e != nil || !ok {
			return e
		}
		valid = true
		v.Contact, v.ContactVerified = address, true
		return set(tx, mobile.AuthChallenges, &r, v)
	})
	if e == nil && !valid {
		return mobile.ErrUnauthorized
	}
	return e
}
