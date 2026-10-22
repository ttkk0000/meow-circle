package mobileapi

import (
	"errors"
	"kitty-circle/internal/mobile"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
)

func (h *Handler) eraseMedia(r *http.Request, operator string, id int64) error {
	work, e := h.Service.PrepareMediaErasure(r.Context(), operator, id)
	if e != nil {
		return e
	}
	if work.Filename == "" || filepath.Base(work.Filename) != work.Filename {
		return mobile.Invalid("invalid erasure target")
	}
	if work.DeletePhysical {
		root, e := filepath.Abs(h.MediaDir)
		if e != nil {
			return e
		}
		target := filepath.Join(root, work.Filename)
		relative, e := filepath.Rel(root, target)
		if e != nil || relative != work.Filename {
			return mobile.ErrForbidden
		}
		if e = os.Remove(target); e != nil && !errors.Is(e, os.ErrNotExist) {
			return e
		}
	}
	return h.Service.FinishMediaErasure(r.Context(), operator, work)
}
func parseID(raw string) (int64, error) {
	id, e := strconv.ParseInt(raw, 10, 64)
	if e != nil || id <= 0 {
		return 0, mobile.Invalid("invalid id")
	}
	return id, nil
}
