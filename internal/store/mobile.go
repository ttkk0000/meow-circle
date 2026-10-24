package store

import (
	"context"
	"encoding/json"
	"sort"
	"strings"
	"time"

	"kitty-circle/internal/domain"
	"kitty-circle/internal/mobile"
)

type memoryMobile struct{ store *MemoryStore }
type memoryMobileTx struct {
	records  map[mobile.Kind]map[int64]mobile.Record
	users    map[int64]domain.User
	seq      int64
	userSeq  int64
	writable bool
}

func (s *MemoryStore) MobileRepository() mobile.Repository { return &memoryMobile{s} }

func (m *memoryMobile) View(ctx context.Context, fn func(mobile.Tx) error) error {
	if err := ctx.Err(); err != nil {
		return err
	}
	m.store.mu.RLock()
	defer m.store.mu.RUnlock()
	return fn(&memoryMobileTx{records: m.store.mobileRecords, users: m.store.users})
}

func (m *memoryMobile) Update(ctx context.Context, fn func(mobile.Tx) error) error {
	if err := ctx.Err(); err != nil {
		return err
	}
	s := m.store
	s.mu.Lock()
	defer s.mu.Unlock()
	tx := &memoryMobileTx{records: make(map[mobile.Kind]map[int64]mobile.Record),
		users: make(map[int64]domain.User), seq: s.mobileSeq, userSeq: s.userSeq, writable: true}
	for kind, rows := range s.mobileRecords {
		tx.records[kind] = make(map[int64]mobile.Record, len(rows))
		for id, row := range rows {
			tx.records[kind][id] = copyMobileRecord(row)
		}
	}
	for id, user := range s.users {
		tx.users[id] = user
	}
	if err := fn(tx); err != nil {
		return err
	}
	if err := ctx.Err(); err != nil {
		return err
	}
	s.mobileRecords, s.mobileSeq, s.users, s.userSeq = tx.records, tx.seq, tx.users, tx.userSeq
	s.usersByName = make(map[string]int64, len(s.users))
	s.usersByPhone = make(map[string]int64)
	for id, user := range s.users {
		s.usersByName[user.Username] = id
		if user.Phone != "" {
			s.usersByPhone[user.Phone] = id
		}
	}
	return nil
}

func copyMobileRecord(r mobile.Record) mobile.Record {
	r.Data = append(json.RawMessage(nil), r.Data...)
	r.Participants = append([]int64(nil), r.Participants...)
	r.MediaIDs = append([]int64(nil), r.MediaIDs...)
	return r
}

func (t *memoryMobileTx) Get(kind mobile.Kind, id int64) (mobile.Record, error) {
	if !kind.Valid() {
		return mobile.Record{}, mobile.Invalid("invalid record kind")
	}
	if r, ok := t.records[kind][id]; ok {
		return copyMobileRecord(r), nil
	}
	return mobile.Record{}, mobile.ErrNotFound
}

func (t *memoryMobileTx) ByKey(kind mobile.Kind, key string) (mobile.Record, error) {
	if key == "" {
		return mobile.Record{}, mobile.ErrNotFound
	}
	for _, r := range t.records[kind] {
		if r.Key == key {
			return copyMobileRecord(r), nil
		}
	}
	return mobile.Record{}, mobile.ErrNotFound
}

func (t *memoryMobileTx) List(kind mobile.Kind, f mobile.Filter) ([]mobile.Record, error) {
	if !kind.Valid() {
		return nil, mobile.Invalid("invalid record kind")
	}
	limit := f.Limit
	if limit <= 0 || limit > 500 {
		limit = 100
	}
	rows := make([]mobile.Record, 0)
	for _, r := range t.records[kind] {
		if r.ID <= f.After || f.OwnerID > 0 && r.OwnerID != f.OwnerID ||
			f.ParticipantID > 0 && r.OwnerID != f.ParticipantID && !mobile.Has(r.Participants, f.ParticipantID) ||
			f.RefID > 0 && r.RefID != f.RefID || f.MediaID > 0 && !mobile.Has(r.MediaIDs, f.MediaID) ||
			f.PublicOnly && !r.Public || f.State != "" && r.State != f.State || f.KeyPrefix != "" && !strings.HasPrefix(r.Key, f.KeyPrefix) {
			continue
		}
		rows = append(rows, copyMobileRecord(r))
	}
	sort.Slice(rows, func(i, j int) bool { return rows[i].ID < rows[j].ID })
	if len(rows) > limit {
		rows = rows[:limit]
	}
	return rows, nil
}

