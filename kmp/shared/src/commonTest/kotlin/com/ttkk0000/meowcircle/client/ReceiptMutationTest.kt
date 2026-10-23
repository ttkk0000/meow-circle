package com.ttkk0000.meowcircle.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

/** Regression source only. No Gradle, test fixture or network has been run. */
class ReceiptMutationTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val session = """{"user":{"id":1,"username":"cat","nickname":"猫友"},"token":"fixture-access","refresh_token":"fixture-refresh","expires_at":"2026-11-20T00:00:00Z"}"""
    private inner class Vault(var value: String? = """{"schema":1,"session":$session}""") : MobileSecretStorage {
        override fun read() = value
        override fun write(value: String?) { this.value = value }
    }
    private fun receipt(key: String, kind: String = "preferences", id: Long = 7) =
        """{"ok":true,"data":{"client_request_id":"$key","command":"fixture.command","object_id":0,"result_kind":"$kind","result_id":$id,"result_version":1,"state":"committed"}}"""

    @Test fun putRetryRestoresOriginalMethodHeaderAndBody() = runTest {
        val vault = Vault()
        var originalKey = ""
        var originalBody = ""
        val first = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method)
            originalKey = requireNotNull(request.headers["Idempotency-Key"])
            originalBody = assertIs<TextContent>(request.body).text
            respond("""{"ok":false,"error":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headers)
        }))
        try { assertFailsWith<OperationResultUnknown> { first.savePreferences(0, PreferenceBody(frequency = "off")) } }
        finally { first.close() }
        var writes = 0
        val restored = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine { request ->
            if (request.url.encodedPath.startsWith("/api/v2/operations/")) respond(receipt(originalKey), headers = headers)
            else {
                writes++
                assertEquals(HttpMethod.Put, request.method)
                assertEquals(originalKey, request.headers["Idempotency-Key"])
                assertEquals(originalBody, assertIs<TextContent>(request.body).text)
                respond("""{"ok":true,"data":{"id":7,"owner_id":1,"version":1,"state":"active","data":{"frequency":"off"}}}""", headers = headers)
            }
        }))
        try {
            assertEquals("committed", restored.retryOriginal(originalKey).state)
            assertEquals(1, writes)
            assertTrue(restored.recoverableCommands().isEmpty())
        } finally { restored.close() }
    }

    @Test fun missingReceiptAfterSuccessfulWriteRemainsUnknown() = runTest {
        val vault = Vault()
        var writes = 0
        val api = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine { request ->
            if (request.url.encodedPath.startsWith("/api/v2/operations/"))
                respond("""{"ok":false,"error":"not found"}""", HttpStatusCode.NotFound, headers)
            else {
                writes++
                respond("""{"ok":true,"data":{"id":7,"owner_id":1,"version":1,"state":"active","data":{}}}""", headers = headers)
            }
        }))
        try {
            assertFailsWith<OperationResultUnknown> { api.react(7, true) }
            assertEquals(1, writes)
            assertEquals("result_unknown", api.recoverableCommands().single().phase)
        } finally { api.close() }
    }

    @Test fun deletionUsesActualReceiptWhenResponseHasNoRecordBody() = runTest {
        var key = ""
        val api = MobileApi("https://example.test", Vault(), "fixture", HttpClient(MockEngine { request ->
            if (request.url.encodedPath.startsWith("/api/v2/operations/")) respond(receipt(key,"entries",9_007_199_254_740_993L), headers = headers)
            else {
                assertEquals(HttpMethod.Delete, request.method)
                key = requireNotNull(request.headers["Idempotency-Key"])
                respond("""{"ok":true,"data":null}""", headers = headers)
            }
        }))
        try {
            api.removeResource("entries", 9_007_199_254_740_993L, 1)
            assertEquals(9_007_199_254_740_993L, api.commandStatus(key)?.receipt?.resultId)
            assertTrue(api.recoverableCommands().isEmpty())
        } finally { api.close() }
    }

    @Test fun existingVaultCommandsDefaultToPostWithoutNewHeader() = runTest {
        val vault = Vault("""{"schema":1,"session":$session,"commands":[{"owner":1,"id":"legacy-command-fixture","path":"entries","body":"{}","phase":"sent"}]}""")
        val api = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertNull(request.headers["Idempotency-Key"])
            respond("""{"ok":true,"data":{"id":7,"owner_id":1,"version":1,"state":"published","data":{}}}""", headers = headers)
        }))
        try { assertEquals("committed", api.retryOriginal("legacy-command-fixture").state) }
        finally { api.close() }
    }

    @Test fun closeCaseAddsTheRequiredRequestKey() = runTest {
        val api = MobileApi("https://example.test", Vault(), "fixture", HttpClient(MockEngine { request ->
            assertEquals("/api/v2/cases/7/close", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            val payload = kotlinx.serialization.json.Json.parseToJsonElement(assertIs<TextContent>(request.body).text).jsonObject
            assertTrue(payload.getValue("client_request_id").jsonPrimitive.content.length >= 8)
            respond("""{"ok":true,"data":{"id":7,"owner_id":1,"version":2,"state":"closed","data":{"type":"lost","title":"found","species":"cat","description":"safe","city":"武汉","area":"附近","latitude":30.5,"longitude":114.3,"occurred_at":"2026-10-22T09:00:00+08:00"}}}""", headers = headers)
        }))
        try { api.closeCase(7, VersionedAction(1, note = "found safely")); assertTrue(api.recoverableCommands().isEmpty()) }
        finally { api.close() }
    }

    @Test fun patchEditingRetainsItsMethodAfterUnknownResponse() = runTest {
        val vault = Vault()
        var writes = 0
        val api = MobileApi("https://example.test", vault, "fixture", HttpClient(MockEngine { request ->
            writes++
            assertEquals(HttpMethod.Patch, request.method)
            respond("""{"ok":false,"error":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headers)
        }))
        try {
            assertFailsWith<OperationResultUnknown> { api.editPet(7, PetInput(PetBody("小满","cat"), 1, clientRequestId = "patch-edit-fixture")) }
            assertEquals(1, writes)
            assertTrue(requireNotNull(vault.value).contains("\"method\":\"PATCH\""))
            assertEquals("patch-edit-fixture", api.recoverableCommands().single().clientRequestId)
        } finally { api.close() }
    }
}
