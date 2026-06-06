import {MaterialIcons} from '@expo/vector-icons';
import {Link, router} from 'expo-router';
import {useEffect, useMemo, useState} from 'react';
import {Alert, Image, Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import {Button, Screen, Txt} from '@/components';
import {api, HttpError} from '@/api';
import {useAuth} from '@/auth';
import {type MndColors, radius, shadow, spacing, typography, useMndTheme} from '@/theme';

const HERO =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuAjyQIAgLp4bQ6UYqUoKE92s5Wi4oUxuAHSdZd8iCLnn2FNMMZU0v5O_AuPd0rH178eIbGh7HBZ9knXo0jmwp2FhkUUEpEPJRR4iobNbJCosa8zsbiRU3oXAqA5a5iU2hoTao9GDe59NeybJwDXFSs99gFm7xtFm-Ujxsw87svinxotym6dSt-31iUgSpn2xINzTDgDQKSjyItj8Pi0JaVXK1cf0wgUodvggUbXdJ6kVJC4hiD0gmRNzyrxPlTxflPxcFFYHkBP';

const USER_RE = /^[A-Za-z0-9_]{3,}$/;

function digitsOnly(s: string) {
  return s.replace(/\D/g, '');
}

export default function RegisterScreen() {
  const { register } = useAuth();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
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
    const timer = setInterval(() => setCodeWait((s) => (s <= 1 ? 0 : s - 1)), 1000);
    return () => clearInterval(timer);
  }, [codeWait]);

  const onSendCode = async () => {
    if (codeWait > 0) return;
    const digits = digitsOnly(phone);
    if (digits.length < 10 || digits.length > 15) {
      Alert.alert('提示', '请输入有效的手机号');
      return;
    }
    try {
      await api.auth.sendVerificationCode({ phone: phone.trim() });
      Alert.alert('提示', '验证码已发送，开发环境可在服务端日志查看', [{ text: '好的' }]);
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
      setError('用户名至少 3 位，仅支持字母、数字、下划线');
      return;
    }
    if (password.length < 6) {
      setError('密码至少 6 位');
      return;
    }
    const phoneDigits = digitsOnly(phone);
    if (phoneDigits.length > 0) {
      if (phoneDigits.length < 10 || phoneDigits.length > 15) {
        setError('请输入有效的手机号');
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
        setError('网络异常，请检查服务是否正在运行');
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <Screen contentStyle={styles.screenInner}>
      <View style={styles.card}>
        <View style={styles.heroWrap}>
          <Image source={{ uri: HERO }} style={styles.heroImg} resizeMode="cover" accessibilityLabel="M&D 注册封面图" />
          <View style={styles.heroBadge}>
            <Text style={styles.heroBadgeText}>M&D · meow & doggie</Text>
          </View>
        </View>

        <Text style={styles.brand}>M&D</Text>
        <Txt muted style={styles.tagline}>
          加入 M&D，先看见猫猫日常，再遇见 doggie 伙伴。
        </Txt>

        <View style={styles.form}>
          <Text style={styles.fieldLabel}>用户名</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="person-outline" size={22} color={colors.onSurfaceSubtle} style={styles.fieldIcon} />
            <TextInput
              value={username}
              onChangeText={setUsername}
              placeholder="给自己起个好听的名字"
              placeholderTextColor={colors.onSurfaceSubtle}
              accessibilityLabel="用户名"
              autoCapitalize="none"
              autoCorrect={false}
              returnKeyType="next"
              style={styles.fieldInput}
            />
          </View>

          <Text style={styles.fieldLabel}>手机号</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="smartphone" size={22} color={colors.onSurfaceSubtle} style={styles.fieldIcon} />
            <TextInput
              value={phone}
              onChangeText={setPhone}
              placeholder="可选，用于验证码注册"
              placeholderTextColor={colors.onSurfaceSubtle}
              accessibilityLabel="手机号"
              keyboardType="phone-pad"
              style={styles.fieldInput}
            />
          </View>

          <Text style={styles.fieldLabel}>验证码</Text>
          <View style={styles.codeRow}>
            <View style={[styles.fieldRow, styles.codeInputWrap]}>
              <MaterialIcons name="sms" size={22} color={colors.onSurfaceSubtle} style={styles.fieldIcon} />
              <TextInput
                value={code}
                onChangeText={setCode}
                placeholder="输入验证码"
                placeholderTextColor={colors.onSurfaceSubtle}
                accessibilityLabel="验证码"
                keyboardType="number-pad"
                style={styles.fieldInput}
              />
            </View>
            <Pressable
              onPress={onSendCode}
              disabled={codeWait > 0}
              accessibilityRole="button"
              accessibilityLabel={codeWait > 0 ? `${codeWait} 秒后可重新获取验证码` : '获取验证码'}
              accessibilityState={{ disabled: codeWait > 0 }}
              style={({ pressed }) => [
                styles.sendCodeBtn,
                codeWait > 0 && styles.sendCodeBtnDisabled,
                pressed && codeWait === 0 && { opacity: 0.88 },
              ]}
            >
              <Text style={styles.sendCodeText}>{codeWait > 0 ? `${codeWait}s` : '获取验证码'}</Text>
            </Pressable>
          </View>

          <Text style={styles.fieldLabel}>密码</Text>
          <View style={styles.fieldRow}>
            <MaterialIcons name="lock-outline" size={22} color={colors.onSurfaceSubtle} style={styles.fieldIcon} />
            <TextInput
              value={password}
              onChangeText={setPassword}
              placeholder="设置登录密码"
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

          <Pressable
            style={styles.agreeRow}
            onPress={() => setAgreed((a) => !a)}
            accessibilityRole="checkbox"
            accessibilityLabel="同意用户协议与隐私政策"
            accessibilityState={{ checked: agreed }}
          >
            <MaterialIcons
              name={agreed ? 'check-box' : 'check-box-outline-blank'}
              size={22}
              color={agreed ? colors.primaryContainer : colors.onSurfaceSubtle}
            />
            <Text style={styles.agreeText}>
              我已阅读并同意<Text style={styles.agreeLink}>《用户协议》</Text>与
              <Text style={styles.agreeLink}>《隐私政策》</Text>
            </Text>
          </Pressable>

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <Button title="立即注册" icon="person-add" onPress={onSubmit} loading={busy} />
        </View>

        <View style={styles.footerRow}>
          <Txt muted>已经有账号？</Txt>
          <Link href="/(auth)/login" asChild>
            <Pressable accessibilityRole="link" accessibilityLabel="直接登录">
              <Text style={styles.linkText}>直接登录</Text>
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
      ...shadow(colors.shadow, 'card'),
    },
    heroImg: {
      width: '100%',
      height: 120,
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
      backgroundColor: colors.surfaceLow,
      borderRadius: radius.lg,
      minHeight: 50,
      paddingRight: spacing.sm,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
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
      backgroundColor: colors.accentSoft,
      borderRadius: radius.lg,
      maxWidth: 112,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
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
    footerRow: {
      flexDirection: 'row',
      justifyContent: 'center',
      alignItems: 'center',
      gap: spacing.xs,
      marginTop: spacing.lg,
    },
    linkText: {
      ...typography.bodySmall,
      color: colors.primaryContainer,
      fontWeight: '700',
    },
  });
}
