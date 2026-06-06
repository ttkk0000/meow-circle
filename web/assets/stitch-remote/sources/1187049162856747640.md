---
version: "beta-2026-06-04"
name: "M&D Global Stitch Design System"
description: "Cat-first, dog-friendly social commerce marketplace and community design system for Google Stitch / DESIGN.md. Updated with Neutral clarification, full-height non-scroll Stitch rules, component-board rules, and GlobalPawLoading."

colors:
  primary: "#FF8A3D"
  secondary: "#FFD166"
  tertiary: "#FF7A45"
  neutral: "#231F20"
  surface: "#FFFFFF"
  on-surface: "#231F20"
  error: "#EF4444"

  honey-primary: "#FF8A3D"
  honey-primary-hover: "#F0782C"
  honey-primary-pressed: "#D9641F"
  honey-primary-container: "#FFF1E6"
  honey-secondary: "#FFD166"
  honey-secondary-container: "#FFF7D6"
  honey-tertiary: "#FF7A45"
  honey-dog-accent: "#7C5CFF"
  honey-neutral: "#231F20"
  honey-background: "#FFF8F2"
  honey-surface: "#FFFFFF"
  honey-surface-variant: "#FFF1E6"
  honey-surface-elevated: "#FFFFFF"
  honey-text-primary: "#231F20"
  honey-text-secondary: "#6B5E57"
  honey-text-tertiary: "#9A8A80"
  honey-text-inverse: "#FFFFFF"
  honey-border: "#F1D8C8"
  honey-divider: "#F5E2D5"
  honey-success: "#22C55E"
  honey-success-container: "#DCFCE7"
  honey-warning: "#F59E0B"
  honey-warning-container: "#FEF3C7"
  honey-error: "#EF4444"
  honey-error-container: "#FEE2E2"
  honey-info: "#3B82F6"
  honey-info-container: "#DBEAFE"
  honey-disabled: "#D6C7BE"
  honey-overlay: "rgba(35, 31, 32, 0.48)"
  honey-scrim: "rgba(35, 31, 32, 0.64)"

  mint-primary: "#2EC4A6"
  mint-primary-hover: "#25AD93"
  mint-primary-pressed: "#1E927C"
  mint-primary-container: "#E7FAF5"
  mint-secondary: "#A7F3D0"
  mint-secondary-container: "#ECFDF5"
  mint-tertiary: "#FF8A65"
  mint-dog-accent: "#3B82F6"
  mint-neutral: "#12312B"
  mint-background: "#F5FFFC"
  mint-surface: "#FFFFFF"
  mint-surface-variant: "#EAFBF6"
  mint-surface-elevated: "#FFFFFF"
  mint-text-primary: "#12312B"
  mint-text-secondary: "#49645E"
  mint-text-tertiary: "#7A938D"
  mint-text-inverse: "#FFFFFF"
  mint-border: "#CDEFE6"
  mint-divider: "#DDF5EF"
  mint-success: "#16A34A"
  mint-success-container: "#DCFCE7"
  mint-warning: "#F59E0B"
  mint-warning-container: "#FEF3C7"
  mint-error: "#EF4444"
  mint-error-container: "#FEE2E2"
  mint-info: "#0EA5E9"
  mint-info-container: "#E0F2FE"
  mint-disabled: "#B9D8D0"
  mint-overlay: "rgba(18, 49, 43, 0.48)"
  mint-scrim: "rgba(18, 49, 43, 0.64)"

  night-primary: "#8B5CF6"
  night-primary-hover: "#A78BFA"
  night-primary-pressed: "#7C3AED"
  night-primary-container: "#2E1F4F"
  night-secondary: "#6366F1"
  night-secondary-container: "#24234A"
  night-tertiary: "#F59E0B"
  night-dog-accent: "#60A5FA"
  night-neutral: "#F8FAFC"
  night-background: "#0B0D12"
  night-surface: "#151821"
  night-surface-variant: "#1F2430"
  night-surface-elevated: "#252B38"
  night-text-primary: "#F8FAFC"
  night-text-secondary: "#CBD5E1"
  night-text-tertiary: "#94A3B8"
  night-text-inverse: "#0B0D12"
  night-border: "#303644"
  night-divider: "#252B36"
  night-success: "#4ADE80"
  night-success-container: "#12351F"
  night-warning: "#FBBF24"
  night-warning-container: "#3A2A0A"
  night-error: "#F87171"
  night-error-container: "#3B1212"
  night-info: "#60A5FA"
  night-info-container: "#102A4C"
  night-disabled: "#475569"
  night-overlay: "rgba(0, 0, 0, 0.56)"
  night-scrim: "rgba(0, 0, 0, 0.72)"

  neutral-primary: "#4B5563"
  neutral-primary-hover: "#374151"
  neutral-primary-pressed: "#1F2937"
  neutral-primary-container: "#F3F4F6"
  neutral-secondary: "#9CA3AF"
  neutral-secondary-container: "#F9FAFB"
  neutral-tertiary: "#F97316"
  neutral-dog-accent: "#2563EB"
  neutral-text-primary: "#111827"
  neutral-background: "#F7F7F8"
  neutral-surface: "#FFFFFF"
  neutral-surface-variant: "#F3F4F6"
  neutral-surface-elevated: "#FFFFFF"
  neutral-text-primary: "#111827"
  neutral-text-secondary: "#4B5563"
  neutral-text-tertiary: "#9CA3AF"
  neutral-text-inverse: "#FFFFFF"
  neutral-border: "#E5E7EB"
  neutral-divider: "#EEF0F2"
  neutral-success: "#16A34A"
  neutral-success-container: "#DCFCE7"
  neutral-warning: "#D97706"
  neutral-warning-container: "#FEF3C7"
  neutral-error: "#DC2626"
  neutral-error-container: "#FEE2E2"
  neutral-info: "#2563EB"
  neutral-info-container: "#DBEAFE"
  neutral-disabled: "#D1D5DB"
  neutral-overlay: "rgba(17, 24, 39, 0.48)"
  neutral-scrim: "rgba(17, 24, 39, 0.64)"

