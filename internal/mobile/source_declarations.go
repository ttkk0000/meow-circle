package mobile

import "context"

type SourceDeclarationInput struct {
	CommandMeta
	Content         EntryInput `json:"content"`
	RightsConfirmed bool       `json:"rights_confirmed"`
	PreviousID      int64      `json:"previous_id"`
}

// 只保留本次清单摘要与来源，不复制草稿正文、位置或照片到声明记录。
type SourceDeclarationBody struct {
	ManifestHash string `json:"manifest_hash"`
	Origin       string `json:"origin"`
	PreviousID   int64  `json:"previous_id"`
}

func (s *Service) DeclareSource(ctx context.Context, actor int64, in SourceDeclarationInput) (out Record, err error) {
	if !in.RightsConfirmed || in.PreviousID < 0 || textValid(in.Content.Entry.Text, 10000, false) != nil || audienceValid(in.Content.Entry.Audience) != nil {
		return out, Invalid("explicit source and material rights confirmation required")
	}
	err = s.command(ctx, actor, "source.declare", in.PreviousID, in, SourceDeclarations, &out, func(tx Tx) error {
		if e := s.active(tx, actor); e != nil {
			return e
		}
		if in.PreviousID > 0 {
			previous, e := tx.Get(SourceDeclarations, in.PreviousID)
			if e != nil {
				return e
			}
			if e = owner(previous, actor); e != nil {
				return e
			}
			if previous.State != "active" {
				return ErrForbidden
			}
		}
		preview, e := s.publicationManifest(tx, actor, in.Content)
		if e != nil {
			return e
		}
		if in.Content.Declaration.ManifestHash != preview.ManifestHash {
			return Denied("publication_declaration_required")
		}
		out = Record{OwnerID: actor, State: "active", RefID: in.PreviousID}
		return save(tx, SourceDeclarations, &out, SourceDeclarationBody{preview.ManifestHash, preview.Origin, in.PreviousID}, actor, "source.declared")
	})
	return
}

func (s *Service) requireSourceRecord(tx Tx, actor int64, declaration PublicationDeclaration) error {
	if declaration.RecordID == 0 {
		return nil
	} // 兼容已取得manifest的旧客户端。
	if declaration.RecordID < 0 {
		return ErrForbidden
	}
	r, e := tx.Get(SourceDeclarations, declaration.RecordID)
	if e != nil {
		return e
	}
	if e = owner(r, actor); e != nil {
		return e
	}
	if r.State != "active" {
		return ErrForbidden
	}
	body, e := Decode[SourceDeclarationBody](r)
	if e != nil {
		return e
	}
	if body.ManifestHash != declaration.ManifestHash || body.Origin != declaration.Origin {
		return Denied("publication_declaration_required")
	}
	return nil
}
