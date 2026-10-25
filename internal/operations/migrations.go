package operations

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"io"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"time"
)

type AppliedMigration struct {
	Name      string
	SHA256    string
	Baseline  bool
	AppliedAt time.Time
}
type Migration struct {
	Name           string     `json:"name"`
	SourceSHA256   string     `json:"source_sha256"`
	RecordedSHA256 string     `json:"recorded_sha256"`
	State          string     `json:"state"`
	Baseline       bool       `json:"baseline"`
	AppliedAt      *time.Time `json:"applied_at,omitempty"`
}

var migrationName = regexp.MustCompile(`^[0-9]{3}_[a-zA-Z0-9_]+\.sql$`)

func (s *Service) Migrations(ctx context.Context) []Migration {
	rows := []Migration{}
	sources := map[string]bool{}
	applied := map[string]AppliedMigration{}
	connected := false
	if s.ReadMigrations != nil {
		if values, e := s.ReadMigrations(ctx); e == nil {
			connected = true
			for _, v := range values {
				applied[v.Name] = v
			}
		}
	}
	entries, _ := os.ReadDir(s.SourceDir)
	for _, entry := range entries {
		if !migrationName.MatchString(entry.Name()) || entry.Type()&os.ModeSymlink != 0 {
			continue
		}
		m := Migration{Name: entry.Name(), State: "unknown"}
		sources[m.Name] = true
		f, e := os.Open(filepath.Join(s.SourceDir, m.Name))
		if e == nil {
			info, e := f.Stat()
			if e == nil && info.Mode().IsRegular() && info.Size() <= 4*1024*1024 {
				b, e := io.ReadAll(io.LimitReader(f, 4*1024*1024+1))
				if e == nil && len(b) <= 4*1024*1024 {
					h := sha256.Sum256(b)
					m.SourceSHA256 = hex.EncodeToString(h[:])
				}
			}
			f.Close()
		}
		if a, ok := applied[m.Name]; ok {
			m.RecordedSHA256, m.Baseline = a.SHA256, a.Baseline
			at := a.AppliedAt
			m.AppliedAt = &at
			m.State = "mismatch"
			if m.SourceSHA256 != "" && m.SourceSHA256 == a.SHA256 {
				m.State = "verified"
				if a.Baseline {
					m.State = "baseline"
				}
			}
		} else if connected {
			m.State = "pending"
		}
		if m.SourceSHA256 == "" {
			m.State = "source_unavailable"
		}
		rows = append(rows, m)
	}
	for name, a := range applied {
		if sources[name] {
			continue
		}
		at := a.AppliedAt
		rows = append(rows, Migration{Name: name, RecordedSHA256: a.SHA256, Baseline: a.Baseline, AppliedAt: &at, State: "source_missing"})
	}
	sort.Slice(rows, func(i, j int) bool { return rows[i].Name < rows[j].Name })
	return rows
}
func ValidEnvironment(name string) bool { return identifier.MatchString(name) }
