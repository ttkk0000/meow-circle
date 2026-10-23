# Android 手机／平板与服务端闭环续作

2026-10-23，AI-20261023-ANDROID-FINISH；main / 5de631b 加当前工作树。本批补源码和静态集成，不将整套产品标为运行完成。沿用批准的 Figma V2 默认浅色、Android 手机／平板共用 Store 和公共 KMP；苹果 UI 后置，未升级依赖。

## 本批已写入的行为

| 业务 | 源码与处理方式 |
|---|---|
| 人员／受众 | `AndroidPeopleFields.kt`、`AndroidSocialFeatures.kt`、`AndroidComposeControls.kt`、`MobileFeatureStore.kt`：分页读取当前已接受好友，按已加载昵称筛选，单选邀请／多选受众，展示已选摘要。加载／分页／失败／失权独立展示，确认前及实际发送前重核，不静默放宽受众。好友申请从明确打开的资料发起；没有全站编号、手机号或邮箱枚举。 |
| 预约参与人 | 创建预约保留原领养／寻宠／宠物／披露事项绑定；`requireAppointmentContext()` 重新读取原事项核对双方，服务端最终检查权限、资格及状态。此路径不要求双方先加好友，不能被好友选择误伤。 |
| 无坐标求助 | `AndroidRegionFields.kt`、`AndroidCareFeatures.kt`、`rescue.go`：城市选择加文字区域；新发布为 `location_precision=area`、坐标为零，明确表示没有地图点。编辑旧粗点时须主动保留原地点，改变城市／区域即不复用旧点。私密补充不进入公开摘要。 |
| 城市提醒 | `AndroidAccountFeatures.kt`、`nearby.go`、`query.go`、`push_policy.go`：城市订阅为 `location_precision=city`，服务端归零坐标和半径，匹配当前城市；只有文字区域的事件不参与距离匹配。已有空精度字段保持旧点位语义，不将零坐标伪装成地点。旧点位订阅只有用户明确确认后才转城市订阅。 |
| 深链续接 | `MobileViewModel.kt`、`MainActivity.kt`、`AndroidMobileApp.kt`、Manifest：严格资源白名单／规范正 int64，拒绝链接携带查询凭据；HTTPS 仅同配置域名的公开分享。编辑／确认／登录／恢复中保留至多一条待查看资源，由用户打开。系统恢复只保存资源路由、对象和账号归属；切换账号清理原私有任务，打开后重新取数及鉴权。 |
| 全部已读 | `inbox_snapshot.go`、`inbox.go`、handler、`InboxReadAllModels.kt`、`MobileApi.kt`、`MobileStore.kt`、`AndroidInboxReadAll.kt`：显式 POST 获取十分钟快照，再主动确认；新消息不核销、不接受请求或办理待办。重核会话／成员权限，读位置只前进；重复的站内聊天通知不重复计数。失权项跳过，成功后重拉真实汇总。 |
| 已读失败／恢复 | 记录原快照、请求号和字节，未知结果可暂收起并继续核对；确定过期可主动重新获取范围并再次确认。持久化 `uncertainReplay` 保留首次提交不明的事实，后来的拒绝不能反证首次未提交；进程恢复仍保留原请求。免打扰数量单独预览，未擅自改变现行角标政策。 |
| 分享落地 | `public_web.go`、`share.go`、router：`/share/{entries\|pets\|cases\|offers}/{id}` 返回服务端 HTML 摘要、App 按钮和游客求助入口；JSON API 的 `relative_url` 指向 HTML。再次检查生命周期、公开权限与报价有效期，私密／撤回／未知统一不可用。模板转义；只允许同源媒体路径；忽略 Host、转发头和查询参数；no-store／no-referrer／CSP。图片按原比例适配窄屏。 |
| 游客安全页 | `web/safety.html/css/js`：无需安装／注册，普通求助、隐私、正式权利通知、真实回执查询和原案补充；正式通知使用受限会话邮箱核验。反声明仍从 App 原案登录和核验，不伪装普通举报。只调用同源安全接口，`credentials: omit`；默认内存，用户勾选后才使用独立 sessionStorage，清本机凭据不声称删除服务端事项。 |
| 游客提交恢复 | 提交前冻结原请求／编号；超时、非有效回执、5xx 均保持结果待核对，404 查询也不授权另建请求。原字节重试、受限凭据查询、补充版本冲突均保留输入。服务端 int64 回执／对象／版本先作词法保护再解析，请求使用精确十进制，避免 JavaScript Number 舍入。 |

人员列表当前用昵称首字占位，`PublicMember` 尚无公开头像字段；不能宣称已接真实头像。候选目录仅好友，不批量拉取不相干人员。群内原有成员管理仍有编号摘要和单独“查看资料”，未把这部分名称体验计为完整交付。

