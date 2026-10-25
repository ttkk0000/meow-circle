import { useEffect, useRef, useState } from "react";
import {
  Button,
  Card,
  Choices,
  Field,
  Modal,
  Notice,
  RowLink,
  TaskPage,
} from "../components/ui";
import { Facts, SchemaForm, schemaDefault } from "../components/SchemaForm";
import { navigate, useRoute } from "../core/router";
import { encodeWire, integer, parseWire } from "../core/wire";

interface OperatorContext {
  id: string;
  roles: string[];
  operator_name: string;
  operator_address: string;
  operator_contact: string;
  published_types: string[];
}
interface Queue {
  items: Array<Record<string, unknown>>;
  next_cursor: bigint;
}
interface Prepared {
  path: string;
  body: Record<string, unknown>;
  title: string;
}

/** 运营凭据仅驻留此组件内存；请求不携带消费者Cookie，也不经过消费者路由白名单。 */
export default function OperatorApp() {
  const route = useRoute();
  const params = new URLSearchParams(route.split("?")[1]);
  const view = params.get("view") ?? "readiness";
  const credential = useRef("");
  const generation = useRef(0);
  const requests = useRef(new Set<AbortController>());
  const [input, setInput] = useState("");
  const [context, setContext] = useState<OperatorContext>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [queue, setQueue] = useState<Queue>({ items: [], next_cursor: 0n });
  const [kind, setKind] = useState("rights-cases");
  const [id, setID] = useState("");
  const [detail, setDetail] = useState<Record<string, unknown>>();
  const [values, setValues] = useState<Record<string, unknown>>({});
  const [prepared, setPrepared] = useState<Prepared>();
  const disconnect = () => {
    generation.current++;
    requests.current.forEach((request) => request.abort());
    requests.current.clear();
    credential.current = "";
    setInput("");
    setContext(undefined);
    setDetail(undefined);
    setValues({});
    setQueue({ items: [], next_cursor: 0n });
    setPrepared(undefined);
    setError("");
    setNotice("");
    setBusy(false);
  };
  useEffect(
    () => () => {
      generation.current++;
      credential.current = "";
      requests.current.forEach((request) => request.abort());
    },
    [],
  );
  useEffect(() => {
    setDetail(undefined);
    setPrepared(undefined);
    setValues({});
    setError("");
  }, [view, kind]);

  async function request<T>(
    path: string,
    body?: Record<string, unknown>,
  ): Promise<T> {
    if (
      !/^(?:context|guardian-invites|guardian-evidence|reviews(?:\/\d+)?|rights-cases(?:\/\d+(?:\/decision)?)?|data-requests\/\d+(?:\/(?:process|hold))?)(?:\?cursor=\d+)?$/.test(
        path,
      )
    )
      throw new Error("运营操作入口无效");
    const token = credential.current;
    if (token.length < 32) throw new Error("请先提供本机运营访问凭据");
    const epoch = generation.current;
    const controller = new AbortController();
    requests.current.add(controller);
    try {
      const response = await fetch(`/api/v2/ops/${path}`, {
        method: body ? "POST" : "GET",
        credentials: "omit",
        referrerPolicy: "no-referrer",
        cache: "no-store",
        signal: controller.signal,
        headers: {
          "X-Operator-Token": token,
          ...(body ? { "Content-Type": "application/json" } : {}),
        },
        body: body ? encodeWire(body) : undefined,
      });
      const raw = await response.text();
      if (generation.current !== epoch)
        throw new DOMException("已关闭运营会话", "AbortError");
      if (raw.length > 2 * 1024 * 1024)
        throw new Error("运营响应过大，请缩小范围");
      const envelope = parseWire(raw) as { ok: boolean; data: T };
      if (!response.ok || !envelope.ok)
        throw new Error(
          response.status === 403
            ? "当前凭据没有此事项角色权限"
            : response.status === 409
              ? "记录已变化，请读取最新版本后重新核对"
              : "运营请求未完成，请保留本次事项并重新读取",
        );
      return envelope.data;
    } finally {
      requests.current.delete(controller);
    }
  }
  async function run(action: () => Promise<void>) {
    const epoch = generation.current;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await action();
    } catch (failure) {
      if (
        generation.current === epoch &&
        !(failure instanceof DOMException && failure.name === "AbortError")
      )
        setError(failure instanceof Error ? failure.message : "操作未完成");
    } finally {
      if (generation.current === epoch) setBusy(false);
    }
  }
  async function readQueue(more = false) {
    const next = await request<Queue>(
      `${kind}?cursor=${more ? queue.next_cursor : 0n}`,
    );
    setQueue({
      items: more ? [...queue.items, ...next.items] : next.items,
      next_cursor: next.next_cursor,
    });
  }
  async function readDetail(value = id) {
    if (!/^\d+$/.test(value) || BigInt(value) <= 0n)
      throw new Error("请输入有效事项编号");
    setID(value);
    setDetail(undefined);
    setPrepared(undefined);
    if (kind === "reviews" || kind === "guardian-invites") {
      const row = queue.items.find(
        (item) => integer(item.id) === BigInt(value),
      );
      if (!row) throw new Error("请先在当前已读取队列中选择核实事项");
      setDetail(row);
      setValues(
        kind === "reviews"
          ? { ...schemaDefault("ReviewDecision"), version: row.version }
          : {
              ...schemaDefault("GuardianEvidenceInput"),
              invite_id: row.id,
              invite_version: row.version,
              guardian_id: row.guardian_id,
              child_id: row.child_id,
              policy_version: row.policy_version,
            },
      );
    } else {
      const row = await request<Record<string, unknown>>(`${kind}/${value}`);
      setDetail(row);
      const receipt = (row.receipt ?? row) as Record<string, unknown>;
      setValues({
        ...schemaDefault(
          kind === "rights-cases" ? "RightsDecision" : "DataHoldInput",
        ),
        version: receipt.version,
      });
    }
  }
  const role =
    kind === "data-requests"
      ? "privacy"
      : kind === "guardian-invites"
        ? "identity_verifier"
        : "moderator";
  const canWork = !!context?.roles.includes(role);
  const go = (next: string) => {
    if (!busy) navigate(`/operator?view=${next}`);
  };
  const title =
    view === "case"
      ? "运营案件与资料保全"
      : view === "emergency"
        ? "安全事件响应"
        : "运营准入核对";

  return (
    <main className="operator-shell">
      <TaskPage
        title={title}
        subtitle="M&amp;D · meow &amp; doggie"
        actions={
          <>
            <a className="button secondary" href="/app/ops">
              技术运维
            </a>
            {context ? (
              <>
                <Button disabled={busy} onClick={() => go("readiness")}>
                  返回运营核对
                </Button>
                <Button disabled={busy} onClick={disconnect}>
                  关闭运营会话
                </Button>
              </>
            ) : null}
          </>
        }
      >
        {!context ? (
          <Card title="仅授权运营人员">
            <p>使用部署方分配的具名运营凭据。消费者账号不能授予运营权限。</p>
            <Field
              label="本机运营访问凭据"
              type="password"
              value={input}
              onChange={setInput}
              disabled={busy}
            />
            <Button
              kind="primary"
              disabled={busy || input.trim().length < 32}
              onClick={() =>
                void run(async () => {
                  credential.current = input.trim();
                  setInput("");
                  try {
                    setContext(await request<OperatorContext>("context"));
                  } catch (failure) {
                    credential.current = "";
                    throw failure;
                  }
                })
              }
            >
              读取本人权限与材料
            </Button>
          </Card>
        ) : (
          <>
            <Notice title={`当前运营人员：${context.id}`}>
              角色：{context.roles.join("、")}
              。材料访问与处理仍由服务端逐次校验。
            </Notice>
            {view === "readiness" ? (
              <>
                <Card title="主体与已发布材料">
                  <Facts
                    value={{
                      operator_name: context.operator_name || "尚未配置",
                      operator_address: context.operator_address || "尚未配置",
                      operator_contact: context.operator_contact || "尚未配置",
                      published_types: context.published_types,
                    }}
                  />
                </Card>
                <Card title="逐项核对">
                  <Facts
                    value={{
                      "主体／经营":
                        "核对主体、备案／适用许可、平台性质与双城业务",
                      未成年人: "核对通道、保护责任人、审计与报告安排",
                      数据与供应商:
                        "核对处理清单、协议、影响评估与实际清理结果",
                      "税务／规则": "核对适用结论、报送与版本发布记录",
                    }}
                  />
                </Card>
                <RowLink
                  title="风险工单与留证"
                  detail="责任人、升级与复核"
                  onClick={() => go("case")}
                />
                <RowLink
                  title="安全事件分流"
                  detail="实际等级、报告对象与处理时限"
                  onClick={() => go("emergency")}
                />
                <Notice title="资料缺失不放行">
                  这里展示配置事实与核对入口，不以样例或一键勾选产生准入结论。
                </Notice>
              </>
            ) : view === "emergency" ? (
              <>
                <Notice title="仅授权运营人员">
                  发现或获知后及时研判、止损、保护必要记录并通知负责人或替补。
                </Notice>
                <Card title="事件响应">
                  <Facts
                    value={{
                      报告分流: "按实际事件等级、主体及适用规则确认对象与时限",
                      个人信息事件: "补救、通知和报告分别处理",
                      供应商与总结: "核对合同、通报、补报与总结记录",
                    }}
                  />
                </Card>
                <Notice title="不等待普通客服窗口">
                  普通首响目标不代替依法及时处置；实际报告渠道和责任人应在部署运营材料中配置。
                </Notice>
                <RowLink
                  title="查看案件与交接"
                  detail="原事项、责任与最小范围保全"
                  onClick={() => go("case")}
                />
              </>
            ) : (
              <>
                <Choices
                  label="事项类型"
                  value={kind}
                  onChange={(value) => {
                    setKind(value);
                    setQueue({ items: [], next_cursor: 0n });
                  }}
                  disabled={busy}
                  choices={[
                    ["rights-cases", "侵权通知链"],
                    ["reviews", "核实与申诉"],
                    ["data-requests", "删除请求与保全"],
                    ["guardian-invites", "待核对监护关系"],
                  ]}
                />
                {!canWork ? (
                  <Notice title="当前角色无权读取此类事项" />
                ) : (
                  <>
                    {kind !== "data-requests" ? (
                      <>
                        <Button
                          disabled={busy}
                          onClick={() => void run(() => readQueue())}
                        >
                          读取事项队列
                        </Button>
                        {queue.items.map((row) => (
                          <RowLink
                            key={String(row.id)}
                            title={`事项 ${row.id} · ${row.state ?? "待核对"}`}
                            detail={String(row.summary ?? "读取原对象与版本")}
                            onClick={() => {
                              if (!busy)
                                void run(() => readDetail(String(row.id)));
                            }}
                          />
                        ))}
                        {queue.next_cursor > 0n ? (
                          <Button
                            disabled={busy}
                            onClick={() => void run(() => readQueue(true))}
                          >
                            载入更多
                          </Button>
                        ) : null}
                      </>
                    ) : null}
                    <Field
                      label="原事项编号"
                      value={id}
                      onChange={(value) => {
                        setID(value);
                        setDetail(undefined);
                        setPrepared(undefined);
                      }}
                      disabled={busy}
                    />
                    <Button
                      disabled={busy}
                      onClick={() => void run(() => readDetail())}
                    >
                      读取原事项与版本
                    </Button>
                    {detail ? (
                      <>
                        <Card title="当前对象与保全清单">
                          <Facts value={detail} />
                        </Card>
                        <Card
                          title={
                            kind === "data-requests"
                              ? "记录具体保全范围"
                              : "事实核对与处理决定"
                          }
                        >
                          <SchemaForm
                            name={
                              kind === "guardian-invites"
                                ? "GuardianEvidenceInput"
                                : kind === "reviews"
                                  ? "ReviewDecision"
                                  : kind === "rights-cases"
                                    ? "RightsDecision"
                                    : "DataHoldInput"
                            }
                            value={values}
                            omit={
                              kind === "guardian-invites"
                                ? [
                                    "invite_id",
                                    "invite_version",
                                    "guardian_id",
                                    "child_id",
                                    "policy_version",
                                  ]
                                : []
                            }
                            onChange={setValues}
                            disabled={busy || !!prepared}
                          />
                          <Button
                            disabled={busy || !!prepared}
                            onClick={() =>
                              setPrepared({
                                path:
                                  kind === "guardian-invites"
                                    ? "guardian-evidence"
                                    : kind === "reviews"
                                      ? `reviews/${id}`
                                      : `${kind}/${id}/${kind === "rights-cases" ? "decision" : "hold"}`,
                                body: values,
                                title: "核对本次版本与处理范围",
                              })
                            }
                          >
                            核对本次处理
                          </Button>
                          {kind === "data-requests" ? (
                            <Button
                              disabled={busy || !!prepared}
                              onClick={() =>
                                setPrepared({
                                  path: `${kind}/${id}/process`,
                                  body: { version: detail.version },
                                  title: "核对选择性处理",
                                })
                              }
                            >
                              处理当前请求的可处理项目
                            </Button>
                          ) : null}
                        </Card>
                      </>
                    ) : null}
                  </>
                )}
                <RowLink
                  title="紧急安全事件"
                  detail="按实际等级升级"
                  onClick={() => go("emergency")}
                />
              </>
            )}
          </>
        )}
        {busy ? <p role="status">正在读取或提交，请保留当前事项…</p> : null}
        {error ? <Notice title={error} error /> : null}
        {notice ? <Notice title={notice} /> : null}
        {prepared ? (
          <Modal
            title={prepared.title}
            busy={busy}
            onClose={() => {
              if (!busy) setPrepared(undefined);
            }}
            actions={
              <Button
                kind="primary"
                disabled={busy}
                onClick={() =>
                  void run(async () => {
                    await request(prepared.path, prepared.body);
                    setPrepared(undefined);
                    setNotice("服务已接受处理，请重新读取当前事项确认结果");
                    if (kind === "guardian-invites") {
                      setDetail(undefined);
                      setValues({});
                      await readQueue();
                    } else await readDetail();
                  })
                }
              >
                确认提交此版本
              </Button>
            }
          >
            <Facts value={prepared.body} />
            <p>仅提交本次事项及所读版本。请求失败后先重新读取，不自动重发。</p>
            {kind === "guardian-invites" ? (
              <Notice title="只记录已核实的监护关系与必要证据引用">
                请先依实际核验流程完成关系核实，不在此填写原始证件、照片或完整身份资料。本操作不会替监护人同意，后续仍需本人核对规则与授权范围。
              </Notice>
            ) : null}
          </Modal>
        ) : null}
      </TaskPage>
    </main>
  );
}
