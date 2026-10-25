package com.ttkk0000.meowcircle.kmpapp.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.MainActivity
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.client.canonicalNotificationId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** 默认不初始化SDK；用户保存推送许可且配置齐全后才申请设备标识。 */
internal class AndroidDevicePush(private val app: MndApplication) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var token = ""
    private var requesting = false
    private val manager get() = app.getSystemService(NotificationManager::class.java)
    private val configured get() = BuildConfig.FCM_CONFIGURED
    private var messaging: FirebaseMessaging? = null

    init {
        refresh()
        scope.launch { app.mobileApi.devicePush.consent.collectLatest { allowed ->
            if (allowed) requestToken()
            else { messaging?.isAutoInitEnabled = false; manager.cancelAll() }
        } }
    }

    fun refresh() {
        app.mobileApi.devicePush.updateChannel(token, if (manager.areNotificationsEnabled()) "granted" else "denied", configured)
        app.mobileApi.devicePush.refresh()
        if (app.mobileApi.devicePush.consent.value) requestToken()
    }
    fun tokenChanged(value: String) { scope.launch { if (value.length in 16..4096) { token = value; refresh() } } }
    private fun requestToken() {
        if (!configured || requesting || !manager.areNotificationsEnabled()) return
        try {
            if (FirebaseApp.getApps(app).isEmpty()) FirebaseApp.initializeApp(app, FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FCM_APP_ID).setApiKey(BuildConfig.FCM_API_KEY)
                .setProjectId(BuildConfig.FCM_PROJECT_ID).setGcmSenderId(BuildConfig.FCM_SENDER_ID).build())
            val client = FirebaseMessaging.getInstance()
            messaging = client
            client.isAutoInitEnabled = true
            requesting = true
            client.token.addOnCompleteListener { result ->
                requesting = false
                if (result.isSuccessful) {
                    val next = result.result.orEmpty()
                    if (next != token) tokenChanged(next)
                } else app.mobileApi.devicePush.channelFailed()
            }
        } catch (_: Exception) { requesting = false; app.mobileApi.devicePush.channelFailed() }
    }

    fun receive(notificationId: String? = null) {
        // 仅传递待核对的通知编号；正文与对象路由由当前账号重新向服务端读取。
        if (app.mobileApi.user.value == null || !manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "M&D动态提醒", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(app, MainActivity::class.java).setAction("com.ttkk0000.meowcircle.OPEN_INBOX")
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notificationId?.let(::canonicalNotificationId)?.let {
            intent.putExtra("mnd_notification_id", it.toString())
        }
        val pending = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(app, CHANNEL).setSmallIcon(R.drawable.ic_mnd_notification)
            .setContentTitle("M&D").setContentText("有新的动态，请打开应用查看")
            .setContentIntent(pending).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setNumber(app.mobileApi.devicePush.badge.value).build()
        manager.notify("mnd-inbox", 1, notification)
    }
    companion object { private const val CHANNEL = "mnd_updates" }
}

class MndFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { (application as MndApplication).devicePush.tokenChanged(token) }
    override fun onMessageReceived(message: RemoteMessage) { (application as MndApplication).devicePush.receive(message.data["notification_id"]) }
}
