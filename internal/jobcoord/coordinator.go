// Package jobcoord coordinates named maintenance batches; business delivery
// leases and provider idempotency keys remain authoritative for outward effects.
package jobcoord

import (
	"context"
	"sync"

	"kitty-circle/internal/mobile"
)

type Batch func(context.Context, mobile.JobInput) (mobile.JobResult, error)
type Coordinator interface {
	Run(context.Context, string, Batch) (mobile.JobResult, error)
}

type Memory struct {
	mu        sync.Mutex
	positions map[string]map[string]mobile.JobPosition
}

func NewMemory() *Memory { return &Memory{positions: map[string]map[string]mobile.JobPosition{}} }
func (m *Memory) Run(ctx context.Context, job string, batch Batch) (mobile.JobResult, error) {
	if !mobile.ValidJob(job) {
		return mobile.JobResult{}, mobile.ErrNotFound
	}
	if !m.mu.TryLock() {
		return mobile.JobResult{State: "idle", RetryAfterSeconds: 5, Positions: map[string]mobile.JobPosition{}}, nil
	}
	defer m.mu.Unlock()
	positions := map[string]mobile.JobPosition{}
	for key, value := range m.positions[job] {
		positions[key] = value
	}
	out, err := batch(ctx, mobile.JobInput{Positions: positions})
	if err == nil {
		m.positions[job] = out.Positions
	}
	return out, err
}
