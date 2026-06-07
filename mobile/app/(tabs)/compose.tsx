import type {ComponentProps} from 'react';
import {useMemo, useState} from 'react';
import {Alert, Image, Pressable, ScrollView, StyleSheet, View} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {router, useRouter} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import {api, HttpError, resolveMediaUrl, type MediaItem} from '@/api';
import {Button, Input, Txt} from '@/components';
import {type MndColors, radius, spacing, useMndTheme} from '@/theme';

const MODES = [
  { value: 'daily_share', label: '猫猫日常', icon: 'pets' },
  { value: 'help', label: '求助问答', icon: 'help-outline' },
  { value: 'activity', label: '活动', icon: 'event' },
  { value: 'trade', label: '好物交易', icon: 'shopping-bag' },
] as const;

type Mode = (typeof MODES)[number]['value'];
type IconName = ComponentProps<typeof MaterialIcons>['name'];
type Draft = { title: string; content: string; tagsRaw: string; priceRaw: string; mediaItems: MediaItem[] };

const EMPTY_DRAFTS: Record<Mode, Draft> = {
  daily_share: { title: '', content: '', tagsRaw: '', priceRaw: '', mediaItems: [] },
  help: { title: '', content: '', tagsRaw: '', priceRaw: '', mediaItems: [] },
  activity: { title: '', content: '', tagsRaw: '', priceRaw: '', mediaItems: [] },
  trade: { title: '', content: '', tagsRaw: '', priceRaw: '', mediaItems: [] },
};

