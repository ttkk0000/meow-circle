import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { View } from 'react-native';
import { api, BASE_URL } from '@/api';
import { useAuth } from '@/auth';
import { Button, Card, Screen, Txt } from '@/components';
import { spacing } from '@/theme';

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const [health, setHealth] = useState<string>('…');

  useEffect(() => {
    api
      .health()
      .then((h) => setHealth(`${h.status} · ${h.store}`))
      .catch(() => setHealth('unreachable'));
  }, []);

  const onLogout = async () => {
    await logout();
    router.replace('/(auth)/login');
  };

  return (
    <Screen contentStyle={{ gap: spacing.lg }}>
      <Card>
        <Txt kind="h1">{user?.nickname || user?.username || '未登录'}</Txt>
        {user?.bio ? <Txt muted>{user.bio}</Txt> : null}
        <View style={{ flexDirection: 'row', gap: spacing.md, marginTop: spacing.sm }}>
          <Txt kind="bodySmall" muted>
            @{user?.username}
          </Txt>
          {user?.created_at ? (
            <Txt kind="bodySmall" muted>
              · 加入于 {new Date(user.created_at).toLocaleDateString()}
            </Txt>
          ) : null}
        </View>
      </Card>

      <Card>
        <Txt kind="h3">关于后端</Txt>
        <Txt kind="mono" muted>
          {BASE_URL}
        </Txt>
        <Txt kind="bodySmall" muted>
          状态：{health}
        </Txt>
      </Card>

      <Button title="退出登录" variant="danger" onPress={onLogout} />
    </Screen>
  );
}
