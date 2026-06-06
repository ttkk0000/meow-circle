import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Pressable, RefreshControl, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {router} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {hrefCompose, hrefPostDetail} from '@/href';
import {api, HttpError, type Post, type PostFeedItem} from '@/api';
import {useAuth} from '@/auth';
import {Card, Txt} from '@/components';
import {FeedTile, MasonryTwoCol, StitchFab, StitchSearchField, StitchTopBar} from '@/stitch';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

const CIRCLES: { key: string; label: string; note: string; match?: (p: Post) => boolean }[] = [
  { key: 'all', label: '全部圈子', note: '浏览所有猫猫与 doggie 友好内容' },
  { key: 'daily_share', label: '猫猫日常', note: '晒猫、陪伴、生活记录', match: (p) => p.category === 'daily_share' },
  { key: 'help', label: '猫猫新手村', note: '新手提问、健康和照护经验', match: (p) => p.category === 'help' },
  { key: 'activity', label: '活动', note: '线下活动、领养和 doggie 友好服务', match: (p) => p.category === 'activity' },
  { key: 'trade', label: '好物交易', note: '猫猫优先的用品、服务和闲置', match: (p) => p.category === 'trade' },
];

export default function DiscoverScreen() {
  const { user } = useAuth();
  const { colors, cycleTheme } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [posts, setPosts] = useState<PostFeedItem[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [circle, setCircle] = useState('all');
  const [q, setQ] = useState('');

  const load = useCallback(async () => {
    setErr(null);
    try {
      setPosts(await api.posts.list({ filter: 'rec' }));
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法加载发现内容');
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

  const selected = CIRCLES.find((c) => c.key === circle) ?? CIRCLES[0];
  const filtered = useMemo(() => {
    if (!posts) return [];
    const s = q.trim().toLowerCase();
    return posts.filter((it) => {
      const circleOk = selected.match ? selected.match(it.post) : true;
      const searchOk =
        !s ||
        it.post.title.toLowerCase().includes(s) ||
        it.post.content.toLowerCase().includes(s) ||
        it.post.tags?.some((t) => t.toLowerCase().includes(s));
      return circleOk && searchOk;
    });
  }, [posts, q, selected]);

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <StitchTopBar
          user={user}
          onAvatarPress={() => router.push('/(tabs)/profile')}
          onNotifyPress={() => Alert.alert('通知', '暂无新通知')}
          onThemePress={cycleTheme}
        />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipsRow}>
          {CIRCLES.map((c) => {
            const on = c.key === circle;
            return (
              <Pressable
                key={c.key}
                onPress={() => setCircle(c.key)}
                accessibilityRole="button"
                accessibilityState={{ selected: on }}
                accessibilityLabel={`切换到${c.label}`}
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
          <StitchSearchField placeholder="搜索圈子、活动、好物" value={q} onChangeText={setQ} />

          <View style={styles.topic}>
            <Txt kind="label" style={styles.eyebrow}>
              本周主题
            </Txt>
            <Txt kind="h2">{selected.label}</Txt>
            <Txt muted>{selected.note}</Txt>
          </View>

          <Pressable
            style={styles.marketCard}
            onPress={() => router.push('/(tabs)/market')}
            accessibilityRole="button"
            accessibilityLabel="打开好物市集"
          >
            <View style={styles.marketIcon}>
              <MaterialIcons name="storefront" size={28} color={colors.primaryContainer} />
            </View>
            <View style={{ flex: 1 }}>
              <Txt kind="h3">好物市集</Txt>
              <Txt kind="bodySmall" muted>
                猫猫优先的用品、服务、领养信息，doggie 友好内容在这里分支。
              </Txt>
            </View>
            <MaterialIcons name="chevron-right" size={24} color={colors.onSurfaceSubtle} />
          </Pressable>

          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
            </Card>
          ) : posts === null ? (
            <Card>
              <Txt muted>正在加载发现内容...</Txt>
            </Card>
          ) : filtered.length === 0 ? (
            <Card>
              <Txt kind="h3">这里暂时空空的</Txt>
              <Txt muted>{q.trim() ? '换个关键词试试。' : '换个圈子看看，或发布一条新动态。'}</Txt>
            </Card>
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
      backgroundColor: colors.accentSoft,
      borderColor: colors.borderMedium,
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
      paddingBottom: 132,
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
      borderColor: colors.border,
    },
    topic: {
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      padding: spacing.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      gap: spacing.sm,
    },
    eyebrow: {
      color: colors.primaryContainer,
    },
    marketIcon: {
      width: 48,
      height: 48,
      borderRadius: radius.lg,
      backgroundColor: colors.accentSoft,
      alignItems: 'center',
      justifyContent: 'center',
    },
  });
}
