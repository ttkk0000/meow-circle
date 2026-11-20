import { useEffect, useId, useRef, useState } from "react";
import { CityField } from "../components/CityField";
import { Button, Check, Field } from "../components/ui";
import { DataTable, OpsNotice, Panel } from "../operations/ui";
import { mainlandCity, normalizeCity } from "../core/cities";
import { id as validID, integer, key, money } from "../core/wire";
import { OperatorError } from "./client";
import { bodyOf, label, time, type Row, type View } from "./models";
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
  conflictVersion?: bigint;
  unknownChecked?: Snapshot;
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
const authorityScopeLabels: Record<string, string> = {
  care: "照护 (care)",
  disposition: "处置权 (disposition)",
  city_rule: "城市规则 (city_rule)",
};
const authoritySpeciesLabels: Record<string, string> = {
  cat: "猫 (cat)",
  dog: "狗 (dog)",
};
const authorityActionOptions: Array<[string, string]> = [
  ["adoption", "领养 (adoption)"],
  ["commercial", "有偿转让／出售 (commercial)"],
  ["offline", "线下接待／交接 (offline)"],
];
const authorityActionOrder = authorityActionOptions.map(([value]) => value);
function parseAuthorityActions(raw: string | undefined) {
  const tokens = (raw ?? "")
    .split(/[,，/\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
  const invalid = tokens.filter((t) => !authorityActionOrder.includes(t));
  const valid = authorityActionOrder.filter((item) => tokens.includes(item));
  return { valid, invalid };
}
function canonicalPositiveID(raw: string | undefined): string {
  const trimmed = (raw ?? "").trim();
  if (!/^[1-9][0-9]{0,18}$/.test(trimmed)) return trimmed;
  try {
    return validID(trimmed).toString();
  } catch {
    return trimmed;
  }
}
function parseRfc3339Future(raw: string, maxDays = Infinity): string | null {
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
  const maxDaysInMonth = [
    31,
    leap ? 29 : 28,
    31,
    30,
    31,
    30,
    31,
    31,
    30,
    31,
    30,
    31,
  ][month - 1];
  if (
    day < 1 ||
    day > maxDaysInMonth ||
    hour > 23 ||
    minute > 59 ||
    second > 59
  )
    return null;
  if (match[7]) {
    const tzHour = Number(match[8]),
      tzMinute = Number(match[9]);
    if (tzHour > 23 || tzMinute > 59) return null;
  }
  const ms = Date.parse(trimmed);
  const now = Date.now();
  if (
    !Number.isFinite(ms) ||
    ms <= now ||
    (Number.isFinite(maxDays) && ms > now + maxDays * 86400000)
  )
    return null;
  return new Date(ms).toISOString();
}
function authorityNaturalKey(v: Record<string, string>) {
  const scope = v.scope || "disposition";
  const species = v.species || "cat";
  const errors: Record<string, string> = {};
  if (!["care", "disposition", "city_rule"].includes(scope))
    errors.scope = "请选择有效的授权范围。";
  if (!["cat", "dog"].includes(species))
    errors.species = "请选择有效的物种。";
  if (scope === "city_rule") {
    const matched = mainlandCity(v.city ?? "");
    if (!matched)
      errors.city = "请选择大陆城市目录中的规范城市（例如 武汉、成都）。";
    return {
      scope,
      species,
      actorID: 0n,
      petID: 0n,
      city: matched?.value ?? normalizeCity(v.city ?? ""),
      valid: Object.keys(errors).length === 0,
      errors,
    };
  }
  let actorID = 0n;
  let petID = 0n;
  try {
    actorID = validID((v.actor_id ?? "").trim());
  } catch {
    errors.actor_id = "请输入有效的正整数用户编号。";
  }
  try {
    petID = validID((v.pet_id ?? "").trim());
  } catch {
    errors.pet_id = "请输入有效的正整数宠物编号。";
  }
  return {
    scope,
    species,
    actorID,
    petID,
    city: "",
    valid: Object.keys(errors).length === 0,
    errors,
  };
}
const taskRole = (flow: RoleTask) =>
  flow === "ageEvidence"
    ? "identity_verifier"
    : flow === "authority"
      ? "authority_verifier"
      : "finance";
const lookupQuery = (v: Record<string, string>) => {
  const scope = v.scope || "disposition";
  const species = v.species || "cat";
  if (scope === "city_rule") {
    return new URLSearchParams({
      scope,
      actor_id: "0",
      pet_id: "0",
      city: normalizeCity(v.city ?? ""),
      species,
    }).toString();
  }
  return new URLSearchParams({
    scope,
    actor_id: canonicalPositiveID(v.actor_id),
    pet_id: canonicalPositiveID(v.pet_id),
    city: "",
    species,
  }).toString();
};
const lookupQueryFromBody = (body: Row) => {
  const scope = String(body.scope || "disposition");
  const species = String(body.species || "cat");
  return new URLSearchParams({
    scope,
    actor_id: scope === "city_rule" ? "0" : String(body.actor_id ?? "0"),
    pet_id: scope === "city_rule" ? "0" : String(body.pet_id ?? "0"),
    city: scope === "city_rule" ? normalizeCity(String(body.city ?? "")) : "",
    species,
  }).toString();
};
function authorityTargetSummary(data: Row | Record<string, string>): string {
  const scope = String(data.scope || "disposition");
  const speciesText = authoritySpeciesLabels[String(data.species || "cat")] ?? String(data.species || "cat");
  if (scope === "city_rule") {
    const cityText = normalizeCity(String(data.city ?? "")) || String(data.city || "未填城市");
    return `${cityText} · ${speciesText}（城市规则）`;
  }
  const scopeText = authorityScopeLabels[scope] ?? scope;
  return `用户 ${String(data.actor_id || "—")} / 宠物 ${String(data.pet_id || "—")}（${scopeText} · ${speciesText}）`;
}
function authorityLostReference(body?: Row, lookup?: Snapshot): string {
  const prefix = lookup?.record?.id
    ? `记录 #${String(lookup.record.id)} · `
    : "";
  if (body) {
    const rule =
      body.scope === "city_rule" && body.rule_version
        ? ` · 规则 ${String(body.rule_version)}`
        : "";
    return `${prefix}${authorityTargetSummary(body)}${rule} · 基准版本 v${String(body.version ?? 0)}`;
  }
  if (lookup?.record?.id)
    return `记录 #${String(lookup.record.id)} · 版本 v${String(lookup.record.version ?? 0)}`;
  return "请按原自然键与可信处理记录核对";
}
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
  const wasAuthorityPermitted = useRef(
    !!session.context?.roles.includes("authority_verifier"),
  );
  const [lostReference, setLostReference] = useState<string>();
  const [roleLost, setRoleLost] = useState<RoleLostState | undefined>();
  const [rows, setRows] = useState<Row[]>([]),
    [loaded, setLoaded] = useState(false),
    [more, setMore] = useState(false),
    [reading, setReading] = useState(false),
    [readError, setReadError] = useState("");
  const readSequence = useRef(0),
    submitSequence = useRef(0),
    readBusy = useRef(false),
    writing = useRef(false);
  const roles = [...(session.context?.roles ?? [])].sort().join("|");
  useEffect(() => {
    const pending = current.current;
    const agePermitted = !!session.context?.roles.includes("identity_verifier");
    const authorityPermitted = !!session.context?.roles.includes(
      "authority_verifier",
    );
    const sameIdentity = !!lastIdentity.current && lastIdentity.current === session.context?.id;
    const keepAgeTask = sameIdentity && agePermitted && pending.flow === "ageEvidence";
    const keepAuthorityTask =
      sameIdentity && authorityPermitted && pending.flow === "authority";
    const keepTask = keepAgeTask || keepAuthorityTask;
    if (keepTask) {
      setLostReference(undefined);
      setRoleLost(undefined);
    } else if (sameIdentity && ["unknown", "sending"].includes(pending.stage)) {
      if (pending.flow === "ageEvidence") {
        setLostReference(
          `可信年龄区间登记 / 用户编号 ${String(pending.prepared?.body.actor_id ?? "请按原证据核对")}`,
        );
        setRoleLost({ flow: "ageEvidence", uncertain: true });
      } else if (pending.flow === "authority") {
        setLostReference(
          `发布授权登记 / ${authorityLostReference(pending.prepared?.body, pending.lookup)}`,
        );
        setRoleLost({ flow: "authority", uncertain: true });
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
      setLostReference(undefined);
      setRoleLost({ flow: "ageEvidence", uncertain: false });
    } else if (
      sameIdentity &&
      (pending.flow === "authority" ||
        (activeView === "authority" && wasAuthorityPermitted.current)) &&
      !authorityPermitted
    ) {
      setLostReference(undefined);
      setRoleLost({ flow: "authority", uncertain: false });
    } else if (lastIdentity.current !== session.context?.id) {
      setLostReference(undefined);
      setRoleLost(undefined);
    }
    wasAgePermitted.current = agePermitted;
    wasAuthorityPermitted.current = authorityPermitted;
    lastIdentity.current = session.context?.id;
    readSequence.current++;
    submitSequence.current++;
    readBusy.current = false;
    writing.current = false;
    if (!keepTask) {
      setState(initial());
    } else if (pending.stage === "sending") {
      setState({
        ...pending,
        stage: "unknown",
        error:
          pending.flow === "authority"
            ? "后台会话已刷新，原授权登记结果仍需人工核对。"
            : "后台会话已刷新，原登记结果仍需人工核对。",
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
    if (flow === "ageEvidence" || flow === "authority") setRoleLost(undefined);
    if (flow === "authority") {
      readSequence.current++;
      setReadError("");
    }
    const old = current.current.flow === flow ? current.current : initial();
    const natural = ["scope", "actor_id", "pet_id", "city", "species"].includes(
      name,
    );
    let nextFieldErrors: Record<string, string> | undefined;
    let nextError: string | undefined;
    if ((flow === "ageEvidence" || flow === "authority") && old.fieldErrors) {
      const remaining = { ...old.fieldErrors };
      delete remaining[name];
      if (flow === "authority" && name === "scope") {
        const drop =
          value === "city_rule"
            ? ["actor_id", "pet_id"]
            : ["city", "rule_version", "actions"];
        for (const k of drop) delete remaining[k];
      }
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
      conflictVersion: undefined,
      unknownChecked: undefined,
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
      (old.flow && old.flow !== "authority") ||
      !["edit", "rejected"].includes(old.stage)
    )
      return;
    const base = old.flow === "authority" ? old : { ...initial(), flow: "authority" as const };
    const nat = authorityNaturalKey(base.values);
    if (!nat.valid) {
      setReadError("");
      setState({
        ...base,
        flow: "authority",
        stage: "edit",
        lookup: undefined,
        lookupKey: undefined,
        conflictVersion: undefined,
        prepared: undefined,
        error: Object.values(nat.errors)[0],
        fieldErrors: nat.errors,
      });
      return;
    }
    setRoleLost(undefined);
    const query = lookupQuery(base.values),
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
        !snapshot ||
        typeof snapshot !== "object" ||
        typeof snapshot.exists !== "boolean"
      )
        throw new Error("当前授权快照响应不完整，请重新读取。");
      if (
        epoch === session.client.epoch &&
        seq === readSequence.current &&
        permitted("authority_verifier") &&
        (!current.current.flow || current.current.flow === "authority") &&
        ["edit", "rejected"].includes(current.current.stage) &&
        lookupQuery(current.current.values) === query
      )
        setState((s) => ({
          ...(s.flow === "authority" ? s : initial()),
          flow: "authority",
          stage: "edit",
          lookup: snapshot,
          lookupKey: query,
          error: undefined,
          fieldErrors: undefined,
          conflictVersion: undefined,
          unknownChecked: undefined,
          prepared: undefined,
        }));
    } catch (error) {
      if (epoch === session.client.epoch && seq === readSequence.current) {
        if (error instanceof OperatorError && error.status === 401) {
          setState(initial());
          session.disconnect();
          return;
        }
        if (error instanceof OperatorError && error.status === 403) {
          setState(initial());
          setRoleLost({ flow: "authority", uncertain: false });
          session.revokeAccess();
          return;
        }
        setReadError(failText(error));
      }
    } finally {
      if (seq === readSequence.current) {
        readBusy.current = false;
        setReading(false);
      }
    }
  }
  async function checkAuthorityUnknown() {
    const old = current.current;
    if (
      !permitted("authority_verifier") ||
      writing.current ||
      readBusy.current ||
      old.flow !== "authority" ||
      old.stage !== "unknown" ||
      !old.prepared
    )
      return;
    const frozenPrepared = old.prepared;
    const query = lookupQueryFromBody(frozenPrepared.body);
    const epoch = session.client.epoch;
    const seq = ++readSequence.current;
    readBusy.current = true;
    setReading(true);
    setReadError("");
    try {
      const snapshot = await session.client.request<Snapshot>(
        "authority?" + query,
      );
      if (
        !snapshot ||
        typeof snapshot !== "object" ||
        typeof snapshot.exists !== "boolean"
      )
        throw new Error("当前授权快照响应不完整，请重新读取。");
      if (
        epoch === session.client.epoch &&
        seq === readSequence.current &&
        permitted("authority_verifier") &&
        current.current.flow === "authority" &&
        current.current.stage === "unknown" &&
        current.current.prepared === frozenPrepared
      )
        setState((s) => ({ ...s, unknownChecked: snapshot }));
    } catch (error) {
      if (epoch === session.client.epoch && seq === readSequence.current) {
        if (error instanceof OperatorError && error.status === 401) {
          setState(initial());
          session.disconnect();
          return;
        }
        if (error instanceof OperatorError && error.status === 403) {
          setLostReference(
            `发布授权登记 / ${authorityLostReference(frozenPrepared.body, old.lookup)}`,
          );
          setState(initial());
          setRoleLost({ flow: "authority", uncertain: true });
          session.revokeAccess();
          return;
        }
        setReadError(failText(error));
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
    if (flow === "ageEvidence" || flow === "authority") setRoleLost(undefined);
    if (flow === "authority") setReadError("");
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
        const validUntil = parseRfc3339Future(v.valid_until ?? "", 366);
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
        const nat = authorityNaturalKey(v);
        const expectedQuery = lookupQuery(v);
        if (!old.lookup || old.lookupKey !== expectedQuery) {
          setState({
            ...old,
            flow,
            stage: "edit",
            prepared: undefined,
            conflictVersion: undefined,
            error:
              Object.values(nat.errors)[0] ??
              "尚未读取当前自然键的真实授权版本，请先点击「读取当前授权版本」后再核对。",
            fieldErrors: nat.valid ? undefined : nat.errors,
          });
          return;
        }
        const authorityErrors: Record<string, string> = { ...nat.errors };
        let ruleVersion = "";
        let parsedActions: { valid: string[]; invalid: string[] } = {
          valid: [],
          invalid: [],
        };
        if (nat.scope === "city_rule") {
          ruleVersion = (v.rule_version ?? "").trim();
          if (!ruleVersion || Array.from(ruleVersion).length > 120)
            authorityErrors.rule_version = "请填写规则版本（1–120字）。";
          parsedActions = parseAuthorityActions(v.actions);
          if (parsedActions.invalid.length > 0 || parsedActions.valid.length === 0)
            authorityErrors.actions =
              "城市规则须至少选择一项允许动作（adoption / commercial / offline）。";
        }
        const evidenceRef = (v.evidence_ref ?? "").trim();
        if (!evidenceRef || Array.from(evidenceRef).length > 120) {
          authorityErrors.evidence_ref = "请填写可信证据引用（1–120字）。";
        }
        const validUntil = parseRfc3339Future(v.valid_until ?? "");
        if (!validUntil) {
          authorityErrors.valid_until =
            "请填写含时区的未来有效截止（例如 2026-11-30T18:00:00+08:00）。";
        }
        if (Object.keys(authorityErrors).length > 0) {
          setState({
            ...old,
            flow,
            stage: "edit",
            prepared: undefined,
            conflictVersion: undefined,
            error: Object.values(authorityErrors)[0],
            fieldErrors: authorityErrors,
          });
          return;
        }
        const readVersion = old.lookup.exists
          ? integer(old.lookup.record?.version)
          : 0n;
        if (old.lookup.exists && readVersion <= 0n) {
          setState({
            ...old,
            flow,
            stage: "edit",
            lookup: undefined,
            lookupKey: undefined,
            prepared: undefined,
            conflictVersion: undefined,
            error: "已读授权记录版本无效，请重新读取当前授权版本。",
          });
          return;
        }
        path = "authority";
        body = {
          version: readVersion,
          actor_id: nat.scope === "city_rule" ? 0n : nat.actorID,
          pet_id: nat.scope === "city_rule" ? 0n : nat.petID,
          scope: nat.scope,
          city: nat.city,
          species: nat.species,
          rule_version: nat.scope === "city_rule" ? ruleVersion : "",
          actions: nat.scope === "city_rule" ? parsedActions.valid : [],
          evidence_ref: evidenceRef,
          valid_until: validUntil!,
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
        conflictVersion: undefined,
        unknownChecked: undefined,
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
    if (old.flow === "authority") {
      if (
        !old.lookup ||
        old.lookupKey !== lookupQuery(old.values) ||
        integer(old.prepared.body.version) !==
          (old.lookup.exists ? integer(old.lookup.record?.version) : 0n)
      )
        return;
    }
    const epoch = session.client.epoch;
    const submitSeq = ++submitSequence.current;
    const submittedFlow = old.flow;
    writing.current = true;
    setReadError("");
    setState({
      ...old,
      stage: "sending",
      error: undefined,
      conflictVersion: undefined,
      unknownChecked: undefined,
    });
    try {
      const result = await session.client.request<Row>(
        old.prepared.path,
        old.prepared.body,
      );
      if (
        epoch !== session.client.epoch ||
        submitSeq !== submitSequence.current ||
        !permitted(taskRole(submittedFlow)) ||
        current.current.flow !== submittedFlow ||
        current.current.stage !== "sending"
      )
        return;
      if (submittedFlow === "authority" && (!result || typeof result !== "object")) {
        setState({
          ...old,
          stage: "unknown",
          unknownChecked: undefined,
          error: "服务端未返回完整授权回执，请人工核对原事项后再确认。",
        });
        return;
      }
      setState({
        ...old,
        stage: "accepted",
        result: result ?? {},
        error: undefined,
      });
    } catch (error) {
      if (
        epoch === session.client.epoch &&
        submitSeq === submitSequence.current &&
        current.current.flow === submittedFlow &&
        current.current.stage === "sending"
      ) {
        if (!permitted(taskRole(submittedFlow))) {
          const uncertainLoss = !(error instanceof OperatorError && !error.uncertain);
          setState(initial());
          if (submittedFlow === "authority") {
            if (uncertainLoss)
              setLostReference(
                `发布授权登记 / ${authorityLostReference(old.prepared.body, old.lookup)}`,
              );
            setRoleLost({ flow: "authority", uncertain: uncertainLoss });
          } else if (submittedFlow === "ageEvidence") {
            if (uncertainLoss)
              setLostReference(
                `可信年龄区间登记 / 用户编号 ${String(old.prepared.body.actor_id ?? "请按原证据核对")}`,
              );
            setRoleLost({ flow: "ageEvidence", uncertain: uncertainLoss });
          }
          return;
        }
        if (submittedFlow === "ageEvidence") {
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
        if (submittedFlow === "authority") {
          if (error instanceof OperatorError && error.status === 401) {
            writing.current = false;
            setState(initial());
            session.disconnect();
            return;
          }
          if (error instanceof OperatorError && error.status === 403) {
            writing.current = false;
            setState(initial());
            setRoleLost({ flow: "authority", uncertain: false });
            session.revokeAccess();
            return;
          }
          if (
            error instanceof OperatorError &&
            error.status === 409 &&
            !error.uncertain
          ) {
            setState({
              ...old,
              stage: "rejected",
              lookup: undefined,
              lookupKey: undefined,
              prepared: undefined,
              result: undefined,
              conflictVersion: integer(old.prepared.body.version),
              unknownChecked: undefined,
              fieldErrors: undefined,
              error:
                "当前授权版本已变化。已保留仍有权限的输入并清除旧版本，请先按原自然键重新读取，再重新确认。",
            });
            return;
          }
          if (error instanceof OperatorError && !error.uncertain) {
            setState({
              ...old,
              stage: "rejected",
              prepared: undefined,
              result: undefined,
              conflictVersion: undefined,
              unknownChecked: undefined,
              fieldErrors: undefined,
              error: failText(error),
            });
            return;
          }
          setState({
            ...old,
            stage: "unknown",
            unknownChecked: undefined,
            error: failText(error),
          });
          return;
        }
        setState({
          ...old,
          stage:
            error instanceof OperatorError && !error.uncertain
              ? "rejected"
              : "unknown",
          error: failText(error),
        });
        checkFailure(error);
      }
    } finally {
      if (epoch === session.client.epoch && submitSeq === submitSequence.current)
        writing.current = false;
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
    checkAuthorityUnknown,
    readRevenue,
    locked:
      writing.current || ["review", "sending", "unknown"].includes(state.stage),
    reset: (acknowledged = false) => {
      if (
        writing.current ||
        (current.current.stage === "unknown" && !acknowledged)
      )
        return;
      readSequence.current++;
      submitSequence.current++;
      setReadError("");
      setRoleLost(undefined);
      setLostReference(undefined);
      setState(initial());
    },
    cancelReview: () => {
      if (!writing.current && current.current.stage === "review")
        setState((s) => ({
          ...s,
          stage: "edit",
          prepared: undefined,
          conflictVersion: undefined,
        }));
    },
    backToEdit: () => {
      if (!writing.current && current.current.stage === "rejected") {
        setReadError("");
        setState((s) => ({
          ...s,
          stage: "edit",
          error: undefined,
          fieldErrors: undefined,
          conflictVersion: undefined,
          prepared: undefined,
        }));
      }
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
  const isAuthority = flow === "authority";
  const ageBandValue = v.age_band ?? "";
  const ageHasFieldError = isAge && Object.keys(s.fieldErrors ?? {}).length > 0;
  const ageIsEmpty =
    isAge &&
    s.stage === "edit" &&
    !ageHasFieldError &&
    !["under14", "14_17", "adult"].includes(ageBandValue);
  const currentAuthorityQuery = isAuthority ? lookupQuery(v) : "";
  const authorityHasLookup =
    isAuthority && !!s.lookup && s.lookupKey === currentAuthorityQuery;
  const authorityIsConflict =
    isAuthority && s.stage === "rejected" && s.conflictVersion !== undefined;
  const authorityIsCityRule =
    isAuthority && (v.scope || "disposition") === "city_rule";
  const authorityHasFieldError =
    isAuthority && Object.keys(s.fieldErrors ?? {}).length > 0;
  const matchedAuthorityCity = authorityIsCityRule
    ? mainlandCity(v.city ?? "")
    : undefined;
  const parsedAuthorityActions = authorityIsCityRule
    ? parseAuthorityActions(v.actions)
    : { valid: [], invalid: [] };
  useEffect(() => {
    if (s.fieldErrors?.age_band) {
      ageField.current?.focus();
    } else if (s.fieldErrors?.city) {
      panelRef.current
        ?.querySelector<HTMLElement>(".operator-city-wrap button")
        ?.focus();
    } else if (s.fieldErrors?.actions) {
      panelRef.current
        ?.querySelector<HTMLElement>(".operator-authority-actions input")
        ?.focus();
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
    if (!(isAge || isAuthority) || s.stage !== "edit") setUnknownClosed(false);
  }, [isAge, isAuthority, s.stage]);
  const fields =
    flow === "ageEvidence"
      ? ["actor_id", "evidence_ref", "valid_until"]
      : flow === "authority"
        ? [
            ...(authorityIsCityRule
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
  const renderAuthoritySummary = (body?: Row, lookup?: Snapshot) => {
    if (!body) return null;
    const scope = String(body.scope ?? "disposition");
    const readVersion = integer(body.version);
    const entries: Array<[string, string]> = [
      ["授权范围", authorityScopeLabels[scope] ?? scope],
      ["物种", authoritySpeciesLabels[String(body.species ?? "cat")] ?? String(body.species ?? "")],
      ...(scope === "city_rule"
        ? ([
            ["规范城市", String(body.city ?? "")],
            ["规则版本", String(body.rule_version ?? "")],
            [
              "允许动作",
              Array.isArray(body.actions)
                ? body.actions
                    .map(
                      (a) =>
                        authorityActionOptions.find(([k]) => k === String(a))?.[1] ??
                        String(a),
                    )
                    .join(" / ")
                : String(body.actions ?? ""),
            ],
          ] as Array<[string, string]>)
        : ([
            ["用户编号", String(body.actor_id ?? "")],
            ["宠物编号", String(body.pet_id ?? "")],
          ] as Array<[string, string]>)),
      [
        "已读授权版本",
        readVersion === 0n ? "0（尚无记录，新登记）" : `v${String(readVersion)}`,
      ],
      ...(lookup?.record?.id
        ? ([["原授权记录编号", `#${String(lookup.record.id)}`]] as Array<
            [string, string]
          >)
        : []),
      ["可信证据引用", String(body.evidence_ref ?? "")],
      ["有效截止（含时区）", String(body.valid_until ?? "")],
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
  const renderAuthoritySnapshot = (snapshot: Snapshot, heading: string) => {
    const rec = snapshot.record;
    const data = rec ? bodyOf(rec) : undefined;
    const entries: Array<[string, string]> =
      snapshot.exists && rec
        ? [
            ["记录编号", `#${String(rec.id)}`],
            ["当前授权版本", `v${String(rec.version)}`],
            ["当前状态", label(rec.state)],
            ["原有效截止", time(data?.valid_until)],
            ["原证据引用", String(data?.evidence_ref ?? "未返回")],
            ...(data?.scope === "city_rule"
              ? ([
                  ["原规则版本", String(data.rule_version ?? "未返回")],
                  [
                    "原允许动作",
                    Array.isArray(data.actions)
                      ? data.actions.map((item) => String(item)).join(" / ")
                      : "未返回",
                  ],
                ] as Array<[string, string]>)
              : []),
          ]
        : [["读取结果", "尚无记录，新登记将使用版本 0"]];
    return (
      <Panel title={heading}>
        <dl>
          {entries.map(([k, val]) => (
            <div className="ops-summary" key={k}>
              <dt>{k}</dt>
              <dd>{val}</dd>
            </div>
          ))}
        </dl>
      </Panel>
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
  const authorityNotice = (() => {
    if (s.stage === "review")
      return {
        title: ack ? "已明确核对，可确认提交本次授权" : "完整核对后再明确确认提交",
        body: "当前对象、范围、已读版本与证据期限已冻结。未勾选确认前禁止写入；如实际记录已变化，请返回重新读取。",
      };
    if (s.stage === "sending")
      return {
        title: "正在提交原内容",
        body: "已锁定本次授权对象、已读版本与原内容，正在等待服务端确定响应；提交期间不可修改、重发或切换岗位。",
      };
    if (s.stage === "accepted")
      return {
        title: "已收到服务端接受响应",
        body: "服务端已返回真实接受响应。仅证明本次授权登记已被受理，不代表全链条已自动放行。",
      };
    if (authorityIsConflict)
      return {
        title: "不能沿用旧版本继续登记",
        body: `已读版本 v${String(s.conflictVersion)} 已过时。旧版本与旧准备内容已清除，保留仍有权限的输入；请先按原自然键重新读取当前授权，再重新确认，禁止自动重发。`,
      };
    if (s.stage === "rejected")
      return {
        title: "本次登记未被接受",
        body: "服务端明确拒绝本次授权登记，不会自动重发；仍有权限的输入已保留，请修正后重新核对当前版本并确认提交。",
      };
    if (s.stage === "unknown")
      return {
        title: unknownAck ? "已人工核对原事项（仅结束本机待办）" : "原登记结果待核对",
        body: unknownAck
          ? "确认仅结束本机待核对记录，不会将未知结果标记为服务端成功；取消勾选后不能结束。"
          : "原对象、提交前版本与输入已冻结保留。本接口无独立操作回执，请读取原对象并结合可信处理记录人工核对，禁止盲目重发。",
      };
    if (authorityHasFieldError)
      return {
        title: "请修正以下字段后再核对",
        body: "城市规则与用户／宠物授权分开校验。请根据下方标红提示修正字段后，再读取当前版本或进入完整核对。",
      };
    if (!authorityHasLookup)
      return authorityIsCityRule
        ? {
            title: "城市规则与用户／宠物授权分开 · 尚未读取当前版本",
            body: "城市规则按规范城市与物种独立读取，不混用用户或宠物编号；未读取当前自然键的真实授权版本前，禁止确认或提交。",
          }
        : {
            title: "先读取对象与范围，再核对当前版本",
            body: "未读取当前自然键的真实授权版本前，禁止确认登记或提交。无记录将使用版本 0；修改对象或范围将使旧回读失效。",
          };
    if (authorityIsCityRule)
      return {
        title: "城市规则与用户／宠物授权分开 · 已读取当前版本",
        body: s.lookup?.exists
          ? `已读取城市规则版本 v${String(s.lookup.record?.version)}；不混用用户或宠物编号，登记不等于全链条准入。`
          : "当前城市与物种尚无记录，新登记将使用版本 0；不混用用户或宠物编号，登记不等于全链条准入。",
      };
    return {
      title: "已读取当前授权版本，可继续填写并核对",
      body: s.lookup?.exists
        ? `当前自然键已有记录，已读取版本 v${String(s.lookup.record?.version)}；修改对象或范围将使回读失效，登记不等于全链条准入。`
        : "当前自然键尚无记录，新登记将使用版本 0；修改对象或范围将使回读失效，登记不等于全链条准入。",
    };
  })();
  const choose = (
    name: string,
    labelText: string,
    options: Array<[string, string]>,
    fallback: string,
  ) => (
    <label className="field" key={name}>
      <span>{labelText}</span>
      <select
        id={`${fieldPrefix}-${name}`}
        ref={name === "age_band" ? ageField : undefined}
        aria-invalid={s.fieldErrors?.[name] ? true : undefined}
        aria-describedby={s.fieldErrors?.[name] ? `${fieldPrefix}-${name}-error` : undefined}
        disabled={frozen || t.reading || authorityIsConflict}
        value={v[name] ?? fallback}
        onChange={(e) => {
          if (isAge || isAuthority) setUnknownClosed(false);
          t.edit(flow, name, e.target.value);
        }}
      >
        {options.map(([value, optLabel]) => (
          <option key={value} value={value}>
            {optLabel}
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
          isAuthority
            ? authorityNotice.title
            : isAge
              ? ageNotice.title
              : "只登记已经发生的实际事实"
        }
      >
        {isAuthority
          ? authorityNotice.body
          : isAge
            ? ageNotice.body
            : "登记不会触发扣款或退款；报价与到访不计收入，退款必须关联原实际收款。"}
      </OpsNotice>
      {(isAge || isAuthority) && unknownClosed && s.stage === "edit" ? (
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
        <Panel title={authorityIsConflict ? "当前版本已变化 · 原输入已保留" : "当前任务与必要字段"}>
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
            {isAuthority && !authorityIsConflict ? (
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
            {!authorityIsConflict
              ? fields.map((name) => {
                  if (isAuthority && name === "city") {
                    return (
                      <div
                        className="field operator-city-wrap"
                        key="city"
                        data-invalid={s.fieldErrors?.city ? "true" : undefined}
                      >
                        <CityField
                          label="规范城市"
                          value={v.city ?? ""}
                          allowEmpty={false}
                          disabled={frozen || t.reading}
                          onChange={(next) => {
                            setUnknownClosed(false);
                            t.edit(flow, "city", next);
                          }}
                        />
                        {matchedAuthorityCity ? (
                          <small className="ops-caption">
                            规范城市值：{matchedAuthorityCity.value}（区划代码{" "}
                            {matchedAuthorityCity.code}）
                          </small>
                        ) : (
                          <small className="ops-caption">
                            仅支持大陆城市目录中的规范城市；选择城市后须读取该城市规则的当前版本。
                          </small>
                        )}
                        {s.fieldErrors?.city ? (
                          <small className="field-error">{s.fieldErrors.city}</small>
                        ) : null}
                      </div>
                    );
                  }
                  if (isAuthority && name === "actions") {
                    return (
                      <fieldset
                        key="actions"
                        className="operator-authority-actions"
                        disabled={frozen || t.reading}
                        data-invalid={s.fieldErrors?.actions ? "true" : undefined}
                      >
                        <legend>允许动作（至少选择一项）</legend>
                        <div className="operator-authority-checks">
                          {authorityActionOptions.map(([actionKey, actionLabel]) => {
                            const checked =
                              parsedAuthorityActions.valid.includes(actionKey);
                            return (
                              <Check
                                key={actionKey}
                                checked={checked}
                                disabled={frozen || t.reading}
                                onChange={(nextChecked) => {
                                  setUnknownClosed(false);
                                  const next = authorityActionOrder.filter((item) =>
                                    item === actionKey
                                      ? nextChecked
                                      : parsedAuthorityActions.valid.includes(item),
                                  );
                                  t.edit(flow, "actions", next.join(","));
                                }}
                              >
                                {actionLabel}
                              </Check>
                            );
                          })}
                        </div>
                        {s.fieldErrors?.actions ? (
                          <small className="field-error">
                            {s.fieldErrors.actions}
                          </small>
                        ) : null}
                      </fieldset>
                    );
                  }
                  return (
                    <Field
                      key={name}
                      label={labels[name]}
                      value={v[name] ?? ""}
                      disabled={frozen || t.reading}
                      error={s.fieldErrors?.[name]}
                      inputMode={
                        (isAge && name === "actor_id") ||
                        (isAuthority && (name === "actor_id" || name === "pet_id"))
                          ? "numeric"
                          : undefined
                      }
                      onChange={(value) => {
                        if (isAge || isAuthority) setUnknownClosed(false);
                        t.edit(flow, name, value);
                      }}
                      hint={
                        name.endsWith("until") || name === "occurred_at"
                          ? isAuthority
                            ? "例如 2026-11-30T18:00:00+08:00"
                            : "例如 2026-11-01T10:00:00+08:00"
                          : undefined
                      }
                    />
                  );
                })
              : null}
            {isAuthority && !authorityIsConflict ? (
              <div className="operator-authority-lookup">
                <div className="ops-actions">
                  <Button
                    kind={authorityHasLookup ? "secondary" : "primary"}
                    disabled={frozen || t.reading}
                    onClick={() => {
                      setUnknownClosed(false);
                      void t.lookupAuthority();
                    }}
                  >
                    {authorityHasLookup
                      ? "重新读取当前授权版本"
                      : "读取当前授权版本"}
                  </Button>
                </div>
                {t.reading && s.stage !== "unknown" ? (
                  <p role="status">正在读取当前授权版本…</p>
                ) : null}
                {authorityHasLookup && s.lookup ? (
                  renderAuthoritySnapshot(s.lookup, "已读取当前授权版本")
                ) : (
                  <p className="ops-caption">
                    尚未读取当前自然键的真实授权版本；未读取前不能确认或提交。
                  </p>
                )}
              </div>
            ) : null}
            {!(isAge || isAuthority) && (s.error || t.readError) ? (
              <OpsNotice title={s.error || t.readError} error />
            ) : null}
            {(isAge || isAuthority) &&
            s.stage !== "rejected" &&
            (isAuthority && s.stage === "unknown"
              ? s.error
              : s.error || t.readError) ? (
              <OpsNotice
                title={
                  (isAuthority && s.stage === "unknown"
                    ? s.error
                    : s.error || t.readError) || ""
                }
                error
              />
            ) : null}
            {isAge && s.stage === "rejected" ? (
              <OpsNotice title="登记明确失败，服务端未接受" error>
                <p>
                  失败原因：{s.error || "服务端未接受本次登记。"}
                  不会自动重发原操作；仍有权限的输入已保留，修正后请重新进入完整核对并明确勾选确认。
                </p>
              </OpsNotice>
            ) : null}
            {authorityIsConflict ? (
              <OpsNotice title="当前版本已变化 · 不能沿用旧版本" error>
                <p>
                  {s.error ||
                    "当前授权版本已变化，旧版本与旧准备内容已清除。"}
                </p>
                <dl>
                  <div className="ops-summary">
                    <dt>本次冻结版本</dt>
                    <dd>v{String(s.conflictVersion)}</dd>
                  </div>
                  <div className="ops-summary">
                    <dt>当前对象</dt>
                    <dd>{authorityTargetSummary(v)}</dd>
                  </div>
                  <div className="ops-summary">
                    <dt>本次结果</dt>
                    <dd>明确冲突，未登记成功</dd>
                  </div>
                  <div className="ops-summary">
                    <dt>下一步</dt>
                    <dd>按原自然键重新读取，再由本人重新确认</dd>
                  </div>
                </dl>
                {t.readError ? <OpsNotice title={t.readError} error /> : null}
                <div className="ops-actions">
                  <Button
                    kind="primary"
                    disabled={t.reading}
                    onClick={() => void t.lookupAuthority()}
                  >
                    重新读取当前授权
                  </Button>
                  <Button disabled={t.reading} onClick={() => t.backToEdit()}>
                    修改对象或输入
                  </Button>
                  {go ? (
                    <Button disabled={t.reading} onClick={() => go("dashboard")}>
                      返回本岗工作台
                    </Button>
                  ) : null}
                </div>
              </OpsNotice>
            ) : null}
            {isAuthority && s.stage === "rejected" && !authorityIsConflict ? (
              <OpsNotice title="本次登记未被接受" error>
                <p>
                  失败原因：{s.error || "服务端明确拒绝本次授权登记。"}
                  不会自动重发原操作；仍有权限的输入已保留，请修正后重新核对当前版本并确认提交。
                </p>
                <dl>
                  <div className="ops-summary">
                    <dt>原对象与范围</dt>
                    <dd>{authorityTargetSummary(v)}</dd>
                  </div>
                  <div className="ops-summary">
                    <dt>本次结果</dt>
                    <dd>明确失败，未登记成功</dd>
                  </div>
                  <div className="ops-summary">
                    <dt>下一步</dt>
                    <dd>修正字段或重新读取当前自然键版本后重新核对</dd>
                  </div>
                </dl>
                {t.readError ? <OpsNotice title={t.readError} error /> : null}
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
              ) : isAuthority ? (
                <OpsNotice
                  title={
                    ack
                      ? "已明确核对，可确认提交本次授权"
                      : "核对后再明确提交"
                  }
                >
                  <p>
                    当前对象、范围、已读版本与有效截止已按本次输入固定。未勾选核对确认前禁止写入；取消勾选将立即退回未确认状态。
                  </p>
                  {renderAuthoritySummary(s.prepared?.body, s.lookup)}
                  <Check checked={ack} onChange={setAck}>
                    我已核对本次对象、范围、已读版本与真实证据，确认提交
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
            {(s.stage === "edit" || s.stage === "rejected") && !authorityIsConflict ? (
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
              ) : isAuthority ? (
                <div className="ops-actions">
                  <Button
                    kind={authorityHasLookup ? "primary" : "secondary"}
                    disabled={t.locked || t.reading || !authorityHasLookup}
                    onClick={() => {
                      setAck(false);
                      setUnknownClosed(false);
                      t.prepare(flow);
                    }}
                  >
                    核对本次内容
                  </Button>
                  {s.stage === "rejected" ? (
                    <Button disabled={t.reading} onClick={() => t.backToEdit()}>
                      返回修改
                    </Button>
                  ) : null}
                  {go ? (
                    <Button disabled={t.reading} onClick={() => go("dashboard")}>
                      返回本岗工作台
                    </Button>
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
              isAge || isAuthority ? (
                <OpsNotice title="正在提交原内容">
                  <p role="status">
                    正在提交原内容，请等待服务端确定响应；等待期间不可修改、重发或切换岗位…
                  </p>
                  {isAge
                    ? renderAgeSummary(s.prepared?.body)
                    : renderAuthoritySummary(s.prepared?.body, s.lookup)}
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
              ) : isAuthority ? (
                <OpsNotice title="已收到服务端接受响应">
                  <p>
                    服务端已返回真实接受响应（记录编号：
                    {s.result?.id ? `#${String(s.result.id)}` : "已受理"}
                    {s.result?.version
                      ? ` · 最新版本 v${String(s.result.version)}`
                      : ""}
                    ）。这仅证明本次授权登记已被受理，不代表全链条已自动放行，也不替代其他审核或同意条件。
                  </p>
                  {renderAuthoritySummary(s.prepared?.body, s.lookup)}
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
              ) : isAuthority ? (
                <OpsNotice
                  title={
                    unknownAck
                      ? "已人工核对原事项（仅结束本机待办）"
                      : "原登记结果待核对"
                  }
                >
                  <p>
                    原对象、提交前版本与输入内容已冻结保留。授权接口无独立操作回执，请先在原事项内读取当前授权状态并结合可信处理记录人工核对，禁止盲目重发，也不能因版本或对象变化推定本次提交成功。
                  </p>
                  {renderAuthoritySummary(s.prepared?.body, s.lookup)}
                  <div className="ops-actions">
                    <Button
                      disabled={t.reading}
                      onClick={() => void t.checkAuthorityUnknown()}
                    >
                      {t.reading ? "正在读取原对象…" : "读取原对象当前授权状态"}
                    </Button>
                  </div>
                  {t.readError ? <OpsNotice title={t.readError} error /> : null}
                  {s.unknownChecked ? (
                    <div className="operator-authority-unknown-check">
                      {renderAuthoritySnapshot(
                        s.unknownChecked,
                        "原对象当前授权回读（仅供参考，非独立操作回执）",
                      )}
                      <p className="ops-caption">
                        注意：即使回读到记录编号或版本已变化，也不能单独证明是本次未知操作导致；请结合可信审计与处理记录核对。
                      </p>
                    </div>
                  ) : null}
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
