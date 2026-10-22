package mobileapi

import (
	"kitty-circle/internal/mobile"
	"net/http"
	"strconv"
)

func (h *Handler) guardians(r *http.Request, actor int64, path []string) (any, error) {
	if len(path) == 1 && r.Method == http.MethodGet {
		id, e := parseID(path[0])
		if e != nil {
			return nil, e
		}
		rows, e := h.Service.Guardians(r.Context(), actor)
		if e != nil {
			return nil, e
		}
		for _, row := range rows {
			if row.ID == id {
				return row, nil
			}
		}
		return nil, mobile.ErrNotFound
	}
	if len(path) == 0 && r.Method == http.MethodGet {
		return h.Service.Guardians(r.Context(), actor)
	}
	if len(path) != 2 || r.Method != http.MethodPost {
		return nil, mobile.ErrNotFound
	}
	id, e := strconv.ParseInt(path[0], 10, 64)
	if e != nil || id <= 0 {
		return nil, mobile.Invalid("invalid id")
	}
	if path[1] == "decision" {
		return invoke(r, func(in mobile.GuardianDecision) (mobile.Record, error) {
			return h.Service.Guardian(r.Context(), actor, id, in)
		})
	}
	if path[1] == "scope" {
		rows, e := h.Service.Guardians(r.Context(), actor)
		if e != nil {
			return nil, e
		}
		for _, row := range rows {
			if row.ID == id && row.OwnerID == actor {
				body, e := mobile.Decode[mobile.GuardianConsent](row)
				if e != nil {
					return nil, e
				}
				return mobile.GuardianScope(row.ID, row.Version, body.PolicyVersion)
			}
		}
		return nil, mobile.ErrForbidden
	}
	return nil, mobile.ErrNotFound
}
