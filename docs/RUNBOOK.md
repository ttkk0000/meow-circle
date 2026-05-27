# Runbook

Last updated: 2026-05-27

This runbook captures how to run, verify, and safely change the current M&D
repository. It is intentionally practical: start here when you need the system
up locally or when another agent needs a clean handoff.

## 1. Fast Local Modes

| Mode | Use When | Commands |
| --- | --- | --- |
| Static Web review | You only need to inspect HTML/CSS/JS and design boards. | `cd web && python3 -m http.server 4173` |
| Go API with memory store | You need the API without external services. | `make run` |
| Go API with Postgres | You need persistence and migrations. | `make up`, set `DATABASE_URL`, then `make run` |
| Go API with Postgres + Redis | You need cache behavior too. | `make up`, set `DATABASE_URL` and `REDIS_URL`, then `make run` |
| Expo mobile | You need the runtime mobile app. | `cd mobile && npm install && npm start` |
| KMP Android | You need Kotlin/Android source validation. | `cd kmp && bash ./gradlew :androidApp:compileDebugKotlin` |

Recommended first smoke test for design-only changes:

```bash
cd web
python3 -m http.server 4173
```

Then open:

- `http://127.0.0.1:4173/index.html`
- `http://127.0.0.1:4173/discover.html`
- `http://127.0.0.1:4173/compose.html`
- `http://127.0.0.1:4173/market.html`
- `http://127.0.0.1:4173/messages.html`
- `http://127.0.0.1:4173/profile.html`
- `http://127.0.0.1:4173/login.html`
- `http://127.0.0.1:4173/register.html`
- `http://127.0.0.1:4173/dashboard.html`
- `http://127.0.0.1:4173/admin.html`
- `http://127.0.0.1:4173/post.html`
- `http://127.0.0.1:4173/cute.html`
- `http://127.0.0.1:4173/pawpop-mobile.html`
- `http://127.0.0.1:4173/pawpop-desktop.html`

## 2. Backend Runtime

`cmd/server/main.go` starts a standard-library `http.Server` and installs
`api.NewRouter()`.

Server defaults:

| Setting | Default | Notes |
| --- | --- | --- |
| Port | `8080` | Controlled by `APP_PORT`. |
| Max open connections | `500` | Controlled by `MAX_OPEN_CONNS`; values `<= 0` disable the guard. |
| Read header timeout | 5s | Protects slow header reads. |
| Read timeout | 30s | Upload-friendly but bounded. |
| Write timeout | 30s | Applies to API/static responses. |
| Idle timeout | 90s | Keep-alive idle timeout. |
| Shutdown drain | 10s | SIGINT/SIGTERM graceful shutdown window. |

Memory-store run:

```bash
make run
```

Postgres/Redis run:

```bash
make up

DATABASE_URL=postgres://meow:meowpass@localhost:5432/meow?sslmode=disable \
REDIS_URL=redis://localhost:6379/0 \
make run
```

Manual migrations for an existing database:

```bash
DATABASE_URL=postgres://meow:meowpass@localhost:5432/meow?sslmode=disable \
make migrate
```

Health checks:

```bash
curl http://127.0.0.1:8080/healthz
curl http://127.0.0.1:8080/readyz
```

Expected `store` values from `/healthz`:

| Env | Store |
| --- | --- |
| No `DATABASE_URL` | `memory` |
| `DATABASE_URL` only | `postgres` |
| `DATABASE_URL` + `REDIS_URL` | `postgres+redis` |

## 3. Environment Variables

