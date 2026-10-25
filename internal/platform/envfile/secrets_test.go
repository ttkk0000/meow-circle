package envfile

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// 仅回归源码；本轮不执行测试。
func TestSecretFileDoesNotOverrideExplicitValue(t *testing.T) {
	name := "MND_FIXTURE_SECRET"
	path := filepath.Join(t.TempDir(), "secret")
	os.WriteFile(path, []byte("private-value\n"), 0o600)
	t.Setenv(name, "configured")
	t.Setenv(name+"_FILE", path)
	if err := Load(name); err == nil || strings.Contains(err.Error(), "private-value") {
		t.Fatal("ambiguous secret accepted or printed")
	}
	t.Setenv(name, "")
	if err := Load(name); err != nil || os.Getenv(name) != "private-value" {
		t.Fatal("file not loaded without final newline")
	}
}
func TestSecretFileRejectsEmptyOversizeAndMissing(t *testing.T) {
	name := "MND_FIXTURE_SECRET"
	path := filepath.Join(t.TempDir(), "secret")
	t.Setenv(name, "")
	t.Setenv(name+"_FILE", path)
	if err := Load(name); err == nil {
		t.Fatal("missing file accepted")
	}
	for _, value := range []string{"\n", strings.Repeat("x", 1024*1024+1)} {
		os.WriteFile(path, []byte(value), 0o600)
		if err := Load(name); err == nil {
			t.Fatal("invalid secret file accepted")
		}
	}
}
