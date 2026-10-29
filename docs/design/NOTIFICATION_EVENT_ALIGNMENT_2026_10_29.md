# 消息事件分类、未读与待办对齐核对

日期：2026-10-29（Asia/Shanghai）。适用当前V2消费者消息中心。核对基线：main / b269504；本文件是源码事实、设计目标和最小修订建议的对照，不能作为已经修改业务代码或执行迁移的证据。

设计依据：[全局未读专项合同](MND_GLOBAL_UNREAD_SPEC.md)、[当前通知与待办稿356:246](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=356-246)。现有稿把好友与邀请放在“通知”；本轮已将UX-16与专项设计统一为关系通知，服务端好友事件仍使用interactions。下文将尚存的源码差距具体到事件、分类、偏好、计数和目标，避免只改Tab文案。

## 1. 现有数据与接口事实

通知模型位于[models.go](../../internal/mobile/models.go)的Notification，包含category、text、target_kind、target_id、read、push_state，以及服务端送达相关字段；外层Record另有通知编号、owner_id和version。当前没有独立event_type、subcategory、target_version或reply_anchor字段。

- category当前是字符串，没有统一的NotificationCategory枚举；notify内部也没有拒绝未知分类的校验。不能把任意字符串都理解为有完整设置、送达和UI支持的已批准分类。
- [notify](../../internal/mobile/service.go)与[pushAllowed](../../internal/mobile/push_policy.go)使用direct_messages、groups、applications、interactions映射提醒偏好；nearby另由订阅规则控制。未知分类仍可能有站内记录，但常规推送偏好映射不会自动开启。
- [KMP NotificationBody与InboxSummary](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/Models.kt)保留category和按分类汇总；[Web合同](../../web-client/src/core/api.ts)把notifications视为字符串键计数字典。
- [OpenAPI](../api/mobile-v2.openapi.json)已有GET notifications、GET notifications/{id}、POST notifications/{id}/read与inbox-summary，但通知读取响应仍走通用Envelope；components没有独立Notification schema。设计文档中列出的字段并不等于所有字段都已有专用schema约束。
- 通知外层version是通知本身的版本，绝不是好友、邀请、群或预约的当前业务版本。打开事项后必须重新读取目标；接收通知不授予业务权限，通知载荷不能直接触发accept、revoke、leave或确认预约。

## 2. 底层分类与三个Tab

| 底层category | 当前源码用途 | UI归属 | 提醒偏好／计数约束 |
|---|---|---|---|
| direct_messages | 私信消息的站内／推送副本 | 聊天；不作为独立通知卡重复展示 | 由会话未读位点计数，副本不再加入global_total |
| groups | 非静音群消息的站内／推送副本 | 聊天；不作为独立通知卡重复展示 | 由群消息位点计数；免打扰政策另见第6节 |
| interactions | 评论、点赞、好友request／accept，以及关注求助的进展更新 | 互动 | 当前使用Preference.interactions；其中好友与求助更新和专项设计有差异 |
| applications | 邀请、入群申请、领养／预约／认领／核验／照护等进度 | 通知 | 当前使用Preference.applications；可容纳关系与邀请，不需立即新增底层分类 |
| nearby | 区域寻宠提醒 | 通知 | 独立订阅、城市与可读性复核；不是营销推荐默认计数 |
| 其他／未知值 | 无统一已批准枚举；旧资料中出现system等示例 | 两端默认落在通知 | UI兜底不证明该类别已经有完整事件生产与推送设置 |

[KMP InboxScreen](../../kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/InboxPages.kt)与[Web InboxPage](../../web-client/src/App.tsx)都按interactions归互动，其他非聊天分类归通知；不是按target_kind或卡片标题重新分类。因此只在某一端将friendships卡片移到通知，会造成列表和服务端Tab数字不一致。

