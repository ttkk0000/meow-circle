package postgres

import (
	"context"
	"kitty-circle/internal/operations"
	"time"
)

func (s *Store) OperationsRepository() operations.Repository { return operations.NewPostgres(s.pool) }
func (s *Store) OperationsSignals(ctx context.Context) []operations.Signal {
	c, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	at := time.Now().UTC()
	state, detail := "reachable", "连接探针成功；不等同于容量、复制或全部业务健康。"
	if e := s.pool.Ping(c); e != nil {
		state, detail = "unreachable", "数据库连接探针失败；请核对网络与服务。"
	}
	return []operations.Signal{{ID: "postgres", Label: "Postgres", State: state, Detail: detail, ObservedAt: &at}, {ID: "redis", Label: "Redis", State: "unknown", Detail: "当前服务未接入 Redis 探针。"}}
}
func (s *Store) OperationsMigrations(ctx context.Context) ([]operations.AppliedMigration, error) {
	rows, e := s.pool.Query(ctx, "SELECT name,sha256,baseline,applied_at FROM public.mnd_schema_migrations ORDER BY name")
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []operations.AppliedMigration{}
	for rows.Next() {
		var v operations.AppliedMigration
		if e = rows.Scan(&v.Name, &v.SHA256, &v.Baseline, &v.AppliedAt); e != nil {
			return nil, e
		}
		out = append(out, v)
	}
	return out, rows.Err()
}
