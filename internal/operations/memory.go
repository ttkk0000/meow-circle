package operations

import (
	"context"
	"encoding/json"
	"sort"
	"sync"
	"time"
)

// MemoryRepository 只供显式开发环境；与生产持久化使用相同版本与事务语义。
type MemoryRepository struct {
	mu       sync.Mutex
	rows     map[string]map[string]map[string]Record
	sequence int64
}
type memoryTx struct {
	rows     map[string]map[string]Record
	sequence *int64
	writable bool
}

func NewMemory() *MemoryRepository {
	return &MemoryRepository{rows: map[string]map[string]map[string]Record{}}
}
func cloneRecord(r Record) Record { r.Data = append([]byte(nil), r.Data...); return r }
func (m *MemoryRepository) transaction(ctx context.Context, env string, writable bool, fn func(Tx) error) error {
	if e := ctx.Err(); e != nil {
		return e
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	rows := map[string]map[string]Record{}
	for kind, items := range m.rows[env] {
		rows[kind] = map[string]Record{}
		for id, r := range items {
			rows[kind][id] = cloneRecord(r)
		}
	}
	seq := m.sequence
	t := &memoryTx{rows: rows, sequence: &seq, writable: writable}
	if e := fn(t); e != nil {
		return e
	}
	if e := ctx.Err(); e != nil {
		return e
	}
	if writable {
		m.rows[env], m.sequence = rows, seq
	}
	return nil
}
func (m *MemoryRepository) View(ctx context.Context, env string, fn func(Tx) error) error {
	return m.transaction(ctx, env, false, fn)
}
func (m *MemoryRepository) Update(ctx context.Context, env string, fn func(Tx) error) error {
	return m.transaction(ctx, env, true, fn)
}
func (t *memoryTx) Get(kind, id string) (Record, error) {
	if !validKind(kind) {
		return Record{}, ErrInvalid
	}
	r, ok := t.rows[kind][id]
	if !ok {
		return Record{}, ErrNotFound
	}
	return cloneRecord(r), nil
}
func (t *memoryTx) List(kind string, f Filter) ([]Record, error) {
	if !validKind(kind) {
		return nil, ErrInvalid
	}
	out := []Record{}
	for _, r := range t.rows[kind] {
		if f.Subject != "" {
			var data struct {
				Job     string `json:"job"`
				Service string `json:"service"`
			}
			if json.Unmarshal(r.Data, &data) != nil || data.Job != f.Subject && data.Service != f.Subject {
				continue
			}
		}
		if f.Before > 0 && r.Sequence >= f.Before || f.State != "" && r.State != f.State || f.OpenOnly && r.State == "resolved" {
			continue
		}
		out = append(out, cloneRecord(r))
	}
	sort.Slice(out, func(i, j int) bool { return out[i].Sequence > out[j].Sequence })
	if len(out) > pageLimit(f.Limit) {
		out = out[:pageLimit(f.Limit)]
	}
	return out, nil
}
func (t *memoryTx) Save(kind string, r *Record) error {
	if !t.writable || !validKind(kind) || r.ID == "" {
		return ErrInvalid
	}
	if t.rows[kind] == nil {
		t.rows[kind] = map[string]Record{}
	}
	previous, exists := t.rows[kind][r.ID]
	if exists && previous.Version != r.Version || !exists && r.Version != 0 || exists && (kind == "audit" || kind == "commands" || kind == "runs") {
		return ErrConflict
	}
	now := time.Now().UTC()
	if !exists {
		*t.sequence = *t.sequence + 1
		r.Sequence = *t.sequence
		r.CreatedAt = now
	} else {
		r.Sequence, r.CreatedAt = previous.Sequence, previous.CreatedAt
	}
	r.UpdatedAt = now
	r.Version++
	t.rows[kind][r.ID] = cloneRecord(*r)
	return nil
}
