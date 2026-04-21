package postgres

import "kitty-circle/internal/domain"

const auditCols = "id, actor, action, target_kind, target_id, note, ip, created_at"

func (s *Store) CreateAuditLog(log domain.AuditLog) domain.AuditLog {
	ctx, cancel := bg()
	defer cancel()
	var got domain.AuditLog
	err := s.pool.QueryRow(ctx, `
		INSERT INTO audit_logs (actor, action, target_kind, target_id, note, ip)
		VALUES ($1, $2, $3, $4, $5, $6)
		RETURNING `+auditCols,
		log.Actor, log.Action, log.TargetKind, log.TargetID, log.Note, log.IP,
	).Scan(&got.ID, &got.Actor, &got.Action, &got.TargetKind, &got.TargetID, &got.Note, &got.IP, &got.CreatedAt)
	if err != nil {
		logErr("CreateAuditLog", err)
		return domain.AuditLog{}
	}
	return got
}

func (s *Store) ListAuditLogs(limit int) []domain.AuditLog {
	if limit <= 0 {
		limit = 100
	}
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT `+auditCols+` FROM audit_logs ORDER BY created_at DESC LIMIT $1`, limit)
	if err != nil {
		logErr("ListAuditLogs", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.AuditLog, 0, 16)
	for rows.Next() {
		var l domain.AuditLog
		if err := rows.Scan(&l.ID, &l.Actor, &l.Action, &l.TargetKind, &l.TargetID, &l.Note, &l.IP, &l.CreatedAt); err != nil {
			continue
		}
		out = append(out, l)
	}
	return out
}
