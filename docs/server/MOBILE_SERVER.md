# 手机端对应的 Go 服务接口与运行交接

> **2026-10-27 现行入口**：[源码与文档基线](../CURRENT_BASELINE.md) · [产品／交付状态](../PRODUCT_AND_DELIVERY_MATRIX.md) · [全部文档目录](../DOCUMENTATION_CATALOG.md) · [Figma文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3)。当前253路径／285操作／139服务端schemas、141Web schemas、87对DTO、45张mobile表／13迁移、28份shared／41份公共UI文件、48个FeaturePage；这些是源码口径。本文带日期的旧数量和“待实施”结论仅适用于当时批次，运行／全量验收未完成。

更新：2026-10-25。当前服务端全量代码逻辑与 UML 图谱见[全栈代码逻辑与 PlantUML 手册](../CODE_LOGIC_AND_UML_REFERENCE.md)与[`docs/uml/`](../uml/README.md)；分项交接见[服务端优先完善与作业交接](SERVER_FIRST_2026_10_23.md)、[证据保全／数据清理](LIFECYCLE_RETENTION_2026_10_23.md)、[领养送养闭环与安全保留](ADOPTION_CLOSURE_2026_10_23.md)、[服务端完整性审计与待完善清单（2026-10-24）](SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md)、[三方官方认证](../client/PROVIDER_AUTH_IMPLEMENTATION_2026_10_25.md)、[外部年龄核验网关](../client/AGE_ASSURANCE_IMPLEMENTATION_2026_10_25.md)、[监护人 15 分钟邀请与成年关系申诉](../client/GUARDIAN_PAGES_IMPLEMENTATION_2026_10_25.md)。设计依据：[手机评审入口](../design/figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md)；机器合同：[OpenAPI 3.1（241 Paths / 271 Operations / 123 Schemas）](../api/mobile-v2.openapi.json)。全部运行结果待验收。

当前账号规则：[认证规划](../design/MND_MOBILE_AUTH_PLAN.md)。显式邮箱注册／OTP／密码登录与恢复、标识唯一约束、当前手机号证明、服务端交流／成年／监护关卡、浏览器 `HttpOnly Cookie + X-MND-CSRF` 会话适配已有源码；手机号登录仅允许已有账号，不自动建号。`008` 与 `009` 迁移、真实渠道／策略和运行尚未验收，微信绑定不能代替手机号交流资格。

## 当前源码覆盖

当前未读合同见[全局未读](../design/MND_GLOBAL_UNREAD_SPEC.md)。服务端区分direct／group／message_requests、站内通知分类和业务待处理，返回首条未读消息编号；GET不推进已读。静态实现不抵扣跨设备与平台运行验收。

| 手机端流程 | 服务端实现 |
|---|---|
| 登录注册与账号管理 | 显式邮箱注册／登录／恢复；已有账号手机号登录；微信、QQ、Apple核验及明确关联；用途绑定二次验证、换绑／解绑、会话轮换和设备推送解绑；Web端 `HttpOnly Cookie + X-MND-CSRF` 会话适配；资格与登录分开 |
| 我的与多个宠物 | 资料、公开头像、Honey/Mint/Night/Neutral、Picnic/Desk/Arcade/Garden；多宠物独立档案、宠物评论（`mobile_pet_comments`）、评论表情反应（`mobile_comment_reactions`）与可见范围 |
| 社区与圈子 | 图文、评论、点赞、评论表情反应、内容更新订阅（`content_subscriptions`）、好友请求/接受/拒绝/撤回/删除/拉黑；圈子及日记的 private/public/friends/selected/excluded；解除好友后即时收回圈子访问 |
| 私信与群聊 | 陌生人先请求，接受前一条文字且不开放图片；群目录检索与入群申请（`group_directory`）、群邀请、成员移除/退出、静音、群主转让（`transfer_owner`）接收/拒绝/撤回/过期、单独解散；显式已读与真实未读数 |
| 寻宠与认领 | lost/found 分开，公开坐标粗化，精确地址私有；私密线索与证据；匹配、争议、双方交接、公开结束；独立附近订阅、5–100km、即时/每日摘要 |
| 领养与安全保留 | 申请、补充、拒绝、撤回；宠物级唯一活跃放置（`adoption_placement`）与候选队列（`waitlisted`）、重开周期递增（`adoption_cycle`）；双方确认试养/交接；完成后 30 天私密近况与求助/退养请求；72 小时紧急救助/寄养交接（`safety_hold`）；介绍协作者与照护协作者权限分开 |
| 预约与披露 | V1/V2、有效/待确认版本分开，恢复 V3；版本冲突、取消/过期、历史；城市与物种准入（猫/狗直接开放，异宠须城市准入+资质有效）及医疗免责确认；发布关系/照护/来源/接待/地点/收款责任/必付费用；报价由服务端快照，分项到访反馈 |
| 审核、证据保全与申诉 | 私密材料、待审/补充/结果、到期、原结论关联申诉；跨单据证据保留（`evidence_retention`）冻结关联消息/历史/媒体防止注销清除；审核后台独立凭据；只提供绑定对象版本的分项核验摘要 |
| 公开阅读与法规制度库 | 独立 `/api/v2/reading/*` 与 `/api/v2/publications/*`；按分类、适用范围、阅读模式筛选；带 SHA-256 校验的 Markdown 附件下载；发布态强制校验运营主体三要素 |
| 多图与草稿 | 每个文件独立上传/重试，内容和草稿按有序 ID 最多 9 张；版本控制；私有材料禁止直接挂到公开发布；JPEG/PNG 重新编码移除原 EXIF |
| 表情与通知 | 128 个官方 Noto 表情、24 张现有猫咪贴纸，Unicode 文本/稳定贴纸 ID，收藏/最近使用；站内通知分类/频率/系统权限、独立附近订阅与内容更新订阅 |

