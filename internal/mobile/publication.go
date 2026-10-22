package mobile

import (
	"context"
	"encoding/json"
)

type PublicationDeclaration struct {
	ManifestHash string `json:"manifest_hash"`
	Origin       string `json:"origin"` // original, ai_generated, ai_modified, unknown
	Confirmed    bool   `json:"confirmed"`
}
type PublicationPreview struct {
	ManifestHash         string `json:"manifest_hash"`
	Origin               string `json:"origin"`
	RequiresConfirmation bool   `json:"requires_confirmation"`
}

func (s *Service) publicationManifest(tx Tx, actor int64, in EntryInput) (PublicationPreview, error) {
	in.Entry.ContentOrigin = ""
	if !HasString([]string{"original", "ai_generated", "ai_modified", "unknown"}, in.Declaration.Origin) {
		return PublicationPreview{}, Invalid("choose a content origin")
	}
	if e := s.publicAttachments(tx, actor, in.MediaIDs); e != nil {
		return PublicationPreview{}, e
	}
	type part struct {
		ID      int64  `json:"id"`
		Hash    string `json:"hash"`
		Version int64  `json:"version"`
	}
	parts := []part{}
	for _, id := range in.MediaIDs {
		r, e := tx.Get(Media, id)
		if e != nil {
			return PublicationPreview{}, e
		}
		m, e := Decode[MediaFile](r)
		if e != nil {
			return PublicationPreview{}, e
		}
		if m.KnownAI && in.Declaration.Origin == "original" {
			return PublicationPreview{}, Denied("known_ai_cannot_be_denied")
		}
		parts = append(parts, part{id, m.SHA256, r.Version})
	}
	b, e := json.Marshal(struct {
		Actor   int64  `json:"actor"`
		Version int64  `json:"version"`
		Entry   Entry  `json:"entry"`
		Parts   []part `json:"parts"`
		Origin  string `json:"origin"`
	}{actor, in.Version, in.Entry, parts, in.Declaration.Origin})
	return PublicationPreview{Fingerprint(string(b)), in.Declaration.Origin, true}, e
}
func (s *Service) PreviewPublication(ctx context.Context, actor int64, in EntryInput) (out PublicationPreview, err error) {
	err = s.Repo.View(ctx, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		var e error
		out, e = s.publicationManifest(tx, actor, in)
		return e
	})
	return
}
