# 私密线索处理与独立会话合同

2026-10-26，`AI-20261026-CARE-PROGRESS`。源码／静态交付，未编译、执行回归或联调。沿用现有Leads／Conversations JSON记录与事务回执，不新增表或迁移。

## 命令与角色

| 命令 | 请求 | 服务端行为 |
|---|---|---|
| `POST /api/v2/leads/{id}/assessment` | `LeadAssessmentInput`：原`client_request_id`、线索`version`、`assessment=helpful/not_match` | 当前求助发布人才可处理，求助须active／matched；只更新assessment、reviewed_by、reviewed_at，不改变原文、照片、求助或Pet.missing |
| `POST /api/v2/leads/{id}/contact` | `LeadContactInput`：原`client_request_id`、线索`version` | 当前发布人或原目击者可发起，服务器推导另一方；校验双方有效、联系资格及屏蔽；创建或返回原线索独立会话 |

实现为 `internal/mobile/lead_review.go`，适配在 `internal/platform/mobileapi/handler.go`。新命令走现有`s.command`，原事务同时写业务、操作回执和outbox；同原请求重放不会制造新记录，改写原请求内容仍拒绝。重放按当前读取权限返回现行记录，关闭后回读过去已提交结果不等于允许新写入。

`Lead`新增可选处理字段，空值兼容待核实；未知处理状态关闭新标记，不能默认为已核实。`Conversation`新增可选`context_kind`／`context_id`；原普通会话空值保持原语义。独立键为`lead:{leadId}:{双方pair}`，不会合并普通私信、其它线索或领养会话，不自动重开已拒绝会话。返回既有记录时也重新核对当前上下文。

陌生人会话pending，朋友关系可按原规则accepted；没有新授予同意或朋友关系。原`directContact`规则继续有效：屏蔽拒绝，非朋友双方须有有效成年人证据。pending只允许发起人一条文字，照片／贴纸与对方回复须接受后发送。

## 当前权限与撤回

读取Leads依赖当前Case.owner或原Lead.owner；旧Participants快照不成为永久授权。读取线索会话还核对context_kind、context_id、RefID和当前双方pair。消息、媒体、收件箱摘要与快照继续走原`canRead`链，因此父事项撤回能传递到关联资源。

- Case关闭：授权历史仍可读；新标记／联系、接受请求及新消息拒绝。
- Case或Lead移除／删除：关联线索与会话撤回读取，消息沿父会话撤回；照片经此链授予的第三方读取撤回，原上传者本人权限保留。
- Case发布人改变：旧发布人失去Lead读取，旧pair会话对双方均关闭；新发布人建立自己的独立pair会话，不接管旧私密聊天。
- 接受／拒绝均先核对线索上下文；接受再检查可写状态与原联系规则。拒绝不会重新开启联系。

跨语言合同已同步OpenAPI、共享DTO及Web派生。导出器的多行struct匹配锚定到换行，避免把单行嵌入Message结构误吞为后面的Lead声明。当前249路径／280操作／130服务端schemas、132Web schemas、81对DTO；45业务表、13迁移未变。

## 回归与未验证项

`internal/mobile/lead_review_test.go`补7项回归源码：处理角色／原证据与走失状态保留、独立会话与pending限制、关闭后历史与写限制、移除后的消息／媒体撤回、版本与屏蔽、成年人规则、当前发布人取代参与人快照。未执行任何测试。

18项只读接线检查、Go格式语法、跨语言字段／路径及客户端引用已核对。实际事务并发、持久仓库、回执网络恢复、媒体网关、原生与Web交互均未运行验收。客户端与设计依据见[交接](../client/PRIVATE_LEAD_FLOW_2026_10_26.md)。
