package com.ttkk0000.meowcircle.client

import kotlin.test.*

/** 仅回归源码，未执行。 */
class AdoptionStatusTest {
    private val terms=ContractTerms("武汉","cat","adoption","共同核对照护责任","已披露健康事项","协商安全接回",0,emptyList(),"不收款",trialUntil="2027-01-01T00:00:00Z")
    private val revision=ContractRevision(1,terms,"fixture-hash",1,listOf(1,2),"effective","2026-10-27T00:00:00Z")
    private fun record(state:String)=MobileRecord(8,1,3,state,AdoptionBody(5,2,1,"本次申请",contracts=listOf(revision),effectiveContract=1,trialConfirmations=listOf(1)))
    @Test fun participantActionsUseCurrentContractAndNeverReusePendingContract() {
        val row=record("trial_proposed")
        assertTrue(availableAdoptionActions(row,2).any {it.first=="confirm_trial"})
        assertFalse(availableAdoptionActions(row,1).any {it.first=="confirm_trial"})
        assertTrue(availableAdoptionActions(row,3).isEmpty())
        val changed=row.copy(data=row.data.copy(contracts=listOf(revision,revision.copy(number=2,status="pending"))))
        assertNull(changed.data.currentContract())
        assertFalse(availableAdoptionActions(changed,2).any {it.first=="confirm_trial"})
        assertTrue(availableAdoptionActions(changed,2).any {it.first=="decline_trial"})
        assertEquals(terms.responsibilities,row.data.currentContract()?.terms?.responsibilities)
    }
    @Test fun careEntryNeedsActualStageAndApplicantProgress() {
        val row=record("trial_active")
        assertFalse(row.data.careStageAvailable(row.state))
        val active=row.copy(data=row.data.copy(trialConfirmations=listOf(1,2)))
        assertTrue(active.data.careStageAvailable("trial_active"))
        assertTrue(active.data.careStageAvailable("return_requested"))
        assertFalse(active.data.careStageAvailable("returned"))
        assertEquals("等待另一方确认试养",adoptionProgressTitle(record("trial_proposed"),1))
        assertEquals("确认收到的试养安排",adoptionProgressTitle(record("trial_proposed"),2))
    }
}
