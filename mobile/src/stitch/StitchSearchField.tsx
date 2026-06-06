import {useMemo} from 'react';
import {StyleSheet, TextInput, type TextInputProps, View} from 'react-native';
import {MaterialIcons} from '@expo/vector-icons';
import {type MndColors, radius, shadow, spacing, typography, useMndTheme} from '@/theme';

export function StitchSearchField({ accessibilityLabel, placeholder, ...props }: TextInputProps) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  return (
    <View style={styles.row}>
      <MaterialIcons name="search" size={22} color={colors.onSurfaceSubtle} style={styles.icon} />
      <TextInput
        placeholder={placeholder}
        placeholderTextColor={colors.onSurfaceSubtle}
        accessibilityLabel={accessibilityLabel ?? (typeof placeholder === 'string' ? placeholder : '搜索')}
        style={styles.input}
        {...props}
      />
    </View>
  );
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: colors.surface,
      borderRadius: radius.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      paddingHorizontal: spacing.md,
      minHeight: 48,
      ...shadow(colors.shadow, 'soft'),
    },
    icon: {
      marginRight: spacing.sm,
    },
    input: {
      flex: 1,
      ...typography.body,
      color: colors.onSurface,
      paddingVertical: spacing.sm,
    },
  });
}
