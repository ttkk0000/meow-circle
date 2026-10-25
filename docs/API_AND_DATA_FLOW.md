# API And Data Flow

> **2026-10-25 v2 Architecture & Data Flow Update (Primary Baseline)**
>
> Since 2026-10-20, all active feature development uses the **`/api/v2`** contract and modular-monolith architecture. The legacy `/api/v1` sections below are retained strictly for historical reference (`MND_ENABLE_LEGACY_AUTH=0` disables v1 writes in production).
>
> ### v2 Quick Reference
> - **Machine Contract**: [`docs/api/mobile-v2.openapi.json`](api/mobile-v2.openapi.json) (`241` Paths / `271` Operations / `123` Server Schemas; `125` Web derived schemas in `web-client/src/core/schemas.json`; `74` cross-language DTO pairs).
> - **Code Logic & PlantUML Diagrams**: [`docs/CODE_LOGIC_AND_UML_REFERENCE.md`](CODE_LOGIC_AND_UML_REFERENCE.md) & [`docs/uml/README.md`](uml/README.md) (12 standalone `.puml` sequence, state, ER, class, and component diagrams).
> - **Response Envelope (`internal/platform/mobileapi`)**:
>   - Success: `{ "ok": true, "data": <T> }`
>   - Error: `{ "ok": false, "error": "<CODE_OR_MESSAGE>" }`
> - **Authentication, Provider Auth, Age Assurance & Guardian Invites (`internal/identity`, `internal/mobile/qualification.go`)**:
>   - Opaque, revocable random session pairs (`access_token` 1h / `refresh_token` 30d, SHA-256 hashed in DB) + browser `HttpOnly Cookie` & `X-MND-CSRF` adapter (`internal/platform/mobileapi/browser.go`).
>   - Three unique login identifiers (`username`, `email`, `phone`) resolve to a single `user_id` (`migrations/005_mobile_auth_identities.sql`). Explicit separation between provider sign-in (`create`) and logged-in identity binding (`link` with `reauth`, returning `409 identity_in_use` on conflict without auto-merge).
>   - External Age Assurance gateway (`internal/identity/age_assurance.go`, `internal/platform/mobileauth/age_gateway.go`) + 15-minute single-use Guardian Invites & Independent Adult Relationship Claims (`internal/identity/guardian_invites.go`).
>   - Login is decoupled from outbound interaction eligibility (`migrations/006_identity_eligibility.sql`); restricted mutations enforce `requireInteractionContract()`, minor guardian scope (`KindGuardians`, including stranger DM `directContact` gate in `internal/mobile/social.go`), and single-use action/resource/version-bound `reauth` grants.
> - **Amap Security Proxy & Coarse Coordinate Privacy (`internal/platform/mobileapi/amap.go`, `internal/mobile/lost_pet.go`)**:
>   - Web Amap JS API 2.0 proxies through `/maps/amap/_AMapService/` with server-injected `jscode`.
>   - Public lost-pet coordinates are quantized to 2 decimal places (`GCJ-02` / `WGS-84` explicit datum isolation).
> - **Transactional Storage (`internal/store/postgres/mobile.go`, `migrations/007`–`009`)**:
>   - **44 `mobile_*` aggregate tables** (`32` in `007_mobile_modules.sql`, `10` in `008_mobile_policy_commands.sql`, `2` in `009_pet_discussions.sql`).
>   - All mutations execute inside PostgreSQL `SERIALIZABLE` transactions with optimistic `version` CAS (`409 VERSION_CONFLICT` on stale writes) and `Idempotency-Key` / `client_request_id` receipt recovery (`mobile_operations`).
> - **Asynchronous Outbox & Worker Jobs (`cmd/worker`, `internal/platform/mobileapi/jobs.go`)**:
>   - 6 independent background jobs authenticated via named operator tokens (`MND_OPERATORS_JSON` + `X-Operator-Token`): `push`, `nearby_alerts`, `content_updates`, `privacy_cleanup`, `provider_revocations`, and `maintenance`.
> - **Detailed v2 Documentation**:
>   - [Code Logic & PlantUML Reference Manual](CODE_LOGIC_AND_UML_REFERENCE.md)
>   - [PlantUML (`.puml`) Diagram Suite](uml/README.md)
>   - [Mobile Server & v2 API Handoff](server/MOBILE_SERVER.md)
>   - [Modular Monolith Architecture](server/ARCHITECTURE.md)
>   - [Server Completeness Audit & Remaining Gaps (2026-10-24)](server/SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md)
>   - [Client Implementation Plan & v2 Store](client/IMPLEMENTATION_PLAN.md)

Last updated: 2026-10-25 (v1 baseline below originally recorded 2026-05-27)

This document maps the current Go API, domain models, persistence strategy, and
client contracts.

## 1. Response Envelope

All JSON API responses use the envelope in `internal/platform/api/router.go`:

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

Created responses use HTTP `201` with `message: "created"`. Errors use the HTTP
status as `code` and a human-readable `message`.

Clients that unwrap the envelope:

