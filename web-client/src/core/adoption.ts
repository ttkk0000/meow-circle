import type { RecordData } from "./api";
import { integer } from "./wire";

export type AdoptionContract = Record<string, unknown>;
export function returnPlans(row: RecordData): Array<Record<string, unknown>> {
  return Array.isArray(row.data.return_plans) ? row.data.return_plans : [];
}
export function returnActionAllowed(
  row: RecordData,
  actor: bigint,
  action: string,
  revision: bigint,
) {
  if (
    actor <= 0n ||
    ![integer(row.data.publisher_id), integer(row.data.applicant_id)].includes(
      actor,
    ) ||
    !["return_requested", "return_agreed"].includes(row.state)
  )
    return false;
  const plan = returnPlans(row).find(
    (plan) => integer(plan.number) === revision,
  );
  if (!plan) return false;
  const pending = plan.state === "pending",
    mine = integer(plan.proposed_by) === actor,
    future =
      Date.parse(String((plan.terms as Record<string, unknown>)?.starts_at)) >
      Date.now();
  if (action === "withdraw") return pending && mine;
  if (action === "accept")
    return pending && !mine && future && !includes(plan.confirmations, actor);
  if (action === "decline") return pending && !mine && future;
  return (
    action === "handover" &&
    plan.state === "effective" &&
    integer(row.data.effective_return) === revision &&
    !includes(plan.handover, actor)
  );
}
export function currentAdoptionContract(
  row: RecordData,
): AdoptionContract | undefined {
  const contracts = (
    Array.isArray(row.data.contracts) ? row.data.contracts : []
  ) as AdoptionContract[];
  if (contracts.some((contract) => contract.status === "pending")) return;
  return contracts.find(
    (contract) =>
      integer(contract.number) === integer(row.data.effective_contract) &&
      contract.status === "effective" &&
      [integer(row.data.publisher_id), integer(row.data.applicant_id)].every(
        (party) =>
          Array.isArray(contract.accepted_by) &&
          contract.accepted_by.some((actor) => integer(actor) === party),
      ),
  );
}
const includes = (value: unknown, actor: bigint) =>
  Array.isArray(value) && value.some((item) => integer(item) === actor);
export function careStageAvailable(row: RecordData) {
  const data = row.data,
    trial = [integer(data.publisher_id), integer(data.applicant_id)].every(
      (actor) => includes(data.trial_confirmations, actor),
    );
  return (
    (row.state === "trial_active" && !data.completed_at && trial) ||
    (["handover_pending", "return_requested", "return_agreed"].includes(
      row.state,
    ) &&
      !data.completed_at &&
      (trial ||
        includes(data.handover_confirmations, integer(data.applicant_id)))) ||
    (["completed", "return_requested", "return_agreed"].includes(row.state) &&
      !!data.completed_at)
  );
}
export function adoptionActionOptions(
  row: RecordData,
  actor: bigint,
): string[] {
  const data = row.data,
    publisher = integer(data.publisher_id) === actor,
    applicant = integer(data.applicant_id) === actor,
    state = row.state;
  if (actor <= 0n || (!publisher && !applicant)) return [];
  const actions: string[] = [],
    contract = currentAdoptionContract(row),
    terms = contract?.terms as Record<string, unknown> | undefined;
  if (publisher && ["submitted", "supplemented"].includes(state))
    actions.push("approve", "reject", "request_supplement");
  if (applicant && state === "needs_info") actions.push("supplement");
  if (
    applicant &&
    [
      "submitted",
      "needs_info",
      "supplemented",
      "approved",
      "trial_proposed",
      "waitlisted",
    ].includes(state)
  )
    actions.push("withdraw");
  if (applicant && state === "withdrawn") actions.push("resubmit");
  if (publisher && state === "waitlisted") actions.push("resume_candidate");
  if (
    ["approved", "trial_proposed"].includes(state) &&
    contract &&
    Date.parse(String(terms?.trial_until)) > Date.now()
  )
    actions.push("propose_trial");
  if (state === "trial_proposed") {
    if (
      contract &&
      !includes(data.trial_confirmations, actor) &&
      Date.parse(String(data.trial_until)) > Date.now()
    )
      actions.push("confirm_trial");
    actions.push("decline_trial");
  }
  if (
    contract &&
    ["approved", "trial_active", "handover_pending"].includes(state) &&
    !includes(data.handover_confirmations, actor)
  )
    actions.push("confirm_handover");
  if (
    [
      "trial_active",
      "handover_pending",
      "completed",
      "return_requested",
      "return_agreed",
    ].includes(state)
  )
    actions.push("support", "request_return");
  return actions;
}
export function adoptionProgressTitle(row: RecordData, actor: bigint) {
  switch (row.state) {
    case "waitlisted":
      return "当前为候补";
    case "needs_info":
      return integer(row.data.applicant_id) === actor
        ? "补充本次申请材料"
        : "等待申请人补充";
    case "trial_proposed":
      return includes(row.data.trial_confirmations, actor)
        ? "等待另一方确认试养"
        : "确认收到的试养安排";
    case "trial_active":
      return "试养进行中";
    case "handover_pending":
      return includes(row.data.handover_confirmations, actor)
        ? "等待另一方确认交接"
        : "核对并确认实际交接";
    case "completed":
      return "交接已完成";
    case "withdrawn":
      return "本次申请已撤回";
    case "rejected":
      return "本次申请暂未通过";
    case "return_requested":
    case "return_agreed":
      return "协商安全接回";
    case "returned":
      return "本次接回已完成";
    default:
      return "领养申请进度";
  }
}
