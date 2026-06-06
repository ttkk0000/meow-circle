# M&D UI Alignment Guide

Last updated: 2026-06-06

This is the implementation-facing guide for keeping Web, desktop boards, mobile clients, and future Stitch updates aligned.

## Source Of Truth

Current Stitch project:

- `projects/13275961100622290348`
- Title: `Kitty Circle Social`

Local mirrors:

- `.stitch/DESIGN.md`: current remote global design markdown.
- `.stitch/metadata.json`: local index of downloaded remote records.
- `.stitch/remote-assets/screens`: original full-size downloaded screenshots.
- `.stitch/remote-assets/sources`: downloaded HTML, SVG, and markdown sources.
- `web/assets/stitch-remote/screens`: browser-visible full-size screenshot copies.
- `web/assets/stitch-remote/sources`: browser-visible HTML, SVG, and markdown source copies.
- `web/mnd-web-client-board.html`: current local sync board.
- `web/stitch-remote-gallery.html`: full-size screenshot gallery with source links.

Important: the Stitch project-level `designTheme` is metadata only. Do not use it as the current design source. Use the V2 markdown and the remote screen assets instead.

## AI Usage Rules

AI agents must read `docs/design/MND_STITCH_AI_RULES.md` before UI work.

Agents may critique, propose alternatives, and point out missing states, but
must label those as suggestions. Implementation should not invent a new visual
direction when the current Stitch V2 mirror already defines the theme,
component, navigation, copy, or platform pattern.

## Product Contract

M&D means `meow & doggie`.

- Visible mark: `M&D`.
- Supporting lockup: `meow & doggie`.
- Cat-first and doggie-friendly.
- Cats lead Feed, Profile, Auth, and default emotional examples.
- Doggie content is valid for Market, services, activities, adoption, and secondary stories.
- Do not rename the product to `MD`, `Meow & Dog`, or generic pet community.

## Theme Contract

The four themes are:

- Honey
- Mint
- Night
- Neutral

Do not treat Primary, Secondary, Tertiary, and Neutral as four global M&D themes. Those are roles inside each theme.

### Honey

Use for Feed, Profile, Pet Profile, Messages normal mode, and Orders normal mode.

- Primary: `#FF8A3D`
- Background: `#FFF8F2`
- Surface: `#FFFFFF`
- Surface Variant: `#FFF1E6`
- Text: `#231F20`
- Border: `#F1D8C8`

### Mint

Use for Market, Product List, Product Detail, Product Publish, product search, and unavailable product states.

- Primary: `#2EC4A6`
- Background: `#F5FFFC`
- Surface: `#FFFFFF`
- Surface Variant: `#EAFBF6`
- Text: `#12312B`
- Border: `#CDEFE6`

### Night

Use only for dark mode. It is purple-primary, not orange-primary.

- Primary: `#8B5CF6`
- Background: `#0B0D12`
- Surface: `#151821`
- Surface Variant: `#1F2430`
- Text: `#F8FAFC`
- Border: `#303644`

### Neutral

Use for restrained neutral styling, documentation, dense information, creator tools, dashboards, and explicit admin/moderation screens. Neutral is a first-class theme.

- Primary: `#4B5563`
- Background: `#F7F7F8`
- Surface: `#FFFFFF`
- Surface Variant: `#F3F4F6`
- Text: `#111827`
- Border: `#E5E7EB`
- Cat accent, rare only: `#F97316`

## Typography

- Use `Outfit` for display, headlines, titles, product titles, and prices.
- Use `Inter` for body, labels, captions, forms, metadata, and tables.
- Do not use decorative fonts.
- Keep implementation letter spacing at `0`.

## Component Rules

- Consumer cards, product cards, and message bubbles: `16px` radius.
- Dialogs: `20px` radius.
- Buttons: `12px` radius or full pill when the control is a chip/action pill.
- Neutral cards: `8px` radius.
- Dense admin tables/controls: `4px` radius.
- Required states: default, hover, pressed, focused, disabled, loading, error, success.
- Use borders and tonal layers first; use shadows subtly.
- No decorative gradient blobs, glassmorphism, neon effects, glossy 3D, or heavy shadows.

## Navigation

Mobile app navigation must be exactly:

- Feed
- Market
- Messages
- Orders
- Profile

Desktop can expand this into a side navigation, but the same core product areas must remain obvious.

## Feature Mapping

Feed:

- Theme: Honey.
- Required: composer entry, feed card, media grid, like, comment, share, save, follow, comments, filter tabs, empty state, skeleton state.

Market:

- Theme: Mint.
- M&D Market is social commerce, not generic cart ecommerce.
- Trade chips: Sell, Trade, Looking For, Free / Donation.
- Actions: Buy Now, Contact Seller, Make Offer, View Details, Save.
- Do not use Add to Cart as the dominant action.

Messages:

- Theme: Honey.
- Include conversation item, message bubble, input bar, attachment, send, unread badge, chat header, compact order context, empty state, skeleton state.

Orders:

- Theme: Honey.
- Official states: `pending_payment`, `paid`, `cancelled`, `shipped`, `refunded`, `completed`.
- Do not use `delivered`.
- Buyer/seller actions must match state rules from `.stitch/DESIGN.md`.

Neutral / true admin:

- Theme: Neutral.
- Do not automatically add admin tables, moderation workflows, audit logs, or management menus.
- Use true admin patterns only for explicit admin/moderation/system-control screens.

## Current Web Entries

- `web/pawpop.html`: current UI index.
- `web/mnd-web-client-board.html`: remote sync board with key Stitch references.
- `web/stitch-remote-gallery.html`: all downloaded full-size Stitch screenshots and local source links.
- `web/cute.html`: working WebUI aligned to the current remote controls.
- `web/pawpop-desktop.html`: desktop mapping board.
- `web/stitch-theme-bridge.css`: compatibility token bridge mapped to V2.

Legacy handmade references, old Stitch maps, and prior-generation design docs have been removed. Do not recreate them or use them as design sources.

## Verification Rules

- Do not run Gradle.
- Do not run Go build/run/test on this machine.
- Use static checks and browser rendering for Web UI.
- Validate local screenshots against `web/assets/stitch-remote/screens` and implementation details against `web/assets/stitch-remote/sources`.