- Web: page scripts call `window.MeowShared` helpers or local fetch helpers.
- Expo: `mobile/src/api.ts` uses `unwrapEnvelope`.
- KMP: `MeowCircleSdk.unwrapData()` decodes `ApiEnvelope`.

## 2. Auth And Admin

| Concern | Current Behavior |
| --- | --- |
| User auth | `Authorization: Bearer <jwt>` |
| JWT | HS256, issued by `internal/platform/auth/jwt.go`, default expiry 72h. |
| Password | PBKDF2-SHA256 with per-user salt. |
| Login rate limit | 5 failed attempts per username/IP within 10 minutes; returns 429. |
| Registration phone verification | Optional normalized phone + SMS code through `phoneotp.Store`. |
| Admin auth | Header `X-Admin-Key`, default `admin123`, override with `ADMIN_KEY`. |
| CORS | `CORS_ALLOW_ORIGIN`, default `*`; allowed headers include `Content-Type`, `Authorization`, `X-Admin-Key`. |

## 3. Route Map

### Health

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/healthz` | no | Service health and current store kind. |
| `GET` | `/readyz` | no | Readiness check. |

### Auth And Users

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | no | Register and return JWT + user. |
| `POST` | `/api/v1/auth/send-verification-code` | no | Send/create phone verification code. |
| `POST` | `/api/v1/auth/login` | no | Login and return JWT + user. |
| `GET` | `/api/v1/auth/me` | bearer | Current user. |
| `PATCH` | `/api/v1/me` | bearer | Update nickname/avatar/bio. |
| `GET` | `/api/v1/me/action-counts` | bearer | Compact badge counts for unread notifications, unread messages, and order actions. |
| `GET` | `/api/v1/users/{id}` | no | Public profile. |

### Search

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/search?q=&type=all|post|listing` | no | Search posts and listings. |

### Posts And Social

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/posts?page=&page_size=&filter=rec|follow` | optional | Feed list with author, like count, liked state, first media. |
| `POST` | `/api/v1/posts` | bearer | Create post after moderation and media ownership checks. |
| `GET` | `/api/v1/posts/{id}` | optional | Post detail with author, media, comments, likes, follow state. |
| `PATCH` | `/api/v1/posts/{id}` | bearer owner | Update own post. |
| `DELETE` | `/api/v1/posts/{id}` | bearer owner | Delete own post. |
| `POST` | `/api/v1/posts/{id}/comments` | bearer | Add comment and notify post author. |
| `POST` | `/api/v1/posts/{id}/like` | bearer | Toggle like. |
| `GET` | `/api/v1/me/posts` | bearer | Current user's posts. |
| `POST` | `/api/v1/me/follow/{userID}` | bearer | Follow user. |
| `DELETE` | `/api/v1/me/follow/{userID}` | bearer | Unfollow user. |

### Listings And Market

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/listings?page=&page_size=` | no | Listing feed. |
| `POST` | `/api/v1/listings` | bearer | Create listing after moderation and media ownership checks. |
| `GET` | `/api/v1/listings/{id}` | no | Listing detail with media. |
| `PATCH` | `/api/v1/listings/{id}` | bearer owner | Update own listing. |
| `DELETE` | `/api/v1/listings/{id}` | bearer owner | Delete own listing. |
| `GET` | `/api/v1/me/listings` | bearer | Current user's listings. |

### Media And Reports

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/media` | bearer | Current user's media. |
| `POST` | `/api/v1/media` | bearer | Multipart upload, field name `file`. |
| `GET` | `/api/v1/media/{id}` | no | Media detail. |
| `DELETE` | `/api/v1/media/{id}` | bearer owner | Delete own media. |
| `POST` | `/api/v1/reports` | bearer | Report post/comment/listing/media. |
| `GET` | `/uploads/{filename}` | no | Static uploaded media file. |

### Messages And Notifications

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/notifications?unread=true` | bearer | Notifications and unread count. |
| `POST` | `/api/v1/notifications/{id}/read` | bearer | Mark one notification read. |
| `POST` | `/api/v1/notifications/read-all` | bearer | Mark all notifications read. |
| `GET` | `/api/v1/me/conversations` | bearer | Conversation list. |
| `GET` | `/api/v1/me/conversations/{peerID}` | bearer | Messages with peer, marks conversation read. |
| `POST` | `/api/v1/messages` | bearer | Send message after moderation and notify recipient. |

