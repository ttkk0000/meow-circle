package cache

import "kitty-circle/internal/mobile"

// Authorisation-dependent aggregates and transactions never enter shared Redis
// caches. Future per-module caches must include viewer and permission version.
func (s *Store) MobileRepository() mobile.Repository { return s.inner.MobileRepository() }
