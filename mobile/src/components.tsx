// Shared UI — Dopamine Petal (Stitch) styling.

import { forwardRef, useEffect, useRef, type ReactElement } from 'react';
import {
  ActivityIndicator,
  Animated,
  Pressable,
  type PressableProps,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  type TextProps,
  View,
  type ViewProps,
} from 'react-native';
import { SafeAreaView, type Edge } from 'react-native-safe-area-context';
import { colors, elevation, radius, spacing, typography } from './theme';

export function Screen({
  children,
  scroll = true,
  style,
  contentStyle,
  refreshControl,
  edges,
  ...rest
}: ViewProps & {
  scroll?: boolean;
  contentStyle?: ViewProps['style'];
  refreshControl?: ReactElement;
  /** Safe-area edges; default top+left+right (bottom uses tab bar / home indicator elsewhere). */
  edges?: Edge[];
}) {
  const safeEdges: Edge[] = edges ?? ['top', 'left', 'right'];
  const body = scroll ? (
    <ScrollView
      contentContainerStyle={[styles.screenContent, contentStyle]}
      keyboardShouldPersistTaps="handled"
      refreshControl={refreshControl}
    >
      {children}
    </ScrollView>
  ) : (
    <View style={[styles.screenContent, contentStyle]}>{children}</View>
  );
  return (
    <SafeAreaView style={[styles.screen, style]} edges={safeEdges} {...rest}>
      {body}
    </SafeAreaView>
  );
}

type TypoKind = keyof typeof typography;

export function Txt({
  kind = 'body',
  muted,
  style,
  ...rest
}: TextProps & { kind?: TypoKind; muted?: boolean }) {
  return (
    <Text
      style={[
        typography[kind],
        { color: muted ? colors.onSurfaceVariant : colors.onSurface },
        style,
      ]}
      {...rest}
    />
  );
}

export function Card({ style, children, ...rest }: ViewProps) {
  return (
    <View style={[styles.card, style]} {...rest}>
      {children}
    </View>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

export function Button({
  title,
  onPress,
  variant = 'primary',
  loading,
  disabled,
  style,
  ...rest
}: Omit<PressableProps, 'children'> & {
  title: string;
  variant?: ButtonVariant;
  loading?: boolean;
}) {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      style={(state) => [
        styles.btn,
        variantStyles[variant].base,
        state.pressed && variantStyles[variant].pressed,
        isDisabled && styles.btnDisabled,
        typeof style === 'function' ? style(state) : style,
      ]}
      {...rest}
    >
      {loading ? (
        <ActivityIndicator color={variantStyles[variant].textColor} />
      ) : (
        <Text style={[styles.btnText, { color: variantStyles[variant].textColor }]}>{title}</Text>
      )}
    </Pressable>
  );
}

const variantStyles: Record<
  ButtonVariant,
  { base: object; pressed: object; textColor: string }
> = {
  primary: {
    base: {
      backgroundColor: colors.primaryContainer,
      borderColor: colors.primaryContainer,
    },
    pressed: { opacity: 0.92, transform: [{ scale: 0.98 }] },
    textColor: colors.onPrimary,
  },
  secondary: {
    base: {
      backgroundColor: colors.surfaceLow,
      borderColor: colors.outlineVariant,
    },
    pressed: { backgroundColor: colors.surfaceContainer },
    textColor: colors.onSurface,
  },
  ghost: {
    base: { backgroundColor: 'transparent', borderColor: 'transparent' },
    pressed: { backgroundColor: colors.brandWeak },
    textColor: colors.primaryContainer,
  },
  danger: {
    base: { backgroundColor: colors.error, borderColor: colors.error },
    pressed: { opacity: 0.9 },
    textColor: colors.onPrimary,
  },
};

export const Input = forwardRef<
  TextInput,
  TextInputProps & { label?: string; error?: string; hint?: string }
>(function Input({ label, error, hint, style, ...rest }, ref) {
  return (
    <View style={{ gap: spacing.xs }}>
      {label ? (
        <Text style={[typography.label, { color: colors.onSurfaceVariant }]}>{label}</Text>
      ) : null}
      <TextInput
        ref={ref}
        placeholderTextColor={colors.outline}
        style={[styles.input, error ? styles.inputError : null, style]}
        {...rest}
      />
      {error ? (
        <Text style={[typography.bodySmall, { color: colors.error }]}>{error}</Text>
      ) : hint ? (
        <Text style={[typography.bodySmall, { color: colors.onSurfaceVariant }]}>{hint}</Text>
      ) : null}
    </View>
  );
});

export function Pill({ children, tone = 'neutral' }: { children: React.ReactNode; tone?: 'neutral' | 'brand' }) {
  return (
    <View
      style={[
        styles.pill,
        tone === 'brand' ? { backgroundColor: colors.brandWeak } : { backgroundColor: colors.surfaceLow },
      ]}
    >
      <Text style={[typography.label, { color: tone === 'brand' ? colors.primaryContainer : colors.onSurfaceVariant }]}>
        {children}
      </Text>
    </View>
  );
}

export function KittyLoader({ label = '正在叫猫咪起床...' }: { label?: string }) {
  const bob = useRef(new Animated.Value(0)).current;
  const pulse = useRef(new Animated.Value(0.45)).current;

  useEffect(() => {
    const bobAnim = Animated.loop(
      Animated.sequence([
        Animated.timing(bob, { toValue: -8, duration: 520, useNativeDriver: true }),
        Animated.timing(bob, { toValue: 0, duration: 620, useNativeDriver: true }),
      ]),
    );
    const pulseAnim = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 680, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0.45, duration: 680, useNativeDriver: true }),
      ]),
    );
    bobAnim.start();
    pulseAnim.start();
    return () => {
      bobAnim.stop();
      pulseAnim.stop();
    };
  }, [bob, pulse]);

  return (
    <View style={styles.loaderWrap}>
      <Animated.Text style={[styles.loaderKitty, { transform: [{ translateY: bob }] }]}>🐈</Animated.Text>
      <Animated.Text style={[styles.loaderDots, { opacity: pulse }]}>• • •</Animated.Text>
      <Txt kind="bodySmall" muted style={styles.loaderLabel}>
        {label}
      </Txt>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  screenContent: {
    padding: spacing.lg,
    gap: spacing.md,
    flexGrow: 1,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    padding: spacing.lg,
    gap: spacing.sm,
    ...elevation.soft,
  },
  btn: {
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.xl,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 48,
  },
  btnText: {
    ...typography.label,
    fontSize: 15,
    letterSpacing: 0.3,
  },
  btnDisabled: {
    opacity: 0.45,
  },
  input: {
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.outlineVariant,
    backgroundColor: colors.surfaceLow,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    color: colors.onSurface,
    fontSize: 16,
    minHeight: 48,
    fontFamily: typography.body.fontFamily,
  },
  inputError: {
    borderColor: colors.error,
  },
  pill: {
    alignSelf: 'flex-start',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.s1 + 2,
    borderRadius: radius.pill,
  },
  loaderWrap: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.canvas,
    gap: spacing.sm,
  },
  loaderKitty: {
    fontSize: 42,
  },
  loaderDots: {
    fontSize: 18,
    color: colors.primaryContainer,
    letterSpacing: 3,
  },
  loaderLabel: {
    color: colors.onSurfaceVariant,
  },
});
