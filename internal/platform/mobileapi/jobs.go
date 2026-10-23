package mobileapi

import (
	"errors"
	"net/http"

	"kitty-circle/internal/mobile"
)

func (h *Handler) runJob(r *http.Request, op Operator, job string, in mobile.JobInput) (out mobile.JobResult, err error) {
	out.Positions = map[string]mobile.JobPosition{}
	if !mobile.HasString([]string{"push", "nearby_alerts", "privacy_cleanup", "provider_revocations", "maintenance"}, job) {
		return out, mobile.ErrNotFound
	}
	for name, position := range in.Positions {
		allowed := job == "maintenance" && mobile.HasString([]string{"safety", "otp", "tokens", "evidence", "uploads"}, name) || job == "provider_revocations" && name == "revocations"
		if !allowed || position.Cursor < 0 || position.After != "" && name != "uploads" {
			return out, mobile.Invalid("invalid job position")
		}
	}
	if job == "maintenance" {
		return h.maintenanceJob(r, op.ID, in)
	}
	if !op.permitsConsumer(job) {
		return out, mobile.ErrForbidden
	}
	if job == "provider_revocations" {
		next, count, e := h.Identity.ProcessRevocations(r.Context(), in.Positions["revocations"].Cursor)
		out.State, out.Processed = "batch_complete", count
		out.Positions["revocations"] = mobile.JobPosition{Cursor: next}
		return out, e
	}
	if job == "privacy_cleanup" && !mobile.HasString(op.Roles, "privacy") {
		return out, mobile.ErrForbidden
	}
	work, e := h.Service.ClaimDeliveries(r.Context(), op.ID, mobile.DeliveryClaim{Consumer: job, Limit: 1})
	if e != nil {
		return out, e
	}
	out.State = "idle"
	if len(work) == 0 {
		return out, nil
	}
	item := work[0]
	lease, e := mobile.Decode[mobile.Delivery](item.Delivery)
	if e != nil {
		return out, e
	}
	out.DeliveryID = item.Delivery.ID
	complete, cursor := true, int64(0)
	switch job {
	case "push":
		err = h.Service.DispatchPush(r.Context(), op.ID, item.Event.Event.AggregateID)
	case "nearby_alerts":
		var batch mobile.AlertBatch
		batch, err = h.Service.NearbyAlerts(r.Context(), item.Event.Event.AggregateID, lease.Cursor)
		cursor, out.Processed, complete = batch.Next, batch.Created, batch.Next == 0
	case "privacy_cleanup":
		complete, out.Processed, err = h.privacyJob(r, op.ID, item.Event.Event.AggregateID)
	}
	// Deleting a notification/case supersedes its pending outward effect.
	// Missing privacy requests still require investigation rather than an ACK.
	if (job == "push" || job == "nearby_alerts") && errors.Is(err, mobile.ErrNotFound) {
		err, complete, cursor = nil, true, 0
	}
	result := mobile.DeliveryResult{Consumer: job, Version: item.Delivery.Version, Success: err == nil && complete,
		ResultRef: "job_batch_confirmed", Cursor: cursor}
	if err != nil {
		result.Cursor, result.ResultRef, result.RetryAfterSeconds = lease.Cursor, "job_result_pending", 60
		var rule mobile.RuleError
		if errors.As(err, &rule) {
			result.ResultRef = rule.Code
		}
	} else if !complete {
		result.ResultRef, result.RetryAfterSeconds = "job_continuation_pending", 30
	}
	if e = h.Service.CompleteDelivery(r.Context(), op.ID, item.Delivery.ID, result); e != nil {
		return out, e
	}
	out.State = "acknowledged"
	if !result.Success {
		out.State, out.RetryAfterSeconds = "retry", result.RetryAfterSeconds
	}
	return out, nil
}

func (h *Handler) privacyJob(r *http.Request, operator string, id int64) (complete bool, processed int, err error) {
	work, err := h.Service.DataRequestWork(r.Context(), operator, id)
	if err != nil {
		return false, 0, err
	}
	if work.State == "completed" {
		return work.ProgressVersion == 1, 0, nil
	}
	if work.State == "needs_assistance" {
		return false, 0, nil // Only an actual operator resolution may finish assistance.
	}
	record, err := h.Service.ProcessDataRequest(r.Context(), operator, id, work.Version)
	if err != nil {
		return false, 0, err
	}
	d, err := mobile.Decode[mobile.DataRequest](record)
	if err != nil {
		return false, 0, err
	}
	for _, item := range d.Items {
		if item.Kind != mobile.Media || item.State != "physical_pending" {
			continue
		}
		if processed >= 20 {
			break
		}
		if err = h.eraseMedia(r, operator, item.ID); err != nil {
			return false, processed, err
		}
		processed++
	}
	if processed > 0 {
		work, err = h.Service.DataRequestWork(r.Context(), operator, id)
		if err != nil {
			return false, processed, err
		}
		record, err = h.Service.ProcessDataRequest(r.Context(), operator, id, work.Version)
		if err != nil {
			return false, processed, err
		}
	}
	return record.State == "completed", processed, nil
}

func (h *Handler) maintenanceJob(r *http.Request, operator string, in mobile.JobInput) (out mobile.JobResult, err error) {
	out.State, out.Positions = "batch_complete", map[string]mobile.JobPosition{}
	for _, name := range []string{"safety", "otp", "tokens", "evidence", "uploads"} {
		position := in.Positions[name]
		var next int64
		var count int
		switch name {
		case "safety":
			next, count, err = h.Service.PurgeExpiredSafety(r.Context(), operator, position.Cursor)
		case "otp":
			next, count, err = h.Identity.PurgeExpiredChallenges(r.Context(), position.Cursor)
		case "tokens":
			next, count, err = h.Identity.PurgeExpiredTokens(r.Context(), position.Cursor)
		case "evidence":
			var result mobile.RetentionExpiryResult
			result, err = h.Service.ExpireRightsEvidence(r.Context(), operator, mobile.RetentionExpiryInput{Cursor: position.Cursor})
			next, count = result.NextCursor, result.Erased
		case "uploads":
			var result mobile.UploadCleanupResult
			result, err = h.cleanupUploads(r, operator, mobile.UploadCleanupInput{Cursor: position.Cursor, After: position.After})
			next, count, position.After = result.NextCursor, result.Expired+result.Removed, result.NextFile
		}
		if err != nil {
			return out, err
		}
		position.Cursor = next
		out.Positions[name], out.Processed = position, out.Processed+count
	}
	return out, nil
}
