# M&D 当前功能交付账本

2026-10-23，AI-20261023-ANDROID-FINISH；main / 5de631b加当前未提交工作树。依据[闭环复审](../bmad/PRODUCT_CLOSURE_AUDIT_2026_10_23.md)及[现行合同](../design/MND_PRODUCT_CLOSURE_CONTRACT_2026_10_23.md)。这里汇总当前事实，不修改历史证据。最新结果见[Android／服务端续作](ANDROID_REMAINING_2026_10_23.md)，前批领养记录保留原验证层次。

## 1. 状态口径及平台范围

Android新增基础框架／组件及方向状态实现见[本批交接](PAD_STATE_AND_UI_2026_10_23.md)、[组件目录](UI_COMPONENTS.md)：10个公共primitive、统一尺寸／主题、主从列表与表单可移动内容，以及ViewModel流程内UI状态。58份源码／30分派静态通过，仍无旋转／焦点／IME的运行证据；下表其他功能状态不因封装组件提升为完整交付。

“源码”指已有对应实现，不能解释成完整功能；“静态”只以原批次证据为准，不替代类型检查。**当前所有功能的构建／联调／设备／真实运营验收均未通过本轮验证。** Android手机和平板共用同一Store及合同，各自布局；iPhone／iPad后置，旧SwiftUI不能抵扣。Web／Desktop保留旧实现，新v2未全接；最小安全网页先于完整Web。

每次更新记录任务ID、设计状态、API／版本／错误、源码入口、迁移／配置、手机／平板差异、异常用例、证据、阻塞及认领。责任角色是建议，不是人员已认领；日期等待真实认领后填写。

## 2. F00–F14 当前记录

| 任务／设计入口 | API与共享／Android源码入口 | 当前层次 | 下一交付与验收；责任角色 |
|---|---|---|---|
| F00 入口／失权／任务恢复 | `MobileStore.kt`、`MobileViewModel.kt`、MainActivity、`public_web.go` | 白名单深链待查看、账号归属／路由恢复及HTTPS分享源码／静态 | 正式域名关联、冷启动／进程恢复、取消登录／失效资源运行验收；平台＋Android |
| F01 三标识／手机／监护 | auth、eligibility、guardians；`MobileApi.kt`、SensitiveStore、`AndroidAuthDialog.kt`、`AndroidAccountFeatures.kt` | 服务端／shared／Android源码，真实渠道待配置 | OAuth SDK、真实资格／监护关系、旧号恢复、撤权／边界年龄；身份＋Android＋运营 |
| F02 社区／详情／互动 | entries/comments/reaction；`MobileStore.kt`、`AndroidMobileApp.kt` | 源码及原读取状态静态；收藏／关注合同未完整 | 发布／受众失权、分页／弱网、未知结果、重复评论；服务端＋shared＋Android |
| F03 好友／圈子／受众 | friendships、audience；`FeatureApi.kt`、`AndroidSocialFeatures.kt`、`AndroidPeopleFields.kt` | 好友分页选择／昵称筛选／受众多选／预览及失权重核源码／静态；首字头像占位 | 真实公开头像合同及关系变化／受众不扩大运行验收；产品＋Android＋服务端 |
| F04 多宠物／两类协作 | pets/introduction/invitations；`WorkspaceApi.kt`、`AndroidSocialFeatures.kt` | 宠物／好友邀请选择、预览与重核源码／静态 | 权限范围、转移／退出、接受邀请／撤回、媒体回收运行；shared＋Android＋服务端 |
| F05 私信／消息请求 | conversations/messages/receipts；`MobileStore.kt`、`AndroidMobileApp.kt` | 源码；独立私信分页原静态 | 首条未读真实定位、拒绝／失权、重试去重、私密缓存；shared＋Android |
| F06 群／交接／邀请 | groups/group_messages/invitations；`AndroidSocialFeatures.kt` | 好友邀请选择／预览源码；原群内成员管理仍有编号摘要与查看资料入口 | 群成员名称体验、交接超时／拒绝、移除后媒体权限、解散重复请求；服务端＋Android |
| F07 候选／独占／接回 | adoptions/contracts/returns/care；`rescue.go`、`contracts.go`、Models／FeatureModels、`AndroidCareFeatures.kt` | pet级独占／候补／周期与接回源码；本批补报价预约旁路、关闭招募后交接、重提协议与接回替代边界；静态 | 并发事务／旧异常数据、失败回滚／幂等、双方资格与完整流程运行；B10/B12服务端＋Android |
| F08 有效版本／提案／暂停 | appointments/actions/feedback；`appointments.go`、`AndroidCareFeatures.kt` | 源码；旧拒绝／取消文案已在本轮规范纠正 | 并发409、风险解除、时区／跨天、未到访争议；服务端＋shared＋Android |
| F09 走失／拾获／目击／认领 | cases/claims/leads/subscriptions；`rescue.go`、`AndroidCareFeatures.kt`、`AndroidRegionFields.kt` | 文字城市／区域发布及城市订阅源码／静态；无坐标不伪造地图点，旧粗点兼容 | 地图渠道、多人争议、无坐标／旧点位编辑运行验收；Android＋供应商＋运营 |
| F10 图文／来源／草稿 | media/drafts/entries/operations；`MobileApi.kt`、`MobileStore.kt`、AndroidMedia | 源码及恢复／读取原静态 | 真实相册／HEIC／部分失败／进程终止、原件用途、清理；shared＋Android＋服务端 |
| F11 表情／贴纸／最近 | expression-catalog/assets/expressions；RichContent、Android输入控件 | 素材和源码；比例／目录原静态 | 中文组合、选区、整字素删除、键盘切换、各输入位置；shared＋Android |
| F12 未读／附近／通知 | notifications/inbox-summary/read-all/preferences/subscriptions；`inbox_snapshot.go`、`AndroidInboxReadAll.kt`、`AndroidAccountFeatures.kt` | 全局快照已读／原请求恢复、城市提醒及站内源码／静态；系统通道未接 | 首未读／新到消息／免打扰／跨设备／进程恢复运行与真实推送；B03＋shared＋Android |
| F13 核实／游客安全／正式通知 | reviews/safety/rights/data；`trust.go`、`rights.go`、PolicyModels、AndroidAccount、`web/safety.*` | Android与最小游客网页源码／静态；原键核对、邮箱验证、受限回执及补充；实际处理未验收 | 正式邮件、材料到期、转送／申诉／升级、凭据恢复／丢失与真实受理；B02/B09/B11＋Web |
| F14 出售／费用／到访 | offers/appointments/feedback、authority；`AndroidOfferFeatures.kt` | 源码；真实准入及合同配置待交付 | 城市／物种、代理／费用变化、换猫／换地、到访不等于成交；B07/B10＋运营 |

