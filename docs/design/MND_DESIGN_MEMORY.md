# M&D Design Memory

Last updated: 2026-06-06

This is the concise current-state memory for M&D design work. It intentionally
does not preserve migration notes or obsolete design directions.

## Product Direction

M&D means `meow & doggie`.

- Visible brand mark: `M&D`.
- Supporting lockup: `meow & doggie`.
- Product tone: cat-first and doggie-friendly.
- Cats lead hero copy, feed examples, profile identity, auth imagery, and default imagery.
- Doggie content is valid for marketplace, services, activities, adoption, and secondary stories.

## Current Design Authority

Current Stitch project:

- `projects/13275961100622290348`

Local source of truth:

- `.stitch/DESIGN.md`
- `.stitch/metadata.json`
- `.stitch/remote-assets`
- `web/assets/stitch-remote`
- `web/mnd-web-client-board.html`
- `web/stitch-remote-gallery.html`
- `docs/design/MND_UI_ALIGNMENT_GUIDE.md`
- `docs/design/MND_STITCH_AI_RULES.md`

The remote set is mobile, theme-board, component-board, document, image, and icon
oriented. Desktop and WebUI surfaces are local mappings of those approved remote
rules, not independent visual directions.

## Current Web Design Sources

- `web/pawpop.html`: current design entry index.
- `web/mnd-web-client-board.html`: Stitch sync board for mobile, themes, components, and implementation contract.
- `web/stitch-remote-gallery.html`: full-size remote screenshots plus source links.
- `web/cute.html`: static WebUI prototype aligned to Stitch V2.
- `web/pawpop-desktop.html`: local desktop mapping board aligned to the remote mobile/component rules.
- `web/stitch-theme-bridge.css`: compatibility token bridge for production-ish static pages.

## Theme Rules

M&D uses four first-class themes only:

- Honey: Feed, Profile, Pet Profile, Messages, Orders.
- Mint: Market, Product List, Product Detail, Product Publish, product search.
- Night: dark mode only.
- Neutral: restrained documentation, creator tools, dashboards, and explicit admin/moderation surfaces.

Neutral is not an OS/default fallback. `sugar` may appear only as a compatibility
alias for Honey in existing runtime code.

## Implementation Notes

- `web/cute.html` is a static visual WebUI prototype and does not call the Go API.
- Production-ish Web pages keep existing API hooks and auth storage behavior.
- Existing auth storage keys remain `meow_token` and `meow_user`.
- Static/demo fallbacks in Web scripts are intentional so design review works without the Go API.
- KMP Android and KMP Desktop must be checked by static source review on this machine.
- Do not run Gradle or Go build/run/test locally.

## Verification

Use allowed static and browser checks:

- `git diff --check`
- JavaScript syntax checks with `node --check` where applicable.
- Browser checks through a static server, for example `python3 -m http.server 4173 --directory web`.
- Desktop viewport checks around `1440x920`.
- Mobile viewport checks around `390x844`.

For UI work, verify:

- No horizontal overflow.
- No browser console errors or warnings.
- M&D brand and `meow & doggie` lockup remain intact.
- Theme usage follows Honey, Mint, Night, and Neutral.
- Remote screenshots and source files render from `web/assets/stitch-remote`.

## Future Updates

Update this file only with current design facts that future agents need.
