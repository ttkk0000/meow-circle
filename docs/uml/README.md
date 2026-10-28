# M&D UML与业务图谱

更新：2026-10-27。依据[现行源码基线](../CURRENT_BASELINE.md)重核12张图；纠正旧图中的虚构SQL表、错误认证路由、MVI方法和用途认证时长。PlantUML保留为仓库源文件，FigJam为可编辑阅读副本，Mermaid转换源在[figjam目录](figjam/)。图是选定主路径／类关系，不等于完整业务状态枚举或运行验证。

| 编号 | 图谱 | 格式 | 关键边界 |
|---|---|---|---|
| 01 | [M&D 系统组件与外部能力边界](01-system-component-architecture.puml) | flowchart | 以所列源码为准；图为核心关系与主路径 |
| 02 | [KMP MVI 与共享UI类关系](02-kmp-mvi-and-clientui-class-diagram.puml) | class | 类图由FigJam原生形状与连接线绘制 |
| 03 | [Go 服务、事务端口与适配器类关系](03-server-domain-and-service-class-diagram.puml) | class | 类图由FigJam原生形状与连接线绘制 |
| 04 | [45张mobile聚合表实际Schema与owner关系](04-database-er-schema.puml) | erDiagram | 45张真实聚合表；仅owner关系为SQL外键，JSONB引用另行约束 |
| 05 | [显式登录、三方认证与绑定冲突边界](05-auth-provider-and-account-linking-sequence.puml) | sequenceDiagram | 以所列源码为准；图为核心关系与主路径 |
| 06 | [年龄核验与十五分钟监护邀请](06-age-assurance-and-guardian-consent-sequence.puml) | sequenceDiagram | 以所列源码为准；图为核心关系与主路径 |
| 07 | [五分钟用途认证、幂等写入与未知结果恢复](07-idempotent-write-and-sensitive-reauth-sequence.puml) | sequenceDiagram | 以所列源码为准；图为核心关系与主路径 |
| 08 | [领养、试养、交接与安全接回状态](08-adoption-exclusivity-contract-and-return-state.puml) | stateDiagram-v2 | 以所列源码为准；图为核心关系与主路径 |
| 09 | [预约有效版本、提案与安全暂停](09-appointment-dual-track-and-safety-hold-state.puml) | stateDiagram-v2 | 以所列源码为准；图为核心关系与主路径 |
| 10 | [走失粗位置、私密线索与认领交接](10-lost-pet-amap-and-claim-handover-sequence.puml) | sequenceDiagram | 以所列源码为准；图为核心关系与主路径 |
| 11 | [资料权利、保全、Worker与检查点](11-data-lifecycle-erasure-and-worker-jobs-sequence.puml) | sequenceDiagram | 以所列源码为准；图为核心关系与主路径 |
| 12 | [未读汇总、十分钟确认快照与会话已读](12-inbox-unread-snapshot-and-chat-sequence.puml) | sequenceDiagram | 以所列源码为准；图为核心关系与主路径 |

## 维护与验证

- 图01为部署／组件关系；图02、03为类关系；图04列完整45张mobile表，但不将逻辑引用伪装成外键；图05–12说明关键时序与状态。
- 原PlantUML图中的account_identities、user_sessions、mobile_command_ledger等并非当前V2表；资格与账本实际复用mobile聚合表。当前id为BIGINT，结构化列＋JSONB data，与旧图的TEXT主键／payload列不同。
- 图03的IdentityService／MobileService是区分两个包Service的图内别名；实际Go类型为identity.Service与mobile.Service。
- 用途级reauth有效5分钟；监护邀请15分钟；全部已读确认快照10分钟。三种时限不可混用。
- 预约／领养图保留双版本、同版双确认及权限前提；未画出的拒绝、补充、候补、超期和归档支路查对应源码，不据简图增加状态迁移。
- 本轮检查源码引用、UTF-8、PlantUML起止标记、Mermaid来源和Figma回读；未运行PlantUML/JVM编译、Gradle、Go构建或应用测试。

[Figma同步与远端入口](../figma-sync/README.md)记录各图节点和实际核对结果。
