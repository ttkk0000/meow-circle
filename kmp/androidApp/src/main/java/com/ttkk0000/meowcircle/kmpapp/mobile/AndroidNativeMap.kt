package com.ttkk0000.meowcircle.kmpapp.mobile

import com.ttkk0000.meowcircle.clientui.*

import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import kotlinx.coroutines.delay

/** 手机／平板共享原生容器；不加载WebView，不开启高德定位服务。 */
@Composable internal fun NativeLostMap(
    points: List<LostMapPoint>, selectedId: String = "", center: LostMapPoint = mapCityCenter(""),
    pickable: Boolean = false, onPick: (LostMapPoint) -> Unit = {}, onSelect: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val preferences = remember { context.getSharedPreferences("mnd-map-privacy", android.content.Context.MODE_PRIVATE) }
    var agreed by remember { mutableStateOf(preferences.getBoolean("amap-map-v1", false)) }
    var retry by remember { mutableStateOf(0) }
    var loaded by remember(retry, agreed) { mutableStateOf(false) }
    var failed by remember(retry, agreed) { mutableStateOf(false) }
    val selectedHandler by rememberUpdatedState(onSelect)
    val pickHandler by rememberUpdatedState(onPick)
    val owner = LocalLifecycleOwner.current
    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f).toArgb()
    if (!BuildConfig.AMAP_CONFIGURED) {
        TaskNotice("地图暂不可用", "地图服务尚未配置，仍可填写城市和文字区域、查看求助列表。")
        return
    }
    if (!agreed) {
        TaskNotice("使用高德地图", "加载地图会向高德发送网络及必要设备信息和当前地图视野；不会自动读取当前位置。")
        TextButton({ uri.openUri("https://lbs.amap.com/pages/privacy/") }) { Text("阅读高德隐私政策") }
        TextButton({ preferences.edit().putBoolean("amap-map-v1", true).apply(); agreed = true }) { Text("同意并加载地图") }
        return
    }
    LaunchedEffect(retry, agreed) { delay(15_000); if (!loaded) failed = true }
    if (failed) {
        TaskNotice("地图暂时无法载入", "可以继续使用城市与文字区域；当前输入和求助列表保留。")
        TextButton({ retry++ }) { Text("重试地图") }
        return
    }
    var native by remember(retry) { mutableStateOf<MapView?>(null) }
    DisposableEffect(owner, native) {
        val view = native
        val observer = LifecycleEventObserver { _, event ->
            if (view != null && native === view) when (event) { Lifecycle.Event.ON_RESUME -> view.onResume(); Lifecycle.Event.ON_PAUSE -> view.onPause(); else -> Unit }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    key(retry) {
        var previous by remember { mutableStateOf<List<LostMapPoint>?>(null) }
        var lastSelected by remember { mutableStateOf<String?>(null) }
        var lastCenter by remember { mutableStateOf<LostMapPoint?>(null) }
        AndroidView(modifier = Modifier.fillMaxWidth().height(320.dp), factory = { ctx ->
            FrameLayout(ctx).apply {
                runCatching {
                    MapsInitializer.updatePrivacyShow(ctx, true, true)
                    MapsInitializer.updatePrivacyAgree(ctx, true)
                    val view = MapView(ctx)
                    view.onCreate(Bundle())
                    addView(view, FrameLayout.LayoutParams(-1, -1))
                    native = view
                    view.map.apply {
                        uiSettings.isScaleControlsEnabled = true
                        isMyLocationEnabled = false
                        setOnMapLoadedListener { loaded = true }
                        setOnMarkerClickListener { marker -> selectedHandler(marker.`object` as? String ?: ""); true }
                    }
                }.onFailure { failed = true }
            }
        }, update = {
            val view = native ?: return@AndroidView
            val map = view.map
            map.setOnMapClickListener { coordinate -> if (pickable) coarseMapPoint(coordinate.latitude, coordinate.longitude)?.let(pickHandler) }
            if (previous != points || lastSelected != selectedId) {
                map.clear()
                points.forEach { point ->
                    val coordinate = point.amapCoordinate(context) ?: return@forEach
                    map.addMarker(MarkerOptions().position(coordinate).title(point.title).snippet("公开大致区域"))?.`object` = point.id
                    if (point.id == selectedId || pickable) map.addCircle(CircleOptions().center(coordinate).radius(1000.0).strokeWidth(2f).strokeColor(primary).fillColor(fill))
                }
                previous = points
            }
            if (lastCenter != center || lastSelected != selectedId) {
                val target = points.firstOrNull { it.id == selectedId } ?: center
                target.amapCoordinate(context)?.let { map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, if (target.id == "country-center") 4f else 12f)) }
                lastCenter = center
                lastSelected = selectedId
            }
        }, onReset = null, onRelease = { container ->
            native = null
            (container.getChildAt(0) as? MapView)?.apply {
                map.setOnMapLoadedListener(null)
                map.setOnMapClickListener(null)
                map.setOnMarkerClickListener(null)
                onPause(); onDestroy()
            }
            container.removeAllViews()
        })
    }
    if (!loaded) Text("地图正在载入…", style = MaterialTheme.typography.labelSmall)
    if (pickable) Text("轻点地图选择大致位置；只保存公里级公开粗点。", style = MaterialTheme.typography.labelSmall)
    TextButton({ preferences.edit().remove("amap-map-v1").apply(); agreed = false }) { Text("停止加载高德地图") }
}

private fun LostMapPoint.amapCoordinate(context: android.content.Context): LatLng? {
    if (coordinateSystem !in listOf("gcj02", "wgs84")) return null
    val raw = LatLng(latitude, longitude)
    return if (coordinateSystem == "gcj02") raw else CoordinateConverter(context)
        .from(CoordinateConverter.CoordType.GPS).coord(raw).convert()
}

