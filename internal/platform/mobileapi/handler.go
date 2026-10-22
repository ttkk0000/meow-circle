// Package mobileapi translates HTTP DTOs into feature commands. It does not
// write repository records or decide participant permissions.
package mobileapi

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net"
	"net/http"
	"strconv"
	"strings"
	"time"

	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/auth"
)

type Handler struct {
	Service            *mobile.Service
	Identity           *identity.Service
	MediaDir, AdminKey string
	limiter            *auth.LoginLimiter
	uploadSlots        chan struct{}
	Operators          []Operator
}

func New(service *mobile.Service, identityService *identity.Service, mediaDir, adminKey string) *Handler {
	return &Handler{Service: service, Identity: identityService, MediaDir: mediaDir, AdminKey: adminKey, limiter: auth.NewLoginLimiter(10, 10*time.Minute, 10*time.Minute), uploadSlots: make(chan struct{}, 2)}
}
func decodeData(data []byte, v any) error { return json.Unmarshal(data, v) }
func payload[T any](r *http.Request) (T, error) {
	var v T
	d := json.NewDecoder(r.Body)
	d.DisallowUnknownFields()
	if e := d.Decode(&v); e != nil {
		return v, mobile.Invalid("invalid JSON payload")
	}
	var extra any
	if d.Decode(&extra) != io.EOF {
		return v, mobile.Invalid("one JSON object required")
	}
	return v, nil
}
func invoke[T any, R any](r *http.Request, fn func(T) (R, error)) (any, error) {
	v, e := payload[T](r)
	if e != nil {
		return nil, e
	}
	return fn(v)
}
func failure(w http.ResponseWriter, e error) {
	status, message := 500, "internal server error"
	var invalid mobile.ValidationError
	var rule mobile.RuleError
	code := "internal_error"
	switch {
	case errors.As(e, &rule):
		status, message, code = 403, rule.Message, rule.Code
		if code == "idempotency_conflict" {
			status = 409
		}
	case errors.As(e, &invalid):
		status, message = 400, invalid.Message
	case errors.Is(e, mobile.ErrUnauthorized):
		status, message = 401, "authentication required"
	case errors.Is(e, mobile.ErrForbidden):
		status, message = 403, "permission denied"
	case errors.Is(e, mobile.ErrNotFound):
		status, message = 404, "not found"
	case errors.Is(e, mobile.ErrConflict):
		status, message = 409, "data changed or action is no longer available; reload before retrying"
	case errors.Is(e, mobile.ErrRateLimited):
		status, message = 429, "too many attempts"
		w.Header().Set("Retry-After", "60")
	case errors.Is(e, mobile.ErrUnavailable):
		status, message = 503, "provider not configured or unavailable"
	case errors.Is(e, context.Canceled):
		status, message = 408, "request cancelled"
	default:
		log.Printf("mobile request failed: %T", e)
	}
	if code == "internal_error" {
		code = map[int]string{400: "invalid_input", 401: "authentication_required", 403: "forbidden", 404: "not_found", 409: "version_conflict", 429: "rate_limited", 503: "service_unavailable", 408: "request_cancelled"}[status]
		if code == "" {
			code = "internal_error"
		}
	}
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]any{"ok": false, "error": message, "error_code": code, "request_id": w.Header().Get("X-Request-ID")})
}
func (h *Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	var requestID [16]byte
	if _, e := rand.Read(requestID[:]); e != nil {
		failure(w, e)
		return
	}
	w.Header().Set("X-Request-ID", hex.EncodeToString(requestID[:]))
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "private, no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	if !strings.HasPrefix(r.URL.Path, "/api/v2/") {
		failure(w, mobile.ErrNotFound)
		return
	}
	path := strings.Split(strings.Trim(strings.TrimPrefix(r.URL.Path, "/api/v2/"), "/"), "/")
	if len(path) == 0 {
		failure(w, mobile.ErrNotFound)
		return
	}
	isUpload := r.Method == http.MethodPost && (len(path) == 1 && path[0] == "media" || len(path) == 2 && path[0] == "safety" && path[1] == "media")
	if !isUpload && r.Body != nil {
		r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	}
	actor := int64(0)
	token := ""
	header := r.Header.Get("Authorization")
	refreshRequest := len(path) == 2 && path[0] == "auth" && path[1] == "refresh"
	if header != "" && !refreshRequest {
		if !strings.HasPrefix(header, "Bearer ") {
			failure(w, mobile.ErrUnauthorized)
			return
		}
		token = strings.TrimPrefix(header, "Bearer ")
		u, e := h.Identity.Authenticate(r.Context(), token)
		if e != nil {
			failure(w, e)
			return
		}
		actor = u.ID
		r = r.WithContext(mobile.WithSession(r.Context(), token))
	}
	var result any
	var e error
	if path[0] == "auth" {
		result, e = h.authentication(r, actor, token, path[1:])
	} else if path[0] == "ops" {
		result, e = h.operations(r, path[1:])
	} else if path[0] == "eligibility" && len(path) == 1 && r.Method == http.MethodGet {
		result, e = h.Service.Eligibility(r.Context(), actor)
	} else if path[0] == "share" && len(path) == 3 && r.Method == http.MethodGet {
		var id int64
		id, e = parseID(path[2])
		if e == nil {
			result, e = h.Service.PublicShare(r.Context(), mobile.Kind(path[1]), id)
		}
	} else if path[0] == "push-devices" && len(path) == 1 && r.Method == http.MethodPost {
		result, e = invoke(r, func(in mobile.PushDeviceInput) (mobile.PushDeviceReceipt, error) {
			return h.Service.RegisterPushDevice(r.Context(), actor, in)
		})
	} else if path[0] == "operations" && len(path) == 2 && r.Method == http.MethodGet {
		result, e = h.Service.Operation(r.Context(), actor, path[1])
	} else if path[0] == "entries" && len(path) == 2 && path[1] == "preview" && r.Method == http.MethodPost {
		result, e = invoke(r, func(in mobile.EntryInput) (mobile.PublicationPreview, error) {
			return h.Service.PreviewPublication(r.Context(), actor, in)
		})
	} else if path[0] == "guardians" {
		result, e = h.guardians(r, actor, path[1:])
	} else if path[0] == "safety" && len(path) == 2 && path[1] == "media" && r.Method == http.MethodPost {
		result, e = h.uploadFor(w, r, 0, r.Header.Get("X-Safety-Token"))
	} else if path[0] == "safety" && len(path) == 4 && path[1] == "media" && path[3] == "content" && (r.Method == http.MethodGet || r.Method == http.MethodHead) {
		var id int64
		id, e = parseID(path[2])
		if e == nil {
			var file mobile.MediaFile
			file, e = h.Service.SafetyMediaFile(r.Context(), r.Header.Get("X-Safety-Token"), id)
			if e == nil {
				e = h.serveMediaFile(w, r, file)
				if e == nil {
					return
				}
			}
		}
	} else if path[0] == "safety" || path[0] == "data-requests" || path[0] == "capability" || (path[0] == "adoptions" && len(path) == 4) {
		result, e = h.policy(r, actor, path)
	} else if len(path) == 1 && path[0] == "expression-catalog" && r.Method == http.MethodGet {
		result = mobile.ExpressionCatalog()
	} else if path[0] == "expression-assets" {
		e = serveExpressionAsset(w, r)
		if e == nil {
			return
		}
	} else if len(path) == 1 && path[0] == "inbox-summary" && r.Method == http.MethodGet {
		result, e = h.Service.Unread(r.Context(), actor)
	} else if len(path) == 1 && path[0] == "profile" && r.Method == http.MethodGet {
		result, e = h.Identity.Profile(r.Context(), actor)
	} else if len(path) == 1 && path[0] == "profile" && r.Method == http.MethodPatch {
		result, e = invoke(r, func(in identity.ProfileInput) (identity.Profile, error) {
			return h.Identity.SaveProfile(r.Context(), actor, in)
		})
	} else if len(path) == 2 && path[0] == "users" && r.Method == http.MethodGet {
		id, parse := strconv.ParseInt(path[1], 10, 64)
		if parse != nil || id <= 0 {
			e = mobile.Invalid("invalid id")
		} else {
			result, e = h.Service.PublicUser(r.Context(), actor, id)
		}
	} else if len(path) == 1 && path[0] == "entry-metrics" && r.Method == http.MethodGet {
		ids := []int64{}
		for _, raw := range strings.Split(r.URL.Query().Get("ids"), ",") {
			id, parse := strconv.ParseInt(raw, 10, 64)
			if parse != nil || id <= 0 {
				e = mobile.Invalid("invalid entry IDs")
				break
			}
			ids = append(ids, id)
		}
		if e == nil {
			result, e = h.Service.Metrics(r.Context(), actor, ids)
		}
	} else if path[0] == "media" && len(path) == 1 && r.Method == http.MethodPost {
		result, e = h.upload(w, r, actor)
	} else if path[0] == "media" && len(path) == 3 && path[2] == "content" {
		id, parse := strconv.ParseInt(path[1], 10, 64)
		if parse != nil || id <= 0 {
			e = mobile.Invalid("invalid id")
		} else {
			e = h.serveMedia(w, r, actor, id)
			if e == nil {
				return
			}
		}
	} else {
		result, e = h.resource(r, actor, path)
	}
	if e != nil {
		failure(w, e)
		return
	}
	_ = json.NewEncoder(w).Encode(map[string]any{"ok": true, "data": result, "request_id": w.Header().Get("X-Request-ID")})
}
func (h *Handler) resource(r *http.Request, actor int64, path []string) (any, error) {
	if len(path) == 2 && path[0] == string(mobile.Friendships) && r.Method == http.MethodPost {
		if actor <= 0 {
			return nil, mobile.ErrUnauthorized
		}
		return invoke(r, func(in mobile.FriendInput) (mobile.Record, error) {
			return h.Service.Friend(r.Context(), actor, path[1], in)
		})
	}
	k := mobile.Kind(path[0])
	id := int64(0)
	if len(path) > 1 {
		var e error
		id, e = strconv.ParseInt(path[1], 10, 64)
		if e != nil || id <= 0 {
			return nil, mobile.Invalid("invalid id")
		}
	}
	if len(path) > 3 {
		return nil, mobile.ErrNotFound
	}
	ctx := r.Context()
	if r.Method == http.MethodGet && len(path) == 3 && path[2] == "verifications" {
		return h.Service.Verifications(ctx, actor, k, id)
	}
	if r.Method == http.MethodGet {
		if len(path) == 1 {
			after, _ := strconv.ParseInt(r.URL.Query().Get("cursor"), 10, 64)
			ref, _ := strconv.ParseInt(r.URL.Query().Get("reference_id"), 10, 64)
			limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))
			values := r.URL.Query()
			lat, _ := strconv.ParseFloat(values.Get("latitude"), 64)
			lon, _ := strconv.ParseFloat(values.Get("longitude"), 64)
			radius, _ := strconv.ParseFloat(values.Get("radius_km"), 64)
			owner, _ := strconv.ParseInt(values.Get("owner_id"), 10, 64)
			return h.Service.ListFiltered(ctx, actor, k, after, ref, limit, mobile.Query{Space: values.Get("space"), State: values.Get("state"), Keyword: values.Get("q"), City: values.Get("city"), Species: values.Get("species"), OwnerID: owner, Latitude: lat, Longitude: lon, RadiusKM: radius})
		}
		if len(path) == 2 {
			return h.Service.Get(ctx, actor, k, id)
		}
		return nil, mobile.ErrNotFound
	}
	if actor <= 0 {
		return nil, mobile.ErrUnauthorized
	}
	if r.Method == http.MethodDelete && len(path) == 2 {
		in, e := payload[mobile.StateInput](r)
		if e != nil {
			return nil, e
		}
		return nil, h.Service.Remove(ctx, actor, k, id, in.Version)
	}
	if len(path) == 1 || len(path) == 2 {
		switch k {
		case mobile.Offers:
			if r.Method == http.MethodPost && id == 0 || r.Method == http.MethodPatch && id > 0 {
				return invoke(r, func(in mobile.OfferInput) (mobile.Record, error) { return h.Service.SaveOffer(ctx, actor, id, in) })
			}
		case mobile.Pets:
			if r.Method == http.MethodPost && id == 0 || r.Method == http.MethodPatch && id > 0 {
				return invoke(r, func(in mobile.PetInput) (mobile.Record, error) { return h.Service.SavePet(ctx, actor, id, in) })
			}
		case mobile.Entries:
			if r.Method == http.MethodPost && id == 0 || r.Method == http.MethodPatch && id > 0 {
				return invoke(r, func(in mobile.EntryInput) (mobile.Record, error) { return h.Service.SaveEntry(ctx, actor, id, in) })
			}
		case mobile.Cases:
			if r.Method == http.MethodPost && id == 0 || r.Method == http.MethodPatch && id > 0 {
				return invoke(r, func(in mobile.CaseInput) (mobile.Record, error) { return h.Service.SaveCase(ctx, actor, id, in) })
			}
		case mobile.Groups:
			if r.Method == http.MethodPost && id == 0 {
				return invoke(r, func(in mobile.GroupInput) (mobile.Record, error) { return h.Service.CreateGroup(ctx, actor, in) })
			}
		case mobile.Conversations:
			if r.Method == http.MethodPost && id == 0 {
				return invoke(r, func(in mobile.ConversationInput) (mobile.Record, error) {
					return h.Service.Conversation(ctx, actor, in)
				})
			}
		case mobile.Invitations:
			if r.Method == http.MethodPost && id == 0 {
				return invoke(r, func(in mobile.InviteInput) (mobile.Record, error) { return h.Service.Invite(ctx, actor, in) })
			}
		case mobile.Adoptions:
			if r.Method == http.MethodPost && id == 0 {
				return invoke(r, func(in mobile.AdoptionInput) (mobile.Record, error) { return h.Service.ApplyAdoption(ctx, actor, in) })
			}
		case mobile.Appointments:
			if r.Method == http.MethodPost && id == 0 {
				return invoke(r, func(in mobile.AppointmentInput) (mobile.Record, error) {
					return h.Service.CreateAppointment(ctx, actor, in)
				})
			}
		case mobile.Reviews:
			if r.Method == http.MethodPost && id == 0 {
				return invoke(r, func(in mobile.ReviewInput) (mobile.Record, error) { return h.Service.SubmitReview(ctx, actor, in) })
			}
		case mobile.VisitFeedback:
			if r.Method == http.MethodPost && id == 0 {
				return invoke(r, func(in mobile.Feedback) (mobile.Record, error) { return h.Service.Feedback(ctx, actor, in) })
			}
		case mobile.Subscriptions:
			if r.Method == http.MethodPut && id == 0 {
				return invoke(r, func(in mobile.SubscriptionInput) (mobile.Record, error) { return h.Service.Subscribe(ctx, actor, in) })
			}
		case mobile.Preferences, mobile.Expressions, mobile.Drafts:
			if r.Method == http.MethodPut && id == 0 {
				return invoke(r, func(in mobile.SettingsInput) (mobile.Record, error) { return h.Service.Settings(ctx, actor, k, in) })
			}
		}
	}
	if r.Method != http.MethodPost {
		return nil, mobile.Invalid("method is not supported for this resource")
	}
	if len(path) != 3 {
		return nil, mobile.ErrNotFound
	}
	action := path[2]
	switch k {
	case mobile.Pets:
		if action == "introduction" {
			return invoke(r, func(in mobile.IntroductionInput) (mobile.Record, error) {
				return h.Service.Introduction(ctx, actor, id, in)
			})
		}
	case mobile.Entries:
		if action == "comments" {
			return invoke(r, func(in mobile.CommentInput) (mobile.Record, error) { return h.Service.Comment(ctx, actor, id, in) })
		}
		if action == "reaction" {
			return invoke(r, func(in struct {
				Liked bool `json:"liked"`
			}) (mobile.Record, error) {
				return h.Service.React(ctx, actor, id, in.Liked)
			})
		}
	case mobile.Cases:
		switch action {
		case "claims":
			return invoke(r, func(in mobile.ClaimInput) (mobile.Record, error) { return h.Service.Claim(ctx, actor, id, in) })
		case "leads":
			return invoke(r, func(in struct {
				Text     string  `json:"text"`
				MediaIDs []int64 `json:"media_ids"`
			}) (mobile.Record, error) {
				return h.Service.Lead(ctx, actor, id, in.Text, in.MediaIDs)
			})
		case "close":
			return invoke(r, func(in mobile.StateInput) (mobile.Record, error) { return h.Service.CloseCase(ctx, actor, id, in) })
		}
	case mobile.Claims:
		return invoke(r, func(in mobile.StateInput) (mobile.Record, error) {
			return h.Service.ClaimAction(ctx, actor, id, action, in)
		})
	case mobile.Adoptions:
		if action == "care_updates" {
			return invoke(r, func(in mobile.CareInput) (mobile.Record, error) { return h.Service.Care(ctx, actor, id, in) })
		}
		return invoke(r, func(in mobile.AdoptionAction) (mobile.Record, error) {
			return h.Service.Adoption(ctx, actor, id, action, in)
		})
	case mobile.Appointments:
		return invoke(r, func(in mobile.AppointmentAction) (mobile.Record, error) {
			return h.Service.Appointment(ctx, actor, id, action, in)
		})
	case mobile.Groups:
		if action == "read" {
			return invoke(r, func(in struct {
				Through int64 `json:"through_message_id"`
			}) (mobile.Record, error) {
				return h.Service.ReadMessages(ctx, actor, id, true, in.Through)
			})
		}
		if action == "messages" {
			return invoke(r, func(in mobile.MessageInput) (mobile.Record, error) {
				return h.Service.Message(ctx, actor, id, true, in)
			})
		}
		return invoke(r, func(in mobile.GroupAction) (mobile.Record, error) { return h.Service.Group(ctx, actor, id, action, in) })
	case mobile.Conversations:
		if action == "read" {
			return invoke(r, func(in struct {
				Through int64 `json:"through_message_id"`
			}) (mobile.Record, error) {
				return h.Service.ReadMessages(ctx, actor, id, false, in.Through)
			})
		}
		if action == "messages" {
			return invoke(r, func(in mobile.MessageInput) (mobile.Record, error) {
				return h.Service.Message(ctx, actor, id, false, in)
			})
		}
		return invoke(r, func(in mobile.StateInput) (mobile.Record, error) {
			return h.Service.ConversationAction(ctx, actor, id, in.Version, action)
		})
	case mobile.Invitations:
		return invoke(r, func(in mobile.StateInput) (mobile.Record, error) {
			return h.Service.Invitation(ctx, actor, id, in.Version, action)
		})
	case mobile.Notifications:
		if action == "read" {
			return invoke(r, func(in mobile.StateInput) (mobile.Record, error) {
				return h.Service.ReadNotification(ctx, actor, id, in.Version)
			})
		}
	case mobile.Reviews:
		if action == "supplement" {
			return invoke(r, func(in struct {
				Version  int64   `json:"version"`
				Note     string  `json:"note"`
				MediaIDs []int64 `json:"media_ids"`
			}) (mobile.Record, error) {
				return h.Service.SupplementReview(ctx, actor, id, mobile.StateInput{Version: in.Version, Note: in.Note}, in.MediaIDs)
			})
		}
	}
	return nil, mobile.ErrNotFound
}
func (h *Handler) authentication(r *http.Request, actor int64, token string, path []string) (any, error) {
	if len(path) == 1 && path[0] == "capabilities" && r.Method == http.MethodGet {
		return h.Identity.Capabilities(), nil
	}
	if len(path) == 1 && path[0] == "methods" && r.Method == http.MethodGet {
		return h.Identity.Methods(r.Context(), actor)
	}
	if r.Method != http.MethodPost {
		return nil, mobile.Invalid("authentication requires POST")
	}
	ctx := r.Context()
	ip, _, _ := net.SplitHostPort(r.RemoteAddr)
	if ip == "" {
		ip = r.RemoteAddr
	}
	if ok, _ := h.limiter.Allow(ip); !ok {
		return nil, mobile.ErrRateLimited
	}
	h.limiter.RecordFailure(ip) // bounds all expensive auth calls, including successful bursts
	if len(path) == 2 && path[0] == "account" {
		in, e := payload[identity.AccountInput](r)
		if e != nil {
			return nil, e
		}
		return nil, h.Identity.Account(ctx, actor, path[1], in)
	}
	if len(path) == 3 && path[0] == "account" && path[2] == "scope" {
		return invoke(r, func(in identity.AccountInput) (mobile.PurposeScope, error) {
			return h.Identity.AccountScope(ctx, actor, path[1], in)
		})
	}
	if len(path) != 1 {
		return nil, mobile.ErrNotFound
	}
	switch path[0] {
	case "oauth-link-scope":
		return invoke(r, func(in identity.ResolveInput) (mobile.PurposeScope, error) {
			return h.Identity.LinkScope(ctx, actor, in.Token)
		})
	case "email-otp":
		return invoke(r, func(in identity.EmailOTPInput) (identity.OTPResult, error) {
			return h.Identity.IssueEmailOTP(ctx, actor, ip, in)
		})
	case "email", "register":
		return invoke(r, func(in identity.EmailInput) (identity.LoginResult, error) {
			return h.Identity.EmailLogin(ctx, in, path[0] == "register")
		})
	case "email-recover":
		in, e := payload[identity.EmailInput](r)
		if e != nil {
			return nil, e
		}
		return nil, h.Identity.RecoverEmail(ctx, in)
	case "verify-phone":
		in, e := payload[struct {
			Code string `json:"code"`
		}](r)
		if e != nil {
			return nil, e
		}
		return nil, h.Identity.VerifyPhone(ctx, actor, in.Code)
	case "otp":
		return invoke(r, func(in identity.OTPInput) (identity.OTPResult, error) { return h.Identity.IssueOTP(ctx, actor, ip, in) })
	case "phone":
		return invoke(r, func(in identity.PhoneLogin) (identity.LoginResult, error) { return h.Identity.PhoneLogin(ctx, in) })
	case "password":
		return invoke(r, func(in identity.PasswordInput) (identity.LoginResult, error) { return h.Identity.Password(ctx, in) })
	case "refresh":
		return invoke(r, func(in struct {
			AccessToken  string `json:"access_token"`
			RefreshToken string `json:"refresh_token"`
			Device       string `json:"device"`
		}) (identity.LoginResult, error) {
			return h.Identity.Refresh(ctx, in.AccessToken, in.RefreshToken, in.Device)
		})
	case "recover":
		in, e := payload[struct {
			Phone    string `json:"phone"`
			Code     string `json:"code"`
			Password string `json:"password"`
		}](r)
		if e != nil {
			return nil, e
		}
		return nil, h.Identity.Recover(ctx, in.Phone, in.Code, in.Password)
	case "reauth":
		in, e := payload[identity.ReauthInput](r)
		if e != nil {
			return nil, e
		}
		grant, e := h.Identity.Reauth(ctx, actor, in)
		return map[string]string{"reauth_token": grant}, e
	case "oauth-start":
		return invoke(r, func(in identity.OAuthStart) (identity.OAuthChallenge, error) { return h.Identity.Start(ctx, actor, in) })
	case "oauth-finish":
		return invoke(r, func(in identity.OAuthFinish) (identity.OAuthResult, error) { return h.Identity.Finish(ctx, actor, in) })
	case "oauth-resolve":
		return invoke(r, func(in identity.ResolveInput) (identity.LoginResult, error) {
			return h.Identity.Resolve(ctx, actor, in)
		})
	case "logout":
		in, e := payload[struct {
			All bool `json:"all"`
		}](r)
		if e != nil {
			return nil, e
		}
		return nil, h.Identity.Logout(ctx, actor, token, in.All)
	}
	return nil, mobile.ErrNotFound
}
func (h *Handler) operations(r *http.Request, path []string) (any, error) {
	op, allowed := h.operator(r, operatorRole(path))
	if !allowed {
		return nil, mobile.ErrForbidden
	}
	r = r.WithContext(mobile.WithOperator(r.Context(), op.ID))
	if len(path) == 1 && path[0] == "upload-expiry" && r.Method == http.MethodPost {
		return invoke(r, func(in mobile.UploadCleanupInput) (mobile.UploadCleanupResult, error) {
			return h.cleanupUploads(r, op.ID, in)
		})
	}
	if len(path) == 1 && (path[0] == "privacy-expiry" || path[0] == "challenge-expiry" || path[0] == "token-expiry") && r.Method == http.MethodPost {
		in, e := payload[struct {
			Cursor int64 `json:"cursor"`
		}](r)
		if e != nil {
			return nil, e
		}
		var next int64
		var count int
		if path[0] == "privacy-expiry" {
			next, count, e = h.Service.PurgeExpiredSafety(r.Context(), op.ID, in.Cursor)
		} else if path[0] == "token-expiry" {
			next, count, e = h.Identity.PurgeExpiredTokens(r.Context(), in.Cursor)
		} else {
			next, count, e = h.Identity.PurgeExpiredChallenges(r.Context(), in.Cursor)
		}
		return map[string]any{"next_cursor": next, "processed": count}, e
	}
	if len(path) == 1 && path[0] == "revenue" && r.Method == http.MethodGet {
		after, _ := strconv.ParseInt(r.URL.Query().Get("cursor"), 10, 64)
		return h.Service.RevenuePage(r.Context(), op.ID, after)
	}
	if len(path) == 1 && path[0] == "revenue" && r.Method == http.MethodPost {
		return invoke(r, func(in mobile.RevenueInput) (mobile.Record, error) {
			return h.Service.RecordRevenue(r.Context(), op.ID, in)
		})
	}
	if len(path) == 3 && path[0] == "push" && path[2] == "dispatch" && r.Method == http.MethodPost {
		if !op.permitsConsumer("push") {
			return nil, mobile.ErrForbidden
		}
		id, e := parseID(path[1])
		if e != nil {
			return nil, e
		}
		return nil, h.Service.DispatchPush(r.Context(), op.ID, id)
	}
	if len(path) == 1 && path[0] == "legacy-erasure" && r.Method == http.MethodPost {
		in, e := payload[struct {
			Cursor int64 `json:"cursor"`
		}](r)
		if e != nil {
			return nil, e
		}
		next, count, e := h.Service.AdaptLegacyErasure(r.Context(), op.ID, in.Cursor)
		return map[string]any{"next_cursor": next, "adapted": count}, e
	}
	if len(path) == 3 && path[0] == "media-erasure" && path[2] == "process" && r.Method == http.MethodPost {
		id, e := parseID(path[1])
		if e != nil {
			return nil, e
		}
		return nil, h.eraseMedia(r, op.ID, id)
	}
	if len(path) == 3 && r.Method == http.MethodPost {
		id, e := strconv.ParseInt(path[1], 10, 64)
		if e != nil || id <= 0 {
			return nil, mobile.Invalid("invalid id")
		}
		if path[0] == "data-requests" {
			switch path[2] {
			case "process":
				return invoke(r, func(in mobile.StateInput) (mobile.Record, error) {
					return h.Service.ProcessDataRequest(r.Context(), op.ID, id, in.Version)
				})
			case "external":
				in, e := payload[mobile.ExternalErasureInput](r)
				if e != nil {
					return nil, e
				}
				return nil, h.Service.RecordExternalErasure(r.Context(), op.ID, id, in)
			case "hold":
				in, e := payload[mobile.DataHoldInput](r)
				if e != nil {
					return nil, e
				}
				return nil, h.Service.SetDataHold(r.Context(), op.ID, id, in)
			}
		}
		if path[0] == "rights-cases" && path[2] == "decision" {
			return invoke(r, func(in mobile.RightsDecision) (mobile.RightsReceipt, error) {
				return h.Service.DecideRightsCase(r.Context(), op.ID, id, in)
			})
		}
	}
	if len(path) == 1 && r.Method == http.MethodPost {
		switch path[0] {
		case "age-evidence":
			in, e := payload[mobile.AgeEvidenceInput](r)
			if e != nil {
				return nil, e
			}
			return nil, h.Service.RecordAgeEvidence(r.Context(), op.ID, in)
		case "guardian-evidence":
			return invoke(r, func(in mobile.GuardianEvidenceInput) (mobile.Record, error) {
				return h.Service.RecordGuardianEvidence(r.Context(), op.ID, in)
			})
		case "authority":
			return invoke(r, func(in mobile.AuthorityInput) (mobile.Record, error) {
				return h.Service.RecordAuthority(r.Context(), op.ID, in)
			})
		case "deliveries":
			in, e := payload[mobile.DeliveryClaim](r)
			if e != nil {
				return nil, e
			}
			if !op.permitsConsumer(in.Consumer) {
				return nil, mobile.ErrForbidden
			}
			return h.Service.ClaimDeliveries(r.Context(), op.ID, in)
		}
	}
	if len(path) == 3 && path[0] == "deliveries" && path[2] == "result" && r.Method == http.MethodPost {
		id, e := strconv.ParseInt(path[1], 10, 64)
		if e != nil {
			return nil, mobile.Invalid("invalid id")
		}
		in, e := payload[mobile.DeliveryResult](r)
		if e != nil {
			return nil, e
		}
		if !op.permitsConsumer(in.Consumer) {
			return nil, mobile.ErrForbidden
		}
		return nil, h.Service.CompleteDelivery(r.Context(), op.ID, id, in)
	}
	if len(path) == 1 && path[0] == "outbox" && r.Method == http.MethodGet {
		cursor, _ := strconv.ParseInt(r.URL.Query().Get("cursor"), 10, 64)
		return h.Service.PendingEvents(r.Context(), cursor)
	}
	if len(path) == 3 && path[0] == "outbox" && path[2] == "ack" && r.Method == http.MethodPost {
		return nil, mobile.Denied("use_per_consumer_delivery")
	}
	if len(path) == 1 && path[0] == "nearby-alerts" && r.Method == http.MethodPost {
		return invoke(r, func(in struct {
			CaseID int64 `json:"case_id"`
			Cursor int64 `json:"cursor"`
		}) (mobile.AlertBatch, error) {
			return h.Service.NearbyAlerts(r.Context(), in.CaseID, in.Cursor)
		})
	}
	if len(path) == 1 && path[0] == "provider-revocations" && r.Method == http.MethodPost {
		if !op.permitsConsumer("provider_revocations") {
			return nil, mobile.ErrForbidden
		}
		in, e := payload[struct {
			Cursor int64 `json:"cursor"`
		}](r)
		if e != nil {
			return nil, e
		}
		next, count, e := h.Identity.ProcessRevocations(r.Context(), in.Cursor)
		return map[string]any{"next_cursor": next, "processed": count}, e
	}
	if len(path) == 2 && path[0] == "claim-disputes" && r.Method == http.MethodPost {
		id, e := strconv.ParseInt(path[1], 10, 64)
		if e != nil {
			return nil, mobile.Invalid("invalid id")
		}
		return invoke(r, func(in mobile.StateInput) (mobile.Record, error) {
			return h.Service.ResolveClaimDispute(r.Context(), id, in)
		})
	}
	if len(path) == 1 && path[0] == "reviews" && r.Method == http.MethodGet {
		cursor, _ := strconv.ParseInt(r.URL.Query().Get("cursor"), 10, 64)
		return h.Service.StaffReviews(r.Context(), cursor)
	}
	if len(path) == 2 && path[0] == "reviews" && r.Method == http.MethodPost {
		id, e := strconv.ParseInt(path[1], 10, 64)
		if e != nil {
			return nil, mobile.Invalid("invalid id")
		}
		return invoke(r, func(in mobile.ReviewDecision) (mobile.Record, error) {
			return h.Service.DecideReview(r.Context(), id, in)
		})
	}
	return nil, mobile.ErrNotFound
}
