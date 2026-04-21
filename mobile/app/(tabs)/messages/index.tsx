import { useCallback, useEffect, useState } from 'react';
import { Pressable, RefreshControl, View } from 'react-native';
import { router } from 'expo-router';
import { api, type Conversation, HttpError } from '@/api';
import { Button, Card, Input, Pill, Screen, Txt } from '@/components';
import { colors, spacing } from '@/theme';

export default function MessagesScreen() {
  const [items, setItems] = useState<Conversation[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [peerInput, setPeerInput] = useState('');

  const load = useCallback(async () => {
    setErr(null);
    try {
      setItems(await api.messages.conversations());
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('加载失败');
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await load();
    } finally {
      setRefreshing(false);
    }
  }, [load]);

  const openPeer = (id: number) => {
    if (id > 0) router.push(`/(tabs)/messages/${id}`);
  };

  const startFromInput = () => {
    const id = parseInt(peerInput.trim(), 10);
    if (!Number.isFinite(id) || id <= 0) return;
    setPeerInput('');
    openPeer(id);
  };

  return (
    <Screen
      scroll
      contentStyle={{ gap: spacing.md }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <View style={{ gap: spacing.xs }}>
        <Txt kind="h1">私信</Txt>
        <Txt muted>与 iOS / Android / Web 共用同一套后端会话。</Txt>
      </View>

      <Card>
        <Txt kind="h3">新对话</Txt>
        <Txt muted style={{ marginBottom: spacing.sm }}>
          输入对方的用户数字 ID（与 Web 个人中心一致）。
        </Txt>
        <Input
          keyboardType="number-pad"
          placeholder="用户 ID"
          value={peerInput}
          onChangeText={setPeerInput}
        />
        <Button title="开始聊天" variant="secondary" onPress={startFromInput} />
      </Card>

      {err ? (
        <Card>
          <Txt kind="h3">加载失败</Txt>
          <Txt muted>{err}</Txt>
        </Card>
      ) : items === null ? (
        <Card>
          <Txt muted>正在加载对话…</Txt>
        </Card>
      ) : items.length === 0 ? (
        <Card>
          <Txt kind="h3">还没有对话</Txt>
          <Txt muted>在社区或市场里点「私信」即可开聊。</Txt>
        </Card>
      ) : (
        items.map((c) => (
          <Pressable key={c.peer.id} onPress={() => openPeer(c.peer.id)}>
            <Card>
              <View
                style={{
                  flexDirection: 'row',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <Txt kind="h3">{c.peer.nickname || c.peer.username}</Txt>
                {c.unread_count > 0 ? (
                  <Pill>
                    <Txt kind="label" style={{ color: colors.danger }}>
                      {c.unread_count}
                    </Txt>
                  </Pill>
                ) : null}
              </View>
              <Txt muted numberOfLines={1}>
                {c.last_message}
              </Txt>
              <Txt kind="bodySmall" muted>
                {new Date(c.updated_at).toLocaleString()}
              </Txt>
            </Card>
          </Pressable>
        ))
      )}
    </Screen>
  );
}
