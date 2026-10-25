// Package operations 保存独立技术运维数据，不复用消费者身份、资料或操作权限。
package operations

import (
	"context"
	"encoding/json"
	"errors"
	"time"
)

var (
	ErrNotFound    = errors.New("operations record not found")
	ErrConflict    = errors.New("operations record changed")
	ErrInvalid     = errors.New("invalid operations command")
	ErrUnavailable = errors.New("operations storage unavailable")
)

type Record struct {
	ID        string          `json:"id"`
	Sequence  int64           `json:"-"`
	Version   int64           `json:"version,string"`
	State     string          `json:"state"`
	CreatedAt time.Time       `json:"created_at"`
	UpdatedAt time.Time       `json:"updated_at"`
	Data      json.RawMessage `json:"data"`
}
type Filter struct {
	Subject  string
	Before   int64
	Limit    int
	State    string
	OpenOnly bool
}
type Page struct {
	Items      []Record `json:"items"`
	NextCursor string   `json:"next_cursor"`
}
type Tx interface {
	Get(kind, id string) (Record, error)
	List(kind string, filter Filter) ([]Record, error)
	Save(kind string, record *Record) error
}
type Repository interface {
	View(context.Context, string, func(Tx) error) error
	Update(context.Context, string, func(Tx) error) error
}

func validKind(kind string) bool {
	switch kind {
	case "runs", "incidents", "releases", "audit", "commands":
		return true
	}
	return false
}
func pageLimit(n int) int {
	if n < 1 || n > 200 {
		return 100
	}
	return n
}
func body(r *Record, data any) error {
	b, e := json.Marshal(data)
	if e == nil {
		r.Data = b
	}
	return e
}
func decode[T any](r Record) (out T, err error) { err = json.Unmarshal(r.Data, &out); return }
