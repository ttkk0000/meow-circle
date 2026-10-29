# 客户端详细实施方案

> **2026-10-29 现行入口**：[源码与文档基线](../CURRENT_BASELINE.md) · [产品／交付状态](../PRODUCT_AND_DELIVERY_MATRIX.md) · [全部文档目录](../DOCUMENTATION_CATALOG.md) · [Figma文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3)。当前数量统一查现行基线；本次核对模块、宿主、公共页面与浏览器适配源码，只修订文档，没有新增构建或运行结果。

当前工程方案：`shared/.../client`承载纯Kotlin业务与数据，独立`clientUi`承载三端公共Compose页面；Android、iOS、Desktop各自注入`ClientUiPlatform`并拥有宿主生命周期、真实窗口和系统能力桥接。Web已经是独立React DOM工程。公共UI选择与[决定](../ai/DECISIONS.md)一致，逐端功能、平台和运行验收仍需完成。

> **2026-10-24 历史批次**：服务端 `/api/v2`（230 Paths / 259 Operations / 112 Schemas，001–009 迁移）、`kmp/shared/.../client`、`kmp/androidApp/.../mobile` 以及独立 `web-client/`（React + TypeScript + Vite，支持 Cookie/CSRF 认证、无损 ID、公开阅读/法规目录与 740 布局追踪）已完成源码落地与静态对齐（详见[2026-10-24客户端说明](FIGMA_ANDROID_WEB_2026_10_24.md)与[服务端完整性审计](../server/SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md)）。该批次之后iOS和Desktop已接入公共`ConsumerApp`；当前平台接线见第2节，不能用当时阶段范围推断仍只有首批基础页。

2026-10-23当时闭环实施顺序：[现行合同](../design/MND_PRODUCT_CLOSURE_CONTRACT_2026_10_23.md)→[当前交付账本](CURRENT_DELIVERY_STATUS_2026_10_23.md)→服务端F07/B10/B12→公共KMP→Android手机／平板。苹果UI后置；最小游客安全网页不等待完整Web。部署与运营按[v2手册](../server/V2_RELEASE_RUNBOOK_2026_10_23.md)，本轮新增规则和修正记录见[领养实施](../server/ADOPTION_CLOSURE_2026_10_23.md)。

2026-10-22实施更新：[本轮服务端/公共KMP源码与静态证据](../server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)。S1基础及S2–S4主要服务命令已进入源码，原“B04尚缺”等为实施前发现。阶段出口仍要求真实配置、调度、UI接线和运行/设备证据，不因本轮源码变更改为上线通过。

建立：2026-10-22；当前方案修订：2026-10-29。本文定义现有源码的维护与交付要求，不是功能完成报告。功能条目见[交付矩阵](FEATURE_DELIVERY_MATRIX.md)，具体任务见[任务拆分](TASKS.md)。

2026-10-22规划输入曾核对45组法律设计、225布局及当时源码／OpenAPI，并形成[系统架构与数据／API合同](../bmad/architecture.md)、[逐项追溯](../bmad/implementation-trace-2026-10-22.json)及[实施准入核对](../bmad/gate-check.md)。后续以本文第12–13节的S0–S7为执行顺序，原C/F/A/I/W/D/B/V编号保留并扩B05–B12。现状是方案可用于补齐服务端，完整写流程／上线准入仍未通过；不重新创建已存在的移动端骨架。

移动端代码已有授权。初始读取事实见带日期实施交接；当前身份／资格／操作回执已有源码，真实配置及运行仍未验收，不再将B04整体称为尚缺。Android API 37／最低26及依赖钉定继续遵守[依赖基线](DEPENDENCY_BASELINE_2026_10_20.md)，不重建骨架或升级依赖。

## 1. 明确决定

1. Android、iOS与Desktop共用KMP业务、数据、会话、校验及可移植状态逻辑。
2. 业务Composable、默认主题与自适应页面放独立`clientUi`；`shared/commonMain`保持业务与数据职责。`androidApp`、`iosUi`、`desktopApp`提供宿主与平台桥接，不各自复制整套业务页面。
3. Android 使用 Jetpack Compose；iOS 使用 Compose Multiplatform for iOS。iOS 现有 SwiftUI 页面是迁移来源，目标主 UI 不继续由 SwiftUI 实现。
4. Android 平板和 iPad 分别适配，并使用各自窗口、输入、返回、焦点和系统能力。
5. Desktop延续Compose Desktop并复用`shared`与`clientUi`；Web使用现有独立React／TypeScript客户端，复用HTTP契约与用例语义，不依赖KMP JS／Wasm。
6. 所有新页面默认呈现当前 Figma 浅色样式。本期没有主题选择器，也不跟随系统自动进入黑夜模式。
7. Honey / Mint / Night / Neutral 保留历史参考与后续扩展能力，不删除历史偏好，也不把旧偏好自动套到新页面。
8. 最初先做文档；随后用户已授权移动端编码与最高可兼容稳定版本升级。运行、测试、安装、提交和发布仍须分别按当次授权处理。

