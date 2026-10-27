import { integer, parseWire, encodeWire } from "../core/wire";

export type Kind =
  | "reviews"
  | "rights-cases"
  | "data-requests"
  | "guardian-invites";
export const views = [
  "dashboard",
  "reviews",
  "reviewDetail",
  "rights",
  "rightsDetail",
  "privacy",
  "guardian",
  "readiness",
  "access",
  "confirm",
  "pending",
  "forbidden",
  "disconnected",
  "emergency",
] as const;
export type View = (typeof views)[number];
export type Row = Record<string, unknown>;
export interface Context {
  id: string;
  roles: string[];
  operator_name: string;
  operator_address: string;
  operator_contact: string;
  published_types: string[];
  documents: Array<{
    key: string;
    title: string;
    type: string;
    revision: string;
    status: string;
    effective_from: string;
    effective_until: string;
    sha256: string;
  }>;
}
export interface Queue {
  items: Row[];
  next_cursor: bigint;
  loaded: boolean;
}
export interface Detail {
  kind: Kind;
  id: string;
  row: Row;
}
export interface Prepared {
  kind: Kind;
  id: string;
  version: bigint;
  path: string;
  body: Row;
  title: string;
  phase: "ready" | "unknown" | "accepted" | "rejected";
  result?: Row;
  checked?: Row;
  explanation?: string;
}
export const sections: Array<[View, string, string]> = [
  ["dashboard", "业务工作台", "house"],
  ["reviews", "举报与申诉", "message-circle"],
  ["rights", "权利事项", "shield-check"],
  ["privacy", "资料与注销", "user-round"],
  ["guardian", "监护核验", "user-round"],
  ["readiness", "发布准入", "check"],
  ["access", "角色与会话", "shield-check"],
];
export const titles: Record<View, string> = {
  dashboard: "业务工作台",
  reviews: "举报与申诉",
  reviewDetail: "事项核实详情",
  rights: "权利事项",
  rightsDetail: "权利处理详情",
  privacy: "资料与注销请求",
  guardian: "监护关系核验",
  readiness: "发布准入",
  access: "角色与会话",
  confirm: "提交确认",
  pending: "结果待核对",
  forbidden: "访问受限",
  disconnected: "未连接",
  emergency: "安全事件响应",
};
export const roleFor = (kind: Kind) =>
  kind === "guardian-invites"
    ? "identity_verifier"
    : kind === "data-requests"
      ? "privacy"
      : "moderator";
export const listView = (kind: Kind): View =>
  (
    ({
      reviews: "reviews",
      "rights-cases": "rights",
      "data-requests": "privacy",
      "guardian-invites": "guardian",
    }) as const
  )[kind];
export const detailView = (kind: Kind): View =>
  kind === "reviews"
    ? "reviewDetail"
    : kind === "rights-cases"
      ? "rightsDetail"
      : listView(kind);
export const pagePath = (view: View, id?: string) =>
  `/operator?view=${view}${id ? `&id=${id}` : ""}`;
export const draftKey = (kind: Kind, id: string) => `${kind}/${id}`;
export const receipt = (row: Row) => (row.receipt ?? row) as Row;
export const versionOf = (row: Row) => integer(receipt(row).version);
export const bodyOf = (row: Row) => (row.data ?? row) as Row;
export function copy<T>(value: T): T {
  return parseWire<T>(encodeWire(value));
}
export const schemaFor = (kind: Kind) =>
  (
    ({
      reviews: "ReviewDecision",
      "rights-cases": "RightsDecision",
      "data-requests": "DataHoldInput",
      "guardian-invites": "GuardianEvidenceInput",
    }) as const
  )[kind];
