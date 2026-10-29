# M&D 全仓文档目录

更新：2026-10-29。原175份Markdown目录为10月27日同步基线，下列增量补录10月28–29日设计／源码交接；覆盖仓库已跟踪Markdown及本轮新增文档；未归属skills目录未纳入。目录逐项记录用途与历史范围，不以文件存在认定内容对应功能已运行。当前事实先读[源码基线](CURRENT_BASELINE.md)，功能与未完成项读[交付矩阵](PRODUCT_AND_DELIVERY_MATRIX.md)，Figma副本见[同步入口](figma-sync/README.md)。


## 2026-10-28–29 当前增量

| 文档 | 说明 |
|---|---|
| [后台按角色工作区详细设计](design/ADMIN_ROLE_WORKSPACES_2026_10_28.md) | 角色与权限现行规范，10月29日交互补充另记 |
| [后台角色源码与验证交付](client/ADMIN_ROLE_WORKSPACES_2026_10_28.md) | 带日期源码／静态交付，未运行 |
| [邀请协作源码检查点](client/BUSINESS_COLLABORATION_2026_10_28.md) | 带日期源码／静态交付，未运行 |
| [后台18稿原始交接](design/figma-study/admin-roles-2026-10-28/README.md) | 10月28日基线，后续修订单独记账 |
| [后台任务交互合同](design/ADMIN_TASK_INTERACTION_2026_10_29.md) | 字段／确认／三结果及失权，设计与接线分层 |
| [消息事件分类与待办对齐](design/NOTIFICATION_EVENT_ALIGNMENT_2026_10_29.md) | 当前实现差距与目标合同，业务源码未改 |
| [协作退出与有限授权补录](design/figma-study/collaboration-2026-10-29/README.md) | 8状态手机／Web16根已记，其余布局待回读 |
| [后台任务稿与验证证据](design/figma-study/admin-task-interaction-2026-10-29/README.md) | 39任务状态＋1评审索引，非运行验收 |
| [Figma与文档详细完善交付](client/FIGMA_DOCS_REFINEMENT_2026_10_29.md) | 本轮结果与剩余接线／运行边界 |
| [Figma文档中心增量同步](figma-sync/delta-2026-10-29/README.md) | 实际写入／分页回读单列，旧manifest保留 |
| [10月28日完整性复审](design/figma-study/audits/2026-10-28-completeness/REVIEW.md) | 原6项发现及当轮截图，后续修订不改旧结论 |