上述为源码实现覆盖，不等于已通过编译、联调或生产验收。接入真实供应商、系统推送和历史清理的边界见下文。

## 通用接口约定

前缀 `/api/v2`，JSON 包装为 `{ "ok": true, "data": ... }` 或 `{ "ok": false, "error": "..." }`。与旧 v1 数字 code 包装不同，客户端接入时需按新契约处理。

Bearer 是随机、可撤销的不透明会话，数据库只存散列。访问有效期 1 小时，刷新有效期 30 天；刷新同时轮换两个令牌，旧会话失效。刷新时将 access_token/refresh_token 放请求体，避免把已过期访问令牌作为认证前置条件。退出当前会话或全部会话分别处理；密码找回、换号、解绑等安全变更撤销既有会话。

更新/状态操作提交读取到的 `version`。409 表示需要重新读取并让用户确认，不能用盲目重试覆盖他人变更。媒体使用 `Idempotency-Key` 请求头；消息、评论使用 `idempotency_key` 字段，同一逻辑重试复用原值。点赞提交目标 `liked` 值，重复提交不会反转状态。

列表 `cursor` 是 ID 游标，`limit` 为 1–100，默认 30；`next_cursor=0` 表示没有进一步候选。权限过滤可能导致空页但游标仍推进，应继续请求直到结束。支持 `reference_id`、`owner_id`、`space`、`state`、`q`、`city`、`species`、`latitude/longitude/radius_km`。过滤不是授权，服务端始终重新检查当前权限。ID 为 int64，JS 客户端需保留精度。

GET 不修改已读；通过 `conversations/{id}/read`、`groups/{id}/read` 的 `through_message_id` 以及 `notifications/{id}/read` 明确确认。`inbox-summary` 提供每个会话的未读数、消息请求数和通知分类数。

`GET entry-metrics?ids=...` 最多批量读取 50 条可见动态的点赞/评论总数和当前用户点赞状态；计数来自数据库，删除评论不计入，客户端不应自行猜测数量。`GET users/{id}` 提供公开作者资料，始终隐藏手机号和凭据。

## 主要命令

完整路径、DTO 字段见 OpenAPI；业务校验以 Go 命令为准。

