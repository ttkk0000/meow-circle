# M&D 设计交接索引

更新：2026-10-16｜品牌：M&D / meow & doggie

本文件负责指路，不替代 PRD、交互规则或工程视觉合同。产品以交流与领养为主，可信找宠为补充。2026-10-16 已交付独立 Figma 非商业学习稿；未修改 Stitch 远程项目或客户端。

## 0. 先看新的 Figma 学习稿

最新增补（待同步）：[宠友圈四屏提案](figma-study/pet-friends-circle-concept-v2.png)／[走失到回家四屏提案](figma-study/pet-lost-linkage-concept-v2.png)，以及 [圈子流程图](figma-study/pet-friends-circle-flows.svg)／[走失联动图](figma-study/pet-lost-linkage-flows.svg)。细则见 [宠友圈设计](figma-study/PET_FRIENDS_CIRCLE_BRIEF.md)／[走失联动设计](figma-study/PET_LOST_LINKAGE_BRIEF.md)。它们是位图与交互规则草案，Figma 编辑连接失败，尚无新增原生画板或交互；下述 134 状态是前阶段范围，不包含这些增补。

当前优先级（2026-10-16）：01 清爽亲切已补强晒宠、评论点赞举报、一对一私信与群聊，并生成手机交互演示。入口为 [手机原型](https://www.figma.com/proto/3OFOC3vu18Kx9sRPjummOC/M-D?page-id=56%3A25&node-id=56-27&starting-point-node-id=56%3A27&scaling=scale-down&content-scaling=fixed)／[八屏总览与三条流程图](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=109-3347)，详见 [社交与聊天交接](figma-study/SOCIAL_CHAT_HANDOFF.md)。134 个手机参考状态，按钮采用 48 高／24 圆角真胶囊；卡片和输入仍为 16。原 59 屏样式统一是前一阶段历史。Pad／桌面／Web／暗色扩展暂缓；全部设计确认后才完善应用代码。工程 Stitch 权威和客户端代码未变。

[打开 M&D 产品与交互设计 V1](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=14-76)，位于用户现有 kitty-circle 教育文件夹。

| 页面 | 已交付内容 |
|---|---|
| 00 设计总览 | 品牌、物种包容、信息架构与三条核心路径 |
| 01 视觉与组件 | Light / Dark Token、字体、10 个主组件与状态样例 |
| 02 核心体验 | 社区、领养、找宠、两类详情、我的，共 6 屏 |
| 03 信任与异常 | 申请、预约、变更、会话、发布、报告，共 6 屏及交接规则 |
| 04 可点击原型（示意） | 12 屏独立快照，部分导航与返回，不模拟真实提交 |
| 05 黑夜版 · 跨端总览 | 跨端规则、组件样例、四端视觉对照快照 |
| 06 黑夜版 · 手机 | 14 屏；社区空态、领养、核验、发布、举报、进度、登录／验证码、走失互助、找宠、我的、消息 |
| 07 黑夜版 · Pad | 2 屏；竖屏领养主从、横屏社区与阅读 |
| 08 黑夜版 · 桌面客户端 | 2 屏；会话与宠物上下文、发布工作区 |
| 09 黑夜版 · Web | 3 屏；游客首页、分享详情、移动网页详情 |
| 10 手机主版本 V2 · 浅色 | 134 个参考状态；社交、私信、群聊与原领养／互助；589 条 NODE 动作含组件切换，另有返回 |
| 11 手机 V2 · 视觉与交接 | 八屏总览、三条流程图、社交组件与按钮方向历史；先手机再扩端 |

前一阶段的 [四端黑夜版对照](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=44-241)与 [21 屏清单与验证](figma-study/NIGHT_DESIGN_BRIEF.md)仍可查阅。该阶段 21 个唯一界面、手机 49 条动作独立于新手机 V2，不混计完成度。当前团队未启用原生 Specs，用户已同意普通 Figma Design；这些页面不是原生 Spec，也不是已上线客户端。

早期学习稿曾探索独立陶土／暖纸方向；当前手机已按用户选择转为清爽亲切：猫咪优先，兼容狗狗和其他宠物，轻浅中性底与珊瑚白字主动作。学习稿不覆盖下面的 Stitch 工程合同。新方向正式用于客户端前，仍需确认并更新权威合同，不能两套 Token 混用。

[学习稿说明与验证边界](MND_FIGMA_STUDY_BRIEF.md)｜[本地预览与节点账本](figma-study/README.md)。提交、支付、上传及接受／拒绝未实现，读屏与设备检查未执行。

### 双城冷启动：已补设计与剩余范围

用户已选择武汉、上海；[规则增补](MND_COLD_START_LOST_PETS_AND_AUTH_SPEC.md)和[四条流程图](flows/cold-start-and-auth.html)已整理。新手机 V2 在旧黑夜版基础上补走失案例结构、私密线索／回执、求助发布与维护、发布者回应、申诉等参考页，收到回执与人工核实分开。仍待完整审核结论状态、物种差异字段和全部异常；真实数据采集、地图、生产短信、输入与提交尚未接入，不能把视觉稿宣称为功能实现。

## 1. 先读产品与流程

1. [整体流程总览](flows/index.html)：七张可切换流程图。
2. [策略研究](../bmad/research-report.md)：用户决定、市场依据与方案假设。
3. [PRD 2.0](../bmad/prd.md)：范围、状态、需求、故事和验收。
4. [真实性与披露规范](MND_TRUST_AND_DISCLOSURE_SPEC.md)：主体、关系、地点、费用、证据、变更和复核。
5. [UX 规格 2.0](../bmad/ux-design.md)：具体页面和异常路径。
6. [客户端交互摘要](MND_CLIENT_INTERACTION_SPEC.md)：工程交接入口。

允许商家、个人和已披露代理。家庭饲养不作为可信认证；有代理也不自动违规。领养与出售清楚标注，具体猫的身份、来源、地点和费用按事项核对。

## 2. 现有客户端的视觉权威

- [UI 对齐指南](MND_UI_ALIGNMENT_GUIDE.md)
- [AI Stitch 规则](MND_STITCH_AI_RULES.md)
- [Stitch V2 设计合同](../../.stitch/DESIGN.md)，项目 `projects/13275961100622290348`
- [本地元数据](../../.stitch/metadata.json)
- `.stitch/remote-assets` 与 `web/assets/stitch-remote`

Honey、Mint、Night、Neutral 四主题、Outfit/Inter、16px 消费卡、8px Neutral 卡等继续沿用。新导航和信任/预约屏需要映射现有组件；文字提案不表示已替换远程设计。

## 3. 现有页面与代码参考

| 表面 | 来源 | 用途与限制 |
|---|---|---|
| 静态 WebUI | `web/cute.html`、`web/cute-ui.css`、`web/cute-ui.js` | 布局参考；不代表完整服务可用 |
| 现有 Web | `web/shared.js`、`web/stitch-theme-bridge.css` 与页面脚本 | 现有 API/演示行为，需区分数据模式 |
| 移动参考 | `web/mnd-web-client-board.html`、`web/pawpop-mobile.html` | 现有屏与组件映射 |
| 桌面参考 | `web/pawpop-desktop.html`、`kmp/desktopApp` | 桌面密度与导航参考 |
| Expo | `mobile/app`、`mobile/src` | 已有客户端，不等于新 PRD 实现 |
| KMP Android | `kmp/androidApp`、`kmp/shared` | 已有三模式与共享模型，待迁移 |

[全尺寸图库](../../web/stitch-remote-gallery.html)、[当前客户端板](../../web/mnd-web-client-board.html)和[桌面板](../../web/pawpop-desktop.html)可用于对照。不要把本轮流程图当 App 视觉稿。

## 4. 建议设计交接顺序

1. 用全貌图确认社区、领养与找猫的业务关系。
2. 设计单猫详情：性质、主体关系、核验、地点与费用。
3. 设计发布向导和材料状态，明确未知、待补充与过期。
4. 设计预约快照、变更差异和拒绝出口。
5. 设计领养申请、可选试养、交接和回访。
6. 设计到访反馈、投诉、复核与申诉。
7. 映射桌面工作台、键盘/读屏和响应式状态。
8. 完成 Stitch 对齐后才进入客户端实施。

## 5. 历史证据与记忆

- [2026-08 产品设计/无障碍审计](MND_PRODUCT_DESIGN_SPEC_AND_AUDIT.md)：保留当时截图、对比度与代码发现；其市场/订单优先级不再代表本版。
- [设计记忆](MND_DESIGN_MEMORY.md)：顶部为当前方向，后续是带日期历史记录。
- [文档总入口](../README.md)：按角色阅读路径和工程资料。
