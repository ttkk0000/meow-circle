import { useEffect, useState } from "react";
import { Button, Field, Modal } from "../components/ui";
import { OpsLink, useOps } from "./context";
import { CreateRelease, IncidentAction } from "./ActionForms";
import { isCancelled, message, OpsError } from "./client";
import { EnvironmentNotice } from "./OverviewPages";
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
            session.report(e);
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
  };
}
function MissingDetail({
  kind,
  error,
  loading,
}: {
  kind: "releases" | "alerts";
  error: string;
  loading: boolean;
}) {
  return (
    <Panel title={loading ? "正在读取记录" : "请先选择记录"}>
      <EmptyState
        title={
          error || (loading ? "正在核对权限与记录版本" : "当前没有可展示的详情")
        }
      >
        <OpsLink view={kind} button>
          返回列表选择
        </OpsLink>
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
    [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    setState(undefined);
    setError("");
    if (!record || demo || !session.context) return;
    const epoch = session.client.epoch;
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
            session.report(e);
        }
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
  };
}
export function ReleaseDetail() {
  const [editing, setEditing] = useState(false);
  const { demo, id, session } = useOps(),
    detail = useDocument<Release>("releases"),
    { preflight, error } = usePreflight(detail.record, detail.revision),
    record = detail.record;
  if (!record)
    return (
      <MissingDetail
        kind="releases"
        error={detail.error}
        loading={detail.loading}
      />
    );
  const r = record.data;
  return (
    <>
      <div className="ops-page-actions">
        <OpsLink view="releases" button>
          返回发布记录
        </OpsLink>
      </div>
      <EnvironmentNotice title="当前发布被前置条件阻止">
        当前记录 {id}。没有发布执行器；填写摘要或证据引用不代表通过外部核验。
      </EnvironmentNotice>
      <div className="ops-columns">
        <Panel title="发布对象" description="候选版本和目标必须保持一致">
          <dl>
            <Summary label="候选版本 / 来源">
              {r.version} / {r.creator}
            </Summary>
            <Summary label="目标环境 / 构件摘要">
              {demo ? "staging · 占位" : session.context?.environment} /{" "}
              <span className="ops-wrap">{r.artifact_sha256 || "未提供"}</span>
            </Summary>
          </dl>
        </Panel>
        <Panel title="执行门槛" description="不可用状态说明缺项">
          <ol className="ops-checklist">
            {(preflight?.checks ?? []).map((check, i) => (
              <li key={check.id}>
                <strong>
                  {String(i + 1).padStart(2, "0")} {check.label}
                </strong>
                <small>{check.detail}</small>
              </li>
            ))}
          </ol>
          {error ? (
            <OpsNotice title="核对结果未读取" error>
              {error}
              <Button onClick={detail.reload}>重新读取</Button>
            </OpsNotice>
          ) : null}
        </Panel>
      </div>
      <DataTable
        title="前置核对清单"
        columns={["核对项", "证据来源", "当前结果", "放行状态", "下一步"]}
        dense
        hideFilters
        empty="正在等待服务端前置核对"
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
              <OpsLink view="access">查看环境信息 →</OpsLink>
            ) : check.id === "artifact" ? (
              <OpsLink view="migrations">查看迁移账本 →</OpsLink>
            ) : (
              "等待外部接入"
            ),
          ],
        }))}
      />
      <div className="ops-actions ops-actions-split">
        <div className="ops-actions">
          <OpsLink view="confirm" id={record.id} button>
            查看发布确认
          </OpsLink>
          {demo || session.context?.capabilities.release_write ? (
            <Button
              disabled={demo || !session.context?.capabilities.release_write}
              onClick={() => setEditing(true)}
            >
              补齐发布资料
            </Button>
          ) : null}
        </div>
        {demo || session.context?.capabilities.release_write ? (
          <Button disabled>执行发布</Button>
        ) : null}
      </div>
      {editing && session.context?.capabilities.release_write ? (
        <CreateRelease
          key={`${record.id}:${record.version}:${session.context.id}`}
          record={record}
          onClose={() => setEditing(false)}
        />
      ) : null}
    </>
  );
}
export function ConfirmRelease() {
  const { demo, session } = useOps(),
    { record, error, loading } = useDocument<Release>("releases"),
    { preflight, error: preflightError } = usePreflight(record),
    [reason, setReason] = useState(""),
    [target, setTarget] = useState("");
  if (!record)
    return <MissingDetail kind="releases" error={error} loading={loading} />;
  const environment = demo
    ? "staging"
    : (session.context?.environment ?? "未配置");
  return (
    <Panel
      title="确认发布申请"
      description="当前是阻止提交状态，没有执行接口。"
      className="ops-confirm"
    >
      <div className="ops-confirm-status">
        <Status state="blocked" />
      </div>
      <dl className="ops-columns">
        <Summary label="目标环境">
          {environment} /{" "}
          {demo
            ? "mnd.example.invalid"
            : session.context?.origin || "域名未配置"}
        </Summary>
        <Summary label="候选版本与构件">
          {record.data.version} /{" "}
          <span className="ops-wrap">
            {record.data.artifact_sha256 || "摘要未提供"}
          </span>
        </Summary>
        <Summary label="审批与恢复证据">
          {record.data.backup_ref || "备份未提供"} /{" "}
          {record.data.approval_ref || "审批未提供"}
        </Summary>
        <Summary label="影响范围">API 与 worker / 范围待外部核对</Summary>
      </dl>
      <div className="ops-form">
        <Field
          label="操作原因（仅本页核对）"
          multiline
          value={reason}
          onChange={setReason}
          hint="本页输入不持久化、不提交；不要输入密码或Token。"
        />
        <Field
          label={`确认目标环境：${environment}`}
          value={target}
          onChange={setTarget}
          hint="输入环境名仅用于人工核对，不会授予执行权限。"
        />
      </div>
      <OpsNotice title="执行条件尚未满足">
        {preflightError ||
          (preflight?.checks
            .filter((c) => c.state !== "verified")
            .map((c) => c.label)
            .join("、") ??
            "前置核对结果未读取")}
        。发布执行器与外部证据验证未接入，确认按钮保持关闭。
      </OpsNotice>
      <div className="ops-actions ops-actions-split">
        <OpsLink view="release" id={record.id} button>
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
