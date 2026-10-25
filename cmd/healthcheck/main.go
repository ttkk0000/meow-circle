// 镜像内健康探针，不依赖 shell/curl，也不读取账号凭据。
package main

import (
	"net/http"
	"os"
	"strconv"
	"time"
)

func main() {
	port := os.Getenv("APP_PORT")
	if port == "" {
		port = "8080"
	}
	n, e := strconv.Atoi(port)
	if e != nil || n < 1 || n > 65535 {
		os.Exit(1)
	}
	c := http.Client{Timeout: 3 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	r, e := c.Get("http://127.0.0.1:" + port + "/healthz")
	if e != nil {
		os.Exit(1)
	}
	r.Body.Close()
	if r.StatusCode != http.StatusOK {
		os.Exit(1)
	}
}
