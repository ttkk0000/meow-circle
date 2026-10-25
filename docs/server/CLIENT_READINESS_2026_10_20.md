# Android / iOS 接入前服务端核对

> **2026-10-25 当前基线**：OpenAPI 3.1 已扩展至 `241` Paths / `271` Operations / `123` Server Schemas（`74` 对跨语言 DTO），PostgreSQL 迁移覆盖 `001`–`009`（44 张 `mobile_*` 表），新增 Web 浏览器 `Cookie + CSRF` 适配、高德地图安全代理（`/maps/amap/_AMapService/`）、官方三方认证 Create vs Link 分离、外部年龄核验网关、未成年人 15 分钟一次性监护人邀请与独立成年关系申诉、独立阅读与法规目录（`/reading/*`、`/publications/*`）、宠物评论与表情反应、内容更新订阅等接口。全栈代码逻辑与 PlantUML 图谱见[全栈代码逻辑与 PlantUML 手册](../CODE_LOGIC_AND_UML_REFERENCE.md)与[`docs/uml/`](../uml/README.md)；最新服务端状态见[服务端完整性审计（2026-10-24）](SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md)与[现行 API 交接](MOBILE_SERVER.md)。

2026-10-23当前：[服务端优先完善与余量](SERVER_FIRST_2026_10_23.md)及[现行API交接](MOBILE_SERVER.md)替代下文早期接口清单；当前209路径／236操作／100schema仅静态检查通过，仍无编译／运行验收。

2026-10-22后续源码修订见[实施记录](SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)：邮箱与手机号资格、会话epoch、用途grant、操作回执及业务/清理合同已新增，原141路径/恒拒绝属于修订前取样。新增代码与008迁移尚未运行验收；此历史核对正文保留，不作为当前代码接口清单。

2026-10-22补核对：当前OpenAPI仍为141路径／167操作／50schema；B04与客户端恒拒绝仍在。通用reauth只绑定账号／有效期／消费状态，不能替代本次用途／对象／版本确认；旧擦除负载只有topic/user_id，普通PendingEvents按Event schema过滤，不能直接当已具备清理消费者。最新设计到服务端的缺口和阶段见[架构](../bmad/architecture.md)、[准入核对](../bmad/gate-check.md)及[任务B05–B12](../client/TASKS.md)。这是静态证据及技术规划补充，不改写下文2026-10-20范围，也没有重新执行构建／测试／迁移。

日期：2026-10-20。工作树 `J:\GolandProjects\kitty-circle`，分支 `main`，HEAD `ebfc469`，另有大量既有在途修改；本记录核对工作树源码，不代表该提交已包含 v2。

结论：**v2 主业务已有源码，可开展公开浏览与客户端基础接线；完整对外交流尚受 B04 认证合同阻塞，服务端没有完善或上线验收的证据。** 本次没有改写既有服务端代码，也没有使用 v1 或演示数据替代 v2。

## 核对证据

| 范围 | 当前证据 | 接入结论 |
|---|---|---|
| HTTP / 业务分层 | `internal/platform/mobileapi/handler.go` 分派到 `internal/mobile` 与 `internal/identity` | 新客户端只使用 `/api/v2` 的 `ok/data/error` 包装 |
| 会话 | `identity/service.go` 随机会话、令牌散列、刷新轮换、退出；`handler.go` 刷新不要求有效 Authorization | 共享层需串行刷新，不能使用旧 JWT / Settings 中的 v1 token |
| 权限与私密投影 | `mobile/service.go` 的 `canRead`、`project`、`Get`、`ListFiltered`；`mobileapi/media.go` | 宠物健康、精确地点及材料不能靠客户端隐藏；媒体另走有鉴权的请求 |
| 并发与存储 | `store/postgres/mobile.go` Serializable 写事务、版本比较、回滚、SQL 错误映射；`007_mobile_modules.sql` | 409 重新读取并重新确认，不自动重放确认操作 |
| 主功能 | social / rescue / appointments / offers / trust / inbox / nearby 模块及对应 handler | 社区、领养、私信、预约、走失等可按实际 DTO 分批接入 |
| 回归源码 | `mobile/service_test.go`、`identity/service_test.go`、`mobileapi/handler_test.go` | 用例源码存在，未执行，不等于通过 |
| 静态契约检查 | 已审阅并执行 `python scripts/check_mobile_static.py` | 32 张表、167 个操作、50 个 schema、128 个 emoji / 24 张贴纸引用通过；此脚本不启动 Go、网络或数据库 |

CodeGraph 能定位部分当前符号，但不证明索引覆盖所有新增文件；本结论最终以磁盘上的源文件为准。

## 明确未就绪项

0. **本轮发现的最新规则差距（B04）**：[认证规划](../design/MND_MOBILE_AUTH_PLAN.md)已更新为三种唯一标识共用一个账号、普通登录不强制短信、当前手机号验证后才可对外交流。服务端尚缺邮箱标识 / 邮件 OTP、标识迁移与唯一约束、当前手机号验证状态和一致的写命令关卡。`auth/phone` 自动建号及非空旧手机号不能代表完成新规则。新客户端保留已有账号密码登录和只读 / 草稿工作；对外提交暂不放行，不用本地 verified 开关补后端权限。
1. **运行证据缺失**：未执行 Go 编译、测试、001–007 数据库迁移、服务启动和真实接口联调；事务、媒体路径、部署配置仍需实际验证。
2. **真实通道**：短信与微信 / QQ / Apple 适配源码存在，但缺少本次供应商联调证据；客户端必须读取 `auth/capabilities`，不能因为有按钮就宣称可登录。
3. **系统推送**：站内通知与 Outbox 已写；APNs / FCM / 厂商推送适配、设备注册和自动调度部署尚未完成。`queued` 不是送达。
4. **游客安全举报**：`resource` 写操作和 `SubmitReview` 均要求登录；设计的免注册安全 / 隐私报告没有可直接使用的 v2 合同。客户端不能伪报成功或绕过到 ops / v1。
5. **后续 Web 依赖**：Cookie / CSRF、无损 int64 浏览器策略、分享元数据仍需专项处理；不阻塞原生客户端使用 Kotlin Long。
6. **生产数据生命周期**：历史回填、注销保留期限、物理清理、媒体多副本共享与备份配额需运维确认；不要通过开放旧写接口绕过。
7. **发布重试语义**：消息 / 评论 / 上传已有幂等键；`EntryInput` 与 `SaveEntry` 的新建发布没有同类客户端幂等键。请求已写入但响应丢失时，不能默认再次创建；恢复对外发布前需要明确结果不明时的核对或幂等合同。

OpenAPI 的通用 Record / Envelope 不是全部精确响应类型的证明。客户端应按资源解码具体 `data`，未知状态禁止关键操作。

## 当前实施顺序

先落共享 v2 请求、会话及任务状态，再接两端 Compose 宿主、默认浅色和各自平板布局。当前可接真实公开内容与已有账号的读取 / 草稿；B04 冻结并接通后，再放行申请、发布与消息等写流程。供应商 / 推送 / 游客举报保持显式缺口，不用固定成功页抵扣。

未运行编译，遵循全局不自行编译规则。实际测试、数据库、供应商与设备结果均未验证。
