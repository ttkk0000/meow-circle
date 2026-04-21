import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, View } from 'react-native';
import { router } from 'expo-router';
import { api, type Post, HttpError } from '@/api';
import { useAuth } from '@/auth';
import { Button, Card, Pill, Screen, Txt } from '@/components';
import { spacing } from '@/theme';

export default function FeedScreen() {
  const [posts, setPosts] = useState<Post[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setErr(null);
    try {
      const data = await api.posts.list();
      setPosts(data);
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('加载失败，请检查后端连接');
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
      contentStyle={{ gap: spacing.md }}
      scroll
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <View style={{ gap: spacing.xs }}>
        <Txt kind="h1">社区动态</Txt>
        <Txt muted>每一只猫都值得被看见。</Txt>
      </View>

      {err ? (
        <Card>
          <Txt kind="h3">加载失败</Txt>
          <Txt muted>{err}</Txt>
        </Card>
      ) : posts === null ? (
        <Card>
          <Txt muted>正在拉取最新动态…</Txt>
        </Card>
      ) : posts.length === 0 ? (
        <Card>
          <Txt kind="h3">还没有动态</Txt>
          <Txt muted>当第一个分享撸猫日常的人吧 🐾</Txt>
        </Card>
      ) : (
        posts.map((p) => <PostCard key={p.id} post={p} />)
      )}
    </Screen>
  );
}

function PostCard({ post }: { post: Post }) {
  const { user } = useAuth();
  const canDm = user && post.author_id !== user.id;

  return (
    <Card>
      <View style={{ flexDirection: 'row', gap: spacing.sm }}>
        <Pill>{post.category === 'help' ? '求助' : '日常'}</Pill>
        {post.tags?.slice(0, 2).map((t) => (
          <Pill key={t}>#{t}</Pill>
        ))}
      </View>
      <Txt kind="h2">{post.title}</Txt>
      {post.content ? (
        <Txt numberOfLines={4} muted>
          {post.content}
        </Txt>
      ) : null}
      <Txt kind="bodySmall" muted>
        {new Date(post.created_at).toLocaleString()}
      </Txt>
      {canDm ? (
        <Button
          title="私信作者"
          variant="secondary"
          onPress={() => router.push(`/(tabs)/messages/${post.author_id}`)}
        />
      ) : null}
    </Card>
  );
}
