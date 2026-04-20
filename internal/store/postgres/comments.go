package postgres

import (
	"errors"

	"bestTry/internal/domain"

	"github.com/jackc/pgx/v5"
)

const commentCols = "id, post_id, author_id, content, created_at"

func scanComment(row pgx.Row) (domain.Comment, error) {
	var c domain.Comment
	err := row.Scan(&c.ID, &c.PostID, &c.AuthorID, &c.Content, &c.CreatedAt)
	return c, err
}

func (s *Store) AddComment(input domain.Comment) (domain.Comment, bool) {
	ctx, cancel := bg()
	defer cancel()
	// Ensure post exists to preserve MemoryStore semantics (return ok=false).
	var exists bool
	if err := s.pool.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM posts WHERE id=$1)`, input.PostID).Scan(&exists); err != nil {
		logErr("AddComment.exists", err)
		return domain.Comment{}, false
	}
	if !exists {
		return domain.Comment{}, false
	}
	row := s.pool.QueryRow(ctx, `
		INSERT INTO comments (post_id, author_id, content)
		VALUES ($1, $2, $3)
		RETURNING `+commentCols,
		input.PostID, input.AuthorID, input.Content)
	c, err := scanComment(row)
	if err != nil {
		logErr("AddComment", err)
		return domain.Comment{}, false
	}
	// Touch post.last_reply_at so ListPosts ordering matches memory backend.
	if _, err := s.pool.Exec(ctx, `UPDATE posts SET last_reply_at = $2 WHERE id = $1`, input.PostID, c.CreatedAt); err != nil {
		logErr("AddComment.touch", err)
	}
	return c, true
}

func (s *Store) ListCommentsByPost(postID int64) []domain.Comment {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT `+commentCols+` FROM comments WHERE post_id=$1 ORDER BY created_at ASC`, postID)
	if err != nil {
		logErr("ListCommentsByPost", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Comment, 0, 8)
	for rows.Next() {
		c, err := scanComment(rows)
		if err != nil {
			continue
		}
		out = append(out, c)
	}
	return out
}

func (s *Store) ListAllComments() []domain.Comment {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT `+commentCols+` FROM comments ORDER BY created_at DESC LIMIT 500`)
	if err != nil {
		logErr("ListAllComments", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Comment, 0, 16)
	for rows.Next() {
		c, err := scanComment(rows)
		if err != nil {
			continue
		}
		out = append(out, c)
	}
	return out
}

func (s *Store) DeleteComment(commentID int64) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `DELETE FROM comments WHERE id=$1`, commentID)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("DeleteComment", err)
		}
		return false
	}
	return tag.RowsAffected() > 0
}
