package api

import (
	"net/http"
	"strconv"

	"bestTry/internal/domain"
)

// audit records an administrator action. The caller is responsible for
// providing a readable label; include enough context to be useful in forensics.
func (r *Router) audit(req *http.Request, action, targetKind string, targetID int64, note string) {
	r.store.CreateAuditLog(domain.AuditLog{
		Actor:      "admin",
		Action:     action,
		TargetKind: targetKind,
		TargetID:   targetID,
		Note:       note,
		IP:         clientIP(req),
	})
}

func clientIP(req *http.Request) string {
	if fwd := req.Header.Get("X-Forwarded-For"); fwd != "" {
		return fwd
	}
	return req.RemoteAddr
}

func (r *Router) handleAdminAudit(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	limit, _ := strconv.Atoi(req.URL.Query().Get("limit"))
	if limit <= 0 || limit > 500 {
		limit = 100
	}
	writeOK(w, map[string]any{"items": r.store.ListAuditLogs(limit)})
}
