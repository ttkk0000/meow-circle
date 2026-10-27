package mobile_test

import (
	"context"
	"kitty-circle/internal/mobile"
	"testing"
	"time"
)

// Source-only: a catalog entry enables rule configuration, never blanket admission.
func TestCityRuleCanBeConfiguredOutsideOriginalTwoCities(t *testing.T) {
	s, _ := fixture(t)
	in := mobile.AuthorityInput{Scope: "city_rule", City: "成都市", Species: "cat", RuleVersion: "fixture-chengdu", Actions: []string{"adoption"}, EvidenceRef: "fixture-only", ValidUntil: time.Now().Add(time.Hour)}
	r, err := s.RecordAuthority(context.Background(), "test-policy", in)
	if err != nil {
		t.Fatal(err)
	}
	body, err := mobile.Decode[mobile.AuthorityInput](r)
	if err != nil || body.City != "成都" {
		t.Fatalf("city was not normalized: %v", err)
	}
	in.City, in.Version = "成都", r.Version
	updated, err := s.RecordAuthority(context.Background(), "test-policy", in)
	if err != nil || updated.ID != r.ID {
		t.Fatalf("alias created a separate city rule: %v", err)
	}
	in.City, in.Version = "不存在的城市", 0
	if _, err := s.RecordAuthority(context.Background(), "test-policy", in); err == nil {
		t.Fatal("unknown city rule accepted")
	}
	in.City = "延吉市"
	if _, err := s.RecordAuthority(context.Background(), "test-policy", in); err != nil {
		t.Fatal(err)
	}
	if _, err := s.RecordAuthority(context.Background(), "", in); err == nil {
		t.Fatal("operator authorization bypassed")
	}
}
