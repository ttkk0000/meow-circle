# Project Structure

Last updated: 2026-06-06

This document maps the real repository structure for M&D. It is meant for
engineers and future agents who need to understand where a feature lives before
editing it.

## 1. High-Level Shape

```text
meow-circle/
├── cmd/server/                  # Go process entry
├── internal/                    # Go domain, API, auth, store, payment, audit
├── migrations/                  # PostgreSQL schema migrations
├── web/                         # Static Web app, WebUI prototype, design boards
├── mobile/                      # Expo / React Native app
├── kmp/                         # Kotlin Multiplatform SDK + Android Compose app
├── docs/                        # Project, design, handoff, and implementation docs
├── scripts/                     # Utility scripts
├── Caddyfile                    # Caddy reverse proxy config
├── Dockerfile                   # Production Go server image
├── docker-compose.yml           # Local Postgres + Redis
├── Makefile                     # Common local commands
└── AGENTS.md                    # Agent memory for M&D design direction
```

The backend is intentionally framework-light: standard-library `net/http`,
manual routing with `http.ServeMux`, plain Go interfaces for persistence, and
static files served from `web/`.

## 2. Runtime Entry Points

| Runtime | Entry | Main Responsibility |
| --- | --- | --- |
| Go server | `cmd/server/main.go` | Creates `http.Server`, sets timeouts and connection guard, installs `api.NewRouter()`, handles graceful shutdown. |
| API router | `internal/platform/api/router.go` | Builds store, auth, payment, moderation, CORS, static file serving, and `/api/v1/*` routes. |
| Static Web | `web/index.html` | Public feed/home, loading static assets and API scripts. |
| WebUI prototype | `web/cute.html` | Independent M&D browser product prototype. |
| Design boards | `web/pawpop.html`, `web/pawpop-mobile.html`, `web/pawpop-desktop.html` | Figma-ready source boards for the three UI sets. |
| Expo app | `mobile/app/_layout.tsx` | Root Expo Router layout and auth provider. |
| KMP Android | `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/MainActivity.kt` | Android Compose host for `MeowApp`. |
| KMP Desktop | `kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/Main.kt` | Compose Desktop client aligned to the Stitch V2 desktop direction. |
| KMP shared SDK | `kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/MeowCircleSdk.kt` | Shared Kotlin HTTP/session client mirroring the Go API envelope. |

## 3. Backend Layers

| Layer | Path | Notes |
| --- | --- | --- |
| Domain | `internal/domain/models.go` | Core JSON/domain models: `User`, `Post`, `Comment`, `Listing`, `Media`, `Report`, `Order`, `Notification`, `Message`, `Conversation`, `AuditLog`. |
| API | `internal/platform/api` | Route handlers, auth middleware, CORS, JSON envelope, file upload, search, messages, orders, reports, admin. |
| Auth | `internal/platform/auth` | PBKDF2 password hashing, HS256 JWT, login rate limiting. |
| Phone OTP | `internal/platform/phoneotp` | In-memory SMS code store for registration verification. |
| Audit filter | `internal/platform/audit` | Keyword moderation before posts/listings/comments/messages are accepted. |
| Payment | `internal/platform/payment` | Provider interface with Mock default and Alipay/WeChat/Stripe stubs enabled by env vars. |
| Store interface | `internal/store/store.go` | The fat `Store` interface used by every API handler. |
| Memory store | `internal/store/memory.go` | Default no-dependency development backend. |
| PostgreSQL store | `internal/store/postgres` | `pgx/v5` implementation, split by table/capability. |
| Redis cache | `internal/store/cache` | Read-through decorator for selected read-heavy endpoints. |

### API Module Map

| File | Owns |
| --- | --- |
| `internal/platform/api/router.go` | Router construction, CORS, auth helpers, static file serving, store/payment setup, core auth handlers. |
| `internal/platform/api/feed.go` | Post feed/detail/create/update/delete, comments, likes, listings basics. |
| `internal/platform/api/search.go` | Cross-post/listing search endpoint. |
| `internal/platform/api/media.go` | Multipart upload, media metadata, upload file cleanup. |
| `internal/platform/api/message.go` | Conversations and direct messages. |
| `internal/platform/api/notification.go` | Notification list and read state. |
| `internal/platform/api/order.go` | Order lifecycle and payment dispatch. |
| `internal/platform/api/report.go` | User reports and admin moderation queues/actions. |
| `internal/platform/api/audit.go` | Admin audit log listing. |
| `internal/platform/api/profile.go` | Current user profile update and public profile lookup. |
| `internal/platform/api/me_follow.go` | Follow/unfollow behavior and related current-user helpers. |
| `internal/platform/api/api_test.go` | API-level test coverage. |