### Orders And Payments

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/payments/methods` | no | Supported payment methods. |
| `POST` | `/api/v1/orders` | bearer buyer | Create order from listing. |
| `GET` | `/api/v1/orders/{id}` | bearer participant | Order detail. |
| `POST` | `/api/v1/orders/{id}/pay` | bearer buyer | Pay pending order. |
| `POST` | `/api/v1/orders/{id}/cancel` | bearer buyer | Cancel pending order. |
| `POST` | `/api/v1/orders/{id}/ship` | bearer seller | Mark paid order shipped. |
| `POST` | `/api/v1/orders/{id}/complete` | bearer buyer | Confirm shipped order received. |
| `POST` | `/api/v1/orders/{id}/refund` | bearer seller | Refund paid/shipped order. |
| `GET` | `/api/v1/me/orders?role=buyer|seller` | bearer | Current user's orders. |

### Admin

All admin endpoints require `X-Admin-Key`.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/admin/summary` | Counts for users/posts/listings/comments/reports/media/orders. |
| `GET` | `/api/v1/admin/posts` | All posts. |
| `DELETE` | `/api/v1/admin/posts/{id}` | Delete post. |
| `GET` | `/api/v1/admin/comments` | All comments. |
| `DELETE` | `/api/v1/admin/comments/{id}` | Delete comment. |
| `GET` | `/api/v1/admin/listings` | All listings. |
| `DELETE` | `/api/v1/admin/listings/{id}` | Delete listing. |
| `GET` | `/api/v1/admin/media?status=pending|approved|rejected` | Media moderation queue. |
| `POST` | `/api/v1/admin/media/{id}/approve` | Approve media. |
| `POST` | `/api/v1/admin/media/{id}/reject` | Reject media. |
| `DELETE` | `/api/v1/admin/media/{id}` | Force delete media. |
| `GET` | `/api/v1/admin/reports?status=open|resolved|dismissed` | Report queue. |
| `GET` | `/api/v1/admin/reports/{id}` | Report detail. |
| `POST` | `/api/v1/admin/reports/{id}/resolve` | Resolve report. |
| `POST` | `/api/v1/admin/reports/{id}/dismiss` | Dismiss report. |
| `GET` | `/api/v1/admin/orders` | All orders. |
| `GET` | `/api/v1/admin/audit-logs?limit=100` | Audit log list. |

## 4. Core Domain Model Map

| Model | Important Fields | Used By |
| --- | --- | --- |
| `User` | username, nickname, phone, avatar, bio, password hash/salt | Auth, profile, posts, messages, orders. |
| `Post` | author, title, content, category, tags, media IDs, last reply | Feed, detail, comments, search. |
| `Comment` | post, author, content | Post detail, notifications, admin. |
| `Listing` | seller, type, title, description, price, media IDs | Market, orders, messages. |
| `Media` | owner, kind, mime, size, filename, URL, status | Uploads, post/listing attachments, review. |
| `Report` | reporter, target kind/id, reason, status, resolution | Safety/admin. |
| `Order` | buyer, seller, listing, amount, status, payment method/tx | Commerce workflow. |
| `Notification` | user, kind, title/body, ref, actor, image, read | Notification center and badges. |
| `Message` | sender, recipient, content, read | Direct messages. |
| `AuditLog` | actor, action, target, note, IP | Admin accountability. |

## 5. Order State Machine

```mermaid
stateDiagram-v2
  [*] --> pending_payment
  pending_payment --> paid: pay
  pending_payment --> cancelled: cancel
  paid --> shipped: ship
  paid --> refunded: refund
  shipped --> completed: complete
  shipped --> refunded: refund
  completed --> [*]
  cancelled --> [*]
  refunded --> [*]
```

Guards:

- Buyer can pay and cancel.
- Seller can ship and refund.
- Only buyer can complete.
- Participants can view order detail.

## 6. Write Path Example: Create Post

```mermaid
sequenceDiagram
  participant C as Client
  participant R as Go Router
  participant A as JWT/Auth
  participant F as Audit Filter
  participant S as Store
  participant N as Notifications

  C->>R: POST /api/v1/posts + Bearer token
  R->>A: parse token
  A-->>R: user claims
  R->>F: check title/content
  F-->>R: accepted or keyword hit
  R->>S: validate media ownership
  R->>S: CreatePost
  R-->>C: 201 envelope(data=post)
  Note over R,N: Comments/messages/orders create notifications later.
```

## 7. Client Contract

| Client | Contract Notes |
| --- | --- |
| Web | Uses `localStorage` keys `meow_token` and `meow_user`; do not rename without migrating scripts. |
| Expo | Uses SecureStore keys `meow.auth.token` and `meow.auth.user`; API base from `EXPO_PUBLIC_API_URL`, Android emulator fallback `10.0.2.2`. |
| KMP | `MeowCircleSdk` stores token/user JSON in `SessionStore`, uses Ktor, and points Android emulator at `10.0.2.2` by default. |

## 8. Persistence Notes

The `Store` interface currently returns zero values/false rather than propagating
errors. Postgres implementations log errors internally. This keeps handlers
simple for MVP, but means production-grade observability should include logs and
admin/health checks.

Redis cache is a read-through decorator for selected reads. Writes evict related
keys. User-specific lists and participant-specific data bypass shared cache to
avoid cross-user leakage.

## 9. Change Checklist For Domain/API Work

When adding a field or feature:

1. Update `internal/domain/models.go`.
2. Update `internal/store/store.go` if persistence behavior changes.
3. Update `internal/store/memory.go`.
4. Update relevant `internal/store/postgres/*.go`.
5. Add a new migration.
6. Update API handler validation/response.
7. Update Web scripts and i18n copy if visible.
8. Update `mobile/src/api.ts` types and KMP `Models.kt`.
9. Update docs in `docs/`.
10. Run only checks approved for the current machine. On this machine, default to static review and Web smoke checks; do not run Go build/run/test or Gradle.
