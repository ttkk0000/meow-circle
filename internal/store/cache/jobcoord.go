package cache

import "kitty-circle/internal/jobcoord"

func (s *Store) JobCoordinator() jobcoord.Coordinator {
	if p, ok := s.inner.(interface{ JobCoordinator() jobcoord.Coordinator }); ok {
		return p.JobCoordinator()
	}
	return nil
}