| Variable | Default | Used By | Notes |
| --- | --- | --- | --- |
| `APP_PORT` | `8080` | Go server | HTTP listen port. |
| `MAX_OPEN_CONNS` | `500` | Go server | Connection guard. |
| `DATABASE_URL` | empty | Store | Empty means memory store. |
| `REDIS_URL` | empty | Cache | Used only with Postgres. |
| `JWT_SECRET` | `change-me-in-production` | Auth | Must be replaced outside development. |
| `ADMIN_KEY` | `admin123` | Admin Web/API | Must be replaced outside development. |
| `CORS_ALLOW_ORIGIN` | `*` | API CORS | Use explicit origins outside development. |
| `ALIPAY_APP_ID` | empty | Payment | Enables Alipay stub provider when set. |
| `ALIPAY_PRIVATE_KEY` | empty | Payment | Stub config, do not commit real keys. |
| `ALIPAY_GATEWAY` | official gateway | Payment | Optional override. |
| `WECHAT_MCH_ID` | empty | Payment | Enables WeChat stub provider when set. |
| `WECHAT_API_KEY` | empty | Payment | Stub config, do not commit real keys. |
| `STRIPE_SECRET_KEY` | empty | Payment | Enables Stripe stub provider when set. |
| `MEOW_LOG_SMS_CODE` | empty | Phone OTP | Set `1` to log development SMS codes. |
| `MEOW_DEV_SMS_CODE` | empty | Phone OTP | Fixed dev code accepted by verification. |
| `EXPO_PUBLIC_API_URL` | auto fallback | Expo | Bundle-time API base URL. |
| `meow.api.base.url` | `http://10.0.2.2:8080` | KMP Android Gradle property | Override Android API base URL. |

Security note: the old `MEOW_*` environment names are backend compatibility
names, not visible product branding. Keep visible branding as `M&D`.

## 4. Upload Storage

Media uploads are handled by `internal/platform/api/media.go`.

| Concern | Current Behavior |
| --- | --- |
| Multipart field | `file` |
| Image limit | 10 MB |
| Video limit | 200 MB |
| Accepted images | JPEG, PNG, WebP, GIF |
| Accepted videos | MP4, WebM, QuickTime/MOV |
| Storage path | `data/uploads` under the process working directory |
| Public URL | `/uploads/{filename}` |
| Default media status | `approved` |

The Docker runtime copies `web/` and `migrations/`, but uploaded files live in
runtime storage. Mount or persist `data/uploads` for non-ephemeral deployments.

## 5. Client Configuration

### Static Web

The Web app is served from `web/` by the Go server. Static server review also
works for most pages because M&D fallback data was added to key scripts.

Important local-storage keys:

| Key | Used By | Do Not Rename Because |
| --- | --- | --- |
| `meow_token` | Web auth | Existing Web scripts expect it. |
| `meow_user` | Web auth | Existing Web scripts expect it. |
| `mnd_cute_theme` | `web/cute-ui.js` | WebUI theme switcher. |
| `mnd_cute_profile_bg` | `web/cute-ui.js` | WebUI profile background switcher. |

Use `?live=1` on `web/cute.html` when validating supported API hydration.

### Expo

Set the backend URL in `mobile/.env`:

```bash
EXPO_PUBLIC_API_URL=http://127.0.0.1:8080
```

For Android emulator API access, use:

```bash
EXPO_PUBLIC_API_URL=http://10.0.2.2:8080
```

Token storage:

| Key | Store |
| --- | --- |
| `meow.auth.token` | Expo SecureStore, localStorage fallback on Web |
| `meow.auth.user` | Expo SecureStore, localStorage fallback on Web |

### KMP Android

The default Android emulator base URL is `http://10.0.2.2:8080`.

Override it per build:

```bash
cd kmp
bash ./gradlew :androidApp:assembleDebug -Pmeow.api.base.url=http://127.0.0.1:8080
```

For a physical Android device, either use the host LAN IP or run:

```bash
adb reverse tcp:8080 tcp:8080
```

then set the Gradle property to `http://127.0.0.1:8080`.

## 6. Verification Matrix

| Change Area | Minimum Check | Stronger Check |
| --- | --- | --- |
| Docs only | `git diff --check -- docs` | Review docs hub links and stale naming search. |
| Web CSS/HTML/JS | Static server smoke | Desktop `1440x1000` + mobile `390x844` browser screenshots. |
| Go API | `make test` | `make integration` with Postgres/Redis. |
| Migrations | `make migrate` against a fresh DB | Integration tests plus manual create/read flows. |
| Expo | `cd mobile && npm run typecheck` | Device/simulator visual smoke. |
| KMP shared/Android | Gradle compile | Emulator visual smoke. |
| Docker | `make docker` | `make docker-run` and `/healthz` check. |

