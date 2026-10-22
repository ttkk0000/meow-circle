package com.ttkk0000.meowcircle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** SwiftUI 启动阶段需要的稳定快照，避免 Swift 直接处理 Kotlin Result。 */
data class IosBootstrapSnapshot(
    val user: User?,
    val healthHint: String,
)

/** SwiftUI 视图生命周期可取消的 KMP 请求句柄。 */
class IosRequestHandle internal constructor(
    private val job: Job,
) {
    fun cancel() {
        job.cancel()
    }
}

/**
 * iOS 的薄平台桥接层。
 *
 * 领域模型、会话与网络仍由 [MeowCircleSdk] 提供；这里只把 suspend + Result 转成
 * Swift 友好的回调，并确保低层异常先经过统一的人类可读映射。
 */
class IosSdkBridge(
    defaultBaseUrl: String,
) {
    private val sdk = MeowCircleSdk(defaultBaseUrl)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val baseUrl: String
        get() = sdk.baseUrl

    fun cachedUser(): User? = sdk.cachedUser()

    fun currentTheme(): String = sdk.getTheme()

    fun setTheme(value: String) {
        sdk.setTheme(value)
    }

    fun currentAppMode(): String? = sdk.sessionStore().getAppMode()

    fun setAppMode(value: String?) {
        sdk.sessionStore().setAppMode(value)
    }

    fun logout() {
        sdk.logout()
    }

    fun bootstrap(completion: (IosBootstrapSnapshot) -> Unit): IosRequestHandle {
        val job =
            scope.launch {
                val user =
                    runCatching { sdk.restoreSession() }.getOrNull()
                        ?: sdk.cachedUser()
                val healthHint =
                    sdk.health().fold(
                        onSuccess = { "服务 ${it.status} · ${it.store}" },
                        onFailure = { humanizeClientFailure(it, sdk.baseUrl) },
                    )
                completion(IosBootstrapSnapshot(user = user, healthHint = healthHint))
            }
        return IosRequestHandle(job)
    }

    fun login(
        username: String,
        password: String,
        completion: (User?, String?) -> Unit,
    ): IosRequestHandle =
        launchResult(
            request = { sdk.login(username.trim(), password) },
            completion = completion,
        )

    fun register(
        username: String,
        password: String,
        nickname: String,
        phone: String,
        smsCode: String,
        completion: (User?, String?) -> Unit,
    ): IosRequestHandle =
        launchResult(
            request = {
                sdk.register(
                    username = username.trim(),
                    password = password,
                    nickname = nickname.trim(),
                    phone = phone.trim(),
                    smsCode = smsCode.trim(),
                )
            },
            completion = completion,
        )

    fun loadFeed(
        filter: String,
        completion: (List<PostFeedItem>?, String?) -> Unit,
    ): IosRequestHandle =
        launchResult(
            request = { sdk.feedPosts(filter) },
            completion = completion,
        )

    fun loadListings(completion: (List<Listing>?, String?) -> Unit): IosRequestHandle =
        launchResult(
            request = { sdk.listings() },
            completion = completion,
        )

    fun loadConversations(completion: (List<Conversation>?, String?) -> Unit): IosRequestHandle =
        launchResult(
            request = { sdk.conversations() },
            completion = completion,
        )

    fun loadOrders(
        role: String,
        completion: (List<Order>?, String?) -> Unit,
    ): IosRequestHandle =
        launchResult(
            request = { sdk.myOrders(role) },
            completion = completion,
        )

    fun close() {
        scope.cancel()
    }

    private fun <T> launchResult(
        request: suspend () -> Result<T>,
        completion: (T?, String?) -> Unit,
    ): IosRequestHandle {
        val job =
            scope.launch {
                request().fold(
                    onSuccess = { completion(it, null) },
                    onFailure = {
                        completion(
                            null,
                            humanizeClientFailure(it, sdk.baseUrl),
                        )
                    },
                )
            }
        return IosRequestHandle(job)
    }
}
