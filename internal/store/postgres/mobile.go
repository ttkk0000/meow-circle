package postgres

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
)

type mobileRepository struct{ store *Store }
type mobileTx struct {
	ctx      context.Context
	tx       pgx.Tx
	writable bool
}

func (s *Store) MobileRepository() mobile.Repository { return &mobileRepository{s} }
func (r *mobileRepository) View(ctx context.Context, fn func(mobile.Tx) error) error {
	return r.transaction(ctx, false, fn)
}
func (r *mobileRepository) Update(ctx context.Context, fn func(mobile.Tx) error) error {
	return r.transaction(ctx, true, fn)
}
func (r *mobileRepository) transaction(ctx context.Context, writable bool, fn func(mobile.Tx) error) error {
	options := pgx.TxOptions{IsoLevel: pgx.RepeatableRead, AccessMode: pgx.ReadOnly}
	if writable {
		options = pgx.TxOptions{IsoLevel: pgx.Serializable, AccessMode: pgx.ReadWrite}
	}
	tx, err := r.store.pool.BeginTx(ctx, options)
	if err != nil {
		return mobileError(err)
	}
	defer func() {
		cleanup, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = tx.Rollback(cleanup)
	}()
	if err := fn(&mobileTx{ctx: ctx, tx: tx, writable: writable}); err != nil {
		return mobileError(err)
	}
	return mobileError(tx.Commit(ctx))
}
func mobileError(err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return mobile.ErrNotFound
	}
	var pg *pgconn.PgError
	if errors.As(err, &pg) {
		switch pg.Code {
		case "23505", "40001", "40P01":
			return mobile.ErrConflict
		}
	}
	return err
}
func mobileTable(k mobile.Kind) (string, error) {
	if !k.Valid() {
		return "", mobile.Invalid("invalid record kind")
	}
	return "mobile_" + string(k), nil
}

const mobileColumns = `id, COALESCE(owner_id,0), participants, record_key, COALESCE(reference_id,0), media_ids, public, state, version, created_at, updated_at, data`

