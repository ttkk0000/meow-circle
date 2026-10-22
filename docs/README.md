# M&D 文档入口

2026-10-22技术规划：[架构与数据／API合同](bmad/architecture.md) · [准入核对](bmad/gate-check.md) · [实施顺序S0–S7](client/IMPLEMENTATION_PLAN.md) · [任务及B05–B12](client/TASKS.md)。已将最新45组225布局追溯到技术任务；当前仍有服务端与实际运营／环境阻塞，规划完成不代表代码或上线验收完成。

2026-10-20设计新补充：[全局未读／99+规范](design/MND_GLOBAL_UNREAD_SPEC.md) · [Figma与接入交接](design/figma-study/GLOBAL_UNREAD_HANDOFF_2026_10_20.md)。8张手机状态、边界板及四端入口已设计；完整跨端新状态和真实未读实现待接入。

AI 接手或换电脑续做：先读[2026-10-22家里接手入口](ai/HOME_HANDOFF_2026_10_22.md)、[本批服务器／公共KMP记录](server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)、[工作跟踪入口](ai/README.md)、[当前快照](ai/CONTEXT.md)、[任务状态](ai/TASKS.md)，按需查[重要决定](ai/DECISIONS.md)与[结果日志](ai/TASK_LOG.md)。当前进度以此汇总及其引用的最新交接为准，下文早期阶段记录保留原时间范围。

当前移动端代码进展（2026-10-20）：[第一批实施与未完成项](client/MOBILE_IMPLEMENTATION_2026_10_20.md) · [最高稳定依赖基线与兼容边界](client/DEPENDENCY_BASELINE_2026_10_20.md) · [服务端接入核对](server/CLIENT_READINESS_2026_10_20.md)。Android API 37 / 最低 26；Android / iOS 独立 Compose 手机与平板结构已写，完整功能仍未完成；B04 认证合同缺失期间对外提交受阻。下文较早阶段的“未修改客户端”记录不代表当前源码状态。

客户端实施文档（2026-10-20）：[详细架构与迁移](client/IMPLEMENTATION_PLAN.md) · [功能交付矩阵](client/FEATURE_DELIVERY_MATRIX.md) · [默认主题／平板／验收](client/DESIGN_AND_ACCEPTANCE.md) · [任务](client/TASKS.md) · [Codex / Gemini交接](client/AGENT_HANDOFF.md)。Android / iOS 共享KMP逻辑、分别实现Compose UI与平板适配；Web和Desktop方案已记录。当前只做设计默认主题，多主题仅保留参考。新架构尚未实施，本轮未运行工程。

更新：2026-10-19
M&D = meow & doggie，猫优先、doggie 友好。

2026-10-20 服务端增补：[手机接口与迁移交接](server/MOBILE_SERVER.md)、[模块化单体决策](server/ARCHITECTURE.md)、[OpenAPI](api/mobile-v2.openapi.json)。按用户新授权完善 Go 服务源码，保留以后拆分微服务的接缝；客户端未改，编译、测试、迁移与供应商联调未执行。

2026-10-20 跨端设计增补：[浅色统一评审入口](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=301-27)与[本轮交接](design/figma-study/CROSS_PLATFORM_LIGHT_REVIEW_2026_10_20.md)。按手机样式新增 Web 18、Desktop 17、平板 21 张页面／状态，含横竖屏及窄分屏；暗色继续暂缓。仅设计稿，生产 Stitch 和客户端未改；下文“其他端暂缓”为手机阶段范围。

## 当前产品方向

交流与领养为主；找猫提供明确标注的出售/有偿转让信息。允许个人、商家、猫舍和已披露代理；重点治理身份冒用、虚假地点、隐瞒关系、换猫与隐藏费用。

家庭饲养不构成可信前提。平台按具体事项披露核验范围，不能承诺完全消除欺骗。首轮信息与预约、不代收款为规划假设，支付服务仍待决定。

## 先看整体

[打开交互流程总览](design/flows/index.html)，含产品全貌、找猫、发布核验、预约变更、领养、投诉申诉和核验边界七张图。

新交付：[Figma 产品与交互设计 V1](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=14-76)及[学习稿说明](design/MND_FIGMA_STUDY_BRIEF.md)。用户已授权独立新视觉与多宠分类（猫咪优先）；该非商业学习探索不代表现有客户端或 Stitch 工程合同已迁移。

本轮增补：[武汉／上海冷启动、走失互助与登录规则](design/MND_COLD_START_LOST_PETS_AND_AUTH_SPEC.md)、[领养核验／举报有效性规范](design/MND_TRUST_AND_DISCLOSURE_SPEC.md)及[四条流程图](design/flows/cold-start-and-auth.html)。首发两城由用户确认；来源采集、地图与生产短信尚未接入。