## 服务端领养复核补正

沿用[前批领养合同与实施](../server/ADOPTION_CLOSURE_2026_10_23.md)，本批修复历史报价预约恢复／确认绕过当前宠物安排、关闭公开招募后阻断已占用交接、接回达成后照护／求助入口不可用、恢复候补误复用旧协议、过期或重复接回确认等边界。重提／恢复再次核对拉黑和当前周期；重新进入候选保留历史但清除当前协议，必须重新确认；生效接回替代旧计划，实际接回后终结其他待处理提案。见 `adoption_placement.go`、`rescue.go`、`contracts.go`、`appointments.go`。

本批不新增表、DDL、SDK 或密钥配置。新增字段使用既有 JSON 数据列，原 008 迁移仍未执行；旧异常多安排数据不能自动选赢家。实际事务隔离、并发重试、回滚、扫描性能与原渠道行为仍需验证。

## 接口与配置交接

- 新增 `POST /api/v2/inbox/read-all/snapshot`（`InboxSnapshotInput` → `InboxReadSnapshot`）、`POST /api/v2/inbox/read-all`（`InboxReadAllInput` → `InboxReadAllResult`）。请求体含原 `client_request_id`；回执头如提供须相同。快照 ID／version 为 int64，确认不接受客户端自选消息游标。
- 游客页复用 `/api/v2/safety/session`、`contact-otp`、`contact-verify`、`tickets`、`tickets/{id}`、`requests/{key}`。凭据只在 `X-Safety-Token`，不进地址栏；刷新凭据过期不能凭回执编号恢复权限。
- `/share/` 已在 Go router 分派，安全页由现有 `web` 静态目录提供；部署必须一并携带三个 safety 文件，反向代理将 `/share/` 交给 Go，页面与 `/api/v2/` 同源。没有发布服务。
- Android `appLinkHost` 从既有 API base URL 取主机，运行只接受对应 HTTPS 域名。正式 HTTPS 域名、证书、`/.well-known/assetlinks.json` 的应用标识和实际签名仍须部署者配置。没有虚构签名指纹或域名关联结果。
- 当前分享元数据使用同源相对地址，正式域名下的社交平台抓取兼容性、绝对 OG URL 和分享渠道验收仍待落实；没有从不可信 Host 推导公开站点。

## 已做的核对与证据

已审阅并执行只读静态脚本；没有启动 API、浏览器、模拟器或业务测试。

| 检查 | 结果及限度 |
|---|---|
| OpenAPI 导出 | 214 路径、104 schemas；源码声明导出，不是接口运行结果 |
| server/shared 字段对齐 | 55 对 DTO，包括两个已读请求／两个结果，以及区域精度字段；无类型检查 |
| KMP 配置／源码 | 55 份 Kotlin／Swift，基础定界符、依赖别名、平台接线和请求字段通过；不是 Kotlin 编译／Android lint |
| Android 页面 | 30 扩展分派、单 Store、方法引用、现行窗口／键盘／资源约束静态通过 |
| 增量集成 | `check_android_finish_static.py`：34 个游客页面控件及引用、同源资源、精确 ID 合同、分享路由与单已读弹窗通过 |
| JavaScript／Go | `node --check web/safety.js` 语法通过；目标 19 份 Go 经 gofmt 格式／语法读取，无类型检查 |
| 回归源码 | 目标文件共 23 个 Go 用例（含前批领养 5 个）、5 个 KMP 用例；均未执行。覆盖快照新到消息／免打扰／失权／过期／单调读位置、领养边界、区域精度、分享注入／失效／int64 和未知请求恢复 |

[本批指纹与检查记录](android-remaining-evidence-2026-10-23.json)单独生成，历史证据保持原值。本批未提交／推送。未运行编译，遵循全局不自行编译规则。

## 仍未交付的部分及下一步

本批把账本中的人员／区域／续接／批量已读和最小网页从“待写”推进到“源码及静态”。仍不能说 Android 手机／平板或服务器整体已经验收：F02 内容收藏／关注合同、群成员名称／真实公开头像等源码体验余量继续按[当前账本](CURRENT_DELIVERY_STATUS_2026_10_23.md)跟进，不用本地布尔开关模拟。

下一阶段先补仍缺的具体功能合同与源码，再由真实环境验证服务端类型、迁移／并发／幂等、手机／平板横竖屏／分屏／大字／IME／进程恢复；真实短信／邮件、OAuth SDK、年龄／监护／经营资格、城市政策、系统推送、地图、正式协议地址、运营受理／调度／备份恢复按[运行手册](../server/V2_RELEASE_RUNBOOK_2026_10_23.md)落实。只在对应功能配置和验收满足后开放，保留现有服务端资格关卡。
