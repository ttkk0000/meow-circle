# M&D Stitch Remote Design Contract

Source of truth: Stitch project `projects/13275961100622290348`.

Local mirrors:

- `.stitch/DESIGN.md`: downloaded `MD_GLOBAL_STITCH_DESIGN_SYSTEM_V2_NEUTRAL_FIXED.md`.
- `.stitch/remote-assets`: downloaded Stitch screenshots, HTML, SVG, and markdown sources.
- `web/assets/stitch-remote`: browser-servable full-size screenshots plus HTML/SVG/Markdown source copies.
- `web/mnd-web-client-board.html`: current local Stitch sync board.
- `web/stitch-remote-gallery.html`: all downloaded full-size screenshot references and source links.

Project-level `designTheme` metadata is not the current design authority. Current implementation follows the uploaded V2 Stitch markdown and the remote theme/component screens.

## Product

M&D means `meow & doggie`.

- Visible mark: `M&D`.
- Supporting lockup: `meow & doggie`.
- Cat-first, doggie-friendly.
- Cats lead Feed, Profile, Auth, and default emotional imagery.
- Doggie content is valid for Market, services, activities, adoption, and secondary stories.

## Theme Model

M&D has four independent themes. Do not treat primary, secondary, tertiary, and neutral as the four themes. Those are roles inside each theme.

### Honey

Use for Feed, Profile, Pet Profile, Messages normal mode, and Orders normal mode.

- Primary: `#FF8A3D`
- Secondary: `#FFD166`
- Tertiary / Cat Accent: `#FF7A45`
- Text Primary: `#231F20`
- Background: `#FFF8F2`
- Surface: `#FFFFFF`
- Surface Variant: `#FFF1E6`
- Border: `#F1D8C8`

### Mint

Use for Market, Product List, Product Detail, Product Publish, product search, and unavailable product states.

- Primary: `#2EC4A6`
- Secondary: `#A7F3D0`
- Tertiary / Cat Accent: `#FF8A65`
- Text Primary: `#12312B`
- Background: `#F5FFFC`
- Surface: `#FFFFFF`
- Surface Variant: `#EAFBF6`
- Border: `#CDEFE6`

### Night

Use for dark mode only. Night is purple-primary, not Honey dark mode.

- Primary: `#8B5CF6`
- Secondary: `#6366F1`
- Tertiary / Cat Accent: `#F59E0B`
- Text Primary: `#F8FAFC`
- Background: `#0B0D12`
- Surface: `#151821`
- Surface Variant: `#1F2430`
- Surface Elevated: `#252B38`
- Border: `#303644`

### Neutral

Use for restrained neutral styling, documentation, dense information, creator tools, dashboards, and explicit admin/moderation screens. Neutral is a first-class theme and should not automatically become an admin dashboard.

- Primary: `#4B5563`
- Secondary: `#9CA3AF`
- Tertiary / rare cat accent: `#F97316`
- Text Primary: `#111827`
- Background: `#F7F7F8`
- Surface: `#FFFFFF`
- Surface Variant: `#F3F4F6`
- Border: `#E5E7EB`

## Typography

- Display, headlines, titles, cards, product titles, and price emphasis: `Outfit`.
- Body, labels, captions, tables, metadata, and dense UI: `Inter`.
- Do not use decorative fonts.
- Do not use negative letter spacing in implementation.

## Layout

Mobile:

- Target frame: `390 x 844`.
- Single-column layout.
- Page margin: `16px`.
- Card padding: `16px`.
- Section spacing: `24px`.
- Main app bottom navigation: Feed, Market, Messages, Orders, Profile.

Desktop:

- Expand mobile hierarchy into wider scanable layouts.
- Consumer desktop may use side navigation and right rail.
- Market detail may use primary content plus seller/order side panel.
- Do not add unrelated care reminders, trending tags, or dashboard widgets by default.

Neutral / true admin:

- Sidebar width: `240px` only for true dashboard/admin contexts.
- Header height: `64px`.
- Main content padding: `24px`.
- Table row height: `48px`.
- Neutral non-admin screens stay professional and restrained without moderation menus.

## Shape And Depth

- Consumer cards, product cards, and message bubbles: `16px`.
- Dialogs: `20px`.
- Buttons: `12px` or full pill where appropriate.
- Neutral cards: `8px`.
- True admin tables and dense controls: `4px`.
- Use borders, tonal separation, and subtle shadows.
- Do not use glassmorphism, neon effects, heavy gradients, glossy 3D, or decorative blobs.

## Components

Required interaction states:

- Default
- Hover where applicable
- Pressed
- Focused
- Disabled
- Loading
- Error
- Success

Global loading:

- Use `GlobalPawLoading` when branded loading is needed.
- Anatomy: partial circular progress ring, centered cat paw icon, `Loading...` label.
- Do not use a dog icon for the global loader.

## Feature Rules

Feed:

- Honey only in normal mode.
- Required: composer entry, feed card, media grid, like, comment, share, save, follow, comments, filter tabs, empty state, skeleton state.

Market:

- Mint only in normal mode.
- M&D Market is social commerce, not cart-first ecommerce.
- Trade type chips: Sell, Trade, Looking For, Free / Donation.
- Allowed actions: Buy Now, Contact Seller, Make Offer, View Details, Save.
- Do not use Add to Cart as the dominant action.

Messages:

- Honey only in normal mode.
- Required: conversation item, message bubble, input bar, attachment, send, unread badge, chat header, compact order context, empty state, skeleton state.

Orders:

- Honey only in normal mode.
- Official states: `pending_payment`, `paid`, `cancelled`, `shipped`, `refunded`, `completed`.
- Do not use `delivered`.
- Hide invalid buyer/seller actions.

Neutral / true admin:

- Only use admin tables, moderation workflows, audit logs, and management menus for explicit admin/moderation screens.
- High-risk admin actions require confirmation.
- Moderation actions should create an audit log item.

## Stitch Output Rules

For design-system boards:

- Show the entire page in one full-height image.
- Extend canvas height as needed.
- No internal scrolling.
- No clipped content.
- No hidden overflow.
- No carousel.
- No horizontal scrolling.
- No sticky scroll panels.
