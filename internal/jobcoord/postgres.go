package jobcoord

import (
	"context"
	"crypto/sha256"
	"encoding/binary"
	"encoding/json"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"kitty-circle/internal/mobile"
)

type Postgres struct{ Pool *pgxpool.Pool }
type result struct {
	out mobile.JobResult
	err error
}

func (p *Postgres) Run(ctx context.Context, job string, batch Batch) (out mobile.JobResult, err error) {
	if !mobile.ValidJob(job) {
		return out, mobile.ErrNotFound
	}
	// 全局锁使用独立会话，外部网关等待不占用业务连接池。
	conn, err := pgx.ConnectConfig(ctx, p.Pool.Config().ConnConfig.Copy())
	if err != nil {
		return out, err
	}
	defer func() {
		cleanup, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		defer cancel()
		_ = conn.Close(cleanup)
	}()
	digest := sha256.Sum256([]byte("M&D worker batch v1:" + job))
	key := int64(binary.BigEndian.Uint64(digest[:8]))
	var locked bool
	if err = conn.QueryRow(ctx, "SELECT pg_try_advisory_lock($1)", key).Scan(&locked); err != nil {
		return out, err
	}
	if !locked {
		return mobile.JobResult{State: "idle", RetryAfterSeconds: 5, Positions: map[string]mobile.JobPosition{}}, nil
	}
	defer func() {
		cleanup, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		defer cancel()
		var released bool
		if e := conn.QueryRow(cleanup, "SELECT pg_advisory_unlock($1)", key).Scan(&released); e != nil || !released {
			// 无法确认解锁的会话直接关闭，不复用。
			_ = conn.Close(cleanup)
		}
	}()
	var raw []byte
	err = conn.QueryRow(ctx, "SELECT positions FROM mnd_worker_checkpoints WHERE job=$1", job).Scan(&raw)
	positions := map[string]mobile.JobPosition{}
	if err != nil && !errors.Is(err, pgx.ErrNoRows) {
		return out, err
	}
	if len(raw) > 0 && (len(raw) > 8192 || json.Unmarshal(raw, &positions) != nil || positions == nil) {
		return out, mobile.ErrUnavailable
	}
	work, cancel := context.WithCancel(ctx)
	defer cancel()
	completed := make(chan result, 1)
	go func() { value, e := batch(work, mobile.JobInput{Positions: positions}); completed <- result{value, e} }()
	heartbeat := time.NewTicker(3 * time.Second)
	defer heartbeat.Stop()
	for {
		select {
		case <-ctx.Done():
			cancel()
			<-completed // 外部端口均接收ctx；不在批次仍执行时主动释放作业锁。
			return out, ctx.Err()
		case <-heartbeat.C:
			check, stop := context.WithTimeout(ctx, 2*time.Second)
			e := conn.Ping(check)
			stop()
			if e != nil {
				cancel()
				<-completed
				return out, mobile.ErrUnavailable
			}
		case done := <-completed:
			if done.err != nil {
				return done.out, done.err
			}
			raw, err = json.Marshal(done.out.Positions)
			if err != nil || len(raw) > 8192 || done.out.Positions == nil {
				return out, mobile.ErrUnavailable
			}
			_, err = conn.Exec(ctx, `INSERT INTO mnd_worker_checkpoints(job,positions) VALUES($1,$2)
			 ON CONFLICT(job) DO UPDATE SET positions=excluded.positions,version=mnd_worker_checkpoints.version+1,updated_at=now()`, job, raw)
			return done.out, err
		}
	}
}