### Store Module Map

| File | Owns |
| --- | --- |
| `internal/store/store.go` | Store interface contract. |
| `internal/store/memory.go` | In-memory implementation for local/dev tests. |
| `internal/store/postgres/postgres.go` | pgx pool setup and shared helpers. |
| `internal/store/postgres/users.go` | User persistence. |
| `internal/store/postgres/posts.go` | Post persistence. |
| `internal/store/postgres/comments.go` | Comment persistence. |
| `internal/store/postgres/listings.go` | Listing persistence. |
| `internal/store/postgres/media.go` | Media persistence. |
| `internal/store/postgres/reports.go` | Report persistence. |
| `internal/store/postgres/orders.go` | Order persistence. |
| `internal/store/postgres/notifications.go` | Notification persistence. |
| `internal/store/postgres/messages.go` | Message/conversation persistence. |
| `internal/store/postgres/social.go` | Likes and follows. |
| `internal/store/postgres/audit.go` | Audit log persistence. |
| `internal/store/cache/cache.go` | Redis read-through cache decorator and cache eviction. |
| `internal/store/postgres/integration_test.go` | Postgres and optional Redis integration tests. |

### Store Selection

`api.buildStore()` chooses persistence from environment variables:

| Env | Store Kind |
| --- | --- |
| no `DATABASE_URL` | `memory` |
| `DATABASE_URL` only | `postgres` |
| `DATABASE_URL` + `REDIS_URL` | `postgres+redis` |

If PostgreSQL is unavailable, the server falls back to memory. If Redis is
unavailable, it falls back to direct PostgreSQL.

## 4. Database And Migrations

| File | Adds |
| --- | --- |
| `migrations/001_init.sql` | Users, posts, comments, listings, media, reports, orders, notifications, messages, audit logs. |
| `migrations/002_social.sql` | Post likes and follows. |
| `migrations/003_user_phone.sql` | Normalized user phone and unique index for non-empty phones. |
| `migrations/004_notifications_actor_image.sql` | Notification actor metadata and image preview URL. |

`docker-compose.yml` mounts all four migrations into the Postgres container's
`docker-entrypoint-initdb.d` directory for first boot. `make migrate` applies the
same migrations manually to an existing database.

## 5. Web Directory Map

| Area | Files | Purpose |
| --- | --- | --- |
| Base Web app | `index.html`, `app.js`, `theme.css`, `styles.css`, `shared.js` | Public feed/home, i18n/theme helpers, shared tokens. |
| M&D bridge | `stitch-theme-bridge.css`, `tw-stitch.js`, `stitch-toast.css` | Maps existing Tailwind/static pages onto M&D tokens and toast behavior. |
| Auth | `login.html`, `register.html`, `auth.js` | Login/register pages with return-to behavior. |
| Content | `discover.html/js`, `compose.html/js`, `post.html/js` | Circles, post creation, post detail/comment/report. |
| Marketplace | `market.html/js` | Listing browsing with M&D static fallback. |
| Messages/profile | `messages.html/js`, `profile.html/js` | Demo-safe conversations and profile background customization. |
| User workbench | `dashboard.html/js/css` | Logged-in creator/seller/admin-adjacent workspace. |
| Platform admin | `admin.html/js/css` | `X-Admin-Key` protected moderation and audit tools. |
| Architecture page | `architecture.html/js/css`, `architecture-data.json` | Visual system/API architecture reference page. |
| WebUI prototype | `cute.html`, `cute-ui.css`, `cute-ui.js` | Independent browser product design and implementation target. |
| Design boards | `pawpop.html`, `pawpop-mobile.html`, `pawpop-desktop.*`, `mnd-web-client-board.*`, `stitch-remote-gallery.html` | Current Stitch V2 overview, mobile, desktop, sync, and gallery boards. |
| Stitch remote assets | `assets/stitch-remote/screens`, `assets/stitch-remote/sources` | Browser-visible full-size Stitch screenshots and HTML/SVG/Markdown sources. |

## 6. Expo Mobile Map

