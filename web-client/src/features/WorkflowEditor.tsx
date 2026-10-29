import { useEffect, useRef, useState } from "react";
import {
  AppointmentCancelTask,
  AppointmentVersionReview,
} from "./AppointmentPages";
import { canConfirmAppointment } from "../core/appointment";
import { invitationActions, invitationReview } from "../core/invitation";
import { InvitationActionTask } from "./InvitationPages";
import { careReadOnlyReason, type CareContext } from "../core/care";
import {
  adoptionActionOptions,
  currentAdoptionContract,
  returnActionAllowed,
} from "../core/adoption";
import { AdoptionActionReview } from "./AdoptionPages";
import { ReturnActionReview } from "./ReturnPages";
import {
  LeadReviewTask,
  LeadPhotoTask,
  LeadFailureTask,
} from "./LeadSubmissionPages";
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
import { preparePhoto, movePhoto } from "../core/photos";
import {
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
  RowLink,
  TaskPage,
} from "../components/ui";
import { workflows, type Context } from "./workflows";
import { PurposeCheck } from "../components/PurposeCheck";
import { Attachments, PhotoPreview } from "../components/Attachments";
import { EntryFields, PeopleSelector } from "../components/ContentFields";
import { DataSelector, type DataSelection } from "../components/DataSelector";

import { WorkflowFields } from "../components/WorkflowFields";
import {
  PublicationPreview,
  PublicationSourceRestriction,
  PublicationSourceReview,
  PublicationSourcePending,
  PublicationSourceResult,
  type SourceDeclarationRecord,
} from "./PublicationPreview";

import { AmapView } from "../maps/AmapView";
import { sameCity } from "../core/cities";
import { cityCenter, type MapPoint } from "../maps/amap";

