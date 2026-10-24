package mobileapi

import (
	"bytes"
	"crypto/sha256"
	_ "embed"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"regexp"
	"strings"
	"time"
	"unicode/utf8"

	"kitty-circle/internal/mobile"
)

//go:embed reading-drafts.json
var readingDrafts []byte

var readingKey = regexp.MustCompile(`^[a-z][a-z0-9-]{0,63}$`)

func readingText(d mobile.ReadingDocument) string {
	var out strings.Builder
	fmt.Fprintf(&out, "M&D · meow & doggie\n%s\n版本：%s\n状态：%s\n", d.Title, d.Revision, d.Status)
	if d.EffectiveFrom != "" {
		fmt.Fprintf(&out, "生效时间：%s\n", d.EffectiveFrom)
	}
	if d.EffectiveUntil != "" {
		fmt.Fprintf(&out, "适用截止：%s\n", d.EffectiveUntil)
	}
	if d.Notice != "" {
		fmt.Fprintf(&out, "\n%s\n", d.Notice)
	}
	if d.Heading != "" {
		fmt.Fprintf(&out, "\n%s\n", d.Heading)
	}
	for _, fact := range d.Facts {
		fmt.Fprintf(&out, "\n%s\n%s\n", fact.Label, fact.Text)
	}
	for _, paragraph := range d.Paragraphs {
		fmt.Fprintf(&out, "\n%s\n", paragraph)
	}
	for _, change := range d.Changes {
		fmt.Fprintf(&out, "\n变更说明：%s\n", change)
	}
	return out.String()
}

// 配置文件属于发布物，普通客户端没有写入口；无配置只提供明确的草案预览。
func loadReadingCatalog() (out mobile.ReadingCatalog, err error) {
	raw := readingDrafts
	if name := os.Getenv("MND_READING_CATALOG"); name != "" {
		file, e := os.Open(name)
		if e != nil {
			return out, mobile.ErrUnavailable
		}
		defer file.Close()
		raw, e = io.ReadAll(io.LimitReader(file, 1<<20+1))
		if e != nil || len(raw) > 1<<20 {
			return out, mobile.ErrUnavailable
		}
	}
	if !utf8.Valid(raw) {
		return out, mobile.ErrUnavailable
	}
	decoder := json.NewDecoder(bytes.NewReader(raw))
	decoder.DisallowUnknownFields()
	if decoder.Decode(&out) != nil || len(out.Documents) > 200 {
		return out, mobile.ErrUnavailable
	}
	if e := decoder.Decode(new(any)); e != io.EOF {
		return out, mobile.ErrUnavailable
	}
	// 运营发布的规则目录不覆盖应用随附指南，也不补造任何正式协议。
	var builtIn mobile.ReadingCatalog
	if json.Unmarshal(readingDrafts, &builtIn) != nil {
		return out, mobile.ErrUnavailable
	}
	for _, guide := range builtIn.Documents {
		if guide.Type != "guide" {
			continue
		}
		found := false
		for _, document := range out.Documents {
			if document.Key == guide.Key {
				found = true
				break
			}
		}
		if !found {
			out.Documents = append(out.Documents, guide)
		}
	}
	seen := map[string]bool{}
	for index := range out.Documents {
		d := &out.Documents[index]
		if !readingKey.MatchString(d.Key) || seen[d.Key] || strings.TrimSpace(d.Title) == "" || strings.TrimSpace(d.Revision) == "" || !mobile.HasString([]string{"guide", "service", "adoption", "privacy", "teen", "seller", "inventory", "sdk", "providers", "retention"}, d.Type) || !mobile.HasString([]string{"reference", "draft", "published", "proposed", "archived"}, d.Status) || d.CoverAsset != "" && (d.CoverAsset != "first-cat" || d.Type != "guide") {
			return out, mobile.ErrUnavailable
		}
		seen[d.Key] = true
		if len(d.Facts)+len(d.Paragraphs) == 0 {
			return out, mobile.ErrUnavailable
		}
		for _, fact := range d.Facts {
			if strings.TrimSpace(fact.Label) == "" || strings.TrimSpace(fact.Text) == "" {
				return out, mobile.ErrUnavailable
			}
		}
		for _, paragraph := range d.Paragraphs {
			if strings.TrimSpace(paragraph) == "" {
				return out, mobile.ErrUnavailable
			}
		}
		if d.Status == "reference" && d.Type != "guide" || d.Key == "care-first-cat" && d.Type != "guide" {
			return out, mobile.ErrUnavailable
		}
		if d.Status == "published" || d.Status == "archived" {
			if strings.TrimSpace(out.OperatorName) == "" || strings.TrimSpace(out.OperatorAddress) == "" || strings.TrimSpace(out.OperatorContact) == "" {
				return out, mobile.ErrUnavailable
			}
			if _, e := time.Parse(time.RFC3339, d.EffectiveFrom); e != nil {
				return out, mobile.ErrUnavailable
			}
		}
		if d.EffectiveFrom != "" {
			if _, e := time.Parse(time.RFC3339, d.EffectiveFrom); e != nil {
				return out, mobile.ErrUnavailable
			}
		}
		if d.EffectiveUntil != "" {
			until, e := time.Parse(time.RFC3339, d.EffectiveUntil)
			from, startError := time.Parse(time.RFC3339, d.EffectiveFrom)
			if e != nil || startError != nil || !until.After(from) {
				return out, mobile.ErrUnavailable
			}
		}
		if d.Status == "archived" && d.EffectiveUntil == "" {
			return out, mobile.ErrUnavailable
		}
		if d.Facts == nil {
			d.Facts = []mobile.ReadingFact{}
		}
		if d.Paragraphs == nil {
			d.Paragraphs = []string{}
		}
		if d.Changes == nil {
			d.Changes = []string{}
		}
		digest := sha256.Sum256([]byte(readingText(*d)))
		d.SHA256 = hex.EncodeToString(digest[:])
	}
	if out.Documents == nil {
		out.Documents = []mobile.ReadingDocument{}
	}
	return out, nil
}

func publicReading(key string) (mobile.ReadingDocument, error) {
	if !readingKey.MatchString(key) {
		return mobile.ReadingDocument{}, mobile.ErrNotFound
	}
	catalog, e := loadReadingCatalog()
	if e != nil {
		return mobile.ReadingDocument{}, e
	}
	for _, document := range catalog.Documents {
		if document.Key == key {
			return document, nil
		}
	}
	return mobile.ReadingDocument{}, mobile.ErrNotFound
}

func (h *Handler) downloadReading(w http.ResponseWriter, r *http.Request, key string) {
	document, e := publicReading(key)
	if e != nil {
		failure(w, e)
		return
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.Header().Set("Content-Disposition", `attachment; filename="mnd-`+document.Key+`.txt"`)
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Content-SHA256", document.SHA256)
	_, _ = io.WriteString(w, readingText(document))
}
