package mobile_test

import (
	"bytes"
	"context"
	"testing"

	"kitty-circle/internal/mobile"
)

func sourceInput(t *testing.T, s *mobile.Service) mobile.SourceDeclarationInput {
	t.Helper()
	content := declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Text: "待发布的日常", Space: "community", Audience: mobile.Audience{Mode: "public"}}})
	return mobile.SourceDeclarationInput{CommandMeta: meta(), Content: content, RightsConfirmed: true}
}

func TestSourceDeclarationCommitsWithoutPublishing(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	in := sourceInput(t, s)
	r, e := s.DeclareSource(ctx, 1, in)
	if e != nil {
		t.Fatal(e)
	}
	if r.ID <= 0 || r.OwnerID != 1 || r.Public {
		t.Fatal("invalid private source record")
	}
	receipt, e := s.Operation(ctx, 1, in.ClientRequestID)
	if e != nil || receipt.ResultKind != mobile.SourceDeclarations || receipt.ResultID != r.ID {
		t.Fatal("missing atomic source receipt", e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error {
		count, err := tx.Count(mobile.Entries, mobile.Filter{OwnerID: 1})
		if count != 0 {
			t.Fatal("a declaration must not publish a post")
		}
		return err
	}); e != nil {
		t.Fatal(e)
	}
}

func TestSourceCorrectionAppendsAndReplaysOriginal(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	in := sourceInput(t, s)
	first, e := s.DeclareSource(ctx, 1, in)
	if e != nil {
		t.Fatal(e)
	}
	replay, e := s.DeclareSource(ctx, 1, in)
	if e != nil || replay.ID != first.ID {
		t.Fatal("original key did not replay", e)
	}
	changed := in
	changed.CommandMeta = meta()
	changed.PreviousID = first.ID
	changed.Content.Declaration.Origin = "ai_generated"
	p, e := s.PreviewPublication(ctx, 1, changed.Content)
	if e != nil {
		t.Fatal(e)
	}
	changed.Content.Declaration.ManifestHash = p.ManifestHash
	second, e := s.DeclareSource(ctx, 1, changed)
	if e != nil || second.ID == first.ID || second.RefID != first.ID {
		t.Fatal("correction did not append", e)
	}
	old, e := s.Get(ctx, 1, mobile.SourceDeclarations, first.ID)
	if e != nil || !bytes.Equal(old.Data, first.Data) {
		t.Fatal("previous declaration was overwritten", e)
	}
}

func TestSourceRecordRejectsStaleContentAndForeignOwner(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	in := sourceInput(t, s)
	r, e := s.DeclareSource(ctx, 1, in)
	if e != nil {
		t.Fatal(e)
	}
	stale := in
	stale.CommandMeta = meta()
	stale.Content.Entry.Text = "已变化的原稿"
	if _, e = s.DeclareSource(ctx, 1, stale); e == nil {
		t.Fatal("stale manifest accepted")
	}
	if _, e = s.Get(ctx, 2, mobile.SourceDeclarations, r.ID); e == nil {
		t.Fatal("another actor read the source record")
	}
	foreign := in.Content
	foreign.CommandMeta = meta()
	p, e := s.PreviewPublication(ctx, 2, foreign)
	if e != nil {
		t.Fatal(e)
	}
	foreign.Declaration.ManifestHash = p.ManifestHash
	foreign.Declaration.RecordID = r.ID
	if _, e = s.SaveEntry(ctx, 2, 0, foreign); e == nil {
		t.Fatal("foreign declaration used for publishing")
	}
	in.CommandMeta = meta()
	in.RightsConfirmed = false
	if _, e = s.DeclareSource(ctx, 1, in); e == nil {
		t.Fatal("missing material rights confirmation accepted")
	}
}

func TestPrivateSourceKeepsPrivateMediaAndKnownAILabel(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	photo := mobile.Record{OwnerID: 1, State: "ready"}
	if e := repo.Update(ctx, func(tx mobile.Tx) error {
		if e := mobile.SetBody(&photo, mobile.MediaFile{Scope: "private", MIME: "image/png", KnownAI: true, SHA256: mobile.Fingerprint("private original")}); e != nil {
			return e
		}
		return tx.Save(mobile.Media, &photo)
	}); e != nil {
		t.Fatal(e)
	}
	in := mobile.EntryInput{Entry: mobile.Entry{Text: "私密原稿", Space: "circle", Audience: mobile.Audience{Mode: "private"}}, MediaIDs: []int64{photo.ID}}
	in.Declaration.Origin = "original"
	if _, e := s.PreviewPublication(ctx, 1, in); e == nil {
		t.Fatal("known AI cannot be denied in a private declaration")
	}
	in.Declaration.Origin = "ai_generated"
	p, e := s.PreviewPublication(ctx, 1, in)
	if e != nil {
		t.Fatal(e)
	}
	in.Declaration.ManifestHash = p.ManifestHash
	if _, e = s.DeclareSource(ctx, 1, mobile.SourceDeclarationInput{CommandMeta: meta(), Content: in, RightsConfirmed: true}); e != nil {
		t.Fatal(e)
	}
	in.Entry.Space = "community"
	in.Entry.Audience.Mode = "public"
	if _, e = s.PreviewPublication(ctx, 1, in); e == nil {
		t.Fatal("private media was promoted to public")
	}
}
