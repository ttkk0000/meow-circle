import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Pressable, RefreshControl, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {router} from 'expo-router';
import {api, HttpError, type Order} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, Pill, Txt} from '@/components';
import {hrefOrderDetail} from '@/href';
import {StitchTopBar} from '@/stitch';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

type OrderRole = 'buyer' | 'seller';

const DEMO_ORDERS: Order[] = [
  {
    id: 101,
    buyer_id: 1,
    seller_id: 5,
    listing_id: 1,
    listing_title: '未拆封猫罐头 6 罐组合',
    amount_cents: 6800,
    currency: 'CNY',
    status: 'pending_payment',
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
  {
    id: 102,
    buyer_id: 1,
    seller_id: 8,
    listing_id: 2,
    listing_title: '周末上门喂猫与铲砂',
    amount_cents: 12000,
    currency: 'CNY',
    status: 'shipped',
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
  {
    id: 103,
    buyer_id: 4,
    seller_id: 1,
    listing_id: 3,
    listing_title: '三个月橘猫领养专用笼/合同',
    amount_cents: 2000,
    currency: 'CNY',
    status: 'paid',
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
];

export default function OrdersScreen() {
  const { user } = useAuth();
  const { colors, cycleTheme } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [role, setRole] = useState<OrderRole>('buyer');
  const [items, setItems] = useState<Order[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [demoMode, setDemoMode] = useState(false);

  const demoItems = useCallback(
    (nextRole: OrderRole) => {
      const currentUserId = user?.id ?? 1;
      const demoOrders = DEMO_ORDERS.map((order) =>
        order.id === 103
          ? { ...order, seller_id: currentUserId }
          : { ...order, buyer_id: currentUserId },
      );
      return demoOrders.filter((order) =>
        nextRole === 'buyer' ? order.buyer_id === currentUserId : order.seller_id === currentUserId,
      );
    },
    [user?.id],
  );

  const load = useCallback(async () => {
    setErr(null);
    if (demoMode) {
      setItems(demoItems(role));
      return;
    }
    try {
      setItems(await api.orders.list(role));
    } catch (e) {
      setItems(null);
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法加载订单，可以先查看演示流程。');
    }
  }, [demoItems, demoMode, role]);

  useEffect(() => {
    load();
  }, [load]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await load();
    } finally {
      setRefreshing(false);
    }
  }, [load]);

  const showDemo = () => {
    setErr(null);
    setDemoMode(true);
    setItems(demoItems(role));
  };

  const replaceOrder = (updated: Order) => {
    setItems((current) => (current ?? []).map((order) => (order.id === updated.id ? updated : order)));
  };

  const runAction = async (order: Order, label: string, action: () => Promise<Order>) => {
    if (demoMode) {
      replaceOrder(mockNextOrder(order, label));
      return;
    }
    setBusyId(order.id);
    try {
      replaceOrder(await action());
    } catch (e) {
      if (e instanceof HttpError) Alert.alert(label, e.payload.message);
      else Alert.alert(label, '操作失败，请稍后再试。');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <StitchTopBar
          user={user}
          title="订单"
          subtitle="交易状态跟踪"
          onAvatarPress={() => router.push('/(tabs)/profile')}
          onNotifyPress={() => Alert.alert('通知', '暂无新通知')}
          onThemePress={cycleTheme}
        />
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <Card style={styles.headerCard}>
            <View style={styles.rowBetween}>
              <View style={{ flex: 1 }}>
                <Txt kind="h2">订单工作台</Txt>
                <Txt muted>买家与卖家状态、支付、发货和售后动作集中在这里。</Txt>
              </View>
              <Button
                title={demoMode ? '演示中' : '演示'}
                variant="ghost"
                icon="auto-awesome"
                onPress={showDemo}
                style={styles.demoButton}
              />
            </View>
            <View style={styles.roleToggle}>
              {(['buyer', 'seller'] as const).map((nextRole) => {
                const on = role === nextRole;
                return (
                  <Pressable
                    key={nextRole}
                    onPress={() => setRole(nextRole)}
                    accessibilityRole="button"
                    accessibilityState={{ selected: on }}
                    accessibilityLabel={nextRole === 'buyer' ? '切换到买家订单' : '切换到卖家订单'}
                    style={[styles.roleButton, on && styles.roleButtonOn]}
                  >
                    <Txt kind="label" style={on ? styles.roleLabelOn : styles.roleLabel}>
                      {nextRole === 'buyer' ? '买家模式' : '卖家模式'}
                    </Txt>
                  </Pressable>
                );
              })}
            </View>
          </Card>

          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
              <View style={styles.actions}>
                <Button title="重试" variant="secondary" onPress={load} />
                <Button title="查看演示" variant="ghost" onPress={showDemo} />
              </View>
            </Card>
          ) : items === null ? (
            <Card>
              <Txt muted>正在加载订单...</Txt>
            </Card>
          ) : items.length === 0 ? (
            <Card>
              <Txt kind="h3">暂无订单</Txt>
              <Txt muted>{role === 'buyer' ? '从市集发起交易后，买家订单会出现在这里。' : '有人购买你的好物后，卖家订单会出现在这里。'}</Txt>
            </Card>
          ) : (
            items.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                role={role}
                busy={busyId === order.id}
                onAction={(label, action) => runAction(order, label, action)}
              />
            ))
          )}
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}

