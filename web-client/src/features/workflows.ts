import type { RecordData } from "../core/api";
import { integer } from "../core/wire";
import { schemaDefault } from "../components/SchemaForm";
import { currentAdoptionContract } from "../core/adoption";

export interface Context {
  kind: string;
  id: bigint;
  record?: RecordData;
  params: URLSearchParams;
}
export interface Workflow {
  title: string;
  schema: string;
  path: (c: Context) => string;
  method?: "POST" | "PATCH" | "PUT" | "DELETE";
  media?: "public" | "private" | "safety";
  envelope?: string;
  omit?: string[];
  initial?: (c: Context) => Record<string, unknown>;
  after?: string;
  afterView?: string;
  preview?: boolean;
  scope?: (c: Context, body: Record<string, unknown>) => string | null;
  guest?: boolean;
  replayable?: boolean;
}
const resource = (name: string) => (c: Context) =>
  c.id > 0n ? `${name}/${c.id}` : name;
const route = (suffix: string) => (c: Context) => `${c.kind}/${c.id}/${suffix}`;
const stored = (c: Context) => c.record?.data ?? {};
export const actionNames: Record<string, string> = {
  accept: "接受",
  decline: "拒绝",
  withdraw: "撤回",
  remove: "解除关系",
  block: "拉黑",
  unblock: "解除拉黑",
  request_supplement: "请求补充",
  supplement: "补充",
  resubmit: "重新提交材料",
  resume_candidate: "邀请继续核对安排",
  approve: "同意申请",
  reject: "拒绝申请",
  propose_trial: "提出试养",
  confirm_trial: "确认试养",
  decline_trial: "拒绝试养提案",
  confirm_handover: "确认本人已交接",
  support: "申请照护协助",
  request_return: "提出安全接回协商",
  match: "材料匹配",
  dispute: "提出争议",
  directory: "群发现与公开简介设置",
  notice: "修改群公告",
  transfer: "提出群主转让",
  accept_transfer: "接受群主转让",
  decline_transfer: "拒绝群主转让",
  revoke_transfer: "撤回群主转让",
  dissolve: "解散群聊",
  leave: "退出群聊",
  remove_member: "移出成员",
  mute: "切换免打扰",
  revoke: "撤销邀请",
  confirm: "确认当前提案",
  cancel: "取消安排",
  safety_hold: "安全暂停",
  safety_repropose: "安全核对并提出新安排",
  restore: "以历史内容提出新提案",
  withdraw_change: "撤回待确认提案",
  close: "结束求助",
  handover: "确认接回交接",
  delete: "删除指定记录",
};
const actionGroups: Record<string, string[]> = {
  adoptions: [
    "resubmit",
    "resume_candidate",
    "request_supplement",
    "supplement",
    "approve",
    "reject",
    "withdraw",
    "propose_trial",
    "confirm_trial",
    "decline_trial",
    "confirm_handover",
    "support",
    "request_return",
  ],
  appointments: [
    "confirm",
    "decline",
    "withdraw_change",
    "cancel",
    "safety_hold",
    "restore",
  ],
  claims: [
    "match",
    "reject",
    "request_supplement",
    "supplement",
    "resubmit",
    "withdraw",
    "dispute",
    "confirm_handover",
  ],
  groups: [
    "notice",
    "directory",
    "transfer",
    "accept_transfer",
    "decline_transfer",
    "revoke_transfer",
    "dissolve",
    "leave",
    "remove_member",
    "mute",
  ],
  friendships: ["accept", "decline", "withdraw", "remove", "block", "unblock"],
  invitations: ["accept", "decline", "revoke", "leave"],
  conversations: ["accept", "decline"],
  cases: ["close"],
};
const actionPath = (c: Context) => {
  const action = c.params.get("action") ?? "";
  if (!actionGroups[c.kind]?.includes(action))
    throw new Error("此操作入口不可用");
  return `${c.kind}/${c.id}/${action}`;
};
const actionWorkflow = (schema: string): Workflow => ({
  title: "核对本次处理",
  schema,
  path: actionPath,
  after: undefined,
  replayable: true,
  initial: (c) => ({ restore_version: integer(c.params.get("revision")) }),
});
const versioned = (
  title: string,
  schema: string,
  name: string,
  body: string,
  media: Workflow["media"],
): Workflow => ({
  title,
  schema,
  path: resource(name),
  media,
  envelope: body,
  initial: (c) => ({
    [body]:
      c.record?.data ??
      (name === "entries"
        ? {
            title: "",
            text: "",
            space: "community",
            category: "daily_share",
            tags: [],
            pet_id: 0n,
            audience: { mode: "public", user_ids: [] },
          }
        : name === "pets"
          ? {
              ...(schemaDefault(schema)[body] as Record<string, unknown>),
              species: "cat",
              audience: { mode: "private", user_ids: [] },
            }
          : schemaDefault(schema)[body]),
  }),
  after: name,
  preview: name === "entries",
  replayable: true,
});
export const workflows: Record<string, Workflow> = {
  "group-join": {
    title: "申请加入宠友群",
    schema: "GroupJoinInput",
    path: (c) => `group-directory/${c.id}/request`,
    after: "invitations",
    replayable: true,
  },
  pet: { ...versioned("宠物档案", "PetInput", "pets", "pet", "public") },
  entry: {
    ...versioned("晒晒我的宠物", "EntryInput", "entries", "entry", "public"),
  },
  case: {
    ...versioned("走失／拾获求助", "CaseInput", "cases", "case", "public"),
  },
  offer: {
    ...versioned(
      "有偿信息与完整披露",
      "OfferInput",
      "offers",
      "offer",
      "public",
    ),
  },
  profile: {
    title: "编辑个人资料",
    schema: "ProfileInput",
    path: () => "profile",
    method: "PATCH",
    media: "public",
    omit: ["theme", "avatar_media_id"],
    initial: (c) => ({
      nickname: c.record?.data.nickname ?? "",
      bio: c.record?.data.bio ?? "",
      background: c.record?.data.background ?? "picnic",
    }),
  },
  friend: {
    title: "好友申请",
    schema: "FriendInput",
    path: () => "friendships/request",
    initial: (c) => ({ user_id: c.kind === "users" ? c.id : 0n }),
    replayable: true,
  },
  invite: {
    title: "协作与成员邀请",
    schema: "InviteInput",
    path: () => "invitations",
    after: "invitations",
    initial: (c) => ({
      target_id: c.id,
      scope: c.params.get("scope") ?? "adoption_editor",
    }),
    replayable: true,
  },
  group: {
    title: "创建群聊",
    schema: "GroupInput",
    path: () => "groups",
    after: "groups",
    replayable: true,
  },
  application: {
    title: "领养申请",
    schema: "AdoptionInput",
    path: () => "adoptions",
    initial: (c) => ({ pet_id: c.id, message: "" }),
    omit: ["pet_id"],
    after: "adoptions",
    replayable: true,
  },
  appointment: {
    title: "预约安排与新提案",
    schema: "AppointmentInput",
    path: () => "appointments",
    initial: (c) => ({
      context_kind: c.params.get("context") ?? "visit",
      context_id: c.id,
      peer_id: integer(c.params.get("peer")),
    }),
    after: "appointments",
    replayable: true,
  },
  "appointment-change": {
    title: "提出预约新版本",
    schema: "AppointmentAction",
    path: (c) =>
      `${c.kind}/${c.id}/${c.record?.state === "safety_hold" ? "safety_repropose" : "propose"}`,
    omit: ["restore_version"],
    initial: (c) => ({
      terms:
        (Array.isArray(c.record?.data.versions)
          ? (c.record!.data.versions as Record<string, unknown>[]).find(
              (v) =>
                integer(v.number) === integer(c.record?.data.pending_version) ||
                integer(v.number) === integer(c.record?.data.effective_version),
            )?.terms
          : undefined) ?? {},
      note: "",
    }),
    after: "appointments",
    replayable: true,
  },
  contract: {
    title: "提出协议新版本",
    schema: "ContractInput",
    path: route("contracts/propose"),
    after: "adoptions",
    replayable: true,
  },
  "contract-accept": {
    title: "接受指定版本协议",
    schema: "ContractInput",
    path: route("contracts/accept"),
    omit: ["terms", "revision"],
    initial: (c) => ({ revision: integer(c.params.get("revision")) }),
    scope: route("contracts/scope"),
    after: "adoptions",
  },
  return: {
    title: "安全接回方案",
    schema: "ReturnInput",
    path: route("returns/propose"),
    omit: ["revision"],
    after: "adoptions",
    replayable: true,
  },
  care: {
    title: "私密照护近况",
    schema: "CareInput",
    path: (c) => `adoptions/${c.id}/care_updates`,
    media: "private",
    after: "adoptions",
    replayable: true,
  },
  "lead-assessment": {
    title: "核对私密线索处理",
    schema: "LeadAssessmentInput",
    path: (c) => {
      if (c.kind !== "leads" || c.id <= 0n) throw new Error("请从原线索进入");
      return `leads/${c.id}/assessment`;
    },
    omit: ["assessment"],
    initial: (c) => ({ assessment: c.params.get("assessment") ?? "" }),
    after: "leads",
    afterView: "reviewed",
    replayable: true,
  },
  "lead-contact": {
    title: "核对站内联系",
    schema: "LeadContactInput",
    path: (c) => {
      if (c.kind !== "leads" || c.id <= 0n) throw new Error("请从原线索进入");
      return `leads/${c.id}/contact`;
    },
    after: "conversations",
    replayable: true,
  },
  lead: {
    title: "私密目击线索",
    schema: "LeadSubmissionInput",
    path: route("lead-submissions"),
    media: "private",
    after: "leads",
    afterView: "submitted",
    replayable: true,
  },
  claim: {
    title: "私密认领材料",
    schema: "ClaimInput",
    path: route("claims"),
    media: "private",
    after: "claims",
    replayable: true,
  },
  review: {
    title: "核实、举报与原结论申诉",
    schema: "ReviewInput",
    path: () => "reviews",
    media: "private",
    initial: (c) => ({
      type: c.params.get("type") ?? "report",
      target_kind: c.kind,
      target_id: c.id,
      appeal_of: integer(c.params.get("appeal")),
    }),
    after: "reviews",
    replayable: true,
  },
  "review-supplement": {
    title: "原事项补充",
    schema: "ReviewSupplement",
    path: route("supplement"),
    media: "private",
    after: "reviews",
  },
  introduction: {
    title: "授权介绍协作",
    schema: "IntroductionInput",
    path: route("introduction"),
    media: "public",
    initial: (c) => ({ bio: c.record?.data.bio ?? "" }),
    after: "pets",
    replayable: true,
  },
  feedback: {
    title: "逐项到访反馈",
    schema: "Feedback",
    path: () => "visit_feedback",
    initial: (c) => ({
      appointment_id: c.id,
      appointment_version: c.record?.data.effective_version ?? 0n,
      items: {
        identity: "unknown",
        pet: "unknown",
        reception: "unknown",
        place: "unknown",
        fees: "unknown",
      },
      note: "",
    }),
  },
  settings: {
    title: "通知设置",
    schema: "SettingsInput",
    path: () => "preferences",
    method: "PUT",
    replayable: true,
    omit: ["expressions", "draft", "key"],
    initial: (c) => ({ preference: c.record?.data ?? {} }),
  },
  nearby: {
    title: "附近走失提醒",
    schema: "SubscriptionInput",
    path: () => "subscriptions",
    method: "PUT",
    initial: (c) => ({
      subscription: c.record?.data ?? {
        city: "",
        latitude: 0,
        longitude: 0,
        radius_km: 10,
        frequency: "daily",
        enabled: false,
      },
    }),
  },
  data: {
    title: "资料删除、正式注销与事项协助",
    schema: "DataRequestInput",
    path: () => "data-requests",
    scope: (_c, b) =>
      b.type === "matters_assistance" ? null : "data-requests/scope",
  },
  safety: {
    title: "安全与隐私求助",
    schema: "RightsInput",
    path: () => "safety/tickets",
    media: "safety",
    guest: true,
    initial: (c) => ({
      type:
        c.params.get("type") ??
        (c.params.get("related") ? "supplement" : "safety"),
      target_kind: c.kind,
      target_id: c.id,
      related_case_id: integer(c.params.get("related")),
      version: integer(c.params.get("version")),
    }),
    scope: (_c, b) =>
      ["rights_notice", "counterstatement"].includes(String(b.type))
        ? "safety/scope"
        : null,
  },
  guardian: {
    title: "核对监护授权的指定范围",
    schema: "GuardianDecision",
    path: (c) => `guardians/${c.id}/decision`,
    initial: () => ({ consent: false }),
    scope: (c, b) => (b.consent ? `guardians/${c.id}/scope` : null),
    after: "guardians",
  },
  "adoption-action": {
    ...actionWorkflow("AdoptionAction"),
    initial: (c) => {
      if (c.params.get("action") !== "propose_trial" || !c.record) return {};
      const contract = currentAdoptionContract(c.record),
        terms = contract?.terms as Record<string, unknown> | undefined;
      return {
        note: terms?.responsibilities ?? "",
        trial_until: terms?.trial_until ?? null,
      };
    },
  },
  "appointment-action": {
    ...actionWorkflow("AppointmentAction"),
    omit: ["terms"],
  },
  "claim-action": { ...actionWorkflow("StateInput"), media: "private" },
  "group-action": {
    ...actionWorkflow("GroupAction"),
    initial: (c) => ({
      notice: c.record?.data.notice ?? "",
      discoverable: c.record?.data.discoverable ?? false,
      city: c.record?.data.city ?? "",
      summary: c.record?.data.summary ?? "",
    }),
  },
  "invitation-action": {
    ...actionWorkflow("StateInput"),
    title: "核对邀请与协作",
  },
  "state-action": { ...actionWorkflow("StateInput"), replayable: false },
  "friend-action": {
    ...actionWorkflow("FriendInput"),
    path: (c) => {
      const action = c.params.get("action") ?? "";
      if (!actionGroups.friendships.includes(action))
        throw new Error("无效好友操作");
      return `friendships/${action}`;
    },
    initial: (c) => ({
      user_id:
        c.record?.data.requester_id === integer(c.params.get("owner"))
          ? c.record?.data.recipient_id
          : c.record?.data.requester_id,
    }),
  },
  "return-action": {
    title: "核对指定接回方案",
    schema: "ReturnInput",
    path: (c) => {
      const action = c.params.get("action");
      if (!["accept", "decline", "withdraw", "handover"].includes(action ?? ""))
        throw new Error("无效接回动作");
      return `adoptions/${c.id}/returns/${action}`;
    },
    omit: ["terms", "revision"],
    initial: (c) => ({ revision: integer(c.params.get("revision")) }),
    replayable: true,
    after: "adoptions",
  },
  "contract-decline": {
    title: "拒绝指定协议提案",
    schema: "ContractInput",
    path: route("contracts/decline"),
    omit: ["terms", "revision"],
    initial: (c) => ({ revision: integer(c.params.get("revision")) }),
    replayable: true,
    after: "adoptions",
  },
  delete: {
    title: "删除指定记录",
    schema: "StateInput",
    path: (c) => {
      if (
        ![
          "entries",
          "pets",
          "offers",
          "entry_comments",
          "pet_comments",
        ].includes(c.kind)
      )
        throw new Error("此记录不能删除");
      return `${c.kind}/${c.id}`;
    },
    method: "DELETE",
  },
};
export function workflowURL(
  flow: string,
  kind = "",
  id: unknown = 0n,
  extra: Record<string, string> = {},
) {
  return (
    "/edit?" +
    new URLSearchParams({ flow, kind, id: String(id), ...extra }).toString()
  );
}
export const stateLabels: Record<string, string> = {
  published: "已发布",
  closed: "已结束",
  committed: "已提交",
  superseded: "已被后续提案替代",
  verified_pending_consent: "等待指定监护人确认",
  active: "有效",
  pending: "待确认",
  accepted: "已接受",
  left: "已退出协作",
  declined: "已拒绝",
  withdrawn: "已撤回",
  removed: "已解除",
  blocked: "已拉黑",
  expired: "已过期",
  revoked: "已撤销",
  dissolved: "已解散",
  submitted: "已提交",
  needs_info: "待补充",
  supplemented: "已补充",
  approved: "已同意",
  rejected: "未通过",
  trial_proposed: "试养提案待确认",
  trial_active: "试养中",
  handover_pending: "等待另一方确认交接",
  completed: "已完成",
  return_requested: "退养协商中",
  return_agreed: "接回方案已同意",
  returned: "双方接回交接完成",
  confirmed: "双方确认有效",
  cancelled: "已取消",
  safety_hold: "安全暂停",
  matched: "材料匹配待交接",
  disputed: "存在争议",
  processing: "处理中",
  held: "保全中",
  partially_held: "部分保全",
  failed: "处理失败",
  effective: "当前有效版本",
  replaced: "已被新版本替代",
};
export const stateLabel = (state: string) => stateLabels[state] ?? "进度待核对";
