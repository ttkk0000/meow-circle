package mobileapi

import (
	"kitty-circle/internal/mobile"
	"net/http"
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
		if e = h.blobs().Remove(r.Context(), work.Filename); e != nil {
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
