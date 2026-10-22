package com.ttkk0000.meowcircle.iosui

import androidx.compose.runtime.*
import androidx.compose.ui.window.ComposeUIViewController
import com.ttkk0000.meowcircle.client.MobileApi
import com.ttkk0000.meowcircle.client.MobileSecretStorage
import com.ttkk0000.meowcircle.client.MobileStore
import com.ttkk0000.meowcircle.client.LocalComposeDrafts

/** One app-owned client coordinates Keychain token rotation across multiple iPad Scenes. */
class IosClient(baseUrl: String, secrets: MobileSecretStorage) {
    private val api = MobileApi(baseUrl, secrets, "ios")
    private val drafts = LocalComposeDrafts(IosComposeDraftFiles(), api.storageScope)

    fun makeViewController() = ComposeUIViewController {
        val store = remember { MobileStore(api, ownsApi = false, localDrafts = drafts) }
        DisposableEffect(store) { onDispose { store.close() } }
        IosMobileApp(store)
    }
}
