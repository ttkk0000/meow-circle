package operations

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"
)

var Jobs = []string{"push", "nearby_alerts", "content_updates", "privacy_cleanup", "provider_revocations", "maintenance", "search_projection", "audit_projection"}
var identifier = regexp.MustCompile(`^[a-z][a-z0-9_-]{0,39}$`)
var requestID = regexp.MustCompile(`^[a-zA-Z0-9_-]{16,80}$`)
var evidenceID = regexp.MustCompile(`^[a-zA-Z0-9][a-zA-Z0-9_./:#-]{2,159}$`)
var digest = regexp.MustCompile(`^[a-f0-9]{64}$`)

type Service struct {
	Repo           Repository
	Environment    string
	Enabled        bool
	StoreKind      string
	Origin         string
	Version        string
	SourceDir      string
	Probe          func(context.Context) []Signal
	ReadMigrations func(context.Context) ([]AppliedMigration, error)
	Now            func() time.Time
}
type Signal struct {
	ID         string     `json:"id"`
	Label      string     `json:"label"`
	State      string     `json:"state"`
	Detail     string     `json:"detail"`
	ObservedAt *time.Time `json:"observed_at,omitempty"`
}
type Run struct {
	Job        string    `json:"job"`
	Operator   string    `json:"operator"`
	Outcome    string    `json:"outcome"`
	ResultCode string    `json:"result_code"`
	Processed  int       `json:"processed"`
	DurationMS int64     `json:"duration_ms"`
	FinishedAt time.Time `json:"finished_at"`
}
type Event struct {
	At       time.Time `json:"at"`
	Actor    string    `json:"actor"`
	Action   string    `json:"action"`
	Note     string    `json:"note"`
	Evidence string    `json:"evidence_ref,omitempty"`
}
type Incident struct {
	Title    string  `json:"title"`
	Service  string  `json:"service"`
	Severity string  `json:"severity"`
	Assignee string  `json:"assignee"`
	Events   []Event `json:"events"`
	Evidence string  `json:"evidence_ref"`
}
type Release struct {
	Version        string `json:"version"`
	ArtifactSHA256 string `json:"artifact_sha256"`
	BackupRef      string `json:"backup_ref"`
	ApprovalRef    string `json:"approval_ref"`
	Reason         string `json:"reason"`
	Creator        string `json:"creator"`
}
type Audit struct {
	Actor     string `json:"actor"`
	Action    string `json:"action"`
	Resource  string `json:"resource"`
	Outcome   string `json:"outcome"`
	RequestID string `json:"request_id"`
}
type Check struct {
	ID     string `json:"id"`
	Label  string `json:"label"`
	State  string `json:"state"`
	Detail string `json:"detail"`
}
type Preflight struct {
	Release    Record  `json:"release"`
	Checks     []Check `json:"checks"`
	CanExecute bool    `json:"can_execute"`
}
type Snapshot struct {
	ObservedAt time.Time   `json:"observed_at"`
	Signals    []Signal    `json:"signals"`
	Runs       Page        `json:"runs"`
	Incidents  Page        `json:"incidents"`
	Releases   Page        `json:"releases"`
	Audit      Page        `json:"audit"`
	Migrations []Migration `json:"migrations"`
}
type Command struct {
	RequestID string `json:"request_id"`
	Version   int64  `json:"version,string"`
	Action    string `json:"action"`
	Note      string `json:"note"`
	Evidence  string `json:"evidence_ref"`
}
type IncidentInput struct {
	RequestID string `json:"request_id"`
	Title     string `json:"title"`
	Service   string `json:"service"`
	Severity  string `json:"severity"`
	Evidence  string `json:"evidence_ref"`
}
type ReleaseInput struct {
	RequestID string `json:"request_id"`
	Release
}
type ReleaseUpdateInput struct {
	RequestID     string  `json:"request_id"`
	RecordVersion int64   `json:"record_version,string"`
	Release       Release `json:"release"`
}
type commandReceipt struct {
	Hash   string `json:"hash"`
	Record Record `json:"record"`
}

