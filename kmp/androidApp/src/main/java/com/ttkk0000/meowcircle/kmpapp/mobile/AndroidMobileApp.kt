package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.clientui.ConsumerApp

@Composable fun AndroidMobileApp(store: MobileStore, pendingLink: PendingTaskLink? = null, openPendingLink: () -> Unit = {},
    dismissPendingLink: () -> Unit = {}, uiSession: AndroidUiSessionState? = null) {
    val context = LocalContext.current
    val platform = remember(context) { AndroidClientPlatform(context) }
    ConsumerApp(store, pendingLink, openPendingLink, dismissPendingLink, uiSession, platform)
}
