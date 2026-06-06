package com.ttkk0000.meowcircle.desktop

import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.SessionStore
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.Listing
import com.ttkk0000.meowcircle.Comment
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer

class MndApiClient(
    val sdk: MeowCircleSdk
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val root: String
        get() = sdk.baseUrl.trimEnd('/')

    private fun HttpRequestBuilder.attachAuth() {
        val token = sdk.sessionStore().getToken()
        if (!token.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun fetchUserStats(userId: Long): Result<UserStats> = runCatching {
        val response = client.get("$root/api/v1/users/$userId/stats") {
            attachAuth()
        }
        unwrapData(response.bodyAsText())
    }

    suspend fun fetchOrders(role: String): Result<List<Order>> = runCatching {
        val response = client.get("$root/api/v1/me/orders?role=$role") {
            attachAuth()
        }
        val data: OrderListResponse = unwrapData(response.bodyAsText())
        data.items.orEmpty()
    }

    suspend fun createOrder(listingId: Long): Result<Order> = runCatching {
        val response = client.post("$root/api/v1/orders") {
            contentType(ContentType.Application.Json)
            attachAuth()
            setBody(CreateOrderRequest(listingId))
        }
        unwrapData(response.bodyAsText())
    }

    suspend fun updateOrderStatus(orderId: Long, action: String): Result<Boolean> = runCatching {
        val response = client.post("$root/api/v1/orders/$orderId/$action") {
            attachAuth()
        }
        response.status.isSuccess()
    }

    suspend fun fetchConversationDetails(peerId: Long): Result<ConversationDetails> = runCatching {
        val response = client.get("$root/api/v1/me/conversations/$peerId") {
            attachAuth()
        }
        unwrapData(response.bodyAsText())
    }

    suspend fun sendMessage(recipientId: Long, content: String): Result<Boolean> = runCatching {
        val response = client.post("$root/api/v1/messages") {
            contentType(ContentType.Application.Json)
            attachAuth()
            setBody(mapOf("recipient_id" to recipientId, "content" to content))
        }
        response.status.isSuccess()
    }

    suspend fun addComment(postId: Long, content: String): Result<Comment> = runCatching {
        val response = client.post("$root/api/v1/posts/$postId/comments") {
            contentType(ContentType.Application.Json)
            attachAuth()
            setBody(mapOf("content" to content))
        }
        unwrapData(response.bodyAsText())
    }

    suspend fun publishListing(type: String, title: String, description: String, priceCents: Long): Result<Listing> = runCatching {
        val response = client.post("$root/api/v1/listings") {
            contentType(ContentType.Application.Json)
            attachAuth()
            setBody(CreateListingRequest(type, title, description, priceCents))
        }
        unwrapData(response.bodyAsText())
    }

    suspend fun fetchAuditLogs(): Result<List<AuditLog>> = runCatching {
        val response = client.get("$root/api/v1/admin/audit-logs") {
            header("X-Admin-Key", "admin123")
            attachAuth()
        }
        val data: AuditLogListResponse = unwrapData(response.bodyAsText())
        data.items.orEmpty()
    }

    private inline fun <reified T> unwrapData(envelopeText: String): T {
        val env = json.decodeFromString(com.ttkk0000.meowcircle.ApiEnvelope.serializer(), envelopeText)
        if (env.code != 0) {
            throw Exception(env.message)
        }
        val data = env.data ?: throw Exception("empty data")
        return json.decodeFromJsonElement(serializer<T>(), data)
    }
}
