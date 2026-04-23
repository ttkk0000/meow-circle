import type { ComponentProps } from 'react';
import { Redirect, Tabs } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '@/auth';
import { colors, typography } from '@/theme';

type IconName = ComponentProps<typeof MaterialIcons>['name'];

function tabIcon(name: IconName) {
  return ({ color, focused }: { color: string; focused: boolean }) => (
    <MaterialIcons
      name={name}
      size={focused ? 26 : 22}
      color={color}
      style={{ transform: [{ scale: focused ? 1.06 : 1 }] }}
    />
  );
}

export default function TabsLayout() {
  const { user, loading } = useAuth();
  const insets = useSafeAreaInsets();
  const tabPadBottom = Math.max(insets.bottom, 10);
  const tabHeight = 56 + tabPadBottom;

  if (!loading && !user) return <Redirect href="/(auth)/login" />;

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primaryContainer,
        tabBarInactiveTintColor: colors.onSurfaceVariant,
        tabBarShowLabel: true,
        tabBarLabelStyle: { ...typography.label, fontSize: 11 },
        tabBarStyle: {
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: colors.tabBar,
          borderTopWidth: 1,
          borderTopColor: 'rgba(255, 90, 119, 0.1)',
          borderTopLeftRadius: 32,
          borderTopRightRadius: 32,
          height: tabHeight,
          paddingBottom: tabPadBottom,
          paddingTop: 10,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: '首页',
          tabBarIcon: tabIcon('home'),
        }}
      />
      <Tabs.Screen
        name="discover"
        options={{
          title: '发现',
          tabBarIcon: tabIcon('explore'),
        }}
      />
      <Tabs.Screen
        name="messages"
        options={{
          title: '消息',
          tabBarIcon: tabIcon('forum'),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: '我',
          tabBarIcon: tabIcon('person'),
        }}
      />
      <Tabs.Screen name="market" options={{ href: null }} />
      <Tabs.Screen name="post" options={{ href: null }} />
      <Tabs.Screen name="compose" options={{ href: null }} />
    </Tabs>
  );
}
