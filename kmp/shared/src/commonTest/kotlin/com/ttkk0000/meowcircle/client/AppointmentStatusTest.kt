package com.ttkk0000.meowcircle.client

import kotlin.test.*

/** 仅回归源码，未运行。 */
class AppointmentStatusTest {
    private val terms = AppointmentTerms("2027-01-01T10:00:00Z","公共会面点","发布人","小满",0,0,"无费用","不收款")
    private fun held() = MobileRecord(8,2,4,"safety_hold",AppointmentBody("visit",5,1,1,2,listOf(
        AppointmentVersion(1,terms,2,listOf(1,2),"effective","2026-10-27T00:00:00Z"),
        AppointmentVersion(2,terms,2,listOf(2),"pending","2026-10-27T00:00:00Z",3,"已核对本次更正",mapOf("2" to "本人的具体核对"))
    ),safetyHoldVersion = 3,safetyReason = "原地点不可用"))
    @Test fun holdOnlyAllowsTheOtherPartyToReviewMatchingSafetyProposal() {
        val row=held()
        assertTrue(row.canConfirmAppointment(1))
        assertFalse(row.canConfirmAppointment(2))
        assertFalse(row.canConfirmAppointment(3))
        assertFalse(row.canConfirmAppointment(null))
        assertFalse(row.copy(data=row.data.copy(safetyHoldVersion=9)).canConfirmAppointment(1))
        assertFalse(row.safetyResolved())
    }
    @Test fun resolvedNoticeRequiresCurrentEffectiveVersionAndBothSpecificReviews() {
        val row=held()
        val resolved=row.copy(state="confirmed",data=row.data.copy(effectiveVersion=2,pendingVersion=0,versions=row.data.versions.map {if(it.number==2L) it.copy(status="effective",confirmations=listOf(1,2),safetyChecks=mapOf("1" to "本人核对","2" to "本人核对")) else it.copy(status="replaced")}))
        assertTrue(resolved.safetyResolved())
        assertFalse(resolved.copy(data=resolved.data.copy(safetyHoldVersion=6)).safetyResolved())
        assertFalse(resolved.copy(state="safety_hold").safetyResolved())
        assertFalse(resolved.copy(data=resolved.data.copy(versions=resolved.data.versions.map {it.copy(safetyChecks=mapOf("2" to "本人核对"))})).safetyResolved())
    }
}
