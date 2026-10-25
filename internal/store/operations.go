package store

import (
	"context"
	"kitty-circle/internal/operations"
)

func (s *MemoryStore) OperationsRepository() operations.Repository {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.operations == nil {
		s.operations = operations.NewMemory()
	}
	return s.operations
}
func (s *MemoryStore) OperationsSignals(context.Context) []operations.Signal {
	return []operations.Signal{{ID: "postgres", Label: "Postgres", State: "not_configured", Detail: "当前为开发用内存存储，不代表 PostgreSQL 健康。"}, {ID: "redis", Label: "Redis", State: "unknown", Detail: "未接入 Redis 探针。"}}
}
