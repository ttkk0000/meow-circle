# M&D 系统架构与技术实施合同

> **2026-10-27 现行入口**：[源码与文档基线](../CURRENT_BASELINE.md) · [产品／交付状态](../PRODUCT_AND_DELIVERY_MATRIX.md) · [全部文档目录](../DOCUMENTATION_CATALOG.md) · [Figma文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3)。当前253路径／285操作／139服务端schemas、141Web schemas、87对DTO、45张mobile表／13迁移、28份shared／41份公共UI文件、48个FeaturePage；这些是源码口径。本文带日期的旧数量和“待实施”结论仅适用于当时批次，运行／全量验收未完成。

> **2026-10-25 架构、代码逻辑与 PlantUML 基线更新**：
> - **全栈代码逻辑与 UML 图谱**：已新增[全栈代码逻辑与 PlantUML 架构总览手册](../CODE_LOGIC_AND_UML_REFERENCE.md)及[独立 PlantUML (`.puml`) 图谱目录（12 张图）](../uml/README.md)，完整覆盖系统组件架构、KMP MVI + `clientUi` 类图、Go 服务端领域类图、PostgreSQL `001`–`009` ER 图及 8 条关键业务时序/状态机图。
> - **产品与体验基线**：已同步至 [PRD v2.1（FR-001 至 FR-042，含 EPIC-008 监护/AI标识/双城准入/数据权利/游客安全/证据保全）](prd.md)、[UX v2.1（UX-01 至 UX-17）](ux-design.md)、[首批城市物种准入与四类标准协议模板](../design/MND_CITY_SPECIES_AND_CONTRACT_TEMPLATES.md)。
> - **服务端基线**：OpenAPI 3.1 已扩至 `241` Paths / `271` Operations / `123` Server Schemas（Web 派生 `125` Schemas，`74` 对跨语言 DTO），PostgreSQL 迁移扩至 `001`–`009`（共 `44` 张 `mobile_*` 聚合表），后台 `cmd/worker` 作业扩至 6 类（`push` / `nearby_alerts` / `content_updates` / `privacy_cleanup` / `provider_revocations` / `maintenance`），并落地浏览器 `HttpOnly Cookie + X-MND-CSRF` 会话适配、高德地图安全代理（`/maps/amap/_AMapService/`）、官方三方认证 Create vs Link 分离与冲突恢复（`409 identity_in_use`）、外部年龄核验网关（`age_gateway.go`）、未成年人 15 分钟一次性监护人邀请与独立成年关系申诉（`guardian_invites.go`）及公开阅读/法规目录（`/api/v2/reading/*`、`/api/v2/publications/*`）。
> - **客户端基线**：`kmp/shared/.../client`（`MviStore` 泛型基类 + `MobileStore` / `MobileFeatureStore` / `SensitiveActionStore` / `ProviderAuthStore` / `AgeAssuranceStore` / `GuardianInviteStore`）、`kmp/clientUi`（`34` 个三端共享 Compose UI 文件、`42` 个 `FeaturePage` 路由）、`kmp/androidApp`、`kmp/iosUi` + `kmp/iosApp`、`kmp/desktopApp` 与 `web-client/`（含 `/app/operator` 独立运营台）已完成全量 Figma 状态映射与静态校验（见[全量 Figma 四端实现](../client/ALL_FIGMA_IMPLEMENTATION_2026_10_24.md)）。
> - **待完善项**：PostgreSQL `GIN(media_ids)` 索引、`SERIALIZABLE` 事务内 `allRecords()` 查询收敛及多副本高可用改造详见[服务端完整性审计与待完善清单（2026-10-24）](../server/SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md)。

规划基线：2026-10-22，AI-20261022-TECH-PLAN；当前状态更新：2026-10-23，AI-20261023-CLOSURE-IMPLEMENT。服务端、公共KMP及Android主要合同已有后续源码，不能再把全部路径称为拟议；运行／迁移／供应商尚未验收。当前差距查[功能账本](../client/CURRENT_DELIVERY_STATUS_2026_10_23.md)，新增状态规则查[闭环合同](../design/MND_PRODUCT_CLOSURE_CONTRACT_2026_10_23.md)。本文承接[PRD](prd.md)、[安全合同](../design/MND_SAFETY_AND_LIFECYCLE_SPEC.md)、[操作合同](../design/MND_LOGIC_FLOW_CONTRACT.md)和[法律设计增补](../design/MND_LEGAL_DESIGN_SUPPLEMENT_2026_10_22.md)。[准入核对](gate-check.md)及[追溯JSON](implementation-trace-2026-10-22.json)保留当时规划快照，不作当前完成证明。

