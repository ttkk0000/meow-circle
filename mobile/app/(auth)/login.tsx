import { MaterialIcons } from '@expo/vector-icons';
import { Link, router } from 'expo-router';
import { useRef, useState } from 'react';
import {
  Alert,
  Image,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Button, Screen, Txt } from '@/components';
import { HttpError } from '@/api';
import { useAuth } from '@/auth';
import { colors, elevation, radius, spacing, typography } from '@/theme';

const HERO =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuB-FnKTFS5DQ8oAH3iRcvHa5Ngelk3efUNTHDr5w1ek6EXQaeWHZYRoPrpVV_cK59IN2j_Crv2_7xGMyHhy2Vemdjjg3m_hfi1hmlbNFhgaNoiWqBl2VfEtnVpiuyZhOoz84xEbmr88EpQWNf-7ceeFoIReM6BR8MexMqiG7VtCitVfN_rIRpibEykCeGn1d3j8DZdRhdlBJ6YVtdP7Pt8pw2nfPVT6XNCD_E58uuqAhqEvt32TEOHBt0d_rFKIf_LUlnzI9AeFc';

export default function LoginScreen() {
  const { login } = useAuth();
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
          setError('用户名或密码错误');
        } else {
          setError(e.payload.message || '登录失败');
        }
      } else {
        setError('网络异常，请检查后端是否在运行');
      }
    } finally {
      setBusy(false);
    }
  };

  const placeholderSocial = () =>
    Alert.alert('提示', '第三方登录即将上线', [{ text: '好的' }]);

  return (
    <Screen contentStyle={styles.screenInner}>
      <View style={styles.glowTop} pointerEvents="none" />
      <View style={styles.glowBottom} pointerEvents="none" />

      <View style={styles.card}>
        <View style={styles.heroWrap}>
          <Image source={{ uri: HERO }} style={styles.heroImg} resizeMode="cover" />
          <View style={styles.heroBadge}>
            <Text style={styles.heroBadgeText}>Kitty Circle</Text>
          </View>
        </View>

        <Text style={styles.brandMeow}>MEOW</Text>
        <Txt kind="h2">欢迎回来</Txt>
        <Txt muted style={styles.tagline}>
          登录以继续您的探索之旅
        </Txt>

        <View style={styles.form}>
          <Text style={styles.fieldLabel}>手机号 / 邮箱</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="person-outline" size={22} color={colors.outline} style={styles.fieldIcon} />
            <TextInput
              value={username}
              onChangeText={setUsername}
              placeholder="请输入您的账号"
              placeholderTextColor={colors.outline}
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
              onPress={() => Alert.alert('提示', '找回密码功能即将上线', [{ text: '好的' }])}
              hitSlop={8}
            >
              <Text style={styles.forgot}>忘记密码？</Text>
            </Pressable>
          </View>
          <View style={styles.fieldRow}>
            <MaterialIcons name="lock-outline" size={22} color={colors.outline} style={styles.fieldIcon} />
            <TextInput
              ref={passwordRef}
              value={password}
              onChangeText={setPassword}
              placeholder="请输入密码"
              placeholderTextColor={colors.outline}
              secureTextEntry={!showPassword}
              returnKeyType="done"
              onSubmitEditing={onSubmit}
              style={styles.fieldInput}
            />
            <Pressable onPress={() => setShowPassword((v) => !v)} style={styles.eyeBtn} hitSlop={8}>
              <MaterialIcons
                name={showPassword ? 'visibility' : 'visibility-off'}
                size={22}
                color={colors.outline}
              />
            </Pressable>
          </View>

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <Button title="登录" onPress={onSubmit} loading={busy} style={styles.submitBtn} />
        </View>

        <View style={styles.dividerRow}>
          <View style={styles.dividerLine} />
          <Text style={styles.dividerText}>其他登录方式</Text>
          <View style={styles.dividerLine} />
        </View>

        <View style={styles.socialRow}>
          <Pressable
            style={({ pressed }) => [styles.socialBtn, pressed && styles.socialBtnPressed]}
            onPress={placeholderSocial}
          >
            <MaterialIcons name="chat-bubble" size={28} color="#07C160" />
          </Pressable>
          <Pressable
            style={({ pressed }) => [styles.socialBtn, pressed && styles.socialBtnPressed]}
            onPress={placeholderSocial}
          >
            <MaterialIcons name="phone-iphone" size={28} color={colors.onSurface} />
          </Pressable>
          <Pressable
            style={({ pressed }) => [styles.socialBtn, pressed && styles.socialBtnPressed]}
            onPress={placeholderSocial}
          >
            <MaterialIcons name="mail-outline" size={28} color="#12B7F5" />
          </Pressable>
        </View>

        <View style={styles.footerRow}>
          <Txt muted>还没有账号？</Txt>
          <Link href="/(auth)/register" asChild>
            <Pressable>
              <Text style={styles.link}>立即注册</Text>
            </Pressable>
          </Link>
        </View>

        <Txt kind="bodySmall" muted style={styles.legal}>
          登录即代表您同意 MEOW 的服务条款与隐私政策（演示文案）
        </Txt>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  screenInner: {
    justifyContent: 'center',
    paddingVertical: spacing.lg,
  },
  glowTop: {
    position: 'absolute',
    top: -80,
    left: -60,
    width: 220,
    height: 220,
    borderRadius: 999,
    backgroundColor: 'rgba(255, 90, 119, 0.18)',
  },
  glowBottom: {
    position: 'absolute',
    bottom: -60,
    right: -40,
    width: 180,
    height: 180,
    borderRadius: 999,
    backgroundColor: 'rgba(253, 175, 24, 0.16)',
  },
  card: {
    backgroundColor: 'rgba(255, 255, 255, 0.92)',
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255, 255, 255, 0.6)',
    padding: spacing.lg,
    ...elevation.soft,
  },
  heroWrap: {
    borderRadius: radius.md,
    overflow: 'hidden',
    marginBottom: spacing.md,
    minHeight: 140,
    ...elevation.card,
  },
  heroImg: {
    width: '100%',
    height: 140,
  },
  heroBadge: {
    position: 'absolute',
    top: spacing.md,
    left: spacing.md,
    backgroundColor: 'rgba(255,255,255,0.35)',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255,255,255,0.45)',
  },
  heroBadgeText: {
    ...typography.label,
    color: colors.primaryContainer,
    fontFamily: typography.h2.fontFamily,
  },
  brandMeow: {
    ...typography.mega,
    color: colors.primaryContainer,
    textAlign: 'center',
    marginBottom: spacing.xs,
  },
  tagline: {
    textAlign: 'center',
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
    backgroundColor: colors.surfaceContainer,
    borderRadius: radius.lg,
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
  forgot: {
    ...typography.label,
    color: colors.primaryContainer,
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
    borderRadius: radius.lg,
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
    backgroundColor: colors.surface500,
  },
  dividerText: {
    ...typography.label,
    color: colors.onSurfaceVariant,
    paddingHorizontal: spacing.sm,
    fontSize: 11,
    letterSpacing: 0.5,
  },
  socialRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: spacing.xl,
  },
  socialBtn: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.surfaceContainer,
    alignItems: 'center',
    justifyContent: 'center',
    ...elevation.soft,
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
  link: {
    ...typography.bodySmall,
    color: colors.primaryContainer,
    fontWeight: '700',
  },
  legal: {
    textAlign: 'center',
    marginTop: spacing.md,
    opacity: 0.78,
    fontSize: 11,
    lineHeight: 16,
  },
});
