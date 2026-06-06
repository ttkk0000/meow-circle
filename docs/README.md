# M&D Docs Hub

Last updated: 2026-06-06

This folder is the documentation entry point for **M&D = meow & doggie**.
The product is cat-first and doggie-friendly: cats lead the emotional identity,
default imagery, feed language, and profile examples; doggie content supports
activities, services, marketplace, adoption, and secondary stories.

## Start Here

| Role | Read First | Then Read |
| --- | --- | --- |
| Product / design | `design/MND_FIGMA_DESIGN_DOCUMENT.md` | `design/MND_UI_ALIGNMENT_GUIDE.md`, `design/MND_STITCH_AI_RULES.md` |
| Backend / API | `PROJECT_STRUCTURE.md` | `API_AND_DATA_FLOW.md`, `RUNBOOK.md` |
| Frontend Web | `FRONTEND_SURFACE_MAP.md` | `design/MND_UI_ALIGNMENT_GUIDE.md`, `RUNBOOK.md` |
| Expo mobile | `FRONTEND_SURFACE_MAP.md` | `RUNBOOK.md`, `design/MND_UI_ALIGNMENT_GUIDE.md` |
| KMP Android | `PROJECT_STRUCTURE.md` | `FRONTEND_SURFACE_MAP.md`, `RUNBOOK.md` |
| Future agents | `../AGENTS.md` | `design/MND_STITCH_AI_RULES.md`, `design/MND_DESIGN_MEMORY.md` |

## Current Source Map

| Area | Source |
| --- | --- |
| Project structure | `PROJECT_STRUCTURE.md` |
| API and data flow | `API_AND_DATA_FLOW.md` |
| Frontend surface map | `FRONTEND_SURFACE_MAP.md` |
| Runbook | `RUNBOOK.md` |
| Figma/design index | `design/MND_FIGMA_DESIGN_DOCUMENT.md` |
| AI Stitch design rules | `design/MND_STITCH_AI_RULES.md` |
| Current UI contract | `design/MND_UI_ALIGNMENT_GUIDE.md` |
| Implementation memory | `design/MND_DESIGN_MEMORY.md` |
| Chinese project onboarding | `../PROJECT_ONBOARDING_ZH.md` |
| Stitch design contract | `../.stitch/DESIGN.md` |
| Stitch remote assets | `../.stitch/remote-assets`, `../web/assets/stitch-remote` |
| WebUI prototype | `../web/cute.html` |
| Mobile design board | `../web/pawpop-mobile.html` |
| Desktop design board | `../web/pawpop-desktop.html` |
| Production Web bridge | `../web/stitch-theme-bridge.css` |
| Expo tokens | `../mobile/src/theme.ts` |
| KMP Android tokens | `../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/theme` |
| KMP Desktop client | `../kmp/desktopApp` |

## Current UI Status

| Surface | Status | Notes |
| --- | --- | --- |
| WebUI | M&D direction implemented | `web/cute.html` is static-safe; use `?live=1` to hydrate supported APIs. |
| Production Web pages | M&D aligned | `stitch-theme-bridge.css` maps existing pages onto the M&D token system. |
| Mobile design board | M&D aligned | `web/pawpop-mobile.html` is a Figma source, not runtime app code. |
| Desktop design board | M&D aligned | `web/pawpop-desktop.html` is the desktop Figma source. |
| Expo app | M&D aligned | Tokens, app metadata, auth/feed/discover/compose/top bar/loading copy were updated. |
| KMP Android | M&D aligned in source | Static source review only on this machine; do not run Gradle. |
| KMP Desktop | M&D aligned in source | `kmp/desktopApp` is a Compose Desktop client using the same Stitch project refs and four-theme token system. |
| Backend/API | Existing behavior preserved | Auth storage keys such as `meow_token` remain for compatibility. |

## Verification

Known-good checks from the latest pass:

- `python scripts/verify_web_smoke.py` (Automated verification of HTML tags, assets, and branding names)
- `git diff --check`

Static Web smoke coverage checked these pages at desktop `1440x1000` and mobile
`390x844`: `/`, discover, compose, market, messages, profile, login, register,
dashboard, admin, post, mobile board, desktop board, and WebUI.

The automated verification check covers:
- HTML link/reference completeness (no broken local assets).
- No forbidden old visible product terms (e.g. `喵友圈`, `Kitty Circle`, etc.).

Known local blockers:
- **Gradle**: Do not run Gradle on this machine; it has repeatedly caused system freezes.
- **Go**: Do not run `go build`, `go run`, or `go test` on this machine; use static Web checks for UI work.

## Documentation Rules

- Keep `M&D` as the visible mark and `meow & doggie` as the lockup text.
- Do not reintroduce `MEOW`, `Kitty Circle`, `Pawpop`, `喵友圈`, or other legacy
  visible product names unless explicitly documenting migration history.
- Keep current implementation notes in `design/MND_DESIGN_MEMORY.md`.
- AI agents must read `design/MND_STITCH_AI_RULES.md` before UI work.
- Put design rules that engineers must follow in `design/MND_UI_ALIGNMENT_GUIDE.md`.
- Prior-generation design references and stale Stitch export maps have been removed. Do not recreate them.