## 1. 系统范围与当前证据

目标是让社区、领养、走失互助、出售披露和预约在同一套身份、授权、版本和资料生命周期规则下工作。首发武汉、上海；猫优先、doggie友好。沿用已确认的14岁以上交流、未满14岁有效监护授权、商业发布及线下签约由成年人以本人账号办理。微信绑定替代当前手机号资格仍未获确认，继续执行现行认证合同。

下表为2026-10-22规划前工作树main／ebfc469的历史检查，已被后续批次部分替代，**不是当前缺口清单**。现行资格已由服务端eligibility消费，禁止据旧“恒拒绝”描述重写或退回本地verified。当前工作树main／5de631b仍含大量未提交成果；具体已写／待接／未运行按当前账本和批次证据逐项核对。

| 范围 | 本轮核对事实 | 实施含义 |
|---|---|---|
| v2 API | 141路径、167操作、50个schema；32种mobile聚合 | 保留已有协议；通用Envelope不代表每个业务响应已冻结 |
| 服务端 | identity、mobile、PostgreSQL事务、媒体、Outbox已有源码 | 在已有模块内增量补命令，不重建一套服务 |
| 交流资格 | mobile.active只验证账号存在／active；MobileStore.requireInteractionContract仍恒拒绝 | B04须在服务端每个受限业务命令中执行；客户端不能自行放行 |
| 再验证 | Reauth生成限时、单次、绑定账号的通用grant | 尚不绑定用途、对象、版本和请求摘要；不能抵扣新用途授权 |
| 请求恢复 | 消息／评论／媒体有幂等键，EntryInput新建缺同类合同 | B12补统一请求登记与结果查询，保留既有键名兼容适配 |
| 删除 | 未完群／交接会硬阻断；删除分支先改资料再写account.erasure_requested | B08改为分类处理和可跟踪清理，不能把queued当全部删除 |
| 擦除事件 | 专用擦除负载只有topic/user_id；PendingEvents过滤不符合Event schema的负载 | 普通Outbox轮询不能直接消费该负载；需专用适配／迁移，不得批量ACK跳过 |
| 安全与侵权 | SubmitReview需登录，现有report/appeal无正式通知／声明链 | 游客安全入口与法律通知链分别扩展，不能改名复用错误用途 |
| 媒体 | 服务端解码后重编码JPEG/PNG | 隐私去元数据、AI必要标识与核验原件用途需要独立处理 |
| Android/iOS | shared/client、Android mobile、iosUi、ComposeApp已存在；framework为MndMobileUI | 补首批实现，禁止按旧文档重新创建宿主或回退依赖 |
| 设计 | 最新45状态×5布局225根，原生静态1770次及修正复查52次无剩余发现 | 作为状态和文案依据；不把Figma变量、示例回执或方向按钮写进生产逻辑 |

源码入口：[业务资格](../../internal/mobile/service.go)、[身份与注销](../../internal/identity/service.go)、[Outbox投影](../../internal/mobile/outbox.go)、[举报](../../internal/mobile/trust.go)、[媒体](../../internal/platform/mobileapi/media.go)、[共享Store](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt)。CodeGraph索引155文件，本次结构查询返回旧v1／Expo，未覆盖新增v2与shared/client，故按当前磁盘补核对；未重建索引。

## 2. 架构模式与决定

继续采用Go模块化单体＋PostgreSQL；Redis可选，作为可失效缓存，不保存唯一权限事实。HTTP、业务命令、事务端口、供应商适配保持分层；同一进程可先承载有租约的后台作业，业务增长后再把作业执行器独立部署。首版不增加消息集群、Kubernetes、跨服务RPC或分布式事务。

