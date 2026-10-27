package com.ttkk0000.meowcircle.client

fun AppointmentBody.safetyProposal(): AppointmentVersion? = versions.firstOrNull {
    it.number == pendingVersion && it.status == "pending" && safetyHoldVersion > 0 &&
        it.safetyHoldVersion == safetyHoldVersion && it.resolutionNote.isNotBlank()
}
fun MobileRecord<AppointmentBody>.canConfirmAppointment(actor: Long?): Boolean {
    if(actor == null || actor !in listOf(ownerId,data.peerId) || state in listOf("cancelled","expired")) return false
    val pending = data.versions.firstOrNull {it.number == data.pendingVersion && it.status == "pending"} ?: return false
    if(actor in pending.confirmations) return false
    return state != "safety_hold" || pending == data.safetyProposal()
}
fun MobileRecord<AppointmentBody>.safetyResolved(): Boolean {
    val effective = data.versions.firstOrNull {it.number == data.effectiveVersion && it.status == "effective"} ?: return false
    return state == "confirmed" && data.safetyHoldVersion > 0 && effective.safetyHoldVersion == data.safetyHoldVersion &&
        effective.resolutionNote.isNotBlank() && listOf(ownerId,data.peerId).all {it in effective.confirmations && !effective.safetyChecks[it.toString()].isNullOrBlank()}
}