选择 Web 独立实现，是为了适合浏览器 DOM、键盘、辅助技术、分享链接和现有 Go 静态托管；这会多维护一份 TypeScript 业务适配，但能通过契约和用例矩阵约束。Desktop 已有 Compose 工程，继续使用可减少另一套宿主和 API 层。

## 2. 当前源码基线与缺口

下表按2026-10-29实际源码核对；初始模块与迁移前事实继续保留在[2026-10-20实施记录](MOBILE_IMPLEMENTATION_2026_10_20.md)。模块、接口和页面源码存在不表示已构建或全量验收。

| 位置 | 当前已有事实 | 继续核对的范围 |
|---|---|---|
| `kmp/settings.gradle.kts` | 已包含shared、clientUi、iosUi、androidApp、desktopApp | 依赖解析及逐端编译尚未验证，不重复创建模块 |
| `kmp/shared/.../client` | v2 MobileApi、MviStore、MobileStore／MobileFeatureStore及专用认证、监护、年龄、用途与恢复模型 | 精确权限、并发、原请求恢复、持久化和真实环境验收 |
| `kmp/clientUi` | 现行基线列41份公共UI文件；ConsumerApp、ClientFeaturePages、默认主题和ClientUiPlatform | 740消费者状态没有逐端全量视觉／业务验收，不能按文件数判功能完成 |
| `androidApp/.../mobile` | AndroidMobileApp接ConsumerApp；AndroidClientPlatform提供系统与窗口能力 | 折叠安全区域、预测性返回、IME、权限和进程恢复逐端验收 |
| `kmp/iosUi`、`iosApp/ComposeApp` | IosMobileApp接ConsumerApp，IosClient管理共享API与Scene Store，Swift薄宿主承载MndMobileUI | Xcode／Native兼容、Scene、安全区、键盘、SDK、签名与设备验收 |
| `kmp/desktopApp/.../Main.kt` | 已创建MobileApi／MobileStore并接ConsumerApp，DesktopClientPlatform提供系统能力 | 各发布OS的窗口、输入法、秘密存储与包分发分别验收 |
| `web-client/` | React／TypeScript／Vite消费者页面及独立业务后台、技术运维入口；core负责HTTP、无损wire、会话、草稿与查询 | 浏览器Cookie／CSRF、跨标签会话、私密媒体、路由及无障碍运行验收 |
| `internal/platform/mobileapi/browser*.go` | `/api/web/v2/`的Cookie／CSRF与刷新源码、独立SPA静态挂载已存在 | 真实密钥、HTTPS／Origin、Cookie、轮换竞态与托管产物验收 |
| `web/`、`mobile/`、旧MeowApp／SwiftUI | 遗留v1客户端、Stitch参考与兼容资源保留 | 不混用旧theme／Envelope／存储键或演示回退，不把旧页当新版验收 |
| `internal/platform/mobileapi`、现行OpenAPI | 服务端路径／操作／schema与DTO数量见现行基线，001–013迁移源码存在 | 未编译、未迁移和未联调，按实际接口逐项验证 |

### 2.1 本地版本记录

历史初读版本为Gradle wrapper8.10.2，Kotlin2.0.21，AGP8.7.2，Compose Multiplatform1.7.3，Android Compose BOM2024.10.01，Ktor3.0.3，coroutines1.9.0，serialization1.7.3，JVM17；Android当时minSdk26 / compileSdk35 / targetSdk35，iOS最低17。该组合已被[依赖基线](DEPENDENCY_BASELINE_2026_10_20.md)及现有配置取代，不能按此历史记录回退；本次不再升级依赖或宣称当前组合已验证。

这些是仓库事实，不是推荐长期冻结的生产组合。现有Compose iOS工程按依赖基线维护；兼容性仍需获授权后在macOS／Xcode验证，不在实施任务中各自升级 Kotlin、CMP、AGP 或 Android BOM。Compose Compiler 插件与 Kotlin 插件需要一致，CMP 与 Jetpack Compose 的发行号也不是任意互换的；官方[兼容说明](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)应在任务 C01 复核。当前旧组合不能推断为新 iOS UI 已兼容。

本规划不擅自提高或降低最低系统、改签名、包名或供应商应用 ID。兼容性选择落入 C01 的版本表后由一个负责人统一修改 Gradle 文件。

## 3. 目录与依赖边界

维护现有`shared`业务模块与独立`clientUi`页面模块，不按早期规划再次搬成`core/data/domain/features`目录或新建平行客户端。当前主要入口如下：

