package audit

import (
	"strings"
	"sync"
)

// Filter is a minimal in-process keyword blocklist.
// Replace with a proper moderation service (alibaba/aliyun green, textcensor) in production.
type Filter struct {
	mu       sync.RWMutex
	keywords []string
}

func NewFilter(keywords []string) *Filter {
	f := &Filter{}
	f.Set(keywords)
	return f
}

func (f *Filter) Set(keywords []string) {
	f.mu.Lock()
	defer f.mu.Unlock()
	out := make([]string, 0, len(keywords))
	for _, k := range keywords {
		k = strings.TrimSpace(strings.ToLower(k))
		if k != "" {
			out = append(out, k)
		}
	}
	f.keywords = out
}

// Check scans input for any blocked keyword (case-insensitive).
// Returns the first hit if any.
func (f *Filter) Check(input string) (hit string, blocked bool) {
	if input == "" {
		return "", false
	}
	lower := strings.ToLower(input)
	f.mu.RLock()
	defer f.mu.RUnlock()
	for _, k := range f.keywords {
		if strings.Contains(lower, k) {
			return k, true
		}
	}
	return "", false
}

func (f *Filter) Keywords() []string {
	f.mu.RLock()
	defer f.mu.RUnlock()
	out := make([]string, len(f.keywords))
	copy(out, f.keywords)
	return out
}

// DefaultKeywords ships a minimal example list; teams should override via config.
func DefaultKeywords() []string {
	return []string{
		"违禁词1", "违禁词2",
		"spam", "scam",
	}
}
