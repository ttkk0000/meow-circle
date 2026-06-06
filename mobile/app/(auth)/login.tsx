import {MaterialIcons} from '@expo/vector-icons';
import {Link, router} from 'expo-router';
import type {ComponentProps} from 'react';
import {useMemo, useRef, useState} from 'react';
import {Alert, Image, Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import {Button, Screen, Txt} from '@/components';
import {HttpError} from '@/api';
import {useAuth} from '@/auth';
import {type MndColors, radius, shadow, spacing, typography, useMndTheme} from '@/theme';

const HERO =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuB-FnKTFS5DQ8oAH3iRcvHa5Ngelk3efUNTHDr5w1ek6EXQaeWHZYRoPrpVV_cK59IN2j_Crv2_7xGMyHhy2Vemdjjg3m_hfi1hmlbNFhgaNoiWqBl2VfEtnVpiuyZhOoz84xEbmr88EpQWNf-7ceeFoIReM6BR8MexMqiG7VtCitVfN_rIRpibEykCeGn1d3j8DZdRhdlBJ6YVtdP7Pt8pw2nfPVT6XNCD_E58uuqAhqEvt32TEOHBt0d_rFKIf_LUlnzI9AeFc';

export default function LoginScreen() {
  const { login } = useAuth();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const socialButtons: { name: ComponentProps<typeof MaterialIcons>['name']; label: string; color: string }[] = [
    { name: 'chat-bubble', label: '微信登录', color: colors.success },
    { name: 'phone-iphone', label: '手机号登录', color: colors.onSurface },
    { name: 'mail-outline', label: '邮箱登录', color: colors.tertiary },
  ];
  const passwordRef = useRef<TextInput>(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async () => {
    setError(null);
    if (!username.trim() || !password) {
      setError('请输入账号和密码');
      return;
    }
    setBusy(true);
    try {
      await login(username.trim(), password);
      router.replace('/(tabs)');
    } catch (e) {
      if (e instanceof HttpError) {
        if (e.status === 429) {
          const wait = e.payload.retry_after;
          setError(wait ? `尝试次数过多，请 ${wait}s 后再试` : '尝试次数过多，请稍后再试');
        } else if (e.status === 401) {
          setError('用户名或密码不正确');
        } else {
          setError(e.payload.message || '登录失败');
        }
      } else {
        setError('网络异常，请检查服务是否正在运行');
      }
    } finally {
      setBusy(false);
    }
  };

  const placeholderSocial = () =>
    Alert.alert('提示', '第三方登录暂未开放，当前请使用账号登录', [{ text: '好的' }]);

  return (
    <Screen contentStyle={styles.screenInner}>
      <View style={styles.card}>
        <View style={styles.heroWrap}>
          <Image source={{ uri: HERO }} style={styles.heroImg} resizeMode="cover" accessibilityLabel="M&D 登录封面图" />
          <View style={styles.heroBadge}>
            <Text style={styles.heroBadgeText}>M&D · meow & doggie</Text>
          </View>
        </View>

        <Text style={styles.brand}>M&D</Text>
        <Txt kind="h2" style={styles.center}>
          欢迎回到猫猫宇宙
        </Txt>
        <Txt muted style={[styles.center, styles.tagline]}>
          猫猫是主角，doggie 也有座位。
        </Txt>

        <View style={styles.form}>
          <Text style={styles.fieldLabel}>账号</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="person-outline" size={22} color={colors.onSurfaceSubtle} style={styles.fieldIcon} />
            <TextInput
              value={username}
              onChangeText={setUsername}
              placeholder="用户名 / 手机号 / 邮箱"
              placeholderTextColor={colors.onSurfaceSubtle}
              accessibilityLabel="账号"
              autoCapitalize="none"
              autoCorrect={false}
              returnKeyType="next"
              blurOnSubmit={false}
              onSubmitEditing={() => passwordRef.current?.focus()}
              style={styles.fieldInput}
            />
          </View>

          <View style={styles.passwordHeader}>
            <Text style={styles.fieldLabel}>密码</Text>
            <Pressable
              onPress={() => Alert.alert('提示', '找回密码暂未开放，请先使用账号密码登录', [{ text: '好的' }])}
              hitSlop={8}
              accessibilityRole="button"
              accessibilityLabel="找回密码"
            >
              <Text style={styles.linkText}>忘记密码？</Text>
            </Pressable>
          </View>
          <View style={styles.fieldRow}>
            <MaterialIcons name="lock-outline" size={22} color={colors.onSurfaceSubtle} style={styles.fieldIcon} />
            <TextInput
              ref={passwordRef}
              value={password}
              onChangeText={setPassword}
              placeholder="请输入密码"
              placeholderTextColor={colors.onSurfaceSubtle}
              accessibilityLabel="密码"
              secureTextEntry={!showPassword}
              returnKeyType="done"
              onSubmitEditing={onSubmit}
              style={styles.fieldInput}
            />
            <Pressable
              onPress={() => setShowPassword((v) => !v)}
              style={styles.eyeBtn}
              hitSlop={8}
              accessibilityRole="button"
              accessibilityLabel={showPassword ? '隐藏密码' : '显示密码'}
              accessibilityState={{ selected: showPassword }}
            >
              <MaterialIcons
                name={showPassword ? 'visibility' : 'visibility-off'}
                size={22}
                color={colors.onSurfaceSubtle}
              />
            </Pressable>
          </View>

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <Button title="登录" icon="login" onPress={onSubmit} loading={busy} style={styles.submitBtn} />
        </View>

        <View style={styles.dividerRow}>
          <View style={styles.dividerLine} />
          <Text style={styles.dividerText}>其他登录方式</Text>
          <View style={styles.dividerLine} />
        </View>

        <View style={styles.socialRow}>
          {socialButtons.map(({ name, label, color }) => (
            <Pressable
              key={name}
              style={({ pressed }) => [styles.socialBtn, pressed && styles.socialBtnPressed]}
              onPress={placeholderSocial}
              accessibilityRole="button"
              accessibilityLabel={label}
            >
              <MaterialIcons name={name} size={26} color={color} />
            </Pressable>
          ))}
        </View>

        <View style={styles.footerRow}>
          <Txt muted>还没有账号？</Txt>
          <Link href="/(auth)/register" asChild>
            <Pressable accessibilityRole="link" accessibilityLabel="立即注册">
              <Text style={styles.linkText}>立即注册</Text>
            </Pressable>
          </Link>
        </View>
      </View>
    </Screen>
  );
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
    screenInner: {
      justifyContent: 'center',
      paddingVertical: spacing.lg,
    },
    card: {
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      padding: spacing.lg,
      ...shadow(colors.shadow, 'soft'),
    },
    heroWrap: {
      borderRadius: radius.md,
      overflow: 'hidden',
      marginBottom: spacing.md,
      minHeight: 140,
      ...shadow(colors.shadow, 'card'),
    },
    heroImg: {
      width: '100%',
      height: 140,
    },
    heroBadge: {
      position: 'absolute',
      top: spacing.md,
      left: spacing.md,
      backgroundColor: colors.tabBar,
      paddingHorizontal: spacing.md,
      paddingVertical: spacing.xs,
      borderRadius: radius.pill,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    heroBadgeText: {
      ...typography.label,
      color: colors.primaryContainer,
    },
    brand: {
      ...typography.mega,
      color: colors.primaryContainer,
      textAlign: 'center',
      marginBottom: spacing.xs,
    },
    center: {
      textAlign: 'center',
    },
    tagline: {
      marginBottom: spacing.md,
    },
    form: {
      gap: spacing.sm,
      marginTop: spacing.sm,
    },
    fieldLabel: {
      ...typography.label,
      color: colors.onSurfaceVariant,
      marginLeft: 2,
      marginBottom: 2,
    },
    fieldRow: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: colors.surfaceLow,
      borderRadius: radius.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      minHeight: 52,
      paddingRight: spacing.sm,
    },
    fieldIcon: {
      marginLeft: spacing.sm,
    },
    fieldInput: {
      flex: 1,
      paddingVertical: spacing.md,
      paddingHorizontal: spacing.sm,
      fontSize: 16,
      fontFamily: typography.body.fontFamily,
      color: colors.onSurface,
    },
    passwordHeader: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginTop: spacing.xs,
    },
    linkText: {
      ...typography.bodySmall,
      color: colors.primaryContainer,
      fontWeight: '700',
    },
    eyeBtn: {
      padding: spacing.xs,
    },
    error: {
      ...typography.bodySmall,
      color: colors.error,
      marginTop: spacing.xs,
    },
    submitBtn: {
      marginTop: spacing.sm,
    },
    dividerRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginTop: spacing.xl,
      marginBottom: spacing.md,
      opacity: 0.85,
    },
    dividerLine: {
      flex: 1,
      height: StyleSheet.hairlineWidth,
      backgroundColor: colors.border,
    },
    dividerText: {
      ...typography.label,
      color: colors.onSurfaceVariant,
      paddingHorizontal: spacing.sm,
      fontSize: 11,
      letterSpacing: 0,
    },
    socialRow: {
      flexDirection: 'row',
      justifyContent: 'center',
      gap: spacing.xl,
    },
    socialBtn: {
      width: 54,
      height: 54,
      borderRadius: radius.pill,
      backgroundColor: colors.surfaceLow,
      alignItems: 'center',
      justifyContent: 'center',
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    socialBtnPressed: {
      opacity: 0.88,
      transform: [{ scale: 0.96 }],
    },
    footerRow: {
      flexDirection: 'row',
      justifyContent: 'center',
      alignItems: 'center',
      gap: spacing.xs,
      marginTop: spacing.lg,
    },
  });
}