```text
kmp/
  shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/
                            # v2 DTO/API、MVI Store、身份／用途／草稿／业务恢复
  shared/src/androidMain/    # Android引擎与平台数据端口
  shared/src/iosMain/        # iOS引擎与平台数据端口；旧Swift桥接保留
  shared/src/desktopMain/    # Desktop引擎与平台数据端口
  clientUi/src/commonMain/   # ConsumerApp、业务页面、主题、组件与平台UI端口
  clientUi/src/androidMain/  # Android的公共UI actual实现
  clientUi/src/iosMain/      # iOS的公共UI actual实现
  clientUi/src/desktopMain/  # Desktop的公共UI actual实现
  androidApp/               # Activity、AndroidClientPlatform、系统与折叠窗口桥接
  iosUi/                    # IosClient、IosMobileApp、UIKit／系统能力桥接
  iosApp/ComposeApp/         # Swift薄宿主、Scene、原生SDK配置与生命周期
  desktopApp/               # JVM窗口与DesktopClientPlatform
web-client/
  src/core/                 # v2 transport、wire、query、session、drafts与业务适配
  src/components/           # Web默认主题组件与表单／媒体／工作区
  src/features/             # 消费者React DOM页面
  src/operator/             # 业务后台，具名岗位与独立会话
  src/operations/           # 技术运维，精确环境与capabilities
web/                        # 旧站、公开安全入口与设计资产
```

### 3.1 依赖规则

| 消费方 | 可以依赖 | 不能依赖 |
|---|---|---|
| `shared/.../client` | Kotlin、协程、序列化、Ktor、纯业务模型与平台数据端口 | Activity、UIViewController、Compose UI、DOM、平台导航栈 |
| `clientUi` | shared的State／Intent／API语义、公共Compose组件与ClientUiPlatform接口 | 具体Activity、UIKit控制器、宿主内部可变状态 |
| `androidApp/.../mobile` | shared、clientUi、Android系统与SDK适配 | iOS／Desktop UI模块的内部实现 |
| `iosUi` | shared、clientUi、CMP／UIKit桥接与iOS SDK | androidApp、Android-only库、第二套Swift业务Store |
| `desktopApp` | shared、clientUi、JVM窗口及系统桥接 | Android／iOS宿主内部实现 |
| Web | HTTP契约、TypeScript wire与业务适配、DOM及同一语义Token／用例 | JVM／Kotlin Native二进制、复制服务端授权判断 |

`iosUi/commonMain`提供iOS壳和端口入口，公共业务UI来自`clientUi/commonMain`；`IosClient.makeViewController()`及UIKit interop留在`iosUi/iosMain`。AndroidMobileApp、IosMobileApp和Desktop Main均接`ConsumerApp`。平台回调只返回能力结果或触发共享Store意图，不另建业务真相源。

公共页面可以共享窗口布局策略；系统API、生命周期与安全区域由宿主隔离。不在一套页面内不断追加平台判断代替`ClientUiPlatform`；有真实平台差异时扩展小端口并分别验收。

## 4. 共享逻辑如何落地

### 4.1 分层对象

| 层 | 责任 | 示例 |
|---|---|---|
| DTO | 精确表达 v2 wire 字段与可空性 | `AppointmentRecordDto`、`TokenPairDto` |
| Domain | 各端一致的业务含义 | 宠物、受众范围、有效预约、待确认提案、材料结果 |
| Repository | 读取／命令与缓存策略 | `AdoptionRepository`、`ConversationRepository` |
| Use case | 一次可独立验证的用户操作 | 登录并恢复上下文、检查申请摘要、重新读取冲突版本 |
| Feature Store | 不可变 State、接收 Intent、调用用例、取消和去重 | `AuthStore`、`PetDetailStore`、`ConversationStore` |
| 平台端口 | 提供系统能力与生命周期适配 | SecretStore、MediaPicker、OAuthLauncher、LocationProvider |

名称为规划，不是现有类。不要把 HTTP `Record.data` 的任意 JSON 直接送到页面；Mapper 按资源类型解码，不认识的状态应显示保守状态并禁止关键提交。

### 4.2 状态所有权

共享 State 包含数据、加载阶段、表单内容、校验结果、提交进度、当前服务端版本、可执行动作和错误恢复信息。选中宠物 ID / 会话 ID 可作为可保存的语义状态。

各端本地管理像素／dp布局、滚动位置、焦点、输入法组合区、TextFieldValue / TextRange、UIKit selection、悬停和动画。业务文本需要保存，输入中的拼音不能被共享层回写反复覆盖。平台把编辑后的规范文本同步给 Store，提交前结束组合输入。

每个工作区只创建一个业务 Store。平板从列表变为主从或旋转时复用同一实例，不再次登录、重复加载、重复已读或重复提交。列表与详情可有独立 Store，但由同一 session / repository 作用域管理。

### 4.3 生命周期与事件

