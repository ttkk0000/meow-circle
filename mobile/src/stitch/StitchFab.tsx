import {type ComponentProps, useMemo} from 'react';
import {Pressable, StyleSheet} from 'react-native';
import {MaterialIcons} from '@expo/vector-icons';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {type MndColors, radius, shadow, useMndTheme} from '@/theme';

const TAB_BAR_REGION = 78;

type IconName = ComponentProps<typeof MaterialIcons>['name'];

type Props = {
  icon: IconName;
  onPress: () => void;
  accessibilityLabel?: string;
};

export function StitchFab({ icon, onPress, accessibilityLabel = '主要操作' }: Props) {
  const insets = useSafeAreaInsets();
  const { colors } = useMndTheme();
  const bottom = insets.bottom + TAB_BAR_REGION + 12;
  const styles = useMemo(() => makeStyles(colors, bottom), [bottom, colors]);

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      style={({ pressed }) => [styles.fab, pressed && styles.pressed]}
    >
      <MaterialIcons name={icon} size={28} color={colors.onPrimary} />
    </Pressable>
  );
}

function makeStyles(colors: MndColors, bottom: number) {
  return StyleSheet.create({
    fab: {
      position: 'absolute',
      right: 24,
      bottom,
      width: 56,
      height: 56,
      borderRadius: radius.pill,
      backgroundColor: colors.primaryContainer,
      alignItems: 'center',
      justifyContent: 'center',
      ...shadow(colors.shadow, 'fab'),
    },
    pressed: {
      opacity: 0.92,
      transform: [{ scale: 0.97 }],
    },
  });
}
