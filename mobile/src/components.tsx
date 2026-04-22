// Shared UI primitives: Button, Input, Card, Screen. All styled with the
// Cursor-inspired tokens in ./theme.ts. Kept in a single file until they
// earn the right to be split up.

import type { ReactElement } from 'react';
import {
  ActivityIndicator,
  Pressable,
  type PressableProps,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  type TextProps,
  View,
  type ViewProps,
} from 'react-native';
import { colors, elevation, radius, spacing, typography } from './theme';

// ===== Screen ================================================================

// `refreshControl` is forwarded explicitly so callers can plug a
// <RefreshControl> into the inner ScrollView without prop-drilling
// through ViewProps.
export function Screen({
  children,
  scroll = true,
  style,
  contentStyle,
  refreshControl,
  ...rest
}: ViewProps & {
  scroll?: boolean;
  contentStyle?: ViewProps['style'];
  refreshControl?: ReactElement;
}) {
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
    <SafeAreaView style={[styles.screen, style]} {...rest}>
      {body}
    </SafeAreaView>
  );
}

// ===== Text ==================================================================

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
        { color: muted ? colors.inkSubtle : colors.ink },
        style,
      ]}
      {...rest}
    />
  );
}

// ===== Card ==================================================================

export function Card({ style, children, ...rest }: ViewProps) {
  return (
    <View style={[styles.card, style]} {...rest}>
      {children}
    </View>
  );
}

// ===== Button ================================================================

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
        <Text style={[styles.btnText, { color: variantStyles[variant].textColor }]}>
          {title}
        </Text>
      )}
    </Pressable>
  );
}

const variantStyles: Record<
  ButtonVariant,
  { base: object; pressed: object; textColor: string }
> = {
  primary: {
    base: { backgroundColor: colors.ink, borderColor: colors.ink },
    pressed: { backgroundColor: colors.danger, borderColor: colors.danger },
    textColor: colors.accentOnDark,
  },
  secondary: {
    base: { backgroundColor: colors.surface200, borderColor: colors.border },
    pressed: { backgroundColor: colors.surface300 },
    textColor: colors.ink,
  },
  ghost: {
    base: { backgroundColor: 'transparent', borderColor: 'transparent' },
    pressed: { backgroundColor: colors.surface200 },
    textColor: colors.ink,
  },
  danger: {
    base: { backgroundColor: colors.danger, borderColor: colors.danger },
    pressed: { backgroundColor: colors.ink },
    textColor: colors.accentOnDark,
  },
};

// ===== Input =================================================================

export function Input({
  label,
  error,
  hint,
  style,
  ...rest
}: TextInputProps & { label?: string; error?: string; hint?: string }) {
  return (
    <View style={{ gap: spacing.xs }}>
      {label ? (
        <Text style={[typography.label, { color: colors.inkMuted }]}>{label}</Text>
      ) : null}
      <TextInput
        placeholderTextColor={colors.inkSubtle}
        style={[styles.input, error ? styles.inputError : null, style]}
        {...rest}
      />
      {error ? (
        <Text style={[typography.bodySmall, { color: colors.danger }]}>{error}</Text>
      ) : hint ? (
        <Text style={[typography.bodySmall, { color: colors.inkSubtle }]}>{hint}</Text>
      ) : null}
    </View>
  );
}

// ===== Pill ==================================================================

export function Pill({ children }: { children: React.ReactNode }) {
  return (
    <View style={styles.pill}>
      <Text style={[typography.label, { color: colors.inkMuted }]}>{children}</Text>
    </View>
  );
}

// ===== styles ================================================================

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.surface100,
  },
  screenContent: {
    padding: spacing.lg,
    gap: spacing.lg,
    flexGrow: 1,
  },
  card: {
    backgroundColor: colors.surface200,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    padding: spacing.lg,
    gap: spacing.sm,
    ...elevation.card,
  },
  btn: {
    borderRadius: radius.md,
    borderWidth: StyleSheet.hairlineWidth,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 44,
  },
  btnText: {
    ...typography.label,
    letterSpacing: 0.5,
  },
  btnDisabled: {
    opacity: 0.5,
  },
  input: {
    borderRadius: radius.md,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.borderStrong,
    backgroundColor: colors.surface100,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    color: colors.ink,
    fontSize: 16,
    minHeight: 44,
  },
  inputError: {
    borderColor: colors.danger,
  },
  pill: {
    alignSelf: 'flex-start',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.s1,
    borderRadius: radius.pill,
    backgroundColor: colors.surface300,
  },
});
