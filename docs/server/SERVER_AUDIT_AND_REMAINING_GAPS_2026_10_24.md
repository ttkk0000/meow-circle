# M&D / meow & doggie 服务端完整性审计与待完善清单（2026-10-24，2026-10-25 复核更新）

2026-10-25续作修正：[本批源码与边界](../client/REMAINING_CODE_IMPLEMENTATION_2026_10_25.md)。当前245 Paths／275 Operations／124 Schemas，75对DTO，12迁移与8类worker源码；011数组／前缀索引、活跃领养筛选、Web续期加密回执、012数据库作业协调、私有S3端口和两类最小投影事件已写。原审计部分“现状”已过期：`identity/rate.go`已是数据库预算，`qualification.go`已按Child RefID筛选，Outbox实际按100条分页，不能照旧文段重复修改。完整搜索引擎接收端及运维执行器按用户要求继续占位；所有构建／迁移／运行均未验证，下文保留为审计历史而非当前缺口逐项判定。

> 状态：2026-10-24 服务端代码与基础设施静态审计快照（2026-10-25 同步最新 OpenAPI `241` Paths / `271` Operations / `123` Schemas 及 PlantUML 图谱 [`docs/uml/`](../uml/README.md)）
> 适用范围：`cmd/server`、`cmd/worker`、`internal/identity`、`internal/mobile`、`internal/platform/mobileapi`、`internal/platform/mobileauth`、`internal/store/postgres`、`migrations/001`–`009`、`docs/api/mobile-v2.openapi.json`、`Dockerfile`、`docker-compose.yml`、`Makefile`
> 验收口径说明：本文基于仓库源码与配置静态审计（未执行 `go build` / `go test` / `gradle`）。所有“已落地”均指**源码与 OpenAPI 契约级落地**，不等同于生产运行态验收。完整代码逻辑与 UML 图解见[全栈代码逻辑与 PlantUML 手册](../CODE_LOGIC_AND_UML_REFERENCE.md)。

---

## 1. 当前服务端源码基线总览（截至 2026-10-25）

经过 `2026-10-22` 至 `2026-10-25` 多批次演进，服务端 `/api/v2` 与 `cmd/worker` 已形成完整的业务域源码闭环：

| 维度 | 当前基线指标 | 关键源码与契约位置 |
|---|---|---|
| **OpenAPI 3.1 契约** | `241` Paths / `271` Operations / `123` Server Schemas（`74` 对跨语言 DTO） | `docs/api/mobile-v2.openapi.json`、`internal/platform/mobileapi/assets.go` |
| **PostgreSQL 迁移** | `001`–`009` 共 9 个 SQL 迁移文件 | `migrations/001_init.sql` 至 `migrations/009_pet_discussions.sql` |
| **`mobile_*` 聚合表** | **44 张**（`007` 32 张 + `008` 10 张 + `009` 2 张） | `internal/mobile/repository.go`（`Kinds` 切片共 44 项）、`internal/store/postgres/mobile.go` |
| **Worker 后台任务** | **6 类**（`push` / `nearby_alerts` / `content_updates` / `privacy_cleanup` / `provider_revocations` / `maintenance`） | `cmd/worker/main.go`、`internal/platform/mobileapi/jobs.go` |
| **认证、三方登录、年龄核验与监护人邀请** | B04 账号合并 + 官方三方 Create vs Link 分离（`409 identity_in_use`）+ 外部年龄核验网关（`age_gateway.go`）+ 未成年人 15 分钟一次性监护人邀请与独立成年关系申诉（`guardian_invites.go`）+ B05 资格重算（`requireInteractionContract()`）+ 浏览器 `HttpOnly Cookie` + `X-MND-CSRF` 适配 | `internal/identity/service.go`、`internal/identity/age_assurance.go`、`internal/identity/guardian_invites.go`、`internal/platform/mobileauth/tencent_apps.go`、`internal/platform/mobileapi/browser.go` |
| **高德地图代理与粗粒度坐标隐私** | Web 高德 JS API 2.0 安全代理 `/maps/amap/_AMapService/`（服务端注入 `jscode`）+ `GCJ-02`/`WGS-84` 小数点后 2 位粗粒度坐标量化 | `internal/platform/mobileapi/amap.go`、`internal/mobile/lost_pet.go` |
| **领养与寄养独占闭环** | 宠物级唯一活跃放置（`validatePetPlacementAvailable`）+ 候选队列（`waitlisted`）+ 重开周期递增（`adoption_cycle`）+ 72h 紧急转交（`safety_hold`） | `internal/mobile/adoption_placement.go`、`internal/mobile/authority.go`、`internal/mobile/service.go` |
| **同城预约与合规准入** | 城市与物种准入（猫/狗直接开放；异宠须 `exotic_approved_city: true` 且资质有效）+ 医疗免责拦截 + 100m 地理隐私模糊 | `internal/mobile/appointment_admission.go`、`internal/mobile/qualification.go` |
| **跨单据证据保留与注销** | 纠纷/回访/申诉/安全保留自动冻结关联消息、状态历史、审计与媒体（`BlockedByHold`），解冻前禁止物理清除 | `internal/mobile/evidence_retention.go`、`internal/mobile/data_requests.go` |
| **公开阅读与法规制度库** | 独立 `/api/v2/reading/*`（发布管理复用现有资源列表的 `owner_id` 查询，不存在 `/api/v2/publications/*`） + SHA-256 附件下载 + 运营主体空值发布拦截 | `internal/platform/mobileapi/reading.go`、`internal/platform/mobileapi/reading-drafts.json` |
| **宠物评论、反应与订阅** | 宠物档案评论（`mobile_pet_comments`）、评论表情反应（`mobile_comment_reactions`）、内容/救助订阅通知（`content_updates`） | `internal/mobile/comment_reactions.go`、`internal/mobile/content_subscriptions.go`、`migrations/009_pet_discussions.sql` |

