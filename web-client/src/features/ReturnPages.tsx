import type { RecordData } from "../core/api";
import { returnPlans } from "../core/adoption";
import { integer } from "../core/wire";
import { Card, Notice } from "../components/ui";
import { stateLabel } from "./workflows";

function scheduledTime(value: unknown) {
  const raw = String(value ?? ""),
    date = new Date(raw);
  return Number.isFinite(date.getTime())
    ? date.toLocaleString("zh-CN", { hour12: false, timeZoneName: "short" })
    : "未提供有效时间";
}
function PlanSummary({
  plan,
  title,
}: {
  plan: Record<string, unknown>;
  title: string;
}) {
  const terms = (plan.terms ?? {}) as Record<string, unknown>;
  return (
    <Card title={title}>
      <dl className="appointment-facts">
        {[
          [
            "提案",
            `R${String(plan.number)} · ${stateLabel(String(plan.state))}`,
          ],
          ["计划时间／确认截止", scheduledTime(terms.starts_at)],
          ["地点", String(terms.place ?? "未提供")],
          ["接回人与接待方", String(terms.reception ?? "未提供")],
          ["接回前照护", String(terms.care_until ?? "未提供")],
          ["费用承担", String(terms.costs ?? "未提供")],
          ["安全与异常联络", String(terms.safety ?? "未提供")],
        ].map(([name, value]) => (
          <div key={name}>
            <dt>{name}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
}
export function ReturnActionReview({
  row,
  revision,
}: {
  row: RecordData;
  revision: bigint;
}) {
  const plans = returnPlans(row),
    plan = plans.find((plan) => integer(plan.number) === revision),
    effective = plans.find(
      (plan) =>
        integer(plan.number) === integer(row.data.effective_return) &&
        plan.state === "effective",
    );
  if (!plan)
    return (
      <Notice title="原接回方案已不可读取" warning>
        请返回读取原事项与最新版本，本机说明继续保留，不确认未知方案。
      </Notice>
    );
  return (
    <>
      <Notice title={"本次仅核对R" + String(revision)}>
        逐项核对全部更正，不复用其它版本的同意。接受方案与确认本人实际交接是两个步骤；拒绝、撤回或到期不表示接回完成。
      </Notice>
      <div className="appointment-panels">
        {effective && effective !== plan ? (
          <PlanSummary plan={effective} title="原有效安排仍保留" />
        ) : null}
        <PlanSummary plan={plan} title="本次接回方案" />
      </div>
      {plan.state === "pending" &&
      Date.parse(String((plan.terms as Record<string, unknown>)?.starts_at)) <=
        Date.now() ? (
        <Notice title="本次未生效提案已过期" warning>
          确认截止为计划接回时间；当前照护、求助与原有效安排继续，须重新协商。
        </Notice>
      ) : null}
    </>
  );
}
export function ReturnProgress({
  row,
  actor,
}: {
  row: RecordData;
  actor: bigint;
}) {
  const plans = returnPlans(row),
    effective = plans.find(
      (plan) => integer(plan.number) === integer(row.data.effective_return),
    ),
    pending = plans
      .filter((plan) => plan.state === "pending")
      .sort((a, b) => (integer(a.number) > integer(b.number) ? -1 : 1))[0];
  if (!["return_requested", "return_agreed", "returned"].includes(row.state))
    return null;
  const both =
    effective?.state === "completed" &&
    [integer(row.data.publisher_id), integer(row.data.applicant_id)].every(
      (party) =>
        Array.isArray(effective.handover) &&
        effective.handover.some((value) => integer(value) === party),
    );
  return (
    <>
      <Notice
        title={
          row.state === "returned" && both
            ? "本次接回已完成"
            : row.state === "return_agreed"
              ? "接回安排已确认，实际交接分别记录"
              : "接回方案仍需双方核对"
        }
      >
        {row.state === "returned" && both
          ? "双方实际交接确认已记录，历史方案继续保留。"
          : "当前照护与求助继续；仅有一方确认或约定成立，不表示实际接回完成。"}
      </Notice>
      {pending ? (
        <PlanSummary
          plan={pending}
          title={
            integer(pending.proposed_by) === actor
              ? "本人新提案，等待对方确认"
              : "对方新提案，待本人核对"
          }
        />
      ) : effective ? (
        <PlanSummary plan={effective} title="当前接回记录" />
      ) : null}
    </>
  );
}
