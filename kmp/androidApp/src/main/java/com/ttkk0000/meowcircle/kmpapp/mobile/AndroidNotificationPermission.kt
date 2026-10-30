package com.ttkk0000.meowcircle.kmpapp.mobile

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.ttkk0000.meowcircle.clientui.*

@Composable internal fun AndroidNotificationPermission(showControls: Boolean, content: @Composable (Boolean) -> Unit) {
    val context = LocalContext.current
    fun allowed(): Boolean = context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    var permitted by remember { mutableStateOf(allowed()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = allowed() }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { permitted = allowed(); onPauseOrDispose {} }
    if (showControls) {
    Text("系统通知权限：${if(permitted) "已开启" else "未开启"}")
    if(Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        FeatureButton("申请系统通知权限") { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    FeatureButton("打开系统通知设置") { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)) }
    }
    content(permitted)
}