Useful docs and code lint:

```bash
# Verify static HTML pages: structure, branding terms (M&D), and check for broken local links
python scripts/verify_web_smoke.py

# Git check and docs search
git diff --check -- docs
rg -n "TODO|TBD|Pawpop|Kitty Circle|喵友圈|铲屎官|Meow Circle" docs
```

Some hits are expected inside historical-file explanations or migration notes.
Do not "fix" historical context by erasing useful warnings.

## 7. Known Local Blockers

These are machine/session-specific observations from the latest pass.

| Blocker | Impact | Status / Workaround |
| --- | --- | --- |
| Go version parser rejected `go 1.25.0` previously | Go tests/server may not run | **Resolved**: The local machine is verified running Go v1.26.2. |
| Shell Node reported `v16.10.0` previously | React Native dependencies warn and expect Node 18+ | **Resolved**: The local machine is verified running Node v24.15.0. |
| KMP Gradle resolution hit HTTP 403 from Maven/Gradle Plugin Portal | Kotlin compile stops before source validation | **Mitigated**: Commented Aliyun Maven/Gradle mirror links have been added to `kmp/settings.gradle.kts` for easy enabling. |
| `kmp/gradlew` may lack execute bit | Direct `./gradlew` can fail | Use `bash ./gradlew ...`. |

Keep these in docs for troubleshooting references.

## 8. Deployment Notes

| File | Role |
| --- | --- |
| `Dockerfile` | Builds a static Go binary in `golang:1.25-alpine`, runs on distroless nonroot. |
| `docker-compose.yml` | Local Postgres 16 and Redis 7 with migration files mounted for first boot. |
| `Caddyfile` | Public `:80` reverse proxy to `127.0.0.1:8080`. |
| `SECURITY.md` | Disclosure/security guidance. |

Production checklist:

1. Set `JWT_SECRET` and `ADMIN_KEY` to strong secrets.
2. Replace wildcard `CORS_ALLOW_ORIGIN` with explicit origins.
3. Use Postgres and run all migrations.
4. Persist `data/uploads`.
5. Decide whether Redis cache is needed for the traffic profile.
6. Configure backup/restore for Postgres and uploaded media.
7. Put Caddy or another TLS proxy in front of the Go process.

## 9. Safe Change Order

For backend/domain changes:

1. Update `internal/domain/models.go`.
2. Update `internal/store/store.go`.
3. Update memory store.
4. Update Postgres store and add a migration.
5. Update API handlers and route docs.
6. Update Web, Expo, and KMP clients.
7. Update `docs/API_AND_DATA_FLOW.md`.
8. Run the relevant checks from the verification matrix.

For UI-system changes:

1. Update `docs/design/MND_UI_ALIGNMENT_GUIDE.md`.
2. Update Web tokens/bridge/prototype.
3. Update Expo `mobile/src/theme.ts` and shared components.
4. Update KMP Android theme files.
5. Update the design boards if the pattern changed.
6. Update `docs/FRONTEND_SURFACE_MAP.md` and `docs/design/MND_DESIGN_MEMORY.md`.
7. Run Web smoke checks and mobile/KMP checks when available.

## 10. Naming Boundaries

Visible product language:

- Keep the mark as `M&D`.
- Keep the lockup/supporting copy as `meow & doggie`.
- Keep cats as the primary emotional center.
- Doggie content remains a companion branch.

Compatibility names that may remain in code:

- Go module: `kitty-circle`.
- Web auth keys: `meow_token`, `meow_user`.
- Mobile auth keys: `meow.auth.token`, `meow.auth.user`.
- Environment variables: `MEOW_LOG_SMS_CODE`, `MEOW_DEV_SMS_CODE`.
- Historical files: `MEOW_CIRCLE_*`, `_stitch_ref`, `STITCH_WEB_*`.
- Internal class/function names such as `MeowCircleSdk` or `Stitch*`.

Only rename compatibility names after checking routes, storage migration,
mobile/KMP clients, and any persisted user data.
