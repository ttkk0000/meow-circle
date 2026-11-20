package com.ttkk0000.meowcircle.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Regression source only; no Gradle or test execution is implied. */
class OperationRecoveryTest {
    private class Vault(var value: String? = null) : MobileSecretStorage {
        var writesUntilFailure: Int? = null
        override fun read() = value
        override fun write(value: String?) {
            writesUntilFailure?.let { remaining ->
                writesUntilFailure = if (remaining == 0) null else remaining - 1
                check(remaining > 0) { "injected vault write failure" }
            }
            this.value = value
        }
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

    private fun authSessionJson(userId: Long, token: String): String =
        """{"ok":true,"data":{"user":{"id":$userId,"username":"cat$userId","nickname":"猫友$userId"},"token":"$token","refresh_token":"refresh-$token","expires_at":"2026-11-20T00:00:00Z"}}"""

    private fun scopeJson(userId: Long): String =
        """{"ok":true,"data":{"purpose":"data_request.submit","resource_type":"accounts","resource_id":$userId,"resource_version":0,"scope_hash":"scope-hash","role":"owner"}}"""

    private fun grantJson(): String = """{"ok":true,"data":{"reauth_token":"grant-proof"}}"""

    private fun dataStatusJson(id: Long, type: String, state: String): String =
        """{"ok":true,"data":{"id":$id,"version":1,"type":"$type","state":"$state","pending":1,"held":0,"external_state":"pending_inventory","progress_version":1}}"""

