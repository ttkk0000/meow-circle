import type {
  Audit,
  Document,
  Incident,
  Release,
  Run,
  Snapshot,
} from "./models";

// 只有用户明确进入“设计示例”模式才加载这些记录；该模式绝不发送运维请求。
const at = "2026-10-25T02:24:08Z";
function document<T>(id: string, state: string, data: T): Document<T> {
  return { id, version: "1", state, created_at: at, updated_at: at, data };
}
const outcomes = [
  "acknowledged",
  "acknowledged",
  "retry",
  "idle",
  "failed",
  "idle",
];
const jobs = [
  "push",
  "nearby_alerts",
  "content_updates",
  "privacy_cleanup",
  "provider_revocations",
  "maintenance",
];
const runs = jobs.map((job, i) =>
  document<Run>(`RUN-DEMO-${281 - i}`, outcomes[i], {
    job,
    operator: "service-demo",
    outcome: outcomes[i],
    result_code: i === 4 ? "job_failed" : "batch_result",
    processed: [128, 36, 52, 0, 0, 0][i],
    duration_ms: 120 + i * 23,
    finished_at: at,
  }),
);
const incidents = [
  "推送撤销任务失败",
  "内容更新心跳延迟",
  "API 探针间歇失败",
  "入口证书临近更新",
  "Redis 连接波动",
  "维护任务耗时上升",
].map((title, i) =>
  document<Incident>(
    `INC-DEMO-${42 - i}`,
    [
      "pending",
      "investigating",
      "observing",
      "pending",
      "resolved",
      "resolved",
    ][i],
    {
      title,
      service: [
        "provider_revocations",
        "content_updates",
        "api",
        "caddy",
        "redis",
        "maintenance",
      ][i],
      severity: i === 0 ? "P1" : i < 3 ? "P2" : "P3",
      assignee: i === 0 ? "" : "oncall-demo",
      evidence_ref: "DEMO-EXAMPLE",
      events: [
        {
          at,
          actor: "system-demo",
          action: "triggered",
          note: "连续失败达到演示阈值；并非真实监控事件。",
        },
        {
          at,
          actor: "oncall-demo",
          action: "note",
          note: "核对证据 → 缓解 → 验证 → 复盘。",
        },
      ],
    },
  ),
);
const releases = ["v0.1.0", "v0.0.9", "v0.0.8", "v0.0.7"].map((version, i) =>
  document<Release>(`REL-DEMO-00${4 - i}`, "blocked", {
    version,
    artifact_sha256: "",
    backup_ref: "",
    approval_ref: "",
    reason: "设计示例，尚未提供环境、构件与恢复证据。",
    creator: "operator-demo",
  }),
);
const audit = [
  "release.prepare",
  "migration.read",
  "incident.claim",
  "alert.review",
  "access.request",
  "worker.read",
].map((action, i) =>
  document<Audit>(`AUD-DEMO-${61 - i}`, "recorded", {
    actor: i % 2 ? "reader-demo" : "operator-demo",
    action,
    resource: i === 0 ? releases[0].id : incidents[0].id,
    outcome: i === 0 ? "blocked" : "recorded",
    request_id: `REQUEST-DEMO-${i + 1}`,
  }),
);
export const demoSnapshot: Snapshot = {
  observed_at: at,
  signals: [
    {
      id: "api",
      label: "API / HTTP",
      state: "unknown",
      detail: "HTTP 可达性 / /healthz",
    },
    {
      id: "postgres",
      label: "Postgres",
      state: "unknown",
      detail: "连接、容量与队列深度",
    },
    { id: "redis", label: "Redis", state: "unknown", detail: "缓存与任务协作" },
    {
      id: "caddy",
      label: "入口与证书",
      state: "unknown",
      detail: "Caddy / 域名 / TLS 到期时间",
    },
  ],
  runs: { items: runs, next_cursor: "" },
  incidents: { items: incidents, next_cursor: "" },
  releases: { items: releases, next_cursor: "" },
  audit: { items: audit, next_cursor: "" },
  migrations: [
    "001_init.sql",
    "002_social.sql",
    "003_user_phone.sql",
    "004_notifications_actor_image.sql",
    "005_adoption.sql",
    "006_listing_category.sql",
    "007_mobile_modules.sql",
    "008_mobile_policy_commands.sql",
    "009_pet_discussions.sql",
  ].map((name) => ({
    name,
    source_sha256: "",
    recorded_sha256: "",
    state: "unknown",
    baseline: false,
  })),
};
