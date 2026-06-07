import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Image, Pressable, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView, useSafeAreaInsets} from 'react-native-safe-area-context';
import {router, useLocalSearchParams, useRouter} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {api, HttpError, type Listing, type MediaItem, resolveMediaUrl} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, Pill, Txt} from '@/components';
import {hrefOrderDetail} from '@/href';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

export default function ListingDetailScreen() {
  const nav = useRouter();
  const { id: rawId } = useLocalSearchParams<{ id: string }>();
  const listingId = parseInt(String(rawId || ''), 10);
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [listing, setListing] = useState<Listing | null>(null);
  const [media, setMedia] = useState<MediaItem[]>([]);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!Number.isFinite(listingId) || listingId <= 0) {
      setErr('无效的市集链接');
      return;
    }
    setErr(null);
    try {
      const data = await api.listings.get(listingId);
      setListing(data.listing);
      setMedia(data.media ?? []);
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法加载商品详情');
    }
  }, [listingId]);

  useEffect(() => {
    void load();
  }, [load]);

  const goBack = () => {
    if (nav.canGoBack()) router.back();
    else router.replace('/(tabs)/market');
  };

  const buyNow = async () => {
    if (!listing) return;
    setBusy(true);
    try {
      const order = await api.orders.create(listing.id, '来自 M&D 商品详情页的订单');
      Alert.alert('订单已创建', `订单 #${order.id} 已进入订单页。`, [
        { text: '继续浏览', style: 'cancel' },
        { text: '查看订单', onPress: () => router.push(hrefOrderDetail(order.id)) },
      ]);
    } catch (e) {
      if (e instanceof HttpError) Alert.alert('购买失败', e.payload.message);
      else Alert.alert('购买失败', '暂时无法创建订单，请稍后再试。');
    } finally {
      setBusy(false);
    }
  };

  const firstImage = media.find((item) => item.kind === 'image');
  const firstImageUri = resolveMediaUrl(firstImage?.url);
  const canContact = !!user && !!listing && user.id !== listing.seller_id;
  const canBuy = canContact && listing.price_cents > 0 && listing.type !== 'adopt';

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <View style={[styles.top, { paddingTop: Math.max(insets.top, spacing.md) }]}>
          <Pressable onPress={goBack} style={styles.iconBtn} accessibilityRole="button" accessibilityLabel="返回">
            <MaterialIcons name="arrow-back-ios-new" size={22} color={colors.onSurface} />
          </Pressable>
          <Txt kind="h3">市集详情</Txt>
          <View style={{ width: 44 }} />
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          {firstImageUri ? (
            <Image source={{ uri: firstImageUri }} style={styles.hero} resizeMode="cover" />
          ) : (
            <View style={styles.heroEmpty}>
              <MaterialIcons name="storefront" size={36} color={colors.primaryContainer} />
              <Txt kind="label" style={{ color: colors.primaryContainer }}>
                M&D Market
              </Txt>
            </View>
          )}

          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
              <Button title="重试" variant="secondary" onPress={load} />
            </Card>
          ) : !listing ? (
            <Card>
              <Txt muted>正在加载市集详情...</Txt>
            </Card>
          ) : (
            <Card style={styles.detailCard}>
              <View style={styles.pillRow}>
                <Pill tone={listing.type === 'adopt' ? 'success' : 'brand'}>{listingLabel(listing.type)}</Pill>
                <Pill tone="neutral">安心交易</Pill>
              </View>
              <Txt kind="h1">{listing.title}</Txt>
              <Txt muted>{listing.description || '卖家暂未填写描述。'}</Txt>
              <View style={styles.priceRow}>
                <Txt kind="h1" style={{ color: colors.error }}>
                  {formatPrice(listing.price_cents, listing.currency)}
                </Txt>
                <Txt kind="bodySmall" muted>
                  卖家 #{listing.seller_id}
                </Txt>
              </View>
              <View style={styles.actionRow}>
                {canBuy ? <Button title="立即购买" icon="bolt" loading={busy} onPress={buyNow} /> : null}
                {canContact ? (
                  <Button
                    title="私信卖家"
                    icon="forum"
                    variant="secondary"
                    disabled={busy}
                    onPress={() => router.push(`/(tabs)/messages/${listing.seller_id}`)}
                  />
                ) : null}
                <Button
                  title="出价"
                  icon="local-offer"
                  variant="ghost"
                  disabled={!canContact || busy}
                  onPress={() => Alert.alert('出价', 'Offer Flow 已列入下一阶段实现。')}
                />
              </View>
            </Card>
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
    hero: {
      width: '100%',
      height: 280,
      borderRadius: radius.xl,
      backgroundColor: colors.surfaceLow,
    },
    heroEmpty: {
      height: 220,
      borderRadius: radius.xl,
      alignItems: 'center',
      justifyContent: 'center',
      gap: spacing.sm,
      backgroundColor: colors.accentSoft,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    detailCard: {
      gap: spacing.md,
    },
    pillRow: {
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
    actionRow: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
  });
}
