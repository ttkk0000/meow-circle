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

// 核验读取仍需独立authority_verifier，不因是后台账号或管理员便放行。
func TestAuthorityLookupRejectsOtherOperatorRoles(t *testing.T) {
	for _, role := range []string{"moderator", "privacy", "identity_verifier", "finance", "evidence_reader", "worker", "ops_viewer", "ops_oncall", "ops_release", "ops_admin"} {
		token := strings.Repeat("fixture-authority-", 3)
		h := Handler{Operators: []Operator{{ID: "fixture-operator", Token: token, Roles: []string{role}}}}
		r := httptest.NewRequest("GET", "/api/v2/ops/authority?scope=city_rule&city=武汉&species=cat", nil)
		r.Header.Set("X-Operator-Token", token)
		if _, e := h.operations(r, []string{"authority"}); e != mobile.ErrForbidden {
			t.Fatal(role, e)
		}
	}
}
