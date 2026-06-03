# M&D UI Alignment Guide

Last updated: 2026-05-27

This is the implementation-facing design contract for the current M&D UI. Use it
when changing Web, Expo mobile, KMP Android, or Figma frames.

## 1. Product Contract

M&D means `meow & doggie`.

Non-negotiables:

- The visible mark is `M&D`.
- The small lockup is `meow & doggie`.
- The product is cat-first and doggie-friendly.
- Cat imagery leads home, feed, auth, profile, and default examples.
- Doggie imagery is allowed for activities, services, marketplace, adoption, and
  secondary stories.
- Do not simplify the brand to `MD`, `Meow & Dog`, or a generic pet community.

## 2. Visual Contract

| Rule | Current Decision |
| --- | --- |
| Tone | Cute, polished, teen/young-adult friendly, not babyish. |
| Primary content | Real cat-first imagery; doggie imagery only when context supports it. |
| Radius | Cards, panels, inputs, and tiles use `8px` / `8dp`; pills remain fully rounded. |
| Letter spacing | Use `0`; avoid negative display tracking. |
| Decoration | No decorative gradient blobs or bokeh/orbs. Use imagery, stripes, panels, badges, and clear hierarchy. |
| Themes | Sugar, Mint, Night for WebUI/design boards; Web legacy pages map light/dark/warm through the bridge. |
| Profile backgrounds | Picnic, Desk, Arcade, Garden. |
| Admin/workbench | Denser and calmer than user pages, but still uses the M&D token family. |

## 3. Token Snapshot

| Token | Value | Notes |
| --- | --- | --- |
| Canvas | `#FFF7EE` | Warm cream default. |
| Surface | `#FFFFFF` / `#FFFAF4` | White cards and cream panels. |
| Soft surface | `#FFF0F6` | Pink-tinted panels. |
| Ink | `#2B1722` | Berry-black text. |
| Muted | `#7F6172` | Secondary copy. |
| Primary | `#FF4F93` | M&D pink. |
| Primary strong | `#DB3177` | Hover/strong text. |
| Accent | `#56C7FF` | Bubble blue. |
| Secondary | `#FFD84D` | Lemon support color. |
| Success | `#006B54` | Trust/success status. |
| Radius | `8px` / `8dp` | Standard UI radius. |
| Pill radius | `9999px` | Chips, switches, compact actions. |

Implementation sources:

- Web bridge tokens: `web/stitch-theme-bridge.css`
- Tailwind CDN tokens: `web/tw-stitch.js`
- WebUI tokens: `web/cute-ui.css`
- Mobile tokens: `mobile/src/theme.ts`
- KMP tokens: `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/theme`

## 4. Platform Mapping

### WebUI

Primary source:

- `web/cute.html`
- `web/cute-ui.css`
- `web/cute-ui.js`

Expected coverage:

- Home/feed, post detail, discover, market, messages, auth, profile background
  switching, compose post, compose listing, media, orders, notifications, safety.
- Static-safe by default.
- Live backend hydration only when opened with `?live=1`.

### Production Web Pages

Primary source:

- `web/stitch-theme-bridge.css`
- `web/shared.js`
- Page scripts such as `web/app.js`, `web/discover.js`, `web/market.js`,
  `web/messages.js`, `web/profile.js`, `web/post.js`, `web/dashboard.js`,
  and `web/admin.js`.

Rules:

- Preserve existing routes and API hooks.
- Preserve auth storage keys `meow_token` and `meow_user`.
- Keep demo/static fallbacks where they support design review without a running
  backend.
- Keep old `rounded-2xl` / `rounded-3xl` Tailwind classes visually mapped to
  `8px` through the bridge unless the element is an intentional pill/action.

### Design Boards

Primary source:

- Overview: `web/pawpop.html`
- Mobile board: `web/pawpop-mobile.html`
- Desktop board: `web/pawpop-desktop.html`

Rules:

- These are Figma source boards, not production apps.
- Mobile, Desktop, and WebUI are independent UI sets.
- Desktop board is the current desktop deliverable. The repo does not currently
  contain a separate native desktop app source tree.

### Expo Mobile

Primary source:

- `mobile/src/theme.ts`
- `mobile/src/components.tsx`
- `mobile/src/stitch/*`
- `mobile/app/(auth)`
- `mobile/app/(tabs)`
- `mobile/app.json`

Rules:

- Use `MndLoader` for visible loading states.
- `KittyLoader` may remain only as a compatibility alias.
- Keep category language cat-first: `猫猫日常`, `猫猫新手村`, `活动`, `好物交易`.
- Keep app metadata visible name as `M&D`.

### KMP Android

Primary source:

- `kmp/androidApp/src/main/AndroidManifest.xml`
- `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/theme/*`
- `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/ui/*`

Rules:

- Manifest label is `M&D`.
- Typography letter spacing is `0.sp`.
- Shapes use `8.dp` except true pills.
- Splash, login, register, feed, discover, compose, and detail screens use
  M&D copy.
- Runtime Sugar theme has been toned down from high-saturation candy pink toward
  warm cream + warm crimson for the first mobile client pass.

## 5. Component Expectations

