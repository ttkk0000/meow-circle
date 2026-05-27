# M&D Docs Hub

Last updated: 2026-05-27

This folder is the documentation entry point for **M&D = meow & doggie**.
The product is cat-first and doggie-friendly: cats lead the emotional identity,
default imagery, feed language, and profile examples; doggie content supports
activities, services, marketplace, adoption, and secondary stories.

## Start Here

| Role | Read First | Then Read |
| --- | --- | --- |
| Product / design | `design/MND_FIGMA_DESIGN_DOCUMENT.md` | `design/MND_UI_ALIGNMENT_GUIDE.md` |
| Backend / API | `PROJECT_STRUCTURE.md` | `API_AND_DATA_FLOW.md`, `RUNBOOK.md` |
| Frontend Web | `FRONTEND_SURFACE_MAP.md` | `design/MND_UI_ALIGNMENT_GUIDE.md`, `RUNBOOK.md` |
| Expo mobile | `FRONTEND_SURFACE_MAP.md` | `RUNBOOK.md`, `design/MND_UI_ALIGNMENT_GUIDE.md` |
| KMP Android | `PROJECT_STRUCTURE.md` | `FRONTEND_SURFACE_MAP.md`, `RUNBOOK.md` |
| Future agents | `../AGENTS.md` | `design/MND_DESIGN_MEMORY.md` |

## Current Source Map

| Area | Source |
| --- | --- |
| Project structure | `PROJECT_STRUCTURE.md` |
| API and data flow | `API_AND_DATA_FLOW.md` |
| Frontend surface map | `FRONTEND_SURFACE_MAP.md` |
| Runbook | `RUNBOOK.md` |
| Figma/design index | `design/MND_FIGMA_DESIGN_DOCUMENT.md` |
| Current UI contract | `design/MND_UI_ALIGNMENT_GUIDE.md` |
| Implementation memory | `design/MND_DESIGN_MEMORY.md` |
| Chinese project onboarding | `../PROJECT_ONBOARDING_ZH.md` |
| Broad product blueprint | `design/MEOW_CIRCLE_FIGMA_DESIGN_DOCUMENT.md` |
| Cute redesign plan | `design/MEOW_CIRCLE_CUTE_FIGMA_WEB_REDESIGN.md` |
| WebUI prototype | `../web/cute.html` |
| Mobile design board | `../web/pawpop-mobile.html` |
| Desktop design board | `../web/pawpop-desktop.html` |
| Production Web bridge | `../web/stitch-theme-bridge.css` |
| Expo tokens | `../mobile/src/theme.ts` |
| KMP Android tokens | `../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/theme` |

## Current UI Status

| Surface | Status | Notes |
| --- | --- | --- |
| WebUI | M&D direction implemented | `web/cute.html` is static-safe; use `?live=1` to hydrate supported APIs. |
| Production Web pages | M&D aligned | `stitch-theme-bridge.css` maps legacy pages onto the M&D token system. |
| Mobile design board | M&D aligned | `web/pawpop-mobile.html` is a Figma source, not runtime app code. |
| Desktop design board | M&D aligned | `web/pawpop-desktop.html` is the desktop Figma source; there is no separate native desktop app tree. |
| Expo app | M&D aligned | Tokens, app metadata, auth/feed/discover/compose/top bar/loading copy were updated. |
| KMP Android | M&D aligned in source | Compile was blocked by Maven/Gradle Plugin Portal 403 before Kotlin compilation. |
| Backend/API | Existing behavior preserved | Auth storage keys such as `meow_token` remain for compatibility. |

## Verification

Known-good checks from the latest pass:

- `cd mobile && npm run typecheck`
- `git diff --check`
- Static Web smoke test through `python3 -m http.server 4173` from `web/`.

Static Web smoke coverage checked these pages at desktop `1440x1000` and mobile
`390x844`: `/`, discover, compose, market, messages, profile, login, register,
dashboard, admin, post, mobile board, desktop board, and WebUI.

The smoke test checked:

- No page errors.
- No horizontal overflow.
- No old visible product terms.
- No visible legacy decorative blur blobs.
- No oversized non-action radii.
- No negative letter spacing.

Known local blockers:

- The current shell Node reported `v16.10.0`; React Native dependencies expect
  Node 18+ for normal development.
- KMP Gradle dependency resolution hit HTTP 403 from Maven/Gradle Plugin Portal
  before source compilation.
- The Go backend previously failed in this local environment with the repo's
  `go.mod` version string; use static Web verification when Go cannot run.

## Documentation Rules

- Keep `M&D` as the visible mark and `meow & doggie` as the lockup text.
- Do not reintroduce `MEOW`, `Kitty Circle`, `Pawpop`, `喵友圈`, or other legacy
  visible product names unless explicitly documenting migration history.
- Keep current implementation notes in `design/MND_DESIGN_MEMORY.md`.
- Put design rules that engineers must follow in `design/MND_UI_ALIGNMENT_GUIDE.md`.
- Treat old file names containing `MEOW_CIRCLE` as historical containers whose
  contents now describe M&D.
