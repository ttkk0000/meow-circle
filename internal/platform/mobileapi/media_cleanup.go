package mobileapi

import (
	"errors"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"time"

	"kitty-circle/internal/mobile"
)

var managedUpload = regexp.MustCompile(`^(mnd-[0-9]+-[0-9]+-[a-f0-9]{64}\.(?:jpg|png))$|^\.upload-(mnd-[0-9]+-[0-9]+-[a-f0-9]{64}\.(?:jpg|png))-[A-Za-z0-9]+$`)

// 由有 privacy 权限的外部调度器分批调用，不在 HTTP 服务中隐式启动后台任务。
func (h *Handler) cleanupUploads(r *http.Request, operator string, in mobile.UploadCleanupInput) (out mobile.UploadCleanupResult, err error) {
	if in.Cursor < 0 || len(in.After) > 250 || in.After != "" && filepath.Base(in.After) != in.After {
		return out, mobile.Invalid("invalid cleanup cursor")
	}
	out.NextCursor, out.Expired, err = h.Service.ExpireMediaUploads(r.Context(), operator, in.Cursor)
	if err != nil {
		return
	}
	dir, e := os.Open(h.MediaDir)
	if errors.Is(e, os.ErrNotExist) {
		return out, h.Service.RecordUploadCleanup(r.Context(), operator, out)
	}
	if e != nil {
		return out, e
	}
	defer dir.Close()
	// 有界内存，游标按名称排序；不扫描旧版媒体或应用目录之外的文件。
	var names []string
	more := false
	for {
		if e = r.Context().Err(); e != nil {
			return out, e
		}
		entries, readErr := dir.ReadDir(100)
		if readErr != nil && !errors.Is(readErr, io.EOF) {
			return out, readErr
		}
		for _, entry := range entries {
			name := entry.Name()
			if entry.Type().IsRegular() && name > in.After && managedUpload.MatchString(name) {
				names = append(names, name)
			}
		}
		sort.Strings(names)
		if len(names) > 100 {
			more = true
			names = names[:100]
		}
		if errors.Is(readErr, io.EOF) {
			break
		}
	}
	if more {
		out.NextFile = names[len(names)-1]
	}
	targets := make([]string, len(names))
	for i, name := range names {
		match := managedUpload.FindStringSubmatch(name)
		targets[i] = match[1] + match[2]
	}
	used, e := h.Service.UploadFilesInUse(r.Context(), targets)
	if e != nil {
		return out, e
	}
	for i, name := range names {
		if state := used[targets[i]]; state != "" && (name == targets[i] || state != "ready") {
			continue
		}
		path := filepath.Join(h.MediaDir, name)
		info, e := os.Lstat(path)
		if errors.Is(e, os.ErrNotExist) {
			continue
		}
		if e != nil {
			return out, e
		}
		if !info.Mode().IsRegular() || info.ModTime().After(h.Service.Now().Add(-15*time.Minute)) {
			continue
		}
		if e = os.Remove(path); errors.Is(e, os.ErrNotExist) {
			continue
		} else if e != nil {
			return out, e
		}
		out.Removed++
	}
	return out, h.Service.RecordUploadCleanup(r.Context(), operator, out)
}
