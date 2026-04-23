import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { hrefCompose, hrefPostDetail } from '@/href';
import { MaterialIcons } from '@expo/vector-icons';
import { api, type Post, type PostFeedItem, HttpError } from '@/api';
import { useAuth } from '@/auth';
import { Txt } from '@/components';
import { FeedTile, MasonryTwoCol, StitchFab, StitchTopBar } from '@/stitch';
import { colors, radius, spacing } from '@/theme';

const CHIPS: { key: string; label: string; match?: (p: Post) => boolean }[] = [
  { key: 'all', label: '全部' },
  { key: 'daily_share', label: '日常', match: (p) => p.category === 'daily_share' },
  { key: 'help', label: '求助', match: (p) => p.category === 'help' },
  { key: 'activity', label: '活动', match: (p) => p.category === 'activity' },
  { key: 'trade', label: '交易', match: (p) => p.category === 'trade' },
];

export default function DiscoverScreen() {
  const { user } = useAuth();
  const [posts, setPosts] = useState<PostFeedItem[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [chip, setChip] = useState('all');

  const load = useCallback(async () => {
    setErr(null);
    try {
      setPosts(await api.posts.list({ filter: 'rec' }));
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

  const filtered = useMemo(() => {
    if (!posts) return [];
    const def = CHIPS.find((c) => c.key === chip);
    if (!def?.match) return posts;
    return posts.filter((it) => def.match!(it.post));
  }, [posts, chip]);

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
          contentContainerStyle={styles.chipsRow}
        >
          {CHIPS.map((c) => {
            const on = c.key === chip;
            return (
              <Pressable
                key={c.key}
                onPress={() => setChip(c.key)}
                style={[styles.chip, on && styles.chipOn]}
              >
                <Txt kind="label" style={on ? styles.chipLabelOn : styles.chipLabel}>
                  {c.label}
                </Txt>
              </Pressable>
            );
          })}
        </ScrollView>

        <ScrollView
          style={styles.scroll}
          contentContainerStyle={styles.scrollContent}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <Pressable style={styles.marketCard} onPress={() => router.push('/(tabs)/market')}>
            <View style={styles.marketIcon}>
              <MaterialIcons name="storefront" size={28} color={colors.primaryContainer} />
            </View>
            <View style={{ flex: 1 }}>
              <Txt kind="h3">好物市集</Txt>
              <Txt kind="bodySmall" muted>
                用品 · 服务 · 领养信息
              </Txt>
            </View>
            <MaterialIcons name="chevron-right" size={24} color={colors.outline} />
          </Pressable>

          {err ? (
            <View style={styles.card}>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
            </View>
          ) : posts === null ? (
            <View style={styles.card}>
              <Txt muted>加载中…</Txt>
            </View>
          ) : filtered.length === 0 ? (
            <View style={styles.card}>
              <Txt kind="h3">这里还空空的</Txt>
              <Txt muted>换个分类看看</Txt>
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
  chipsRow: {
    paddingHorizontal: spacing.lg,
    paddingBottom: spacing.sm,
    gap: spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
  },
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    backgroundColor: colors.surface,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
  },
  chipOn: {
    backgroundColor: colors.brandWeak,
    borderColor: 'rgba(255, 90, 119, 0.35)',
  },
  chipLabel: {
    color: colors.onSurfaceVariant,
  },
  chipLabelOn: {
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
  marketCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    padding: spacing.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255, 90, 119, 0.14)',
  },
  marketIcon: {
    width: 48,
    height: 48,
    borderRadius: radius.lg,
    backgroundColor: colors.brandWeak,
    alignItems: 'center',
    justifyContent: 'center',
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
