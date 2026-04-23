import type { ComponentProps } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors, elevation, radius } from '@/theme';

const TAB_BAR_REGION = 72;

type IconName = ComponentProps<typeof MaterialIcons>['name'];

type Props = {
  icon: IconName;
  onPress: () => void;
  accessibilityLabel?: string;
};

export function StitchFab({ icon, onPress, accessibilityLabel }: Props) {
  const insets = useSafeAreaInsets();
  const bottom = insets.bottom + TAB_BAR_REGION + 12;

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      style={({ pressed }) => [styles.fab, { bottom }, pressed && styles.fabPressed]}
    >
      <MaterialIcons name={icon} size={28} color={colors.onPrimary} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fab: {
    position: 'absolute',
    right: 24,
    width: 56,
    height: 56,
    borderRadius: radius.pill,
    backgroundColor: colors.primaryContainer,
    alignItems: 'center',
    justifyContent: 'center',
    ...elevation.fab,
  },
  fabPressed: {
    opacity: 0.92,
    transform: [{ scale: 0.97 }],
  },
});
