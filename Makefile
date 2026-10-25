SHELL := /bin/sh
GO     ?= go
BIN    ?= ./bin/server
PORT   ?= 8080
KMP_DIR ?= kmp
MOBILE_DIR ?= mobile
KMP_GRADLE ?= ./gradlew
EXPO ?= npx expo
EAS ?= npx eas
MEOW_API_BASE_URL ?= http://127.0.0.1:8080

.PHONY: help
help: ## Show this help
	@awk 'BEGIN{FS=":.*## "}/^[a-zA-Z0-9_-]+:.*## /{printf "  \033[36m%-24s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

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
migrate: ## Apply pending migrations with checksums and an explicit legacy baseline
	$(GO) run ./cmd/migrate

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

# ----- Client packaging -------------------------------------------------------

.PHONY: package-kmp-android
package-kmp-android: ## Package KMP Android release APK
	cd $(KMP_DIR) && $(KMP_GRADLE) :androidApp:assembleRelease -Pmeow.api.base.url=$(MEOW_API_BASE_URL)

.PHONY: package-kmp-android-debug
package-kmp-android-debug: ## Package KMP Android debug APK
	cd $(KMP_DIR) && $(KMP_GRADLE) :androidApp:assembleDebug -Pmeow.api.base.url=$(MEOW_API_BASE_URL)

.PHONY: package-kmp-desktop
package-kmp-desktop: ## Package KMP desktop app for the current OS
	cd $(KMP_DIR) && $(KMP_GRADLE) :desktopApp:packageDistributionForCurrentOS

.PHONY: package-kmp
package-kmp: package-kmp-android package-kmp-desktop ## Package KMP Android release APK + desktop distribution

.PHONY: package-kmp-ios-framework
package-kmp-ios-framework: ## Build/sign KMP shared iOS framework for Xcode (macOS/Xcode only)
	cd $(KMP_DIR) && $(KMP_GRADLE) :iosUi:embedAndSignAppleFrameworkForXcode

.PHONY: package-rn-web
package-rn-web: ## Export RN/Expo web static bundle
	cd $(MOBILE_DIR) && $(EXPO) export --platform web

.PHONY: package-rn-android
package-rn-android: ## Build RN/Expo Android release locally
	cd $(MOBILE_DIR) && $(EXPO) run:android --variant release

.PHONY: package-rn-android-eas
package-rn-android-eas: ## Build RN/Expo Android with EAS
	cd $(MOBILE_DIR) && $(EAS) build --platform android

.PHONY: package-rn-ios
package-rn-ios: ## Build RN/Expo iOS release locally (macOS/Xcode only)
	cd $(MOBILE_DIR) && $(EXPO) run:ios --configuration Release

.PHONY: package-rn-ios-eas
package-rn-ios-eas: ## Build RN/Expo iOS with EAS
	cd $(MOBILE_DIR) && $(EAS) build --platform ios

.PHONY: package-rn
package-rn: package-rn-web package-rn-android ## Export RN/Expo web + local Android release

# ----- KMP Android ------------------------------------------------------------

.PHONY: kmp-android-build
kmp-android-build: ## Build KMP Android debug APK
	cd kmp && ./gradlew :androidApp:assembleDebug

.PHONY: kmp-android-run
kmp-android-run: ## Install and run KMP Android app on connected emulator/device
	adb install -r kmp/androidApp/build/outputs/apk/debug/androidApp-debug.apk
	adb shell am start -n com.ttkk0000.meowcircle.kmpapp/.MainActivity

# ----- Meta -------------------------------------------------------------------

.PHONY: clean
clean: ## Remove build + coverage artefacts
	rm -rf bin coverage.out coverage.html

# ----- Current delivery entry points (explicit invocation only) ----------------
.PHONY: delivery-init delivery-build delivery-up delivery-stop delivery-status app-android-debug app-android-release app-android-run app-ios-prepare app-ios-simulator app-ios-archive app-ios-export app-ios-run app-desktop-package
DELIVERY_DIR ?= deploy/local
API_URL ?= https://example.invalid
DEVICE ?=
EXPORT_OPTIONS ?=
delivery-init: ## Initialize private production settings; DOMAIN and IMAGE are required
	python3 scripts/delivery.py init --directory "$(DELIVERY_DIR)" --domain "$(DOMAIN)" --image "$(IMAGE)"
delivery-build: ## Build API/Web/worker/migration image, without publishing
	python3 scripts/delivery.py build --directory "$(DELIVERY_DIR)"
delivery-up: ## Stop old writers, migrate and start production services
	python3 scripts/delivery.py up --directory "$(DELIVERY_DIR)"
delivery-stop: ## Stop production services while preserving volumes
	python3 scripts/delivery.py stop --directory "$(DELIVERY_DIR)"
delivery-status: ## Inspect production container state
	python3 scripts/delivery.py status --directory "$(DELIVERY_DIR)"
app-android-debug: ## Build current Android debug APK
	python3 scripts/mobile.py android debug --api-url "$(MEOW_API_BASE_URL)"
app-android-release: ## Build signed APK and AAB with externally supplied signing credentials
	python3 scripts/mobile.py android release --api-url "$(API_URL)"
app-android-run: ## Build/install/launch debug APK on the explicitly selected device
	python3 scripts/mobile.py android run --api-url "$(MEOW_API_BASE_URL)" --device "$(DEVICE)"
app-ios-prepare: ## Generate current Xcode workspace and resolve pinned Pods
	python3 scripts/mobile.py ios prepare
app-ios-simulator: ## Build current iPhone/iPad simulator app
	python3 scripts/mobile.py ios simulator --api-url "$(MEOW_API_BASE_URL)"
app-ios-archive: ## Archive a signed iPhone/iPad app using MND_IOS_TEAM_ID
	python3 scripts/mobile.py ios archive --api-url "$(API_URL)"
app-ios-export: ## Export IPA using a reviewed ExportOptions plist; never uploads
	python3 scripts/mobile.py ios export --export-options "$(EXPORT_OPTIONS)"
app-ios-run: ## Install/launch an existing simulator app on an explicit UDID
	python3 scripts/mobile.py ios run --device "$(DEVICE)"
app-desktop-package: ## Package current OS desktop distribution
	python3 scripts/mobile.py desktop package
