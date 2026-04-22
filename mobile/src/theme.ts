// Design tokens — Cursor Design System (see DESIGN.md).
// One source of truth for the whole mobile app. Values are kept byte-for-byte
// aligned with `web/theme.css` so a component styled in React Native reads
// visually identical to its web counterpart.
//
// Font note: Cursor's CursorGothic / jjannon / berkeleyMono are proprietary.
// The web ships Inter / EB Garamond / JetBrains Mono as free drop-ins. On
// mobile we currently fall back to platform defaults; wire `expo-google-fonts`
// + `expo-font` when we're ready to ship the custom trio.

import { Platform } from 'react-native';

export const colors = {
  // Cursor warm-cream surface scale
  surface100: '#f7f7f4',
  surface200: '#f2f1ed', // canvas (body background)
  surface300: '#ebeae5',
  surface400: '#e6e5e0',
  surface500: '#e1e0db',

  // Ink (warm near-black)
  ink: '#26251e',
  inkMuted: 'rgba(38, 37, 30, 0.72)',
  inkSubtle: 'rgba(38, 37, 30, 0.55)',
  inkInvert: '#f8f7f2',

  // Borders
  border: 'rgba(38, 37, 30, 0.1)',
  borderMedium: 'rgba(38, 37, 30, 0.2)',
  borderStrong: 'rgba(38, 37, 30, 0.55)',
  borderSolid: '#26251e',

  // Brand + accent (Cursor orange + gold)
  brand: '#f54e00',
  brandStrong: '#c03d00',
  brandWeak: 'rgba(245, 78, 0, 0.08)',
  brandText: '#8a2c00',
  gold: '#c08532',
  goldWeak: 'rgba(192, 133, 50, 0.14)',

  // Semantic
  danger: '#cf2d56', // signature crimson — hover + destructive
  dangerBg: 'rgba(207, 45, 86, 0.1)',
  dangerBorder: 'rgba(207, 45, 86, 0.25)',
  success: '#1f8a65',
  successBg: 'rgba(31, 138, 101, 0.12)',
  warning: '#b45309',
  warningBg: 'rgba(192, 133, 50, 0.16)',

  // Keep legacy aliases so existing callsites compile
  accent: '#26251e',
  accentOnDark: '#f7f7f4',

  // AI Timeline palette (DESIGN.md §4)
  tlThink: '#dfa88f',
  tlGrep: '#9fc9a2',
  tlRead: '#9fbbe0',
  tlEdit: '#c0a8dd',

  overlay: 'rgba(38, 37, 30, 0.55)',
} as const;

export const radius = {
  sm: 4,
  md: 8,
  lg: 10,
  xl: 16,
  pill: 9999,
} as const;

/** Spacing — numeric values match `web/theme.css` `--space-1` … `--space-10`. */
export const spacing = {
  s1: 4,
  s2: 8,
  s3: 12,
  s4: 16,
  s5: 24,
  s6: 32,
  s7: 48,
  s8: 64,
  s9: 96,
  s10: 128,
  /** @alias s1 */ xs: 4,
  /** @alias s2 */ sm: 8,
  /** @alias s3 */ md: 12,
  /** @alias s4 */ lg: 16,
  /** @alias s5 */ xl: 24,
  /** @alias s6 */ xxl: 32,
} as const;

export const fontFamily = {
  display: Platform.select({
    ios: 'System',
    android: 'sans-serif-medium',
    default: 'System',
  }) as string,
  body: Platform.select({
    ios: 'Georgia',
    android: 'serif',
    default: 'Georgia',
  }) as string,
  mono: Platform.select({
    ios: 'Menlo',
    android: 'monospace',
    default: 'Menlo',
  }) as string,
} as const;

// Letter-spacing scale from DESIGN.md §3 (negative tracking for display copy).
export const tracking = {
  display: -1.0,
  section: -0.5,
  sub: -0.2,
  caption: 0.6,
} as const;

export const typography = {
  h1: {
    fontFamily: fontFamily.display,
    fontSize: 32,
    fontWeight: '600' as const,
    letterSpacing: tracking.display,
    lineHeight: 36,
  },
  h2: {
    fontFamily: fontFamily.display,
    fontSize: 24,
    fontWeight: '600' as const,
    letterSpacing: tracking.section,
    lineHeight: 28,
  },
  h3: {
    fontFamily: fontFamily.display,
    fontSize: 18,
    fontWeight: '600' as const,
    letterSpacing: tracking.sub,
    lineHeight: 24,
  },
  body: {
    fontFamily: fontFamily.body,
    fontSize: 16,
    lineHeight: 24,
  },
  bodySmall: {
    fontFamily: fontFamily.body,
    fontSize: 14,
    lineHeight: 20,
  },
  label: {
    fontFamily: fontFamily.display,
    fontSize: 12,
    fontWeight: '500' as const,
    letterSpacing: tracking.caption,
    textTransform: 'uppercase' as const,
  },
  mono: {
    fontFamily: fontFamily.mono,
    fontSize: 13,
  },
} as const;

// Diffused shadows — Cursor's signature "28 / 70" double-shadow. React Native
// only takes a single layer per view; we approximate the hero value here.
export const elevation = {
  card: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.04,
    shadowRadius: 12,
    elevation: 2,
  },
  floating: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 14 },
    shadowOpacity: 0.14,
    shadowRadius: 32,
    elevation: 6,
  },
} as const;

export const theme = {
  colors,
  radius,
  spacing,
  fontFamily,
  typography,
  tracking,
  elevation,
};
export type Theme = typeof theme;
