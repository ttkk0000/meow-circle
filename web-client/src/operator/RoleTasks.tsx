import { useEffect, useId, useRef, useState } from "react";
import { Button, Check, Field } from "../components/ui";
import { DataTable, OpsNotice, Panel } from "../operations/ui";
import { id as validID, integer, key, money } from "../core/wire";
import { OperatorError } from "./client";
import { bodyOf, time, type Row, type View } from "./models";
import type { OperatorSession } from "./useOperator";

export type RoleTask = "ageEvidence" | "authority" | "received" | "refund";
type Snapshot = { exists: boolean; record?: Row };
type RoleLostState = { flow: RoleTask; uncertain: boolean };
type TaskState = {
  flow?: RoleTask;
  values: Record<string, string>;
  stage: "edit" | "review" | "sending" | "unknown" | "accepted" | "rejected";
  prepared?: { path: string; body: Row };
  error?: string;
  fieldErrors?: Record<string, string>;
  result?: Row;
  lookup?: Snapshot;
  lookupKey?: string;
};
class TaskFieldError extends Error {
  constructor(readonly field: string, message: string) { super(message); }
}
const initial = (): TaskState => ({ values: {}, stage: "edit" });
const ageBandLabels: Record<string, string> = {
  under14: "未满14岁 (under14)",
  "14_17": "14–17岁 (14_17)",
  adult: "成年 (adult)",
};
const formatAgeValue = (name: string, value: unknown) =>
  name === "age_band"
    ? (ageBandLabels[String(value ?? "")] ?? String(value ?? ""))
    : String(value ?? "");
function parseAgeValidUntil(raw: string): string | null {
  const trimmed = raw.trim();
  const match =
    /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d{1,9})?(?:Z|([+-])(\d{2}):(\d{2}))$/i.exec(
      trimmed,
    );
  if (!match) return null;
  const year = Number(match[1]),
    month = Number(match[2]),
    day = Number(match[3]),
    hour = Number(match[4]),
    minute = Number(match[5]),
    second = Number(match[6]);
  if (month < 1 || month > 12) return null;
  const leap = (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
  const maxDays = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][
    month - 1
  ];
  if (day < 1 || day > maxDays || hour > 23 || minute > 59 || second > 59)
    return null;
  if (match[7]) {
    const tzHour = Number(match[8]),
      tzMinute = Number(match[9]);
    if (tzHour > 23 || tzMinute > 59) return null;
  }
  const ms = Date.parse(trimmed);
  const now = Date.now();
  if (!Number.isFinite(ms) || ms <= now || ms > now + 366 * 86400000)
    return null;
  return new Date(ms).toISOString();
}
const taskRole = (flow: RoleTask) =>
  flow === "ageEvidence"
    ? "identity_verifier"
    : flow === "authority"
      ? "authority_verifier"
      : "finance";
const lookupQuery = (v: Record<string, string>) =>
  new URLSearchParams({
    scope: v.scope || "disposition",
    actor_id: v.actor_id || "0",
    pet_id: v.pet_id || "0",
    city: v.city || "",
    species: v.species || "cat",
  }).toString();
export function isRoleTask(view: View): view is RoleTask {
  return ["ageEvidence", "authority", "received", "refund"].includes(view);
}