stitch_output_rules:
  full_height_non_scrollable: true
  extend_canvas_height_as_needed: true
  show_all_sections_at_once: true
  no_internal_scrolling: true
  no_hidden_overflow: true
  no_clipped_content: true
  no_horizontal_scrolling: true
  no_carousels: true
  no_sticky_scroll_panels: true
  preferred_artifact_type: "single full-height design-system image"

typography:
  display-lg:
    fontFamily: "Outfit"
    fontSize: "36px"
    fontWeight: 700
    lineHeight: 1.12
    letterSpacing: "-0.02em"
  display-md:
    fontFamily: "Outfit"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.18
    letterSpacing: "-0.015em"
  headline-lg:
    fontFamily: "Outfit"
    fontSize: "24px"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "-0.01em"
  headline-md:
    fontFamily: "Outfit"
    fontSize: "20px"
    fontWeight: 650
    lineHeight: 1.3
    letterSpacing: "-0.005em"
  title-md:
    fontFamily: "Outfit"
    fontSize: "18px"
    fontWeight: 650
    lineHeight: 1.35
  title-sm:
    fontFamily: "Outfit"
    fontSize: "16px"
    fontWeight: 650
    lineHeight: 1.4
  body-lg:
    fontFamily: "Inter"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.55
  body-md:
    fontFamily: "Inter"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
  body-sm:
    fontFamily: "Inter"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.45
  label-lg:
    fontFamily: "Inter"
    fontSize: "15px"
    fontWeight: 650
    lineHeight: 1.35
  label-md:
    fontFamily: "Inter"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1.3
  label-sm:
    fontFamily: "Inter"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "0.02em"
  caption:
    fontFamily: "Inter"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.4
  price:
    fontFamily: "Outfit"
    fontSize: "20px"
    fontWeight: 700
    lineHeight: 1.2
  neutral-table-header:
    fontFamily: "Inter"
    fontSize: "12px"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "0.06em"
  neutral-table-cell:
    fontFamily: "Inter"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.45

rounded:
  none: "0px"
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  full: "9999px"