| 路径 | 行为 |
|---|---|
| `GET auth/capabilities` | 返回实际配置的 SMS/登录渠道，iOS 客户端显示 Apple |
| `POST auth/otp` | phone + purpose（login/recover/bind_phone/reauth）；后两种必须登录 |
| `POST auth/phone` | phone/code/device/accept_terms；仅登录已有账号，未知手机号不建号 |
| `POST auth/oauth-start` → `oauth-finish` → `oauth-resolve` | 随机 state/nonce；SDK 回调后服务端核验；未关联时显式创建或先登录已有账号、二次验证后关联；不自动按昵称/邮箱合并 |
| `POST auth/reauth`、`auth/account/{phone,password,unlink,email}` | 短期单次用途凭证绑定会话、动作、对象、版本及范围；注销使用独立data-requests流程 |
| `POST friendships/{request,accept,decline,withdraw,remove,block,unblock}` | 请求体含 user_id/version；关注关系不会自动成为好友 |
| `POST cases/{id}/claims`、`leads`，`claims/{id}/{action}` | 私密认领与线索；match 仅配对，不完成所有权确认；双方 confirm_handover 后结束 |
| `POST adoptions`、`adoptions/{id}/{action}` | pet_id/message；试养、交接、支持/退养分别流转，协作者无决策权限 |
| `POST invitations`、`invitations/{id}/{accept,decline,revoke}` | group_member/adoption_editor/caretaker 三种明确范围；照护邀请由领养人发起 |
| `POST appointments`、`appointments/{id}/{action}` | context_kind 为 adoption/case/visit/offer；只有参与双方可确认；恢复通过 restore_version 建新版本 |
| `POST offers` / `PATCH offers/{id}` | 币种 CNY、金额分；total_cents 必须等于基础价加全部必付项；不提供平台收款 |
| `POST visit_feedback` | identity/pet/reception/place/fees 五项各选 match/mismatch/unknown，关联有效预约版本 |
| `PUT preferences/expressions/drafts/subscriptions` | 对应结构化设置和版本；草稿 media_ids 顺序即展示顺序 |

报价预约的 snapshot 由服务端读取信息版本生成，客户端不能自行注入。非报价场景需要明确填写时间、地点、接待、宠物、全部费用及收款方。精确个人地址不随公开信息返回；只有在参与双方的预约内容中明确共享。示例“橘子/2400 元”是设计示例，不向生产数据库自动写入。

## 媒体与表情

`POST media` 为单文件 multipart：file、scope（默认 private，可明确 public），请求头 `Idempotency-Key`。限制原/处理后各 10 MiB、20MP、JPEG/PNG；最多 9 张由关联命令校验。拒绝视频、任意文件和私有证据误挂公开内容。新媒体放 `data/mobile-media`，不会暴露为静态 `/uploads` 目录。

`GET media/{id}/content` 每次按当前权限检查；解除好友、退群及拉黑即时收回相应读取。响应private/no-store，不返回磁盘文件名。公开头像需明确public上传。私密证据读取使用命名运营凭据`X-Operator-Token`及`evidence_reader`角色，材料到期不因保全自动延长读权限；v2不接受共享AdminKey。

`GET expression-catalog` 返回完整目录，`expression-assets/v1/*` 提供已有素材；保留 Noto 的 NOTICE。表情是普通 Unicode；贴纸是 `sticker-01` 到 `sticker-24`，客户端从 6×4 图集按目录取格。收藏/最近使用记录目录 ID，选择表情不会触发发送；发送仍需显式消息/评论命令。服务端不负责客户端面板的分页和点击交互。

## 配置、迁移与运维

默认按非开发环境校验：DATABASE_URL 必填，JWT_SECRET 与 ADMIN_KEY 各至少 32 字节。配置 PostgreSQL 后连接或 schema 检查失败会停止启动，不降级到内存。Redis 失败可以回到同一 PostgreSQL，不改变数据归属。

配置模板见 [environment.example](environment.example)。服务只读取进程环境，不会自动加载模板或 .env 文件。

本地开发需显式 `APP_ENV=development`；没有 DATABASE_URL 时才使用内存。演示账号/内容仅 `MND_SEED_DEMO=1` 开启。v1 写入与旧 JWT 仅开发 `MND_ENABLE_LEGACY_AUTH=1`；模拟支付仅开发 `MND_ENABLE_MOCK_PAYMENT=1`。生产没有已实现的支付渠道，不注册现有支付桩。

