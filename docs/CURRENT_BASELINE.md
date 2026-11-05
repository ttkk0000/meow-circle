# M&D 现行文档与源码基线

更新：2026-11-05（Asia/Shanghai）。本轮fetch及ff-only同步核对：main与origin/main均为48a3cfd，无新增提交；继续全部设计实现的批次005–020仍在未提交工作树。本轮仅核对文档，未恢复业务开发；任务未全部完成。本页取代更早入口数量，带日期交接保留当时结果，详见[本轮文档核对](DOCUMENTATION_REVIEW_2026_11_05.md)。

[Figma 文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3) · [完整文档目录](DOCUMENTATION_CATALOG.md) · [功能状态与发布边界](PRODUCT_AND_DELIVERY_MATRIX.md) · [UML 图谱](uml/README.md) · [同步清单](figma-sync/README.md)。

## 源码事实

2026-11-05远端设计对账补充：Figma已有10月30日通知事件／评论定位评审及文档增量，宣称145／147 schemas和90对DTO；当前工作树实际仍为下表141／143、88对，缺该批4份来源文档及4个通知schema。详见[核对与来源](design/figma-study/audits/2026-11-05-remote-check/REVIEW.md)；本地数量不按远端正文直接抬升，原未提交认证成果保留，通知批次待找回对账。

当前进度：[严格按Figma分批实施](client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)已记二十批；最新旧号／新号及手机号补充验证。33shared／53公共UI、100Web／457本地引用通过；019四语法通过，020初五完整后最终五份受内存门槛保护待读，旧015–017待读由018补齐。未类型／编译／测试／运行／全量视觉，任务未完成。

| 维度 | 当前值 | 读取依据 |
|---|---|---|
| OpenAPI Paths / Operations | 255 / 288 | [OpenAPI](api/mobile-v2.openapi.json)，Operations 仅计 HTTP 方法 |
| 服务端 / Web schemas | 141 / 143 | OpenAPI components.schemas / [Web派生](../web-client/src/core/schemas.json) |
| DTO 映射 | 88 对 | [映射脚本](../scripts/check_server_shared_contract_static.py)的 mapping 去重键；不是全部数据类数量 |
| SQL 迁移 | 13 份，001–013 | [migrations](../migrations)，不含 backfill |
| mobile 业务聚合表 | 45 张 | [Kind枚举](../internal/mobile/repository.go)；008通过SQL循环创建10张，不能只数CREATE TABLE字面量 |
| 额外技术表 | mnd_ops_records、mnd_worker_checkpoints | 010 / 012，不计入45张mobile表 |
| shared / clientUi 公共 Kotlin 文件 | 33 / 53 | 各模块 client / clientui 目录直接子文件，包含城市目录派生文件；新增账号／安全与协议说明专用页面 |
| Web TypeScript / TSX 文件 | 100 | web-client/src 递归统计.ts及.tsx；457本地引用为批次020历史静态记录，本轮未重跑该脚本 |
| FeaturePage | 48 个 | MobileFeatureStore.kt枚举；不能当独立Figma状态数量 |
| Worker 接受的作业名 | 8 类 | [ValidJob](../internal/mobile/jobs.go)；不等于真实调度、外部接收端已上线 |
| Figma 消费者状态台账 | 748条＝740既有＋8协作增量 | [实现追踪](client/figma-implementation-trace-2026-10-24.json)；增量五布局40根已完成[最终细化、96动作回读与五布局截图](design/figma-study/collaboration-refinement-2026-10-29/README.md)，全量748状态逐端视觉／运行仍待；另有角色18稿、业务后台13稿、运维14稿 |

2026-10-29的[设计与文档详细完善](client/FIGMA_DOCS_REFINEMENT_2026_10_29.md)记录后台增加39任务状态及1评审索引、协作8状态手机／Web来源补录，消费者追踪748条。该批次列出的消息分类／待办及后台未知结果等差距，后续源码与静态交付见[拉取后代码续作](client/REMOTE_CONTINUATION_2026_10_29.md)；全量逐端运行仍未验收，不能继续把当时“业务源码未改”当作当前结论。

当前新增批次见[设计与代码续作](client/CONTINUE_DESIGN_CODE_2026_10_28.md)、[后台角色实现](client/ADMIN_ROLE_WORKSPACES_2026_10_28.md)；五布局40份协作稿独立记账，2026-10-29已通过正常工具完成原五项收尾，审批额度问题保留为历史。本轮还补10照片返回、15结果返回与16平板同状态切换，96动作独立回读通过，五张完整稿图已查看，未新增消费者状态或业务源码。[source-baseline.json](figma-sync/source-baseline.json)保留2026-10-27文档同步时的源码快照。这些事实不是构建、联调或发布成功证据。

2026-10-28–29续作已补邀请详情／限定事项列表、退出／撤销全页、失权／410清理与Web试养／接回／预约完整核对；[本批13旧＋8新状态源码分支](client/collaboration-page-source-map-2026-10-28.json)单列，不将748条索引视为整体完成。当时Web90份TS/TSX／407本地引用及Node18语法读取30份（24完整无诊断＋6基线对照／片段）为历史检查点，当前文件数量与最新批次020的待读边界以上表及分批进度为准；Node24失败仍留历史，未运行验收。

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
2. 源码与静态：页面和合同源码已分批写入。最新见[批次001–020进度及恢复点](client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)；[10月27日恢复点](client/COMPLETE_LANDING_2026_10_27.md)与[城市素材交接](client/MAINLAND_CITIES_AND_CAT_ASSETS_2026_10_26.md)保留对应历史批次。
3. 运行与发布：当前748条消费者状态追踪没有全量逐端运行／视觉验收；SDK正式参数、签名、真实环境、数据库、权限／并发／网络恢复、无障碍与运营值守仍需验证。完整搜索接收端及运维发布／重启执行器保持用户指定占位。

本轮按仓库规则不运行编译、构建、test、lint或应用。历史静态通过、测试源码存在、设计截图均不升级为本轮运行证据。
