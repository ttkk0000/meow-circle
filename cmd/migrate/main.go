// 显式迁移入口：单连接锁、逐文件事务和摘要账本；旧库必须人工指定已核实基线。
package main

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"flag"
	"fmt"
	"github.com/jackc/pgx/v5"
	"kitty-circle/internal/platform/envfile"
	"log"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"
)

type migration struct{ name, hash, sql string }

func load(directory string) (out []migration, err error) {
	paths, e := filepath.Glob(filepath.Join(directory, "[0-9][0-9][0-9]_*.sql"))
	if e != nil {
		return nil, e
	}
	sort.Strings(paths)
	if len(paths) == 0 {
		return nil, errors.New("no migrations found")
	}
	for i, path := range paths {
		name := filepath.Base(path)
		if !strings.HasPrefix(name, fmt.Sprintf("%03d_", i+1)) {
			return nil, errors.New("migration sequence is incomplete or duplicated")
		}
		raw, e := os.ReadFile(path)
		if e != nil {
			return nil, errors.New("cannot read migration")
		}
		digest := sha256.Sum256(raw)
		body := strings.TrimSpace(string(raw))
		for strings.HasPrefix(body, "--") {
			line := strings.IndexByte(body, '\n')
			if line < 0 {
				body = ""
				break
			}
			body = strings.TrimSpace(body[line+1:])
		}
		begin := strings.HasPrefix(strings.ToUpper(body), "BEGIN;")
		commit := strings.HasSuffix(strings.ToUpper(body), "COMMIT;")
		if begin != commit {
			return nil, errors.New("unbalanced migration transaction")
		}
		if begin {
			body = strings.TrimSpace(body[len("BEGIN;") : len(body)-len("COMMIT;")])
		}
		if body == "" {
			return nil, errors.New("empty migration")
		}
		out = append(out, migration{name, hex.EncodeToString(digest[:]), body})
	}
	return out, nil
}
func apply(ctx context.Context, conn *pgx.Conn, m migration, baseline bool) error {
	tx, e := conn.Begin(ctx)
	if e != nil {
		return e
	}
	defer tx.Rollback(context.Background())
	if !baseline {
		if _, e = tx.Conn().PgConn().Exec(ctx, m.sql).ReadAll(); e != nil {
			return fmt.Errorf("migration %s failed; transaction rolled back", m.name)
		}
	}
	if _, e = tx.Exec(ctx, "INSERT INTO public.mnd_schema_migrations(name,sha256,baseline) VALUES($1,$2,$3)", m.name, m.hash, baseline); e != nil {
		return errors.New("cannot record migration")
	}
	return tx.Commit(ctx)
}
func run(directory, baseline string) error {
	files, e := load(directory)
	if e != nil {
		return e
	}
	if e = envfile.Load("DATABASE_URL"); e != nil {
		return e
	}
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		return errors.New("DATABASE_URL or DATABASE_URL_FILE is required")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Minute)
	defer cancel()
	conn, e := pgx.Connect(ctx, dsn)
	if e != nil {
		return errors.New("cannot connect to migration database")
	}
	defer conn.Close(context.Background())
	if _, e = conn.Exec(ctx, "SELECT pg_advisory_lock(2026102501)"); e != nil {
		return errors.New("cannot acquire migration lock")
	}
	defer conn.Exec(context.Background(), "SELECT pg_advisory_unlock(2026102501)")
	if _, e = conn.Exec(ctx, "SET search_path = public"); e != nil {
		return e
	}
	var legacy bool
	if e = conn.QueryRow(ctx, "SELECT to_regclass('public.users') IS NOT NULL").Scan(&legacy); e != nil {
		return e
	}
	if _, e = conn.Exec(ctx, "CREATE TABLE IF NOT EXISTS public.mnd_schema_migrations(name text PRIMARY KEY,sha256 text NOT NULL,baseline boolean NOT NULL DEFAULT false,applied_at timestamptz NOT NULL DEFAULT now())"); e != nil {
		return e
	}
	rows, e := conn.Query(ctx, "SELECT name,sha256 FROM public.mnd_schema_migrations ORDER BY name")
	if e != nil {
		return e
	}
	applied := map[string]string{}
	for rows.Next() {
		var n, h string
		if e = rows.Scan(&n, &h); e != nil {
			rows.Close()
			return e
		}
		applied[n] = h
	}
	e = rows.Err()
	rows.Close()
	if e != nil {
		return e
	}
	if len(applied) == 0 && legacy && baseline == "" {
		return errors.New("existing database has no migration ledger; verify schema and specify -baseline-through before continuing")
	}
	baselineIndex := -1
	if baseline != "" {
		if !legacy || len(applied) > 0 {
			return errors.New("baseline is only allowed for a reviewed existing database without a ledger")
		}
		for i, m := range files {
			if m.name == baseline {
				baselineIndex = i
			}
		}
		if baselineIndex < 0 {
			return errors.New("baseline filename is not in migration sequence")
		}
	}
	known := map[string]bool{}
	missing := false
	for _, m := range files {
		known[m.name] = true
		if h, ok := applied[m.name]; ok {
			if h != m.hash || missing {
				return errors.New("migration digest changed or ledger has a gap")
			}
		} else {
			missing = true
		}
	}
	for n := range applied {
		if !known[n] {
			return errors.New("database ledger is newer than this source")
		}
	}
	if baselineIndex >= 0 {
		tx, err := conn.Begin(ctx)
		if err != nil {
			return err
		}
		for _, m := range files[:baselineIndex+1] {
			if _, err = tx.Exec(ctx, "INSERT INTO public.mnd_schema_migrations(name,sha256,baseline) VALUES($1,$2,true)", m.name, m.hash); err != nil {
				tx.Rollback(context.Background())
				return errors.New("cannot record reviewed migration baseline")
			}
		}
		if err = tx.Commit(ctx); err != nil {
			return errors.New("cannot commit reviewed migration baseline")
		}
		for _, m := range files[:baselineIndex+1] {
			applied[m.name] = m.hash
		}
	}
	for i, m := range files {
		if _, ok := applied[m.name]; ok {
			continue
		}
		if e = apply(ctx, conn, m, i <= baselineIndex); e != nil {
			return e
		}
		log.Printf("migration=%s recorded baseline=%t", m.name, i <= baselineIndex)
	}
	return nil
}
func main() {
	directory := flag.String("dir", "migrations", "migration directory")
	baseline := flag.String("baseline-through", "", "last migration explicitly verified on an existing database")
	flag.Parse()
	if e := run(*directory, *baseline); e != nil {
		log.Fatal(e)
	}
}
