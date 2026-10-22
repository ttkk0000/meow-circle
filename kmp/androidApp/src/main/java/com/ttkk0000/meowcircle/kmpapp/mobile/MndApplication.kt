package com.ttkk0000.meowcircle.kmpapp.mobile

import android.app.Application
import com.ttkk0000.meowcircle.client.MobileApi
import com.ttkk0000.meowcircle.client.LocalComposeDrafts
import com.ttkk0000.meowcircle.client.LocalWorkspaceDrafts
import com.ttkk0000.meowcircle.kmpapp.BuildConfig

/** Activities have separate navigation stores but one process-wide refresh coordinator. */
class MndApplication : Application() {
    val mobileApi by lazy { MobileApi(BuildConfig.API_BASE_URL, AndroidSecretStorage(this), "android") }
    val composeDrafts by lazy { LocalComposeDrafts(AndroidComposeDraftFiles(this), mobileApi.storageScope) }
    val workspaceDrafts by lazy { workspace("mnd-workflows","mnd-workflows-guest") }
    val replyDrafts by lazy { workspace("mnd-replies","mnd-replies-guest") }
    private fun workspace(directory: String,guestDirectory: String) = LocalWorkspaceDrafts(
        AndroidComposeDraftFiles(this,directory,1024L * 1024),AndroidComposeDraftFiles(this,guestDirectory,1024L * 1024),mobileApi.storageScope)
}
