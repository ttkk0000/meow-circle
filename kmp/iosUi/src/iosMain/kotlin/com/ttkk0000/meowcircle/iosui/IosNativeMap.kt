@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.ttkk0000.meowcircle.iosui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*
import kotlinx.coroutines.delay
import platform.Foundation.*
import platform.UIKit.UIView

/** Swift只处理原生地图，不持有业务Store、令牌或私密坐标。 */
interface IosMapSurface {
    val view: UIView
    fun render(points: List<LostMapPoint>, selectedId: String, center: LostMapPoint, pickable: Boolean, tint: Int)
    fun close()
}
data class IosMapEvent(val kind: String, val id: String = "", val latitude: Double = 0.0, val longitude: Double = 0.0)
interface IosMapFactory {
    val configured: Boolean
    fun create(onEvent: (IosMapEvent) -> Unit): IosMapSurface
}
object IosMapPlatform {
    var factory: IosMapFactory? = null
}

@Composable internal actual fun IosNativeLostMap(points: List<LostMapPoint>, selectedId: String, center: LostMapPoint,
    pickable: Boolean, onPick: (LostMapPoint) -> Unit, onSelect: (String) -> Unit) {
    val factory = IosMapPlatform.factory
    val preferences = NSUserDefaults.standardUserDefaults
    var agreed by remember { mutableStateOf(preferences.boolForKey("mnd-amap-map-v1")) }
    val uri = LocalUriHandler.current
    var retry by remember { mutableStateOf(0) }
    var loaded by remember(retry, agreed) { mutableStateOf(false) }
    var failed by remember(retry, agreed) { mutableStateOf(false) }
    val pick by rememberUpdatedState(onPick)
    val select by rememberUpdatedState(onSelect)
    val canPick by rememberUpdatedState(pickable)
    if (factory?.configured != true) { Text("地图暂不可用，仍可使用城市与文字区域。"); return }
    if (!agreed) {
        Text("使用高德地图会发送网络及必要设备信息和地图视野，不会自动读取当前位置。")
        TextButton({ uri.openUri("https://lbs.amap.com/pages/privacy/") }) { Text("阅读高德隐私政策") }
        TextButton({ preferences.setBool(true, "mnd-amap-map-v1"); agreed = true }) { Text("同意并加载地图") }
        return
    }
    LaunchedEffect(retry, agreed) { delay(15_000); if (!loaded) failed = true }
    if (failed) { Text("地图未能载入，当前输入和列表保留。"); TextButton({ retry++ }) { Text("重试地图") }; return }
    val tint = MaterialTheme.colorScheme.primary.toArgb()
    key(retry) {
        val surface = remember(factory) { factory.create { event -> when (event.kind) {
            "loaded" -> loaded = true
            "failed" -> failed = true
            "select" -> select(event.id)
            "pick" -> if (canPick) coarseMapPoint(event.latitude, event.longitude)?.let(pick)
        } } }
        UIKitView(factory = { surface.view }, modifier = Modifier.fillMaxWidth().height(320.dp),
            update = { surface.render(points, selectedId, center, pickable, tint) }, onRelease = { surface.close() },
            properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative, isNativeAccessibilityEnabled = true))
    }
    if (!loaded) Text("地图正在载入…")
    if (pickable) Text("轻点地图选择最后见到的大致位置，只保存公里级公开粗点。")
    TextButton({ preferences.removeObjectForKey("mnd-amap-map-v1"); agreed = false }) { Text("停止加载高德地图") }
}
