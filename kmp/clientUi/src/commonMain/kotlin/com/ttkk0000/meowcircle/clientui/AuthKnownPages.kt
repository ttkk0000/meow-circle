package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

@Composable internal fun AuthKnownTask(store: MobileStore, known: RememberedIdentity, busy: Boolean, onPhone: () -> Unit, onOther: () -> Unit) {
    val platform = LocalClientPlatform.current
    var agreed by rememberSessionUiState("known.agreed", "transient:auth", generation = platform.policies) { false }
    MobileTaskDialog(store::cancelLogin, title = "欢迎回来", navigationEnabled = !busy) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // A signed-out identity is display metadata, not permission to fetch its private avatar.
                MobilePhoto(store, null, null, "上次账号头像", Modifier.size(56.dp).clip(MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(known.user.nickname.ifBlank { known.user.username }, style = MaterialTheme.typography.titleMedium)
                    Text(if (known.method.isBlank()) "上次在本机登录的账号" else "上次使用${known.method}登录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TaskNotice("继续使用同一账号", "登录原账号后，宠物、日记、收藏和聊天都会保留。请再次验证；返回的其他身份不会按昵称合并。")
        CheckLine("我已阅读并同意服务协议与隐私政策", agreed, { agreed = it }, !busy && platform.policies.ready)
        Row {
            TextButton({ platform.openLink(platform.policies.terms) }, enabled = !busy && platform.policies.ready) { Text("服务协议") }
            TextButton({ platform.openLink(platform.policies.privacy) }, enabled = !busy && platform.policies.ready) { Text("隐私政策") }
        }
        ProviderEntry(store, store.state.value.capabilities.providers, busy || !agreed || !platform.policies.ready,
            scope = "transient:auth", heading = "快捷登录", knownLayout = true)
        MndQuietButton("换用手机号", !busy, onClick = onPhone)
        MndQuietButton("其他登录方式", !busy, onClick = onOther)
    }
}
