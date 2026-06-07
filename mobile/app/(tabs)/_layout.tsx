import {useEffect, useState, type ComponentProps} from 'react';
import {Redirect, Tabs} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {Text, View} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {api, type ActionCounts} from '@/api';
import {useAuth} from '@/auth';
import {radius, typography, useMndTheme} from '@/theme';

type IconName = ComponentProps<typeof MaterialIcons>['name'];

function compactBadgeCount(value: number) {
  if (value > 99) return '99+';
  return String(value);
}

function tabIcon(name: IconName, badge = 0, badgeColor = '#EF4444', badgeTextColor = '#FFFFFF') {
  return ({ color, focused }: { color: string; focused: boolean }) => (
    <View style={{ minWidth: 32, minHeight: 28, alignItems: 'center', justifyContent: 'center' }}>
      <MaterialIcons
        name={name}
        size={focused ? 26 : 22}
        color={color}
        style={{ transform: [{ scale: focused ? 1.06 : 1 }] }}
      />
      {badge > 0 ? (
        <View
          style={{
            position: 'absolute',
            top: 0,
            right: 0,
            minWidth: 17,
            height: 17,
            paddingHorizontal: 4,
            borderRadius: 999,
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: badgeColor,
          }}
        >
          <Text
            style={{
              color: badgeTextColor,
              fontSize: 10,
              fontWeight: '800',
              lineHeight: 12,
            }}
          >
            {compactBadgeCount(badge)}
          </Text>
        </View>
      ) : null}
    </View>
  );
}

export default function TabsLayout() {
  const { user, loading } = useAuth();
  const { colors } = useMndTheme();
  const [actionCounts, setActionCounts] = useState<ActionCounts | null>(null);
  const insets = useSafeAreaInsets();
  const tabPadBottom = Math.max(insets.bottom, 10);
  const tabHeight = 58 + tabPadBottom;
  const messageBadge =
    (actionCounts?.unread_messages ?? 0) + (actionCounts?.unread_notifications ?? 0);
  const orderBadge = actionCounts?.order_actions ?? 0;

  useEffect(() => {
    if (!user) {
      setActionCounts(null);
      return undefined;
    }
    let active = true;
    const load = async () => {
      try {
        const next = await api.actionCounts.get();
        if (active) setActionCounts(next);
      } catch {
        if (active) setActionCounts(null);
      }
    };
    void load();
    const timer = setInterval(() => void load(), 60000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [user?.id]);

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
      <Tabs.Screen
        name="messages"
        options={{ title: '消息', tabBarIcon: tabIcon('forum', messageBadge, colors.error, colors.onPrimary) }}
      />
      <Tabs.Screen
        name="orders"
        options={{ title: '订单', tabBarIcon: tabIcon('receipt', orderBadge, colors.error, colors.onPrimary) }}
      />
      <Tabs.Screen name="profile" options={{ title: '我的', tabBarIcon: tabIcon('person') }} />
      <Tabs.Screen name="discover" options={{ href: null }} />
      <Tabs.Screen name="compose" options={{ href: null }} />
      <Tabs.Screen name="post" options={{ href: null }} />
      <Tabs.Screen name="listing" options={{ href: null }} />
      <Tabs.Screen name="order" options={{ href: null }} />
    </Tabs>
  );
}
