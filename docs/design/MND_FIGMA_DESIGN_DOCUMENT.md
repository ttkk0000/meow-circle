# M&D Figma Design Document Index

Last updated: 2026-05-27

M&D = `meow & doggie`.

Use this as the current entry point for design handoff. The older file names still contain `MEOW_CIRCLE` because they were created before the brand reposition, but their contents now describe M&D.

## Primary Docs

- `docs/README.md`: Documentation hub, role-based reading order, current status, and verification notes.

- `docs/design/MND_UI_ALIGNMENT_GUIDE.md`: Current implementation-facing UI contract: tokens, platform mapping, copy rules, QA checklist, and known local blockers.

- `docs/design/MEOW_CIRCLE_FIGMA_DESIGN_DOCUMENT.md`: Product architecture, Figma file structure, personas, flows, components, and handoff rules.

- `docs/design/MEOW_CIRCLE_CUTE_FIGMA_WEB_REDESIGN.md`: Current cute redesign direction, three UI sets, screen inventory, tokens, components, profile background system, and migration plan.

- `docs/design/MND_DESIGN_MEMORY.md`: Short memory for future agents: what changed, why M&D means `meow & doggie`, and what to preserve.

## Current Status

| Surface | Status | Source |
| --- | --- | --- |
| WebUI | Implemented as M&D prototype | `web/cute.html`, `web/cute-ui.css`, `web/cute-ui.js` |
| Production Web | M&D-aligned through bridge and page updates | `web/stitch-theme-bridge.css`, `web/shared.js`, page scripts |
| Mobile board | Figma-ready HTML source | `web/pawpop-mobile.html` |
| Desktop board | Figma-ready HTML source | `web/pawpop-desktop.html` |
| Expo mobile | Source-aligned to M&D | `mobile/src/theme.ts`, `mobile/app`, `mobile/src/stitch` |
| KMP Android | Source-aligned to M&D | `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp` |

There is no separate native desktop app source tree in this repo right now.
Use the desktop design board and browser/admin/workbench pages as desktop sources.

## Visual Sources

- `web/pawpop.html`: three-set overview.
- `web/pawpop-mobile.html`: mobile design board.
- `web/pawpop-desktop.html`: desktop design board.
- `web/cute.html`: browser WebUI prototype and implementation target.
- Production Web pages now aligned enough to use as secondary Figma sources:
  `web/index.html`, `web/discover.html`, `web/compose.html`, `web/market.html`,
  `web/post.html`, `web/messages.html`, `web/profile.html`, `web/login.html`,
  `web/register.html`, `web/dashboard.html`, and `web/admin.html`.
- Expo mobile implementation sources:
  `mobile/src/theme.ts`, `mobile/src/components.tsx`, `mobile/src/stitch/*`, and
  `mobile/app/(auth)`, `mobile/app/(tabs)`.
- KMP Android implementation sources:
  `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/theme/*` and
  `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/ui/*`.

## Figma Pages To Build

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

## Immediate Figma Work Order

1. Build `02 M&D Tokens` from `MND_UI_ALIGNMENT_GUIDE.md`.
2. Build `03 Components` with button, chip, input, nav, feed tile, listing card,
   chat bubble, profile background picker, status badge, and admin row variants.
3. Recreate `04 Mobile App` from `web/pawpop-mobile.html` and Expo source.
4. Recreate `05 Desktop App` from `web/pawpop-desktop.html`.
5. Recreate `06 WebUI` from `web/cute.html`.
6. Add admin/trust flows from `web/dashboard.html` and `web/admin.html`.
7. Add clickable prototypes for browse, publish, market-to-chat/order,
   profile-background switch, and report-to-admin-review.
