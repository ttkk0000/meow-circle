# Figma 缺口与分端派工清单

盘点日期 2026-11-19（UTC+08:00）。源码起点 `main / f9a2dab`，工作树起点干净。优先级和分工是本次派工建议，负责人留空，由派工人填写。任务 ID 沿用[客户端原表](TASKS.md)，同一 ID 下以本文的任务标题和端区分领取范围。

## 七项派工复核 · 2026-11-19

本轮核对 `main / b0e49ab` 的七个派工提交，并在同一工作树修复 Web 与 KMP 的注销回执恢复问题。**7 项源码／静态均已签收，运行验收待完成。** 下表的签收范围以对应子任务为准。跨端注销、Android 平板全量、后台 39 态与运维 14 稿等父项继续待验收。

| 任务 | 源码提交 | 本轮复核状态 | 验收余项 |
|---|---|---|---|
| Web 评论锚点底栏 | `3ce4b43` | 源码与静态已签收。编辑器跟随锚点读回及权限，重读清除旧拒绝缓存。[入口](../../web-client/src/features/ResourcePage.tsx#L154) | 浏览器四态、焦点、途中失权与恢复 |
| 原生共享 Compose 注销五态 | `f3a03e5` | 源码与静态已签收。入口、五态、受限回执匹配及返回已接线。[页面](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountDeletionPages.kt#L80) | Android、iPhone/iPad、Desktop 分开验收。第三方／邮箱用途验证与部分进度明细仍受合同限制 |
| Web 后台可信年龄区间 | `307f41f` | 源码与静态已签收。确认守卫、明确失败、未知处理、换岗清理及同岗恢复已有分支。[任务逻辑](../../web-client/src/operator/RoleTasks.tsx#L104) | 实际岗位授权、输入与撤权并发。发布授权、收款、退款另有子项 |
| KMP 注销原回执恢复 | `83f0e87` ＋工作树修复 | **源码与静态已签收。** 保存回执和退出本机会话成功后再释放原请求关联。[恢复调用](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileApi.kt#L402) | 本机持久化故障、换号／同账号重新登录及跨进程情形。回归用例已写，尚未执行 |
| Web 运维发布核对 | `76dc261` | 源码与静态已签收。五条路径有入口、环境／角色守卫和前置检查，发布／重启执行器仍为占位。[发布确认](../../web-client/src/operations/DetailPages.tsx#L436) | 未连接、过期授权、预检失败、版本更新的浏览器／联调验收 |
| Android 平板／折叠屏宿主 | `661491b` | 宿主源码与静态已签收。折痕 Insets、系统回跳及评论锚点恢复有源码。共用 Android UI 检查仍受既有断言阻塞。[窗口](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidAdaptiveWindow.kt#L28) | 宿主子项记录的跨层缺口与折叠、旋转、分屏、IME、真实设备验收 |
| Web 消费端注销五态 | `b0e49ab` ＋工作树修复 | **源码与静态已签收。** 按原请求及受限凭证核对，404 保持待核对，注销说明留在账号草稿。[回执核对](../../web-client/src/features/AccountDeletionPages.tsx#L287) | 五态浏览器走查、切换账号、跨标签页及受限凭证恢复 |

**修复与复核记录**

- **KMP。** 回执或退出写入失败时保留原请求与同代会话关联。安全存储写入成功后，在会话流通知退出前交付回执。新增回归用例源码，覆盖直接提交后分别在两处写入失败、首次核对退出写入再失败，以及仅靠原编号核对恢复。 [会话持久化](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileApi.kt#L156) · [原请求恢复](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileApi.kt#L402) · [回归用例](../../kmp/shared/src/commonTest/kotlin/com/ttkk0000/meowcircle/client/OperationRecoveryTest.kt#L162)
- **Web。** 以原请求编号、凭证和已知回执 ID 核对真实注销状态。退出要求本页仍能确认当前账号及浏览器会话就是发起请求时的会话；刷新导致关联丢失时走只读回执核对。404 继续保留原请求与提交锁。匿名能力记录只保存查询需要的字段，读取旧注销记录时清理遗留说明快照。 [受限记录与原请求](../../web-client/src/features/AccountDeletionPages.tsx#L250) · [编辑页核对](../../web-client/src/features/WorkflowEditor.tsx#L1595) · [恢复列表](../../web-client/src/features/Recovery.tsx#L263)

初轮 10 个只读检查有 9 个通过，Android UI 检查仍在既有 `Mnd*` 名称白名单中止（`scripts/check_android_ui_static.py:54`）。整改后重跑 Web、页面缺口、KMP、公共 UI 和服务端／共享层合同共 5 项只读检查，均通过。3 份 TSX 与新增 Kotlin 回归用例解析通过，`MobileApi.kt` 的解析器诊断与 HEAD 一致。未运行编译，遵循全局不自行编译规则。单测、浏览器、真实服务及设备未验证。

## 1. 先看哪些真有缺口

| 优先级 | 页面／功能 | 当前证据 | 余项 |
|---|---|---|---|
| P0 · 合同缺失 | 账号暂时受限 `authRestricted` | 原稿明确区分「部分操作不可用」「等待复核」「继续浏览／处理与申诉」。现行 identity 的 `active()` 对非 active 返回未授权，消费者台账仍标合同缺失。 | 产品／后端先定可读范围、受限动作、复核出口和服务端强制校验。再接共享 Compose 与 Web 页面。保持手机号／年龄资格和账号处置原因各自独立。[Figma 手机](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=238-371744) · [Web](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=311-379980) · [服务端](../../internal/identity/service.go#L124) |
| P0 · 专用状态待补或逐项核对 | 注销影响／用途验证／处理中／处理结果／暂未完成；`authRevoked`、`authReturnUnavailable` | 双端已有资料请求、用途确认、受理后退出、受限查询、登录返回预览。本轮核对到这些设计状态仍只有功能族映射，通用返回流程已读取详情，但未逐项证明设计中的专用结果和恢复。 | 逐个触发并记录真实路由、角色和错误，缺哪个补哪个。对端撤销、主动退出、安全变更、普通过期按服务端可证实原因表现。[注销原稿](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=238-371757) · [原内容不可访问](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=238-371764) · [批次 020](FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md#批次020旧号验证新号填写与交流前验证) |
| P0 · Web 可见差异 | 通知跳精确评论，四状态 | Go、Compose 与 Web 均已有目标、版本和锚点；原稿不可查看／读取失败两态收起底栏，Compose 已条件渲染，Web `NotificationCommentPage` 仍无条件传入 `Composer`，只控制 `canSend`。 | Web 对齐底栏和焦点，保留原位重读及返回消息中心。两端跑四态与失权恢复。额外四稿独立追踪。[四状态](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=958-371820) · [Web 源码](../../web-client/src/features/ResourcePage.tsx#L135) · [现行通知实现](NOTIFICATION_ALIGNMENT_2026_11_06.md) |
| P1 · 逐态核对 | 消费者其余状态、业务后台与技术运维 | 公共页面、路由及大量业务分支已写入。消费者台账 614 条只映射到功能族，全表 748 条运行标记为 false；原 Figma 中后台多套稿另计。 | 逐端沿真实入口对账。验收队列按第 3–5 节领取；发现的源码缺口补实现，已有分支回填证据。 |
| 受阻 · 环境／决定 | 正式登录、年龄核验、地图、推送、数据库和上线；完整搜索接收端与运维发布／重启 | 现有接线、错误态和部分服务端接口可审查。供应商实配与运行待验证；完整搜索及运维执行器是已确认的占位。 | 见第 5 节。运营实际收支登记与消费者支付分开，支付、担保、赔付方案仍待决定。[交付边界](../PRODUCT_AND_DELIVERY_MATRIX.md) |

之前的 FC01–FC04 收尾、发布按钮、通知偏好和 `authKnown` 已有后续源码。派工时按现有实现核对终态／失权返回、介绍／照护固定操作、后台年龄字段错误、各尺寸发布按钮、已有进程内合法登录记录和清除逻辑。[实施批次 001–020](FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md) · [现行 Compose 发布入口](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt#L130)

## 2. 设计来源与勾选办法

本轮连接 Figma 回读原设计和文档中心各 12 个顶层页。把本地状态台账与四个现行消费者原稿页逐 ID 核对，3,772 个唯一画板均存在、位于对应页且宽高有效。还查看了账号暂时受限、评论四态、财务任务内台账三张原稿截图，并回读注销、返回失效和岗位撤回的节点。全量核对到画板存在与位置，代码结论来自重点路径静态对照，后续逐态验收按本清单执行。

| 客户端 | 使用台账的设计列 | 原稿入口 |
|---|---|---|
| Android 手机 | `phone`，748 个参考根 | [手机](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=56-25) |
| Android 平板／折叠 | `tabletP`／`tabletL` 各 748，`tabletN` 16 个窄窗样例 | [平板](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=403-3) |
| iPhone | `phone`，同一手机原稿，用 iOS 的宿主交互验收 | [手机](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=56-25) |
| iPad | `tabletP`／`tabletL`／`tabletN`，独立记录 iPad 结果 | [平板](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=403-3) |
| Web 消费端 | `web` 748，`webN` 16 个窄窗样例 | [Web](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=403-2) |
| 原生 Desktop | `desktop`，748 个参考根 | [桌面](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=403-4) |
| Web 后台 | 业务 13 稿，岗位 18 稿，任务 39 状态和 1 张索引，技术运维 14 稿 | [业务](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=843-21) · [岗位](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=888-1402) · [任务索引](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=932-2803) · [运维](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=793-21) |

[原逐状态索引（含对应源码和设计节点）](figma-implementation-trace-2026-10-24.json) · [Figma 跨端索引](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=403-5) · [文档中心 10 月 30 日增量](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3?node-id=35-19)。3 个 `operator-reference` 由 Web 后台负责，原生记「不适用」。协作增量 8 状态已在 748 内；评论四状态评审、独立后台、历史页及组件页按各自来源记账。文档中心 35:15 仍留「目标版本／回复锚点仍未提供」旧句，当前源码以 11 月 6 日实现为准。

**领取与签收。** 从第 3–5 节复制一项派给一个 AI，在该项后填负责人、状态和证据链接。状态用「未领／进行中／待验收／受阻／完成」，可完成的范围仍继续推进。若一个包过大，按台账 `family + state key + 客户端` 拆分；原任务 ID 保留。功能包的 `[x]` 表示该项写明的交付完成且证据已挂上。缺运行授权时，界面源码与静态可以分别签收，运行框保留空白；同一画板即使由多个平台复用，平台证据仍各填一次。

```text
任务 ID / 本文标题 / 客户端和布局：
负责人 / 状态 / 分支和 HEAD / 最近更新时间：
范围：台账 family、state key、Figma 节点或后台任务键
允许写入的文件：
证据：入口 → 触发条件 → 实际结果，附源码行或补丁
逐态回填：每条 state key × 客户端列出节点／路由／角色／预期／实际／证据
[ ] 源码／状态接线对照完成
[ ] 静态审查完成
[ ] 目标端视觉／键鼠或系统交互验收通过
[ ] 真实业务与失败恢复验收通过
依赖／未验证项／下一步：
```

没有代码差异的任务用源码行和触发条件签收源码对照。没有适用场景的格写「不适用」及依据。每个包至少覆盖可以触达的入口、加载／空、失败／重试、受限／失权、已提交／结果未知、返回与刷新中实际适用的分支。截图记录窗口、状态、版本和角色，敏感材料脱敏；运行签收附环境及实际结果。任务开始与结束按 [AI 跟踪流程](../ai/README.md) 登记和释放。

## 3. 公共源码与产品功能任务

共享 Compose 的业务页只实现一份，由 Android、iOS 和 Desktop 各宿主验收。Web 消费端使用独立 DOM。后端负责人统一维护 identity、OpenAPI、DTO 与幂等合同；只有合同受影响时才改共享文件。下面的 F 项同时给分端任务提供页单，按每包选定的 `family` 枚举所有原始状态键。[共享 UI 入口](../../kmp/clientUi/README.md) · [Web 入口](../../web-client/README.md)

### 第一批 · 明确的缺口与安全返回

- [ ] **F01／B04／B05 · 账号暂时受限。** 后端／产品先评审真实限制范围、复核状态、用户可读摘要和申诉入口，确认是否允许保留当前登录。认证 API、共享 Compose、Web 再分别接线。用允许浏览／被禁动作、撤销限制、不能取得摘要三条路径核对；禁止用普通年龄或手机号异常硬凑。阻塞项先回填需要的决定及负责方。
- [ ] **F01／F13／B08 · 账号注销五态。** 从「账号与登录方式→资料删除、注销与协助」进入。用 `authDeleteImpact`、`authDeleteVerify`、`authDeleteProcessing`、`authDeleteStatus`、`authDeleteFailure` 逐态检查。复用现有资料请求和用途级授权。明确勾选影响、受理后退出、仅持受限查询看进度、分项处理／必要保全、失败复用原申请。区分提交结果未知与已经受理；回执号和已完成结论来自服务端。[原生入口](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/FeaturePages.kt) · [Web 接线](../../web-client/src/features/WorkflowEditor.tsx)
  - [x] **原生共享 Compose（`kmp/clientUi`）子项：F01／F13／B08 账号注销五态。**（父项及 Web、Android／iOS／Desktop 编译与设备验收仍待签收）
    - 负责人 / 完成层级 / HEAD：Antigravity (Gemini) / 源码／状态接线对照 ＋ 静态审查完成（未编译、未运行测试或真机验证，跨端父项保留 `[ ]`） / `main` @ `f9a2dab`
    - 入口与外壳协调：[`AccountPages.kt#L63-L133,L221-L248`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountPages.kt#L63-L133)（`FeaturePage.DataRequests` 分流 `close_account` 与受限回执查询／加载／失败视图，`RestrictedResults` 在 `Recovery` 遇 `close_account` 回执提供「查看注销结果详情」跳 `kind = "status"`）、[`FeaturePages.kt#L17-L30`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/FeaturePages.kt#L17-L30)（敏感流程全屏分派至 `AccountDeletionSensitivePage`）、[`ReadingPages.kt#L17-L86`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ReadingPages.kt#L17-L86)（`featureTitle` 随五态动态同步标题）、[`ConsumerApp.kt#L52-L133,L202-L214,L272-L276`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt#L52-L133)（`DATA_REQUEST_UI_SCOPE` 保留、`handleAccountDeletionBack()` 步进返回／忙锁及 `MobileShell` 与 `ContextPane` 标题联动）；原设计节点：Phone `238:371757`/`371759`/`371760`/`371761`/`371762`，Desktop `311:380033`/`380041`/`380045`/`380049`/`380053`，TabletP `311:380034`/`380042`/`380046`/`380050`/`380054`，TabletL `311:380035`/`380043`/`380047`/`380051`/`380055`。
    - 五态源码路由与行号：
      - 态 1 `authDeleteImpact`（注销账号前 ＋ 查看未完成事项）：[`AccountDeletionPages.kt#L187-L294`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountDeletionPages.kt#L187-L294)（`LoadingScope` 范围核对、按 `adoptions`/`appointments`/`reviews`/`cases`/`pets` 读取真实待结事项且不自动结案、`impactAgreed` 显式勾选）。
      - 态 2 `authDeleteVerify`（确认本人操作 ＋ 二次确认提交）：[`AccountDeletionPages.kt#L296-L489`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountDeletionPages.kt#L296-L489)（`purpose = "reauth"` 短信验证码或当前密码用途验证，验证通过进入 `ReadyToSubmit` 后仍需勾选 `finalConfirmed` 方可提交）。
      - 态 3 `authDeleteProcessing`（注销申请受理中 ＋ 受理后退出 ＋ 分项／保全进度）：[`AccountDeletionPages.kt#L491-L531,L641-L660,L722-L759`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountDeletionPages.kt#L491-L531)（由 `Complete` 或真实 `DataRequestStatus` 驱动；区分 `owner == null` 已退出与 `owner != null` 待退出；展示 `erased`/`pending`/`held`/`physicalPending`/`providerPending`/`restrictedCopies`/`pendingFields`/`externalState` 与下次复核时间；受理不显示为已全部删除）。
      - 态 4 `authDeleteStatus`（注销处理结果 ／ 受限副本保留）：[`AccountDeletionPages.kt#L668-L720`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountDeletionPages.kt#L668-L720)（仅由受限查询真实服务端 `DataRequestStatus` 进入 `DataRequestPhase.Complete` 与 `RetainedCopies`；支持重新核对、查看其他受限进度及承认会话结束返回公开浏览）。
      - 态 5 `authDeleteFailure`（注销暂未完成 ／ 结果未知 ／ 原申请恢复 ／ 受限查询失败）：[`AccountDeletionPages.kt#L533-L636,L761-L804`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountDeletionPages.kt#L533-L636)（区分 `ResultUnknown` 调用 `f.sensitive.reconcile()`、`Failed` 凭证失效引导查原回执与保留 `clientRequestId`+`scope` 复用原申请、及 `AccountDeletionLookupFailureCard` 保留原 `RestrictedReference` 安全重查，不重复建单）。
    - 阻塞项（需改动本次只读的 `kmp/shared` 或 Go API 合同）：
      1. `OAuth／邮箱方式用途验证合同缺失`：`SensitiveActionStore.verify` 与服务端 `POST /api/v2/auth/reauth` 仅接受密码或已绑定手机号 `reauth` OTP，尚无邮箱 OTP 或微信／QQ／Apple 外部重认证合同；UI 已通过 `LaunchedEffect` 补充 `identityMethods()` 读取并在存在第三方关联方式时明示限制。
      2. `ResultUnknown 对账确认后的本地会话同步缺失`：`SensitiveActionStore.reconcile()`（[`SensitiveActionStore.kt#L145-L178`](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/SensitiveActionStore.kt#L145-L178)）确认已受理后不会清除本地登录态（`SessionEnd(AccountRequest)`）或回写 `resultId` 到本机受限凭证；UI 已展示待退出警示并以 `clientRequestId` 查询或跳转 `Recovery`。
      3. `受限查询类型前置区分、保全明细与撤销查询凭证接口缺失`：`RestrictedReference` 未区分 `close_account`/`erase_fields`，`DataRequestStatus` 缺逐项保全依据与明确失败原因字段，且客户端未封装 `POST /api/v2/data-requests/{id}/revoke-access`；UI 仅如实展示现行字段，无推测展示。
    - 静态核对：5 份 `kmp/clientUi` 文件 UTF-8 无 BOM／CRLF／括号引号平衡／`Mnd*` 复用核对通过；`scripts/check_client_ui_static.py` 与 `scripts/check_server_shared_contract_static.py` 通过；`scripts/check_android_ui_static.py` 第 11–48 项通过并在预存未改动 `Components.kt` 的 `Mnd*` 白名单断言（L54）中止；未运行 Gradle 编译／类型检查／单测／设备验收。
  - [x] **KMP 公共客户端（`kmp/shared`）恢复子项：B08／C05 账号注销结果未知后恢复原会话与受限回执。**（含 11 月 19 日安全存储故障修复，源码和只读静态已签收，编译、单测和运行待验收）
    - 负责人 / 完成层级 / HEAD / 目标阻塞：Antigravity (Gemini) / 源码实现 ＋ 只读静态审查完成（未运行 Gradle 编译、类型检查、单元测试、真机或提交推送） / `main` @ `f9a2dab` / 针对上方原生子项阻塞 2（`ResultUnknown` 对账恢复）
    - 真实接口与规则核对：`MobileApi` 公共客户端实为 `FeatureApi.submitDataRequest()` → `MobileApi.submitDataRequestSecure()`（无 `createDataRequest` 命名）与只读 `FeatureApi.findDataRequest(key, token)`（`GET /api/v2/data-requests/requests/{key}` ＋ `X-Request-Access`）。`reconcile` 严格只查回执、不重发 `POST /api/v2/data-requests`、不实际发起注销；未改现行后端／OpenAPI DTO 字段。
    - 改动证据与行为对齐：
      - [`MobileApi.kt#L77-L111,L138-L139,L338-L448`](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileApi.kt#L338-L448)：`submitDataRequestSecure()` 于 `sessionMutex.withLock` 登记原登录用户 `ownerId`、会话代次 `accountGeneration` 与 `(clientRequestId, type, queryToken)` 到 `pendingDataRequests`（已知非 `408` `4xx` 拒绝清理同代次记录、`OperationResultUnknown` 保留，正常成功路径补 `NonCancellable` 与 `owner` 校验）；新增 `reconcileDataRequest()` 与 `isConfirmedDataRequestReceipt()`，校验 `receipt.id > 0`、`receipt.type == input.type`、分类型确认态（`close_account`/`erase_fields` 仅接受 `processing`/`partially_held`/`completed`，`matters_assistance` 仅接受 `needs_assistance`/`completed`）、非负计数与 `progressVersion != 0` 下的非 `NeedsReconciliation` 相位；随后核对 `vault.limitedAccess` 同请求键＋同 `queryToken` 且 `id == 0L || id == receipt.id` 后回写 `receipt.id`；仅当 `input.type == "close_account"`、存在 `pendingDataRequests` 记录且当前 `session?.user?.id` 与 `accountGeneration` 仍匹配原提交代次及原 Store `revision` 时才持久化 `SessionEndKind.AccountRequest` 对应的会话清除，在成功落盘后、会话流通知退出前触发 `onBeforeEndSession`，最后释放原恢复关联；`erase_fields` 与 `matters_assistance` 仅补写 `id` 并保持原会话；错类型、404、冲突、异常与换号／并发登录／旧回调均不清除新会话或受限凭证。
      - [`SensitiveActionStore.kt#L24-L256`](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/SensitiveActionStore.kt#L181-L256)：`confirm()` 捕获 `confirmOwner` 与 `confirmGeneration`（`api.currentAccountGeneration()`）并在 `OperationResultUnknown` 同账号路径保留；`reconcile()` 增加 `reconcileJob` 防重入并发并改调 `api.reconcileDataRequest()`，仅在确认结束原会话回调中置 `expectedSignOut = true` 并先切至 `SensitiveStage.Complete`（含 `resultId`／`clientRequestId`），使 `init` 内 `api.user.collect` 收到 `null` 时保留结果状态而不误退回 `Idle`；`user != null && stage != Submitting` 时清理 `expectedSignOut` 防止状态融合残留；`Rights` 与 `reconcileOperation` 保持原逻辑。
      - [`MobileStore.kt#L184`](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt#L184)：`accountChanged()` 扩展 `sensitiveSignOut` 判断（`|| api.sessionEnd?.kind == SessionEndKind.AccountRequest`），保证经对账进入 `Complete` 后触发注销退出时同样清理原账号本地草稿、工作区与待回复缓存。
      - [`OperationRecoveryTest.kt#L98-L331`](../../kmp/shared/src/commonTest/kotlin/com/ttkk0000/meowcircle/client/OperationRecoveryTest.kt#L98-L331)：新增 4 组纯静态 `MockEngine` 回归测试源码（同代次 `close_account` 未知后仅 `GET` 对账并回写 `id` ＋ `AccountRequest` 清会话；同账号并发重登与换号直接对账不清除新会话或凭证；进行中对账遇换号旧回调安全中止并保持新会话；404／错类型保持 `ResultUnknown` 与 `erase_fields`／`matters_assistance` 保留会话；仅源码，未执行）。
    - 静态核对：`scripts/check_kmp_client_static.py`（142 Kotlin/Swift 语法定界符／无 BOM／合同字段）、`scripts/check_server_shared_contract_static.py`（90 对 DTO）、`scripts/check_client_ui_static.py`（54 公共 UI / 48 分派）、`scripts/check_mobile_static.py`（45 表 / 288 接口 / 145 schema）与 `git diff --check -- kmp/shared` 均通过；4 份改动 Kotlin 文件 UTF-8 无 BOM 且保持原行尾（3 份 CRLF、`MobileStore.kt` 原 LF）。
    - 剩余风险与合同边界（无法求证部分如实留阻，不猜测）：
      1. `DataRequestStatus` 响应体未含 `owner_id`、`client_request_id` 与服务端 `session_epoch`（见只读 [`data_requests.go#L53-L69`](../../internal/mobile/data_requests.go#L53-L69)），本机 `RestrictedAccess` 亦不持久化所属账号或代次；若应用进程重启导致内存 `pendingDataRequests` 与 `SensitiveActionStore.intent` 清空，现有合同下无法求证现存会话仍为原注销提交代次，按保守规则仅回写 `id` 而不自动退出新会话。
      2. 服务端受理 `close_account` 时已递增 `Accounts.session_epoch`（只读 [`data_requests.go#L203`](../../internal/mobile/data_requests.go#L203)）；若 `ResultUnknown` 期间任意并发已认证请求触发 `401` 刷新失败，`MobileApi.checked()` 会先行按 `SessionEndKind.Expired` 清退会话并重置 `SensitiveActionStore`，此后需在受限 `Recovery` 由用户手动核对（`MobileFeatureStore.lookup()` 本次保持原行为未改）。
      3. 同子项阻塞 1（OAuth／邮箱 `reauth`）、阻塞 3（`RestrictedReference` 类型前置区分与 `revoke-access`）、并行 Compose／Web 对齐，以及本批全部 Kotlin 编译与 `OperationRecoveryTest` 单测运行均待后续单独授权验证。
  - [x] **Web 消费端（`web-client/src/features`）子项：F01／F13／B08 账号注销五态。**（含 11 月 19 日会话、404 与本机记录修复，源码和只读静态已签收，跨端父项和浏览器运行待验收）
    - 负责人 / 完成层级 / HEAD / 最近更新时间：Antigravity (Gemini) / 源码实现 ＋ 状态触发对照 ＋ 只读静态与 TSX 语法核对完成（未编译、未类型检查、未构建、未运行测试或启动浏览器、未实际发起注销；跨端父项保留 `[ ]`） / `main` @ `661491b` / 2026-11-19（UTC+08:00）
    - 适用 Figma Web 节点：`311:380032`（`authDeleteImpact` 注销账号前）、`311:380040`（`authDeleteVerify` 确认本人操作）、`311:380044`（`authDeleteProcessing` 注销申请处理中）、`311:380048`（`authDeleteStatus` 注销处理结果）、`311:380052`（`authDeleteFailure` 注销暂未完成）。
    - 入口与三类请求分流：
      - 只读入口核对：[`Account.tsx#L77-L80,L184`](../../web-client/src/features/Account.tsx#L77-L80)（「我的」与「账号与安全」均通过 `workflowURL("data")` 进入 `/edit?flow=data`）。
      - 表单前置与三路径分流：[`WorkflowEditor.tsx#L210-L231,L315-L335,L679-L695,L1700-L1703,L1874-L1901`](../../web-client/src/features/WorkflowEditor.tsx#L210-L231) 支持 `?type=` 初始预选并在 `prepare()` 校验 `erase_fields`／`close_account`／`matters_assistance`；选 `close_account` 时自动清空 `fields` 与 `selections`、主按钮文案切为「查看注销影响」，并预检 `listCapabilities()` 提示本机既有注销受限查询凭证（跳 `/recovery?view=close-account&request={requestId}` 防重交）；`erase_fields`（[`WorkflowEditor.tsx#L1902-L1935`](../../web-client/src/features/WorkflowEditor.tsx#L1902-L1935)）与 `matters_assistance`（[`WorkflowEditor.tsx#L1937-L1950`](../../web-client/src/features/WorkflowEditor.tsx#L1937-L1950)）保持原有字段勾选与 `DataSelector` 核对提交流程。
    - 五态与子视图触发条件及源码行号：
      - 态 1 `authDeleteImpact`（`311:380032` 注销账号前 ＋ 未完成事项子视图）：
        - 触发条件：`flow === "data"` 且 `prepared.body.type === "close_account"`（`POST capability` 与 `POST data-requests/scope` 准备完成），`deleteStep === "impact"`（子视图为 `deleteStep === "matters"`）。
        - 源码实现：[`AccountDeletionPages.tsx#L872-L968`](../../web-client/src/features/AccountDeletionPages.tsx#L872-L968)（标题「注销账号前」／主标题「请确认你了解这些影响」，展示三路径分流切换按钮、「宠物与日记／未完成事项／必要历史／补充说明」四项影响事实、「受理与清除分开」警示与 `impactAgreed` 显式勾选「我已了解注销影响，并确认提交申请」，未勾选或无 `scope` 时锁定「继续验证身份」）；未完成事项子视图 [`AccountDeletionPages.tsx#L324-L445,L861-L870`](../../web-client/src/features/AccountDeletionPages.tsx#L324-L445) 按 `adoptions`、`appointments`、`reviews`、`cases`（`owner_id`）、`pets`（`owner_id`）调用 `usePages` 只读展示真实分页记录与详情跳转，不提供一键自动结案，支持一键切「改用未结事项协助」。
      - 态 2 `authDeleteVerify`（`311:380040` 确认本人操作 ＋ 已通过验证待最终确认）：
        - 触发条件：态 1 勾选 `impactAgreed` 并点击「继续验证身份」进入 `deleteStep === "verify"`（提交中为 `"submitting"`）。
        - 源码实现：[`AccountDeletionPages.tsx#L533-L813,L970-L1254`](../../web-client/src/features/AccountDeletionPages.tsx#L970-L1254) 读取 `GET auth/methods` 与 `GET auth/capabilities`；支持已绑定手机号 `purpose: "reauth"` 短信验证码（校验完整绑定手机号 `validLoginPhone`、冷却倒计时、`otpExpiryMillis` 有效期与 6 位 `AuthOtpField`，调用 `POST auth/reauth`）、当前账号密码（`POST auth/reauth`）及已关联且当前 Web 环境已配置官方凭证的微信／QQ 用途重验（`POST auth/oauth-start` 带 `scope` ＋ `providerPopup` ＋ `POST auth/oauth-finish` 取 `reauth_token`）；提供「换用已关联方式验证」显式切换器，切换方式、返回上一步、账号或 `scope` 变化及卸载时立即清空密码、验证码与内存 `reauth_token` 并通知取消挂起弹窗；取得 `grant` 后进入「已通过本人验证 · 待你最终确认提交」卡片（[`AccountDeletionPages.tsx#L1232-L1249`](../../web-client/src/features/AccountDeletionPages.tsx#L1232-L1249)），必须再次显式勾选 `finalConfirmed`（「我已确认注销此账号，并了解提交后会立即退出」）才启用危险主按钮「提交注销申请」，绝不在验证成功时自动提交。
      - 态 3 `authDeleteProcessing`（`311:380044` 注销申请处理中 ／ 申请已受理 ／ 必要保全）：
        - 触发条件：`POST data-requests` 返回 `200 OK`（或 `ResultUnknown` 经 `GET data-requests/requests/{requestId}` 核实已受理为 `close_account`），先在发送前持久化不含表单快照的 `(0n, capability:{requestId})`（[发送预写](../../web-client/src/features/WorkflowEditor.tsx#L1048)），核对真实回执后写入回执 ID 并清理原账号草稿，再按原会话匹配结果退出并跳转至受限路由 `/recovery?view=close-account&request={requestId}`（网址不含任何 token，[`WorkflowEditor.tsx#L1064-L1109`](../../web-client/src/features/WorkflowEditor.tsx#L1064-L1109)）；在受限结果页由 `analyzeDataRequestProgress(status)` 判定 `phase === "processing" || phase === "held"`。
        - 源码实现：[`AccountDeletionPages.tsx#L122-L231,L260-L322,L1417-L1538,L1593-L1653`](../../web-client/src/features/AccountDeletionPages.tsx#L1593-L1653) 展示「申请已受理，当前账号已安全退出」或「部分内容处于必要保全」，明确提示「申请已受理不等于资料已全部清除」，并通过 `DataRequestProgressBlock` 如实展示 `id`、`version`、`state`、`external_state`、`erased`、`pending`、`held`、`physical_pending`、`provider_pending`、`restricted_copies`、`pending_fields`、`review_required` 与 `next_review_at`。
      - 态 4 `authDeleteStatus`（`311:380048` 注销处理结果：已完成 ／ 受限副本保留 ＋ 撤销本机查询凭证）：
        - 触发条件：`/recovery?view=close-account&request={requestId}` 凭 `X-Request-Access` 调 `GET data-requests/requests/{key}` 返回真实 `DataRequestStatus`，且 `progress_version === 1n`、`state === "completed"`、`pending === 0n`、`held === 0n`、`physical_pending === 0n`、`provider_pending === 0n`、`pending_fields` 为空（`restricted_copies === 0n` 为 `"complete"`，`restricted_copies > 0n` 为 `"retained_copies"`）。
        - 源码实现：[`AccountDeletionPages.tsx#L1655-L1743`](../../web-client/src/features/AccountDeletionPages.tsx#L1655-L1743) 严格区分「账号注销已完成」与「注销主体已处理，仍有受限副本保留」（绝不将存在 `restricted_copies` 的回执显示为全部副本清零）；支持「重新核对结果」、带二次确认的「撤销本机查询凭证」（调用 `POST data-requests/{id}/revoke-access` 后清理 `(0n, capability:{requestId})`，[`AccountDeletionPages.tsx#L1540-L1564,L1701-L1741`](../../web-client/src/features/AccountDeletionPages.tsx#L1540-L1564)）、「查看本设备其他受限进度」与「浏览公开内容」。
      - 态 5 `authDeleteFailure`（`311:380052` 注销暂未完成：范围加载失败 ／ 明确拒绝 ／ 结果未知 `ResultUnknown` ／ 受限查询失败或协议版本待核对）：
        - 触发条件：① `prepare()` 阶段 `POST capability` 或 `POST data-requests/scope` 失败（`failureKind = "scope_failed"`，[`WorkflowEditor.tsx#L862-L869`](../../web-client/src/features/WorkflowEditor.tsx#L862-L869)）；② `POST data-requests` 返回明确 `400/403/404/409/422/429` 拒绝（`failureKind = "rejected"`，清理 `uncertain`，保留 `prepared` 与原 `requestId` 留在注销失败视图，[`WorkflowEditor.tsx#L1122-L1168`](../../web-client/src/features/WorkflowEditor.tsx#L1122-L1168)）；③ 请求已发出但遇网络异常／`401`／`408`／`5xx`（`failureKind = "result_unknown"`，`draft.uncertain = true`，锁定盲目重发，[`AccountDeletionPages.tsx#L815-L859,L1256-L1414`](../../web-client/src/features/AccountDeletionPages.tsx#L1256-L1414)）；④ `/recovery?view=close-account&request={requestId}` 受限查询遇到 `404`／`403`／网络错误、类型不匹配或 `progress_version !== 1n`（`phase === "needs_reconciliation"`，[`AccountDeletionPages.tsx#L1745-L1825`](../../web-client/src/features/AccountDeletionPages.tsx#L1745-L1825)）。
        - 源码实现：`ResultUnknown` 从编辑页或恢复中心按原编号和受限凭证执行只读查询。只有回执编号、类型和受理状态匹配才更新受限记录。原账号的草稿按实际保存路由和原请求号查找，持久化成功后只结束本页仍能证明的原提交会话。404 保留原编号和待核对状态，继续提供查询入口。 [原请求读取](../../web-client/src/features/AccountDeletionPages.tsx#L287) · [账号草稿匹配](../../web-client/src/features/AccountDeletionPages.tsx#L1480) · [编辑页入口](../../web-client/src/features/WorkflowEditor.tsx#L1595)
    - 已确认只读合同边界与阻塞记录（未猜测、未越界修改 Go 或全局协议）：
      1. `邮箱用途验证与 Apple Web 重验合同缺失`：只读 [`internal/identity/email.go#L63`](../../internal/identity/email.go#L63) 与 [`internal/identity/service.go#L700-L738`](../../internal/identity/service.go#L700-L738) 仅支持密码、绑定手机号 `reauth` OTP 及已配置 Web 凭证的微信／QQ `Scope` OAuth（[`browser_provider.go#L14`](../../internal/platform/mobileapi/browser_provider.go#L14) 排除 `apple`）；Web 态 2 已在「验证方式合同与可用性说明」中如实披露，不提供无效邮箱或 Apple 按钮。
      2. `受限状态接口缺少逐项保全依据、责任岗位与失败原因明细`：只读 [`internal/mobile/data_requests.go#L53-L69`](../../internal/mobile/data_requests.go#L53-L69) 的 `DataRequestStatus` 仅返回 `held`、`next_review_at`、`review_required`、`restricted_copies` 等聚合字段，未下发 `DataHold` 逐条法务依据或处理器明细；Web 态 3／态 4 仅展示真实返回计数与复核时间并明示接口边界。
    - 验收勾选（按验证层次分离）：
      - [x] Web 注销五态源码实现、`WorkflowEditor.tsx`／`Recovery.tsx` 局部接线与派工回填完成
      - [x] 只读静态与语法检查通过：`python -X utf8 scripts/check_web_client_static.py`（整改后 102 份 TS/TSX、476 个本地引用、147 份 schema）、`python -X utf8 scripts/check_page_gap_static.py`、`syntax-reader.cjs` 隔离进程 tree-sitter TSX 语法检查（`AccountDeletionPages.tsx`、`WorkflowEditor.tsx`、`Recovery.tsx` 均 `errors: []`）及 `git -c core.safecrlf=false diff --check` 全部通过
      - [ ] Web 浏览器实际渲染、五态视觉与交互走查、前后端联调运行验收（遵循全局不自行编译／测试／启动规则，且禁止实际发起注销，留待后续授权验收）
- [ ] **F00／F01／C05／C06 · 会话结束与原任务失效。** 核对 `authSessionExpired`、`authLogoutConfirm`、`authLoggedOut`、`authRevoked` 和 `authReturnUnavailable`。登录成功后分别返回帖子／宠物申请／发布草稿／会话，验证对象被删除、访问范围变化、换账号、不安全返回链接及网络失败。失效目标给独立恢复出口；保留当前账号合法草稿，停止失权编辑和自动提交。后台会话撤销与普通过期若仅有同一 401，优先明确合同，客户端避免猜测具体成因。[原生登录续接](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt#L798) · [Web 返回预览](../../web-client/src/features/AuthReturnTask.tsx)
- [x] **F12／W02 · Web 评论锚点底栏。** 在不可查看／读取失败态移除编辑器和焦点入口；可见／更新态恢复当前评论上下文和允许的回复。保留同一锚点重读、正确父项验证、回消息中心和显式已读。对应 Figma 四个节点 `958:371822`、`958:374615`、`958:377408`、`958:380184`。新增权限变化后旧评论与编辑框不再可见的证据。
  - 任务 ID / 本文标题 / 客户端和布局：`F12／W02` / Web 评论锚点底栏 / Web 消费端（独立 React DOM；`web`、`webN`）
  - 负责人 / 状态 / 分支和 HEAD / 最近更新时间：Antigravity (Gemini) / 待验收（源码修复与静态审查完成；视觉、浏览器运行与真实业务待验收） / `main` @ `f9a2dab` / 2026-11-19（UTC+08:00）
  - 范围：`settings-notifications` / 精确评论锚点四状态板 `958:371820`（`958:371822` 可见、`958:374615` 已更新、`958:377408` 不可查看、`958:380184` 读取失败）＋ 正在读取态与中途失权处理
  - 允许写入的文件：`web-client/src/features/ResourcePage.tsx`、`web-client/src/features/chat.tsx`
  - 证据（入口 → 触发条件 → 实际结果）：
    - 入口与校验：`App.tsx#L522-L524` 由 `notificationReplyAnchor` 解析正整型锚点进入 [`NotificationCommentPage`](../../web-client/src/features/ResourcePage.tsx#L143-L291)；挂载/重读执行 `reread`（`ResourcePage.tsx#L173-L193`），清理旧列表缓存 `invalidateQueries(listPrefix)` 并调用 `reload(path)` 以序号 `readSeq` 确认当前可见权限，同时校验 `anchor.kind === expectedKind`、父对象 `row.reference_id === id` 与 `row.state === "published"`（`ResourcePage.tsx#L152-L208`）。
    - 正在读取态：`checkedScope !== scopeKey || result.loading || (!revoked && !row && !result.error)` 时 `reading === true`、`visible === false`（`ResourcePage.tsx#L209-L215`），`TaskPage` 的 `actions` 为 `undefined`（`ResourcePage.tsx#L224-L240`、`ui.tsx#L306`），底栏 `Composer` 与全部焦点入口不渲染，只显示「正在读取评论」与「返回消息中心」（`ResourcePage.tsx#L243-L244,L281-L283`）。
    - 可见（`958:371822`）与已更新（`958:374615`）：仅在 `!reading && matches && !result.error && !result.denied && !revoked`（`visible === true`）时把 `<Composer compact allowMedia={false} canSend={visible} ... />` 传入 `TaskPage` `actions`（`ResourcePage.tsx#L224-L241`）；版本一致展示「通知提到的评论」，`row!.version !== anchor.version` 展示「评论已更新」与当前内容（`ResourcePage.tsx#L254-L263`），并渲染当前评论与同父项分页评论列表（`ResourcePage.tsx#L264-L279`）。`Composer` 继续执行 `eligibility` 回复资格检查、按 `/${kind}/${id}/composer` 隔离草稿（`chat.tsx#L275,L295-L303,L606-L620`），并修复 `compact` 评论框占位文案为「说点什么…」（`chat.tsx#L686-L692`）。显式已读保留在 [`NotificationPage`](../../web-client/src/features/NotificationPage.tsx#L22-L40)，查看不标已读。
    - 不可查看（`958:377408`）与读取失败（`958:380184`）：非法锚点、父对象/发布态不匹配、`result.denied`（401/403/404/410）或 `revoked` 均令 `unavailable === true`、`visible === false`，展示「这条评论当前不可查看」（`ResourcePage.tsx#L216-L220,L245-L252`）；非失权网络/服务异常令 `unavailable === false`、`visible === false`，展示「暂时无法读取评论」（`ResourcePage.tsx#L245-L252`）。两种非可见态均令 `actions` 为 `undefined`，彻底移除底栏 `Composer` 及其焦点入口且不渲染旧评论（`ResourcePage.tsx#L224-L241,L245-L253`），仅保留「返回消息中心」与「重新读取评论」（`ResourcePage.tsx#L281-L288`）。
    - 中途失权收敛：已进入可见态后，若列表分页 `Comments` 触发 `page.denied`（`ResourcePage.tsx#L895`）、锚点 `CommentBatch` 的点赞查询 `metrics.denied`（`ResourcePage.tsx#L785-L787`）或点赞动作捕获 `isAccessRevoked(e)`（401/404/410 或 `403` 且无资格限制子码，见 `ResourcePage.tsx#L136-L142,L802-L807`），立即调用 `markRevoked`（`ResourcePage.tsx#L197-L201`）置 `revoked = true`、清空 `reply` 并清理 `listPrefix`，使旧 `<CommentBatch>`、`<Comments>` 与底栏 `<Composer>` 同时从 DOM 收起并切换至不可查看态；若 `Composer.send` 遭遇上述失权错误，在落盘清除 `uncertain` 保护草稿后调用 `onUnavailable`（`chat.tsx#L584-L598`）触发 `reread(true)` 重新向服务端核对权限，避免把普通资格/联系受限误判为失权。
  - 静态核对：`python -X utf8 scripts/check_web_client_static.py` 通过（101 份 TS/TSX、461 个本地引用、147 份 schema、22 枚原 SVG、字体／贴纸与依赖锁一致）；`syntax-reader.cjs`（Node 18 / 384MiB / 1 wasm task，可用内存 6459 MiB）读取 `ResourcePage.tsx` 与 `chat.tsx` 均返回 `errors: []`；`git diff --check` 无错误。
  - [x] 源码／状态接线对照完成
  - [x] 静态审查完成
  - [ ] 目标端视觉／键鼠或系统交互验收通过
  - [ ] 真实业务与失败恢复验收通过
  - 依赖／未验证项／下一步：未运行编译、类型检查（`tsc`）、打包（`vite`）、测试或启动浏览器（遵循全局不自行编译规则）；需在真实 Web 运行环境验收桌面/窄窗视觉、Tab 焦点迁移、中文 IME 及 401/403/404/410 失权与恢复链路。

### 第二批 · 按功能核对及补齐页面状态

以下按 19 个 family 覆盖 748 条。领取一项后用所列 family 筛选原台账。

- [ ] **F00／F01／B04／B05** · `auth` 105。登录／显式注册、第三方选择与冲突、恢复回执补充、密码／验证码／过期重发、绑定解除／换号、年龄区间／资格及第一批余项。核对批次 020 最终五份静态证据；真实取消、过期、返回和最后一种登录方式不得解除。
- [ ] **C02／C08／F02** · `community` 24 ＋ `global-states` 4。社区首页、指南、内容详情、评论、点赞、删除与不可用；首读失败区别空、刷新保留合规快照、原游标续页、失权清除旧内容。共用发布、返回和页脚用默认浅色、既有组件。
- [ ] **F03／F06** · `circle-friends` 47 ＋ `groups` 35。好友申请、通过／拒绝、圈子受众、群目录与申请、成员、移交、退出、解散。用不同账号核对角色、受众变动和屏蔽后访问；移动两种返回及桌面主从栏都回原列表。
- [ ] **F04** · `pet-diary` 44。我的宠物、档案、日记、评论、受众修改、协作身份；按宠物隔离，切换宠物后草稿、图片与邀请各归原对象。介绍／试养协作的限定范围另与 F07 联验。
- [ ] **F05／F11** · `messages` 20 ＋ `expressions` 91。消息中心、请求、私信会话、首条未读、发送失败／同键恢复；系统键盘／表情切换、光标与字素、常用／收藏与移除、贴纸预览和发送。私密聊天内容及草稿按账号与会话隔离。
- [ ] **F07／B10** · `adoption` 102。领养介绍、候选与补材料、双方核对协议、试养、交接、安全接回、近况、私密照护、协作退出。覆盖同宠独占、旧周期／过期、照片返回、失权后只读／收起、退出／撤销范围。五布局协作 8 状态用[专用源码状态表](collaboration-page-source-map-2026-10-28.json)与[设计终稿](../design/figma-study/collaboration-refinement-2026-10-29/README.md)。
- [ ] **F08／B10** · `appointments` 29。预约安排、原安排／新提案／历史、风险暂停、暂停后同版双确认、独立取消、未知结果与回执。双方分别核对 409 后重读、过期与并发取消，返回保持原事项。
- [ ] **F09／B03** · `lost-found` 73 ＋ `nearby` 28。走失／拾获列表、地图／列表回退、粗位置、认领争议、线索照片／检查／失败／回执、私密线索和独立证人会话、区域提醒。关闭案件或撤权后清理旧私密材料，公开页可见范围按服务端数据。
- [ ] **F10／B06** · `publishing-media` 20。发布入口、九图选择／排序／重试、草稿、完整预览、范围确认、来源与 AI 声明独立回执。换媒体后重新核对；回到编辑保留草稿；未知复查原请求，避免重复提交。
- [ ] **F12／B03** · `settings-notifications` 16。未读 0／99+、十分钟全部已读快照、待处理语义、通知偏好与 19:00 汇总、系统权限、事件与评论锚点。过期快照或失败不清零，新到消息保留，查看目标不等于已读或同意。链接目标数据按当前权限重读。
- [ ] **F13／B02／B05／B08／B09** · `verification-reports` 29 ＋ `safety-data` 58。核实、举报／申诉、游客安全求助、监护邀请与关系核验、侵权通知／声明、资料请求／保全。用不同会话确认原案追加、票据限范围、失效、清理失败以及不被交流资格门槛误挡。
- [ ] **F14／B07／B10** · `offers-visits` 11。城市／物种准入、发布资格、出售／有偿转让性质、费用总额与明细、接待关系、到访反馈。武汉／上海按有效规则验证，城市目录只作选择器；无支付合同的页面保持信息及预约范围。
- [ ] **F13／W02** · `rules` 9。指南、规则、隐私与出版历史、版本生效、更新订阅与失效。阅读返回原列表；公开路由可清晰到达，登录信息不进分享。
- [ ] **B11／W02** · `operator-reference` 3。原有 Web 运营参考状态在业务后台按角色回填证据，和现行 13＋18＋39 稿分别交叉核验，禁止恢复消费者运营权限。

## 4. 按客户端领取

每端先领取一组页单。完成时将本端适用的第 3 节状态逐条登记；这里列出的宿主场景必须由本端证据覆盖。跨端发现的相同公共 Compose 问题交公共负责人修一次，各端回归。

### Android 手机 · A01／A02／A03

- [ ] **认证与账号（P0，F00／F01／F13）。** 手机 `phone` 原稿。验登录／换号／受限／注销／登录回原内容五态、深链冷启动、多次系统返回、验证码自动填充与键盘遮挡。锁屏或杀进程后重进，核对旧账号观察结果不会混到新账号。
- [ ] **消息与带媒体写入（P1，F02／F05／F10／F11／F12）。** Push/App Link 到通知、评论、会话；显式已读和评论四态。照片拒绝／取消／不可读、排序、上传失败、草稿与回执；系统返回只关闭当前键盘／底部任务层。
- [ ] **领养、信任与地图（P1，F03／F04／F06–F09／F13／F14）。** 领养／预约／线索核权、横屏或在线失败还原；定位被拒可手选城市和回列表，公开粗点与私密线索分开。
- [ ] **手机全量（V02）。** 余下社区与规则、宠物空间、圈子、群、附近、表情和设置状态，完成 `phone` 适用行和 TalkBack／大字号证据。入口 [Android 宿主](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile)。

### Android 平板与折叠 · A04

- [ ] **主从栏（P1，F02–F06／F12）。** 社区详情、群和私信、通知锚点、宠物切换；横竖屏、窄分屏、折叠／展开保持选中对象、已读锚点、滚动及返回目标。
- [ ] **分栏表单（P1，F01／F07–F10／F13／F14）。** 认证／注销、发布预览、预约对照、试养照片及线索受限表单在折痕、软键盘和窗口变化下不遮操作；不重建请求、不丢草稿或清空校验。区分横竖稿中的返回和同状态切换。
- [ ] **平板全量（V02）。** 分别登记 `tabletP`、`tabletL`、`tabletN`；验证折痕、外接键盘焦点、大字体和名单／日期选择器，有折叠方案时登记对应设备。[Android 自适应窗口](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidAdaptiveWindow.kt) · [布局契约](../design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)
  - [x] **Android 宿主（`kmp/androidApp/.../mobile`）子项：A04 · 平板／折叠屏宿主适配与三条代表路径核对。**（父项「主从栏／分栏表单／平板全量」、跨层 `clientUi`/`MainActivity` 问题与设备／视觉／运行验收仍保留 `[ ]` 待签收）
    - 任务 ID / 本文标题 / 客户端和布局：`A04` / Android 平板／折叠屏宿主适配 / Android 平板与折叠屏（共用 `kmp/clientUi` + `kmp/androidApp/.../mobile` 薄宿主；对应 `tabletP` 834×1194、`tabletL` 1194×834、`tabletN` 744×1133 及折叠半开态势）
    - 负责人 / 状态 / 分支和 HEAD / 最近更新时间：Antigravity (Gemini) / 源码修复与静态审查完成（未编译、未运行、未实机／模拟器或截图视觉验收） / `main` @ `f3a03e5` / 2026-11-19（UTC+08:00）
    - 范围：Figma 平板页 [403:3](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=403-3)（本轮 MCP 远端已读取 `403:3` 完整节点树及 `311:379703`、`311:379707`、`325:61505`、`325:61507`，其余台账 `tabletP` 748 / `tabletL` 748 / `tabletN` 16 节点因 Figma MCP Starter 调用配额上限改对照本轮已拉取 `403:3` XML 与 `cross-platform-full-coverage.json`）＋ `CROSS_PLATFORM_ADAPTIVE_CONTRACT.md`；核对折痕／Insets／有效窗口尺寸／返回分发及三条代表路径（消息主从栏与未读锚点、发布／预约分栏表单与软键盘、横竖屏和折叠／分屏任务保持）。
    - 允许写入的文件：`kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/{AndroidAdaptiveWindow,AndroidClientPlatform,AndroidDateFields,AndroidNotificationPermission,AndroidPhotoPicker,AndroidProviderAuth,MobileViewModel}.kt`、本派工子项及自身 AI 跟踪（`MainActivity.kt`、`AndroidManifest.xml`、`kmp/clientUi`、`kmp/shared`、其他端与 Gradle 严格只读）。
    - 宿主已修缺口（入口 → 触发条件 → 实际结果）：
      1. **折痕＋Insets 双重扣减修复**（[`AndroidAdaptiveWindow.kt#L21-L64`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidAdaptiveWindow.kt#L21-L64)）：原 `.absolutePadding(left,top,right,bottom)` 未调用 `.consumeWindowInsets(...)`；桌面／悬停横向折叠（`bottom > 0`）或竖向折叠排除半屏（`left/right > 0`）时，下游 `MobileShell` 的 `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` 与 `Row(...imePadding())`（[`ConsumerApp.kt#L194-L196`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt#L194-L196)）会在已避让铰链的半屏内二次扣减系统栏／异形屏／IME 高度。现补充 `.consumeWindowInsets(PaddingValues.Absolute(left = left, top = top, right = right, bottom = bottom))` 并增加畸形倒置 `fold.bounds` 保护与共享 `Context.findActivity()` 解包。
      2. **第三方官方授权页面系统返回死锁与分屏外跳修复**（[`AndroidProviderAuth.kt#L72-L77,L124-L128`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidProviderAuth.kt#L72-L77)、[`AndroidClientPlatform.kt#L16-L65`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidClientPlatform.kt#L16-L65)）：用户在透明 `OfficialProviderActivity` 等待微信／QQ 授权期间按系统返回关闭 Activity（`isFinishing == true`）原仅清空 `qqListener`，未通知 `AndroidProviderAuth`，导致 `authorizeProvider()` 挂起且 QQ `pending` 槽位泄漏并永久阻塞后续授权返回 `409 provider_pending`；现新增 `cancelFromHost()` 仅在 `continuation?.isActive == true` 时清理并发槽并恢复 `null`（进入 `Cancelled`，同时保留行 39–43 从 `ProviderAuthTask` 主动取消时的 QQ 防串号保护，且不破坏横竖屏重建 `!isFinishing` 重附着）。另在 `AndroidClientPlatform.kt` 提取 `startExternalActivity()` 对存活宿主 Activity 省略强制 `FLAG_ACTIVITY_NEW_TASK` 以维持平板分屏返回栈（复用至 `openNotificationSettings`、`openLink` 与 [`AndroidNotificationPermission.kt#L18,L27-L29`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidNotificationPermission.kt#L18)），并补 `decodeImage()` 空字节／无效宽高提前返回与异常兜底。
      3. **系统选择器与日程弹窗在禁用／配置切换／异常下的宿主稳健性**（[`AndroidDateFields.kt#L29,L54-L82`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidDateFields.kt#L29)、[`AndroidPhotoPicker.kt#L27-L38,L83-L95`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidPhotoPicker.kt#L27)）：`DateTimeField` 补充 `if (dateOpen && enabled)` 与 `if (enabled) pendingDay?.let` 及 `context.findActivity()`，避免表单转 `busy`／确认弹窗／未知结果期间仍可经系统弹窗改写草稿；`PhotoPickerButton` 改为 `applicationContext.contentResolver`＋`rememberUpdatedState` 持有最新 `remaining`/`onPhotos`/`onError`，并捕获 `launcher.launch(...)` 异常转明确错误回调。
      4. **评论通知锚点进程恢复保留**（[`MobileViewModel.kt#L43-L44,L71-L98`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/MobileViewModel.kt#L43)）：原 `SavedStateHandle` 只编码 `"pets:${id}"`/`"entries:${id}"` 而丢失 `NotificationReplyAnchor`，分屏切换或系统杀进程重建后精确评论锚点退化为普通详情；现用资源白名单格式 `:kind:id:version` 保留并恢复 `replyAnchor`（仅资源标识，不存敏感数据）。
    - 三条代表路径与四维度静态核对：
      - **路径 1 · 消息主从栏与未读锚点**：Figma `tabletP` `311:379702`/`311:379706`/`311:379718`、`tabletL` `311:379703`/`311:379707`/`311:379719`、`tabletN` `325:61505`。宿主 `MobileViewModel` 保存 `Chat`/`Notifications` 与 `uiSession`（[`InboxPages.kt#L14-L15`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/InboxPages.kt#L14) 列表分类／仅未读与滚动位置随 `SaveableStateProvider` 保留），现补 `SavedStateHandle` 精确评论锚点恢复与折痕 Insets 消费；**只读跨层发现**：① 邻接宿主 [`MainActivity.kt#L35,L48-L53`](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/MainActivity.kt#L35) 的 `consumeNotification(intent)` 在 `savedInstanceState != null` 时仍重复执行且对无 extra 的 `Intent.ACTION_MAIN` 冷启／旋转均误调 `queueInboxNotification()`；② 公共 Compose [`ConsumerApp.kt#L180`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt#L180) 在 `Messages` 首页（`destination == Home`）令 `showMaster = false`，使 `tabletL` 展示 `ContextPane`＋单栏 `InboxScreen` 且弹出软键盘时 `!keyboard` 会卸载左栏；③ [`ConsumerApp.kt#L688-L700`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt#L688)「定位首条未读」立即 `scrollToItem(if(leadContext) 1 else 0)`，未等待异步加载完成也未定位至 `firstUnread` 条目索引。
      - **路径 2 · 发布／预约分栏表单与软键盘**：Figma `tabletP` `311:378834`/`311:378838`/`311:378870`/`311:379782`/`311:379786`/`311:379838`、`tabletL` `311:378835`/`311:378839`/`311:378871`/`311:379783`/`311:379787`/`311:379839`、`tabletN` `325:61495`/`325:61507`。`AndroidManifest.xml#L36` 配 `adjustResize`；`PublishScreen`（`ConsumerApp.kt#L770-L802`）与 `AppointmentEdit`（`CarePages.kt#L55-L81,L265-L272`）共用 `TaskColumn` 底栏动作区＋`FormPair`（`FormLayout.kt#L8-L19`）与 `rememberPrivateEditor`（只存光标于 Bundle、原文留 Store 与私有草稿）；宿主修复折痕 IME 双重扣减、日期弹窗禁用守卫与选图异常处理；**只读跨层发现**：① `AndroidPhotoPicker.kt#L31,L34` 使用 Compose `rememberCoroutineScope()`，选图后若在 30 MiB IO 压缩中途旋转／折叠重建 Activity 会取消转码协程（改脱离 Composition 的作用域需调整 `ClientUiPlatform.PhotoPicker` 或由 `ViewModel` 托管）；② `FormPair` 在 `maxWidth / fontScale >= 620.dp`（`ClientMetrics.kt#L19`）切换双列，`tabletN`（744dp 扣 80dp 轨＋32dp 内边距＝632dp）仍触发双列而契约要求窄分屏表单单列；③ `ConsumerApp.kt#L133-L138` 先于 `Scaffold(safeDrawing)`／`.imePadding()` 用原始 `maxWidth`/`maxHeight` 计算 `clientWindowPolicy`（且源码阈值 `680/980/1100` 与 Figma `744/834/1194` 及 600dp 正文门槛存在映射差异，另未过滤完全位于下折痕裁剪区内的 `WindowInsets.ime`）；④ [`TaskDialogs.kt#L22,L29`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/TaskDialogs.kt#L22) 在独立 `Dialog` 窗口中应用 `LocalDialogRegion` 后未在 `.safeDrawingPadding().imePadding()` 前消费该边距。
      - **路径 3 · 横竖屏和折叠／分屏时的任务保持与返回分发**：`AndroidManifest.xml#L32-L36,L54` 不锁定 `screenOrientation` 也不声明 `configChanges` 绕过重建，`MainActivity` 重建期间业务 `MobileStore`/`MobileFeatureStore`/`uiSession`/`SavedStateHandle` 路由留在 `MobileViewModel`，`AndroidAdaptiveWindow` 通过 `collectAsStateWithLifecycle(emptyList())` 在 `STARTED` 状态响应折叠态和分屏拖拽；无 Figma `Prototype only / Orientation switch` 按钮；**只读跨层发现**：除上述 `MainActivity.kt` 通知重建重弹及 `SavedStateHandle` 路由字符串与历史栈局限外，[`ConsumerApp.kt#L270`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt#L270) 与 [`FeaturePages.kt#L130`](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/FeaturePages.kt#L130) 的 `MobileAlertDialog.onDismissRequest` 未拦截 `busy` 状态（`MobileTaskDialog` 已检查 `navigationEnabled`），系统返回在确认请求发出期间仍可关闭弹窗框架。
    - 静态核对：7 份目标 Kotlin 文件均通过 UTF-8 无 BOM、CRLF 无孤立 LF、`git diff --check -- kmp/androidApp`；`scripts/check_kmp_client_static.py`（142 份 Kotlin/Swift 语法定界符／配置／请求合同）、`scripts/check_client_ui_static.py`（54 公共 UI / 48 分派 / 8 端口 / 748 索引）、`scripts/check_android_feature_static.py`（48 扩展分派 / 自适应与 Manifest 检查）与 `scripts/check_mobile_static.py`（45 表 / 288 接口 / 145 schema）均退出 0；预存基线脚本 `scripts/check_android_ui_static.py` 与 `scripts/check_provider_auth_static.py` 仍分别在未改动的 `Components.kt#L54`（`MndQuietButton` 白名单）和 `AccountPages.kt#L26` 处预失败。未运行 Gradle 编译、构建、安装、单测或真机／模拟器验证（遵循全局不自行编译规则）。
    - [x] 源码／状态接线对照完成
    - [x] 静态审查完成
    - [ ] 目标端视觉／键鼠或系统交互验收通过
    - [ ] 真实业务与失败恢复验收通过
    - 依赖／未验证项／下一步：① 跨层与邻接只读问题交对应负责人跟进（`MainActivity.kt` `consumeNotification`、公共 `ConsumerApp.kt` 窗口有效可用尺寸/IME/主从逻辑/未读滚动定位/`MobileAlertDialog` `busy` 返回拦截、`TaskDialogs.kt` 弹窗 `consumeWindowInsets`、`FormLayout.kt` `tabletN` 单列门槛）；② 待获授权后按 `tabletP`/`tabletL`/`tabletN` 与折叠／外接键盘／大字体／真机完成 A04 全量运行与视觉签收。

### iPhone · I01／I02／I03

- [ ] **账号与系统回跳（P0，F00／F01／F13）。** Compose 认证页和薄宿主验证 Apple／微信／QQ 官方回调、取消／不可用、Keychain 状态、Universal Link 和侧滑返回。注销退出后受限查询及换账号清理单验。
- [ ] **手机任务与系统能力（P1，F04／F05／F07–F12）。** 系统照片选择器／相册取消、键盘输入／表情、私密照片返回、定位拒绝、APNs 授权与站内未读、评论四态。核对 Safe Area、输入法与底栏不互相遮挡。
- [ ] **iPhone 全量（V02）。** 按 `phone` 跑余下社区、群、宠物、领养、预约、安全和规则状态。带 VoiceOver／大字证据；iOS 运行须目标 macOS/Xcode 环境。[Compose iOS 宿主](../../kmp/iosApp/ComposeApp) · [iOS UI 模块](../../kmp/iosUi)

### iPad · I04

- [ ] **分屏与多窗口（P1，F02–F06／F12）。** 核对横竖稿、分屏窄窗、多个 Scene 的当前事项、消息选择／未读锚点、导航栏和焦点；一个 Scene 的临时输入不污染另一 Scene。
- [ ] **业务横竖稿（P1，F01／F07–F10／F13／F14）。** 认证、注销、预约历史对照、协作照片／回执返回、线索表单和发布预览在缩放／旋转中保留当前事项。用键盘与触控分别检查有确认／未知结果时的离开和恢复。
- [ ] **iPad 全量（V02）。** 单独登记 `tabletP`、`tabletL`、`tabletN` 适用项；补 iPad Picker、地图、深链、VoiceOver、多任务及外接键盘证据。[iOS 窗口与系统桥接](../../kmp/iosUi/src/iosMain/kotlin/com/ttkk0000/meowcircle/iosui/IosClientPlatform.kt) · [布局契约](../design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)

### Web 消费端 · W01／W02／W03／W04

- [ ] **账号、路由与注销（P0，F00／F01／F13）。** 独立 DOM 对齐认证／注销／受限／原内容失效，按 `/app` 真入口测试深链接刷新、浏览器返回／前进、新标签、跨标签退出、Cookie／CSRF、清除账号私密缓存。运行前保留安全路由白名单。
- [ ] **评论与消息（P0，F05／F11／F12）。** 先交第 3 节评论底栏修复；再核对公开详情／私密访问、直接刷新、评论父对象、403/404/410 与临时故障、原位重试、浏览器焦点和站内计数。
- [ ] **响应式业务页（P1，F02–F04／F06–F10／F13／F14）。** 宽屏双栏和窄屏单栏跑发布、详情、预约对照、领养／接回、协作照护、失权与线索照片，核对固定操作、返回、私密对象及归属不变。对照 `webN` 的 16 个明确窄窗样例。
- [ ] **浏览器全量（V02）。** 按 `web` 与 `webN` 适用行回填证据，独立覆盖移动浏览器触屏、桌面浏览器键鼠、浏览器授权／地图失败回退、键盘焦点与缩放、中文 IME、草稿／Blob 与公开分享。 [Web 路由](../../web-client/src/App.tsx) · [Web 工程](../../web-client/README.md)

### 原生 Desktop · D01／D02／D03

- [ ] **桌面任务工作区（P1，F01／F02／F04–F08／F10／F12）。** 按 `desktop` 核对会话主从栏、发布预览、预约版本对照、试养照护／照片、评论锚点及认证注销。宽／窄拖窗不跳对象，任务确认和未知回执保持原范围。
- [ ] **系统桥接（P1，F00／F01／F09–F12）。** 核对系统浏览器授权回跳、凭据、文件选择／拖放、分享与失败路径。地图按当前桌面桥接走 Web 与列表回退；正式渠道不可用时显示真实状态。覆盖键盘导航、快捷键、中文 IME、关闭窗口时草稿和未决请求恢复。
- [ ] **Desktop 全量（V02）。** 补社区、好友、群、宠物、规则及权限状态；记录实际原生窗口尺寸和结果。Web 证据单独存。 [Desktop 宿主](../../kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/Main.kt) · [系统桥接](../../kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/DesktopClientPlatform.kt)

## 5. 后台、技术依赖与分派顺序

### Web 业务后台 · B11／W02

- [ ] **业务原 13 稿。** 工作台、举报／申诉、事项核实详情、权利列表／详情、资料注销、监护、发布准入、角色会话、提交确认、结果未知、受限／未连接逐页签收。测试分页、权限白名单与私密字段、队列退出后恢复及动作审计。
- [ ] **岗位 18 稿。** 业务五岗 `moderator`、`privacy`、`identity_verifier`、`authority_verifier`、`finance` 与技术四岗各按授权验导航、跨角色拒绝与作用域；多角色切换后草稿／材料清理；`role-lost` 验当前任务中途撤权。技术四岗实际运行由下一节负责人回填。
- [ ] **39 个任务态。** 分年龄、发布授权、实际收款、实际退款四单独领取。把[39 个稳定键及节点](../design/figma-study/admin-task-interaction-2026-10-29/README.md)逐条对应 `OperatorApp`、`useOperator` 与页面分支。先补任何缺的字段修正／显式确认／发送中／失败／未知／人工核对；版本冲突重新读取真实对象。财务在原任务里按来源看台账，冻结侧栏角色切换，返回原请求，现场记录与执行结果分开；城市授权与其他授权分别验必填与对象版本。[业务后台源码](../../web-client/src/operator)
  - [x] **子项 · B11／W02 · Web 业务后台：可信年龄区间登记（`/app/operator` `ageEvidence` 10 态 + `role-lost`）。**（源码与静态已签收；真实角色、运行与 39 态父项待验收）
    - 任务 ID / 本文标题 / 客户端和布局：B11／W02 / 可信年龄区间登记 / Web 业务后台（`/app/operator?view=ageEvidence`，桌面及窄屏响应式布局）
    - 负责人 / 状态 / 分支和 HEAD / 最近更新时间：Antigravity (Gemini) / 待验收（源码与静态审查完成，真实后台运行待验收） / `main` `f9a2dab` / 2026-11-19（UTC+08:00）
    - 范围：`/app/operator` `ageEvidence` 10 个年龄态（`age-empty`、`age-field-error`、`age-edit`、`age-review`、`age-confirmed`、`age-sending`、`age-accepted`、`age-rejected`、`age-unknown`、`age-unknown-done`）及岗位撤回 `role-lost`
    - 允许写入的文件：`web-client/src/operator/RoleTasks.tsx`、`web-client/src/operator/OperatorApp.tsx`、`web-client/src/operator/useOperator.ts`、`web-client/src/operator/operator.css`、本派工子项及自身三份 AI 跟踪
    - 证据与已修缺口：入口 `/app/operator?view=ageEvidence`（`identity_verifier`）→ ① 修正 `operator.css` 字段错误边框／提示被 `operations.css` 优先级覆盖问题；② 补 `ageEvidence` 四字段全量校验、RFC3339 严格含时区校验（未来且 366 天内）、编辑态按字段清除错误与错误字段自动聚焦；③ 增 `submit(confirmed)` 与 stage/prepared 切换重置 `ack` 的双层守卫，明确区分 `age-review`/`age-confirmed`；④ `age-sending` 冻结展示提交原内容且锁定切岗／侧栏「技术运维」出口与回跳提示；⑤ 区分 `age-accepted`（明确不返回独立记录编号、不代本人授权或监护人同意）与 `age-unknown`/`age-unknown-done`（保留原请求、`reset(acknowledged)` Hook 守卫、结束后明确提示仅结束本机待办）；⑥ 提交中 403 与角色撤回立即清私密材料／未提交输入／队列／详情／草稿，仅保留脱敏最小恢复引用（仅用户编号）并展示 `role-lost`。
    - 逐态回填：

      | 状态键 | Figma 节点 | 路由与角色 | 触发条件与行为 | 源码行 |
      |---|---|---|---|---|
      | `age-empty` | [930:3264](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=930-3264) | `/app/operator?view=ageEvidence` · `identity_verifier` | 初始进入 `stage === "edit"`、无 `fieldErrors` 且 `age_band` 为空（`""`），不默认成年；显示「初始没有默认年龄区间」与工作台返回入口 | `RoleTasks.tsx` [L26](../../web-client/src/operator/RoleTasks.tsx#L26)、[L568-L575](../../web-client/src/operator/RoleTasks.tsx#L568-L575)、[L669-L673](../../web-client/src/operator/RoleTasks.tsx#L669-L673)、[L744-L756](../../web-client/src/operator/RoleTasks.tsx#L744-L756)、[L910-L930](../../web-client/src/operator/RoleTasks.tsx#L910-L930) |
      | `age-field-error` | [930:3366](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=930-3366) | `/app/operator?view=ageEvidence` · `identity_verifier` | 点击「核对本次内容」执行 `prepare("ageEvidence")`，任一字段非法（未选区间／非正整数编号／引用空或超 120 字／截止非严格含时区未来 366 天内）留在 `edit` 并设 `fieldErrors`、`aria-invalid`、红框／红字及焦点，禁止进入核对；单字段编辑清除对应错误 | `RoleTasks.tsx` [L36-L66](../../web-client/src/operator/RoleTasks.tsx#L36-L66)、[L171-L178](../../web-client/src/operator/RoleTasks.tsx#L171-L178)、[L267-L299](../../web-client/src/operator/RoleTasks.tsx#L267-L299)、[L575-L583](../../web-client/src/operator/RoleTasks.tsx#L575-L583)、[L661-L668](../../web-client/src/operator/RoleTasks.tsx#L661-L668)、[L687-L706](../../web-client/src/operator/RoleTasks.tsx#L687-L706)、[L817-L819](../../web-client/src/operator/RoleTasks.tsx#L817-L819)；`operator.css` [L71-L78](../../web-client/src/operator/operator.css#L71-L78) |
      | `age-edit` | [927:1225](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=927-1225) | `/app/operator?view=ageEvidence` · `identity_verifier` | `stage === "edit"` 且已选区间且无 `fieldErrors`（含首次选定区间、修正错项、或从核对／失败返回修改）；支持继续输入、点击「核对本次内容」或返回工作台 | `RoleTasks.tsx` [L156-L190](../../web-client/src/operator/RoleTasks.tsx#L156-L190)、[L300-L306](../../web-client/src/operator/RoleTasks.tsx#L300-L306)、[L674-L677](../../web-client/src/operator/RoleTasks.tsx#L674-L677)、[L780-L798](../../web-client/src/operator/RoleTasks.tsx#L780-L798)、[L910-L930](../../web-client/src/operator/RoleTasks.tsx#L910-L930) |
      | `age-review` | [927:1329](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=927-1329) | `/app/operator?view=ageEvidence` · `identity_verifier` | `prepare` 通过后进入 `stage === "review"` 且 `ack === false`；字段冻结展示固定对象／区间／引用／截止摘要，未勾选时「确认登记」禁用且 `submit(false)` 禁止写入 | `RoleTasks.tsx` [L361-L368](../../web-client/src/operator/RoleTasks.tsx#L361-L368)、[L374-L384](../../web-client/src/operator/RoleTasks.tsx#L374-L384)、[L515-L518](../../web-client/src/operator/RoleTasks.tsx#L515-L518)、[L584-L588](../../web-client/src/operator/RoleTasks.tsx#L584-L588)、[L613-L638](../../web-client/src/operator/RoleTasks.tsx#L613-L638)、[L828-L861](../../web-client/src/operator/RoleTasks.tsx#L828-L861) |
      | `age-confirmed` | [927:1427](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=927-1427) | `/app/operator?view=ageEvidence` · `identity_verifier` | 在 `age-review` 显式勾选确认（`ack === true`）；启用「确认登记」触发 `submit(true)`，取消勾选退回 `age-review`，点击「返回修改」保留原输入退回编辑 | `RoleTasks.tsx` [L374-L384](../../web-client/src/operator/RoleTasks.tsx#L374-L384)、[L634-L638](../../web-client/src/operator/RoleTasks.tsx#L634-L638)、[L828-L861](../../web-client/src/operator/RoleTasks.tsx#L828-L861) |
      | `age-sending` | [927:1528](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=927-1528) | `/app/operator?view=ageEvidence` · `identity_verifier` | 点击「确认登记」进入 `stage === "sending"` 请求 `POST /api/v2/ops/age-evidence`；冻结展示提交原内容且无重发／返回，锁定导航、切岗与侧栏「技术运维」入口 | `RoleTasks.tsx` [L374-L392](../../web-client/src/operator/RoleTasks.tsx#L374-L392)、[L504-L505](../../web-client/src/operator/RoleTasks.tsx#L504-L505)、[L639-L643](../../web-client/src/operator/RoleTasks.tsx#L639-L643)、[L944-L951](../../web-client/src/operator/RoleTasks.tsx#L944-L951)；`OperatorApp.tsx` [L75-L79](../../web-client/src/operator/OperatorApp.tsx#L75-L79)、[L206](../../web-client/src/operator/OperatorApp.tsx#L206)、[L222-L227](../../web-client/src/operator/OperatorApp.tsx#L222-L227)、[L290-L305](../../web-client/src/operator/OperatorApp.tsx#L290-L305) |
      | `age-accepted` | [928:1307](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=928-1307) | `/app/operator?view=ageEvidence` · `identity_verifier` | 请求在同 `epoch` 返回成功进入 `stage === "accepted"`；如实提示无独立记录编号、仅代表接受区间证据而不代本人授权或监护同意，提供「结束本次登记」与回工作台 | `RoleTasks.tsx` [L393-L399](../../web-client/src/operator/RoleTasks.tsx#L393-L399)、[L613](../../web-client/src/operator/RoleTasks.tsx#L613)、[L644-L648](../../web-client/src/operator/RoleTasks.tsx#L644-L648)、[L956-L985](../../web-client/src/operator/RoleTasks.tsx#L956-L985) |
      | `age-rejected` | [928:1397](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=928-1397) | `/app/operator?view=ageEvidence` · `identity_verifier` | 收到非 401/403 的确定拒绝进入 `stage === "rejected"`；清 `prepared` 保留输入并展示明确失败说明，禁止自动重发，可「返回修改」或改后重核 | `RoleTasks.tsx` [L416-L425](../../web-client/src/operator/RoleTasks.tsx#L416-L425)、[L519-L528](../../web-client/src/operator/RoleTasks.tsx#L519-L528)、[L649-L653](../../web-client/src/operator/RoleTasks.tsx#L649-L653)、[L820-L827](../../web-client/src/operator/RoleTasks.tsx#L820-L827)、[L910-L930](../../web-client/src/operator/RoleTasks.tsx#L910-L930) |
      | `age-unknown` | [928:1492](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=928-1492) | `/app/operator?view=ageEvidence` · `identity_verifier` | 遇到超时／断网／408／5xx／响应不确定（`uncertain`）保留原 `prepared` 与输入进入 `stage === "unknown"`（`unknownAck === false`）；冻结内容，禁止重发与未勾选 `reset()`，提示经可信记录人工核对 | `RoleTasks.tsx` [L426-L431](../../web-client/src/operator/RoleTasks.tsx#L426-L431)、[L506-L514](../../web-client/src/operator/RoleTasks.tsx#L506-L514)、[L654-L660](../../web-client/src/operator/RoleTasks.tsx#L654-L660)、[L1001-L1038](../../web-client/src/operator/RoleTasks.tsx#L1001-L1038)；`client.ts` [L85-L105](../../web-client/src/operator/client.ts#L85-L105) |
      | `age-unknown-done` | [930:2349](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=930-2349) | `/app/operator?view=ageEvidence` · `identity_verifier` | 在 `age-unknown` 显式勾选人工核对确认（`unknownAck === true`）后启用「仅结束本机待核对」；取消勾选退回 `age-unknown`，结束调用 `reset(true)` 并明示非服务端接受响应 | `RoleTasks.tsx` [L506-L514](../../web-client/src/operator/RoleTasks.tsx#L506-L514)、[L589-L591](../../web-client/src/operator/RoleTasks.tsx#L589-L591)、[L654-L660](../../web-client/src/operator/RoleTasks.tsx#L654-L660)、[L725-L736](../../web-client/src/operator/RoleTasks.tsx#L725-L736)、[L1001-L1038](../../web-client/src/operator/RoleTasks.tsx#L1001-L1038) |
      | `role-lost` | [930:3078](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=930-3078) | `/app/operator?view=ageEvidence`→`forbidden`/`access` · 权限变更后无该岗 | ① 提交中 HTTP 403 立即清 `state`、设 `roleLost` 并调 `revokeAccess()` 取消在途请求、清队列／详情／草稿并重读角色；② 任务／`sending`/`unknown` 期间失权立即清私密材料与未提交输入（待确认态仅留用户编号最小引用），展示「岗位权限已变化 · 原任务停止」与角色入口 | `RoleTasks.tsx` [L105-L146](../../web-client/src/operator/RoleTasks.tsx#L105-L146)、[L403-L415](../../web-client/src/operator/RoleTasks.tsx#L403-L415)；`useOperator.ts` [L90-L101](../../web-client/src/operator/useOperator.ts#L90-L101)、[L120-L169](../../web-client/src/operator/useOperator.ts#L120-L169)、[L396-L418](../../web-client/src/operator/useOperator.ts#L396-L418)；`OperatorApp.tsx` [L65-L69](../../web-client/src/operator/OperatorApp.tsx#L65-L69)、[L306-L327](../../web-client/src/operator/OperatorApp.tsx#L306-L327) |

    - [x] 源码／状态接线对照完成
    - [x] 静态审查完成（`check_admin_role_workspaces_static.py`、`check_operator_pages_static.py`、`check_web_client_static.py` 与 `git diff --check` 通过；未运行编译，遵循全局不自行编译规则）
    - [ ] 目标端视觉／键鼠或系统交互验收通过
    - [ ] 真实业务与失败恢复验收通过
    - 依赖／未验证项／下一步：整个「39 个任务态」父项（发布授权、实际收款、实际退款）及真实 Go 后端／浏览器键鼠焦点／网络恢复运行仍待验收。

### Web 技术运维 · B11／V03

- [ ] **14 稿及四岗接线。** 总览、服务与依赖、Worker 列表／详情、版本发布、迁移账本、告警列表／事件详情、操作审计、环境权限、发布核对／确认、未连接、受限。按 `ops_viewer`、`ops_oncall`、`ops_release`、`ops_admin` 验环境隔离、角色权限与审计，演示视图和真实数据有明显区分。
- [ ] **执行能力如实展示。** 验构件、备份恢复、审批、队列、检查点、未知处置与真实不可用状态。发布与重启执行器作为受阻的外部能力单独记；只在产品范围变更且授权明确后实施。[运维页面](../../web-client/src/operations) · [执行前置](../../internal/operations/service.go#L410)
  - [x] **子项 · B11／V03 · 技术运维发布核对与不可用状态（`/app/ops` 五条发布与系统状态路径）**（整套 14 稿与真实运行／构建验收仍待签收，父项保持 `[ ]`）
    - 任务 ID / 本文标题 / 客户端和布局：`B11／V03` / 技术运维发布核对与不可用状态 / Web 技术运维（`web-client/src/operations`，`/app/ops` 桌面及窄屏响应式布局）
    - 负责人 / 状态 / 分支和 HEAD / 最近更新时间：Antigravity (Gemini) / 待验收（源码补齐与静态审查完成；类型检查、构建与真实运行待验收） / `main` @ `f3a03e5` / 2026-11-19（UTC+08:00）
    - 范围：`793:25` 版本与发布（`releases`）、`793:30` 发布核对详情（`release`）、`793:33` 环境未连接（`disconnected`）、`793:34` 访问受限（`forbidden`）、`793:35` 发布确认（`confirm`），对照现行 Go `/api/ops/v1` 响应（[`handler.go`](../../internal/platform/opsapi/handler.go#L124-L216)、[`service.go`](../../internal/operations/service.go#L359-L420)）；业务后台 `operator`、Go、Figma 及其余页面保持只读。
    - 允许写入的文件：`web-client/src/operations/{context.tsx,useOperations.ts,OpsApp.tsx,OverviewPages.tsx,RecordPages.tsx,DetailPages.tsx,ActionForms.tsx,operations.css}`、本子项及自身三份 AI 跟踪。
    - 已确认并补齐的源码差异：
      1. `未连接态穿透与发布页区分`：此前直接打开 `?view=releases`／`?view=release`／`?view=confirm` 且尚未连接真实环境时，列表页提示「可以登记候选构件与核对证据」及「尚无发布准备记录」，详情与确认页显示「请先选择记录」；现已在未连接态显式切换为未连接提示／[`SystemState`](../../web-client/src/operations/OverviewPages.tsx#L387-L490) 并保留「连接环境」「返回版本与发布」「返回运维总览」出口，`503 environment_unconfigured` 同步清零过时会话上下文与快照。
      2. `发布对象与确认页分项展示及版本同步`：`ReleaseDetail` 此前将「目标环境 / 构件摘要」合并且未展示 `backup_ref`、`approval_ref`、`reason` 与记录状态；`ConfirmRelease` 亦将版本与构件、备份与审批合并展示且硬编码 `<Status state="blocked" />`。现已将**真实环境与域名、候选版本与记录状态、构件摘要（SHA-256）、备份恢复证据、外部审批证据、变更原因／影响范围**全部拆分为独立摘要项，并以 `latestRelease()` 同步 `preflight.release` 最新版本（[`DetailPages.tsx#L219-L234`](../../web-client/src/operations/DetailPages.tsx#L219-L234)）；`CreateRelease` 编辑保存后经 `detail.apply(row)`（[`DetailPages.tsx#L86-L93,L425-L432`](../../web-client/src/operations/DetailPages.tsx#L86-L93)）即时刷新详情并重拉 `/preflight`。
      3. `403 访问受限导航锁定修复与受限范围保留`：`useOperations.report()` 在 `403` 清空 `context` 前保留 `deniedScope`，并通过 `clearForbidden()` 与 `OpsLink`／`go()` 解除在 `?view=forbidden` 点击「查看角色与权限」「返回运维总览」或侧栏链接时的无限跳回锁定（同时保持 `token`/`context`/`snapshot` 为空，绝不泄露受限数据）；增加「切换账号重新连接」出口。
      4. `前置核对失败／加载态／角色写守卫与表单规则对齐`：`usePreflight()` 新增 `loading` 态，在 `ReleaseDetail`／`ConfirmRelease` 对加载中／读取失败／`unknown` 分别展示「正在核对」「核对读取失败」与「重新读取」按钮，绝不显示为可发布或已成功；非 `ops_release`（`!capabilities.release_write`）隐藏写按钮并禁用确认页本地核对输入框；`CreateRelease` 对齐 Go `evidenceID`／`digest`／rune 长度与控制字符校验（[`ActionForms.tsx#L117-L239`](../../web-client/src/operations/ActionForms.tsx#L117-L239)）并在 `operations.css` 补全 `.ops-workspace .field` 错误样式（[`operations.css#L786-L793`](../../web-client/src/operations/operations.css#L786-L793)）；「执行发布」与「确认并提交」继续保持禁用占位，未接入任何发布或重启执行器。
    - 逐节点回填：

      | Figma 节点 | 路径与角色 | 入口 → 触发条件 → 实际结果与源码行 |
      |---|---|---|
      | `793:25` 版本与发布 | `/app/ops?view=releases`（`?mode=demo`／`ops_viewer`／`ops_oncall`／`ops_release`／`ops_admin`／未连接） | [`OpsApp.tsx#L271-L272`](../../web-client/src/operations/OpsApp.tsx#L271-L272) → [`Releases`](../../web-client/src/operations/RecordPages.tsx#L89-L239) + [`CreateRelease`](../../web-client/src/operations/ActionForms.tsx#L123-L240)：按 `capabilities.release_write` 控制「登记发布准备」显隐（`demo` 下禁用），四项指标分别区分当前环境版本、候选版本、当前已读被阻止数、执行权限「未接入」；未连接态显示「尚未连接真实环境」与「连接真实环境／查看未连接说明」按钮，读失败提示「发布准备记录读取失败」；保留 `CommandModal` 原 `request_id` 幂等与 `4xx`/`5xx`/`environment_unconfigured` 处理（[`ActionForms.tsx#L8-L116`](../../web-client/src/operations/ActionForms.tsx#L8-L116)）。 |
      | `793:30` 发布核对详情 | `/app/ops?view=release&id=...`（`?mode=demo` 未带 `id` 默认预览 `REL-DEMO-004`；真实模式需先连接且校验 `id`） | [`OpsApp.tsx#L49-L55,L284-L285`](../../web-client/src/operations/OpsApp.tsx#L49-L55) → [`useDocument`](../../web-client/src/operations/DetailPages.tsx#L28-L94) + [`usePreflight`](../../web-client/src/operations/DetailPages.tsx#L134-L218) + [`ReleaseDetail`](../../web-client/src/operations/DetailPages.tsx#L235-L435)：未连接渲染 [`SystemState`](../../web-client/src/operations/OverviewPages.tsx#L387-L490)，读详情失败渲染 [`MissingDetail`](../../web-client/src/operations/DetailPages.tsx#L92-L133)（含「重新读取」）；「发布对象」面板分项展示目标环境、候选版本与状态、SHA-256、备份恢复、审批证据、变更原因（L282–L308）；「执行门槛」与「前置核对清单」读取 `GET /releases/{id}/preflight` 展示 4 项 check（`environment`/`artifact`/`recovery`/`executor`），加载与读取失败保持阻止且提供「重新读取」（L310–L403）；按 `capabilities.release_write` 且 `state === "blocked"` 控制「补齐发布资料」，「执行发布」始终禁用占位（L404–L432）。 |
      | `793:33` 环境未连接 | `/app/ops?view=disconnected` 或未连接直达 `release`/`confirm`，及 `503 environment_unconfigured` | [`useOperations.ts#L38-L65`](../../web-client/src/operations/useOperations.ts#L38-L65) + [`OpsApp.tsx#L292-L297`](../../web-client/src/operations/OpsApp.tsx#L292-L297) → [`SystemState`](../../web-client/src/operations/OverviewPages.tsx#L387-L490)：`environment_unconfigured` 立即断开本地会话与过时快照；未连接态展示三步接入指引与「连接环境／返回版本与发布／返回运维总览」；已连接预览 `view=disconnected` 时明确提示当前已连接环境及仅作状态结构说明。 |
      | `793:34` 访问受限 | `/app/ops?view=forbidden` 或任意请求返回 `403`（`environment_forbidden`/`role_forbidden`/`origin_forbidden`） | [`useOperations.ts#L28-L65`](../../web-client/src/operations/useOperations.ts#L28-L65) + [`context.tsx#L52-L55`](../../web-client/src/operations/context.tsx#L52-L55) + [`OpsApp.tsx#L78-L89,L103`](../../web-client/src/operations/OpsApp.tsx#L78-L89) → [`SystemState`](../../web-client/src/operations/OverviewPages.tsx#L387-L490)：`403` 立即清空凭据／上下文／快照并保留 `deniedScope` 与 `requestID`，自动跳转 `forbidden` 隐藏受限数据；提供「切换账号重新连接」「查看角色与权限」「返回运维总览」，经 `clearForbidden()` 解除离开限制同时保持零受限数据。 |
      | `793:35` 发布确认 | `/app/ops?view=confirm&id=...`（`?mode=demo` 未带 `id` 默认预览 `REL-DEMO-004`；真实模式仅作不可提交核对） | [`OpsApp.tsx#L49-L55,L290-L291`](../../web-client/src/operations/OpsApp.tsx#L49-L55) → [`ConfirmRelease`](../../web-client/src/operations/DetailPages.tsx#L436-L566)：未连接转 [`SystemState`](../../web-client/src/operations/OverviewPages.tsx#L387-L490)，记录缺失／失败转 [`MissingDetail`](../../web-client/src/operations/DetailPages.tsx#L92-L133)；右上角状态标签随 `preflightError`/`preflightLoading`/`activeRecord.state` 动态变化（绝不硬编码成功或可发布，L474–L482）；分项展示目标环境、候选版本、构件摘要、备份恢复、审批证据、影响范围（L483–L509）；非 `release_write` 禁用本地核对输入（L510–L534）；`target` 不匹配当前环境标红提示（L460–L463,L525–L533）；核对失败可「重新读取」，「确认并提交」无条件 `disabled` 且不绑定提交请求（L535–L563）。 |

    - [x] 源码／状态接线对照完成
    - [x] 静态审查完成（`scripts/check_operations_static.py`、`scripts/check_web_client_static.py`、`scripts/check_admin_role_workspaces_static.py`、`scripts/check_operator_pages_static.py`、`@babel/parser` 7 份 TS/TSX 语法解析、`postcss.parse`、`git diff --check` 与 CRLF／UTF-8 无 BOM 核对通过；未运行编译、类型检查或测试，遵循全局不自行编译规则）
    - [ ] 目标端视觉／键鼠或系统交互验收通过
    - [ ] 真实业务与失败恢复验收通过
    - 依赖／未验证项／下一步：未执行 `tsc` 类型检查、`vite` 构建、Go 测试、浏览器交互及真实环境联调；整套 14 稿（含非发布页及跨角色联调）与部署执行器接入边界仍待后续授权后验收。

### 公共后端与授权后验收 · B01–B12／V01–V03

- [ ] **合同与授权。** 优先处理部分受限摘要、可区分的会话结束原因、注销受理／查询／最终结果、通知版本与评论可见性。冻结错误码、版本、作用域、同键回执和清理机制后再并行派前端。
- [ ] **正式渠道。** 分别记录邮箱／短信、微信／QQ／Apple、年龄核验、地图、设备推送的配置负责人和已验证客户端。验证取消、拒绝、过期、回调、去重、排队与真实送达。配置与真实终止条件缺失时继续交付可用的离线／不可用页面和静态证据。
- [ ] **数据库与发布前环境。** 有权限的负责人验证 001–013 迁移、回滚、真实权限变更、并发幂等、媒体失权清理和作业消费。完整搜索接收端、运维执行器、正式发布与支付相关决定单列阻塞，不代填完成。[交付手册](DELIVERY_GUIDE_2026_10_25.md)
- [ ] **文档中心对账。** 如获 Figma 写入授权，局部修订 35:15 的旧句并保留原评论，挂现行实际代码和验收状态。它独立于功能实现。[需更新的快照](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3?node-id=35-15)

推荐顺序为 **并行做明确缺口 → 按功能完成双前端状态 → 分端拿证据 → 合并核对**。第一轮可发给四个负责人：合同与注销／会话、公共 Compose、Web 消费端、业务后台；技术运维和分端验收可独立准备已存在的路径。共享 Store／路由入口、OpenAPI／DTO、公共样式和后台会话逻辑各指定一人合并，避免同一个文件由多个任务同时改。原始台账保留设计键与历史证据，各 AI 回填自己任务的实际交付和剩余项。

**本轮验收边界。** Figma 节点结构与三处原稿截图、代表性源码及台账已静态核对。未运行编译，遵循全局不自行编译规则。测试、设备／浏览器运行、供应商及数据库均未验证。
