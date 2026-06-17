package com.ttkk0000.meowcircle

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer

/**
 * Shared API + session logic for Meow Circle (mirrors `mobile/src/api.ts` core flows).
 *
 * @param baseUrl e.g. `http://10.0.2.2:8080` (Android emulator) or your LAN IP for a device.
 */
class MeowCircleSdk(
    private val defaultBaseUrl: String,
    private val session: SessionStore = SessionStore(),
) {
    val baseUrl: String
        get() = session.getApiUrl(defaultBaseUrl)

    private val root: String
        get() = baseUrl.trimEnd('/')

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

    private val http: HttpClient = createPlatformHttpClient(json)

    fun sessionStore(): SessionStore = session

    fun cachedUser(): User? {
        val raw = session.getUserJson() ?: return null
        return runCatching { json.decodeFromString(User.serializer(), raw) }.getOrNull()
    }

    fun getTheme(): String = session.getTheme()

    fun setTheme(theme: String) {
        session.setTheme(theme)
    }

    fun logout() {
        session.clear()
    }

    suspend fun health(): Result<HealthData> =
        runCatching {
            unwrapData(httpGet("/healthz", auth = false))
        }

    suspend fun login(username: String, password: String): Result<User> =
        runCatching {
            val envelopeText =
                httpPost(
                    "/api/v1/auth/login",
                    auth = false,
                    body = LoginBody(username = username, password = password),
                )
            val data: LoginData = unwrapData(envelopeText)
            session.setToken(data.token)
            session.setUserJson(json.encodeToString(User.serializer(), data.user))
            data.user
        }

    suspend fun me(): User {
        val text = httpGet("/api/v1/auth/me", auth = true)
        val user: User = unwrapData(text)
        session.setUserJson(json.encodeToString(User.serializer(), user))
        return user
    }

    /**
     * If a token exists, validates with `/api/v1/auth/me`. On 401, clears session and returns null.
     */
    suspend fun restoreSession(): User? {
        val token = session.getToken() ?: return null
        if (token.isBlank()) return null
        return try {
            me()
        } catch (e: ApiException) {
            if (e.statusCode == 401) {
                session.clear()
                null
            } else {
                throw e
            }
        }
    }

    suspend fun feedPosts(filter: String = "rec"): Result<List<PostFeedItem>> =
        runCatching {
            val path = "/api/v1/posts?page=1&page_size=40&filter=$filter"
            val page: PostsPage = unwrapData(httpGet(path, auth = true))
            page.items.orEmpty()
        }

    suspend fun listings(type: String? = null, category: String? = null): Result<List<Listing>> =
        runCatching {
            var url = "/api/v1/listings"
            val params = mutableListOf<String>()
            if (type != null) params.add("type=$type")
            if (category != null) params.add("category=$category")
            if (params.isNotEmpty()) {
                url += "?" + params.joinToString("&")
            }
            val authed = !session.getToken().isNullOrBlank()
            val page: ListingsPage = unwrapData(httpGet(url, auth = authed))
            page.items.orEmpty()
        }

    suspend fun listingDetail(listingId: Long): Result<ListingDetailData> =
        runCatching {
            val authed = !session.getToken().isNullOrBlank()
            unwrapData(httpGet("/api/v1/listings/$listingId", auth = authed))
        }

    suspend fun myListings(): Result<List<Listing>> =
        runCatching {
            val page: ListingsPage = unwrapData(httpGet("/api/v1/me/listings", auth = true))
            page.items.orEmpty()
        }

    suspend fun createListing(
        type: String = "product",
        title: String,
        description: String,
        priceCents: Long,
        currency: String = "CNY",
        mediaIds: List<Long> = emptyList(),
    ): Result<Listing> =
        runCatching {
            unwrapData(
                httpPost(
                    "/api/v1/listings",
                    auth = true,
                    body =
                        CreateListingBody(
                            type = type.trim().ifBlank { "product" },
                            title = title.trim(),
                            description = description.trim(),
                            priceCents = priceCents.coerceAtLeast(0L),
                            currency = currency.trim().ifBlank { "CNY" },
                            mediaIds = mediaIds,
                        ),
                ),
            )
        }

    suspend fun register(
        username: String,
        password: String,
        nickname: String = "",
        phone: String = "",
        smsCode: String = "",
    ): Result<User> =
        runCatching {
            val nick = nickname.trim().ifBlank { username.trim() }
            val envelopeText =
                httpPost(
                    "/api/v1/auth/register",
                    auth = false,
                    body =
                        RegisterBody(
                            username = username.trim(),
                            password = password,
                            nickname = nick,
                            phone = phone.trim(),
                            smsCode = smsCode.trim(),
                        ),
                )
            val data: LoginData = unwrapData(envelopeText)
            session.setToken(data.token)
            session.setUserJson(json.encodeToString(User.serializer(), data.user))
            data.user
        }

    /** POST follow or DELETE unfollow; returns new following state. */
    suspend fun setFollowing(
        userId: Long,
        follow: Boolean,
    ): Result<Boolean> =
        runCatching {
            val path = "/api/v1/me/follow/$userId"
            val text =
                if (follow) {
                    httpPostEmpty(path, auth = true)
                } else {
                    httpDelete(path, auth = true)
                }
            val out: FollowActionResult = unwrapData(text)
            out.following
        }

    suspend fun postDetail(postId: Long): Result<PostDetailData> =
        runCatching {
            val authed = !session.getToken().isNullOrBlank()
            unwrapData(httpGet("/api/v1/posts/$postId", auth = authed))
        }

    suspend fun createPost(
        title: String,
        content: String,
        category: String = "daily_share",
        tags: List<String> = emptyList(),
        mediaIds: List<Long> = emptyList(),
    ): Result<Post> =
        runCatching {
            unwrapData(
                httpPost(
                    "/api/v1/posts",
                    auth = true,
                    body =
                        CreatePostBody(
                            title = title.trim(),
                            content = content.trim(),
                            category = category,
                            tags = tags,
                            mediaIds = mediaIds,
                        ),
                ),
            )
        }

    suspend fun addComment(
        postId: Long,
        content: String,
    ): Result<Comment> =
        runCatching {
            unwrapData(
                httpPost(
                    "/api/v1/posts/$postId/comments",
                    auth = true,
                    body = CreateCommentBody(content = content.trim()),
                ),
            )
        }

    suspend fun togglePostLike(postId: Long): Result<LikeActionResult> =
        runCatching {
            unwrapData(httpPostEmpty("/api/v1/posts/$postId/like", auth = true))
        }

    suspend fun userProfile(userId: Long): Result<User> =
        runCatching {
            val authed = !session.getToken().isNullOrBlank()
            unwrapData(httpGet("/api/v1/users/$userId", auth = authed))
        }

    suspend fun getUserPets(userId: Long): Result<List<Pet>> =
        runCatching {
            val authed = !session.getToken().isNullOrBlank()
            unwrapData(httpGet("/api/v1/users/$userId/pets", auth = authed))
        }

    suspend fun conversations(): Result<List<Conversation>> =
        runCatching {
            val payload: ConversationsPayload = unwrapData(httpGet("/api/v1/me/conversations", auth = true))
            payload.items.orEmpty()
        }

    suspend fun conversationWithPeer(peerId: Long): Result<ConversationDetailData> =
        runCatching {
            unwrapData(httpGet("/api/v1/me/conversations/$peerId", auth = true))
        }

    suspend fun sendMessage(
        recipientId: Long,
        content: String,
    ): Result<Message> =
        runCatching {
            unwrapData(
                httpPost(
                    "/api/v1/messages",
                    auth = true,
                    body = SendMessageBody(recipientId = recipientId, content = content.trim()),
                ),
            )
        }

    suspend fun notifications(unreadOnly: Boolean = false): Result<NotificationsPayload> =
        runCatching {
            val suffix = if (unreadOnly) "?unread=true" else ""
            unwrapData(httpGet("/api/v1/notifications$suffix", auth = true))
        }

    suspend fun publicUser(userId: Long): Result<User> =
        runCatching {
            unwrapData(httpGet("/api/v1/users/$userId", auth = false))
        }

    suspend fun updateMe(
        nickname: String,
        avatarUrl: String,
        bio: String,
    ): Result<User> =
        runCatching {
            val updated: User =
                unwrapData(
                    httpPatch(
                        "/api/v1/me",
                        auth = true,
                        body =
                            UpdateMeBody(
                                nickname = nickname.trim(),
                                avatarUrl = avatarUrl.trim(),
                                bio = bio.trim(),
                            ),
                    ),
                )
            session.setUserJson(json.encodeToString(User.serializer(), updated))
            updated
        }

    private suspend fun httpGet(
        path: String,
        auth: Boolean,
    ): String {
        val response =
            http.get("$root$path") {
                if (auth) attachBearer()
            }
        return readResponse(response)
    }

    private suspend inline fun <reified B : Any> httpPost(
        path: String,
        auth: Boolean,
        body: B,
    ): String {
        val response =
            http.post("$root$path") {
                contentType(ContentType.Application.Json)
                if (auth) attachBearer()
                setBody(body)
            }
        return readResponse(response)
    }

    private suspend fun httpPostEmpty(
        path: String,
        auth: Boolean,
    ): String {
        val response =
            http.post("$root$path") {
                if (auth) attachBearer()
            }
        return readResponse(response)
    }

    private suspend fun httpDelete(
        path: String,
        auth: Boolean,
    ): String {
        val response =
            http.delete("$root$path") {
                if (auth) attachBearer()
            }
        return readResponse(response)
    }

    private suspend inline fun <reified B : Any> httpPatch(
        path: String,
        auth: Boolean,
        body: B,
    ): String {
        val response =
            http.patch("$root$path") {
                contentType(ContentType.Application.Json)
                if (auth) attachBearer()
                setBody(body)
            }
        return readResponse(response)
    }

    private suspend fun readResponse(response: HttpResponse): String {
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val msg =
                runCatching {
                    json.decodeFromString(ApiEnvelope.serializer(), text).message
                }.getOrDefault(text)
            if (response.status.value == 401) {
                session.clear()
            }
            throw ApiException(response.status.value, msg)
        }
        return text
    }

    private fun HttpRequestBuilder.attachBearer() {
        val t = session.getToken()
        if (!t.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $t")
        }
    }

    private inline fun <reified T> unwrapData(envelopeText: String): T {
        val env = json.decodeFromString(ApiEnvelope.serializer(), envelopeText)
        if (env.code != 0) {
            throw ApiException(env.code, env.message)
        }
        val data = env.data ?: throw ApiException(0, "empty data")
        return json.decodeFromJsonElement(serializer<T>(), data)
    }

    suspend fun myOrders(role: String = "buyer"): Result<List<Order>> =
        runCatching {
            val payload: OrdersPayload = unwrapData(httpGet("/api/v1/me/orders?role=$role", auth = true))
            payload.items.orEmpty()
        }

    suspend fun orderDetail(orderId: Long): Result<Order> =
        runCatching {
            unwrapData(httpGet("/api/v1/orders/$orderId", auth = true))
        }

    suspend fun createOrder(listingId: Long, note: String = ""): Result<Order> =
        runCatching {
            unwrapData(
                httpPost(
                    "/api/v1/orders",
                    auth = true,
                    body = CreateOrderBody(listingId = listingId, note = note)
                )
            )
        }

    suspend fun payOrder(orderId: Long, method: String = "mock"): Result<Order> =
        runCatching {
            unwrapData(
                httpPost(
                    "/api/v1/orders/$orderId/pay",
                    auth = true,
                    body = PayOrderBody(method = method)
                )
            )
        }

    suspend fun cancelOrder(orderId: Long): Result<Order> =
        runCatching {
            unwrapData(httpPostEmpty("/api/v1/orders/$orderId/cancel", auth = true))
        }

    suspend fun shipOrder(orderId: Long): Result<Order> =
        runCatching {
            unwrapData(httpPostEmpty("/api/v1/orders/$orderId/ship", auth = true))
        }

    suspend fun completeOrder(orderId: Long): Result<Order> =
        runCatching {
            unwrapData(httpPostEmpty("/api/v1/orders/$orderId/complete", auth = true))
        }

    suspend fun refundOrder(orderId: Long): Result<Order> =
        runCatching {
            unwrapData(httpPostEmpty("/api/v1/orders/$orderId/refund", auth = true))
        }

    suspend fun createReport(
        targetKind: String,
        targetId: Long,
        reason: String,
    ): Result<Report> =
        runCatching {
            unwrapData(
                httpPost(
                    "/api/v1/reports",
                    auth = true,
                    body =
                        ReportBody(
                            targetKind = targetKind.trim(),
                            targetId = targetId,
                            reason = reason.trim(),
                        ),
                ),
            )
        }
}
