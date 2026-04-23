/**
 * Dopamine Petal / Stitch kit — aligned with web `tw-stitch.js` + DESIGN.md in zip.
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
  surfaceLow: '#f5f3ee',
  surfaceContainer: '#f0eee9',
  onSurface: '#1b1c19',
  onSurfaceVariant: '#594042',
  primary: '#b52044',
  primaryContainer: '#ff5a77',
  onPrimary: '#ffffff',
  secondary: '#805600',
  secondaryContainer: '#fdaf18',
  tertiary: '#006b54',
  tertiaryContainer: '#00a986',
  outline: '#8d7072',
  outlineVariant: '#e1bec0',
  error: '#ba1a1a',

  /** Legacy aliases — keep RN screens/components compiling */
  surface100: '#F9F7F2',
  surface200: '#ffffff',
  surface300: '#f5f3ee',
  surface400: '#eae8e3',
  surface500: '#e4e2dd',
  ink: '#1b1c19',
  inkMuted: '#594042',
  inkSubtle: 'rgba(89, 64, 66, 0.62)',
  inkInvert: '#ffffff',
  border: 'rgba(141, 112, 114, 0.22)',
  borderMedium: 'rgba(141, 112, 114, 0.35)',
  borderStrong: '#8d7072',
  borderSolid: '#1b1c19',
  brand: '#ff5a77',
  brandWeak: 'rgba(255, 90, 119, 0.12)',
  gold: '#fdaf18',
  goldWeak: 'rgba(253, 175, 24, 0.18)',
  danger: '#ba1a1a',
  dangerBg: 'rgba(186, 26, 26, 0.08)',
  dangerBorder: 'rgba(186, 26, 26, 0.28)',
  success: '#006b54',
  successBg: 'rgba(0, 169, 134, 0.12)',
  accent: '#1b1c19',
  accentOnDark: '#ffffff',
  overlay: 'rgba(27, 28, 25, 0.45)',
  tabBar: 'rgba(249, 247, 242, 0.95)',
} as const;

export const radius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
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
  tight: -0.8,
  label: 0.2,
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
    shadowColor: '#ff5a77',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 14,
    elevation: 3,
  },
  fab: {
    shadowColor: '#ff5a77',
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
