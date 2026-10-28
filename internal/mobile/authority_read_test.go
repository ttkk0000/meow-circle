package mobile_test

import (
	"context"
	"errors"
	"kitty-circle/internal/mobile"
	"testing"
	"time"
)

// 仅回归源码，未执行；按自然键读取版本不能授予全量目录权限。
func TestAuthoritySnapshotProvidesCurrentVersionForExactScope(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	p := pet(t, s, false)
	lookup := mobile.AuthorityLookup{ActorID: 1, PetID: p.ID, Scope: "care", Species: "cat"}
	missing, e := s.AuthoritySnapshot(ctx, "test-verifier", lookup)
	if e != nil || missing.Exists || missing.Record != nil {
		t.Fatal(missing, e)
	}
	in := mobile.AuthorityInput{ActorID: 1, PetID: p.ID, Scope: "care", Species: "cat", EvidenceRef: "trusted-care-reference", ValidUntil: s.Now().Add(24 * time.Hour)}
	r, e := s.RecordAuthority(ctx, "test-verifier", in)
	if e != nil {
		t.Fatal(e)
	}
	snapshot, e := s.AuthoritySnapshot(ctx, "test-verifier", lookup)
	if e != nil || !snapshot.Exists || snapshot.Record == nil || snapshot.Record.ID != r.ID || snapshot.Record.Version != r.Version {
		t.Fatal(snapshot, e)
	}
	in.Version = snapshot.Record.Version
	in.EvidenceRef = "updated-trusted-reference"
	if _, e = s.RecordAuthority(ctx, "test-verifier", in); e != nil {
		t.Fatal(e)
	}
	if _, e = s.RecordAuthority(ctx, "test-verifier", in); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("旧版本仍可覆盖新记录", e)
	}
	lookup.Scope = "disposition"
	other, e := s.AuthoritySnapshot(ctx, "test-verifier", lookup)
	if e != nil || other.Exists || other.Record != nil {
		t.Fatal("读到了其他授权范围", other, e)
	}
}

func TestAuthoritySnapshotRejectsUnnamedOperatorAndInvalidScope(t *testing.T) {
	s, _ := fixture(t)
	for _, in := range []mobile.AuthorityLookup{{Scope: "unknown", Species: "cat"}, {Scope: "city_rule", City: "不存在的城市", Species: "cat"}, {Scope: "care", Species: "cat"}} {
		if _, e := s.AuthoritySnapshot(context.Background(), "test-verifier", in); e == nil {
			t.Fatal("无效对象或范围获准读取", in)
		}
	}
	if _, e := s.AuthoritySnapshot(context.Background(), "", mobile.AuthorityLookup{Scope: "city_rule", City: "武汉", Species: "cat"}); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatal(e)
	}
}
