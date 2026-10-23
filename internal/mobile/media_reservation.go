package mobile

import (
	"context"
	"fmt"
	"time"
)

// Reserve before touching disk. Failed writes remain tracked, count toward the
// guest quota and can be retried after their lease; they cannot create unlimited
// unregistered files by repeatedly failing a post-write quota check.
func (s *Service) ReserveMedia(ctx context.Context, actor int64, token, key string, file MediaFile) (out Record, err error) {
	if len(key) < 8 || len(key) > 100 || len(file.SHA256) != 64 || (file.MIME != "image/png" && file.MIME != "image/jpeg") {
		return out, Invalid("invalid media reservation")
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		ref := int64(0)
		recordKey := fmt.Sprintf("%d:%s", actor, key)
		if token != "" {
			session, _, e := safetySession(tx, token, s.Now())
			if e != nil {
				return e
			}
			ref = session.ID
			recordKey = "safety:" + Fingerprint(token) + ":" + key
			file.Scope = "private"
		} else {
			if e := s.checkActor(ctx, tx, actor); e != nil {
				return e
			}
		}
		r, e := tx.ByKey(Media, recordKey)
		if e == nil {
			old, e := Decode[MediaFile](r)
			if e != nil {
				return e
			}
			if old.SHA256 != file.SHA256 || old.Scope != file.Scope || old.KnownAI != file.KnownAI || old.SourceLabel != file.SourceLabel {
				return Denied("idempotency_conflict")
			}
			if r.State == "ready" {
				out = r
				return nil
			}
			if r.State != "upload_expired" && r.State != "uploading" || old.UploadLeaseUntil != nil && old.UploadLeaseUntil.After(s.Now()) {
				return ErrConflict
			}
		} else if e != ErrNotFound {
			return e
		} else {
			if token != "" {
				count, e := tx.Count(Media, Filter{RefID: ref})
				if e != nil {
					return e
				}
				if count >= 36 {
					return ErrRateLimited
				}
			}
			r = Record{OwnerID: actor, RefID: ref, Key: recordKey, State: "uploading"}
			if e = SetBody(&r, file); e != nil {
				return e
			}
			if e = tx.Save(Media, &r); e != nil {
				return e
			}
		}
		// 每次租约使用不同物理文件；旧进程迟到写入不能覆盖新一次重试。
		extension := ".jpg"
		if file.MIME == "image/png" {
			extension = ".png"
		}
		file.Filename = fmt.Sprintf("mnd-%d-%d-%s%s", r.ID, r.Version+1, file.SHA256, extension)
		r.State = "uploading"
		until := s.Now().Add(2 * time.Minute)
		file.UploadLeaseUntil = &until
		if e = SetBody(&r, file); e != nil {
			return e
		}
		if e = tx.Save(Media, &r); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
func (s *Service) CompleteReservedMedia(ctx context.Context, actor int64, token string, id, version int64) (out Record, err error) {
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(Media, id)
		if e != nil {
			return e
		}
		if token != "" {
			session, _, e := safetySession(tx, token, s.Now())
			if e != nil {
				return e
			}
			if r.RefID != session.ID || r.OwnerID != 0 {
				return ErrForbidden
			}
		} else {
			if e = s.checkActor(ctx, tx, actor); e != nil {
				return e
			}
			if e = owner(r, actor); e != nil {
				return e
			}
		}
		if e = expected(r, version); e != nil {
			return e
		}
		file, e := Decode[MediaFile](r)
		if e != nil {
			return e
		}
		if r.State != "uploading" || file.UploadLeaseUntil == nil || !file.UploadLeaseUntil.After(s.Now()) {
			return ErrConflict
		}
		r.State = "ready"
		file.UploadLeaseUntil = nil
		if e = save(tx, Media, &r, file, actor, "media.stored"); e != nil {
			return e
		}
		out = r
		return nil
	})
	return
}
