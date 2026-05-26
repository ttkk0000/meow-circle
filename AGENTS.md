# Agent Memory: M&D / meow & doggie

## Product Direction

M&D means `meow & doggie`. The product is cat-first and doggie-friendly:

- Cats are the primary emotional center for hero copy, feed examples, profile identity, and default imagery.
- Doggie content is allowed as a companion branch for activities, services, marketplace items, adoption, and secondary stories.
- Do not rename the brand to `MD`, `Meow & Dog`, or generic pet community. Keep the visible mark as `M&D` and the lockup/supporting copy as `meow & doggie`.

## Current UI Work

The new cute UI direction lives in three repo-local entries:

- `web/pawpop.html`: overview for the three UI sets.
- `web/pawpop-mobile.html`: mobile design board.
- `web/pawpop-desktop.html`: desktop design board, including search/empty states and admin review detail.
- `web/cute.html`: browser WebUI prototype with navigation, theme switching, profile background switching, feed/search, marketplace, messages, auth, studio, orders, notifications, and safety.

The old Stitch pages still exist for functional coverage. Their shared visual bridge is `web/stitch-theme-bridge.css`, now mapped to the M&D cute token direction.

## Design Rules

- Cute, polished, teen/young-adult friendly, but not babyish.
- Use real cat-first imagery; use doggie imagery where the content is clearly dog/service/market related.
- Keep cards at 8px radius where possible. Pills are fine for buttons, chips, switches, and compact actions.
- No decorative gradient blobs. Use imagery, stripes, panels, badges, and clear hierarchy.
- Theme system: Sugar, Mint, Night.
- Profile background system: Picnic, Desk, Arcade, Garden.

## Implementation Notes

- `web/cute-ui.js` hydrates posts/listings from APIs only with `?live=1`, and stays static-safe by default.
- `web/cute-ui.js` also wires login/register, post compose, and listing compose to existing `/api/v1` endpoints when a server is running.
- `web/market.js`, `web/post.js`, `web/messages.js`, and `web/profile.js` include M&D static/demo fallbacks so design review works without a running Go API.
- `web/discover.html` / `web/discover.js` now use the M&D circle hero, real cat imagery, theme controls, and cat-first circle cards.
- `web/compose.html` is an M&D compose surface with a cat-first hero, theme controls, and labels/placeholders aligned to the new language.
- `web/login.html` and `web/register.html` keep their auth behavior but now carry M&D brand panels and cat-first copy.
- `web/dashboard.html`, `web/dashboard.css`, `web/admin.html`, and `web/admin.css` were visually tightened into the M&D admin/workbench direction while preserving existing JS hooks.
- Expo mobile is now aligned to M&D tokens in `mobile/src/theme.ts`, M&D loaders/top bar/copy, cat-first Feed/Discover/Compose/Auth screens, and the Expo app name/slug/background.
- KMP Android is now aligned to M&D tokens in `StitchPalette.kt`, `StitchShape.kt`, `MeowStitchTheme.kt`, plus M&D Splash/Login/Register/Feed/Compose/Detail copy and 8dp panel radius.
- The desktop design-board source is `web/pawpop-desktop.html` + `web/pawpop-desktop.css`; there is no separate native desktop app directory in this repo right now.
- Existing auth storage keys remain `meow_token` and `meow_user` to avoid breaking backend-facing flows.
- The local Go tool previously failed on this machine with `invalid go version '1.25.0': must match format 1.23`; validate static UI with Node/Playwright when Go cannot run.

## Key Docs

- `docs/design/MEOW_CIRCLE_FIGMA_DESIGN_DOCUMENT.md`: broad product/Figma blueprint, now repositioned to M&D.
- `docs/design/MEOW_CIRCLE_CUTE_FIGMA_WEB_REDESIGN.md`: current cute redesign and three-set Figma plan.
- `docs/design/MND_FIGMA_DESIGN_DOCUMENT.md`: current design-doc index to point users/agents to the right handoff files.
- `docs/design/MND_DESIGN_MEMORY.md`: concise record of the M&D redesign work.
