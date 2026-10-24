# 服务端优先完善与作业交接

> **2026-10-24 后续更新**：在本文批次之后，服务端已新增公开阅读与法规制度库（`internal/platform/mobileapi/reading.go`）、浏览器 `HttpOnly Cookie + X-MND-CSRF` 会话适配（`browser.go`）、宠物评论与表情反应（`009_pet_discussions.sql` / `comment_reactions.go`）、内容/救助订阅通知与第 6 个后台作业 `content_updates`（`content_subscriptions.go`）、群目录检索与群主转让（`group_directory.go`）、宠物唯一放置与候选队列（`adoption_placement.go`）、跨单据证据保留（`evidence_retention.go`）。当前 OpenAPI 为 `230` Paths / `259` Operations / `112` Schemas，共 `44` 张 `mobile_*` 聚合表。最新服务端架构短板与待完善清单见[服务端完整性审计（2026-10-24）](SERVER_AUDIT_AND_REMAINING_GAPS_2026_10_24.md)。

2026-10-23，AI-20261023-SERVER-FIRST。用户要求服务器未完善时优先处理；本轮只扩服务端，保留前批公共KMP和生命周期未提交成果。基线main／`5de631b669d312ddd461d015ed137dad16c6ed02`，本批未提交／推送。源码与静态检查完成，不能据此宣称服务器已可上线。

## 本批修正

| 范围 | 当前源码行为 | 入口 |
|---|---|---|
| HTTP认证与写事务之间撤销 | authenticated mutation在实际事务内重新核对同一会话、epoch和账号；OAuth／验证码外部调用前后均核对。内部无会话调用仍是受信服务接口，HTTP不走该旁路 | `internal/mobile/mutation.go`、`internal/identity/mutation.go` |
| 旧写操作回执 | 设置、订阅、点赞、线索、反馈、已读、邀请／会话动作及删除支持可选Idempotency-Key；原DTO保留。已有StateInput的client_request_id也接入，头体不同拒绝 | `commands.go`、`social.go`、`rescue.go`、`inbox.go`、HTTP handler |
| 未知结果恢复 | 只向原操作者返回最小committed元数据；结果已删除或访问已收回不抹去提交事实。资源内容重放仍核对当前权限，群已读回执单独核对当前成员身份；删除重放返回空内容墓碑 | `commands.go` |
| 自然去重与预约反馈 | 相同账号／案件／线索内容及附件不重复通知；同预约有效版本／参与者只留一份到访反馈，不同内容冲突；缺失、已取消或未来的有效条款不能提交新反馈 | `rescue.go`、`appointments.go` |
| 推送设备隐私 | 设备绑定当前会话散列；刷新事务迁移绑定。单会话退出禁用对应设备及旧无绑定记录；全部退出／换绑／恢复／注销禁用全部设备并清token。禁用token后重新注册创建新ID，旧清理清单保留原属主 | `push.go`、`push_policy.go`、identity安全变更 |
| 推送重试 | accepted／delivered／suppressed重复派发不再报错；Lookup在Send前，稳定供应商去重键；发送前重核设备、账号、设置。附近提醒另核当前订阅、距离、案件公开／活动状态和拉黑 | `push.go`、`push_policy.go` |
| 供应商撤销 | 内存及PostgreSQL支持参数化KeyPrefix；撤销仅扫描revoke:命名空间，不被前100条普通事件遮蔽；遵守租约／重试时间，接受后清凭据并独立ACK | `identity/revocation.go`、两仓库mobile.go |
| 消费与清理 | 每消费者租约和CAS；附近提醒分批游标持久化，最后一批才ACK；已删除的通知／案件按失效结束。隐私进度事件不再递归触发自己；媒体实际物理清理后再核对资料进度 | `delivery.go`、`mobileapi/jobs.go` |

旧调用不传Idempotency-Key时继续版本／目标状态语义，不代表全部旧调用已有强制回执。需要响应丢失恢复的调用方应持久保存原键和原载荷，并查询`GET /operations/{key}`；404不能授权换新键再次提交。新建业务仍使用现有必填client_request_id。回执不携带正文或恢复已失去的访问权。

通知已读写响应也会去除供应商接受回执，数据库原记录仍保留。供应商已接受的外部请求不能撤回，本批在Lookup／Send前重核权限，未声称跨HTTP提供原子撤销。

## 实际作业接口

固定路径均为`POST /api/v2/ops/jobs/<job>`，请求`{"positions":{}}`。返回JobResult的state、processed、delivery_id、retry_after_seconds和positions。idle表示本批未领到工作，retry表示仍待继续，acknowledged仅本消费者完成，batch_complete仅扫描批结束；均不能解释为全系统或所有用户完成。