## 00 导航与现行基线

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [M&D 现行文档与源码基线](CURRENT_BASELINE.md) | `docs/CURRENT_BASELINE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 全仓文档目录](DOCUMENTATION_CATALOG.md) | `docs/DOCUMENTATION_CATALOG.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 产品、源码与验收状态矩阵](PRODUCT_AND_DELIVERY_MATRIX.md) | `docs/PRODUCT_AND_DELIVERY_MATRIX.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 文档入口](README.md) | `docs/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Figma文档与图谱同步](figma-sync/README.md) | `docs/figma-sync/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |

## 01 产品与UX

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [M&D 产品、业务场景与编码实施闭环复审](bmad/PRODUCT_CLOSURE_AUDIT_2026_10_23.md) | `docs/bmad/PRODUCT_CLOSURE_AUDIT_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [M&D 系统架构与技术实施合同](bmad/architecture.md) | `docs/bmad/architecture.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 技术实施准入核对](bmad/gate-check.md) | `docs/bmad/gate-check.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 社区、领养与可信找猫 PRD](bmad/prd.md) | `docs/bmad/prd.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 中国市场策略与产品判断](bmad/research-report.md) | `docs/bmad/research-report.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 社区、领养与可信找猫 UX 规格](bmad/ux-design.md) | `docs/bmad/ux-design.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |

## 02 设计规范与合同

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [M&D 法律与合规补充复审](design/LEGAL_COMPLIANCE_REVIEW_2026_10_22.md) | `docs/design/LEGAL_COMPLIANCE_REVIEW_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [M&D 武汉／上海双城分物种准入、四方标准协议与运营 SOP 草案](design/MND_CITY_SPECIES_AND_CONTRACT_TEMPLATES.md) | `docs/design/MND_CITY_SPECIES_AND_CONTRACT_TEMPLATES.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 客户端交互交接索引](design/MND_CLIENT_INTERACTION_SPEC.md) | `docs/design/MND_CLIENT_INTERACTION_SPEC.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 冷启动、走失互助与登录规则增补](design/MND_COLD_START_LOST_PETS_AND_AUTH_SPEC.md) | `docs/design/MND_COLD_START_LOST_PETS_AND_AUTH_SPEC.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Design Memory](design/MND_DESIGN_MEMORY.md) | `docs/design/MND_DESIGN_MEMORY.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 设计交接索引](design/MND_FIGMA_DESIGN_DOCUMENT.md) | `docs/design/MND_FIGMA_DESIGN_DOCUMENT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 新视觉与 Figma 学习稿](design/MND_FIGMA_STUDY_BRIEF.md) | `docs/design/MND_FIGMA_STUDY_BRIEF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 全局未读与消息中心设计](design/MND_GLOBAL_UNREAD_SPEC.md) | `docs/design/MND_GLOBAL_UNREAD_SPEC.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 法律复审对应设计增补](design/MND_LEGAL_DESIGN_SUPPLEMENT_2026_10_22.md) | `docs/design/MND_LEGAL_DESIGN_SUPPLEMENT_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [M&D 六处操作与状态合同](design/MND_LOGIC_FLOW_CONTRACT.md) | `docs/design/MND_LOGIC_FLOW_CONTRACT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 手机登录、注册与快捷登录规划](design/MND_MOBILE_AUTH_PLAN.md) | `docs/design/MND_MOBILE_AUTH_PLAN.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 业务闭环与交互补全合同](design/MND_PRODUCT_CLOSURE_CONTRACT_2026_10_23.md) | `docs/design/MND_PRODUCT_CLOSURE_CONTRACT_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [M&D 2026-08 产品设计与 UX / 无障碍历史审计](design/MND_PRODUCT_DESIGN_SPEC_AND_AUDIT.md) | `docs/design/MND_PRODUCT_DESIGN_SPEC_AND_AUDIT.md` | 带日期／历史证据，按原日期解释 |
| [M&D 安全、年龄与业务生命周期合同](design/MND_SAFETY_AND_LIFECYCLE_SPEC.md) | `docs/design/MND_SAFETY_AND_LIFECYCLE_SPEC.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Stitch AI Rules](design/MND_STITCH_AI_RULES.md) | `docs/design/MND_STITCH_AI_RULES.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 真实性、披露与预约规范](design/MND_TRUST_AND_DISCLOSURE_SPEC.md) | `docs/design/MND_TRUST_AND_DISCLOSURE_SPEC.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D UI Alignment Guide](design/MND_UI_ALIGNMENT_GUIDE.md) | `docs/design/MND_UI_ALIGNMENT_GUIDE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 产品、合规与 Figma 审查](design/PRODUCT_COMPLIANCE_FIGMA_AUDIT_2026_10_21.md) | `docs/design/PRODUCT_COMPLIANCE_FIGMA_AUDIT_2026_10_21.md` | 带日期／历史证据，按原日期解释 |
| [M&D 项目概览幻灯片内容 (Figma Slides Deck)](design/PROJECT_OVERVIEW_DECK_CONTENT.md) | `docs/design/PROJECT_OVERVIEW_DECK_CONTENT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 敏捷看板与排期大纲 (FigJam Sprint Board)](design/SPRINT_PLANNING_BOARD.md) | `docs/design/SPRINT_PLANNING_BOARD.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |

## 03 客户端与交付

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [部署与App签名占位配置](../deploy/examples/README.md) | `deploy/examples/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Codex / Gemini 客户端协作交接](client/AGENT_HANDOFF.md) | `docs/client/AGENT_HANDOFF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [年龄与成年资格页面及接口（2026-10-25）](client/AGE_ASSURANCE_IMPLEMENTATION_2026_10_25.md) | `docs/client/AGE_ASSURANCE_IMPLEMENTATION_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [全端公共页面实施进展（2026-10-24）](client/ALL_FIGMA_IMPLEMENTATION_2026_10_24.md) | `docs/client/ALL_FIGMA_IMPLEMENTATION_2026_10_24.md` | 带日期／历史证据，按原日期解释 |
| [高德原生与 Web 地图源码接入](client/AMAP_IMPLEMENTATION_2026_10_24.md) | `docs/client/AMAP_IMPLEMENTATION_2026_10_24.md` | 带日期／历史证据，按原日期解释 |
| [高德寻宠地图接入与成本评估](client/AMAP_LOST_PET_ASSESSMENT_2026_10_24.md) | `docs/client/AMAP_LOST_PET_ASSESSMENT_2026_10_24.md` | 带日期／历史证据，按原日期解释 |
| [Android 手机与平板功能续作](client/ANDROID_FEATURES_2026_10_22.md) | `docs/client/ANDROID_FEATURES_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [安卓手机与平板页面补齐](client/ANDROID_PAGES_2026_10_23.md) | `docs/client/ANDROID_PAGES_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [Android 手机／平板读取状态补全 · 2026-10-23](client/ANDROID_READ_STATES_2026_10_23.md) | `docs/client/ANDROID_READ_STATES_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [Android 手机／平板与服务端闭环续作](client/ANDROID_REMAINING_2026_10_23.md) | `docs/client/ANDROID_REMAINING_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [全量落地任务：2026-10-27 执行恢复点](client/COMPLETE_LANDING_2026_10_27.md) | `docs/client/COMPLETE_LANDING_2026_10_27.md` | 带日期／历史证据，按原日期解释 |
| [M&D 当前功能交付账本](client/CURRENT_DELIVERY_STATUS_2026_10_23.md) | `docs/client/CURRENT_DELIVERY_STATUS_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [部署、打包与安装运行入口（2026-10-25）](client/DELIVERY_GUIDE_2026_10_25.md) | `docs/client/DELIVERY_GUIDE_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [KMP 客户端依赖升级基线](client/DEPENDENCY_BASELINE_2026_10_20.md) | `docs/client/DEPENDENCY_BASELINE_2026_10_20.md` | 带日期／历史证据，按原日期解释 |
| [默认主题、独立平板适配与验收](client/DESIGN_AND_ACCEPTANCE.md) | `docs/client/DESIGN_AND_ACCEPTANCE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [功能交付矩阵](client/FEATURE_DELIVERY_MATRIX.md) | `docs/client/FEATURE_DELIVERY_MATRIX.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Android／Web Figma 页面实施与验证边界](client/FIGMA_ANDROID_WEB_2026_10_24.md) | `docs/client/FIGMA_ANDROID_WEB_2026_10_24.md` | 带日期／历史证据，按原日期解释 |
| [发布来源流程与分享进入续作](client/FIGMA_FLOW_CONTINUATION_2026_10_26.md) | `docs/client/FIGMA_FLOW_CONTINUATION_2026_10_26.md` | 带日期／历史证据，按原日期解释 |
| [Figma页面缺口核对与源码完善（2026-10-25）](client/FIGMA_PAGE_GAPS_2026_10_25.md) | `docs/client/FIGMA_PAGE_GAPS_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [Figma 独立页面续审与源码收口](client/FIGMA_READING_PUBLICATIONS_2026_10_24.md) | `docs/client/FIGMA_READING_PUBLICATIONS_2026_10_24.md` | 带日期／历史证据，按原日期解释 |
| [监护邀请、关系核实与授权页面（2026-10-25）](client/GUARDIAN_PAGES_IMPLEMENTATION_2026_10_25.md) | `docs/client/GUARDIAN_PAGES_IMPLEMENTATION_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [客户端详细实施方案](client/IMPLEMENTATION_PLAN.md) | `docs/client/IMPLEMENTATION_PLAN.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [大陆城市兼容与猫咪配图](client/MAINLAND_CITIES_AND_CAT_ASSETS_2026_10_26.md) | `docs/client/MAINLAND_CITIES_AND_CAT_ASSETS_2026_10_26.md` | 带日期／历史证据，按原日期解释 |
| [Android / iOS 手机和平板实施记录](client/MOBILE_IMPLEMENTATION_2026_10_20.md) | `docs/client/MOBILE_IMPLEMENTATION_2026_10_20.md` | 带日期／历史证据，按原日期解释 |
| [平板方向切换状态与UI组件续作](client/PAD_STATE_AND_UI_2026_10_23.md) | `docs/client/PAD_STATE_AND_UI_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [开发者平台注册进度与接续（2026-10-25）](client/PLATFORM_REGISTRATION_2026_10_25.md) | `docs/client/PLATFORM_REGISTRATION_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [私密线索页面与目击者会话续作](client/PRIVATE_LEAD_FLOW_2026_10_26.md) | `docs/client/PRIVATE_LEAD_FLOW_2026_10_26.md` | 带日期／历史证据，按原日期解释 |
| [官方登录与账号关联源码（2026-10-25）](client/PROVIDER_AUTH_IMPLEMENTATION_2026_10_25.md) | `docs/client/PROVIDER_AUTH_IMPLEMENTATION_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [M&D 客户端代码实施与完成标准](client/README.md) | `docs/client/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [剩余编码续作与实际边界（2026-10-25）](client/REMAINING_CODE_IMPLEMENTATION_2026_10_25.md) | `docs/client/REMAINING_CODE_IMPLEMENTATION_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [SDK配置核对与公众号回调源码（2026-10-25）](client/SDK_OFFICIAL_ACCOUNT_CODE_2026_10_25.md) | `docs/client/SDK_OFFICIAL_ACCOUNT_CODE_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [远端同步、线索提交补页与后台管理设计](client/SYNC_PAGES_ADMIN_2026_10_26.md) | `docs/client/SYNC_PAGES_ADMIN_2026_10_26.md` | 带日期／历史证据，按原日期解释 |
| [客户端代码任务拆分](client/TASKS.md) | `docs/client/TASKS.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Android UI 基础框架与组件维护规范](client/UI_COMPONENTS.md) | `docs/client/UI_COMPONENTS.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [移动端字体来源](../kmp/androidApp/src/main/assets/fonts/NOTICE.md) | `kmp/androidApp/src/main/assets/fonts/NOTICE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Android 手机／平板原生宿主](../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/README.md) | `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 公共页面与组件](../kmp/clientUi/README.md) | `kmp/clientUi/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Noto Emoji artwork attribution](../kmp/clientUi/src/commonMain/composeResources/files/emoji/NOTICE.md) | `kmp/clientUi/src/commonMain/composeResources/files/emoji/NOTICE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [iOS 入口](../kmp/ios/README.md) | `kmp/ios/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D iPhone／iPad Compose宿主](../kmp/iosApp/README.md) | `kmp/iosApp/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Mobile](../mobile/README.md) | `mobile/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Web 消费端](../web-client/README.md) | `web-client/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |

## 04 服务端与API

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [M&D V2 API速查表](API_CATALOG.md) | `docs/API_CATALOG.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D V2 数据字典](DATA_DICTIONARY.md) | `docs/DATA_DICTIONARY.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [产品闭环修订与领养源码跟进](server/ADOPTION_CLOSURE_2026_10_23.md) | `docs/server/ADOPTION_CLOSURE_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [M&D Go 服务架构决策](server/ARCHITECTURE.md) | `docs/server/ARCHITECTURE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Android / iOS 接入前服务端核对](server/CLIENT_READINESS_2026_10_20.md) | `docs/server/CLIENT_READINESS_2026_10_20.md` | 带日期／历史证据，按原日期解释 |
| [案件证据与数据清理续作](server/LIFECYCLE_RETENTION_2026_10_23.md) | `docs/server/LIFECYCLE_RETENTION_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [手机端对应的 Go 服务接口与运行交接](server/MOBILE_SERVER.md) | `docs/server/MOBILE_SERVER.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 技术运维控制台 · 2026-10-25](server/OPERATIONS_CONSOLE_2026_10_25.md) | `docs/server/OPERATIONS_CONSOLE_2026_10_25.md` | 带日期／历史证据，按原日期解释 |
| [私密线索处理与独立会话合同](server/PRIVATE_LEAD_CONTRACT_2026_10_26.md) | `docs/server/PRIVATE_LEAD_CONTRACT_2026_10_26.md` | 带日期／历史证据，按原日期解释 |
| [M&D / meow & doggie 服务端完整性审计与待完善清单（2026-10-24，2026-10-25 复核更新）](server/SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md) | `docs/server/SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md` | 带日期／历史证据，按原日期解释 |
| [服务端优先完善与作业交接](server/SERVER_FIRST_2026_10_23.md) | `docs/server/SERVER_FIRST_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [服务端与KMP共享源码实施记录](server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md) | `docs/server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [独立来源声明合同与存储](server/SOURCE_DECLARATIONS_2026_10_26.md) | `docs/server/SOURCE_DECLARATIONS_2026_10_26.md` | 带日期／历史证据，按原日期解释 |
| [上传落盘、进程恢复与过期回收续作](server/UPLOAD_RECOVERY_2026_10_22.md) | `docs/server/UPLOAD_RECOVERY_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [M&D v2 部署、运营与恢复交接手册](server/V2_RELEASE_RUNBOOK_2026_10_23.md) | `docs/server/V2_RELEASE_RUNBOOK_2026_10_23.md` | 带日期／历史证据，按原日期解释 |

