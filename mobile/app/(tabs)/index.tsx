import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { hrefCompose, hrefPostDetail } from '@/href';
import { api, type PostFeedItem, HttpError } from '@/api';
import { useAuth } from '@/auth';
import { Txt } from '@/components';
import {
  FeedTile,
  MasonryTwoCol,
  StitchFab,
  StitchSearchField,
  StitchTopBar,
} from '@/stitch';
import { colors, radius, spacing } from '@/theme';

const FEED_FILTERS = [
  { key: 'rec' as const, label: '推荐' },
  { key: 'new' as const, label: '最新' },
  { key: 'follow' as const, label: '关注' },
];

export default function HomeScreen() {
  const { user } = useAuth();
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
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('加载失败，请检查后端连接');
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
    if (!items) return [];
    const s = q.trim().toLowerCase();
    if (!s) return items;
    return items.filter(
      (it) =>
        it.post.title.toLowerCase().includes(s) ||
        it.post.content.toLowerCase().includes(s) ||
        it.post.tags?.some((t) => t.toLowerCase().includes(s)),
    );
  }, [items, q]);

  const onFilterPress = (key: 'rec' | 'new' | 'follow') => {
    if (key === 'follow' && !user) {
      Alert.alert('关注', '请先登录后再查看关注动态');
      return;
    }
    setFeedFilter(key);
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
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterRow}
        >
          {FEED_FILTERS.map((f) => {
            const on = feedFilter === f.key;
            return (
              <Pressable
                key={f.key}
                onPress={() => onFilterPress(f.key)}
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
          <StitchSearchField
            placeholder="搜索喵友动态…"
            value={q}
            onChangeText={setQ}
            returnKeyType="search"
          />

          <View style={styles.banner}>
            <Txt kind="h2" style={styles.bannerTitle}>
              春日萌友季
            </Txt>
            <Txt kind="bodySmall" muted>
              晒出毛孩子 · 每一只都值得被看见
            </Txt>
          </View>

          {err ? (
            <View style={styles.card}>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
            </View>
          ) : feedFilter === 'follow' && !user ? (
            <View style={styles.card}>
              <Txt kind="h3">关注</Txt>
              <Txt muted>登录后即可看你关注的铲屎官发布的动态。</Txt>
            </View>
          ) : items === null ? (
            <View style={styles.card}>
              <Txt muted>正在拉取最新动态…</Txt>
            </View>
          ) : filtered.length === 0 ? (
            <View style={styles.card}>
              <Txt kind="h3">还没有动态</Txt>
              <Txt muted>点右下角发布第一条吧</Txt>
            </View>
          ) : (
            <MasonryTwoCol
              data={filtered}
              keyExtractor={(it) => String(it.post.id)}
              renderItem={(it, { column }) => (
                <FeedTile
                  item={it}
                  column={column}
                  onPress={() => router.push(hrefPostDetail(it.post.id))}
                />
              )}
            />
          )}
        </ScrollView>
        <StitchFab
          icon="add"
          accessibilityLabel="发布动态"
          onPress={() => router.push(hrefCompose())}
        />
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
    backgroundColor: colors.brandWeak,
    borderColor: 'rgba(255, 90, 119, 0.35)',
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
    paddingBottom: 120,
    gap: spacing.lg,
  },
  banner: {
    backgroundColor: colors.brandWeak,
    borderRadius: radius.xl,
    padding: spacing.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255, 90, 119, 0.18)',
  },
  bannerTitle: {
    color: colors.primaryContainer,
    marginBottom: spacing.xs,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    padding: spacing.lg,
    gap: spacing.sm,
  },
});
