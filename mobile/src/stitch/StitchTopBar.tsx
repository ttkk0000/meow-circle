import {useMemo} from 'react';
import {Image, Pressable, StyleSheet, Text, View} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {resolveMediaUrl, type User} from '@/api';
import {IconButton} from '@/components';
import {type MndColors, radius, spacing, typography, useMndTheme} from '@/theme';

type Props = {
  user: User | null;
  title?: string;
  subtitle?: string;
  onAvatarPress?: () => void;
  onNotifyPress?: () => void;
  onThemePress?: () => void;
};

export function StitchTopBar({
  user,
  title = 'M&D',
  subtitle = 'meow & doggie',
  onAvatarPress,
  onNotifyPress,
  onThemePress,
}: Props) {
  const insets = useSafeAreaInsets();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const avatarUri = resolveMediaUrl(user?.avatar_url);

  return (
    <View style={[styles.wrap, { paddingTop: Math.max(insets.top, spacing.md) }]}>
      <Pressable
        onPress={onAvatarPress}
        style={styles.avatarBtn}
        accessibilityRole="button"
        accessibilityLabel="个人主页"
      >
        {avatarUri ? (
          <Image source={{ uri: avatarUri }} style={styles.avatarImg} accessible={false} />
        ) : (
          <View style={[styles.avatarImg, styles.avatarFallback]}>
            <Text style={styles.avatarGlyph}>M</Text>
          </View>
        )}
      </Pressable>
      <View style={styles.lockup}>
        <Text style={styles.wordmark}>{title}</Text>
        <Text style={styles.sub}>{subtitle}</Text>
      </View>
      <View style={styles.actions}>
        {onThemePress ? <IconButton icon="palette" label="切换主题" onPress={onThemePress} /> : null}
        <IconButton icon="notifications-none" label="通知" onPress={onNotifyPress} tone="brand" />
      </View>
    </View>
  );
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
    wrap: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: spacing.lg,
      paddingBottom: spacing.md,
      backgroundColor: colors.canvas,
    },
    avatarBtn: {
      width: 44,
      height: 44,
      borderRadius: radius.pill,
      alignItems: 'center',
      justifyContent: 'center',
    },
    avatarImg: {
      width: 40,
      height: 40,
      borderRadius: radius.pill,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.borderMedium,
    },
    avatarFallback: {
      backgroundColor: colors.primaryContainer,
      alignItems: 'center',
      justifyContent: 'center',
    },
    avatarGlyph: {
      ...typography.label,
      fontSize: 16,
      color: colors.onPrimary,
    },
    lockup: {
      alignItems: 'center',
      gap: 1,
      flex: 1,
    },
    wordmark: {
      ...typography.h1,
      fontSize: 20,
      letterSpacing: 0,
      color: colors.primaryContainer,
    },
    sub: {
      ...typography.label,
      color: colors.onSurfaceVariant,
    },
    actions: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.xs,
    },
  });
}