“关系与邀请／业务进度／审核与系统”目前是设计中的展示分组，不是三个已经存在的底层category枚举。最小修订可在applications内按原事项类型展示关系邀请，不需要新增没有偏好字段支持的relations分类。

## 3. 事件对照表

“建议归属”为对齐现有专项设计的建议，不表示本文件已经改变源码；主交接应按实际实施结果更新本表。所有待办动作都须读取当前对象、角色、权限和版本，不由通知状态推断。

| 事件／来源 | 当前通知与接收人 | 建议归属／目标 | 已读与待办／范围边界 |
|---|---|---|---|
| friend.request | social.go的Friend：给目标用户，interactions，Friendships/关系ID，文本“好友关系有更新” | 通知→关系与邀请；底层applications，仍指向Friendships | 通知未读与待接受分离；当前Pending尚未统计Friendships |
| friend.accept | 给原申请方，interactions，Friendships/关系ID，同上通用文本 | 通知→关系与邀请；底层applications | 接受结果是进展，不新增待接受任务；建议文本区分“收到申请”和“申请已被接受” |
| friend.decline／withdraw／remove／block／unblock | 当前只保存关系状态，没有此处个人通知 | 本轮不为这些动作补统一通知 | 不通知拉黑、拒绝的具体内部原因；是否需要温和结果通知另行定义 |
| invitation.create：group_member | 给被邀者，applications，Invitations/邀请ID | 通知→关系与邀请；保留applications | 仅pending、当前接收方且接受截止未过才是待办；阅读不入群 |
| invitation.create：adoption_editor／caretaker | 给被邀者，applications，Invitations/邀请ID | 通知→关系与邀请；保留applications | 介绍编辑与私密照护范围分开；接受截止不等于已接受授权的终止日 |
| group.join_requested | 给当前群主，applications，Invitations/申请ID | 通知→关系与邀请；保留applications | 当前有效group_request才占待审额度；需群主当前权限 |
| group.request_transferred | 群主变更时向新群主通知有效待审申请，applications，Invitations/申请ID | 通知→关系与邀请；保留applications | 是同一申请的接收责任变化；旧群主不再有读取／批准权，不能复制为新的业务申请 |
| group_request接受 | 给申请人，applications，Groups/群ID，“入群申请已被接受” | 通知→关系与邀请；保留applications | 结果通知，目标是现有群；不继续保留同申请的待接受状态 |
| 其他invitation.accept／decline／revoke／leave | 当前保存领域事件；除上述group_request接受分支外，没有统一反向个人通知 | 本轮不声称全部终态都有双方通知 | 失权时立即清缓存／禁写，由当前授权判断；不能等待“撤权通知”才关闭 |
| group.transfer／accept_transfer／decline_transfer／revoke_transfer | 当前Group保存领域事件；没有专门个人通知 | 如纳入完整关系消息体验，需单独定义接收人和目标；建议applications | 当前Pending也未统计待接受群主转让；不能把它计成已有覆盖 |
| group.notice／leave／remove_member／dissolve | 当前保存群记录及领域事件；没有统一成员变更个人通知 | 本轮不新增每成员广播 | 不因文档列“成员变更”便假造已送达；静音、退群与失权单独复核 |
| 评论／回复、动态点赞、评论点赞 | 给被互动者，interactions；目标多为父Entries／Pets | 互动；保留interactions | 现有通知没有具体回复锚点，不能声称已精确定位到每条回复；本人动作不自发通知 |
| CaseWatchAlerts关注求助进展 | content_subscriptions.go：interactions，Cases/求助ID，文本“关注的寻宠求助有了新的进展” | 专项合同将寻宠进度归通知；建议先明确订阅进展分组，再决定applications | 当前key为case-watch:caseID:caseVersion:recipient，每版本每人至多一条；不能按所有Cases目标一概重分类 |
| 区域寻宠提醒 | nearby.go：nearby，Cases/求助ID，可带多个CaseIDs | 通知→业务进度／附近提醒；保留nearby及独立订阅 | 保持位置粗精度、订阅规则与当前可见性；不能复制成第二条个人通知 |
| 领养／接回／预约／认领／照护／审核更新 | applications，按对应原事项目标给相关方 | 通知→业务进度或审核；保留applications | 只通知已发生的状态；通知读取不接受合同、交接、接回或预约 |
| 私信／群消息 | direct_messages／groups，Conversations／Groups目标 | 聊天 | 消息原始记录和提醒副本不是两次未读；消息请求的未读数与等待接受分开 |

