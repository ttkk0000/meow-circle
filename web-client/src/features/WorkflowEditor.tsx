import { useEffect, useRef, useState } from "react";
import { api, explain, HttpError, type RecordData } from "../core/api";
import {
  clearDraft,
  newDraft,
  readDraft,
  saveDraft,
  type Draft,
  type PhotoDraft,
} from "../core/drafts";
import { encodeWire, integer, key, parseWire } from "../core/wire";
import { navigate } from "../core/router";
import { invalidateQueries } from "../core/query";
import { useSession } from "../core/session";
import { preparePhoto } from "../core/photos";
import {
  SchemaForm,
  Facts,
  schemaDefault,
  schemas,
  type Schema,
} from "../components/SchemaForm";
import {
  Button,
  Card,
  Check,
  Choices,
  Field,
  Modal,
  Notice,
  PrivateImage,
  TaskPage,
} from "../components/ui";
import { workflows, type Context } from "./workflows";
import { PurposeCheck } from "../components/PurposeCheck";
import { Attachments, PhotoPreview } from "../components/Attachments";
import { EntryFields } from "../components/ContentFields";
import { LocateButton } from "./MapPage";

interface Prepared {
  body: Record<string, unknown>;
  scope?: Record<string, unknown>;
  path: string;
  method: string;
}
const privateFields = new Set([
  "password",
  "code",
  "reauth_token",
  "query_token",
  "token",
  "safety_token",
]);
function safeSnapshot(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(safeSnapshot);
  if (value && typeof value === "object")
    return Object.fromEntries(
      Object.entries(value)
        .filter(([k]) => !privateFields.has(k))
        .map(([k, v]) => [k, safeSnapshot(v)]),
    );
  return value;
}
function contractValue(value: unknown, raw: Schema): unknown {
  const schema = raw.$ref
    ? schemas[raw.$ref.split("/").at(-1)!]
    : raw.anyOf
      ? raw.anyOf.find((s) => s.type !== "null")!
      : raw;
  if (value == null) return value;
  if (Array.isArray(value))
    return value.map((item) => contractValue(item, schema.items ?? {}));
  if (typeof value === "object" && schema.properties)
    return Object.fromEntries(
      Object.entries(value)
        .filter(([k]) => k in schema.properties!)
        .map(([k, v]) => [k, contractValue(v, schema.properties![k])]),
    );
  return value;
}