---

## 2. 服务端仍需完善的 6 大核心问题（按优先级排序）

尽管业务领域模型与 API 契约已高度完备，但在**部署引导一致性、数据库索引、高并发事务边界、多实例水平扩展、静态合规数据注入、外部网关运行态联调**六个方面仍存在明确缺口，建议按 `P0 -> P1 -> P2` 顺序完善：

### 2.1 `[P0]` 数据库迁移脚本与容器/Makefile 启动配置不一致（会导致容器启动直接报错）

2026-10-24 Codex续作：以下是审计时发现，后续已补Compose的008／009新库挂载、Makefile 001–009顺序及Dockerfile的Web构建阶段／worker与Web产物复制。这里只完成源码修订，未执行构建、迁移或容器启动，不能把原P0直接判定为运行通过；既有数据库仍须核对实际迁移记录，只应用未执行文件。

* **现状与根因**：
  1. `internal/store/postgres/mobile.go:278-297` 中的 `CheckMobileSchema()` 在服务启动（`MND_AUTO_MIGRATE=false` 默认值）及 `/healthz` 检查时，强制校验以下表必须存在：
     * `005`–`006`：`user_identities`、`user_interaction_contracts`
     * `007`：`mobile_pets`、`mobile_media_objects`、`mobile_audit_events`
     * `008`：`mobile_qualifications`、`mobile_operations`、`mobile_data_requests`
     * `009`：`mobile_pet_comments`、`mobile_comment_reactions`
  2. 但 `docker-compose.yml:21-27` 仅挂载了 `001_init.sql` 至 `007_mobile_modules.sql`，**漏挂了 `008_mobile_policy_commands.sql` 和 `009_pet_discussions.sql`**。使用 `docker compose up` 初始化新库时，`cmd/server` 会因缺少 `008`/`009` 表而在 `CheckMobileSchema()` 阶段直接退出。
  3. `Makefile:20-25` 的 `make migrate` 目标仅执行了 `001`–`004`，**未包含 `005`–`009`**。
  4. `Dockerfile:5-22` 仅编译了 `/out/meow-server`（`./cmd/server`），**未编译 `./cmd/worker`**，且未打包 `web-client/dist`（导致容器内默认 `/app/` 静态挂载路径返回 `503 WEB_CLIENT_DIST_UNAVAILABLE`）。
* **完善建议**：
  1. 在 `docker-compose.yml` 的 `postgres.volumes` 中补齐 `008_mobile_policy_commands.sql` 与 `009_pet_discussions.sql` 挂载，并可增加 `worker` 服务定义（复用同一镜像执行 `/app/meow-worker -loop`）。
  2. 在 `Makefile` 的 `migrate` 目标中按序补齐 `005`–`009`。
  3. 在 `Dockerfile` 编译阶段增加 `go build -trimpath -ldflags="-s -w" -o /out/meow-worker ./cmd/worker`，并在多阶段构建中可选构建 `web-client/dist` 复制到 `/app/web-client/dist`。

