import {type ComponentProps, forwardRef, type ReactElement, type ReactNode, useEffect, useMemo, useRef,} from 'react';
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
import {type Edge, SafeAreaView} from 'react-native-safe-area-context';
import {MaterialIcons} from '@expo/vector-icons';
import {type MndColors, radius, shadow, spacing, typography, useMndTheme,} from './theme';

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
  edges?: Edge[];
}) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
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
  subtle,
  style,
  ...rest
}: TextProps & { kind?: TypoKind; muted?: boolean; subtle?: boolean }) {
  const { colors } = useMndTheme();
  const color = subtle
    ? colors.onSurfaceSubtle
    : muted
      ? colors.onSurfaceVariant
      : colors.onSurface;
  return <Text style={[typography[kind], { color }, style]} {...rest} />;
}

export function Card({ style, children, ...rest }: ViewProps) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  return (
    <View style={[styles.card, style]} {...rest}>
      {children}
    </View>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'neutral';

export function Button({
  title,
  onPress,
  variant = 'primary',
  loading,
  disabled,
  icon,
  style,
  accessibilityLabel,
  ...rest
}: Omit<PressableProps, 'children'> & {
  title: string;
  variant?: ButtonVariant;
  loading?: boolean;
  icon?: ComponentProps<typeof MaterialIcons>['name'];
}) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const variantStyles = getButtonVariantStyles(colors);
  const isDisabled = Boolean(disabled || loading);
  const textColor = variantStyles[variant].textColor;

  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? title}
      accessibilityState={{ disabled: isDisabled, busy: Boolean(loading) }}
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
        <ActivityIndicator color={textColor} />
      ) : (
        <>
          {icon ? <MaterialIcons name={icon} size={18} color={textColor} /> : null}
          <Text style={[styles.btnText, { color: textColor }]}>{title}</Text>
        </>
      )}
    </Pressable>
  );
}

function getButtonVariantStyles(colors: MndColors): Record<
  ButtonVariant,
  { base: object; pressed: object; textColor: string }
> {
  return {
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
        borderColor: colors.border,
      },
      pressed: { backgroundColor: colors.surfaceContainer },
      textColor: colors.onSurface,
    },
    neutral: {
      base: {
        backgroundColor: colors.surface,
        borderColor: colors.border,
      },
      pressed: { backgroundColor: colors.surfaceLow },
      textColor: colors.onSurface,
    },
    ghost: {
      base: { backgroundColor: 'transparent', borderColor: 'transparent' },
      pressed: { backgroundColor: colors.accentSoft },
      textColor: colors.primaryContainer,
    },
    danger: {
      base: { backgroundColor: colors.error, borderColor: colors.error },
      pressed: { opacity: 0.9 },
      textColor: colors.onPrimary,
    },
  };
}

export const Input = forwardRef<
  TextInput,
  TextInputProps & { label?: string; error?: string; hint?: string }
>(function Input({ label, error, hint, style, accessibilityLabel, accessibilityHint, placeholder, ...rest }, ref) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  return (
    <View style={{ gap: spacing.xs }}>
      {label ? <Text style={[typography.label, { color: colors.onSurfaceVariant }]}>{label}</Text> : null}
      <TextInput
        ref={ref}
        placeholder={placeholder}
        placeholderTextColor={colors.onSurfaceSubtle}
        accessibilityLabel={accessibilityLabel ?? label ?? (typeof placeholder === 'string' ? placeholder : undefined)}
        accessibilityHint={accessibilityHint ?? error ?? hint}
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

export function Pill({
  children,
  tone = 'neutral',
}: {
  children: ReactNode;
  tone?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger';
}) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const palette = {
    neutral: { bg: colors.surfaceLow, fg: colors.onSurfaceVariant },
    brand: { bg: colors.accentSoft, fg: colors.primaryContainer },
    success: { bg: colors.successBg, fg: colors.success },
    warning: { bg: colors.warningBg, fg: colors.warning },
    danger: { bg: colors.errorBg, fg: colors.error },
  }[tone];
  return (
    <View style={[styles.pill, { backgroundColor: palette.bg }]}>
      <Text style={[typography.label, { color: palette.fg }]}>{children}</Text>
    </View>
  );
}

export function IconButton({
  icon,
  label,
  onPress,
  tone = 'neutral',
}: {
  icon: ComponentProps<typeof MaterialIcons>['name'];
  label: string;
  onPress?: () => void;
  tone?: 'neutral' | 'brand';
}) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      style={({ pressed }) => [
        styles.iconButton,
        tone === 'brand' && { backgroundColor: colors.accentSoft },
        pressed && styles.pressed,
      ]}
    >
      <MaterialIcons
        name={icon}
        size={22}
        color={tone === 'brand' ? colors.primaryContainer : colors.onSurface}
      />
    </Pressable>
  );
}

export function EmptyState({
  title,
  body,
  action,
}: {
  title: string;
  body: string;
  action?: ReactElement;
}) {
  return (
    <Card>
      <Txt kind="h3">{title}</Txt>
      <Txt muted>{body}</Txt>
      {action}
    </Card>
  );
}

export function MndLoader({ label = 'M&D 正在整理猫猫宇宙...' }: { label?: string }) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
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
      <Animated.Text style={[styles.loaderMark, { transform: [{ translateY: bob }] }]}>M&D</Animated.Text>
      <Animated.Text style={[styles.loaderDots, { opacity: pulse }]}>...</Animated.Text>
      <Txt kind="bodySmall" muted style={styles.loaderLabel}>
        {label}
      </Txt>
    </View>
  );
}

export const KittyLoader = MndLoader;

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
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
      ...shadow(colors.shadow, 'soft'),
    },
    btn: {
      borderRadius: radius.pill,
      borderWidth: StyleSheet.hairlineWidth,
      paddingVertical: spacing.md,
      paddingHorizontal: spacing.xl,
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 48,
      flexDirection: 'row',
      gap: spacing.sm,
    },
    btnText: {
      ...typography.label,
      fontSize: 15,
      letterSpacing: 0,
    },
    btnDisabled: {
      opacity: 0.45,
    },
    input: {
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
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
    iconButton: {
      width: 44,
      height: 44,
      borderRadius: radius.pill,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.surface,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    pressed: {
      opacity: 0.88,
      transform: [{ scale: 0.97 }],
    },
    loaderWrap: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.canvas,
      gap: spacing.sm,
    },
    loaderMark: {
      ...typography.mega,
      color: colors.primaryContainer,
    },
    loaderDots: {
      ...typography.h3,
      color: colors.primaryContainer,
      letterSpacing: 3,
    },
    loaderLabel: {
      color: colors.onSurfaceVariant,
    },
  });
}
