package mobileapi

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"kitty-circle/internal/mobile"
)

// 仅补回归源码，未在本轮执行测试。
func TestReadingDraftDownloadIsPublicCompleteAndDoesNotClaimPublication(t *testing.T) {
	t.Setenv("MND_READING_CATALOG", "")
	catalog, err := loadReadingCatalog()
	if err != nil {
		t.Fatal(err)
	}
	if catalog.OperatorName != "" {
		t.Fatal("invented operator")
	}
	for _, document := range catalog.Documents {
		if document.Status != "draft" && document.Status != "reference" {
			t.Fatal("draft became published", document.Key)
		}
	}
	document, err := publicReading("draft-service")
	if err != nil {
		t.Fatal(err)
	}
	w := httptest.NewRecorder()
	r := httptest.NewRequest("GET", "/api/v2/reading/draft-service/download", nil)
	r.Header.Set("Authorization", "Bearer expired-session")
	(&Handler{}).ServeHTTP(w, r)
	if w.Code != 200 || w.Body.String() != readingText(document) {
		t.Fatal(w.Code, w.Body.String())
	}
	digest := sha256.Sum256(w.Body.Bytes())
	if document.SHA256 != hex.EncodeToString(digest[:]) || w.Header().Get("X-Content-SHA256") != document.SHA256 {
		t.Fatal("inconsistent download digest")
	}
	if !strings.Contains(w.Header().Get("Content-Disposition"), "mnd-draft-service.txt") || !strings.HasPrefix(w.Header().Get("Content-Type"), "text/plain") {
		t.Fatal(w.Header())
	}
	if !strings.Contains(w.Body.String(), document.Facts[0].Text) || !strings.Contains(w.Body.String(), "草案") {
		t.Fatal("incomplete download")
	}
	if _, err := publicReading("../secret"); err != mobile.ErrNotFound {
		t.Fatal("unsafe key accepted", err)
	}
}

func TestReadingReleasePreservesGuideAndRejectsUntraceableVersions(t *testing.T) {
	filename := filepath.Join(t.TempDir(), "reading.json")
	t.Setenv("MND_READING_CATALOG", filename)
	catalog := mobile.ReadingCatalog{OperatorName: "fixture operator", OperatorAddress: "fixture address", OperatorContact: "fixture contact", Documents: []mobile.ReadingDocument{
		{Key: "service-example", Type: "service", Title: "fixture", Revision: "fixture-r1", Status: "published", EffectiveFrom: "2026-10-24T00:00:00Z", Paragraphs: []string{"complete fixture text"}},
	}}
	write := func(value mobile.ReadingCatalog) {
		t.Helper()
		raw, err := json.Marshal(value)
		if err != nil {
			t.Fatal(err)
		}
		if err = os.WriteFile(filename, raw, 0600); err != nil {
			t.Fatal(err)
		}
	}
	write(catalog)
	result, err := loadReadingCatalog()
	if err != nil || len(result.Documents) != 2 {
		t.Fatal(result, err)
	}
	if _, err := publicReading("care-first-cat"); err != nil {
		t.Fatal("release removed built-in guide", err)
	}
	catalog.OperatorContact = " "
	write(catalog)
	if _, err := loadReadingCatalog(); err == nil {
		t.Fatal("publication accepted without actual contact")
	}
	catalog.OperatorContact = "fixture contact"
	catalog.Documents[0].Status = "archived"
	write(catalog)
	if _, err := loadReadingCatalog(); err == nil {
		t.Fatal("history accepted without end date")
	}
	catalog.Documents[0].EffectiveUntil = "2026-10-23T00:00:00Z"
	write(catalog)
	if _, err := loadReadingCatalog(); err == nil {
		t.Fatal("inverted date range accepted")
	}
	catalog.Documents[0].EffectiveUntil = "2026-10-25T00:00:00Z"
	write(catalog)
	if _, err := loadReadingCatalog(); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filename, []byte("{} {}"), 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := loadReadingCatalog(); err == nil {
		t.Fatal("multiple JSON values accepted")
	}
	if err := os.WriteFile(filename, []byte{0xff}, 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := loadReadingCatalog(); err == nil {
		t.Fatal("invalid encoding accepted")
	}
}
