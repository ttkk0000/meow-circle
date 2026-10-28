# M&D 客户端代码实施与完成标准

> **2026-10-28 现行入口**：[源码与文档基线](../CURRENT_BASELINE.md) · [产品／交付状态](../PRODUCT_AND_DELIVERY_MATRIX.md) · [全部文档目录](../DOCUMENTATION_CATALOG.md) · [Figma文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3)。当前255路径／288操作／141服务端schemas、143Web schemas、88对DTO、45张mobile表／13迁移、29份shared／41份公共UI文件、48个FeaturePage；这些是源码口径。本文带日期的旧数量和“待实施”结论仅适用于当时批次，运行／全量验收未完成。

2026-10-28：[后台按角色先设计后编码](ADMIN_ROLE_WORKSPACES_2026_10_28.md)补9个人员工作区、角色／页面与字段权限、18份Figma新稿和年龄／授权／实际收支入口；旧邀请码闭环见[检查点](BUSINESS_COLLABORATION_2026_10_28.md)。均未运行验收。

2026-10-27：[全量落地任务本轮恢复点](COMPLETE_LANDING_2026_10_27.md)补业务后台13页／双后台公共框架、预约暂停与取消状态、领养／协议内试养／交接／接回和私密照护；58份设计上下文与22项只读检查。源码和静态层已记录，740状态及各端运行验收没有升级为完成。

2026-10-26：[远端同步与线索提交补页](SYNC_PAGES_ADMIN_2026_10_26.md)补照片／完整检查／失败／收到四状态及版本化原请求，修正公共主入口遗漏；[业务后台管理13稿](../design/figma-study/admin-2026-10-26/README.md)已建于Figma，保留原技术运维。38公共UI／48分派，未编译或运行验收。

2026-10-26：[私密线索与目击者会话续作](PRIVATE_LEAD_FLOW_2026_10_26.md)补充列表／空态、详情、两类真实处理结果和线索独立会话，服务端持续核对父权限与关闭状态；37公共UI／47扩展分派，源码和静态验证不等于运行验收。

2026-10-26：[发布来源与分享进入续作](FIGMA_FLOW_CONTINUATION_2026_10_26.md)补齐7个来源状态、完整预览、独立声明与原请求恢复、iOS分享接入；36公共UI／45扩展分派，45表／13迁移，源码与静态边界见专项记录。

2026-10-25 用户要求核对并完善缺失页面：[本批页面与交互补齐](FIGMA_PAGE_GAPS_2026_10_25.md)已写源码，包含独立状态／权限说明、真实媒体进度与排序、通知安全定位；公共UI35份、45扩展分派。740索引继续保留未运行验收标记。

2026-10-25 用户暂停平台注册、先写代码：[SDK配置核对与公众号安全模式回调](SDK_OFFICIAL_ACCOUNT_CODE_2026_10_25.md)已补源码；真实AppID／Key继续占位，未编译或联调。

最新源码续作与待配置／待运行边界：[2026-10-25剩余编码交付](REMAINING_CODE_IMPLEMENTATION_2026_10_25.md)。iOS当前为Swift宿主＋Compose页面，不是SwiftUI页面。

2026-10-25：[当前部署与App打包运行入口](DELIVERY_GUIDE_2026_10_25.md)已补源码及静态核对，未执行部署／构建／运行。

2026-10-25 全栈代码逻辑总览与 PlantUML 架构图谱收口：新增[全栈代码逻辑与 PlantUML 架构总览手册](../CODE_LOGIC_AND_UML_REFERENCE.md)及[独立 PlantUML (`.puml`) 图谱目录（12 张图）](../uml/README.md)，完整梳理 `kmp/shared` 泛型 `MviStore` 体系（`MobileStore`、`MobileFeatureStore`、`SensitiveActionStore`、`ProviderAuthStore`、`AgeAssuranceStore`、`GuardianInviteStore`）、`kmp/clientUi`（`34` 个三端共享 Compose UI 文件、`42` 个 `FeaturePage` 二级业务页、`ClientUiPlatform` 端口）、Android/iOS/Desktop 宿主、Web 消费端与 `/app/operator` 独立运营台，以及 Go 服务端（`241` Paths / `271` Operations / `123` Schemas / `74` 对跨语言 DTO / `44` 张 `mobile_*` 表）的端到端代码逻辑与状态机。

