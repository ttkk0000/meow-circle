package postgres

import (
	"errors"
	"strings"
	"time"

	"kitty-circle/internal/domain"

	"github.com/jackc/pgx/v5"
)

const postCols = "id, author_id, title, content, category, tags, media_ids, created_at, last_reply_at"

func scanPost(row pgx.Row) (domain.Post, error) {
	var p domain.Post
	var cat string
	var tags []string
	var mediaIDs []int64
	var lastReply *time.Time
	if err := row.Scan(&p.ID, &p.AuthorID, &p.Title, &p.Content, &cat, &tags, &mediaIDs, &p.CreatedAt, &lastReply); err != nil {
		return domain.Post{}, err
	}
	p.Category = domain.PostCategory(cat)
	p.Tags = tags
	p.MediaIDs = mediaIDs
	if lastReply != nil {
		p.LastReplyAt = *lastReply
	} else {
		p.LastReplyAt = p.CreatedAt
	}
	return p, nil
}

func (s *Store) CreatePost(input domain.Post) domain.Post {
	ctx, cancel := bg()
	defer cancel()
	if input.Category == "" {
		input.Category = domain.CategoryDailyShare
	}
	row := s.pool.QueryRow(ctx, `
		INSERT INTO posts (author_id, title, content, category, tags, media_ids, last_reply_at)
		VALUES ($1, $2, $3, $4, $5, $6, now())
		RETURNING `+postCols,
		input.AuthorID, input.Title, input.Content, string(input.Category), nonNilStrings(input.Tags), nonNilInt64s(input.MediaIDs))
	p, err := scanPost(row)
	if err != nil {
		logErr("CreatePost", err)
		return domain.Post{}
	}
	return p
}

func (s *Store) ListPosts() []domain.Post {
	return s.listPosts(`SELECT `+postCols+` FROM posts ORDER BY last_reply_at DESC NULLS LAST, created_at DESC`, nil)
}

func (s *Store) ListPostsByAuthor(authorID int64) []domain.Post {
	return s.listPosts(`SELECT `+postCols+` FROM posts WHERE author_id = $1 ORDER BY created_at DESC`, []any{authorID})
}

func (s *Store) GetPost(postID int64) (domain.Post, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `SELECT `+postCols+` FROM posts WHERE id = $1`, postID)
	p, err := scanPost(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetPost", err)
		}
		return domain.Post{}, false
	}
	return p, true
}

func (s *Store) UpdatePost(post domain.Post) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `
		UPDATE posts SET title=$2, content=$3, category=$4, tags=$5, media_ids=$6, last_reply_at=$7
		WHERE id=$1`,
		post.ID, post.Title, post.Content, string(post.Category),
		nonNilStrings(post.Tags), nonNilInt64s(post.MediaIDs), nullableTime(post.LastReplyAt))
	if err != nil {
		logErr("UpdatePost", err)
		return false
	}
	return tag.RowsAffected() > 0
}

func (s *Store) DeletePost(postID int64) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `DELETE FROM posts WHERE id=$1`, postID)
	if err != nil {
		logErr("DeletePost", err)
		return false
	}
	return tag.RowsAffected() > 0
}

func (s *Store) SearchPosts(keyword string) []domain.Post {
	keyword = strings.TrimSpace(keyword)
	if keyword == "" {
		return nil
	}
	like := "%" + strings.ToLower(keyword) + "%"
	return s.listPosts(`
		SELECT `+postCols+` FROM posts
		WHERE LOWER(title) LIKE $1 OR LOWER(content) LIKE $1
		ORDER BY created_at DESC LIMIT 50`, []any{like})
}

func (s *Store) listPosts(sql string, args []any) []domain.Post {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, sql, args...)
	if err != nil {
		logErr("listPosts", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Post, 0, 16)
	for rows.Next() {
		p, err := scanPost(rows)
		if err != nil {
			logErr("listPosts.scan", err)
			continue
		}
		out = append(out, p)
	}
	return out
}

// ===== helpers =====

func nonNilStrings(v []string) []string {
	if v == nil {
		return []string{}
	}
	return v
}

func nonNilInt64s(v []int64) []int64 {
	if v == nil {
		return []int64{}
	}
	return v
}

func nullableTime(t time.Time) any {
	if t.IsZero() {
		return nil
	}
	return t
}