| Component | Required Behavior |
| --- | --- |
| Brand lockup | Show `M&D` plus `meow & doggie` where space allows. |
| Top navigation | Keep active state obvious, with icon support where available. |
| Buttons | Primary actions use M&D pink; icon buttons should use recognizable icons and accessible labels. |
| Cards | 8px radius, real content/image first, no nested card decoration. |
| Search | Search input is prominent on feed/discover/market, with static fallback behavior for design review. |
| Feed tile | Cat-first imagery, concise title, visible category/status metadata. |
| Listing card | Price, seller/trust signal, type, and image must be scannable. |
| Messages | Demo conversations are allowed when logged out or API is unavailable. |
| Profile background picker | Persist the selected background locally where implemented. |
| Admin rows | Dense rows, clear status chips, visible action affordances and audit context. |

## 6. Copy Rules

Use:

- `M&D`
- `meow & doggie`
- `猫猫日常`
- `猫猫新手村`
- `M&D 伙伴`
- `写给猫猫宇宙的一条新动态`
- `猫猫是主角，doggie 也有座位`

Avoid as visible product language:

- `MEOW`
- `Kitty Circle`
- `Pawpop`
- `喵友圈`
- `铲屎官`
- `布偶猫交流群`
- Overly childish baby talk.
- Internal API or moderation terms on normal user pages.

Exception: legacy names may appear only in migration notes, comments that identify
old references, package/module names that would be risky to rename, or environment
variables such as `MEOW_LOG_SMS_CODE`.

## 7. Figma Handoff Rules

Create or maintain these Figma pages:

- `00 Cover`
- `01 Product Map`
- `02 M&D Tokens`
- `03 Components`
- `04 Mobile App`
- `05 Desktop App`
- `06 WebUI`
- `07 Profile Customization`
- `08 Trust & Commerce`
- `09 Admin Console`
- `10 Prototypes`
- `11 Future Concepts`

Frame naming:

- `Mobile / Home Feed`
- `Mobile / Discover`
- `Mobile / Compose`
- `Mobile / Auth`
- `Desktop / Home Workbench`
- `Desktop / Search & States`
- `Desktop / Admin Review Detail`
- `WebUI / Home`
- `WebUI / Market`
- `WebUI / Messages`
- `Admin / Reports`
- `Admin / Media Review`

Each frame should include:

- Happy state.
- Empty state when relevant.
- Loading or skeleton state when relevant.
- Error/blocked state when relevant.
- Notes on API dependency if the state is not static.

## 8. QA Checklist

Run or manually verify before handing off a UI change:

- No horizontal overflow at mobile `390px` and desktop `1440px`.
- No visible old product names.
- No negative letter spacing.
- No decorative blur blobs/orbs.
- Non-action cards, panels, inputs, and tiles are visually `8px`.
- Buttons and chips fit their labels.
- Important images render and are not dark/blurred/cropped beyond recognition.
- Theme switching still works where implemented.
- Profile background switching still works where implemented.
- Static Web pages remain useful without a running Go API.
- Auth/API behavior is not broken by visual changes.

Recommended commands:

```bash
# Verify static HTML pages, brand terms alignment, and check for broken local links
python scripts/verify_web_smoke.py

# Typecheck Expo mobile app
cd mobile && npm run typecheck

# Check for check warnings in git
git diff --check
```

For Web visual smoke tests, check:

- `/`
- `/discover.html`
- `/compose.html`
- `/market.html`
- `/messages.html`
- `/profile.html`
- `/login.html`
- `/register.html`
- `/dashboard.html`
- `/admin.html`
- `/post.html`
- `/pawpop-mobile.html`
- `/pawpop-desktop.html`
- `/cute.html`

## 9. Known Local Blockers

- **Go version parser**: **Resolved** (Running Go v1.26.2 locally).
- **Node.js version**: **Resolved** (Running Node.js v24.15.0 locally).
- **KMP Gradle dependency resolution 403**: **Mitigated** (Optional Aliyun mirror sources have been added in `kmp/settings.gradle.kts` to bypass the 403 resolution blocks).
- **kmp/gradlew execute bit**: Use `bash ./gradlew ...` on Windows shells or if execute bit is missing.

## 10. Next Useful Improvements

- Add Figma export screenshots or direct Figma nodes once a `use_figma` tool is available.
- **Automated Web smoke checklist**: **Implemented** (Created a lightweight python-based automated smoke test script in `scripts/verify_web_smoke.py`).
- Add native screenshots for Expo and KMP once the local Node/Gradle blockers are cleared.
- Continue replacing risky internal legacy names only when it does not affect route/API/package compatibility.

## Related Engineering Docs

- `docs/PROJECT_STRUCTURE.md`: repository structure, runtime entry points,
  backend layers, infra, and common task entry points.
- `docs/API_AND_DATA_FLOW.md`: route map, domain model map, order state machine,
  auth/admin contract, and client persistence notes.
- `docs/FRONTEND_SURFACE_MAP.md`: Web, WebUI, design board, Expo, and KMP screen
  inventory.
- `docs/RUNBOOK.md`: local run modes, env vars, verification matrix, deployment
  notes, and known local blockers.
