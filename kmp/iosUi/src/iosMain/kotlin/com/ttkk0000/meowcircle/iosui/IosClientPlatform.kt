@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.ttkk0000.meowcircle.iosui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.clientui.*
import org.jetbrains.skia.Image as SkiaImage
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*

/** 原生系统适配不持有业务草稿；每个Scene独立创建，禁止全局窗口尺寸。 */
internal class IosClientPlatform : ClientUiPlatform {
    override val kind = ClientPlatformKind.IOS
    override val policies get() = ClientPolicyLinks(
        NSBundle.mainBundle.objectForInfoDictionaryKey("MND_TERMS_URL") as? String ?: "",
        NSBundle.mainBundle.objectForInfoDictionaryKey("MND_PRIVACY_URL") as? String ?: "")
    private var providerController: UIViewController? = null
    override val providers get() = setOf("wechat","qq","apple").filter { IosProviderPlatform.factory?.configured(it) == true }.toSet()
    override suspend fun authorizeProvider(provider: String, challenge: ProviderChallenge): ProviderCredential? =
        authorizeIosProvider(provider,challenge,providerController ?: throw MobileApiException(503,"provider Scene unavailable"))?.let { credential ->
            val key = if(provider == "wechat") "MND_WECHAT_IOS_APP_ID" else if(provider == "qq") "MND_QQ_IOS_APP_ID" else ""
            credential.copy(client = "ios",appId = if(key.isEmpty()) "" else NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: "")
        }
    private val backActions = mutableStateListOf<() -> Unit>()

    @Composable override fun Window(content: @Composable BoxWithConstraintsScope.() -> Unit) {
        val controller = LocalUIViewController.current
        DisposableEffect(controller) { providerController = controller; onDispose { if(providerController === controller) providerController = null } }
        val edge = with(LocalDensity.current) { 24.dp.toPx() }
        val distance = with(LocalDensity.current) { 80.dp.toPx() }
        BoxWithConstraints(Modifier.fillMaxSize().pointerInput(edge, distance) {
            var startedAtEdge = false
            var moved = 0f
            detectHorizontalDragGestures(onDragStart = { startedAtEdge = it.x <= edge; moved = 0f },
                onHorizontalDrag = { change, delta ->
                    if (startedAtEdge && backActions.isNotEmpty()) { moved += delta; if (moved > 0) change.consume() }
                }, onDragEnd = { if (startedAtEdge && moved >= distance) backActions.lastOrNull()?.invoke() },
                onDragCancel = { moved = 0f })
        }, content = content)
    }

