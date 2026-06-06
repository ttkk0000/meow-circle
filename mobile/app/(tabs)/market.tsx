import {useCallback, useEffect, useMemo, useState} from 'react';
import {Pressable, RefreshControl, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView, useSafeAreaInsets} from 'react-native-safe-area-context';
import {router, useRouter} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {api, HttpError, type Listing} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, Pill, Txt} from '@/components';
import {StitchSearchField} from '@/stitch';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

export default function MarketScreen() {
  const nav = useRouter();
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [items, setItems] = useState<Listing[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [q, setQ] = useState('');

  const load = useCallback(async () => {
    setErr(null);
    try {
      setItems(await api.listings.list());
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法加载市集内容');
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

  const filtered = useMemo(() => {
    if (!items) return [];
    const s = q.trim().toLowerCase();
    if (!s) return items;
    return items.filter(
      (item) =>
        item.title.toLowerCase().includes(s) ||
        item.description.toLowerCase().includes(s) ||
        item.type.toLowerCase().includes(s),
    );
  }, [items, q]);

  const canBack = nav.canGoBack();
  const goBack = () => {
    if (canBack) router.back();
    else router.replace('/(tabs)/discover');
  };

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <View style={[styles.top, { paddingTop: Math.max(insets.top, spacing.md) }]}>
          <Pressable onPress={goBack} style={styles.back} accessibilityRole="button" accessibilityLabel="返回">
            <MaterialIcons name="arrow-back-ios-new" size={22} color={colors.onSurface} />
          </Pressable>
          <View style={styles.topTitle}>
            <Txt kind="h2">好物市集</Txt>
            <Txt kind="label" muted>
              猫猫优先市集
            </Txt>
          </View>
          <View style={styles.topSpacer} />
        </View>
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <View style={styles.hero}>
            <Txt kind="label" style={styles.eyebrow}>
              M&D 安心交易
            </Txt>
            <Txt muted>猫猫用品、领养、上门服务优先；doggie 友好内容作为伴随分支。</Txt>
          </View>
          <StitchSearchField placeholder="搜索用品、服务、领养" value={q} onChangeText={setQ} />

          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
              <Button title="重试" variant="secondary" onPress={load} />
            </Card>
          ) : items === null ? (
            <Card>
              <Txt muted>正在加载市集...</Txt>
            </Card>
          ) : filtered.length === 0 ? (
            <Card>
              <Txt kind="h3">暂时没有匹配的好物</Txt>
              <Txt muted>换个关键词，或稍后再回来看看。</Txt>
            </Card>
          ) : (
            filtered.map((listing) => {
              const canDm = user && listing.seller_id !== user.id;
              return (
                <Card key={listing.id} style={styles.listingCard}>
                  <View style={styles.listingTop}>
                    <Pill tone={listing.type === 'adopt' ? 'success' : 'brand'}>{listingLabel(listing.type)}</Pill>
                    <Pill tone="neutral">已接入 M&D 安全规则</Pill>
                  </View>
                  <Txt kind="h3">{listing.title}</Txt>
                  <Txt muted numberOfLines={3}>
                    {listing.description}
                  </Txt>
                  <View style={styles.priceRow}>
                    <Txt kind="h2" style={{ color: colors.error }}>
                      {formatPrice(listing.price_cents, listing.currency)}
                    </Txt>
                    <Txt kind="bodySmall" muted>
                      卖家 #{listing.seller_id}
                    </Txt>
                  </View>
                  {canDm ? (
                    <Button
                      title="私信卖家"
                      icon="forum"
                      variant="secondary"
                      onPress={() => router.push(`/(tabs)/messages/${listing.seller_id}`)}
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

function listingLabel(type: string): string {
  if (type === 'product') return '商品';
  if (type === 'service') return '服务';
  if (type === 'adopt') return '领养';
  return type;
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
      paddingHorizontal: spacing.lg,
      paddingBottom: spacing.sm,
      borderBottomWidth: StyleSheet.hairlineWidth,
      borderBottomColor: colors.border,
    },
    back: {
      padding: spacing.sm,
      width: 44,
      minHeight: 44,
      alignItems: 'center',
      justifyContent: 'center',
    },
    topTitle: {
      alignItems: 'center',
      gap: 1,
    },
    topSpacer: {
      width: 44,
    },
    content: {
      padding: spacing.lg,
      gap: spacing.md,
      paddingBottom: 120,
    },
    hero: {
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      padding: spacing.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      gap: spacing.sm,
    },
    eyebrow: {
      color: colors.primaryContainer,
    },
    listingCard: {
      gap: spacing.md,
    },
    listingTop: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
    priceRow: {
      flexDirection: 'row',
      alignItems: 'baseline',
      justifyContent: 'space-between',
      gap: spacing.md,
    },
  });
}