export function WorkflowEditor({
  flow,
  context,
}: {
  flow: string;
  context: Context;
}) {
  const workflow = workflows[flow];
  const session = useSession();
  const owner = session.user?.id ?? 0n;
  const route = location.pathname.slice(4) + location.search;
  const [draft, setDraft] = useState<Draft>();
  const latest = useRef<Draft | undefined>(undefined);
  const revision = useRef(0);
  const queue = useRef(Promise.resolve());
  const blocked = useRef(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [restoring, setRestoring] = useState(true);
  const [prepared, setPrepared] = useState<Prepared>();
  const [conflict, setConflict] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<RecordData>();
  const [agreed, setAgreed] = useState(false);
  const [grant, setGrant] = useState("");
  const [contact, setContact] = useState("");
  const [contactCode, setContactCode] = useState("");
  const [verifiedContact, setVerifiedContact] = useState("");
  const [uploading, setUploading] = useState<Set<string>>(new Set());
  const formOmit = [...(workflow?.omit ?? [])];
  if (flow === "group-action") {
    const action = context.params.get("action");
    if (action !== "directory")
      formOmit.push("discoverable", "city", "summary");
    if (action !== "notice") formOmit.push("notice");
    if (!["transfer", "remove_member"].includes(action ?? ""))
      formOmit.push("user_id");
  }
  if (
    flow === "adoption-action" &&
    context.params.get("action") !== "propose_trial"
  )
    formOmit.push("trial_until");
  if (
    flow === "appointment-action" &&
    context.params.get("action") !== "restore"
  )
    formOmit.push("restore_version");
  const initial = () => {
    if (!workflow) return {};
    const defaults = schemaDefault(workflow.schema);
    for (const key of formOmit) delete defaults[key];
    return { ...defaults, ...workflow.initial?.(context) };
  };
  useEffect(() => {
    let active = true;
    blocked.current = false;
    setRestoring(true);
    setDraft(undefined);
    setPrepared(undefined);
    setError("");
    void readDraft(owner, route)
      .then(async (saved) => {
        if (!active) return;
        const fresh = newDraft(
          owner,
          route,
          initial(),
          context.record?.version ?? integer(context.params.get("version")),
        );
        if (saved) fresh.revision = saved.revision;
        if (
          workflow?.media &&
          context.record &&
          ([
            "pet",
            "entry",
            "case",
            "offer",
            "profile",
            "introduction",
            "review-supplement",
          ].includes(flow) ||
            (flow === "claim-action" &&
              ["supplement", "resubmit"].includes(
                context.params.get("action") ?? "",
              )))
        ) {
          fresh.photos = (context.record.media_ids ?? []).map((id) => ({
            key: key(),
            name: "原有照片",
            type: "image/jpeg",
            mediaId: id,
            scope: workflow.media!,
          }));
          const avatar = integer(context.record.data.avatar_media_id);
          if (flow === "profile" && avatar)
            fresh.photos = [
              {
                key: key(),
                name: "当前头像",
                type: "image/jpeg",
                mediaId: avatar,
                scope: "public",
              },
            ];
        }
        const restored =
          saved &&
          (Object.keys(saved.values).length ||
            saved.photos.length ||
            saved.uncertain)
            ? saved
            : fresh;
        if (context.params.get("access")) {
          const capability = await readDraft(
            0n,
            `capability:${context.params.get("access")}`,
          );
          if (capability?.access)
            restored.access = {
              ...capability.access,
              requestId: restored.requestId,
            };
          else throw new Error("原事项查询能力已失效，请从恢复中心核对");
        }
        if (!active) return;
        latest.current = restored;
        revision.current = restored.revision;
        setDraft(restored);
        setRestoring(false);
      })
      .catch((error) => {
        if (active) {
          setError(explain(error));
          setRestoring(false);
        }
      });
    return () => {
      active = false;
    };
  }, [owner, route]);
  function update(change: (old: Draft) => Draft) {
    const old = latest.current;
    if (!old || busy || prepared || old.uncertain) return;
    const next = change(old);
    latest.current = next;
    setDraft(next);
    void persist().catch((error) => setError(explain(error)));
  }
  function replace(next: Draft) {
    latest.current = next;
    setDraft(next);
  }
  async function persist() {
    if (blocked.current)
      throw new Error("本机草稿保存发生冲突，请先核对已保存版本");
    const epoch = api.epoch;
    const currentKey = latest.current?.key;
    const run = queue.current
      .catch(() => {})
      .then(async () => {
        const current = latest.current;
        if (!current || current.key !== currentKey || epoch !== api.epoch)
          throw new Error("账号或任务已变化，请重新进入");
        const saved = await saveDraft({
          ...current,
          revision: revision.current,
        });
        revision.current = saved.revision;
        if (latest.current?.key === currentKey) {
          latest.current = { ...latest.current, revision: saved.revision };
          setDraft(latest.current);
        }
      });
    queue.current = run;
    try {
      await run;
    } catch (error) {
      blocked.current = true;
      throw error;
    }
  }
  async function ensureAccess() {
    const current = latest.current;
    if (!current) throw new Error("请先恢复草稿");
    if (current.access) return current.access;
    const value = await api.request<{ token: string }>(
      flow === "data" ? "capability" : "safety/session",
      { method: "POST", body: {} },
    );
    const access = {
      type: flow === "data" ? ("data" as const) : ("safety" as const),
      token: value.token,
      requestId: current.requestId,
    };
    replace({ ...latest.current!, access });
    await persist();
    return access;
  }
  function photoScope(): PhotoDraft["scope"] {
    const values = latest.current?.values;
    const content = (values?.entry ?? values?.pet) as
      | { audience?: { mode?: string } }
      | undefined;
    return content?.audience?.mode === "private"
      ? "private"
      : (workflow.media ?? "private");
  }
  async function selectPhotos(files: FileList | null) {
    if (!files || !latest.current || busy) return;
    setBusy(true);
    setError("");
    try {
      const selected: PhotoDraft[] = [];
      for (const file of Array.from(files).slice(
        0,
        (flow === "profile" ? 1 : 9) - latest.current.photos.length,
      ))
        selected.push(await preparePhoto(file, photoScope()));
      replace({
        ...latest.current,
        photos: [...latest.current.photos, ...selected],
      });
      await persist();
      setNotice("原照片已保存在此浏览器，点击逐张上传；不会自动发送表单");
    } catch (error) {
      setError(explain(error));
    } finally {
      setBusy(false);
    }
  }
  async function upload(photo: PhotoDraft) {
    if (!latest.current || !photo.blob || uploading.has(photo.key) || busy)
      return;
    setUploading((old) => new Set(old).add(photo.key));
    setError("");
    try {
      await persist();
      const headers: Record<string, string> = { "Idempotency-Key": photo.key };
      if (photo.scope === "safety")
        headers["X-Safety-Token"] = (await ensureAccess()).token;
      const form = new FormData();
      form.append("scope", photo.scope === "public" ? "public" : "private");
      form.append("file", photo.blob, photo.name);
      const result = await api.request<RecordData>(
        photo.scope === "safety" ? "safety/media" : "media",
        { method: "POST", body: form, headers, owner },
      );
      replace({
        ...latest.current!,
        photos: latest.current!.photos.map((p) =>
          p.key === photo.key
            ? { ...p, mediaId: result.id, blob: undefined, error: undefined }
            : p,
        ),
      });
      await persist();
    } catch (error) {
      setError(explain(error));
    } finally {
      setUploading((old) => {
        const next = new Set(old);
        next.delete(photo.key);
        return next;
      });
    }
  }
  async function prepare() {
    const current = latest.current;
    if (!current || !workflow || busy || blocked.current || conflict) return;
    setBusy(true);
    setError("");
    setGrant("");
    setAgreed(false);
    try {
      if (current.photos.some((p) => !p.mediaId) || uploading.size)
        throw new Error("请先完成原照片上传，失败照片可按原编号重试");
      const body = contractValue(
        current.values,
        schemas[workflow.schema],
      ) as Record<string, unknown>;
      const properties = schemas[workflow.schema]?.properties ?? {};
      if (properties.client_request_id)
        body.client_request_id = current.requestId;
      if (properties.idempotency_key) body.idempotency_key = current.requestId;
      if (properties.version) body.version = current.baseVersion;
      if (properties.media_ids)
        body.media_ids = current.photos.map((p) => p.mediaId);
      if (flow === "profile") {
        const user = session.user!;
        body.nickname = body.nickname || user.nickname;
        body.bio = body.bio ?? user.bio ?? "";
        body.theme = context.record?.data.theme ?? "honey";
        body.background =
          body.background || context.record?.data.background || "picnic";
        body.avatar_media_id = current.photos[0]?.mediaId ?? 0n;
      }
      if (["appointment", "appointment-change"].includes(flow) && body.terms)
        delete (body.terms as Record<string, unknown>).snapshot;
      if (flow === "data") {
        const access = await ensureAccess();
        body.query_token = access.token;
      }
      if (flow === "safety") {
        await ensureAccess();
        if (
          ["rights_notice", "counterstatement"].includes(String(body.type)) &&
          verifiedContact !== contact.trim()
        )
          throw new Error("请先验证此次联系邮箱，修改邮箱后需重新验证");
        body.confirmed = true;
      }
      if (workflow.preview) {
        const entry = body.entry as Record<string, unknown>;
        const audience = entry.audience as { mode?: string } | undefined;
        if (entry.space === "circle" && audience?.mode === "public")
          throw new Error("宠友圈不能自动放宽为公开");
        if (!session.authenticated && audience?.mode !== "private")
          throw new Error("请先登录并核对交流资格");
        if (
          audience?.mode !== "private" &&
          current.photos.some((p) => p.scope !== "public")
        )
          throw new Error(
            "私密照片不能自动改为公开用途，请明确移除并重新选择要公开的照片",
          );
        const origin = current.ui?.origin ?? "unknown";
        body.declaration = audience?.mode === "private" ? {} : { origin };
        if (audience?.mode !== "private") {
          const preview = await api.request<{
            manifest_hash: string;
            origin: string;
          }>("entries/preview", { method: "POST", body, owner });
          body.declaration = {
            manifest_hash: preview.manifest_hash,
            origin: preview.origin,
            confirmed: true,
          };
        }
      }
      await persist();
      const scopePath = workflow.scope?.(context, body);
      const scope =
        scopePath && !(flow === "safety" && !session.authenticated)
          ? await api.request<Record<string, unknown>>(scopePath, {
              method: "POST",
              body,
              owner,
            })
          : undefined;
      setPrepared({
        body,
        path: workflow.path(context),
        method:
          workflow.method ??
          (context.id > 0n && ["pet", "entry", "case", "offer"].includes(flow)
            ? "PATCH"
            : "POST"),
        scope,
      });
    } catch (error) {
      setError(explain(error));
    } finally {
      setBusy(false);
    }
  }
  async function loadCurrent() {
    setBusy(true);
    setError("");
    try {
      const path =
        flow === "profile"
          ? "profile"
          : context.id > 0n
            ? `${context.kind}/${context.id}`
            : context.kind;
      if (!path) throw new Error("请回到原事项核对当前状态，本机输入保留");
      const value = await api.request<Record<string, unknown>>(path, { owner });
      const row = Array.isArray(value.items)
        ? (value.items[0] as RecordData)
        : value.user
          ? {
              ...context.record!,
              version: integer(value.version),
              data: { ...value, ...(value.user as Record<string, unknown>) },
            }
          : (value as unknown as RecordData);
      if (!row?.version) throw new Error("尚未读到可核对的当前版本");
      setCurrentRecord(row);
    } catch (error) {
      setError(explain(error));
    } finally {
      setBusy(false);
    }
  }
  async function submit() {
    if (
      !prepared ||
      !agreed ||
      busy ||
      (prepared.scope && !grant) ||
      !latest.current
    )
      return;
    setBusy(true);
    setError("");
    const proof = grant;
    setGrant("");
    let sent = false;
    try {
      const body = {
        ...prepared.body,
        ...(prepared.scope ? { reauth_token: proof } : {}),
      };
      const sensitive = !!workflow.scope;
      replace({
        ...latest.current,
        uncertain: true,
        original: {
          path: prepared.path,
          method: prepared.method,
          body: encodeWire(sensitive ? safeSnapshot(body) : body),
          replayable: !!workflow.replayable && !sensitive,
        },
      });
      await persist();
      sent = true;
      const headers: Record<string, string> = {};
      if (flow === "safety")
        headers["X-Safety-Token"] = latest.current!.access!.token;
      const result = await api.request<RecordData | { id?: bigint }>(
        prepared.path,
        { method: prepared.method, body, headers, owner },
      );
      const current = latest.current!;
      if (current.access) {
        const recovery = newDraft(0n, `capability:${current.requestId}`);
        recovery.access = { ...current.access, id: result?.id };
        await saveDraft(recovery);
      }
      const cleared = await clearDraft(current);
      replace(cleared);
      revision.current = cleared.revision;
      setPrepared(undefined);
      invalidateQueries();
      if (flow === "data" && body.type === "close_account") {
        api.endSession();
        navigate("/recovery");
      } else if (workflow.after && (result?.id ?? context.id) > 0n)
        navigate(`/${workflow.after}/${result?.id ?? context.id}`);
      else {
        setNotice("已提交，请以当前记录或受限查询结果为准");
        navigate(
          flow === "safety" || flow === "data"
            ? "/recovery"
            : context.id > 0n && flow !== "delete"
              ? `/${context.kind}/${context.id}`
              : "/mine",
        );
      }
    } catch (error) {
      if (
        !sent ||
        (error instanceof HttpError &&
          [400, 401, 403, 404, 409, 422, 429].includes(error.status))
      ) {
        if (latest.current) {
          replace({ ...latest.current, uncertain: false, original: undefined });
          await persist().catch(() => {});
        }
      }
      if (error instanceof HttpError && error.status === 409) setConflict(true);
      setError(
        sent ? "原操作已发送，请先核对结果。" + explain(error) : explain(error),
      );
      setPrepared(undefined);
    } finally {
      setBusy(false);
    }
  }
  if (!workflow)
    return (
      <TaskPage title="表单不可用">
        <Notice title="未识别此业务入口" error />
      </TaskPage>
    );
  if (!workflow.guest && !session.authenticated)
    return (
      <TaskPage title={workflow.title}>
        <Notice title="请先登录后继续">
          登录后会重新校验当前对象与权限，不自动提交。
        </Notice>
      </TaskPage>
    );
  return (
    <TaskPage
      title={workflow.title}
      subtitle={
        context.id > 0n
          ? `关联记录 #${context.id} · 基准版本 ${draft?.baseVersion ?? context.record?.version ?? 0n}`
          : undefined
      }
      actions={
        <Button
          kind="primary"
          disabled={
            busy ||
            restoring ||
            !draft ||
            !!draft.uncertain ||
            uploading.size > 0 ||
            blocked.current ||
            conflict
          }
          onClick={() => void prepare()}
        >
          核对本次内容
        </Button>
      }
    >
      {restoring ? <p role="status">正在恢复本机私有草稿…</p> : null}
      {draft && ["case", "nearby"].includes(flow) ? (
        <LocateButton
          onLocation={(latitude, longitude) =>
            update((old) => {
              const field = flow === "case" ? "case" : "subscription";
              return {
                ...old,
                values: {
                  ...old.values,
                  [field]: {
                    ...(old.values[field] as Record<string, unknown>),
                    latitude,
                    longitude,
                  },
                },
                requestId: key(),
              };
            })
          }
          onError={setError}
        />
      ) : null}
      {conflict ? (
        <Notice title="服务端版本已变化，当前输入保留">
          <Button onClick={() => void loadCurrent()} disabled={busy}>
            读取当前服务端版本
          </Button>
          {currentRecord ? (
            <>
              <p>
                当前版本 {String(currentRecord.version)} ·{" "}
                {String(currentRecord.state ?? "当前资料")}
              </p>
              <Facts value={currentRecord.data} />
              <Button
                disabled={busy}
                onClick={() => {
                  if (
                    confirm(
                      "已核对当前版本，保留本机输入并以此版本重新准备？不会自动提交。",
                    )
                  ) {
                    update((old) => ({
                      ...old,
                      baseVersion: currentRecord.version,
                      requestId: key(),
                    }));
                    setConflict(false);
                    setCurrentRecord(undefined);
                  }
                }}
              >
                保留输入，以此版本重新核对
              </Button>
            </>
          ) : null}
        </Notice>
      ) : null}
      {error ? (
        <Notice title={error} error>
          {blocked.current ? (
            <Button
              onClick={() => {
                if (confirm("放弃未保存输入，读取另一标签页已保存的草稿？"))
                  location.reload();
              }}
            >
              读取已保存版本
            </Button>
          ) : null}
        </Notice>
      ) : null}
      {notice ? <Notice title={notice} /> : null}
      {draft?.uncertain ? (
        <Notice title="原操作结果待核对">
          当前正文、照片和原编号已锁定；先到恢复中心核对，不能重新创建请求。
          <Button onClick={() => navigate("/recovery")}>核对原操作</Button>
        </Notice>
      ) : null}
      {workflow.media && draft ? (
        <Card
          title={
            workflow.media === "safety" ? "当前事项的私密材料" : "添加照片"
          }
        >
          <Attachments
            photos={draft.photos}
            disabled={busy || !!prepared || !!draft.uncertain}
            limit={flow === "profile" ? 1 : 9}
            uploading={uploading}
            onSelect={(files) => void selectPhotos(files)}
            onUpload={(photo) => void upload(photo)}
            onRemove={(photoKey) =>
              update((old) => ({
                ...old,
                photos: old.photos.filter((p) => p.key !== photoKey),
              }))
            }
            onMove={(index, direction) =>
              update((old) => {
                const photos = [...old.photos];
                [photos[index + direction], photos[index]] = [
                  photos[index],
                  photos[index + direction],
                ];
                return { ...old, photos };
              })
            }
          />
        </Card>
      ) : null}
      {draft && !restoring ? (
        flow === "entry" ? (
          <EntryFields
            value={(draft.values.entry ?? {}) as Record<string, unknown>}
            disabled={busy || !!prepared || !!draft.uncertain}
            onChange={(entry) =>
              update((old) => ({
                ...old,
                values: { ...old.values, entry },
                requestId: key(),
              }))
            }
          />
        ) : (
          <SchemaForm
            name={workflow.schema}
            value={draft.values}
            disabled={busy || !!prepared || !!draft.uncertain}
            omit={formOmit}
            onChange={(values) =>
              update((old) => ({ ...old, values, requestId: key() }))
            }
          />
        )
      ) : null}
      {workflow.preview ? (
        <Choices
          label="内容来源"
          value={draft?.ui?.origin ?? "unknown"}
          onChange={(origin) =>
            update((old) => ({
              ...old,
              ui: { ...old.ui, origin },
              requestId: key(),
            }))
          }
          disabled={busy || !!prepared || !!draft?.uncertain}
          choices={[
            ["original", "原创／实拍"],
            ["ai_generated", "AI生成"],
            ["ai_modified", "AI修改"],
            ["unknown", "来源尚不确定"],
          ]}
        />
      ) : null}
      {flow === "safety" &&
      ["rights_notice", "counterstatement"].includes(
        String(draft?.values.type),
      ) ? (
        <Card title="此次通知的联系邮箱">
          <Field
            label="联系邮箱"
            value={contact}
            onChange={(value) => {
              setContact(value);
              setVerifiedContact("");
            }}
          />
          <Button
            disabled={busy || !contact.trim()}
            onClick={() => {
              setBusy(true);
              void ensureAccess()
                .then((access) =>
                  api.request("safety/contact-otp", {
                    method: "POST",
                    body: { email: contact.trim(), purpose: "rights_contact" },
                    headers: { "X-Safety-Token": access.token },
                  }),
                )
                .then(() => setNotice("验证码已发送，请在有效期内验证"))
                .catch((error) => setError(explain(error)))
                .finally(() => setBusy(false));
            }}
          >
            发送联系邮箱验证码
          </Button>
          <Field
            label="邮箱验证码"
            value={contactCode}
            onChange={setContactCode}
          />
          <Button
            disabled={busy || !contactCode}
            onClick={() => {
              const address = contact.trim();
              setBusy(true);
              void ensureAccess()
                .then((access) =>
                  api.request("safety/contact-verify", {
                    method: "POST",
                    body: { email: address, code: contactCode },
                    headers: { "X-Safety-Token": access.token },
                  }),
                )
                .then(() => setVerifiedContact(address))
                .catch((error) => setError(explain(error)))
                .finally(() => {
                  setContactCode("");
                  setBusy(false);
                });
            }}
          >
            验证当前联系方式
          </Button>
        </Card>
      ) : null}
      {prepared ? (
        <Modal
          title="核对本次操作"
          busy={busy}
          onClose={() => {
            setPrepared(undefined);
            setGrant("");
          }}
          actions={
            <Button
              kind="primary"
              onClick={() => void submit()}
              disabled={busy || !agreed || (!!prepared.scope && !grant)}
            >
              确认并提交
            </Button>
          }
        >
          <Facts value={prepared.body} />
          {flow === "contract-accept" &&
          Array.isArray(context.record?.data.contracts) ? (
            <Facts
              value={
                (
                  context.record!.data.contracts as Record<string, unknown>[]
                ).find(
                  (v) => integer(v.number) === integer(prepared.body.revision),
                )?.terms
              }
            />
          ) : null}
          {prepared.scope ? (
            <PurposeCheck
              scope={prepared.scope}
              busy={busy}
              onGrant={setGrant}
            />
          ) : null}
          <Check checked={agreed} onChange={setAgreed} disabled={busy}>
            我已核对对象、范围、版本与上述内容
          </Check>
          {error ? <Notice title={error} error /> : null}
        </Modal>
      ) : null}
    </TaskPage>
  );
}
