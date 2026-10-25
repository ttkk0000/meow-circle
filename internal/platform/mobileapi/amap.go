package mobileapi

import (
	"io"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"
)

// AmapProxy只代理地图用到的固定路径；安全密钥始终留在服务端。
// 原生Android／iOS Key不经过此代理，不转发Cookie、Authorization或私密事项。
func AmapProxy(key, securityCode, origin string) http.Handler {
	client := &http.Client{Timeout: 10 * time.Second, CheckRedirect: func(_ *http.Request, _ []*http.Request) error { return http.ErrUseLastResponse }}
	var mu sync.Mutex
	var second int64
	var requests int
	slots := make(chan struct{}, 8)
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		if r.Method != http.MethodGet {
			w.Header().Set("Allow", "GET")
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}
		if key == "" || securityCode == "" || origin == "" {
			http.Error(w, "map provider unavailable", http.StatusServiceUnavailable)
			return
		}
		ref, err := url.Parse(r.Header.Get("Referer"))
		if err != nil || ref.User != nil || ref.Scheme+"://"+ref.Host != origin || r.Header.Get("Origin") != "" && r.Header.Get("Origin") != origin {
			http.Error(w, "origin not allowed", http.StatusForbidden)
			return
		}
		path := strings.TrimPrefix(r.URL.Path, "/maps/amap/_AMapService")
		host := "restapi.amap.com"
		switch path {
		case "/v4/map/styles":
			host = "webapi.amap.com"
		case "/v3/assistant/coordinate/convert", "/v3/geocode/regeo":
		default:
			http.NotFound(w, r)
			return
		}
		query := r.URL.Query()
		if len(r.URL.RawQuery) > 4096 || query.Get("key") != key {
			http.Error(w, "invalid map request", http.StatusBadRequest)
			return
		}
		mu.Lock()
		now := time.Now().Unix()
		if second != now {
			second, requests = now, 0
		}
		requests++
		allowed := requests <= 20
		mu.Unlock()
		if !allowed {
			w.Header().Set("Retry-After", "1")
			http.Error(w, "map busy", http.StatusTooManyRequests)
			return
		}
		select {
		case slots <- struct{}{}:
			defer func() { <-slots }()
		default:
			http.Error(w, "map busy", http.StatusTooManyRequests)
			return
		}
		query.Set("key", key)
		query.Set("jscode", securityCode)
		upstream := &url.URL{Scheme: "https", Host: host, Path: path, RawQuery: query.Encode()}
		request, err := http.NewRequestWithContext(r.Context(), http.MethodGet, upstream.String(), nil)
		if err != nil {
			http.Error(w, "map unavailable", http.StatusBadGateway)
			return
		}
		response, err := client.Do(request)
		if err != nil {
			http.Error(w, "map unavailable", http.StatusBadGateway)
			return
		}
		defer response.Body.Close()
		if response.StatusCode != http.StatusOK {
			http.Error(w, "map unavailable", http.StatusBadGateway)
			return
		}
		body, err := io.ReadAll(io.LimitReader(response.Body, (4<<20)+1))
		if err != nil || len(body) > 4<<20 {
			http.Error(w, "map unavailable", http.StatusBadGateway)
			return
		}
		w.Header().Set("Content-Type", response.Header.Get("Content-Type"))
		_, _ = w.Write(body)
	})
}
