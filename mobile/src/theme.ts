/**
 * M&D mobile design tokens.
 *
 * The Stitch project defines four first-class client themes:
 * Honey, Mint, Night, and Neutral. Neutral is a real theme, not an
 * OS/default fallback. Cards, panels, inputs, and tiles keep the 8dp Stitch radius.
 */

import {
  createContext,
  createElement,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import {Platform} from 'react-native';
import * as SecureStore from 'expo-secure-store';

export const fontFamily = {
  regular: 'PlusJakartaSans_400Regular',
  medium: 'PlusJakartaSans_500Medium',
  semiBold: 'PlusJakartaSans_600SemiBold',
  bold: 'PlusJakartaSans_700Bold',
  extraBold: 'PlusJakartaSans_800ExtraBold',
  fallback: Platform.select({
    ios: 'System',
    android: 'sans-serif',
    default: 'System',
  }) as string,
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
    letterSpacing: 0,
    lineHeight: 28,
  },
  h3: {
    fontFamily: fontFamily.bold,
    fontSize: 17,
    fontWeight: '700' as const,
    letterSpacing: 0,
    lineHeight: 22,
  },
  body: {
    fontFamily: fontFamily.medium,
    fontSize: 16,
    fontWeight: '500' as const,
    letterSpacing: 0,
    lineHeight: 24,
  },
  bodySmall: {
    fontFamily: fontFamily.regular,
    fontSize: 14,
    fontWeight: '400' as const,
    letterSpacing: 0,
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

function withAliases(base: {
  canvas: string;
  surface: string;
  surfaceLow: string;
  surfaceContainer: string;
  surfaceRaised: string;
  onSurface: string;
  onSurfaceVariant: string;
  onSurfaceSubtle: string;
  primary: string;
  primaryStrong: string;
  primaryContainer: string;
  onPrimary: string;
  secondary: string;
  secondaryContainer: string;
  tertiary: string;
  tertiaryContainer: string;
  accent: string;
  accentSoft: string;
  outline: string;
  outlineVariant: string;
  border: string;
  borderMedium: string;
  borderStrong: string;
  error: string;
  errorBg: string;
  success: string;
  successBg: string;
  warning: string;
  warningBg: string;
  overlay: string;
  tabBar: string;
  shadow: string;
  isDark: boolean;
}) {
  return {
    ...base,
    surface100: base.canvas,
    surface200: base.surface,
    surface300: base.surfaceLow,
    surface400: base.surfaceContainer,
    surface500: base.border,
    ink: base.onSurface,
    inkMuted: base.onSurfaceVariant,
    inkSubtle: base.onSurfaceSubtle,
    inkInvert: base.onPrimary,
    borderSolid: base.onSurface,
    brand: base.primaryContainer,
    brandWeak: base.accentSoft,
    gold: base.secondary,
    goldWeak: base.warningBg,
    danger: base.error,
    dangerBg: base.errorBg,
    dangerBorder: base.errorBg,
    accentOnDark: base.onPrimary,
  } as const;
}

export const themes = {
  honey: {
    name: 'honey',
    label: 'Honey',
    colors: withAliases({
      canvas: '#FFF8F2',
      surface: '#FFFFFF',
      surfaceLow: '#FFF1E6',
      surfaceContainer: '#FFF8F2',
      surfaceRaised: '#FFFFFF',
      onSurface: '#231F20',
      onSurfaceVariant: '#6B5E57',
      onSurfaceSubtle: '#8A7266',
      primary: '#9A4600',
      primaryStrong: '#F0782C',
      primaryContainer: '#FF8A3D',
      onPrimary: '#FFFFFF',
      secondary: '#FFD166',
      secondaryContainer: '#FFF2C2',
      tertiary: '#7C5CFF',
      tertiaryContainer: '#EFE9FF',
      accent: '#FF7A45',
      accentSoft: 'rgba(255, 138, 61, 0.15)',
      outline: '#CBA88F',
      outlineVariant: '#F1D8C8',
      border: '#F1D8C8',
      borderMedium: '#E9E0E1',
      borderStrong: '#8A7266',
      error: '#EF4444',
      errorBg: 'rgba(239, 68, 68, 0.1)',
      success: '#22C55E',
      successBg: 'rgba(34, 197, 94, 0.12)',
      warning: '#F59E0B',
      warningBg: 'rgba(245, 158, 11, 0.14)',
      overlay: 'rgba(35, 31, 32, 0.45)',
      tabBar: 'rgba(255, 248, 242, 0.96)',
      shadow: '#FF8A3D',
      isDark: false,
    }),
  },
  mint: {
    name: 'mint',
    label: 'Mint',
    colors: withAliases({
      canvas: '#F5FFFC',
      surface: '#FFFFFF',
      surfaceLow: '#EAFBF6',
      surfaceContainer: '#CDEFE6',
      surfaceRaised: '#FFFFFF',
      onSurface: '#12312B',
      onSurfaceVariant: '#4D6866',
      onSurfaceSubtle: '#6F8580',
      primary: '#12312B',
      primaryStrong: '#16A34A',
      primaryContainer: '#2EC4A6',
      onPrimary: '#FFFFFF',
      secondary: '#A7F3D0',
      secondaryContainer: '#DDFBF0',
      tertiary: '#0EA5E9',
      tertiaryContainer: '#E0F2FE',
      accent: '#FF8A65',
      accentSoft: 'rgba(46, 196, 166, 0.13)',
      outline: '#8CCFC0',
      outlineVariant: '#CDEFE6',
      border: '#CDEFE6',
      borderMedium: '#A7F3D0',
      borderStrong: '#4D6866',
      error: '#EF4444',
      errorBg: 'rgba(239, 68, 68, 0.1)',
      success: '#16A34A',
      successBg: 'rgba(22, 163, 74, 0.12)',
      warning: '#F59E0B',
      warningBg: 'rgba(245, 158, 11, 0.14)',
      overlay: 'rgba(18, 49, 43, 0.45)',
      tabBar: 'rgba(245, 255, 252, 0.96)',
      shadow: '#2EC4A6',
      isDark: false,
    }),
  },
  night: {
    name: 'night',
    label: 'Night',
    colors: withAliases({
      canvas: '#0B0D12',
      surface: '#151821',
      surfaceLow: '#1F2430',
      surfaceContainer: '#252B38',
      surfaceRaised: '#151821',
      onSurface: '#F8FAFC',
      onSurfaceVariant: '#CBD5E1',
      onSurfaceSubtle: '#94A3B8',
      primary: '#F8FAFC',
      primaryStrong: '#A78BFA',
      primaryContainer: '#8B5CF6',
      onPrimary: '#FFFFFF',
      secondary: '#FBBF24',
      secondaryContainer: 'rgba(251, 191, 36, 0.16)',
      tertiary: '#60A5FA',
      tertiaryContainer: 'rgba(96, 165, 250, 0.16)',
      accent: '#6366F1',
      accentSoft: 'rgba(139, 92, 246, 0.17)',
      outline: '#475569',
      outlineVariant: '#303644',
      border: 'rgba(248, 250, 252, 0.14)',
      borderMedium: 'rgba(248, 250, 252, 0.24)',
      borderStrong: 'rgba(248, 250, 252, 0.38)',
      error: '#F87171',
      errorBg: 'rgba(248, 113, 113, 0.14)',
      success: '#4ADE80',
      successBg: 'rgba(74, 222, 128, 0.14)',
      warning: '#F59E0B',
      warningBg: 'rgba(245, 158, 11, 0.16)',
      overlay: 'rgba(0, 0, 0, 0.62)',
      tabBar: 'rgba(11, 13, 18, 0.96)',
      shadow: '#000000',
      isDark: true,
    }),
  },
  neutral: {
    name: 'neutral',
    label: 'Neutral',
    colors: withAliases({
      canvas: '#F7F7F8',
      surface: '#FFFFFF',
      surfaceLow: '#F3F4F6',
      surfaceContainer: '#E5E7EB',
      surfaceRaised: '#FFFFFF',
      onSurface: '#111827',
      onSurfaceVariant: '#4B5563',
      onSurfaceSubtle: '#6B7280',
      primary: '#4B5563',
      primaryStrong: '#374151',
      primaryContainer: '#4B5563',
      onPrimary: '#FFFFFF',
      secondary: '#9CA3AF',
      secondaryContainer: '#F3F4F6',
      tertiary: '#F97316',
      tertiaryContainer: 'rgba(249, 115, 22, 0.12)',
      accent: '#F97316',
      accentSoft: 'rgba(75, 85, 99, 0.12)',
      outline: '#9CA3AF',
      outlineVariant: '#E5E7EB',
      border: '#E5E7EB',
      borderMedium: '#D1D5DB',
      borderStrong: '#4B5563',
      error: '#DC2626',
      errorBg: 'rgba(220, 38, 38, 0.1)',
      success: '#16A34A',
      successBg: 'rgba(22, 163, 74, 0.12)',
      warning: '#D97706',
      warningBg: 'rgba(217, 119, 6, 0.12)',
      overlay: 'rgba(17, 24, 39, 0.45)',
      tabBar: 'rgba(247, 247, 248, 0.96)',
      shadow: '#111827',
      isDark: false,
    }),
  },
} as const;

export type MndThemeName = keyof typeof themes;
export type MndTheme = (typeof themes)[MndThemeName];
export type MndColors = (typeof themes.honey)['colors'];

export const themeOrder: MndThemeName[] = ['honey', 'mint', 'night', 'neutral'];
export const defaultThemeName: MndThemeName = 'honey';
const THEME_KEY = 'mnd.mobile.theme';

export const colors = themes.honey.colors;

export const elevation = {
  card: {
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 14,
    elevation: 3,
  },
  fab: {
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.22,
    shadowRadius: 16,
    elevation: 8,
  },
  soft: {
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.04,
    shadowRadius: 12,
    elevation: 2,
  },
} as const;

export function shadow(color: string, kind: keyof typeof elevation = 'soft') {
  return {
    shadowColor: color,
    ...elevation[kind],
  };
}

export const theme = {
  ...themes.honey,
  radius,
  spacing,
  fontFamily,
  typography,
  tracking,
  elevation,
};

export type Theme = typeof theme;

type MndThemeContextValue = {
  themeName: MndThemeName;
  setThemeName: (name: MndThemeName) => void;
  cycleTheme: () => void;
  theme: MndTheme;
  colors: MndColors;
  isDark: boolean;
};

const MndThemeContext = createContext<MndThemeContextValue | null>(null);

function isThemeName(value: string | null): value is MndThemeName {
  return !!value && value in themes;
}

function getWebStorage():
  | { getItem: (key: string) => string | null; setItem: (key: string, value: string) => void }
  | null {
  const maybeGlobal = globalThis as {
    localStorage?: { getItem: (key: string) => string | null; setItem: (key: string, value: string) => void };
  };
  return maybeGlobal.localStorage ?? null;
}

async function readStoredTheme(): Promise<MndThemeName | null> {
  if (Platform.OS === 'web') {
    try {
      const value = getWebStorage()?.getItem(THEME_KEY) ?? null;
      return isThemeName(value) ? value : null;
    } catch {
      return null;
    }
  }
  const value = await SecureStore.getItemAsync(THEME_KEY).catch(() => null);
  return isThemeName(value) ? value : null;
}

async function writeStoredTheme(name: MndThemeName) {
  if (Platform.OS === 'web') {
    try {
      getWebStorage()?.setItem(THEME_KEY, name);
    } catch {
      /* ignore */
    }
    return;
  }
  await SecureStore.setItemAsync(THEME_KEY, name).catch(() => null);
}

export function MndThemeProvider({ children }: { children: ReactNode }) {
  const [themeName, setThemeNameState] = useState<MndThemeName>(defaultThemeName);

  useEffect(() => {
    let active = true;
    readStoredTheme().then((stored) => {
      if (active && stored) setThemeNameState(stored);
    });
    return () => {
      active = false;
    };
  }, []);

  const setThemeName = useCallback((name: MndThemeName) => {
    setThemeNameState(name);
    writeStoredTheme(name);
  }, []);

  const cycleTheme = useCallback(() => {
    setThemeNameState((current) => {
      const next = themeOrder[(themeOrder.indexOf(current) + 1) % themeOrder.length];
      writeStoredTheme(next);
      return next;
    });
  }, []);

  const value = useMemo<MndThemeContextValue>(() => {
    const selected = themes[themeName];
    return {
      themeName,
      setThemeName,
      cycleTheme,
      theme: selected,
      colors: selected.colors,
      isDark: selected.colors.isDark,
    };
  }, [cycleTheme, setThemeName, themeName]);

  return createElement(MndThemeContext.Provider, { value }, children);
}

export function useMndTheme(): MndThemeContextValue {
  const ctx = useContext(MndThemeContext);
  if (!ctx) {
    const selected = themes[defaultThemeName];
    return {
      themeName: defaultThemeName,
      setThemeName: () => undefined,
      cycleTheme: () => undefined,
      theme: selected,
      colors: selected.colors,
      isDark: selected.colors.isDark,
    };
  }
  return ctx;
}
