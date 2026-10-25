// Package opsapi 提供独立技术运维入口；具名凭据与环境范围由部署方配置。
package opsapi

import (
	"bytes"
	"context"
	"crypto/rand"
	"crypto/subtle"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"kitty-circle/internal/operations"
	"kitty-circle/internal/platform/mobileapi"
)

type Handler struct {
	Service   *operations.Service
	Operators []mobileapi.Operator
	Origin    string
}
type Capabilities struct {
	Read          bool `json:"read"`
	IncidentWrite bool `json:"incident_write"`
	ReleaseWrite  bool `json:"release_write"`
	Deploy        bool `json:"deploy"`
	WorkerRetry   bool `json:"worker_retry"`
}
type Context struct {
	ID           string       `json:"id"`
	Roles        []string     `json:"roles"`
	Environment  string       `json:"environment"`
	Origin       string       `json:"origin"`
	Store        string       `json:"store"`
	Version      string       `json:"version"`
	Capabilities Capabilities `json:"capabilities"`
}

func has(values []string, value string) bool {
	for _, v := range values {
		if v == value {
			return true
		}
	}
	return false
}
func (h *Handler) authenticate(r *http.Request) (mobileapi.Operator, bool) {
	token := r.Header.Get("X-Operator-Token")
	if len(token) < 32 || len(token) > 4096 {
		return mobileapi.Operator{}, false
	}
	for _, op := range h.Operators {
		if subtle.ConstantTimeCompare([]byte(token), []byte(op.Token)) == 1 {
			return op, true
		}
	}
	return mobileapi.Operator{}, false
}
func capabilities(op mobileapi.Operator, env string) Capabilities {
	scope := has(op.Environments, env)
	read := scope && (has(op.Roles, "ops_viewer") || has(op.Roles, "ops_oncall") || has(op.Roles, "ops_release") || has(op.Roles, "ops_admin"))
	return Capabilities{Read: read, IncidentWrite: read && has(op.Roles, "ops_oncall"), ReleaseWrite: read && has(op.Roles, "ops_release")}
}
func respond(w http.ResponseWriter, status int, data any) {
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(data)
}
func fail(w http.ResponseWriter, status int, code string) {
	respond(w, status, map[string]any{"ok": false, "error_code": code, "request_id": w.Header().Get("X-Request-ID")})
}
func failure(w http.ResponseWriter, e error) {
	switch {
	case errors.Is(e, operations.ErrInvalid):
		fail(w, 400, "invalid_input")
	case errors.Is(e, operations.ErrNotFound):
		fail(w, 404, "not_found")
	case errors.Is(e, operations.ErrConflict):
		fail(w, 409, "version_conflict")
	case errors.Is(e, operations.ErrUnavailable):
		fail(w, 503, "service_unavailable")
	case errors.Is(e, context.Canceled) || errors.Is(e, context.DeadlineExceeded):
		fail(w, 503, "request_timeout")
	default:
		fail(w, 503, "storage_unavailable")
	}
}
func payload[T any](r *http.Request) (out T, err error) {
	raw, err := io.ReadAll(io.LimitReader(r.Body, 32*1024+1))
	if err != nil || len(raw) > 32*1024 {
		return out, operations.ErrInvalid
	}
	d := json.NewDecoder(bytes.NewReader(raw))
	d.DisallowUnknownFields()
	if err = d.Decode(&out); err != nil {
		return out, operations.ErrInvalid
	}
	var extra any
	if d.Decode(&extra) != io.EOF {
		return out, operations.ErrInvalid
	}
	return
}
func (h *Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "private, no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Referrer-Policy", "no-referrer")
	var id [16]byte
	if _, e := rand.Read(id[:]); e != nil {
		fail(w, 503, "request_id_unavailable")
		return
	}
	w.Header().Set("X-Request-ID", hex.EncodeToString(id[:]))
	if r.Method != http.MethodGet && r.Method != http.MethodPost {
		w.Header().Set("Allow", "GET, POST")
		fail(w, 405, "method_not_allowed")
		return
	}
	op, ok := h.authenticate(r)
	if !ok {
		fail(w, 401, "authentication_required")
		return
	}
	if h.Service == nil || !h.Service.Enabled {
		fail(w, 503, "environment_unconfigured")
		return
	}
	caps := capabilities(op, h.Service.Environment)
	if !caps.Read {
		fail(w, 403, "environment_forbidden")
		return
	}
	if origin := r.Header.Get("Origin"); origin != "" && origin != h.Origin {
		fail(w, 403, "origin_forbidden")
		return
	}
	if !strings.HasPrefix(r.URL.Path, "/api/ops/v1/") {
		fail(w, 404, "not_found")
		return
	}
	path := strings.TrimPrefix(r.URL.Path, "/api/ops/v1/")
	parts := strings.Split(path, "/")
	if strings.Contains(path, "..") || len(parts) > 3 {
		fail(w, 404, "not_found")
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 8*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	var out any
	var e error
	switch {
	case r.Method == http.MethodGet && path == "context":
		out = Context{ID: op.ID, Roles: op.Roles, Environment: h.Service.Environment, Origin: h.Service.Origin, Store: h.Service.StoreKind, Version: h.Service.Version, Capabilities: caps}
	case r.Method == http.MethodGet && path == "snapshot":
		out, e = h.Service.Snapshot(ctx)
	case r.Method == http.MethodGet && len(parts) == 1 && has([]string{"runs", "incidents", "releases", "audit"}, parts[0]):
		cursor := int64(0)
		if raw := r.URL.Query().Get("cursor"); raw != "" {
			cursor, e = strconv.ParseInt(raw, 10, 64)
			if e != nil || cursor < 0 {
				e = operations.ErrInvalid
				break
			}
		}
		out, e = h.Service.List(ctx, parts[0], cursor)
	case r.Method == http.MethodGet && len(parts) == 2 && has([]string{"incidents", "releases", "runs", "audit"}, parts[0]):
		out, e = h.Service.Get(ctx, parts[0], parts[1])
	case r.Method == http.MethodGet && len(parts) == 3 && parts[0] == "releases" && parts[2] == "preflight":
		out, e = h.Service.Preflight(ctx, parts[1])
	case r.Method == http.MethodPost && path == "incidents":
		if !caps.IncidentWrite {
			fail(w, 403, "role_forbidden")
			return
		}
		var in operations.IncidentInput
		in, e = payload[operations.IncidentInput](r)
		if e == nil {
			out, e = h.Service.CreateIncident(ctx, op.ID, in)
		}
	case r.Method == http.MethodPost && len(parts) == 3 && parts[0] == "incidents" && parts[2] == "actions":
		if !caps.IncidentWrite {
			fail(w, 403, "role_forbidden")
			return
		}
		var in operations.Command
		in, e = payload[operations.Command](r)
		if e == nil {
			out, e = h.Service.UpdateIncident(ctx, op.ID, parts[1], in)
		}
	case r.Method == http.MethodPost && path == "releases":
		if !caps.ReleaseWrite {
			fail(w, 403, "role_forbidden")
			return
		}
		var in operations.ReleaseInput
		in, e = payload[operations.ReleaseInput](r)
		if e == nil {
			out, e = h.Service.CreateRelease(ctx, op.ID, in)
		}
	case r.Method == http.MethodPost && len(parts) == 3 && parts[0] == "releases" && parts[2] == "actions":
		if !caps.ReleaseWrite {
			fail(w, 403, "role_forbidden")
			return
		}
		var in operations.ReleaseUpdateInput
		in, e = payload[operations.ReleaseUpdateInput](r)
		if e == nil {
			out, e = h.Service.UpdateRelease(ctx, op.ID, parts[1], in)
		}
	default:
		fail(w, 404, "not_found")
		return
	}
	if e != nil {
		failure(w, e)
		return
	}
	respond(w, 200, map[string]any{"ok": true, "data": out, "request_id": w.Header().Get("X-Request-ID")})
}