2026-10-24全端公共页面进展见[专项记录](ALL_FIGMA_IMPLEMENTATION_2026_10_24.md)。新增独立 `kmp/clientUi` 公共页面库，Android／iOS／Desktop接同一套42功能入口及组件，Web独立DOM、运营三稿有独立权限入口。旧“苹果后置”不再作为本轮范围；源码／静态不代表740状态及平台全部验收。

2026-10-24地图专项：[高德原生／Web源码接入](AMAP_IMPLEMENTATION_2026_10_24.md)覆盖Android手机／平板、iPhone／iPad原生SDK和Web地图；Desktop通过浏览器复用Web入口。只对本次地图范围替代苹果后置。公开粗点与坐标系合同已同步，真实Key／许可、SDK解析、编译和运行验收尚未完成，用户仍要求不推送。

2026-10-25当前专用流程：[监护邀请与保护](GUARDIAN_PAGES_IMPLEMENTATION_2026_10_25.md)、[年龄核验](AGE_ASSURANCE_IMPLEMENTATION_2026_10_25.md)、[官方授权](PROVIDER_AUTH_IMPLEMENTATION_2026_10_25.md)。源码／静态边界分别记录，均未运行验收。

2026-10-24历史批次：Android手机／平板的Figma对齐、公共页面（含独立公开阅读与法规制度库 `AndroidReadingContent.kt` / `AndroidPublicationsContent.kt`）、宠物评论与反应、群目录及新Web消费端（`web-client/`，含 `ReadingPages.tsx` / `Publications.tsx`）见[本批说明](FIGMA_ANDROID_WEB_2026_10_24.md)、[740状态追踪](figma-implementation-trace-2026-10-24.json)及[Web接入](../../web-client/README.md)。服务端与产品同步状态见[PRD v2.1](../bmad/prd.md)、[UX v2.1](../bmad/ux-design.md)、[Figma产品对齐核查](../design/figma-study/FIGMA_PRODUCT_ALIGNMENT_CHECK_2026_10_24.md)与[服务端完整性审计（2026-10-24）](../server/SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md)（OpenAPI 扩至 `230` Paths / `259` Operations / `112` Schemas / `44` 张 `mobile_*` 表）。保留2026-10-23的日期／人员选择、分页、方向状态、深链、领养闭环与服务端作业。本批新增源码／静态结果另记，不把映射、历史证据或页面入口当作全量视觉／运行通过；此历史批次的iOS／Desktop未迁移结论已由上方公共UI续作替代，运行仍未验收。

2026-10-23最新：已接入[Android UI框架／10个基础组件与维护规范](UI_COMPONENTS.md)，主题与尺寸集中；[平板状态续作](PAD_STATE_AND_UI_2026_10_23.md)补主从列表／双列表单的可移动内容、日期与人员筛选、流程内输入状态和同意绑定。58份客户端源码与30扩展分派等静态通过，横竖屏／焦点／IME／设备仍未运行验收。只改Android入口，未迁移苹果或旧Web／Desktop。

2026-10-23最新续作：[Android手机／平板及服务器闭环补实](ANDROID_REMAINING_2026_10_23.md)已写人员／受众选择、文字区域／城市提醒、深链待查看、快照全部已读、公开分享与免注册安全网页，并复核领养边界。214路径／104schemas、55对DTO、55客户端文件／30分派等静态通过，运行未验收；下文“人员／区域／最小网页待写”是前批快照，当前事实查[交付账本](CURRENT_DELIVERY_STATUS_2026_10_23.md)。

2026-10-23闭环整改已接续：[设计／文档与领养源码跟进](../server/ADOPTION_CLOSURE_2026_10_23.md)、[当前交付账本](CURRENT_DELIVERY_STATUS_2026_10_23.md)。规范冲突已纠正，Figma五布局10处接回说明保存回读／2图核对；服务端先补独占／候补、申请周期和接回退出，shared／Android接新状态。仅源码／静态，人员与区域选择、HTTPS／游客网页、真实配置与运行仍按账本继续，苹果UI后置。

