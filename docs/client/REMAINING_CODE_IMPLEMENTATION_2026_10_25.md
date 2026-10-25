# 剩余编码续作与实际边界（2026-10-25）

任务：`AI-20261025-REMAINING-CODE`。用户要求全面补齐其它未完成编码。本批在已有工作树上继续写入源码，没有回退 Android 页面迁移、地图、官方认证、Web、运维或 Gemini 文档成果。本轮起点为 `main/77c479a`。交付核对时发现同一工作树已出现 `eed8fcd`，包含大部分既有和本批源码；**本会话没有执行提交或推送**。当前为 `main/eed8fcd`，另有Web回跳排版／异常守卫和交接记录尚未提交。用户随后明确“暂时保留占位，先交付本轮源码”，运维执行器与完整搜索接收端按此范围不继续实施。

iOS现有页面主体是Kotlin Compose Multiplatform，Swift提供宿主和系统／SDK桥，**不是全Swift／SwiftUI页面实现**。用户已明确选择“不迁移，保留Swift宿主＋Compose页面”，并确认继续KMP共用逻辑；本批没有进行页面层迁移。

## 本批源码

| 事项 | 实际修改与入口 | 边界 |
|---|---|---|
| 数据库查询 | `Filter.States/ExcludeStates`、内存／Postgres 同语义，`List/Count` 共用谓词；领养占用只读至多2条活跃记录；候补／关闭仍完整处理目标范围 | 不截断保全、注销、资料导出或其他必须完整处理的业务 |
| 索引与清理查询 | [011迁移](../../migrations/011_mobile_query_indexes.sql) 为44表补数组GIN及键前缀索引，已有等价GIN不重复建；转义字面前缀；孤立媒体按文件内的ID只读本批目标 | 未应用迁移，未声称查询计划／性能已实测 |
| Web多副本续期 | `identity.BrowserRefresh` 与轮换同事务保存30秒加密回执；核对旧刷新证明、账号代次、继任会话及撤销；普通原生刷新仍拒绝重放 | 各副本仍须固定同一密钥；无内存回执或粘性会话依赖；未实测并发 |
| Desktop官方授权 | `DesktopProviderAuth`＋`/app/desktop-provider`；复用正式Web应用及HTTPS官方回跳；临时IPv4本机端口检查Host／Origin／随机路径／原state；表单POST返回本次结果 | 不向浏览器传应用会话；取消／超时关闭监听；原有账号关联确认仍由共享Store处理；未运行浏览器／安装包 |
| 原生设备推送 | 应用级 `DevicePushCoordinator`、Android FCM、iOS APNs；保存后的账号许可与设备系统权限分别管理；Token轮换只替换同会话绑定，版本查询可恢复不确定结果；无旧Token也能撤回当前会话；全局关闭与绑定清除同事务 | FCM默认不初始化，APNs默认NO；供应商接收不代表设备送达；仅服务器汇总更新角标；点击只回应用并刷新，不覆盖原任务或伪造已读 |
| Worker多副本协调 | `internal/jobcoord`＋[012迁移](../../migrations/012_worker_checkpoints.sql)；独立Postgres会话持有作业锁、检测连接丢失、数据库位点；业务投递租约和外部幂等键保留 | 开发内存实现只保证本进程；本地worker文件只是恢复缓存；崩溃后的业务仍依靠已有幂等重试，不能宣称恰好一次 |
| 私有媒体存储 | `mediablob.Storage`／Disk／HTTPS S3路径式适配；上传仍处理图片、限制大小；读取仍走原API权限并校验大小／SHA256；孤立清理／保全／删除接同一端口 | 默认local；S3仅接受从未开启版本控制的独立桶；删除标记不算物理清除；不会自动迁移旧目录，也不承诺任意云厂商的所有S3扩展均兼容 |
| 搜索／审计事件 | 2个独立具名worker消费既有投递租约，严格网关回执与稳定幂等键；仅投递失效／审计元数据，不复制正文、私信、材料、Token或精确位置 | 不是完整搜索引擎实现；外部接收端须持久化去重，搜索候选仍回源核验；未连接真实网关 |
| 阅读目录 | 四份协议完整草案＋双城物种／行为矩阵，SDK草案补原生推送与Desktop授权；沿用公共正文、目录和下载合同 | 15份材料、10类；新增5份均为draft，无生效时间或主体，不授予城市准入，不改原合同／资格；法规内容尚未核验为现行法律 |
| 交付接线 | 12份顺序迁移；8个worker、13个Compose服务；公开配置／Secret文件加载、Android及iOS手动CI变量、APNs Archive能力、Desktop模板与运行模块 | 所有配置仍占位；没有执行CI、构建、迁移、部署或打包 |

