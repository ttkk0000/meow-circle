package com.ttkk0000.meowcircle.kmpapp.mobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.client.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.round

@Composable internal fun CoarseLocationButton(taskKey: Any,onLocation: (Double,Double) -> Unit,onError: (String) -> Unit) {
    val context = LocalContext.current
    val key by rememberUpdatedState(taskKey)
    val locationHandler by rememberUpdatedState(onLocation)
    val errorHandler by rememberUpdatedState(onError)
    val scope = rememberCoroutineScope()
    var signal by remember { mutableStateOf<CancellationSignal?>(null) }
    var timeout by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var permissionKey by remember { mutableStateOf<Any?>(null) }
    DisposableEffect(taskKey) { onDispose { signal?.cancel();timeout?.cancel();signal = null;busy = false } }
    fun locate(expected: Any) {
        if(expected != key) return
        busy = true
        fun finish(location: Location?) { if(expected != key) return; timeout?.cancel();busy = false;signal = null
            if(location == null || (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) !in 0..600_000_000_000L) errorHandler("本次未获得近期位置，可手动填写城市和大致中心")
            else locationHandler(round(location.latitude * 100) / 100,round(location.longitude * 100) / 100)
        }
        runCatching {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = listOf(LocationManager.NETWORK_PROVIDER,LocationManager.PASSIVE_PROVIDER).firstOrNull { it in manager.allProviders && manager.isProviderEnabled(it) }
            if(provider == null) { finish(null);return@runCatching }
            if(Build.VERSION.SDK_INT >= 30) { val cancellation = CancellationSignal();signal = cancellation
                timeout = scope.launch { delay(10_000);cancellation.cancel();if(expected == key) { busy = false;errorHandler("本次定位超时，可继续手动填写大致区域") } }
                manager.getCurrentLocation(provider,cancellation,context.mainExecutor) { finish(it) }
            } else finish(manager.getLastKnownLocation(provider))
        }.onFailure { busy = false;errorHandler("暂无法读取大致位置，可手动填写城市和中心") }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        val pending = permissionKey;permissionKey = null
        if(pending != null && pending == key) { if(allowed) locate(pending) else errorHandler("未获得定位权限，仍可手动选择大致区域") }
    }
    TextButton({ if(context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate(key) else { permissionKey = key;permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } },enabled = !busy) { Text(if(busy) "正在读取本次大致位置" else "本次使用大致位置") }
}

@Composable internal fun CoarseMap(latitude: Double,longitude: Double) {
    val context = LocalContext.current
    val template = context.getString(R.string.mnd_map_embed_template)
    val url = remember(template,latitude,longitude) { runCatching {
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
        val raw = template.replace("{lat}",String.format(java.util.Locale.ROOT,"%.2f",latitude)).replace("{lon}",String.format(java.util.Locale.ROOT,"%.2f",longitude)).replace("{zoom}","12")
        Uri.parse(raw).takeIf { it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null }?.toString()
    }.getOrNull() }
    if(url == null) { TaskNotice("地图暂不可用","仍可按城市与公开大致区域查看求助，不影响提供私密线索或认领材料。");return }
    var failed by remember(url) { mutableStateOf(false) }
    if(failed) { TaskNotice("地图未能载入","当前求助列表保留，可继续查看原事项。");TextButton({ failed = false }) { Text("重试地图") };return }
    AndroidView(modifier = Modifier.fillMaxWidth().height(320.dp),factory = { ctx -> WebView(ctx).apply {
        settings.javaScriptEnabled = true;settings.allowFileAccess = false;settings.allowContentAccess = false;settings.setGeolocationEnabled(false);settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.safeBrowsingEnabled = true
        webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?,request: WebResourceRequest?): Boolean = request?.url?.let { it.scheme != "https" || it.host != Uri.parse(url).host } ?: true
            override fun onReceivedError(view: WebView?,request: WebResourceRequest?,error: android.webkit.WebResourceError?) { if(request?.isForMainFrame == true) failed = true }
        }
        loadUrl(url)
    } },update = { view -> if(view.url != url) view.loadUrl(url) },onReset = null,onRelease = { view -> view.stopLoading();view.destroy() })
}

@Composable internal fun MapFeature(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f = store.features
    FormField(f,s,"city","城市，可手动切换")
    FormField(f,s,"latitude","大致中心纬度",numeric = true);FormField(f,s,"longitude","大致中心经度",numeric = true)
    CoarseLocationButton("${store.state.value.user?.id}-$route",{ lat,lon -> f.edit("latitude",lat.toString());f.edit("longitude",lon.toString()) },f::notify)
    FeatureButton("按当前大致区域查看",!s.loading) { f.reload() }
    val first = s.mapCases.items.firstOrNull { it.id == (f.field("selected").toLongOrNull() ?: route.id) } ?: s.mapCases.items.firstOrNull()
    if(first != null) CoarseMap(first.data.latitude,first.data.longitude)
    TaskNotice("公开位置为大致区域","不展示家庭地址或精确距离。拒绝定位仍可按城市查看，不持续后台读取位置。")
    s.mapCases.items.forEach { row -> FeatureSection(row.data.title) { Text("${row.data.city} · ${row.data.area}");TextButton({ f.edit("selected",row.id.toString()) }) { Text("查看此大致区域") }; NavigationRow("查看原求助与进度") { f.closeRoute();store.open(MobileDestination.Case(row.id)) } } }
    MoreFeature(s.mapCases.next,s.loading) { f.reload(true) }
    if(s.mapCases.loaded && s.mapCases.items.isEmpty()) Text("当前条件暂无求助")
}
