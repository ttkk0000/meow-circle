import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Pressable, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView, useSafeAreaInsets} from 'react-native-safe-area-context';
import {router, useLocalSearchParams, useRouter} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {api, HttpError, type Order} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, Pill, Txt} from '@/components';
import {hrefListingDetail} from '@/href';
import {type MndColors, spacing, useMndTheme} from '@/theme';

export default function OrderDetailScreen() {
  const nav = useRouter();
  const { id: rawId } = useLocalSearchParams<{ id: string }>();
  const orderId = parseInt(String(rawId || ''), 10);
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [order, setOrder] = useState<Order | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!Number.isFinite(orderId) || orderId <= 0) {
      setErr('无效的订单链接');
      return;
    }
    setErr(null);
    try {
      setOrder(await api.orders.get(orderId));
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法加载订单详情');
    }
  }, [orderId]);

  useEffect(() => {
    void load();
  }, [load]);

  const goBack = () => {
    if (nav.canGoBack()) router.back();
    else router.replace('/(tabs)/orders');
  };

  const runAction = async (label: string, action: () => Promise<Order>) => {
    setBusy(true);
    try {
      setOrder(await action());
    } catch (e) {
      if (e instanceof HttpError) Alert.alert(label, e.payload.message);
      else Alert.alert(label, '操作失败，请稍后再试。');
    } finally {
      setBusy(false);
    }
  };

  const role = order && user?.id === order.seller_id ? 'seller' : 'buyer';

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <View style={[styles.top, { paddingTop: Math.max(insets.top, spacing.md) }]}>
          <Pressable onPress={goBack} style={styles.iconBtn} accessibilityRole="button" accessibilityLabel="返回">
            <MaterialIcons name="arrow-back-ios-new" size={22} color={colors.onSurface} />
          </Pressable>
          <Txt kind="h3">订单详情</Txt>
          <View style={{ width: 44 }} />
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
              <Button title="重试" variant="secondary" onPress={load} />
            </Card>
          ) : !order ? (
            <Card>
              <Txt muted>正在加载订单详情...</Txt>
            </Card>
          ) : (
            <>
              <Card style={styles.detailCard}>
                <View style={styles.rowBetween}>
                  <View style={{ flex: 1 }}>
                    <Txt kind="h2">{order.listing_title || `订单 #${order.id}`}</Txt>
                    <Txt muted>订单 #{order.id}</Txt>
                  </View>
                  <Pill tone={statusTone(order.status)}>{statusLabel(order.status)}</Pill>
                </View>
                <Txt kind="h1" style={{ color: colors.error }}>
                  {formatPrice(order.amount_cents, order.currency)}
                </Txt>
                <View style={styles.metaGrid}>
                  <Meta label="买家" value={`#${order.buyer_id}`} />
                  <Meta label="卖家" value={`#${order.seller_id}`} />
                  <Meta label="商品" value={`#${order.listing_id}`} />
                  <Meta label="角色" value={role === 'seller' ? '卖家' : '买家'} />
                </View>
                <View style={styles.actionRow}>
                  <Button
                    title="商品详情"
                    icon="storefront"
                    variant="secondary"
                    disabled={busy}
                    onPress={() => router.push(hrefListingDetail(order.listing_id))}
                  />
                  {role === 'buyer' && order.status === 'pending_payment' ? (
                    <>
                      <Button title="立即支付" icon="bolt" loading={busy} onPress={() => runAction('立即支付', () => api.orders.pay(order.id))} />
                      <Button title="取消订单" variant="secondary" disabled={busy} onPress={() => runAction('取消订单', () => api.orders.cancel(order.id))} />
                    </>
                  ) : null}
                  {role === 'buyer' && order.status === 'shipped' ? (
                    <Button title="确认收货" loading={busy} onPress={() => runAction('确认收货', () => api.orders.complete(order.id))} />
                  ) : null}
                  {role === 'seller' && order.status === 'paid' ? (
                    <>
                      <Button title="确认发货" loading={busy} onPress={() => runAction('确认发货', () => api.orders.ship(order.id))} />
                      <Button title="退款" variant="danger" disabled={busy} onPress={() => runAction('退款', () => api.orders.refund(order.id))} />
                    </>
                  ) : null}
                  {role === 'seller' && order.status === 'shipped' ? (
                    <Button title="退款" variant="danger" loading={busy} onPress={() => runAction('退款', () => api.orders.refund(order.id))} />
                  ) : null}
                </View>
              </Card>
              <Card style={styles.detailCard}>
                <Txt kind="h3">状态时间线</Txt>
                <TimelineRow label="创建" value={order.created_at} active />
                <TimelineRow label="支付" value={order.paid_at} active={!!order.paid_at} />
                <TimelineRow label="发货" value={order.shipped_at} active={!!order.shipped_at} />
                <TimelineRow label="完成" value={order.completed_at} active={!!order.completed_at} />
              </Card>
            </>
          )}
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <View>
      <Txt kind="label" muted>
        {label}
      </Txt>
      <Txt>{value}</Txt>
    </View>
  );
}

function TimelineRow({ label, value, active }: { label: string; value?: string | null; active: boolean }) {
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: spacing.md }}>
      <Txt muted={!active}>{label}</Txt>
      <Txt muted>{value ? new Date(value).toLocaleString() : '未发生'}</Txt>
    </View>
  );
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
    top: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: spacing.md,
      paddingBottom: spacing.sm,
      borderBottomWidth: StyleSheet.hairlineWidth,
      borderBottomColor: colors.border,
    },
    iconBtn: {
      width: 44,
      minHeight: 44,
      alignItems: 'center',
      justifyContent: 'center',
    },
    content: {
      padding: spacing.lg,
      paddingBottom: 120,
      gap: spacing.md,
    },
    detailCard: {
      gap: spacing.md,
    },
    rowBetween: {
      flexDirection: 'row',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
      gap: spacing.md,
    },
    metaGrid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.lg,
    },
    actionRow: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
  });
}