func New(repo Repository, env string) *Service {
	return &Service{Repo: repo, Environment: env, Enabled: identifier.MatchString(env), SourceDir: "migrations", Now: func() time.Time { return time.Now().UTC() }}
}
func newID() (string, error) {
	var b [16]byte
	_, e := rand.Read(b[:])
	return hex.EncodeToString(b[:]), e
}
func has(values []string, v string) bool {
	for _, s := range values {
		if s == v {
			return true
		}
	}
	return false
}
func text(value string, max int, required bool) bool {
	return utf8.ValidString(value) && utf8.RuneCountInString(value) <= max && (!required || strings.TrimSpace(value) != "") && !strings.ContainsAny(value, "\x00\r")
}
func validEvidence(value string, required bool) bool {
	return value == "" && !required || evidenceID.MatchString(value)
}
func (s *Service) ready() error {
	if s == nil || !s.Enabled || s.Repo == nil {
		return ErrUnavailable
	}
	return nil
}
func (s *Service) List(ctx context.Context, kind string, before int64) (out Page, err error) {
	out.Items = []Record{}
	if e := s.ready(); e != nil {
		return out, e
	}
	if !has([]string{"runs", "incidents", "releases", "audit"}, kind) || before < 0 {
		return out, ErrInvalid
	}
	err = s.Repo.View(ctx, s.Environment, func(tx Tx) error {
		var e error
		out.Items, e = tx.List(kind, Filter{Before: before, Limit: 100})
		return e
	})
	if len(out.Items) == 100 {
		out.NextCursor = strconv.FormatInt(out.Items[99].Sequence, 10)
	}
	return
}
func (s *Service) Get(ctx context.Context, kind, id string) (out Record, err error) {
	if e := s.ready(); e != nil {
		return out, e
	}
	if !has([]string{"incidents", "releases", "runs", "audit"}, kind) || len(id) > 80 {
		return out, ErrInvalid
	}
	err = s.Repo.View(ctx, s.Environment, func(tx Tx) error { var e error; out, e = tx.Get(kind, id); return e })
	return
}
func (s *Service) Snapshot(ctx context.Context) (out Snapshot, err error) {
	if e := s.ready(); e != nil {
		return out, e
	}
	out.ObservedAt = s.Now()
	out.Signals = []Signal{{ID: "api", Label: "API / HTTP", State: "responding", Detail: "当前运维请求已由 API 响应，不代表所有依赖健康。", ObservedAt: &out.ObservedAt}}
	if s.Probe != nil {
		out.Signals = append(out.Signals, s.Probe(ctx)...)
	}
	out.Signals = append(out.Signals, Signal{ID: "caddy", Label: "入口与证书", State: "unknown", Detail: "未接入外部入口或 TLS 到期采集。"})
	for _, item := range []struct {
		kind string
		page *Page
	}{{"runs", &out.Runs}, {"incidents", &out.Incidents}, {"releases", &out.Releases}, {"audit", &out.Audit}} {
		*item.page, err = s.List(ctx, item.kind, 0)
		if err != nil {
			return out, err
		}
	}
	out.Migrations = s.Migrations(ctx)
	return out, nil
}

