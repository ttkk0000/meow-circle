import type {ComponentProps} from 'react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Pressable, RefreshControl, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {router} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {api, type Conversation, HttpError} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, Input, Txt} from '@/components';
import {StitchTopBar} from '@/stitch';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

type IconName = ComponentProps<typeof MaterialIcons>['name'];

const MESSAGE_SECTIONS: { icon: IconName; label: string; body: string }[] = [
  { icon: 'forum', label: '私信', body: '来自动态、市集和伙伴主页的对话。' },
  { icon: 'favorite-border', label: '点赞收藏', body: '点赞、收藏和评论提醒会集中到这里。' },
  { icon: 'person-add-alt', label: '新粉丝', body: '新的关注关系会在这里保持可见。' },
  { icon: 'notifications-none', label: '通知', body: '系统通知和审核提醒。' },
] as const;

export default function MessagesScreen() {
  const { user } = useAuth();
  const { colors, cycleTheme } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
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
      else setErr('暂时无法加载私信列表');
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
          onThemePress={cycleTheme}
        />
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <View style={styles.sectionGrid}>
            {MESSAGE_SECTIONS.map((section, index) => (
              <Pressable
                key={section.label}
                style={styles.sectionCard}
                onPress={() => (index === 0 ? setShowNew((v) => !v) : Alert.alert(section.label, section.body))}
                accessibilityRole="button"
                accessibilityLabel={index === 0 ? '开始新的私信' : `查看${section.label}`}
                accessibilityState={index === 0 ? { selected: showNew } : undefined}
              >
                <MaterialIcons name={section.icon} size={26} color={index === 0 ? colors.primaryContainer : colors.onSurfaceVariant} />
                <Txt kind="label" style={styles.sectionLabel}>
                  {section.label}
                </Txt>
              </Pressable>
            ))}
          </View>

          {showNew ? (
            <Card>
              <Txt kind="h3">开始私聊</Txt>
              <Txt muted style={{ marginBottom: spacing.sm }}>
                输入对方的用户数字 ID，或从动态详情、市集卖家入口继续聊天。
              </Txt>
              <Input keyboardType="number-pad" placeholder="用户 ID" value={peerInput} onChangeText={setPeerInput} />
              <Button title="开始聊天" icon="send" variant="secondary" onPress={startFromInput} />
            </Card>
          ) : null}

          <Txt kind="h3" style={{ marginTop: spacing.sm }}>
            私信列表
          </Txt>

          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
              <Button title="重试" variant="secondary" onPress={load} />
            </Card>
          ) : items === null ? (
            <Card>
              <Txt muted>正在加载对话...</Txt>
            </Card>
          ) : items.length === 0 ? (
            <Card>
              <Txt kind="h3">还没有对话</Txt>
              <Txt muted>可以从动态详情或市集卖家入口发起私信。</Txt>
            </Card>
          ) : (
            items.map((conversation) => {
              const peerName = conversation.peer.nickname || conversation.peer.username;
              return (
                <Pressable
                  key={conversation.peer.id}
                  onPress={() => openPeer(conversation.peer.id)}
                  accessibilityRole="button"
                  accessibilityLabel={`打开与 ${peerName} 的对话${conversation.unread_count > 0 ? `，${conversation.unread_count} 条未读` : ''}`}
                >
                  <Card>
                    <View style={styles.rowBetween}>
                      <Txt kind="h3">{peerName}</Txt>
                      {conversation.unread_count > 0 ? (
                        <View style={styles.badge}>
                          <Txt kind="label" style={{ color: colors.onPrimary }}>
                            {conversation.unread_count}
                          </Txt>
                        </View>
                      ) : null}
                    </View>
                    <Txt muted numberOfLines={1}>
                      {conversation.last_message}
                    </Txt>
                    <Txt kind="bodySmall" muted>
                      {new Date(conversation.updated_at).toLocaleString()}
                    </Txt>
                  </Card>
                </Pressable>
              );
            })
          )}
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
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
    sectionGrid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
    sectionCard: {
      width: '48%',
      flexGrow: 1,
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      paddingVertical: spacing.lg,
      paddingHorizontal: spacing.sm,
      alignItems: 'center',
      gap: spacing.sm,
    },
    sectionLabel: {
      color: colors.onSurfaceVariant,
      textAlign: 'center',
    },
    rowBetween: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: spacing.md,
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
}
