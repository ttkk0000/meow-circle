# M&D 设计交接索引

> **2026-10-29 现行入口**：[源码与文档基线](../CURRENT_BASELINE.md) · [产品／交付状态](../PRODUCT_AND_DELIVERY_MATRIX.md) · [全部文档目录](../DOCUMENTATION_CATALOG.md) · [Figma文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3)。当前255路径／288操作／141服务端schemas、143Web schemas、88对DTO、45张mobile表／13迁移、29份shared／44份公共UI文件、48个FeaturePage；这些是源码口径。本文带日期的旧数量和“待实施”结论仅适用于当时批次，运行／全量验收未完成。

2026-10-29本轮：[协作40稿最终细化](figma-study/collaboration-refinement-2026-10-29/README.md)完成五布局工作区／照片入口、20处示例事项邀请版本、退出结果返回及平板同状态切换；96动作独立回读无失败，五张完整稿图已看。原审批额度问题已恢复；未新增消费者状态、改业务源码或运行工程。

2026-10-29：[后台关键任务39状态与评审索引](figma-study/admin-task-interaction-2026-10-29/README.md)／[交互合同](ADMIN_TASK_INTERACTION_2026_10_29.md)已细化表单、确认、失败／未知与撤权；[协作退出8状态补录](figma-study/collaboration-2026-10-29/README.md)纳入748条追踪。当前消息分类／待办／已读差距见[事件对齐表](NOTIFICATION_EVENT_ALIGNMENT_2026_10_29.md)，不把设计统一当业务源码已修改。

2026-10-28：[后台按角色详细设计18稿](figma-study/admin-roles-2026-10-28/README.md)与[原生权限规范](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3?node-id=21-2)先完成并核对，再接[源码实现](../client/ADMIN_ROLE_WORKSPACES_2026_10_28.md)。9个人员岗位、worker与附加证据能力分别处理；未运行验收。

2026-10-27：[业务后台13稿源码读取](figma-study/operator-implementation-2026-10-27/README.md)、[预约20稿](figma-study/appointment-implementation-2026-10-27/README.md)、[领养／接回25稿](figma-study/adoption-implementation-2026-10-27/README.md)已用于本轮实际页面与公共组件接线；[源码恢复点](../client/COMPLETE_LANDING_2026_10_27.md)区分设计、源码／静态与未完成的逐端验收，不将740条映射视为全量完成。

2026-10-26新增：[业务后台管理13稿](figma-study/admin-2026-10-26/README.md)已写入原文件第11个页面“10 后台管理 · 业务与核验”，[原型起点843:22](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=843-22)。复用既有组件与Light V2，13图已核；消费者线索提交四状态的源码续作见[本批交接](../client/SYNC_PAGES_ADMIN_2026_10_26.md)。设计、源码与运行验收分别记录。

2026-10-23现行增补：[业务闭环与交互合同](MND_PRODUCT_CLOSURE_CONTRACT_2026_10_23.md)补领养独占／候补、申请周期、接回截止／撤回、人员与区域选择、HTTPS及游客入口；[当前交付账本](../client/CURRENT_DELIVERY_STATUS_2026_10_23.md)连接设计与源码／验收。已有候补／冲突／撤回／过期五布局继续复用；远端本轮核对与修订状态另留证，不把文档更新当画板写入。

更新：2026-10-22｜品牌：M&D / meow & doggie

新移动端已获批准使用Figma手机V2默认浅色，Noto Sans SC／Manrope、既有语义色及组件；Android／iOS与各自平板按[移动实施记录](../client/MOBILE_IMPLEMENTATION_2026_10_20.md)和[自适应合同](figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)实施。旧Stitch镜像及未迁移旧端保留各自规则，不能用历史“待批准”重新阻塞已授权工作，也不能混用两套Token。

本轮用户已授权完善产品内容与Figma。[安全、年龄与生命周期合同](MND_SAFETY_AND_LIFECYCLE_SPEC.md)新增监护、成年人业务、单宠试养／候补、安全接回、预约风险暂停、资料权利与游客报告。[本轮修订交接](figma-study/DESIGN_CLOSURE_2026_10_21.md)与[精确节点账本](figma-study/design-closure-2026-10-21-state.json)区分设计、检查和实施缺口。

