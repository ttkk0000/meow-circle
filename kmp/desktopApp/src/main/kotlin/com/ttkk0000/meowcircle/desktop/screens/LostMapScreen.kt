package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.MeowCircleSdk
import java.awt.Desktop
import java.net.URI

/** Desktop复用同一Web地图；不把原生会话或令牌放进浏览器链接。 */
@Composable fun LostMapScreen(sdk: MeowCircleSdk) {
    var failed by remember { mutableStateOf(false) }
    val url = remember {
        runCatching {
            val configured = System.getenv("MND_WEB_MAP_URL")?.takeIf { it.isNotBlank() }
            val value = configured ?: URI(sdk.sessionStore().getApiUrl("http://127.0.0.1:8080")).resolve("/app/map").toString()
            URI(value).takeIf { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() && it.userInfo == null }
        }.getOrNull()
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("走失互助地图", style = MaterialTheme.typography.headlineSmall)
        Text("查看猫咪最后出现的大致区域、拾获求助与原事项进度。")
        Text("地图将在系统浏览器打开，与Web端使用同一高德地图。发布时使用浏览器中的登录状态。")
        Button(onClick = {
            failed = runCatching {
                check(url != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(url)
            }.isFailure
        }, enabled = url != null) { Text("打开高德Web互助地图") }
        if (url == null || failed) Text("暂时无法打开浏览器，请在Web端进入走失互助地图。", color = MaterialTheme.colorScheme.error)
    }
}