## 05 架构与UML

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [M&D API与数据流](API_AND_DATA_FLOW.md) | `docs/API_AND_DATA_FLOW.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 全栈代码逻辑与UML参考](CODE_LOGIC_AND_UML_REFERENCE.md) | `docs/CODE_LOGIC_AND_UML_REFERENCE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 前端与多端页面地图](FRONTEND_SURFACE_MAP.md) | `docs/FRONTEND_SURFACE_MAP.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 项目结构与源码入口](PROJECT_STRUCTURE.md) | `docs/PROJECT_STRUCTURE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 运行、迁移与交付核对入口](RUNBOOK.md) | `docs/RUNBOOK.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D UML与业务图谱](uml/README.md) | `docs/uml/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |

## 06 设计审计与历史

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [手机按钮方向 01 / 02](design/figma-study/BUTTON_COMPARISON_BRIEF.md) | `docs/design/figma-study/BUTTON_COMPARISON_BRIEF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [全流程跨端布局与状态契约](design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md) | `docs/design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [新增平台适配缺项核对](design/figma-study/CROSS_PLATFORM_COVERAGE_AUDIT_2026_10_20.md) | `docs/design/figma-study/CROSS_PLATFORM_COVERAGE_AUDIT_2026_10_20.md` | 带日期／历史证据，按原日期解释 |
| [全量跨端补齐执行记录](design/figma-study/CROSS_PLATFORM_FULL_COMPLETION_PLAN.md) | `docs/design/figma-study/CROSS_PLATFORM_FULL_COMPLETION_PLAN.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [全量跨端逐项映射](design/figma-study/CROSS_PLATFORM_FULL_MAPPING.md) | `docs/design/figma-study/CROSS_PLATFORM_FULL_MAPPING.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [全量跨端设计交接](design/figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md) | `docs/design/figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md` | 带日期／历史证据，按原日期解释 |
| [M&D 浅色跨端设计计划](design/figma-study/CROSS_PLATFORM_LIGHT_PLAN.md) | `docs/design/figma-study/CROSS_PLATFORM_LIGHT_PLAN.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 浅色跨端设计评审](design/figma-study/CROSS_PLATFORM_LIGHT_REVIEW_2026_10_20.md) | `docs/design/figma-study/CROSS_PLATFORM_LIGHT_REVIEW_2026_10_20.md` | 带日期／历史证据，按原日期解释 |
| [产品与合规设计完善交接](design/figma-study/DESIGN_CLOSURE_2026_10_21.md) | `docs/design/figma-study/DESIGN_CLOSURE_2026_10_21.md` | 带日期／历史证据，按原日期解释 |
| [表情比例修正 · 2026-10-23](design/figma-study/EXPRESSION_ASPECT_2026_10_23.md) | `docs/design/figma-study/EXPRESSION_ASPECT_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [Figma 画布排版整理 · 2026-10-22](design/figma-study/FIGMA_CANVAS_LAYOUT_2026_10_22.md) | `docs/design/figma-study/FIGMA_CANVAS_LAYOUT_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [Figma 设计台账与产品文档全量对齐核查（2026-10-24）](design/figma-study/FIGMA_PRODUCT_ALIGNMENT_CHECK_2026_10_24.md) | `docs/design/figma-study/FIGMA_PRODUCT_ALIGNMENT_CHECK_2026_10_24.md` | 带日期／历史证据，按原日期解释 |
| [清爽亲切 · 手机关键页应用](design/figma-study/FRIENDLY_DIRECTION_BRIEF.md) | `docs/design/figma-study/FRIENDLY_DIRECTION_BRIEF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [手机 V2 · Friendly 全量统一](design/figma-study/FRIENDLY_MOBILE_ROLLOUT.md) | `docs/design/figma-study/FRIENDLY_MOBILE_ROLLOUT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [全局未读设计交接](design/figma-study/GLOBAL_UNREAD_HANDOFF_2026_10_20.md) | `docs/design/figma-study/GLOBAL_UNREAD_HANDOFF_2026_10_20.md` | 带日期／历史证据，按原日期解释 |
| [M&D 手机互动图标与表情修订](design/figma-study/INTERACTION_AND_EXPRESSION_REFINEMENT.md) | `docs/design/figma-study/INTERACTION_AND_EXPRESSION_REFINEMENT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 法律设计增补交接](design/figma-study/LEGAL_DESIGN_HANDOFF_2026_10_22.md) | `docs/design/figma-study/LEGAL_DESIGN_HANDOFF_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [M&D 逻辑与设计完整性复审](design/figma-study/LOGIC_AND_DESIGN_REVIEW_2026_10_21.md) | `docs/design/figma-study/LOGIC_AND_DESIGN_REVIEW_2026_10_21.md` | 带日期／历史证据，按原日期解释 |
| [六处逻辑修订交接](design/figma-study/LOGIC_FIX_HANDOFF_2026_10_21.md) | `docs/design/figma-study/LOGIC_FIX_HANDOFF_2026_10_21.md` | 带日期／历史证据，按原日期解释 |
| [M&D 手机端 Figma 本轮评审](design/figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md) | `docs/design/figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md` | 带日期／历史证据，按原日期解释 |
| [M&D 手机主版本 V2 · 中国大陆使用场景](design/figma-study/MOBILE_V2_BRIEF.md) | `docs/design/figma-study/MOBILE_V2_BRIEF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Figma导航与规范补全 · 2026-10-23](design/figma-study/NAVIGATION_DOC_2026_10_23.md) | `docs/design/figma-study/NAVIGATION_DOC_2026_10_23.md` | 带日期／历史证据，按原日期解释 |
| [M&D 四端黑夜版 · Design 创作清单](design/figma-study/NIGHT_DESIGN_BRIEF.md) | `docs/design/figma-study/NIGHT_DESIGN_BRIEF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [平板横竖屏原型预览优化 · 2026-10-22](design/figma-study/PAD_PREVIEW_2026_10_22.md) | `docs/design/figma-study/PAD_PREVIEW_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [M&D 宠友圈 · 手机设计方案](design/figma-study/PET_FRIENDS_CIRCLE_BRIEF.md) | `docs/design/figma-study/PET_FRIENDS_CIRCLE_BRIEF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [宠友圈四屏视觉稿提示](design/figma-study/PET_FRIENDS_CIRCLE_IMAGE_PROMPT.md) | `docs/design/figma-study/PET_FRIENDS_CIRCLE_IMAGE_PROMPT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 自有宠物走失联动 · 手机设计方案](design/figma-study/PET_LOST_LINKAGE_BRIEF.md) | `docs/design/figma-study/PET_LOST_LINKAGE_BRIEF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [走失联动四屏视觉稿提示](design/figma-study/PET_LOST_LINKAGE_IMAGE_PROMPT.md) | `docs/design/figma-study/PET_LOST_LINKAGE_IMAGE_PROMPT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Figma 学习稿交接](design/figma-study/README.md) | `docs/design/figma-study/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [远端 Figma 整体展示核对](design/figma-study/REMOTE_FIGMA_CHECK_2026_10_21.md) | `docs/design/figma-study/REMOTE_FIGMA_CHECK_2026_10_21.md` | 带日期／历史证据，按原日期解释 |
| [M&D 晒宠、互动、私信与群聊设计计划](design/figma-study/SOCIAL_CHAT_DESIGN_PLAN.md) | `docs/design/figma-study/SOCIAL_CHAT_DESIGN_PLAN.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 手机社交与聊天交接](design/figma-study/SOCIAL_CHAT_HANDOFF.md) | `docs/design/figma-study/SOCIAL_CHAT_HANDOFF.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 手机表情与贴纸底部交互](design/figma-study/WECHAT_STYLE_EXPRESSION_INTERACTION.md) | `docs/design/figma-study/WECHAT_STYLE_EXPRESSION_INTERACTION.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 业务后台管理设计](design/figma-study/admin-2026-10-26/README.md) | `docs/design/figma-study/admin-2026-10-26/README.md` | 带日期／历史证据，按原日期解释 |
| [领养、试养、交接与接回：2026-10-27](design/figma-study/adoption-implementation-2026-10-27/README.md) | `docs/design/figma-study/adoption-implementation-2026-10-27/README.md` | 带日期／历史证据，按原日期解释 |
| [预约状态设计读取：2026-10-27](design/figma-study/appointment-implementation-2026-10-27/README.md) | `docs/design/figma-study/appointment-implementation-2026-10-27/README.md` | 带日期／历史证据，按原日期解释 |
| [学习稿示意素材](design/figma-study/assets/README.md) | `docs/design/figma-study/assets/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [快捷登录矢量位置稿](design/figma-study/assets/auth-providers/README.md) | `docs/design/figma-study/assets/auth-providers/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 猫咪贴纸生成记录](design/figma-study/assets/mnd-cat-stickers-prompt.md) | `docs/design/figma-study/assets/mnd-cat-stickers-prompt.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Noto Emoji artwork attribution](design/figma-study/assets/noto-emoji/NOTICE.md) | `docs/design/figma-study/assets/noto-emoji/NOTICE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 普通手机版设计完整度审计](design/figma-study/audits/2026-10-16-mobile-readiness/MOBILE_READINESS_AUDIT.md) | `docs/design/figma-study/audits/2026-10-16-mobile-readiness/MOBILE_READINESS_AUDIT.md` | 带日期／历史证据，按原日期解释 |
| [M&D 手机端产品／设计文档覆盖复核](design/figma-study/audits/2026-10-19-mobile-docs/MOBILE_DOC_COVERAGE_AUDIT.md) | `docs/design/figma-study/audits/2026-10-19-mobile-docs/MOBILE_DOC_COVERAGE_AUDIT.md` | 带日期／历史证据，按原日期解释 |
| [文档与新版设计一致性审查](design/figma-study/audits/2026-10-21-doc-design-consistency/REVIEW.md) | `docs/design/figma-study/audits/2026-10-21-doc-design-consistency/REVIEW.md` | 带日期／历史证据，按原日期解释 |
| [业务后台设计到代码：2026-10-27](design/figma-study/operator-implementation-2026-10-27/README.md) | `docs/design/figma-study/operator-implementation-2026-10-27/README.md` | 带日期／历史证据，按原日期解释 |
| [M&D 后台运维设计 · 2026-10-25](design/figma-study/ops-2026-10-25/README.md) | `docs/design/figma-study/ops-2026-10-25/README.md` | 带日期／历史证据，按原日期解释 |
| [Web 导航发布按钮设计](design/figma-study/publish-button-2026-10-26/README.md) | `docs/design/figma-study/publish-button-2026-10-26/README.md` | 带日期／历史证据，按原日期解释 |

