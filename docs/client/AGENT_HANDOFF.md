# Codex / Gemini 客户端协作交接

家里继续优先读[2026-10-22换机交接](../ai/HOME_HANDOFF_2026_10_22.md)；本批源码／静态检查已收尾，未运行工程。下文有日期的旧基线保留作历史，不代表当前源码缺失。

2026-10-22代码接手优先读[本轮实施记录](../server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)。B04/B05/B12服务合同已写，资格从服务端读取；旧“恒拒绝/合同缺失”描述保留为历史，不再执行。禁止用本地开关放行，未知/不可用合同仍关闭。新增008迁移未执行；不重建现有iosUi，不扩大到端侧UI。

2026-10-22接手补充：先读[最新架构](../bmad/architecture.md)、[S0–S7顺序](IMPLEMENTATION_PLAN.md)、[任务B05–B12](TASKS.md)及[逐项追溯](../bmad/implementation-trace-2026-10-22.json)。最新45组法律稿已映射到真实命令和服务端缺口，原稿数量不代表接口已支持。iosUi／MndMobileUI与ComposeApp已经存在；不要按旧“规划新增”重新创建。当前没有向Gemini、其他会话或代理实际分派任务。

当前代码交接优先读[移动端实施记录](MOBILE_IMPLEMENTATION_2026_10_20.md)、[版本基线](DEPENDENCY_BASELINE_2026_10_20.md)及[服务端接入核对](../server/CLIENT_READINESS_2026_10_20.md)。新增 shared/client、Android mobile UI、iosUi 与 ComposeApp；第一批源码已写，完整功能未完成。此段原为2026-10-20基线；B04认证资格响应与后端写关卡已在2026-10-22补源码，真实资格配置、UI和运行仍未验收。不要为了演示解除 `requireInteractionContract()`；不要恢复旧手机号自动建号入口。

当前跨端设计入口：[完整范围与边界](../design/figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md) · [逐项节点映射](../design/figma-study/CROSS_PLATFORM_FULL_MAPPING.md) · [窗口与交互契约](../design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)。领取模块后以 `mobileKey` 查四种布局，不再用原 56 张代表稿推断完整页面。复杂表情动作以手机源原型与交接规则为依据，Figma 静态覆盖不能替代实际 UI 状态机和设备验收。

日期：2026-10-20。用途：代码与设计交接。本文最初为文档阶段生成，后续移动端已获编码授权；没有向 Gemini 或其他聊天发送任务，也没有启动子代理。构建和运行仍未授权。

## 1. 接手必须知道的决定

- Android / iOS 共用KMP逻辑，各自画Compose UI。Android用Jetpack Compose；iOS用Compose Multiplatform for iOS，独立`iosUi`已建、完整业务待补。旧SwiftUI保留作迁移参考。
- Android平板和iPad独立适配。布局／导航对象不放`shared/commonMain`；共享Store不能因两个面板而重复提交。
- Desktop继续Compose Desktop＋shared；Web建议独立React / TypeScript / Vite＋同一v2契约。
- 当前只用设计默认浅色主题。旧Honey／Mint／Night／Neutral保留参考与迁移信息，预留扩展，但不开放切换、不跟随系统黑夜、不把旧主题偏好套到新UI。
- 新业务用`/api/v2`，包装`ok/data/error`、轮换token pair、版本与幂等。现有v1 SDK、SwiftUI、Expo与桌面样例不能算新客户端完成。
- 只做当次用户授权范围。当前文档完成不等于已授权编码或运行，未来授权编码也不默认包含构建、测试、设备启动、提交或发布。

## 2. 最小必读

1. 当前工作树适用的AGENTS及全局协作规则，确认override和用户当次授权。
2. [客户端入口](README.md)、[实施方案](IMPLEMENTATION_PLAN.md)、[任务](TASKS.md)。
3. 对应F条目和[默认主题／平板／验收](DESIGN_AND_ACCEPTANCE.md)。
4. 对应真实源码、[v2服务交接](../server/MOBILE_SERVER.md)、[OpenAPI](../api/mobile-v2.openapi.json)。
5. 手机[评审交接](../design/figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md)、[全量跨端交接](../design/figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md)及[本轮安全修订](../design/figma-study/DESIGN_CLOSURE_2026_10_21.md)，按当前节点和页面定位；原56张为历史代表稿。

