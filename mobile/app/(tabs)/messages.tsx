import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, View } from 'react-native';
import { api, type Conversation, HttpError } from '@/api';
import { Card, Pill, Screen, Txt } from '@/components';
import { colors, spacing } from '@/theme';

export default function MessagesScreen() {
  const [items, setItems] = useState<Conversation[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

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

  return (
    <Screen
      scroll
      contentStyle={{ gap: spacing.md }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <View style={{ gap: spacing.xs }}>
        <Txt kind="h1">私信</Txt>
        <Txt muted>和其他猫奴直接聊聊。</Txt>
      </View>

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
          <Txt muted>去社区里找感兴趣的帖子，点作者头像聊聊吧。</Txt>
        </Card>
      ) : (
        items.map((c) => (
          <Card key={c.peer.id}>
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
        ))
      )}
    </Screen>
  );
}
