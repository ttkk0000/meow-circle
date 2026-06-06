import type {ComponentProps} from 'react';
import {useMemo, useState} from 'react';
import {Pressable, ScrollView, StyleSheet, View} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {router, useRouter} from 'expo-router';
import {MaterialIcons} from '@expo/vector-icons';
import {api, HttpError} from '@/api';
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
type Draft = { title: string; content: string; tagsRaw: string };

const EMPTY_DRAFTS: Record<Mode, Draft> = {
  daily_share: { title: '', content: '', tagsRaw: '' },
  help: { title: '', content: '', tagsRaw: '' },
  activity: { title: '', content: '', tagsRaw: '' },
  trade: { title: '', content: '', tagsRaw: '' },
};

export default function ComposeScreen() {
  const nav = useRouter();
  const insets = useSafeAreaInsets();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [mode, setMode] = useState<Mode>('daily_share');
  const [drafts, setDrafts] = useState<Record<Mode, Draft>>(EMPTY_DRAFTS);
  const [busy, setBusy] = useState(false);
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
      await api.posts.create({
        title: draft.title.trim(),
        content: draft.content.trim(),
        category: mode,
        tags,
      });
      setDrafts((current) => ({ ...current, [mode]: { title: '', content: '', tagsRaw: '' } }));
      router.replace('/(tabs)');
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message || '发布失败，请稍后重试。');
      else setErr('发布失败，请检查网络或后端服务。');
    } finally {
      setBusy(false);
    }
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
          placeholder="写下猫猫日常、求助问题、活动信息或好物说明。"
          value={draft.content}
          onChangeText={(content) => updateDraft({ content })}
          multiline
          style={styles.bodyInput}
        />
        <Input
          label="标签"
          placeholder="用逗号分隔，例如：M&D, 猫猫, 新手"
          value={draft.tagsRaw}
          onChangeText={(tagsRaw) => updateDraft({ tagsRaw })}
        />

        {err ? (
          <Txt kind="bodySmall" style={{ color: colors.error }}>
            {err}
          </Txt>
        ) : null}

        <Button title="发布动态" icon="send" loading={busy} onPress={publish} />
      </ScrollView>
    </View>
  );
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
  });
}