不要全量重读历史日志、重建579个Figma参考根或启动整个项目来“熟悉工程”。CodeGraph可用时先取所需结构；返回旧文件时回当前源码。

## 3. 当前交接基线

2026-10-20只读记录：`J:/GolandProjects/kitty-circle`，`main`，HEAD `ebfc469`。大量客户端框架、v2服务、OpenAPI和设计文件未提交／未跟踪；这份HEAD不包含全部当前工作。

接手时重新记录绝对路径、分支、HEAD、`git status --short`和工作树列表。不要自行切分支、拉取、合并、重置或复制覆盖来“同步”。新工作树不会自动继承未提交资料；只有确认任务基线一致后才继续。

当前已存在`kmp/iosUi` Compose模块和`kmp/iosApp/ComposeApp`薄宿主；旧SwiftUI保留作历史参考，不在新主目标内。第一批源码不代表完整功能或平台验收，具体见移动端实施记录。

## 4. 并行协作的文件所有权

| 文件／区域 | 修改负责人 | 其他参与者怎么做 |
|---|---|---|
| 全局Gradle、版本目录、模块build、settings | C01协调者 | 通过变更单提出依赖，不在各自任务里直接升级 |
| `iosApp/project.yml`、framework导出、Info.plist基础配置 | C01 / I01先后交接 | 系统能力需要新配置时提交清晰的键、理由和使用位置 |
| v2 DTO、Domain类型、共享Store合同 | 对应C/F当前负责人 | 使用冻结接口；缺字段报告，不各自复制一套类型 |
| OpenAPI、Go HTTP／权限／状态机 | 服务端负责人 | 客户端只提出具体用例和失败证据，不擅改服务器授权 |
| Android功能UI／adaptive | 当前A任务负责人 | iOS／Web不在此修改；公共素材通过合同同步 |
| iOS功能UI／adaptive | 当前I任务负责人 | 不把Android组件拷进来后只改包名 |
| Web / Desktop | 当前W / D任务负责人 | 不改对方入口或旧站资产，除非任务明确涉及迁移 |
| 默认主题合同和公共文档索引 | C02 / 协调者 | 平台本地修正需同步差异，不能暗改统一视觉 |

“合同冻结”指记录当前类／字段／签名、样例和状态语义供消费，不是创建一套新的远程登记服务。代码尚未实现的接口可先定义小型协议与fake实现，但必须写明未接真实服务；不得长期复制业务规则等待以后合并。

## 5. 公共接口变更单

```text
变更编号／提出方：
阻塞任务与实际用例：
当前签名／字段／版本：
最小新增或修改：
权限、可空性、错误和状态影响：
受影响消费者：Android / iOS / Desktop / Web
旧数据、旧会话和序列化迁移：
需要的服务端路径／供应商能力：
建议修改负责人：
合同生效版本与消费者升级顺序：
验证方式和未验证项：
```

例如iPad需要在主从间保留选择，应在共享层已有selectedId合同上扩展，而不是把iOS导航栈塞进共享State。浏览器大ID应改统一Transport／契约，而不是在一个页面里`String(number)`修补。

## 6. 可复制给Gemini的提示词

下面模板供用户后续明确授权代码任务时使用。本轮不执行模板。用户填入任务ID和可写目录；若只要求文档或分析，替换执行意图，不默认开始编码。

### 6.1 Android单功能任务

```text
请在 J:/GolandProjects/kitty-circle 完成客户端任务 A02 + F05（Android消息UI）；
这次授权对应源码工作，不授权自行构建、安装、测试、启动设备、提交或发布。

先读取适用AGENTS和全局规则，再读 docs/client/README.md、IMPLEMENTATION_PLAN.md、
TASKS.md、FEATURE_DELIVERY_MATRIX.md中的F05、DESIGN_AND_ACCEPTANCE.md。
重新核对工作树、分支、HEAD和未提交资料；不要用HEAD覆盖当前在途源码。

目标：消费已确认的共享Conversation Store和v2合同，用Android Jetpack Compose画手机消息UI。
只允许修改 androidApp 中该功能UI和本任务明确涉及的Android适配文件。
不修改shared公共协议、Gradle版本、iOS、Web或Go接口；接口不足先写最小变更单并继续独立工作。

主题只用设计默认浅色；旧四主题保留参考，不加切换、不跟随系统黑夜。
Android平板属于A04：本次如未包含A04，不得标平板已完成。
覆盖消息请求、发送／失败重试、真实未读、显式已读、草稿、表情和会话切换。
不能使用设计固定数字或demo fallback假装真实服务。

完成后按TASKS.md模板报告源码、静态检查、未运行构建和未验证的设备／服务内容。
不要擅自联系其他聊天或给另一个代理发任务。
```

