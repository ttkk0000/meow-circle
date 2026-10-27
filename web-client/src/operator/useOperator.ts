import { useEffect, useRef, useState } from "react";
import { schemaDefault } from "../components/SchemaForm";
import { id as validID, integer } from "../core/wire";
import { OperatorClient, OperatorError } from "./client";
import {
  copy,
  draftKey,
  formError,
  roleFor,
  schemaFor,
  versionOf,
  type Context,
  type Detail,
  type Kind,
  type Prepared,
  type Queue,
  type Row,
} from "./models";

const emptyQueue = (): Queue => ({ items: [], next_cursor: 0n, loaded: false });
const emptyQueues = (): Record<Kind, Queue> => ({
  reviews: emptyQueue(),
  "rights-cases": emptyQueue(),
  "data-requests": emptyQueue(),
  "guardian-invites": emptyQueue(),
});
export function useOperator() {
  const [client] = useState(() => new OperatorClient());
  const [context, setContext] = useState<Context>();
  const contextRef = useRef<Context | undefined>(undefined);
  const [queues, setQueues] = useState(emptyQueues);
  const [detail, setDetail] = useState<Detail>();
  const [drafts, setDrafts] = useState<Record<string, Row>>({});
  const [prepared, setPrepared] = useState<Prepared>();
  const preparedRef = useRef<Prepared | undefined>(undefined);
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [forbidden, setForbidden] = useState(false);
  const reads = useRef(0),
    selection = useRef(0),
    writing = useRef(false),
    queueReads = useRef(new Set<Kind>());
  function remember(next: Prepared | undefined) {
    preparedRef.current = next;
    setPrepared(next);
  }
  function clear() {
    contextRef.current = undefined;
    setContext(undefined);
    setQueues(emptyQueues());
    setDetail(undefined);
    setDrafts({});
    remember(undefined);
    reads.current = 0;
    selection.current++;
    writing.current = false;
    queueReads.current.clear();
    setBusy(false);
    setError("");
    setForbidden(false);
  }
  function disconnect() {
    client.disconnect();
    clear();
  }
  useEffect(() => () => client.disconnect(), [client]);
  useEffect(() => {
    const leave = () => disconnect();
    window.addEventListener("pagehide", leave);
    return () => window.removeEventListener("pagehide", leave);
  }, [client]);
  async function perform(action: () => Promise<void>) {
    const epoch = client.epoch;
    reads.current++;
    setBusy(true);
    setError("");
    try {
      await action();
    } catch (failure) {
      if (epoch !== client.epoch) return;
      if (failure instanceof OperatorError && failure.status === 401) {
        disconnect();
        setError(failure.message);
        return;
      }
      if (failure instanceof OperatorError && failure.status === 403) {
        client.cancel();
        selection.current++;
        reads.current = 0;
        queueReads.current.clear();
        setBusy(false);
        writing.current = false;
        setForbidden(true);
        setQueues(emptyQueues());
        setDetail(undefined);
      }
      setError(failure instanceof Error ? failure.message : "操作未完成。");
    } finally {
      if (epoch === client.epoch) {
        reads.current = Math.max(0, reads.current - 1);
        setBusy(reads.current > 0);
      }
    }
  }
  async function readContext() {
    const value = await client.request<Context>("context");
    if (
      !value ||
      typeof value.id !== "string" ||
      !Array.isArray(value.roles) ||
      !value.roles.every((r) => typeof r === "string")
    )
      throw new Error("后台身份响应不完整。");
    contextRef.current = value;
    setContext(value);
    setQueues(
      (current) =>
        Object.fromEntries(
          Object.entries(current).map(([kind, queue]) => [
            kind,
            value.roles.includes(roleFor(kind as Kind)) ? queue : emptyQueue(),
          ]),
        ) as Record<Kind, Queue>,
    );
    setDetail((current) =>
      current && !value.roles.includes(roleFor(current.kind))
        ? undefined
        : current,
    );
    setForbidden(false);
    return value;
  }
  async function connect(token: string) {
    disconnect();
    client.connect(token);
    await perform(async () => {
      await readContext();
    });
  }
  function permits(kind: Kind) {
    return !!contextRef.current?.roles.includes(roleFor(kind));
  }
  async function readQueue(kind: Kind, more = false) {
    if (!permits(kind)) {
      setForbidden(true);
      return;
    }
    if (queueReads.current.has(kind)) return;
    const epoch = client.epoch,
      cursor = more ? queues[kind].next_cursor : 0n;
    if (more && cursor <= 0n) return;
    queueReads.current.add(kind);
    await perform(async () => {
      try {
        const page = await client.request<{
          items: Row[];
          next_cursor: bigint;
        }>(`${kind === "reviews" ? "review-queue" : kind}?cursor=${cursor}`);
        if (!Array.isArray(page.items))
          throw new Error("队列响应不完整，请重新读取。");
        setQueues((current) => {
          const items = more
            ? [...current[kind].items, ...page.items]
            : page.items;
          const unique = [
            ...new Map(items.map((row) => [String(row.id), row])).values(),
          ];
          return {
            ...current,
            [kind]: {
              items: unique,
              next_cursor: integer(page.next_cursor),
              loaded: true,
            },
          };
        });
      } finally {
        if (epoch === client.epoch) queueReads.current.delete(kind);
      }
    });
  }
  function initial(kind: Kind, row: Row): Row {
    const base = schemaDefault(schemaFor(kind));
    if (kind === "guardian-invites")
      return {
        ...base,
        invite_id: row.id,
        invite_version: row.version,
        guardian_id: row.guardian_id,
        child_id: row.child_id,
        policy_version: row.policy_version,
      };
    return { ...base, version: versionOf(row) };
  }
  async function loadDetail(kind: Kind, rawID: string) {
    const seq = ++selection.current;
    setDetail(undefined);
    await perform(async () => {
      const id = String(validID(rawID));
      if (!permits(kind)) throw new OperatorError(403);
      const row = await client.request<Row>(`${kind}/${id}`);
      if (seq !== selection.current) return;
      setDetail({ kind, id, row });
      setDrafts((current) =>
        current[draftKey(kind, id)]
          ? current
          : { ...current, [draftKey(kind, id)]: initial(kind, row) },
      );
    });
  }
  function clearDetail() {
    selection.current++;
    setDetail(undefined);
  }
  function edit(value: Row) {
    if (!detail || writing.current) return;
    setDrafts((current) => ({
      ...current,
      [draftKey(detail.kind, detail.id)]: value,
    }));
  }
  function adoptVersion() {
    if (!detail || writing.current) return;
    const current = drafts[draftKey(detail.kind, detail.id)] ?? {};
    const base = initial(detail.kind, detail.row);
    edit({
      ...current,
      ...(detail.kind === "guardian-invites"
        ? { invite_version: base.invite_version }
        : { version: base.version }),
    });
  }
  function prepare(process = false): boolean {
    if (!detail || !permits(detail.kind) || writing.current) return false;
    if (preparedRef.current && preparedRef.current.phase !== "rejected") {
      setError("请先核对并结束已有提交记录，避免丢失原操作。");
      return false;
    }
    const values = drafts[draftKey(detail.kind, detail.id)] ?? {};
    const issue = process
      ? versionOf(detail.row) <= 0n
        ? "缺少原事项版本。"
        : ""
      : formError(detail.kind, values, detail.row);
    if (issue) {
      setError(issue);
      return false;
    }
    const body = process ? { version: versionOf(detail.row) } : copy(values);
    const path =
      detail.kind === "guardian-invites"
        ? "guardian-evidence"
        : detail.kind === "reviews"
          ? `reviews/${detail.id}`
          : `${detail.kind}/${detail.id}/${detail.kind === "rights-cases" ? "decision" : process ? "process" : "hold"}`;
    remember({
      kind: detail.kind,
      id: detail.id,
      version: versionOf(detail.row),
      path,
      body,
      title: process
        ? "处理当前可处理项目"
        : detail.kind === "guardian-invites"
          ? "记录已核实监护关系"
          : detail.kind === "data-requests"
            ? "更新具体保全范围"
            : String(values.state),
      phase: "ready",
    });
    return true;
  }
  async function submit() {
    const pending = preparedRef.current;
    if (!pending || pending.phase !== "ready" || writing.current) return;
    const epoch = client.epoch;
    writing.current = true;
    await perform(async () => {
      try {
        const identity = await readContext();
        if (!identity.roles.includes(roleFor(pending.kind)))
          throw new OperatorError(403);
        const latest = await client.request<Row>(
          `${pending.kind}/${pending.id}`,
        );
        if (versionOf(latest) !== pending.version) throw new OperatorError(409);
        if (!pending.path.endsWith("/process")) {
          const issue = formError(pending.kind, pending.body, latest);
          if (issue) throw new Error(issue);
        }
        remember({
          ...pending,
          phase: "unknown",
          explanation: "已发出请求，等待服务端结果。",
        });
        try {
          const result = await client.request<Row>(pending.path, pending.body);
          remember({
            ...pending,
            phase: "accepted",
            result: result ?? {},
            explanation:
              "已收到服务端接受响应；具体处理进度仍需重新读取原事项。",
          });
        } catch (failure) {
          if (failure instanceof OperatorError && !failure.uncertain)
            remember({
              ...pending,
              phase: "rejected",
              explanation: failure.message,
            });
          else if (client.epoch === epoch)
            remember({
              ...pending,
              phase: "unknown",
              explanation:
                "未取得确定响应。原事项、版本与说明已保留，先回读，再人工核对处理记录。",
            });
          throw failure;
        }
      } finally {
        if (client.epoch === epoch) writing.current = false;
      }
    });
    const epochAfter = client.epoch;
    if (epochAfter === epoch) writing.current = false;
  }
  // 写入期间禁止自动重发；回读只显示当前版本，不能作为原操作的独立回执。
  async function checkPending() {
    const pending = preparedRef.current;
    if (!pending || writing.current) return;
    await perform(async () => {
      if (!permits(pending.kind)) throw new OperatorError(403);
      const checked = await client.request<Row>(
        `${pending.kind}/${pending.id}`,
      );
      remember({ ...pending, checked });
    });
  }
  function finishPending(acknowledged = false) {
    const pending = preparedRef.current;
    if (
      !pending ||
      writing.current ||
      (pending.phase === "unknown" && (!pending.checked || !acknowledged))
    )
      return;
    // 结束的是本机记录；未知结果不会被改写成成功，也不生成新的重试请求。
    setDrafts((current) => {
      const next = { ...current };
      delete next[draftKey(pending.kind, pending.id)];
      return next;
    });
    remember(undefined);
    clearDetail();
  }
  return {
    client,
    context,
    queues,
    detail,
    drafts,
    prepared,
    busy,
    error,
    forbidden,
    connect,
    disconnect,
    refresh: () =>
      perform(async () => {
        await readContext();
      }),
    readQueue,
    loadDetail,
    clearDetail,
    edit,
    adoptVersion,
    prepare,
    submit,
    checkPending,
    finishPending,
    cancelPreparation: () => {
      if (preparedRef.current?.phase === "ready") remember(undefined);
    },
    permits,
  };
}
export type OperatorSession = ReturnType<typeof useOperator>;
