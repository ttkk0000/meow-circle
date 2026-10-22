package mobile

import (
	"crypto/subtle"
	"encoding/json"
	"fmt"
	"time"
)

type PurposeScope struct {
	Purpose         string `json:"purpose"`
	ResourceType    string `json:"resource_type"`
	ResourceID      int64  `json:"resource_id"`
	ResourceVersion int64  `json:"resource_version"`
	ScopeHash       string `json:"scope_hash"`
	Role            string `json:"role"`
}
type PurposeGrant struct {
	Scope             PurposeScope `json:"scope"`
	SessionKey        string       `json:"session_key"`
	ExpiresAt         time.Time    `json:"expires_at"`
	ConsumedOperation string       `json:"consumed_operation,omitempty"`
}

func ScopeFor(purpose, kind string, id, version int64, role string, value any) (PurposeScope, error) {
	b, e := json.Marshal(value)
	return PurposeScope{purpose, kind, id, version, Fingerprint(string(b)), role}, e
}
func (v PurposeScope) Valid() bool {
	if v.ResourceID < 0 || v.ResourceVersion < 0 || len(v.ScopeHash) != 64 || len(v.Role) > 40 || v.Role == "" {
		return false
	}
	for _, c := range v.ScopeHash {
		if !(c >= '0' && c <= '9' || c >= 'a' && c <= 'f') {
			return false
		}
	}
	return HasString([]string{"account.phone", "account.password", "account.unlink", "account.email", "identity.link", "guardian.consent", "data_request.submit", "rights_notice.submit", "contract.accept"}, v.Purpose)
}
func ConsumePurpose(tx Tx, actor int64, session, token, operation string, scope PurposeScope, now time.Time) error {
	if session == "" || len(token) < 32 || operation == "" || !scope.Valid() {
		return Denied("purpose_grant_required")
	}
	if e := CheckSession(tx, actor, session, now); e != nil {
		return e
	}
	r, e := tx.ByKey(AuthChallenges, Fingerprint(token))
	if e != nil {
		return Denied("purpose_grant_required")
	}
	v, e := Decode[PurposeGrant](r)
	if e != nil {
		return e
	}
	a, _ := json.Marshal(v.Scope)
	b, _ := json.Marshal(scope)
	if r.OwnerID != actor || r.State != "purpose_grant" || v.SessionKey != session || !v.ExpiresAt.After(now) || subtle.ConstantTimeCompare(a, b) != 1 {
		return Denied("purpose_grant_invalid")
	}
	v.ConsumedOperation, r.State = operation, "consumed"
	if e = SetBody(&r, v); e != nil {
		return e
	}
	return tx.Save(AuthChallenges, &r)
}
func CheckSession(tx Tx, actor int64, key string, now time.Time) error {
	r, e := tx.ByKey(Sessions, key)
	if e != nil {
		return ErrUnauthorized
	}
	v, e := Decode[struct {
		ExpiresAt time.Time `json:"expires_at"`
		Epoch     int64     `json:"epoch"`
	}](r)
	if e != nil {
		return e
	}
	if r.OwnerID != actor || r.State != "active" || !v.ExpiresAt.After(now) {
		return ErrUnauthorized
	}
	a, e := tx.ByKey(Accounts, fmt.Sprint(actor))
	if e == ErrNotFound {
		return nil
	}
	if e != nil {
		return e
	}
	body, e := Decode[struct {
		RevokeBefore time.Time `json:"revoke_before"`
		SessionEpoch int64     `json:"session_epoch"`
	}](a)
	if e != nil {
		return e
	}
	if a.State != "active" || v.Epoch != body.SessionEpoch || v.Epoch == 0 && !r.CreatedAt.After(body.RevokeBefore) {
		return ErrUnauthorized
	}
	return nil
}
