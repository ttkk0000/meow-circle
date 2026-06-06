import {Redirect, Stack} from 'expo-router';
import {useAuth} from '@/auth';
import {typography, useMndTheme} from '@/theme';

export default function AuthLayout() {
  const { user, loading } = useAuth();
  const { colors } = useMndTheme();
  if (!loading && user) return <Redirect href="/(tabs)" />;
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.canvas },
        headerTitleStyle: { color: colors.onSurface, ...typography.h3 },
        contentStyle: { backgroundColor: colors.canvas },
        headerTintColor: colors.onSurface,
        headerShadowVisible: false,
        headerBackTitle: ' ',
        headerShown: false,
      }}
    >
      <Stack.Screen name="login" />
      <Stack.Screen name="register" />
    </Stack>
  );
}
