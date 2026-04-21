package postgres

import (
	"errors"

	"kitty-circle/internal/domain"

	"github.com/jackc/pgx/v5"
)

const userCols = "id, username, nickname, avatar_url, bio, password_hash, password_salt, created_at"

func scanUser(row pgx.Row) (domain.User, error) {
	var u domain.User
	err := row.Scan(&u.ID, &u.Username, &u.Nickname, &u.AvatarURL, &u.Bio,
		&u.PasswordHash, &u.PasswordSalt, &u.CreatedAt)
	return u, err
}

func (s *Store) CreateUser(user domain.User) (domain.User, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `
		INSERT INTO users (username, nickname, avatar_url, bio, password_hash, password_salt)
		VALUES ($1, $2, $3, $4, $5, $6)
		RETURNING `+userCols,
		user.Username, user.Nickname, user.AvatarURL, user.Bio, user.PasswordHash, user.PasswordSalt)
	u, err := scanUser(row)
	if err != nil {
		logErr("CreateUser", err)
		return domain.User{}, false
	}
	return u, true
}

func (s *Store) FindUserByUsername(username string) (domain.User, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `SELECT `+userCols+` FROM users WHERE username = $1`, username)
	u, err := scanUser(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("FindUserByUsername", err)
		}
		return domain.User{}, false
	}
	return u, true
}

func (s *Store) GetUser(id int64) (domain.User, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `SELECT `+userCols+` FROM users WHERE id = $1`, id)
	u, err := scanUser(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetUser", err)
		}
		return domain.User{}, false
	}
	return u, true
}

func (s *Store) UpdateUserProfile(id int64, nickname, avatarURL, bio string) (domain.User, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `
		UPDATE users
		SET nickname = COALESCE(NULLIF($2, ''), nickname),
		    avatar_url = $3,
		    bio = $4
		WHERE id = $1
		RETURNING `+userCols,
		id, nickname, avatarURL, bio)
	u, err := scanUser(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("UpdateUserProfile", err)
		}
		return domain.User{}, false
	}
	return u, true
}

func (s *Store) CountUsers() int {
	ctx, cancel := bg()
	defer cancel()
	var n int
	if err := s.pool.QueryRow(ctx, `SELECT COUNT(*) FROM users`).Scan(&n); err != nil {
		logErr("CountUsers", err)
		return 0
	}
	return n
}

func (s *Store) GetUsers(ids []int64) map[int64]domain.User {
	out := map[int64]domain.User{}
	if len(ids) == 0 {
		return out
	}
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, `SELECT `+userCols+` FROM users WHERE id = ANY($1)`, ids)
	if err != nil {
		logErr("GetUsers", err)
		return out
	}
	defer rows.Close()
	for rows.Next() {
		u, err := scanUser(rows)
		if err != nil {
			logErr("GetUsers.scan", err)
			continue
		}
		out[u.ID] = u
	}
	return out
}