    @Test
    fun closeAccountUnknownReconcileWritesReceiptIdAndEndsOriginalSession() = runTest {
        val vault = Vault()
        val key = "close_req_unknown_01"
        val queryToken = "capability_token_0123456789abcdef0123456789abcdef"
        var writes = 0
        var lookups = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v2/auth/password" -> respond(authSessionJson(7, "sess-1"), headers = headers)
                "/api/v2/data-requests/scope" -> respond(scopeJson(7), headers = headers)
                "/api/v2/auth/reauth" -> respond(grantJson(), headers = headers)
                "/api/v2/data-requests" -> {
                    writes++
                    respond("""{"ok":false,"error":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headers)
                }
                "/api/v2/data-requests/requests/$key" -> {
                    lookups++
                    assertEquals(queryToken, request.headers["X-Request-Access"])
                    assertNull(request.headers[HttpHeaders.Authorization])
                    respond(dataStatusJson(42L, "close_account", "processing"), headers = headers)
                }
                else -> error("unexpected path ${request.url.encodedPath}")
            }
        }
        val api = MobileApi("https://example.test", vault, "test", HttpClient(engine))
        val store = SensitiveActionStore(api, backgroundScope)
        try {
            api.password("cat7", "unused")
            val input = DataRequestInput("close_account", key, queryToken)
            store.prepare(SensitiveIntent.DataRequest(input))
            store.state.first { it.stage == SensitiveStage.Review }
            store.verify(password = "unused")
            store.state.first { it.stage == SensitiveStage.ReadyToSubmit }
            store.confirm(true)
            val unknown = store.state.first { it.stage == SensitiveStage.ResultUnknown }
            assertEquals(key, unknown.clientRequestId)
            assertEquals(7L, api.user.value?.id)
            assertEquals(0L, api.limitedRequestAccess().single { it.clientRequestId == key }.id)

            store.reconcile()
            val done = store.state.first { it.stage == SensitiveStage.Complete }
            assertEquals(1, writes)
            assertEquals(1, lookups)
            assertEquals(42L, done.resultId)
            assertEquals("data_requests", done.resultKind)
            assertEquals(key, done.clientRequestId)
            assertEquals(42L, api.limitedRequestAccess().single { it.clientRequestId == key }.id)
            assertNull(api.user.value)
            assertEquals(SessionEndKind.AccountRequest, api.sessionEnd?.kind)
            assertNull(api.rememberedIdentity)
        } finally {
            store.close()
            api.close()
        }
    }

    @Test
    fun closeAccountKeepsOriginalSessionBindingUntilReceiptAndSignOutAreSaved() = runTest {
        for (successfulWritesAfterSubmit in 0..1) {
            val vault = Vault()
            val key = "close_vault_failure_$successfulWritesAfterSubmit"
            val queryToken = "capability_token_0123456789abcdef0123456789abcdef"
            var submissions = 0
            val engine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/api/v2/auth/password" -> respond(authSessionJson(7, "sess-1"), headers = headers)
                    "/api/v2/data-requests" -> {
                        submissions++
                        vault.writesUntilFailure = successfulWritesAfterSubmit
                        respond("""{"ok":true,"data":{"id":42,"owner_id":7,"version":1,"state":"processing","data":{"type":"close_account","fields":[],"items":[],"holds":[],"external_state":"pending_inventory"}}}""", headers = headers)
                    }
                    "/api/v2/data-requests/requests/$key" -> respond(dataStatusJson(42, "close_account", "processing"), headers = headers)
                    else -> error("unexpected path ${request.url.encodedPath}")
                }
            }
            val api = MobileApi("https://example.test", vault, "test", HttpClient(engine))
            try {
                api.password("cat7", "unused")
                val input = DataRequestInput("close_account", key, queryToken)
                assertFailsWith<OperationResultUnknown> { api.submitDataRequestSecure(input) }
                assertEquals(if (successfulWritesAfterSubmit == 0) 0L else 42L, api.limitedRequestAccess().single().id)
                assertEquals(7L, api.user.value?.id)

                // Fail the sign-out write after a possible receipt write on the first lookup.
                vault.writesUntilFailure = 1 - successfulWritesAfterSubmit
                var published = false
                assertFailsWith<MobileStorageException> {
                    api.reconcileDataRequest(input, onBeforeEndSession = { published = true })
                }
                assertFalse(published)
                assertEquals(7L, api.user.value?.id)
                assertEquals(42L, api.limitedRequestAccess().single().id)

                val resolved = api.reconcileDataRequest(input, onBeforeEndSession = {
                    assertEquals(7L, api.user.value?.id)
                    published = true
                })
                assertTrue(resolved.endedOriginalSession)
                assertTrue(published)
                assertNull(api.user.value)
                assertEquals(SessionEndKind.AccountRequest, api.sessionEnd?.kind)
                assertEquals(1, submissions)
            } finally {
                api.close()
            }
        }
    }

    @Test
    fun concurrentSameUserLoginOrSwitchOrStaleCallbackDoesNotClearNewSessionOrCredentials() = runTest {
        val vault = Vault()
        val key = "close_req_concurrent_01"
        val queryToken = "capability_token_abcdef0123456789abcdef0123456789"
        var loginCount = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v2/auth/password" -> {
                    loginCount++
                    if (loginCount <= 2) respond(authSessionJson(7, "sess-7-$loginCount"), headers = headers)
                    else respond(authSessionJson(8, "sess-8"), headers = headers)
                }
                "/api/v2/data-requests/scope" -> respond(scopeJson(7), headers = headers)
                "/api/v2/auth/reauth" -> respond(grantJson(), headers = headers)
                "/api/v2/data-requests" -> respond("""{"ok":false,"error":"timeout"}""", HttpStatusCode.RequestTimeout, headers)
                "/api/v2/data-requests/requests/$key" -> respond(dataStatusJson(55L, "close_account", "processing"), headers = headers)
                else -> error("unexpected path ${request.url.encodedPath}")
            }
        }
        val api = MobileApi("https://example.test", vault, "test", HttpClient(engine))
        val store = SensitiveActionStore(api, backgroundScope)
        try {
            api.password("cat7", "unused")
            val input = DataRequestInput("close_account", key, queryToken)
            store.prepare(SensitiveIntent.DataRequest(input))
            store.state.first { it.stage == SensitiveStage.Review }
            store.verify(password = "unused")
            store.state.first { it.stage == SensitiveStage.ReadyToSubmit }
            store.confirm(true)
            store.state.first { it.stage == SensitiveStage.ResultUnknown }

            // Concurrent login for the same user creates a newer session generation before reconcile.
            api.password("cat7", "unused")
            store.reconcile()
            val done = store.state.first { it.stage == SensitiveStage.Complete }
            assertEquals(55L, done.resultId)
            assertEquals(7L, api.user.value?.id)
            assertNull(api.sessionEnd)
            assertNotNull(api.rememberedIdentity)
            assertTrue(vault.value.orEmpty().contains("sess-7-2"))
            assertEquals(55L, api.limitedRequestAccess().single { it.clientRequestId == key }.id)

            // Account switch also never clears the new user's session or credentials on direct stale reconcile.
            api.password("cat8", "unused")
            val switched = api.reconcileDataRequest(input, expectedOwner = 7L, expectedGeneration = 1L)
            assertFalse(switched.endedOriginalSession)
            assertEquals(8L, api.user.value?.id)
            assertNull(api.sessionEnd)
            assertTrue(vault.value.orEmpty().contains("sess-8"))
        } finally {
            store.close()
            api.close()
        }
    }

    @Test
    fun staleInFlightReconcileCallbackAfterAccountSwitchPreservesNewAccountSession() = runTest {
        val vault = Vault()
        val key = "close_req_inflight_01"
        val queryToken = "capability_token_fedcba9876543210fedcba9876543210"
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var loginCount = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v2/auth/password" -> {
                    loginCount++
                    if (loginCount == 1) respond(authSessionJson(7, "sess-7"), headers = headers)
                    else respond(authSessionJson(8, "sess-8"), headers = headers)
                }
                "/api/v2/data-requests/scope" -> respond(scopeJson(7), headers = headers)
                "/api/v2/auth/reauth" -> respond(grantJson(), headers = headers)
                "/api/v2/data-requests" -> respond("""{"ok":false,"error":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headers)
                "/api/v2/data-requests/requests/$key" -> {
                    if (!started.isCompleted) started.complete(Unit)
                    gate.await()
                    respond(dataStatusJson(66L, "close_account", "processing"), headers = headers)
                }
                else -> error("unexpected path ${request.url.encodedPath}")
            }
        }
        val api = MobileApi("https://example.test", vault, "test", HttpClient(engine))
        val store = SensitiveActionStore(api, backgroundScope)
        try {
            api.password("cat7", "unused")
            val input = DataRequestInput("close_account", key, queryToken)
            store.prepare(SensitiveIntent.DataRequest(input))
            store.state.first { it.stage == SensitiveStage.Review }
            store.verify(password = "unused")
            store.state.first { it.stage == SensitiveStage.ReadyToSubmit }
            store.confirm(true)
            store.state.first { it.stage == SensitiveStage.ResultUnknown }

            store.reconcile()
            started.await()
            api.password("cat8", "unused")
            gate.complete(Unit)
            store.state.first { it.stage == SensitiveStage.Idle }
            assertEquals(8L, api.user.value?.id)
            assertNull(api.sessionEnd)
            assertTrue(vault.value.orEmpty().contains("sess-8"))
        } finally {
            if (!gate.isCompleted) gate.complete(Unit)
            store.close()
            api.close()
        }
    }

