package mobileapi

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

// SPAHandler 只托管已生成的Web产物；缺少产物明确503，API不会落到SPA fallback。
func SPAHandler(directory string) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet && r.Method != http.MethodHead {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}
		relative := strings.TrimPrefix(r.URL.Path, "/app/")
		if strings.Contains(relative, "..") || strings.Contains(relative, "\\") {
			http.NotFound(w, r)
			return
		}
		target := filepath.Join(directory, filepath.FromSlash(relative))
		if info, e := os.Stat(target); e == nil && !info.IsDir() {
			w.Header().Set("X-Content-Type-Options", "nosniff")
			http.ServeFile(w, r, target)
			return
		}
		if strings.HasPrefix(relative, "assets/") || filepath.Ext(relative) != "" {
			http.NotFound(w, r)
			return
		}
		index := filepath.Join(directory, "index.html")
		if _, e := os.Stat(index); e != nil {
			http.Error(w, "Web client assets have not been built", http.StatusServiceUnavailable)
			return
		}
		w.Header().Set("Cache-Control", "no-cache")
		http.ServeFile(w, r, index)
	})
}