消费者合同为 **245 Paths／275 Operations／124 Schemas**，Web派生 **126 Schemas／236消费者操作**，共享合同核对 **75对DTO**。技术运维仍是独立的13路径／15操作／25 Schemas。计数是源码事实，不是功能或运行验收率。

## 配置与接口

- Android：[占位模板](../../deploy/examples/android.env.example) 的4项 `MND_FCM_*` 为公开应用配置，必须一起提供；服务端推送凭据仍只在网关。固定 `firebase-messaging:26.0.1`；按[Firebase原生接收及Token回调](https://firebase.google.com/docs/cloud-messaging/android/get-started)接线，未解析SDK或执行Gradle。
- iOS：[占位模板](../../deploy/examples/ios.env.example) 中 `MND_APNS_ENABLED=NO`。开启时，Archive由脚本注入实际Profile匹配的 `aps-environment`；开发者自行运行Xcode时也须提供匹配entitlements。原生桥依照[Apple APNs注册接口](https://developer.apple.com/documentation/uikit/uiapplication/registerforremotenotifications%28%29/)接入，真实设备令牌、证书和权限仍待验证。
- Desktop：[公开运行配置模板](../../deploy/examples/desktop.env.example) 中AppID须与Web构建一致；服务端仍配置Web应用的Secret和固定HTTPS回跳。不把Web构建变量误当作Desktop包内Secret。
- 媒体：[服务端配置模板](../server/environment.example)／[运行配置模板](../../deploy/runtime.example.env)。S3需要私有HTTPS端点、region、独立bucket、专用prefix及凭据文件，权限至少覆盖对应前缀的读取／写入／删除／列举、桶版本状态及PublicAccessBlock读取；上传／读取要求桶级4项公开访问阻断均为true，不支持核验的兼容实现保持关闭。当前签名按[AWS SigV4](https://docs.aws.amazon.com/AmazonS3/latest/developerguide/sig-v4-header-based-auth.html)实现；[版本桶删除行为](https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html)不能当作物理清除。只检查源码，尚未对接实际S3服务。
- 投影网关：`MND_SEARCH_PROJECTION_URL/TOKEN_FILE`、`MND_AUDIT_PROJECTION_URL/TOKEN_FILE`。`POST <配置端点>/events`，Bearer服务凭据，`Idempotency-Key=<consumer>:<event_id>`；严格返回 `{consumer,event_id,accepted}`。`accepted=true` 代表可靠接收，不代表搜索已更新、审计已长期归档或删除已完成。
- 已有部署配置升级须追加两位独立worker的具名凭据／consumer范围；不能重新初始化并覆盖已有Secret。实际执行入口仍见[交付手册](DELIVERY_GUIDE_2026_10_25.md)。

## 本轮验证

仅执行只读脚本、Python AST／XML／YAML解析、Kotlin／Swift／TS语法树、Go格式语法读取、合同派生一致性和 `git diff --check`。新增7个Go回归函数（含S3版本状态子场景）仅写源码，**没有执行测试**。详细文件指纹与实际检查结果见[本批证据](remaining-code-evidence-2026-10-25.json)。

遵循用户及AGENTS规则，未运行编译、类型检查、build、assemble、test、lint、应用或服务启动、CI、迁移、设备／浏览器操作；没有提交或推送。语法树不检查类型、平台ABI、SDK依赖解析、权限弹窗或真实设备送达。

## 仍未完成，不能写成全部交付

1. 技术运维的部署／重启／人工任务重试执行器、真实构件／备份／审批证据验证、外部监控和告警通知仍未实现对应外部适配；现有界面按设计保持关闭，不以填写引用放行。生产平台及执行合同未提供，不能把本批的worker事件接线算作这些执行器。
2. 搜索消费端目前发送最小失效事件，完整搜索引擎建索引、查询和删除回执接收端仍需按实际引擎实施。本批未替用户选择或假装已运行外部引擎。
3. 正式AppID、地图Key、推送／短信／邮件／年龄核验等供应商配置，城市规则的实际审核发布、运营主体与正式协议、签名／域名／服务器仍待真实配置。
4. SDK、媒体签名、跨副本故障恢复、所有Figma状态及手机／平板／Web／Desktop逐端运行验收仍未执行。现有740条源码索引不等于逐状态验收。

上述区分代码余项、配置余项与运行余项，不能把后两者改记成代码已经运行，也不能把第一、二项藏进“只差配置”。
