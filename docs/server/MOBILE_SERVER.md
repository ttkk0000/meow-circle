# 手机端对应的 Go 服务接口与运行交接

更新：2026-10-20。设计依据：[当前手机评审入口](../design/figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md)。架构依据：[模块化单体决策](ARCHITECTURE.md)。机器契约：[OpenAPI 3.1](../api/mobile-v2.openapi.json)。客户端界面未修改。

2026-10-20 账号新决定（待实施）：[认证规划第0节](../design/MND_MOBILE_AUTH_PLAN.md)规定账号名、邮箱、手机号唯一且共用同一user_id，普通登录不强制短信，首次交流前验证当前手机号、换号保留原账号。下表仍为当前源码能力；邮箱验证／登录／恢复、三标识唯一约束与迁移、当前手机号验证记录及统一交流关卡尚未实现，需按客户端任务B04同步接口。现有phone自动建号接口不能被视为该新规则已落地。

## 当前源码覆盖

2026-10-20 UI新增[全局未读合同](../design/MND_GLOBAL_UNREAD_SPEC.md)与[设计交接](../design/figma-study/GLOBAL_UNREAD_HANDOFF_2026_10_20.md)：真实数去重、99+、消息请求未读与待接受分离、静音建议、全部已读含静音的快照及跨设备收敛。现有`inbox-summary`不等于已满足新合同，尤其不能将direct与message_requests无条件重复相加；本任务未改接口或服务端实现。

| 手机端流程 | 服务端实现 |
|---|---|
| 登录注册与账号管理 | 手机验证码合并登录/创建；密码备用/找回；微信、QQ、Apple；取消、失败、首次创建/关联选择、绑定冲突、内部任务恢复；换号、单次二次验证、解绑保护、退出/全部退出、注销处理 |
| 我的与多个宠物 | 资料、公开头像、Honey/Mint/Night/Neutral、Picnic/Desk/Arcade/Garden；多宠物独立档案与可见范围 |
| 社区与圈子 | 图文、评论、点赞、好友请求/接受/拒绝/撤回/删除/拉黑；圈子及日记的 private/public/friends/selected/excluded；解除好友后即时收回圈子访问 |
| 私信与群聊 | 陌生人先请求，接受前一条文字且不开放图片；群邀请、成员移除/退出、静音、群主接收/拒绝/撤回/过期、单独解散；显式已读与真实未读数 |
| 寻宠与认领 | lost/found 分开，公开坐标粗化，精确地址私有；私密线索与证据；匹配、争议、双方交接、公开结束；独立附近订阅、5–100km、即时/每日摘要 |
| 领养 | 申请、补充、拒绝、撤回；双方确认试养/交接；完成后 30 天私密近况与求助/退养请求；介绍协作者与照护协作者权限分开 |
| 预约与披露 | V1/V2、有效/待确认版本分开，恢复 V3；版本冲突、取消/过期、历史；发布关系/照护/来源/接待/地点/收款责任/必付费用；报价由服务端快照，分项到访反馈 |
| 审核与申诉 | 私密材料、待审/补充/结果、到期、原结论关联申诉；审核后台独立凭据；只提供绑定对象版本的分项核验摘要，不能把上传材料当成所有权或健康保证 |
| 多图与草稿 | 每个文件独立上传/重试，内容和草稿按有序 ID 最多 9 张；版本控制；私有材料禁止直接挂到公开发布；JPEG/PNG 重新编码移除原 EXIF |
| 表情与通知 | 128 个官方 Noto 表情、24 张现有猫咪贴纸，Unicode 文本/稳定贴纸 ID，收藏/最近使用；站内通知分类/频率/系统权限、独立附近订阅 |

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
| `POST auth/phone` | phone/code/device/accept_terms；首次创建必须同意条款 |
| `POST auth/oauth-start` → `oauth-finish` → `oauth-resolve` | 随机 state/nonce；SDK 回调后服务端核验；未关联时显式创建或先登录已有账号、二次验证后关联；不自动按昵称/邮箱合并 |
| `POST auth/reauth`、`auth/account/{phone,password,unlink,delete}` | 5 分钟单次凭证，不能只凭已有登录态修改登录方式；仅第三方登录的用户可以通过已绑定第三方再次授权获得二次验证凭证 |
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

`GET media/{id}/content` 每次按所属内容的当前权限检查；好友范围的圈子/日记在解除好友后收回访问，协作材料和群图片分别检查当前授权及成员资格。已经接受的私信会话独立于圈子权限，拉黑后禁止访问。响应 private/no-store，返回值不包含磁盘文件名。公开头像必须主动选择 public 上传。审核员使用独立 X-Admin-Key 只能读取有关审核且未到期的材料。

`GET expression-catalog` 返回完整目录，`expression-assets/v1/*` 提供已有素材；保留 Noto 的 NOTICE。表情是普通 Unicode；贴纸是 `sticker-01` 到 `sticker-24`，客户端从 6×4 图集按目录取格。收藏/最近使用记录目录 ID，选择表情不会触发发送；发送仍需显式消息/评论命令。服务端不负责客户端面板的分页和点击交互。

## 配置、迁移与运维

