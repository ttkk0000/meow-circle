package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

fun notificationSettingsTitle(s: FeatureState): String = when (s.notificationView) {
    NotificationSettingsView.Frequency -> "提醒频率"
    NotificationSettingsView.Permission -> "系统通知权限"
    NotificationSettingsView.PermissionHelp -> "开启系统通知"
    NotificationSettingsView.Saved -> when (s.notificationResult?.data?.frequency) {
        "digest" -> "已改为每日汇总"
        "instant" -> "已使用即时通知"
        "off" -> "已关闭非紧急提醒"
        else -> "通知偏好已保存"
    }
    else -> "通知偏好"
}

@Composable fun NotificationSettingsContent(store: MobileStore, s: FeatureState) {
    val f = store.features
    val platform = LocalClientPlatform.current
    val old = s.preference?.data ?: PreferenceBody()
    val input = old.copy(
        directMessages = f.field("direct", old.directMessages.toString()).toBoolean(),
        groups = f.field("groups", old.groups.toString()).toBoolean(),
        applications = f.field("applications", old.applications.toString()).toBoolean(),
        interactions = f.field("interactions", old.interactions.toString()).toBoolean(),
        frequency = f.field("frequency", old.frequency),
        pushPermission = f.field("allow_push", old.pushPermission.toString()).toBoolean())
    val editable = !s.busy && !s.loading && !s.unconfirmedResult && !s.conflict && s.error == null && s.draftReady
    fun save(value: PreferenceBody = input) {
        if (!editable || value.frequency !in listOf("instant", "digest", "off")) return
        val version = f.version(s.preference?.version ?: 0)
        val summary = "${notificationFrequencyLabel(value.frequency)}\n私信：${if(value.directMessages) "开启" else "关闭"} · 群聊：${if(value.groups) "开启" else "关闭"} · 业务进度：${if(value.applications) "开启" else "关闭"}\n设备推送：${if(value.pushPermission) "允许" else "关闭"}。站内消息与未读仍保留。"
        f.confirm("保存通知偏好", summary) {
            val record = f.api.savePreferences(version, value)
            f.notificationPreferencesSaved(record, value, version)
        }
    }
    NotificationPermission(showControls = s.notificationView == NotificationSettingsView.PermissionHelp) { permitted ->
        val push by f.api.devicePush.status.collectAsState()
        val systemStatus = when {
            platform.kind == ClientPlatformKind.Desktop -> "桌面暂不提供设备推送"
            !permitted -> "系统通知尚未允许"
            !input.pushPermission -> "系统已允许 · 账号推送未开启"
            push.state == "enabled" -> "设备通道已登记，送达由系统决定"
            else -> "系统已允许 · ${push.message}"
        }
        when (s.notificationView) {
            NotificationSettingsView.Saved -> {
                val saved = s.notificationResult
                if (saved != null) {
                    Text(notificationSettingsTitle(s), style = PageHeadlineStyle)
                    TaskNotice("当前状态", if (!saved.data.pushPermission) "${notificationFrequencyLabel(saved.data.frequency)}的偏好已保存。设备推送未开启，你仍可查看站内消息。" else when (saved.data.frequency) {
                        "digest" -> "非紧急动态在中国标准时间每天19:00进入汇总投递时段；你随时可以在应用内查看。"
                        "instant" -> "新的非紧急动态将按已开启的类别进入投递队列。"
                        else -> "非紧急设备提醒已关闭；站内消息、未读和待处理事项仍然保留。"
                    })
                    if (!saved.data.pushPermission || !permitted || push.state != "enabled") Text("偏好已保存。$systemStatus；保存偏好不代表推送已经送达。", style = MaterialTheme.typography.bodySmall)
                    FeaturePrimary("返回通知设置", !s.busy) { f.back() }
                }
            }
            NotificationSettingsView.Frequency -> {
                NavigationRow("即时通知", "按已开启的类别接收${if(input.frequency == "instant") " · 当前选择" else ""}") { if(editable) { f.edit("frequency", "instant"); save(input.copy(frequency = "instant")) } }
                NavigationRow("每日汇总", "中国标准时间每天19:00${if(input.frequency == "digest") " · 当前选择" else ""}") { if(editable) { f.edit("frequency", "digest"); save(input.copy(frequency = "digest")) } }
                NavigationRow("关闭非紧急提醒", "保留站内消息${if(input.frequency == "off") " · 当前选择" else ""}") { if(editable) { f.edit("frequency", "off"); save(input.copy(frequency = "off")) } }
                TaskNotice("重要进度", "安全、账号与需及时确认的状态会在应用内保留。频率不改变未读或业务处理状态，附近提醒单独管理。")
            }
            NotificationSettingsView.Permission, NotificationSettingsView.PermissionHelp -> {
                TaskNotice("可以继续使用 M&D", "你仍能在消息页查看进度。账号的推送偏好、设备系统权限与通道登记分别管理。")
                Text(systemStatus)
                if (s.notificationView == NotificationSettingsView.Permission) {
                    PreferenceSwitch("允许设备推送", input.pushPermission, editable) { f.edit("allow_push", it.toString()) }
                    Text("仅保存偏好后启用或停用设备登记；当前修改可返回通知偏好，点击完成后核对保存。", style = MaterialTheme.typography.bodySmall)
                    FeatureButton("了解开启方式", !s.busy) { f.setNotificationView(NotificationSettingsView.PermissionHelp) }
                    FeaturePrimary("稍后再说", !s.busy) { f.setNotificationView(NotificationSettingsView.Preferences) }
                } else {
                    if (platform.kind != ClientPlatformKind.Desktop) {
                        Text("在系统设置中找到 M&D，进入通知设置并允许通知。返回后会重新读取本机权限。")
                        if (platform.kind == ClientPlatformKind.IOS && permitted) FeatureButton("打开系统通知设置", !s.busy) { platform.openNotificationSettings() }
                    }
                    FeatureButton("重新核对设备通道", !s.busy) { f.api.devicePush.refresh() }
                    FeaturePrimary("返回应用", !s.busy) { f.setNotificationView(NotificationSettingsView.Permission) }
                }
            }
            NotificationSettingsView.Preferences, NotificationSettingsView.Summary -> {
                if (s.notificationView == NotificationSettingsView.Preferences) {
                    PreferenceSwitch("私信消息", input.directMessages, editable) { f.edit("direct", it.toString()) }
                    PreferenceSwitch("宠友群消息", input.groups, editable) { f.edit("groups", it.toString()) }
                    PreferenceSwitch("申请与照护进度", input.applications, editable) { f.edit("applications", it.toString()) }
                    // Existing server category remains separately editable; changing it never changes unread totals.
                    PreferenceSwitch("社区评论与点赞", input.interactions, editable) { f.edit("interactions", it.toString()) }
                } else NavigationRow("管理通知类别", "私信、群聊、申请与互动") { f.setNotificationView(NotificationSettingsView.Preferences) }
                NavigationRow("附近走失提醒", "独立管理范围与频率") { if(!s.busy) f.open(FeatureRoute(FeaturePage.Nearby)) }
                NavigationRow("提醒频率", notificationFrequencyLabel(input.frequency)) { if(!s.busy) f.setNotificationView(NotificationSettingsView.Frequency) }
                NavigationRow("系统通知权限", systemStatus) { if(!s.busy) f.setNotificationView(NotificationSettingsView.Permission) }
                FeaturePrimary("完成", editable) { save() }
            }
        }
    }
}

@Composable private fun PreferenceSwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.surface, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Switch(checked, onCheckedChange = null, enabled = enabled, colors = SwitchDefaults.colors(
                checkedTrackColor = c.secondary, checkedThumbColor = c.surface,
                uncheckedTrackColor = c.surfaceVariant, uncheckedThumbColor = c.onSurfaceVariant, uncheckedBorderColor = c.outline,
                disabledCheckedTrackColor = ClientPublishDisabled, disabledCheckedThumbColor = ClientFocusColor,
                disabledUncheckedTrackColor = ClientPublishDisabled, disabledUncheckedThumbColor = ClientFocusColor))
        }
    }
}
