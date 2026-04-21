import { Redirect, Tabs } from 'expo-router';
import { Text, View } from 'react-native';
import { useAuth } from '@/auth';
import { colors, spacing, typography } from '@/theme';

// Text-glyph tab icons keep us dependency-free until we add
// @expo/vector-icons; swap at will.
const icon = (glyph: string) => ({ color }: { color: string }) => (
  <Text style={{ color, fontSize: 18 }}>{glyph}</Text>
);

export default function TabsLayout() {
  const { user, loading } = useAuth();
  if (!loading && !user) return <Redirect href="/(auth)/login" />;

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: colors.ink,
        tabBarInactiveTintColor: colors.inkSubtle,
        tabBarStyle: {
          backgroundColor: colors.surface100,
          borderTopColor: colors.border,
        },
        tabBarLabelStyle: { ...typography.label, fontSize: 11 },
        headerStyle: { backgroundColor: colors.surface100 },
        headerShadowVisible: false,
        headerTitleStyle: { color: colors.ink, ...typography.h3 },
        headerLeft: () => (
          <View style={{ paddingLeft: spacing.lg }}>
            <Text style={{ fontSize: 20 }}>🐾</Text>
          </View>
        ),
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: '社区', tabBarIcon: icon('◉') }}
      />
      <Tabs.Screen
        name="market"
        options={{ title: '市场', tabBarIcon: icon('◎') }}
      />
      <Tabs.Screen
        name="messages"
        options={{
          title: '消息',
          tabBarIcon: icon('✉'),
          // Nested Stack in ./messages/_layout.tsx supplies its own headers.
          headerShown: false,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{ title: '我的', tabBarIcon: icon('◇') }}
      />
    </Tabs>
  );
}
