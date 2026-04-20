import { Link, router } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';
import { Button, Card, Input, Screen, Txt } from '@/components';
import { HttpError } from '@/api';
import { useAuth } from '@/auth';
import { colors, spacing } from '@/theme';

export default function RegisterScreen() {
  const { register } = useAuth();
  const [username, setUsername] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async () => {
    setError(null);
    if (!username.trim()) {
      setError('请填写用户名');
      return;
    }
    if (password.length < 8) {
      setError('密码至少 8 位');
      return;
    }
    setBusy(true);
    try {
      await register(username.trim(), password, nickname.trim() || undefined);
      router.replace('/(tabs)');
    } catch (e) {
      if (e instanceof HttpError) {
        if (e.status === 409) setError('用户名已被占用');
        else setError(e.payload.message || '注册失败');
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
        <Txt kind="h1">创建账号</Txt>
        <Txt muted>一起来撸猫吧</Txt>

        <View style={{ gap: spacing.md, marginTop: spacing.lg }}>
          <Input
            label="用户名"
            placeholder="仅英文、数字、下划线"
            autoCapitalize="none"
            autoCorrect={false}
            value={username}
            onChangeText={setUsername}
          />
          <Input
            label="昵称（选填）"
            placeholder="给别人看的名字"
            value={nickname}
            onChangeText={setNickname}
          />
          <Input
            label="密码"
            placeholder="至少 8 位"
            secureTextEntry
            value={password}
            onChangeText={setPassword}
            hint="用 scrypt 加盐哈希保存，我们看不到明文"
            error={error ?? undefined}
          />
          <Button title="注册并登录" onPress={onSubmit} loading={busy} />
        </View>

        <View style={{ flexDirection: 'row', gap: spacing.xs, marginTop: spacing.md }}>
          <Txt muted>已经有账号？</Txt>
          <Link href="/(auth)/login">
            <Txt style={{ color: colors.danger, fontWeight: '600' }}>去登录</Txt>
          </Link>
        </View>
      </Card>
    </Screen>
  );
}
