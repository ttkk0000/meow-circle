import { Link, router } from 'expo-router';
import { useState } from 'react';
import { Image, View } from 'react-native';
import { Button, Card, Input, Screen, Txt } from '@/components';
import { HttpError } from '@/api';
import { useAuth } from '@/auth';
import { colors, elevation, radius, spacing } from '@/theme';

export default function LoginScreen() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async () => {
    setError(null);
    if (!username.trim() || !password) {
      setError('请输入用户名和密码');
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

  return (
    <Screen contentStyle={{ justifyContent: 'center' }}>
      <Card>
        <View
          style={{
            position: 'relative',
            marginBottom: spacing.sm,
          }}
        >
          <Image
            source={{
              uri: 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=1200&q=80',
            }}
            style={{
              width: '100%',
              height: 160,
              borderRadius: radius.md,
              ...elevation.card,
            }}
            resizeMode="cover"
          />
          <Image
            source={{
              uri: 'https://images.unsplash.com/photo-1517849845537-4d257902454a?auto=format&fit=crop&w=400&q=80',
            }}
            style={{
              position: 'absolute',
              right: spacing.sm,
              bottom: spacing.sm,
              width: 56,
              height: 56,
              borderRadius: radius.pill,
              borderWidth: 2,
              borderColor: colors.surface100,
            }}
            resizeMode="cover"
          />
        </View>
        <Txt kind="h1">欢迎回来 🐾</Txt>
        <Txt muted>猫咪是主角，汪星人也来打个招呼</Txt>

        <View style={{ gap: spacing.md, marginTop: spacing.lg }}>
          <Input
            label="用户名"
            placeholder="your-name"
            autoCapitalize="none"
            autoCorrect={false}
            value={username}
            onChangeText={setUsername}
          />
          <Input
            label="密码"
            placeholder="至少 8 位"
            secureTextEntry
            value={password}
            onChangeText={setPassword}
            error={error ?? undefined}
          />
          <Button title="登录" onPress={onSubmit} loading={busy} />
        </View>

        <View style={{ flexDirection: 'row', gap: spacing.xs, marginTop: spacing.md }}>
          <Txt muted>还没账号？</Txt>
          <Link href="/(auth)/register">
            <Txt style={{ color: colors.danger, fontWeight: '600' }}>去注册</Txt>
          </Link>
        </View>
      </Card>
    </Screen>
  );
}
