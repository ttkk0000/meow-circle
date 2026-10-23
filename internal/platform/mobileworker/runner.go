// Package mobileworker drives only the named v2 jobs API. It stores cursors,
// never operator tokens, provider receipts, message contents or user material.
package mobileworker

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"time"

	"kitty-circle/internal/mobile"
)

type Runner struct {
	endpoint, token, target string
	client                  *http.Client
}

type Checkpoint struct {
	Schema    int                           `json:"schema"`
	Target    string                        `json:"target"`
	Positions map[string]mobile.JobPosition `json:"positions"`
}

func New(apiURL, token, job string, allowLoopbackHTTP bool) (*Runner, error) {
	if !mobile.HasString([]string{"push", "nearby_alerts", "privacy_cleanup", "provider_revocations", "maintenance"}, job) || len(token) < 32 {
		return nil, errors.New("valid worker job and operator token are required")
	}
	u, e := url.Parse(apiURL)
	if e != nil || u.User != nil || u.RawQuery != "" || u.Fragment != "" || u.Host == "" || strings.TrimRight(u.Path, "/") != "/api/v2" {
		return nil, errors.New("worker API URL must identify /api/v2 without credentials or query")
	}
	ip := net.ParseIP(u.Hostname())
	loopback := strings.EqualFold(u.Hostname(), "localhost") || ip != nil && ip.IsLoopback()
	if u.Scheme != "https" && !(allowLoopbackHTTP && u.Scheme == "http" && loopback) {
		return nil, errors.New("worker requires HTTPS; explicit loopback HTTP is development-only")
	}
	u.Path = strings.TrimRight(u.Path, "/") + "/ops/jobs/" + job
	hash := sha256.Sum256([]byte(u.String()))
	return &Runner{endpoint: u.String(), token: token, target: hex.EncodeToString(hash[:]), client: &http.Client{
		Timeout:       30 * time.Second,
		CheckRedirect: func(*http.Request, []*http.Request) error { return errors.New("worker redirects are disabled") },
	}}, nil
}

func (r *Runner) Tick(ctx context.Context, checkpoint Checkpoint) (out mobile.JobResult, err error) {
	if checkpoint.Schema != 1 || checkpoint.Target != r.target {
		return out, errors.New("worker checkpoint belongs to another API or schema")
	}
	b, err := json.Marshal(mobile.JobInput{Positions: checkpoint.Positions})
	if err != nil {
		return out, err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, r.endpoint, bytes.NewReader(b))
	if err != nil {
		return out, errors.New("invalid worker request")
	}
	req.Header.Set("X-Operator-Token", r.token)
	req.Header.Set("Content-Type", "application/json")
	response, err := r.client.Do(req)
	if err != nil {
		return out, errors.New("worker API request failed; checkpoint retained")
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return out, fmt.Errorf("worker API returned HTTP %d; checkpoint retained", response.StatusCode)
	}
	var envelope struct {
		OK   bool             `json:"ok"`
		Data mobile.JobResult `json:"data"`
	}
	body, e := io.ReadAll(io.LimitReader(response.Body, (1<<20)+1))
	if e != nil || len(body) > 1<<20 {
		return out, errors.New("worker response exceeds its limit; checkpoint retained")
	}
	decoder := json.NewDecoder(bytes.NewReader(body))
	var extra any
	if e := decoder.Decode(&envelope); e != nil || !envelope.OK {
		return out, errors.New("invalid worker response; checkpoint retained")
	}
	if decoder.Decode(&extra) != io.EOF {
		return out, errors.New("invalid worker response; checkpoint retained")
	}
	if !mobile.HasString([]string{"idle", "retry", "acknowledged", "batch_complete"}, envelope.Data.State) || envelope.Data.Processed < 0 || envelope.Data.RetryAfterSeconds < 0 || envelope.Data.RetryAfterSeconds > 86400 || envelope.Data.Positions == nil {
		return out, errors.New("unknown worker state; checkpoint retained")
	}
	for _, position := range envelope.Data.Positions {
		if position.Cursor < 0 || len(position.After) > 250 {
			return out, errors.New("invalid worker continuation; checkpoint retained")
		}
	}
	return envelope.Data, nil
}

func (r *Runner) Load(path string) (out Checkpoint, err error) {
	out = Checkpoint{Schema: 1, Target: r.target, Positions: map[string]mobile.JobPosition{}}
	info, err := os.Lstat(path)
	if errors.Is(err, os.ErrNotExist) {
		return out, nil
	}
	if err != nil {
		return out, err
	}
	if !info.Mode().IsRegular() || info.Size() > 1<<20 {
		return out, errors.New("invalid worker checkpoint file")
	}
	b, err := os.ReadFile(path)
	if err != nil {
		return out, err
	}
	if len(b) > 1<<20 || json.Unmarshal(b, &out) != nil || out.Schema != 1 || out.Target != r.target || out.Positions == nil {
		return out, errors.New("invalid or mismatched worker checkpoint; manual review required")
	}
	for _, position := range out.Positions {
		if position.Cursor < 0 || len(position.After) > 250 {
			return out, errors.New("invalid worker continuation; manual review required")
		}
	}
	return out, nil
}

func (r *Runner) Save(path string, result mobile.JobResult) error {
	b, err := json.Marshal(Checkpoint{Schema: 1, Target: r.target, Positions: result.Positions})
	if err != nil {
		return err
	}
	dir := filepath.Dir(path)
	if err = os.MkdirAll(dir, 0700); err != nil {
		return err
	}
	f, err := os.CreateTemp(dir, ".worker-checkpoint-*")
	if err != nil {
		return err
	}
	defer os.Remove(f.Name())
	if _, err = f.Write(b); err == nil {
		err = f.Sync()
	}
	if closeErr := f.Close(); err == nil {
		err = closeErr
	}
	if err != nil {
		return err
	}
	return os.Rename(f.Name(), path)
}
