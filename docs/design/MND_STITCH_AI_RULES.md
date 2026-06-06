# M&D Stitch AI Rules

Last updated: 2026-06-06

This file tells AI agents how to work on M&D UI without drifting away from the
current Stitch project.

## Design Authority

Use this Stitch project as the source of truth:

- `projects/13275961100622290348`

Use the local mirror before relying on memory or inventing a new design:

- `.stitch/DESIGN.md`
- `.stitch/metadata.json`
- `.stitch/remote-assets`
- `web/assets/stitch-remote`
- `web/mnd-web-client-board.html`
- `web/stitch-remote-gallery.html`
- `docs/design/MND_UI_ALIGNMENT_GUIDE.md`

The screenshots in the mirror are full-size downloads, not thumbnail-only
references. Prefer those local files when comparing layout and visual details.

## Non-Negotiable Rules

- Keep the visible brand as `M&D`; keep the lockup/supporting copy as `meow & doggie`.
- Use only the four Stitch themes: Honey, Mint, Night, and Neutral.
- Treat Neutral as a first-class theme, not a system/default fallback.
- Do not recreate old `_stitch_ref`, `STITCH_WEB_17`, or prior-generation design docs.
- Do not invent new color palettes, typography, navigation models, decorative effects, or component geometry when Stitch already defines the rule.
- Do not introduce generic pet-community branding, `MD`, `Meow & Dog`, `Kitty Circle`, `Pawpop`, or old visible product names.
- Do not add decorative gradient blobs, glassmorphism, neon styling, glossy 3D, or heavy shadows.
- Use real cat-first imagery by default; doggie imagery is valid for services, marketplace, activities, adoption, and secondary stories.

## Platform Mapping

- Web and desktop Web: use `web/pawpop-desktop.html`, `web/mnd-web-client-board.html`, `web/stitch-remote-gallery.html`, and `web/assets/stitch-remote`.
- WebUI prototype: use `web/cute.html`, `web/cute-ui.css`, and `web/cute-ui.js`.
- Expo mobile: use `mobile/src/theme.ts`, `mobile/src/components.tsx`, `mobile/src/stitch`, and `mobile/app`.
- KMP Android: use `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/theme` and `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/ui`.
- KMP Desktop: use `kmp/desktopApp` and keep it aligned with the desktop board and Stitch desktop refs.

## What AI May Suggest

AI agents may critique the design, identify missing states, and propose
improvements. Suggestions must be clearly labeled as suggestions or options.

Implementation must still follow the Stitch V2 system unless the user explicitly
approves a deviation. If a needed feature has no exact Stitch screen, map it to
the closest existing theme, component, and platform pattern before designing
anything new.

## Required Workflow For UI Changes

1. Read `AGENTS.md`.
2. Read this file.
3. Read `.stitch/DESIGN.md` and `docs/design/MND_UI_ALIGNMENT_GUIDE.md`.
4. Inspect relevant local Stitch screenshots or sources in `web/assets/stitch-remote`.
5. Make the smallest implementation that follows the existing Stitch system.
6. Run static checks that are allowed on this machine.

Do not run Gradle or Go build/run/test on this machine.
