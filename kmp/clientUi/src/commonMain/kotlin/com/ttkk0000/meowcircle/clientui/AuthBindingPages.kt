package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

internal fun authMethodLabel(provider: String): String = when (provider) {
    "wechat" -> "微信"; "qq" -> "QQ"; "apple" -> "Apple"; "email" -> "邮箱"
    else -> "其他已关联方式"
}

/** Only server-reported associations are described; this is not an eligibility check. */
@Composable internal fun AuthUnbindTask(methods: IdentityMethods?, identityId: Long, busy: Boolean, onKeep: () -> Unit, onConfirm: () -> Unit) {
    val selected = methods?.linked?.firstOrNull { it.id == identityId }
    val remaining = buildList {
        if (methods?.passwordSet == true) add("账号密码")
        methods?.phone?.takeIf { it.isNotBlank() }?.let { add("手机号 $it") }
        methods?.linked?.filter { it.id != identityId }?.forEach { add(authMethodLabel(it.provider)) }
    }
    MobileTaskDialog(onKeep, title = "解除登录关联", navigationEnabled = !busy) {
        AuthFacts(listOf("登录方式" to (selected?.let { authMethodLabel(it.provider) } ?: "本次方式待核对"),
            "其他可用方式" to if (methods == null) "尚未读取" else remaining.joinToString("、").ifBlank { "尚未确认其他方式" }))
        TaskNotice("解除后会怎样", "此方式将不能登录当前账号，资料和其他已关联方式保留。成功后旧会话会失效，请重新登录；最终是否允许解除由服务核对。")
        MndSecondaryButton("保留关联", !busy, modifier = Modifier.fillMaxWidth(), onClick = onKeep)
        TaskSecondary("确认解除", !busy && selected != null, onConfirm)
    }
}

@Composable internal fun AuthBindTask(store: MobileStore, user: MobileUser, method: String, destination: String, replacing: Boolean,
    busy: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    MobileTaskDialog(onCancel, title = if (replacing) "更新登录方式" else "关联登录方式", navigationEnabled = !busy) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MobilePhoto(store, avatarMediaId(user.avatarUrl), user.id, "当前账号头像", Modifier.size(56.dp).clip(MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(user.nickname.ifBlank { user.username }, style = MaterialTheme.typography.titleMedium)
                    Text("当前账号", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        AuthFacts(listOf((if (replacing) "将更新的方式" else "将关联的方式") to method, "本次联系方式" to destination))
        TaskNotice("确认是你本人的账号", "下次可用此方式登录同一个M&D账号。此次确认不创建新账号，也不改变交流资格；成功后旧会话会失效，请重新登录。")
        MndQuietButton(if (replacing) "暂不更新" else "暂不关联", !busy, onClick = onCancel)
        TaskPrimary(if (replacing) "确认更新" else "确认关联", !busy, onConfirm)
    }
}
