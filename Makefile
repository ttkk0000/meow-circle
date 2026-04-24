SHELL := /bin/sh
GO     ?= go
BIN    ?= ./bin/server
PORT   ?= 8080

.PHONY: help
help: ## Show this help
	@awk 'BEGIN{FS":.*?## "}/^[a-zA-Z_-]+:.*?## /{printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ----- Go ---------------------------------------------------------------------

.PHONY: tidy
tidy: ## go mod tidy
	$(GO) mod tidy

.PHONY: build
build: ## Build the server binary into ./bin/server
	$(GO) build -trimpath -ldflags="-s -w" -o $(BIN) ./cmd/server

.PHONY: run
run: ## Run the server locally (uses in-memory store unless DATABASE_URL is set)
	APP_PORT=$(PORT) $(GO) run ./cmd/server

.PHONY: vet
vet: ## go vet
	$(GO) vet ./...

.PHONY: lint
lint: ## golangci-lint (requires golangci-lint v2 in PATH)
	golangci-lint run ./...

.PHONY: test
test: ## Unit tests with race detector
	$(GO) test -race -count=1 -timeout=120s ./...

.PHONY: cover
cover: ## Tests + HTML coverage report
	$(GO) test -race -count=1 -covermode=atomic -coverprofile=coverage.out ./...
	$(GO) tool cover -html=coverage.out -o coverage.html
	@echo "→ coverage.html"

.PHONY: integration
integration: ## Integration tests against $$DATABASE_URL / $$REDIS_URL
	$(GO) test -tags=integration -race -count=1 -timeout=180s ./internal/store/postgres/...

# ----- Infra ------------------------------------------------------------------

.PHONY: up
up: ## docker compose up -d (Postgres + Redis)
	docker compose up -d

.PHONY: down
down: ## docker compose down
	docker compose down

.PHONY: migrate
migrate: ## Apply migrations/*.sql (001–004) to $$DATABASE_URL
	psql "$$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrations/001_init.sql
	psql "$$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrations/002_social.sql
	psql "$$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrations/003_user_phone.sql
	psql "$$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrations/004_notifications_actor_image.sql

.PHONY: docker
docker: ## Build the production container image
	docker build -t kitty-circle:local .

.PHONY: docker-run
docker-run: docker ## Build and run the container (port 8080)
	docker run --rm -p $(PORT):8080 kitty-circle:local

# ----- Mobile -----------------------------------------------------------------

.PHONY: mobile-install
mobile-install: ## Install Expo mobile app deps
	cd mobile && npm install

.PHONY: mobile
mobile: ## Start Expo dev server
	cd mobile && npx expo start

# ----- Meta -------------------------------------------------------------------

.PHONY: clean
clean: ## Remove build + coverage artefacts
	rm -rf bin coverage.out coverage.html
