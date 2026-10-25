package jobcoord

import (
	"context"
	"errors"
	"testing"

	"kitty-circle/internal/mobile"
)

func TestFailedBatchRetainsAuthoritativeCheckpoint(t *testing.T) {
	coordinator := NewMemory()
	ctx := context.Background()
	if _, err := coordinator.Run(ctx, "maintenance", func(context.Context, mobile.JobInput) (mobile.JobResult, error) {
		return mobile.JobResult{Positions: map[string]mobile.JobPosition{"otp": {Cursor: 7}}}, nil
	}); err != nil {
		t.Fatal(err)
	}
	failed := errors.New("batch interrupted")
	if _, err := coordinator.Run(ctx, "maintenance", func(_ context.Context, in mobile.JobInput) (mobile.JobResult, error) {
		if in.Positions["otp"].Cursor != 7 {
			t.Fatal("checkpoint lost")
		}
		return mobile.JobResult{Positions: map[string]mobile.JobPosition{"otp": {Cursor: 99}}}, failed
	}); !errors.Is(err, failed) {
		t.Fatal(err)
	}
	if _, err := coordinator.Run(ctx, "maintenance", func(_ context.Context, in mobile.JobInput) (mobile.JobResult, error) {
		if in.Positions["otp"].Cursor != 7 {
			t.Fatal("failed batch advanced checkpoint")
		}
		return mobile.JobResult{Positions: in.Positions}, nil
	}); err != nil {
		t.Fatal(err)
	}
}
