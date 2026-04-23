---
name: Dopamine Petal
colors:
  surface: '#fbf9f4'
  surface-dim: '#dbdad5'
  surface-bright: '#fbf9f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3ee'
  surface-container: '#f0eee9'
  surface-container-high: '#eae8e3'
  surface-container-highest: '#e4e2dd'
  on-surface: '#1b1c19'
  on-surface-variant: '#594042'
  inverse-surface: '#30312e'
  inverse-on-surface: '#f2f1ec'
  outline: '#8d7072'
  outline-variant: '#e1bec0'
  surface-tint: '#b52044'
  primary: '#b52044'
  on-primary: '#ffffff'
  primary-container: '#ff5a77'
  on-primary-container: '#62001d'
  inverse-primary: '#ffb2b9'
  secondary: '#805600'
  on-secondary: '#ffffff'
  secondary-container: '#fdaf18'
  on-secondary-container: '#694600'
  tertiary: '#006b54'
  on-tertiary: '#ffffff'
  tertiary-container: '#00a986'
  on-tertiary-container: '#003528'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdadc'
  primary-fixed-dim: '#ffb2b9'
  on-primary-fixed: '#400010'
  on-primary-fixed-variant: '#91002f'
  secondary-fixed: '#ffddb0'
  secondary-fixed-dim: '#ffba46'
  on-secondary-fixed: '#281800'
  on-secondary-fixed-variant: '#614000'
  tertiary-fixed: '#75f9d1'
  tertiary-fixed-dim: '#55dcb6'
  on-tertiary-fixed: '#002118'
  on-tertiary-fixed-variant: '#00513f'
  background: '#fbf9f4'
  on-background: '#1b1c19'
  surface-variant: '#e4e2dd'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 36px
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  container-padding: 16px
  card-gap: 12px
---

## Brand & Style

This design system draws inspiration from the high-energy, visually dense, and emotionally expressive world of modern Chinese social commerce. The brand personality is "Vibrant Sophistication"—a blend of youthful energy (Dopamine style) and premium execution. It targets a Gen-Z and Millennial demographic that values curation, aesthetic pleasure, and tactile digital experiences.

The design style is **Tactile Minimalism**. It utilizes heavy whitespace and a clean cream foundation but punctuates it with "pops" of high-saturation color and ultra-soft, squishy UI elements. The goal is to evoke a sense of joy, discovery, and physical comfort, moving away from flat, clinical interfaces toward something that feels alive and "clickable."

## Colors

The palette is centered around a "Dopamine" philosophy—using color to trigger a positive emotional response. 

- **Primary (Petal Pink):** A trendy, vibrant coral-pink used for primary actions, active states, and brand highlights.
- **Secondary (Sunlight):** A warm, saturated yellow used for "New" tags, ratings, and secondary calls to action.
- **Tertiary (Fresh Mint):** An energetic green for success states and price-related tags.
- **Background (Cream):** A warm, off-white (#F9F7F2) that reduces eye strain and makes the vibrant accent colors feel more premium and less aggressive than pure white.
- **Surface:** Pure white (#FFFFFF) is reserved strictly for cards and elevated containers to create a clear "layering" effect against the cream background.

## Typography

The typography system uses **Plus Jakarta Sans** as the English equivalent to high-quality rounded Chinese fonts like HarmonyOS Sans or MiSans. The weight distribution is intentional: heavy weights (700-800) are used for headlines to create a "bold" editorial feel similar to magazine layouts.

Body text maintains a medium weight (500) where possible to ensure legibility against the cream background. Line heights are generous to allow for the visual density common in "discovery" style feeds. For Chinese character implementation, ensure a fallback to `system-ui` or specific rounded fonts to maintain the "soft" aesthetic across languages.

## Layout & Spacing

The layout utilizes a **Fluid Grid** system optimized for mobile vertical scrolling. A standard 16px side margin is used for the primary container. Inside discovery feeds (the "Xiaohongshu" look), a 2-column staggered grid is the standard, using a 12px gutter to maximize image real estate while maintaining clear separation.

Spacing is based on a 4px baseline, but the system favors larger increments (16px, 24px) to create the "airy" and spacious feeling required to offset the vibrant colors. Elements should feel grouped by proximity, with large 32px gaps between distinct logical sections.

## Elevation & Depth

This design system rejects harsh shadows in favor of **Ambient Tonal Depth**. Depth is communicated through:

1.  **Layering:** White cards (#FFFFFF) sit atop the Cream background (#F9F7F2).
2.  **Soft Glows:** Instead of black shadows, use ultra-diffused shadows with a hint of the primary color or a neutral warm grey (e.g., `rgba(255, 90, 119, 0.08)`).
3.  **Backdrop Blurs:** For overlays and navigation bars, use a 20px Gaussian blur with 80% opacity to maintain the "Glassmorphism" influence while keeping the cream-tinted warmth.
4.  **Pressed States:** Buttons do not just change color; they physically scale down slightly (0.96 scale) to simulate a tactile "squish."

## Shapes

The shape language is extremely organic and "bubbly." The base unit for roundedness is **24px (rounded-xl)** for all primary cards and modal containers. 

Buttons and chips utilize a **full pill-shape** to provide a friendly, non-threatening touch target. Small elements like checkboxes and inner image containers should never drop below a 12px radius. This consistency in extreme rounding reinforces the playful, "Dopamine" aesthetic and differentiates the product from more rigid, corporate competitors.

## Components

- **Buttons:** Primary buttons use a vibrant Petal Pink gradient with white text. They should have a subtle inner-glow at the top to feel "inflated."
- **Cards:** Product and content cards must use a 24px corner radius. The image within the card should have a top-only 24px radius or be inset with a 16px margin.
- **Chips/Tags:** Used for categorization. These should have a background color that is a 10% opacity version of the text color (e.g., Pink text on a pale pink background) to maintain the "Dopamine" vibe without overwhelming the user.
- **Input Fields:** Soft cream backgrounds slightly darker than the main page background, with 24px rounded corners and no border except when focused. Focus state uses a 2px Petal Pink border.
- **Playful Icons:** Iconography should be "thick-stroke" (2pt or 2.5pt) with rounded ends. Use two-tone icons where a secondary color (like Sunlight yellow) is used for a small accent dot or spark within the icon.
- **Bottom Navigation:** A frosted cream bar with a slight lift. Icons in the active state should scale up and bounce slightly when tapped.