---

### 2.2 `[P1]` PostgreSQL 缺少 `GIN(media_ids)`、部分 `GIN(participants)` 及 `record_key` 前缀索引

* **现状与根因**：
  1. **`media_ids` 全表扫描风险**：`internal/store/postgres/mobile.go:132-135` 与 `203-206` 在执行 `Filter.MediaID` 查询时生成 `media_ids @> ARRAY[$N]::bigint[]`。该查询在媒体清理（`privacy_cleanup`）与证据保留引用检查中被频繁调用，但 **`007`、`008`、`009` 共 44 张 `mobile_*` 表均未创建 `USING GIN (media_ids)` 索引**，数据量增长后将触发全表顺序扫描。
  2. **`participants` GIN 索引覆盖不全**：`007_mobile_modules.sql` 仅对 11 张表创建了 `USING GIN (participants)`，而 `008_mobile_policy_commands.sql`（如 `mobile_agreements`、`mobile_handovers`、`mobile_disputes`、`mobile_appeals`）和 `009_pet_discussions.sql`（`mobile_pet_comments`、`mobile_comment_reactions`）均未建立 `GIN(participants)` 索引，按参与人过滤时无法命中索引。
  3. **`KeyPrefix` 索引失效风险**：`internal/store/postgres/mobile.go:136-139` 与 `207-209` 使用 `strpos(record_key, $N) = 1` 进行前缀匹配（例如 `outbox.go`、`comment_reactions.go` 按前缀检索）。PostgreSQL 的普通 B-Tree 索引无法优化函数包裹列 `strpos(record_key, ...)`，在高频调用时会退化为全表或全索引扫描。
* **完善建议**：
  1. 新增 `migrations/010_mobile_indexes.sql`，为需要按 `media_ids` 和 `participants` 检索的 `mobile_*` 表补齐 `USING GIN (media_ids)` 与 `USING GIN (participants)` 索引。
  2. 将 `internal/store/postgres/mobile.go` 中的 `strpos(record_key, $N) = 1` 改为 `record_key LIKE $N || '%'`（配合转义 `_`/`%`）并结合 `text_pattern_ops` 索引，或改用范围条件 `record_key >= $prefix AND record_key < $prefix_upper` 以直接命中现有 `UNIQUE(record_key)` B-Tree 索引。

---

### 2.3 `[P1]` `SERIALIZABLE` 事务内存在无上界的 `allRecords()` 内存全量拉取

* **现状与根因**：
  1. `internal/store/postgres/mobile.go:304` 的 `Tx()` 统一开启 `sql.LevelSerializable`（PostgreSQL `SERIALIZABLE` 隔离级别）。
  2. `internal/mobile/service.go` 中的辅助函数 `allRecords(tx, kind, filter)` 会以每页 `100` 条循环调用 `tx.List(...)` 直至拉取完所有匹配记录到 Go 内存中过滤。
  3. 目前多个核心写路径在 `SERIALIZABLE` 事务内调用了 `allRecords`：
     * `internal/mobile/adoption_placement.go:53`：按 `PetID` 拉取该宠物的全部历史申请记录以检查活跃放置或晋升候选（`promoteWaitlistedApplication`）。
     * `internal/mobile/qualification.go:196`：在预约准入检查中调用 `allRecords(tx, KindGuardians, Filter{Status: "verified", Limit: 100})` 拉取**全平台所有已认证监护人**到内存中再比对 `provider.UserID`！
     * `internal/mobile/evidence_retention.go:196-265`：在创建证据保留单与执行清理时，通过 `allRecords` 扫描关联纠纷、申诉、回访、历史记录。
     * `internal/mobile/data_requests.go:200`：在账号注销/导出时对 40+ 张表逐表执行 `allRecords`。
     * `internal/mobile/outbox.go:155,213`：在后台任务扫描待投递事件时拉取全部 `pending`/`retry` 记录。
  4. **隐患**：在 `SERIALIZABLE` 事务中执行大范围读取（尤其是 `qualification.go:196` 按 `Status: "verified"` 扫描全表监护人）会在 PostgreSQL 中产生大量页级/表级 `SIREAD` 谓词锁，极易在高并发预约或申请时引发 `40001 (serialization_failure)` 冲突，且随数据量增长导致内存与延迟线性劣化。
