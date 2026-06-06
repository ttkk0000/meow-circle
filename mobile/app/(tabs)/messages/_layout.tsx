import {Stack} from 'expo-router';
import {typography, useMndTheme} from '@/theme';

export default function MessagesStackLayout() {
  const { colors } = useMndTheme();
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.canvas },
        headerShadowVisible: false,
        headerTitleStyle: { color: colors.onSurface, ...typography.h3 },
        headerTintColor: colors.onSurface,
        contentStyle: { backgroundColor: colors.canvas },
      }}
    >
      <Stack.Screen name="index" options={{ headerShown: false }} />
      <Stack.Screen name="[peerId]" options={{ title: '对话' }} />
    </Stack>
  );
}
