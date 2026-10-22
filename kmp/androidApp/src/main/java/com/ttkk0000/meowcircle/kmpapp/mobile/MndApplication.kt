package com.ttkk0000.meowcircle.kmpapp.mobile

import android.app.Application
import com.ttkk0000.meowcircle.client.MobileApi
import com.ttkk0000.meowcircle.kmpapp.BuildConfig

/** Activities have separate navigation stores but one process-wide refresh coordinator. */
class MndApplication : Application() {
    val mobileApi by lazy { MobileApi(BuildConfig.API_BASE_URL, AndroidSecretStorage(this), "android") }
}
