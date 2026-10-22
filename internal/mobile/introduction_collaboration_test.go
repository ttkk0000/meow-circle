package mobile_test

import (
	"context"
	"errors"
	"testing"

	"kitty-circle/internal/mobile"
)

// 回归源码：本次不执行测试。保留照片只允许当前宠物已附媒体，不变成跨对象授权。
func TestIntroductionEditorRetainsOnlyCurrentPetPhotos(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	photo := func(owner int64, scope string, knownAI bool) mobile.Record {
		t.Helper()
		r := mobile.Record{OwnerID: owner, State: "ready"}
		if e := repo.Update(ctx, func(tx mobile.Tx) error {
			if e := mobile.SetBody(&r, mobile.MediaFile{Scope: scope, MIME: "image/png", KnownAI: knownAI}); e != nil {
				return e
			}
			return tx.Save(mobile.Media, &r)
		}); e != nil {
			t.Fatal(e)
		}
		return r
	}
	original := photo(1, "public", false)
	unrelated := photo(1, "public", false)
	private := photo(4, "private", false)
	generated := photo(4, "public", true)
	added := photo(4, "public", false)
	if e := repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.Pets, p.ID)
		if e != nil {
			return e
		}
		r.MediaIDs = []int64{original.ID}
		if e = tx.Save(mobile.Pets, &r); e != nil {
			return e
		}
		p = r
		return nil
	}); e != nil {
		t.Fatal(e)
	}
	invite, e := s.Invite(ctx, 1, mobile.InviteInput{CommandMeta: meta(), Scope: "adoption_editor", TargetID: p.ID, UserID: 4})
	if e != nil {
		t.Fatal(e)
	}
	invite, e = s.Invitation(ctx, 4, invite.ID, invite.Version, "accept")
	if e != nil {
		t.Fatal(e)
	}
	p, e = s.Introduction(ctx, 4, p.ID, mobile.IntroductionInput{CommandMeta: meta(), Version: p.Version, Bio: "更新介绍，保留原照片", MediaIDs: []int64{original.ID}})
	if e != nil || len(p.MediaIDs) != 1 || p.MediaIDs[0] != original.ID {
		t.Fatalf("retaining current photo: %+v %v", p, e)
	}
	for _, media := range []mobile.Record{unrelated, private, generated} {
		if _, e = s.Introduction(ctx, 4, p.ID, mobile.IntroductionInput{CommandMeta: meta(), Version: p.Version, Bio: "不允许的照片", MediaIDs: []int64{media.ID}}); e == nil {
			t.Fatalf("accepted forbidden media %d", media.ID)
		}
	}
	p, e = s.Introduction(ctx, 4, p.ID, mobile.IntroductionInput{CommandMeta: meta(), Version: p.Version, Bio: "保留并添加自己的公开照片", MediaIDs: []int64{original.ID, added.ID}})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Invitation(ctx, 1, invite.ID, invite.Version, "revoke"); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Introduction(ctx, 4, p.ID, mobile.IntroductionInput{CommandMeta: meta(), Version: p.Version, Bio: "撤销后不可继续修改", MediaIDs: []int64{original.ID}}); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatalf("revoked editor: %v", e)
	}
}
