package com.ttkk0000.meowcircle.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Regression source only; no Gradle or test execution is implied. */
class OperationRecoveryTest {
    private class Vault(var value: String? = null) : MobileSecretStorage {
        override fun read() = value
        override fun write(value: String?) { this.value = value }
    }
    private val headers = headersOf(HttpHeaders.ContentType,"application/json")

    @Test
    fun publicationIsPreparedDurablyBeforeAnyNetworkWrite() = runTest {
        val vault = Vault()
        var writes = 0
        val engine = MockEngine { request -> when (request.url.encodedPath) {
            "/api/v2/auth/password" -> respond("""{"ok":true,"data":{"user":{"id":7,"username":"cat","nickname":"猫友"},"token":"access","refresh_token":"refresh","expires_at":"2026-11-01T00:00:00Z"}}""", headers = headers)
            "/api/v2/entries" -> {
                writes++
                respond("""{"ok":true,"data":{"id":9007199254740993,"owner_id":7,"version":1,"state":"published","data":{"text":"原始草稿"}}}""", headers = headers)
            }
            else -> error("Unexpected request")
        } }
        val api = MobileApi("https://mnd.example", vault, "test", HttpClient(engine))
        api.password("cat", "password")
        val input = EntryInput(EntryBody("原始草稿"), clientRequestId = "prepared-publication",
            declaration = PublicationDeclaration("manifest", "original", true))
        val prepared = api.preparePublication(input)
        assertEquals(input, prepared)
        assertEquals("prepared", api.commandStatus(input.clientRequestId)?.phase)
        assertEquals(0, writes)
        val restored = MobileApi("https://mnd.example", vault, "test", HttpClient(engine))
        assertEquals(7L, restored.user.value?.id)
        assertEquals("prepared", restored.commandStatus(input.clientRequestId)?.phase)
        assertEquals(9007199254740993, restored.publish(prepared).id)
        assertEquals(1, writes)
        api.close(); restored.close()
    }

    @Test
    fun lostPublishResponseSurvivesRestartAndIsNotReposted() = runTest {
        val vault = Vault()
        var writes = 0
        val engine = MockEngine { request -> when (request.url.encodedPath) {
            "/api/v2/auth/password" -> respond("""{"ok":true,"data":{"user":{"id":7,"username":"cat","nickname":"猫友"},"token":"access","refresh_token":"refresh","expires_at":"2026-11-01T00:00:00Z"}}""",headers = headers)
            "/api/v2/entries" -> { writes++; throw IllegalStateException("response lost") }
            "/api/v2/operations/original-request" -> respond("""{"ok":true,"data":{"client_request_id":"original-request","command":"entry.save","object_id":0,"result_kind":"entries","result_id":9007199254740993,"result_version":1,"state":"committed"}}""",headers = headers)
            else -> error("unexpected path ${request.url}")
        } }
        val input = EntryInput(EntryBody("小满"),clientRequestId = "original-request",declaration = PublicationDeclaration("server-manifest","original",true))
        MobileApi("https://example.test",vault,"test",HttpClient(engine)).let { api ->
            try { api.password("cat","unused");assertFailsWith<OperationResultUnknown> { api.publish(input) };assertEquals(1,writes) }
            finally { api.close() }
        }
        assertTrue(vault.value.orEmpty().contains("result_unknown"))
        val restored = MobileApi("https://example.test",vault,"test",HttpClient(MockEngine { request ->
            if (request.url.encodedPath.startsWith("/api/v2/operations/")) respond("""{"ok":true,"data":{"client_request_id":"original-request","command":"entry.save","object_id":0,"result_kind":"entries","result_id":9007199254740993,"result_version":1,"state":"committed"}}""",headers = headers)
            else { writes++; error("must reconcile before reposting") }
        }))
        try {
            assertFailsWith<OperationResultUnknown> { restored.publish(input) }
            assertEquals(1,writes)
            assertEquals(9_007_199_254_740_993L,restored.reconcileOperation("original-request").resultId)
            assertTrue(restored.recoverableCommands().isEmpty())
        } finally { restored.close() }
    }

    @Test
    fun notFoundDoesNotClearUnknownOutcome() = runTest {
        val vault = Vault("""{"schema":1,"session":{"user":{"id":1,"username":"cat","nickname":"猫友"},"token":"access","refresh_token":"refresh","expires_at":"2026-11-01T00:00:00Z"},"commands":[{"owner":1,"id":"still-unknown","path":"entries","body":"{}","phase":"result_unknown"}]}""")
        val api = MobileApi("https://example.test",vault,"test",HttpClient(MockEngine {
            respond("""{"ok":false,"error":"not found","error_code":"not_found"}""",HttpStatusCode.NotFound,headers)
        }))
        try { assertFailsWith<MobileApiException> { api.reconcileOperation("still-unknown") };assertEquals("result_unknown",api.recoverableCommands().single().phase) }
        finally { api.close() }
    }
}