| job | 命名凭据要求 | 一次调用与续接 |
|---|---|---|
| push | worker角色＋push consumer | 领1个事件，当前权限重核、供应商查询／发送／接受记录后ACK；摘要未到时间不抢占 |
| nearby_alerts | worker＋nearby_alerts | 领1事件／至多100订阅；分批游标存在Delivery，部分完成保持pending，30秒后续接 |
| privacy_cleanup | worker与privacy＋privacy_cleanup | 领1事件；每次Process最多改100个目标；每tick至多清20物理文件，有实际清理后再Process一次。保全／外部待办／协助保持retry，只有完成证据满足才ACK |
| provider_revocations | worker＋provider_revocations | 扫至多100撤销事件，positions.revocations.cursor回传；每项独立租约／确认 |
| maintenance | privacy，无consumer要求 | safety／otp／tokens／evidence／uploads各一批，分别回传cursor；uploads另有after文件游标 |

`GET /ops/data-requests/{id}`仅privacy访问，审计读取，返回分项ID／状态／进度版本；不返回查询能力、请求备注、材料、联系方式、保全依据或供应商凭据。它不是普通账号的查询接口，注销后仍使用原X-Request-Access能力。

命名运营身份由MND_OPERATORS_JSON配置，各作业单独凭据、最小roles／consumers，不复用客户端会话或共享AdminKey。直接nearby-alerts入口也检查consumer。HTTP层先做角色检查，作业层再做consumer和隐私组合角色检查。

## 独立执行程序

`cmd/worker`和`internal/platform/mobileworker`已有源码，不在API服务中自动启动。本轮未启动它。运行环境模板见[environment.example](environment.example)。

部署步骤（尚未执行）：先在明确获准的环境完成构建／迁移／回归，配置服务真实渠道与命名运营身份；每个job单独设置MND_WORKER_JOB、MND_WORKER_API_URL、MND_WORKER_OPERATOR_TOKEN和持久MND_WORKER_STATE_FILE。程序默认`-once=true`只跑一批；外部调度器每次调用，或显式`-once=false`使用间隔循环。默认30秒，配置允许5–3600秒。

目标必须HTTPS及/api/v2，禁止URL内凭据／查询／重定向。显式MND_WORKER_ALLOW_LOOPBACK_HTTP=1只允许本机loopback开发地址。密钥只从环境入请求头，不入游标文件或日志。默认游标目录data/mobile-worker已忽略Git；应部署在持久私有目录，一个job独占一个文件。

游标绑定API及job的SHA256与schema1，写临时文件、同步后重命名；损坏或目标不符停止并要求人工核对，不静默重置。断网、非2xx、错误包装、过大／额外JSON和未知状态均保留旧游标，不回显供应商错误正文。API事务提交但本地游标写失败时，可重复请求同批；实际副作用仍由租约、CAS和供应商去重保护。文件不提供多进程锁，不能让多个进程共享同一游标文件；Windows实际替换／崩溃恢复仍待运行核对。

## 检查证据与剩余边界

[本批证据](server-first-evidence-2026-10-23.json)记录源码指纹、四份只读脚本及Go格式／语法结果：209路径、236操作、100schemas、42表、48对共享DTO、45份Kotlin／Swift和Android30扩展页面静态通过。本批4份回归文件21个用例仅写源码，覆盖撤销竞态、回执／权限、去重、分批清理、推送、撤销队列、作业角色／物理清理、worker地址／游标／错误隐私；未执行。

未运行编译，遵循全局不自行编译规则。没有Go类型检查、应用测试、数据库／文件实际作业、迁移、真实渠道、服务／设备、提交／推送或部署结果。未新增依赖或迁移，已有008仍未执行；JSON扩展和查询条件不要求新表。

仍需完成的实际交付：

1. 在明确获准环境执行类型／编译和回归、008迁移与冲突核对、PostgreSQL事务／故障恢复和平台联调；修复真实结果后留证。
2. 配置短信／邮件／OAuth／推送／撤销供应商、有效年龄监护／城市规则、真实运营角色／责任人、任务调度、期限复核和外部处理者／受限备份回执；未知仍关闭，不能填模拟完成。
3. Outbox领取只保留100条一页的内存，但可能遍历历史；保全关联检查、资料总清单及注销枚举也可能扫描较多记录，本批只限制目标变更／文件批数，不声称总查询或事务耗时有界。需用实际数据核对索引、规模和租约时长，再决定归档／投影优化。
4. search_projection／audit_projection专用外部投影作业不在本批；本地审计记录已有写入，不伪称外部投影上线。事项协助、保全复核及脱敏转送等人工工作仍须实际处理，作业不能代替履责。历史擦除仍用独立legacy-erasure适配入口，本程序不会自动替运营决定历史范围。
5. 当前客户端暂未全部传新可选回执头；后续在公共Transport中接入原键／载荷持久化，分别验收Android／iOS。Web的cookie／CSRF与无损ID、完整iOS页面及系统渠道仍按原任务另接，不在服务器优先批次顺带改写。

本批承接[前批生命周期](LIFECYCLE_RETENTION_2026_10_23.md)和[服务器／共享合同](SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)。旧证据JSON保留生成时快照，不以本批源码重写其历史指纹；B01–B12不能整体勾成已运行完成。
