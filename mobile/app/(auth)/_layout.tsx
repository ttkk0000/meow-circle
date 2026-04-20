import { Redirect, Stack } from 'expo-router';
import { useAuth } from '@/auth';
import { colors } from '@/theme';

export default function AuthLayout() {
  const { user, loading } = useAuth();
  if (!loading && user) return <Redirect href="/(tabs)" />;
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface100 },
        headerTitleStyle: { color: colors.ink },
        contentStyle: { backgroundColor: colors.surface100 },
        headerShadowVisible: false,
        headerBackTitle: ' ',
      }}
    >
      <Stack.Screen name="login" options={{ title: '登录' }} />
      <Stack.Screen name="register" options={{ title: '注册' }} />
    </Stack>
  );
}
