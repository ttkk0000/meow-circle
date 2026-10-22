package com.ttkk0000.meowcircle.kmpapp

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color
import androidx.activity.viewModels
import com.ttkk0000.meowcircle.kmpapp.mobile.MndApplication
import com.ttkk0000.meowcircle.kmpapp.mobile.MobileViewModel
import com.ttkk0000.meowcircle.kmpapp.mobile.AndroidMobileApp

class MainActivity : ComponentActivity() {
    private val appViewModel by viewModels<MobileViewModel> {
        val app = application as MndApplication
        MobileViewModel.Factory(app.mobileApi, app.composeDrafts, app.workspaceDrafts, app.replyDrafts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (savedInstanceState == null) appViewModel.openLink(intent?.data)
        setContent {
            AndroidMobileApp(appViewModel.store)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appViewModel.openLink(intent.data)
    }
}
