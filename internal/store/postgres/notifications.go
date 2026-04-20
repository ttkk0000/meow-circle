package postgres

import (
	"bestTry/internal/domain"

	"github.com/jackc/pgx/v5"
)

const notifCols = "id, user_id, kind, title, body, ref_id, read, created_at"

func scanNotif(row pgx.Row) (domain.Notification, error) {
	var n domain.Notification
	var kind string
	err := row.Scan(&n.ID, &n.UserID, &kind, &n.Title, &n.Body, &n.RefID, &n.Read, &n.CreatedAt)
	if err != nil {
		return domain.Notification{}, err
	}
	n.Kind = domain.NotificationKind(kind)
	return n, nil
}

func (s *Store) CreateNotification(n domain.Notification) domain.Notification {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `
		INSERT INTO notifications (user_id, kind, title, body, ref_id, read)
		VALUES ($1, $2, $3, $4, $5, FALSE)
		RETURNING `+notifCols,
		n.UserID, string(n.Kind), n.Title, n.Body, n.RefID)
	got, err := scanNotif(row)
	if err != nil {
		logErr("CreateNotification", err)
		return domain.Notification{}
	}
	return got
}

func (s *Store) ListNotifications(userID int64, unreadOnly bool) []domain.Notification {
	ctx, cancel := bg()
	defer cancel()
	var rows pgx.Rows
	var err error
	if unreadOnly {
		rows, err = s.pool.Query(ctx, `SELECT `+notifCols+` FROM notifications WHERE user_id=$1 AND read=FALSE ORDER BY created_at DESC LIMIT 100`, userID)
	} else {
		rows, err = s.pool.Query(ctx, `SELECT `+notifCols+` FROM notifications WHERE user_id=$1 ORDER BY created_at DESC LIMIT 100`, userID)
	}
	if err != nil {
		logErr("ListNotifications", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Notification, 0, 16)
	for rows.Next() {
		n, err := scanNotif(rows)
		if err != nil {
			continue
		}
		out = append(out, n)
	}
	return out
}

func (s *Store) CountUnreadNotifications(userID int64) int {
	ctx, cancel := bg()
	defer cancel()
	var n int
	if err := s.pool.QueryRow(ctx, `SELECT COUNT(*) FROM notifications WHERE user_id=$1 AND read=FALSE`, userID).Scan(&n); err != nil {
		logErr("CountUnreadNotifications", err)
		return 0
	}
	return n
}

func (s *Store) MarkNotificationRead(id, userID int64) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `UPDATE notifications SET read=TRUE WHERE id=$1 AND user_id=$2 AND read=FALSE`, id, userID)
	if err != nil {
		logErr("MarkNotificationRead", err)
		return false
	}
	return tag.RowsAffected() > 0
}

func (s *Store) MarkAllNotificationsRead(userID int64) int {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `UPDATE notifications SET read=TRUE WHERE user_id=$1 AND read=FALSE`, userID)
	if err != nil {
		logErr("MarkAllNotificationsRead", err)
		return 0
	}
	return int(tag.RowsAffected())
}
