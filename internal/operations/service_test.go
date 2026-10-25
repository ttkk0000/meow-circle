package operations

import (
	"context"
	"errors"
	"testing"
	"time"
)

// 回归源码：验证响应丢失后重放不会重复创建，且请求号不能换内容复用。
func TestCommandReplayAndEnvironmentIsolation(t *testing.T) {
	ctx := context.Background()
	repo := NewMemory()
	s := New(repo, "staging")
	in := IncidentInput{RequestID: "request-incident-0001", Title: "任务异常", Service: "push", Severity: "P2"}
	first, e := s.CreateIncident(ctx, "alice", in)
	if e != nil {
		t.Fatal(e)
	}
	again, e := s.CreateIncident(ctx, "alice", in)
	if e != nil || first.ID != again.ID || first.Version != again.Version {
		t.Fatalf("replay changed result: %v", e)
	}
	page, e := s.List(ctx, "audit", 0)
	if e != nil || len(page.Items) != 1 {
		t.Fatalf("duplicate audit: %v", e)
	}
	in.Title = "替换原请求内容"
	if _, e = s.CreateIncident(ctx, "alice", in); !errors.Is(e, ErrConflict) {
		t.Fatalf("reused key accepted: %v", e)
	}
	other := New(repo, "production")
	if _, e = other.Get(ctx, "incidents", first.ID); !errors.Is(e, ErrNotFound) {
		t.Fatalf("cross environment read: %v", e)
	}
}
func TestIncidentCannotCloseOnIdleOrContinuation(t *testing.T) {
	ctx := context.Background()
	s := New(NewMemory(), "staging")
	r, e := s.CreateIncident(ctx, "alice", IncidentInput{RequestID: "request-incident-0002", Title: "撤销任务异常", Service: "provider_revocations", Severity: "P2"})
	if e != nil {
		t.Fatal(e)
	}
	r, e = s.UpdateIncident(ctx, "alice", r.ID, Command{RequestID: "request-claim-000001", Version: r.Version, Action: "claim", Note: "接手核对"})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.UpdateIncident(ctx, "bob", r.ID, Command{RequestID: "request-note-0000001", Version: r.Version, Action: "note", Note: "不能代替负责人"}); !errors.Is(e, ErrConflict) {
		t.Fatalf("assignee bypass: %v", e)
	}
	r, e = s.UpdateIncident(ctx, "alice", r.ID, Command{RequestID: "request-observe-0001", Version: r.Version, Action: "observe", Note: "缓解后观察", Evidence: "TICKET-001"})
	if e != nil {
		t.Fatal(e)
	}
	for _, outcome := range []string{"idle", "continuation"} {
		if e = s.RecordJob(ctx, Run{Job: "provider_revocations", Operator: "worker", Outcome: outcome, ResultCode: "batch_result", FinishedAt: r.CreatedAt.Add(time.Second)}); e != nil {
			t.Fatal(e)
		}
	}
	command := Command{RequestID: "request-resolve-0001", Version: r.Version, Action: "resolve", Note: "验证恢复", Evidence: "TICKET-002"}
	if _, e = s.UpdateIncident(ctx, "alice", r.ID, command); !errors.Is(e, ErrConflict) {
		t.Fatalf("idle incorrectly closed incident: %v", e)
	}
	if e = s.RecordJob(ctx, Run{Job: "provider_revocations", Operator: "worker", Outcome: "batch_complete", Processed: 1, ResultCode: "batch_result", FinishedAt: r.CreatedAt.Add(2 * time.Second)}); e != nil {
		t.Fatal(e)
	}
	if r, e = s.UpdateIncident(ctx, "alice", r.ID, command); e != nil || r.State != "resolved" {
		t.Fatalf("confirmed recovery rejected: %v", e)
	}
}
func TestReleaseReferencesNeverEnableDeployment(t *testing.T) {
	ctx := context.Background()
	s := New(NewMemory(), "staging")
	in := ReleaseInput{RequestID: "request-release-0001", Release: Release{Version: "v1", Reason: "发布准备"}}
	r, e := s.CreateRelease(ctx, "alice", in)
	if e != nil {
		t.Fatal(e)
	}
	v := in.Release
	v.ArtifactSHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
	v.BackupRef = "BACKUP-001"
	v.ApprovalRef = "APPROVAL-001"
	r, e = s.UpdateRelease(ctx, "bob", r.ID, ReleaseUpdateInput{RequestID: "request-release-0002", RecordVersion: r.Version, Release: v})
	if e != nil {
		t.Fatal(e)
	}
	check, e := s.Preflight(ctx, r.ID)
	if e != nil || check.CanExecute || check.Release.State != "blocked" {
		t.Fatalf("references granted execution: %v", e)
	}
	stored, e := decode[Release](r)
	if e != nil || stored.Creator != "alice" {
		t.Fatalf("creator was overwritten: %v", e)
	}
}
