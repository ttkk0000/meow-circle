package mobileapi

import (
	"context"
	"errors"
	"log"
	"net/http"
	"time"

	"kitty-circle/internal/mobile"
	"kitty-circle/internal/operations"
)

// 在已授权作业结束后记录最小观测；遥测失败不把已执行的业务冒充为未执行。
func (h *Handler) observedJob(r *http.Request, op Operator, job string, in mobile.JobInput) (mobile.JobResult, error) {
	start := time.Now()
	out, err := h.runJob(r, op, job, in)
	if h.Operations != nil && h.Operations.Enabled {
		known := false
		for _, name := range operations.Jobs {
			if name == job {
				known = true
				break
			}
		}
		var invalid mobile.ValidationError
		permitted := job == "maintenance" || op.permitsConsumer(job)
		if known && permitted && !errors.Is(err, mobile.ErrForbidden) && !errors.Is(err, mobile.ErrNotFound) && !errors.As(err, &invalid) {
			state, code := out.State, "batch_result"
			if err != nil {
				state, code = "failed", "job_failed"
			}
			if state == "retry" {
				code = "completion_pending"
				if err == nil && out.RetryAfterSeconds == 30 {
					state, code = "continuation", "continuation_pending"
				}
			}
			ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
			defer cancel()
			if e := h.Operations.RecordJob(ctx, operations.Run{Job: job, Operator: op.ID, Outcome: state, ResultCode: code, Processed: out.Processed, DurationMS: time.Since(start).Milliseconds(), FinishedAt: time.Now().UTC()}); e != nil {
				log.Printf("operations job observation unavailable: %T", e)
			}
		}
	}
	return out, err
}
