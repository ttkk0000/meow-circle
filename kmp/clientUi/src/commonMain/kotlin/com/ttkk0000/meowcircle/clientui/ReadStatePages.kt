package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

/** Figma 56:81–84的公共状态结构；图标用原24px资源，48px只是留白容器。 */
@Composable fun HonestStateCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.TopStart) { DesignIcon("state-paw-print", null) }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable fun ReadFailureContent(failure: ClientReadFailure?, error: String, retry: () -> Unit, readAt: String? = null, enabled: Boolean = true, actions: @Composable ColumnScope.() -> Unit = {}) {
    val kind = failure ?: ClientReadFailure.Service
    val copy = kind.copyText()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HonestStateCard(copy.title, copy.detail)
        if (readAt != null) Text("上次成功读取：${displayTime(readAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MndSecondaryButton("重新读取", enabled = enabled, onClick = retry)
        actions()
    }
}

@Composable fun UnavailableTaskActions(store: MobileStore, failure: ClientReadFailure?, includeHome: Boolean = true) {
    if (failure == null || failure in listOf(ClientReadFailure.Connection, ClientReadFailure.Service, ClientReadFailure.Busy)) {
        FeatureButton("查看草稿与待核对操作") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
        if (includeHome) TaskPrimary("回到社区") { store.features.closeRoute(); store.select(MobileTab.Community) }
        return
    }
    if (failure == ClientReadFailure.SignIn) FeatureButton("重新登录", !store.state.value.busy) { store.showLogin() }
    FeatureButton("查看已有沟通") { store.features.closeRoute(); store.select(MobileTab.Messages) }
    FeatureButton("报告问题或申请复核") { store.features.open(FeatureRoute(FeaturePage.Safety)) }
    FeatureButton("找找其他领养信息") { store.features.closeRoute(); store.select(MobileTab.Adoption) }
    if (includeHome) TaskPrimary("回到社区") { store.features.closeRoute(); store.select(MobileTab.Community) }
}

@Composable fun EmptyResults(title: String = "这次还没有合适的结果", detail: String = "可以放宽区域或条件；不把其他城市内容偷偷当成本地结果。", actions: @Composable ColumnScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) { HonestStateCard(title, detail); actions() }
}

@Composable fun PendingNotificationBanner(store: MobileStore, state: MobileState, enabled: Boolean) {
    if (state.pendingNotification == null) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (state.notificationLoading) "正在核对原通知…" else "有待查看的通知，当前输入仍保留。")
            state.notificationError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row {
                TextButton(store::openPendingNotification, enabled = enabled) { Text(if (state.pendingNotification.id == 0L) "查看消息中心" else "查看原事项") }
                TextButton(store::dismissNotification) { Text("暂不查看") }
            }
        }
    }
}

@Composable fun NotificationHelpContent(store: MobileStore) {
    val platform = LocalClientPlatform.current
    var unavailable by rememberSessionUiState("notification-settings-unavailable") { false }
    MndScreenTitle("开启系统通知")
    TaskNotice("当前状态", if (platform.kind == ClientPlatformKind.Desktop)
        "桌面端仍可读取站内消息。当前客户端未提供原生系统推送通道，请在消息中心查看最新进度。"
        else "在系统设置中找到 M&D，进入通知设置并允许通知。返回后，M&D 会重新读取权限状态；拒绝后仍可查看站内消息。")
    if (platform.kind != ClientPlatformKind.Desktop) FeatureButton("打开系统通知设置") { unavailable = !platform.openNotificationSettings() }
    if (unavailable) Text("暂时无法打开系统设置，请从设备设置中找到 M&D 的通知选项。")
    FeatureButton("重新核对设备通道") { store.api.devicePush.refresh() }
    FeaturePrimary("返回原页面") { store.features.back() }
}

@Composable fun LocationHelpContent(store: MobileStore) {
    HonestStateCard("手动选城市，一样能浏览", "不会因为拒绝定位阻止阅读。目击位置也可以手动说明。")
    var city by rememberSessionUiState("location-help.city") { store.state.value.petCity }
    CityField(city,{ city = it },allowEmpty = false)
    FeatureButton("查看所选城市",city.isNotBlank()) {
        store.features.closeRoute(); store.select(MobileTab.Adoption)
        val state = store.state.value
        store.filterPets(state.petKeyword, city, state.petSpecies)
    }
    TaskNotice("按需请求权限", "只有你选择地图或拍照等动作时再解释原因；拒绝后提供替代。公开信息只填写大致区域，不填写门牌或私人精确地址。")
    FeatureButton("查看求助并提供手动目击线索") { store.features.closeRoute(); store.open(MobileDestination.Help) }
    FeaturePrimary("浏览全部城市") { store.features.closeRoute(); store.select(MobileTab.Adoption); store.filterPets("","","") }
}

@Composable fun PhotoHelpContent(store: MobileStore) {
    HonestStateCard("不用开放全部相册", "优先使用系统照片选择器，只分享你选中的照片。取消后仍可编辑文字与保存草稿。首版暂不支持视频。")
    FeatureButton("返回继续编辑") { store.features.back() }
    FeaturePrimary("返回上一步") { store.features.back() }
}
