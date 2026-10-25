import { useState, type ReactNode } from "react";
import { Button, Modal } from "../components/ui";
import { OpsLink, useOps } from "./context";
import { formatTime, type Signal } from "./models";
import {
  DataTable,
  Metric,
  OpsIcon,
  OpsNotice,
  Panel,
  Status,
  Summary,
} from "./ui";

export function EnvironmentNotice({
  title,
  children,
}: {
  title?: string;
  children?: ReactNode;
}) {
  const { demo, data } = useOps();
  return (
    <OpsNotice
      title={
        title ??
        (demo
          ? "设计示例 · 未连接真实环境"
          : data
            ? "按来源和时间分别核对信号"
            : "尚未连接真实环境")
      }
    >
      {children ??
        (demo
          ? "下方数据仅用于设计评审；示例模式不会发送运维请求。"
          : data
            ? `采集时间 ${formatTime(data.observed_at)}；缺失或过期的数据保持未知，不汇总为完整健康。`
            : "连接具名运维账号后读取目标环境。当前只显示源码目录，不填入虚构的成功状态。")}
    </OpsNotice>
  );
}
export function Overview({
  onConnect,
  onPreview,
}: {
  onConnect: () => void;
  onPreview: () => void;
}) {
  const { data, demo, session } = useOps(),
    signals = data?.signals ?? [],
    runs = data?.runs.items ?? [];
  const pg = signals.find((s) => s.id === "postgres"),
    redis = signals.find((s) => s.id === "redis");
  const dependencyState = [pg, redis].some((s) => s?.state === "unreachable")
    ? "unreachable"
    : [pg, redis].every((s) => s?.state === "reachable")
      ? "reachable"
      : "unknown";
  const latest = runs[0];
  const cards = [
    {
      label: "API 探针",
      value: "HTTP 可达性 / /healthz",
      state: signals.find((s) => s.id === "api")?.state ?? "unknown",
    },
    {
      label: "Postgres 与 Redis",
      value: "分别观察连接、容量与队列",
      state: dependencyState,
    },
    {
      label: "后台任务",
      value: "6 类 worker / 最近执行采样",
      state: latest?.state ?? "unknown",
    },
    {
      label: "入口与证书",
      value: "Caddy / 域名 / TLS 到期时间",
      state: signals.find((s) => s.id === "caddy")?.state ?? "unknown",
    },
  ];
  return (
    <>
      <EnvironmentNotice />
      <div className="ops-metrics">
        <Metric
          label="编排服务"
          value="11"
          hint="来源：API、依赖、迁移与6类worker编排"
        />
        <Metric
          label="后台任务类型"
          value="06"
          hint="按任务分别观察，不汇总为完整健康"
        />
        <Metric
          label="发布前置核对"
          value={
            demo
              ? "03"
              : data
                ? String(
                    data.releases.items.filter((r) => r.state === "blocked")
                      .length,
                  )
                : "—"
          }
          hint={
            demo
              ? "环境、构件与恢复证据待补齐"
              : "当前已读发布准备中被阻止的记录"
          }
        />
        <Metric
          label="真实环境采集"
          value={
            demo || !data
              ? "—"
              : data.signals.filter((s) => s.observed_at).length
          }
          hint={
            demo || !data
              ? "未接入 · 不展示虚构可用率"
              : "已带观测时间的信号数量；分别判断"
          }
        />
      </div>
      <div className="ops-columns ops-columns-wide">
        <Panel title="环境信号" description="探针、数据依赖与任务结果分开判断">
          <dl className="ops-signal-list">
            {cards.map((item) => (
              <div className="ops-signal-row" key={item.label}>
                <Summary label={item.label}>{item.value}</Summary>
                <Status state={item.state} />
              </div>
            ))}
          </dl>
          <OpsLink view="services" button>
            查看服务清单
          </OpsLink>
        </Panel>
        <Panel
          title="值班待办"
          description={
            demo ? "按风险处理，所有事项均为演示" : "先核对数据来源与执行前提"
          }
        >
          <ol className="ops-todos">
            {[
              ["接入观测数据", "核对环境身份、采集时间与来源。"],
              ["补齐发布前置证据", "核对构件摘要、审批与恢复方案。"],
              ["验证恢复演练", "保留备份证据与恢复演练记录。"],
            ].map(([title, hint], i) => (
              <li key={title}>
                <span>{String(i + 1).padStart(2, "0")}</span>
                <div>
                  <strong>{title}</strong>
                  <small>{hint}</small>
                </div>
              </li>
            ))}
          </ol>
          <OpsLink view="access" button>
            查看环境接入
          </OpsLink>
        </Panel>
      </div>
      {!demo && !session.context ? (
        <div className="ops-actions">
          <Button kind="primary" onClick={onConnect}>
            连接真实环境
          </Button>
          <Button onClick={onPreview}>查看设计示例</Button>
        </div>
      ) : null}
    </>
  );
}
export function Services() {
  const { data } = useOps(),
    [selected, setSelected] = useState<Signal>();
  const catalog = [
    { id: "api", label: "api · /healthz", kind: "Go HTTP / DB、Redis" },
    { id: "postgres", label: "postgres", kind: "数据库 / 存储卷" },
    { id: "redis", label: "redis", kind: "缓存与任务协作" },
    { id: "caddy", label: "caddy", kind: "入口 / 域名与 TLS" },
    { id: "migrate", label: "migrate", kind: "一次性迁移任务" },
    { id: "workers", label: "worker × 6", kind: "后台任务 / DB、Redis" },
  ];
  return (
    <>
      <EnvironmentNotice title="服务信号分别采集">
        服务目录来自现有编排；连接探针不能代替资源、容量、复制、队列与证书观测。
      </EnvironmentNotice>
      <DataTable
        title="服务目录 · 源码映射"
        columns={["名称 / 探针", "类型 / 依赖", "采集时间", "观测状态", "详情"]}
        placeholder="搜索服务名称或依赖"
        rows={catalog.map((item) => {
          const signal = data?.signals.find((s) => s.id === item.id);
          return {
            key: item.id,
            state: signal?.state ?? "unknown",
            search: `${item.label} ${item.kind}`,
            cells: [
              item.label,
              item.kind,
              formatTime(signal?.observed_at),
              <Status state={signal?.state ?? "unknown"} />,
              item.id === "migrate" ? (
                <OpsLink view="migrations">查看迁移账本 →</OpsLink>
              ) : item.id === "workers" ? (
                <OpsLink view="workers">查看任务清单 →</OpsLink>
              ) : (
                <Button
                  kind="quiet"
                  onClick={() =>
                    setSelected(
                      signal ?? {
                        id: item.id,
                        label: item.label,
                        state: "unknown",
                        detail: "尚未采集此项信号。",
                      },
                    )
                  }
                >
                  查看观测说明 →
                </Button>
              ),
            ],
          };
        })}
      />
      <p className="ops-caption">
        资源曲线、日志聚合与证书观测尚未接入。重启、扩容及服务器命令没有网页执行入口。
      </p>
      {selected ? (
        <Modal
          title={selected.label}
          onClose={() => setSelected(undefined)}
          actions={<Button onClick={() => setSelected(undefined)}>关闭</Button>}
        >
          <dl>
            <Summary label="当前观测">
              <Status state={selected.state} />
            </Summary>
            <Summary label="采集时间">
              {formatTime(selected.observed_at)}
            </Summary>
            <Summary label="信号说明">{selected.detail}</Summary>
            <Summary label="容量 / 复制 / 证书">
              尚未接入对应采集，不推断为正常。
            </Summary>
          </dl>
        </Modal>
      ) : null}
    </>
  );
}
export function Access({
  onConnect,
  onPreview,
}: {
  onConnect: () => void;
  onPreview: () => void;
}) {
  const { session, demo } = useOps(),
    context = session.context;
  return (
    <>
      <div className="ops-page-actions">
        <OpsLink view="disconnected" button>
          查看未连接状态
        </OpsLink>
      </div>
      <EnvironmentNotice
        title={
          demo
            ? "占位环境 · 尚未接入"
            : context
              ? "当前环境身份与权限"
              : "环境尚未连接"
        }
      />
      <div className="ops-columns">
        <Panel title="环境身份" description="配置来源只显示引用与公开身份">
          <dl>
            <Summary label="环境 / 域名">
              {demo
                ? "staging · mnd.example.invalid"
                : context
                  ? `${context.environment} · ${context.origin || "域名未配置"}`
                  : "等待配置 / 未连接"}
            </Summary>
            <Summary label="存储 / 当前版本">
              {context
                ? `${context.store} / ${context.version || "版本身份未配置"}`
                : "等待回读，不把占位版本当线上版本"}
            </Summary>
          </dl>
        </Panel>
        <Panel title="权限来源" description="服务端逐次按账号、环境和动作校验">
          <dl>
            <Summary label="当前身份">
              {demo ? "值班账号 · 示例" : (context?.id ?? "未连接账号")}
            </Summary>
            <Summary label="生效角色">
              {demo
                ? "只读设计示例"
                : (context?.roles.join(" / ") ?? "未回读 / 默认不可执行")}
            </Summary>
          </dl>
        </Panel>
      </div>
      <DataTable
        title="角色与能力"
        columns={["角色", "观察范围", "处置权限", "发布执行", "边界"]}
        dense
        hideFilters
        rows={[
          {
            key: "viewer",
            state: "unavailable",
            search: "",
            cells: [
              "ops_viewer",
              "授权环境 / 采集",
              "只读",
              <Status state="unavailable" />,
              <OpsLink view="forbidden">查看访问受限示例 →</OpsLink>,
            ],
          },
          {
            key: "oncall",
            state: "unavailable",
            search: "",
            cells: [
              "ops_oncall",
              "授权环境 / 事件",
              "认领 / 记录 / 验证",
              <Status state="unavailable" />,
              "事件版本与回执留痕",
            ],
          },
          {
            key: "release",
            state: "unavailable",
            search: "",
            cells: [
              "ops_release",
              "授权环境 / 构件",
              "发布准备 / 核对",
              <Status state="unavailable" />,
              "引用不等于验证通过",
            ],
          },
          {
            key: "admin",
            state: "unavailable",
            search: "",
            cells: [
              "ops_admin",
              "授权环境 / 审计",
              "不自动获得处置权限",
              <Status state="unavailable" />,
              "账号由部署方配置",
            ],
          },
        ]}
      />
      {!demo && !context ? (
        <div className="ops-actions">
          <Button kind="primary" onClick={onConnect}>
            连接具名账号
          </Button>
          <Button onClick={onPreview}>查看设计示例</Button>
        </div>
      ) : null}
      <p className="ops-caption">
        权限变更需由部署方更新具名配置并重新连接。当前没有网页自授权入口。
      </p>
    </>
  );
}
export function SystemState({
  forbidden = false,
  onConnect,
}: {
  forbidden?: boolean;
  onConnect: () => void;
}) {
  const { demo, session } = useOps();
  return (
    <Panel
      title={forbidden ? "需要访问授权" : "等待接入环境"}
      description={
        forbidden
          ? "这里不通过前端按钮状态推断服务端权限。"
          : "不会把空值显示为健康或成功。"
      }
      className="ops-system-state"
    >
      <div className="ops-state-content">
        <OpsIcon name={forbidden ? "shield-check" : "sliders-horizontal"} />
        <h2>{forbidden ? "你暂时不能访问这个环境" : "还没有可观测的数据"}</h2>
        <p>
          {forbidden
            ? "请联系环境管理员申请明确的资源范围和操作权限；不要共用账号或凭据。"
            : "环境身份、采集地址和访问授权配置后，才会回读服务、任务与版本。"}
        </p>
        {forbidden ? (
          <dl>
            <Summary label="请求范围">
              {demo
                ? "staging / 发布管理（示例）"
                : (session.context?.environment ??
                  "当前环境 / 以服务端授权为准")}
            </Summary>
            <Summary label="请求标识">
              {session.requestID || "状态说明示例；实际拒绝以请求标识为准"}
            </Summary>
          </dl>
        ) : (
          <ol>
            {[
              ["确认目标环境", "核对域名、环境标识和配置来源。"],
              [
                "接入采集与权限",
                "配置采集端与授权引用，不在页面保存明文密钥。",
              ],
              ["验证采集完整性", "检查来源、时间、版本身份和缺失信号。"],
            ].map(([title, hint], i) => (
              <li key={title}>
                <strong>
                  {String(i + 1).padStart(2, "0")} {title}
                </strong>
                <p>{hint}</p>
              </li>
            ))}
          </ol>
        )}
        <div className="ops-actions">
          {forbidden ? (
            <OpsLink view="access" button>
              查看角色与权限
            </OpsLink>
          ) : demo ? (
            <OpsLink view="access" button>
              查看环境配置
            </OpsLink>
          ) : (
            <Button kind="primary" onClick={onConnect}>
              连接环境
            </Button>
          )}
          <OpsLink view="overview" button>
            返回运维总览
          </OpsLink>
        </div>
      </div>
    </Panel>
  );
}