- 共享逻辑注入受控 CoroutineScope / dispatcher，明确关闭方法；Android ViewModel、iOS 宿主和 Desktop 窗口分别负责创建／释放，不在 commonMain 引用 `viewModelScope`。
- `CancellationException` 必须继续传播，不用 `runCatching` 把取消当普通网络错误或成功空态。请求 ID / session epoch 防止晚到结果写入新账号。
- 长期数据使用 StateFlow；重建后仍必须执行的导航、权限请求、任务返回采用有 ID 的待处理意图和确认消费机制。不能只依赖 replay=0 的事件流。
- 一个工作区只有一个平台副作用消费者。平板两个面板不能分别消费同一个 `receiveAsFlow`，导致导航、消息或系统授权随机被其中一个抢走。
- 重复点击、重复回调和前后台切换按 operation ID 去重；界面撤销加载不等于服务端命令已回滚。

### 4.4 平台能力端口

| 端口 | 共享层负责 | 平台实现负责 |
|---|---|---|
| SecretStore | token pair 原子替换、迁移标识 | Android Keystore 支持的加密存储、iOS Keychain、桌面系统凭据库 |
| Preferences / DraftStore | 账号隔离、schema 版本、冲突语义 | 持久化、原子写入、生命周期恢复；敏感数据加密 |
| OAuthLauncher | 交易状态、渠道、回调去重、返回任务 | 官方 SDK、系统授权面板、URL 回跳；不携带服务器 secret |
| MediaAccess | 队列、媒体 ID、顺序、scope 和重试键 | Picker、受控文件句柄、图像转换、流式读取与资源释放 |
| Permission / Location | 业务上为什么请求、区域订阅参数 | 系统权限、粗／精度位置、手动选城市与拒绝后的出口 |
| Notification | 类别偏好、站内计数、目标解析 | 系统权限、APNs／Android 渠道、设备 token 注册适配 |
| Link / Share | 站内目的地、隐私与登录续接 | Android Intent / App Link、iOS Universal Link、系统分享面板 |
| Clock / Connectivity | 过期判断、退避与用例时间 | 实际时钟、前后台／网络提示；网络提示不替代请求结果 |

端口以小接口和明确结果类型实现。避免通过一个任意字符串的 `PlatformBridge.execute()` 汇集所有能力，也不要求所有接口都写 expect / actual；可构造注入时优先注入。

## 5. v2 数据与 API 迁移合同

### 5.1 接入步骤

1. 读取 OpenAPI 与 `internal/platform/mobileapi/handler.go`、对应业务 DTO；记录响应实例、状态与字段缺口。
2. 建 v2 transport / DTO / Mapper，独立于旧 SDK。第一阶段可保留旧 `MeowCircleSdk` 门面，但禁止新流程通过 v1 写接口兜底。
3. 按功能切换 Repository，再由 Android、iOS、Desktop 共用；Web 对同一契约建 TypeScript Mapper。
4. 一次只切换一个明确入口。历史订单等只读入口标注来源；没有映射前不把旧申请转成新交接结果。
5. 删除旧入口前核对调用方、用户记录迁移与回退路径；不以重命名 package 代替协议迁移。

### 5.2 必须遵守的协议

| 问题 | 客户端实现要求 |
|---|---|
| 包装 | v2 使用 `ok/data/error`，HTTP 状态参与判断；不能按旧 `code == 0` 处理 |
| 响应类型 | 当前 OpenAPI 多处仍为通用 Envelope。先为实际用例补响应样例／精确 DTO；生成器产出 `Any` 不能算类型完成 |
| 错误 | 当前 `error` 是文本，稳定业务错误码不足；按 HTTP 分类与已知状态处理，不能匹配英文句子决定权限／流程 |
| ID | Kotlin使用`Long`；Web wire层用`parseWire`在进入Number前识别大整数为`bigint`，URL和显示时再规范转文本。请求与原操作持久化用同一`encodeWire`；禁止经`JSON.parse`／Number中转后再转字符串 |
| 分页 | cursor / limit / next_cursor，0 表示结束；空 items 但游标推进时继续；防止游标不动的死循环，取消时停止 |
| 时间 | 存储与 wire 时间按契约解析，展示转当地时区；预约显示时区，时间比较使用同一时基，不能按显示文本比较 |
| 金额 | 保留整数分与币种；不以浮点元累计费用，声明、必付项和总额分别展示 |
| 版本 | 更新携带读取到的 version；409 重新读取并保留本地草稿，展示差异后再决定，不自动确认新版本 |
| 幂等 | 同一逻辑重试复用消息／评论 `idempotency_key` 与媒体 `Idempotency-Key`；内容变更是新操作；未获支持的命令不假定天然幂等 |
| 点赞 | 提交目标 liked 值而非 toggle；失败恢复到服务端状态，不把连续点击和网络重试混为一次反转 |
| 已读 | GET 不修改已读；在活动会话且消息确实可见后显式提交 through_message_id，隐藏面板和预加载不提交 |
| 能力 | 渠道、上传限制和服务状态来自实际能力；缺能力显示不可用及替代入口，不自动改演示数据 |

