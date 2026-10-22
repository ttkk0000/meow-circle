package mobile

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
)

type sessionContextKey struct{}
type operatorContextKey struct{}

func WithOperator(ctx context.Context, id string) context.Context {
	return context.WithValue(ctx, operatorContextKey{}, id)
}
func OperatorID(ctx context.Context) string {
	v, _ := ctx.Value(operatorContextKey{}).(string)
	return v
}

func WithSession(ctx context.Context, token string) context.Context {
	return context.WithValue(ctx, sessionContextKey{}, Fingerprint(token))
}
func SessionKey(ctx context.Context) string {
	v, _ := ctx.Value(sessionContextKey{}).(string)
	return v
}

type CommandMeta struct {
	ClientRequestID string `json:"client_request_id"`
}

func CommandIdentity(input any) (string, string, error) { return commandKey(input) }

type Operation struct {
	ClientRequestID string `json:"client_request_id"`
	Command         string `json:"command"`
	ObjectID        int64  `json:"object_id"`
	PayloadHash     string `json:"payload_hash"`
	ResultKind      Kind   `json:"result_kind"`
	ResultID        int64  `json:"result_id"`
	ResultVersion   int64  `json:"result_version"`
	State           string `json:"state"`
}

func commandKey(input any) (string, string, error) {
	b, e := json.Marshal(input)
	if e != nil {
		return "", "", e
	}
	var fields map[string]json.RawMessage
	if e = json.Unmarshal(b, &fields); e != nil {
		return "", "", e
	}
	key := ""
	for _, name := range []string{"client_request_id", "idempotency_key"} {
		if raw := fields[name]; raw != nil {
			if e = json.Unmarshal(raw, &key); e != nil {
				return "", "", e
			}
			if key != "" {
				break
			}
		}
	}
	if len(key) < 8 || len(key) > 100 {
		return "", "", Invalid("client_request_id of 8 to 100 ASCII characters required")
	}
	for _, c := range key {
		if !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_') {
			return "", "", Invalid("invalid client_request_id")
		}
	}
	delete(fields, "reauth_token") // sensitive one-use token is not the logical command body
	delete(fields, "password")
	delete(fields, "code")
	canonical, e := json.Marshal(fields)
	return key, Fingerprint(string(canonical)), e
}

// Replay returns only a freshly permission-checked projection. Operation rows
// and business effects commit together; a lost response never creates a new key.
func (s *Service) command(ctx context.Context, actor int64, name string, object int64, input any, kind Kind, out *Record, fn func(Tx) error) error {
	key, hash, e := commandKey(input)
	if e != nil {
		return e
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if session := SessionKey(ctx); session != "" {
			if e := CheckSession(tx, actor, session, s.Now()); e != nil {
				return e
			}
		}
		r, e := tx.ByKey(Operations, fmt.Sprintf("%d:%s", actor, key))
		if e == nil {
			v, e := Decode[Operation](r)
			if e != nil {
				return e
			}
			if v.Command != name || v.ObjectID != object || v.PayloadHash != hash {
				return Denied("idempotency_conflict")
			}
			current, e := tx.Get(v.ResultKind, v.ResultID)
			if e != nil {
				return e
			}
			ok, e := s.canRead(tx, v.ResultKind, current, actor)
			if e != nil {
				return e
			}
			if !ok {
				return ErrForbidden
			}
			*out, e = project(v.ResultKind, current, actor)
			return e
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		if e = fn(tx); e != nil {
			return e
		}
		r = Record{OwnerID: actor, Key: fmt.Sprintf("%d:%s", actor, key), State: "committed"}
		v := Operation{key, name, object, hash, kind, out.ID, out.Version, "committed"}
		if e = SetBody(&r, v); e != nil {
			return e
		}
		return tx.Save(Operations, &r)
	})
}
func (s *Service) Operation(ctx context.Context, actor int64, key string) (out Operation, err error) {
	if len(key) < 8 || len(key) > 100 {
		return out, Invalid("invalid client_request_id")
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		r, e := tx.ByKey(Operations, fmt.Sprintf("%d:%s", actor, key))
		if e != nil {
			return e
		}
		out, e = Decode[Operation](r)
		if e != nil {
			return e
		}
		current, e := tx.Get(out.ResultKind, out.ResultID)
		if e != nil {
			return e
		}
		ok, e := s.canRead(tx, out.ResultKind, current, actor)
		if out.ResultKind == Accounts {
			ok, e = current.OwnerID == actor, nil
		}
		if e != nil {
			return e
		}
		if !ok {
			return ErrForbidden
		}
		out.PayloadHash = ""
		return nil
	})
	return
}