| 环境项 | 用途 |
|---|---|
| DATABASE_URL / REDIS_URL | 主数据和可选旧版公共读缓存；新隐私流程直接查询事务库 |
| JWT_SECRET | 会话相关验证码 HMAC 密钥及开发旧 JWT；不得使用默认值 |
| ADMIN_KEY | 旧后台兼容配置；不授权v2 ops或私密证据读取 |
| SMS_GATEWAY_URL / SMS_GATEWAY_TOKEN | 自有 HTTPS 短信网关；POST phone/code/purpose，需返回 accepted=true 和非空 message_id；没有配置则 503，不输出验证码 |
| WECHAT_LOGIN_APP_ID / WECHAT_LOGIN_APP_SECRET | 微信移动应用服务器核验 |
| QQ_LOGIN_APP_ID / QQ_LOGIN_APP_SECRET | QQ 服务器获取身份并核对应用 ID；支持 SDK access_token 或 server-side code |
| APPLE_LOGIN_CLIENT_ID / APPLE_LOGIN_CLIENT_SECRET | Apple 应用标识和运营方签发、维护有效期的客户端密钥 JWT |
| APPLE_TOKEN_ENCRYPTION_KEY | 独立、稳定的 32 字节密钥的标准 Base64；用于 Apple 撤销令牌加密，不可随普通会话密钥轮换丢弃 |
| CORS_ALLOW_ORIGIN / MND_WEB_ORIGIN | Web来源、浏览器Cookie/CSRF会话源及Idempotency-Key跨域配置；CORS不是ops权限控制 |
| MND_READING_CATALOG | 外部公开阅读与法规制度库 JSON 路径（未设置时使用内嵌 `reading-drafts.json`） |
| EMAIL_GATEWAY_URL / EMAIL_GATEWAY_TOKEN | 真实邮件OTP网关；未配置时关闭相关渠道 |
| MND_OPERATORS_JSON | 命名运营身份、roles与consumers；v2使用X-Operator-Token |
| PUSH_GATEWAY_URL / PUSH_GATEWAY_TOKEN | HTTPS查询／发送网关，稳定键去重；accepted不等于delivered |
| MND_WORKER_* | 独立作业进程的目标、命名凭据、作业、间隔和游标文件；见本批交接 |

迁移需备份后按001–013顺序执行；当前45张mobile聚合表，另有010运维记录及012检查点表，013新增来源声明。docker-compose.yml已挂载至013，PostgreSQL Schema校验要求至013；本轮未执行迁移。旧完整性审计仅作为当时缺口快照。历史导入 `migrations/backfill/legacy_to_mobile.sql` 不是启动步骤；旧图片、关注、订单或支付不转换为新权限、好友或交接证明。

`healthz` 是存活检查；`readyz` 有超时并真正读取事务存储，错误返回 503。新媒体目录需要持久卷、备份与磁盘配额；多副本必须共享媒体存储或替换为对象存储适配器。内存仓库不适合多副本。

`GET ops/outbox`仅供受限事件检查，v2全局ACK已禁止。按消费者使用租约、CAS、重试与独立ACK，或调用`POST ops/jobs/{push,nearby_alerts,content_updates,privacy_cleanup,provider_revocations,maintenance}`实际执行一批。附近提醒保存分批游标；内容更新推送订阅动态/救助进展；供应商撤销使用专用命名空间；隐私清理仅在数据库／物理项和外部回执满足完成条件后ACK。独立`cmd/worker`默认单批，连续模式需明确部署，服务不会隐式启动任务。

真实短信／邮件／OAuth／推送／撤销渠道及调度部署仍需运营配置和运行验收。推送网关适配、独立作业与逐项清理已有源码；本轮未向供应商发送请求。queued是待处理，accepted仅供应商接受；受限备份／保全和人工协助仍需实际履责，不自动判定账号归属、所有权或欺骗。

## 验证边界

已编写事务回滚/版本冲突、好友权限收回、群主转让、预约 V3、认领双方确认、协作者权限、私信请求/已读、私有图片、OTP 锁定/重放、短信失败和会话轮换等回归用例源码。新增 OpenAPI 导出是只读取源码并写文档的静态脚本，不调用 Go、业务接口或数据库。

未运行编译，遵循全局不自行编译规则。实际测试、数据库迁移、服务启动、供应商联调、手机/设备行为均未验证。本轮没有修改客户端、提交或推送代码。

外部协议依据：[Apple 用户核验](https://developer.apple.com/documentation/signinwithapple/verifying-a-user)、[Apple 授权码交换](https://developer.apple.com/documentation/signinwithapplerestapi/generate-and-validate-tokens)、[Apple 撤销](https://developer.apple.com/documentation/signinwithapplerestapi/revoke-tokens)、[QQ Token](https://wiki.connect.qq.com/使用authorization_code获取access_token)、[QQ OpenID](https://wiki.connect.qq.com/获取用户openid_oauth2-0)。微信官方移动应用文档本轮工具无法直接读取，适配器仍须在实际开放平台应用上联调，不能据源码视为验收完成。
