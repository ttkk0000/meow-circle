import { Stack } from 'expo-router';
import { colors, typography } from '@/theme';

export default function MessagesStackLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface100 },
        headerShadowVisible: false,
        headerTitleStyle: { color: colors.ink, ...typography.h3 },
        headerTintColor: colors.ink,
      }}
    >
      <Stack.Screen name="index" options={{ title: '私信' }} />
      <Stack.Screen name="[peerId]" options={{ title: '对话' }} />
    </Stack>
  );
}
