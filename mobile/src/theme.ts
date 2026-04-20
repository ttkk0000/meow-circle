// Design tokens mirroring web/theme.css (Cursor Design System):
// warm-cream surfaces, near-black text, crimson danger/hover, three-font
// system (display / serif / mono). Kept intentionally small — one source
// of truth for the whole app.

import { Platform } from 'react-native';

export const colors = {
  surface100: '#F7F3EC', // canvas
  surface200: '#F1EADF',
  surface300: '#E8DFCE',
  surface400: '#D9CCB3',
  surface500: '#B9A987',

  ink: '#1C1A17',
  inkMuted: '#4A453E',
  inkSubtle: '#7A7065',

  border: 'rgba(28, 26, 23, 0.12)',
  borderStrong: 'rgba(28, 26, 23, 0.24)',

  accent: '#1C1A17', // ink-as-accent
  accentOnDark: '#F7F3EC',
  danger: '#B3261E', // crimson — hover / destructive

  success: '#4B6A4F',
  warning: '#B08B3F',

  overlay: 'rgba(28, 26, 23, 0.55)',
} as const;

export const radius = {
  sm: 6,
  md: 10,
  lg: 14,
  xl: 20,
  pill: 999,
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const fontFamily = {
  // Platform defaults give us "display-ish" grotesk + serif + mono without
  // shipping custom fonts. Swap with `expo-font` later when we add the
  // Cursor trio (Söhne / Source Serif / JetBrains Mono).
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

export const typography = {
  h1: { fontFamily: fontFamily.display, fontSize: 28, fontWeight: '600' as const, letterSpacing: -0.3 },
  h2: { fontFamily: fontFamily.display, fontSize: 22, fontWeight: '600' as const, letterSpacing: -0.2 },
  h3: { fontFamily: fontFamily.display, fontSize: 18, fontWeight: '600' as const },
  body: { fontFamily: fontFamily.body, fontSize: 16, lineHeight: 24 },
  bodySmall: { fontFamily: fontFamily.body, fontSize: 14, lineHeight: 20 },
  label: { fontFamily: fontFamily.display, fontSize: 13, fontWeight: '500' as const, letterSpacing: 0.3 },
  mono: { fontFamily: fontFamily.mono, fontSize: 13 },
} as const;

export const elevation = {
  // Diffused shadows — the Cursor look.
  card: {
    shadowColor: '#1C1A17',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.08,
    shadowRadius: 18,
    elevation: 3,
  },
  floating: {
    shadowColor: '#1C1A17',
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.12,
    shadowRadius: 32,
    elevation: 6,
  },
} as const;

export const theme = { colors, radius, spacing, fontFamily, typography, elevation };
export type Theme = typeof theme;
