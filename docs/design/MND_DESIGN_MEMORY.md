# M&D Design Memory

Date: 2026-05-26

## Decision

The product is now positioned as **M&D = meow & doggie**.

The emphasis is **cat-first**: cats lead the emotional identity, default imagery, feed content, profile examples, and community language. Doggie content is still part of the system, especially in marketplace, services, activities, and adoption.

## What Changed

- Rebranded the cute WebUI from the older Pawpop/MEOW direction to M&D.
- Updated the three design-board entries:
  - `web/pawpop.html`
  - `web/pawpop-mobile.html`
  - `web/pawpop-desktop.html`
  - `web/cute.html`
- Added missing desktop Figma-detail surfaces:
  - Search/results/empty state
  - Admin review detail with audit/action layout
- Reworked `web/stitch-theme-bridge.css` so legacy functional pages use M&D colors, typography, radius, and overall cute token direction.
- Updated key legacy page visible branding from MEOW/Kitty Circle/喵友圈 to M&D.
- Updated README/onboarding/docs to explain M&D and the cat-first/doggie-friendly rule.
- Added this memory doc and root `AGENTS.md` so future agents inherit the design context.
- Continued production Web migration after the redesign:
  - `web/market.html` / `web/market.js` now have M&D market hero, search/type filters, image cards, and static fallback listings.
  - `web/post.js` now renders an M&D demo post detail when opened without an id or backend.
  - `web/messages.js` now renders demo conversations when logged out or when conversation APIs are unavailable.
  - `web/profile.html` / `web/profile.js` now support M&D profile background switching and demo profile content.
- Second alignment pass:
  - `web/stitch-theme-bridge.css` now includes light/dark/warm M&D tokens, stronger card/button/form overrides, hidden legacy blur blobs, and shared M&D helpers.
  - `web/discover.html` / `web/discover.js` now have an M&D circle hero, real cat imagery, theme controls, and cat-first circle cards.
  - `web/compose.html` now has an M&D compose hero, theme controls, and cat-first placeholders/labels.
  - `web/login.html` and `web/register.html` now carry M&D brand panels and cat-first/doggie-friendly copy.
  - `web/dashboard.html`, `web/dashboard.css`, `web/admin.html`, and `web/admin.css` now use the M&D workbench/admin direction while preserving existing JS hooks and route behavior.
- Third alignment pass:
  - Expo mobile tokens now use the M&D palette, 8px panel radius, zero letter spacing, and M&D shadow color.
  - Expo visible surfaces now use M&D copy and cat-first defaults across splash/loading, auth, top bar, feed, discover, compose, market borders, messages, profile, and post detail.
  - The old `KittyLoader` export remains as a compatibility alias, but the visible loader is `MndLoader` and renders `M&D`.
  - Expo app metadata now uses the `M&D` name, `mnd` slug, and M&D cream splash/adaptive icon background.
  - KMP Android tokens now use M&D colors, 8dp shapes, zero letter spacing, and M&D pink shadows.
  - KMP Android visible surfaces now use M&D copy across manifest label, top bar, splash, login, register, feed/discover, compose, and post detail fallback identity.
  - Web design-board radii were tightened so ordinary mobile/desktop cards, inputs, panels, and chat bubbles use 8px; pills and device mock shells remain intentionally rounded.

## Implementation Notes

- `web/cute-ui.js` fetches live posts/listings only when opened with `?live=1`; by default it avoids API calls so static previews stay clean.
- `web/cute-ui.js` includes basic API wiring for login, register, compose post, and compose listing.
- Auth localStorage keys remain `meow_token` and `meow_user` because existing production scripts expect them.
- Theme localStorage keys for the cute WebUI are now `mnd_cute_theme` and `mnd_cute_profile_bg`.
- Production-ish static fallbacks are intentional for design review. Use `?live=1` on pages that support it when validating against the Go API.
- Latest static UI verification used `python3 -m http.server 4173` from `web/` plus Playwright desktop/mobile checks for discover, compose, login, register, dashboard, admin, home, market, messages, profile, and post pages. No horizontal overflow or unhandled page errors were found in the checked states.
- Latest follow-up verification also checked 14 Web/UI sources at desktop `1440x1000` and mobile `390x844`: `/`, discover, compose, market, messages, profile, login, register, dashboard, admin, post, mobile board, desktop board, and WebUI. Checks passed for page errors, horizontal overflow, old visible terms, visible legacy blur blobs, oversized non-action radii, and negative letter spacing.
- Expo `npm run typecheck` passed after local dependencies were installed. Current shell Node is `v16.10.0`, which causes React Native engine warnings; use Node 18+ for normal mobile development.
- KMP compile was attempted with `bash ./gradlew :androidApp:compileDebugKotlin`. The Gradle wrapper downloaded, but dependency resolution was blocked by Maven/Gradle Plugin Portal HTTP 403 responses before Kotlin compilation began.
- Current repository has WebUI and HTML desktop design-board sources, plus Expo mobile and KMP Android. No separate native desktop app source tree exists beyond the desktop board and browser/admin/workbench surfaces.

## Figma Handoff

There is no active `use_figma` tool in this session, so the repo contains Figma-ready source artifacts instead of a directly-written Figma file.

Recommended Figma pages:

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

Use the local HTML pages as visual source for Figma frame creation.
