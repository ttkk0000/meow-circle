# M&D 客户端代码实施与完成标准

2026-10-22源码更新：[服务端/共享层实施记录](../server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)。本轮已实现身份资格、用途授权、幂等恢复及主要法律/业务合同源码，新增008迁移与精确公共KMP DTO/API；端侧UI与实际运行仍待验收。下文“只完善规划”和“B04缺失”均为上一批次的历史状态，不能再据其恢复旧自动建号或本地验证。

2026-10-22最新技术规划：[系统架构与数据／API合同](../bmad/architecture.md)、[实施准入核对](../bmad/gate-check.md)、[逐项追溯](../bmad/implementation-trace-2026-10-22.json)。实施方案已按最新设计改为S0–S7、任务扩B05–B12，优先补服务端准入、用途、幂等、资料清理及安全运营，再接真实业务切片；本轮只完善规划，没有新增业务代码或运行验收。

更新：2026-10-20。状态：Android / iOS 第一批共享逻辑、独立 Compose 手机 / 平板入口与依赖升级源码已写；完整功能尚未完成、未验收。当前事实见[实施记录](MOBILE_IMPLEMENTATION_2026_10_20.md)、[依赖基线](DEPENDENCY_BASELINE_2026_10_20.md)和[服务端接入核对](../server/CLIENT_READINESS_2026_10_20.md)。B04 认证合同缺失期间，对外提交保持阻塞。

本次用户决定：Android / iOS 基于 KMP 共用逻辑，各自使用 Compose UI 绘制页面，各自完成平板适配；Web、桌面采用下述工程方案。当前统一使用设计稿默认浅色主题，旧多主题保留参考与扩展位置，暂不开放切换。用户已授权移动端代码；Android 面向 Android 17 / API 37、最低 API 26。编码授权不包含构建或设备运行。

历史基线覆盖与本轮增补分别记录；当前入口见[安全修订交接](../design/figma-study/DESIGN_CLOSURE_2026_10_21.md)。2026-10-20历史覆盖：[完整跨端交接](../design/figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md) · [逐项映射](../design/figma-study/CROSS_PLATFORM_FULL_MAPPING.md) · [自适应契约](../design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)。569 个产品／交互状态各有 Web、Desktop、平板横竖屏对应稿，另补 32 张窄窗样例；布局与静态内容回读通过。复杂表情内部动作仍参考手机源原型，未完成跨端全量交互或设备验收。设计覆盖和本目录的客户端实施完成度分别记录，不能把画板状态当作代码已完成。

## 阅读顺序

| 文档 | 解决的问题 |
|---|---|
| [实施方案](IMPLEMENTATION_PLAN.md) | 架构、当前代码差距、目录、共享逻辑、平台能力、迁移与推进顺序 |
| [功能交付矩阵](FEATURE_DELIVERY_MATRIX.md) | 每个功能有哪些页面、接口、状态、平台差异与完成条件 |
| [视觉、平板与验收](DESIGN_AND_ACCEPTANCE.md) | 默认主题、Android 平板、iPad、Web 和桌面布局及验证矩阵 |
| [任务拆分](TASKS.md) | 可领取任务 ID、依赖、文件所有权、交付物与阶段出口 |
| [Codex / Gemini 交接](AGENT_HANDOFF.md) | 接手提示词、协作顺序、公共文件保护、结果记录模板 |

建议先读实施方案的“明确决定”和“当前源码基线”，再领取单个任务。不要仅看截图开始复制页面，也不要把这份文档存在当作运行、构建、提交或发布授权。

## 架构决定摘要

| 客户端 | 业务与数据逻辑 | UI 与自适应 |
|---|---|---|
| Android 手机 / 平板 | `kmp/shared` | 现有 `kmp/androidApp` 中的 Jetpack Compose，独立 Android 平板布局 |
| iPhone / iPad | 同一个 `kmp/shared` | 已新增 `kmp/iosUi`，使用 Compose Multiplatform for iOS；`iosApp/ComposeApp` 为薄宿主，独立 iPad 布局；旧 SwiftUI 留档 |
| Desktop | 复用 `kmp/shared` | 延续 `kmp/desktopApp` / Compose Desktop，独立桌面工作区 |
| Web / 移动网页 / 平板网页 | 独立 TypeScript，复用同一 HTTP 契约与用例规则 | 建议新增 `web-client`，React + TypeScript + Vite，适配浏览器与公开分享 |

KMP 共享不包括业务 Composable、Android Activity、UIKit 控件、导航栈对象和窗口布局。iOS 的 Compose 是 Kotlin UI，不是 SwiftUI；SwiftUI / UIKit 仅作宿主及系统控件桥接。各端保持相同任务、权限、状态与默认视觉，不要求页面代码共用。

## 依据与当前状态

- 手机：[2026-10-19 评审交接](../design/figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md)、[Figma 手机入口](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=238-371769)。579 个参考根含变体和覆盖层，不等于独立页面数。
- 跨端：[2026-10-20 浅色跨端交接](../design/figma-study/CROSS_PLATFORM_LIGHT_REVIEW_2026_10_20.md)、[Figma 总览](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=301-27)。Web18、Desktop17、平板21 为固定设计参考，不是已运行客户端。
- 服务：[服务端交接](../server/MOBILE_SERVER.md)、[模块化单体](../server/ARCHITECTURE.md)、[OpenAPI](../api/mobile-v2.openapi.json)。源码与文档存在不代表编译、数据库迁移或真实供应商联调已完成。
- 业务：[PRD](../bmad/prd.md)、[UX](../bmad/ux-design.md)、[认证规划](../design/MND_MOBILE_AUTH_PLAN.md)、[真实性与披露](../design/MND_TRUST_AND_DISCLOSURE_SPEC.md)。最新手机规则与服务端版本语义优先于旧摘要中的冲突表述。
- 工程视觉：[Stitch 对齐指南](../design/MND_UI_ALIGNMENT_GUIDE.md)仍描述旧客户端合同。实施默认手机样式时先通过任务 C02 同步视觉映射，不能混用旧 Honey 橙色和当前珊瑚色。

2026-10-20 只读基线：`J:/GolandProjects/kitty-circle`，分支 `main`，HEAD `ebfc469`；存在大量未提交和未跟踪的服务端、iOS、设计资料。后续接手必须重新核对，不能只检出 HEAD 后声称已拿到当前代码。

最初文档阶段未改客户端；随后授权的第一批源码与升级交付见实施记录。未运行编译，遵循全局不自行编译规则；应用测试、设备、SDK、数据库迁移均未验证。

历史文档阶段检查：6份新文档及7份相关入口／记忆，共13个文本文件通过UTF-8无BOM检查，113个本地文档链接有效，相关已跟踪差异空白检查通过。后续代码与配置的静态检查另见当前实施记录，不能用历史文档检查抵扣。