spacing:
  xxs: "4px"
  xs: "8px"
  sm: "12px"
  md: "16px"
  lg: "24px"
  xl: "32px"
  xxl: "48px"
  mobile-margin: "16px"
  mobile-card-padding: "16px"
  desktop-gutter: "24px"
  neutral-sidebar-width: "240px"
  neutral-header-height: "64px"
  neutral-table-row-height: "48px"

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.honey-text-inverse}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
    height: "48px"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    borderColor: "{colors.primary}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
    height: "48px"
  button-destructive:
    backgroundColor: "{colors.honey-error}"
    textColor: "{colors.honey-text-inverse}"
    typography: "{typography.label-md}"
    rounded: "{rounded.md}"
    padding: "10px 14px"
  input-field:
    backgroundColor: "{colors.honey-surface}"
    textColor: "{colors.honey-text-primary}"
    borderColor: "{colors.honey-border}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "12px 14px"
    height: "48px"
  card:
    backgroundColor: "{colors.honey-surface}"
    textColor: "{colors.honey-text-primary}"
    borderColor: "{colors.honey-border}"
    rounded: "{rounded.lg}"
    padding: "{spacing.mobile-card-padding}"
  chip:
    backgroundColor: "{colors.honey-primary-container}"
    textColor: "{colors.primary}"
    rounded: "{rounded.full}"
    typography: "{typography.label-sm}"
    padding: "6px 10px"
  bottom-navigation:
    backgroundColor: "{colors.honey-surface}"
    textColor: "{colors.honey-text-secondary}"
    activeColor: "{colors.primary}"
    borderColor: "{colors.honey-divider}"
    height: "64px"
  feed-card:
    backgroundColor: "{colors.honey-surface}"
    textColor: "{colors.honey-text-primary}"
    borderColor: "{colors.honey-border}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  market-product-card:
    backgroundColor: "{colors.mint-surface}"
    textColor: "{colors.mint-text-primary}"
    borderColor: "{colors.mint-border}"
    accentColor: "{colors.mint-primary}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  order-card:
    backgroundColor: "{colors.honey-surface}"
    textColor: "{colors.honey-text-primary}"
    borderColor: "{colors.honey-border}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  message-bubble-sender:
    backgroundColor: "{colors.honey-primary}"
    textColor: "{colors.honey-text-inverse}"
    rounded: "{rounded.lg}"
    padding: "10px 14px"
  message-bubble-receiver:
    backgroundColor: "{colors.honey-primary-container}"
    textColor: "{colors.honey-text-primary}"
    rounded: "{rounded.lg}"
    padding: "10px 14px"
  neutral-data-table:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text-primary}"
    borderColor: "{colors.neutral-border}"
    typography: "{typography.neutral-table-cell}"
    rounded: "{rounded.xs}"
    rowHeight: "{spacing.neutral-table-row-height}"
  neutral-button-primary:
    backgroundColor: "{colors.neutral-primary}"
    textColor: "{colors.neutral-text-inverse}"
    typography: "{typography.label-md}"
    rounded: "{rounded.xs}"
    padding: "8px 12px"
  global-paw-loading:
    name: "GlobalPawLoading"
    shape: "partial circular progress ring with centered cat paw icon and Loading... label"
    sizes:
      sm: "24px"
      md: "40px"
      lg: "56px"
    honey:
      ringColor: "{colors.honey-primary}"
      pawColor: "{colors.honey-primary}"
      inactiveRingColor: "{colors.honey-primary-container}"
      textColor: "{colors.honey-text-primary}"
    mint:
      ringColor: "{colors.mint-primary}"
      pawColor: "{colors.mint-primary}"
      inactiveRingColor: "{colors.mint-primary-container}"
      textColor: "{colors.mint-text-primary}"
    night:
      ringColor: "{colors.night-primary}"
      pawColor: "{colors.night-primary}"
      inactiveRingColor: "{colors.night-primary-container}"
      textColor: "{colors.night-text-primary}"
      previewBackground: "{colors.night-surface}"
    neutral-text-primary:
      ringColor: "{colors.neutral-primary}"
      pawColor: "{colors.neutral-primary}"
      inactiveRingColor: "{colors.neutral-primary-container}"
      textColor: "{colors.neutral-text-primary}"
---

# M&D Global DESIGN.md

## Overview

M&D means **Meow & Doggie**. It is a cat-first, dog-friendly social commerce marketplace and community.

M&D combines social feed publishing, marketplace trading, private messaging, order management, creator analytics, admin moderation, reports, media review, and audit logs.

The emotional direction is warm, clean, friendly, modern, trustworthy, lightly playful, and community-driven. Cats are the primary emotional cue. Dogs are welcome, but secondary. Consumer UI should feel approachable and warm; marketplace UI should feel safe and credible; admin UI should feel professional and restrained.

M&D uses four independent visual themes:

1. **Honey Theme** for Feed, Profile, Pet Profile, Messages normal mode, and Orders normal mode.
2. **Mint Theme** for Market, Product List, Product Detail, Product Publish, and product search.
3. **Night Theme** for dark mode.
4. **Neutral Theme** for restrained neutral professional styling, dense system documentation, creator tools, dashboards, and true admin/moderation screens when explicitly requested.

Critical rule: one screen uses exactly one theme. Do not mix theme colors. Design-system overview boards may show multiple theme swatches only as isolated reference examples; those swatches must not become the page-level style.

## Colors

The official DESIGN.md structure uses palette roles such as `primary`, `secondary`, `tertiary`, and `neutral`. In M&D, those are **roles inside each theme**, not the four themes themselves.

Do not create one mixed palette where Primary = Honey, Secondary = Mint, Tertiary = Night, Neutral = Admin. That is wrong.

Correct model:
- Honey has its own primary, secondary, tertiary, and neutral.
- Mint has its own primary, secondary, tertiary, and neutral.
- Night has its own primary, secondary, tertiary, and neutral.
- Neutral has its own primary, secondary, tertiary, and neutral.

### Default Palette