/** 新岗位任务仍由独立OperatorClient发送；原内容和未知状态仅驻留当前页面。 */
export function useRoleTasks(session: OperatorSession, activeView?: View) {
  const [state, setState] = useState<TaskState>(initial);
  const current = useRef(state);
  current.current = state;
  const lastIdentity = useRef(session.context?.id);
  const wasAgePermitted = useRef(
    !!session.context?.roles.includes("identity_verifier"),
  );
  const [lostReference, setLostReference] = useState<string>();
  const [roleLost, setRoleLost] = useState<RoleLostState | undefined>();
  const [rows, setRows] = useState<Row[]>([]),
    [loaded, setLoaded] = useState(false),
    [more, setMore] = useState(false),
    [reading, setReading] = useState(false),
    [readError, setReadError] = useState("");
  const readSequence = useRef(0),
    readBusy = useRef(false),
    writing = useRef(false);
  const roles = [...(session.context?.roles ?? [])].sort().join("|");
  useEffect(() => {
    const pending = current.current;
    const agePermitted = !!session.context?.roles.includes("identity_verifier");
    const sameIdentity = !!lastIdentity.current && lastIdentity.current === session.context?.id;
    const keepAgeTask = sameIdentity && agePermitted && pending.flow === "ageEvidence";
    if (keepAgeTask) {
      setLostReference(undefined);
      setRoleLost(undefined);
    } else if (sameIdentity && ["unknown", "sending"].includes(pending.stage)) {
      if (pending.flow === "ageEvidence") {
        setLostReference(
          `可信年龄区间登记 / 用户编号 ${String(pending.prepared?.body.actor_id ?? "请按原证据核对")}`,
        );
        setRoleLost({ flow: "ageEvidence", uncertain: true });
      } else {
        setLostReference(
          `${pending.flow ?? "原任务"} / ${String(pending.prepared?.body.client_request_id ?? pending.lookup?.record?.id ?? pending.prepared?.body.actor_id ?? "请按原证据核对")}`,
        );
      }
    } else if (
      sameIdentity &&
      (pending.flow === "ageEvidence" ||
        (activeView === "ageEvidence" && wasAgePermitted.current)) &&
      !agePermitted
    ) {
      setRoleLost({ flow: "ageEvidence", uncertain: false });
    } else if (lastIdentity.current !== session.context?.id) {
      setLostReference(undefined);
      setRoleLost(undefined);
    }
    wasAgePermitted.current = agePermitted;
    lastIdentity.current = session.context?.id;
    readSequence.current++;
    readBusy.current = false;
    writing.current = false;
    if (!keepAgeTask) {
      setState(initial());
    } else if (pending.stage === "sending") {
      setState({
        ...pending,
        stage: "unknown",
        error: "后台会话已刷新，原登记结果仍需人工核对。",
      });
    }
    setRows([]);
    setLoaded(false);
    setMore(false);
    setReading(false);
    setReadError("");
  }, [session.client.epoch, session.context?.id, roles]);
  const permitted = (role: string) => !!session.context?.roles.includes(role);
  const checkFailure = (error: unknown) => {
    if (error instanceof OperatorError && error.status === 401)
      session.disconnect();
    else if (error instanceof OperatorError && error.status === 403)
      void session.refresh();
  };
  const failText = (error: unknown) =>
    error instanceof Error ? error.message : "当前操作未完成。";
  function edit(flow: RoleTask, name: string, value: string) {
    if (
      writing.current ||
      ["review", "sending", "unknown", "accepted"].includes(
        current.current.stage,
      )
    )
      return;
    if (flow === "ageEvidence") setRoleLost(undefined);
    const old = current.current.flow === flow ? current.current : initial();
    const natural = ["scope", "actor_id", "pet_id", "city", "species"].includes(
      name,
    );
    let nextFieldErrors: Record<string, string> | undefined;
    let nextError: string | undefined;
    if (flow === "ageEvidence" && old.fieldErrors) {
      const remaining = { ...old.fieldErrors };
      delete remaining[name];
      if (Object.keys(remaining).length > 0) {
        nextFieldErrors = remaining;
        nextError = Object.values(remaining)[0];
      }
    }
    setState({
      ...old,
      flow,
      values: { ...old.values, [name]: value },
      stage: "edit",
      error: nextError,
      fieldErrors: nextFieldErrors,
      prepared: undefined,
      result: undefined,
      ...(natural ? { lookup: undefined, lookupKey: undefined } : {}),
    });
  }
  async function lookupAuthority() {
    const old = current.current;
    if (
      !permitted("authority_verifier") ||
      writing.current ||
      readBusy.current ||
      old.flow !== "authority" ||
      !["edit","rejected"].includes(old.stage)
    )
      return;
    const query = lookupQuery(old.values),
      epoch = session.client.epoch,
      seq = ++readSequence.current;
    readBusy.current = true;
    setReading(true);
    setReadError("");
    try {
      const snapshot = await session.client.request<Snapshot>(
        "authority?" + query,
      );
      if (
        epoch === session.client.epoch &&
        seq === readSequence.current &&
        lookupQuery(current.current.values) === query
      )
        setState((s) => ({ ...s, stage: "edit", lookup: snapshot, lookupKey: query, error: undefined, prepared: undefined }));
    } catch (error) {
      if (epoch === session.client.epoch) {
        setReadError(failText(error));
        checkFailure(error);
      }
    } finally {
      if (seq === readSequence.current) {
        readBusy.current = false;
        setReading(false);
      }
    }
  }
  function prepare(flow: RoleTask) {
    const oldState = current.current;
    if (
      !permitted(taskRole(flow)) ||
      writing.current ||
      (oldState.flow &&
        oldState.flow !== flow &&
        ["review", "sending", "unknown"].includes(oldState.stage)) ||
      (oldState.flow === flow &&
        ["review", "sending", "unknown", "accepted"].includes(oldState.stage))
    )
      return;
    if (flow === "ageEvidence") setRoleLost(undefined);
    const old = oldState.flow === flow ? oldState : { ...initial(), flow },
      v = old.values;
    const text = (name: string, max: number) => {
      const value = (v[name] ?? "").trim();
      if (!value || Array.from(value).length > max)
        throw new TaskFieldError(name, "请核对必要说明和字数限制。");
      return value;
    };
    const id = (name: string) => {
      try { return validID(v[name]); }
      catch (error) { throw new TaskFieldError(name, failText(error)); }
    };
    const instant = (name: string, future: boolean, maxDays = Infinity) => {
      const ms = Date.parse(v[name] ?? "");
      if (
        !Number.isFinite(ms) ||
        (future
          ? ms <= Date.now() || ms > Date.now() + maxDays * 86400000
          : ms > Date.now())
      )
        throw new TaskFieldError(name, "请核对实际时间或有效期限。");
      return new Date(ms).toISOString();
    };
    try {
      let path: string, body: Row;
      if (flow === "ageEvidence") {
        const band = v.age_band ?? "";
        const ageErrors: Record<string, string> = {};
        if (!["under14", "14_17", "adult"].includes(band)) {
          ageErrors.age_band = "请选择已核实的年龄区间。";
        }
        let actorID = 0n;
        try {
          actorID = validID((v.actor_id ?? "").trim());
        } catch {
          ageErrors.actor_id = "请输入有效的正整数用户编号。";
        }
        const evidenceRef = (v.evidence_ref ?? "").trim();
        if (!evidenceRef || Array.from(evidenceRef).length > 120) {
          ageErrors.evidence_ref =
            "请填写可信证据引用（1–120字，不录入原始证件或生日）。";
        }
        const validUntil = parseAgeValidUntil(v.valid_until ?? "");
        if (!validUntil) {
          ageErrors.valid_until =
            "请填写含时区的未来有效截止（不超过366天，例如 2026-11-01T10:00:00+08:00）。";
        }
        if (Object.keys(ageErrors).length > 0) {
          setState({
            ...old,
            flow,
            stage: "edit",
            prepared: undefined,
            error: Object.values(ageErrors)[0],
            fieldErrors: ageErrors,
          });
          return;
        }
        path = "age-evidence";
        body = {
          actor_id: actorID,
          age_band: band,
          evidence_ref: evidenceRef,
          valid_until: validUntil!,
        };
      } else if (flow === "authority") {
        if (!old.lookup || old.lookupKey !== lookupQuery(v))
          throw Error("请先按当前对象与范围读取授权记录，再核对所读版本。");
        const scope = v.scope || "disposition",
          species = v.species || "cat";
        if (!["care", "disposition", "city_rule"].includes(scope))
          throw new TaskFieldError("scope", "请选择有效的授权类型。");
        if (!["cat", "dog"].includes(species))
          throw new TaskFieldError("species", "请选择有效的物种。");
        const city = scope === "city_rule" ? text("city", 120) : "";
        const actions =
          scope === "city_rule"
            ? (v.actions ?? "").split(/[,，\s]+/).filter(Boolean)
            : [];
        if (
          scope === "city_rule" &&
          (!actions.length ||
            actions.some(
              (a) => !["adoption", "commercial", "offline"].includes(a),
            ))
        )
          throw new TaskFieldError("actions", "城市规则须明确允许的领养、有偿或线下动作。");
        path = "authority";
        body = {
          version: integer(old.lookup.record?.version),
          actor_id: scope === "city_rule" ? 0n : id("actor_id"),
          pet_id: scope === "city_rule" ? 0n : id("pet_id"),
          scope,
          city,
          species,
          rule_version: scope === "city_rule" ? text("rule_version", 120) : "",
          actions,
          evidence_ref: text("evidence_ref", 120),
          valid_until: instant("valid_until", true),
        };
      } else {
        const amount = id("amount_cents");
        if (amount > 100000000n)
          throw new TaskFieldError("amount_cents", "请核对实际金额，单笔不得超过100000000分。");
        path = "revenue";
        body = {
          client_request_id: key(),
          kind: flow === "refund" ? "actual_refund" : "actual_received",
          original_id: flow === "refund" ? id("original_id") : 0n,
          party_id: id("party_id"),
          appointment_id: id("appointment_id"),
          appointment_version: id("appointment_version"),
          amount_cents: amount,
          currency: "CNY",
          occurred_at: instant("occurred_at", false),
          source_ref: text("source_ref", 160),
          responsible: text("responsible", 120),
        };
      }
      setState({
        ...old,
        flow,
        stage: "review",
        prepared: { path, body },
        error: undefined,
        fieldErrors: undefined,
      });
    } catch (error) {
      setState({ ...old, flow, stage: "edit", prepared: undefined, error: failText(error),
        fieldErrors: error instanceof TaskFieldError ? { [error.field]: error.message } : undefined });
    }
  }
  async function submit(confirmed = false) {
    const old = current.current;
    if (
      !confirmed ||
      writing.current ||
      !old.flow ||
      !old.prepared ||
      old.stage !== "review" ||
      !permitted(taskRole(old.flow))
    )
      return;
    const epoch = session.client.epoch;
    writing.current = true;
    setState({ ...old, stage: "sending", error: undefined });
    try {
      const result = await session.client.request<Row>(
        old.prepared.path,
        old.prepared.body,
      );
      if (epoch === session.client.epoch)
        setState({
          ...old,
          stage: "accepted",
          result: result ?? {},
          error: undefined,
        });
    } catch (error) {
      if (epoch === session.client.epoch) {
        if (old.flow === "ageEvidence") {
          if (error instanceof OperatorError && error.status === 401) {
            writing.current = false;
            setState(initial());
            session.disconnect();
            return;
          }
          if (error instanceof OperatorError && error.status === 403) {
            writing.current = false;
            setState(initial());
            setRoleLost({ flow: "ageEvidence", uncertain: false });
            session.revokeAccess();
            return;
          }
          if (error instanceof OperatorError && !error.uncertain) {
            setState({
              ...old,
              stage: "rejected",
              prepared: undefined,
              error: failText(error),
              fieldErrors: undefined,
            });
            return;
          }
          setState({
            ...old,
            stage: "unknown",
            error: failText(error),
          });
          return;
        }
        const changedAuthority = old.flow === "authority" && error instanceof OperatorError && error.status === 409 && !error.uncertain;
        setState({
          ...old,
          stage:
            error instanceof OperatorError && !error.uncertain
              ? "rejected"
              : "unknown",
          lookup: changedAuthority ? undefined : old.lookup,
          lookupKey: changedAuthority ? undefined : old.lookupKey,
          error: changedAuthority ? "当前授权已变化。输入已保留，请重新读取当前授权版本，再核对提交。" : failText(error),
        });
        checkFailure(error);
      }
    } finally {
      if (epoch === session.client.epoch) writing.current = false;
    }
  }
  async function readRevenue(append = false) {
    if (!permitted("finance") || readBusy.current) return;
    const epoch = session.client.epoch,
      seq = ++readSequence.current,
      cursor = append
        ? rows.reduce(
            (max, row) => (integer(row.id) > max ? integer(row.id) : max),
            0n,
          )
        : 0n;
    readBusy.current = true;
    setReading(true);
    setReadError("");
    try {
      const result = await session.client.request<Row[] | null>(
        `revenue?cursor=${cursor}`,
      );
      const next = result ?? [];
      if (!Array.isArray(next)) throw Error("台账响应不完整，请重新读取。");
      if (epoch !== session.client.epoch || seq !== readSequence.current)
        return;
      setRows((old) => [
        ...new Map(
          [...(append ? old : []), ...next].map((row) => [String(row.id), row]),
        ).values(),
      ]);
      setLoaded(true);
      setMore(next.length === 100);
    } catch (error) {
      if (epoch === session.client.epoch) {
        setReadError(failText(error));
        checkFailure(error);
      }
    } finally {
      if (seq === readSequence.current) {
        readBusy.current = false;
        setReading(false);
      }
    }
  }
  return {
    state,
    rows,
    loaded,
    more,
    reading,
    readError,
    lostReference,
    roleLost,
    edit,
    prepare,
    submit,
    lookupAuthority,
    readRevenue,
    locked:
      writing.current || ["review", "sending", "unknown"].includes(state.stage),
    reset: (acknowledged = false) => {
      if (
        writing.current ||
        (current.current.stage === "unknown" && !acknowledged)
      )
        return;
      setRoleLost(undefined);
      setState(initial());
    },
    cancelReview: () => {
      if (!writing.current && current.current.stage === "review")
        setState((s) => ({ ...s, stage: "edit", prepared: undefined }));
    },
    backToEdit: () => {
      if (!writing.current && current.current.stage === "rejected")
        setState((s) => ({
          ...s,
          stage: "edit",
          error: undefined,
          fieldErrors: undefined,
          prepared: undefined,
        }));
    },
  };
}
type Tasks = ReturnType<typeof useRoleTasks>;
const labels: Record<string, string> = {
  actor_id: "用户编号",
  pet_id: "宠物编号",
  evidence_ref: "可信证据引用（最多120字）",
  valid_until: "有效截止（含时区）",
  city: "规范城市",
  rule_version: "规则版本",
  actions: "允许动作（adoption / commercial / offline，逗号分隔）",
  party_id: "预约参与方编号",
  appointment_id: "预约编号",
  appointment_version: "双方确认的预约版本",
  amount_cents: "实际金额（整数分）",
  occurred_at: "实际发生时间（含时区）",
  source_ref: "真实来源回执引用（最多160字）",
  responsible: "具名负责人（最多120字）",
  original_id: "原实际收款记录编号",
};
export function RoleTaskPage({
  flow,
  tasks: t,
  go,
}: {
  flow: RoleTask;
  tasks: Tasks;
  go?: (view: View, id?: string) => void;
}) {
  const [ack, setAck] = useState(false),
    s = t.state.flow === flow ? t.state : initial(),
    v = s.values;
  const [unknownAck, setUnknownAck] = useState(false);
  const [unknownClosed, setUnknownClosed] = useState(false);
  const [showLedger, setShowLedger] = useState(false);
  const fieldPrefix = useId();
  const ageField = useRef<HTMLSelectElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const isAge = flow === "ageEvidence";
  const ageBandValue = v.age_band ?? "";
  const ageHasFieldError = isAge && Object.keys(s.fieldErrors ?? {}).length > 0;
  const ageIsEmpty =
    isAge &&
    s.stage === "edit" &&
    !ageHasFieldError &&
    !["under14", "14_17", "adult"].includes(ageBandValue);
  useEffect(() => {
    if (s.fieldErrors?.age_band) {
      ageField.current?.focus();
    } else if (s.fieldErrors && Object.keys(s.fieldErrors).length > 0) {
      panelRef.current
        ?.querySelector<HTMLElement>('[aria-invalid="true"]')
        ?.focus();
    }
  }, [s.fieldErrors]);
  useEffect(() => {
    setAck(false);
    setUnknownAck(false);
    setShowLedger(false);
  }, [flow, s.stage, s.prepared]);
  useEffect(() => {
    if (!isAge || s.stage !== "edit") setUnknownClosed(false);
  }, [isAge, s.stage]);
  const fields =
    flow === "ageEvidence"
      ? ["actor_id", "evidence_ref", "valid_until"]
      : flow === "authority"
        ? [
            ...(v.scope === "city_rule"
              ? ["city", "rule_version", "actions"]
              : ["actor_id", "pet_id"]),
            "evidence_ref",
            "valid_until",
          ]
        : [
            ...(flow === "refund" ? ["original_id"] : []),
            "party_id",
            "appointment_id",
            "appointment_version",
            "amount_cents",
            "occurred_at",
            "source_ref",
            "responsible",
          ];
  const frozen = t.locked || s.stage === "accepted";
  const renderAgeSummary = (body?: Row) => {
    if (!body) return null;
    const entries: Array<[string, string]> = [
      ["用户编号", formatAgeValue("actor_id", body.actor_id)],
      ["可信年龄区间", formatAgeValue("age_band", body.age_band)],
      ["可信证据引用", formatAgeValue("evidence_ref", body.evidence_ref)],
      ["有效截止（含时区）", formatAgeValue("valid_until", body.valid_until)],
    ];
    return (
      <dl>
        {entries.map(([k, val]) => (
          <div className="ops-summary" key={k}>
            <dt>{k}</dt>
            <dd>{val}</dd>
          </div>
        ))}
      </dl>
    );
  };
  const ageNotice = (() => {
    if (s.stage === "review")
      return {
        title: ack ? "已明确核对，可确认登记" : "完整核对后再确认提交",
        body: "请逐项核对下方冻结的用户编号、可信年龄区间、证据引用与有效截止。不录入原始证件或生日；未勾选确认时禁止写入。",
      };
    if (s.stage === "sending")
      return {
        title: "正在提交原内容",
        body: "已锁定本次提交对象与内容，正在等待服务端确定响应；提交期间不可修改、重发或切换岗位。",
      };
    if (s.stage === "accepted")
      return {
        title: "已收到服务端接受响应",
        body: "服务端已受理本次可信年龄区间登记；仅代表接受区间证据，不代替本人授权或监护人同意。",
      };
    if (s.stage === "rejected")
      return {
        title: "登记明确失败，服务端未接受",
        body: "未接受本次登记，不会自动重发；保留仍有权限的输入，请核对失败原因并修改后重新完整核对。",
      };
    if (s.stage === "unknown")
      return {
        title: unknownAck ? "已人工核对原事项（仅结束本机待办）" : "原登记结果待核对",
        body: unknownAck
          ? "确认仅结束本机待核对记录，不会将未知结果标记为服务端成功；取消勾选后不能结束。"
          : "原请求内容已冻结保留。本接口无独立操作回执，请通过可信处理记录人工核对，禁止直接重发。",
      };
    if (ageHasFieldError)
      return {
        title:
          s.fieldErrors?.age_band && Object.keys(s.fieldErrors).length === 1
            ? "请选择已核实的年龄区间"
            : "请修正以下字段后再核对",
        body: "不默认成年，也不录入原始证件或生日。请根据下方标红提示修正字段后，再进入完整核对。",
      };
    if (ageIsEmpty)
      return {
        title: "初始没有默认年龄区间",
        body: "不默认成年，也不录入原始证件或生日。请选择可信证据支持的区间后，再完整核对。",
      };
    return {
      title: "可信年龄区间，不代替本人授权",
      body: "不录入原始证件或生日，关系证据与监护人同意分别处理。",
    };
  })();
  const choose = (
    name: string,
    label: string,
    options: Array<[string, string]>,
    fallback: string,
  ) => (
    <label className="field" key={name}>
      <span>{label}</span>
      <select
        id={`${fieldPrefix}-${name}`}
        ref={name === "age_band" ? ageField : undefined}
        aria-invalid={s.fieldErrors?.[name] ? true : undefined}
        aria-describedby={s.fieldErrors?.[name] ? `${fieldPrefix}-${name}-error` : undefined}
        disabled={frozen || t.reading}
        value={v[name] ?? fallback}
        onChange={(e) => {
          if (isAge) setUnknownClosed(false);
          t.edit(flow, name, e.target.value);
        }}
      >
        {options.map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>
      {s.fieldErrors?.[name] ? <small className="field-error" id={`${fieldPrefix}-${name}-error`}>{s.fieldErrors[name]}</small> : null}
    </label>
  );
  return (
    <>
      <OpsNotice
        title={
          flow === "authority"
            ? "先读取对象与范围，再核对版本"
            : isAge
              ? ageNotice.title
              : "只登记已经发生的实际事实"
        }
      >
        {flow === "authority"
          ? "仅按明确对象读取当前授权；登记不等于全链条准入。"
          : isAge
            ? ageNotice.body
            : "登记不会触发扣款或退款；报价与到访不计收入，退款必须关联原实际收款。"}
      </OpsNotice>
      {isAge && unknownClosed && s.stage === "edit" ? (
        <OpsNotice title="本机待核对已结束（非服务端接受响应）">
          <p>
            本次仅关闭本机待核对记录，并未将该登记标记为服务端成功。原提交是否生效请以可信处理记录与核查结果为准；如需继续，可重新填写并完整核对。
          </p>
          {go ? (
            <div className="ops-actions">
              <Button onClick={() => go("dashboard")}>返回本岗工作台</Button>
            </div>
          ) : null}
        </OpsNotice>
      ) : null}
      {t.state.flow && t.state.flow !== flow && t.locked ? (
        <OpsNotice title="另一个任务仍待核对">
          请返回原任务核对结果，不能另发同类操作。
        </OpsNotice>
      ) : (
        <Panel title="当前任务与必要字段">
          <div ref={panelRef}>
            {flow === "ageEvidence"
              ? choose(
                  "age_band",
                  "可信年龄区间",
                  [
                    ["", "请选择已核实区间"],
                    ["under14", "未满14岁"],
                    ["14_17", "14–17岁"],
                    ["adult", "成年"],
                  ],
                  "",
                )
              : null}
            {flow === "authority" ? (
              <>
                {choose(
                  "scope",
                  "授权范围",
                  [
                    ["care", "照护"],
                    ["disposition", "处置权"],
                    ["city_rule", "城市规则"],
                  ],
                  "disposition",
                )}
                {choose(
                  "species",
                  "物种",
                  [
                    ["cat", "猫"],
                    ["dog", "狗"],
                  ],
                  "cat",
                )}
              </>
            ) : null}
            {fields.map((name) => (
              <Field
                key={name}
                label={labels[name]}
                value={v[name] ?? ""}
                disabled={frozen || t.reading}
                error={s.fieldErrors?.[name]}
                inputMode={isAge && name === "actor_id" ? "numeric" : undefined}
                onChange={(value) => {
                  if (isAge) setUnknownClosed(false);
                  t.edit(flow, name, value);
                }}
                hint={
                  name.endsWith("until") || name === "occurred_at"
                    ? "例如 2026-11-01T10:00:00+08:00"
                    : undefined
                }
              />
            ))}
            {flow === "authority" ? (
              <>
                <Button
                  disabled={frozen || t.reading}
                  onClick={() => void t.lookupAuthority()}
                >
                  读取当前授权版本
                </Button>
                {s.lookup ? (
                  <p>
                    当前范围：
                    {s.lookup.exists
                      ? `已有记录，版本 ${String(s.lookup.record?.version)}`
                      : "尚无记录，新登记使用版本0"}
                  </p>
                ) : null}
              </>
            ) : null}
            {(!isAge || s.stage !== "rejected") && (s.error || t.readError) ? (
              <OpsNotice title={s.error || t.readError} error />
            ) : null}
            {isAge && s.stage === "rejected" ? (
              <OpsNotice title="登记明确失败，服务端未接受" error>
                <p>
                  失败原因：{s.error || "服务端未接受本次登记。"}
                  不会自动重发原操作；仍有权限的输入已保留，修正后请重新进入完整核对并明确勾选确认。
                </p>
              </OpsNotice>
            ) : null}
            {s.stage === "review" ? (
              isAge ? (
                <OpsNotice
                  title={
                    ack
                      ? "已明确核对，确认后将提交本次登记"
                      : "核对后再明确提交"
                  }
                >
                  <p>
                    当前用户编号、可信年龄区间、证据引用与有效截止已按本次输入固定。未勾选核对确认前禁止写入；取消勾选将回到未确认状态。
                  </p>
                  {renderAgeSummary(s.prepared?.body)}
                  <Check checked={ack} onChange={setAck}>
                    我已核对本次对象、范围与真实证据，确认提交
                  </Check>
                  <div className="ops-actions">
                    <Button
                      kind="primary"
                      disabled={!ack}
                      onClick={() => void t.submit(ack)}
                    >
                      确认登记
                    </Button>
                    <Button
                      onClick={() => {
                        setAck(false);
                        t.cancelReview();
                      }}
                    >
                      返回修改
                    </Button>
                  </div>
                </OpsNotice>
              ) : (
                <OpsNotice title="核对后再明确提交">
                  <p>
                    当前对象、范围、金额、来源与期限已按本次输入固定。原记录版本及原请求编号保持不变。
                  </p>
                  <dl>
                    {Object.entries(s.prepared?.body ?? {}).map(([name, value]) => (
                      <div key={name}>
                        <dt>
                          {labels[name] ??
                            (
                              {
                                kind: "登记类型",
                                scope: "授权范围",
                                age_band: "年龄区间",
                                species: "物种",
                                currency: "币种",
                                version: "已读版本",
                                client_request_id: "原请求编号",
                              } as Record<string, string>
                            )[name] ??
                            name}
                        </dt>
                        <dd>{String(value)}</dd>
                      </div>
                    ))}
                  </dl>
                  <Check checked={ack} onChange={setAck}>
                    我已核对对象、范围与实际证据，确认提交
                  </Check>
                  <Button
                    kind="primary"
                    disabled={!ack}
                    onClick={() => void t.submit(ack)}
                  >
                    确认登记
                  </Button>
                  <Button
                    onClick={() => {
                      setAck(false);
                      t.cancelReview();
                    }}
                  >
                    返回修改
                  </Button>
                </OpsNotice>
              )
            ) : null}
            {s.stage === "edit" || s.stage === "rejected" ? (
              isAge ? (
                <div className="ops-actions">
                  <Button
                    kind="primary"
                    disabled={t.locked || t.reading}
                    onClick={() => {
                      setAck(false);
                      setUnknownClosed(false);
                      t.prepare(flow);
                    }}
                  >
                    核对本次内容
                  </Button>
                  {s.stage === "rejected" ? (
                    <Button onClick={() => t.backToEdit()}>返回修改</Button>
                  ) : null}
                  {go ? (
                    <Button onClick={() => go("dashboard")}>返回本岗工作台</Button>
                  ) : null}
                </div>
              ) : (
                <Button
                  kind="primary"
                  disabled={t.locked || t.reading}
                  onClick={() => {
                    setAck(false);
                    t.prepare(flow);
                  }}
                >
                  核对本次内容
                </Button>
              )
            ) : null}
            {s.stage === "sending" ? (
              isAge ? (
                <OpsNotice title="正在提交原内容">
                  <p role="status">
                    正在提交原内容，请等待服务端确定响应；等待期间不可修改、重发或切换岗位…
                  </p>
                  {renderAgeSummary(s.prepared?.body)}
                </OpsNotice>
              ) : (
                <p role="status">正在提交原内容，请等待确定响应…</p>
              )
            ) : null}
            {s.stage === "accepted" ? (
              isAge ? (
                <OpsNotice title="已收到服务端接受响应">
                  <p>
                    服务端已返回接受响应（本接口不返回独立记录编号）。这仅表示已接受可信年龄区间证据，不代替本人授权或监护人同意，也不代表全链条已自动放行。
                  </p>
                  {renderAgeSummary(s.prepared?.body)}
                  <div className="ops-actions">
                    <Button
                      kind="primary"
                      onClick={() => {
                        setAck(false);
                        t.reset();
                      }}
                    >
                      结束本次登记
                    </Button>
                    {go ? (
                      <Button
                        onClick={() => {
                          setAck(false);
                          t.reset();
                          go("dashboard");
                        }}
                      >
                        返回本岗工作台
                      </Button>
                    ) : null}
                  </div>
                </OpsNotice>
              ) : (
                <OpsNotice title="已收到服务端接受响应">
                  记录编号：{String(s.result?.id ?? "本接口不返回独立记录")}
                  。这不代表已自动放行、监护人已同意或已发生支付。
                  <Button
                    onClick={() => {
                      setAck(false);
                      t.reset();
                    }}
                  >
                    结束本次登记
                  </Button>
                </OpsNotice>
              )
            ) : null}
            {s.stage === "unknown" ? (
              isAge ? (
                <OpsNotice
                  title={
                    unknownAck
                      ? "已人工核对原事项（仅结束本机待办）"
                      : "原登记结果待核对"
                  }
                >
                  <p>
                    原请求与输入内容已冻结保留。可信年龄区间接口无独立操作回执，请通过可信处理记录人工核对实际结果，禁止直接重发，也不能因对象变化推定本次成功。
                  </p>
                  {renderAgeSummary(s.prepared?.body)}
                  <p className="ops-caption">
                    此动作只关闭本机待办，不把原登记标记为服务端成功；取消勾选后不能结束。
                  </p>
                  <Check
                    checked={unknownAck}
                    onChange={setUnknownAck}
                    disabled={t.reading}
                  >
                    已人工核对原事项和处理记录，仅结束本机待办，不标记为成功
                  </Check>
                  <div className="ops-actions">
                    <Button
                      kind={unknownAck ? "primary" : "secondary"}
                      disabled={!unknownAck || t.reading}
                      onClick={() => {
                        if (!unknownAck) return;
                        setUnknownAck(false);
                        setUnknownClosed(true);
                        t.reset(true);
                      }}
                    >
                      {unknownAck ? "仅结束本机待核对" : "结束本机待核对"}
                    </Button>
                  </div>
                </OpsNotice>
              ) : (
                <OpsNotice title="原登记结果待核对">
                  <p>
                    原内容已冻结。年龄与授权没有独立操作回执；请通过可信处理记录人工核对，不能直接重发。财务可重新读取实际台账核对来源。
                  </p>
                  {s.prepared?.body.client_request_id ? <p>原请求编号：{String(s.prepared.body.client_request_id)}</p> : null}
                  {flow === "received" || flow === "refund" ? <>
                    <Button disabled={t.reading} onClick={() => { setShowLedger(true); void t.readRevenue(); }}>在原待办内读取实际台账核对</Button>
                    {showLedger ? <Panel title="核对台账，原待办继续保留">
                      <RoleRevenuePage tasks={t} />
                      <Button disabled={t.reading} onClick={() => setShowLedger(false)}>返回原未知登记</Button>
                    </Panel> : null}
                  </> : null}
                  <Check checked={unknownAck} onChange={setUnknownAck} disabled={t.reading}>
                    已人工核对原事项和处理记录，仅结束本机待办，不标记为成功
                  </Check>
                  <Button
                    disabled={!unknownAck || t.reading}
                    onClick={() => {
                      setUnknownAck(false);
                      t.reset(unknownAck);
                    }}
                  >
                    结束本机待核对
                  </Button>
                </OpsNotice>
              )
            ) : null}
          </div>
        </Panel>
      )}
    </>
  );
}
export function RoleRevenuePage({ tasks: t }: { tasks: Tasks }) {
  return (
    <>
      <OpsNotice title="当前已读的实际收支">
        只显示本次读取范围，不推算全站营业额；台账只登记已经发生的事实，不执行支付。
      </OpsNotice>
      <Button disabled={t.reading} onClick={() => void t.readRevenue()}>
        重新读取实际台账
      </Button>
      {t.readError ? <OpsNotice title={t.readError} error /> : null}
      <DataTable
        title="实际收款与关联退款"
        columns={[
          "记录编号",
          "事实与原收款",
          "金额／币种",
          "发生时间",
          "来源与负责人",
        ]}
        loading={t.reading}
        more={t.more ? () => void t.readRevenue(true) : undefined}
        empty={t.loaded ? "当前读取范围无记录" : "尚未读取台账"}
        rows={t.rows.map((row) => {
          const v = bodyOf(row).input as Row | undefined;
          return {
            key: String(row.id),
            state: String(v?.kind ?? ""),
            search: `${row.id} ${v?.source_ref ?? ""}`,
            cells: [
              String(row.id),
              v?.kind === "actual_refund"
                ? `实际退款 · 原收款 ${v.original_id}`
                : "实际收款",
              `${money(integer(v?.amount_cents))} CNY`,
              time(v?.occurred_at),
              `${v?.source_ref ?? ""} · ${v?.responsible ?? ""}`,
            ],
          };
        })}
      />
    </>
  );
}
