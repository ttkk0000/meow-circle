package com.ttkk0000.meowcircle.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Regression source only; no tests, servers or real network are executed. */
class InboxReadAllTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private class Vault(var value: String? = """{"schema":1,"session":{"user":{"id":2,"username":"cat","nickname":"猫友"},"token":"fixture-access","refresh_token":"fixture-refresh","expires_at":"2026-11-20T00:00:00Z"}}""") : MobileSecretStorage {
        override fun read() = value
        override fun write(value: String?) { this.value = value }
    }

    @Test fun hideKeepsUnknownIntentAndKnownFailuresRemainRetryable() {
        val snapshot = InboxReadSnapshot(7, 1, 12, 3, "2026-10-23T12:10:00Z")
        val unknown = InboxReadAllState(snapshot = snapshot, error = "unknown", resultUnknown = true,
            prepareRequestId = "original-prepare", confirmRequestId = "original-confirm", visible = true)
        val hidden = unknown.hidden()
        assertFalse(hidden.visible)
        assertEquals(snapshot, hidden.snapshot)
        assertEquals("original-confirm", hidden.confirmRequestId)
        assertTrue(hidden.resultUnknown)
        assertTrue(hidden.canRetry)
        assertTrue(unknown.copy(resultUnknown = false, error = "network refused").canRetry)
        assertEquals(unknown.copy(submitting = true), unknown.copy(submitting = true).hidden())
    }

    @Test fun reminderCountDoesNotDoubleCountRequestsPendingTasksOrChatPushCopies() {
        val summary = InboxSummary(direct = mapOf("7" to 3), groups = mapOf("8" to 2),
            mutedGroups = mapOf("8" to 2), messageRequests = 3,
            pending = mapOf("message_requests" to 1, "appointments" to 4),
            notifications = mapOf("direct_messages" to 3, "groups" to 2, "system" to 1))
        // Keep the currently implemented reminder policy. Muted data is separate
        // for confirmation until the proposed badge policy is confirmed.
        assertEquals(6, summary.reminderTotal)
    }

    @Test fun unknownConfirmationSurvivesRestartAndRetainsOriginalSnapshotBodyAndKey() = runTest {
        val vault = Vault()
        val input = InboxReadAllInput(9_007_199_254_740_993L, 1, "read-all-original-fixture")
        var originalBody = ""
        val first = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine { request ->
            assertEquals("/api/v2/inbox/read-all", request.url.encodedPath)
            assertEquals(input.clientRequestId, request.headers["Idempotency-Key"])
            originalBody = assertIs<TextContent>(request.body).text
            respond("""{"ok":false,"error":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headers)
        }))
        try { assertFailsWith<OperationResultUnknown> { first.confirmInboxReadAll(input) } }
        finally { first.close() }
        var writes = 0
        val restored = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine { request ->
            if (request.url.encodedPath.startsWith("/api/v2/operations/")) {
                respond("""{"ok":true,"data":{"client_request_id":"${input.clientRequestId}","command":"inbox.read_all","object_id":9007199254740993,"result_kind":"receipts","result_id":9007199254740993,"result_version":2,"state":"committed"}}""", headers = headers)
            } else {
                writes++
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/api/v2/inbox/read-all", request.url.encodedPath)
                assertEquals(input.clientRequestId, request.headers["Idempotency-Key"])
                assertEquals(originalBody, assertIs<TextContent>(request.body).text)
                respond("""{"ok":true,"data":{"snapshot_id":9007199254740993,"applied_count":2,"skipped_count":1}}""", headers = headers)
            }
        }))
        try {
            assertEquals(input.clientRequestId, restored.recoverableCommands().single().clientRequestId)
            assertEquals(input, restored.pendingInboxReadAll()?.confirmation)
            assertEquals("committed", restored.retryOriginal(input.clientRequestId).state)
            val result = restored.confirmInboxReadAll(input)
            assertEquals(input.snapshotId, result.snapshotId)
            assertEquals(2, result.appliedCount)
            assertEquals(1, result.skippedCount)
            assertEquals(2, writes) // original retry, then ACL-checked result replay
            assertTrue(restored.recoverableCommands().isEmpty())
        } finally { restored.close() }
    }

    @Test fun refusalAfterUnknownReplayCannotDiscardTheOriginalIntent() = runTest {
        val vault = Vault()
        val input = InboxReadAllInput(7, 1, "uncertain-replay-fixture")
        val first = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine {
            respond("""{"ok":false,"error":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headers)
        }))
        try { assertFailsWith<OperationResultUnknown> { first.confirmInboxReadAll(input) } }
        finally { first.close() }
        val restored = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine {
            respond("""{"ok":false,"error":"expired","error_code":"inbox_snapshot_expired"}""", HttpStatusCode.Forbidden, headers)
        }))
        try {
            assertFailsWith<OperationResultUnknown> { restored.retryOriginal(input.clientRequestId) }
            assertEquals("result_unknown", restored.commandStatus(input.clientRequestId)?.phase)
            assertEquals(input, restored.pendingInboxReadAll()?.confirmation)
        } finally { restored.close() }
    }

    @Test fun snapshotExpiryHasDedicatedErrorAndIsNotRetriedAsNewScope() = runTest {
        var writes = 0
        val api = MobileApi("https://example.test", Vault(), "fixture", HttpClient(MockEngine {
            writes++
            respond("""{"ok":false,"error":"expired","error_code":"inbox_snapshot_expired"}""", HttpStatusCode.Forbidden, headers)
        }))
        try {
            val error = assertFailsWith<MobileApiException> {
                api.confirmInboxReadAll(InboxReadAllInput(7, 1, "expired-read-all-fixture"))
            }
            assertEquals("inbox_snapshot_expired", error.code)
            assertTrue(error.mobileMessage().contains("重新获取"))
            assertEquals(1, writes)
            assertEquals("rejected", api.commandStatus("expired-read-all-fixture")?.phase)
        } finally { api.close() }
    }
}
