package mobileapi

import (
	"crypto/subtle"
	"encoding/json"
	"fmt"
	"kitty-circle/internal/mobile"
	"net/http"
	"os"
	"strings"
)

// Credentials are loaded from deployment configuration, never consumer code.
// Each named worker is restricted to its own consumer and functional roles.
type Operator struct {
	ID        string   `json:"id"`
	Token     string   `json:"token"`
	Roles     []string `json:"roles"`
	Consumers []string `json:"consumers"`
}

func OperatorsFromEnvironment() ([]Operator, error) {
	raw := os.Getenv("MND_OPERATORS_JSON")
	if raw == "" {
		return nil, nil
	}
	var out []Operator
	if e := json.Unmarshal([]byte(raw), &out); e != nil {
		return nil, fmt.Errorf("invalid MND_OPERATORS_JSON")
	}
	ids, tokens := map[string]bool{}, map[string]bool{}
	for _, op := range out {
		if strings.TrimSpace(op.ID) == "" || strings.TrimSpace(op.ID) != op.ID || len(op.ID) > 80 || len(op.Token) < 32 || ids[op.ID] || tokens[op.Token] || len(op.Roles) == 0 {
			return nil, fmt.Errorf("invalid or duplicate named operator")
		}
		ids[op.ID], tokens[op.Token] = true, true
		for _, consumer := range op.Consumers {
			if !mobile.ValidConsumer(consumer) {
				return nil, fmt.Errorf("invalid operator consumer")
			}
		}
		for _, role := range op.Roles {
			switch role {
			case "identity_verifier", "authority_verifier", "worker", "privacy", "moderator", "evidence_reader", "finance":
			default:
				return nil, fmt.Errorf("invalid operator role")
			}
		}
	}
	return out, nil
}
func (h *Handler) operator(r *http.Request, role string) (Operator, bool) {
	token := r.Header.Get("X-Operator-Token")
	if len(token) < 32 {
		return Operator{}, false
	}
	for _, op := range h.Operators {
		if subtle.ConstantTimeCompare([]byte(op.Token), []byte(token)) != 1 {
			continue
		}
		for _, allowed := range op.Roles {
			if allowed == role {
				return op, true
			}
		}
	}
	return Operator{}, false
}
func operatorRole(path []string) string {
	if len(path) == 0 {
		return ""
	}
	switch path[0] {
	case "revenue":
		return "finance"
	case "push":
		return "worker"
	case "age-evidence", "guardian-evidence":
		return "identity_verifier"
	case "authority":
		return "authority_verifier"
	case "deliveries", "outbox", "provider-revocations", "nearby-alerts":
		return "worker"
	case "data-requests", "legacy-erasure", "media-erasure", "privacy-expiry", "challenge-expiry", "token-expiry", "upload-expiry":
		return "privacy"
	default:
		return "moderator"
	}
}
func (op Operator) permitsConsumer(name string) bool {
	for _, v := range op.Consumers {
		if v == name && strings.TrimSpace(name) != "" {
			return true
		}
	}
	return false
}
