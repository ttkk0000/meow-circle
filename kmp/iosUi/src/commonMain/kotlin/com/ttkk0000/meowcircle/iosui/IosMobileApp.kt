package com.ttkk0000.meowcircle.iosui

import androidx.compose.runtime.Composable
import com.ttkk0000.meowcircle.client.MobileStore
import com.ttkk0000.meowcircle.clientui.ClientUiPlatform
import com.ttkk0000.meowcircle.clientui.ConsumerApp

/** 每个Scene拥有自己的页面和Store，API与Keychain刷新协调由IosClient持有。 */
@Composable internal fun IosMobileApp(store: MobileStore, platform: ClientUiPlatform) =
    ConsumerApp(store = store, platform = platform)
