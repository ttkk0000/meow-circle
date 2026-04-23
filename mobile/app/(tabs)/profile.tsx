import { useCallback, useEffect, useState } from 'react';
import { Alert, Image, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { hrefCompose, hrefPostDetail } from '@/href';
import { api, resolveMediaUrl, type Post, HttpError } from '@/api';
import { useAuth } from '@/auth';
import { Button, Txt } from '@/components';
import { StitchTopBar } from '@/stitch';
import { colors, radius, spacing } from '@/theme';

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const [posts, setPosts] = useState<Post[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setErr(null);
    try {
      setPosts(await api.posts.my());
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

  const onLogout = async () => {
    await logout();
    router.replace('/(auth)/login');
  };

  const avatarUri = resolveMediaUrl(user?.avatar_url);

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <StitchTopBar
          user={user}
          onAvatarPress={() => Alert.alert('我', '已经在个人主页啦')}
          onNotifyPress={() => Alert.alert('通知', '暂无新通知')}
        />
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <View style={styles.header}>
            {avatarUri ? (
              <Image source={{ uri: avatarUri }} style={styles.avatar} />
            ) : (
              <View style={[styles.avatar, styles.avatarPh]}>
                <Txt kind="mega">🐱</Txt>
              </View>
            )}
            <View style={{ flex: 1 }}>
              <Txt kind="h1">{user?.nickname || user?.username || '未登录'}</Txt>
              <Txt kind="bodySmall" muted>
                @{user?.username}
              </Txt>
              {user?.bio ? (
                <Txt muted style={{ marginTop: spacing.sm }}>
                  {user.bio}
                </Txt>
              ) : null}
            </View>
          </View>

          <View style={styles.stats}>
            <View style={styles.stat}>
              <Txt kind="h2">{posts?.length ?? '—'}</Txt>
              <Txt kind="label" muted>
                动态
              </Txt>
            </View>
            <View style={styles.stat}>
              <Txt kind="h2">0</Txt>
              <Txt kind="label" muted>
                关注
              </Txt>
            </View>
            <View style={styles.stat}>
              <Txt kind="h2">0</Txt>
              <Txt kind="label" muted>
                粉丝
              </Txt>
            </View>
          </View>

          <Txt kind="h3" style={{ marginTop: spacing.md }}>
            我的动态
          </Txt>

          {err ? (
            <View style={styles.card}>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
            </View>
          ) : posts === null ? (
            <View style={styles.card}>
              <Txt muted>加载中…</Txt>
            </View>
          ) : posts.length === 0 ? (
            <View style={styles.card}>
              <Txt muted>还没有发布过动态</Txt>
              <Button
                title="去发布"
                variant="secondary"
                onPress={() => router.push(hrefCompose())}
              />
            </View>
          ) : (
            <View style={styles.grid}>
              {posts.map((p) => (
                <Pressable
                  key={p.id}
                  style={styles.gridItem}
                  onPress={() => router.push(hrefPostDetail(p.id))}
                >
                  <Txt kind="label" numberOfLines={2}>
                    {p.title}
                  </Txt>
                  <Txt kind="bodySmall" muted numberOfLines={2} style={{ marginTop: spacing.xs }}>
                    {p.content}
                  </Txt>
                </Pressable>
              ))}
            </View>
          )}

          <Button title="退出登录" variant="danger" onPress={onLogout} />
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
  header: {
    flexDirection: 'row',
    gap: spacing.lg,
    alignItems: 'flex-start',
  },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255, 90, 119, 0.35)',
  },
  avatarPh: {
    backgroundColor: colors.surfaceLow,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stats: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    paddingVertical: spacing.lg,
  },
  stat: {
    flex: 1,
    alignItems: 'center',
    gap: spacing.xs,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  gridItem: {
    width: '48%',
    flexGrow: 1,
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255, 90, 119, 0.12)',
    padding: spacing.md,
    minHeight: 100,
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
