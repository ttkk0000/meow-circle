import type { RecordData } from "./api";
import { integer } from "./wire";

export type AppointmentVersion = Record<string, unknown>;
export function appointmentVersions(row: RecordData): AppointmentVersion[] {
  return Array.isArray(row.data.versions) ? row.data.versions : [];
}
export function appointmentPending(row: RecordData) {
  return appointmentVersions(row).find(
    (v) =>
      integer(v.number) === integer(row.data.pending_version) &&
      v.status === "pending",
  );
}
export function appointmentEffective(row: RecordData) {
  return appointmentVersions(row).find(
    (v) =>
      integer(v.number) === integer(row.data.effective_version) &&
      v.status === "effective",
  );
}
export function safetyProposal(row: RecordData) {
  const pending = appointmentPending(row),
    hold = integer(row.data.safety_hold_version);
  return pending &&
    hold > 0n &&
    integer(pending.safety_hold_version) === hold &&
    String(pending.resolution_note ?? "").trim()
    ? pending
    : undefined;
}
export function canConfirmAppointment(row: RecordData, actor: bigint) {
  if (
    actor <= 0n ||
    ![row.owner_id, integer(row.data.peer_id)].includes(actor) ||
    ["cancelled", "expired"].includes(row.state)
  )
    return false;
  const pending = appointmentPending(row);
  return (
    !!pending &&
    !(
      Array.isArray(pending.confirmations) &&
      pending.confirmations.some((v) => integer(v) === actor)
    ) &&
    (row.state !== "safety_hold" || pending === safetyProposal(row))
  );
}
export function appointmentSafetyResolved(row: RecordData) {
  const effective = appointmentEffective(row),
    hold = integer(row.data.safety_hold_version);
  const checks = effective?.safety_checks as Record<string, string> | undefined;
  return (
    row.state === "confirmed" &&
    hold > 0n &&
    integer(effective?.safety_hold_version) === hold &&
    !!String(effective?.resolution_note ?? "").trim() &&
    [row.owner_id, integer(row.data.peer_id)].every(
      (actor) =>
        Array.isArray(effective?.confirmations) &&
        effective.confirmations.some((v) => integer(v) === actor) &&
        !!checks?.[String(actor)]?.trim(),
    )
  );
}
