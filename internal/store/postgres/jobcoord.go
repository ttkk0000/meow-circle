package postgres

import "kitty-circle/internal/jobcoord"

func (s *Store) JobCoordinator() jobcoord.Coordinator { return &jobcoord.Postgres{Pool: s.pool} }
