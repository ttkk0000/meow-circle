import type { ReactNode } from "react";
import type { RecordData } from "../core/api";
import { integer } from "../core/wire";
import { navigate } from "../core/router";
import { invitationActions, invitationReview } from "../core/invitation";
import { Button, Card, Check, Notice, RowLink, TaskPage } from "../components/ui";
import { actionNames, stateLabel, workflowURL } from "./workflows";

export function collaborationScope(row: RecordData) {
  return row.data.scope === "caretaker" ? "照护协作" : "介绍协作";
}

export function CollaborationInvitation({ row, actor }: { row: RecordData; actor: bigint }) {
  const recipient = integer(row.data.recipient_id) === actor;
  const allowed = actor > 0n && (recipient || row.owner_id === actor);
  const scope = collaborationScope(row);
  const title = row.state === "left" ? `${recipient ? "已退出" : "对方已退出"}${scope}`
    : row.state === "accepted" ? recipient ? `已加入${scope}` : `${scope}已授权`
    : row.state === "declined" ? `${scope}邀请已拒绝`
    : row.state === "expired" ? "邀请已过期"
    : row.state === "revoked" ? `${scope}授权已结束` : `${scope}邀请`;
  if (!allowed) return (
    <TaskPage title="当前协作不可用">
      <Notice title="不再显示受限资料">请用收到邀请或发出邀请的账号重新核对。</Notice>
      <RowLink title="返回消息" to="/messages" />
    </TaskPage>
  );
  const active = row.state === "accepted";
  return (
    <TaskPage title={title} subtitle={`邀请 #${row.id} · 事项 #${integer(row.data.target_id)} · 当前版本 ${row.version}`}>
      <Notice title={active ? "本次有限授权" : row.state === "left" || row.state === "revoked" ? "仅结束本次协作权限" : "邀请范围"}>
        {invitationReview(row, row.state === "revoked" ? "revoke" : row.state === "left" && recipient ? "leave" : "accept").detail}
      </Notice>
      {row.data.note ? <Card title="邀请说明"><p>{String(row.data.note)}</p></Card> : null}
      <p>当前状态：{stateLabel(row.state)}</p>
      {row.state === "pending" ? <p>接受邀请截止：{String(row.data.expires_at ?? "待核对")}；截止只限制尚未接受的邀请。</p> : null}
      {active && recipient ? (
        <RowLink title={row.data.scope === "caretaker" ? "查看授权照护近况" : "维护该宠物介绍"}
          to={row.data.scope === "caretaker" ? `/care-collaboration/${integer(row.data.target_id)}`
            : workflowURL("introduction", "pets", integer(row.data.target_id))} />
      ) : null}
      {invitationActions(row, actor).map(action => (
        <RowLink key={action} title={action === "leave" ? invitationReview(row, action).title : actionNames[action]}
          detail={`以本次读取版本 ${row.version} 主动核对，不自动提交`}
          to={workflowURL("invitation-action", "invitations", row.id, { action })} />
      ))}
      {row.state === "left" ? <Notice title="当前记录已退出">这是当前邀请状态；核对某次未知请求时仍需读取原操作回执。</Notice> : null}
      {row.state === "left" ? <RowLink title="核对原操作回执" to="/recovery" /> : null}
      <RowLink title="返回邀请与协作列表" to={`/invitations?ref=${integer(row.data.target_id)}&scope=${String(row.data.scope)}`} />
      <RowLink title="返回消息" to="/messages" />
    </TaskPage>
  );
}

export function InvitationActionTask({ row, action, baseVersion, requestId, prepared, uncertain, busy, error, conflict,
  agreed, onAgree, onReview, onSubmit, onEdit, conflictActions,
}: {
  row: RecordData; action: "leave" | "revoke"; baseVersion: bigint; requestId: string; prepared: boolean; uncertain: boolean;
  busy: boolean; error: string; conflict: boolean; agreed: boolean;
  onAgree: (value: boolean) => void; onReview: () => void; onSubmit: () => void; onEdit: () => void;
  conflictActions: ReactNode;
}) {
  const review = invitationReview(row, action);
  const confirmLabel = action === "leave" ? `确认退出${collaborationScope(row)}` : row.state === "pending" ? "确认撤销邀请" : row.data.scope === "caretaker" ? "确认撤销照护授权" : "确认结束介绍协作";
  const cancelLabel = action === "leave" ? "保留当前协作" : row.state === "pending" ? "保留邀请" : "保留当前授权";
  const disabled = busy || conflict || uncertain;
  return (
    <TaskPage title={uncertain ? "操作结果待核对" : conflict ? "邀请状态已变化" : error ? "本次操作未完成" : review.title}
      actions={uncertain ? <Button disabled={busy} onClick={() => navigate("/recovery")}>核对原操作结果</Button>
        : <Button disabled={disabled || (prepared && !agreed)} onClick={prepared ? onSubmit : onReview}>
          {busy ? "正在提交…" : prepared ? confirmLabel : "核对本次范围"}
        </Button>}>
      <Notice title={`${review.title}？`}>{review.detail}</Notice>
      <Card title="本次操作对象">
        <p>事项 #{String(integer(row.data.target_id))} · 邀请 #{String(row.id)}</p>
        <p>基准版本 {String(baseVersion)} · 上次读取 {stateLabel(row.state)}</p>
        {uncertain ? <p className="caption muted">原请求编号：{requestId}</p> : null}
      </Card>
      {uncertain ? <Notice title="原请求、对象与版本保持">读取状态变化不代表本次操作成功，不会自动重新提交。先核对原操作回执。</Notice> : null}
      {error ? <Notice title={error} error /> : null}
      {conflictActions}
      {prepared ? <Check checked={agreed} onChange={onAgree} disabled={disabled}>我已核对本次事项、邀请、版本与操作范围</Check> : null}
      {prepared ? <Button disabled={disabled} onClick={onEdit}>返回核对范围</Button> : null}
      <Button disabled={busy || uncertain} onClick={() => navigate(`/invitations/${row.id}`)}>
        {conflict ? "返回当前邀请" : cancelLabel}
      </Button>
    </TaskPage>
  );
}
