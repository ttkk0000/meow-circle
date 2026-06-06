import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Pressable, RefreshControl, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {router} from 'expo-router';
import {hrefCompose, hrefPostDetail} from '@/href';
import {api, HttpError, type PostFeedItem} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, EmptyState, Txt} from '@/components';
import {FeedTile, MasonryTwoCol, StitchFab, StitchSearchField, StitchTopBar} from '@/stitch';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

const FEED_FILTERS = [
  { key: 'rec' as const, label: '推荐' },
  { key: 'new' as const, label: '最新' },
  { key: 'follow' as const, label: '关注' },
];

const DEMO_FEED: PostFeedItem[] = [
  {
    post: {
      id: 1001,
      author_id: 1,
      title: '今日猫猫编辑台：晒太阳、罐头和新玩具',
      content: '午后的窗台刚好够暖，奶牛猫把新玩具推到毯子边，等铲屎官一起开箱。',
      category: 'daily_share',
      tags: ['M&D', '猫猫日常'],
      created_at: new Date().toISOString(),
    },
    author: {
      id: 1,
      username: 'momo',
      nickname: 'Momo',
      created_at: new Date().toISOString(),
    },
    like_count: 128,
    liked: false,
    first_media: null,
  },
  {
    post: {
      id: 1002,
      author_id: 2,
      title: '新手村提问：幼猫到家第一周怎么安排？',
      content: '把求助内容放进猫猫新手村，方便被看见。',
      category: 'help',
      tags: ['新手', '健康'],
      created_at: new Date().toISOString(),
    },
    author: {
      id: 2,
      username: 'nana',
      nickname: 'Nana',
      created_at: new Date().toISOString(),
    },
    like_count: 46,
    liked: false,
    first_media: null,
  },
];

export default function HomeScreen() {
  const { user } = useAuth();
  const { colors, cycleTheme } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [items, setItems] = useState<PostFeedItem[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [q, setQ] = useState('');
  const [feedFilter, setFeedFilter] = useState<'rec' | 'new' | 'follow'>('rec');

  const load = useCallback(async () => {
    setErr(null);
    if (feedFilter === 'follow' && !user) {
      setItems([]);
      return;
    }
    try {
      setItems(await api.posts.list({ filter: feedFilter }));
    } catch (e) {
      setItems(null);
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法连接服务，可以先查看精选内容。');
    }
  }, [feedFilter, user]);

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

  const filtered = useMemo(() => {
    const source = items ?? [];
    const s = q.trim().toLowerCase();
    if (!s) return source;
    return source.filter(
      (it) =>
        it.post.title.toLowerCase().includes(s) ||
        it.post.content.toLowerCase().includes(s) ||
        it.post.tags?.some((t) => t.toLowerCase().includes(s)),
    );
  }, [items, q]);

  const onFilterPress = (key: 'rec' | 'new' | 'follow') => {
    if (key === 'follow' && !user) {
      Alert.alert('关注', '登录后可以查看关注伙伴发布的动态。');
      return;
    }
    setFeedFilter(key);
  };

  const showDemo = () => {
    setErr(null);
    setItems(DEMO_FEED);
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
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterRow}>
          {FEED_FILTERS.map((f) => {
            const on = feedFilter === f.key;
            return (
              <Pressable
                key={f.key}
                onPress={() => onFilterPress(f.key)}
                accessibilityRole="button"
                accessibilityState={{ selected: on }}
                accessibilityLabel={`切换到${f.label}动态`}
                style={[styles.filterChip, on && styles.filterChipOn]}
              >
                <Txt kind="label" style={on ? styles.filterLabelOn : styles.filterLabel}>
                  {f.label}
                </Txt>
              </Pressable>
            );
          })}
        </ScrollView>
        <ScrollView
          style={styles.scroll}
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <StitchSearchField placeholder="搜索 M&D 猫猫动态" value={q} onChangeText={setQ} returnKeyType="search" />

          <View style={styles.editorial}>
            <Txt kind="label" style={styles.eyebrow}>
              今日猫猫编辑台
            </Txt>
            <Txt kind="h2">M&D 猫猫主角季</Txt>
            <Txt kind="bodySmall" muted>
              猫猫日常先被看见，doggie 伙伴在活动、服务和领养故事里自然出现。
            </Txt>
          </View>

          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
              <View style={styles.actions}>
                <Button title="重试" variant="secondary" onPress={load} />
                <Button title="查看精选" variant="ghost" onPress={showDemo} />
              </View>
            </Card>
          ) : items === null ? (
            <Card>
              <Txt muted>正在拉取最新动态...</Txt>
            </Card>
          ) : filtered.length === 0 ? (
            <EmptyState
              title="还没有动态"
              body="换个筛选或点发布，写下第一条猫猫日常。"
              action={<Button title="去发布" variant="secondary" onPress={() => router.push(hrefCompose())} />}
            />
          ) : (
            <MasonryTwoCol
              data={filtered}
              keyExtractor={(it) => String(it.post.id)}
              renderItem={(it, { column }) => (
                <FeedTile item={it} column={column} onPress={() => router.push(hrefPostDetail(it.post.id))} />
              )}
            />
          )}
        </ScrollView>
        <StitchFab icon="add" accessibilityLabel="发布动态" onPress={() => router.push(hrefCompose())} />
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
    filterRow: {
      paddingHorizontal: spacing.lg,
      paddingBottom: spacing.sm,
      gap: spacing.sm,
      flexDirection: 'row',
      alignItems: 'center',
    },
    filterChip: {
      paddingHorizontal: spacing.lg,
      paddingVertical: spacing.sm,
      borderRadius: radius.pill,
      backgroundColor: colors.surface,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    filterChipOn: {
      backgroundColor: colors.accentSoft,
      borderColor: colors.borderMedium,
    },
    filterLabel: {
      color: colors.onSurfaceVariant,
    },
    filterLabelOn: {
      color: colors.primaryContainer,
    },
    scroll: {
      flex: 1,
    },
    scrollContent: {
      paddingHorizontal: spacing.lg,
      paddingBottom: 132,
      gap: spacing.lg,
    },
    editorial: {
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      padding: spacing.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      gap: spacing.sm,
    },
    eyebrow: {
      color: colors.primaryContainer,
      textTransform: 'none',
      letterSpacing: 0,
    },
    actions: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
  });
}
