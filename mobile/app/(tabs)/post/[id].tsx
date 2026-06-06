import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, View} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {router, useLocalSearchParams, useRouter} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {api, type Comment, HttpError, type MediaItem, type Post, resolveMediaUrl, type User} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, Input, Pill, Txt} from '@/components';
import {categoryLabel} from '@/stitch';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

function formatCompactCount(n: number): string {
  const x = Math.floor(n) || 0;
  if (x >= 10000) return `${(x / 10000).toFixed(1).replace(/\.0$/, '')}w`;
  if (x >= 1000) return `${(x / 1000).toFixed(1).replace(/\.0$/, '')}k`;
  return String(x);
}

export default function PostDetailScreen() {
  const nav = useRouter();
  const { id: rawId } = useLocalSearchParams<{ id: string }>();
  const postId = parseInt(String(rawId || ''), 10);
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
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
      else setErr('暂时无法加载动态详情');
    }
  }, [postId]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleFollow = async () => {
    if (!user || !author || user.id === author.id) return;
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
      else Alert.alert('错误', '操作失败，请稍后再试');
    }
  };

  const toggleLike = async () => {
    if (!user) {
      Alert.alert('点赞', '请先登录后再点赞');
      return;
    }
    const optimisticLiked = !liked;
    const optimisticCount = Math.max(0, likeCount + (optimisticLiked ? 1 : -1));
    setLiked(optimisticLiked);
    setLikeCount(optimisticCount);
    try {
      const result = await api.posts.toggleLike(postId);
      setLiked(result.liked);
      setLikeCount(result.like_count);
    } catch (e) {
      setLiked(!optimisticLiked);
      setLikeCount(likeCount);
      if (e instanceof HttpError) Alert.alert('错误', e.payload.message);
      else Alert.alert('错误', '点赞失败，请稍后再试');
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
      else setErr('发送失败，请稍后再试');
    } finally {
      setSending(false);
    }
  };

  const leadImages = media
    .map((item) => ({ item, uri: resolveMediaUrl(item.url) }))
    .filter((entry) => entry.uri && entry.item.kind === 'image');

  const goBack = () => {
    if (nav.canGoBack()) router.back();
    else router.replace('/(tabs)');
  };

  if (!Number.isFinite(postId) || postId <= 0) {
    return (
      <View style={styles.centered}>
        <Txt kind="h3">无效链接</Txt>
        <Button title="返回" variant="secondary" onPress={goBack} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={insets.top + 48}>
      <View style={[styles.topBar, { paddingTop: Math.max(insets.top, spacing.md) }]}>
        <Pressable onPress={goBack} style={styles.iconBtn} accessibilityRole="button" accessibilityLabel="返回">
          <MaterialIcons name="arrow-back-ios-new" size={22} color={colors.onSurface} />
        </Pressable>
        <Txt kind="h3">动态详情</Txt>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {err ? (
          <Card style={styles.marginCard}>
            <Txt style={{ color: colors.error }}>{err}</Txt>
            <Button title="重试" variant="secondary" onPress={load} />
          </Card>
        ) : !post ? (
          <Card style={styles.marginCard}>
            <Txt muted>正在加载动态...</Txt>
          </Card>
        ) : (
          <>
            {leadImages.length ? (
              leadImages.map(({ item, uri }) => (
                <Image
                  key={item.id}
                  source={{ uri: uri! }}
                  style={styles.hero}
                  resizeMode="cover"
                  accessibilityLabel={`动态图片：${post.title}`}
                />
              ))
            ) : (
              <View style={styles.placeholderHero}>
                <MaterialIcons name="pets" size={34} color={colors.primaryContainer} />
                <Txt kind="label" style={{ color: colors.primaryContainer }}>
                  M&D 猫猫故事
                </Txt>
              </View>
            )}
            <Card style={styles.marginCard}>
              <Pill tone="brand">{categoryLabel(post.category)}</Pill>
              <Txt kind="h1">{post.title}</Txt>
              <Txt muted>{post.content}</Txt>
              {post.tags?.length ? (
                <Txt kind="bodySmall" muted>
                  {post.tags.map((tag) => `#${tag}`).join(' ')}
                </Txt>
              ) : null}
              {author ? (
                <View style={styles.authorRow}>
                  <View style={{ flex: 1 }}>
                    <Txt kind="bodySmall" muted>
                      {author.nickname || author.username}
                    </Txt>
                    <Txt kind="label" muted>
                      @{author.username}
                    </Txt>
                  </View>
                  {user && user.id !== author.id ? (
                    <>
                      <Pressable
                        onPress={toggleFollow}
                        style={[styles.followPill, followingAuthor && styles.followPillOn]}
                        accessibilityRole="button"
                        accessibilityLabel={followingAuthor ? '取消关注作者' : '关注作者'}
                        accessibilityState={{ selected: followingAuthor }}
                      >
                        <Txt kind="label" style={{ color: followingAuthor ? colors.onSurfaceVariant : colors.primaryContainer }}>
                          {followingAuthor ? '已关注' : '关注'}
                        </Txt>
                      </Pressable>
                      <Pressable
                        onPress={() => router.push(`/(tabs)/messages/${author.id}`)}
                        style={styles.messagePill}
                        accessibilityRole="button"
                        accessibilityLabel="私信作者"
                      >
                        <MaterialIcons name="forum" size={16} color={colors.onPrimary} />
                        <Txt kind="label" style={{ color: colors.onPrimary }}>
                          私信
                        </Txt>
                      </Pressable>
                    </>
                  ) : null}
                </View>
              ) : null}
              <Pressable
                onPress={toggleLike}
                style={styles.likeRow}
                accessibilityRole="button"
                accessibilityLabel="点赞"
                accessibilityState={{ selected: liked }}
              >
                <MaterialIcons name={liked ? 'favorite' : 'favorite-border'} size={26} color={liked ? colors.error : colors.onSurfaceSubtle} />
                <Txt kind="h3" style={{ color: liked ? colors.error : colors.onSurface }}>
                  {formatCompactCount(likeCount)}
                </Txt>
              </Pressable>
            </Card>

            <Txt kind="h3" style={styles.commentHeading}>
              评论 {comments.length}
            </Txt>
            {comments.length === 0 ? (
              <Card style={styles.marginCard}>
                <Txt muted>还没有评论，写下第一条有帮助的回复吧。</Txt>
              </Card>
            ) : (
              comments.map((comment) => (
                <View key={comment.id} style={styles.comment}>
                  <Txt kind="label" muted>
                    用户 {comment.author_id}
                  </Txt>
                  <Txt>{comment.content}</Txt>
                  <Txt kind="bodySmall" muted>
                    {new Date(comment.created_at).toLocaleString()}
                  </Txt>
                </View>
              ))
            )}
          </>
        )}
        <View style={{ height: 120 }} />
      </ScrollView>

      {post && user ? (
        <View style={[styles.composer, { paddingBottom: Math.max(insets.bottom, spacing.md) }]}>
          <Input placeholder="说点什么..." value={commentText} onChangeText={setCommentText} style={styles.composerInput} />
          <Button title="发送" icon="send" loading={sending} onPress={sendComment} style={styles.sendBtn} />
        </View>
      ) : null}
    </KeyboardAvoidingView>
  );
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
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
      borderBottomColor: colors.border,
    },
    iconBtn: {
      minWidth: 44,
      minHeight: 44,
      padding: spacing.sm,
      alignItems: 'center',
      justifyContent: 'center',
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
    placeholderHero: {
      height: 220,
      alignItems: 'center',
      justifyContent: 'center',
      gap: spacing.sm,
      backgroundColor: colors.accentSoft,
    },
    marginCard: {
      marginHorizontal: spacing.lg,
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
      flexWrap: 'wrap',
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
      borderColor: colors.border,
    },
    messagePill: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.xs,
      paddingHorizontal: spacing.md,
      paddingVertical: spacing.xs,
      borderRadius: radius.pill,
      backgroundColor: colors.primaryContainer,
    },
    commentHeading: {
      marginHorizontal: spacing.lg,
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
      borderTopColor: colors.border,
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
}