function OrderCard({
  order,
  role,
  busy,
  onAction,
}: {
  order: Order;
  role: OrderRole;
  busy: boolean;
  onAction: (label: string, action: () => Promise<Order>) => void;
}) {
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  return (
    <Card style={styles.orderCard}>
      <View style={styles.rowBetween}>
        <View style={{ flex: 1 }}>
          <Txt kind="h3">{order.listing_title || `订单 #${order.id}`}</Txt>
          <Txt kind="bodySmall" muted>
            订单 #{order.id} · {formatPrice(order.amount_cents, order.currency)}
          </Txt>
        </View>
        <Pill tone={statusTone(order.status)}>{statusLabel(order.status)}</Pill>
      </View>
      <Txt muted>{statusCopy(order.status, role)}</Txt>
      <View style={styles.actions}>
        <Button
          title="详情"
          icon="info"
          variant="secondary"
          disabled={busy}
          onPress={() => router.push(hrefOrderDetail(order.id))}
        />
        {role === 'buyer' && order.status === 'pending_payment' ? (
          <>
            <Button title="立即支付" loading={busy} onPress={() => onAction('立即支付', () => api.orders.pay(order.id))} />
            <Button
              title="取消订单"
              variant="secondary"
              disabled={busy}
              onPress={() => onAction('取消订单', () => api.orders.cancel(order.id))}
            />
          </>
        ) : null}
        {role === 'buyer' && order.status === 'shipped' ? (
          <Button title="确认收货" loading={busy} onPress={() => onAction('确认收货', () => api.orders.complete(order.id))} />
        ) : null}
        {role === 'seller' && order.status === 'paid' ? (
          <>
            <Button title="确认发货" loading={busy} onPress={() => onAction('确认发货', () => api.orders.ship(order.id))} />
            <Button
              title="退款"
              variant="danger"
              disabled={busy}
              onPress={() => onAction('退款', () => api.orders.refund(order.id))}
            />
          </>
        ) : null}
        {role === 'seller' && order.status === 'shipped' ? (
          <Button
            title="退款"
            variant="danger"
            loading={busy}
            onPress={() => onAction('退款', () => api.orders.refund(order.id))}
          />
        ) : null}
      </View>
    </Card>
  );
}

function mockNextOrder(order: Order, label: string): Order {
  if (label === '立即支付') return { ...order, status: 'paid' };
  if (label === '取消订单') return { ...order, status: 'cancelled' };
  if (label === '确认发货') return { ...order, status: 'shipped' };
  if (label === '确认收货') return { ...order, status: 'completed' };
  if (label === '退款') return { ...order, status: 'refunded' };
  return order;
}

function statusLabel(status: string): string {
  return {
    pending_payment: '待支付',
    paid: '已支付',
    cancelled: '已取消',
    shipped: '已发货',
    refunded: '已退款',
    completed: '已完成',
  }[status] ?? status;
}

function statusTone(status: string): 'neutral' | 'brand' | 'success' | 'warning' | 'danger' {
  if (status === 'pending_payment') return 'warning';
  if (status === 'paid' || status === 'shipped') return 'brand';
  if (status === 'completed') return 'success';
  if (status === 'refunded') return 'danger';
  return 'neutral';
}

function statusCopy(status: string, role: OrderRole): string {
  if (role === 'buyer' && status === 'pending_payment') return '买家可支付或取消订单。';
  if (role === 'buyer' && status === 'shipped') return '卖家已发货，收到后可确认收货。';
  if (role === 'seller' && status === 'paid') return '买家已支付，卖家可确认发货或退款。';
  if (role === 'seller' && status === 'shipped') return '商品已发出，卖家仍可处理退款。';
  if (status === 'completed') return '交易已完成。';
  if (status === 'cancelled') return '订单已取消。';
  if (status === 'refunded') return '订单已退款。';
  return '当前状态暂无可执行动作。';
}

function formatPrice(cents: number, currency: string): string {
  const amount = (cents / 100).toFixed(2);
  if (currency === 'CNY') return `¥ ${amount}`;
  if (currency === 'USD') return `$ ${amount}`;
  return `${amount} ${currency}`;
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
    root: {
      flex: 1,
      backgroundColor: colors.canvas,
    },
    safe: {
      flex: 1,
    },
    content: {
      paddingHorizontal: spacing.lg,
      paddingBottom: 120,
      gap: spacing.md,
    },
    headerCard: {
      gap: spacing.md,
    },
    rowBetween: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: spacing.md,
    },
    demoButton: {
      minWidth: 88,
    },
    roleToggle: {
      flexDirection: 'row',
      borderRadius: radius.pill,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      backgroundColor: colors.surfaceLow,
      padding: 3,
      gap: 3,
    },
    roleButton: {
      flex: 1,
      alignItems: 'center',
      paddingVertical: spacing.sm,
      borderRadius: radius.pill,
    },
    roleButtonOn: {
      backgroundColor: colors.primaryContainer,
    },
    roleLabel: {
      color: colors.onSurfaceVariant,
    },
    roleLabelOn: {
      color: colors.onPrimary,
    },
    orderCard: {
      gap: spacing.md,
    },
    actions: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
  });
}
