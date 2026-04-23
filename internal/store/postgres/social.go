package postgres

import (
	"github.com/jackc/pgx/v5"
)

func (s *Store) Follow(followerID, followingID int64) bool {
	if followerID == followingID {
		return false
	}
	ctx, cancel := bg()
	defer cancel()
	var n int
	if err := s.pool.QueryRow(ctx, `SELECT COUNT(*) FROM users WHERE id = $1 OR id = $2`, followerID, followingID).Scan(&n); err != nil || n != 2 {
		return false
	}
	_, err := s.pool.Exec(ctx,
		`INSERT INTO follows (follower_id, following_id) VALUES ($1,$2) ON CONFLICT DO NOTHING`,
		followerID, followingID,
	)
	if err != nil {
		logErr("Follow", err)
		return false
	}
	return true
}

func (s *Store) Unfollow(followerID, followingID int64) bool {
	ctx, cancel := bg()
	defer cancel()
	_, err := s.pool.Exec(ctx, `DELETE FROM follows WHERE follower_id=$1 AND following_id=$2`, followerID, followingID)
	if err != nil {
		logErr("Unfollow", err)
		return false
	}
	return true
}

func (s *Store) IsFollowing(followerID, followingID int64) bool {
	ctx, cancel := bg()
	defer cancel()
	var exists bool
	err := s.pool.QueryRow(ctx,
		`SELECT EXISTS(SELECT 1 FROM follows WHERE follower_id=$1 AND following_id=$2)`,
		followerID, followingID,
	).Scan(&exists)
	if err != nil {
		logErr("IsFollowing", err)
		return false
	}
	return exists
}

func (s *Store) ListFollowingIDs(followerID int64) []int64 {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT following_id FROM follows WHERE follower_id=$1 ORDER BY following_id`, followerID)
	if err != nil {
		logErr("ListFollowingIDs", err)
		return nil
	}
	defer rows.Close()
	var out []int64
	for rows.Next() {
		var id int64
		if err := rows.Scan(&id); err != nil {
			logErr("ListFollowingIDs.scan", err)
			continue
		}
		out = append(out, id)
	}
	return out
}

func (s *Store) TogglePostLike(userID, postID int64) (liked bool, count int64, ok bool) {
	ctx, cancel := bg()
	defer cancel()
	var postExists bool
	if err := s.pool.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM posts WHERE id=$1)`, postID).Scan(&postExists); err != nil || !postExists {
		return false, 0, false
	}
	tx, err := s.pool.BeginTx(ctx, pgx.TxOptions{})
	if err != nil {
		logErr("TogglePostLike.begin", err)
		return false, 0, false
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var had bool
	_ = tx.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM post_likes WHERE user_id=$1 AND post_id=$2)`, userID, postID).Scan(&had)
	if had {
		if _, err := tx.Exec(ctx, `DELETE FROM post_likes WHERE user_id=$1 AND post_id=$2`, userID, postID); err != nil {
			logErr("TogglePostLike.delete", err)
			return false, 0, false
		}
		liked = false
	} else {
		if _, err := tx.Exec(ctx, `INSERT INTO post_likes (user_id, post_id) VALUES ($1,$2)`, userID, postID); err != nil {
			logErr("TogglePostLike.insert", err)
			return false, 0, false
		}
		liked = true
	}
	if err := tx.QueryRow(ctx, `SELECT COUNT(*) FROM post_likes WHERE post_id=$1`, postID).Scan(&count); err != nil {
		logErr("TogglePostLike.count", err)
		return false, 0, false
	}
	if err := tx.Commit(ctx); err != nil {
		logErr("TogglePostLike.commit", err)
		return false, 0, false
	}
	return liked, count, true
}

func (s *Store) BatchPostLikeCounts(postIDs []int64) map[int64]int64 {
	out := make(map[int64]int64, len(postIDs))
	if len(postIDs) == 0 {
		return out
	}
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT post_id, COUNT(*) FROM post_likes WHERE post_id = ANY($1) GROUP BY post_id`, postIDs)
	if err != nil {
		logErr("BatchPostLikeCounts", err)
		for _, id := range postIDs {
			out[id] = 0
		}
		return out
	}
	defer rows.Close()
	for rows.Next() {
		var pid, c int64
		if err := rows.Scan(&pid, &c); err != nil {
			logErr("BatchPostLikeCounts.scan", err)
			continue
		}
		out[pid] = c
	}
	for _, id := range postIDs {
		if _, ok := out[id]; !ok {
			out[id] = 0
		}
	}
	return out
}

func (s *Store) BatchUserLikedPosts(userID int64, postIDs []int64) map[int64]bool {
	if userID == 0 || len(postIDs) == 0 {
		return nil
	}
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT post_id FROM post_likes WHERE user_id=$1 AND post_id = ANY($2)`, userID, postIDs)
	if err != nil {
		logErr("BatchUserLikedPosts", err)
		return nil
	}
	defer rows.Close()
	out := make(map[int64]bool, len(postIDs))
	for rows.Next() {
		var pid int64
		if err := rows.Scan(&pid); err != nil {
			continue
		}
		out[pid] = true
	}
	return out
}
