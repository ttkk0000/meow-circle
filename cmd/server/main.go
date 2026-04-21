package main

import (
	"context"
	"errors"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"sync/atomic"
	"syscall"
	"time"

	"bestTry/internal/platform/api"
)

func main() {
	port := getEnv("APP_PORT", "8080")
	maxOpenConns := getEnvInt("MAX_OPEN_CONNS", 500)
	var openConns atomic.Int64
	trackedConns := sync.Map{}

	server := &http.Server{
		Addr:              ":" + port,
		Handler:           api.NewRouter(),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       30 * time.Second, // uploads may take a bit
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       90 * time.Second,
		ConnState: func(conn net.Conn, state http.ConnState) {
			switch state {
			case http.StateNew:
				if _, loaded := trackedConns.LoadOrStore(conn, struct{}{}); loaded {
					return
				}
				current := openConns.Add(1)
				if maxOpenConns > 0 && current > int64(maxOpenConns) {
					log.Printf("max open connections reached (%d), rejecting new connection", maxOpenConns)
					_ = conn.Close()
				}
			case http.StateClosed, http.StateHijacked:
				if _, loaded := trackedConns.LoadAndDelete(conn); loaded {
					openConns.Add(-1)
				}
			}
		},
	}

	// Graceful shutdown: listen for SIGINT / SIGTERM and drain in-flight
	// requests for up to 10s before exiting.
	shutdownCtx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	serverErr := make(chan error, 1)
	go func() {
		log.Printf("meow-circle server listening on :%s", port)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			serverErr <- err
		}
		close(serverErr)
	}()

	select {
	case err := <-serverErr:
		if err != nil {
			log.Fatalf("server stopped with error: %v", err)
		}
	case <-shutdownCtx.Done():
		log.Printf("shutdown signal received, draining for up to 10s...")
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := server.Shutdown(ctx); err != nil {
			log.Printf("graceful shutdown failed: %v — forcing close", err)
			_ = server.Close()
		}
		log.Printf("bye 🐾")
	}
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}

func getEnvInt(key string, fallback int) int {
	raw := os.Getenv(key)
	if raw == "" {
		return fallback
	}
	value, err := strconv.Atoi(raw)
	if err != nil {
		log.Printf("invalid %s=%q, fallback to %d", key, raw, fallback)
		return fallback
	}
	return value
}
