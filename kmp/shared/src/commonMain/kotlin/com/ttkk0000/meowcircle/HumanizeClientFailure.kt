package com.ttkk0000.meowcircle

/**
 * Turns low-level Ktor / engine errors into short operator hints (Chinese).
 * API errors ([ApiException]) pass through the server message unchanged.
 */
fun humanizeClientFailure(
    throwable: Throwable,
    apiBaseUrl: String,
): String {
    if (throwable is ApiException) return throwable.message ?: "请求失败"
    val raw =
        generateSequence(throwable) { it.cause }
            .mapNotNull { it.message?.trim()?.takeIf { msg -> msg.isNotEmpty() } }
            .joinToString(" ")
    val m = raw.lowercase()
    return when {
        "socket timeout" in m ||
            "timeout has expired" in m ||
            "read timed out" in m ||
            "request timeout" in m ->
            "连接后端超时。\n请在仓库根目录启动：go run ./cmd/server\n当前地址：$apiBaseUrl\n" +
                "· 模拟器默认用 10.0.2.2 访问你这台电脑；服务必须在「Windows 本机」监听 8080（若只在 WSL 里跑服务，模拟器是连不到的）。\n" +
                "· 仍不通可试：终端执行 adb reverse tcp:8080 tcp:8080，把应用 API 改为 http://127.0.0.1:8080 后重装 APK。\n" +
                "· 检查 8080 是否已被占用：netstat -ano | findstr :8080；放行 Windows 防火墙；真机请改为电脑局域网 IP。"
        "connection refused" in m ||
            "failed to connect" in m ||
            "ECONNREFUSED" in raw ->
            "无法连接后端（连接被拒绝）。\n请先启动服务：go run ./cmd/server\n目标：$apiBaseUrl"
        "unable to resolve host" in m ||
            "unknownhost" in m.replace(" ", "") ->
            "无法解析主机名，请检查地址是否正确。\n当前：$apiBaseUrl"
        else -> raw.ifBlank { "网络请求失败" }
    }
}