func scanMobile(row pgx.Row) (mobile.Record, error) {
	var r mobile.Record
	err := row.Scan(&r.ID, &r.OwnerID, &r.Participants, &r.Key, &r.RefID, &r.MediaIDs,
		&r.Public, &r.State, &r.Version, &r.CreatedAt, &r.UpdatedAt, &r.Data)
	return r, mobileError(err)
}
func (t *mobileTx) Get(k mobile.Kind, id int64) (mobile.Record, error) {
	table, err := mobileTable(k)
	if err != nil {
		return mobile.Record{}, err
	}
	lock := ""
	if t.writable {
		lock = " FOR UPDATE"
	}
	return scanMobile(t.tx.QueryRow(t.ctx, "SELECT "+mobileColumns+" FROM "+table+" WHERE id=$1"+lock, id))
}
func (t *mobileTx) ByKey(k mobile.Kind, key string) (mobile.Record, error) {
	table, err := mobileTable(k)
	if err != nil {
		return mobile.Record{}, err
	}
	if key == "" {
		return mobile.Record{}, mobile.ErrNotFound
	}
	lock := ""
	if t.writable {
		lock = " FOR UPDATE"
	}
	return scanMobile(t.tx.QueryRow(t.ctx, "SELECT "+mobileColumns+" FROM "+table+" WHERE record_key=$1"+lock, key))
}
func (t *mobileTx) List(k mobile.Kind, f mobile.Filter) ([]mobile.Record, error) {
	table, err := mobileTable(k)
	if err != nil {
		return nil, err
	}
	limit := f.Limit
	if limit <= 0 || limit > 500 {
		limit = 100
	}
	where, args := []string{"id > $1"}, []any{f.After}
	add := func(predicate string, value any) {
		args = append(args, value)
		where = append(where, fmt.Sprintf(predicate, len(args)))
	}
	if f.OwnerID > 0 {
		add("owner_id = $%d", f.OwnerID)
	}
	if f.ParticipantID > 0 {
		args = append(args, f.ParticipantID)
		where = append(where, fmt.Sprintf("(owner_id=$%d OR participants @> ARRAY[$%d]::bigint[])", len(args), len(args)))
	}
	if f.RefID > 0 {
		add("reference_id = $%d", f.RefID)
	}
	if f.MediaID > 0 {
		add("media_ids @> ARRAY[$%d]::bigint[]", f.MediaID)
	}
	if f.PublicOnly {
		where = append(where, "public=TRUE")
	}
	if f.State != "" {
		add("state = $%d", f.State)
	}
	if f.KeyPrefix != "" {
		add("strpos(record_key, $%d) = 1", f.KeyPrefix)
	}
	args = append(args, limit)
	query := "SELECT " + mobileColumns + " FROM " + table + " WHERE " + strings.Join(where, " AND ") + fmt.Sprintf(" ORDER BY id LIMIT $%d", len(args))
	rows, err := t.tx.Query(t.ctx, query, args...)
	if err != nil {
		return nil, mobileError(err)
	}
	defer rows.Close()
	result := make([]mobile.Record, 0)
	for rows.Next() {
		r, err := scanMobile(rows)
		if err != nil {
			return nil, err
		}
		result = append(result, r)
	}
	return result, mobileError(rows.Err())
}
func (t *mobileTx) Save(k mobile.Kind, r *mobile.Record) error {
	if !t.writable {
		return mobile.ErrForbidden
	}
	table, err := mobileTable(k)
	if err != nil {
		return err
	}
	r.Participants, r.MediaIDs = mobile.Unique(r.Participants), mobile.Unique(r.MediaIDs)
	var saved mobile.Record
	if r.ID == 0 {
		if r.Version != 0 {
			return mobile.ErrConflict
		}
		saved, err = scanMobile(t.tx.QueryRow(t.ctx, "INSERT INTO "+table+
			" (owner_id,participants,record_key,reference_id,media_ids,public,state,data) VALUES (NULLIF($1,0),$2,$3,NULLIF($4,0),$5,$6,$7,$8) RETURNING "+mobileColumns,
			r.OwnerID, r.Participants, r.Key, r.RefID, r.MediaIDs, r.Public, r.State, []byte(r.Data)))
	} else {
		saved, err = scanMobile(t.tx.QueryRow(t.ctx, "UPDATE "+table+
			" SET owner_id=NULLIF($3,0),participants=$4,record_key=$5,reference_id=NULLIF($6,0),media_ids=$7,public=$8,state=$9,data=$10,version=version+1,updated_at=now() WHERE id=$1 AND version=$2 RETURNING "+mobileColumns,
			r.ID, r.Version, r.OwnerID, r.Participants, r.Key, r.RefID, r.MediaIDs, r.Public, r.State, []byte(r.Data)))
		if errors.Is(err, mobile.ErrNotFound) {
			return mobile.ErrConflict
		}
	}
	if err != nil {
		return err
	}
	*r = saved
	return nil
}

