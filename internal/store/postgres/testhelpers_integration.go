//go:build integration

package postgres

import (
	"context"
	"time"
)

// TruncateAllForTest wipes every table in the schema and restarts the
// identity sequences. Only available when the `integration` build tag is
// set so production binaries never link it.
func (s *Store) TruncateAllForTest(ctx context.Context) error {
	c, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	_, err := s.pool.Exec(c, `
		TRUNCATE TABLE audit_logs, messages, notifications, orders,
		                reports, media, comments, listings, posts, users
		RESTART IDENTITY CASCADE
	`)
	return err
}
