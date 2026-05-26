import { StyleSheet, TextInput, type TextInputProps, View } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { colors, radius, spacing, typography } from '@/theme';

export function StitchSearchField(props: TextInputProps) {
  return (
    <View style={styles.row}>
      <MaterialIcons name="search" size={22} color={colors.outline} style={styles.icon} />
      <TextInput
        placeholderTextColor={colors.outline}
        style={styles.input}
        {...props}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    paddingHorizontal: spacing.md,
    minHeight: 48,
    ...{
      shadowColor: colors.primaryContainer,
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.06,
      shadowRadius: 10,
      elevation: 2,
    },
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