默认按非开发环境校验：DATABASE_URL 必填，JWT_SECRET 与 ADMIN_KEY 各至少 32 字节。配置 PostgreSQL 后连接或 schema 检查失败会停止启动，不降级到内存。Redis 失败可以回到同一 PostgreSQL，不改变数据归属。

配置模板见 [environment.example](environment.example)。服务只读取进程环境，不会自动加载模板或 .env 文件。

本地开发需显式 `APP_ENV=development`；没有 DATABASE_URL 时才使用内存。演示账号/内容仅 `MND_SEED_DEMO=1` 开启。v1 写入与旧 JWT 仅开发 `MND_ENABLE_LEGACY_AUTH=1`；模拟支付仅开发 `MND_ENABLE_MOCK_PAYMENT=1`。生产没有已实现的支付渠道，不注册现有支付桩。

| 环境项 | 用途 |
|---|---|
| DATABASE_URL / REDIS_URL | 主数据和可选旧版公共读缓存；新隐私流程直接查询事务库 |
| JWT_SECRET | 会话相关验证码 HMAC 密钥及开发旧 JWT；不得使用默认值 |
| ADMIN_KEY | 受控后台/运维入口，生产需由网关网络策略进一步限制；当前为单操作员凭据，尚非多角色后台系统 |
| SMS_GATEWAY_URL / SMS_GATEWAY_TOKEN | 自有 HTTPS 短信网关；POST phone/code/purpose，需返回 accepted=true 和非空 message_id；没有配置则 503，不输出验证码 |
| WECHAT_LOGIN_APP_ID / WECHAT_LOGIN_APP_SECRET | 微信移动应用服务器核验 |
| QQ_LOGIN_APP_ID / QQ_LOGIN_APP_SECRET | QQ 服务器获取身份并核对应用 ID；支持 SDK access_token 或 server-side code |
| APPLE_LOGIN_CLIENT_ID / APPLE_LOGIN_CLIENT_SECRET | Apple 应用标识和运营方签发、维护有效期的客户端密钥 JWT |
| APPLE_TOKEN_ENCRYPTION_KEY | 独立、稳定的 32 字节密钥的标准 Base64；用于 Apple 撤销令牌加密，不可随普通会话密钥轮换丢弃 |
| CORS_ALLOW_ORIGIN | Web 客户端来源；允许 Idempotency-Key；运维接口不依靠 CORS 作为权限 |

迁移先备份，再按序应用 001–007。新 Docker 数据卷会执行挂载脚本；已有数据卷不会因为修改 compose 自动补迁移。可选历史导入见 `migrations/backfill/legacy_to_mobile.sql`，不是自动启动步骤。旧图片、关注、订单和支付结果不自动变成新权限/好友/交接证明。生产切换客户端前先确认历史数据处理与 v1 写接口关闭的影响。

`healthz` 是存活检查；`readyz` 有超时并真正读取事务存储，错误返回 503。新媒体目录需要持久卷、备份与磁盘配额；多副本必须共享媒体存储或替换为对象存储适配器。内存仓库不适合多副本。

`GET ops/outbox?cursor=...` 获取待处理事件，成功处理后 `POST ops/outbox/{id}/ack`；未处理不要 ACK。本地调度器可用 `case.saved` 调用 `POST ops/nearby-alerts`，按订阅游标每批 100 处理，重试不重复提醒；关闭信息不会再生成新提醒。`POST ops/provider-revocations` 轮询 Apple 撤销作业，失败保留待处理，分页扫描后从 0 开始下一轮，以重试失败项。保护所有 ops 凭据与网络入口。

尚未提供真实供应商账号或设备推送凭据，本轮没有实际发送 SMS、登录第三方或推送。站内通知与调度入口已写好，APNs/FCM/厂商推送适配器、自动调度部署、注销保留期限和历史数据物理清理策略仍需接入与验收；`queued` 表示待处理，绝不是已送达。恢复/退养/举报中需要人工核验的事项进入受控记录，不自动变更账号归属或判定欺骗。

## 验证边界

已编写事务回滚/版本冲突、好友权限收回、群主转让、预约 V3、认领双方确认、协作者权限、私信请求/已读、私有图片、OTP 锁定/重放、短信失败和会话轮换等回归用例源码。新增 OpenAPI 导出是只读取源码并写文档的静态脚本，不调用 Go、业务接口或数据库。

未运行编译，遵循全局不自行编译规则。实际测试、数据库迁移、服务启动、供应商联调、手机/设备行为均未验证。本轮没有修改客户端、提交或推送代码。

外部协议依据：[Apple 用户核验](https://developer.apple.com/documentation/signinwithapple/verifying-a-user)、[Apple 授权码交换](https://developer.apple.com/documentation/signinwithapplerestapi/generate-and-validate-tokens)、[Apple 撤销](https://developer.apple.com/documentation/signinwithapplerestapi/revoke-tokens)、[QQ Token](https://wiki.connect.qq.com/使用authorization_code获取access_token)、[QQ OpenID](https://wiki.connect.qq.com/获取用户openid_oauth2-0)。微信官方移动应用文档本轮工具无法直接读取，适配器仍须在实际开放平台应用上联调，不能据源码视为验收完成。
