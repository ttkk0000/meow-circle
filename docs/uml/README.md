# M&D (`meow & doggie`) PlantUML (`.puml`) 架构与全栈代码逻辑图谱索引

本目录存放 M&D V2 全栈代码库（Go 服务端、PostgreSQL `001`–`009`、KMP `shared`、KMP `clientUi`、`androidApp`、`iosUi`/`iosApp`、`desktopApp`、`web-client` 消费端与 `/app/operator` 运营台）的**独立 PlantUML 源码文件（`.puml`）**。

所有 PlantUML 图均与 2026-10-25 最新代码基线（`241` 个 OpenAPI Paths、`271` 个 Operations、`123` 个服务端 Schemas、`74` 对跨语言 DTO、`44` 张 `mobile_*` 业务表、`34` 个 `kmp/clientUi` 共享 Compose 文件、`42` 个 `FeaturePage` 页面）严格对齐。完整代码逻辑详解请参阅主手册 [`docs/CODE_LOGIC_AND_UML_REFERENCE.md`](../CODE_LOGIC_AND_UML_REFERENCE.md)。

---

## 1. 图谱清单与代码映射矩阵

| 序号 | PlantUML 文件 | 图类型 | 核心覆盖范围与对应源码目录 |
| :--- | :--- | :--- | :--- |
| **01** | [`01-system-component-architecture.puml`](./01-system-component-architecture.puml) | 组件与部署架构图 (Component Diagram) | 六端客户端（Android / iOS / Desktop / Web 消费端 / Web `/app/operator`）、KMP `shared` + `clientUi`、Go `cmd/api` + `cmd/worker`、PostgreSQL 16、外部网关（微信/QQ/Apple/外部年龄网关/高德地图代理/S3/阿里云短信）全链路拓扑 |
| **02** | [`02-kmp-mvi-and-clientui-class-diagram.puml`](./02-kmp-mvi-and-clientui-class-diagram.puml) | 类图 (Class Diagram) | `kmp/shared` MVI 基类 `MviStore<S,I,E>`、`MobileStore`、`MobileFeatureStore`、`SensitiveActionStore`、`ProviderAuthStore`、`AgeAssuranceStore`、`GuardianInviteStore`、`42` 个 `FeaturePage`、`kmp/clientUi` 共享组件库与 `ClientUiPlatform` 三端实现、`MobileMviViewModel` |
| **03** | [`03-server-domain-and-service-class-diagram.puml`](./03-server-domain-and-service-class-diagram.puml) | 类图 (Class Diagram) | Go 服务端 `internal/identity`、`internal/mobile`、`internal/platform/mobileapi`、`internal/platform/mobileauth`、`internal/platform/postgres`、`internal/platform/mobileworker` 核心接口、服务类与中间件 |
| **04** | [`04-database-er-schema.puml`](./04-database-er-schema.puml) | 数据库实体关系图 (ER Diagram) | `migrations/001`–`009` 全量表结构：身份与会话表、资质与监护人同意表、幂等账本 `mobile_command_ledger`、证据保全锁 `mobile_evidence_holds`、媒体预留 `mobile_media_reservations`、Outbox 队列及 `44` 张 `mobile_*` 领域表（含 `009_pet_discussions.sql`） |
| **05** | [`05-auth-provider-and-account-linking-sequence.puml`](./05-auth-provider-and-account-linking-sequence.puml) | 时序图 (Sequence Diagram) | 密码/短信验证码显式登录（禁止短信隐式自动注册）、微信/QQ/Apple 官方三方登录（原生 SDK 与 Web 浏览器重定向）、Create vs Link 严格分离、`purpose_reauth` 授权绑定与 `409 identity_in_use` 冲突恢复 |
| **06** | [`06-age-assurance-and-guardian-consent-sequence.puml`](./06-age-assurance-and-guardian-consent-sequence.puml) | 时序图 (Sequence Diagram) | 外部年龄核验网关（HMAC-SHA256 挑战与回调）、未成年人 15 分钟一次性监护人邀请码、成年监护人（实名+18+）`reauth` 同意、独立成年关系申诉（`identity_verifier` 审核）、未成年人陌生人私信 `directContact` 事务级拦截 |
| **07** | [`07-idempotent-write-and-sensitive-reauth-sequence.puml`](./07-idempotent-write-and-sensitive-reauth-sequence.puml) | 时序图 (Sequence Diagram) | `SensitiveActionStore` 10 分钟单次有效 `purpose_reauth_grants`、四端加密 `CommandVault`、`X-Client-Request-Id` + PostgreSQL `SERIALIZABLE` 幂等账本、`OperationResultUnknown` 对账恢复与本地草稿/照片队列保护 |
| **08** | [`08-adoption-exclusivity-contract-and-return-state.puml`](./08-adoption-exclusivity-contract-and-return-state.puml) | 状态机图 (State Diagram) | `F07` 领养宠物级试养排他锁（同一 `pet_id` 仅允许 1 个活跃试养）、候补自动切换、领养协议修订号 `rev_no` 签署、领养后双向退回提议/撤回/72h 过期与 `adoption_cycle` 周期递增归档 |
| **09** | [`09-appointment-dual-track-and-safety-hold-state.puml`](./09-appointment-dual-track-and-safety-hold-state.puml) | 状态机图 (State Diagram) | `F08` 上下文绑定准入（必须绑定有效领养申请/寻宠认领/服务咨询）、双轨改期状态机（`effectiveVersion` 原生效排期保留 + `pendingProposal` 待确认提议）与 `safety_hold` 安全冻结 |
| **10** | [`10-lost-pet-amap-and-claim-handover-sequence.puml`](./10-lost-pet-amap-and-claim-handover-sequence.puml) | 时序图 (Sequence Diagram) | `F09` 高德地图合规隐私弹窗、`GCJ-02`/`WGS-84` 坐标系显式标注与小数点后 2 位粗粒度坐标量化、Web `/maps/amap/_AMapService/` 安全代理、同坐标系附近检索、私密认领与双向交接确认 |
| **11** | [`11-data-lifecycle-erasure-and-worker-jobs-sequence.puml`](./11-data-lifecycle-erasure-and-worker-jobs-sequence.puml) | 时序图 (Sequence Diagram) | `F12` 数据导出/擦除与账号注销冷静期、跨案件 `mobile_evidence_holds` 证据保全阻断、客户端 `DataRequestProgress` 透明展示，以及 `cmd/worker` 6 个后台定时任务完整执行流 |
| **12** | [`12-inbox-unread-snapshot-and-chat-sequence.puml`](./12-inbox-unread-snapshot-and-chat-sequence.puml) | 时序图 (Sequence Diagram) | `F11` 全局 `99+` 未读聚合、10 分钟 `InboxReadSnapshot` 水位线一键已读、会话 `firstUnread` 锚点、128 个 Noto Unicode 表情与 24 个 `144dp` 贴纸发送，以及 `009` 宠物/动态嵌套评论与表情反应事务切换 |

---

## 2. 渲染与校验方式

- **IDE 预览**：在 IntelliJ IDEA / Android Studio / VS Code 中安装 **PlantUML** 插件即可直接打开 `.puml` 文件实时预览矢量图。
- **命令行渲染**：
  ```bash
  plantuml -tsvg docs/uml/*.puml
  ```
- **静态完整性检查**：所有 `.puml` 文件均包含成对的 `@startuml` 与 `@enduml` 标记，采用 UTF-8 无 BOM 编码，并由仓库静态文档检查脚本统一验证。