2026-10-23整体复审：[产品／业务／编码闭环报告](../bmad/PRODUCT_CLOSURE_AUDIT_2026_10_23.md)列出9项优先缺口、18类业务覆盖和14组待验收案例。产品主干较完整，当前不判整体完成；优先补F07宠物级独占／候补与接回退出，再完善Android用户选择、最小公开安全入口和当前交付账本。该报告为文档／代表性源码审查，不是运行或上线通过。

2026-10-23续作：[Android手机／平板读取状态](ANDROID_READ_STATES_2026_10_23.md)补刷新保留、分页原游标重试、空／末页、照片与贴纸比例；仅源码／静态检查，苹果UI后置，运行未验收。

2026-10-23最新优先级：**先安卓手机／平板页面，苹果页面本阶段后置**。[页面补齐与接线](ANDROID_PAGES_2026_10_23.md)继续五主入口／30扩展页，补日期／真实记录选择、分项进度、平板大字／键盘和原方法回执恢复；本批源码／静态通过，正式渠道和手机／平板运行未验收。下文“回执头待后续接入”等为此前快照。

2026-10-23服务端优先：[作业／回执／会话续作](../server/SERVER_FIRST_2026_10_23.md)已写源码和静态核对；本轮不扩客户端，新可选回执头仍需后续公共Transport持久化原键／载荷接入，运行未验收。

2026-10-22源码更新：[服务端/共享层实施记录](../server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)。本轮已实现身份资格、用途授权、幂等恢复及主要法律/业务合同源码，新增008迁移与精确公共KMP DTO/API；端侧UI与实际运行仍待验收。下文“只完善规划”和“B04缺失”均为上一批次的历史状态，不能再据其恢复旧自动建号或本地验证。

2026-10-22最新技术规划：[系统架构与数据／API合同](../bmad/architecture.md)、[实施准入核对](../bmad/gate-check.md)、[逐项追溯](../bmad/implementation-trace-2026-10-22.json)。实施方案已按最新设计改为S0–S7、任务扩B05–B12，优先补服务端准入、用途、幂等、资料清理及安全运营，再接真实业务切片；本轮只完善规划，没有新增业务代码或运行验收。

更新：2026-10-20。状态：Android / iOS 第一批共享逻辑、独立 Compose 手机 / 平板入口与依赖升级源码已写；完整功能尚未完成、未验收。当前事实见[实施记录](MOBILE_IMPLEMENTATION_2026_10_20.md)、[依赖基线](DEPENDENCY_BASELINE_2026_10_20.md)和[服务端接入核对](../server/CLIENT_READINESS_2026_10_20.md)。B04 认证合同缺失期间，对外提交保持阻塞。

本次用户决定：Android / iOS 基于 KMP 共用逻辑，各自使用 Compose UI 绘制页面，各自完成平板适配；Web、桌面采用下述工程方案。当前统一使用设计稿默认浅色主题，旧多主题保留参考与扩展位置，暂不开放切换。用户已授权移动端代码；Android 面向 Android 17 / API 37、最低 API 26。编码授权不包含构建或设备运行。

历史基线覆盖与本轮增补分别记录；当前入口见[安全修订交接](../design/figma-study/DESIGN_CLOSURE_2026_10_21.md)。2026-10-20历史覆盖：[完整跨端交接](../design/figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md) · [逐项映射](../design/figma-study/CROSS_PLATFORM_FULL_MAPPING.md) · [自适应契约](../design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)。569 个产品／交互状态各有 Web、Desktop、平板横竖屏对应稿，另补 32 张窄窗样例；布局与静态内容回读通过。复杂表情内部动作仍参考手机源原型，未完成跨端全量交互或设备验收。设计覆盖和本目录的客户端实施完成度分别记录，不能把画板状态当作代码已完成。

## 阅读顺序

| 文档 | 解决的问题 |
|---|---|
| [全栈代码逻辑与 PlantUML 手册](../CODE_LOGIC_AND_UML_REFERENCE.md) · [PlantUML 图谱索引](../uml/README.md) | 全栈架构、KMP MVI + `clientUi` 类图、Go 服务端领域类图、数据库 ER 图与 8 条核心业务时序/状态机 |
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
