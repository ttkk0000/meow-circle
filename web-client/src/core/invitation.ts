import type { RecordData } from "./api";
import { integer } from "./wire";

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