源码来源：[social.go](../../internal/mobile/social.go)、[group_directory.go](../../internal/mobile/group_directory.go)、[content_subscriptions.go](../../internal/mobile/content_subscriptions.go)、[inbox.go](../../internal/mobile/inbox.go)。

## 4. 事件去重与原请求

现有幂等具有不同层次，不能笼统声称“同一个target_id只通知一次”。

1. 好友、邀请创建、群申请等command在原子事务内写业务结果和通知，并以actor＋client_request_id检查命令名、对象和载荷摘要；同一原请求回放不再次执行notify。失败或结果未知继续原请求，不换key制造第二次命令。
2. invitation动作等mutate支持现有Idempotency-Key／client_request_id传入的可选持久回执。只有实际传入键才有该层保证；不把普通版本校验泛称为完整持久回放。
3. notify本身创建通知记录时没有通用独立事件去重键。两个不同且合法的关系命令可以有同一个target_id，应分别保留其时序；不能仅按目标删除后一次事件。
4. 关注求助进展有独立case-watch复合键，可逐版本去重；普通notify没有该键，不能借用其规则推导其他事件都已去重。
5. 推送是已存在通知的外部效果。供应商重试、推送展示和站内列表不能重新创建个人通知或再次累加全局数；accepted／delivered等供应商状态不等于本人已读。
6. 业务状态与待办从当前目标记录重新判断。过期／撤回／已接受邀请即使仍有未读历史通知，也不再是有效接受任务。

## 5. 已读与跳转的当前差异

服务端ReadNotification将一条本人通知改为read；PrepareInboxReadAll保存10分钟快照，Confirm只核销快照内通知ID和会话位点。后续新消息保留，快照成功没有“撤销已读”接口。

两端目前的操作不同，应在统一交互说明中显式选择：

- KMP通知行提供“标为已读”和“查看原事项”两个动作；MobileStore.openNotification注释与实现明确打开事项本身不标已读、不提交。系统通知回跳先回读当前用户的通知再打开目标，也不在载荷上执行业务命令。
- Web消息中心点击通知行时，先POST read，再跳目标；从系统通知链接进入NotificationPage的“查看原事项”只导航，不写已读。
- 因而“查看原事项”这个相近语义在不同入口产生不同已读效果。建议统一为“实际阅读通知正文后更新该通知已读，打开目标独立于业务同意”；或保留显式标记模式并更正Web入口。该选择需要落实到交互稿和源码，不能由本文件暗中选定。
- 目标失效、缺少权限或已删除时，保留可核销的本人通知说明；不以读取原私密材料为核销前置条件，不继续显示接受／撤销旧动作。
- 当前KMP的Friendships、Invitations目标落到对应目录，Web可落指定资源详情；“回到具体申请”不能笼统算两端同等精确。应补目标ID选中／定位或明确目录回退文案。

## 6. 已知计数边界，不在本轮扩展政策

[inbox.go](../../internal/mobile/inbox.go)按通知记录聚合category；direct_messages和groups副本不计第二次global_total。Pending单独统计Adoptions、Appointments、Invitations、Claims、Reviews、Guardians，另有message_requests；当前不含Friendships及群主转让。

免打扰群当前仍计入global_total；源码明确将排除总数保留为未确认设计建议。[专项合同](MND_GLOBAL_UNREAD_SPEC.md)也标明该政策为建议。不能借分类修订静默切换为“免打扰完全不计数”；两者应分别决定和验收。

