// Package postgres provides a PostgreSQL-backed implementation of the
// store.Store interface using pgx/v5. It is selected automatically when the
// DATABASE_URL environment variable is set.
package postgres

import (
	"context"
	"errors"
	"log"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Store is a pgxpool-backed persistence layer.
type Store struct {
	pool *pgxpool.Pool
}

// New connects to the given DATABASE_URL and returns a Store.
func New(ctx context.Context, dsn string) (*Store, error) {
	cfg, err := pgxpool.ParseConfig(dsn)
	if err != nil {
		return nil, err
	}
	cfg.MaxConns = 20
	cfg.MinConns = 2
	cfg.MaxConnLifetime = 30 * time.Minute

	pool, err := pgxpool.NewWithConfig(ctx, cfg)
	if err != nil {
		return nil, err
	}
	pingCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()
	if err := pool.Ping(pingCtx); err != nil {
		pool.Close()
		return nil, err
	}
	return &Store{pool: pool}, nil
}

// Close releases the underlying pool.
func (s *Store) Close() { s.pool.Close() }

// bg returns a short-lived context for individual queries when no caller
// context is available. The API layer is synchronous so a 10s cap is safe.
func bg() (context.Context, context.CancelFunc) {
	return context.WithTimeout(context.Background(), 10*time.Second)
}

// logErr reports a non-nil, non-NoRows error via the default logger.
func logErr(op string, err error) {
	if err == nil || errors.Is(err, pgx.ErrNoRows) {
		return
	}
	log.Printf("postgres: %s: %v", op, err)
}
