package mobileapi

import (
	"bytes"
	"embed"
	"kitty-circle/internal/mobile"
	"net/http"
	"path"
	"strings"
	"time"
)

//go:embed assets/v1/*
var assets embed.FS

func serveExpressionAsset(w http.ResponseWriter, r *http.Request) error {
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		return mobile.Invalid("asset requires GET")
	}
	name := strings.TrimPrefix(r.URL.Path, "/api/v2/expression-assets/")
	if path.Clean(name) != name || !strings.HasPrefix(name, "v1/") {
		return mobile.ErrNotFound
	}
	data, e := assets.ReadFile("assets/" + name)
	if e != nil {
		return mobile.ErrNotFound
	}
	if strings.HasSuffix(name, ".svg") {
		w.Header().Set("Content-Type", "image/svg+xml")
	} else if strings.HasSuffix(name, ".png") {
		w.Header().Set("Content-Type", "image/png")
	} else {
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	}
	w.Header().Set("Cache-Control", "public, max-age=86400")
	http.ServeContent(w, r, path.Base(name), time.Time{}, bytes.NewReader(data))
	return nil
}
