package com.ttkk0000.meowcircle.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 只写回归源码；执行需用户另行授权。 */
class TaskLinksTest {
    private val trusted = "https://mnd.example.invalid"

    @Test fun sameHttpsOriginAcceptsCanonical64BitIdsAndDefaultPort() {
        val target = TaskLinkTarget("pets",Long.MAX_VALUE)
        assertEquals(target,parseTaskLink("$trusted/share/pets/${Long.MAX_VALUE}",trusted))
        assertEquals(target,parseTaskLink("https://mnd.example.invalid:443/share/pets/${Long.MAX_VALUE}",trusted))
    }
    @Test fun doesNotAcceptAnotherHostUserInfoOrNonHttpsShare() {
        listOf("https://other.example.invalid/share/pets/1","https://mnd.example.invalid@evil.invalid/share/pets/1",
            "https://mnd.example.invalid.evil.invalid/share/pets/1","http://mnd.example.invalid/share/pets/1",
            "https://mnd.example.invalid:8443/share/pets/1").forEach { assertNull(parseTaskLink(it,trusted)) }
    }
    @Test fun rejectsActionsQueriesFragmentsEscapesAndAmbiguousIds() {
        listOf("mnd://pets/0","mnd://pets/01","mnd://pets/+1","mnd://pets/-1","mnd://pets/9223372036854775808",
            "mnd://pets/%31","mnd://pets/1?token=secret","mnd://pets/1#confirm","mnd://pets/1/delete",
            "mnd://pets:443/1","mnd://pets//1","mnd://pets/1/","mnd://accounts/1").forEach { assertNull(parseTaskLink(it,trusted)) }
    }
    @Test fun privateTargetsOnlyUseTheInternalScheme() {
        listOf("adoptions","appointments","claims","reviews","groups","conversations").forEach { kind ->
            assertNull(parseTaskLink("$trusted/share/$kind/1",trusted))
            assertEquals(TaskLinkTarget(kind,1),parseTaskLink("mnd://$kind/1",trusted))
        }
    }
}
