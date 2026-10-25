import { useState } from "react";
import { Button, Modal } from "../components/ui";
import { OpsLink, useOps } from "./context";
import { EnvironmentNotice } from "./OverviewPages";
import { CreateIncident, CreateRelease } from "./ActionForms";
import {
  formatTime,
  jobNames,
  stateLabel,
  type Audit as AuditData,
  type Document,
  type Migration,
  type Resource,
} from "./models";
import { DataTable, Metric, Status, Summary } from "./ui";

function useMore(kind: Resource) {
  const { session, demo, data } = useOps();
  return !demo && data?.[kind].next_cursor
    ? () => void session.more(kind)
    : undefined;
}
export function Workers() {
  const { data, demo, session } = useOps(),
    runs = data?.runs.items ?? [],
    more = useMore("runs");
  return (
    <>
      <EnvironmentNotice>
        {demo
          ? "任务类型来自源码；执行量、时间与结果均为示例。"
          : "下方为API接收的任务批次结果；未观测到的任务保持未知，不将结果记录等同于进程心跳。"}
      </EnvironmentNotice>
      <DataTable
        title="任务清单 · 6 类 worker"
        columns={[
          "任务 / 职责",
          "当前采样处理量",
          "最近结果时间",
          "批次状态",
          "详情",
        ]}
        placeholder="搜索任务名称或职责"
        rows={Object.entries(jobNames).map(([job, label]) => {
          const samples = runs.filter((r) => r.data.job === job),
            latest = samples[0];
          const stale =
            !demo && latest && data
              ? new Date(data.observed_at).getTime() -
                  new Date(latest.data.finished_at).getTime() >
                120_000
              : false;
          const state = stale ? "unknown" : (latest?.state ?? "unknown");
          return {
            key: job,
            state,
            search: `${job} ${label}`,
            cells: [
              `${job} · ${label}`,
              samples.length
                ? `${samples.reduce((sum, r) => sum + r.data.processed, 0)} / ${samples.length}次采样`
                : "—",
              formatTime(latest?.data.finished_at),
              <Status state={state}>
                {stale ? "采样过期" : stateLabel(state)}
              </Status>,
              <OpsLink view="worker" id={job}>
                查看任务详情 →
              </OpsLink>,
            ],
          };
        })}
      />
      <p className="ops-caption">
        采样范围：当前已读 {runs.length}{" "}
        条结果。任务重试必须针对明确执行、复用幂等标识并核对回执；此处没有批量重跑入口。
      </p>
      {more ? (
        <Button
          disabled={session.busy}
          onClick={() => void session.more("runs")}
        >
          读取更早任务结果
        </Button>
      ) : null}
    </>
  );
}
export function Releases() {
  const { data, demo, session } = useOps(),
    [create, setCreate] = useState(false),
    rows = data?.releases.items ?? [];
  return (
    <>
      <div className="ops-page-actions">
        <Button
          kind="primary"
          disabled={demo || !session.context?.capabilities.release_write}
          onClick={() => setCreate(true)}
        >
          登记发布准备
        </Button>
      </div>
      <EnvironmentNotice title="发布执行后端尚未接入">
        可以登记候选构件与核对证据，当前没有部署、重启或回滚的执行能力。
      </EnvironmentNotice>
      <div className="ops-metrics">
        <Metric
          label="当前环境版本"
          value={demo ? "—" : session.context?.version || "—"}
          hint="来自服务端版本配置，不推断部署成功"
        />
        <Metric
          label="候选版本"
          value={rows[0]?.data.version ?? "—"}
          hint="当前已读列表中的最新准备记录"
        />
        <Metric
          label="被阻止记录"
          value={data ? rows.filter((r) => r.state === "blocked").length : "—"}
          hint="填写引用不等于外部验证通过"
        />
        <Metric
          label="发布执行权限"
          value="未接入"
          hint="网页没有服务器命令与部署执行器"
        />
      </div>
      <DataTable
        title={demo ? "发布记录 · 演示" : "发布准备记录"}
        columns={["发布 / 版本", "目标环境", "更新时间", "核对状态", "操作"]}
        hideFilters
        rows={rows.map((r) => ({
          key: r.id,
          state: r.state,
          search: `${r.id} ${r.data.version}`,
          cells: [
            <span title={r.id}>
              {r.id.startsWith("REL-DEMO") ? r.id : r.id.slice(0, 12)} ·{" "}
              {r.data.version}
            </span>,
            demo ? "staging · 示例" : (session.context?.environment ?? "—"),
            formatTime(r.updated_at),
            <Status state={r.state} />,
            <OpsLink view="release" id={r.id}>
              进入发布核对 →
            </OpsLink>,
          ],
        }))}
        more={useMore("releases")}
        loading={session.busy}
        empty="尚无发布准备记录"
      />
      <p className="ops-caption">
        准备构件 → 核对环境与恢复证据 → 外部审批与验证 →
        执行后端。当前执行后端缺失，所有发布保持关闭。
      </p>
      {create && session.context ? (
        <CreateRelease
          key={session.context.id}
          onClose={() => setCreate(false)}
        />
      ) : null}
    </>
  );
}
export function Migrations() {
  const { data } = useOps(),
    [selected, setSelected] = useState<Migration>();
  return (
    <>
      <EnvironmentNotice title="源码与持久化账本分别核对">
        校验状态来自当前源码 SHA-256
        与数据库账本比较；显式基线只是登记，不代表脚本实际执行过。
      </EnvironmentNotice>
      <DataTable
        title="版本账本 · 源码与数据库"
        columns={["版本", "源文件", "账本 / 校验和", "校验状态", "详情"]}
        dense
        placeholder="搜索版本或迁移文件"
        empty="尚未读取迁移目录与账本"
        rows={(data?.migrations ?? []).map((row) => ({
          key: row.name,
          state: row.state,
          search: `${row.name} ${row.state} ${row.source_sha256} ${row.recorded_sha256}`,
          cells: [
            row.name.slice(0, 3),
            <span className="ops-ellipsis" title={row.name}>
              {row.name}
            </span>,
            <span className="ops-ellipsis" title={row.recorded_sha256}>
              {row.recorded_sha256
                ? `${row.recorded_sha256.slice(0, 12)}…`
                : "账本未回读"}
            </span>,
            <Status state={row.state} />,
            <Button kind="quiet" onClick={() => setSelected(row)}>
              查看校验说明 →
            </Button>,
          ],
        }))}
      />
      <p className="ops-caption">
        使用事务、校验和与 advisory lock；校验不一致时停止。不提供任意
        SQL、网页应用迁移或“一键回退”。
      </p>
      {selected ? (
        <Modal
          title={selected.name}
          onClose={() => setSelected(undefined)}
          actions={<Button onClick={() => setSelected(undefined)}>关闭</Button>}
        >
          <dl>
            <Summary label="校验状态">
              <Status state={selected.state} />
            </Summary>
            <Summary label="源码 SHA-256">
              <code className="ops-wrap">
                {selected.source_sha256 || "未读取"}
              </code>
            </Summary>
            <Summary label="账本 SHA-256">
              <code className="ops-wrap">
                {selected.recorded_sha256 || "未读取"}
              </code>
            </Summary>
            <Summary label="登记时间 / 基线">
              {formatTime(selected.applied_at)} /{" "}
              {selected.baseline ? "人工显式基线；不代表执行" : "非基线或未知"}
            </Summary>
          </dl>
        </Modal>
      ) : null}
    </>
  );
}
export function Alerts() {
  const { data, demo, session } = useOps(),
    [create, setCreate] = useState(false);
  return (
    <>
      <div className="ops-page-actions">
        <Button
          kind="primary"
          disabled={demo || !session.context?.capabilities.incident_write}
          onClick={() => setCreate(true)}
        >
          登记事件
        </Button>
      </div>
      <EnvironmentNotice>
        {demo
          ? "监控采集、规则和通知渠道尚未接入；以下均为演示。"
          : "任务失败可生成待认领事件，也可由授权值班人员登记。外部监控规则和通知渠道未接入。"}
      </EnvironmentNotice>
      <DataTable
        title={demo ? "事件队列 · 演示" : "事件队列"}
        columns={["事件 / 等级", "影响范围", "发生时间", "处理状态", "操作"]}
        placeholder="搜索事件、服务或关键词"
        rows={(data?.incidents.items ?? []).map((r) => ({
          key: r.id,
          state: r.state,
          level: r.data.severity,
          search: `${r.id} ${r.data.title} ${r.data.service} ${r.data.assignee}`,
          cells: [
            <span className="ops-ellipsis" title={`${r.id} / ${r.data.title}`}>
              {r.id.startsWith("INC-DEMO") ? r.id : r.id.slice(0, 12)} ·{" "}
              {r.data.severity}
            </span>,
            <span title={r.data.service}>{r.data.title}</span>,
            formatTime(r.created_at),
            <Status state={r.state} />,
            <OpsLink view="incident" id={r.id}>
              进入事件处理 →
            </OpsLink>,
          ],
        }))}
        more={useMore("incidents")}
        loading={session.busy}
        empty="当前没有已读取的运维事件"
      />
      <p className="ops-caption">
        没有事件不代表没有故障。结案前确认影响、恢复证据与责任人；worker事件还会核对事件后的最新任务结果。
      </p>
      {create && session.context ? (
        <CreateIncident
          key={session.context.id}
          onClose={() => setCreate(false)}
        />
      ) : null}
    </>
  );
}
const actionNames: Record<string, string> = {
  "release.prepare": "登记发布准备",
  "incident.create": "登记事件",
};
export function Audit() {
  const { data, demo, session } = useOps(),
    [selected, setSelected] = useState<Document<AuditData>>();
  return (
    <>
      <EnvironmentNotice title="只展示脱敏审计摘要">
        不展示密钥、供应方正文、原始身份资料、完整请求体或消费者私密内容。
      </EnvironmentNotice>
      <DataTable
        title={demo ? "操作记录 · 演示" : "操作审计"}
        columns={["时间 / 编号", "操作 / 资源", "操作者", "结果", "详情"]}
        placeholder="搜索事件编号、操作者或资源"
        rows={(data?.audit.items ?? []).map((r) => ({
          key: r.id,
          state: r.state,
          search: `${r.id} ${r.data.actor} ${r.data.action} ${r.data.resource} ${r.data.request_id}`,
          cells: [
            <span title={r.id}>
              {formatTime(r.created_at)} · {r.id.slice(0, 8)}
            </span>,
            <span
              className="ops-ellipsis"
              title={`${r.data.action} / ${r.data.resource}`}
            >
              {actionNames[r.data.action] ?? r.data.action}
            </span>,
            r.data.actor,
            <Status state={r.data.outcome} />,
            <Button kind="quiet" onClick={() => setSelected(r)}>
              查看脱敏摘要 →
            </Button>,
          ],
        }))}
        more={useMore("audit")}
        loading={session.busy}
        empty="尚无已记录的运维命令"
      />
      {selected ? (
        <Modal
          title="审计摘要"
          onClose={() => setSelected(undefined)}
          actions={<Button onClick={() => setSelected(undefined)}>关闭</Button>}
        >
          <dl>
            {Object.entries({
              事件编号: selected.id,
              操作者: selected.data.actor,
              动作: selected.data.action,
              资源: selected.data.resource,
              结果: stateLabel(selected.data.outcome),
              原请求编号: selected.data.request_id,
              记录时间: formatTime(selected.created_at),
            }).map(([label, value]) => (
              <Summary label={label} key={label}>
                {value}
              </Summary>
            ))}
          </dl>
        </Modal>
      ) : null}
    </>
  );
}
