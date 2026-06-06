import {useCallback, useEffect, useMemo, useState} from 'react';
import {Alert, Image, Platform, Pressable, RefreshControl, ScrollView, StyleSheet, View} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {router} from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import {hrefCompose, hrefPostDetail} from '@/href';
import {api, HttpError, type Post, resolveMediaUrl} from '@/api';
import {useAuth} from '@/auth';
import {Button, Card, Pill, Txt} from '@/components';
import {StitchTopBar} from '@/stitch';
import {type MndColors, radius, spacing, themeOrder, themes, useMndTheme} from '@/theme';

const PROFILE_BG_KEY = 'mnd.mobile.profileBackground';

const PROFILE_BACKGROUNDS = {
  picnic: {
    label: 'Picnic',
    note: '野餐毯和晒太阳的猫猫',
  },
  desk: {
    label: 'Desk',
    note: '书桌边的键盘搭子',
  },
  arcade: {
    label: 'Arcade',
    note: '夜间游戏感名片',
  },
  garden: {
    label: 'Garden',
    note: '花园角落，doggie 友好',
  },
} as const;

type ProfileBackgroundName = keyof typeof PROFILE_BACKGROUNDS;
type ProfileBackgroundStyle = { color: string; border: string; text: string; pillTone: 'neutral' | 'brand' };

function isProfileBackground(value: string | null): value is ProfileBackgroundName {
  return !!value && value in PROFILE_BACKGROUNDS;
}

function getWebStorage():
  | { getItem: (key: string) => string | null; setItem: (key: string, value: string) => void }
  | null {
  const maybeGlobal = globalThis as {
    localStorage?: { getItem: (key: string) => string | null; setItem: (key: string, value: string) => void };
  };
  return maybeGlobal.localStorage ?? null;
}

async function readProfileBackground(): Promise<ProfileBackgroundName | null> {
  if (Platform.OS === 'web') {
    try {
      const value = getWebStorage()?.getItem(PROFILE_BG_KEY) ?? null;
      return isProfileBackground(value) ? value : null;
    } catch {
      return null;
    }
  }
  const value = await SecureStore.getItemAsync(PROFILE_BG_KEY).catch(() => null);
  return isProfileBackground(value) ? value : null;
}

