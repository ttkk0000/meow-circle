package com.ttkk0000.meowcircle.client

import kotlin.test.*
import kotlin.time.Instant

/** 仅回归源码，未执行。 */
class InvitationStatusTest {
    private val now = Instant.parse("2026-10-27T00:00:00Z")
    private fun invitation(state: String = "pending", scope: String = "caretaker", expires: String = "2026-10-28T00:00:00Z") =
        MobileRecord(8,1,3,state,InvitationBody(scope,5,2,expires))

    @Test fun expiredRecipientCanDeclineButCannotAcceptAndStrangerHasNoActions() {
        val expired = invitation(expires = "2026-10-26T00:00:00Z")
        assertEquals(listOf("decline"),availableInvitationActions(expired,2,now).map {it.first})
        assertEquals(listOf("accept","decline"),availableInvitationActions(invitation(),2,now).map {it.first})
        assertTrue(availableInvitationActions(invitation(),3,now).isEmpty())
        assertEquals(listOf("revoke"),availableInvitationActions(expired,1,now).map {it.first})
    }
    @Test fun acceptedCareDoesNotExpireAtAcceptanceDeadlineAndLeaveIsScoped() {
        val accepted = invitation("accepted",expires = "2026-10-26T00:00:00Z")
        assertEquals(listOf("leave"),availableInvitationActions(accepted,2,now).map {it.first})
        assertEquals(listOf("revoke"),availableInvitationActions(accepted,1,now).map {it.first})
        assertTrue(availableInvitationActions(invitation("left"),2,now).isEmpty())
        assertTrue(availableInvitationActions(invitation("accepted","group_request"),1,now).isEmpty())
        assertTrue(availableInvitationActions(invitation("accepted","group_member"),2,now).isEmpty())
    }
    @Test fun malformedDeadlineAndUnknownScopeCannotBeAccepted() {
        assertEquals(listOf("decline"),availableInvitationActions(invitation(expires = "invalid"),2,now).map {it.first})
        assertEquals(listOf("decline"),availableInvitationActions(invitation(scope = "unknown"),2,now).map {it.first})
    }
}
