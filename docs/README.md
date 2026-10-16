# M&D 文档入口

更新：2026-10-16
M&D = meow & doggie，猫优先、doggie 友好。

## 当前产品方向

交流与领养为主；找猫提供明确标注的出售/有偿转让信息。允许个人、商家、猫舍和已披露代理；重点治理身份冒用、虚假地点、隐瞒关系、换猫与隐藏费用。

家庭饲养不构成可信前提。平台按具体事项披露核验范围，不能承诺完全消除欺骗。首轮信息与预约、不代收款为规划假设，支付服务仍待决定。

## 先看整体

[打开交互流程总览](design/flows/index.html)，含产品全貌、找猫、发布核验、预约变更、领养、投诉申诉和核验边界七张图。

新交付：[Figma 产品与交互设计 V1](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=14-76)及[学习稿说明](design/MND_FIGMA_STUDY_BRIEF.md)。用户已授权独立新视觉与多宠分类（猫咪优先）；该非商业学习探索不代表现有客户端或 Stitch 工程合同已迁移。

本轮增补：[武汉／上海冷启动、走失互助与登录规则](design/MND_COLD_START_LOST_PETS_AND_AUTH_SPEC.md)、[领养核验／举报有效性规范](design/MND_TRUST_AND_DISCLOSURE_SPEC.md)及[四条流程图](design/flows/cold-start-and-auth.html)。首发两城由用户确认；来源采集、地图与生产短信尚未接入。

当前主线：[手机 V2 浅色对照](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=61-12)、[59 屏范围／依据／检查边界](design/figma-study/MOBILE_V2_BRIEF.md)。按用户反馈重做配色与手机层级，先完成手机，再扩其它端；329 条导航仅为静态示意。新增 10–11，旧 [四端黑夜版](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=44-241)保留；非商业普通 Design，不是原生 Specs 或客户端实施。

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

产品范围以当前 PRD 为准；具体披露规则以真实性规范为准；UX 负责交互。Stitch V2 继续是视觉权威。新导航属于拟实施方案，尚未修改远程 Stitch 或客户端；旧截图不能证明新流程已实现。

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
