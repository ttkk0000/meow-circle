package main

import (
	"context"
	"flag"
	"kitty-circle/internal/platform/envfile"
	"log"
	"os"
	"os/signal"
	"path/filepath"
	"strconv"
	"syscall"
	"time"

	"kitty-circle/internal/platform/mobileworker"
)

func main() {
	if err := envfile.Load("MND_WORKER_OPERATOR_TOKEN"); err != nil {
		log.Fatal(err)
	}
	once := flag.Bool("once", true, "run one bounded batch; set -once=false for an explicit continuous worker")
	flag.Parse()
	job := os.Getenv("MND_WORKER_JOB")
	runner, e := mobileworker.New(os.Getenv("MND_WORKER_API_URL"), os.Getenv("MND_WORKER_OPERATOR_TOKEN"), job, os.Getenv("MND_WORKER_ALLOW_LOOPBACK_HTTP") == "1")
	if e != nil {
		log.Fatal(e)
	}
	interval := 30
	if raw := os.Getenv("MND_WORKER_INTERVAL_SECONDS"); raw != "" {
		interval, e = strconv.Atoi(raw)
		if e != nil || interval < 5 || interval > 3600 {
			log.Fatal("worker interval must be 5 to 3600 seconds")
		}
	}
	stateFile := os.Getenv("MND_WORKER_STATE_FILE")
	if stateFile == "" {
		stateFile = filepath.Join("data", "mobile-worker", job+".json")
	}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	for {
		checkpoint, e := runner.Load(stateFile)
		if e != nil {
			log.Fatal(e)
		}
		result, e := runner.Tick(ctx, checkpoint)
		if e != nil {
			if ctx.Err() != nil {
				return
			}
			if *once {
				log.Fatal(e)
			}
			log.Printf("job=%s retry: %v", job, e)
		} else {
			if e = runner.Save(stateFile, result); e != nil {
				log.Fatal(e)
			}
			log.Printf("job=%s state=%s processed=%d", job, result.State, result.Processed)
		}
		if *once {
			return
		}
		timer := time.NewTimer(time.Duration(interval) * time.Second)
		select {
		case <-ctx.Done():
			timer.Stop()
			return
		case <-timer.C:
		}
	}
}
