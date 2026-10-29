import type { RecordData } from "./api";
import { integer } from "./wire";

export function invitationReview(row: RecordData, action: string) {
  const scope = String(row.data.scope ?? "");
  if (action === "leave")
    return scope === "caretaker"
      ? {
          title: "退出本次照护协作",
          detail: "退出后，你将不能再通过本次授权查看或记录私密照护近况。已有记录保留，原领养、试养和交接事项继续；退出协作不代表完成交接或接回。",
        }
      : {
          title: "退出本次介绍协作",
          detail: "退出后，你将不能再通过本次授权维护该宠物的介绍与照片。已有记录和原领养事项保留，宠物归属不变；公开资料仍按原可见范围展示。",
        };
  if (action === "revoke")
    return {
      title: row.state === "pending" ? "撤销本次邀请" : scope === "caretaker" ? "撤销本次照护授权" : scope === "adoption_editor" ? "结束本次介绍协作" : "撤销本次邀请或授权",
      detail: "仅撤销这份邀请或已接受授权。已有记录及原事项保留，不取消领养、试养、预约或交接。",
    };
  return {
    title: action === "accept" ? "接受本次邀请" : action === "decline" ? "拒绝本次邀请" : "核对本次邀请",
    detail:
      action === "accept" && scope === "caretaker"
        ? "仅获得本次事项的私密照护近况权限；申请材料、协议与完整宠物档案不会开放，也不能代替双方确认试养、交接或接回。"
        : action === "accept" && scope === "adoption_editor"
          ? "仅维护该宠物的领养介绍与照片，不获得领养决定、私密申请材料、协议或宠物归属权限。"
          : "仅处理这份邀请；接受截止只限制尚未接受的邀请，当前权限仍需重新核对。",
  };
}

// 接受截止只限制 pending 邀请；已接受授权仍由服务端按当前资源逐次核验。
export function invitationActions(
  row: RecordData,
  actor: bigint,
  now = Date.now(),
): string[] {
  if (actor <= 0n) return [];
  const scope = String(row.data.scope ?? "");
  const recipient = integer(row.data.recipient_id) === actor;
  if (row.state === "pending") {
    if (recipient) {
      const future = Date.parse(String(row.data.expires_at)) > now;
      return future &&
        [
          "adoption_editor",
          "caretaker",
          "group_member",
          "group_request",
        ].includes(scope)
        ? ["accept", "decline"]
        : ["decline"];
    }
    return row.owner_id === actor ? ["revoke"] : [];
  }
  if (row.state === "accepted") {
    if (recipient && ["adoption_editor", "caretaker"].includes(scope))
      return ["leave"];
    if (
      row.owner_id === actor &&
      ["adoption_editor", "caretaker", "group_member"].includes(scope)
    )
      return ["revoke"];
  }
  return [];
}
