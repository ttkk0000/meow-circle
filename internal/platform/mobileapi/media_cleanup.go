package mobileapi

import (
	"kitty-circle/internal/mediablob"
	"kitty-circle/internal/mobile"
	"net/http"
	"path/filepath"
	"time"
)

var managedUpload = mediablob.Managed

// 由有 privacy 权限的外部调度器分批调用，不在 HTTP 服务中隐式启动后台任务。
func (h *Handler) cleanupUploads(r *http.Request, operator string, in mobile.UploadCleanupInput) (out mobile.UploadCleanupResult, err error) {
	if in.Cursor < 0 || len(in.After) > 250 || in.After != "" && filepath.Base(in.After) != in.After {
		return out, mobile.Invalid("invalid cleanup cursor")
	}
	out.NextCursor, out.Expired, err = h.Service.ExpireMediaUploads(r.Context(), operator, in.Cursor)
	if err != nil {
		return
	}
	objects, next, e := h.blobs().List(r.Context(), in.After)
	if e != nil {
		return out, e
	}
	out.NextFile = next
	names := make([]string, len(objects))
	for i, object := range objects {
		names[i] = object.Name
	}
	targets := make([]string, len(names))
	for i, name := range names {
		match := managedUpload.FindStringSubmatch(name)
		if len(match) != 3 {
			return out, mobile.ErrUnavailable
		}
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
		if objects[i].Modified.After(h.Service.Now().Add(-15 * time.Minute)) {
			continue
		}
		if e = h.blobs().Remove(r.Context(), name); e != nil {
			return out, e
		}
		out.Removed++
	}
	return out, h.Service.RecordUploadCleanup(r.Context(), operator, out)
}
