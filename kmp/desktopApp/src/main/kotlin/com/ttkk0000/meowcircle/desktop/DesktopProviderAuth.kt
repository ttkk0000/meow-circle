package com.ttkk0000.meowcircle.desktop

import com.sun.net.httpserver.HttpServer
import com.ttkk0000.meowcircle.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Executors

/** 官方网页授权使用已登记的HTTPS回跳；本机端口只接收本次一次性结果，不接收会话。 */
internal class DesktopProviderAuth(private val endpoint: DesktopEndpoint) {
    private val pending = Mutex()
    private val apps = buildMap {
        System.getenv("MND_WECHAT_WEB_APP_ID")?.takeIf { it.matches(Regex("wx[a-zA-Z0-9]{16}")) && it != "wx0000000000000000" }?.let { put("wechat", it) }
        System.getenv("MND_QQ_WEB_APP_ID")?.takeIf { it.matches(Regex("[1-9][0-9]{4,19}")) }?.let { put("qq", it) }
    }
    val providers: Set<String> get() = if (URI(endpoint.web).scheme == "https") apps.keys else emptySet()

    suspend fun authorize(provider: String, challenge: ProviderChallenge): ProviderCredential? {
        val app = apps[provider]?.takeIf { provider in providers }
            ?: throw MobileApiException(503, "desktop official application is not configured")
        if (!challenge.state.matches(Regex("[A-Za-z0-9_-]{32,200}"))) throw MobileApiException(503, "invalid provider challenge")
        if (!pending.tryLock()) throw MobileApiException(409, "authorization already pending", "provider_pending")
        try {
            return withContext(Dispatchers.IO) {
                val secret = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
                val path = "/mnd/provider/$secret"
                val result = CompletableDeferred<ProviderCredential?>()
                val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "mnd-provider-return").apply { isDaemon = true } }
                val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 4)
                try {
                    val host = "127.0.0.1:${server.address.port}"
                    server.executor = executor
                    server.createContext(path) { exchange ->
                        var accepted = false
                        var credential: ProviderCredential? = null
                        try {
                            if (exchange.requestMethod == "POST" && exchange.requestURI.rawPath == path && exchange.requestURI.rawQuery == null &&
                                exchange.requestHeaders.getFirst("Host") == host && exchange.requestHeaders.getFirst("Origin") == endpoint.web &&
                                exchange.requestHeaders.getFirst("Content-Type")?.substringBefore(';') == "application/x-www-form-urlencoded") {
                                val body = exchange.requestBody.readNBytes(8193)
                                if (body.size <= 8192) {
                                    val fields = body.toString(UTF_8).split('&').map { part ->
                                        val pair = part.split('=', limit = 2)
                                        URLDecoder.decode(pair[0], UTF_8) to URLDecoder.decode(pair.getOrElse(1) { "" }, UTF_8)
                                    }
                                    val values = fields.toMap()
                                    val code = values["code"].orEmpty()
                                    if (values.size == fields.size && values.keys == setOf("state", "code", "cancelled") &&
                                        values["state"] == challenge.state && code.length <= 4096 && code.none { it.isISOControl() } &&
                                        values["cancelled"] in setOf("true", "false") && (values["cancelled"] == "true" || code.isNotEmpty())) {
                                        accepted = !result.isCompleted
                                        credential = if (values["cancelled"] == "true") null else ProviderCredential(code = code, appId = app, client = "web")
                                    }
                                }
                            }
                        } catch (_: Exception) { /* 不记录官方凭据或本机回跳正文。 */ }
                        try {
                            exchange.responseHeaders.set("Cache-Control", "no-store")
                            exchange.responseHeaders.set("Content-Type", "text/plain; charset=utf-8")
                            exchange.responseHeaders.set("Referrer-Policy", "no-referrer")
                            exchange.responseHeaders.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
                            val text = (if (accepted) "请回到 M&D 桌面窗口继续核验。" else "此回跳不可使用，请回到 M&D 重新授权。").toByteArray(UTF_8)
                            exchange.sendResponseHeaders(if (accepted) 200 else 403, text.size.toLong())
                            exchange.responseBody.use { it.write(text) }
                        } finally { exchange.close(); if (accepted) result.complete(credential) }
                    }
                    server.start()
                    val callback = "http://$host$path"
                    fun encode(value: String) = URLEncoder.encode(value, UTF_8)
                    val bridge = URI("${endpoint.web}/app/desktop-provider#provider=${encode(provider)}&state=${encode(challenge.state)}&callback=${encode(callback)}")
                    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) throw MobileApiException(503, "browser unavailable")
                    Desktop.getDesktop().browse(bridge)
                    withTimeout(5 * 60_000L) { result.await() }
                } finally {
                    server.stop(0)
                    executor.shutdownNow()
                }
            }
        } finally { pending.unlock() }
    }
}
