import {useMemo} from 'react';
import {Image, Pressable, StyleSheet, View} from 'react-native';
import {MaterialIcons} from '@expo/vector-icons';
import {type PostFeedItem, resolveMediaUrl} from '@/api';
import {Txt} from '@/components';
import {type MndColors, radius, shadow, spacing, useMndTheme} from '@/theme';

function formatCompactCount(n: number): string {
  const x = Math.floor(n) || 0;
  if (x >= 10000) return `${(x / 10000).toFixed(1).replace(/\.0$/, '')}w`;
  if (x >= 1000) return `${(x / 1000).toFixed(1).replace(/\.0$/, '')}k`;
  return String(x);
}

export function feedTileHeight(postId: number, column: 0 | 1): number {
  const h = 230 + (postId % 4) * 18 + (column === 1 ? 24 : 0);
  return Math.min(h, 330);
}

export function categoryLabel(c: string): string {
  if (c === 'daily_share') return '猫猫日常';
  if (c === 'help') return '猫猫新手村';
  if (c === 'activity') return '活动';
  if (c === 'trade') return '好物交易';
  return c;
}

type TileProps = {
  item: PostFeedItem;
  column: 0 | 1;
  onPress: () => void;
};

export function FeedTile({ item, column, onPress }: TileProps) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const post = item.post;
  const author = item.author;
  const h = feedTileHeight(post.id, column);
  const who = author.nickname || author.username || `用户 ${post.author_id}`;
  const imageUri = resolveMediaUrl(item.first_media?.url);

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`打开动态：${post.title}`}
      style={({ pressed }) => [styles.tile, { minHeight: h }, pressed && styles.pressed]}
    >
      <View style={styles.cover}>
        {imageUri ? (
          <Image source={{ uri: imageUri }} style={styles.coverImage} resizeMode="cover" accessible={false} />
        ) : (
          <View style={styles.coverFallback}>
            <MaterialIcons name="pets" size={28} color={colors.primaryContainer} />
            <Txt kind="label" style={styles.coverText}>
              M&D 猫猫故事
            </Txt>
          </View>
        )}
      </View>
      <View style={styles.body}>
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
      </View>
    </Pressable>
  );
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
    tile: {
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      overflow: 'hidden',
      ...shadow(colors.shadow, 'soft'),
    },
    pressed: {
      opacity: 0.94,
      transform: [{ scale: 0.99 }],
    },
    cover: {
      height: 122,
      backgroundColor: colors.surfaceLow,
    },
    coverImage: {
      width: '100%',
      height: '100%',
    },
    coverFallback: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      gap: spacing.xs,
      backgroundColor: colors.accentSoft,
    },
    coverText: {
      color: colors.primaryContainer,
      textAlign: 'center',
    },
    body: {
      padding: spacing.md,
      gap: spacing.xs,
    },
    cat: {
      color: colors.primaryContainer,
    },
    meta: {
      marginTop: spacing.xs,
    },
  });
}
