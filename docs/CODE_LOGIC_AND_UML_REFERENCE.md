# M&D (`meow & doggie`) 全栈代码逻辑与 PlantUML 架构总览手册

> **版本基线**：2026-10-25 全量源码基线
> **关联 UML 源码目录**：[`docs/uml/`](./uml/README.md)（共 12 份独立 `.puml` PlantUML 架构、类图、ER 图、时序图与状态机图）
> **核心契约权威**：[`docs/api/mobile-v2.openapi.json`](./api/mobile-v2.openapi.json)（`241` Paths / `271` Operations / `123` Server Schemas）

本文档系统化梳理 M&D (`meow & doggie`) 仓库中**所有服务端与多端客户端代码模块的核心逻辑、状态机不变量、事务隔离级别、安全边界与跨端复用机制**，并与 [`docs/uml/`](./uml/README.md) 下的 12 份独立 PlantUML (`.puml`) 文件一一对应。

---

## 1. 全栈架构量化基线与组件拓扑

- **对应 PlantUML 图**：[`docs/uml/01-system-component-architecture.puml`](./uml/01-system-component-architecture.puml)

### 1.1 2026-10-25 代码库核心指标

| 维度 | 精确数量 / 规格 | 权威代码位置 |
| :--- | :--- | :--- |
| **OpenAPI 3.1 契约** | `241` Paths / `271` Operations / `123` Server Schemas（Web 派生 `125` Schemas） | [`docs/api/mobile-v2.openapi.json`](./api/mobile-v2.openapi.json)、[`web-client/src/core/schemas.json`](../web-client/src/core/schemas.json) |
| **跨语言 DTO 对齐** | `74` 对核心请求/响应 DTO（Go 结构体 ↔ Kotlin `@Serializable` ↔ TypeScript 类型） | [`internal/identity/`](../internal/identity/)、[`internal/mobile/`](../internal/mobile/)、[`kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/) |
| **PostgreSQL 迁移与表** | `001`–`009` 共 9 份 SQL 迁移；含身份/安全/账本/证据锁/Outbox 表 + `44` 张 `mobile_*` 领域资源表 | [`migrations/001_init.sql`](../migrations/001_init.sql) .. [`migrations/009_pet_discussions.sql`](../migrations/009_pet_discussions.sql) |
| **KMP 共享逻辑 (`kmp/shared`)** | `20` 个 `commonMain` Kotlin 源码文件；包含 `MviStore` 基类、`MobileStore`、`MobileFeatureStore` 及 4 个专业子 Store | [`kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/) |
| **KMP 共享 UI (`kmp/clientUi`)** | `34` 个 `commonMain` Compose 源码文件 + 三端 `PlatformText.kt`；统一承载 `42` 个 `FeaturePage` 二级业务页 | [`kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/) |
| **多端宿主覆盖** | Android Phone/Tablet (`API 26..37`)、iPhone/iPad (`iosUi`/`iosApp`)、Compose Desktop (`JVM 21`)、Web 消费端 (`/app/*`)、Web 运营工作台 (`/app/operator`) | [`kmp/androidApp/`](../kmp/androidApp/)、[`kmp/iosUi/`](../kmp/iosUi/)、[`kmp/desktopApp/`](../kmp/desktopApp/)、[`web-client/`](../web-client/) |
| **后台定时任务 (`cmd/worker`)** | `6` 个幂等后台任务（`push`、`nearby_alerts`、`content_updates`、`privacy_cleanup`、`provider_revocations`、`maintenance`） | [`cmd/worker/main.go`](../cmd/worker/main.go)、[`internal/platform/mobileworker/runner.go`](../internal/platform/mobileworker/runner.go) |
| **静态校验脚本** | `12` 个 Python 静态源码与契约门禁脚本 | [`scripts/`](../scripts/) |

---

## 2. KMP 共享 MVI 框架与 `kmp/clientUi` 多端复用代码逻辑

- **对应 PlantUML 图**：[`docs/uml/02-kmp-mvi-and-clientui-class-diagram.puml`](./uml/02-kmp-mvi-and-clientui-class-diagram.puml)

### 2.1 `MviStore<State, Intent, Effect>` 泛型状态机基类

位于 [`kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MviStore.kt`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MviStore.kt)：

1. **统一状态与副作用流契约 (`MviStateOwner<State, Intent, Effect>`)**：
   - `state: StateFlow<State>`：单向不可变 UI 状态快照（通过 `MutableStateFlow.update(reducer)` 原子更新）。
   - `effects: SharedFlow<Effect>`：单次消费副作用流（`extraBufferCapacity = 32`，用于导航跳转、Toast 提示、外部链接拉起）。
   - `dispatch(intent: Intent)`：统一意图分发入口，委托给子类实现的 `protected abstract fun handleIntent(intent: Intent)`。
2. **协程异常守卫 (`launchGuarded`)**：
   - 所有异步网络与存储操作通过 `launchGuarded(onError = ...)` 在 `scope` 内启动，自动透传 `CancellationException`，避免协程取消被吞，同时将业务/网络异常收敛到统一错误回调。

### 2.2 核心 Store 与子 Store 组合关系

| Store 类名 | 继承/组合关系 | 核心代码职责与状态迁移逻辑 |
| :--- | :--- | :--- |
| **`MobileStore`** | `: MviStore<MobileUiState, MobileIntent, MobileEffect>` | 管理应用主壳 (`TopLevelDestination` 五大一级导航：Home / Discover / Create / Messages / Profile)、Bootstrap 会话冷启动恢复、密码/短信显式登录、首页 Feed、宠物档案、会话列表、128 个 Unicode 表情与 24 个 `144dp` 贴纸发送、全局未读 `99+` 与 10 分钟快照已读。 |
| **`MobileFeatureStore`** | `: MviStore<MobileFeatureUiState, MobileFeatureIntent, MobileFeatureEffect>` | 管理 `42` 个 `FeaturePage` 二级页面的数据拉取、表单状态与乐观/悲观写操作；内部组合持有 `sensitive`、`providerAuth`、`ageAssurance`、`guardianInvites` 四个专业 Store。 |
| **`SensitiveActionStore`** | `: MviStore<SensitiveActionUiState, SensitiveActionIntent, SensitiveActionEffect>` | 负责高风险写操作的 10 分钟单次有效 `purpose_reauth` 授权获取、四端加密 `CommandVault` 持久化、`X-Client-Request-Id` 幂等命令跟踪、`OperationResultUnknown` 断网未知结果对账恢复，以及 `LocalPhotoBatchDraft` 照片批量草稿恢复。 |
| **`ProviderAuthStore`** | 组合于 `MobileFeatureStore.providerAuth` | 负责微信 (`wechat`)、QQ (`qq`)、Apple (`apple`) 三方官方登录 (`startSignIn`) 与已登录账号显式绑定 (`startLink`) 的双轨状态管理；捕获 `409 identity_in_use` 并记录 `ProviderAccountConflict`，绝不触发静默账号合并。 |
| **`AgeAssuranceStore`** | 组合于 `MobileFeatureStore.ageAssurance` | 管理外部年龄核验网关会话创建 (`POST /api/v2/qualification/age-assurance/sessions`)、HMAC 签名回调完成 (`POST /api/v2/qualification/age-assurance/callback`) 与 `ageBand` 状态刷新。 |
| **`GuardianInviteStore`** | `: MviStore<GuardianInviteUiState, GuardianInviteIntent, GuardianInviteEffect>` | 管理未成年人生成/撤销 15 分钟一次性监护人邀请码、监护人按邀请码查询与 `reauth` 接受授权、以及独立成年关系申诉提交。 |

### 2.3 `kmp/clientUi` 三端共享 Compose UI 架构与 `ClientUiPlatform` 端口

位于 [`kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/)（共 `34` 个通用 Kotlin 文件）：

1. **自适应主壳 (`ConsumerApp.kt` & `AdaptiveLayout.kt`)**：
   - `Compact (<=600dp)`：单栏布局 + 底部 5 Tab 胶囊导航栏（中间圆角凸起 `+` 发布按钮）。
   - `Medium (601..839dp)`：左侧 `NavigationRail` + 主内容区 + 可折叠抽屉。
   - `Expanded (>=840dp)`：左侧导航栏 + 主次双栏 (`List-Detail` / `Feed + Context Inspector`)，严禁将手机单栏简单居中拉伸。
2. **42 个 `FeaturePage` 统一路由分发 (`FeaturePages.kt`)**：
   - **社交与关系 (`SocialPages.kt`)**：`FollowRequests`、`BlockedAccounts`、`Search`、`TopicDetail`、`Favorites`、`Lists`、`Notifications`、`NotificationPreferences`、`ContentSubscriptions` (`ContentSubscriptions.kt`)。
   - **创作、宠物、出版与共读 (`AccountPages.kt` / `PublicationsPage.kt` / `ReadingPages.kt`)**：`EntryComposer`、`PetWorkspace`（含 `009` 宠物嵌套评论与表情反应）、`Publications`、`PublicationDetail`、`PublicationWorkspace`、`ReadingLibrary`、`ReadingPlans`、`ReadingNotes`。
   - **关爱中心与高德地图 (`CarePages.kt` / `MapPage.kt`)**：`AdoptionCenter`、`AdoptionListingEditor`、`AdoptionApplicationCenter`、`Appointments`、`LostPetCenter`、`LostMap`。
   - **服务报价、争议与举报 (`OfferPages.kt`)**：`OfferCenter`、`SellerOnboarding`、`DisputeCenter`、`ReportsSafety`。
   - **资质、年龄核验、监护人与安全隐私 (`AgePages.kt` / `GuardianPages.kt` / `ProviderAuthPages.kt` / `AccountPages.kt`)**：`QualificationCenter`、`AgeAssurance`、`GuardianConsent`、`GuardianMinorInvites`、`GuardianAcceptInvite`、`AdultRelationshipClaims`、`SessionManagement`、`AccountIdentities`、`ProviderSignIn`、`AccountConflict`、`ReauthHistory`、`DataPrivacyCenter`、`AccountClosure`、`MediaQueue`、`UnknownCommandRecovery`。
3. **公共复用组件 (`PageComponents.kt` & `StatusComponents.kt`)**：
   - `ChoiceChipBar<T>`：泛型单选/多选胶囊筛选栏。
   - `SegmentedTextBar`：分段切换导航条。
   - `PhotoQueueList`：本地图片上传队列与状态卡片（支持移除、重试与草稿恢复）。
   - `OtpVerificationFields`：短信验证码输入与倒计时触发组件。
   - `MviStatusBanner`：统一渲染 MVI Store 的 `loading`、`error`、`notice` 顶部状态条。
   - `PendingLinkBanner`：外部浏览器跳转（如外部年龄网关、三方 OAuth、桌面端 `/app/map`）待处理提示横幅。
4. **平台能力桥接 (`ClientUiPlatform.kt`) 与三端实现**：
   - **Android (`AndroidClientPlatform.kt` + `AndroidMviBase.kt`)**：提供 `MobileMviViewModel<S, I, E, Store>` 生命周期桥接、`ActivityResultContracts.PickMultipleVisualMedia` 本地相册选择、`AndroidNativeMap` 高德 3D 地图 `MapView` 嵌入、`AndroidProviderAuth`（微信 `WXEntryActivity` / QQ `Tencent`）。
   - **iOS (`IosClientPlatform.kt` + `AmapNativeMap.swift` + `NativeProviderAuth.swift`)**：提供 `PHPickerViewController` 媒体选择、`MAMapView` 原生高德地图、`ASAuthorizationController` (Sign in with Apple) + 微信 `WXApi` + QQ `TencentOAuth` 原生桥接、以及 Keychain 安全存储。
   - **Desktop (`DesktopClientPlatform.kt` + `DesktopStorage.kt` + `LostMapScreen.kt`)**：提供 `FileDialog` 本地文件选择、`java-keyring:1.0.4` 系统钥匙串保存随机 256-bit AES-GCM 主密钥加密的命令金库与草稿、以及桌面端跳转系统浏览器 `/app/map` 拾取 `GCJ-02` 粗粒度坐标回填。

---

## 3. Go V2 服务端分层架构与核心领域服务代码逻辑

- **对应 PlantUML 图**：[`docs/uml/03-server-domain-and-service-class-diagram.puml`](./uml/03-server-domain-and-service-class-diagram.puml)

### 3.1 服务端核心包职责划分

| Go 包路径 | 核心类型与文件 | 代码逻辑与安全边界 |
| :--- | :--- | :--- |
| **`cmd/server`** | [`main.go`](../cmd/server/main.go) | 解析环境变量、初始化 PostgreSQL 连接池、注入外部适配器（阿里云短信、媒体处理、腾讯/Apple OAuth 验签器、外部年龄网关、高德安全代理）、挂载 `mobileapi.Handler` 并启动 HTTP 服务。 |
| **`cmd/worker`** | [`main.go`](../cmd/worker/main.go)、[`internal/platform/mobileworker/runner.go`](../internal/platform/mobileworker/runner.go) | 独立后台进程，按固定周期顺序调度 6 个幂等后台作业，支持单次执行 (`-once`) 与持久 SHA-256 游标文件。 |
| **`internal/identity`** | `service.go`、`age_assurance.go`、`guardian_invites.go`、`mutation.go`、`revocation.go` | 管理账号生命周期、显式注册与登录、会话刷新与吊销、10 分钟单次有效 `reauth` 授权、资质状态计算、外部年龄核验会话与回调、未成年人 15 分钟监护人邀请与独立成年关系申诉。 |
| **`internal/mobile`** | `service.go`、`social.go`、`rescue.go`、`adoption_placement.go`、`contracts.go`、`appointments.go`、`appointment_admission.go`、`offers.go`、`inbox.go`、`inbox_snapshot.go`、`publication.go`、`reading.go`、`data_requests.go`、`evidence_retention.go`、`jobs.go` | 实现全部消费端与运营端领域逻辑：Feed 与动态发布、宠物与共同管理人、`009` 宠物/动态嵌套评论与表情反应、关注/拉黑与未成年人陌生人私信拦截、领养排他锁与双向退回、预约双轨改期与安全冻结、寻宠粗粒度地图与双向交接、报价咨询与争议保全、消息快照已读、出版修订工作流、阅读计划与笔记、数据导出/擦除与账号注销。 |
| **`internal/platform/mobileapi`** | `handler.go`、`browser.go`、`browser_provider.go`、`amap.go`、`guardian_invites.go`、`ops_auth.go`、`ops_context.go`、`jobs.go` | HTTP 路由与中间件层：同时支持原生端 `Authorization: Bearer` 与 Web 端 `HttpOnly Cookie + X-MND-CSRF + Origin 校验`；提供高德地图反向代理 `/maps/amap/_AMapService/`；强制校验运营端命名角色权限。 |
| **`internal/platform/mobileauth`** | `tencent_apps.go`、`age_gateway.go` | 实现微信 (`code2session` / OAuth2)、QQ (`me` + `get_user_info`)、Apple (`JWKS` RS256 ID Token 验签) 多应用变体（Native / Web）凭证核验，以及外部年龄核验网关的 HMAC-SHA256 签名构造与常量时间验签。 |
| **`internal/store/postgres`** | [`mobile.go`](../internal/store/postgres/mobile.go) | 基于 `database/sql` 实现 `SERIALIZABLE` 事务级 `mobile_operations` 幂等命令执行、乐观锁版本 CAS、Outbox 批处理，以及 `44` 张 `mobile_*` 表的结构化列 + JSONB 读写。 |

---

## 4. PostgreSQL 数据库 Schema 与存储引擎设计 (`001`–`009`)

- **对应 PlantUML 图**：[`docs/uml/04-database-er-schema.puml`](./uml/04-database-er-schema.puml)

### 4.1 迁移演进脉络

1. **`001_initial_schema.sql`**：基础用户表 `users` 与早期社区基础结构。
2. **`002_v2_identity_auth.sql`**：引入多身份绑定表 `account_identities` (`UNIQUE(provider, subject_hash)`)、短信/验证挑战表 `auth_challenges`、多设备会话表 `user_sessions`、用户资质状态表 `user_qualification_status`、监护人同意表 `guardian_consents`、以及目的级二次认证授权表 `purpose_reauth_grants`。
3. **`003_v2_domain_foundations.sql`**：建立核心业务基础索引与约束。
4. **`004_v2_mobile_platform.sql`**：建立幂等账本 `mobile_command_ledger`、媒体预留表 `mobile_media_reservations`、Outbox 事件表 `mobile_outbox_events` 以及首批核心 `mobile_*` 业务表。
5. **`005_v2_mobile_ops_and_holds.sql`**：建立跨案件证据保全表 `mobile_evidence_holds`、举报与安全案件表、运营审计日志表。
6. **`006_v2_reading_publications_and_care_ext.sql`**：建立出版表 (`mobile_publications`、`mobile_publication_revisions`)、共读体系表 (`mobile_reading_items`、`mobile_reading_progress`、`mobile_reading_plans`、`mobile_reading_notes`)、内容订阅表 (`mobile_content_subscriptions`)。
7. **`007_v2_age_assurance_sessions.sql`**：建立外部年龄核验会话跟踪结构与回调审计字段。
8. **`008_v2_guardian_invites_and_adult_claims.sql`**：建立未成年人 15 分钟监护人邀请表 `mobile_guardian_invites` 与独立成年关系申诉表 `mobile_adult_relationship_claims`。
9. **`009_pet_discussions.sql`**：建立宠物与动态嵌套评论表 `mobile_pet_comments` 与评论表情反应表 `mobile_comment_reactions`。

### 4.2 混合关系列 + JSONB 存储模式

所有 `44` 张 `mobile_*` 领域表均遵循**“高频检索与事务不变量字段提升为强类型索引列 + 完整领域聚合根序列化为 `payload JSONB`”**的设计模式：
- 例如 `mobile_lost_reports` 将 `owner_id`、`pet_id`、`status`、`datum` (`GCJ-02`/`WGS-84`)、`coarse_lat`、`coarse_lng` 作为独立索引列，确保数据库层直接执行同坐标系附近矩形范围检索，同时绝不在 JSONB 中落盘高精度原始 GPS。
- 例如 `mobile_adoption_listings` 将 `pet_id`、`status`、`adoption_cycle` 作为索引列，在 PostgreSQL `SERIALIZABLE` 事务中锁定同一 `pet_id` 防止并发产生两个试养流程。

---

## 5. 十二大核心业务与安全链路代码逻辑详解

### 5.1 统一认证、官方三方登录（微信/QQ/Apple）与账号绑定冲突恢复 (`B04` / `F01`)

- **对应 PlantUML 图**：[`docs/uml/05-auth-provider-and-account-linking-sequence.puml`](./uml/05-auth-provider-and-account-linking-sequence.puml)
- **核心源码**：
  - 服务端：[`internal/identity/service.go`](../internal/identity/service.go)、[`internal/platform/mobileauth/tencent_apps.go`](../internal/platform/mobileauth/tencent_apps.go)、[`internal/platform/mobileapi/browser_provider.go`](../internal/platform/mobileapi/browser_provider.go)
  - 客户端：[`kmp/shared/.../ProviderAuthStore.kt`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/ProviderAuthStore.kt)、[`kmp/clientUi/.../ProviderAuthPages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ProviderAuthPages.kt)、[`web-client/src/features/ProviderSignIn.tsx`](../web-client/src/features/ProviderSignIn.tsx)、[`web-client/src/core/provider.ts`](../web-client/src/core/provider.ts)
- **关键代码逻辑与不变量**：
  1. **严禁短信验证码隐式自动注册**：`SignInWithPhoneCode` 校验 `auth_challenges` 通过后，若 `account_identities(provider='phone', subject_hash)` 不存在，立即返回 `404 account_not_found`，要求客户端走显式注册流程。
  2. **Create（登录/注册）与 Link（已登录账号绑定）双轨分离**：
     - 未登录态调用 `POST /api/v2/auth/providers/{provider}/sign-in`（或浏览器回调 `mode=sign_in`）：若三方 `subject_hash` 已绑定则签发该用户会话；若未绑定则在 `SERIALIZABLE` 事务中创建新用户并绑定该三方身份。
     - 已登录态调用 `POST /api/v2/account/identities/{provider}/link`（或浏览器回调 `mode=link`）：**必须携带 `X-Reauth-Token` (`purpose = "account_identity_change"`)**。
  3. **`409 identity_in_use` 冲突保护（绝不静默合并账号）**：
     - 当已登录用户 A 尝试绑定的微信/QQ/Apple 身份已绑定在用户 B 名下时，服务端返回 `409 Conflict` (`code = "identity_in_use"`) 及脱敏冲突提示；客户端 `ProviderAuthStore` 捕获后跳转 `AccountConflict` 页面，引导用户切换登录至原账号解绑后再绑定。

### 5.2 外部年龄核验网关、15 分钟监护人邀请、独立成年关系申诉与未成年人陌生人私信拦截 (`B05` / `F02`)

- **对应 PlantUML 图**：[`docs/uml/06-age-assurance-and-guardian-consent-sequence.puml`](./uml/06-age-assurance-and-guardian-consent-sequence.puml)
- **核心源码**：
  - 服务端：[`internal/identity/age_assurance.go`](../internal/identity/age_assurance.go)、[`internal/identity/guardian_invites.go`](../internal/identity/guardian_invites.go)、[`internal/platform/mobileauth/age_gateway.go`](../internal/platform/mobileauth/age_gateway.go)、[`internal/mobile/guardian_invites.go`](../internal/mobile/guardian_invites.go)、[`internal/mobile/social.go`](../internal/mobile/social.go)
  - 客户端：[`kmp/shared/.../AgeAssuranceStore.kt`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/AgeAssuranceStore.kt)、[`kmp/shared/.../GuardianInviteStore.kt`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/GuardianInviteStore.kt)、[`kmp/clientUi/.../AgePages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AgePages.kt)、[`kmp/clientUi/.../GuardianPages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/GuardianPages.kt)
- **关键代码逻辑与不变量**：
  1. **外部年龄核验网关 (`AgeGateway`)**：
     - 客户端禁止直接提交自称年龄段 (`ageBand`) 来通过核验；必须由 `StartAgeAssurance` 生成带 HMAC-SHA256 签名的外部核验挑战 URL，并由外部网关回调 `POST /api/v2/qualification/age-assurance/callback` 验签后更新 `user_qualification_status.age_band` (`minor_under_16` / `minor_16_17` / `adult_18_plus`)。
  2. **未成年人 15 分钟一次性监护人邀请码**：
     - 未成年人发起 `POST /api/v2/qualification/guardian-invites` 时，服务端自动吊销该未成年人此前未消费的待处理邀请，生成一次性随机邀请码，仅将 `SHA-256(inviteCode)` 存入 `mobile_guardian_invites`（有效期 `15` 分钟）。
  3. **监护人双路径核验与 `reauth` 授权**：
     - **路径 A（直接接受）**：监护人账号必须已具备 `age_band = 'adult_18_plus'` 且 `real_name_verified = true`，且 `guardianUserID != minorUserID`，并通过 `purpose = "guardian_consent_grant"` 的二次认证 (`X-Reauth-Token`) 方可接受邀请，激活 `communityPosting`、`directContact`、`careParticipation` 权限范围。
     - **路径 B（独立成年关系申诉）**：提交证明材料至 `POST /api/v2/qualification/adult-relationship-claims`，由具备 `identity_verifier` 角色的运营人员在 `/app/operator` 审核通过后生效。
  4. **未成年人陌生人私信事务级拦截 (`directContact`)**：
     - 在 `internal/mobile/social.go` 发送私信或创建直接会话时，服务端在事务内检查发送方与接收方的 `age_band` 及互相关注关系：若任一方为未成年人（或年龄未知）且双方**非互相关注好友**，则该未成年人必须拥有处于 `active` 状态且包含 `"directContact"` scope 的 `guardian_consents`，否则直接返回 `403 guardian_consent_required`。

### 5.3 幂等命令账本 (`SERIALIZABLE`)、10 分钟单次 `purpose_reauth`、加密 `CommandVault` 与断网未知结果恢复 (`B07` / `B12`)

- **对应 PlantUML 图**：[`docs/uml/07-idempotent-write-and-sensitive-reauth-sequence.puml`](./uml/07-idempotent-write-and-sensitive-reauth-sequence.puml)
- **核心源码**：
  - 服务端：[`internal/identity/service.go`](../internal/identity/service.go)、[`internal/mobile/commands.go`](../internal/mobile/commands.go)、[`internal/store/postgres/mobile.go`](../internal/store/postgres/mobile.go)
  - 客户端：[`kmp/shared/.../SensitiveActionStore.kt`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/SensitiveActionStore.kt)、[`kmp/desktopApp/.../DesktopStorage.kt`](../kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/DesktopStorage.kt)、[`web-client/src/core/drafts.ts`](../web-client/src/core/drafts.ts)、[`web-client/src/features/Recovery.tsx`](../web-client/src/features/Recovery.tsx)
- **关键代码逻辑与不变量**：
  1. **10 分钟单次有效目的级二次认证 (`purpose_reauth_grants`)**：
     - 每个高风险操作绑定明确的 `purpose`（如 `adoption_application`、`adoption_contract`、`account_identity_change`、`guardian_consent_grant`、`data_erasure`、`account_closure`）。
     - 令牌仅存储 SHA-256 哈希，TTL 为 10 分钟，在业务写事务中原子设置 `consumed_at = NOW()`，单次消费后立即失效。
  2. **PostgreSQL `SERIALIZABLE` 幂等命令账本 (`mobile_command_ledger` / `mobile_operations`)**：
     - 客户端每次写操作生成确定性 `X-Client-Request-Id` 并计算请求体 SHA-256 `payload_hash`。
     - 若同一 `(user_id, request_id)` 重放且 `payload_hash` 一致，服务端直接原样返回账本中已落盘的 `response_status` 与 `response_json`（且无需再次消耗新的 `reauthToken`）；若 `payload_hash` 不一致，返回 `409 idempotency_payload_mismatch`。
  3. **`OperationResultUnknown` 与四端加密 `CommandVault`**：
     - 当网络超时或连接中断导致客户端无法确认服务端事务是否已提交时，`SensitiveActionStore` 将命令标记为 `UNKNOWN_RESULT` 并保留在本地加密金库（Android `EncryptedSharedPreferences`、iOS `Keychain`、Desktop `java-keyring + AES-256-GCM`、Web `IndexedDB + WebCrypto AES-GCM`），同时保护关联的 `LocalPhotoBatchDraft` 本地照片队列不丢失，引导用户在 `UnknownCommandRecovery` 页面执行幂等对账或重放。

### 5.4 `F07` 领养宠物级试养排他锁、协议修订与领养后双向退回状态机

- **对应 PlantUML 图**：[`docs/uml/08-adoption-exclusivity-contract-and-return-state.puml`](./uml/08-adoption-exclusivity-contract-and-return-state.puml)
- **核心源码**：[`internal/mobile/adoption_placement.go`](../internal/mobile/adoption_placement.go)、[`internal/mobile/contracts.go`](../internal/mobile/contracts.go)、[`internal/mobile/rescue.go`](../internal/mobile/rescue.go)、[`kmp/clientUi/.../CarePages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/CarePages.kt)
- **关键代码逻辑与不变量**：
  1. **宠物级试养排他锁 (`pet_id` Exclusivity)**：
     - 在选择试养申请人 (`SelectTrialAdoptionApplicant`) 时，服务端在 `SERIALIZABLE` 事务内校验该 `pet_id` 下不存在任何其他处于 `trial_active` 的申请；一旦锁定成功，同一 `pet_id` 下的其余待处理申请自动转入 `waitlisted`（候补）；若当前试养在完成前取消，候补申请自动恢复为可审核状态。
  2. **领养协议修订号严格对齐 (`rev_no`)**：
     - 每次修改领养协议条款都会生成递增的 `rev_no`，使旧修订版本自动失效；双方签署动作必须显式绑定当前最新 `rev_no` 并携带 `reauthToken`。
  3. **领养完成后双向退回提议与 `adoption_cycle` 周期隔离**：
     - 领养完成后，送养人或领养人均可发起带 72 小时有效期的退回提议 (`ProposeAdoptionReturn`)；提议方可在对方确认前主动撤回，或由 `cmd/worker` 在超过 72 小时后自动过期归档。
     - 当对方确认接受退回时，领养申请转入 `returned_closed`，同时将领养帖的 `adoption_cycle` 递增 (`cycle + 1`)，历史周期的旧申请按周期号自动归档隔离，互不污染。

### 5.5 `F08` 上下文绑定预约准入、双轨改期 (`effectiveVersion` + `pendingProposal`) 与安全冻结

- **对应 PlantUML 图**：[`docs/uml/09-appointment-dual-track-and-safety-hold-state.puml`](./uml/09-appointment-dual-track-and-safety-hold-state.puml)
- **核心源码**：[`internal/mobile/appointments.go`](../internal/mobile/appointments.go)、[`internal/mobile/appointment_admission.go`](../internal/mobile/appointment_admission.go)、[`kmp/clientUi/.../CarePages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/CarePages.kt)
- **关键代码逻辑与不变量**：
  1. **上下文绑定准入 (`Context-Bound Admission`)**：
     - 创建预约 (`POST /api/v2/appointments`) 必须关联一个有效的业务上下文（活跃的领养申请 `adoption_application`、活跃的寻宠认领 `lost_claim`、或有效的服务报价咨询 `offer_inquiry`），严禁无上下文的陌生人冷启动骚扰预约。
  2. **双轨改期状态机 (`effectiveVersion` + `pendingProposal`)**：
     - 预约一旦确认即锁定 `effectiveVersion = v1`。
     - 当任一方发起改期提议时，新方案写入 `pendingProposal (v2)`，而 **`effectiveVersion (v1)` 保持原样生效**。
     - 仅当对方显式接受 `pendingProposal` 时，才执行 `effectiveVersion := pendingProposal; pendingProposal := null`；若提议被撤回、被拒绝或由 `cmd/worker` 超时过期，`pendingProposal` 清空，原 `effectiveVersion (v1)` 丝毫不受影响。
  3. **安全冻结 (`safety_hold`)**：
     - 当关联举报、争议或由 `safety_specialist` 运营触发安全接管时，预约立即进入 `safety_hold` 状态，冻结一切用户侧改期、签到或完成操作，并同步挂载 `mobile_evidence_holds`。

### 5.6 `F09` 寻宠粗粒度坐标隐私 (`GCJ-02`/`WGS-84`)、高德地图代理、同坐标系附近检索与认领交接

- **对应 PlantUML 图**：[`docs/uml/10-lost-pet-amap-and-claim-handover-sequence.puml`](./uml/10-lost-pet-amap-and-claim-handover-sequence.puml)
- **核心源码**：
  - 服务端：[`internal/mobile/rescue.go`](../internal/mobile/rescue.go)、[`internal/mobile/nearby.go`](../internal/mobile/nearby.go)、[`internal/platform/mobileapi/amap.go`](../internal/platform/mobileapi/amap.go)
  - 客户端：[`kmp/shared/.../LostMap.kt`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/LostMap.kt)、[`kmp/clientUi/.../MapPage.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/MapPage.kt)、[`kmp/androidApp/.../AndroidNativeMap.kt`](../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidNativeMap.kt)、[`kmp/iosUi/.../IosNativeMap.kt`](../kmp/iosUi/src/iosMain/kotlin/com/ttkk0000/meowcircle/iosui/IosNativeMap.kt)、[`kmp/iosApp/ComposeApp/AmapNativeMap.swift`](../kmp/iosApp/ComposeApp/AmapNativeMap.swift)、[`web-client/src/maps/AmapView.tsx`](../web-client/src/maps/AmapView.tsx)
- **关键代码逻辑与不变量**：
  1. **高德地图合规隐私同意与安全代理**：
     - Android (`MapsInitializer.updatePrivacyShow/Agree`) 与 iOS (`MAMapView.updatePrivacyShow/Agree`) 在用户未点击同意高德隐私弹窗前，绝不实例化原生地图 SDK。
     - Web 端高德 JS API 2.0 通过服务端 [`/maps/amap/_AMapService/`](../internal/platform/mobileapi/amap.go) 反向代理注入 `jscode` (`MEOW_AMAP_WEB_SECURITY_JS_CODE`)，浏览器源码与网络面板中不暴露安全密钥。
  2. **小数点后 2 位粗粒度坐标量化与坐标系显式隔离 (`GCJ-02` vs `WGS-84`)**：
     - 客户端 `LostMap.kt` 与服务端 `rescue.go` / `nearby.go` 双重强制将经纬度截断/四舍五入至小数点后 `2` 位（约 `1.1 km` 网格精度），并显式携带 `datum` (`"GCJ-02"` 或 `"WGS-84"`)。
     - 附近寻宠检索严格按同 `datum` 过滤，绝不跨坐标系混算距离。
  3. **私密认领审核与双向交接确认**：
     - 拾获人提交线索或认领 (`mobile_lost_claims`) 时，精确联系方式与会面地点默认隐藏；仅当失主审核接受认领进入 `in_handover` 后，双方才解锁上下文绑定预约与私密沟通；最终须失主与拾获人双向确认交接，寻宠报告才转入 `resolved_reunited` 并从公开地图下架。

### 5.7 `F10` 服务与闲置仅咨询边界 (`Inquiry-Only`)、争议介入与跨案件证据保全 (`mobile_evidence_holds`)

- **核心源码**：[`internal/mobile/offers.go`](../internal/mobile/offers.go)、[`internal/mobile/evidence_retention.go`](../internal/mobile/evidence_retention.go)、[`kmp/clientUi/.../OfferPages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/OfferPages.kt)
- **关键代码逻辑与不变量**：
  1. **严禁应用内收银台、托管资金或钱包余额**：`OfferCenter` 与 `mobile_offers` / `mobile_offer_inquiries` 仅提供报价展示、卖家准入审核 (`SellerOnboarding`)、咨询沟通与服务条款确认，不包含任何支付网关或平台抽成字段。
  2. **争议与证据保全联动**：当买卖/服务双方发起争议 (`DisputeCenter`) 或安全举报 (`ReportsSafety`) 时，系统自动或由运营创建证据保全锁；只要目标用户或关联媒体存在任意 `status = 'active'` 的证据锁，任何后台对象清理、数据擦除或账号注销均被强制阻断。

### 5.8 `F11` 全局 `99+` 未读聚合、10 分钟 `InboxReadSnapshot` 快照已读、表情贴纸与 `009` 嵌套讨论

- **对应 PlantUML 图**：[`docs/uml/12-inbox-unread-snapshot-and-chat-sequence.puml`](./uml/12-inbox-unread-snapshot-and-chat-sequence.puml)
- **核心源码**：[`internal/mobile/inbox.go`](../internal/mobile/inbox.go)、[`internal/mobile/inbox_snapshot.go`](../internal/mobile/inbox_snapshot.go)、[`internal/mobile/comment_reactions.go`](../internal/mobile/comment_reactions.go)、[`migrations/009_pet_discussions.sql`](../migrations/009_pet_discussions.sql)、[`kmp/clientUi/.../InboxPages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/InboxPages.kt)、[`kmp/clientUi/.../InboxReadAll.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/InboxReadAll.kt)
- **关键代码逻辑与不变量**：
  1. **全局未读 `99+` 封顶与 10 分钟水位线快照一键已读 (`InboxReadSnapshot`)**：
     - `GET /api/v2/inbox/summary` 汇总私信与通知未读数（超过 99 时 `badgeText = "99+"`），并生成有效期 `10` 分钟的 `InboxReadSnapshot` (`watermark_at`)。
     - 用户点击“一键已读”提交 `snapshotToken` 时，服务端仅将 `created_at <= watermark_at` 的消息与通知标记为已读，水位线之后新到达的实时消息保持未读，彻底杜绝并发漏读；若快照超过 10 分钟则返回 `409 inbox_snapshot_expired`。
  2. **`firstUnread` 锚点、128 个 Noto Unicode 表情与 24 个 `144dp` 贴纸**：
     - 会话详情按用户上次已读序号计算 `firstUnreadMessageId` 供客户端精准定位首条未读分割线；聊天输入栏内置 128 个 Unicode 表情网格与 24 个 M&D 猫狗贴纸（渲染尺寸严格固定为 `144dp`）。
  3. **宠物与动态嵌套评论及表情反应 (`009_pet_discussions.sql`)**：
     - `mobile_pet_comments` 支持对宠物档案或动态条目发表多级回复；`mobile_comment_reactions` 在 `SERIALIZABLE` 事务中原子切换用户对评论的表情反应并实时重算聚合计数。

### 5.9 `F04` / `F05` 共读图书馆/计划/笔记与专栏出版多版本修订审核流

- **核心源码**：
  - 服务端：[`internal/mobile/publication.go`](../internal/mobile/publication.go)、[`internal/mobile/reading.go`](../internal/mobile/reading.go)、[`internal/platform/mobileapi/reading.go`](../internal/platform/mobileapi/reading.go)
  - 客户端：[`kmp/clientUi/.../PublicationsPage.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/PublicationsPage.kt)、[`kmp/clientUi/.../ReadingPages.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ReadingPages.kt)、[`kmp/clientUi/.../ContentSubscriptions.kt`](../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ContentSubscriptions.kt)
- **关键代码逻辑与不变量**：
  1. **专栏出版双版本隔离 (`publication.go`)**：
     - 已上线专栏的线上公开版本 (`published_rev_no`) 与作者正在编辑或提交审核的新修订号 (`draft` / `pending_review` / `changes_requested`) 完全隔离；新修订提交审核或被打回修改期间，已发布的线上版本继续对读者正常展示。
  2. **共读进度、计划与笔记 (`ReadingLibrary` / `ReadingPlans` / `ReadingNotes`)**：
     - 支持按养宠阶段订阅共读清单、打卡更新章节阅读进度、管理周期性阅读计划与公开/私密读书笔记。

### 5.10 `F12` 数据导出/擦除、账号注销冷静期与 `cmd/worker` 六大定时任务

- **对应 PlantUML 图**：[`docs/uml/11-data-lifecycle-erasure-and-worker-jobs-sequence.puml`](./uml/11-data-lifecycle-erasure-and-worker-jobs-sequence.puml)
- **核心源码**：[`internal/mobile/data_requests.go`](../internal/mobile/data_requests.go)、[`internal/mobile/erasure.go`](../internal/mobile/erasure.go)、[`internal/mobile/jobs.go`](../internal/mobile/jobs.go)、[`internal/platform/mobileapi/jobs.go`](../internal/platform/mobileapi/jobs.go)、[`internal/platform/mobileworker/runner.go`](../internal/platform/mobileworker/runner.go)
- **关键代码逻辑与不变量**：
  1. **注销与擦除前置阻断检查**：
     - 用户携带 `reauthToken` 提交数据擦除或账号注销时，服务端实时检查是否存在活跃的证据保全锁 (`evidence_retention.go`) 或未完结的试养/认领义务；若存在则将状态设为 `blocked_by_hold` 并向客户端透明展示 [`DataRequestProgress.kt`](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/DataRequestProgress.kt)。
  2. **`cmd/worker` 六大幂等后台任务 (`mobileworker.Runner`)**：
     - `push`：校验当前会话与权限后派发站内/外部推送。
     - `nearby_alerts`：按 `GCJ-02`/`WGS-84` 同坐标系分批计算附近寻宠提醒。
     - `content_updates`：投递专栏、共读与救助进展订阅更新。
     - `privacy_cleanup`：处理排队中的数据导出打包、数据擦除脱敏、冷静期届满的账号注销及无证据锁媒体物理清理。
     - `provider_revocations`：扫描 `revoke:` 命名空间并重试外部 OAuth/Apple 令牌吊销。
     - `maintenance`：推进 15 分钟监护人邀请、10 分钟收件箱快照、72 小时领养退回提议、超时预约改期提议与过期媒体预留清理。

### 5.11 Web 消费端 (`/app/*`) 与运营工作台 (`/app/operator`) 安全隔离 (`B06` / `B13`)

- **核心源码**：[`web-client/src/operator/OperatorApp.tsx`](../web-client/src/operator/OperatorApp.tsx)、[`internal/platform/mobileapi/ops_auth.go`](../internal/platform/mobileapi/ops_auth.go)、[`internal/platform/mobileapi/ops_context.go`](../internal/platform/mobileapi/ops_context.go)
- **关键代码逻辑与不变量**：
  1. **会话与凭据物理隔离**：
     - 消费端 Web (`/app/*`) 使用 `HttpOnly; SameSite=Lax` Cookie + `X-MND-CSRF` 双重校验。
     - 运营工作台 (`/app/operator`) 仅在内存中保存命名运营人员 (`operatorId` + 角色凭证)，所有 `fetch` 请求强制指定 `credentials: 'omit'`，严禁复用或携带消费端浏览器 Cookie，也严禁在 `localStorage` / `sessionStorage` 落盘运营凭证。
  2. **四大运营最小权限角色 (`RBAC`)**：
     - `ops_reviewer`：常规内容、卖家准入、专栏修订审核。
     - `safety_specialist`：安全举报升级、预约 `safety_hold` 冻结/解冻、跨案件证据保全锁加锁/释放。
     - `privacy_officer`：数据导出/擦除与账号注销队列督办。
     - `identity_verifier`：监护人邀请督办 (`GET /api/v2/ops/guardian-invites`) 与独立成年关系申诉审核 (`POST /api/v2/ops/adult-relationship-claims/{claimId}/review`)。

---

## 6. 静态门禁校验脚本矩阵（12 项全绿基线）

由于开发机严禁运行 `gradle` 或 `go build`/`go test`（见 [`AGENTS.md`](../AGENTS.md)），仓库通过以下 `12` 个 Python 静态分析脚本对 OpenAPI 契约、Go 服务端、KMP 共享层、`kmp/clientUi`、Android/iOS/Desktop 宿主与 Web 客户端进行端到端静态校验：

| 脚本路径 | 校验范围与核心断言 |
| :--- | :--- |
| [`scripts/check_server_shared_contract_static.py`](../scripts/check_server_shared_contract_static.py) | 校验 OpenAPI `241` Paths / `271` Operations / `123` Schemas 与 Go/KMP `74` 对 DTO 字段、枚举、错误码一致性 |
| [`scripts/check_mobile_static.py`](../scripts/check_mobile_static.py) | 校验 Go 服务端 `internal/identity`、`internal/mobile`、`cmd/server`、`cmd/worker` 路由、状态机与 SQL 迁移完整性 |
| [`scripts/check_kmp_client_static.py`](../scripts/check_kmp_client_static.py) | 校验 `kmp/shared` 中 `MviStore`、`MobileStore`、`MobileFeatureStore`、`SensitiveActionStore` 与 API 客户端契约 |
| [`scripts/check_client_ui_static.py`](../scripts/check_client_ui_static.py) | 校验 `kmp/clientUi` `34` 个共享文件、`42` 个 `FeaturePage` 分发、`ClientUiPlatform` 三端实现与 Desktop 钥匙串存储 |
| [`scripts/check_android_ui_static.py`](../scripts/check_android_ui_static.py) | 校验 Android 宿主自适应窗口 (`Compact`/`Medium`/`Expanded`)、MVI 基类桥接与高德地图接入 |
| [`scripts/check_android_feature_static.py`](../scripts/check_android_feature_static.py) | 校验 Android 端 42 个二级功能页、敏感重认证弹窗与本地草稿恢复链路 |
| [`scripts/check_android_finish_static.py`](../scripts/check_android_finish_static.py) | 校验 Android `API 37` 配置、权限声明、微信 `WXEntryActivity` 与安全存储 |
| [`scripts/check_provider_auth_static.py`](../scripts/check_provider_auth_static.py) | 校验微信/QQ/Apple 官方三方认证、浏览器重定向、Create vs Link 分离与 `409 identity_in_use` 冲突页 |
| [`scripts/check_age_assurance_static.py`](../scripts/check_age_assurance_static.py) | 校验外部年龄核验网关 HMAC 挑战/回调、`AgeAssuranceStore` 与 `AgePages` 多端闭环 |
| [`scripts/check_guardian_invites_static.py`](../scripts/check_guardian_invites_static.py) | 校验 15 分钟一次性监护人邀请码、独立成年关系申诉、`identity_verifier` 审核与未成年人私信 `directContact` 拦截 |
| [`scripts/check_web_client_static.py`](../scripts/check_web_client_static.py) | 校验 `web-client` 消费端 SPA、`/app/operator` 隔离、高德地图代理与 `125` 个派生 Schema |
| [`scripts/check_figma_reading_static.py`](../scripts/check_figma_reading_static.py) | 校验共读 (`ReadingLibrary`/`Plans`/`Notes`)、专栏出版 (`Publications`/`Revisions`) 与 `009` 宠物讨论闭环 |
