package mobile_test

import (
	"context"
	"errors"
	"testing"

	"kitty-circle/internal/mobile"
)

// 本轮仅补关键回归源码，不运行测试。
func TestPetDiscussionUsesOriginalKeyAndCannotReplyAcrossParents(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	in := mobile.CommentInput{Content: mobile.RichContent{Text: "照护情况如何？"}, IdempotencyKey: "pet-comment-original"}
	comment, err := s.PetComment(ctx, 2, p.ID, in)
	if err != nil {
		t.Fatal(err)
	}
	replayed, err := s.PetComment(ctx, 2, p.ID, in)
	if err != nil || replayed.ID != comment.ID || replayed.Version != comment.Version {
		t.Fatal("original key changed result", err)
	}
	changed := in
	changed.Content.Text = "另一个问题"
	if _, err = s.PetComment(ctx, 2, p.ID, changed); err == nil {
		t.Fatal("same key accepted changed body")
	}
	other := pet(t, s, true)
	if _, err = s.PetComment(ctx, 2, other.ID, mobile.CommentInput{Content: mobile.RichContent{Text: "wrong parent"}, ReplyTo: comment.ID, IdempotencyKey: "cross-pet-comment"}); err == nil {
		t.Fatal("cross-parent reply accepted")
	}
	if err = s.Remove(ctx, 2, mobile.PetComments, comment.ID, comment.Version); err != nil {
		t.Fatal(err)
	}
	if _, err = s.PetComment(ctx, 1, p.ID, mobile.CommentInput{Content: mobile.RichContent{Text: "deleted parent reply"}, ReplyTo: comment.ID, IdempotencyKey: "deleted-comment-reply"}); err == nil {
		t.Fatal("deleted reply accepted")
	}
	page, err := s.List(ctx, 0, mobile.PetComments, 0, p.ID, 30)
	if err != nil || len(page.Items) != 0 {
		t.Fatal("deleted comment exposed", err)
	}
	profileOnly := pet(t, s, false)
	if _, err = s.PetComment(ctx, 2, profileOnly.ID, mobile.CommentInput{Content: mobile.RichContent{Text: "not adoption"}, IdempotencyKey: "not-adoption-comment"}); !errors.Is(err, mobile.ErrForbidden) {
		t.Fatal("ordinary profile opened adoption discussion", err)
	}
}

func TestCommentLikesRemainSeparateAndFollowCurrentParentPrivacy(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	comment, err := s.PetComment(ctx, 2, p.ID, mobile.CommentInput{Content: mobile.RichContent{StickerID: "sticker-01"}, IdempotencyKey: "pet-sticker-comment"})
	if err != nil {
		t.Fatal(err)
	}
	for i := 0; i < 2; i++ {
		if _, err = s.ReactComment(ctx, 3, mobile.PetComments, comment.ID, true); err != nil {
			t.Fatal(err)
		}
	}
	metrics, err := s.CommentMetrics(ctx, 3, mobile.PetComments, []int64{comment.ID})
	if err != nil || len(metrics) != 1 || metrics[0].Likes != 1 || !metrics[0].ViewerLiked {
		t.Fatal("desired-state reaction duplicated", metrics, err)
	}
	if _, err = s.ReactComment(ctx, 3, mobile.Comments, comment.ID, true); !errors.Is(err, mobile.ErrNotFound) {
		t.Fatal("comment kind confused", err)
	}
	petBody := body[mobile.Pet](t, p)
	petBody.Audience = mobile.Audience{Mode: "private"}
	petBody.AdoptionAvailable = false
	_, err = s.SavePet(ctx, 1, p.ID, mobile.PetInput{CommandMeta: meta(), Version: p.Version, Pet: petBody})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Get(ctx, 3, mobile.PetComments, comment.ID); !errors.Is(err, mobile.ErrNotFound) {
		t.Fatal("comment escaped parent privacy", err)
	}
	metrics, err = s.CommentMetrics(ctx, 3, mobile.PetComments, []int64{comment.ID})
	if err != nil || len(metrics) != 0 {
		t.Fatal("private comment count leaked", metrics, err)
	}
	if _, err = s.ReactComment(ctx, 3, mobile.PetComments, comment.ID, false); !errors.Is(err, mobile.ErrForbidden) {
		t.Fatal("revoked reader changed reaction", err)
	}
}

func TestOwnPublicationListSkipsDeletedBodies(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	entry, err := s.SaveEntry(ctx, 1, 0, declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Text: "待删除的日常", Space: "community", Audience: mobile.Audience{Mode: "public"}}}))
	if err != nil {
		t.Fatal(err)
	}
	if err = s.Remove(ctx, 1, mobile.Entries, entry.ID, entry.Version); err != nil {
		t.Fatal(err)
	}
	page, err := s.ListFiltered(ctx, 1, mobile.Entries, 0, 0, 30, mobile.Query{OwnerID: 1})
	if err != nil || len(page.Items) != 0 {
		t.Fatal("deleted empty body returned as a typed publication", err)
	}
}
