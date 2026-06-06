import {useCallback, useEffect, useMemo, useState} from 'react';
import {ActivityIndicator, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View} from 'react-native';
import {useLocalSearchParams} from 'expo-router';
import {api, HttpError, type Message, type User} from '@/api';
import {useAuth} from '@/auth';
import {Button, Input, Txt} from '@/components';
import {type MndColors, radius, spacing, typography, useMndTheme} from '@/theme';

export default function ConversationScreen() {
  const { peerId: rawPeer } = useLocalSearchParams<{ peerId: string }>();
  const peerId = parseInt(String(rawPeer || ''), 10);
  const { user } = useAuth();
  const { colors } = useMndTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const [peer, setPeer] = useState<User | null>(null);
  const [messages, setMessages] = useState<Message[] | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [content, setContent] = useState('');
  const [sending, setSending] = useState(false);

  const load = useCallback(async () => {
    if (!Number.isFinite(peerId) || peerId <= 0) {
      setErr('无效的用户 ID');
      return;
    }
    setErr(null);
    try {
      const data = await api.messages.withPeer(peerId);
      setPeer(data.peer);
      setMessages(data.messages ?? []);
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('暂时无法加载对话');
    }
  }, [peerId]);

  useEffect(() => {
    load();
  }, [load]);

  const send = async () => {
    const text = content.trim();
    if (!text || !Number.isFinite(peerId) || peerId <= 0) return;
    setSending(true);
    try {
      await api.messages.send({ recipient_id: peerId, content: text });
      setContent('');
      await load();
    } catch (e) {
      if (e instanceof HttpError) setErr(e.payload.message);
      else setErr('发送失败，请稍后再试');
    } finally {
      setSending(false);
    }
  };

  if (!Number.isFinite(peerId) || peerId <= 0) {
    return (
      <View style={styles.centered}>
        <Txt kind="h3">无效会话</Txt>
        <Txt muted>请从私信列表、市集或动态详情进入。</Txt>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={88}>
      {err ? (
        <View style={[styles.banner, { margin: spacing.lg }]}>
          <Txt style={{ color: colors.error }}>{err}</Txt>
          <Button title="重试" variant="ghost" onPress={load} />
        </View>
      ) : null}

      <ScrollView style={styles.flex} contentContainerStyle={styles.thread} keyboardShouldPersistTaps="handled">
        {peer ? (
          <View style={{ marginBottom: spacing.md }}>
            <Txt kind="h2">{peer.nickname || peer.username}</Txt>
            <Txt muted>@{peer.username}</Txt>
          </View>
        ) : null}
        {messages === null ? (
          <ActivityIndicator color={colors.onSurface} style={{ marginTop: spacing.xl }} />
        ) : messages.length === 0 ? (
          <Txt muted>还没有消息，先打个招呼吧。</Txt>
        ) : (
          messages.map((message) => {
            const mine = user && message.sender_id === user.id;
            return (
              <View key={message.id} style={[styles.bubbleWrap, mine ? styles.bubbleWrapMine : styles.bubbleWrapTheirs]}>
                <View style={[styles.bubble, mine ? styles.bubbleMine : styles.bubbleTheirs]}>
                  <Txt style={mine ? styles.bubbleTextMine : undefined}>{message.content}</Txt>
                  <Txt kind="bodySmall" muted style={{ marginTop: spacing.s1 }}>
                    {new Date(message.created_at).toLocaleString()}
                  </Txt>
                </View>
              </View>
            );
          })
        )}
      </ScrollView>

      <View style={styles.composer}>
        <Input placeholder="写点什么..." value={content} onChangeText={setContent} multiline style={styles.input} />
        <Button title="发送" icon="send" loading={sending} onPress={send} disabled={!content.trim()} />
      </View>
    </KeyboardAvoidingView>
  );
}

function makeStyles(colors: MndColors) {
  return StyleSheet.create({
    flex: {
      flex: 1,
      backgroundColor: colors.canvas,
    },
    centered: {
      flex: 1,
      backgroundColor: colors.canvas,
      padding: spacing.lg,
      justifyContent: 'center',
      gap: spacing.sm,
    },
    banner: {
      gap: spacing.sm,
    },
    thread: {
      padding: spacing.lg,
      gap: spacing.sm,
      flexGrow: 1,
    },
    bubbleWrap: {
      flexDirection: 'row',
    },
    bubbleWrapMine: {
      justifyContent: 'flex-end',
    },
    bubbleWrapTheirs: {
      justifyContent: 'flex-start',
    },
    bubble: {
      maxWidth: '88%',
      padding: spacing.md,
      borderRadius: radius.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    bubbleMine: {
      backgroundColor: colors.primaryContainer,
    },
    bubbleTheirs: {
      backgroundColor: colors.surface,
    },
    bubbleTextMine: {
      color: colors.onPrimary,
      ...typography.body,
    },
    composer: {
      padding: spacing.lg,
      gap: spacing.sm,
      borderTopWidth: StyleSheet.hairlineWidth,
      borderTopColor: colors.border,
      backgroundColor: colors.surface,
    },
    input: {
      minHeight: 44,
      maxHeight: 120,
    },
  });
}
