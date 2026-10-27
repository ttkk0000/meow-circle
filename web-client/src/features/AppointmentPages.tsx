import type { ReactNode } from "react";
import type { RecordData } from "../core/api";
import {
  appointmentEffective,
  appointmentPending,
  appointmentSafetyResolved,
  appointmentVersions,
  canConfirmAppointment,
  safetyProposal,
} from "../core/appointment";
import { integer, money } from "../core/wire";
import { navigate } from "../core/router";
import {
  Button,
  Card,
  Check,
  Field,
  Notice,
  RowLink,
  TaskPage,
} from "../components/ui";
import { stateLabel, workflowURL } from "./workflows";

function Terms({ value }: { value: unknown }) {
  const t = (value ?? {}) as Record<string, unknown>;
  return (
    <dl className="appointment-facts">
      {[
        ["时间", String(t.starts_at ?? "未提供")],
        ["地点", String(t.place ?? "未提供")],
        ["接待方", String(t.reception ?? "未提供")],
        ["宠物", String(t.pet_name ?? "未提供")],
        [
          "全部费用",
          money(t.amount_cents) + " · 到访费 " + money(t.visit_fee_cents),
        ],
        [
          "必付费用与收款方",
          String(t.required_fees ?? "") + " · " + String(t.payee ?? ""),
        ],
      ].map(([name, value]) => (
        <div key={name}>
          <dt>{name}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}
export function AppointmentVersionReview({
  row,
  revision,
}: {
  row: RecordData;
  revision?: bigint;
}) {
  const current = appointmentEffective(row),
    proposed = revision
      ? appointmentVersions(row).find(
          (version) =>
            integer(version.number) === revision &&
            version.status === "replaced",
        )
      : appointmentPending(row);
  return (
    <div className="appointment-panels">
      <Card
        title={
          row.state === "safety_hold"
            ? "暂停前的安排（不可据此出发）"
            : "当前有效安排"
        }
      >
        {current ? (
          <Terms value={current.terms} />
        ) : (
          <p>双方尚未确认有效安排</p>
        )}
      </Card>
      <Card title={revision ? "历史内容将新建提案" : "本次待确认的新版本"}>
        {proposed ? (
          <>
            <strong>V{String(proposed.number)}</strong>
            <Terms value={proposed.terms} />
            {proposed.resolution_note ? (
              <p>具体更正与核对：{String(proposed.resolution_note)}</p>
            ) : null}
          </>
        ) : (
          <p>当前没有可核对的提案或已替代历史版本。</p>
        )}
      </Card>
    </div>
  );
}
export function AppointmentDetail({
  row,
  actor,
}: {
  row: RecordData;
  actor: bigint;
}) {
  const held = row.state === "safety_hold",
    ended = ["cancelled", "expired"].includes(row.state),
    party =
      actor > 0n && [row.owner_id, integer(row.data.peer_id)].includes(actor);
  const effective = appointmentEffective(row),
    pending = appointmentPending(row),
    safety = safetyProposal(row);
  const action = (name: string) =>
    workflowURL("appointment-action", "appointments", row.id, { action: name });
  return (
    <TaskPage
      title={
        held
          ? safety
            ? "新安排等待双方核对确认"
            : "预约已进入安全暂停"
          : row.state === "cancelled"
            ? "本次预约已取消"
            : appointmentSafetyResolved(row)
              ? "新安排已确认"
              : "预约安排"
      }
      subtitle={"#" + row.id + " · 记录版本 " + row.version}
    >
      <Notice
        warning={held}
        title={
          held
            ? "先不要按原安排出发"
            : row.state === "cancelled"
              ? "本次安排已结束"
              : appointmentSafetyResolved(row)
                ? "安全核对说明与双方确认已记录"
                : "有效安排与提案分别保留"
        }
      >
        {held
          ? "修改提案、拒绝、撤回或到期均不能解除暂停。原原因：" +
            String(row.data.safety_reason || row.data.reason || "请核对原记录")
          : row.state === "cancelled"
            ? "申请、会话与必要证据保留。再次见面须重新提出预约并由双方确认。"
            : "未回复不等于同意，历史内容恢复须生成新版本并重新确认。"}
      </Notice>
      <div className="appointment-panels">
        <Card title={held ? "暂停前的安排（不可据此出发）" : "当前有效安排"}>
          {effective ? (
            <>
              <strong>V{String(effective.number)}</strong>
              <Terms value={effective.terms} />
            </>
          ) : (
            <p>双方尚未确认有效安排</p>
          )}
        </Card>
        {pending ? (
          <Card title={"待确认新提案 V" + String(pending.number)}>
            <Terms value={pending.terms} />
            <p>
              已确认：
              {Array.isArray(pending.confirmations)
                ? pending.confirmations
                    .map((v) => "用户 #" + String(v))
                    .join("、")
                : "暂无"}
            </p>
            {held ? (
              <p>
                本次问题更正与核对：
                {String(pending.resolution_note ?? "尚未完成核对")}
              </p>
            ) : null}
            {party && (!held || safety === pending) ? (
              <>
                {canConfirmAppointment(row, actor) ? (
                  <RowLink
                    title={
                      held ? "核对问题、更正并确认此新版本" : "核对并确认此版本"
                    }
                    to={action("confirm")}
                  />
                ) : null}
                <RowLink
                  title={
                    integer(pending.proposed_by) === actor
                      ? "撤回本次提案"
                      : "拒绝本次提案"
                  }
                  detail={held ? "安全暂停继续" : "保留原有效安排"}
                  to={action(
                    integer(pending.proposed_by) === actor
                      ? "withdraw_change"
                      : "decline",
                  )}
                />
              </>
            ) : null}
          </Card>
        ) : null}
      </div>
      {party && !ended ? (
        <Card title={held ? "重新协商与独立出口" : "安排变更"}>
          <RowLink
            title={held ? "安全核对与新安排" : "提出普通改约"}
            detail={
              held ? "具体问题、更正与双方新确认" : "新提案不会覆盖原有效安排"
            }
            to={workflowURL(
              "appointment-change",
              "appointments",
              row.id,
              held ? { safety: "1" } : {},
            )}
          />
          {!held ? (
            <RowLink title="因具体安全问题暂停" to={action("safety_hold")} />
          ) : null}
          <RowLink
            title="取消本次预约"
            detail="先确认取消范围，不等举报处理"
            to={action("cancel")}
          />
        </Card>
      ) : null}
      <Card title="预约版本记录">
        {appointmentVersions(row)
          .slice()
          .reverse()
          .map((version) => (
            <section className="version-section" key={String(version.number)}>
              <strong>
                V{String(version.number)} · {stateLabel(String(version.status))}
              </strong>
              <Terms value={version.terms} />
              {version.resolution_note ? (
                <p>安全核对记录：{String(version.resolution_note)}</p>
              ) : null}
              {party && !held && !ended && version.status === "replaced" ? (
                <RowLink
                  title={"以历史V" + String(version.number) + "内容提出新版本"}
                  to={workflowURL(
                    "appointment-action",
                    "appointments",
                    row.id,
                    { action: "restore", revision: String(version.number) },
                  )}
                />
              ) : null}
            </section>
          ))}
      </Card>
      <RowLink
        title="独立报告相关问题"
        detail="报告不会自动取消预约"
        to={workflowURL("safety", "appointments", row.id)}
      />
      {party &&
      row.state === "confirmed" &&
      effective &&
      Date.parse(
        String((effective.terms as Record<string, unknown>)?.starts_at),
      ) <= Date.now() ? (
        <RowLink
          title="逐项填写本次到访反馈"
          to={workflowURL("feedback", "appointments", row.id)}
        />
      ) : null}
      <RowLink title="返回预约列表" to="/appointments" />
    </TaskPage>
  );
}

export function AppointmentCancelTask({
  row,
  baseVersion,
  note,
  onNote,
  prepared,
  uncertain,
  busy,
  error,
  conflict,
  agreed,
  onAgree,
  onReview,
  onSubmit,
  onEdit,
  conflictActions,
}: {
  row: RecordData;
  baseVersion: bigint;
  note: string;
  onNote: (value: string) => void;
  prepared: boolean;
  uncertain: boolean;
  busy: boolean;
  error: string;
  conflict: boolean;
  agreed: boolean;
  onAgree: (value: boolean) => void;
  onReview: () => void;
  onSubmit: () => void;
  onEdit: () => void;
  conflictActions: ReactNode;
}) {
  const held = row.state === "safety_hold";
  return (
    <TaskPage
      title={
        uncertain
          ? "取消结果待确认"
          : error
            ? "取消暂未完成"
            : "确认取消这次预约"
      }
      actions={
        uncertain ? (
          <Button
            kind="primary"
            disabled={busy}
            onClick={() => navigate("/recovery")}
          >
            查询本次原请求
          </Button>
        ) : (
          <>
            <Button
              disabled={busy}
              onClick={() => navigate("/appointments/" + row.id)}
            >
              {held ? "保留预约并继续暂停" : "保留本次预约"}
            </Button>
            <Button
              kind="primary"
              disabled={
                busy || conflict || !note.trim() || (prepared && !agreed)
              }
              onClick={prepared ? onSubmit : onReview}
            >
              {prepared ? "确认取消本次预约" : "核对取消范围"}
            </Button>
          </>
        )
      }
    >
      <Notice
        warning={held && !uncertain}
        title={
          uncertain
            ? "原请求、版本与说明已保留"
            : held
              ? "上次读取为安全暂停"
              : "取消范围仅本次预约"
        }
      >
        {uncertain
          ? "当前取消结果尚未确认，查询或退出等待不会确认成功。先核对原请求，不重建请求。"
          : held
            ? "退出或保留仍保持暂停；当前状态仍需服务端核对，不按原安排出发。"
            : "原申请、会话与必要证据保留，取消不自动视为无故失约。"}
      </Notice>
      <Card title={"预约 #" + row.id}>
        <p>
          本次基准版本 {String(baseVersion)} · 上次读取 {stateLabel(row.state)}
        </p>
        <p>取消范围：仅本次预约</p>
        <p>再次见面：须重新提出预约并由双方确认</p>
      </Card>
      {error ? <Notice title={error} error /> : null}
      {conflictActions}
      <Field
        label="取消原因"
        value={note}
        onChange={onNote}
        multiline
        maxLength={1000}
        disabled={busy || prepared || uncertain}
      />
      {prepared && !uncertain ? (
        <>
          <Check checked={agreed} onChange={onAgree} disabled={busy}>
            我确认取消本次预约，了解原申请和会话仍保留。
          </Check>
          <Button disabled={busy} onClick={onEdit}>
            返回核对原因
          </Button>
        </>
      ) : null}
      {!prepared && error && !uncertain ? (
        <Button disabled={busy} onClick={onEdit}>
          返回核对后重试
        </Button>
      ) : null}
    </TaskPage>
  );
}
