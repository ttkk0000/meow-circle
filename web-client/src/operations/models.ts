export const sections = [
  ["overview", "运维总览", "house"],
  ["services", "服务与依赖", "sliders-horizontal"],
  ["workers", "Worker 任务", "clock"],
  ["releases", "版本与发布", "arrow-right"],
  ["migrations", "迁移账本", "check"],
  ["alerts", "告警事件", "bell"],
  ["audit", "操作审计", "bookmark"],
  ["access", "环境与权限", "shield-check"],
] as const;
export type Section = (typeof sections)[number][0];
export type View =
  | Section
  | "release"
  | "worker"
  | "incident"
  | "disconnected"
  | "forbidden"
  | "confirm";
export const views: readonly string[] = [
  ...sections.map(([key]) => key),
  "release",
  "worker",
  "incident",
  "disconnected",
  "forbidden",
  "confirm",
];
export const jobNames: Record<string, string> = {
  push: "推送派发",
  nearby_alerts: "附近提醒",
  content_updates: "内容更新",
  privacy_cleanup: "隐私清理",
  provider_revocations: "授权撤销",
  maintenance: "维护",
  search_projection: "搜索失效事件",
  audit_projection: "审计归档事件",
};
export interface Document<T> {
  id: string;
  version: string;
  state: string;
  created_at: string;
  updated_at: string;
  data: T;
}
export interface Page<T> {
  items: Document<T>[];
  next_cursor: string;
}
export interface Run {
  job: string;
  operator: string;
  outcome: string;
  result_code: string;
  processed: number;
  duration_ms: number;
  finished_at: string;
}
export interface Event {
  at: string;
  actor: string;
  action: string;
  note: string;
  evidence_ref?: string;
}
export interface Incident {
  title: string;
  service: string;
  severity: string;
  assignee: string;
  events: Event[];
  evidence_ref: string;
}
export interface Release {
  version: string;
  artifact_sha256: string;
  backup_ref: string;
  approval_ref: string;
  reason: string;
  creator: string;
}
export interface Audit {
  actor: string;
  action: string;
  resource: string;
  outcome: string;
  request_id: string;
}
export interface Signal {
  id: string;
  label: string;
  state: string;
  detail: string;
  observed_at?: string;
}
export interface Migration {
  name: string;
  source_sha256: string;
  recorded_sha256: string;
  state: string;
  baseline: boolean;
  applied_at?: string;
}
export interface Snapshot {
  observed_at: string;
  signals: Signal[];
  runs: Page<Run>;
  incidents: Page<Incident>;
  releases: Page<Release>;
  audit: Page<Audit>;
  migrations: Migration[];
}
export interface Context {
  id: string;
  roles: string[];
  environment: string;
  origin: string;
  store: string;
  version: string;
  capabilities: {
    read: boolean;
    incident_write: boolean;
    release_write: boolean;
    deploy: boolean;
    worker_retry: boolean;
  };
}
export interface Check {
  id: string;
  label: string;
  state: string;
  detail: string;
}
export interface Preflight {
  release: Document<Release>;
  checks: Check[];
  can_execute: boolean;
}
export type Resource = "runs" | "incidents" | "releases" | "audit";
export const labels: Record<string, string> = {
  unknown: "未知",
  not_configured: "未配置",
  responding: "API 已响应",
  reachable: "连接可达",
  unreachable: "连接失败",
  pending: "待认领",
  investigating: "处理中",
  observing: "观察中",
  resolved: "已恢复并结案",
  failed: "失败",
  retry: "待重试",
  idle: "空闲",
  acknowledged: "本批已确认",
  batch_complete: "本批已完成",
  continuation: "分批续接",
  verified: "校验一致",
  baseline: "显式基线",
  mismatch: "校验不一致",
  source_missing: "源码缺失",
  source_unavailable: "源码不可读",
  blocked: "已阻止",
  recorded: "已记录",
  missing: "待补齐",
  reference_only: "仅引用待验证",
  unavailable: "未接入",
  created: "登记",
  triggered: "触发",
  claim: "认领",
  note: "补充记录",
  observe: "进入观察",
  resolve: "验证并结案",
};
export function stateLabel(state: string) {
  return labels[state] ?? state;
}
export function formatTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
      }).format(date);
}
export function sectionFor(view: View): Section {
  return view === "release" || view === "confirm"
    ? "releases"
    : view === "worker"
      ? "workers"
      : view === "incident"
        ? "alerts"
        : view === "disconnected" || view === "forbidden"
          ? "access"
          : view;
}