func (t *memoryMobileTx) Save(kind mobile.Kind, r *mobile.Record) error {
	if !t.writable {
		return mobile.ErrForbidden
	}
	if !kind.Valid() || !json.Valid(r.Data) {
		return mobile.Invalid("invalid record")
	}
	if r.OwnerID > 0 {
		if _, ok := t.users[r.OwnerID]; !ok {
			return mobile.ErrNotFound
		}
	}
	if t.records[kind] == nil {
		t.records[kind] = make(map[int64]mobile.Record)
	}
	for id, other := range t.records[kind] {
		if id != r.ID && r.Key != "" && other.Key == r.Key {
			return mobile.ErrConflict
		}
	}
	now := time.Now().UTC()
	if r.ID == 0 {
		if r.Version != 0 {
			return mobile.ErrConflict
		}
		t.seq++
		r.ID, r.CreatedAt = t.seq, now
	} else {
		old, ok := t.records[kind][r.ID]
		if !ok {
			return mobile.ErrNotFound
		}
		if old.Version != r.Version {
			return mobile.ErrConflict
		}
		r.CreatedAt = old.CreatedAt
	}
	r.Version++
	r.UpdatedAt = now
	r.Participants = mobile.Unique(r.Participants)
	r.MediaIDs = mobile.Unique(r.MediaIDs)
	t.records[kind][r.ID] = copyMobileRecord(*r)
	return nil
}

func (t *memoryMobileTx) Count(k mobile.Kind, f mobile.Filter) (int64, error) {
	var count int64
	f.Limit = 500
	for {
		rows, e := t.List(k, f)
		if e != nil {
			return 0, e
		}
		count += int64(len(rows))
		if len(rows) < 500 {
			return count, nil
		}
		f.After = rows[len(rows)-1].ID
	}
}

func (t *memoryMobileTx) Delete(kind mobile.Kind, id, version int64) error {
	if !t.writable {
		return mobile.ErrForbidden
	}
	r, err := t.Get(kind, id)
	if err != nil {
		return err
	}
	if r.Version != version {
		return mobile.ErrConflict
	}
	delete(t.records[kind], id)
	return nil
}

func (t *memoryMobileTx) User(id int64) (domain.User, error) {
	u, ok := t.users[id]
	if !ok {
		return domain.User{}, mobile.ErrNotFound
	}
	return u, nil
}
func (t *memoryMobileTx) UserByPhone(phone string) (domain.User, error) {
	if phone != "" {
		for _, u := range t.users {
			if u.Phone == phone {
				return u, nil
			}
		}
	}
	return domain.User{}, mobile.ErrNotFound
}
func (t *memoryMobileTx) UserByName(name string) (domain.User, error) {
	for _, u := range t.users {
		if strings.EqualFold(u.Username, strings.TrimSpace(name)) {
			return u, nil
		}
	}
	return domain.User{}, mobile.ErrNotFound
}
func (t *memoryMobileTx) SaveUser(u *domain.User) error {
	if !t.writable {
		return mobile.ErrForbidden
	}
	u.Username = strings.TrimSpace(u.Username)
	if u.Username == "" {
		return mobile.Invalid("username required")
	}
	for id, other := range t.users {
		if id != u.ID && (strings.EqualFold(other.Username, u.Username) || u.Phone != "" && other.Phone == u.Phone) {
			return mobile.ErrConflict
		}
	}
	if u.ID == 0 {
		t.userSeq++
		u.ID, u.CreatedAt = t.userSeq, time.Now().UTC()
	} else {
		old, ok := t.users[u.ID]
		if !ok {
			return mobile.ErrNotFound
		}
		u.CreatedAt = old.CreatedAt
	}
	t.users[u.ID] = *u
	return nil
}
