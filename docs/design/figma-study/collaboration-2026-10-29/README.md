# 协作退出与有限授权：既有 Figma 稿追踪补录

2026-10-29（Asia/Shanghai）。本批补录远端在2026-10-28已经存在的8个状态，不重复创建或修改设计稿。来源为[上轮MCP实读证据](../audits/2026-10-28-completeness/evidence.json)；完整节点、源码入口及验证边界见[state.json](state.json)。

[手机分组](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=910-357845) · [Web分组](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=915-386337) · [现行实现追踪](../../../client/figma-implementation-trace-2026-10-24.json)。

## 状态与稳定节点

| 状态键 | 内容 | 手机 | Web |
|---|---|---|---|
| caretakerLeaveConfirm | 本人退出照护协作的完整确认 | `910:357846` | `915:386338` |
| caretakerLeft | 已退出照护协作 | `910:357867` | `915:386431` |
| publisherLeaveConfirm | 本人退出介绍协作的完整确认 | `910:357887` | `915:386523` |
| publisherLeft | 已退出介绍协作 | `910:357908` | `915:386616` |
| caretakerWorkspace | 获授权的私密照护近况 | `910:357927` | `915:386707` |
| caretakerReadOnly | 获授权历史近况仅可查阅 | `910:357947` | `915:386799` |
| caretakerUnavailable | 照护权限已失效或不可访问 | `910:357967` | `915:386891` |
| publisherWorkspace | 获授权的宠物介绍与照片 | `910:357987` | `915:386983` |

目前只登记上述实读的手机／Web共16个根节点。本轮尝试读取平板、Desktop页顶层对应分组时MCP加载超时，已停止该脚本；其余布局未逐状态核对，不能推算ID，也不能将超时写成远端缺稿。上轮已看过的手机只读态、退出确认两张原始PNG继续保留在[审查目录](../audits/2026-10-28-completeness/REVIEW.md)，本批没有新增截图、播放原型或全布局视觉复验。

## 实现入口与权限语义

| 范围 | 公共 Compose／Web 源码 | 真实入口 |
|---|---|---|
| 邀请角色动作与终态 | `SocialPages.kt`／`ResourcePage.tsx`，列表为`Records.tsx` | `/invitations`、`/invitations/{invitationId}` |
| 本人退出／所有人撤销确认 | `SocialPages.kt`的现有确认层／`WorkflowEditor.tsx` | `/edit?flow=invitation-action&kind=invitations&id={invitationId}&action=leave`，撤销为`action=revoke` |
| 获授权照护、只读与不可访问 | `CarePages.kt`／`CareCollaboration.tsx` | `/care-collaboration/{adoptionId}`；先读取`adoptions/{id}/care-context` |
| 介绍与照片维护 | `SocialPages.kt`／`WorkflowEditor.tsx` | `/edit?flow=introduction&kind=pets&id={petId}` |

介绍协作目标是`pet_id`，私密照护目标是`adoption_id`，接受／拒绝／撤销／本人退出操作目标是`invitation_id`。接受邀请截止仅约束pending状态，不是已接受授权的终止日。已接受的关系仍须由服务端逐次核对资源归属、原授权人、拉黑及有效阶段。

本人退出与所有人撤权分别记录；拒绝、撤销和退出只要求有效账号，不因互动资格缺失而阻止收回授权。照护`can_post=false`只读不等于失去已授予的历史读取权；401／403、撤权或记录不可访问不能沿用旧私密材料。授权照护页面不读取完整申请、协议、身份材料或公开宠物编辑权限。源码依据包括[邀请角色动作](../../../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/InvitationStatus.kt)、[Web邀请动作](../../../../web-client/src/core/invitation.ts)、[照护上下文](../../../../internal/mobile/care_context.go)和[业务检查点](../../../client/BUSINESS_COLLABORATION_2026_10_28.md)。

## 追踪变更与实际检查

- 保留原740个状态及其所有设计根节点／来源，新增8条协作状态，当前追踪为748条；这是设计状态索引数量，不是已完成或已验收数量。
- 纠正17条旧邀请／协作状态的源码与路由归属，原生从笼统的`CarePages.kt`改到实际`SocialPages.kt`，Web分别指向邀请列表、详情、邀请／介绍编辑或版本化动作确认。没有借此宣称对应固定示例已逐像素实现。
- 生成器写入后默认一致性校验通过；公共UI与页面缺口两项只读静态检查通过，3份Python源码AST、旧740条根节点／来源保留、新状态键唯一性、运行标记及`git diff --check`通过。两个静态脚本仍严格断言740基线＋8增量与全部`runtime_verified=false`。

未运行编译、构建、Gradle、Go、test、lint、类型检查或应用；未执行接口联调、原型播放、设备／浏览器、迁移、注册、部署、提交或推送。新增状态的源码归属、既有设计截图和本地静态通过均不能升级为各端运行证据。Figma文档中心的后续同步须另记实际写入与回读，旧manifest不因本批本地补录而变成最新全文快照。
