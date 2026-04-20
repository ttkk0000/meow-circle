package api

import (
	"net/http"
	"strconv"
	"strings"

	"bestTry/internal/domain"
)

// ===== User-facing =====

func (r *Router) handleCreateReport(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	user, _ := currentUser(req)
	var payload struct {
		TargetKind domain.ReportTargetKind `json:"target_kind"`
		TargetID   int64                   `json:"target_id"`
		Reason     string                  `json:"reason"`
	}
	if err := decodeJSON(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	if !validReportTarget(payload.TargetKind) || payload.TargetID <= 0 {
		writeError(w, http.StatusBadRequest, "target_kind and target_id are required")
		return
	}
	reason := strings.TrimSpace(payload.Reason)
	if reason == "" {
		writeError(w, http.StatusBadRequest, "reason is required")
		return
	}
	if len(reason) > 500 {
		reason = reason[:500]
	}

	if !r.reportTargetExists(payload.TargetKind, payload.TargetID) {
		writeError(w, http.StatusNotFound, "target not found")
		return
	}

	report := r.store.CreateReport(domain.Report{
		ReporterID: user.ID,
		TargetKind: payload.TargetKind,
		TargetID:   payload.TargetID,
		Reason:     reason,
	})
	writeCreated(w, report)
}

// ===== Admin-facing =====

func (r *Router) handleAdminReports(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	status := domain.ReportStatus(strings.TrimSpace(req.URL.Query().Get("status")))
	writeOK(w, map[string]any{"items": r.store.ListReports(status)})
}

func (r *Router) handleAdminReportChild(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	path := strings.TrimPrefix(req.URL.Path, "/api/v1/admin/reports/")
	parts := strings.Split(path, "/")
	if len(parts) == 0 || parts[0] == "" {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	id, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid report id")
		return
	}

	if len(parts) == 1 {
		switch req.Method {
		case http.MethodGet:
			report, ok := r.store.GetReport(id)
			if !ok {
				writeError(w, http.StatusNotFound, "report not found")
				return
			}
			writeOK(w, report)
			return
		default:
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
	}

	if len(parts) == 2 {
		action := parts[1]
		if req.Method != http.MethodPost {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		var payload struct {
			Resolution   string `json:"resolution"`
			DeleteTarget bool   `json:"delete_target"`
		}
		_ = decodeJSONOptional(req, &payload)

		report, ok := r.store.GetReport(id)
		if !ok {
			writeError(w, http.StatusNotFound, "report not found")
			return
		}

		switch action {
		case "resolve":
			report.Status = domain.ReportStatusResolved
		case "dismiss":
			report.Status = domain.ReportStatusDismissed
		default:
			writeError(w, http.StatusNotFound, "unknown action")
			return
		}
		report.Resolution = strings.TrimSpace(payload.Resolution)
		report.HandledBy = "admin"
		r.store.UpdateReport(report)

		if action == "resolve" && payload.DeleteTarget {
			r.deleteReportTarget(report)
		}

		r.audit(req, "report_"+action, string(report.TargetKind), report.TargetID, report.Resolution)
		title := "Your report has been " + string(report.Status)
		r.notify(report.ReporterID, domain.NotificationReportHandled, title, report.Resolution, report.ID)

		writeOK(w, report)
		return
	}

	writeError(w, http.StatusNotFound, "not found")
}

// ===== Admin: Media moderation =====

func (r *Router) handleAdminMedia(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	status := domain.MediaStatus(strings.TrimSpace(req.URL.Query().Get("status")))
	writeOK(w, map[string]any{"items": r.store.ListMediaByStatus(status)})
}

func (r *Router) handleAdminMediaChild(w http.ResponseWriter, req *http.Request) {
	if !r.checkAdmin(w, req) {
		return
	}
	path := strings.TrimPrefix(req.URL.Path, "/api/v1/admin/media/")
	parts := strings.Split(path, "/")
	if len(parts) == 0 || parts[0] == "" {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	id, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid media id")
		return
	}

	if len(parts) == 1 {
		if req.Method != http.MethodDelete {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		m, ok := r.store.DeleteMedia(id)
		if !ok {
			writeError(w, http.StatusNotFound, "media not found")
			return
		}
		r.removeMediaFile(m)
		r.audit(req, "delete_media", "media", id, m.Filename)
		writeOK(w, map[string]any{"deleted": true, "id": id})
		return
	}

	if len(parts) == 2 {
		action := parts[1]
		if req.Method != http.MethodPost {
			writeError(w, http.StatusMethodNotAllowed, "method not allowed")
			return
		}
		var newStatus domain.MediaStatus
		switch action {
		case "approve":
			newStatus = domain.MediaStatusApproved
		case "reject":
			newStatus = domain.MediaStatusRejected
		default:
			writeError(w, http.StatusNotFound, "unknown action")
			return
		}
		if !r.store.UpdateMediaStatus(id, newStatus) {
			writeError(w, http.StatusNotFound, "media not found")
			return
		}
		m, _ := r.store.GetMedia(id)
		r.audit(req, "media_"+action, "media", id, "")
		writeOK(w, m)
		return
	}

	writeError(w, http.StatusNotFound, "not found")
}

// ===== Helpers =====

func validReportTarget(kind domain.ReportTargetKind) bool {
	switch kind {
	case domain.ReportTargetPost, domain.ReportTargetComment, domain.ReportTargetListing, domain.ReportTargetMedia:
		return true
	}
	return false
}

func (r *Router) reportTargetExists(kind domain.ReportTargetKind, id int64) bool {
	switch kind {
	case domain.ReportTargetPost:
		_, ok := r.store.GetPost(id)
		return ok
	case domain.ReportTargetListing:
		_, ok := r.store.GetListing(id)
		return ok
	case domain.ReportTargetMedia:
		_, ok := r.store.GetMedia(id)
		return ok
	case domain.ReportTargetComment:
		for _, c := range r.store.ListAllComments() {
			if c.ID == id {
				return true
			}
		}
	}
	return false
}

func (r *Router) deleteReportTarget(report domain.Report) {
	switch report.TargetKind {
	case domain.ReportTargetPost:
		r.store.DeletePost(report.TargetID)
	case domain.ReportTargetComment:
		r.store.DeleteComment(report.TargetID)
	case domain.ReportTargetListing:
		r.store.DeleteListing(report.TargetID)
	case domain.ReportTargetMedia:
		if m, ok := r.store.DeleteMedia(report.TargetID); ok {
			r.removeMediaFile(m)
		}
	}
}
