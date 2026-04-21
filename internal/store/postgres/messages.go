package postgres

import (
	"time"

	"kitty-circle/internal/domain"
)

const messageCols = "id, sender_id, recipient_id, content, read, created_at"

func (s *Store) CreateMessage(m domain.Message) domain.Message {
	ctx, cancel := bg()
	defer cancel()
	var got domain.Message
	err := s.pool.QueryRow(ctx, `
		INSERT INTO messages (sender_id, recipient_id, content, read)
		VALUES ($1, $2, $3, FALSE)
		RETURNING `+messageCols,
		m.SenderID, m.RecipientID, m.Content,
	).Scan(&got.ID, &got.SenderID, &got.RecipientID, &got.Content, &got.Read, &got.CreatedAt)
	if err != nil {
		logErr("CreateMessage", err)
		return domain.Message{}
	}
	return got
}

func (s *Store) ListMessagesBetween(a, b int64) []domain.Message {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `
		SELECT `+messageCols+` FROM messages
		WHERE (sender_id=$1 AND recipient_id=$2) OR (sender_id=$2 AND recipient_id=$1)
		ORDER BY created_at ASC`, a, b)
	if err != nil {
		logErr("ListMessagesBetween", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Message, 0, 16)
	for rows.Next() {
		var m domain.Message
		if err := rows.Scan(&m.ID, &m.SenderID, &m.RecipientID, &m.Content, &m.Read, &m.CreatedAt); err != nil {
			continue
		}
		out = append(out, m)
	}
	return out
}

func (s *Store) MarkConversationRead(self, peer int64) int {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx,
		`UPDATE messages SET read=TRUE WHERE recipient_id=$1 AND sender_id=$2 AND read=FALSE`,
		self, peer)
	if err != nil {
		logErr("MarkConversationRead", err)
		return 0
	}
	return int(tag.RowsAffected())
}

// ListConversations aggregates messages touching userID into per-peer summaries:
// peer info, last message, last sender, unread count, updated_at.
func (s *Store) ListConversations(userID int64) []domain.Conversation {
	ctx, cancel := bg()
	defer cancel()

	rows, err := s.pool.Query(ctx, `
		WITH conv AS (
			SELECT
				CASE WHEN sender_id = $1 THEN recipient_id ELSE sender_id END AS peer_id,
				content, sender_id, recipient_id, read, created_at,
				ROW_NUMBER() OVER (
					PARTITION BY (CASE WHEN sender_id = $1 THEN recipient_id ELSE sender_id END)
					ORDER BY created_at DESC
				) AS rn
			FROM messages
			WHERE sender_id = $1 OR recipient_id = $1
		),
		latest AS (SELECT peer_id, content, sender_id, created_at FROM conv WHERE rn = 1),
		unread AS (
			SELECT sender_id AS peer_id, COUNT(*)::int AS n
			FROM messages
			WHERE recipient_id = $1 AND read = FALSE
			GROUP BY sender_id
		)
		SELECT l.peer_id, l.content, l.sender_id, l.created_at, COALESCE(u.n, 0),
		       p.id, p.username, p.nickname, p.avatar_url, p.bio, p.created_at
		FROM latest l
		LEFT JOIN unread u ON u.peer_id = l.peer_id
		LEFT JOIN users  p ON p.id     = l.peer_id
		ORDER BY l.created_at DESC
		LIMIT 100`, userID)
	if err != nil {
		logErr("ListConversations", err)
		return nil
	}
	defer rows.Close()

	out := make([]domain.Conversation, 0, 16)
	for rows.Next() {
		var (
			c         domain.Conversation
			peerID    int64
			peerRefID *int64
			username  *string
			nickname  *string
			avatar    *string
			bio       *string
			peerCrAt  *time.Time
		)
		if err := rows.Scan(
			&peerID, &c.LastMessage, &c.LastSenderID, &c.UpdatedAt, &c.UnreadCount,
			&peerRefID, &username, &nickname, &avatar, &bio, &peerCrAt,
		); err != nil {
			logErr("ListConversations.scan", err)
			continue
		}
		if peerRefID != nil {
			c.Peer.ID = *peerRefID
		} else {
			c.Peer.ID = peerID
		}
		if username != nil {
			c.Peer.Username = *username
		} else {
			c.Peer.Username = "deleted"
		}
		if nickname != nil {
			c.Peer.Nickname = *nickname
		}
		if avatar != nil {
			c.Peer.AvatarURL = *avatar
		}
		if bio != nil {
			c.Peer.Bio = *bio
		}
		if peerCrAt != nil {
			c.Peer.CreatedAt = *peerCrAt
		}
		out = append(out, c)
	}
	return out
}
