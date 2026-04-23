import { useCallback, useEffect, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { router, useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { api, type Listing, HttpError } from '@/api';
import { useAuth } from '@/auth';
import { Button, Card, Pill, Txt } from '@/components';
import { colors, radius, spacing } from '@/theme';

export default function MarketScreen() {
  const r = useRouter();
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const [items, setItems] = useState<Listing[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setErr(null);
    try {
      setItems(await api.listings.list());
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('加载失败');
    }
  }, []);

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

  const canBack = r.canGoBack();

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <View style={[styles.top, { paddingTop: Math.max(insets.top, spacing.md) }]}>
          <Pressable
            onPress={() => router.back()}
            disabled={!canBack}
            style={[styles.back, !canBack && styles.backHidden]}
            accessibilityLabel="返回"
          >
            <MaterialIcons name="arrow-back-ios-new" size={22} color={colors.onSurface} />
          </Pressable>
          <Txt kind="h2">好物市集</Txt>
          <View style={styles.topSpacer} />
        </View>
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
      <View style={{ gap: spacing.xs }}>
        <Txt muted>宠物用品、领养、上门服务。</Txt>
      </View>

      {err ? (
        <Card>
          <Txt kind="h3">加载失败</Txt>
          <Txt muted>{err}</Txt>
        </Card>
      ) : items === null ? (
        <Card>
          <Txt muted>正在加载商品…</Txt>
        </Card>
      ) : items.length === 0 ? (
        <Card>
          <Txt kind="h3">暂时空空如也</Txt>
          <Txt muted>等一会儿会有好物上架的。</Txt>
        </Card>
      ) : (
        items.map((l) => {
          const canDm = user && l.seller_id !== user.id;
          return (
            <Card key={l.id}>
              <View style={{ flexDirection: 'row', gap: spacing.sm }}>
                <Pill>{listingLabel(l.type)}</Pill>
              </View>
              <Txt kind="h3">{l.title}</Txt>
              <Txt muted numberOfLines={2}>
                {l.description}
              </Txt>
              <Txt kind="h2" style={{ color: colors.danger }}>
                {formatPrice(l.price_cents, l.currency)}
              </Txt>
              {canDm ? (
                <Button
                  title="私信卖家"
                  variant="secondary"
                  onPress={() => router.push(`/(tabs)/messages/${l.seller_id}`)}
                />
              ) : null}
            </Card>
          );
        })
      )}
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
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
    paddingHorizontal: spacing.lg,
    paddingBottom: spacing.sm,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(255, 90, 119, 0.1)',
  },
  back: {
    padding: spacing.sm,
    width: 44,
  },
  backHidden: {
    opacity: 0,
  },
  topSpacer: {
    width: 44,
  },
  content: {
    padding: spacing.lg,
    gap: spacing.md,
    paddingBottom: 120,
  },
});

function listingLabel(t: string): string {
  if (t === 'product') return '商品';
  if (t === 'service') return '服务';
  if (t === 'adopt') return '领养';
  return t;
}

function formatPrice(cents: number, currency: string): string {
  const amount = (cents / 100).toFixed(2);
  if (currency === 'CNY') return `¥ ${amount}`;
  if (currency === 'USD') return `$ ${amount}`;
  return `${amount} ${currency}`;
}
