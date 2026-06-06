import type {ComponentProps} from 'react';
import {Redirect, Tabs} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {useAuth} from '@/auth';
import {radius, typography, useMndTheme} from '@/theme';

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
  const { colors } = useMndTheme();
  const insets = useSafeAreaInsets();
  const tabPadBottom = Math.max(insets.bottom, 10);
  const tabHeight = 58 + tabPadBottom;

  if (!loading && !user) return <Redirect href="/(auth)/login" />;

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primaryContainer,
        tabBarInactiveTintColor: colors.onSurfaceSubtle,
        tabBarShowLabel: true,
        tabBarLabelStyle: { ...typography.label, fontSize: 11 },
        tabBarStyle: {
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: colors.tabBar,
          borderTopWidth: 1,
          borderTopColor: colors.border,
          borderTopLeftRadius: radius.lg,
          borderTopRightRadius: radius.lg,
          height: tabHeight,
          paddingBottom: tabPadBottom,
          paddingTop: 10,
        },
      }}
    >
      <Tabs.Screen name="index" options={{ title: '首页', tabBarIcon: tabIcon('home') }} />
      <Tabs.Screen name="market" options={{ title: '市集', tabBarIcon: tabIcon('storefront') }} />
      <Tabs.Screen name="messages" options={{ title: '消息', tabBarIcon: tabIcon('forum') }} />
      <Tabs.Screen name="orders" options={{ title: '订单', tabBarIcon: tabIcon('receipt') }} />
      <Tabs.Screen name="profile" options={{ title: '我的', tabBarIcon: tabIcon('person') }} />
      <Tabs.Screen name="discover" options={{ href: null }} />
      <Tabs.Screen name="compose" options={{ href: null }} />
      <Tabs.Screen name="post" options={{ href: null }} />
    </Tabs>
  );
}