精确方法和字段查[OpenAPI](../api/mobile-v2.openapi.json)，功能验收仍以[功能矩阵](FEATURE_DELIVERY_MATRIX.md)及[设计与适配](DESIGN_AND_ACCEPTANCE.md)为准。表中源文件名须在对应Go／client／Android mobile目录定位；没有声明所有API已经运行。

## 3. 九项复审的设计／文档出口

| 复审项 | 设计／文档已落实 | 代码／现实交付状态 |
|---|---|---|
| 单宠互斥与其他申请 | 新合同§2；SC-AD及F07引用 | 本轮优先实施，结果查本轮记录 |
| 再次申请／接回退出 | 新合同§2–3；LR04含截止、本人撤回和原安排 | 本轮优先实施；运行未验证 |
| 旧规范冲突 | PRD NFR007、UX07与末尾、寻宠导航已统一 | 预约源码已有拒绝／取消分离；保持一致 |
| 公开分享／游客网页 | 新合同§5与runbook：必须先交最小网页 | HTML摘要／游客页及App续接源码／静态已补；正式域名关联、OG兼容与环境验收待交付 |
| 编号／坐标录入 | 新合同§4：可访问人员选择、受众多选、区域降级 | 好友／受众／邀请及文字区域已补源码；预约保留事项绑定，群成员名称与公开头像仍待 |
| 真实资格／渠道 | 新合同§6及runbook证据表 | 供应商／人工方式、正式政策及人员待落实 |
| 当前交付账本 | 本表、架构基线／API章节和实施优先级更新 | 随实际结果逐项更新，历史JSON保持快照 |
| 运营／部署／恢复 | [v2运行与放行手册](../server/V2_RELEASE_RUNBOOK_2026_10_23.md) | 真实迁移／调度／清理／演练待获准执行 |
| Figma及全端验收 | 原稿复用与核对另留证，不以画板数计完工 | 原两项额外远端证据、原型／各端设备尚待核验 |

## 4. 证据与开放条件

本批静态与指纹见[续作证据](android-remaining-evidence-2026-10-23.json)：214路径／104schemas、55对DTO、55份客户端源码与30扩展分派，以及34网页控件集成、JS语法与Go格式读取。历史检查仍查原批次：[Android页面](android-pages-evidence-2026-10-23.json)、[读取状态](android-read-states-evidence-2026-10-23.json)、[服务器](../server/server-first-evidence-2026-10-23.json)。新代码不能沿用旧哈希或运行结果。任一功能对外开放前，收齐对应版本、迁移、真实配置、权限／故障／恢复用例、目标端设备和运营受理证据。

未运行编译，遵循全局不自行编译规则。