async function writeProfileBackground(name: ProfileBackgroundName) {
  if (Platform.OS === 'web') {
    try {
      getWebStorage()?.setItem(PROFILE_BG_KEY, name);
    } catch {
      /* ignore */
    }
    return;
  }
  await SecureStore.setItemAsync(PROFILE_BG_KEY, name).catch(() => null);
}

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const { colors, themeName, setThemeName, cycleTheme } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [posts, setPosts] = useState<Post[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [profileBackground, setProfileBackgroundState] = useState<ProfileBackgroundName>('picnic');
  const activeBackground = PROFILE_BACKGROUNDS[profileBackground];
  const backgroundStyles = useMemo<Record<ProfileBackgroundName, ProfileBackgroundStyle>>(
    () => ({
      picnic: {
        color: colors.secondaryContainer,
        border: colors.outlineVariant,
        text: colors.onSurfaceVariant,
        pillTone: 'neutral',
      },
      desk: {
        color: colors.surfaceLow,
        border: colors.border,
        text: colors.onSurfaceVariant,
        pillTone: 'neutral',
      },
      arcade: {
        color: colors.surfaceContainer,
        border: colors.primaryContainer,
        text: colors.onSurface,
        pillTone: 'brand',
      },
      garden: {
        color: colors.tertiaryContainer,
        border: colors.borderMedium,
        text: colors.onSurfaceVariant,
        pillTone: 'neutral',
      },
    }),
    [colors],
  );
  const activeBackgroundStyle = backgroundStyles[profileBackground];

  const load = useCallback(async () => {
    setErr(null);
    try {
      setPosts(await api.posts.my());
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法加载个人动态');
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    let active = true;
    readProfileBackground().then((stored) => {
      if (active && stored) setProfileBackgroundState(stored);
    });
    return () => {
      active = false;
    };
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await load();
    } finally {
      setRefreshing(false);
    }
  }, [load]);

  const onLogout = async () => {
    await logout();
    router.replace('/(auth)/login');
  };

  const setProfileBackground = (name: ProfileBackgroundName) => {
    setProfileBackgroundState(name);
    writeProfileBackground(name);
  };

  const avatarUri = resolveMediaUrl(user?.avatar_url);

  return (
    <View style={styles.root}>
      <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.safe}>
        <StitchTopBar
          user={user}
          onAvatarPress={() => Alert.alert('我的', '已经在个人主页')}
          onNotifyPress={() => Alert.alert('通知', '暂无新通知')}
          onThemePress={cycleTheme}
        />
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          <View
            style={[
              styles.profileCover,
              { backgroundColor: activeBackgroundStyle.color, borderColor: activeBackgroundStyle.border },
            ]}
          >
            <View style={styles.coverMeta}>
              <Pill tone={activeBackgroundStyle.pillTone}>{activeBackground.label}</Pill>
              <Txt kind="label" style={{ color: activeBackgroundStyle.text }}>
                {activeBackground.note}
              </Txt>
            </View>
            <View style={styles.header}>
              {avatarUri ? (
                <Image source={{ uri: avatarUri }} style={styles.avatar} accessibilityLabel="我的头像" />
              ) : (
                <View style={[styles.avatar, styles.avatarFallback]}>
                  <Txt kind="h2" style={{ color: colors.onPrimary }}>
                    M
                  </Txt>
                </View>
              )}
              <View style={{ flex: 1 }}>
                <Txt kind="h1">{user?.nickname || user?.username || '未登录用户'}</Txt>
                <Txt kind="bodySmall" muted>
                  @{user?.username || 'guest'}
                </Txt>
                <Txt muted style={{ marginTop: spacing.sm }}>
                  {user?.bio || '猫猫优先，doggie 友好。'}
                </Txt>
              </View>
            </View>
          </View>

          <View style={styles.stats}>
            <View style={styles.stat}>
              <Txt kind="h2">{posts?.length ?? '-'}</Txt>
              <Txt kind="label" muted>
                动态
              </Txt>
            </View>
            <View style={styles.stat}>
              <Txt kind="h2">0</Txt>
              <Txt kind="label" muted>
                关注
              </Txt>
            </View>
            <View style={styles.stat}>
              <Txt kind="h2">0</Txt>
              <Txt kind="label" muted>
                粉丝
              </Txt>
            </View>
          </View>

          <Card>
            <View style={styles.rowBetween}>
              <View>
                <Txt kind="h3">主题</Txt>
                <Txt kind="bodySmall" muted>
                  Honey / Mint / Night / Neutral
                </Txt>
              </View>
              <Pill tone="brand">{themes[themeName].label}</Pill>
            </View>
            <View style={styles.themeRow}>
              {themeOrder.map((name) => {
                const palette = themes[name].colors;
                const on = name === themeName;
                return (
                  <Pressable
                    key={name}
                    onPress={() => setThemeName(name)}
                    accessibilityRole="button"
                    accessibilityLabel={`切换到 ${themes[name].label}`}
                    accessibilityState={{ selected: on }}
                    style={[styles.themeButton, on && styles.themeButtonOn]}
                  >
                    <View style={[styles.themeSwatch, { backgroundColor: palette.primaryContainer }]} />
                    <Txt kind="label" style={on ? styles.themeLabelOn : styles.themeLabel}>
                      {themes[name].label}
                    </Txt>
                  </Pressable>
                );
              })}
            </View>
          </Card>

          <Card>
            <View>
              <Txt kind="h3">个人背景</Txt>
              <Txt kind="bodySmall" muted>
                Picnic / Desk / Arcade / Garden
              </Txt>
            </View>
            <View style={styles.themeRow}>
              {Object.entries(PROFILE_BACKGROUNDS).map(([name, background]) => {
                const key = name as ProfileBackgroundName;
                const on = key === profileBackground;
                const swatchStyle = backgroundStyles[key];
                return (
                  <Pressable
                    key={key}
                    onPress={() => setProfileBackground(key)}
                    accessibilityRole="button"
                    accessibilityLabel={`切换到 ${background.label} 背景`}
                    accessibilityState={{ selected: on }}
                    style={[styles.themeButton, on && styles.themeButtonOn]}
                  >
                    <View
                      style={[
                        styles.themeSwatch,
                        { backgroundColor: swatchStyle.color, borderColor: swatchStyle.border },
                      ]}
                    />
                    <Txt kind="label" style={on ? styles.themeLabelOn : styles.themeLabel}>
                      {background.label}
                    </Txt>
                  </Pressable>
                );
              })}
            </View>
          </Card>

          <View style={styles.rowBetween}>
            <Txt kind="h3">我的动态</Txt>
            <Button title="发布" icon="add" variant="ghost" onPress={() => router.push(hrefCompose())} />
          </View>

          {err ? (
            <Card>
              <Txt kind="h3">加载失败</Txt>
              <Txt muted>{err}</Txt>
              <Button title="重试" variant="secondary" onPress={load} />
            </Card>
          ) : posts === null ? (
            <Card>
              <Txt muted>正在加载个人动态...</Txt>
            </Card>
          ) : posts.length === 0 ? (
            <Card>
              <Txt muted>还没有发布过动态。</Txt>
              <Button title="去发布" variant="secondary" onPress={() => router.push(hrefCompose())} />
            </Card>
          ) : (
            <View style={styles.grid}>
              {posts.map((post) => (
                <Pressable
                  key={post.id}
                  style={styles.gridItem}
                  onPress={() => router.push(hrefPostDetail(post.id))}
                  accessibilityRole="button"
                  accessibilityLabel={`打开动态：${post.title}`}
                >
                  <View style={styles.gridCover}>
                    <Txt kind="label" style={{ color: colors.primaryContainer }}>
                      M&D
                    </Txt>
                  </View>
                  <Txt kind="label" numberOfLines={2}>
                    {post.title}
                  </Txt>
                  <Txt kind="bodySmall" muted numberOfLines={2} style={{ marginTop: spacing.xs }}>
                    {post.content}
                  </Txt>
                </Pressable>
              ))}
            </View>
          )}

          <Card>
            <Txt kind="h3">设置</Txt>
            <Txt muted>资料编辑、隐私与安全还需要继续对齐；背景已经支持本地切换。</Txt>
            <Button title="退出登录" icon="logout" variant="danger" onPress={onLogout} />
          </Card>
        </ScrollView>
      </SafeAreaView>
    </View>
  );
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
    header: {
      flexDirection: 'row',
      gap: spacing.lg,
      alignItems: 'flex-start',
      backgroundColor: colors.tabBar,
      borderRadius: radius.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      padding: spacing.md,
    },
    profileCover: {
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      padding: spacing.lg,
      gap: spacing.md,
    },
    coverMeta: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: spacing.md,
      flexWrap: 'wrap',
    },
    avatar: {
      width: 76,
      height: 76,
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.borderMedium,
    },
    avatarFallback: {
      backgroundColor: colors.primaryContainer,
      alignItems: 'center',
      justifyContent: 'center',
    },
    stats: {
      flexDirection: 'row',
      backgroundColor: colors.surface,
      borderRadius: radius.xl,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      paddingVertical: spacing.lg,
    },
    stat: {
      flex: 1,
      alignItems: 'center',
      gap: spacing.xs,
    },
    rowBetween: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: spacing.md,
    },
    themeRow: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
    themeButton: {
      minWidth: '47%',
      flexGrow: 1,
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      paddingHorizontal: spacing.md,
      paddingVertical: spacing.sm,
      borderRadius: radius.lg,
      backgroundColor: colors.surfaceLow,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    themeButtonOn: {
      backgroundColor: colors.accentSoft,
      borderColor: colors.borderMedium,
    },
    themeSwatch: {
      width: 18,
      height: 18,
      borderRadius: radius.pill,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    themeLabel: {
      color: colors.onSurfaceVariant,
    },
    themeLabelOn: {
      color: colors.primaryContainer,
    },
    grid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: spacing.sm,
    },
    gridItem: {
      width: '48%',
      flexGrow: 1,
      backgroundColor: colors.surface,
      borderRadius: radius.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
      overflow: 'hidden',
      minHeight: 150,
    },
    gridCover: {
      height: 70,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.accentSoft,
      marginBottom: spacing.sm,
    },
  });
}
