package com.ttkk0000.meowcircle.client

import kotlin.test.*

/** 仅回归源码；未执行编译或测试。 */
class LeadSubmissionStatusTest {
    @Test fun receivedPageRequiresOriginalOwnerAndMinimumCommittedVersion() {
        val lead = MobileRecord(11,2,3,"submitted",LeadBody("仍需核对的原始目击"),referenceId = 5)
        assertTrue(leadSubmissionRecorded(lead,2,3))
        assertFalse(leadSubmissionRecorded(lead,1,3))
        assertFalse(leadSubmissionRecorded(lead,null,3))
        assertFalse(leadSubmissionRecorded(lead,2,0))
        assertFalse(leadSubmissionRecorded(lead.copy(version = 2),2,3))
        assertFalse(leadSubmissionRecorded(lead.copy(state = "removed"),2,3))
    }
    @Test fun unsupportedOrOverflowResultMarkerDoesNotShowReceivedPage() {
        assertEquals(3L,leadSubmissionMinimumVersion("submitted-3"))
        for(kind in listOf("submitted","submitted-0","submitted--1","submitted-9223372036854775808","reviewed-3"))
            assertNull(leadSubmissionMinimumVersion(kind))
    }
}
