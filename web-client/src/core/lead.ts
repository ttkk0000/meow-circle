import type { RecordData } from "./api";
import type { DTOLead } from "./contract";

export type LeadRecord = RecordData<DTOLead>;
const states = {
  pending: {
    title: "待核实",
    detail: "不公开精确位置，不改变目击者的原始描述。",
  },
  helpful: {
    title: "有帮助",
    detail: "仍不等于找回；不改写对方原话，不自动公开其位置。",
  },
  not_match: {
    title: "不匹配",
    detail: "不是对目击者作违规认定，后续仍可继续核实。",
  },
  unknown: {
    title: "处理状态待核对",
    detail: "尚未读到支持的处理状态，请重新读取；原线索保持不变。",
  },
};
export function leadState(value: unknown) {
  if (value === undefined || value === "" || value === "pending")
    return states.pending;
  return value === "helpful"
    ? states.helpful
    : value === "not_match"
      ? states.not_match
      : states.unknown;
}
/** 入口预测不替代服务端当前资格、关系和版本校验。 */
export function canAssessLead(
  lead: LeadRecord,
  parent: RecordData,
  owner: bigint,
) {
  return (
    owner > 0n &&
    owner === parent.owner_id &&
    lead.reference_id === parent.id &&
    ["active", "matched"].includes(parent.state) &&
    !["deleted", "removed"].includes(lead.state) &&
    leadState(lead.data.assessment) !== states.unknown
  );
}
export function canContactLead(
  lead: LeadRecord,
  parent: RecordData,
  owner: bigint,
) {
  return (
    owner > 0n &&
    lead.reference_id === parent.id &&
    [lead.owner_id, parent.owner_id].includes(owner) &&
    lead.owner_id !== parent.owner_id &&
    ["active", "matched"].includes(parent.state) &&
    !["deleted", "removed"].includes(lead.state)
  );
}
