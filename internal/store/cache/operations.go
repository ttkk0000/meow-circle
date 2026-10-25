package cache

import (
	"context"
	"kitty-circle/internal/operations"
	"time"
)

// 运维授权与运行快照不经过共享缓存，Redis自身只做有界连接探针。
func (s *Store) OperationsRepository() operations.Repository {
	if p, ok := s.inner.(interface{ OperationsRepository() operations.Repository }); ok {
		return p.OperationsRepository()
	}
	return nil
}
func (s *Store) OperationsMigrations(ctx context.Context) ([]operations.AppliedMigration, error) {
	if p, ok := s.inner.(interface {
		OperationsMigrations(context.Context) ([]operations.AppliedMigration, error)
	}); ok {
		return p.OperationsMigrations(ctx)
	}
	return nil, operations.ErrUnavailable
}
func (s *Store) OperationsSignals(ctx context.Context) []operations.Signal {
	out := []operations.Signal{}
	if p, ok := s.inner.(interface {
		OperationsSignals(context.Context) []operations.Signal
	}); ok {
		for _, v := range p.OperationsSignals(ctx) {
			if v.ID != "redis" {
				out = append(out, v)
			}
		}
	}
	c, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	at := time.Now().UTC()
	state, detail := "reachable", "连接探针成功；不代表队列、内存或副本均正常。"
	if e := s.rdb.Ping(c).Err(); e != nil {
		state, detail = "unreachable", "Redis 连接探针失败。"
	}
	return append(out, operations.Signal{ID: "redis", Label: "Redis", State: state, Detail: detail, ObservedAt: &at})
}
