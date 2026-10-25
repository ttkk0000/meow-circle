package main

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// 仅回归源码；不连接数据库，不代表迁移已运行。
func TestMigrationEnvelopePreservesInnerBlockAndDigest(t *testing.T) {
	dir := t.TempDir()
	source := "-- fixture\nBEGIN;\nDO $$ BEGIN PERFORM 1; END $$;\nCOMMIT;\n"
	os.WriteFile(filepath.Join(dir, "001_fixture.sql"), []byte(source), 0o600)
	files, err := load(dir)
	if err != nil || len(files) != 1 || !strings.Contains(files[0].sql, "DO $$ BEGIN") || strings.HasPrefix(files[0].sql, "BEGIN;") {
		t.Fatal("transaction wrapper was not isolated", err)
	}
	original := files[0].hash
	os.WriteFile(filepath.Join(dir, "001_fixture.sql"), []byte(source+"\n"), 0o600)
	files, err = load(dir)
	if err != nil || files[0].hash == original {
		t.Fatal("raw source change was not fingerprinted", err)
	}
}
func TestMigrationSequenceAndUnbalancedTransactionFailClosed(t *testing.T) {
	dir := t.TempDir()
	file := filepath.Join(dir, "001_fixture.sql")
	os.WriteFile(file, []byte("BEGIN; SELECT 1;"), 0o600)
	if _, err := load(dir); err == nil {
		t.Fatal("unbalanced transaction accepted")
	}
	os.WriteFile(file, []byte("SELECT 1;"), 0o600)
	os.WriteFile(filepath.Join(dir, "003_fixture.sql"), []byte("SELECT 1;"), 0o600)
	if _, err := load(dir); err == nil {
		t.Fatal("migration gap accepted")
	}
}
