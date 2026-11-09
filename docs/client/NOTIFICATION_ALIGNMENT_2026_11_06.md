# 通知事件与精确评论定位续作

日期：2026-11-06（Asia/Shanghai）。任务 `AI-20261106-NOTIFICATION-ALIGN`。本轮完成通知相关源码、派生合同与静态核对；未运行编译，遵循全局不自行编译规则。类型检查、测试、应用／设备、数据库与推送服务均未验证。

## 来源与同步事实

用户要求拉取代码并继续完善。当前唯一工作树为 main，HEAD `a300a01ac38e3183d9202e7910f62f850ea128f9`；fetch 成功，本地领先 origin/main 一个提交，随后 `merge --ff-only origin/main` 返回 Already up to date。未提交、推送、暂存或切换分支。本轮起点的 127 个未跟踪文件已记录原始 SHA-256，收尾复核结果见[静态证据](notification-alignment-2026-11-06/checks.json)。认证005–020及旧核对证据保留。

[11月5日核对](../design/figma-study/audits/2026-11-05-remote-check/REVIEW.md)发现远端10月30日通知合同未完整存在本地。本轮检索 Git 历史、3个不可达提交和717个不可达blob，未找到原事件／锚点源码；[恢复检查](notification-alignment-2026-11-06/git-recovery.json)记录实际范围。以下是按已确认合同重新编写的实现，不是找回或复原原批次。原4份历史来源文档仍缺失，不伪造其正文或原交付指纹。

设计依据为[评论定位四状态评审板](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=958-371820)：可见958:371822、已更新958:374615、不可用958:377408、读取失败958:380184。已读高保真评论、提示、返回控件上下文及截图；复用已有浅色token、评论组件、真实作者资料和原SVG。额外4稿独立记账，不把748状态改成752，也不把共享源码算成五布局运行验收。

## 已写源码

| 层次 | 行为与文件 |
|---|---|
| Go事件与公开视图 | [models.go](../../internal/mobile/models.go)、[service.go](../../internal/mobile/service.go)新增可选event_type、事务内读取的target_version及kind／id／version组成的reply_anchor；公开NotificationView不暴露推送状态、计划、送达回执和内部case列表 |
| 评论／回复与点赞 | [social.go](../../internal/mobile/social.go)、[comment_reactions.go](../../internal/mobile/comment_reactions.go)保留父事项为target，新评论／新回复为精确anchor；回复作者与父作者分别去重、自身不通知，沿用原请求幂等和点赞期望状态 |
| 业务事件 | 既有好友、邀请、消息、入群、领养、预约、认领、核验、订阅等生产点写入明确event_type；协议与群主转让结果补相关方通知，仍用当前角色、资格、版本与原请求门禁；附近提醒保留独立偏好规则 |
| 推送权限 | [notification_policy.go](../../internal/mobile/notification_policy.go)、[push_policy.go](../../internal/mobile/push_policy.go)发送前重新核对父事项和精确评论可见性；删除、错父项、屏蔽或失权关闭待发推送。站内通知归属与手动已读不要求目标仍可读取 |
| shared／Compose | [MobileStore.kt](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt)与[ConsumerApp.kt](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt)按精确评论GET核对编号、父项与published状态，提供可见／版本变化／不可用／网络失败分支和重读；分页去重，已知失权收起正文，删除当前锚点后重读 |
| Web | [notification.ts](../../web-client/src/core/notification.ts)严格解析正int64父项与锚点，非法锚点回到原事项；[ResourcePage.tsx](../../web-client/src/features/ResourcePage.tsx)进入时重新读取权限，使用原评论组件、分页和编辑器，已知失权停止展示正文 |
| 草稿与未知结果 | 锚点仅用于阅读定位，输入／贴纸／附件／请求编号仍归原父事项；切入、返回、发送成功和原回执恢复用同一草稿身份，未知结果继续保留原请求，未自动发送 |

查看评论与“标为已读”是独立动作；返回消息中心不会代为回复或处理业务。版本变化显示当前可见内容，通知的旧版本不授予写权限。历史通知缺失的事件、版本和锚点保持空值，不批量补写、改通知ID／版本／已读状态，也不重发历史通知。本轮没有数据库迁移。

## 合同与静态验证

[OpenAPI](../api/mobile-v2.openapi.json)通知列表／详情／已读响应增加NotificationView、NotificationReplyAnchor、NotificationRecord、NotificationPage四个schema；[Web派生](../../web-client/src/core/schemas.json)与[KMP DTO](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/Models.kt)同步。路径255、操作288不变；当前145服务端／147Web schemas、90对跨语言DTO。33份shared／53份公共UI、48功能分派、101份Web源码／461本地引用；原SVG22枚、消费者索引748条不变。

验证证据：

- [checks.json](notification-alignment-2026-11-06/checks.json)：合同导出一致性、DTO／UI／Web只读脚本、Go语法格式、编码／链接与旧未跟踪文件保留。
- [syntax.json](notification-alignment-2026-11-06/syntax.json)：8份修改文件，6份完整语法无诊断；MobileStore与ConsumerApp分别保留12／1个HEAD既有语法诊断，签名相同，不据此声称完整文件编译通过。
- [fragments.json](notification-alignment-2026-11-06/fragments.json)：12组相关Kotlin片段，10组无语法诊断；草稿工作区与标题2组分别保留2／1个HEAD相同诊断，无新增诊断。
- [notification_anchor_test.go](../../internal/mobile/notification_anchor_test.go)：3项回归源码覆盖重复回复／点赞、删除评论后关闭待发推送且仍可手动已读、历史通知不伪造元数据。两份既有推送夹具补真实可读目标。测试未执行。

语法读取逐次检查至少1024MiB内存余量，以独立Node进程、384MiB堆、单Wasm任务读取；不做类型分析、编译、测试或应用启动。[读取脚本](notification-alignment-2026-11-06/read-syntax.py)与[片段读取脚本](notification-alignment-2026-11-06/read-fragments.cjs)随证据保存。静态结果不能证明并发、数据库事务、真实设备、推送服务、响应式像素或无障碍已验收。

## Figma文档对账与后续

本轮只读[远端Spec证据](notification-alignment-2026-11-06/figma-read.json)，未修改远端。35:15仍有过期句“目标版本／回复锚点仍未提供”，35:3与35:17仍列shared29／公共UI41；10月30日旧源码交付与当前本地重新实现须分清来源。建议后续同步时将旧句改为“当前通知事件、目标版本与精确评论锚点已有源码及静态核对，去重／多设备与各端运行仍待验收”，并以本页更新当前入口；保留旧日期、评论与历史指纹。

后续按原全量任务继续认证删除／会话／返回及其余状态，748逐端验收仍未完成。构建／测试及设备运行须按当次明确授权和项目禁令处理；本记录不授予执行权限。无后台任务或自动续做。
