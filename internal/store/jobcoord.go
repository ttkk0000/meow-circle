package store

import "kitty-circle/internal/jobcoord"

// 内存存储仅供单进程开发，不能据此宣称跨副本协调。
func (s *MemoryStore) JobCoordinator() jobcoord.Coordinator { return jobcoord.NewMemory() }
