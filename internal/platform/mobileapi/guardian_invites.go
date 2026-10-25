package mobileapi

import (
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"net/http"
	"strconv"
	"time"
)

// 绑定实际已发布的儿童专门规则。草案或缺失配置不开放发起及领取。
func (h *Handler) guardianInviteInfo() identity.GuardianInviteInfo {
	info := identity.GuardianInviteInfo{}
	catalog, err := loadReadingCatalog()
	if err != nil {
		return info
	}
	latest := time.Time{}
	for _, doc := range catalog.Documents {
		if doc.Type != "teen" || doc.Status != "published" {
			continue
		}
		from, e := time.Parse(time.RFC3339, doc.EffectiveFrom)
		if e != nil || from.After(time.Now()) || from.Before(latest) {
			continue
		}
		if doc.EffectiveUntil != "" {
			until, e := time.Parse(time.RFC3339, doc.EffectiveUntil)
			if e != nil || !until.After(time.Now()) {
				continue
			}
		}
		latest = from
		info.PolicyVersion = doc.SHA256
		info.PolicyLabel = doc.Revision
		info.PolicyKey = doc.Key
	}
	return h.Identity.GuardianInviteCapability(info)
}
func (h *Handler) guardianInvitations(r *http.Request, actor int64, path []string) (any, error) {
	if actor <= 0 {
		return nil, mobile.ErrUnauthorized
	}
	info := h.guardianInviteInfo()
	if len(path) == 1 && path[0] == "config" && r.Method == http.MethodGet {
		return info, nil
	}
	if len(path) == 0 && r.Method == http.MethodGet {
		cursor := int64(0)
		if raw := r.URL.Query().Get("cursor"); raw != "" {
			var e error
			cursor, e = strconv.ParseInt(raw, 10, 64)
			if e != nil || cursor < 0 {
				return nil, mobile.Invalid("invalid guardian invitation cursor")
			}
		}
		return h.Identity.GuardianInvites(r.Context(), actor, cursor, info)
	}
	if len(path) == 1 && path[0] != "claim" && r.Method == http.MethodGet {
		id, e := parseID(path[0])
		if e != nil {
			return nil, e
		}
		return h.Identity.GuardianInvite(r.Context(), actor, id, info)
	}
	if r.Method != http.MethodPost {
		return nil, mobile.ErrNotFound
	}
	limitKey := "guardian:" + strconv.FormatInt(actor, 10)
	if ok, _ := h.limiter.Allow(limitKey); !ok {
		return nil, mobile.ErrRateLimited
	}
	h.limiter.RecordFailure(limitKey)
	if len(path) == 0 {
		return invoke(r, func(in identity.GuardianInviteStart) (identity.GuardianInviteStatus, error) {
			return h.Identity.CreateGuardianInvite(r.Context(), actor, info, in)
		})
	}
	if len(path) == 1 && path[0] == "claim" {
		return invoke(r, func(in identity.GuardianInviteClaim) (identity.GuardianInviteStatus, error) {
			return h.Identity.ClaimGuardianInvite(r.Context(), actor, info, in)
		})
	}
	if len(path) == 2 && path[1] == "cancel" {
		id, e := parseID(path[0])
		if e != nil {
			return nil, e
		}
		return invoke(r, func(in mobile.StateInput) (identity.GuardianInviteStatus, error) {
			return h.Identity.CancelGuardianInvite(r.Context(), actor, id, in, info)
		})
	}
	return nil, mobile.ErrNotFound
}
