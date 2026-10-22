# M&D UI Alignment Guide

Last updated: 2026-10-15 (document scope clarification; visual tokens unchanged)

2026-10-20 scoped mobile implementation update: the newly authorized Android / iOS entry follows the approved Figma phone V2 default light mode and [adaptive contract](figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md), as recorded in [mobile implementation](../client/MOBILE_IMPLEMENTATION_2026_10_20.md). The old four-theme rules below remain historical / legacy-client references for that scope; they do not override the user's current mobile default-style decision. The Stitch mirror is unchanged. Full feature and device acceptance remain incomplete.

This is the implementation-facing guide for keeping Web, desktop boards, mobile clients, and future Stitch updates aligned.

## Product And Visual Scope

The current product proposal is defined in [PRD 2.0](../bmad/prd.md),
[UX specification](../bmad/ux-design.md), and the
[trust/disclosure specification](MND_TRUST_AND_DISCLOSURE_SPEC.md).
It supports clearly disclosed adoption and cat sales by individuals, businesses,
and authorized agents. Household keeping is not a trust prerequisite.

This guide and the Stitch mirror remain the visual authority. The proposed
unified navigation and trust/appointment screens require explicit mapping to
existing Stitch components before implementation. This documentation update
does not claim that remote designs or clients have already been migrated.

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

Honey references demonstrate Feed, Profile, Pet Profile, Messages, and legacy
Orders. Consumer pages inherit the selected global theme rather than forcing Honey.

- Primary: `#FF8A3D`
- Background: `#FFF8F2`
- Surface: `#FFFFFF`
- Surface Variant: `#FFF1E6`
- Text: `#231F20`
- Border: `#F1D8C8`

### Mint

Mint is a global theme option. Some Stitch app boards show Market and product
flows in Mint to demonstrate that theme, but KMP/Expo app screens must not force
Market to Mint. Feed, Market, Messages, Orders, Profile, auth, and product
flows all inherit the user's current global theme.

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

## Existing Reference Navigation And Proposed Migration

The current Stitch reference navigation is:

- Feed
- Market
- Messages
- Orders
- Profile

The product proposal recommends 社区 / 领养 / 找猫 / 消息 / 我的, with
contextual publishing actions. This is a proposed business change, not an
assertion that the Stitch reference has been updated. See the current UX
specification for migration and historical-order access. Desktop mapping should
preserve the same task meanings and permissions.

## Feature Mapping

Feed:

- Theme: inherit the selected global theme; Honey is a reference example.
- Required: composer entry, feed card, media grid, like, comment, share, save, follow, comments, filter tabs, empty state, skeleton state.

Market:

- Theme: inherit the current global theme. Mint screenshots are theme examples,
  not a Market-only override.
- M&D Market is social commerce, not generic cart ecommerce.
- Trade chips: Sell, Trade, Looking For, Free / Donation.
- Actions: Buy Now, Contact Seller, Make Offer, View Details, Save.
- Do not use Add to Cart as the dominant action.

Messages:

- Theme: inherit the selected global theme; Honey is a reference example.
- Include conversation item, message bubble, input bar, attachment, send, unread badge, chat header, compact order context, empty state, skeleton state.

Orders:

- Theme: inherit the selected global theme; Honey is a reference example.
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
- Follow the global execution boundary: without current user authorization,
  use static source/document checks only. Historical browser-rendering records
  are not a claim of fresh verification.
- Validate local screenshots against `web/assets/stitch-remote/screens` and implementation details against `web/assets/stitch-remote/sources`.