* **完善建议**：
  1. **立即修复 `qualification.go:196`**：在查询 `KindGuardians` 时直接传入 `Filter{OwnerID: provider.UserID, Status: "verified", Limit: 10}`（利用 `(owner_id, updated_at DESC)` 索引），避免扫描全平台监护人。
  2. **收敛 `Filter` 下推能力**：在 `mobile.Filter` 与 `postgres/mobile.go` 中支持 `Statuses []string`（`status = ANY($N)`）与有限条数查询，让 `adoption_placement.go` 只查询活跃状态（而非拉取所有历史已取消/已拒绝申请）。
  3. **Outbox 批处理限流**：在 `outbox.go` 的 `ProcessDue` 中使用 `Limit: batchSize` 分批拉取并独立提交事务，避免单次扫描全量积压事件。

---

### 2.4 `[P1/P2]` 单实例有状态假设与多副本水平扩展（HA）短板

* **现状与根因**：
  1. **内存级限流器与连接计数**：`internal/identity/rate.go`（登录/验证码频控）与 `internal/platform/auth/ratelimit.go`（HTTP IP 限流、WebSocket 每用户最大连接数）均使用进程内 `sync.Mutex + map`。多副本部署时，攻击者可通过轮询不同实例绕过频控，且单用户连接数限制无法跨实例共享。
  2. **本地磁盘媒体存储**：`internal/platform/mobileapi/server.go` 将上传媒体保存在本地目录 `MND_MOBILE_MEDIA_DIR`（默认 `data/mobile-media`）。多副本部署或容器重建时，若未挂载共享存储或接入对象存储（S3/OSS/COS），会出现跨实例读取 404。
  3. **Worker 本地状态文件与并发锁缺失**：`cmd/worker/main.go` 将执行位点写在本地 `MND_WORKER_STATE_DIR`（`data/mobile-worker/*.json`），且没有 PostgreSQL Advisory Lock（`pg_try_advisory_lock`）。若同时启动两个 `cmd/worker` 副本，会并发重复执行 `push` / `privacy_cleanup`。
  4. **浏览器会话签名密钥**：`internal/platform/mobileapi/browser.go` 支持 Cookie 会话，多副本下需确保环境变量显式固定同一套 `JWT_SECRET`，否则跨实例请求会触发会话失效。
* **完善建议**：
  1. 在 `cmd/worker` 每轮 Job 执行前增加基于 PostgreSQL 的 `pg_try_advisory_lock(job_hash)` 或数据库锁表，并将 Job 游标状态持久化到 PostgreSQL 表（或复用 `mobile_operations`）。
  2. 抽象 `MediaStorage` 接口，保留 `LocalDiskStorage` 作为开发默认实现，新增 `S3CompatibleStorage`（支持预签名上传/下载与私有桶读取）供生产环境使用。
  3. 生产多实例部署时，支持基于 Redis 或 PostgreSQL 滑动窗口的分布式限流实现（保留内存实现作为单机 fallback）。

---

### 2.5 `[P2]` 公开阅读目录（`reading-drafts.json`）与武汉/上海城市准入模板数据待注入

* **现状与根因**：
  1. `internal/platform/mobileapi/reading-drafts.json` 目前仅内置了 **1 篇** 照护指南（`care-first-cat`）和 **9 篇** 简略草案摘要，且所有条目的 `operator_name`、`operator_address`、`operator_contact` 均为空字符串 `""`。
  2. 根据 `internal/platform/mobileapi/reading.go:108-112` 的门禁逻辑：当 `status == "published"` 但缺少主体名称、地址或联系方式时，服务端会直接拒绝加载并报错。因此当前默认目录只能停留在 `draft` 状态。
  3. 产品文档已于 `2026-10-24` 完成《武汉/上海首批城市物种准入矩阵与四类标准协议模板》（`docs/design/MND_CITY_SPECIES_AND_CONTRACT_TEMPLATES.md`），但服务端种子数据与 `reading-drafts.json` 尚未将该文档中的 **武汉/上海猫狗及异宠准入细则**、**四类标准电子协议完整正文（领养/送养/救助转交/服务商家入驻）** 同步写入。
  4. `docs/server/environment.example` 尚未收录新增的 `MND_WEB_ORIGIN`（浏览器 CORS/CSRF 源）与 `MND_READING_CATALOG`（外部阅读目录 JSON 路径）两个环境变量。
