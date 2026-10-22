# 全量跨端设计交接

2026-10-20｜状态：跨端可视状态与布局已补齐，结构回读通过；客户端尚未实施。

[打开统一评审入口](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=301-27) · [表情场景索引](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=343-2) · [逐项映射](CROSS_PLATFORM_FULL_MAPPING.md)。统一入口已更新，旧 56 张代表稿标为历史参考。

本轮承接“补齐所有”：569 个产品／交互状态均已建立 Web、Desktop、平板竖屏与横屏的原生可编辑对应稿，共 2,276 张；另有 32 张 Web 窄窗与平板窄分屏样例。10 个手机评审入口不作为产品页面重复计算，将映射到跨端索引。

| 布局 | 数量 | 参考尺寸 |
|---|---:|---|
| Web | 569 | 1440×1000 |
| Desktop | 569 | 1440×1000 |
| 平板竖屏 | 569 | 834×1194 |
| 平板横屏 | 569 | 1194×834 |
| Web 窄窗样例 | 16 | 390×844 |
| 平板窄分屏样例 | 16 | 744×1133 |

这些数量包含参数、角色与交互状态变体，不是 2,308 条独立业务流程。正文沿用最新手机原生内容和默认浅色；布局按各端导航、宽度、滚动、输入和操作区重新组织。原 56 张代表稿保留为历史参考。

## 模块入口

| 模块 | 产品／状态数 | Figma 页面 |
|---|---:|---|
| 社区 | 26 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-2) |
| 发布与素材 | 14 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-3) |
| 领养与照护 | 59 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-4) |
| 核实与处理 | 29 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-5) |
| 宠物空间 | 44 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-6) |
| 走失与拾获 | 71 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-7) |
| 消息 | 20 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-8) |
| 预约安排 | 20 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-9) |
| 账号与登录 | 56 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-10) |
| 设置与通知 | 16 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-11) |
| 好物与到访 | 9 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-12) |
| 内容与恢复 | 4 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-13) |
| 群聊与管理 | 35 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-14) |
| 宠友圈 | 47 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-15) |
| 附近提醒 | 28 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-16) |
| 表情与输入 | 91 | [打开模块](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=311-17) |

表情按 16 个输入场景和连续编辑拆成子页，精确对应页见 `cross-platform-full-state.json` 的 `pageByKey`。输入面板保留 128 个表情与 24 张猫咪贴纸的原生组件，不把整屏截图作为设计稿。

## 使用方式

1. 按功能在 `cross-platform-full-plan.json` 找到 `mobileKey`。
2. 在 `cross-platform-full-state.json` 的 `roots` 查看 `web/`、`desktop/`、`tabletP/`、`tabletL/` 对应的精确节点；`webN/`、`tabletN/` 是窄窗样例。
3. 读取 [自适应与交互契约](CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)，再结合 `docs/client` 的架构、任务、接口和验收要求实施。
4. Android 与 iOS 共享 KMP 逻辑，分别实现 Compose UI 和各自的平板适配；Desktop 与 Web 按既定客户端方案实施。新设计对应稿不代表客户端代码已经完成。

## 验证与边界

已逐项回读 2,308 个目标根：最新结果未发现源正文缺失、字体缺失、默认模式不符或布局越界。16 个逻辑模块均有截图抽查，另查 Web 窄窗输入与平板窄分屏。连续输入的 16 处行尾越界已修正，聊天气泡与会话切换修整后的 420 张相关稿已复核通过。汇总见 `cross-platform-full-validation.json`；原始批次在 `cross-platform-full-checks/`，截图观察在 `cross-platform-full-visual-review.json`。

同模块内使用可用的 Figma 页面内跳转；跨模块或场景页使用精确节点链接。图中案例是固定设计示意，不执行真实授权、上传、定位或发送，也不构成真实客户端的路由、键盘、读屏、设备或同步验收。

复杂表情／贴纸内部动作的全量重绑多次触发 Figma MCP 超时。本轮会逐项完成其可视状态、布局、原文、字体及基础导航检查，内部发送、最近／收藏与连续输入行为以手机源原型和既有表情交接为依据；不把这部分标成跨端全量动作验证通过。具体已验证节点与检查范围保存在逐批记录中。

当前只交付手机 V2 默认浅色。旧 Honey／Mint／Night／Neutral 保留作参考和未来扩展，不新增切换器或随系统进入深色。

本轮只改 Figma 与设计／交接资料，未改客户端代码；未运行编译、构建或应用测试，遵守全局不自行编译规则。
