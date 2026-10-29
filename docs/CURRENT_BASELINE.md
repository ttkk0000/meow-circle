# M&D 现行文档与源码基线

更新：2026-10-29（Asia/Shanghai）。已包含邀请协作及后台角色批次，成果随本页所在Git提交保存，尚未推送。Git起点源码：`main@1fe5302e7eb21c5767b84505dea69f47481f8c5f`。本页取代各入口中更早的“当前数量”；带日期交接继续保留当时结果。

[Figma 文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3) · [完整文档目录](DOCUMENTATION_CATALOG.md) · [功能状态与发布边界](PRODUCT_AND_DELIVERY_MATRIX.md) · [UML 图谱](uml/README.md) · [同步清单](figma-sync/README.md)。

## 源码事实

| 维度 | 当前值 | 读取依据 |
|---|---|---|
| OpenAPI Paths / Operations | 255 / 288 | [OpenAPI](api/mobile-v2.openapi.json)，Operations 仅计 HTTP 方法 |
| 服务端 / Web schemas | 141 / 143 | OpenAPI components.schemas / [Web派生](../web-client/src/core/schemas.json) |
| DTO 映射 | 88 对 | [映射脚本](../scripts/check_server_shared_contract_static.py)的 mapping 去重键；不是全部数据类数量 |
| SQL 迁移 | 13 份，001–013 | [migrations](../migrations)，不含 backfill |
| mobile 业务聚合表 | 45 张 | [Kind枚举](../internal/mobile/repository.go)；008通过SQL循环创建10张，不能只数CREATE TABLE字面量 |
| 额外技术表 | mnd_ops_records、mnd_worker_checkpoints | 010 / 012，不计入45张mobile表 |
| shared / clientUi 公共 Kotlin 文件 | 29 / 41 | 各模块 client / clientui 目录直接子文件，包含城市目录派生文件 |
| FeaturePage | 48 个 | MobileFeatureStore.kt枚举；不能当独立Figma状态数量 |
| Worker 接受的作业名 | 8 类 | [ValidJob](../internal/mobile/jobs.go)；不等于真实调度、外部接收端已上线 |
| Figma 消费者状态台账 | 748条＝740既有＋8协作增量 | [实现追踪](client/figma-implementation-trace-2026-10-24.json)；增量手机／Web16根已登记，平板／Desktop待回读；另有角色18稿、业务后台13稿、运维14稿 |

本轮[设计与文档详细完善](client/FIGMA_DOCS_REFINEMENT_2026_10_29.md)仍未提交：后台增加39任务状态及1评审索引，协作8状态完成手机／Web来源补录，消费者追踪748条。消息分类与待办等现有源码差距已明确列账，没有通过文档修订冒充业务接线完成。

当前新增批次见[后台角色实现](client/ADMIN_ROLE_WORKSPACES_2026_10_28.md)；[source-baseline.json](figma-sync/source-baseline.json)保留2026-10-27文档同步时的源码快照。这些是源码统计，不是构建、联调或发布成功证据。

## 当前产品与平台口径

- 品牌为 M&D / meow & doggie；社区与领养为主，猫咪优先，doggie友好，允许的其他伴侣小宠按物种规则开放。出售／有偿转让明确标注性质；不把信息与预约写成支付担保或平台成交。
- 武汉／上海是已确认冷启动试点。大陆城市目录兼容是技术覆盖，31省级、694城市、共745地区项不等于全国业务已开放；行为、物种和城市仍由版本化准入规则决定，未知规则关闭。
- Android / iOS / Desktop 当前共享KMP逻辑及 clientUi Compose 页面，iOS保留Swift宿主与系统能力桥接；Web使用独立React DOM。手机和平板按窗口适配，不由设备名称授予权限。
- 当前Figma V2默认浅色用于新客户端；旧Stitch四主题仅在未迁移范围与历史参考保留。原Figma设计文件不被本轮文档中心取代。
- 登录与互动资格分开，服务端eligibility是权威；年龄未知不可放行。禁止短信隐式注册；三方登录、绑定、冲突恢复分开。微信绑定作为替代发帖门槛仍是未确认建议。
- 全部已读快照有效期10分钟，用于确认快照已知范围；**没有“成功后撤销已读”的接口**。新消息保持未读，已读通知不表示同意业务动作。

