import { useEffect, useRef, useState } from "react";
import { Button, Check, Field } from "../components/ui";
import { DataTable, OpsNotice, Panel } from "../operations/ui";
import { id as validID, integer, key, money } from "../core/wire";
import { OperatorError } from "./client";
import { bodyOf, time, type Row, type View } from "./models";
import type { OperatorSession } from "./useOperator";

export type RoleTask = "ageEvidence" | "authority" | "received" | "refund";
type Snapshot = { exists: boolean; record?: Row };
type TaskState = {
  flow?: RoleTask;
  values: Record<string, string>;
  stage: "edit" | "review" | "sending" | "unknown" | "accepted" | "rejected";
  prepared?: { path: string; body: Row };
  error?: string;
  result?: Row;
  lookup?: Snapshot;
  lookupKey?: string;
};
const initial = (): TaskState => ({ values: {}, stage: "edit" });
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
export function useRoleTasks(session: OperatorSession) {
  const [state, setState] = useState<TaskState>(initial);
  const current = useRef(state);
  current.current = state;
  const lastIdentity = useRef(session.context?.id);
  const [lostReference, setLostReference] = useState<string>();
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
    if (
      lastIdentity.current &&
      lastIdentity.current === session.context?.id &&
      ["unknown", "sending"].includes(pending.stage)
    ) {
      setLostReference(
        `${pending.flow ?? "原任务"} / ${String(pending.prepared?.body.client_request_id ?? pending.lookup?.record?.id ?? pending.prepared?.body.actor_id ?? "请按原证据核对")}`,
      );
    } else if (lastIdentity.current !== session.context?.id)
      setLostReference(undefined);
    lastIdentity.current = session.context?.id;
    readSequence.current++;
    readBusy.current = false;
    writing.current = false;
    setState(initial());
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
      ["review", "unknown"].includes(current.current.stage)
    )
      return;
    const old = current.current.flow === flow ? current.current : initial();
    const natural = ["scope", "actor_id", "pet_id", "city", "species"].includes(
      name,
    );
    setState({
      ...old,
      flow,
      values: { ...old.values, [name]: value },
      stage: "edit",
      error: undefined,
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
    if (
      !permitted(taskRole(flow)) ||
      writing.current ||
      current.current.stage === "unknown"
    )
      return;
    const old =
        current.current.flow === flow
          ? current.current
          : { ...initial(), flow },
      v = old.values;
    const text = (name: string, max: number) => {
      const value = (v[name] ?? "").trim();
      if (!value || Array.from(value).length > max)
        throw Error("请核对必要说明和字数限制。");
      return value;
    };
    const id = (name: string) => {
      return validID(v[name]);
    };
    const instant = (name: string, future: boolean, maxDays = Infinity) => {
      const ms = Date.parse(v[name] ?? "");
      if (
        !Number.isFinite(ms) ||
        (future
          ? ms <= Date.now() || ms > Date.now() + maxDays * 86400000
          : ms > Date.now())
      )
        throw Error("请核对实际时间或有效期限。");
      return new Date(ms).toISOString();
    };
    try {
      let path: string, body: Row;
      if (flow === "ageEvidence") {
        const band = v.age_band ?? "";
        if (!["under14", "14_17", "adult"].includes(band))
          throw Error("请选择有效的年龄区间。");
        path = "age-evidence";
        body = {
          actor_id: id("actor_id"),
          age_band: band,
          evidence_ref: text("evidence_ref", 120),
          valid_until: instant("valid_until", true, 366),
        };
      } else if (flow === "authority") {
        if (!old.lookup || old.lookupKey !== lookupQuery(v))
          throw Error("请先按当前对象与范围读取授权记录，再核对所读版本。");
        const scope = v.scope || "disposition",
          species = v.species || "cat";
        if (
          !["care", "disposition", "city_rule"].includes(scope) ||
          !["cat", "dog"].includes(species)
        )
          throw Error("请选择有效的授权类型和物种。");
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
          throw Error("城市规则须明确允许的领养、有偿或线下动作。");
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
          throw Error("请核对实际金额，单笔不得超过100000000分。");
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
      });
    } catch (error) {
      setState({ ...old, flow, error: failText(error) });
    }
  }
  async function submit() {
    const old = current.current;
    if (
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
    edit,
    prepare,
    submit,
    lookupAuthority,
    readRevenue,
    locked:
      writing.current || ["review", "sending", "unknown"].includes(state.stage),
    reset: () => {
      if (!writing.current) setState(initial());
    },
    cancelReview: () => {
      if (!writing.current)
        setState((s) => ({ ...s, stage: "edit", prepared: undefined }));
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
}: {
  flow: RoleTask;
  tasks: Tasks;
}) {
  const [ack, setAck] = useState(false),
    s = t.state.flow === flow ? t.state : initial(),
    v = s.values;
  const [unknownAck,setUnknownAck] = useState(false);
  const [showLedger,setShowLedger] = useState(false);
  useEffect(() => { setUnknownAck(false); setShowLedger(false); },[flow,s.stage,s.prepared]);
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
  const choose = (
    name: string,
    label: string,
    options: Array<[string, string]>,
    fallback: string,
  ) => (
    <label className="field" key={name}>
      <span>{label}</span>
      <select
        disabled={frozen || t.reading}
        value={v[name] ?? fallback}
        onChange={(e) => t.edit(flow, name, e.target.value)}
      >
        {options.map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>
    </label>
  );
  return (
    <>
      <OpsNotice
        title={
          flow === "authority"
            ? "先读取对象与范围，再核对版本"
            : flow === "ageEvidence"
              ? "可信年龄区间，不代替本人授权"
              : "只登记已经发生的实际事实"
        }
      >
        {flow === "authority"
          ? "仅按明确对象读取当前授权；登记不等于全链条准入。"
          : flow === "ageEvidence"
            ? "不录入原始证件或生日，关系证据与监护人同意分别处理。"
            : "登记不会触发扣款或退款；报价与到访不计收入，退款必须关联原实际收款。"}
      </OpsNotice>
      {t.state.flow && t.state.flow !== flow && t.locked ? (
        <OpsNotice title="另一个任务仍待核对">
          请返回原任务核对结果，不能另发同类操作。
        </OpsNotice>
      ) : (
        <Panel title="当前任务与必要字段">
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
              onChange={(value) => t.edit(flow, name, value)}
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
          {s.error || t.readError ? (
            <OpsNotice title={s.error || t.readError} error />
          ) : null}
          {s.stage === "review" ? (
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
                onClick={() => void t.submit()}
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
          ) : null}
          {s.stage === "edit" || s.stage === "rejected" ? (
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
          ) : null}
          {s.stage === "sending" ? (
            <p role="status">正在提交原内容，请等待确定响应…</p>
          ) : null}
          {s.stage === "accepted" ? (
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
          ) : null}
          {s.stage === "unknown" ? (
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
                  t.reset();
                }}
              >
                结束本机待核对
              </Button>
            </OpsNotice>
          ) : null}
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
