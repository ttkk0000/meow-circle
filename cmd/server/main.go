package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"bestTry/internal/platform/api"
)

func main() {
	port := getEnv("APP_PORT", "8080")

	server := &http.Server{
		Addr:              ":" + port,
		Handler:           api.NewRouter(),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	log.Printf("cat share server listening on :%s", port)
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("server stopped with error: %v", err)
	}
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
