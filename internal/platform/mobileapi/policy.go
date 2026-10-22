package mobileapi

import (
	"kitty-circle/internal/identity"
	"net"
	"net/http"
	"strconv"

	"kitty-circle/internal/mobile"
)

func (h *Handler) policy(r *http.Request, actor int64, path []string) (any, error) {
	ctx := r.Context()
	if len(path) == 0 {
		return nil, mobile.ErrNotFound
	}
	if path[0] == "capability" && len(path) == 1 && r.Method == http.MethodPost {
		if actor <= 0 {
			return nil, mobile.ErrUnauthorized
		}
		token, e := h.Service.NewDataCapability(ctx, actor)
		return map[string]string{"token": token}, e
	}
	if path[0] == "data-requests" {
		if len(path) == 3 && path[1] == "requests" && r.Method == http.MethodGet {
			return h.Service.DataRequestByKey(ctx, path[2], r.Header.Get("X-Request-Access"))
		}
		if len(path) == 3 && path[2] == "revoke-access" && r.Method == http.MethodPost {
			id, e := parseID(path[1])
			if e != nil {
				return nil, e
			}
			return nil, h.Service.RevokeDataRequestAccess(ctx, id, r.Header.Get("X-Request-Access"))
		}
		if len(path) == 1 && r.Method == http.MethodPost {
			return invoke(r, func(in mobile.DataRequestInput) (mobile.Record, error) {
				return h.Service.SubmitDataRequest(ctx, actor, in)
			})
		}
		if len(path) == 2 && path[1] == "scope" && r.Method == http.MethodPost {
			if actor <= 0 {
				return nil, mobile.ErrUnauthorized
			}
			return invoke(r, func(in mobile.DataRequestInput) (mobile.PurposeScope, error) { return mobile.DataScope(actor, in) })
		}
		if len(path) == 2 && r.Method == http.MethodGet {
			id, e := strconv.ParseInt(path[1], 10, 64)
			if e != nil {
				return nil, mobile.Invalid("invalid id")
			}
			return h.Service.DataRequestStatus(ctx, id, r.Header.Get("X-Request-Access"))
		}
	}
	if path[0] == "safety" {
		if len(path) == 2 && path[1] == "session" && r.Method == http.MethodPost {
			ip, _, _ := net.SplitHostPort(r.RemoteAddr)
			if ip == "" {
				ip = r.RemoteAddr
			}
			if ok, _ := h.limiter.Allow("safety:" + ip); !ok {
				return nil, mobile.ErrRateLimited
			}
			h.limiter.RecordFailure("safety:" + ip)
			return h.Service.NewSafetySession(ctx)
		}
		token := r.Header.Get("X-Safety-Token")
		if len(path) == 3 && path[1] == "requests" && r.Method == http.MethodGet {
			return h.Service.RightsByKey(ctx, token, path[2])
		}
		if len(path) == 2 && path[1] == "contact-otp" && r.Method == http.MethodPost {
			in, e := payload[identity.EmailOTPInput](r)
			if e != nil {
				return nil, e
			}
			in.Purpose, in.SafetyToken = "rights_contact", token
			return h.Identity.IssueEmailOTP(ctx, 0, r.RemoteAddr, in)
		}
		if len(path) == 2 && path[1] == "contact-verify" && r.Method == http.MethodPost {
			in, e := payload[identity.EmailInput](r)
			if e != nil {
				return nil, e
			}
			return nil, h.Identity.VerifySafetyContact(ctx, token, in)
		}
		if len(path) == 2 && path[1] == "tickets" && r.Method == http.MethodPost {
			return invoke(r, func(in mobile.RightsInput) (mobile.RightsReceipt, error) {
				return h.Service.SubmitRightsCase(ctx, actor, token, in)
			})
		}
		if len(path) == 3 && path[1] == "tickets" && r.Method == http.MethodGet {
			id, e := strconv.ParseInt(path[2], 10, 64)
			if e != nil {
				return nil, mobile.Invalid("invalid id")
			}
			return h.Service.RightsStatus(ctx, token, id)
		}
		if len(path) == 2 && path[1] == "scope" && r.Method == http.MethodPost {
			if actor <= 0 {
				return nil, mobile.ErrUnauthorized
			}
			return invoke(r, func(in mobile.RightsInput) (mobile.PurposeScope, error) { return mobile.RightsScope(actor, in) })
		}
	}
	if path[0] == "adoptions" && len(path) == 4 && r.Method == http.MethodPost {
		id, e := strconv.ParseInt(path[1], 10, 64)
		if e != nil || id <= 0 {
			return nil, mobile.Invalid("invalid id")
		}
		if path[2] == "contracts" {
			if path[3] == "scope" {
				return invoke(r, func(in mobile.ContractInput) (mobile.PurposeScope, error) {
					return h.Service.ContractScope(ctx, actor, id, in.Version, in.Revision)
				})
			}
			return invoke(r, func(in mobile.ContractInput) (mobile.Record, error) {
				return h.Service.Contract(ctx, actor, id, path[3], in)
			})
		}
		if path[2] == "returns" {
			return invoke(r, func(in mobile.ReturnInput) (mobile.Record, error) {
				return h.Service.Return(ctx, actor, id, path[3], in)
			})
		}
	}
	return nil, mobile.ErrNotFound
}
