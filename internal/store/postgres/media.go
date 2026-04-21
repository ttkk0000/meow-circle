package postgres

import (
	"errors"

	"kitty-circle/internal/domain"

	"github.com/jackc/pgx/v5"
)

const mediaCols = "id, owner_id, kind, mime, size, filename, url, status, created_at"

func scanMedia(row pgx.Row) (domain.Media, error) {
	var m domain.Media
	var kind, status string
	err := row.Scan(&m.ID, &m.OwnerID, &kind, &m.MIME, &m.Size, &m.Filename, &m.URL, &status, &m.CreatedAt)
	if err != nil {
		return domain.Media{}, err
	}
	m.Kind = domain.MediaKind(kind)
	m.Status = domain.MediaStatus(status)
	return m, nil
}

func (s *Store) CreateMedia(media domain.Media) domain.Media {
	ctx, cancel := bg()
	defer cancel()
	if media.Status == "" {
		media.Status = domain.MediaStatusPending
	}
	row := s.pool.QueryRow(ctx, `
		INSERT INTO media (owner_id, kind, mime, size, filename, url, status)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING `+mediaCols,
		media.OwnerID, string(media.Kind), media.MIME, media.Size, media.Filename, media.URL, string(media.Status))
	m, err := scanMedia(row)
	if err != nil {
		logErr("CreateMedia", err)
		return domain.Media{}
	}
	return m
}

func (s *Store) GetMedia(id int64) (domain.Media, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `SELECT `+mediaCols+` FROM media WHERE id=$1`, id)
	m, err := scanMedia(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetMedia", err)
		}
		return domain.Media{}, false
	}
	return m, true
}

func (s *Store) GetMediaBatch(ids []int64) []domain.Media {
	if len(ids) == 0 {
		return nil
	}
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT `+mediaCols+` FROM media WHERE id = ANY($1)`, ids)
	if err != nil {
		logErr("GetMediaBatch", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Media, 0, len(ids))
	for rows.Next() {
		m, err := scanMedia(rows)
		if err != nil {
			continue
		}
		out = append(out, m)
	}
	return out
}

func (s *Store) ListMediaByOwner(ownerID int64) []domain.Media {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT `+mediaCols+` FROM media WHERE owner_id=$1 ORDER BY created_at DESC`, ownerID)
	if err != nil {
		logErr("ListMediaByOwner", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Media, 0, 16)
	for rows.Next() {
		m, err := scanMedia(rows)
		if err != nil {
			continue
		}
		out = append(out, m)
	}
	return out
}

func (s *Store) ListMediaByStatus(status domain.MediaStatus) []domain.Media {
	ctx, cancel := bg()
	defer cancel()
	var rows pgx.Rows
	var err error
	if status == "" {
		rows, err = s.pool.Query(ctx, `SELECT `+mediaCols+` FROM media ORDER BY created_at DESC LIMIT 200`)
	} else {
		rows, err = s.pool.Query(ctx, `SELECT `+mediaCols+` FROM media WHERE status=$1 ORDER BY created_at DESC LIMIT 200`, string(status))
	}
	if err != nil {
		logErr("ListMediaByStatus", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Media, 0, 16)
	for rows.Next() {
		m, err := scanMedia(rows)
		if err != nil {
			continue
		}
		out = append(out, m)
	}
	return out
}

func (s *Store) UpdateMediaStatus(id int64, status domain.MediaStatus) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `UPDATE media SET status=$2 WHERE id=$1`, id, string(status))
	if err != nil {
		logErr("UpdateMediaStatus", err)
		return false
	}
	return tag.RowsAffected() > 0
}

func (s *Store) DeleteMedia(id int64) (domain.Media, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `DELETE FROM media WHERE id=$1 RETURNING `+mediaCols, id)
	m, err := scanMedia(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("DeleteMedia", err)
		}
		return domain.Media{}, false
	}
	return m, true
}
