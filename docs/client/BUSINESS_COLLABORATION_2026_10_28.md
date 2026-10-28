# 邀请协作业务代码检查点

2026-10-28，任务AI-20261027-BUSINESS-CODE。用户随后要求后台先按角色完成详细设计，再接代码；本批已写修改保留在main工作树，没有提交或推送，不继续扩大邀请协作范围。

## 已写源码

- 服务端接受邀请及使用协作授权时复核原授权人、资源归属、拉黑与有效状态；照护邀请复用真实试养／交接照护阶段。已接受授权的接受截止不被误作授权终止日。
- 邀请新增本人退出介绍／照护协作；拒绝、撤销与退出只要求有效账号，不要求仍有互动资格。保留版本与原请求回放。
- 群申请只计算有效group_request；群主交接按页迁移全部待处理申请，过期申请迁移访问范围但不推待办通知；普通邀请不占申请额度。撤销pending入群邀请不移除独立入群者，撤销accepted成员同时清理静音与群主转让提案。
- 新增GET `/api/v2/adoptions/{id}/care-context`，只返回adoption_id、state、can_post、reason；不授予照护协作者完整申请、协议或身份材料读取权。
- KMP公共页面与Web接入邀请角色动作、退出协作、受限照护页面；修正Web将pet_id用作caretaker目标及提交近况后跳错adoption的问题。
- 导出与Web合同已派生：255路径／287操作／140服务端schemas、142Web schemas、88对DTO；45张mobile聚合表和13迁移不变。

主要入口：`internal/mobile/invitation_policy.go`、`care_context.go`、`social.go`、`group_directory.go`、`inbox.go`；`kmp/shared/.../client/InvitationStatus.kt`及`MobileFeatureStore.kt`；`web-client/src/core/{invitation,care}.ts`及`features/CareCollaboration.tsx`。

## 实际验证与待续

六项既有只读检查通过：mobile、server_shared_contract、kmp_client、client_ui、web_client、remaining_code。7份TS/TSX经Prettier语法读取与格式化；7份完整Kotlin语法树无错误，MobileFeatureStore大文件的既有语法器错误仍需按修改方法片段复核。9份Go经gofmt语法与格式读取。后续补写的回归源码与最终差异仍待收尾复核，不将早先检查结果升级为最终验收。

Go与KMP回归只写源码，未执行。按仓库规则未运行编译、构建、Gradle、test、lint、类型检查或应用；没有迁移、注册、部署或提交推送。

2026-10-27文档Figma同步回执保留当时快照；本次新增源码与文档尚未做该中心增量同步，不再声称当前全部文件与旧manifest完全一致。当前优先工作转为后台角色设计与后续代码。

## 最终静态接续

2026-10-28后台角色批次结束前已对本批保留源码一起收尾：九项只读脚本、22份合并范围TS/TSX语法、7份完整Kotlin与5个状态／修改片段、12份合并范围Go格式语法、BOM／冲突和diff核对。完整MobileFeatureStore的HEAD既有catch语法器限制未被算作整文件通过；回归源码仍未执行，构建／运行／平台验收仍未完成。源码指纹与结果统一见[后台角色批次](ADMIN_ROLE_WORKSPACES_2026_10_28.md)。
