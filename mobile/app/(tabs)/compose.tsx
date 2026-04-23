import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { api, HttpError } from '@/api';
import { Button, Input, Txt } from '@/components';
import { colors, radius, spacing } from '@/theme';

const CATS = [
  { value: 'daily_share', label: '日常' },
  { value: 'help', label: '求助' },
  { value: 'activity', label: '活动' },
  { value: 'trade', label: '交易' },
] as const;

export default function ComposeScreen() {
  const insets = useSafeAreaInsets();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState<string>('daily_share');
  const [tagsRaw, setTagsRaw] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const publish = async () => {
    setErr(null);
    if (!title.trim() || !content.trim()) {
      setErr('标题和内容都要填哦');
      return;
    }
    setBusy(true);
    try {
      const tags = tagsRaw
        .split(/[,，\s]+/)
        .map((t) => t.trim())
        .filter(Boolean);
      await api.posts.create({
        title: title.trim(),
        content: content.trim(),
        category,
        tags,
      });
      router.back();
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('发布失败');
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.root}>
      <View style={[styles.header, { paddingTop: Math.max(insets.top, spacing.md) }]}>
        <Pressable onPress={() => router.back()} style={styles.hBtn} accessibilityLabel="关闭">
          <MaterialIcons name="close" size={26} color={colors.onSurface} />
        </Pressable>
        <Txt kind="h2">MEOW</Txt>
        <Pressable onPress={publish} disabled={busy} style={styles.hBtn} accessibilityLabel="发布">
          <Txt kind="h3" style={{ color: colors.primaryContainer }}>
            发布
          </Txt>
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={[styles.body, { paddingBottom: Math.max(insets.bottom, spacing.lg) + 80 }]}
        keyboardShouldPersistTaps="handled"
      >
        <Input label="标题" placeholder="今天猫咪做了什么？" value={title} onChangeText={setTitle} />
        <Input
          label="正文"
          placeholder="写下来…"
          value={content}
          onChangeText={setContent}
          multiline
          style={{ minHeight: 140, textAlignVertical: 'top' }}
        />

        <Txt kind="label" muted>
          分类
        </Txt>
        <View style={styles.catRow}>
          {CATS.map((c) => {
            const on = c.value === category;
            return (
              <Pressable
                key={c.value}
                onPress={() => setCategory(c.value)}
                style={[styles.catChip, on && styles.catChipOn]}
              >
                <Txt kind="label" style={on ? styles.catOn : styles.catOff}>
                  {c.label}
                </Txt>
              </Pressable>
            );
          })}
        </View>

        <Input
          label="标签"
          placeholder="用逗号分隔，例如：英短,零食"
          value={tagsRaw}
          onChangeText={setTagsRaw}
        />

        {err ? (
          <Txt kind="bodySmall" style={{ color: colors.error }}>
            {err}
          </Txt>
        ) : null}

        <Button title="发布动态" loading={busy} onPress={publish} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
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
    borderBottomColor: 'rgba(255, 90, 119, 0.1)',
    backgroundColor: colors.canvas,
  },
  hBtn: {
    minWidth: 48,
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  body: {
    padding: spacing.lg,
    gap: spacing.md,
  },
  catRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  catChip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    backgroundColor: colors.surface,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
  },
  catChipOn: {
    backgroundColor: colors.brandWeak,
    borderColor: 'rgba(255, 90, 119, 0.35)',
  },
  catOn: {
    color: colors.primaryContainer,
  },
  catOff: {
    color: colors.onSurfaceVariant,
  },
});
