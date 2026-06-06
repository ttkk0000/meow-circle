# M&D Design Memory

Created: 2026-05-26

Last updated: 2026-06-03

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
- Reworked `web/stitch-theme-bridge.css` so existing functional pages use M&D colors, typography, radius, and overall cute token direction.
- Updated key existing page visible branding from MEOW/Kitty Circle/喵友圈 to M&D.
- Updated README/onboarding/docs to explain M&D and the cat-first/doggie-friendly rule.
- Added this memory doc and root `AGENTS.md` so future agents inherit the design context.
- Continued production Web migration after the redesign:
  - `web/market.html` / `web/market.js` now have M&D market hero, search/type filters, image cards, and static fallback listings.
  - `web/post.js` now renders an M&D demo post detail when opened without an id or backend.
  - `web/messages.js` now renders demo conversations when logged out or when conversation APIs are unavailable.
  - `web/profile.html` / `web/profile.js` now support M&D profile background switching and demo profile content.
- Second alignment pass:
  - `web/stitch-theme-bridge.css` now includes Honey/Mint/Night/Neutral M&D tokens, stronger card/button/form overrides, hidden prior decorative blur backgrounds, and shared M&D helpers.
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
- Documentation pass:
  - Added `docs/README.md` as the docs hub.
  - Added `docs/design/MND_UI_ALIGNMENT_GUIDE.md` as the current implementation-facing UI contract.
  - Added `docs/PROJECT_STRUCTURE.md` to map backend, Web, Expo, KMP, infra, migrations, and common edit entry points.
  - Added `docs/API_AND_DATA_FLOW.md` to map API routes, auth/admin behavior, core models, order state, clients, and persistence notes.
  - Added `docs/FRONTEND_SURFACE_MAP.md` to map production Web, WebUI, design boards, Expo routes, and KMP screens.
  - Added `docs/RUNBOOK.md` to capture local run modes, env vars, upload storage, verification matrix, deployment notes, and known local blockers.
  - Expanded `docs/design/MND_FIGMA_DESIGN_DOCUMENT.md` with current status and immediate Figma work order.
  - Historical design containers were later removed during the Stitch V2 reset.
- Runtime client interaction pass:
  - Added `docs/design/MND_CLIENT_INTERACTION_SPEC.md` as the page-by-page runtime client interaction contract.
  - Started KMP Android home/feed realignment from the Figma-ready M&D direction: compact top bar, editorial home header, restrained warm search field, better feed loading/empty/error states, less candy-pink Sugar palette, stronger middle compose tab, and image/placeholder-led feed cards.
  - KMP shared SDK now tolerates `items: null` for posts and conversations by treating it as an empty list, preventing Kotlin JSON serializer messages from appearing in the feed.

## Implementation Notes

- 2026-06-06 Stitch remote reset:
  - Current source of truth is Stitch project `projects/13275961100622290348`.
  - Downloaded remote assets now live in `.stitch/remote-assets`; browser-visible full-size screenshot and source copies live in `web/assets/stitch-remote`.
  - `.stitch/DESIGN.md` mirrors `MD_GLOBAL_STITCH_DESIGN_SYSTEM_V2_NEUTRAL_FIXED.md`.
  - Root `DESIGN.md` and `docs/design/MND_UI_ALIGNMENT_GUIDE.md` were rewritten around the Stitch V2 rules.
  - `web/pawpop.html`, `web/mnd-web-client-board.html`, `web/stitch-remote-gallery.html`, `web/cute.html`, `web/pawpop-desktop.html`, and `web/stitch-theme-bridge.css` now use the remote Honey/Mint/Night/Neutral system.
  - Earlier handmade reference exports, old Stitch maps, and prior-generation design docs were removed. Do not restore them as design sources.

- 2026-06-06 remote asset clarity fix:
  - `scripts/sync-stitch-remote-assets.ps1` now requests Googleusercontent screenshots with `=s0`, so local screenshots keep original Stitch canvas dimensions instead of 512px thumbnails.
  - Stitch HTML/SVG/Markdown sources are copied into both `.stitch/remote-assets/sources` and `web/assets/stitch-remote/sources`.
  - Current themes: Honey for Feed/Profile/Messages/Orders, Mint for Market/Product flows, Night for dark mode, Neutral for restrained docs, creator tools, dashboards, and explicit admin/moderation surfaces.
- 2026-06-06 AI handoff rule:
  - Added `docs/design/MND_STITCH_AI_RULES.md`, plus `CLAUDE.md` and `GEMINI.md`, so other AI agents know the current Stitch project and must follow the local Stitch mirror instead of inventing a new UI direction. Agents may suggest improvements, but implementation must map to the existing Stitch V2 rules unless the user approves a deviation.
- `web/cute.html` is now a static remote-aligned WebUI prototype for visual parity. It does not call the Go API.
- Auth localStorage keys in production scripts remain `meow_token` and `meow_user`; the current static WebUI prototype does not depend on auth.
- Theme localStorage keys for the current WebUI prototype are `mnd_theme` and `mnd_profile_bg`.
- Production-ish static fallbacks are intentional for design review. Use `?live=1` on pages that support it when validating against the Go API.
- Latest static UI verification used `python3 -m http.server 4173` from `web/` plus Playwright desktop/mobile checks for discover, compose, login, register, dashboard, admin, home, market, messages, profile, and post pages. No horizontal overflow or unhandled page errors were found in the checked states.
- Latest follow-up verification also checked 14 Web/UI sources at desktop `1440x1000` and mobile `390x844`: `/`, discover, compose, market, messages, profile, login, register, dashboard, admin, post, mobile board, desktop board, and WebUI. Checks passed for page errors, horizontal overflow, removed-brand visible terms, residual blur backgrounds, oversized non-action radii, and negative letter spacing.
- Do not run Gradle or Go build/run/test on this machine. Use static source review and browser/Web smoke checks for UI work.
- Current repository has WebUI, HTML desktop design-board sources, Expo mobile, KMP Android, and a KMP Compose Desktop client in `kmp/desktopApp`.

## How Future Agents Should Continue

- Start with `docs/README.md`.
- Use `docs/RUNBOOK.md` when you need to run, validate, or deploy the project.
- Use `docs/design/MND_UI_ALIGNMENT_GUIDE.md` as the source of truth for UI alignment.
- Preserve behavior before renaming internal compatibility identifiers.
- Prefer adding repeatable smoke-test scripts instead of relying only on manual browser checks.
- Keep this memory file updated after any meaningful design, implementation, or validation pass.

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
