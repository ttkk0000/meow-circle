package com.ttkk0000.meowcircle.iosui

import androidx.compose.runtime.*
import androidx.compose.ui.window.ComposeUIViewController
import com.ttkk0000.meowcircle.client.MobileApi
import com.ttkk0000.meowcircle.client.MobileSecretStorage
import com.ttkk0000.meowcircle.client.MobileStore
import com.ttkk0000.meowcircle.client.LocalComposeDrafts
import com.ttkk0000.meowcircle.client.LocalWorkspaceDrafts
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** One app-owned client coordinates Keychain token rotation across multiple iPad Scenes. */
class IosClient(baseUrl: String, secrets: MobileSecretStorage) {
    private val api = MobileApi(baseUrl, secrets, "ios")
    private val pushScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    init {
        pushScope.launch { api.devicePush.consent.collectLatest { IosPushPlatform.controller?.setEnabled(it) } }
        pushScope.launch { api.devicePush.badge.collectLatest { IosPushPlatform.controller?.setBadge(it) } }
    }
    fun updatePushChannel(token: String, permission: String, available: Boolean) = api.devicePush.updateChannel(token, permission, available)
    private val scenes = mutableSetOf<MobileStore>()
    fun refreshPush() { api.devicePush.refresh(); scenes.forEach { it.refreshInbox() } }
    fun pushFailed() = api.devicePush.channelFailed()
    private val drafts = LocalComposeDrafts(IosComposeDraftFiles(), api.storageScope)

    private fun workspace(name: String) = LocalWorkspaceDrafts(
        IosComposeDraftFiles(name, 1024uL * 1024uL), IosComposeDraftFiles("$name-guest", 1024uL * 1024uL), api.storageScope)
    private val workspaces = workspace("mnd-workflows")
    private val replies = workspace("mnd-replies")

    fun makeViewController() = ComposeUIViewController {
        val store = remember { MobileStore(api, ownsApi = false, localDrafts = drafts, localWorkspaces = workspaces, localReplies = replies) }
        DisposableEffect(store) { scenes.add(store); onDispose { scenes.remove(store); store.close() } }
        val platform = remember { IosClientPlatform() }
        IosMobileApp(store, platform)
    }
}