func (t *mobileTx) Count(k mobile.Kind, f mobile.Filter) (int64, error) {
	table, e := mobileTable(k)
	if e != nil {
		return 0, e
	}
	where, args := []string{"id > $1"}, []any{f.After}
	add := func(predicate string, value any) {
		args = append(args, value)
		where = append(where, fmt.Sprintf(predicate, len(args)))
	}
	if f.OwnerID > 0 {
		add("owner_id=$%d", f.OwnerID)
	}
	if f.RefID > 0 {
		add("reference_id=$%d", f.RefID)
	}
	if f.State != "" {
		add("state=$%d", f.State)
	}
	if f.KeyPrefix != "" {
		add("strpos(record_key, $%d) = 1", f.KeyPrefix)
	}
	if f.PublicOnly {
		where = append(where, "public=TRUE")
	}
	if f.ParticipantID > 0 {
		args = append(args, f.ParticipantID)
		where = append(where, fmt.Sprintf("(owner_id=$%d OR participants @> ARRAY[$%d]::bigint[])", len(args), len(args)))
	}
	if f.MediaID > 0 {
		add("media_ids @> ARRAY[$%d]::bigint[]", f.MediaID)
	}
	var count int64
	e = t.tx.QueryRow(t.ctx, "SELECT count(*) FROM "+table+" WHERE "+strings.Join(where, " AND "), args...).Scan(&count)
	return count, mobileError(e)
}
func (t *mobileTx) Delete(k mobile.Kind, id, version int64) error {
	if !t.writable {
		return mobile.ErrForbidden
	}
	table, err := mobileTable(k)
	if err != nil {
		return err
	}
	result, err := t.tx.Exec(t.ctx, "DELETE FROM "+table+" WHERE id=$1 AND version=$2", id, version)
	if err != nil {
		return mobileError(err)
	}
	if result.RowsAffected() != 1 {
		return mobile.ErrConflict
	}
	return nil
}
func (t *mobileTx) User(id int64) (domain.User, error) {
	u, err := scanUser(t.tx.QueryRow(t.ctx, "SELECT "+userCols+" FROM users WHERE id=$1", id))
	return u, mobileError(err)
}
func (t *mobileTx) UserByPhone(phone string) (domain.User, error) {
	if phone == "" {
		return domain.User{}, mobile.ErrNotFound
	}
	u, err := scanUser(t.tx.QueryRow(t.ctx, "SELECT "+userCols+" FROM users WHERE phone=$1", phone))
	return u, mobileError(err)
}
func (t *mobileTx) UserByName(name string) (domain.User, error) {
	u, err := scanUser(t.tx.QueryRow(t.ctx, "SELECT "+userCols+" FROM users WHERE lower(username)=lower($1)", name))
	return u, mobileError(err)
}
func (t *mobileTx) SaveUser(u *domain.User) error {
	if !t.writable {
		return mobile.ErrForbidden
	}
	var saved domain.User
	var err error
	if u.ID == 0 {
		saved, err = scanUser(t.tx.QueryRow(t.ctx, `INSERT INTO users (username,nickname,phone,avatar_url,bio,password_hash,password_salt) VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING `+userCols,
			u.Username, u.Nickname, u.Phone, u.AvatarURL, u.Bio, u.PasswordHash, u.PasswordSalt))
	} else {
		saved, err = scanUser(t.tx.QueryRow(t.ctx, `UPDATE users SET username=$2,nickname=$3,phone=$4,avatar_url=$5,bio=$6,password_hash=$7,password_salt=$8 WHERE id=$1 RETURNING `+userCols,
			u.ID, u.Username, u.Nickname, u.Phone, u.AvatarURL, u.Bio, u.PasswordHash, u.PasswordSalt))
	}
	if err != nil {
		return mobileError(err)
	}
	*u = saved
	return nil
}

// Readiness is called at startup, never to silently substitute volatile storage.
func (s *Store) CheckMobileSchema(ctx context.Context) error {
	var ready bool
	err := s.pool.QueryRow(ctx, "SELECT to_regclass('mobile_accounts') IS NOT NULL AND to_regclass('mobile_outbox') IS NOT NULL AND to_regclass('mobile_qualifications') IS NOT NULL AND to_regclass('mobile_operations') IS NOT NULL AND to_regclass('mobile_data_requests') IS NOT NULL AND to_regclass('mobile_pet_comments') IS NOT NULL AND to_regclass('mobile_comment_reactions') IS NOT NULL").Scan(&ready)
	if err != nil {
		return err
	}
	if !ready {
		return fmt.Errorf("mobile schema missing: apply migrations through 009_pet_discussions.sql")
	}
	return nil
}