// 命令回执与业务变化、审计在同一事务；响应丢失后相同请求号只读取原结果。
func (s *Service) command(ctx context.Context, actor, request, action string, input any, fn func(Tx) (Record, error)) (out Record, err error) {
	if e := s.ready(); e != nil {
		return out, e
	}
	if actor == "" || !requestID.MatchString(request) {
		return out, ErrInvalid
	}
	b, e := json.Marshal(input)
	if e != nil {
		return out, e
	}
	hash := sha256.Sum256(append([]byte(action+":"), b...))
	key := sha256.Sum256([]byte(actor + ":" + request))
	receiptID := hex.EncodeToString(key[:])
	expectedHash := hex.EncodeToString(hash[:])
	err = s.Repo.Update(ctx, s.Environment, func(tx Tx) error {
		existing, e := tx.Get("commands", receiptID)
		if e == nil {
			receipt, e := decode[commandReceipt](existing)
			if e != nil {
				return e
			}
			if receipt.Hash != expectedHash {
				return ErrConflict
			}
			out = receipt.Record
			return nil
		}
		if !errors.Is(e, ErrNotFound) {
			return e
		}
		out, e = fn(tx)
		if e != nil {
			return e
		}
		auditID, e := newID()
		if e != nil {
			return e
		}
		a := Record{ID: auditID, State: "recorded"}
		if e = body(&a, Audit{Actor: actor, Action: action, Resource: out.ID, Outcome: out.State, RequestID: request}); e != nil {
			return e
		}
		if e = tx.Save("audit", &a); e != nil {
			return e
		}
		receipt := Record{ID: receiptID, State: "completed"}
		if e = body(&receipt, commandReceipt{Hash: expectedHash, Record: out}); e != nil {
			return e
		}
		return tx.Save("commands", &receipt)
	})
	return
}
func (s *Service) CreateIncident(ctx context.Context, actor string, in IncidentInput) (Record, error) {
	if !text(in.Title, 160, true) || !has(append(append([]string{}, Jobs...), "api", "postgres", "redis", "caddy"), in.Service) || !has([]string{"P1", "P2", "P3"}, in.Severity) || !validEvidence(in.Evidence, false) {
		return Record{}, ErrInvalid
	}
	return s.command(ctx, actor, in.RequestID, "incident.create", in, func(tx Tx) (Record, error) {
		id, e := newID()
		if e != nil {
			return Record{}, e
		}
		r := Record{ID: id, State: "pending"}
		d := Incident{Title: in.Title, Service: in.Service, Severity: in.Severity, Evidence: in.Evidence, Events: []Event{{At: s.Now(), Actor: actor, Action: "created", Note: "人工登记事件", Evidence: in.Evidence}}}
		if e = body(&r, d); e == nil {
			e = tx.Save("incidents", &r)
		}
		return r, e
	})
}
func (s *Service) UpdateIncident(ctx context.Context, actor, id string, in Command) (Record, error) {
	if in.Version < 1 || !text(in.Note, 500, true) || !validEvidence(in.Evidence, false) {
		return Record{}, ErrInvalid
	}
	return s.command(ctx, actor, in.RequestID, "incident."+id+"."+in.Action, in, func(tx Tx) (Record, error) {
		r, e := tx.Get("incidents", id)
		if e != nil {
			return r, e
		}
		if r.Version != in.Version || r.State == "resolved" {
			return r, ErrConflict
		}
		d, e := decode[Incident](r)
		if e != nil {
			return r, e
		}
		if len(d.Events) >= 200 && in.Action == "note" {
			return r, ErrInvalid
		}
		if in.Action == "claim" {
			if r.State != "pending" {
				return r, ErrConflict
			}
			d.Assignee = actor
			r.State = "investigating"
		} else {
			if d.Assignee != actor {
				return r, ErrConflict
			}
			switch in.Action {
			case "note":
			case "observe":
				if r.State != "investigating" || !validEvidence(in.Evidence, true) {
					return r, ErrInvalid
				}
				r.State = "observing"
			case "resolve":
				if r.State != "observing" || !validEvidence(in.Evidence, true) {
					return r, ErrInvalid
				}
				if has(Jobs, d.Service) {
					runs, e := tx.List("runs", Filter{Limit: 200, Subject: d.Service})
					if e != nil {
						return r, e
					}
					confirmed := false
					for _, run := range runs {
						v, e := decode[Run](run)
						if e != nil {
							return r, e
						}
						if v.Job == d.Service {
							if v.Outcome == "idle" || v.Outcome == "continuation" {
								continue
							}
							confirmed = (v.Outcome == "acknowledged" || v.Outcome == "batch_complete" && v.Processed > 0) && v.FinishedAt.After(r.CreatedAt)
							break
						}
					}
					if !confirmed {
						return r, ErrConflict
					}
				}
				r.State = "resolved"
			default:
				return r, ErrInvalid
			}
		}
		d.Events = append(d.Events, Event{At: s.Now(), Actor: actor, Action: in.Action, Note: in.Note, Evidence: in.Evidence})
		if in.Evidence != "" {
			d.Evidence = in.Evidence
		}
		if e = body(&r, d); e == nil {
			e = tx.Save("incidents", &r)
		}
		return r, e
	})
}
func (s *Service) CreateRelease(ctx context.Context, actor string, in ReleaseInput) (Record, error) {
	if !text(in.Version, 80, true) || !text(in.Reason, 500, true) || in.ArtifactSHA256 != "" && !digest.MatchString(in.ArtifactSHA256) || !validEvidence(in.BackupRef, false) || !validEvidence(in.ApprovalRef, false) {
		return Record{}, ErrInvalid
	}
	in.Creator = actor
	return s.command(ctx, actor, in.RequestID, "release.prepare", in, func(tx Tx) (Record, error) {
		id, e := newID()
		if e != nil {
			return Record{}, e
		}
		r := Record{ID: id, State: "blocked"}
		if e = body(&r, in.Release); e == nil {
			e = tx.Save("releases", &r)
		}
		return r, e
	})
}
func (s *Service) UpdateRelease(ctx context.Context, actor, id string, in ReleaseUpdateInput) (Record, error) {
	v := in.Release
	if in.RecordVersion < 1 || !text(v.Version, 80, true) || !text(v.Reason, 500, true) || v.ArtifactSHA256 != "" && !digest.MatchString(v.ArtifactSHA256) || !validEvidence(v.BackupRef, false) || !validEvidence(v.ApprovalRef, false) {
		return Record{}, ErrInvalid
	}
	in.Release.Creator = ""
	return s.command(ctx, actor, in.RequestID, "release.update."+id, in, func(tx Tx) (Record, error) {
		r, e := tx.Get("releases", id)
		if e != nil {
			return r, e
		}
		if r.Version != in.RecordVersion || r.State != "blocked" {
			return r, ErrConflict
		}
		previous, e := decode[Release](r)
		if e != nil {
			return r, e
		}
		v.Creator = previous.Creator
		if e = body(&r, v); e == nil {
			e = tx.Save("releases", &r)
		}
		return r, e
	})
}
func (s *Service) Preflight(ctx context.Context, id string) (out Preflight, err error) {
	out.Release, err = s.Get(ctx, "releases", id)
	if err != nil {
		return
	}
	r, e := decode[Release](out.Release)
	if e != nil {
		return out, e
	}
	out.Checks = []Check{{ID: "environment", Label: "目标环境与配置", State: "verified", Detail: s.Environment + " / 当前服务端固定环境"}, {ID: "artifact", Label: "构件与迁移计划", State: "missing", Detail: "需要真实不可变构件摘要与来源校验"}, {ID: "recovery", Label: "恢复方案与审批", State: "missing", Detail: "需要外部备份／恢复及审批证据验证"}, {ID: "executor", Label: "发布执行后端", State: "unavailable", Detail: "未接入发布执行器；即使填写引用也不会自动放行"}}
	if digest.MatchString(r.ArtifactSHA256) {
		out.Checks[1].State = "reference_only"
		out.Checks[1].Detail = "摘要已填写；尚未连接构件仓库验证"
	}
	if r.BackupRef != "" && r.ApprovalRef != "" {
		out.Checks[2].State = "reference_only"
		out.Checks[2].Detail = "引用已记录；尚未连接外部证据验证"
	}
	return
}

