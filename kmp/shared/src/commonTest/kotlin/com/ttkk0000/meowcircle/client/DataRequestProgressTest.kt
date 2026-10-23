package com.ttkk0000.meowcircle.client

import kotlin.test.*

/** Regression source only; no Gradle or test execution. */
class DataRequestProgressTest {
    private val base = DataRequestStatus(9_007_199_254_740_993L, 1, "erase_fields", "completed", 0, 0, "not_required", progressVersion = 1)

    @Test fun onlyFullSelectedCompletionIsFinal() {
        assertTrue(base.progress().isFinal)
        for (status in listOf(
            base.copy(pending = 1), base.copy(held = 1), base.copy(pendingFields = listOf("bio")),
            base.copy(pending = 1, physicalPending = 1), base.copy(pending = 1, providerPending = 1),
            base.copy(externalState = "pending_inventory"), base.copy(state = "unknown"), base.copy(erased = -1), base.copy(progressVersion = 0),
        )) {
            assertFalse(status.progress().isFinal)
            assertEquals(DataRequestPhase.NeedsReconciliation, status.progress().phase)
        }
    }

    @Test fun overdueHoldStaysHeldAndPhysicalQueueStaysPending() {
        val held = base.copy(state = "partially_held", held = 1, reviewRequired = true, nextReviewAt = "2026-10-23T00:00:00Z").progress()
        assertEquals(DataRequestPhase.Held, held.phase)
        assertTrue(held.reviewRequired)
        assertFalse(held.isFinal)
        val physical = base.copy(state = "processing", pending = 1, physicalPending = 1).progress()
        assertEquals(DataRequestPhase.Processing, physical.phase)
        assertFalse(physical.isFinal)
        assertEquals(DataRequestPhase.Assistance, base.copy(state = "needs_assistance").progress().phase)
        val copies = base.copy(restrictedCopies = 1).progress()
        assertEquals(DataRequestPhase.RetainedCopies, copies.phase)
        assertFalse(copies.isFinal)
    }
}