Android、iOS和Desktop复用KMP业务／数据／会话及状态逻辑；Android Compose、iosUi Compose、Desktop Compose分别负责UI。Web独立React／TypeScript／Vite，复用HTTP契约和验收场景。宿主与布局不进shared。CMP由UIViewController嵌入iOS薄宿主，沿用[官方集成边界](https://kotlinlang.org/docs/multiplatform/compose-swiftui-integration.html)。

| 决定 | 收益 | 代价及复核条件 |
|---|---|---|
| 模块化单体 | 关键跨聚合规则可在一个事务中成立；复用现有实现 | 要限制跨模块任意写表；真实负载／责任边界需要时才拆服务 |
| 单一共享业务层、各端独立UI | 身份、幂等、版本规则一致；保留平台输入与窗口体验 | Web有独立适配；通过同一契约和场景矩阵约束 |
| PostgreSQL持久作业＋Outbox | 业务与事件一起提交，避免立即引入消息集群 | 必须实现claim、租约、重试、去重、失败升级与积压监控 |
| 增量演进v2 | 保留首批客户端和旧只读记录 | 新字段先可兼容读取；破坏性变更必须协调，不能静默改ID或error类型 |
| 受控人工复核 | 先交付有权限、记录和负责人的真实闭环 | 没有值守、适用规则或供应商时相关能力保持不可用 |

## 3. 组件与责任

以下新增类型／文件为规划名称，尚未创建；采用现有包内按功能文件组织，不先拆空Gradle／Go模块。

| 组件 | 现有／拟落点 | 提供与依赖 | 数据／责任及任务 |
|---|---|---|---|
| 身份与准入 | internal/identity；mobile中的命令关卡 | session、eligibility、用途grant；依赖Repository／核验适配器 | 三标识、当前手机、年龄／监护、重核；B04/B05 |
| 请求协议 | mobile命令层＋mobileapi DTO | operation登记、同键重试、结果查询；依赖事务 | 请求摘要、幂等、精确响应／错误码；B12/C03 |
| 媒体与来源 | mobile媒体规则＋mobileapi媒体适配器 | 上传、隐私处理、来源声明与发布核对 | 账号／草稿／媒体指纹／版本；B06/C07/F10 |
| 来源及准入 | mobile核实／宠物／offers | 处分资格、城市物种行为规则；依赖核实、规则版本 | 照护与处分分开，未知类别关闭；B07/F07/F09/F14 |
| 数据权利 | identity＋mobile资料请求命令 | 删除范围预览、最终确认、协助、保留解释、清理作业 | 正式注销与事项协助独立；B08 |
| 安全／法律案件 | mobile trust的明确命令 | 游客安全票据、通知、声明、补充、复核与转送 | 同案但不同用途、私密材料、期限与访问；B02/B09/F13 |
| 合同及生命周期 | mobile appointments/rescue＋独立条款修订 | 双方同版本确认、接回、安全暂停与取消 | 合同、预约、交付、付款分状态；B10/F07/F08/F14 |
| 规则与运营 | 受控ops入口＋规则发布适配器 | 命名操作员权限、版本清单、案件与升级 | 主体／供应商事实、保全、审计／涉税记录；B11 |
| 通知与作业 | mobile inbox/outbox＋受控runner | 站内通知、按消费者去重；供应商由端口接入 | 已读与待处理分开；queued不等于送达；B03/B12/F12 |
| 共享客户端 | 现有shared/client逐功能拆Store/Repository | 精确DTO、会话、CommandState、恢复点 | 无平台UI／导航对象；C03–C07/F00–F14 |
| 平台UI | Android mobile、iosUi、desktopApp、拟web-client | State/Intent及系统端口 | 输入法、焦点、滚动、窗口与安全存储；A/I/D/W |

消费方只能调用明确业务命令，不能通过通用Record／Kind任意写状态。后台审核、涉税、原件访问与作业消费也使用各自最小端口，不把X-Admin-Key放进任何消费者客户端。运营权限必须对应命名操作员、作用域与审计；受控服务凭据与人员会话分离。

### 3.1 服务端关卡

每条受限命令在同一事务中读取账号／会话代次、当前资源、版本、角色、年龄／监护、当前手机及业务资格。按动作决定条件：交流、成年商业行为、资料权利、游客安全和私有草稿分别判断。不要用一个“所有POST必须手机号验证”的中间件挡住隐私求助、监护撤回和资料权利。

普通登录、当前手机资格、敏感操作再验证、成年资格、监护关系和处分权限是不同事实。手机号验证或监护授权不能自动满足成年签约；核验结论应绑定方法、范围、对象和有效性，优先使用必要结果而非全量原件。客户端预检查只用于说明下一步，最终命令始终重新校验。

### 3.2 命令提交与结果不明

客户端保存client_request_id、逻辑命令摘要、原任务和草稿引用。新编辑／新确认生成新请求；同一请求的网络重试复用原ID。服务端按“账号或受控游客主体＋请求ID”唯一，校验用途、对象、版本及正文规范化摘要；同键不同摘要返回明确冲突。

处理顺序：验证当前身份；查询已有operation并按当前权限投影；不存在时校验本次用途grant与资源；在同一事务中登记请求、消费grant、修改聚合、写Outbox并提交。重复请求只返回原有最小回执，不再次执行业务。grant消费与业务失败一起回滚；重试已提交操作不要求再次消费旧grant，也不能因此取得已撤销的私密内容。

operation的pending／committed／rejected描述命令持久化结果；案件的received／reviewing、合同的单方确认、清理的待执行是另一层业务状态。客户端result_unknown描述响应丢失，不由超时改成成功或失败。查询not_found也不自动生成另一请求，因为原请求可能仍在到达／执行。离开页面不撤回命令，取消业务需独立明确命令。

POST不因网络失败天然可重放；这里的去重是应用合同，需要持久化与正文校验。[HTTP语义](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.2.2)也要求谨慎处理非幂等重试。现有消息／评论body中的idempotency_key和上传Header适配到同一规则，不能直接改字段破坏兼容。

### 3.3 用途、版本与后台作业

PurposeGrant绑定actor、session_epoch、purpose、resource_type/id、resource_version、scope_hash、role、expires_at、consumed_operation_id；只存token摘要。普通reauth成功只是核验步骤，正式资料删除／声明／合同确认还需当次范围与版本。关键变化撤销旧确认；同一设备转屏保留草稿，不能保留失效资格。

作业协议记录event_id、consumer、lease_owner、lease_until、attempt、next_retry_at和结果摘要。claim、续租与ACK用版本／租约校验；唯一(event_id, consumer)防重复副作用。外部网络调用在数据库事务之外；超时先核对供应商结果，不能盲发第二次。只有该消费者的处理结果持久化后ACK，失败进入退避和受控失败队列。一个消费者成功不能使其他消费者漏处理。

PostgreSQL SERIALIZABLE仍会发生可识别的序列化冲突，需要应用明确处理；它不能代替幂等或租约。[官方事务说明](https://www.postgresql.org/docs/current/transaction-iso.html)支持此边界。先保留现有409与重新核对语义，只有确认安全、无外部副作用的事务才考虑有界重试；不全局自动重放用户确认。

## 4. 数据模型与迁移

以下是逻辑实体／候选表名，按任务逐批选择扩现有表或建表，不一次创建全部空表；现有users、mobile_*及typed JSONB保留。查询、授权、唯一性与任务扫描字段用关系列；复杂快照使用有版本的明确Go类型，不接受任意JSON消费者写入。

| 实体／候选表 | 核心字段与约束 | 归属 |
|---|---|---|
| account_identifiers | user_id、kind、normalized_lookup、verified_at、状态；类型内有效标识唯一；旧冲突不自动合号 | B04 |
| purpose_grants | actor、session_epoch、purpose、resource/version、scope_hash、expiry、consumed_operation；token摘要唯一 | B05 |
| guardian_authorizations | child、guardian、授权范围／规则版本、方法、有效期、撤回记录；儿童账号不转成人 | B05 |
| operation_requests | actor_scope＋request_id唯一；command、payload_hash、资源引用、持久化状态、最小回执 | B12 |
| media_references及Media来源扩展 | owner／resource/version／media_id／用途；输入与输出指纹、来源评估、适用标识版本；共享物理对象受引用保护 | B06/B08 |
| data_requests | request_kind、subject、scope/version、验证引用、保留项与阶段、受限查询凭据摘要 | B08 |
| legal_cases | 案件类型、原通知关联、目标版本、阶段与独立期限；不把所有求助都当正式通知 | B09 |
| legal_submissions | case、submission_type、actor_role、scope_hash、private_evidence_refs、created_at；追加不可覆盖原文 | B09 |
| contract_revisions | 关系类型、对象、不可变文本／关键事项hash、版本、生效条件与失效原因 | B10 |
| contract_acceptances | revision、party、角色、资格引用、operation；同一方同版本唯一；不能修改旧确认 | B10 |
| policy_versions | 文本／供应商／主体事实、适用范围、审核人、发布时间／生效期及历史 | B11 |
| admission_rules | city、species、action、rule_version、有效期、必要材料与支持状态；未知默认关闭 | B07 |
| outbox_deliveries | event＋consumer唯一、租约、重试、结果与告警；专用擦除负载有明确适配 | B12 |

身份迁移先检测用户名／邮箱／手机号的规范化及历史冲突；保留账号主键。冲突交人工处理，禁止通过新登录自动合并。涉及旧users字段时定义过渡双写与核对，再切唯一读源，不在迁移完成前仅凭非空手机号标已验证。

新DDL采用当前最大编号后的分批扩展，执行前重新核对编号占用，不重写001–007历史迁移；本轮不写或执行SQL。先扩列／表与兼容读取，再回填、核对、切换写入，最后观察后收紧约束。回填保留来源、幂等键和批次，出错可暂停。删除、撤权及已确认条款不可通过回退应用“恢复”。

### 4.1 资料清理与保留

正式注销、选择字段删除、未完事项协助使用不同request_kind。预览返回可清理项、受限保留项、依据／目的、复核节点和处理出口，不用“先办完所有群／领养事项”全量拒绝。DR-HELP只是协助，不产生注销授权。注销前核验及最终确认绑定本次范围；注销后凭独立、可撤销、限范围查询凭据查进度，不能要求重新注册或恢复被撤销的全账号登录。

作业逐项覆盖数据库、媒体引用与派生图、私密缓存、供应商副本和备份恢复约束；只有仍需保留的最小字段进入legal hold，记录依据、责任人、复核与解除。共享文件不能因一个账号请求直接按文件名删除。备份设置到期与删除／保全日志，恢复前重放限制，防止重新公开已删资料。每项有状态和结果证据，不以“事件已排队”显示全部完成。

旧account.erasure_requested必须识别原结构并转换／关联专用请求和作业；普通PendingEvents当前跳过该结构。迁移不可一键ACK该事件，不把缺少消费者当作已处理。保留期、第三方和跨境去向由真实经营事实配置，未核定的高风险功能不放开。

### 4.2 媒体、资格与合同

上传先限额／校验用途并提取必要来源信息，再做隐私转换，生成适用的可见／隐式标识与派生对象；不为了保留AI信息原样公开家庭位置等EXIF。规范字段和编码按实际适用标准核定，不能自造字段后声称符合标准。AI结果不确定时保留待核对，不自动认定欺骗。生成／合成内容不能替代真实宠物或授权核验材料。

发布声明绑定账号、草稿版本和全部媒体指纹；替换／裁剪／重新编码后的输出重新核对绑定。用户声明与平台适用标识分别保存；“未使用AI”不能覆盖已知来源证据。

处分核对按原主授权、拾获照护、依法收容分别处理；争议只暂停送养／出售，保留安全照护与认领。城市规则区分猫犬及领养、出售、交接等行为，不能用“其他宠物”或自动改城绕过。

合同修订与双方确认独立记录；关键费用、对象、接待、地点、健康或责任变化形成新版本。预约effectiveVersion与pendingProposal分开，safety_hold独立停止原安排；安全取消不跳到“新建举报”，安全接回的请求／本方提案／对方回应／交接各自有命令。未回复不接受，一方确认不产生双方生效、交付或付款。先承载已核定的信息、预约及领养／转让确认，不引入未批准的代收、担保或赔付。

## 5. API与客户端合同

下表是规划时的接口责任清单，名称不自动等于实际路径。当前精确路径／DTO以[OpenAPI](../api/mobile-v2.openapi.json)及Go handler为准；身份、资格、用途、操作回执、资料请求和案件等已存在源码，Web Cookie／CSRF、部分全局已读和平台渠道仍需交付。修改前核对当前合同，禁止重复创建已存在接口；新领养状态按闭环合同冻结后同步服务端／shared／Android。

| 合同组 | 已有／拟议入口 | 冻结重点 |
|---|---|---|
| 身份与资格 | 已有auth/password、auth/reauth等；拟GET auth/eligibility、当前手机验证及年龄／监护命令 | account/session_epoch、verified_phone状态与有效期、age_band、guardian_scope；分动作deny_reasons |
| 操作结果 | 拟POST各业务命令携带client_request_id；GET operations/{client_request_id} | 持久化结果、资源版本、同键冲突、查询权限、限时最小回执 |
| 来源与发布 | 拟POST drafts/{id}/ai-declarations；扩展entries发布命令 | draft_version、媒体manifest_hash、声明ID、标识／限制和变更失效 |
| 资格与城市 | 拟pets/{id}/disposition-checks及admission查询 | 城市／物种／行为、规则版本、核对范围；不证明司法权属或健康 |
| 数据权利 | 拟data-requests、/{id}/confirm、/{id}/supplements及结果查询 | erase_fields／close_account／matters_assistance类型不互换；用途grant、保留项、作业明细 |
| 游客安全 | 拟public/safety-reports及受限票据／附件入口 | 无需注册；最小字段、配额、私密结果、按需回联；不受交流手机关卡阻挡 |
| 正式通知链 | 拟rights-notices、/{id}/counterstatements、/{id}/supplements | 正式通知／声明／补充独立用途、必要身份与证据、依法转送及期限；不是普通appeal |
| 条款与生命周期 | 拟contracts、/{id}/acceptances；扩展已有adoption/appointment明确动作 | type、revision/hash、当前主体／角色／资格、双确认和safety_hold |
| 规则及运营 | 拟有效policy／provider清单与admission规则；受控ops案件／任务 | 命名操作员、字段脱敏、双人复核适用项、证据和受控导出 |
| 通知／分享 | 扩现有inbox-summary、notifications；B02公开分享 | 阅读与待处理分开、真实计数、目标权限重读、公开媒体撤回；推送不作内容真相 |

完整URL均在/api/v2下；表中省略此前缀。复用既有手机号OTP、媒体和业务资源时只加明确用途，不在客户端对通用state字段随意PUT。旧auth/account/delete等入口必须路由到新规则或明确关闭相应旧写法，不能留下绕过用途确认的旁路；所有v1写入保持既定关闭策略。

游客可以提交目标描述或链接，即使当前无权读取目标，也不能因此失去安全求助入口；受理不向其披露私密目标信息。限范围票据和附件配额由服务端派生，不能由客户端自报actor_scope；不主动抓取用户任意URL，避免把报告入口变成服务端网络代理。正式通知另按其必要身份和证据合同处理，不推广为所有求助均需实名。

经营／涉税台账区分报价、预约、当事人交易陈述与依法核定的报送事实；不把预约报价自动计作已成交收入。保留及报送范围按实际业务核定，受控导出须有用途、权限与审计，不自动对外发送。

### 5.1 Wire兼容、错误与恢复

沿用ok/data/error包装及现有error文本字段，新增稳定error_code、受控error_details、request_id；新客户端按HTTP＋code处理，不能匹配英文句子。典型码包括PHONE_VERIFICATION_REQUIRED、GUARDIAN_AUTH_REQUIRED、ADULT_REQUIRED、PURPOSE_GRANT_EXPIRED、RESOURCE_CHANGED、AI_SOURCE_REVIEW_REQUIRED、ADMISSION_UNAVAILABLE、IDEMPOTENCY_CONFLICT。具体枚举和状态码在C03/B12冻结，不把这里的草案当已支持。

资源ID、游标、版本保持当前int64契约；Kotlin用Long。Web在JSON文本进入Number前做无损解析，领域／URL中使用十进制字符串；禁止String(JSON.parse后已经舍入的number)。operation请求ID、token等是不同用途的opaque字符串。未来若统一改wire资源ID为字符串，需单独版本／兼容迁移，不在本轮静默改v2。

401串行刷新且最多安全恢复一次；403保留任务说明缺哪项资格；409重读差异后再次确认；429遵守服务端退避；503显示能力不可用。超时与取消保留请求ID，先查结果。未知状态按安全只读／受限处理，不映射成成功。

Web认证由B01冻结HttpOnly/Secure、合适的SameSite、Origin与CSRF防护以及跨标签刷新协调；令牌不进localStorage。原生和Desktop用各自秘密存储。匿名安全票据仅能查本次事项，不能当全账号会话或复用ops凭据。

### 5.2 客户端状态及五布局落地

共享层使用每任务CommandState：editing → validating → submitting → result_unknown／submitted／needs_correction，携带operation、用途、对象及基准版本。提交新请求先清旧结果；服务端committed仅证明相应写入成立，UI再按案件／合同／清理阶段展示。最终同意不预勾，账号、用途、对象、条款或角色变化重新确认。

保留当前client入口，按功能抽Repository/Mapper/Store，不做第一步全仓搬包。恢复记录只持久化必要任务ID、草稿schema和安全媒体引用；不保存OTP、通用grant或身份证正文。退出／切号递增epoch、取消请求、清私密缓存与未发送队列，晚到响应不能写入新账号。

Android平板／iPad以实际窗口和Scene为准；同一任务只拥有一个Store，旋转、分屏及双面板不重复提交／已读。焦点、滚动锚点、IME组合区由各端保存；Figma的“预览横屏／竖屏”控件不进入客户端。分别验证手机、两类平板、浏览器和桌面，不用一个平台结果抵扣另一个。

## 6. 非功能需求映射

下列为架构设计目标和未来验收方法，均未运行。本次NFR-004/005/008部分覆盖，缺真实保留期、恢复目标或运营事实；不得把合计覆盖率当已满足。

| NFR | 具体方案／可衡量条件 | 验证方法 | 规划状态 |
|---|---|---|---|
| 001 性能 | 列表／详情P95≤800ms，不含媒体；试点基准草案100活动会话、20请求/s、读写8:2、1万公开记录／10万消息、30min；5xx<1% | 经授权在明确环境压测，分公开／私有／后台队列，上传独立限流；按实际试点修订负载 | 完整方案，未测 |
| 002 一致性 | operation＋CAS＋事务；同一请求只产生一次业务，旧版本／重复双确认不覆盖 | 并发、响应丢失、重复回调、账号切换、乱序与崩溃场景 | 完整方案，未测 |
| 003 权限 | 每个命令及媒体查询重核角色／范围；后台命名身份；负例无越权，原件访问全留痕 | 直接API跨账号／撤权／过期／儿童／游客／staff权限矩阵 | 完整方案，未测 |
| 004 隐私 | 最小公开投影、受控材料、字段级清理、hold与供应商清单；期限尚待实际核定 | 逐类数据及副本对账、日志泄露检查、删除与留存冲突回放 | 部分 |
| 005 恢复 | 业务快照＋不可变修订＋审计／删除日志＋作业断点；RPO/RTO、备份周期与地点由实际环境确定 | 授权后在隔离环境恢复，对账版本链、媒体引用、删除限制和未完成作业 | 部分 |
| 006 可访问性 | Web WCAG2.2 AA；普通关键文字4.5:1、非文字3:1；Android 48dp／iOS 44pt目标；语义／放大／键盘／减弱动效 | TalkBack、VoiceOver、键盘与对比／缩放逐端验收 | 完整方案，未测 |
| 007 多端 | 同一状态和code、当前默认浅色、字体／Token映射、窗口自适应；不沿用旧四主题选择器 | 相同案例跑五布局／实际平台，检查旋转和恢复 | 完整方案，未测 |
| 008 运营 | 接单、升级、替补、原案处理及可审计记录；普通投诉首响目标≤24h仅在排班成立后对外承诺 | 命名负责人／时段／容量核定，紧急事件独立演练 | 部分 |

可访问性技术依据：[W3C WCAG 2.2](https://www.w3.org/TR/WCAG22/)。原生触控目标来自现行PRD；字体、API依赖钉定继续使用[仓库基线](../client/DEPENDENCY_BASELINE_2026_10_20.md)，本次未重新选型／升级或验证组合兼容性。

## 7. 技术栈与工程边界

| 层 | 方案 | 当前边界 |
|---|---|---|
| HTTP/领域 | 现有Go v2＋identity/mobile | 不以旧v1替代新权限 |
| 持久化 | PostgreSQL、typed JSONB、CAS与事务 | 当前001–007未有本轮运行证据；新增DDL分任务 |
| 缓存／文件 | Redis可选、现有媒体端口；对象存储按实际环境适配 | 私密响应no-store；媒体引用与派生对象受生命周期管理 |
| 作业 | 持久claim/lease＋按消费者投递记录 | 首版可同进程启动，但需明确部署与优雅停止；本轮未启动 |
| 客户端 | 既有KMP与独立Android/iosUi/Desktop；拟Web React/TS/Vite | 不改变当前依赖版本、包名、签名或最低系统 |
| 运行与供应商 | TLS、独立秘密、真实能力配置、备份与可观测性 | 不购买／启用云资源；缺配置的能力false，不回落演示成功 |

## 8. 取舍与范围控制

先交付真实公开读取、合法账号登录和私有草稿，再让一个受限写闭环具备资格、幂等、回执、投诉及删除出口。Android和iOS同行接入同一冻结合同；平板在该功能切片内一起验收。完整Web／Desktop后续接入，公开分享与免注册安全网页提前作为首版必要出口。

R1是否开放某功能取决于该功能的B任务、真实通道、政策、运营及验收，而不是所有F00–F14一齐完成。未开放的群管理、商业或地图能力有明确不可用入口；不能借“暂缓某页面”暴露旧接口绕过规则。完整目标仍在任务表保留，不能为赶首发删掉必要隐私／失败／撤回路径。

不新增支付、托管、担保、赔付、跨城运输或全自动认证。本方案也不把静态设计交互当合法有效签名或监管核验。受控人工台账可以承接审计、税务适用和案件复核，前提是人员、权限、证据、期限与交接真实存在。

## 9. 部署、迁移与验证

逻辑环境分本地开发、隔离集成、邀请试点；凭据、数据库、媒体和日志分别隔离，使用脱敏样例，生产材料不复制入开发。具体地域、资源、备案／许可、供应商、密钥保管与备份参数尚待实际核定，没有在本文假设已经购买或部署。

采用expand → backfill → compare → switch → observe的渐进迁移；先备份并演练，逐功能开关放量。回退只关闭新写入／回到兼容读取，保留operation、审计、删除限制、条款确认与新数据；不回滚这些事实来配合旧二进制。旧网页、Expo和SwiftUI保留必要历史只读，未映射的数据不假冒新业务。

记录request/operation ID、聚合版本、job_id、consumer及脱敏错误码，不记录token／OTP、证件、聊天或家庭正文。监控API失败／拒绝、operation不明时长、Outbox积压与租约过期、删除超期、案件到期、供应商不可用和备份恢复结果；告警对象与值守待B11落实。

验证分五层：文档／源码静态、构建与依赖解析、数据库／服务合同测试、实际供应商与端到端、各平台／无障碍／恢复／性能验收。编写场景与执行场景分开记账；执行后才填通过。迁移和发布另按当次授权，不因测试通过自动部署。

## 10. 后续与开始实施的入口

第一批实施只领取S0的契约盘点及B04/B05/B12基础接口和命令设计，见[阶段表](../client/IMPLEMENTATION_PLAN.md)与[准入核对](gate-check.md)。后续按一条业务切片接服务端、共享逻辑、Android/iOS与平板；用逐项验收证据推动阶段，不设置未经估算的全量截止日期。

扩容先基于实测优化查询和索引、媒体处理及作业；有独立负载／团队责任后才拆媒体／通知执行器，再评估服务边界。不承诺今天的跨聚合事务能零成本拆成微服务。

## 附录：追溯与验证边界

[机器可读追溯](implementation-trace-2026-10-22.json)逐项覆盖PRD34个FR、8个NFR和本轮45组设计／225布局。FR覆盖表示有组件归属及实施方法，NFR覆盖含部分覆盖；均不是源码交付率、运行通过率或法律合规率。

[本轮输入与静态证据](technical-plan-evidence-2026-10-22.json)记录源码发现、当前OpenAPI统计和15个输入指纹，便于后续确认是否基于同一批未提交文件；不是运行日志。

未运行编译，遵循全局不自行编译规则。未运行应用测试、服务、数据库迁移、设备或原型播放；未提交、推送、部署、办理手续或向其他人／会话发消息。bmad/project.yaml当前不存在；沿用项目规则和已确认技术约束，不自行初始化BMAD工程，文档使用中文。
