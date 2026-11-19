import { useEffect, useState } from "react";
import { Button, Field, Modal } from "../components/ui";
import { OpsLink, useOps } from "./context";
import { CreateRelease, IncidentAction } from "./ActionForms";
import { isCancelled, message, OpsError } from "./client";
import { EnvironmentNotice, SystemState } from "./OverviewPages";
import {
  formatTime,
  jobNames,
  stateLabel,
  type Document,
  type Incident,
  type Preflight,
  type Release,
  type Resource,
  type Run,
} from "./models";
import {
  DataTable,
  EmptyState,
  Metric,
  OpsNotice,
  Panel,
  Status,
  Summary,
} from "./ui";

function useDocument<T>(kind: Resource) {
  const { data, demo, id, session } = useOps(),
    source = data?.[kind].items.find((r) => r.id === id) as
      | Document<T>
      | undefined;
  const [remote, setRemote] = useState<{
      id: string;
      epoch: number;
      record: Document<T>;
    }>(),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(false),
    [revision, setRevision] = useState(0);
  const canRead = demo || !!session.context;
  const cached =
    remote?.id === id && remote.epoch === session.client.epoch
      ? remote.record
      : undefined;
  const record = canRead
    ? cached && BigInt(cached.version) >= BigInt(source?.version ?? "0")
      ? cached
      : source
    : undefined;
  useEffect(() => {
    let active = true;
    setError("");
    setLoading(false);
    if (demo || !session.context || !id || (source && revision === 0)) return;
    const epoch = session.client.epoch;
    setLoading(true);
    void session
      .readDocument<T>(kind, id)
      .then((row) => {
        if (active && epoch === session.client.epoch)
          setRemote({ id, epoch, record: row });
      })
      .catch((e) => {
        if (active && epoch === session.client.epoch && !isCancelled(e)) {
          setError(message(e));
          if (e instanceof OpsError && (e.status === 401 || e.status === 403))
            session.report(
              e,
              `${session.context?.environment ?? "当前环境"} / ${kind === "releases" ? "发布核对" : "事件处理"}`,
            );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [kind, id, demo, session.context, source, revision]);
  return {
    record,
    error,
    loading,
    revision,
    reload: () => {
      setRemote(undefined);
      setRevision((value) => value + 1);
    },
    apply: (row: Document<T>) => {
      setRemote({ id: row.id, epoch: session.client.epoch, record: row });
    },
  };
}
function MissingDetail({
  kind,
  error,
  loading,
  onRetry,
}: {
  kind: "releases" | "alerts";
  error: string;
  loading: boolean;
  onRetry?: () => void;
}) {
  return (
    <Panel
      title={
        loading
          ? "正在读取记录"
          : error
            ? kind === "releases"
              ? "发布记录读取失败"
              : "记录读取失败"
            : "请先选择记录"
      }
    >
      <EmptyState
        title={
          error || (loading ? "正在核对权限与记录版本" : "当前没有可展示的详情")
        }
      >
        <div className="ops-actions">
          {error && onRetry ? (
            <Button kind="primary" disabled={loading} onClick={onRetry}>
              重新读取
            </Button>
          ) : null}
          <OpsLink view={kind} button>
            返回列表选择
          </OpsLink>
        </div>
      </EmptyState>
    </Panel>
  );
}
function usePreflight(record?: Document<Release>, revision = 0) {
  const { demo, session } = useOps(),
    [state, setState] = useState<{
      id: string;
      epoch: number;
      value: Preflight;
    }>(),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(false);
  useEffect(() => {
    let active = true;
    setState(undefined);
    setError("");
    if (!record || demo || !session.context) {
      setLoading(false);
      return;
    }
    const epoch = session.client.epoch;
    setLoading(true);
    void session.client
      .request<Preflight>(`releases/${record.id}/preflight`)
      .then((value) => {
        if (active && epoch === session.client.epoch)
          setState({ id: record.id, epoch, value });
      })
      .catch((e) => {
        if (active && epoch === session.client.epoch && !isCancelled(e)) {
          setError(message(e));
          if (e instanceof OpsError && (e.status === 401 || e.status === 403))
            session.report(
              e,
              `${session.context?.environment ?? "当前环境"} / 发布前置核对`,
            );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [record?.id, record?.version, demo, session.context, revision]);
  const preview: Preflight | undefined =
    demo && record
      ? {
          release: record,
          can_execute: false,
          checks: [
            {
              id: "environment",
              label: "目标环境与配置",
              state: "missing",
              detail: "环境身份未回读",
            },
            {
              id: "artifact",
              label: "构件与迁移计划",
              state: "missing",
              detail: "构件摘要未提供",
            },
            {
              id: "recovery",
              label: "恢复方案与审批",
              state: "missing",
              detail: "备份、恢复与审批待补齐",
            },
            {
              id: "executor",
              label: "发布执行后端",
              state: "unavailable",
              detail: "未接入，不执行部署",
            },
          ],
        }
      : undefined;
  return {
    preflight:
      preview ??
      (state?.id === record?.id && state?.epoch === session.client.epoch
        ? state?.value
        : undefined),
    error,
    loading,
  };
}
function latestRelease(
  record: Document<Release>,
  preflight?: Preflight,
): Document<Release> {
  if (
    preflight &&
    preflight.release.id === record.id &&
    BigInt(preflight.release.version) >= BigInt(record.version)
  ) {
    return preflight.release;
  }
  return record;
}
export function ReleaseDetail() {
  const [editing, setEditing] = useState(false);
  const { demo, session, connect } = useOps(),
    detail = useDocument<Release>("releases"),
    { preflight, error, loading: preflightLoading } = usePreflight(
      detail.record,
      detail.revision,
    ),
    record = detail.record;
  if (!demo && !session.context)
    return <SystemState onConnect={connect} returnToReleases />;
  if (!record)
    return (
      <MissingDetail
        kind="releases"
        error={detail.error}
        loading={detail.loading}
        onRetry={detail.reload}
      />
    );
  const activeRecord = latestRelease(record, preflight),
    r = activeRecord.data,
    canWrite = !demo && !!session.context?.capabilities.release_write,
    canEdit = canWrite && activeRecord.state === "blocked";
  return (
    <>
      <div className="ops-page-actions">
        <OpsLink view="releases" button>
          返回发布记录
        </OpsLink>
      </div>
      <EnvironmentNotice
        title={
          error
            ? "前置核对读取失败（发布保持阻止）"
            : preflightLoading
              ? "正在读取服务端前置核对（发布保持阻止）"
              : activeRecord.state === "blocked"
                ? "当前发布被前置条件阻止"
                : `当前发布状态：${stateLabel(activeRecord.state)}（未接入执行器）`
        }
      >
        {error
          ? `当前记录 ${activeRecord.id} 未能读取服务端前置核对结论，不推算为可发布或已成功；请重新读取核对。`
          : `当前记录 ${activeRecord.id}（记录版本 v${activeRecord.version}）。没有发布或重启执行器；填写摘要或证据引用不代表通过外部核验。`}
      </EnvironmentNotice>
      <div className="ops-columns">
        <Panel
          title="发布对象"
          description="环境、版本、构件摘要、备份恢复与审批证据分别展示与核对"
        >
          <dl>
            <Summary label="目标环境">
              {demo
                ? "staging · 占位 / mnd.example.invalid"
                : `${session.context?.environment || "未配置"} / ${session.context?.origin || "域名未配置"}`}
            </Summary>
            <Summary label="候选版本与记录状态">
              {r.version} · 登记人 {r.creator} · 记录 v{activeRecord.version} ·{" "}
              <Status state={activeRecord.state} />
            </Summary>
            <Summary label="构件摘要（SHA-256）">
              <code className="ops-wrap">{r.artifact_sha256 || "未提供"}</code>
            </Summary>
            <Summary label="备份恢复证据">
              <span className="ops-wrap">{r.backup_ref || "未提供"}</span>
            </Summary>
            <Summary label="外部审批证据">
              <span className="ops-wrap">{r.approval_ref || "未提供"}</span>
            </Summary>
            <Summary label="变更原因">
              <span className="ops-wrap">{r.reason || "未提供"}</span>
            </Summary>
          </dl>
        </Panel>
        <Panel
          title="执行门槛"
          description="不可用、未读取或失败状态均明确阻止，不能只禁用按钮"
        >
          {preflightLoading && !preflight ? (
            <p className="ops-caption" role="status">
              正在读取服务端前置核对与执行门槛…
            </p>
          ) : null}
          {preflight?.checks.length ? (
            <ol className="ops-checklist">
              {preflight.checks.map((check, i) => (
                <li key={check.id}>
                  <strong>
                    {String(i + 1).padStart(2, "0")} {check.label}{" "}
                    <Status state={check.state} />
                  </strong>
                  <small>{check.detail}</small>
                </li>
              ))}
            </ol>
          ) : null}
          {error ? (
            <OpsNotice title="核对结果未读取" error>
              <p>{error}</p>
              <Button disabled={detail.loading} onClick={detail.reload}>
                重新读取
              </Button>
            </OpsNotice>
          ) : null}
        </Panel>
      </div>
      <DataTable
        title="前置核对清单"
        columns={["核对项", "证据来源", "当前结果", "放行状态", "下一步"]}
        dense
        hideFilters
        empty={
          error
            ? "前置核对读取失败"
            : preflightLoading
              ? "正在等待服务端前置核对"
              : "暂无服务端前置核对项"
        }
        emptyDescription={
          error
            ? "未能读取服务端核对结论，不推算为可发布或已验证；请点击上方重新读取。"
            : "等待服务端返回四项前置核对结果；缺失、仅引用待验证及执行器未接入均保持阻止。"
        }
        rows={(preflight?.checks ?? []).map((check) => ({
          key: check.id,
          state: check.state,
          search: "",
          cells: [
            check.label,
            check.id === "artifact"
              ? "版本摘要 / 迁移计划"
              : check.id === "recovery"
                ? "备份证据 / 审批记录"
                : check.id === "executor"
                  ? "发布执行器"
                  : "环境身份 / 配置引用",
            check.detail,
            <Status state={check.state} />,
            check.id === "environment" ? (
              <OpsLink view="access">查看环境与权限 →</OpsLink>
            ) : check.id === "artifact" ? (
              check.state === "missing" && canEdit ? (
                <Button kind="quiet" onClick={() => setEditing(true)}>
                  补齐不可变构件摘要 →
                </Button>
              ) : (
                <OpsLink view="migrations">查看迁移账本 →</OpsLink>
              )
            ) : check.id === "recovery" ? (
              check.state === "missing" && canEdit ? (
                <Button kind="quiet" onClick={() => setEditing(true)}>
                  补齐恢复证据与审批 →
                </Button>
              ) : check.state === "missing" ? (
                demo ? (
                  "补齐恢复证据与审批"
                ) : (
                  "需 ops_release 角色补齐引用"
                )
              ) : (
                "等待外部备份／审批核验接入"
              )
            ) : (
              "执行器未接入（保持禁用）"
            ),
          ],
        }))}
      />
      <div className="ops-actions ops-actions-split">
        <div className="ops-actions">
          <OpsLink view="confirm" id={activeRecord.id} button>
            查看发布确认
          </OpsLink>
          {demo || canWrite ? (
            <Button
              disabled={demo || !canEdit}
              onClick={() => setEditing(true)}
            >
              补齐发布资料
            </Button>
          ) : null}
        </div>
        {demo || canWrite ? <Button disabled>执行发布</Button> : null}
      </div>
      {!demo && !canWrite ? (
        <p className="ops-caption">
          当前账号无发布资料写入权限（需 ops_release）；网页没有发布或重启执行器，仅可查看核对详情。
        </p>
      ) : null}
      {editing && canEdit && session.context ? (
        <CreateRelease
          key={`${activeRecord.id}:${activeRecord.version}:${session.context.id}`}
          record={activeRecord}
          onClose={() => setEditing(false)}
          onSaved={(row) => detail.apply(row)}
        />
      ) : null}
    </>
  );
}
export function ConfirmRelease() {
  const { demo, session, connect } = useOps(),
    detail = useDocument<Release>("releases"),
    record = detail.record,
    { preflight, error: preflightError, loading: preflightLoading } =
      usePreflight(record, detail.revision),
    [reason, setReason] = useState(""),
    [target, setTarget] = useState("");
  if (!demo && !session.context)
    return <SystemState onConnect={connect} returnToReleases />;
  if (!record)
    return (
      <MissingDetail
        kind="releases"
        error={detail.error}
        loading={detail.loading}
        onRetry={detail.reload}
      />
    );
  const activeRecord = latestRelease(record, preflight),
    environment = demo
      ? "staging"
      : (session.context?.environment ?? "未配置"),
    canWrite = !demo && !!session.context?.capabilities.release_write,
    targetError =
      target && target.trim() !== environment
        ? `输入与当前固定环境 ${environment} 不一致；且即使一致也不授予执行权限。`
        : "",
    unmetChecks =
      preflight?.checks
        .filter((c) => c.state !== "verified")
        .map((c) => `${c.label}（${stateLabel(c.state)}）`) ?? [];
  return (
    <Panel
      title="确认发布申请"
      description="当前是阻止提交状态，没有执行接口。"
      className="ops-confirm"
    >
      <div className="ops-confirm-status">
        {preflightError ? (
          <Status state="failed">核对读取失败</Status>
        ) : preflightLoading && !preflight ? (
          <Status state="unknown">正在核对</Status>
        ) : (
          <Status state={activeRecord.state} />
        )}
      </div>
      <dl className="ops-columns">
        <Summary label="目标环境">
          {environment} /{" "}
          {demo
            ? "mnd.example.invalid"
            : session.context?.origin || "域名未配置"}
        </Summary>
        <Summary label="候选版本">
          {activeRecord.data.version} · {activeRecord.id}
        </Summary>
        <Summary label="构件摘要（SHA-256）">
          <code className="ops-wrap">
            {activeRecord.data.artifact_sha256 || "摘要未提供"}
          </code>
        </Summary>
        <Summary label="备份与恢复证据">
          <span className="ops-wrap">
            {activeRecord.data.backup_ref || "备份未提供"}
          </span>
        </Summary>
        <Summary label="外部审批证据">
          <span className="ops-wrap">
            {activeRecord.data.approval_ref || "审批未提供"}
          </span>
        </Summary>
        <Summary label="影响范围">API 与 worker / 范围待外部核对</Summary>
      </dl>
      {!demo && !canWrite ? (
        <p className="ops-caption">
          当前账号无发布准备写入权限（需 ops_release），且发布执行器尚未接入；本页仅用于只读核对。
        </p>
      ) : null}
      <div className="ops-form">
        <Field
          label="操作原因（仅本页核对）"
          multiline
          value={reason}
          onChange={setReason}
          disabled={!demo && !canWrite}
          placeholder="填写本次变更目的、风险与观察计划；不要输入密码或Token。"
          hint="本页输入不持久化、不提交；不要输入密码或Token。"
        />
        <Field
          label={`确认目标环境：${environment}`}
          value={target}
          onChange={setTarget}
          disabled={!demo && !canWrite}
          placeholder={`输入 ${environment} 以核对目标环境`}
          error={targetError}
          hint="输入环境名仅用于人工核对，不会授予执行权限。"
        />
      </div>
      {preflightError ? (
        <OpsNotice title="前置核对读取失败" error>
          <p>
            {preflightError}。未能确认服务端前置核对结论，发布执行保持阻止。
          </p>
          <Button disabled={detail.loading} onClick={detail.reload}>
            重新读取
          </Button>
        </OpsNotice>
      ) : preflightLoading && !preflight ? (
        <OpsNotice title="正在读取服务端前置核对">
          正在核对环境、构件与恢复证据状态；发布执行器与外部证据验证未接入，确认按钮保持关闭。
        </OpsNotice>
      ) : (
        <OpsNotice title="执行条件尚未满足">
          {unmetChecks.length
            ? `未满足条件：${unmetChecks.join("、")}`
            : "服务端未开放发布执行能力"}
          。发布执行器与外部证据验证未接入，确认按钮保持关闭。
        </OpsNotice>
      )}
      <div className="ops-actions ops-actions-split">
        <OpsLink view="release" id={activeRecord.id} button>
          返回补齐核对
        </OpsLink>
        <Button kind="primary" disabled>
          确认并提交
        </Button>
      </div>
    </Panel>
  );
}
export function WorkerDetail() {
  const { data, id, demo } = useOps(),
    job = Object.hasOwn(jobNames, id) ? id : demo ? "provider_revocations" : "",
    [selected, setSelected] = useState<Document<Run>>();
  if (!job)
    return (
      <Panel title="请先选择任务">
        <OpsLink view="workers" button>
          返回任务清单
        </OpsLink>
      </Panel>
    );
  const rows = (data?.runs.items ?? []).filter((r) => r.data.job === job),
    latest = rows[0],
    incident = data?.incidents.items.find(
      (r) => r.data.service === job && r.state !== "resolved",
    );
  return (
    <>
      <div className="ops-page-actions">
        {incident ? (
          <OpsLink view="incident" id={incident.id} button>
            查看关联事件
          </OpsLink>
        ) : (
          <OpsLink view="workers" button>
            返回任务清单
          </OpsLink>
        )}
      </div>
      <EnvironmentNotice title={`${job} / ${jobNames[job]}`}>
        执行结果来自API任务入口；不展示原始外部回执或消费者资料，不从此处重试任务。
      </EnvironmentNotice>
      <div className="ops-metrics">
        <Metric
          label="失败次数 / 当前采样"
          value={
            data
              ? rows.filter((r) => ["failed", "retry"].includes(r.state)).length
              : "—"
          }
          hint={`当前已读${rows.length}条结果，不冒充全部历史`}
        />
        <Metric
          label="最近耗时"
          value={latest ? `${latest.data.duration_ms} ms` : "—"}
          hint="批次响应耗时，与进程心跳分别观察"
        />
        <Metric
          label="最近结果时间"
          value={formatTime(latest?.data.finished_at)}
          hint="采样过期时不能判断任务仍在运行"
        />
        <Metric
          label="执行控制"
          value="未接入"
          hint="真实重试需单独的执行权限与幂等核对"
        />
      </div>
      <DataTable
        title={demo ? "执行记录 · 演示" : "执行记录"}
        columns={["执行编号", "具名执行者", "完成时间", "批次结果", "操作"]}
        dense
        hideFilters
        empty="尚未采集此任务的执行结果"
        rows={rows.map((r) => ({
          key: r.id,
          state: r.state,
          search: r.id,
          cells: [
            <span title={r.id}>
              {r.id.startsWith("RUN-DEMO") ? r.id : r.id.slice(0, 12)}
            </span>,
            r.data.operator,
            formatTime(r.data.finished_at),
            <Status state={r.state} />,
            <Button kind="quiet" onClick={() => setSelected(r)}>
              查看脱敏结果 →
            </Button>,
          ],
        }))}
      />
      <Panel
        title="脱敏结果摘录"
        description="只保留结果码、计数与耗时，不保存供应方正文"
      >
        <div className="ops-log">
          {rows.length ? (
            rows.slice(0, 3).map((r) => (
              <div key={r.id}>
                <time>{formatTime(r.data.finished_at)}</time>
                <span>
                  {r.data.result_code} / {stateLabel(r.state)} / 处理{" "}
                  {r.data.processed} 项
                </span>
              </div>
            ))
          ) : (
            <EmptyState title="暂无任务结果日志" />
          )}
        </div>
      </Panel>
      {selected ? (
        <Modal
          title="任务执行结果"
          onClose={() => setSelected(undefined)}
          actions={<Button onClick={() => setSelected(undefined)}>关闭</Button>}
        >
          <dl>
            <Summary label="执行编号">{selected.id}</Summary>
            <Summary label="任务 / 状态">
              {job} / {stateLabel(selected.state)}
            </Summary>
            <Summary label="结果码 / 已处理计数">
              {selected.data.result_code} / {selected.data.processed}
            </Summary>
            <Summary label="完成时间 / 耗时">
              {formatTime(selected.data.finished_at)} /{" "}
              {selected.data.duration_ms} ms
            </Summary>
          </dl>
        </Modal>
      ) : null}
    </>
  );
}
export function IncidentDetail() {
  const { demo, session } = useOps(),
    detail = useDocument<Incident>("incidents"),
    record = detail.record,
    [action, setAction] = useState("");
  if (!record)
    return (
      <MissingDetail
        kind="alerts"
        error={detail.error}
        loading={detail.loading}
      />
    );
  const incident = record.data,
    own = incident.assignee === session.context?.id,
    canWork = !demo && session.context?.capabilities.incident_write;
  return (
    <>
      <div className="ops-page-actions">
        <OpsLink view="alerts" button>
          返回事件队列
        </OpsLink>
      </div>
      <EnvironmentNotice title={`${incident.severity} / ${incident.title}`}>
        事件 {record.id}。处置说明与实际观测分开记录，恢复后继续验证和复盘。
      </EnvironmentNotice>
      <div className="ops-columns ops-columns-wide">
        <Panel
          title="影响与责任"
          description="每个动作重新检查角色、负责人和版本"
        >
          <dl>
            <Summary label="影响范围">
              {incident.service} /{" "}
              {demo ? "staging（示例）" : session.context?.environment}
            </Summary>
            <Summary label="责任人与当前阶段">
              {incident.assignee || "尚未认领"} /{" "}
              <Status state={record.state} />
            </Summary>
            <Summary label="最近证据引用">
              {incident.evidence_ref || "未提供"}
            </Summary>
          </dl>
        </Panel>
        <Panel title="处置前核对" description="每次动作保留原因与回执">
          <ol className="ops-checklist">
            <li>01 核对失败执行与幂等性</li>
            <li>02 检查外部供应方回执</li>
            <li>03 评估缓解措施与影响范围</li>
          </ol>
          <div className="ops-actions">
            {!canWork ? (
              <p className="ops-caption">
                当前岗位仅可查看事件；处置须具备值班角色。
              </p>
            ) : record.state === "pending" ? (
              <Button
                kind="primary"
                disabled={!canWork}
                onClick={() => setAction("claim")}
              >
                认领事件
              </Button>
            ) : record.state !== "resolved" ? (
              <>
                <Button
                  disabled={!canWork || !own}
                  onClick={() => setAction("note")}
                >
                  补充记录
                </Button>
                <Button
                  kind="primary"
                  disabled={!canWork || !own}
                  onClick={() =>
                    setAction(
                      record.state === "investigating" ? "observe" : "resolve",
                    )
                  }
                >
                  {record.state === "investigating" ? "进入观察" : "验证并结案"}
                </Button>
              </>
            ) : (
              <Status state="resolved" />
            )}
          </div>
          {!demo && !canWork ? (
            <p className="ops-caption">当前角色仅可读取此记录。</p>
          ) : null}
        </Panel>
      </div>
      <Panel
        title={demo ? "事件时间线 · 演示" : "事件时间线"}
        description="按时间记录事实，不覆盖历史处置"
      >
        <ol className="ops-timeline">
          {incident.events.map((event, i) => (
            <li key={`${event.at}-${i}`}>
              <time>{formatTime(event.at)}</time>
              <div>
                <strong>
                  {stateLabel(event.action)} · {event.actor}
                </strong>
                <p>{event.note}</p>
                {event.evidence_ref ? (
                  <small>证据：{event.evidence_ref}</small>
                ) : null}
              </div>
            </li>
          ))}
        </ol>
      </Panel>
      {action && session.context ? (
        <IncidentAction
          key={`${record.id}:${record.version}:${session.context.id}`}
          record={record}
          action={action}
          onClose={() => setAction("")}
          onSaved={detail.reload}
        />
      ) : null}
    </>
  );
}