interface Prepared {
  body: Record<string, unknown>;
  scope?: Record<string, unknown>;
  path: string;
  method: string;
  photos?: PhotoDraft[];
  fingerprint?: string;
}
function publicationFingerprint(draft: Draft) {
  return encodeWire({
    values: draft.values,
    photos: draft.photos.map((p) => [p.key, p.mediaId, p.scope]),
    origin: draft.ui?.origin,
    requestId: draft.requestId,
    version: draft.baseVersion,
  });
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
  const [commandCode, setCommandCode] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [restoring, setRestoring] = useState(true);
  const [prepared, setPrepared] = useState<Prepared>();
  const [leadStage, setLeadStage] = useState<"form" | "photos" | "failure">(
    "form",
  );
  useEffect(() => {
    if (flow === "lead" && draft?.uncertain && !busy && !prepared)
      setLeadStage("failure");
  }, [flow, draft?.uncertain, busy, prepared]);
  const [sourceRestricted, setSourceRestricted] = useState(false);
  const [sourceReview, setSourceReview] = useState(false);
  const [sourceResult, setSourceResult] = useState<SourceDeclarationRecord>();
  const [conflict, setConflict] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<RecordData>();
  const [agreed, setAgreed] = useState(false);
  const [grant, setGrant] = useState("");
  const [contact, setContact] = useState("");
  const [contactCode, setContactCode] = useState("");
  const [verifiedContact, setVerifiedContact] = useState("");
  const [uploading, setUploading] = useState<Set<string>>(new Set());
  const [uploadProgress, setUploadProgress] = useState<
    Record<string, number | undefined>
  >({});
  const formOmit = [...(workflow?.omit ?? [])];
  if (flow === "data") formOmit.push("selections", "fields");
  if (["friend", "friend-action", "invite", "group-action"].includes(flow))
    formOmit.push("user_id");
  if (flow === "invite") formOmit.push("target_id", "scope");
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
  else if (flow === "adoption-action") formOmit.push("trial_until", "note");
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
    setCommandCode("");
    setBusy(false);
    setAgreed(false);
    setGrant("");
    setDraft(undefined);
    setPrepared(undefined);
    setSourceRestricted(false);
    setLeadStage("form");
    setSourceReview(false);
    setSourceResult(undefined);
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
    const draftKey = latest.current.key;
    const epoch = api.epoch;
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
        {
          method: "POST",
          body: form,
          headers,
          owner,
          onUploadProgress: (percent) => {
            if (epoch === api.epoch && latest.current?.key === draftKey)
              setUploadProgress((old) =>
                old[photo.key] === percent
                  ? old
                  : { ...old, [photo.key]: percent },
              );
          },
        },
      );
      if (epoch !== api.epoch || latest.current?.key !== draftKey) return;
      replace({
        ...latest.current,
        photos: latest.current.photos.map((p) =>
          p.key === photo.key
            ? { ...p, mediaId: result.id, blob: undefined, error: undefined }
            : p,
        ),
      });
      await persist();
    } catch (error) {
      if (epoch !== api.epoch || latest.current?.key !== draftKey) return;
      const message = explain(error);
      replace({
        ...latest.current,
        photos: latest.current.photos.map((p) =>
          p.key === photo.key ? { ...p, error: message } : p,
        ),
      });
      setError(message);
      try {
        await persist();
      } catch (saveError) {
        setError(explain(saveError));
      }
    } finally {
      setUploadProgress((old) => {
        const next = { ...old };
        delete next[photo.key];
        return next;
      });
      setUploading((old) => {
        const next = new Set(old);
        next.delete(photo.key);
        return next;
      });
    }
  }
  async function prepare() {
    const current = latest.current;
    if (
      !current ||
      !workflow ||
      busy ||
      blocked.current ||
      conflict ||
      current.uncertain
    )
      return;
    const epoch = api.epoch;
    const fingerprint = publicationFingerprint(current);
    let sourceRequestSent = false;
    let sourceCommitted = false;
    setCommandCode("");
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
      if (
        flow === "invitation-action" &&
        (context.kind !== "invitations" ||
          !context.record ||
          !invitationActions(context.record, owner).includes(
            context.params.get("action") ?? "",
          ))
      )
        throw new Error(
          "当前邀请状态或操作角色已变化，请刷新核对。原请求编号继续保留。",
        );
      if (flow === "care") {
        if (context.kind !== "adoptions" || context.id <= 0n)
          throw new Error("请从获授权的照护事项进入。");
        const care = await api.request<CareContext>(
          `adoptions/${context.id}/care-context`,
        );
        if (!care.can_post) throw new Error(careReadOnlyReason(care));
      }
      if (flow === "lead") {
        if (
          context.kind !== "cases" ||
          !context.record ||
          !["active", "matched"].includes(context.record.state) ||
          context.record.owner_id === owner
        )
          throw new Error("当前求助不能新增目击线索；原稿继续保留");
        if (
          typeof body.text !== "string" ||
          !body.text.trim() ||
          body.text.length > 2000
        )
          throw new Error("请填写不超过2000字的目击描述，原输入未清空");
      }
      if (
        flow === "appointment-change" &&
        context.record?.state === "safety_hold" &&
        !String(body.note ?? "").trim()
      )
        throw new Error(
          "请填写具体问题、更正与本人核对说明；双方新确认前保持暂停。",
        );
      if (
        flow === "appointment-action" &&
        context.params.get("action") === "confirm" &&
        context.record?.state === "safety_hold" &&
        (!canConfirmAppointment(context.record, owner) ||
          !String(body.note ?? "").trim())
      )
        throw new Error(
          "先核对本次问题、更正与新安排并填写本人说明，不能确认旧版本或代替对方同意。",
        );
      if (flow === "adoption-action" && context.record) {
        const action = context.params.get("action") ?? "";
        if (!adoptionActionOptions(context.record, owner).includes(action))
          throw new Error(
            "当前事项、本人角色或协议不允许此动作，请返回读取最新申请进度。",
          );
        if (action === "propose_trial") {
          const terms = currentAdoptionContract(context.record)?.terms as
            | Record<string, unknown>
            | undefined;
          if (
            !terms ||
            body.note !== terms.responsibilities ||
            Date.parse(String(body.trial_until)) !==
              Date.parse(String(terms.trial_until))
          )
            throw new Error(
              "本次试养必须采用双方已接受的当前协议。输入继续保留，需要更正时先提出协议新版本。",
            );
        }
      }
      if (
        flow === "return-action" &&
        context.record &&
        !returnActionAllowed(
          context.record,
          owner,
          context.params.get("action") ?? "",
          integer(body.revision),
        )
      )
        throw new Error(
          "此接回方案、本人角色、确认截止或当前版本不允许该动作；原输入保留，请返回核对最新记录。",
        );
      if (flow === "case") {
        const details = body.case as Record<string, unknown>;
        const old = context.record?.data;
        const keep =
          !!current.ui?.keepLocation &&
          old &&
          old.location_precision !== "area" &&
          sameCity(String(details.city ?? ""), String(old.city ?? "")) &&
          details.area === old.area;
        const selection = current.ui?.mapLocation;
        const picked =
          selection &&
          sameCity(selection.city, String(details.city ?? "")) &&
          selection.area === details.area
            ? selection
            : undefined;
        body.case = {
          ...details,
          location_precision: picked || keep ? "approximate" : "area",
          latitude: picked?.latitude ?? (keep ? old.latitude : 0),
          longitude: picked?.longitude ?? (keep ? old.longitude : 0),
          coordinate_system:
            picked?.coordinateSystem ??
            (keep ? (old.coordinate_system ?? "") : ""),
        };
      }
      if (flow === "nearby")
        body.subscription = {
          ...(body.subscription as Record<string, unknown>),
          latitude: 0,
          longitude: 0,
          radius_km: 0,
          location_precision: "city",
          coordinate_system: "",
        };
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
        body.declaration = { origin };
        const preview = await api.request<{
          manifest_hash: string;
          origin: string;
        }>("entries/preview", { method: "POST", body, owner });
        if (epoch !== api.epoch || latest.current?.key !== current.key) return;
        const previous = current.ui?.source;
        let prior: SourceDeclarationRecord | undefined;
        if (previous?.recordId) {
          try {
            prior = await api.request<SourceDeclarationRecord>(
              `source_declarations/${previous.recordId}`,
              { owner },
            );
          } catch (error) {
            if (
              !(error instanceof HttpError && [403, 404].includes(error.status))
            )
              throw error;
          }
        }
        const previousId =
          prior?.owner_id === owner && prior.state === "active" ? prior.id : 0n;
        let record: SourceDeclarationRecord;
        if (
          prior &&
          previousId > 0n &&
          prior.data.manifest_hash === preview.manifest_hash &&
          prior.data.origin === preview.origin
        ) {
          record = prior;
        } else {
          const requestId = key();
          const content = {
            ...body,
            declaration: {
              manifest_hash: preview.manifest_hash,
              origin: preview.origin,
              confirmed: false,
            },
          };
          const sourceBody = {
            client_request_id: requestId,
            content,
            rights_confirmed: true,
            previous_id: previousId,
          };
          replace({
            ...latest.current,
            uncertain: true,
            ui: {
              ...latest.current.ui,
              source: {
                requestId,
                manifestHash: preview.manifest_hash,
                origin: preview.origin,
              },
            },
            original: {
              path: "source_declarations",
              method: "POST",
              body: encodeWire(sourceBody),
              replayable: true,
            },
          });
          await persist();
          if (epoch !== api.epoch || latest.current?.key !== current.key)
            return;
          sourceRequestSent = true;
          record = await api.request<SourceDeclarationRecord>(
            "source_declarations",
            { method: "POST", body: sourceBody, owner },
          );
          sourceCommitted = true;
        }
        if (epoch !== api.epoch || latest.current?.key !== current.key) return;
        if (
          record.id <= 0n ||
          record.owner_id !== owner ||
          record.state !== "active" ||
          record.data.manifest_hash !== preview.manifest_hash ||
          record.data.origin !== preview.origin
        )
          throw new Error("来源记录与当前账号或内容不符，请保留原请求核对");
        replace({
          ...latest.current,
          uncertain: false,
          original: undefined,
          ui: {
            ...latest.current.ui,
            source: {
              requestId: latest.current.ui?.source?.requestId ?? "",
              manifestHash: preview.manifest_hash,
              origin: preview.origin,
              recordId: record.id,
            },
          },
        });
        if (sourceCommitted) setSourceResult(record);
        body.declaration = {
          manifest_hash: preview.manifest_hash,
          origin: preview.origin,
          confirmed: true,
          record_id: record.id,
        };
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
      if (epoch !== api.epoch || latest.current?.key !== current.key) return;
      if (fingerprint !== publicationFingerprint(latest.current))
        throw new Error("草稿已变化，请重新核对发布内容");
      setPrepared({
        body,
        ...(workflow.preview || flow === "lead"
          ? { photos: current.photos.map((p) => ({ ...p })), fingerprint }
          : {}),
        path: workflow.path(context),
        method:
          workflow.method ??
          (context.id > 0n && ["pet", "entry", "case", "offer"].includes(flow)
            ? "PATCH"
            : "POST"),
        scope,
      });
    } catch (error) {
      if (epoch === api.epoch && latest.current?.key === current.key) {
        if (
          !sourceCommitted &&
          latest.current.original?.path === "source_declarations" &&
          (!sourceRequestSent ||
            (error instanceof HttpError &&
              [400, 401, 403, 404, 409, 422, 429].includes(error.status)))
        ) {
          replace({ ...latest.current, uncertain: false, original: undefined });
          await persist().catch(() => {});
        }
        if (epoch !== api.epoch || latest.current?.key !== current.key) return;
        setError(explain(error));
        if (flow === "lead") setLeadStage("failure");
        if (
          workflow.preview &&
          error instanceof HttpError &&
          error.code === "known_ai_cannot_be_denied"
        )
          setSourceRestricted(true);
      }
    } finally {
      if (epoch === api.epoch && latest.current?.key === current.key)
        setBusy(false);
    }
  }
  async function querySource() {
    const current = latest.current;
    const source = current?.ui?.source;
    if (
      !current ||
      !source ||
      busy ||
      current.original?.path !== "source_declarations"
    )
      return;
    const epoch = api.epoch;
    const same = () =>
      epoch === api.epoch && latest.current?.key === current.key;
    setBusy(true);
    setError("");
    try {
      const receipt = await api.request<{
        client_request_id: string;
        command: string;
        state: string;
        result_kind: string;
        result_id: bigint;
      }>(`operations/${source.requestId}`, { owner });
      if (!same()) return;
      if (
        receipt.client_request_id !== source.requestId ||
        receipt.command !== "source.declare" ||
        receipt.state !== "committed" ||
        receipt.result_kind !== "source_declarations"
      )
        throw new Error("原声明仍待确认，请勿重复提交");
      const record = await api.request<SourceDeclarationRecord>(
        `source_declarations/${receipt.result_id}`,
        { owner },
      );
      if (!same()) return;
      if (
        record.id !== receipt.result_id ||
        record.owner_id !== owner ||
        record.state !== "active" ||
        record.data.manifest_hash !== source.manifestHash ||
        record.data.origin !== source.origin
      )
        throw new Error("原声明回执与记录不一致，请保留输入核对");
      replace({
        ...latest.current!,
        uncertain: false,
        original: undefined,
        ui: {
          ...latest.current!.ui,
          source: { ...source, recordId: record.id },
        },
      });
      setSourceResult(record);
      setPrepared(undefined);
      setAgreed(false);
      await persist();
    } catch (error) {
      if (same())
        setError(
          error instanceof HttpError && error.status === 404
            ? "暂未查到声明回执，保留原请求，请勿重复提交"
            : explain(error),
        );
    } finally {
      if (same()) setBusy(false);
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
  async function submit(leadConfirmed = false) {
    if (
      !prepared ||
      (!agreed && !(flow === "lead" && leadConfirmed)) ||
      busy ||
      (prepared.scope && !grant) ||
      !latest.current || latest.current.uncertain || conflict || blocked.current
    )
      return;
    if (
      prepared.fingerprint &&
      prepared.fingerprint !== publicationFingerprint(latest.current)
    ) {
      setPrepared(undefined);
      setAgreed(false);
      setError("草稿已变化，请重新核对发布内容");
      return;
    }
    setBusy(true);
    setError("");
    const proof = grant;
    setGrant("");
    const epoch = api.epoch;
    const taskKey = latest.current.key;
    const currentTask = () =>
      epoch === api.epoch && latest.current?.key === taskKey;
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
      if (!currentTask()) return;
      sent = true;
      const headers: Record<string, string> = {};
      if (flow === "safety")
        headers["X-Safety-Token"] = latest.current!.access!.token;
      const result = await api.request<RecordData | { id?: bigint }>(
        prepared.path,
        { method: prepared.method, body, headers, owner },
      );
      if (!currentTask()) return;
      const current = latest.current!;
      if (current.access) {
        const recovery = newDraft(0n, `capability:${current.requestId}`);
        recovery.access = { ...current.access, id: result?.id };
        await saveDraft(recovery);
        if (!currentTask()) return;
      }
      const cleared = await clearDraft(current);
      if (!currentTask()) return;
      replace(cleared);
      revision.current = cleared.revision;
      setPrepared(undefined);
      invalidateQueries();
      if (flow === "data" && body.type === "close_account") {
        api.endSession();
        navigate("/recovery");
      } else if (flow === "care") navigate(`/care-collaboration/${context.id}`);
      else if (workflow.after && (result?.id ?? context.id) > 0n)
        navigate(
          `/${workflow.after}/${result?.id ?? context.id}${workflow.afterView ? `?view=${encodeURIComponent(workflow.afterView)}&version=${result && "version" in result ? String(result.version) : "0"}` : ""}`,
        );
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
      if (!currentTask()) return;
      setCommandCode(error instanceof HttpError ? error.code : "");
      const knownFailure = !sent || (error instanceof HttpError &&
        [400, 401, 403, 404, 409, 422, 429].includes(error.status));
      if (knownFailure) {
        if (latest.current) {
          replace({ ...latest.current, uncertain: false, original: undefined });
          await persist().catch(() => {});
        }
      }
      if (!currentTask()) return;
      if (
        workflow.preview &&
        error instanceof HttpError &&
        error.code === "known_ai_cannot_be_denied"
      )
        setSourceRestricted(true);
      if (error instanceof HttpError && error.status === 409) setConflict(true);
      setError(
        sent && !knownFailure ? "原操作已发送，请先核对结果。" + explain(error) : explain(error),
      );
      if (flow === "lead") setLeadStage("failure");
      setPrepared(undefined);
    } finally {
      if (currentTask()) setBusy(false);
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
  const conflictReview = conflict ? (
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
  ) : null;
  const mediaEditor =
    workflow.media && draft ? (
      <Card
        title={workflow.media === "safety" ? "当前事项的私密材料" : "添加照片"}
      >
        <Attachments
          photos={draft.photos}
          disabled={busy || !!prepared || !!draft.uncertain}
          limit={flow === "profile" ? 1 : 9}
          uploading={uploading}
          progress={uploadProgress}
          onSelect={(files) => void selectPhotos(files)}
          onUpload={upload}
          onSaveOrder={persist}
          onRemove={(photoKey) =>
            update((old) => ({
              ...old,
              photos: old.photos.filter((p) => p.key !== photoKey),
            }))
          }
          onMove={(index, direction) =>
            update((old) => ({
              ...old,
              photos: movePhoto(old.photos, index, direction),
            }))
          }
        />
      </Card>
    ) : null;
  if (flow === "invitation-action" && ["leave", "revoke"].includes(context.params.get("action") ?? "") &&
      context.kind === "invitations" && context.record && draft && !restoring &&
      ["caretaker", "adoption_editor"].includes(String(context.record.data.scope)))
    return (
      <InvitationActionTask row={context.record} action={context.params.get("action") as "leave" | "revoke"} baseVersion={draft.baseVersion} requestId={draft.requestId}
        prepared={!!prepared} uncertain={!!draft.uncertain} busy={busy} error={error}
        conflict={conflict || blocked.current || !invitationActions(currentRecord ?? context.record, owner).includes(context.params.get("action") ?? "")}
        agreed={agreed} onAgree={setAgreed} onReview={() => void prepare()} onSubmit={() => void submit()}
        onEdit={() => { if(!busy && !draft.uncertain) { setPrepared(undefined); setAgreed(false); setError(""); } }}
        conflictActions={<>{conflictReview}{blocked.current ? <Notice title="此草稿已由其它页面更新">请返回本机草稿核对已保存版本；本页不会覆盖或提交。<RowLink title="查看本机草稿" to="/recovery" /></Notice> : null}</>} />
    );
  if (
    flow === "appointment-action" &&
    context.params.get("action") === "cancel" &&
    context.record &&
    draft &&
    !restoring
  )
    return (
      <AppointmentCancelTask
        row={context.record}
        baseVersion={draft.baseVersion}
        note={String(draft.values.note ?? "")}
        onNote={(note) =>
          update((old) => ({
            ...old,
            values: { ...old.values, note },
            requestId: key(),
          }))
        }
        prepared={!!prepared}
        uncertain={!!draft.uncertain}
        busy={busy}
        error={error}
        conflict={conflict || blocked.current}
        agreed={agreed}
        onAgree={setAgreed}
        onReview={() => void prepare()}
        onSubmit={() => void submit()}
        onEdit={() => {
          setPrepared(undefined);
          setError("");
          setAgreed(false);
        }}
        conflictActions={
          <>
            {conflictReview}
            {blocked.current ? (
              <Notice title="此草稿已由其它页面更新">
                <Button
                  disabled={busy}
                  onClick={() => {
                    if (
                      confirm(
                        "放弃本页未保存输入，读取其它页面已保存的私有草稿？",
                      )
                    )
                      location.reload();
                  }}
                >
                  读取已保存版本
                </Button>
              </Notice>
            ) : null}
          </>
        }
      />
    );
  if (flow === "lead" && leadStage === "failure")
    return (
      <LeadFailureTask
        error={error}
        uncertain={!!draft?.uncertain}
        conflict={conflict}
        busy={busy}
        onBack={() => setLeadStage("form")}
        onRetry={() => {
          if (draft?.uncertain) navigate("/recovery");
          else if (conflict) setLeadStage("form");
          else {
            setLeadStage("form");
            void prepare();
          }
        }}
      />
    );
  if (flow === "lead" && prepared)
    return (
      <LeadReviewTask
        parent={context.record}
        text={String(prepared.body.text ?? "")}
        count={prepared.photos?.length ?? 0}
        busy={busy}
        disabled={busy || conflict || !!draft?.uncertain || blocked.current}
        error={error}
        onBack={() => {
          if (!busy) {
            setPrepared(undefined);
            setAgreed(false);
          }
        }}
        onSubmit={() => void submit(true)}
      />
    );
  if (flow === "lead" && leadStage === "photos")
    return (
      <LeadPhotoTask
        photo={draft?.photos[0]?.mediaId}
        busy={busy}
        error={error}
        canEdit={!busy && !restoring && !draft?.uncertain && !blocked.current}
        locked={
          busy ||
          restoring ||
          !!draft?.uncertain ||
          uploading.size > 0 ||
          !!draft?.photos.some((p) => !p.mediaId) ||
          blocked.current
        }
        onBack={() => setLeadStage("form")}
        onSkip={() => {
          update((old) => ({ ...old, photos: [], requestId: key() }));
          setLeadStage("form");
        }}
      >
        {mediaEditor}
      </LeadPhotoTask>
    );
  if (workflow.preview && sourceResult)
    return (
      <PublicationSourceResult
        record={sourceResult}
        busy={busy}
        error={error}
        onContinue={() => {
          if (!busy) {
            setSourceResult(undefined);
            if (!prepared) void prepare();
          }
        }}
        onCorrect={() => {
          if (!busy) {
            setSourceResult(undefined);
            setPrepared(undefined);
            setSourceReview(true);
            setAgreed(false);
          }
        }}
      />
    );
  if (
    workflow.preview &&
    draft?.uncertain &&
    draft.original?.path === "source_declarations" &&
    draft.ui?.source &&
    sourceReview
  )
    return (
      <PublicationSourcePending
        requestId={draft.ui.source.requestId}
        busy={busy}
        error={error}
        onQuery={() => void querySource()}
        onBack={() => {
          if (!busy) setSourceReview(false);
        }}
      />
    );
  if (workflow.preview && sourceRestricted)
    return (
      <PublicationSourceRestriction
        onBack={() => {
          setSourceRestricted(false);
          setSourceReview(false);
          setAgreed(false);
        }}
      />
    );
  if (workflow.preview && prepared)
    return (
      <PublicationPreview
        body={prepared.body}
        photos={prepared.photos ?? []}
        user={session.user}
        busy={busy}
        agreed={agreed}
        error={error}
        onAgree={setAgreed}
        onSubmit={() => void submit()}
        onSource={() => {
          if (!busy) {
            setPrepared(undefined);
            setAgreed(false);
            setSourceReview(true);
          }
        }}
        onBack={() => {
          if (!busy) {
            setPrepared(undefined);
            setSourceReview(false);
            setAgreed(false);
            setGrant("");
          }
        }}
      />
    );
  if (workflow.preview && sourceReview && draft)
    return (
      <PublicationSourceReview
        entry={(draft.values.entry ?? {}) as Record<string, unknown>}
        origin={draft.ui?.origin ?? "unknown"}
        busy={busy}
        error={error}
        onOrigin={(origin) =>
          update((old) => ({
            ...old,
            ui: { ...old.ui, origin },
            requestId: key(),
          }))
        }
        onConfirm={() => void prepare()}
        onBack={() => {
          if (!busy) setSourceReview(false);
        }}
      />
    );
  const preparedReview = prepared ? (
    <>
      <Facts value={prepared.body} />
      {flow === "return-action" && context.record ? (
        <ReturnActionReview row={context.record} revision={integer(prepared.body.revision)} />
      ) : null}
      {flow === "adoption-action" && context.record ? (
        <AdoptionActionReview row={context.record} action={context.params.get("action") ?? ""} />
      ) : null}
      {flow === "appointment-action" && context.record && ["confirm", "restore"].includes(context.params.get("action") ?? "") ? (
        <AppointmentVersionReview row={context.record}
          revision={context.params.get("action") === "restore" ? integer(context.params.get("revision")) : undefined} />
      ) : null}
      {flow === "contract-accept" && Array.isArray(context.record?.data.contracts) ? (
        <Facts value={(context.record!.data.contracts as Record<string, unknown>[])
          .find(v => integer(v.number) === integer(prepared.body.revision))?.terms} />
      ) : null}
      {prepared.scope ? <PurposeCheck scope={prepared.scope} busy={busy} onGrant={setGrant} /> : null}
      <Check checked={agreed} onChange={setAgreed} disabled={busy}>
        我已核对对象、范围、版本与上述内容
      </Check>
      {error ? <Notice title={error} error /> : null}
    </>
  ) : null;
  const reviewAction = context.params.get("action") ?? "";
  const fullPageReview = flow === "data" || flow === "return-action" ||
    flow === "adoption-action" && ["propose_trial", "confirm_trial", "confirm_handover"].includes(reviewAction) ||
    flow === "appointment-action" && ["confirm", "restore"].includes(reviewAction);
  if(prepared && fullPageReview && (context.record || flow === "data")) {
    const returnReviewTitles: Record<string,string> = { accept: "核对并回应接回方案", decline: "拒绝本次接回提案", withdraw: "撤回本人接回提案", handover: "确认本人已完成接回交接" };
    const reviewTitle = flow === "data" ? prepared.body.type === "close_account" ? "核对注销影响并最终确认" : prepared.body.type === "matters_assistance" ? "核对本次事项协助请求" : "核对所选删除范围并最终确认"
      : flow === "return-action"
      ? (returnReviewTitles[reviewAction] ?? workflow.title)
      : flow === "adoption-action" ? reviewAction === "confirm_handover" ? "核对并确认实际交接" : "确认试养安排"
      : reviewAction === "restore" ? "核对历史安排的新提案" : "核对本次预约安排";
    return (
      <TaskPage title={reviewTitle} subtitle={flow === "data" ? undefined : `关联记录 #${context.id} · 基准版本 ${draft?.baseVersion ?? context.record?.version ?? 0n}`}
        scrollScope="confirmation"
        actions={<>
          <Button disabled={busy || !!draft?.uncertain} onClick={() => { setPrepared(undefined); setGrant(""); setAgreed(false); }}>返回核对内容</Button>
          <Button kind="primary" disabled={busy || conflict || blocked.current || !!draft?.uncertain || !agreed || (!!prepared.scope && !grant)}
            onClick={() => void submit()}>{busy ? "正在提交…" : reviewTitle}</Button>
        </>}>
        {flow === "data" ? <Notice title={prepared.body.type === "matters_assistance" ? "仅请求事项协助" : "只处理本次指定范围"}>
          {prepared.body.type === "close_account" ? "注销请求提交后会退出当前账号；未结事项与必要保全分别处理，不能将请求已受理显示为资料已全部删除。"
            : prepared.body.type === "matters_assistance" ? "事项协助不代替正式注销，不直接删除未选择资料，也不免除现有照护与交接责任。"
            : "保留账号与未选择内容；仅处理本次字段及指定记录，必要保全与处理结果在受限进度中分别显示。"}
        </Notice> : null}
        {preparedReview}
      </TaskPage>
    );
  }
  return (
    <TaskPage
      title={
        commandCode === "pet_placement_conflict" && flow === "adoption-action"
          ? "试养状态已变化"
          : flow === "adoption-action" &&
              ["propose_trial", "confirm_trial", "confirm_handover"].includes(
                context.params.get("action") ?? "",
              )
            ? context.params.get("action") === "confirm_handover"
              ? "核对并确认实际交接"
              : "确认试养安排"
            : flow === "appointment-change" &&
                context.record?.state === "safety_hold"
              ? "安全核对与新安排"
              : flow === "invitation-action" && context.record
                ? invitationReview(context.record, context.params.get("action") ?? "").title
                : workflow.title
      }
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
          onClick={() =>
            workflow.preview ? setSourceReview(true) : void prepare()
          }
        >
          核对本次内容
        </Button>
      }
    >
      {flow === "lead-assessment" ? (
        <Notice
          title={
            draft?.values.assessment === "helpful"
              ? "标记有帮助"
              : draft?.values.assessment === "not_match"
                ? "标记不匹配"
                : "处理标记待核对"
          }
        >
          只保存本条线索的处理标记；保留原始描述和照片，不将求助改为已找回。提交仍按当前发布人角色与版本校验。
        </Notice>
      ) : null}
      {flow === "introduction" && context.record && context.record.owner_id !== owner ? (
        <>
          <Notice title="本次仅限介绍与照片">
            仅维护该宠物的领养介绍与照片；不能邀请其他协作者、修改宠物归属、读取私密申请或代替发布人决定领养。
          </Notice>
          <RowLink title="查看协作与退出" to={`/invitations?ref=${context.id}&scope=adoption_editor`} />
        </>
      ) : null}
      {flow === "invitation-action" && context.record ? (
        <Notice title={invitationReview(context.record, context.params.get("action") ?? "").title}>
          {invitationReview(context.record, context.params.get("action") ?? "").detail}
          <p>事项 #{String(context.record.data.target_id ?? "待核对")} · 邀请 #{String(context.record.id)} · 基准版本 {String(draft?.baseVersion ?? context.record.version)}</p>
        </Notice>
      ) : null}
      {flow === "lead-contact" ? (
        <Notice title="本条私密线索的站内联系">
          会话对象由服务器从原线索核对；对方接受前限一条文字，请勿付费换线索。
        </Notice>
      ) : null}
      {flow === "settings" ? (
        <RowLink
          title="系统通知权限说明"
          detail="拒绝系统权限仍可读取站内消息"
          to="/notification-help"
        />
      ) : null}
      {flow === "nearby" || flow === "case" ? (
        <RowLink
          title="不提供定位也可以"
          detail="手动选城市、填写大致区域"
          to="/location-help"
        />
      ) : null}
      {restoring ? <p role="status">正在恢复本机私有草稿…</p> : null}
      {flow === "adoption-action" &&
      commandCode === "pet_placement_conflict" ? (
        <Notice title="未能确认本次安排" warning>
          当前宠物已有其它有效安排，原说明与输入继续保留；先读取最新进度，不沿用旧按钮继续交接。
          <RowLink title="查看最新申请状态" to={"/adoptions/" + context.id} />
        </Notice>
      ) : null}
      {flow === "adoption-action" && context.record ? (
        <AdoptionActionReview
          row={context.record}
          action={context.params.get("action") ?? ""}
        />
      ) : null}
      {flow === "return-action" && context.record && draft ? (
        <ReturnActionReview
          row={context.record}
          revision={integer(draft.values.revision)}
        />
      ) : null}
      {flow === "adoption-action" &&
      context.params.get("action") === "propose_trial" &&
      context.record &&
      draft
        ? (() => {
            const contract = currentAdoptionContract(context.record),
              terms = contract?.terms as Record<string, unknown> | undefined;
            if (
              !terms ||
              (draft.values.note === terms.responsibilities &&
                Date.parse(String(draft.values.trial_until)) ===
                  Date.parse(String(terms.trial_until)))
            )
              return null;
            return (
              <Notice title="本机原试养草稿与当前生效协议不同">
                原说明保留在本机；本次须主动选择采用双方已接受的安排，不自动改写或提交。
                <Button
                  disabled={
                    busy || !!prepared || draft.uncertain || blocked.current
                  }
                  onClick={() =>
                    update((old) => ({
                      ...old,
                      ui: {
                        ...old.ui,
                        previousTrial: {
                          note: String(old.values.note ?? ""),
                          trialUntil: old.values.trial_until
                            ? String(old.values.trial_until)
                            : null,
                        },
                      },
                      values: {
                        ...old.values,
                        note: terms.responsibilities,
                        trial_until: terms.trial_until,
                      },
                      requestId: key(),
                    }))
                  }
                >
                  采用此生效协议的试养安排
                </Button>
              </Notice>
            );
          })()
        : null}
      {flow === "adoption-action" && draft?.ui?.previousTrial ? (
        <Card title="本机保留的原试养说明">
          <p className="prose">{draft.ui.previousTrial.note}</p>
          <p>{draft.ui.previousTrial.trialUntil ?? "原草稿未填写截止"}</p>
        </Card>
      ) : null}
      {flow === "appointment-change" &&
      context.record?.state === "safety_hold" ? (
        <Notice title="安全暂停尚未解除">
          填写具体问题、更正与本人核对说明。服务端会重新核对相关披露与资格；双方确认同一新版本前继续不出发。
        </Notice>
      ) : null}
      {flow === "appointment-action" &&
      context.record &&
      ["confirm", "restore"].includes(context.params.get("action") ?? "") ? (
        <AppointmentVersionReview
          row={context.record}
          revision={
            context.params.get("action") === "restore"
              ? integer(context.params.get("revision"))
              : undefined
          }
        />
      ) : null}
      {session.authenticated &&
      [
        "appointment",
        "appointment-change",
        "appointment-action",
        "adoption-action",
        "contract",
        "contract-accept",
        "return",
        "return-action",
        "offer",
      ].includes(flow) ? (
        <RowLink
          title="核对本次成年业务资格"
          detail="保留原草稿，核验后仍需主动确认"
          onClick={() => {
            if (busy || restoring || blocked.current) return;
            setBusy(true);
            void persist()
              .then(() =>
                navigate(
                  `/age-check?from=${encodeURIComponent(route)}&label=${encodeURIComponent(workflow.title)}`,
                ),
              )
              .catch((failure) => setError(explain(failure)))
              .finally(() => setBusy(false));
          }}
        />
      ) : null}
      {draft && flow === "data" ? (
        <>
          {draft.values.type === "erase_fields" ? (
            <Card title="需要处理的个人资料字段">
              {[
                ["bio", "个人介绍"],
                ["avatar", "头像"],
                ["nickname", "昵称"],
              ].map(([value, label]) => (
                <Check
                  key={value}
                  checked={
                    Array.isArray(draft.values.fields) &&
                    draft.values.fields.includes(value)
                  }
                  disabled={busy || !!prepared || !!draft.uncertain}
                  onChange={(selected) =>
                    update((old) => {
                      const fields = Array.isArray(old.values.fields)
                        ? old.values.fields
                        : [];
                      return {
                        ...old,
                        values: {
                          ...old.values,
                          fields: selected
                            ? [...fields, value]
                            : fields.filter((field) => field !== value),
                        },
                        requestId: key(),
                      };
                    })
                  }
                >
                  {label}
                </Check>
              ))}
            </Card>
          ) : null}
          {draft.values.type !== "close_account" ? (
            <DataSelector
              value={(draft.values.selections ?? []) as DataSelection[]}
              onChange={(selections) =>
                update((old) => ({
                  ...old,
                  values: { ...old.values, selections },
                  requestId: key(),
                }))
              }
              assistance={draft.values.type === "matters_assistance"}
              disabled={busy || !!prepared || !!draft.uncertain}
            />
          ) : null}
        </>
      ) : null}
      {draft &&
      (flow === "invite" ||
        (flow === "group-action" &&
          ["transfer", "remove_member"].includes(
            context.params.get("action") ?? "",
          ))) ? (
        <PeopleSelector
          value={integer(draft.values.user_id)}
          onChange={(user_id) =>
            update((old) => ({
              ...old,
              values: { ...old.values, user_id },
              requestId: key(),
            }))
          }
          disabled={busy || !!prepared || !!draft.uncertain}
          ids={
            flow === "group-action" &&
            Array.isArray(context.record?.data.members)
              ? (context.record?.data.members as unknown[]).map((value) =>
                  integer(value),
                )
              : undefined
          }
        />
      ) : null}
      {draft && ["case", "nearby"].includes(flow) ? (
        <Notice
          title={
            flow === "nearby" ? "本次按城市保存提醒" : "城市与文字区域即可发布"
          }
        >
          {flow === "nearby"
            ? "确认后按所选城市匹配；原距离范围会改为城市，不要求经纬度。"
            : "不把文字区域当成地图点。可在下方主动选点；保留原点时须确认城市／区域未变。"}
          {flow === "case" &&
          context.record &&
          context.record.data.location_precision !== "area" ? (
            <Check
              checked={draft.ui?.keepLocation ?? false}
              disabled={busy || !!prepared || !!draft.uncertain}
              onChange={(keepLocation) =>
                update((old) => ({
                  ...old,
                  ui: { ...old.ui, keepLocation },
                  requestId: key(),
                }))
              }
            >
              主动保留原大致地图点
            </Check>
          ) : null}
        </Notice>
      ) : null}
      {draft && flow === "case"
        ? (() => {
            const body = (draft.values.case ?? {}) as Record<string, unknown>;
            const city = String(body.city ?? ""),
              area = String(body.area ?? "");
            const selected = draft.ui?.mapLocation;
            const point: MapPoint | undefined =
              selected && selected.city === city && selected.area === area
                ? { ...selected, id: "picked", title: "本次公开大致区域" }
                : undefined;
            return (
              <Card title="最后见到的大致位置（可选）">
                <AmapView
                  points={point ? [point] : []}
                  selectedId={point?.id}
                  center={point ?? cityCenter(city)}
                  disabled={busy || !!prepared || !!draft.uncertain}
                  onPick={(value) =>
                    update((old) => ({
                      ...old,
                      ui: {
                        ...old.ui,
                        keepLocation: false,
                        mapLocation: {
                          latitude: value.latitude,
                          longitude: value.longitude,
                          coordinateSystem: "gcj02",
                          city,
                          area,
                        },
                      },
                      requestId: key(),
                    }))
                  }
                />
                {selected && !point ? (
                  <p>
                    城市或文字区域已变化，请重新选点；原点不会随新区域发布。
                  </p>
                ) : null}
                {selected ? (
                  <Button
                    disabled={busy || !!prepared || !!draft.uncertain}
                    onClick={() =>
                      update((old) => ({
                        ...old,
                        ui: {
                          ...old.ui,
                          mapLocation: undefined,
                          keepLocation: false,
                        },
                        requestId: key(),
                      }))
                    }
                  >
                    取消地图点，只保留文字区域
                  </Button>
                ) : null}
              </Card>
            );
          })()
        : null}
      {conflictReview}
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
          {draft.original?.path === "source_declarations" ? (
            <Button onClick={() => setSourceReview(true)}>
              查询原来源声明
            </Button>
          ) : null}
        </Notice>
      ) : null}
      {flow === "lead" ? (
        <RowLink
          title={`照片说明与选择（${draft?.photos.length ?? 0} 张）`}
          disabled={busy || restoring || !!draft?.uncertain || blocked.current}
          onClick={() => {
            if (!busy && !draft?.uncertain && !restoring)
              setLeadStage("photos");
          }}
        />
      ) : (
        mediaEditor
      )}
      {draft && !restoring ? (
        flow === "lead" ? (
          <Field
            label="目击描述与手动地点"
            multiline
            value={String(draft.values.text ?? "")}
            hint={`${String(draft.values.text ?? "").length}/2000 字；仅描述你实际看到的情况，不公开电话或门牌。`}
            disabled={busy || !!prepared || !!draft.uncertain}
            onChange={(text) =>
              update((old) => ({
                ...old,
                values: { ...old.values, text },
                requestId: key(),
              }))
            }
          />
        ) : flow === "entry" ? (
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
          <WorkflowFields
            flow={flow}
            envelope={workflow.envelope}
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
        <RowLink
          title="内容来源与 AI 声明"
          detail="按实际来源选择；服务端核对后仍需确认发布"
          onClick={() => {
            if (!busy && !prepared && !draft?.uncertain) setSourceReview(true);
          }}
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
          {preparedReview}
        </Modal>
      ) : null}
    </TaskPage>
  );
}
