import { Pressable, StyleSheet, View } from 'react-native';
import type { PostFeedItem } from '@/api';
import { Txt } from '@/components';
import { colors, elevation, radius, spacing } from '@/theme';

function formatCompactCount(n: number): string {
  const x = Math.floor(n) || 0;
  if (x >= 10000) return `${(x / 10000).toFixed(1).replace(/\.0$/, '')}w`;
  if (x >= 1000) return `${(x / 1000).toFixed(1).replace(/\.0$/, '')}k`;
  return String(x);
}

export function feedTileHeight(postId: number, column: 0 | 1): number {
  const h = 118 + (postId % 6) * 16 + (column === 1 ? 28 : 0);
  return Math.min(h, 220);
}

export function categoryLabel(c: string): string {
  if (c === 'daily_share') return '日常';
  if (c === 'help') return '求助';
  if (c === 'activity') return '活动';
  if (c === 'trade') return '交易';
  return c;
}

type TileProps = {
  item: PostFeedItem;
  column: 0 | 1;
  onPress: () => void;
};

export function FeedTile({ item, column, onPress }: TileProps) {
  const post = item.post;
  const author = item.author;
  const h = feedTileHeight(post.id, column);
  const who = author.nickname || author.username || `用户 ${post.author_id}`;
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.tile, { minHeight: h }, pressed && styles.pressed]}
    >
      <View style={styles.accent} />
      <Txt kind="label" style={styles.cat}>
        {categoryLabel(post.category)}
      </Txt>
      <Txt kind="h3" numberOfLines={3}>
        {post.title}
      </Txt>
      {post.tags?.length ? (
        <Txt kind="bodySmall" muted numberOfLines={1}>
          {post.tags.slice(0, 2).map((t) => `#${t}`).join(' ')}
        </Txt>
      ) : null}
      <Txt kind="bodySmall" muted numberOfLines={1} style={styles.meta}>
        {who} · ♥ {formatCompactCount(item.like_count)}
      </Txt>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  tile: {
    backgroundColor: colors.surface,
    borderRadius: radius.xl,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255, 90, 119, 0.14)',
    padding: spacing.md,
    overflow: 'hidden',
    ...elevation.soft,
  },
  pressed: {
    opacity: 0.94,
    transform: [{ scale: 0.99 }],
  },
  accent: {
    position: 'absolute',
    left: 0,
    top: 0,
    bottom: 0,
    width: 4,
    backgroundColor: colors.primaryContainer,
    opacity: 0.85,
    borderTopLeftRadius: radius.xl,
    borderBottomLeftRadius: radius.xl,
  },
  cat: {
    color: colors.primaryContainer,
    marginBottom: spacing.xs,
    marginLeft: spacing.sm,
  },
  meta: {
    marginTop: spacing.xs,
    marginLeft: spacing.sm,
  },
});