## 当前从哪里进入

- [业务后台管理843:22](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=843-22)：工作台、举报／权利／资料／监护／准入、角色及确认／异常状态；[节点与截图](figma-study/admin-2026-10-26/nodes-and-validation.json)。
- [技术运维793:22](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=793-22)：原独立14稿与组件展示保留，[交接](figma-study/ops-2026-10-25/README.md)。2026-10-22的9页目录为历史快照，现行文件另含本两页。
- 2026-10-28 [协作退出与有限工作区](figma-study/collaboration-exit-2026-10-28/README.md)：五布局原40份可编辑稿保留，原241链接检查为历史；2026-10-29[最终入口／标注及五布局截图](figma-study/collaboration-refinement-2026-10-29/README.md)已完成，96动作独立回读通过。[源码与静态交付](../client/REMOTE_CONTINUATION_2026_10_29.md)另记，未运行验收。
- 2026-10-27 [发布按钮全布局评审](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=818-17528)／[交接与代码状态](figma-study/publish-button-2026-10-26/README.md)：手机、Web、平板横竖屏与Desktop的3081个入口统一完整药丸／白圈加号，四类20状态与6张最终图已检查。2026-10-28共享Compose导航／快捷入口也已写源码，Web与原生运行验收均未完成。
- 最新[法律设计补充评审入口662:398665](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=662-398665)与[交接／最终验证状态](figma-study/LEGAL_DESIGN_HANDOFF_2026_10_22.md)：45组五布局225稿、四个独立回调与角色样例板；[法律设计合同](MND_LEGAL_DESIGN_SUPPLEMENT_2026_10_22.md)区分事项协助、正式注销、通知／声明与合同版本。真实经营与接口仍另行验收。
- 最新[平板横竖屏预览优化](figma-study/PAD_PREVIEW_2026_10_22.md)：88个Flow收敛为两个，704组同状态横竖屏稿可点右上角预览按钮互切；按钮仅作Figma演示，客户端不实现。直接使用文档中的Pad竖屏／横屏预览链接，设备转动后仍需手动切换。
- 最新[画布排版整理](figma-study/FIGMA_CANVAS_LAYOUT_2026_10_22.md)：9页核对、7页紧凑排布，手机画布高度减少88.2%；节点、原稿尺寸及交互引用保留。当前页位置以下方12页目录为准，旧17／18页记录保留为历史证据。
- 最新[六处操作逻辑修订入口513:1860930](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=513-1860930)与[交接及静态条件结果](figma-study/LOGIC_FIX_HANDOFF_2026_10_21.md)：覆盖短信、成年核对、安全取消、接回双边版本、原工单追加和删除最终确认。本轮84组五布局，其中12组覆盖上轮同ID原稿；来源优先级见本轮账本。
- [项目总览395:3](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=395-3)。
- 设备索引：[手机425:1839840](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=425-1839840)、[Web425:1839880](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=425-1839880)、[平板425:1839920](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=425-1839920)、[Desktop425:1839960](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=425-1839960)。
- [安全、账号与规则评审入口513:1860930](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=513-1860930)当前位于05功能索引；各设备原稿已按设备归档，原根ID保留，旧增补页471:524不再是当前入口。
- [产品与安全修订评审入口493:1861258](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=493-1861258)：本轮账号／监护、领养接回、规则数据和跨端未读集中入口；58组五布局及32张未读跨端稿的检查结果见修订交接。
- 先前[全量状态索引301:27](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=301-27)、[手机复核238:371769](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=238-371769)仍作为历史阶段入口，不等于当前设备目录。

## 当前页面位置

2026-10-29按本轮MCP回读维护为12页；原稿按设备归档，功能页保留跨端原稿链接。2026-10-22的9页目录是历史基线，后续业务后台、技术运维及角色页已并入下表。画板链接与页面ID分别核对，不能因旧页面消失推断旧画板丢失。

