import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { resolveMediaUrl, type User } from '@/api';
import { colors, radius, spacing, typography } from '@/theme';

type Props = {
  user: User | null;
  onAvatarPress?: () => void;
  onNotifyPress?: () => void;
};

export function StitchTopBar({ user, onAvatarPress, onNotifyPress }: Props) {
  const insets = useSafeAreaInsets();
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
          <Image source={{ uri: avatarUri }} style={styles.avatarImg} />
        ) : (
          <View style={[styles.avatarImg, styles.avatarFallback]}>
            <Text style={styles.avatarGlyph}>M</Text>
          </View>
        )}
      </Pressable>
      <View style={styles.lockup}>
        <Text style={styles.meow}>M&D</Text>
        <Text style={styles.sub}>meow & doggie</Text>
      </View>
      <Pressable
        onPress={onNotifyPress}
        style={styles.iconBtn}
        accessibilityRole="button"
        accessibilityLabel="通知"
      >
        <MaterialIcons name="notifications-none" size={26} color={colors.primaryContainer} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    paddingBottom: spacing.md,
    backgroundColor: colors.canvas,
  },
  avatarBtn: {
    borderRadius: radius.pill,
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
  },
  meow: {
    ...typography.h1,
    fontSize: 20,
    letterSpacing: 0,
    color: colors.primaryContainer,
  },
  sub: {
    ...typography.label,
    color: colors.onSurfaceVariant,
  },
  iconBtn: {
    padding: spacing.xs,
    borderRadius: radius.md,
  },
});
