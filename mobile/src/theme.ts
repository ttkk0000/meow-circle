/**
 * M&D cute kit — cat-first, doggie-friendly, aligned with the web M&D tokens.
 * Plus Jakarta Sans is loaded in `app/_layout.tsx` via `@expo-google-fonts/plus-jakarta-sans`.
 */

import { Platform } from 'react-native';

/** Font keys must match `useFonts` in root layout. */
export const fontFamily = {
  regular: 'PlusJakartaSans_400Regular',
  medium: 'PlusJakartaSans_500Medium',
  semiBold: 'PlusJakartaSans_600SemiBold',
  bold: 'PlusJakartaSans_700Bold',
  extraBold: 'PlusJakartaSans_800ExtraBold',
  /** Fallback before fonts load */
  system: Platform.select({
    ios: 'System',
    android: 'sans-serif',
    default: 'System',
  }) as string,
} as const;

export const colors = {
  canvas: '#F9F7F2',
  surface: '#ffffff',
  surfaceLow: '#fffaf4',
  surfaceContainer: '#fff0f6',
  onSurface: '#2b1722',
  onSurfaceVariant: 'rgba(43, 23, 34, 0.72)',
  primary: '#db3177',
  primaryContainer: '#ff4f93',
  onPrimary: '#ffffff',
  secondary: '#26b8d8',
  secondaryContainer: '#56c7ff',
  tertiary: '#006b54',
  tertiaryContainer: '#00a986',
  outline: 'rgba(43, 23, 34, 0.42)',
  outlineVariant: 'rgba(43, 23, 34, 0.16)',
  error: '#ba1a1a',

  /** Legacy aliases — keep RN screens/components compiling */
  surface100: '#fff7ee',
  surface200: '#ffffff',
  surface300: '#fffaf4',
  surface400: '#fff0f6',
  surface500: '#ffd0e2',
  ink: '#2b1722',
  inkMuted: 'rgba(43, 23, 34, 0.72)',
  inkSubtle: 'rgba(43, 23, 34, 0.58)',
  inkInvert: '#ffffff',
  border: 'rgba(43, 23, 34, 0.13)',
  borderMedium: 'rgba(43, 23, 34, 0.24)',
  borderStrong: 'rgba(43, 23, 34, 0.42)',
  borderSolid: '#2b1722',
  brand: '#ff4f93',
  brandWeak: 'rgba(255, 79, 147, 0.13)',
  gold: '#56c7ff',
  goldWeak: 'rgba(86, 199, 255, 0.18)',
  danger: '#ba1a1a',
  dangerBg: 'rgba(186, 26, 26, 0.08)',
  dangerBorder: 'rgba(186, 26, 26, 0.28)',
  success: '#006b54',
  successBg: 'rgba(0, 169, 134, 0.12)',
  accent: '#56c7ff',
  accentOnDark: '#ffffff',
  overlay: 'rgba(43, 23, 34, 0.45)',
  tabBar: 'rgba(255, 247, 238, 0.95)',
} as const;

export const radius = {
  sm: 8,
  md: 8,
  lg: 8,
  xl: 8,
  xxl: 8,
  pill: 9999,
} as const;

export const spacing = {
  s1: 4,
  s2: 8,
  s3: 12,
  s4: 16,
  s5: 24,
  s6: 32,
  s7: 48,
  s8: 64,
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const tracking = {
  tight: 0,
  label: 0,
} as const;

export const typography = {
  mega: {
    fontFamily: fontFamily.extraBold,
    fontSize: 28,
    fontWeight: '800' as const,
    letterSpacing: tracking.tight,
    lineHeight: 34,
  },
  h1: {
    fontFamily: fontFamily.extraBold,
    fontSize: 24,
    fontWeight: '800' as const,
    letterSpacing: tracking.tight,
    lineHeight: 30,
  },
  h2: {
    fontFamily: fontFamily.bold,
    fontSize: 22,
    fontWeight: '700' as const,
    lineHeight: 28,
  },
  h3: {
    fontFamily: fontFamily.bold,
    fontSize: 17,
    fontWeight: '700' as const,
    lineHeight: 22,
  },
  body: {
    fontFamily: fontFamily.medium,
    fontSize: 16,
    fontWeight: '500' as const,
    lineHeight: 24,
  },
  bodySmall: {
    fontFamily: fontFamily.regular,
    fontSize: 14,
    fontWeight: '400' as const,
    lineHeight: 20,
  },
  label: {
    fontFamily: fontFamily.semiBold,
    fontSize: 12,
    fontWeight: '600' as const,
    letterSpacing: tracking.label,
    lineHeight: 16,
  },
  mono: {
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'Menlo' }) as string,
    fontSize: 13,
  },
} as const;

export const elevation = {
  card: {
    shadowColor: '#ff4f93',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 14,
    elevation: 3,
  },
  fab: {
    shadowColor: '#ff4f93',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.22,
    shadowRadius: 16,
    elevation: 8,
  },
  soft: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.04,
    shadowRadius: 12,
    elevation: 2,
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
