import { useEffect, useRef, useState } from "react";
import {
  api,
  explain,
  HttpError,
  type Inbox,
  type Page,
  type RecordData,
} from "../core/api";
import {
  clearDraft,
  newDraft,
  readDraft,
  saveDraft,
  type Draft,
  type PhotoDraft,
} from "../core/drafts";
import { useQuery, reload } from "../core/query";
import { useSession } from "../core/session";
import { navigate } from "../core/router";
import { encodeWire, integer, key } from "../core/wire";
import { preparePhoto } from "../core/photos";
import {
  Button,
  Card,
  Choices,
  Icon,
  Modal,
  Notice,
  PrivateImage,
  QueryState,
} from "../components/ui";
import { Author } from "./Records";
import { Attachments } from "../components/Attachments";
import { workflowURL } from "./workflows";

interface Emoji {
  id: string;
  text: string;
  label: string;
  asset_url: string;
}
interface Sticker {
  id: string;
  label: string;
  column: bigint;
  row: bigint;
}
interface Catalog {
  emoji: Emoji[];
  stickers: Sticker[];
}
interface Expressions {
  favorites: string[];
  recent: string[];
}
function StickerImage({ item }: { item: Sticker }) {
  return (
    <span
      role="img"
      aria-label={item.label}
      className="sticker"
      style={{
        backgroundPosition: `${Number(item.column) * 20}% ${(Number(item.row) * 100) / 3}%`,
      }}
    />
  );
}
export function RichText({ content }: { content: Record<string, unknown> }) {
  const catalog = useQuery<Catalog>("expression-catalog");
  const selected = catalog.data?.stickers.find(
    (item) => item.id === content.sticker_id,
  );
  return (
    <div className="stack">
      {content.text ? <p className="prose">{String(content.text)}</p> : null}
      {selected ? (
        <StickerImage item={selected} />
      ) : content.sticker_id ? (
        <span className="muted">此表情暂不可显示</span>
      ) : null}
    </div>
  );
}
/** 输入、照片及原发送编号按账号＋对象隔离；恢复只载入，不会触发发送。 */
export function Composer({
  kind,
  id,
  reply = 0n,
  onSent,
  canSend = true,
  allowMedia = true,
}: {
  kind: "entries" | "conversations" | "groups";
  id: bigint;
  reply?: bigint;
  onSent: () => void;
  canSend?: boolean;
  allowMedia?: boolean;
}) {
  const owner = useSession().user?.id ?? 0n;
  const route = `/${kind}/${id}/composer`;
  const [draft, setDraft] = useState<Draft>();
  const latest = useRef<Draft | undefined>(undefined);
  const revision = useRef(0);
  const queue = useRef(Promise.resolve());
  const blocked = useRef(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [picker, setPicker] = useState(false);
  const [expressionTab, setExpressionTab] = useState("emoji");
  const [preview, setPreview] = useState<Sticker>();
  const [composing, setComposing] = useState(false);
  const [guestDraft, setGuestDraft] = useState<Draft>();
  const textarea = useRef<HTMLTextAreaElement>(null);
  const composingRef = useRef(false);
  const catalog = useQuery<Catalog>("expression-catalog");
  const expressions = useQuery<Page<Expressions>>(owner ? "expressions" : null);
  const expression = expressions.data?.items[0];
  const qualification = useQuery<{
    interaction: boolean;
    contract_version: bigint;
  }>(owner ? "eligibility" : null);
  const allowed =
    !!owner &&
    canSend &&
    qualification.data?.contract_version === 1n &&
    qualification.data.interaction;
  useEffect(() => {
    let active = true;
    setDraft(undefined);
    latest.current = undefined;
    setGuestDraft(undefined);
    blocked.current = false;
    setError("");
    void readDraft(owner, route)
      .then((saved) => {
        if (!active) return;
        const d = saved ?? newDraft(owner, route, { text: "", cursor: 0 });
        latest.current = d;
        revision.current = d.revision;
        setDraft(d);
      })
      .catch((e) => {
        if (active) setError(explain(e));
      });
    if (owner && kind === "entries")
      void readDraft(0n, route)
        .then((saved) => {
          if (
            active &&
            saved &&
            !saved.uncertain &&
            (saved.values.text || saved.photos.length)
          )
            setGuestDraft(saved);
        })
        .catch((e) => {
          if (active) setError(explain(e));
        });
    return () => {
      active = false;
    };
  }, [owner, route]);
  async function persist() {
    const epoch = api.epoch;
    const identity = latest.current?.key;
    const next = queue.current
      .catch(() => {})
      .then(async () => {
        if (
          blocked.current ||
          !latest.current ||
          epoch !== api.epoch ||
          identity !== latest.current.key
        )
          throw new Error("请核对当前账号和已保存草稿");
        const saved = await saveDraft({
          ...latest.current,
          revision: revision.current,
        });
        revision.current = saved.revision;
        if (latest.current?.key === identity) {
          latest.current = { ...latest.current, revision: saved.revision };
          setDraft(latest.current);
        }
      });
    queue.current = next;
    try {
      await next;
    } catch (e) {
      blocked.current = true;
      throw e;
    }
  }
  function update(change: (d: Draft) => Draft) {
    if (!latest.current || busy || latest.current.uncertain || blocked.current)
      return;
    latest.current = change(latest.current);
    setDraft(latest.current);
    void persist().catch((e) => setError(explain(e)));
  }
  function insert(text: string) {
    if (composingRef.current || !latest.current) return;
    const element = textarea.current;
    const old = String(latest.current.values.text ?? "");
    const boundaries = [
      0,
      ...Array.from(
        new Intl.Segmenter("zh", { granularity: "grapheme" }).segment(old),
        (s) => s.index + s.segment.length,
      ),
    ];
    const boundary = (v: number) =>
      boundaries.reduce((a, b) => (b <= v ? b : a), 0);
    const start = boundary(element?.selectionStart ?? old.length);
    const end = boundary(element?.selectionEnd ?? old.length);
    const value = old.slice(0, start) + text + old.slice(end);
    const cursor = start + text.length;
    update((d) => ({
      ...d,
      values: { ...d.values, text: value, cursor },
      requestId: key(),
    }));
    requestAnimationFrame(() => {
      element?.focus();
      element?.setSelectionRange(cursor, cursor);
    });
  }
  async function select(files: FileList | null) {
    if (!files || !latest.current || busy || !allowMedia) return;
    const current = latest.current;
    const epoch = api.epoch;
    setBusy(true);
    try {
      const photos: PhotoDraft[] = [];
      for (const file of Array.from(files).slice(0, 9 - current.photos.length))
        photos.push(await preparePhoto(file, "private"));
      if (epoch !== api.epoch || latest.current?.key !== current.key) return;
      latest.current = {
        ...current,
        photos: [...current.photos, ...photos],
      };
      setDraft(latest.current);
      await persist();
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  async function upload(photo: PhotoDraft) {
    if (!photo.blob || busy || !latest.current || !allowMedia) return;
    const current = latest.current;
    const epoch = api.epoch;
    setBusy(true);
    try {
      await persist();
      const form = new FormData();
      form.append("scope", "private");
      form.append("file", photo.blob, photo.name);
      const media = await api.request<RecordData>("media", {
        method: "POST",
        headers: { "Idempotency-Key": photo.key },
        body: form,
        owner,
      });
      if (epoch !== api.epoch || latest.current?.key !== current.key) return;
      latest.current = {
        ...latest.current,
        photos: latest.current.photos.map((p) =>
          p.key === photo.key
            ? { ...p, mediaId: media.id, blob: undefined }
            : p,
        ),
      };
      setDraft(latest.current);
      await persist();
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  async function rememberExpression(item: string, favorite = false) {
    if (!owner) return;
    try {
      const old = expression?.data ?? { favorites: [], recent: [] };
      const next = favorite
        ? {
            ...old,
            favorites: old.favorites.includes(item)
              ? old.favorites.filter((x) => x !== item)
              : [...old.favorites, item].slice(0, 100),
          }
        : {
            ...old,
            recent: [item, ...old.recent.filter((x) => x !== item)].slice(
              0,
              40,
            ),
          };
      await api.request("expressions", {
        method: "PUT",
        body: { version: expression?.version ?? 0n, expressions: next },
        owner,
      });
      void reload("expressions");
    } catch (e) {
      setError(explain(e));
    }
  }
  async function send(sticker?: Sticker) {
    if (
      !allowed ||
      busy ||
      composingRef.current ||
      !latest.current ||
      blocked.current ||
      latest.current.uncertain
    )
      return;
    setBusy(true);
    setError("");
    let sent = false;
    try {
      const current = latest.current;
      const text = sticker ? "" : String(current.values.text ?? "");
      if (!allowMedia && sticker)
        throw new Error(
          "消息请求只能发送第一条文字，请先保留照片或等待对方接受",
        );
      if (!sticker && !text.trim() && (!allowMedia || !current.photos.length))
        throw new Error("请先输入内容或选择照片");
      if (!sticker && allowMedia && current.photos.some((p) => !p.mediaId))
        throw new Error("请先逐张完成上传");
      const body =
        kind === "entries"
          ? {
              content: { text, sticker_id: sticker?.id ?? "" },
              reply_to: reply,
              idempotency_key: current.requestId,
            }
          : {
              content: { text, sticker_id: sticker?.id ?? "" },
              media_ids:
                sticker || !allowMedia
                  ? []
                  : current.photos.map((p) => p.mediaId),
              idempotency_key: current.requestId,
            };
      const path = `${kind}/${id}/${kind === "entries" ? "comments" : "messages"}`;
      latest.current = {
        ...current,
        uncertain: true,
        original: {
          path,
          method: "POST",
          body: encodeWire(body),
          replayable: true,
        },
      };
      setDraft(latest.current);
      await persist();
      sent = true;
      await api.request(path, { method: "POST", body, owner });
      if (latest.current?.key !== current.key || api.session.user?.id !== owner)
        return;
      const cleared = await clearDraft(latest.current!);
      const next =
        sticker || !allowMedia
          ? await saveDraft({
              ...cleared,
              values: sticker
                ? current.values
                : { ...current.values, text: "", cursor: 0 },
              photos: current.photos,
            })
          : cleared;
      latest.current = next;
      revision.current = next.revision;
      setDraft(next);
      setPreview(undefined);
      onSent();
      if (sticker) void rememberExpression(sticker.id);
    } catch (e) {
      if (
        !sent ||
        (e instanceof HttpError &&
          [400, 401, 403, 404, 409, 422, 429].includes(e.status))
      ) {
        latest.current = {
          ...latest.current!,
          uncertain: false,
          original: undefined,
        };
        setDraft(latest.current);
        await persist().catch(() => {});
      }
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  const disabled = busy || !draft || !!draft.uncertain || blocked.current;
  return (
    <div className="composer">
      {!allowed ? (
        <Notice
          title={
            !owner
              ? "登录后继续交流"
              : !canSend
                ? "当前会话尚不能发送内容"
                : "请先在账号安全中核对交流资格"
          }
        >
          <Button kind="quiet" onClick={() => navigate("/security")}>
            查看账号安全
          </Button>
        </Notice>
      ) : null}
      {error ? <Notice title={error} error /> : null}
      {!allowMedia && draft?.photos.length ? (
        <Notice title="这次请求只发送文字">
          已选照片保留在原草稿，等待对方接受后再由你确认发送。
        </Notice>
      ) : null}
      {guestDraft && draft && !draft.uncertain ? (
        <Notice title="此条日常有登录前保留的文字">
          <Button
            disabled={disabled}
            onClick={() => {
              if (
                String(draft.values.text ?? "").trim() &&
                !confirm("用登录前的文字替换当前输入？")
              )
                return;
              update((d) => ({
                ...d,
                values: {
                  ...d.values,
                  text: guestDraft.values.text,
                  cursor: guestDraft.values.cursor,
                },
                requestId: key(),
              }));
              setGuestDraft(undefined);
            }}
          >
            明确载入登录前文字，暂不发送
          </Button>
        </Notice>
      ) : null}
      {draft?.uncertain ? (
        <Notice title="原发送结果待核对">
          <Button onClick={() => navigate("/recovery")}>
            按原编号核对，暂不重复发送
          </Button>
        </Notice>
      ) : null}
      <div className="composer-row">
        <textarea
          aria-label={kind === "entries" ? "说点什么…" : "发送消息…"}
          ref={textarea}
          rows={1}
          maxLength={2000}
          value={String(draft?.values.text ?? "")}
          disabled={disabled}
          onChange={(e) => {
            const text = e.target.value;
            const cursor = e.target.selectionStart;
            update((d) => ({
              ...d,
              values: { ...d.values, text, cursor },
              requestId: key(),
            }));
          }}
          onCompositionStart={() => {
            composingRef.current = true;
            setComposing(true);
          }}
          onCompositionEnd={() => {
            composingRef.current = false;
            setComposing(false);
          }}
          onSelect={(e) => {
            if (latest.current && !latest.current.uncertain)
              latest.current.values.cursor = e.currentTarget.selectionStart;
          }}
        />
        <button
          className="toolbar-icon"
          type="button"
          aria-label="打开表情"
          aria-expanded={picker}
          disabled={disabled || composing}
          onClick={() => setPicker(!picker)}
        >
          <Icon name="smile" />
        </button>
        <Button
          kind="primary"
          disabled={disabled || !allowed || composing}
          onClick={() => void send()}
        >
          发送
        </Button>
      </div>
      {kind !== "entries" && draft ? (
        <Attachments
          photos={draft.photos}
          disabled={disabled || !canSend || !allowMedia}
          onSelect={(files) => void select(files)}
          onUpload={(photo) => void upload(photo)}
          onRemove={(photoKey) =>
            update((d) => ({
              ...d,
              photos: d.photos.filter((p) => p.key !== photoKey),
            }))
          }
          onMove={(index, direction) =>
            update((d) => {
              const photos = [...d.photos];
              [photos[index + direction], photos[index]] = [
                photos[index],
                photos[index + direction],
              ];
              return { ...d, photos };
            })
          }
        />
      ) : null}
      {picker ? (
        <Card title="表情与贴纸">
          <p className="caption muted">
            Unicode 表情插入当前光标；贴纸先预览，再由你确认发送。
          </p>
          <Choices
            label="表情分类"
            value={expressionTab}
            onChange={setExpressionTab}
            choices={[
              ["emoji", "表情"],
              ["stickers", "贴纸"],
              ["recent", "最近"],
              ["favorites", "收藏"],
            ]}
          />
          {["recent", "favorites"].includes(expressionTab) &&
          !expression?.data[expressionTab as "recent" | "favorites"]?.length ? (
            <p className="muted">这里还没有表情，发送或收藏后会显示。</p>
          ) : null}
          <div className="expression-grid">
            {(expressionTab === "stickers"
              ? []
              : expressionTab === "emoji"
                ? catalog.data?.emoji
                : expression?.data[expressionTab as "recent" | "favorites"]
                    .map((id) =>
                      catalog.data?.emoji.find((emoji) => emoji.id === id),
                    )
                    .filter((item): item is Emoji => !!item)
            )?.map((emoji) => (
              <div key={emoji.id} className="expression-cell">
                <button
                  type="button"
                  title={emoji.label}
                  disabled={disabled || composing}
                  onClick={() => {
                    insert(emoji.text);
                    void rememberExpression(emoji.id);
                  }}
                >
                  {emoji.text}
                </button>
                <button
                  type="button"
                  className="expression-save"
                  aria-label={`${expression?.data.favorites.includes(emoji.id) ? "取消收藏" : "收藏"}${emoji.label}`}
                  aria-pressed={
                    expression?.data.favorites.includes(emoji.id) ?? false
                  }
                  disabled={disabled || !owner}
                  onClick={() => void rememberExpression(emoji.id, true)}
                >
                  {expression?.data.favorites.includes(emoji.id)
                    ? "已收藏"
                    : "收藏"}
                </button>
              </div>
            ))}
          </div>
          <div className="expression-grid">
            {(expressionTab === "emoji"
              ? []
              : expressionTab === "stickers"
                ? catalog.data?.stickers
                : expression?.data[expressionTab as "recent" | "favorites"]
                    .map((id) =>
                      catalog.data?.stickers.find(
                        (sticker) => sticker.id === id,
                      ),
                    )
                    .filter((item): item is Sticker => !!item)
            )?.map((sticker) => (
              <button
                type="button"
                key={sticker.id}
                aria-label={`预览${sticker.label}`}
                disabled={disabled || composing}
                onClick={() => setPreview(sticker)}
              >
                <StickerImage item={sticker} />
              </button>
            ))}
          </div>
        </Card>
      ) : null}
      {preview ? (
        <Modal
          title={preview.label}
          busy={busy}
          onClose={() => setPreview(undefined)}
          actions={
            <Button
              kind="primary"
              disabled={disabled || !allowed || composing || !allowMedia}
              onClick={() => void send(preview)}
            >
              确认发送此贴纸
            </Button>
          }
        >
          <StickerImage item={preview} />
          <Button
            onClick={() => void rememberExpression(preview.id, true)}
            disabled={busy}
          >
            {expression?.data.favorites.includes(preview.id)
              ? "取消收藏"
              : "收藏此表情"}
          </Button>
          <p className="muted caption">
            当前文字和照片会保留；这次仅发送所选贴纸。
          </p>
        </Modal>
      ) : null}
    </div>
  );
}
export function Chat({
  kind,
  id,
}: {
  kind: "groups" | "conversations";
  id: bigint;
}) {
  const owner = useSession().user?.id ?? 0n;
  const path = `${kind}/${id}`;
  const parent = useQuery<RecordData>(path);
  const summary = useQuery<Inbox>("inbox-summary");
  const child = kind === "groups" ? "group_messages" : "messages";
  const first =
    (kind === "groups"
      ? summary.data?.group_first_unread
      : summary.data?.direct_first_unread)?.[String(id)] ?? 0n;
  const [cursor, setCursor] = useState(0n);
  const messagesPath = `${child}?reference_id=${id}&cursor=${cursor}`;
  const messages = useQuery<Page>(messagesPath);
  const [loaded, setLoaded] = useState<RecordData[]>([]);
  const [error, setError] = useState("");
  const visible = useRef<HTMLDivElement>(null);
  const through = useRef(0n);
  const reading = useRef(false);
  const [mark, setMark] = useState(first);
  useEffect(() => {
    if (messages.denied || parent.denied) setLoaded([]);
  }, [messages.denied, parent.denied]);
  useEffect(() => {
    if (!messages.data) return;
    setLoaded((old) => {
      const rows = new Map((cursor ? old : []).map((row) => [row.id, row]));
      for (const row of messages.data!.items) rows.set(row.id, row);
      return [...rows.values()].sort((a, b) =>
        a.id < b.id ? -1 : a.id > b.id ? 1 : 0,
      );
    });
  }, [messages.data, cursor]);
  useEffect(() => {
    const node = visible.current;
    if (!node || !parent.data) return;
    let active = true;
    const epoch = api.epoch;
    const inView = new Set<bigint>();
    const readVisible = () => {
      if (
        document.visibilityState !== "visible" ||
        !document.hasFocus() ||
        !active
      )
        return;
      const highest = [...inView].reduce((a, b) => (a > b ? a : b), 0n);
      if (highest <= through.current || reading.current) return;
      reading.current = true;
      void api
        .request(`${path}/read`, {
          method: "POST",
          body: { through_message_id: highest },
          owner,
        })
        .then(() => {
          if (active && api.epoch === epoch) {
            through.current = highest;
            void reload("inbox-summary");
          }
        })
        .catch((e) => {
          if (active) setError(explain(e));
        })
        .finally(() => {
          reading.current = false;
        });
    };
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const value = BigInt((entry.target as HTMLElement).dataset.message!);
          if (entry.isIntersecting && entry.intersectionRatio >= 0.6)
            inView.add(value);
          else inView.delete(value);
        }
        readVisible();
      },
      { root: node, threshold: [0.6] },
    );
    for (const child of node.querySelectorAll("[data-message]"))
      observer.observe(child);
    document.addEventListener("visibilitychange", readVisible);
    window.addEventListener("focus", readVisible);
    return () => {
      active = false;
      observer.disconnect();
      document.removeEventListener("visibilitychange", readVisible);
      window.removeEventListener("focus", readVisible);
    };
  }, [loaded, path, parent.data, owner]);
  const p = parent.data;
  const data = p?.data;
  const member =
    kind === "groups" &&
    Array.isArray(data?.members) &&
    data.members.includes(owner);
  const canSend =
    !!p &&
    (kind === "groups"
      ? p.state === "active" && member
      : p.state === "accepted" ||
        (p.state === "pending" &&
          p.owner_id === owner &&
          !!messages.data &&
          !messages.loading &&
          loaded.length === 0));
  return (
    <section className="chat-page">
      <header className="task-toolbar">
        <h1>{kind === "groups" ? String(data?.name ?? "群聊") : "私信"}</h1>
        {kind === "groups" ? (
          <Button
            kind="quiet"
            onClick={() => navigate(`/groups/${id}?view=manage`)}
          >
            群资料与管理
          </Button>
        ) : null}
      </header>
      <QueryState
        loading={parent.loading}
        error={parent.error}
        onRetry={() => void reload(path)}
      >
        {kind === "groups" ? (
          <Notice
            title={`群公告 · ${Array.isArray(data?.members) ? data.members.length : "—"} 位成员`}
          >
            {String(data?.notice || "暂无公告")}
          </Notice>
        ) : p?.state === "pending" ? (
          <Notice title="这是一条消息请求">
            接受前只能发一条文字内容，不能发送图片。
            {integer(data?.recipient_id) === owner ? (
              <div className="actions-row">
                {["accept", "decline"].map((action) => (
                  <Button
                    key={action}
                    onClick={() =>
                      navigate(
                        workflowURL("state-action", kind, id, { action }),
                      )
                    }
                  >
                    {action === "accept" ? "核对并接受" : "核对并拒绝"}
                  </Button>
                ))}
              </div>
            ) : null}
          </Notice>
        ) : null}
        {first > 0n ? (
          <Button
            kind="quiet"
            onClick={() => {
              setMark(first);
              setLoaded([]);
              setCursor(first - 1n);
            }}
          >
            跳到首条未读 #{String(first)}
          </Button>
        ) : null}
        {cursor > 0n ? (
          <Button
            kind="quiet"
            onClick={() => {
              setLoaded([]);
              setCursor(0n);
            }}
          >
            从历史起点读取
          </Button>
        ) : null}
        {error ? <Notice title={error} error /> : null}
        <div className="chat-messages" ref={visible}>
          <QueryState
            loading={messages.loading}
            error={messages.error}
            onRetry={() => void reload(messagesPath)}
          >
            {loaded.map((row) => (
              <div
                key={String(row.id)}
                data-message={String(row.id)}
                className={`bubble ${row.owner_id === owner ? "mine" : ""}`}
              >
                {row.id === (mark || first) ? (
                  <p className="caption disclosure">以下是本次首条未读</p>
                ) : null}
                <Author id={row.owner_id} />
                <RichText content={row.data} />
                {row.media_ids?.map((media) => (
                  <PrivateImage key={String(media)} id={media} alt="会话照片" />
                ))}
              </div>
            ))}
            {messages.data?.next_cursor ? (
              <Button
                onClick={() => setCursor(messages.data!.next_cursor)}
                disabled={messages.loading}
              >
                继续读取消息
              </Button>
            ) : null}
          </QueryState>
        </div>
        <Composer
          kind={kind}
          id={id}
          canSend={canSend}
          allowMedia={kind === "groups" || p?.state === "accepted"}
          onSent={() => {
            void reload(messagesPath);
            void reload(path);
            void reload("inbox-summary");
          }}
        />
      </QueryState>
    </section>
  );
}