// 仅记录已授权任务的结果码与计数；不保存供应方正文、Token、游标或消费者资料。
func (s *Service) RecordJob(ctx context.Context, run Run) error {
	if e := s.ready(); e != nil {
		return e
	}
	if !has(Jobs, run.Job) || !has([]string{"idle", "acknowledged", "retry", "continuation", "batch_complete", "failed"}, run.Outcome) || !text(run.ResultCode, 80, true) {
		return ErrInvalid
	}
	return s.Repo.Update(ctx, s.Environment, func(tx Tx) error {
		id, e := newID()
		if e != nil {
			return e
		}
		r := Record{ID: id, State: run.Outcome}
		if e = body(&r, run); e != nil {
			return e
		}
		if e = tx.Save("runs", &r); e != nil {
			return e
		}
		if run.Outcome != "failed" && run.Outcome != "retry" {
			return nil
		}
		open, e := tx.List("incidents", Filter{OpenOnly: true, Subject: run.Job, Limit: 1})
		if e != nil {
			return e
		}
		for _, item := range open {
			d, e := decode[Incident](item)
			if e != nil {
				return e
			}
			if d.Service == run.Job {
				return nil
			}
		}
		incidentID, e := newID()
		if e != nil {
			return e
		}
		incident := Record{ID: incidentID, State: "pending"}
		if e = body(&incident, Incident{Title: fmt.Sprintf("%s 任务未完成", run.Job), Service: run.Job, Severity: "P2", Events: []Event{{At: s.Now(), Actor: "system", Action: "triggered", Note: "任务执行返回失败或待重试结果", Evidence: "run:" + id}}, Evidence: "run:" + id}); e != nil {
			return e
		}
		return tx.Save("incidents", &incident)
	})
}
