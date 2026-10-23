package mobileapi

import (
	"html/template"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"

	"kitty-circle/internal/mobile"
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

var shareTemplate = template.Must(template.New("share").Parse(`<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>{{.Title}} · M&amp;D</title><meta name="description" content="{{.Description}}"><meta property="og:title" content="{{.Title}}"><meta property="og:description" content="{{.Description}}">{{if .Image}}<meta property="og:image" content="{{.Image}}">{{end}}</head><body><main><h1>{{.Title}}</h1><p>{{.Description}}</p><a href="{{.Link}}">在 M&amp;D 中查看</a></main></body></html>`))

// ShareHandler 以游客权限重新读取公开投影；私密或撤回内容不输出预览。
func (h *Handler) ShareHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}
	parts := strings.Split(strings.Trim(strings.TrimPrefix(r.URL.Path, "/share/"), "/"), "/")
	if len(parts) != 2 || !mobile.HasString([]string{"entries", "pets", "cases", "offers"}, parts[0]) {
		http.NotFound(w, r)
		return
	}
	id, e := parseID(parts[1])
	if e != nil {
		http.NotFound(w, r)
		return
	}
	card, e := h.Service.PublicShare(r.Context(), mobile.Kind(parts[0]), id)
	if e != nil {
		http.NotFound(w, r)
		return
	}
	title, description := card.Title, card.Summary
	if runes := []rune(description); len(runes) > 160 {
		description = string(runes[:160]) + "…"
	}
	// 地址由本站路径组成，不信任Host／客户端提供的图片URL。
	link := "/app/" + parts[0] + "/" + parts[1]
	image := ""
	if origin, e := url.Parse(os.Getenv("MND_WEB_ORIGIN")); e == nil && origin.Scheme == "https" && origin.Host != "" && origin.User == nil && origin.Path == "" && origin.RawQuery == "" && origin.Fragment == "" && strings.HasPrefix(card.ImageURL, "/api/v2/media/") {
		image = origin.String() + card.ImageURL
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	_ = shareTemplate.Execute(w, map[string]string{"Title": title, "Description": description, "Link": link, "Image": image})
}