## 迁移顺序

| 顺序 | 文件 | 说明 |
|---|---|---|
| 001 | [001_init.sql](../migrations/001_init.sql) | 按SQL源码读取；未执行 |
| 002 | [002_social.sql](../migrations/002_social.sql) | 按SQL源码读取；未执行 |
| 003 | [003_user_phone.sql](../migrations/003_user_phone.sql) | 按SQL源码读取；未执行 |
| 004 | [004_notifications_actor_image.sql](../migrations/004_notifications_actor_image.sql) | 按SQL源码读取；未执行 |
| 005 | [005_adoption.sql](../migrations/005_adoption.sql) | 按SQL源码读取；未执行 |
| 006 | [006_listing_category.sql](../migrations/006_listing_category.sql) | 按SQL源码读取；未执行 |
| 007 | [007_mobile_modules.sql](../migrations/007_mobile_modules.sql) | mobile核心聚合表 |
| 008 | [008_mobile_policy_commands.sql](../migrations/008_mobile_policy_commands.sql) | 策略、资格与幂等聚合表 |
| 009 | [009_pet_discussions.sql](../migrations/009_pet_discussions.sql) | 按SQL源码读取；未执行 |
| 010 | [010_operations_console.sql](../migrations/010_operations_console.sql) | 按SQL源码读取；未执行 |
| 011 | [011_mobile_query_indexes.sql](../migrations/011_mobile_query_indexes.sql) | 按SQL源码读取；未执行 |
| 012 | [012_worker_checkpoints.sql](../migrations/012_worker_checkpoints.sql) | 按SQL源码读取；未执行 |
| 013 | [013_source_declarations.sql](../migrations/013_source_declarations.sql) | 独立来源声明 |

Docker初始化挂载已包含013；已有数据库不会因新增挂载自动升级。[交付手册](client/DELIVERY_GUIDE_2026_10_25.md)中的执行步骤需获得运行授权后再做，本文不执行迁移。

用途级reauth为5分钟单次授权，监护邀请15分钟，全部已读确认快照10分钟；时限按当前identity与inbox_snapshot源码，不能互换。

## 事务与数据口径

`internal/store/postgres/mobile.go`使用pgx：只读事务REPEATABLE READ，写事务SERIALIZABLE。资源共用Record封套，真实列为id、owner_id、participants、record_key、reference_id、media_ids、public、state、version、created_at、updated_at、data。业务结构位于JSONB data；引用ID和数组关系有领域约束，不能把逻辑关系全部画成数据库外键。

资格、监护、幂等、消息快照分别存放于mobile_qualifications、mobile_guardians、mobile_operations、mobile_receipts等聚合。旧UML中的account_identities、user_sessions、mobile_command_ledger、mobile_inbox_snapshots不属于当前V2真实表；已在本轮图谱中纠正。

## 验证层次与剩余工作

1. 文档与设计：本地合同、表格、图谱和Figma阅读副本分别记账；文档同步不改变需求批准状态。
2. 源码与静态：页面和合同源码已分批写入。最近源码批次见[10月27日恢复点](client/COMPLETE_LANDING_2026_10_27.md)及[城市素材交接](client/MAINLAND_CITIES_AND_CAT_ASSETS_2026_10_26.md)。
3. 运行与发布：当前748条消费者状态追踪没有全量逐端运行／视觉验收；SDK正式参数、签名、真实环境、数据库、权限／并发／网络恢复、无障碍与运营值守仍需验证。完整搜索接收端及运维发布／重启执行器保持用户指定占位。

本轮按仓库规则不运行编译、构建、test、lint或应用。历史静态通过、测试源码存在、设计截图均不升级为本轮运行证据。