401、403、404、409、429（含 Retry-After）、503 和用户取消要有不同恢复策略。当前源码取消可能返回408，正常客户端主动取消不必弹“网络出错”。5xx 不自动重发不可确认是否已生效的写操作。

### 5.3 会话与缓存

access / refresh 成对轮换。一个进程使用会话级单次刷新协调，等待者复用结果；刷新失败不能无限递归。刷新请求使用过期 access 与 refresh 的请求体契约，不要求一个仍有效的 Bearer。刷新后的原请求最多重放一次；有副作用的请求需要原幂等键或重新读状态核实。

会话状态至少区分游客、恢复中、有效登录、暂时无法验证、已过期／撤销。缓存头像和昵称不是有效凭据；当前 Android / iOS 的缓存用户回退不能直接沿用为已认证判断。

秘密凭据不再进入普通 Settings。旧 `meow.auth.token` / Web `meow_token` 属于历史凭据，不能当 v2 refresh token；保留非敏感偏好与允许迁移的草稿，要求重新登录，并完成一次性迁移标记。

账号切换和退出后取消请求、递增 epoch、清理私密内存／磁盘／图片缓存和待发送队列。公开缓存可以单独保留；私密缓存需按账号与用途分区。应用关闭后不可持久化明文身份证材料、验证码或 SDK 回调凭据。

服务器撤销好友／协作／群权限后，已取回的字节无法由服务端远程“收回”。客户端在失去访问时清除内存与本地引用；私密媒体遵守 no-store，避免把图片库默认磁盘缓存视为已满足权限撤回。

## 6. Android 实施

保留现有 `androidApp`，先在应用入口注入共享依赖。现有 Android MVI 可作生命周期外壳，业务恢复、校验、请求和状态迁到 shared；不要同时保留两套相互更新的业务 State。

公共功能页面和组件维护在`clientUi`，Android入口只收集宿主状态并连接系统能力；页面通过共享Store发起业务意图，不在Composable组合过程中直接发网络请求。主导航保留社区／领养／发布／消息／我的，互助为上下文入口，新用户不先选旧模式。

Android平板由AndroidClientPlatform／AndroidAdaptiveWindow提供可用窗口和折叠安全区域，`clientUi/ConsumerApp.kt`的公共窗口策略决定导航轨、主从与上下文栏。实际条件含宽高、字号与IME，见[视觉与验收](DESIGN_AND_ACCEPTANCE.md)；不得依据设备型号、屏幕英寸或单个`isTablet`判定。

照片选择、权限、推送、微信／QQ 回调、分享与返回在 Android 适配器完成；清楚区分系统返回、预测性返回适配、面板关闭与页面退出。进程恢复保存安全的目的地 ID、筛选和草稿引用，不序列化整个凭据或媒体流。

## 7. iOS / iPadOS 实施

### 7.1 新 UI 模块与薄宿主

现有`:iosUi`包含iOS arm64／simulator arm64，依赖shared与clientUi；静态framework名为`MndMobileUI`。继续核对平台系统能力与完整功能状态，保留现有名称、bundle ID及模块接线，不创建另一套宿主。

当前`IosClient.makeViewController()`返回承载Compose的UIViewController，调用IosMobileApp→ConsumerApp。`iosApp/ComposeApp`保留SwiftUI的UIViewControllerRepresentable薄宿主，不承载第二套业务导航与Store；源码文件名仍为`MainViewController.kt`。

`clientUi`负责公共页面与Compose业务导航；`iosUi`通过IosClient／IosClientPlatform负责Scene内Store、系统窗口与返回、Picker、日期、地图、授权及推送桥接；`iosApp`负责Swift宿主、Scene、原生SDK配置、应用链接与权限声明。系统能力通过真实UIKit／Swift接口接入。

当前Xcode源描述的预构建任务已指向 `:iosUi:embedAndSignAppleFrameworkForXcode`，framework路径为iosUi产物。后续核对资源、字体、导出符号和实际macOS/Xcode兼容；源描述已接线仍不证明宿主已构建或设备通过。

### 7.2 旧 SwiftUI 迁移顺序

1. 保留现有 `MndIOS/Features`、`Architecture` 和旧 Bridge，写明仅旧入口使用。
2. 维护已接入ConsumerApp的Compose根；后续获授权验证UIKit／SwiftUI宿主的生命周期、字体和Insets，不重建平行根。
3. 一个入口绑定一个业务 Store。Compose 页面接入后，其旧 Swift Store 不再同时订阅或发同类请求。
4. 按功能替换 SwiftUI 页面并核对原导航／任务返回；替代路径完成验证后，单独清理无调用旧代码。
5. 不在首次迁移直接删除整个 `iosApp`，也不把 `IosSdkBridge` 继续扩充成新业务的必经回调中转站。

