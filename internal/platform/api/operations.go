package api

import (
	"context"
	"fmt"
	"kitty-circle/internal/jobcoord"
	"kitty-circle/internal/operations"
	"kitty-circle/internal/platform/opsapi"
	"os"
	"path/filepath"
)

func (r *Router) configureOperations() error {
	if p, ok := r.store.(interface{ JobCoordinator() jobcoord.Coordinator }); ok {
		r.mobileAPI.Jobs = p.JobCoordinator()
	}
	env := os.Getenv("MND_OPS_ENVIRONMENT")
	if env != "" && !operations.ValidEnvironment(env) {
		return fmt.Errorf("invalid MND_OPS_ENVIRONMENT")
	}
	var repo operations.Repository
	if p, ok := r.store.(interface{ OperationsRepository() operations.Repository }); ok {
		repo = p.OperationsRepository()
	}
	s := operations.New(repo, env)
	s.StoreKind = r.storeKind
	s.Origin = r.browserAPI.Origin
	s.Version = os.Getenv("MND_RELEASE_VERSION")
	s.SourceDir = filepath.Join(getWorkingDir(), "migrations")
	if os.Getenv("MND_CONFIG_MODE") == "placeholder" {
		s.Enabled = false
	}
	if p, ok := r.store.(interface {
		OperationsSignals(context.Context) []operations.Signal
	}); ok {
		s.Probe = p.OperationsSignals
	}
	if p, ok := r.store.(interface {
		OperationsMigrations(context.Context) ([]operations.AppliedMigration, error)
	}); ok {
		s.ReadMigrations = p.OperationsMigrations
	}
	r.operationsAPI = &opsapi.Handler{Service: s, Operators: r.mobileAPI.Operators, Origin: s.Origin}
	r.mobileAPI.Operations = s
	return nil
}
