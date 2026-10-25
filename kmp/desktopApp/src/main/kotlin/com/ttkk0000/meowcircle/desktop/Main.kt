package com.ttkk0000.meowcircle.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.clientui.ConsumerApp

/** 活跃入口仅使用v2 Store；旧Stitch源码保留为设计参考。 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "M&D · meow & doggie",
        state = rememberWindowState(width = 1280.dp, height = 840.dp)) {
        val endpoint = remember { DesktopEndpoint.fromEnvironment() }
        val paths = remember(endpoint) { DesktopPrivateFiles.root(endpoint.api) }
        val api = remember(endpoint) { MobileApi(endpoint.api, DesktopSecretStorage(paths), "desktop") }
        val store = remember(api) {
            fun files(name: String, limit: Int = 128 * 1024) = DesktopPrivateFiles(paths.resolve(name), limit)
            fun workspace(name: String) = LocalWorkspaceDrafts(files(name, 1024 * 1024), files("$name-guest", 1024 * 1024), api.storageScope)
            MobileStore(api, localDrafts = LocalComposeDrafts(files("compose"), api.storageScope),
                localWorkspaces = workspace("workflows"), localReplies = workspace("replies"))
        }
        val platform = remember(window, endpoint) { DesktopClientPlatform(window, endpoint) }
        DisposableEffect(store) { onDispose { store.close() } }
        ConsumerApp(store = store, platform = platform)
    }
}
