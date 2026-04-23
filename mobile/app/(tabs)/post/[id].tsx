import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { router, useLocalSearchParams } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import {
  api,
  resolveMediaUrl,
  type Comment,
  type MediaItem,
  type Post,
  type User,
  HttpError,
} from '@/api';
import { useAuth } from '@/auth';
import { Button, Input, Txt } from '@/components';
import { categoryLabel } from '@/stitch';
import { colors, radius, spacing } from '@/theme';

function formatCompactCount(n: number): string {
  const x = Math.floor(n) || 0;
  if (x >= 10000) return `${(x / 10000).toFixed(1).replace(/\.0$/, '')}w`;
  if (x >= 1000) return `${(x / 1000).toFixed(1).replace(/\.0$/, '')}k`;
  return String(x);
}

export default function PostDetailScreen() {
  const { id: rawId } = useLocalSearchParams<{ id: string }>();
  const postId = parseInt(String(rawId || ''), 10);
  const insets = useSafeAreaInsets();
  const { user } = useAuth();

  const [post, setPost] = useState<Post | null>(null);
  const [media, setMedia] = useState<MediaItem[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [err, setErr] = useState<string | null>(null);
  const [commentText, setCommentText] = useState('');
  const [sending, setSending] = useState(false);
  const [author, setAuthor] = useState<User | null>(null);
  const [likeCount, setLikeCount] = useState(0);
  const [liked, setLiked] = useState(false);
  const [followingAuthor, setFollowingAuthor] = useState(false);

  const load = useCallback(async () => {
    if (!Number.isFinite(postId) || postId <= 0) {
      setErr('无效的动态');
      return;
    }
    setErr(null);
    try {
      const data = await api.posts.get(postId);
      setPost(data.post);
      setMedia(data.media ?? []);
      setComments(data.comments ?? []);
      setAuthor(data.author ?? null);
      setLikeCount(data.like_count ?? 0);
      setLiked(data.liked ?? false);
      setFollowingAuthor(!!data.following_author);
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('加载失败');
    }
  }, [postId]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleFollow = async () => {
    if (!user || !author) return;
    if (user.id === author.id) return;
    try {
      if (followingAuthor) {
        await api.unfollowUser(author.id);
        setFollowingAuthor(false);
      } else {
        await api.followUser(author.id);
        setFollowingAuthor(true);
      }
    } catch (e) {
      if (e instanceof HttpError) Alert.alert('错误', e.payload.message);
      else Alert.alert('错误', '操作失败');
    }
  };

  const toggleLike = async () => {
    if (!user) {
      Alert.alert('点赞', '请先登录后再点赞');
      return;
    }
    try {
      const r = await api.posts.toggleLike(postId);
      setLiked(r.liked);
      setLikeCount(r.like_count);
    } catch (e) {
      if (e instanceof HttpError) Alert.alert('错误', e.payload.message);
      else Alert.alert('错误', '操作失败');
    }
  };

  const sendComment = async () => {
    const text = commentText.trim();
    if (!text || !user) return;
    setSending(true);
    try {
      await api.posts.addComment(postId, text);
      setCommentText('');
      await load();
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('发送失败');
    } finally {
      setSending(false);
    }
  };

  if (!Number.isFinite(postId) || postId <= 0) {
    return (
      <View style={styles.centered}>
        <Txt kind="h3">无效链接</Txt>
        <Button title="返回" variant="secondary" onPress={() => router.back()} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={insets.top + 48}
    >
      <View style={[styles.topBar, { paddingTop: Math.max(insets.top, spacing.md) }]}>
        <Pressable onPress={() => router.back()} style={styles.iconBtn} accessibilityLabel="返回">
          <MaterialIcons name="arrow-back-ios-new" size={22} color={colors.onSurface} />
        </Pressable>
        <Txt kind="h3">动态</Txt>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {err ? (
          <View style={styles.card}>
            <Txt style={{ color: colors.error }}>{err}</Txt>
            <Button title="重试" variant="secondary" onPress={load} />
          </View>
        ) : !post ? (
          <View style={styles.card}>
            <Txt muted>加载中…</Txt>
          </View>
        ) : (
          <>
            {media.map((m) => {
              const uri = resolveMediaUrl(m.url);
              if (!uri || m.kind !== 'image') return null;
              return (
                <Image key={m.id} source={{ uri }} style={styles.hero} resizeMode="cover" />
              );
            })}
            <View style={styles.card}>
              <Txt kind="label" style={{ color: colors.primaryContainer }}>
                {categoryLabel(post.category)}
              </Txt>
              <Txt kind="h1">{post.title}</Txt>
              <Txt muted>{post.content}</Txt>
              {post.tags?.length ? (
                <Txt kind="bodySmall" muted>
                  {post.tags.map((t) => `#${t}`).join(' ')}
                </Txt>
              ) : null}
              {author ? (
                <View style={styles.authorRow}>
                  <Txt kind="bodySmall" muted style={{ flex: 1 }}>
                    {author.nickname || author.username}
                  </Txt>
                  {user && user.id !== author.id ? (
                    <Pressable
                      onPress={toggleFollow}
                      style={({ pressed }) => [
                        styles.followPill,
                        followingAuthor && styles.followPillOn,
                        pressed && styles.followPillPressed,
                      ]}
                      accessibilityRole="button"
                      accessibilityLabel={followingAuthor ? '取消关注' : '关注'}
                    >
                      <Txt
                        kind="label"
                        style={{
                          color: followingAuthor ? colors.onSurfaceVariant : colors.primaryContainer,
                        }}
                      >
                        {followingAuthor ? '已关注' : '关注'}
                      </Txt>
                    </Pressable>
                  ) : null}
                </View>
              ) : null}
              <Pressable
                onPress={toggleLike}
                style={styles.likeRow}
                accessibilityRole="button"
                accessibilityLabel="点赞"
              >
                <MaterialIcons
                  name={liked ? 'favorite' : 'favorite-border'}
                  size={26}
                  color={liked ? colors.primaryContainer : colors.outline}
                />
                <Txt
                  kind="h3"
                  style={{ color: liked ? colors.primaryContainer : colors.onSurface }}
                >
                  {formatCompactCount(likeCount)}
                </Txt>
              </Pressable>
            </View>

            <Txt kind="h3" style={{ marginHorizontal: spacing.lg }}>
              评论 {comments.length}
            </Txt>
            {comments.map((c) => (
              <View key={c.id} style={styles.comment}>
                <Txt kind="label" muted>
                  用户 {c.author_id}
                </Txt>
                <Txt>{c.content}</Txt>
                <Txt kind="bodySmall" muted>
                  {new Date(c.created_at).toLocaleString()}
                </Txt>
              </View>
            ))}
          </>
        )}
        <View style={{ height: 120 }} />
      </ScrollView>

      {post && user ? (
        <View
          style={[
            styles.composer,
            { paddingBottom: Math.max(insets.bottom, spacing.md) },
          ]}
        >
          <Input
            placeholder="说点什么…"
            value={commentText}
            onChangeText={setCommentText}
            style={styles.composerInput}
          />
          <Button
            title="发送"
            loading={sending}
            onPress={sendComment}
            style={styles.sendBtn}
          />
        </View>
      ) : null}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
    paddingBottom: spacing.sm,
    backgroundColor: colors.canvas,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(255, 90, 119, 0.1)',
  },
  iconBtn: {
    padding: spacing.sm,
  },
  scroll: {
    paddingBottom: spacing.xl,
    gap: spacing.md,
  },
  hero: {
    width: '100%',
    height: 280,
    backgroundColor: colors.surfaceLow,
  },
  card: {
    marginHorizontal: spacing.lg,
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    padding: spacing.lg,
    gap: spacing.sm,
  },
  likeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    alignSelf: 'flex-start',
    marginTop: spacing.xs,
  },
  authorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginTop: spacing.xs,
  },
  followPill: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.primaryContainer,
  },
  followPillOn: {
    backgroundColor: colors.surfaceLow,
    borderColor: colors.outlineVariant,
  },
  followPillPressed: {
    opacity: 0.88,
  },
  comment: {
    marginHorizontal: spacing.lg,
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    gap: spacing.xs,
  },
  composer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing.sm,
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    backgroundColor: colors.surface,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: 'rgba(255, 90, 119, 0.12)',
  },
  composerInput: {
    flex: 1,
  },
  sendBtn: {
    marginBottom: 4,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.canvas,
    gap: spacing.md,
    padding: spacing.lg,
  },
});
