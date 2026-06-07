# M&D Technical Improvement Outline

Last updated: 2026-06-07

This outline is the execution map for improving backend, Web, Expo, KMP, and future iOS work. It assumes the current product direction stays M&D = `meow & doggie`, cat-first and doggie-friendly, and that UI work follows Stitch V2 with one active global theme at runtime: Honey, Mint, Night, or Neutral.

## 1. North Star

M&D should feel like a complete social commerce pet product, not separate demos stitched together.

The next technical push should make these loops reliable end to end:

- Share: create post, attach media, browse feed, react, comment, follow.
- Shop: publish listing, contact seller, make offer or buy now, pay, ship, complete, refund.
- Chat: message around posts/listings/orders with unread state and safety controls.
- Profile: show identity, pets, posts, listings, themes, and profile background consistently.
- Safety: report, moderate media/content, audit admin action, keep risky actions traceable.

## 2. Non-Negotiables

- Keep visible brand as `M&D` and supporting copy as `meow & doggie`.
- Keep cats as the default emotional center; use doggie content for services, activities, adoption, and secondary stories.
- Use the Stitch V2 token system; do not invent new themes or page-specific hardcoded colors.
- Runtime clients use one global theme at a time.
- Mobile navigation stays Feed, Market, Messages, Orders, Profile.
- Order states stay `pending_payment`, `paid`, `cancelled`, `shipped`, `refunded`, `completed`; do not add `delivered`.
- Preserve existing auth storage keys unless a migration is included.
- On this machine, do not run Gradle or Go build/run/test. Use static checks, Node checks, Web smoke checks, and source audits.

## 3. Backend P0: Make The API Production-Shaped

Goal: keep the current lightweight Go shape, but remove the biggest risks before adding more product surface.

- Store errors: evolve `internal/store.Store` from silent zero-value failures toward explicit errors. Start with write-heavy flows: auth, posts, listings, orders, messages, media.
- Request contexts: thread `req.Context()` into Postgres calls so timeouts, cancellation, and future tracing work.
- Migrations: move from manual SQL application to a versioned migration runner in local/CI/deploy, while keeping the existing SQL files.
- API contract: generate or maintain an OpenAPI-style route contract from `docs/API_AND_DATA_FLOW.md` plus handlers, then use it to verify clients.
- Pagination: replace list-all behavior with stable page/cursor contracts for feed, listings, messages, notifications, reports, media, admin queues.
- Validation layer: centralize input trimming, length limits, enum validation, media ownership checks, and response errors.
- Idempotency: add idempotency keys for order creation, payment, refund, and media upload retry.
- Config hardening: require non-default `ADMIN_KEY`, JWT secret, CORS allowlist, and upload directory in production mode.

## 4. Backend P1: Complete Product Loops

Feed and social:

- Feed ranking: add a simple score using recency, follow graph, likes, comments, media presence, and blocked/reported state.
- Save/bookmark: add saved posts and saved listings.
- Comment threads: support reply-to-comment and author metadata in one response.
- Follow graph: add follower/following list endpoints and profile counters that do not scan large tables.
- Post visibility: add draft/private/deleted/moderated states instead of hard deletes as the only moderation tool.

Market and orders:

- Listing lifecycle: add active/sold/paused/moderated states.
- Offer flow: buyer can make offer; seller can accept/decline/counter; accepted offer creates order.
- Order detail: include listing preview media, buyer/seller display info, and latest message/order context.
- Shipping: add shipping address snapshot, carrier, tracking number, shipment note, and timeline events.
- Refund/dispute: split refund request, seller approval, admin escalation, and final refunded status.
- Payment provider boundary: keep mock provider for dev, but formalize provider payloads and webhooks for Alipay/WeChat/Stripe.

Messages:

- Realtime path: add SSE or WebSocket for conversations, unread counts, and notifications; keep polling fallback.
- Attachments: allow images in chat after media ownership and moderation checks.
- Order/listing context: conversation can pin an order or listing preview card.
- Read receipts: keep read state per participant and expose last-read timestamps.
- Safety: block user, report conversation/message, and throttle repeated sends.

Media and moderation:

- Image processing: store width/height, generate thumbnail, reject suspicious MIME/extension mismatches.
- Video processing: store duration and poster frame; keep size and duration limits.
- Moderation queue: support pending/rejected media for all clients and reason codes for users.
- Object storage: abstract local filesystem so S3/R2/OSS can replace `data/uploads` later.

Notifications:

- Notification preferences: message/order/comment/report-handled toggles.
- Push-ready payloads: create a provider boundary for Expo push, APNs, FCM, and Web Push.
- Badges: expose one compact endpoint for unread messages, notifications, and order action counts.

## 5. Frontend P0: Make Clients Consistent

Goal: one product contract, many clients.

- Shared contract matrix: maintain a table covering Web, Expo, KMP Android, KMP Desktop, and iOS reference for every core flow.
- API client normalization: align envelope unwrap, error text, auth expiry handling, media URL resolution, and retry behavior across Web, Expo, and KMP.
- Theme consistency: all pages/components read global theme tokens; no page-specific hardcoded market/feed colors.
- Empty/loading/error states: every feed/list/detail/order/message surface gets consistent skeleton, empty, retry, and offline states.
- Navigation parity: Feed, Market, Messages, Orders, Profile must be visible in all mobile clients; desktop can expand but must keep the same areas obvious.
- Accessibility: label icon buttons, ensure touch targets, preserve text contrast for Honey/Mint/Night/Neutral, and keep dynamic text from overflowing.

## 6. Frontend P1: Product UX Completion

Production Web:

