import { MaterialIcons } from '@expo/vector-icons';
import { Link, router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
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
import { HttpError, api } from '@/api';
import { useAuth } from '@/auth';
import { colors, elevation, radius, spacing, typography } from '@/theme';

const HERO_REG =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuAjyQIAgLp4bQ6UYqUoKE92s5Wi4oUxuAHSdZd8iCLnn2FNMMZU0v5O_AuPd0rH178eIbGh7HBZ9knXo0jmwp2FhkUUEpEPJRR4iobNbJCosa8zsbiRU3oXAqA5a5iU2hoTao9GDe59NeybJwDXFSs99gFm7xtFm-Ujxsw87svinxotym6dSt-31iUgSpn2xINzTDgDQKSjyItj8Pi0JaVXK1cf0wgUodvggUbXdJ6kVJC4hiD0gmRNzyrxPlTxflPxcFFYHkBP';

const USER_RE = /^[A-Za-z0-9_]{3,}$/;

function digitsOnly(s: string) {
  return s.replace(/\D/g, '');
}

export default function RegisterScreen() {
  const { register } = useAuth();
  const passwordRef = useRef<TextInput>(null);
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [agreed, setAgreed] = useState(false);
  const [codeWait, setCodeWait] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (codeWait <= 0) return;
    const t = setInterval(() => setCodeWait((s) => (s <= 1 ? 0 : s - 1)), 1000);
    return () => clearInterval(t);
  }, [codeWait]);

  const onSendCode = async () => {
    if (codeWait > 0) return;
    const d = digitsOnly(phone);
    if (d.length < 10 || d.length > 15) {
      Alert.alert('提示', '请输入有效的手机号码');
      return;
    }
    try {
      await api.auth.sendVerificationCode({ phone: phone.trim() });
      Alert.alert('提示', '验证码已发送（开发环境可在服务端日志查看）', [{ text: '好的' }]);
      setCodeWait(60);
    } catch (e) {
      const msg = e instanceof HttpError ? e.payload.message : '发送失败';
      Alert.alert('提示', msg, [{ text: '好的' }]);
    }
  };

  const onSubmit = async () => {
    setError(null);
    if (!agreed) {
      setError('请先阅读并同意用户协议与隐私政策');
      return;
    }
    const u = username.trim();
    if (!USER_RE.test(u)) {
      setError('用户名至少 3 位，仅字母、数字、下划线');
      return;
    }
    if (password.length < 6) {
      setError('密码至少 6 位');
      return;
    }
    const phoneDigits = digitsOnly(phone);
    if (phoneDigits.length > 0) {
      if (phoneDigits.length < 10 || phoneDigits.length > 15) {
        setError('请输入有效的手机号码');
        return;
      }
      if (!code.trim()) {
        setError('填写手机号后，请先获取并填写验证码');
        return;
      }
    }
    setBusy(true);
    try {
      await register(u, password, {
        phone: phoneDigits.length > 0 ? phone.trim() : undefined,
        sms_code: phoneDigits.length > 0 ? code.trim() : undefined,
      });
      router.replace('/(tabs)');
    } catch (e) {
      if (e instanceof HttpError) {
        if (e.status === 409) setError(e.payload.message || '用户名或手机号已被占用');
        else setError(e.payload.message || '注册失败');
      } else {
        setError('网络异常，请检查后端是否在运行');
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <Screen contentStyle={styles.screenInner}>
      <View style={styles.glowTop} pointerEvents="none" />
      <View style={styles.card}>
        <View style={styles.heroWrap}>
          <Image source={{ uri: HERO_REG }} style={styles.heroImg} resizeMode="cover" />
        </View>

        <Text style={styles.brandMeow}>MEOW</Text>
        <Txt muted style={styles.tagline}>
          加入喵圈，发现更多可爱日常
        </Txt>

        <View style={styles.form}>
          <Text style={styles.fieldLabel}>用户名</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="person-outline" size={22} color={colors.outline} style={styles.fieldIcon} />
            <TextInput
              value={username}
              onChangeText={setUsername}
              placeholder="给自己起个好听的名字"
              placeholderTextColor={colors.outline}
              autoCapitalize="none"
              autoCorrect={false}
              returnKeyType="next"
              style={styles.fieldInput}
            />
          </View>

          <Text style={styles.fieldLabel}>手机号码</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="smartphone" size={22} color={colors.outline} style={styles.fieldIcon} />
            <TextInput
              value={phone}
              onChangeText={setPhone}
              placeholder="输入您的手机号"
              placeholderTextColor={colors.outline}
              keyboardType="phone-pad"
              style={styles.fieldInput}
            />
          </View>

          <Text style={styles.fieldLabel}>验证码</Text>
          <View style={styles.codeRow}>
            <View style={[styles.fieldRow, styles.codeInputWrap]}>
              <MaterialIcons name="sms" size={22} color={colors.outline} style={styles.fieldIcon} />
              <TextInput
                value={code}
                onChangeText={setCode}
                placeholder="输入验证码"
                placeholderTextColor={colors.outline}
                keyboardType="number-pad"
                style={styles.fieldInput}
              />
            </View>
            <Pressable
              onPress={onSendCode}
              disabled={codeWait > 0}
              style={({ pressed }) => [
                styles.sendCodeBtn,
                codeWait > 0 && styles.sendCodeBtnDisabled,
                pressed && codeWait === 0 && { opacity: 0.88 },
              ]}
            >
              <Text style={styles.sendCodeText}>
                {codeWait > 0 ? `${codeWait}秒后重试` : '获取验证码'}
              </Text>
            </Pressable>
          </View>

          <Text style={styles.fieldLabel}>密码</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="lock-outline" size={22} color={colors.outline} style={styles.fieldIcon} />
            <TextInput
              ref={passwordRef}
              value={password}
              onChangeText={setPassword}
              placeholder="设置登录密码"
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

          <Pressable style={styles.agreeRow} onPress={() => setAgreed((a) => !a)}>
            <MaterialIcons
              name={agreed ? 'check-box' : 'check-box-outline-blank'}
              size={22}
              color={agreed ? colors.primaryContainer : colors.outline}
            />
            <Text style={styles.agreeText}>
              我已阅读并同意 <Text style={styles.agreeLink}>《用户协议》</Text> 与{' '}
              <Text style={styles.agreeLink}>《隐私政策》</Text>
            </Text>
          </Pressable>

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <Button title="立即注册" onPress={onSubmit} loading={busy} style={styles.submitBtn} />
        </View>

        <View style={styles.footerRow}>
          <Txt muted>已经有账号？</Txt>
          <Link href="/(auth)/login" asChild>
            <Pressable>
              <Text style={styles.link}>直接登录</Text>
            </Pressable>
          </Link>
        </View>
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
    top: -72,
    right: -48,
    width: 200,
    height: 200,
    borderRadius: 999,
    backgroundColor: 'rgba(255, 90, 119, 0.14)',
  },
  card: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    padding: spacing.lg,
    ...elevation.soft,
  },
  heroWrap: {
    borderRadius: radius.md,
    overflow: 'hidden',
    marginBottom: spacing.md,
    ...elevation.card,
  },
  heroImg: {
    width: '100%',
    height: 120,
  },
  brandMeow: {
    ...typography.mega,
    color: colors.primaryContainer,
    textAlign: 'center',
  },
  tagline: {
    textAlign: 'center',
    marginBottom: spacing.md,
  },
  form: {
    gap: spacing.xs,
  },
  fieldLabel: {
    ...typography.label,
    color: colors.onSurfaceVariant,
    marginLeft: 2,
    marginTop: spacing.xs,
  },
  fieldRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surfaceContainer,
    borderRadius: radius.lg,
    minHeight: 50,
    paddingRight: spacing.sm,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  fieldIcon: {
    marginLeft: spacing.sm,
  },
  fieldInput: {
    flex: 1,
    paddingVertical: spacing.sm + 2,
    paddingHorizontal: spacing.sm,
    fontSize: 15,
    fontFamily: typography.body.fontFamily,
    color: colors.onSurface,
  },
  codeRow: {
    flexDirection: 'row',
    alignItems: 'stretch',
    gap: spacing.sm,
  },
  codeInputWrap: {
    flex: 1,
    minWidth: 0,
  },
  sendCodeBtn: {
    justifyContent: 'center',
    paddingHorizontal: spacing.md,
    backgroundColor: colors.brandWeak,
    borderRadius: radius.lg,
    maxWidth: 120,
  },
  sendCodeBtnDisabled: {
    opacity: 0.55,
  },
  sendCodeText: {
    ...typography.label,
    color: colors.primaryContainer,
    textAlign: 'center',
    fontSize: 11,
  },
  eyeBtn: {
    padding: spacing.xs,
  },
  agreeRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  agreeText: {
    ...typography.bodySmall,
    color: colors.onSurfaceVariant,
    flex: 1,
    lineHeight: 20,
  },
  agreeLink: {
    color: colors.primaryContainer,
    fontWeight: '700',
  },
  error: {
    ...typography.bodySmall,
    color: colors.error,
    marginTop: spacing.sm,
  },
  submitBtn: {
    marginTop: spacing.md,
    borderRadius: radius.lg,
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
});