## 7. 最小修订方案与旧通知

建议按顺序落实，设计／文档／源码／运行分别记账：

1. 先以现有Figma专项稿统一好友request、accept归“通知→关系与邀请”，底层复用applications；UX-16移除“好友申请属于互动”的旧句。同步提醒设置说明：好友关系更新将遵循申请／业务通知开关，不再遵循互动开关。
2. 在后台确认群、邀请与好友的待办责任后，补Friendships pending接收人统计和入口定位；明确群主转让是否纳入本批，不把未实现项混入已完成列表。
3. 单独解决关注求助进展分类，不按Cases目标通配，因为同一目标类型也有附近提醒、私密线索或其他业务事件。
4. 对关系终态与群变更没有通知的分支，先定义必要通知、接收人、隐私文本、当前失权及去重键，再补源码；不默认向所有成员广播。
5. 列表、汇总、偏好、推送和深链接必须使用同一有效分类口径，不能仅移动客户端卡片。

**是否必须执行数据库迁移：**

- 如果目标只要求之后新事件正确分类，可以仅修新notify并明确历史通知保持旧类；这不等于新旧记录已统一。
- 若目标要求历史好友通知、Tab数字与设置全面一致，旧category也必须在读取／汇总／送达中统一，或执行受控数据迁移。不能仅更新一个客户端过滤条件。
- 可安全限定的历史集合是target_kind=friendships且旧category=interactions；两个好友事件共享泛化文本，不能可靠从文本区分request和accept。但二者都归关系通知时，不需要据此拆出事件类型。
- 不能全量把interactions改成applications；评论／点赞仍应留互动。关注求助进展的迁移应使用case-watch记录键或可靠事件来源，不能仅按target_kind=cases判断。
- 迁移或兼容层必须保留通知ID、owner、read／state、原事件关系和已投递事实；禁止重新创建通知、重置已读或为已accepted／delivered推送排队。待发送记录的偏好判断则应按已明确的新口径复核。
- 全部已读快照保存通知ID和是否计入总数；好友分类前后都属于非聊天事件，不应因此重复准备快照或增加全局事件数。若改变通知版本导致单条旧read请求409，客户端应重新读取通知后继续；绝不把通知版本传作原业务版本。
- 本文件没有运行SQL、维护作业或消息发送，也不批准线上迁移。最小方案可选服务端统一有效分类的兼容读取，或后续带快照／回滚检查的受控JSONB更新；实际采用哪种必须在实现交接中记载。

## 8. 后续静态与运行验收条目

| 场景 | 需要证明的行为 |
|---|---|
| 收到好友申请并查看通知 | 只影响该通知已读，申请仍等待本人决定；Tab数字与列表同类 |
| 接受好友申请，原请求结果丢失后恢复 | 同一命令只产生一次结果通知，不重发关系命令；申请方不再有该待办 |
| 历史好友通知分类兼容 | ID与已读不变，全局数不增加；互动／通知Tab与推送设置一致 |
| 邀请过期／撤回／退出 | 未读历史说明可查看或核销，不可继续使用旧授权按钮 |
| 群主变更且有多页申请 | 新群主只收到当前有效申请待办；旧群主不能继续读／批准，过期不另发待办 |
| 关注求助更新重复消费 | 同一求助版本每用户至多一条；无权读取者不收到新的私密内容 |
| 10分钟全部已读快照 | 新消息不被吞掉，失权内容跳过，读过仍待处理的任务保留 |
| 切账号／未知目标／失权 | 不显示前账号内容，不执行通知载荷动作，不保留永久无法核销角标 |

本轮仅做源码／文档读取与本文件静态检查；未编译、构建、Gradle、Go build/run/test、test、lint、类型检查、应用启动或真实消息／迁移。表内运行验收均尚未执行。
