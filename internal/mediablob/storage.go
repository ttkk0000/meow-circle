// Package mediablob stores only opaque processed media. Authorization and
// evidence retention are always checked by the caller before accessing blobs.
package mediablob

import (
	"context"
	"errors"
	"io"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"time"
)

const MaxBytes = 10 << 20

var ErrNotFound = errors.New("media blob not found")
var ErrUnavailable = errors.New("media blob storage unavailable")
var Managed = regexp.MustCompile(`^(mnd-[0-9]+-[0-9]+-[a-f0-9]{64}\.(?:jpg|png))$|^\.upload-(mnd-[0-9]+-[0-9]+-[a-f0-9]{64}\.(?:jpg|png))-[A-Za-z0-9]+$`)

type Object struct {
	Name     string
	Modified time.Time
	Size     int64
}
type Storage interface {
	Put(context.Context, string, []byte) error
	Read(context.Context, string) ([]byte, time.Time, error)
	Remove(context.Context, string) error
	List(context.Context, string) ([]Object, string, error)
}
type Disk struct{ Dir string }

func (d Disk) path(name string) (string, error) {
	if !Managed.MatchString(name) || filepath.Base(name) != name {
		return "", ErrUnavailable
	}
	return filepath.Join(d.Dir, name), nil
}
func (d Disk) Put(ctx context.Context, name string, data []byte) error {
	target, err := d.path(name)
	if err != nil {
		return err
	}
	if len(data) == 0 || len(data) > MaxBytes {
		return ErrUnavailable
	}
	if err = ctx.Err(); err != nil {
		return err
	}
	if err = os.MkdirAll(d.Dir, 0700); err != nil {
		return err
	}
	info, err := os.Lstat(d.Dir)
	if err != nil || !info.IsDir() {
		return ErrUnavailable
	}
	f, err := os.CreateTemp(d.Dir, ".upload-"+name+"-*")
	if err != nil {
		return err
	}
	defer os.Remove(f.Name())
	_, err = f.Write(data)
	if err == nil {
		err = f.Sync()
	}
	if closed := f.Close(); err == nil {
		err = closed
	}
	if err != nil {
		return err
	}
	if err = ctx.Err(); err != nil {
		return err
	}
	return os.Rename(f.Name(), target)
}
func (d Disk) Read(ctx context.Context, name string) ([]byte, time.Time, error) {
	path, err := d.path(name)
	if err != nil {
		return nil, time.Time{}, err
	}
	if err = ctx.Err(); err != nil {
		return nil, time.Time{}, err
	}
	info, err := os.Lstat(path)
	if errors.Is(err, os.ErrNotExist) {
		return nil, time.Time{}, ErrNotFound
	}
	if err != nil || !info.Mode().IsRegular() || info.Size() > MaxBytes {
		return nil, time.Time{}, ErrUnavailable
	}
	f, err := os.Open(path)
	if err != nil {
		return nil, time.Time{}, err
	}
	defer f.Close()
	data, err := io.ReadAll(io.LimitReader(f, MaxBytes+1))
	if err != nil || len(data) > MaxBytes {
		return nil, time.Time{}, ErrUnavailable
	}
	return data, info.ModTime(), nil
}
func (d Disk) Remove(ctx context.Context, name string) error {
	path, err := d.path(name)
	if err != nil {
		return err
	}
	if err = ctx.Err(); err != nil {
		return err
	}
	err = os.Remove(path)
	if errors.Is(err, os.ErrNotExist) {
		return nil
	}
	return err
}
func (d Disk) List(ctx context.Context, after string) ([]Object, string, error) {
	dir, err := os.Open(d.Dir)
	if errors.Is(err, os.ErrNotExist) {
		return []Object{}, "", nil
	}
	if err != nil {
		return nil, "", err
	}
	defer dir.Close()
	objects := []Object{}
	more := false
	for {
		if err = ctx.Err(); err != nil {
			return nil, "", err
		}
		entries, readErr := dir.ReadDir(100)
		if readErr != nil && !errors.Is(readErr, io.EOF) {
			return nil, "", readErr
		}
		for _, entry := range entries {
			if entry.Name() > after && entry.Type().IsRegular() && Managed.MatchString(entry.Name()) {
				info, err := entry.Info()
				if err != nil {
					return nil, "", err
				}
				objects = append(objects, Object{entry.Name(), info.ModTime(), info.Size()})
			}
		}
		sort.Slice(objects, func(i, j int) bool { return objects[i].Name < objects[j].Name })
		if len(objects) > 100 {
			more = true
			objects = objects[:100]
		}
		if errors.Is(readErr, io.EOF) {
			break
		}
	}
	next := ""
	if more {
		next = objects[len(objects)-1].Name
	}
	return objects, next, nil
}
