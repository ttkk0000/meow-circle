package postgres

import (
	"errors"

	"kitty-circle/internal/domain"

	"github.com/jackc/pgx/v5"
)

const reportCols = "id, reporter_id, target_kind, target_id, reason, status, resolution, handled_by, created_at, updated_at"

func scanReport(row pgx.Row) (domain.Report, error) {
	var r domain.Report
	var kind, status string
	err := row.Scan(&r.ID, &r.ReporterID, &kind, &r.TargetID, &r.Reason,
		&status, &r.Resolution, &r.HandledBy, &r.CreatedAt, &r.UpdatedAt)
	if err != nil {
		return domain.Report{}, err
	}
	r.TargetKind = domain.ReportTargetKind(kind)
	r.Status = domain.ReportStatus(status)
	return r, nil
}

func (s *Store) CreateReport(r domain.Report) domain.Report {
	ctx, cancel := bg()
	defer cancel()
	if r.Status == "" {
		r.Status = domain.ReportStatusOpen
	}
	row := s.pool.QueryRow(ctx, `
		INSERT INTO reports (reporter_id, target_kind, target_id, reason, status, resolution, handled_by)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING `+reportCols,
		r.ReporterID, string(r.TargetKind), r.TargetID, r.Reason, string(r.Status), r.Resolution, r.HandledBy)
	got, err := scanReport(row)
	if err != nil {
		logErr("CreateReport", err)
		return domain.Report{}
	}
	return got
}

func (s *Store) GetReport(id int64) (domain.Report, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `SELECT `+reportCols+` FROM reports WHERE id=$1`, id)
	r, err := scanReport(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetReport", err)
		}
		return domain.Report{}, false
	}
	return r, true
}

func (s *Store) ListReports(status domain.ReportStatus) []domain.Report {
	ctx, cancel := bg()
	defer cancel()
	var rows pgx.Rows
	var err error
	if status == "" {
		rows, err = s.pool.Query(ctx, `SELECT `+reportCols+` FROM reports ORDER BY created_at DESC LIMIT 200`)
	} else {
		rows, err = s.pool.Query(ctx, `SELECT `+reportCols+` FROM reports WHERE status=$1 ORDER BY created_at DESC LIMIT 200`, string(status))
	}
	if err != nil {
		logErr("ListReports", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Report, 0, 16)
	for rows.Next() {
		r, err := scanReport(rows)
		if err != nil {
			continue
		}
		out = append(out, r)
	}
	return out
}

func (s *Store) UpdateReport(report domain.Report) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `
		UPDATE reports SET status=$2, resolution=$3, handled_by=$4, updated_at=now()
		WHERE id=$1`,
		report.ID, string(report.Status), report.Resolution, report.HandledBy)
	if err != nil {
		logErr("UpdateReport", err)
		return false
	}
	return tag.RowsAffected() > 0
}