### 6.2 iOS / iPad任务

```text
请完成 docs/client/TASKS.md 的 I02 + F07（iPhone Compose领养UI）。
若I01尚未提供可用宿主，请先核对并写清缺口，不另做一套SwiftUI业务实现。

读取适用AGENTS及客户端实施、交付矩阵、默认主题与验收文档；核对当前工作树。
Android与iOS共享KMP业务，iOS主页面必须在iosUi中用Compose Multiplatform实现；
iosApp保留薄宿主和必要原生系统桥接，不创建第二套Swift业务Store。
当前仅使用设计默认浅色，旧多主题只预留参考。

可写范围：本次领养功能的iosUi目录；公共协议、全局Gradle、Xcode配置变更先交协调者。
实现资料、申请摘要、补充／拒绝／撤回、试养／双方交接、私密近况及退出路径。
I04是独立iPad适配，若未授权或未执行，必须写未完成，不把iPhone拉宽算iPad通过。

本次只授权源码工作，不运行构建／测试／模拟器／设备，不提交或发布。
Windows静态检查不能写成macOS／Xcode或Apple SDK验证通过。
按任务交付模板报告，保留旧SwiftUI代码直到替代验证，不清理无关内容。
```

### 6.3 共享基础任务

```text
请先完成 docs/client/TASKS.md 的 C03、C04（v2合同与共享Transport），暂不写各端页面。
读取适用规则、现有MeowCircleSdk、SessionStore、服务交接和OpenAPI实际DTO。
新接口用ok/data/error；列出通用Envelope响应缺口，不生成Any后直接交给UI。
定义精确ID、版本、游标、HTTP错误、取消和重试边界，保留v1旧调用方兼容。
公共模型中不能出现Compose、Activity、UIKit、DOM或平台导航栈。
代码依当前任务授权推进；不自行运行构建／测试、安装、提交或修改服务端权限。
完成后交付冻结接口、示例输入输出、可写文件清单、静态证据和剩余集成缺口。
```

### 6.4 Web或Desktop任务的附加约束

Web：写`web-client`，依v2实际合同；大ID无损解析、Cookie／CSRF依赖和鉴权图片明确。
旧`web`设计资产与兼容站不整目录覆盖，不在localStorage持久化新长期refresh token。

Desktop：写现有`desktopApp`并复用shared。列清真实数据／静态样例，系统凭据库不可用时不明文落盘；窗口缩放和IME属于独立验收项。

## 7. 接收代码时的审查

1. 对照任务范围和允许目录检查差异，先保留其他在途修改。
2. 核对公共依赖／签名是否绕过变更单，调用方和生命周期是否完整。
3. 对照F功能检查权限、有效／待确认版本、幂等、私密媒体、取消、切账号和恢复。
4. 默认主题必须与设计一致；存在旧多主题代码不是缺陷，出现本期切换入口或自动黑夜则需修正。
5. 检查“已完成”对应的真实证据；静态通过、测试文件存在和运行通过分别记录。
6. 在用户已授权的范围内整合、审查和修复。未授权Git提交／推送时保留可评审差异，不自行提交。

出现未解决冲突、供应商缺失或无macOS时，记录具体影响并推进不受影响工作。不能靠跳过失败用例、放宽服务器校验、隐藏按钮或伪造数据使清单看起来完成。

## 8. 交接结果模板

```text
当前任务和完成等级：
工作树／分支／HEAD／合同版本：
变更文件与新入口：
公共接口变更及消费者：
手机／Android平板／iPad／Web／Desktop各自状态：
默认主题、旧多主题预留方式：
实际使用的后端版本／供应商能力：
静态检查：
实际构建、测试、设备、浏览器结果：
未执行时：未运行编译，遵循全局不自行编译规则；相关项未验证。
剩余任务ID与阻塞：
可继续的下一项：
```