### 7.3 iPad 独立工作

根据当前 Scene 内容区域和 Compose 约束测量布局，处理窗口缩放、分屏、Stage Manager、安全区、悬浮键盘和外接键盘。iPad与Android平板共享clientUi业务Composable与窗口策略，分别接入平台约束和系统API并独立验收。

选中对象、草稿和会话身份跨旋转保留；单页返回列表后仍定位原项目。手势返回不能与侧边面板、输入框和照片排序拖动冲突。多窗口如后续开放，凭据由进程级会话管理，窗口路由与编辑草稿隔离，不能两个 Scene 同时静默覆盖同一草稿。

iOS 构建、签名、Apple SDK 和设备验证必须在 macOS / Xcode 环境完成；Windows 静态审查不能替代。当前 iOS17 基线保留，官方最新示例的 API 和 Info.plist 要求需按锁定版本逐项核对。

## 8. Web 实施

### 8.1 工程与托管

维护现有`web-client/` React + TypeScript + Vite项目，继续保留`web/`旧站、设计板与兼容资源。`browser_static.go`已有`/app/` SPA挂载，缺产物明确503，API和缺失静态资源不落SPA fallback。本次不安装依赖或生成构建产物。

React负责页面组合与浏览器生命周期，现有`core/router.ts`与`core/query.ts`管理路由和读取状态，临时UI状态留在功能组件；维护现有查询与会话边界，不另建平行缓存或更换路由依赖。