* **完善建议**：
  1. 更新 `docs/server/environment.example`（本次审计已同步补齐 `MND_WEB_ORIGIN` 与 `MND_READING_CATALOG`）。
  2. 在下一轮服务端代码批次中，将 `docs/design/MND_CITY_SPECIES_AND_CONTRACT_TEMPLATES.md` 的四类标准协议条款与武汉/上海城市准入说明增补进 `reading-drafts.json`（或生产环境挂载的 `MND_READING_CATALOG` 文件），并在管理端配置武汉（`420100`）、上海（`310100`）的城市准入种子参数。

---

### 2.6 `[P1/P2]` 外部网关、异步投影消费端与 Go 工具链版本运行态验证

* **现状与根因**：
  1. **Go 工具链版本声明**：`go.mod:3` 声明为 `go 1.25.0`。在部分使用标准 `1.23.x` / `1.24.x` 工具链格式的机器或 CI 环境中曾触发 `invalid go version '1.25.0': must match format 1.23`。
  2. **搜索与审计外部投影消费端待对接**：`internal/mobile/outbox.go` 已在写路径中稳定生成 `search_projection` 与 `audit_projection` Outbox 事件，但目前僅完成了本地投影表落盘与队列推进，尚未对接外部搜索引擎（如 Meilisearch / Elasticsearch）或独立 SIEM 审计归档网关。
  3. **生产外部网关未联调**：`SMS_GATEWAY_URL`、`EMAIL_GATEWAY_URL`、WeChat/QQ/Apple OAuth、APNs/FCM `PUSH_GATEWAY_URL`、`MND_CONTENT_PROPAGATOR` 在 `MND_ENV=production` 下均采用 fail-closed 策略（未配置即返回 `503 GATEWAY_UNAVAILABLE`），尚未在真实预发环境中完成端到端联调验收。
* **完善建议**：
  1. 在具备合规 Go 工具链的 CI/预发容器内执行 `migrations/001`–`009` 全量建库与 `cmd/server`、`cmd/worker` 启动验证。
  2. 统一团队与 CI 的 Go 版本声明规范（如确认目标环境为 Go 1.23/1.24，可将 `go.mod` 调整为兼容格式）。
  3. 按 `docs/server/V2_RELEASE_RUNBOOK_2026_10_23.md` 逐项完成真实网关凭证注入与回归测试。

---

## 3. 服务端完善优先级路线图（Action Checklist）

| 优先级 | 模块 / 文件 | 改动类型 | 具体收口动作 |
|---|---|---|---|
| **P0** | `docker-compose.yml` / `Makefile` / `Dockerfile` | 部署与构建配置 | 补齐 `008`、`009` SQL 挂载与 `make migrate` 列表；在 `Dockerfile` 中增加 `cmd/worker` 编译产物与 `web-client/dist` 静态产物构建。 |
| **P1** | `internal/mobile/qualification.go` | 事务查询优化 | 第 196 行 `allRecords(tx, KindGuardians, ...)` 增加 `OwnerID: provider.UserID` 过滤，消除 `SERIALIZABLE` 事务内全表扫描监护人。 |
| **P1** | `migrations/010_mobile_indexes.sql`（待建） + `internal/store/postgres/mobile.go` | 数据库索引与查询 | 为 `mobile_*` 表补齐 `GIN(media_ids)` 与缺失的 `GIN(participants)` 索引；将 `strpos(record_key, $1) = 1` 优化为可走 B-Tree 索引的前缀范围匹配。 |
| **P1** | `internal/mobile/adoption_placement.go` / `outbox.go` | 事务边界收敛 | 支持 `Filter.Statuses` 下推过滤，减少 `allRecords` 内存过滤行数；`outbox.go` 改为按 `Limit` 分批拉取处理。 |
| **P2** | `cmd/worker/main.go` / `internal/platform/mobileapi/server.go` | 多副本高可用 | 为 `cmd/worker` 增加 PostgreSQL Advisory Lock 防多副本重复执行；为媒体存储增加对象存储（S3/OSS）接口适配。 |
| **P2** | `internal/platform/mobileapi/reading-drafts.json` | 合规内容数据 | 注入武汉/上海城市物种准入细则与四类标准协议完整草案；上线前通过 `MND_OPERATORS_JSON` 或阅读目录注入真实运营主体三要素。 |
| **P2** | `docs/server/environment.example` | 运维配置文档 | 补齐 `MND_WEB_ORIGIN` 与 `MND_READING_CATALOG` 环境变量说明（已在本次文档更新中完成）。 |
