package com.ttkk0000.meowcircle.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Contract regression sources only; execution requires the user's separate test authorization. */
class MobileApiContractTest {
    private class Vault(var value: String? = null) : MobileSecretStorage {
        override fun read() = value
        override fun write(value: String?) { this.value = value }
    }
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private fun session(token: String) = """{"ok":true,"data":{"user":{"id":1,"username":"cat","nickname":"猫友"},"token":"$token","refresh_token":"refresh-$token","expires_at":"2026-11-20T00:00:00Z"}}"""
    private val profile = """{"ok":true,"data":{"user":{"id":1,"username":"cat","nickname":"猫友"},"version":1}}"""

    @Test
    fun concurrentUnauthorizedRequestsRotateOnlyOnce() = runTest {
        var refreshes = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v2/auth/password" -> respond(session("old"), headers = jsonHeaders)
                "/api/v2/auth/refresh" -> {
                    assertNull(request.headers[HttpHeaders.Authorization])
                    refreshes++
                    delay(20)
                    respond(session("new"), headers = jsonHeaders)
                }
                "/api/v2/profile" -> {
                    if (request.headers[HttpHeaders.Authorization] == "Bearer old") {
                        delay(10)
                        respond("""{"ok":false,"error":"expired"}""", HttpStatusCode.Unauthorized, jsonHeaders)
                    } else respond(profile, headers = jsonHeaders)
                }
                else -> error("Unexpected path ${request.url.encodedPath}")
            }
        }
        val vault = Vault()
        val api = MobileApi("https://example.test", vault, "test", HttpClient(engine))
        try {
            api.password("cat", "unused")
            val users = awaitAll(async { api.profile() }, async { api.profile() })
            assertEquals(listOf(1L, 1L), users.map { it.user.id })
            assertEquals(1, refreshes)
            assertTrue(vault.value.orEmpty().contains("refresh-new"))
        } finally { api.close() }
    }

    @Test
    fun conflictDoesNotReplayAConfirmation() = runTest {
        var writes = 0
        val api = MobileApi("https://example.test", Vault(), "test", HttpClient(MockEngine { request ->
            if (request.url.encodedPath == "/api/v2/auth/password") respond(session("current"), headers = jsonHeaders)
            else {
                writes++
                respond("""{"ok":false,"error":"version conflict"}""", HttpStatusCode.Conflict, jsonHeaders)
            }
        }))
        try {
            api.password("cat", "unused")
            val failure = assertFailsWith<MobileApiException> { api.adoptionAction(5, "confirm_handover", AdoptionActionInput(3)) }
            assertEquals(409, failure.status)
            assertEquals(1, writes)
        } finally { api.close() }
    }

    @Test
    fun cancellationKeepsTheSessionAndRemainsCancellation() = runTest {
        val vault = Vault()
        val entered = CompletableDeferred<Unit>()
        val api = MobileApi("https://example.test", vault, "test", HttpClient(MockEngine { request ->
            if (request.url.encodedPath == "/api/v2/auth/password") respond(session("current"), headers = jsonHeaders)
            else { entered.complete(Unit); awaitCancellation() }
        }))
        try {
            api.password("cat", "unused")
            val before = vault.value
            val loading = launch { api.profile() }
            entered.await()
            loading.cancelAndJoin()
            assertTrue(loading.isCancelled)
            assertEquals(before, vault.value)
        } finally { api.close() }
    }

    @Test
    fun nativeIdsKeepInt64PrecisionAndPrivateFieldsMayBeAbsent() = runTest {
        val id = 9_007_199_254_740_993L
        val api = MobileApi("https://example.test", Vault(), "test", HttpClient(MockEngine {
            respond("""{"ok":true,"data":{"id":$id,"owner_id":2,"version":1,"state":"active","data":{"name":"小满","species":"cat","audience":{"mode":"public"},"adoption_available":true}}}""", headers = jsonHeaders)
        }))
        try {
            val pet = api.pet(id)
            assertEquals(id, pet.id)
            assertEquals("", pet.data.health)
        } finally { api.close() }
    }
}
