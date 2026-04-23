import { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { api, type Conversation, HttpError } from '@/api';
import { useAuth } from '@/auth';
import { Button, Card, Input, Txt } from '@/components';
import { StitchTopBar } from '@/stitch';
import { colors, radius, spacing } from '@/theme';

export default function MessagesScreen() {
  const { user } = useAuth();
  const [items, setItems] = useState<Conversation[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [peerInput, setPeerInput] = useState('');
  const [showNew, setShowNew] = useState(false);

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
    setShowNew(false);
    openPeer(id);
  };

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <StitchTopBar
          user={user}
          onAvatarPress={() => router.push('/(tabs)/profile')}
          onNotifyPress={() => Alert.alert('通知', '暂无新通知')}
        />
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <View style={styles.bentoRow}>
            <Pressable style={styles.bento} onPress={() => setShowNew((v) => !v)}>
              <MaterialIcons name="add-comment" size={28} color={colors.primaryContainer} />
              <Txt kind="label" style={styles.bentoLabel}>
                新对话
              </Txt>
            </Pressable>
            <Pressable
              style={styles.bento}
              onPress={() => Alert.alert('通知', '暂无新通知')}
            >
              <MaterialIcons name="notifications-active" size={28} color={colors.secondaryContainer} />
              <Txt kind="label" style={styles.bentoLabel}>
                通知
              </Txt>
            </Pressable>
            <Pressable style={styles.bento} onPress={() => router.push('/(tabs)/market')}>
              <MaterialIcons name="storefront" size={28} color={colors.tertiaryContainer} />
              <Txt kind="label" style={styles.bentoLabel}>
                市集
              </Txt>
            </Pressable>
          </View>

          {showNew ? (
            <Card>
              <Txt kind="h3">开始私聊</Txt>
              <Txt muted style={{ marginBottom: spacing.sm }}>
                输入对方的用户数字 ID
              </Txt>
              <Input
                keyboardType="number-pad"
                placeholder="用户 ID"
                value={peerInput}
                onChangeText={setPeerInput}
              />
              <Button title="开始聊天" variant="secondary" onPress={startFromInput} />
            </Card>
          ) : null}

          <Txt kind="h3" style={{ marginTop: spacing.sm }}>
            私信列表
          </Txt>

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
              <Txt muted>在动态详情里也可以找到私信入口。</Txt>
            </Card>
          ) : (
            items.map((c) => (
              <Pressable key={c.peer.id} onPress={() => openPeer(c.peer.id)}>
                <Card>
                  <View style={styles.rowBetween}>
                    <Txt kind="h3">{c.peer.nickname || c.peer.username}</Txt>
                    {c.unread_count > 0 ? (
                      <View style={styles.badge}>
                        <Txt kind="label" style={{ color: colors.onPrimary }}>
                          {c.unread_count}
                        </Txt>
                      </View>
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
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  safe: {
    flex: 1,
  },
  content: {
    paddingHorizontal: spacing.lg,
    paddingBottom: 120,
    gap: spacing.md,
  },
  bentoRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  bento: {
    flex: 1,
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255, 90, 119, 0.12)',
    paddingVertical: spacing.lg,
    paddingHorizontal: spacing.sm,
    alignItems: 'center',
    gap: spacing.sm,
  },
  bentoLabel: {
    color: colors.onSurfaceVariant,
    textAlign: 'center',
  },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  badge: {
    minWidth: 22,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderRadius: radius.pill,
    backgroundColor: colors.primaryContainer,
    alignItems: 'center',
  },
});