    @Composable override fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
        val current by rememberUpdatedState(onBack)
        val callback = remember { { current() } }
        DisposableEffect(enabled) {
            if (enabled) backActions.add(callback)
            onDispose { backActions.remove(callback) }
        }
    }

    @Composable override fun PhotoPicker(remaining: Int, enabled: Boolean, onSelected: (List<SelectedPhoto>) -> Unit, onError: (String) -> Unit) =
        IosSelectPhotos(remaining, enabled, onSelected, onError)

    @Composable override fun DateField(store: MobileFeatureStore, state: FeatureState, key: String, label: String,
        fallback: String, dateOnly: Boolean, optional: Boolean) {
        val value = store.field(key, fallback)
        var opened by rememberSessionUiState("$key.dateOpen") { false }
        val formatter = remember(dateOnly) { NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = "en_US_POSIX")
            dateFormat = "yyyy-MM-dd"
            lenient = false
        } }
        val initial = remember(value, dateOnly) {
            if (dateOnly) formatter.dateFromString(value) ?: NSDate()
            else NSISO8601DateFormatter().apply { formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds }.dateFromString(value)
                ?: NSISO8601DateFormatter().dateFromString(value) ?: NSDate()
        }
        val enabled = !state.busy && !state.loading && state.confirmation == null && !state.unconfirmedResult
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge)
            OutlinedButton({ opened = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (value.isBlank()) "选择${if (dateOnly) "日期" else "日期与时间"}" else if (dateOnly) value else displayTime(value))
            }
            if (!dateOnly) Text("按本机时区选择，确认页显示实际时间。", style = MaterialTheme.typography.bodySmall)
            if (optional && value.isNotBlank()) TextButton({ store.edit(key, "") }, enabled = enabled) { Text("清除日期") }
        }
        if (opened && enabled) {
            val picker = remember(state.route, key) { UIDatePicker(frame = CGRectMake(0.0, 0.0, 320.0, 216.0)).apply {
                datePickerMode = if (dateOnly) UIDatePickerMode.UIDatePickerModeDate else UIDatePickerMode.UIDatePickerModeDateAndTime
                preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
                locale = NSLocale(localeIdentifier = "zh_CN")
                date = initial
                if (dateOnly) maximumDate = NSDate()
            } }
            MobileAlertDialog({ opened = false }, { Text(label) }, {
                UIKitView(factory = { picker }, modifier = Modifier.fillMaxWidth().height(216.dp))
            }, { TextButton({
                val selected = picker.date
                store.edit(key, if (dateOnly) formatter.stringFromDate(selected) else NSISO8601DateFormatter().stringFromDate(selected))
                opened = false
            }) { Text("确认选择") } }, { TextButton({ opened = false }) { Text("返回编辑") } })
        }
    }

    @Composable override fun Map(points: List<LostMapPoint>, selectedId: String, center: LostMapPoint, pickable: Boolean,
        onPick: (LostMapPoint) -> Unit, onSelect: (String) -> Unit) = IosNativeLostMap(points, selectedId, center, pickable, onPick, onSelect)

    @Composable override fun NotificationPermission(showControls: Boolean, content: @Composable (Boolean) -> Unit) {
        val center = remember { UNUserNotificationCenter.currentNotificationCenter() }
        var allowed by remember { mutableStateOf(false) }
        var known by remember { mutableStateOf(false) }
        var requesting by remember { mutableStateOf(false) }
        val refresh = remember(center) { {
            center.getNotificationSettingsWithCompletionHandler { settings -> NSOperationQueue.mainQueue.addOperationWithBlock {
                allowed = settings?.authorizationStatus in listOf(UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral)
                known = true
            } }
        } }
        DisposableEffect(center) {
            refresh()
            val observer = NSNotificationCenter.defaultCenter.addObserverForName(UIApplicationDidBecomeActiveNotification,
                `object` = null, queue = NSOperationQueue.mainQueue) { refresh() }
            onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
        }
        if (!known) Text("正在读取系统通知设置…")
        else {
            content(allowed)
            if (showControls && !allowed) {
                Text("系统通知尚未允许，站内消息仍可读取。")
                TextButton({ requesting = true
                    center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound) { _, _ ->
                        NSOperationQueue.mainQueue.addOperationWithBlock { requesting = false; refresh() }
                    }
                }, enabled = !requesting) { Text(if (requesting) "正在请求权限" else "允许系统通知") }
                TextButton({ NSURL(string = UIApplicationOpenSettingsURLString)?.let {
                    UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null)
                } }) { Text("打开系统通知设置") }
            }
        }
    }

    override fun decodeImage(bytes: ByteArray): ImageBitmap? = runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
    override fun openNotificationSettings(): Boolean {
        val target = NSURL(string = UIApplicationOpenSettingsURLString) ?: return false
        if (!UIApplication.sharedApplication.canOpenURL(target)) return false
        UIApplication.sharedApplication.openURL(target, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }
    override fun openLink(url: String): Boolean {
        if (!isPublicHttps(url)) return false
        val target = NSURL(string = url) ?: return false
        if (target.scheme != "https" || target.host.isNullOrBlank() || target.user != null || target.password != null) return false
        if (!UIApplication.sharedApplication.canOpenURL(target)) return false
        UIApplication.sharedApplication.openURL(target, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }
}
