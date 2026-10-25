package mobile

import (
	"context"
	"errors"
	"strconv"
	"strings"
	"time"
)

type UploadCleanupInput struct {
	Cursor int64  `json:"cursor"`
	After  string `json:"after"`
}

type UploadCleanupResult struct {
	NextCursor int64  `json:"next_cursor"`
	Expired    int    `json:"expired"`
	NextFile   string `json:"next_file"`
	Removed    int    `json:"removed"`
}

// 保全不因上传租约过期而自动释放；期限复核和解除由原业务处理。
func uploadHold(tx Tx, r Record, now time.Time) (bool, error) {
	held, _, err := mediaRetention(tx, r, now)
	return held, err
}

func (s *Service) ExpireMediaUploads(ctx context.Context, operator string, after int64) (next int64, count int, err error) {
	if operator == "" || after < 0 {
		return 0, 0, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		rows, e := tx.List(Media, Filter{After: after, State: "uploading", Limit: 100})
		if e != nil {
			return e
		}
		for _, r := range rows {
			next = r.ID
			file, e := Decode[MediaFile](r)
			if e != nil {
				return e
			}
			if file.UploadLeaseUntil != nil && file.UploadLeaseUntil.After(s.Now()) {
				continue
			}
			held, e := uploadHold(tx, r, s.Now())
			if e != nil {
				return e
			}
			if held {
				continue
			}
			r.State = "upload_expired"
			file.UploadLeaseUntil = nil
			if e = SetBody(&r, file); e != nil {
				return e
			}
			if e = tx.Save(Media, &r); e != nil {
				return e
			}
			if e = audit(tx, operator, "media.upload_expired", Media, r.ID); e != nil {
				return e
			}
			count++
		}
		if len(rows) < 100 {
			next = 0
		}
		return nil
	})
	return
}

// 物理扫描只删除本批命名规范的无引用文件。租约重试不会再次使用旧文件名。
func (s *Service) UploadFilesInUse(ctx context.Context, names []string) (used map[string]string, err error) {
	if len(names) > 100 {
		return nil, Invalid("cleanup batch exceeds 100 files")
	}
	wanted := map[int64][]string{}
	for _, name := range names {
		parts := strings.SplitN(name, "-", 4)
		if len(parts) != 4 || parts[0] != "mnd" {
			return nil, Invalid("invalid cleanup target")
		}
		id, e := strconv.ParseInt(parts[1], 10, 64)
		if e != nil || id <= 0 {
			return nil, Invalid("invalid cleanup target")
		}
		wanted[id] = append(wanted[id], name)
	}
	used = map[string]string{}
	err = s.Repo.View(ctx, func(tx Tx) error {
		// 文件名已包含不可变的媒体ID；只读本批目标，不扫描整张媒体表。
		for id, matches := range wanted {
			r, e := tx.Get(Media, id)
			if errors.Is(e, ErrNotFound) {
				continue
			}
			if e != nil {
				return e
			}
			if r.State == "erased" {
				continue
			}
			file, e := Decode[MediaFile](r)
			if e != nil {
				return e
			}
			held, e := uploadHold(tx, r, s.Now())
			if e != nil {
				return e
			}
			active := r.State != "upload_expired" && r.State != "uploading" ||
				r.State == "uploading" && file.UploadLeaseUntil != nil && file.UploadLeaseUntil.After(s.Now())
			for _, name := range matches {
				if held {
					used[name] = "held"
				} else if active && name == file.Filename {
					used[name] = r.State
				}
			}
		}
		return nil
	})
	return
}

func (s *Service) RecordUploadCleanup(ctx context.Context, operator string, result UploadCleanupResult) error {
	if operator == "" {
		return ErrForbidden
	}
	return s.Repo.Update(ctx, func(tx Tx) error {
		r := Record{State: "recorded"}
		if e := SetBody(&r, struct {
			Operator string              `json:"operator"`
			Action   string              `json:"action"`
			Result   UploadCleanupResult `json:"result"`
		}{operator, "media.upload_sweep", result}); e != nil {
			return e
		}
		return tx.Save(AuditEntries, &r)
	})
}