- Consolidate page helpers into one typed-ish `MeowShared` API layer and reduce duplicate fetch/error/toast code.
- Add live order and message detail flows, not only list/demo fallbacks.
- Add media upload to compose, listing publish, profile avatar, and chat attachments.
- Add listing detail page with Buy Now, Contact Seller, Make Offer, Save, seller trust, and unavailable state.
- Add notification center and global unread badges.
- Add account settings for theme, profile background, API/session state, and safety controls.

Expo:

- Finish media upload and image picking for post/listing/profile/chat.
- Add listing detail, product publish, offer flow, order detail, and chat order context.
- Add notification badge source and push registration shell.
- Add offline/demo mode switch only as a dev affordance, not mixed into normal runtime copy.
- Add typed route helpers for every dynamic route and remove stringly route duplication.

KMP Android:

- Keep Compose UI aligned to Stitch V2 and the global theme rule.
- Add order detail, listing detail, chat detail parity, media upload affordances, and notification badges.
- Add explicit retry and auth-expired states using shared SDK errors.
- Continue static validation only on this machine unless the Gradle restriction is explicitly lifted.

KMP Desktop:

- Treat desktop as a real workbench, not a stretched mobile app.
- Add dense listing/order/message split panes with the same global theme.
- Keep desktop-side profile/settings controls in sync with KMP shared session values.
- Add keyboard-friendly search, filters, and refresh actions.

iOS:

- Add a SwiftUI Stitch reference app under `kmp/ios` or a generated `iosApp` once the project is ready to commit Xcode files.
- Mirror the same five-tab navigation, theme tokens, profile background picker, orders state machine, and shared SDK calls.
- Keep `shared.framework` integration documented and make the bridge explicit for suspend functions.

## 7. Admin And Safety P0/P1

P0:

- Replace default admin key in production and show a clear setup failure when unsafe.
- Add admin login/session instead of relying only on a raw `X-Admin-Key` prompt.
- Make admin destructive actions require reason text and audit logs.
- Add moderation status filters and bulk actions for media/reports.

P1:

- User management: suspend, unsuspend, review user content, see risk signals.
- Trust and safety dashboard: reports by type, open age, action SLA, repeated reporters/targets.
- Marketplace rules: enforce prohibited item categories, missing price/address rules, and seller warnings.
- Content policy feedback: users see why media/content was rejected and how to appeal.

## 8. Data, Search, And Performance

- PostgreSQL indexes: add targeted indexes for feeds, listings, messages, notifications, orders, reports, and media status.
- Search: use `tsvector` plus `pg_trgm` for posts/listings before adding an external search engine.
- Feed cache: cache anonymous feed/listing reads; bypass or segment user-personalized reads.
- Media delivery: add cache headers and thumbnail URLs; plan CDN/object storage.
- N+1 control: batch author/media/like/follow data for feed, listings, and admin queues.
- Rate limits: extend beyond login to registration, upload, comments, messages, order actions, and reports.

## 9. Engineering System

- Test strategy: keep unit/API/integration tests in CI; local agents on this machine use static checks because Go and Gradle are blocked.
- Static checks: keep `git diff --check`, Node syntax checks for Web scripts, Expo typecheck, and custom smoke checks.
- CI matrix: backend tests, Web smoke, Expo typecheck, KMP compile on a safe CI runner, packaging dry-run, Docker build.
- Release commands: keep Makefile one-key commands for KMP Android/Desktop and RN/Expo; add iOS shared-framework command as documentation/dry-run until a real Xcode project lands.
- Feature flags: add environment-backed feature flags for offers, realtime chat, push, media moderation, and iOS beta.
- Observability: structured logs, request IDs, route latency, store backend status, payment/provider events, upload failures, and admin audit visibility.

## 10. Suggested Execution Order

Phase 1: Contract and safety foundation.

- Add this outline to docs and keep it linked from the docs hub.
- Create a client parity checklist for current Web/Expo/KMP/Desktop/iOS.
- Add API error/context/migration plan issues before broad feature coding.
- Add production config checks for admin key, JWT secret, CORS, and uploads.

Phase 2: Core user loops.

- Implement listing detail and order detail across Web and Expo first.
- Add media upload to compose/listing/profile.
- Add messages detail parity with order/listing context.
- Add notification badges and compact unread endpoint.

Phase 3: Commerce depth.

- Add offer flow.
- Add shipping/tracking timeline.
- Add refund/dispute flow.
- Add seller workbench and admin review actions.

Phase 4: Scale and polish.

- Add realtime chat/notifications.
- Add better search/ranking.
- Add push registration and delivery providers.
- Add iOS SwiftUI client once the KMP shared bridge is settled.

## 11. First Implementation Slice I Would Start With

If we are opening the throttle, the best first slice is:

1. Add a compact unread/action-count endpoint: notifications, messages, orders needing action.
2. Build listing detail and order detail contracts in backend, Web, Expo, and KMP shared models.
3. Add media upload to Expo compose and production Web compose.
4. Add order/listing context cards inside messages.
5. Add a client parity checklist that marks every surface as done, partial, or missing.

This gives the biggest product lift without prematurely rewriting the backend.

## 12. Execution Notes

- 2026-06-07: Started the first implementation slice with `/api/v1/me/action-counts`, Expo tab badges, Web shared action-count helper, Web compose trade/listing publish with media IDs, KMP Android bottom-nav badges, KMP Desktop rail badges, KMP shared action-count model/client method, Expo listing detail, Expo order detail, Expo compose media upload for posts/listings, KMP shared listing/order detail methods, package Make targets, and `CLIENT_PARITY_CHECKLIST.md`.
