# 年龄与成年资格页面及接口（2026-10-25）

本阶段补的是 Figma `ageRange`、`adultAssurance` 和 `adultNotice` 至 `adultTaskReview` 的专用源码。此前这些状态只宽泛归到登录页，不能视为已落实。本阶段仍只有源码及静态证据；正式核验通道为空，不产生真实成年结论，也不代表全部 Figma 页面或设备验收完成。

## 页面与复用

- 公共 Compose `AgePages.kt`：年龄使用范围、成年业务说明、用途及材料范围确认、通道可用性、打开服务、等待／原请求查询、取消、失败、不可用、未达资格、过期／已被后续复核替换、通过、返回原任务再核对。
- `AgeAssuranceStore.kt`：Android、iOS、Desktop 共用状态，按当前账号清理；用途确认冻结任务 key／label。外链打开与回到应用都不能设置成年资格；只有真实服务端状态驱动通过页面。
- `FeaturePage.Age`／`Adult` 与 Web `/age`／`age-check`：账号安全、预约、协议、接回与披露表单入口接入，保留原 Store／表单。Web 跳转前等待原草稿保存成功；核验后返回原任务，重新读角色、当前手机、资料与版本，仍需用户主动提交。
- 复用已有 `TaskColumn`、`TaskPrimary`／`TaskSecondary`、`TaskSection`、`TaskNotice`、`NavigationRow`，Web 复用 `TaskPage`／`Notice`／`Card`／`RowLink`。新增警示色只作为全局状态 token `#fff5de`，来自原 Figma，不增加页面主题。

年龄区间页用于说明保护范围，点击“年满18岁”不会更新资格。未达资格继续展示限制与私密复核入口，不提供“临时改成成人”。当前手机、监护、年龄、业务角色仍是不同事实。

## 服务接口

| 方法与路径 | 行为 |
|---|---|
| `GET /api/v2/auth/age-assurance/config` | 当前登录账号可读的通道名称、隐私说明、可用性；不是核验结论 |
| `GET /api/v2/auth/age-assurance/current` | 本账号当前请求；没有记录返回 `data:null` |
| `POST /api/v2/auth/age-assurance` | 明确同意、原编号和冻结任务；先保存请求，再调用供应商 |
| `POST /api/v2/auth/age-assurance/{id}/query` | 只查询本账号原请求，不创建核验；最小查询间隔3秒 |

消费者只有三份最小 DTO：`AgeProviderInfo`、`AgeAssuranceInput`、`AgeAssuranceStatus`。供应商 nonce、reference、证据引用和原件不在客户端状态接口中。可打开的短时官方页面 URL 只在本账号等待状态返回，不写本机普通草稿；结果终结时清掉 URL／nonce／reference。

请求复用 `auth_challenges`，不用新表；15分钟核验期限与约24小时的短期恢复记录期限分开。现有维护任务清理短期记录，`operations` 账本保留原编号与原请求指纹；记录已清理后重放相同编号仍只返回过期，不再调用供应商。并发新编号也会复用本账号尚未结束的核验，并记入同一原请求的账本引用。

服务端仅在供应商 reference／nonce、期限、固定状态及必要来源全部有效时写入资格；原会话在仓库事务内再检查。返回通过时最多接受366天有效期，与原具名运营入口的范围一致。核验过程中已有新年龄复核结果时不覆盖；已通过的结果过期或被后续复核替换，也不会继续投影为当前有效。

`RecordTrustedAgeProof` 抽取原 `RecordAgeEvidence` 的事务内保存与审计，原具名运营权限仍由运营路由校验；消费者没有自报 `adult`、上传证件到公开媒体或绕过 `requireInteractionContract()` 的入口。核验通过不自动发布、签约、确认预约或写入业务操作。

## 核验通道占位合同

这是本阶段的工程适配边界，**不是已经选择或开通某家正式核验供应商**。与已有邮件／短信网关一样，由部署方的可信网关对接实际供应商。

服务端环境项：`AGE_GATEWAY_URL`、`AGE_GATEWAY_TOKEN`、`AGE_PROVIDER_NAME`、`AGE_PROVIDER_PRIVACY_URL`、`AGE_PROVIDER_LAUNCH_ORIGIN`。当前模板全部为空，保持关闭；API／启动源码均没有读取真实凭据。完整配置必须为 HTTPS，应用可打开页面限定到一个配置好的 HTTPS origin；不跟随网关 HTTP 重定向，不把 endpoint 或 Secret交给客户端。

网关提供：

- `POST /requests`：接收 `purpose=adult_business`、服务端随机 `reference`／`nonce` 和 `expires_at`；`Idempotency-Key` 为原 reference。相同 reference 不得创建新的核验。
- `POST /requests/{reference}/query`：接收同一份请求，查询原结果；此读取不发送创建用幂等头，不能缓存首个 pending 结果后永不更新。
- 返回严格最小 JSON：`reference`、`nonce`、`state`、`launch_url`、`valid_until`、`evidence_ref`。状态只接受 `pending/passed/not_eligible/cancelled/failed/expired`；**passed 必须代表有效成年资格核验通过**。有效期限和必要来源是服务端证据，不取自客户端。

请求不发送 M&D 账号 ID、任务正文、证件或照片。响应拒收合同外字段；失败日志不记录原响应。网络异常及结果丢失保留 unknown，只查询原 reference，不自动重新 Create。供应商尚未配置或不可用时页面允许继续浏览与保留任务，不伪造通过。

## 设计与静态证据

本阶段读取并保存原上下文／PNG：`460:17378`、`460:17413`、`506:6819/6824/6829/6834/6839/6844/6849/6854/6859/6864`，位于 [设计证据目录](../design/figma-study/implementation-2026-10-24)。这些是设计源截图，不是应用运行结果；没有把设计截图作为实现素材。

新增 `check_age_assurance_static.py` 核对最小 DTO、原编号、角色／会话、期限、可信结果及多端入口；`age_assurance_test.go` 包含同意、重放、另账号、错误挑战、结果替换、unknown、过期与清理后重放的回归源码，**未执行测试**。合同现为235路径／264操作／116服务schema与118 Web schema；69对DTO静态一致。

未运行编译、类型检查、依赖解析、test、lint、服务／浏览器／模拟器／应用或实际供应商。真实通道映射、超时与进程恢复、手机和平板布局、焦点／IME、浏览器弹窗及原生外链仍待获得运行授权后的分别验收。继续其余专用流程，不将740条源码族追踪当作全部页面完成。