Go 继续提供业务 API 与静态托管，不另建同业务 Node 服务。Vite 可与现有后端集成，见[官方后端集成说明](https://vite.dev/guide/backend-integration.html)。路径需区分新客户端、旧站和 `/api/*`；刷新任意新客户端深链接要返回正确入口，API 404 不能被 SPA fallback 吞掉。

### 8.2 浏览器认证与媒体

原生v2仍使用Bearer与body refresh；浏览器已有`browser.go`的`/api/web/v2/`适配层，提供封装会话Cookie、HttpOnly／SameSite与安全传输配置、刷新／注销及Origin／CSRF校验；Web core/api.ts通过该适配层访问受限资源。Cookie与密钥、实际HTTPS／Origin及轮换竞态仍未运行验收，不能把源码存在写成生产认证通过。

适配器不能把 ops/admin 凭据暴露给浏览器，不能根据客户端传来的 actor ID 代替认证。需要跨标签页的会话失效通知与刷新协调；多副本部署也必须核对轮换竞态，不能把进程内锁当分布式保证。

浏览器会话按现有session／CSRF接口恢复；不把长期refresh token另存localStorage，也不把旧Web token静默升级成v2会话。Cookie／配置不可用时展示实际受限状态，不切回旧v1认证。

`<img>` 无法随意设置 Bearer header。受限媒体须通过同源 Cookie 适配获取，或受控鉴权 fetch / Blob URL 显示；退出、失权和卸载时撤销 Blob URL。不得把 Bearer 写入图片 URL、分享链接、日志或缓存键。

### 8.3 公开访问与分享

Web 使用真实 URL 表达公开宠物／动态／案例，筛选及返回目标保留白名单；登录后重新校验资源，不自动提交。内容撤回、404、权限改变和离线分别呈现。

`public_web.go`已有受公开ACL约束的服务端分享HTML，输出允许公开的标题、摘要与预览图，并链接回App、Web及安全反馈。分享平台抓取、撤回／失权、缓存与真实链接续接仍需运行验收；Vite SPA本身不被当作SSR／SEO能力，私密内容不输出公开元数据。

桌面浏览器、平板浏览器和移动网页属于同一 Web 工程的响应式布局，不调用 Android / iOS UI。优先完成 390、744／834、1194、1440 的窗口与键盘布局，细则见验收文档。

## 9. Desktop 实施

继续使用`kmp/desktopApp`：当前`Main.kt`已创建MobileApi／MobileStore并注入DesktopClientPlatform，接入公共ConsumerApp。公共业务页面维护在clientUi，桌面窗口、文件、系统凭据和浏览器授权维护在desktopApp，不恢复旧静态工作区作为主入口。

桌面有常驻导航、列表／详情、会话／上下文及发布预览；布局看窗口可用宽度，缩窗时优先收起第三栏，保留返回与当前对象。快捷键需要菜单或可发现提示，输入框与中文输入法中不得拦截普通文字键；Enter / Shift+Enter 行为可配置但要一致。

桌面秘密使用系统凭据库，系统不支持时允许本次内存会话并说明无法持久登录，禁止明文文件兜底。文件选择和拖放走同一媒体规则。关闭有未同步编辑的窗口时明确本机草稿状态，后台轮询随窗口／前后台取消。

首期以当前 Windows 开发环境为主要桌面验收端，macOS / Linux 仅在明确列入发布范围且拥有对应构建与签名环境后验收。不能把一次 JVM 源码通过写成所有平台安装包通过。

## 10. 缓存、离线与富文本

### 10.1 草稿和上传

共享草稿 schema 至少包括 schemaVersion、accountId、draftId、场景、关联宠物／对象 ID、受众、正文、媒体队列、baseVersion、更新时间和同步状态。会话刷新、旋转和打开另一个页面不得清空未提交草稿。

图片按 `Selected / Preparing / Uploading / Uploaded / Failed / Removed` 记录，每项有稳定 client ID、上传幂等键、顺序和服务端媒体 ID。取消并不保证服务器没收到，恢复后先核对结果。选中临时 URI / 文件句柄需复制到受控草稿存储或持有合法读取权限，不能假设重启后仍有效。

现服务仅 JPEG / PNG、单张10MiB、20MP、关联最多9张。iOS HEIC 等由端侧提供可解释转换，拒绝转换失败；流式上传限制内存峰值，避免一次把9张全量读进公共 ByteArray。服务端去 EXIF 不替代客户端临时文件和权限管理。

离线只支持被明确允许的缓存读取和本机草稿编辑。不离线排队自动确认预约、交接、解绑、注销或提交私密材料。重新联网后读新版本，展示差异并等待用户意图；本机保存不等于跨端已同步。

### 10.2 表情与贴纸

目录来自 `expression-catalog` 和现有官方素材，保留许可证。128 表情作为完整 Unicode 序列，24 贴纸以稳定 ID 表达；不能把贴纸截图或本地文件路径直接当消息正文。

光标、选择区和 IME 属于各端编辑器；共享层负责序列、草稿与目录身份。删除遵守 grapheme cluster，覆盖 ZWJ、肤色、旗帜和组合字符，不能直接删除一个 UTF-16 code unit。连续插入不自动发送，收起面板保留内容，最近使用以实际已发送结果更新；收藏失败可恢复。

### 10.3 消息更新

当前源码没有可当作已部署能力的 WebSocket / SSE 消息通道。先用可取消、带游标的受控轮询与手动刷新；前台会话按实际用例设频率，后台退避。推送仅提示重新读取，不作为内容真相，也不把 queued 标成送达。

## 11. 已知前后端缺口

| 缺口 | 证据与处理 |
|---|---|
| 真实服务尚未验收 | v2 服务端未编译／迁移／供应商联调；客户端可以写适配与独立样例，但不能标联调完成 |
| 具体响应类型 | OpenAPI 许多成功响应只引用通用 Envelope；C03 补精确响应样例和 Decoder 测试计划，缺项回报服务端负责人 |
| Web int64验收 | wire仍为int64数字；core/wire.ts已有进入Number前的bigint解析与请求编码。大ID、路由及原请求持久化全链条需运行验证 |
| 浏览器Cookie／CSRF验收 | browser.go及Web core/api.ts已有适配源码；真实HTTPS／Origin、Cookie、密钥、并发刷新与跨标签失效未验收 |
| Web／Desktop 三方渠道 | capabilities 描述供应商配置，不足以证明移动应用凭据支持浏览器／桌面 OAuth；先交付真实支持的手机号／密码，未授权渠道不冒用移动 App ID |
| 游客安全／隐私报告验收 | `web/safety.html`／`safety.js`及`/api/v2/safety/*`已有独立免账号通道，按安全会话而非普通reviews写接口处理；限流、凭据恢复、证据、人工响应与清理仍需真实验证 |
| 系统推送 | APNs / Android 实际通道、设备注册与回执需服务端端口和供应商配置；站内通知先交付，系统推送单列阻塞 |
| 地图与定位 | 来源、地图供应商、坐标精度与真数据未联调；列表与手动城市可先做，不能把设计图当可导航地图 |
| 注销与材料保留 | 只能显示实际服务端处理阶段，不能把匿名化／queued 写成已物理清除全部数据 |

上述缺口属于明确的集成任务与发布门槛，不授权客户端作者自行修改服务端安全规则。

## 12. 实施顺序与完成分级

下列阶段是交付依赖，不是已执行任务或日历承诺；各阶段只领取一个明确切片，负责人实际认领后登记。

| 阶段 | 范围与原任务 | 必须交付的出口 | 依赖与开放边界 |
|---|---|---|---|
| S0 合同盘点与基础冻结 | C01/C03；B11基础规则／ops身份；B12请求协议；B04/B05输入／响应设计 | 当前源码／OpenAPI差异表，精确DTO和错误、状态／权限矩阵、operation与用途grant样例、DDL／迁移设计、命名负责人及待定事实清单 | 现有游客／读取／草稿可继续；拟议API未写入前不称冻结；不升级工具链 |
| S1 服务端身份与一致性基础 | B04、B05、B12基础；C04–C06、F00/F01；A01/I01既有壳补齐 | 三标识冲突处理、当前手机资格、年龄／监护、用途grant、同键同结果与未知结果查询、会话／账号隔离 | S0；每个命令服务端重核；只有真实合同接通并验证后才能替换客户端恒拒绝拦截 |
| S2 第一条内容闭环 | B06；F02/F10、F04必要宠物关联、C07；Android/iOS手机与A04/I04同切片 | 读取→编辑草稿→上传／来源处理→预览／资格→明确提交→原请求核对→回执；负例保留输入，转屏不重复提交 | S1；写流程开发／验证与公开开放分开，不能绕过S3安全和资料出口 |
| S3 权利、安全与运营闭环 | B02、B08、B09、B11；B12作业；F13；W01/W03最小公开安全／分享入口 | 游客报告、通知／声明／原案补充、正式注销／协助分流、字段清理／hold／备份限制、命名操作员及升级、可核对的作业结果 | 邀请试点的必要前置；不必等待完整Web应用或系统推送；旧擦除事件先适配再消费 |
| S4 领养、商业与双城准入 | B07/B10；F07/F08/F09/F14与相应平台UI | 来源／城市规则、领养／出售条款、双版本确认、试养／安全接回、预约hold／取消、费用与到访反馈 | S1/S3；对应物种／城市／行为及人员／模板未就绪时关闭该类写入口；不实现支付担保 |
| S5 完整功能扩展 | F03/F04/F05/F06/F09/F11/F12/F14剩余范围；B03及平台系统能力 | 好友／圈子、协作、消息与群管理、走失交接、贴纸、真实未读／待处理、通知／推送等逐项完整交付 | 复用已冻结服务合同；R1/R2按PRD优先级，暂缓能力显式不可用，任务不删除；先站内后真实推送 |
| S6 完整Web／Desktop | W01–W04、D01–D03、B01 | Web无损ID、Cookie／CSRF／跨标签会话、键盘／辅助技术；Desktop共享Store、系统秘密存储与窗口恢复 | 依赖相应功能合同，无需等待所有手机页面；S3必要网页已先交付，不在此拖延安全出口 |
| S7 分端验收与试点放行 | V01–V03及对应A/I/W/D任务，运营／运维 | 获授权后完成构建／迁移／合同／真实通道、故障／删除恢复、无障碍与各平台验收，收齐真实资质、值守与回退证据 | 未验证的端或功能单独关闭；测试通过不自动授予发布授权；有负责人和证据才邀请试点 |

Web 工程与各端样式可以在契约冻结后并行写；上述依赖是功能和验证顺序，不要求等所有手机界面结束才创建 Web。

代码状态分为：待实施、源码已写、静态审查通过、构建通过、接口联调通过、设备／平台验收通过。只有功能、异常、迁移、隐私及目标平台检查满足，才称该范围“完成”；截图、Figma、JSON 解析或存在测试文件均不能替代后续等级。

任何工程命令、测试、设备启动、提交、推送和发布按用户当次授权执行。本文没有默认授予这些权限，也不要求因无构建授权而停止已经授权的文档和源码工作。

## 13. 当前续作与估算

先从现行交付矩阵选一个仍缺源码或验收证据的切片，核对既有C03／B04／B05／B12合同与实际调用，不重做已有身份、幂等、浏览器或UI骨架。记录当前请求响应、允许与拒绝动作、错误分类、身份／用途／版本绑定、同键重试与结果查询、事务与作业责任、迁移兼容及权限变化恢复；消费者与ops示例分别列出。真实人员与环境须独立确认，不能用角色名当已分派。

“当前手机资格＋单次用途授权＋社区发布operation”保留为贯通验收候选；先读已有实现与恢复合同，验证码、草稿、媒体和发布仍受服务端真实能力与资格控制。若后续获得运行授权，应在同一功能内分别核对Android手机／平板、iPhone／iPad与Desktop的公共页面和平台桥接，以及Web的独立DOM与会话；Figma示例回执不填入真实State。

每个任务的完成证据必须列出源码／DTO与路由、数据库唯一性／事务与权限、正常与负例场景、未知结果和回退、五布局对应状态、运行及平台证据。只有源码与静态证据时标“源码已写／静态通过”，不整项勾完成；没有实际运行授权时保留清晰的待执行列表。

工期先依据首条切片的实际工作量、供应商与人员可用性估算，再按服务端、共享层、Android/iOS与平板、Web/Desktop、运营／运维拆人日和依赖；日期在负责人认领后填写。当前不承诺全量X周完成，也不把需要供应商或运营事实的等待时间计成已执行开发。详细模型与接口见[架构](../bmad/architecture.md)，每项设计到任务的映射见[追溯JSON](../bmad/implementation-trace-2026-10-22.json)。