当前手机主线：[Figma本轮评审与覆盖追踪](design/figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md)、[认证规划](design/MND_MOBILE_AUTH_PLAN.md)、[手机评审入口](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=238-371769)。本轮213个新补／重绘状态，合并历史579个唯一参考根ID；含变体与覆盖层，不代表独立流程或功能验收。先手机，用户确认Figma后再开发，其他端暂缓。

## 当前文档与权威范围

| 要解决的问题 | 文档 | 适用范围 |
|---|---|---|
| 为什么这样定位、哪些是用户决定 | [市场策略](bmad/research-report.md) | 2.0；事实、建议和待验证假设 |
| 做什么、范围与验收 | [PRD](bmad/prd.md) | 2.0；业务需求、优先级、状态和追踪 |
| 身份/来源/地点怎样披露 | [真实性规范](design/MND_TRUST_AND_DISCLOSURE_SPEC.md) | 字段、证据、预约版本、反馈和复核 |
| 用户怎么完成任务 | [UX 规格](bmad/ux-design.md) | 2.0；页面、错误、退出、无障碍 |
| 客户端怎样交接 | [交互摘要](design/MND_CLIENT_INTERACTION_SPEC.md) | 精简索引，不重复完整 PRD |
| 视觉怎样保持一致 | [UI 对齐指南](design/MND_UI_ALIGNMENT_GUIDE.md) | Stitch V2 色彩、字体、形状与映射 |
| 设计素材从哪里来 | [设计交接索引](design/MND_FIGMA_DESIGN_DOCUMENT.md) | 本地镜像、设计板、交接顺序 |
| 当时发现过什么问题 | [2026-08 历史审计](design/MND_PRODUCT_DESIGN_SPEC_AND_AUDIT.md) | 历史代码/截图证据，不是当前产品范围 |
| 已做与未做如何区分 | [设计记忆](design/MND_DESIGN_MEMORY.md) | 当前决策摘要和带日期历史记录 |

产品范围以当前PRD及已确认增补为准；[安全生命周期合同](design/MND_SAFETY_AND_LIFECYCLE_SPEC.md)统一年龄、试养接回、预约风险与资料权利。新移动端采用已批准Figma V2默认浅色，旧Stitch按未迁移旧端范围保留；[现行设计入口](design/MND_FIGMA_DESIGN_DOCUMENT.md)与代码／运行结果分别记录。

## 按角色阅读

| 角色 | 阅读顺序 |
|---|---|
| 产品/项目负责人 | 流程总览 → 策略 → PRD → 待决策项 |
| 设计 | 流程总览 → UX → 真实性规范 → Stitch/UI 指南 |
| 前端/移动端 | PRD → 交互摘要 → UX → UI 指南 |
| 后端 | PRD → 真实性规范 → 现有 API/结构文档 |
| 运营 | 真实性规范 → 发布/投诉流程图 → PRD 运营门槛 |
| 后续 AI | 项目规则 → 当前 PRD → 对应目录和素材；不要把历史审计当现行需求 |

## 工程资料

- [项目结构](PROJECT_STRUCTURE.md)
- [API 与数据流](API_AND_DATA_FLOW.md)
- [前端页面地图](FRONTEND_SURFACE_MAP.md)
- [运行资料](RUNBOOK.md)
- [本地技术改进提纲（旧端工程参考）](TECHNICAL_IMPROVEMENT_OUTLINE.md)
- [本地客户端对齐清单（旧端工程参考）](CLIENT_PARITY_CHECKLIST.md)
- [中文上手说明](../PROJECT_ONBOARDING_ZH.md)

这些工程资料主要记录已有系统，不自动表明已实现新一版真实性能力。运行命令是参考，不构成本轮执行授权。

## 视觉来源

- [Stitch V2 设计合同](../.stitch/DESIGN.md)
- [远程素材本地目录](../.stitch/remote-assets)
- [客户端设计板](../web/mnd-web-client-board.html)
- [全尺寸参考图库](../web/stitch-remote-gallery.html)
- [桌面设计板](../web/pawpop-desktop.html)
- [静态 WebUI](../web/cute.html)

不要恢复已移除的旧手工设计稿或旧 Stitch 导出。保留 M&D 品牌、Honey/Mint/Night/Neutral、Picnic/Desk/Arcade/Garden 背景体系。

## 本次整理的验证边界

2026-10-15：仅修改相关文档和独立流程图。检查本地链接、需求编号/引用、SVG/HTML 结构、编码与差异；实际结果见本次交付。未运行编译，遵循全局不自行编译规则；项目运行、测试和设备表现未验证。

历史自动化或截图记录见原报告的证据日期，不能当作本次已重新执行。