The default top-level `colors.primary`, `colors.secondary`, `colors.tertiary`, and `colors.neutral` tokens are set to the Honey Theme because Honey is the default consumer theme.

- **Primary (#FF8A3D):** Warm orange for primary consumer actions.
- **Secondary (#FFD166):** Soft honey yellow for supporting emphasis.
- **Tertiary (#FF7A45):** Cat accent used for cat-first emotional cues.
- **Neutral (#231F20):** Deep warm ink for consumer text.

### Honey Theme

Use for Feed, Profile, Pet Profile, Messages normal mode, and Orders normal mode.

Palette roles:
- Primary: #FF8A3D
- Secondary: #FFD166
- Tertiary / Cat Accent: #FF7A45
- Neutral / Text Primary: #231F20
- Background: #FFF8F2
- Surface: #FFFFFF
- Surface Variant: #FFF1E6
- Border: #F1D8C8

Status colors:
- Success: #22C55E
- Warning: #F59E0B
- Error: #EF4444
- Info: #3B82F6
- Disabled: #D6C7BE

### Mint Theme

Use for Market, Product List, Product Detail, Product Publish, Product Search, and Product Unavailable State.

Palette roles:
- Primary: #2EC4A6
- Secondary: #A7F3D0
- Tertiary / Cat Accent: #FF8A65
- Neutral / Text Primary: #12312B
- Background: #F5FFFC
- Surface: #FFFFFF
- Surface Variant: #EAFBF6
- Border: #CDEFE6

Status colors:
- Success: #16A34A
- Warning: #F59E0B
- Error: #EF4444
- Info: #0EA5E9
- Disabled: #B9D8D0

### Night Theme

Use only for dark mode screens.

Night is a black, deep gray, and purple theme. Night must not be a Honey dark mode. Night primary is purple, not orange. Amber / orange is only a cat accent or warning status color.

Palette roles:
- Primary: #8B5CF6
- Secondary: #6366F1
- Tertiary / Cat Accent: #F59E0B
- Neutral / Text Primary: #F8FAFC
- Background: #0B0D12
- Surface: #151821
- Surface Variant: #1F2430
- Surface Elevated: #252B38
- Border: #303644

Status colors:
- Success: #4ADE80
- Warning: #FBBF24
- Error: #F87171
- Info: #60A5FA
- Disabled: #475569

### Neutral Theme

Use as a restrained neutral professional style. It is not automatically a management platform. Use it for neutral documentation, system rules, dense information, creator tools, dashboards, and true admin/moderation screens only when the screen is explicitly about admin, moderation, reports, audit logs, or system management.

Palette roles:
- Primary: #4B5563
- Secondary: #9CA3AF
- Tertiary / Cat Accent: #F97316
- Neutral / Text Primary: #111827
- Background: #F7F7F8
- Surface: #FFFFFF
- Surface Variant: #F3F4F6
- Border: #E5E7EB

Status colors:
- Success: #16A34A
- Warning: #D97706
- Error: #DC2626
- Info: #2563EB
- Disabled: #D1D5DB

### Theme Isolation

- Honey screens use Honey tokens only.
- Mint screens use Mint tokens only.
- Night screens use Night tokens only.
- Neutral screens use Neutral tokens only. Do not automatically turn Neutral into an admin dashboard unless explicitly requested.
- Do not borrow colors across themes.
- Do not invent new theme colors.
- Do not use generic Primary / Secondary / Tertiary / Neutral as four global M&D themes.

## Typography

M&D uses two practical type families:
- **Outfit** for display, headlines, cards, titles, and price emphasis.
- **Inter** for body copy, labels, captions, tables, metadata, and dense UI.

Typography should feel modern and friendly, but not childish. Consumer UI can be softer. Market and Orders must prioritize trust, readability, and scanability. Neutral UI must be restrained, professional, and readable without automatically becoming a management platform.

Core levels:
- `display-lg`: hero titles and landing headers.
- `display-md`: mobile hero titles.
- `headline-lg`: screen titles.
- `headline-md`: section headers.
- `title-md`: card titles.
- `body-md`: normal mobile body text.
- `body-sm`: supporting body text.
- `label-md`: buttons, tabs, chips, and compact UI.
- `caption`: timestamps, helper text, and metadata.
- `price`: product prices and order totals.
- `neutral-table-header`: uppercase table headers.
- `neutral-table-cell`: dense admin data.

Rules:
- Do not use decorative fonts.
- Do not use more than two font families.
- Do not make Neutral typography playful.
- Keep price text clear and prominent in Market and Orders.

## Layout

M&D uses a mobile-first layout system.

### Mobile

- Frame target: 390 脳 844 or similar.
- Layout: single-column.
- Page margin: 16px.
- Card padding: 16px.
- Section spacing: 24px.
- List item spacing: 12px.
- Primary actions should be thumb-friendly.
- Main app screens use bottom navigation: Feed, Market, Messages, Orders, Profile.

### Web

- Desktop content may use wider layouts and two-column detail pages.
- Consumer desktop may use top navigation or side navigation.
- Market and product detail pages may use a primary content area plus a secondary seller/order panel.

### Neutral

Neutral is a restrained visual style, not necessarily a management platform. Use it for neutral documentation, dense information, creator tools, and real admin pages only when requested.

When the screen is explicitly a dashboard or admin system:
- It may use a left sidebar and top header.
- Sidebar width: 240px.
- Header height: 64px.
- Main content padding: 24px.
- Table row height: 48px.
- Layouts may favor data density, filters, tables, and audit trails.

When the screen is not explicitly a dashboard/admin page:
- Keep the neutral style restrained and professional.
- Do not add management menus, moderation workflows, audit logs, or admin tables by default.

### Spacing Strategy

Use an 8px rhythm with a 4px micro-step:
- 4px for hairline gaps and micro alignment.
- 8px for compact spacing.
- 12px for small component gaps.
- 16px for mobile margins and card padding.
- 24px for section spacing.
- 32px for large groups.
- 48px for major vertical breaks.

## Elevation & Depth

M&D uses subtle depth through tonal layers, borders, and light shadows, not heavy 3D effects.

Consumer pages:
- Use soft cards on warm or mint backgrounds.
- Product cards may use a subtle shadow or a clear border.
- Dialogs and bottom sheets can use stronger depth.

Neutral pages:
- Prefer borders and tonal separation.
- Avoid heavy shadows.
- Keep UI restrained and professional.
- Do not add neutral-dashboard patterns unless the screen explicitly needs them.

Rules:
- No neumorphism.
- No glossy 3D effects.
- No random glassmorphism.
- No heavy dark shadows on consumer pages.

## Shapes

M&D uses rounded, friendly shapes for consumer UI and restrained corners for admin UI.

Shape rules:
- Consumer cards: 16px radius.
- Product cards: 16px radius.
- Message bubbles: 16px radius.
- Dialogs: 20px radius.
- Buttons: 12px radius or full pill where appropriate.
- Pet avatars: full radius.
- Neutral cards: 8px radius.
- True admin tables and dense controls: 4px radius.
- Neutral should not look cute or toy-like.

## Components

### Global Components

Use the existing tokens and component styles for buttons, inputs, search bars, text areas, selects, checkboxes, radio buttons, switches, avatars, pet avatars, badges, tags, chips, dividers, tooltips, toasts, dialogs, bottom sheets, and dropdown menus.

Interactive states must include default, hover where applicable, pressed, focused, disabled, loading, error, and success.

### Navigation Components

Mobile bottom navigation:
- Feed
- Market
- Messages
- Orders
- Profile

Neutral navigation:
- Use simple restrained labels for neutral documentation or creator tools.
- Use admin labels such as Users, Feed Review, Comments, Media, Reports, Audit Logs, and Settings only for true admin/moderation pages.

### Social Feed Components

Honey Theme only in normal mode.

Required components:
- Feed Card
- Post Composer Entry
- Post Media Grid
- Like Button
- Comment Button
- Share Button
- Follow Button
- Comment Item
- Comment Input Bar
- User Mini Card
- Pet Mini Card
- Feed Filter Tabs
- Feed Empty State
- Feed Skeleton State

Feed card anatomy:
- pet avatar,
- user name,
- pet type,
- timestamp,
- follow button,
- post text,
- media area,
- like action,
- comment action,
- share action.

Do not add unrelated modules such as care reminders or trending tags unless explicitly requested.

### Market Components

Mint Theme only in normal mode.

Required components:
- Product Card
- Product Image Gallery
- Price Tag
- Trade Type Chip
- Seller Credit Badge
- Product Filter Bar
- Price Range Filter
- Trade Type Filter
- Product Publish Entry
- Product Detail Header
- Seller Info Card
- Product Description Block
- Product Status Badge
- Product Empty State
- Product Skeleton State

Product card anatomy:
- product image,
- product title,
- price,
- trade type,
- seller credit badge,
- favorite / save action,
- primary marketplace action.

Allowed marketplace actions:
- Buy Now
- Contact Seller
- Make Offer
- View Details
- Save

Do not use generic Add to Cart or Cart as the primary model. M&D Market is a social commerce marketplace, not a generic shopping cart app.

Trade type chips:
- Sell
- Trade
- Looking For
- Free / Donation

### Messages Components

Honey Theme only in normal mode.

Required components:
- Conversation Item
- Message Bubble
- Message Input Bar
- Attachment Button
- Send Button
- Unread Badge
- Chat Header
- Compact Order Context Card
- Message Empty State
- Message Skeleton State

### Orders Components

Honey Theme only in normal mode.

Official order states:
- pending_payment
- paid
- cancelled
- shipped
- refunded
- completed

Order status mapping:
- pending_payment -> warning
- paid -> info
- shipped -> info
- completed -> success
- cancelled -> disabled / neutral
- refunded -> warning / error

Buyer actions:
- pending_payment: Pay Now, Cancel Order
- paid: Contact Seller
- shipped: Confirm Receipt, Contact Seller
- completed: Leave Review, View Details
- cancelled: View Details
- refunded: View Details

Seller actions:
- pending_payment: View Details
- paid: Ship Order, Contact Buyer
- shipped: View Logistics
- completed: View Details
- cancelled: View Details
- refunded: View Details

Invalid action rules:
- Do not show Ship Order to buyer.
- Do not show Pay Now to seller.
- Do not show Confirm Receipt before shipped.
- Do not show Leave Review before completed.
- Do not show Cancel Order after shipped.
- Do not show invalid actions for cancelled, refunded, or completed states.

### Dashboard Components

Neutral Theme only when the page is explicitly a dashboard or creator analytics surface.

Required components:
- Metric Card
- Sales Chart Card
- Content Performance Card
- Recent Orders List
- Top Products List
- Follower Growth Card
- Engagement Rate Card
- Dashboard Filter Bar
- Date Range Selector

Dashboard UI must be data-oriented, readable, and professional.

### True Admin / Moderation Components

Neutral Theme only. Use these only when the page is explicitly about admin, moderation, reports, user management, media review, audit logs, or system controls.

Required components:
- Admin Data Table
- Admin Filter Bar
- Admin Search Bar
- User Management Row
- User Detail Panel
- Feed Review Card
- Comment Moderation Row
- Media Review Card
- Report Handling Card
- Audit Log Item
- Approve Button
- Reject Button
- Resolve Button
- Dismiss Button
- Delete Comment Button
- Admin Confirmation Dialog
- Admin Batch Action Bar
- Admin Status Badge
- Admin Empty State
- Admin Error State

True admin action mapping:
- Approve: success
- Reject: error
- Resolve: success
- Dismiss: neutral
- Delete Comment: error

Neutral / true admin rules:
- Neutral must not look cute.
- Neutral must not borrow Honey or Mint colors as page-level styling.
- Neutral uses restrained neutral UI.
- True high-risk admin actions require confirmation.
- True moderation actions should create an Audit Log item.
- Do not add admin tables, moderation workflows, audit logs, or management menus unless explicitly requested.


## Google Stitch Output Rules

These rules must be included in every Stitch prompt for M&D design-system boards.

### Full-Height Non-Scrollable Board Rule

Stitch must output one complete, full-height design-system image.

Required:
- Extend the canvas height as needed.
- Show every section and component fully at once.
- Do not create internal scrolling.
- Do not crop cards, tables, components, or bottom content.
- Do not hide overflow.
- Do not use horizontal scrolling.
- Do not use carousels.
- Do not use sticky scroll panels.
- Do not hide component states behind scroll.
- A long image is preferred over a scrollable frame.

Use this sentence in prompts:

> Show the entire page content in one full-height image. Extend the canvas height as needed. No internal scrolling, no clipped content, no hidden overflow, no carousel, no horizontal scrolling, and no sticky scroll panels.

### Stitch Prompting Style

For best Stitch results:
- Work screen by screen.
- Name the exact screen being changed.
- Say whether it is a minor refinement or a major redesign.
- Preserve the current structure only when requested.
- Specify the page-level theme clearly.
- Specify exact tokens and hex values.
- Specify what must not change.
- Specify what must be removed.
- Ask for real UI preview plus design/spec breakdown for component boards.

## Component Documentation Board Rules

M&D design-system documentation boards should not be plain style collages. They should explain reusable components.

Every important component section should show:
1. **Real UI Preview** 鈥?how the component appears in an actual M&D product screen.
2. **Design / Spec Breakdown** 鈥?anatomy, token source, radius, spacing, typography, states, and usage rules.

Recommended board structure:
- Header with page title and short subtitle.
- Optional left sidebar in the current page-level theme.
- Section number markers using the page-level primary token.
- White/surface cards with official theme borders.
- Real preview on one side and specs on the other.
- Bottom empty/skeleton state section where relevant.

Component board visual rules:
- Use flat solid token colors only.
- Do not use random colors.
- Do not use gradients.
- Do not use glassmorphism.
- Do not use heavy shadows.
- Do not leave large empty areas.
- Do not show tiny unreadable labels.
- Keep Chinese-English spacing clean.
- Use Outfit for page titles, section titles, component names, product titles, and price emphasis.
- Use Inter for body text, metadata, labels, tables, specs, captions, and action text.

## Theme-Specific Board Guidance

### Honey Documentation Boards

Use for Honey component boards such as Basic Components, Social Feed Components, Messages & Orders Components, and Honey interaction examples.

Page-level tokens:
- Background: #FFF8F2
- Surface: #FFFFFF
- Surface Variant: #FFF1E6
- Primary: #FF8A3D
- Text Primary: #231F20
- Text Secondary: #6B5E57
- Border: #F1D8C8
- Divider: #F5E2D5

Board style:
- Warm Honey background.
- White rounded cards.
- Honey orange section markers.
- Warm borders.
- Subtle soft shadows.
- Cat-first, friendly, trustworthy, and clean.
- Not childish.

### Mint Documentation Boards

Use for Market and Product component boards.

Page-level tokens:
- Background: #F5FFFC
- Surface: #FFFFFF
- Surface Variant: #EAFBF6
- Primary: #2EC4A6
- Text Primary: #12312B
- Text Secondary: #49645E
- Border: #CDEFE6
- Divider: #DDF5EF

Board style:
- Fresh, clean, safe, breathable, and trustworthy.
- Marketplace/product-trust oriented.
- Do not use cart-first ecommerce language.
- Do not use Add to Cart as the dominant action.

### Night Documentation Boards

Use only for dark-mode documentation boards or dark-mode app screens.

Page-level tokens:
- Background: #0B0D12
- Surface: #151821
- Surface Variant: #1F2430
- Surface Elevated: #252B38
- Primary: #8B5CF6
- Text Primary: #F8FAFC
- Text Secondary: #CBD5E1
- Border: #303644

Board style:
- Black, deep gray, and purple first.
- Purple is the primary action color.
- Amber is only Cat Accent or Warning.
- Do not make Night a Honey dark mode.

### Neutral Documentation Boards

Use Neutral as a neutral professional style, not automatically as a management-platform page.

Page-level tokens:
- Background: #F7F7F8
- Surface: #FFFFFF
- Surface Variant: #F3F4F6
- Primary: #4B5563
- Text Primary: #111827
- Text Secondary: #4B5563
- Border: #E5E7EB
- Divider: #EEF0F2

Board style:
- Restrained.
- Neutral.
- Low-saturation.
- Professional.
- Good for system documentation and dense information.
- Do not add management menus, moderation workflows, audit logs, admin tables, or dashboard widgets unless explicitly requested.

When showing four theme variants on a neutral or documentation board, label the fourth variant as **Neutral**.

## Global Paw Loading Component

M&D uses a cat-first loading component called **GlobalPawLoading**.

### Anatomy

- Partial circular progress ring.
- Centered cat paw icon.
- `Loading...` label below.
- Clean, reusable, soft, not overly childish.
- Do not use a generic spinner when a branded M&D loading component is requested.
- Do not use a dog icon.

### Sizes

- SM: 24px
- MD: 40px
- LG: 56px

### Theme Variants

Each screen uses only the loading variant that matches the current screen theme.

Honey Paw Loading:
- Ring / active stroke: #FF8A3D
- Paw icon: #FF8A3D
- Soft inactive ring / container: #FFF1E6
- Text: #231F20

Mint Paw Loading:
- Ring / active stroke: #2EC4A6
- Paw icon: #2EC4A6
- Soft inactive ring / container: #E7FAF5
- Text: #12312B

Night Paw Loading:
- Ring / active stroke: #8B5CF6
- Paw icon: #8B5CF6
- Soft inactive ring / container: #2E1F4F
- Preview background: #151821
- Text: #F8FAFC

Neutral Paw Loading:
- Ring / active stroke: #4B5563
- Paw icon: #4B5563
- Soft inactive ring / container: #F3F4F6
- Text: #111827

Usage rule:
- One screen uses one loading theme variant only.
- Design-system boards may show all four variants as isolated component examples.
- Showing multiple variants as examples does not mean the page-level theme is mixed.

## Avatar Component Guidance

Avatars are global components and may adapt to the current theme.

Required avatar types:
- User Avatar
- Pet Avatar
- Seller Avatar
- Neutral Avatar
- Empty / Placeholder Avatar

Sizes:
- XS: 24px
- SM: 32px
- MD: 40px
- LG: 56px
- XL: 72px

Status/ring states:
- Default
- Online
- Verified
- Active
- Disabled

Theme rings:
- Honey ring: #FF8A3D
- Mint ring: #2EC4A6
- Night ring: #8B5CF6
- Neutral ring: #4B5563

Pet/cat accent variants:
- Honey cat accent: #FF7A45
- Mint cat accent: #FF8A65
- Night cat accent: #F59E0B
- Neutral rare cat accent: #F97316

Do not invent invalid hex colors such as `#7CSCF`.

## Social Feed Component Board Requirements

Honey Social Feed boards should include:
- Post Composer Entry
- Feed Card Complete Anatomy
- Post Media Grid: 1 image, 2 images, 3 images, 4 images
- Social Actions & States
- Comments & Utilities
- User Mini Card
- Pet Mini Card
- Feed Filter Tabs
- Feed Empty State
- Feed Skeleton State

Feed Card real preview must include:
- Pet avatar
- User name
- Pet name or pet type
- Timestamp
- Follow button
- Post text
- Media area
- Like, Comment, Share, Save actions
- Engagement counts

## Market Component Board Requirements

Mint Market boards should include:
- Product Card
- Product Image Gallery
- Price Tag
- Trade Type Chip
- Seller Credit Badge
- Product Filter Bar
- Price Range Filter
- Trade Type Filter
- Product Publish Entry
- Product Detail Header
- Seller Info Card
- Product Description Block
- Product Status Badge
- Product Empty State
- Product Skeleton State

Official trade type chips:
- Sell
- Trade
- Looking For
- Free / Donation

Allowed marketplace actions:
- Buy Now
- Contact Seller
- Make Offer
- View Details
- Save

Do not use:
- `Sale` as a trade type. Use `Sell`.
- Add to Cart as the dominant market action.
- Cart-first ecommerce behavior.

## Messages & Orders Component Board Requirements

Honey Messages & Orders boards should include:
- Conversation Item
- Message Bubble Sender
- Message Bubble Receiver
- Message Input Bar
- Attachment Button
- Send Button
- Unread Badge
- Chat Header
- Compact Order Context Card
- Order Status Badge
- Order Status Timeline
- Buyer Action Bar
- Seller Action Bar
- Order Card
- Message Empty State
- Order Empty State
- Message Skeleton State
- Order Skeleton State

Official order states:
- pending_payment
- paid
- cancelled
- shipped
- refunded
- completed

Do not use `delivered` as an official order state. Use `completed`.

Order status mapping:
- pending_payment -> warning
- paid -> info
- shipped -> info
- completed -> success
- cancelled -> disabled / neutral
- refunded -> warning / error

Buyer actions:
- pending_payment: Pay Now, Cancel Order
- paid: Contact Seller
- shipped: Confirm Receipt, Contact Seller
- completed: Leave Review, View Details
- cancelled: View Details
- refunded: View Details

Seller actions:
- pending_payment: View Details
- paid: Ship Order, Contact Buyer
- shipped: View Logistics
- completed: View Details
- cancelled: View Details
- refunded: View Details

Invalid action rules:
- Do not show Ship Order to buyer.
- Do not show Pay Now to seller.
- Do not show Confirm Receipt before shipped.
- Do not show Leave Review before completed.
- Do not show Cancel Order after shipped.
- Do not show invalid actions for cancelled, refunded, or completed states.

## Do's and Don'ts

### Do

- Always ask Stitch for a full-height non-scrollable image for design-system boards.
- Extend the canvas height as needed so every section is visible.
- Use one theme per screen.
- Use Honey for Feed, Profile, Messages normal mode, and Orders normal mode.
- Use Mint for Market and Product flows.
- Use Night only for dark mode.
- Use Neutral for restrained neutral styling, creator tools, dashboards, and explicit admin/moderation screens.
- Use official order states and action rules.
- Use marketplace actions instead of cart-first actions.
- Keep consumer pages warm and trustworthy.
- Keep market pages clean and safe.
- Keep Neutral pages professional and restrained.
- Use subtle pet cues rather than childish decoration.
- Use GlobalPawLoading for branded loading states when a M&D loader is requested.
- Label the fourth theme as Neutral when showing four variants.
- Maintain readable contrast.

### Don't

- Do not create one mixed color board where Primary, Secondary, Tertiary, and Neutral represent four themes.
- Do not treat Primary / Secondary / Tertiary / Neutral as the four M&D themes.
- Do not mix Honey, Mint, Night, and Neutral colors in one screen.
- Do not make Night Theme orange-primary.
- Do not make Neutral cute or playful.
- Do not add unrelated pet health features unless explicitly requested.
- Do not create Care Reminders or Trending Tags by default.
- Do not use Add to Cart as the dominant Market action.
- Do not show invalid order actions.
- Do not use glassmorphism, neon effects, heavy gradients, or glossy 3D styling.
- Do not use random colors outside this file.
- Do not make design-system boards scrollable.
- Do not crop or hide bottom content.
- Do not automatically convert Neutral into a management platform.
- Do not add admin tables, moderation workflows, or audit logs unless explicitly requested.
