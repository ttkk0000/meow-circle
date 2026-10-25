package operations

import (
	"context"
	"errors"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
	"time"
)

type PostgresRepository struct{ pool *pgxpool.Pool }
type postgresTx struct {
	ctx      context.Context
	tx       pgx.Tx
	env      string
	writable bool
}

func NewPostgres(pool *pgxpool.Pool) *PostgresRepository { return &PostgresRepository{pool: pool} }
func storageError(e error) error {
	if errors.Is(e, pgx.ErrNoRows) {
		return ErrNotFound
	}
	var p *pgconn.PgError
	if errors.As(e, &p) {
		if p.Code == "40001" || p.Code == "23505" || p.Code == "40P01" {
			return ErrConflict
		}
		if p.Code == "42P01" {
			return ErrUnavailable
		}
	}
	return e
}
func (p *PostgresRepository) transaction(ctx context.Context, env string, write bool, fn func(Tx) error) error {
	options := pgx.TxOptions{IsoLevel: pgx.RepeatableRead, AccessMode: pgx.ReadOnly}
	if write {
		options = pgx.TxOptions{IsoLevel: pgx.Serializable, AccessMode: pgx.ReadWrite}
	}
	tx, e := p.pool.BeginTx(ctx, options)
	if e != nil {
		return storageError(e)
	}
	defer func() {
		c, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		defer cancel()
		_ = tx.Rollback(c)
	}()
	if e = fn(&postgresTx{ctx: ctx, tx: tx, env: env, writable: write}); e != nil {
		return storageError(e)
	}
	return storageError(tx.Commit(ctx))
}
func (p *PostgresRepository) View(ctx context.Context, env string, fn func(Tx) error) error {
	return p.transaction(ctx, env, false, fn)
}
func (p *PostgresRepository) Update(ctx context.Context, env string, fn func(Tx) error) error {
	return p.transaction(ctx, env, true, fn)
}

const recordColumns = "record_id,sequence,version,state,created_at,updated_at,data"

func scanRecord(row pgx.Row) (Record, error) {
	var r Record
	e := row.Scan(&r.ID, &r.Sequence, &r.Version, &r.State, &r.CreatedAt, &r.UpdatedAt, &r.Data)
	return r, storageError(e)
}
func (t *postgresTx) Get(kind, id string) (Record, error) {
	if !validKind(kind) {
		return Record{}, ErrInvalid
	}
	lock := ""
	if t.writable {
		lock = " FOR UPDATE"
	}
	return scanRecord(t.tx.QueryRow(t.ctx, "SELECT "+recordColumns+" FROM mnd_ops_records WHERE environment=$1 AND kind=$2 AND record_id=$3"+lock, t.env, kind, id))
}
func (t *postgresTx) List(kind string, f Filter) ([]Record, error) {
	if !validKind(kind) {
		return nil, ErrInvalid
	}
	rows, e := t.tx.Query(t.ctx, "SELECT "+recordColumns+" FROM mnd_ops_records WHERE environment=$1 AND kind=$2 AND ($3::bigint=0 OR sequence<$3) AND ($4::text='' OR state=$4) AND (NOT $5::boolean OR state<>'resolved') AND ($7::text='' OR COALESCE(data->>'job',data->>'service')=$7) ORDER BY sequence DESC LIMIT $6", t.env, kind, f.Before, f.State, f.OpenOnly, pageLimit(f.Limit), f.Subject)
	if e != nil {
		return nil, storageError(e)
	}
	defer rows.Close()
	out := []Record{}
	for rows.Next() {
		r, e := scanRecord(rows)
		if e != nil {
			return nil, e
		}
		out = append(out, r)
	}
	return out, storageError(rows.Err())
}
func (t *postgresTx) Save(kind string, r *Record) error {
	if !t.writable || !validKind(kind) || r.ID == "" {
		return ErrInvalid
	}
	if r.Version > 0 && (kind == "runs" || kind == "audit" || kind == "commands") {
		return ErrConflict
	}
	var row pgx.Row
	if r.Version == 0 {
		row = t.tx.QueryRow(t.ctx, "INSERT INTO mnd_ops_records(environment,kind,record_id,state,data) VALUES($1,$2,$3,$4,$5) RETURNING "+recordColumns, t.env, kind, r.ID, r.State, r.Data)
	} else {
		row = t.tx.QueryRow(t.ctx, "UPDATE mnd_ops_records SET state=$4,data=$5,version=version+1,updated_at=now() WHERE environment=$1 AND kind=$2 AND record_id=$3 AND version=$6 RETURNING "+recordColumns, t.env, kind, r.ID, r.State, r.Data, r.Version)
	}
	next, e := scanRecord(row)
	if errors.Is(e, ErrNotFound) {
		return ErrConflict
	}
	if e == nil {
		*r = next
	}
	return e
}
