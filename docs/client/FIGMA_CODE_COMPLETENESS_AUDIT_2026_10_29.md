# Figma 对应代码完整性复核

2026-10-29（Asia/Shanghai），`AI-20261029-FIGMA-CODE-AUDIT`。工作树为 main／a6b3653，保留上批未提交设计文档与截图。本轮只读审查源码和 Figma，未修改业务代码或远端设计。

**结论：尚未全部完成。** 已有大量页面、业务接线与状态分支，但本次仍确认 4 处设计到代码的具体差异；全量逐状态／逐端验收也没有完成。不能将“748 条均有源码归属”或“40 张 Figma 稿细化完成”解释为代码全部按设计完成。

## 检查范围与证据

- 原 Figma 文件实时目录为 12 页；重新读取 `915:386983` 介绍协作工作区及 `930:3366` 年龄字段错误的高保真上下文和截图。五布局协作稿的最新动作／截图沿用[刚完成的设计细化证据](../design/figma-study/collaboration-refinement-2026-10-29/README.md)。没有再次逐帧渲染全部设计。
- 全量读取[748 条消费者追踪](figma-implementation-trace-2026-10-24.json)，包括 3,740 个五布局引用及 32 个窄屏补充引用。所有适用的源码路径均存在。Android／iOS／Desktop 各归属 24 个公共源码文件，Web 归属 28 个；这只是路径存在及归属检查。
- 当前公共 UI 有 43 份 Kotlin，Web 有 90 份 TS／TSX。CodeGraph 用于结构定位，补充读取被裁剪的当前源码；没有因为索引或函数名存在就认定功能验收完成。
- 后台另有角色首页／任务 18 稿、业务后台 13 稿、运维 14 稿及角色任务 39 状态；其 1 个评审索引不是产品页面。这些增量独立于 748 条消费者索引。3 个旧运营参考状态的原生源码为空是既定 Web 专属范围，不算漏掉原生页面。
- 数量、输入指纹、具体发现和新截图见[本轮证据](figma-code-audit-2026-10-29/evidence.json)。

| 追踪中的状态口径 | 条数 | 能证明什么 |
|---|---:|---|
| 仅映射到功能族源码，未逐状态视觉核对 | 670 | 能找到所属功能，不能据此判断每个状态完整实现 |
| 标有专用状态源码，运行未验收 | 65 | 有更具体的实现归属，仍需核对完整交互与平台呈现 |
| 外部配置与运行仍需验证 | 10 | 包括官方登录、年龄／监护与设备权限等边界，不代表 10 项都缺源码 |
| Web 运营参考专用源码，运行未验收 | 3 | 既定 Web 专属参考范围 |

748 条的 `runtime_verified` 均为 false。这表示没有已记录的逐状态运行验收，**不表示 748 项均无法运行**。`contract_gaps: 0` 也不是“UI 差异为 0”。本轮没有计算虚假的完成百分比。

## 确认的代码差异

### FC01 / P2：退出结果与不可用页的顶部返回未同步

最新稿要求退出照护、退出介绍、授权不可用的顶部返回进入本布局消息页。原生 [ConsumerApp.kt](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt#L137) 仍调用通用 `f.back()`；[MobileFeatureStore.kt](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileFeatureStore.kt#L209) 在退出结果存在时只清除 `invitationOutcome`，返回邀请详情，而非消息页。Web [App.tsx](../../web-client/src/App.tsx#L280) 仍固定调用 [router.back](../../web-client/src/core/router.ts#L54)，取决于历史或回社区。

底部“返回消息”已有部分正确代码，不能代替顶部动作。应只为这些终态／不可用页面接入指定消息目标，保留确认页及其他任务的原返回语义。本项是导航差异，不据此声称越权或缓存泄露。

### FC02 / P2：原生介绍协作没有落实固定底部主按钮

Figma 介绍协作工作区以“保存介绍与照片”为底部固定主操作。当前 [SocialPages.kt](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/SocialPages.kt#L156) 使用正文内 `FeatureButton("仅更新介绍与照片")`；[FeatureButton](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/FeaturePages.kt#L229) 直接绘制次按钮，没有注册到 [TaskColumn 固定操作区](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/PageComponents.kt#L29)。这一差异随公共 UI 影响 Android／iOS／Desktop 及平板布局。

保存、版本和照片逻辑已有，不能说整个功能未实现；仍需改为该任务的固定主操作，同时保留现有真实提交核对。系统照片选择器是平台适配，不要求把学习示例相册伪造为真实系统相册。

### FC03 / P2：Web 可新增照护页底栏仍是重新读取

Figma 可新增照护工作区的底部主操作为“记录私密照护近况”；只读状态才使用重新读取权限。当前 [CareCollaboration.tsx](../../web-client/src/features/CareCollaboration.tsx#L42) 的 `TaskPage.actions` 无条件放重新读取，而 `can_post=true` 的记录入口在[滚动正文](../../web-client/src/features/CareCollaboration.tsx#L61)。

新增路由与权限判断已有，差距是按状态区分固定底栏。应让可新增状态使用持久的记录入口，只读状态保留读取权限，并继续遵循当前核权与失败关闭规则。

### FC04 / P2：后台年龄字段错误没有落实字段错误态

实时读取的 [Figma 930:3366](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=930-3366) 使用年龄区间错误边框，并保留纠正入口。当前 [RoleTasks.tsx](../../web-client/src/operator/RoleTasks.tsx#L183) 已阻止空年龄进入核对，但年龄 [select](../../web-client/src/operator/RoleTasks.tsx#L425) 未按字段错误设置状态或错误关联，只在下方显示[全局 OpsNotice](../../web-client/src/operator/RoleTasks.tsx#L531)。

需要补年龄字段的错误状态、`aria-invalid` 与对应错误文本关联，保留摘要和服务端校验。本项不表示空年龄能提交，更不能通过默认成年来“修复”。

## 已有成果与保留边界

最新消息好友／case-watch 分类与偏好兼容、好友及群主转让待处理、指定关系跳转、Web 显式已读均已有源码。后台未知任务内的台账面板、独立人工核对确认以及授权 409 后重读版本也已补；本轮回读确认，并与最新源码证据的指纹一致，不把旧文档中的这些待办重新报成缺失。

协作邀请／退出的全页确认、真实响应核对、照护只读／失权收起等已有实现；Figma 固定学习编号不应写入生产代码。Figma 的横竖屏方向按钮与后台评审索引仅用于设计预览，不要求作为产品入口实现。

真实短信／邮件、官方账号、年龄／监护、地图与推送等环境配置和平台接入仍需联调。完整搜索接收端与运维发布／重启执行器按既定用户决定保留占位；本轮不将其改为自动补全任务，也不因存在界面就宣称真实执行器可用。

## 下一步与验证层次

先处理 FC01–FC04，再按状态补角色、版本、错误、权限、返回与宽窄布局的具体证据。其余 670 条需要逐项确认，不能仅批量改追踪状态为完成。

本轮完成的是全量索引完整性和重点源码／设计审查，不是 3,772 个设计引用的逐像素验收。**未运行编译，遵循全局不自行编译规则**；未类型检查、构建、测试、lint、启动应用／浏览器／设备或播放原型，未提交／推送。真实运行与完整平台验收仍未完成。