export default function ComposeScreen() {
  const nav = useRouter();
  const insets = useSafeAreaInsets();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [mode, setMode] = useState<Mode>('daily_share');
  const [drafts, setDrafts] = useState<Record<Mode, Draft>>(EMPTY_DRAFTS);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const draft = drafts[mode];
  const selectedMode = MODES.find((m) => m.value === mode)!;

  const updateDraft = (patch: Partial<Draft>) => {
    setDrafts((current) => ({
      ...current,
      [mode]: { ...current[mode], ...patch },
    }));
  };

  const close = () => {
    if (nav.canGoBack()) router.back();
    else router.replace('/(tabs)');
  };

  const publish = async () => {
    setErr(null);
    if (!draft.title.trim() || !draft.content.trim()) {
      setErr('标题和正文都要填写。');
      return;
    }
    setBusy(true);
    try {
      const tags = draft.tagsRaw
        .split(/[,，\s]+/)
        .map((t) => t.trim())
        .filter(Boolean);
      const media_ids = draft.mediaItems.map((item) => item.id);
      if (mode === 'trade') {
        const price = parsePriceToCents(draft.priceRaw);
        if (price == null || price <= 0) {
          setErr('请填写有效价格。');
          return;
        }
        await api.listings.create({
          type: 'product',
          title: draft.title.trim(),
          description: draft.content.trim(),
          price_cents: price,
          currency: 'CNY',
          media_ids,
        });
        setDrafts((current) => ({ ...current, [mode]: blankDraft() }));
        router.replace('/(tabs)/market');
      } else {
        await api.posts.create({
          title: draft.title.trim(),
          content: draft.content.trim(),
          category: mode,
          tags,
          media_ids,
        });
        setDrafts((current) => ({ ...current, [mode]: blankDraft() }));
        router.replace('/(tabs)');
      }
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message || '发布失败，请稍后重试。');
      else setErr('发布失败，请检查网络或后端服务。');
    } finally {
      setBusy(false);
    }
  };

  const pickMedia = async () => {
    setErr(null);
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('需要相册权限', '请允许 M&D 访问相册后再上传媒体。');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.All,
      allowsMultipleSelection: true,
      quality: 0.88,
    });
    if (result.canceled || result.assets.length === 0) return;
    setUploading(true);
    try {
      const uploaded: MediaItem[] = [];
      for (const asset of result.assets.slice(0, 6)) {
        const media = await api.media.upload({
          uri: asset.uri,
          name: asset.fileName ?? undefined,
          type: asset.mimeType ?? undefined,
        });
        uploaded.push(media);
      }
      updateDraft({ mediaItems: draft.mediaItems.concat(uploaded) });
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message || '上传失败，请稍后再试。');
      else setErr('上传失败，请稍后再试。');
    } finally {
      setUploading(false);
    }
  };

  const removeMedia = (id: number) => {
    updateDraft({ mediaItems: draft.mediaItems.filter((item) => item.id !== id) });
  };

  return (
    <View style={styles.root}>
      <View style={[styles.header, { paddingTop: Math.max(insets.top, spacing.md) }]}>
        <Pressable onPress={close} style={styles.hBtn} accessibilityRole="button" accessibilityLabel="关闭">
          <MaterialIcons name="close" size={26} color={colors.onSurface} />
        </Pressable>
        <View style={styles.headerCenter}>
          <Txt kind="h3" style={{ color: colors.primaryContainer }}>
            M&D 发布台
          </Txt>
          <Txt kind="label" muted>
            {selectedMode.label}
          </Txt>
        </View>
        <Pressable
          onPress={publish}
          disabled={busy}
          style={styles.hBtn}
          accessibilityRole="button"
          accessibilityLabel="发布"
          accessibilityState={{ disabled: busy }}
        >
          <Txt kind="h3" style={{ color: colors.primaryContainer }}>
            发布
          </Txt>
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={[styles.body, { paddingBottom: Math.max(insets.bottom, spacing.lg) + 104 }]}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.hero}>
          <Txt kind="label" style={styles.eyebrow}>
            Compose · M&D
          </Txt>
          <Txt kind="h2">写给猫猫宇宙的一条新动态</Txt>
          <Txt muted>猫猫日常、求助、活动优先，doggie 可以在服务、领养和活动里自然出现。</Txt>
        </View>

        <View style={styles.modeGrid}>
          {MODES.map((item) => {
            const on = item.value === mode;
            return (
              <Pressable
                key={item.value}
                onPress={() => setMode(item.value)}
                accessibilityRole="button"
                accessibilityState={{ selected: on }}
                accessibilityLabel={`选择${item.label}模式`}
                style={[styles.modeCard, on && styles.modeCardOn]}
              >
                <MaterialIcons name={item.icon as IconName} size={22} color={on ? colors.primaryContainer : colors.onSurfaceVariant} />
                <Txt kind="label" style={on ? styles.modeLabelOn : styles.modeLabel}>
                  {item.label}
                </Txt>
              </Pressable>
            );
          })}
        </View>

        <Input
          label="标题"
          placeholder="今天想记录哪一刻？"
          value={draft.title}
          onChangeText={(title) => updateDraft({ title })}
        />
        <Input
          label="正文"
          placeholder={mode === 'trade' ? '写清楚成色、取货方式、适用猫猫或 doggie 的情况。' : '写下猫猫日常、求助问题、活动信息或好物说明。'}
          value={draft.content}
          onChangeText={(content) => updateDraft({ content })}
          multiline
          style={styles.bodyInput}
        />
        {mode === 'trade' ? (
          <Input
            label="价格（元）"
            placeholder="例如 68"
            keyboardType="decimal-pad"
            value={draft.priceRaw}
            onChangeText={(priceRaw) => updateDraft({ priceRaw })}
          />
        ) : (
          <Input
            label="标签"
            placeholder="用逗号分隔，例如：M&D, 猫猫, 新手"
            value={draft.tagsRaw}
            onChangeText={(tagsRaw) => updateDraft({ tagsRaw })}
          />
        )}

        <View style={styles.mediaCard}>
          <View style={styles.mediaHead}>
            <View style={{ flex: 1 }}>
              <Txt kind="h3">媒体</Txt>
              <Txt kind="bodySmall" muted>
                {draft.mediaItems.length ? `已选择 ${draft.mediaItems.length} 个` : '图片或视频'}
              </Txt>
            </View>
            <Button title="选择" icon="add-photo-alternate" variant="secondary" loading={uploading} onPress={pickMedia} />
          </View>
          {draft.mediaItems.length ? (
            <View style={styles.mediaGrid}>
              {draft.mediaItems.map((item) => {
                const uri = resolveMediaUrl(item.url);
                return (
                  <View key={item.id} style={styles.mediaThumb}>
                    {item.kind === 'image' && uri ? (
                      <Image source={{ uri }} style={styles.mediaImage} resizeMode="cover" />
                    ) : (
                      <View style={styles.videoThumb}>
                        <MaterialIcons name="videocam" size={28} color={colors.primaryContainer} />
                      </View>
                    )}
                    <Pressable
                      onPress={() => removeMedia(item.id)}
                      style={styles.removeMedia}
                      accessibilityRole="button"
                      accessibilityLabel="移除媒体"
                    >
                      <MaterialIcons name="close" size={16} color={colors.onPrimary} />
                    </Pressable>
                  </View>
                );
              })}
            </View>
          ) : null}
        </View>

        {err ? (
          <Txt kind="bodySmall" style={{ color: colors.error }}>
            {err}
          </Txt>
        ) : null}

        <Button title={mode === 'trade' ? '发布交易' : '发布动态'} icon="send" loading={busy} disabled={uploading} onPress={publish} />
      </ScrollView>
    </View>
  );
}

