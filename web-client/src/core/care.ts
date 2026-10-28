import type { DTOCareContext } from "./contract";

export type CareContext = Required<DTOCareContext>;
export function careReadOnlyReason(context: CareContext): string {
  if (context.reason === "interaction_required")
    return "当前互动资格未满足，已授权的历史近况仍可查阅。";
  if (context.reason === "care_recipient_required")
    return "由接收照护方或其授权协作者补充近况。";
  return "当前阶段已结束近况提交，历史记录仍可查阅。";
}
