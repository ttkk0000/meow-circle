package mobileapi

import (
	"net/http/httptest"
	"strings"
	"testing"

	"kitty-circle/internal/mobile"
)

// 仅补角色边界回归源码，未执行。
func TestBusinessPageReadsRejectConsumerAndUnrelatedOperatorRoles(t *testing.T) {
	for _, path := range [][]string{{"review-queue"}, {"reviews", "1"}, {"guardian-invites", "1"}, {"data-requests", "1"}, {"rights-cases", "1"}} {
		for _, role := range []string{"ops_admin", "worker", "evidence_reader", "finance"} {
			token := strings.Repeat("fixture-only-", 4)
			h := Handler{Operators: []Operator{{ID: "fixture-operator", Token: token, Roles: []string{role}}}}
			r := httptest.NewRequest("GET", "/api/v2/ops/"+strings.Join(path, "/"), nil)
			r.Header.Set("X-Operator-Token", token)
			if _, err := h.operations(r, path); err != mobile.ErrForbidden {
				t.Fatal(path, role, err)
			}
			r.Header.Del("X-Operator-Token")
			r.Header.Set("Authorization", "Bearer consumer-fixture")
			if _, err := h.operations(r, path); err != mobile.ErrForbidden {
				t.Fatal("consumer granted operator read", path, err)
			}
		}
	}
}

func TestOperatorContextIncludesTraceablePublicDocumentsWithoutCredential(t *testing.T) {
	t.Setenv("MND_READING_CATALOG", "")
	value, err := operatorContext(Operator{ID: "fixture-operator", Token: "never-return-credential", Roles: []string{"moderator"}})
	if err != nil || len(value.Documents) == 0 || len(value.PublishedTypes) != 0 {
		t.Fatal(value, err)
	}
	for _, document := range value.Documents {
		if document.Revision == "" || document.SHA256 == "" {
			t.Fatal("untraceable public document", document.Key)
		}
	}
}