function blankDraft(): Draft {
  return { title: '', content: '', tagsRaw: '', priceRaw: '', mediaItems: [] };
}

function parsePriceToCents(raw: string): number | null {
  const normalized = raw.trim().replace(',', '.');
  if (!normalized) return null;
  const amount = Number(normalized);
  if (!Number.isFinite(amount)) return null;
  return Math.round(amount * 100);
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
    root: {
      flex: 1,
      backgroundColor: colors.canvas,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: spacing.lg,
      paddingBottom: spacing.md,
      borderBottomWidth: StyleSheet.hairlineWidth,
      borderBottomColor: colors.border,
      backgroundColor: colors.canvas,
    },
    hBtn: {
      minWidth: 48,
      minHeight: 44,
      paddingVertical: spacing.sm,
      alignItems: 'center',
      justifyContent: 'center',
    },
    headerCenter: {
      alignItems: 'center',
      gap: 1,
    },
    body: {
      padding: spacing.lg,
      gap: spacing.md,
    },
    hero: {
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      padding: spacing.lg,
      gap: spacing.sm,
    },
    eyebrow: {
      color: colors.primaryContainer,
    },
    modeGrid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
    modeCard: {
      width: '48%',
      flexGrow: 1,
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      paddingHorizontal: spacing.md,
      paddingVertical: spacing.md,
      borderRadius: radius.lg,
      backgroundColor: colors.surface,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    modeCardOn: {
      backgroundColor: colors.accentSoft,
      borderColor: colors.borderMedium,
    },
    modeLabel: {
      color: colors.onSurfaceVariant,
    },
    modeLabelOn: {
      color: colors.primaryContainer,
    },
    bodyInput: {
      minHeight: 150,
      textAlignVertical: 'top',
    },
    mediaCard: {
      gap: spacing.md,
      padding: spacing.lg,
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      backgroundColor: colors.surface,
    },
    mediaHead: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: spacing.md,
    },
    mediaGrid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
    mediaThumb: {
      width: 86,
      height: 86,
      overflow: 'hidden',
      borderRadius: radius.lg,
      backgroundColor: colors.surfaceLow,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    mediaImage: {
      width: '100%',
      height: '100%',
    },
    videoThumb: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.accentSoft,
    },
    removeMedia: {
      position: 'absolute',
      top: 6,
      right: 6,
      width: 24,
      height: 24,
      borderRadius: 999,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.overlay,
    },
  });
}
