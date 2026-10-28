# M&D 全栈代码逻辑与UML参考

更新：2026-10-27，源码main@1fe5302。[精确数量及迁移清单](CURRENT_BASELINE.md) · [12张UML](uml/README.md) · [功能状态表](PRODUCT_AND_DELIVERY_MATRIX.md)。本手册重写旧版中无法由源码支持的表名、方法、路由和时限；仅记录源码可确认的机制，不声称完整运行通过。

## 系统层次与端口

| 层 | 实际入口 | 职责与约束 |
|---|---|---|
| 多端共享业务 | [kmp/shared](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/) | 会话、MVI Store、DTO、原请求、草稿与敏感操作恢复 |
| Android / iOS / Desktop公共UI | [kmp/clientUi](../kmp/clientUi/README.md) | Compose页面与组件；平台能力由ClientUiPlatform实现；iOS保留Swift宿主 |
| Web | [web-client](../web-client/README.md) | 独立React DOM；/app消费端、/app/operator业务后台、/app/ops技术运维 |
| HTTP | [mobileapi.Handler](../internal/platform/mobileapi/handler.go) | 认证、路由与错误适配；浏览器Cookie、Origin与X-MND-CSRF；后台具名权限 |
| 身份 | [identity.Service](../internal/identity/service.go) | 显式认证、登录与绑定分离、资格、年龄与监护、会话撤销 |
| 业务 | [mobile.Service](../internal/mobile/service.go) | 无HTTP／SQL／SDK依赖的领域命令；当前身份、父资源、版本、资格与保全 |
| 存储 | [Repository / Tx](../internal/mobile/repository.go)与[pgx适配器](../internal/store/postgres/mobile.go) | 只读REPEATABLE READ，写SERIALIZABLE；白名单Kind映射表；乐观版本与事务回滚 |
| 后台作业 | [cmd/worker](../cmd/worker/main.go)与[jobs](../internal/mobile/jobs.go) | 每进程单个具名作业，默认单批执行；8种合法作业名，游标／租约／检查点与真实回执分开 |

## MVI真实接口

[MviStore.kt](../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MviStore.kt)定义MviStateOwner：state为StateFlow，effects公开为Flow，输入方法accept(intent)，close释放任务。MviStore内部使用MutableSharedFlow（extraBufferCapacity=32）、SupervisorJob、按名称管理的jobs以及reduce／setState／sendEffect；不存在旧文档描述的统一dispatch、handleIntent、launchGuarded接口。

MobileStore承接主壳，MobileFeatureStore承接48个FeaturePage与专业Store组合；SensitiveActionStore、ProviderAuthStore、AgeAssuranceStore、GuardianInviteStore各管对应流程。UI旋转／分屏不应重建业务状态。平台桥接和窗口适配查[客户端规范](client/UI_COMPONENTS.md)与[跨端合同](design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)。

## 实际数据库与引用语义

45张mobile表由007、008动态SQL、009、013构成；另有旧端users等表和010／012技术表。Record包含BIGINT id、owner_id、participants、record_key、reference_id、media_ids、public、state、version、时间及JSONB data。mobile_accounts／identities／sessions／auth_challenges承接身份；mobile_qualifications／guardians承接资格；mobile_operations为幂等操作；mobile_receipts承接收件箱快照；mobile_outbox与deliveries承接投递；mobile_source_declarations承接追加声明。

预约版本与协议／接回修订位于对应聚合data，不存在旧图宣称的独立mobile_adoption_contracts或mobile_inbox_snapshots表。owner_id有SQL外键，其它reference_id／participants／data引用由领域逻辑约束。精确结构以[迁移](../migrations/)为准；图04不伪造数据库约束。

## 关键主路径与不变量

| 图 | 实际入口 | 不变量 |
|---|---|---|
| 05 认证与绑定 | [identity](../internal/identity/service.go)、[email](../internal/identity/email.go)、[browser_provider](../internal/platform/mobileapi/browser_provider.go) | OpenAPI实际为auth/password、auth/phone、auth/email、oauth-start／finish／resolve；禁止短信隐式注册，绑定冲突不静默合并 |
| 06 年龄与监护 | [age_assurance](../internal/identity/age_assurance.go)、[guardian_invites](../internal/identity/guardian_invites.go) | 未知资格关闭；15分钟一次性邀请；当前双方资格与scope重核 |
| 07 敏感写与幂等 | [commands](../internal/mobile/commands.go)、[purpose](../internal/mobile/purpose.go) | auth/reauth为5分钟purpose_grant；保存于mobile_auth_challenges；Idempotency-Key与client_request_id一致，mobile_operations回执不能脱离当前ACL |
| 08 领养与接回 | [rescue](../internal/mobile/rescue.go)、[placement](../internal/mobile/adoption_placement.go)、[contracts](../internal/mobile/contracts.go) | pet_id与adoption_cycle排他；同版协议、试养与实际交接分开；effective接回方案才可交接，双方交接后returned |
| 09 预约双轨 | [appointments](../internal/mobile/appointments.go) | 有效版本与提案分开；历史replaced恢复只产生新提案；同安全暂停版本的新提案双方核对后才解除 |
| 10 走失与私密线索 | [rescue](../internal/mobile/rescue.go)、[lead_submission](../internal/mobile/lead_submission.go)、[lead_review](../internal/mobile/lead_review.go) | 城市／坐标系明确，公开位置量化；线索私密，本人附件与当前父权限重核；收到不等于核实 |
| 11 生命周期与Worker | [data_requests](../internal/mobile/data_requests.go)、[evidence_retention](../internal/mobile/evidence_retention.go)、[jobs](../internal/platform/mobileapi/jobs.go) | 接受请求不等于执行完成；保全阻断清理；外部投递成功须真实回执，检查点不冒充业务完成 |
| 12 未读与快照 | [inbox_snapshot](../internal/mobile/inbox_snapshot.go) | GET汇总不创建快照；显式POST创建10分钟确认范围，确认只推进快照位置，之后新消息仍未读；没有已读撤销合同 |

## 近期补充与权限边界

- [来源声明](server/SOURCE_DECLARATIONS_2026_10_26.md)：独立追加、不覆盖旧声明；发布预览和真实回执保持原请求。
- [私密线索](server/PRIVATE_LEAD_CONTRACT_2026_10_26.md)：Case与Lead版本不可混用；独立会话持续复核当前父权限。
- [10月27日源码批次](client/COMPLETE_LANDING_2026_10_27.md)：业务后台13页、暂停重提、协议内试养、effective接回和私密照护；原回执未知时不自动重新提交。
- [城市目录](client/MAINLAND_CITIES_AND_CAT_ASSETS_2026_10_26.md)：目录与业务准入分开；猫咪配图不冒充实际领养、出售或走失宠物。

## 核对与交付边界

全量740状态、真实设备、数据库迁移、SDK、并发、网络恢复和运营验收仍未完成；完整搜索接收端与运维发布／重启执行器保持用户指定占位。Figma图谱与文档是阅读副本，仓库文本与PlantUML／Mermaid源为可追溯输入。本轮只做文档与非编译静态核对，不执行构建、test、lint、应用或发布。