const labels: Record<string, string> = {
  submitted: "待核实",
  in_review: "核实中",
  needs_info: "需补充",
  approved: "核实通过",
  rejected: "已拒绝",
  expired: "已过期",
  accepted: "已受理",
  closed: "已结案",
  report: "举报",
  appeal: "申诉",
  verification: "信息核实",
  recovery: "恢复申请",
  support: "支持请求",
  notice: "权利通知",
  counter: "反通知",
  supplement: "补充",
  safety: "安全事项",
  guardian_relation_pending: "待核对关系",
  guardian_relation_verified: "关系已核实，待本人授权",
  verified_pending_consent: "关系已核实，待本人授权",
  processing: "处理中",
  partially_held: "部分保全",
  erased: "已删除",
  completed: "已完成",
  physical_pending: "物理副本待清理",
  provider_pending: "供应商待核对",
  held: "保全中",
  pending: "待处理",
  restricted: "受限副本",
  draft: "草案",
  published: "已发布",
  proposed: "拟议",
  archived: "已归档",
  reference: "参考材料",
  pending_review: "待核对送达",
  not_required: "无需送达",
  pending_transfer: "待送达",
  transferred: "已有实际送达记录",
};
export const label = (value: unknown) =>
  labels[String(value)] ?? (String(value ?? "") || "未提供");
export function time(value: unknown) {
  if (
    !value ||
    String(value).startsWith("0001-01-01") ||
    !Number.isFinite(Date.parse(String(value)))
  )
    return "未提供";
  return new Date(String(value)).toLocaleString("zh-CN", { hour12: false });
}
export function formError(kind: Kind, values: Row, row: Row): string {
  if (
    integer(
      kind === "guardian-invites" ? values.invite_version : values.version,
    ) !== versionOf(row) ||
    versionOf(row) <= 0n
  )
    return "原事项版本已变化，请重新读取并明确采用新版本。";
  const text = (v: unknown, max: number) =>
    typeof v === "string" && v.trim().length > 0 && Array.from(v).length <= max;
  const future = (v: unknown, max = Infinity) =>
    typeof v === "string" &&
    Date.parse(v) > Date.now() &&
    Date.parse(v) <= Date.now() + max * 86400000;
  if (kind === "reviews") {
    if (
      !["in_review", "needs_info", "approved", "rejected", "expired"].includes(
        String(values.state),
      ) ||
      !text(values.summary, 1000)
    )
      return "选择处理方向并填写必要说明（最多1000字）。";
    if (
      bodyOf(row).type === "verification" &&
      values.state === "approved" &&
      !future(values.valid_until, 90)
    )
      return "信息核实通过须填写90天内的有效截止。";
  } else if (kind === "rights-cases") {
    if (
      !["in_review", "needs_info", "accepted", "rejected", "closed"].includes(
        String(values.state),
      ) ||
      !text(values.summary, 2000) ||
      !future(values.review_due)
    )
      return "选择处理方向，填写必要说明和未来复核时间。";
    if (
      ![
        "pending_review",
        "not_required",
        "pending_transfer",
        "transferred",
      ].includes(String(values.transfer_state))
    )
      return "核对实际送达状态。";
    if (
      values.transfer_state === "transferred" &&
      !text(values.transfer_receipt, 160)
    )
      return "已有实际送达记录时须填写真实回执引用。";
    if (
      values.evidence_until &&
      (!future(values.evidence_until, 90) ||
        !text(values.retention_basis, 1000) ||
        !text(values.responsible, 120))
    )
      return "延长证据保全须填写90天内期限、具体依据与负责人。";
  } else if (kind === "guardian-invites") {
    if (
      row.state !== "guardian_relation_pending" ||
      !future(row.expires_at) ||
      !text(values.evidence_ref, 120) ||
      !future(values.valid_until, 366)
    )
      return "仅处理有效待核对邀请；填写已核实证据引用及366天内期限。";
  } else {
    const hold = values.hold as Row | undefined;
    if (
      !hold ||
      !text(hold.basis, 1000) ||
      !text(hold.responsible, 120) ||
      !future(hold.review_at, 90)
    )
      return "保全须填写具体范围、依据、负责人及90天内复核时间。";
  }
  return "";
}