    @Test
    fun wrongTypeAndNotFoundKeepUnknownStateAndEraseOrAssistanceKeepSession() = runTest {
        val vault = Vault()
        val eraseKey = "erase_req_unknown_01"
        val assistKey = "assist_req_unknown_01"
        val eraseToken = "capability_erase_0123456789abcdef0123456789abcdef"
        val assistToken = "capability_assist_0123456789abcdef0123456789abcdef"
        var eraseLookups = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v2/auth/password" -> respond(authSessionJson(7, "sess-keep"), headers = headers)
                "/api/v2/data-requests/scope" -> respond(scopeJson(7), headers = headers)
                "/api/v2/auth/reauth" -> respond(grantJson(), headers = headers)
                "/api/v2/data-requests" -> respond("""{"ok":false,"error":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headers)
                "/api/v2/data-requests/requests/$eraseKey" -> when (++eraseLookups) {
                    1 -> respond("""{"ok":false,"error":"not found","error_code":"not_found"}""", HttpStatusCode.NotFound, headers)
                    2 -> respond(dataStatusJson(77L, "close_account", "processing"), headers = headers)
                    else -> respond(dataStatusJson(88L, "erase_fields", "processing"), headers = headers)
                }
                "/api/v2/data-requests/requests/$assistKey" -> respond(dataStatusJson(99L, "matters_assistance", "needs_assistance"), headers = headers)
                else -> error("unexpected path ${request.url.encodedPath}")
            }
        }
        val api = MobileApi("https://example.test", vault, "test", HttpClient(engine))
        val store = SensitiveActionStore(api, backgroundScope)
        try {
            api.password("cat7", "unused")
            val eraseInput = DataRequestInput("erase_fields", eraseKey, eraseToken, fields = listOf("bio"))
            store.prepare(SensitiveIntent.DataRequest(eraseInput))
            store.state.first { it.stage == SensitiveStage.Review }
            store.verify(password = "unused")
            store.state.first { it.stage == SensitiveStage.ReadyToSubmit }
            store.confirm(true)
            store.state.first { it.stage == SensitiveStage.ResultUnknown }

            store.reconcile()
            store.state.first { it.stage == SensitiveStage.ResultUnknown && it.error != null }
            assertEquals(0L, api.limitedRequestAccess().single { it.clientRequestId == eraseKey }.id)
            assertEquals(7L, api.user.value?.id)

            store.reconcile()
            store.state.first { it.stage == SensitiveStage.ResultUnknown && it.error == "原请求回执尚待核对，请保留原编号" }
            assertEquals(0L, api.limitedRequestAccess().single { it.clientRequestId == eraseKey }.id)
            assertEquals(7L, api.user.value?.id)

            store.reconcile()
            val eraseDone = store.state.first { it.stage == SensitiveStage.Complete }
            assertEquals(88L, eraseDone.resultId)
            assertEquals(88L, api.limitedRequestAccess().single { it.clientRequestId == eraseKey }.id)
            assertEquals(7L, api.user.value?.id)
            assertNull(api.sessionEnd)

            val assistInput = DataRequestInput("matters_assistance", assistKey, assistToken, note = "need help")
            store.prepare(SensitiveIntent.DataRequest(assistInput))
            store.state.first { it.stage == SensitiveStage.Review }
            store.confirm(true)
            store.state.first { it.stage == SensitiveStage.ResultUnknown }
            store.reconcile()
            val assistDone = store.state.first { it.stage == SensitiveStage.Complete }
            assertEquals(99L, assistDone.resultId)
            assertEquals(99L, api.limitedRequestAccess().single { it.clientRequestId == assistKey }.id)
            assertEquals(7L, api.user.value?.id)
            assertNull(api.sessionEnd)
        } finally {
            store.close()
            api.close()
        }
    }
}