| Area | Files | Purpose |
| --- | --- | --- |
| App metadata | `mobile/app.json` | Visible app name, slug, icon/splash config. |
| Router | `mobile/app` | Expo Router file routes: auth stack, tabs, nested messages/post routes. |
| Auth | `mobile/src/auth.tsx` | `AuthProvider`, login/register/logout state. |
| API client | `mobile/src/api.ts` | Typed fetch client, token storage, media URL resolution, API envelope unwrap. |
| Theme | `mobile/src/theme.ts` | M&D mobile colors, radius, typography, spacing, elevations. |
| Shared UI | `mobile/src/components.tsx` | Screen, Card, Button, Input, Txt, Pill, M&D loader. |
| Stitch-era components | `mobile/src/stitch/*` | Reused components now visually aligned to M&D. Internal names may remain for compatibility. |

The Expo app stores tokens under `meow.auth.token` / `meow.auth.user` through
SecureStore, falling back to `localStorage` on Web.

## 7. KMP Map

| Area | Files | Purpose |
| --- | --- | --- |
| Shared SDK | `kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle` | Ktor client, session store, models, API envelope, humanized failures. |
| Android app | `kmp/androidApp` | Compose Android app using the shared SDK. |
| Android theme | `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/theme` | M&D palette, typography, shapes, shadows. |
| Android UI | `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/ui` | Splash, login, register, feed, compose, post detail. |
| Desktop app | `kmp/desktopApp` | Compose Desktop app using the shared SDK and Stitch V2 desktop refs. |
| Desktop theme | `kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/MndDesktopTheme.kt` | Honey, Mint, Night, and Neutral desktop tokens. |
| iOS note | `kmp/ios/README.md` | Guidance for future SwiftUI/iOS integration around the shared SDK. |

KMP versions are centralized in `kmp/gradle/libs.versions.toml`:

- Kotlin `2.0.21`
- Android Gradle Plugin `8.7.2`
- Ktor `3.0.3`
- Compose BOM `2024.10.01`
- Compose Multiplatform `1.7.3`

## 8. Infra And Commands

| File / Command | Purpose |
| --- | --- |
| `Makefile` | `make run`, `make test`, `make up`, `make migrate`, mobile helpers, Docker helpers. |
| `docker-compose.yml` | Local Postgres 16 and Redis 7. |
| `Dockerfile` | Multi-stage Go build into distroless nonroot runtime. |
| `Caddyfile` | Public `:80` Caddy reverse proxy to Go on `127.0.0.1:8080`. |
| `SECURITY.md` | Security advisory routing. |
| `.github/workflows` | CI workflows, if enabled in GitHub. |

See `docs/RUNBOOK.md` for environment variables, local modes, deployment notes,
and current machine-specific blockers.

Useful local commands for static UI review:

```bash
cd web && python3 -m http.server 4173
```

Do not run Gradle or Go build/run/test commands on this machine. Ask before
running any broad verification command.

## 9. Naming And Legacy Boundaries

The product is now M&D. Some internal names remain for compatibility:

- Go module is still `kitty-circle`.
- Old Web design references and stale Stitch export maps have been removed.
- Some component/file names still include `Stitch`.
- Existing auth storage keys remain `meow_token` / `meow_user` on Web and
  `meow.auth.*` on mobile.

Do not rename these casually. Rename only when route/package/API compatibility
has been checked.

## 10. Where To Start For Common Tasks

| Task | Start Here |
| --- | --- |
| Add or change API route | `internal/platform/api/router.go`, then relevant API module file. |
| Add a domain field | `internal/domain/models.go`, `internal/store/store.go`, memory store, Postgres store, migrations, clients. |
| Change feed card shape/content | `internal/platform/api/feed.go`, `web/app.js`, `mobile/src/stitch/FeedTile.tsx`, KMP `FeedTileCard.kt`. |
| Change auth behavior | `internal/platform/api/router.go`, `internal/platform/auth`, `web/auth.js`, `mobile/src/auth.tsx`, KMP `MeowCircleSdk.kt`. |
| Change M&D visual tokens | `docs/design/MND_UI_ALIGNMENT_GUIDE.md`, then `web/stitch-theme-bridge.css`, `web/cute-ui.css`, `mobile/src/theme.ts`, KMP Android and Desktop theme files. |
| Add new Web page | Add `.html` + `.js` + CSS if needed, route static file if pretty URL is needed, update `shared.js` i18n if visible copy is shared. |
| Add mobile tab/screen | Add Expo route under `mobile/app`, extend API types if needed, reuse `mobile/src/components.tsx`. |
| Add KMP screen | Add UI Composable under KMP Android or Desktop, add SDK method/model in `kmp/shared` if API access is needed. |