## 07 AI跟踪与仓库说明

| 文档 | 来源 | 状态解释 |
|---|---|---|
| [kitty-circle — `.cursor` index](../.cursor/README.md) | `.cursor/README.md` | 协作规则／工具参考，不是产品需求 |
| [Cursor User Rules — Karpathy coding + kitty-circle user-facing UX (Git-tracked)](../.cursor/USER_RULES_PASTE_DESIGN.md) | `.cursor/USER_RULES_PASTE_DESIGN.md` | 协作规则／工具参考，不是产品需求 |
| [kitty-circle UI and experience (user-facing)](../.cursor/skills/besttry-design-user-context/SKILL.md) | `.cursor/skills/besttry-design-user-context/SKILL.md` | 协作规则／工具参考，不是产品需求 |
| [Karpathy Guidelines](../.cursor/skills/karpathy-guidelines/SKILL.md) | `.cursor/skills/karpathy-guidelines/SKILL.md` | 协作规则／工具参考，不是产品需求 |
| [Impeccable Design Context — kitty-circle 喵友圈](../.impeccable.md) | `.impeccable.md` | 协作规则／工具参考，不是产品需求 |
| [当前证据](../.planning/2026-10-16-mobile-closure/findings.md) | `.planning/2026-10-16-mobile-closure/findings.md` | 带日期／历史证据，按原日期解释 |
| [进度](../.planning/2026-10-16-mobile-closure/progress.md) | `.planning/2026-10-16-mobile-closure/progress.md` | 带日期／历史证据，按原日期解释 |
| [手机设计闭环](../.planning/2026-10-16-mobile-closure/task_plan.md) | `.planning/2026-10-16-mobile-closure/task_plan.md` | 带日期／历史证据，按原日期解释 |
| [Findings](../.planning/2026-10-20-server-mobile/findings.md) | `.planning/2026-10-20-server-mobile/findings.md` | 带日期／历史证据，按原日期解释 |
| [Progress](../.planning/2026-10-20-server-mobile/progress.md) | `.planning/2026-10-20-server-mobile/progress.md` | 带日期／历史证据，按原日期解释 |
| [Go server alignment with mobile design](../.planning/2026-10-20-server-mobile/task_plan.md) | `.planning/2026-10-20-server-mobile/task_plan.md` | 带日期／历史证据，按原日期解释 |
| [M&D Global DESIGN.md](../.stitch/DESIGN.md) | `.stitch/DESIGN.md` | 带日期／历史证据，按原日期解释 |
| [M&D Global DESIGN.md](../.stitch/remote-assets/sources/11388975682457720262.md) | `.stitch/remote-assets/sources/11388975682457720262.md` | 带日期／历史证据，按原日期解释 |
| [M&D Global DESIGN.md](../.stitch/remote-assets/sources/1187049162856747640.md) | `.stitch/remote-assets/sources/1187049162856747640.md` | 带日期／历史证据，按原日期解释 |
| [Agent Memory: M&D / meow & doggie](../AGENTS.md) | `AGENTS.md` | 协作规则／工具参考，不是产品需求 |
| [Claude Project Instructions](../CLAUDE.md) | `CLAUDE.md` | 协作规则／工具参考，不是产品需求 |
| [M&D Stitch Remote Design Contract](../DESIGN.md) | `DESIGN.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Gemini Project Instructions](../GEMINI.md) | `GEMINI.md` | 协作规则／工具参考，不是产品需求 |
| [kitty-circle 项目上手文档（Android / 鸿蒙背景版）](../PROJECT_ONBOARDING_ZH.md) | `PROJECT_ONBOARDING_ZH.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D / meow & doggie Server (Go + Caddy)](../README.md) | `README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Security Policy](../SECURITY.md) | `SECURITY.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Design System Inspired by Cursor](../cursor/DESIGN.md) | `cursor/DESIGN.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Client Parity Checklist](CLIENT_PARITY_CHECKLIST.md) | `docs/CLIENT_PARITY_CHECKLIST.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D 产品流程图阅读说明](PRODUCT_FLOW_REFERENCE.md) | `docs/PRODUCT_FLOW_REFERENCE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Technical Improvement Outline](TECHNICAL_IMPROVEMENT_OUTLINE.md) | `docs/TECHNICAL_IMPROVEMENT_OUTLINE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [当前项目快照](ai/CONTEXT.md) | `docs/ai/CONTEXT.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [重要决定与待确认项](ai/DECISIONS.md) | `docs/ai/DECISIONS.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [家里 Codex 接手：服务器与 Android/iOS 公共 KMP](ai/HOME_HANDOFF_2026_10_22.md) | `docs/ai/HOME_HANDOFF_2026_10_22.md` | 带日期／历史证据，按原日期解释 |
| [AI 工作跟踪与接手入口](ai/README.md) | `docs/ai/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [AI 任务状态表](ai/TASKS.md) | `docs/ai/TASKS.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [AI 工作结果日志](ai/TASK_LOG.md) | `docs/ai/TASK_LOG.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [Noto Emoji artwork attribution](../internal/platform/mobileapi/assets/v1/NOTICE.md) | `internal/platform/mobileapi/assets/v1/NOTICE.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [中国大陆城市目录](../internal/region/README.md) | `internal/region/README.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Global DESIGN.md](../web/assets/stitch-remote/sources/11388975682457720262.md) | `web/assets/stitch-remote/sources/11388975682457720262.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |
| [M&D Global DESIGN.md](../web/assets/stitch-remote/sources/1187049162856747640.md) | `web/assets/stitch-remote/sources/1187049162856747640.md` | 现行入口或持续维护规范；正文历史段落按日期解释 |

## 2026-10-28新增批次

上文175份为2026-10-27全量同步清单，本批新增入口单列，旧Figma manifest不自动升级。

- [后台角色详细设计](design/ADMIN_ROLE_WORKSPACES_2026_10_28.md)
- [角色设计18稿与截图](design/figma-study/admin-roles-2026-10-28/README.md)
- [后台角色源码与验证](client/ADMIN_ROLE_WORKSPACES_2026_10_28.md)
- [邀请协作源码检查点](client/BUSINESS_COLLABORATION_2026_10_28.md)