| 页面 | 当前ID | 用途 |
|---|---|---|
| 00 项目总览 | 395:2 | 项目及设备入口 |
| 01 手机 · 原稿与交互 | 56:25 | 手机功能索引、原稿及交互，含新增法律设计分组 |
| 02 Web · 原稿与交互 | 403:2 | Web索引、原稿及交互，含新增法律设计分组 |
| 03 平板 · 原稿与交互 | 403:3 | 平板索引、横竖屏原稿及交互，含新增法律设计分组 |
| 04 桌面 · 原稿与交互 | 403:4 | 桌面索引、原稿及交互，含新增法律设计分组 |
| 05 功能索引 · 跨端原稿链接 | 403:5 | 功能／说明分组、跨端链接、各轮修订及法律补充评审入口 |
| 06 规范与组件 | 14:2 | 语义变量、字体、原生组件 |
| 07 早期概念原型（非现行） | 30:2 | 早期阶段演示，不是现行起点 |
| 08 历史归档 | 0:1 | 历史资料 |
| 09 后台运维 · 设计与交互 | 793:21 | 原14稿，环境与capabilities边界 |
| 10 后台管理 · 业务与核验 | 843:21 | 原13稿，业务事项与异常状态 |
| 11 后台角色 · 工作区与权限 | 888:1402 | 9岗位／原18稿及本轮39任务状态＋1评审索引；入口932:2803 |

## 内容与实施合同

1. 产品：[PRD](../bmad/prd.md)、[UX](../bmad/ux-design.md)、[真实性与披露](MND_TRUST_AND_DISCLOSURE_SPEC.md)。
2. 现行增补：[认证](MND_MOBILE_AUTH_PLAN.md)、[安全生命周期](MND_SAFETY_AND_LIFECYCLE_SPEC.md)、[冷启动与寻宠](MND_COLD_START_LOST_PETS_AND_AUTH_SPEC.md)、[全局未读](MND_GLOBAL_UNREAD_SPEC.md)。
3. 跨端：[全量交接](figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md)、[逐项映射](figma-study/CROSS_PLATFORM_FULL_MAPPING.md)、[自适应合同](figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)、[本轮修订账本](figma-study/design-closure-2026-10-21-state.json)。
4. 实施：[客户端入口](../client/README.md)、[功能矩阵](../client/FEATURE_DELIVERY_MATRIX.md)、[服务端缺口](../server/CLIENT_READINESS_2026_10_20.md)。

历史569状态／2308根与原56张代表稿是有日期的基线；后加未读及本轮安全修订分别记账，不能把入口卡数量当作完整状态覆盖。旧账本pages／pageByKey记录的是生成时位置，恢复时依据稳定rootID的当前PAGE祖先，按当前页面拆批；不要直接重跑依赖旧页面的配方。

## 保留的参考与验证边界

- 当前远端目录与画布依据：[10月22日排版整理](figma-study/FIGMA_CANVAS_LAYOUT_2026_10_22.md)。[10月21日只读核对](figma-study/REMOTE_FIGMA_CHECK_2026_10_21.md)描述当时17页快照，后续18页位置亦为历史。
- [原手机评审](figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md)、[原56张代表稿](figma-study/CROSS_PLATFORM_LIGHT_REVIEW_2026_10_20.md)、旧V1／黑夜阶段仅作带日期的历史参考。
- 旧工程参照：[Stitch合同](../../.stitch/DESIGN.md)、[UI对齐指南](MND_UI_ALIGNMENT_GUIDE.md)、[本地图库](../../web/stitch-remote-gallery.html)、[桌面板](../../web/pawpop-desktop.html)；不将它们伪称已迁移。
- 设计稿、可点击固定状态、结构检查、真实API、平台构建和设备验收分别记录。B04、年龄／监护服务、人工运营、正式政策和真实供应商尚不因本轮画板更新而完成。

## 本地旧端实现参考

本次合并保留 `web/cute.html`、`web/pawpop-desktop.html`、Expo 和 `kmp/desktopApp` 的业务接线；它们仍是旧端参考，不能抵扣新版 v2 的实施与验收。客户端对齐范围见 [本地清单](../CLIENT_PARITY_CHECKLIST.md)。
