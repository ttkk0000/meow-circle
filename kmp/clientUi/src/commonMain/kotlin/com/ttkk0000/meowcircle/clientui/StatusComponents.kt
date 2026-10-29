package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

data class PendingTaskLink(val kind: String, val id: Long, val owner: Long) {
    val label: String get() = when(kind) {
        "pets" -> "宠物详情"; "entries" -> "社区动态"; "pet-comments" -> "领养交流"; "cases" -> "寻宠求助"
        "offers" -> "有偿信息"; "adoptions" -> "领养进度"; "appointments" -> "预约安排"; "groups" -> "群聊"
        "conversations" -> "私信"; "claims" -> "认领进度"; "reviews" -> "核实事项"
        "invitations" -> "邀请与协作"; "leads" -> "私密线索"; else -> "原任务"
    }
}

/**
 * 跨页面复用的 MVI 顶栏／表单状态提示条（统一处理加载进度条、可关闭横幅通知、主错误与草稿错误）。
 */
@Composable
fun MviStatusBanner(
    loading: Boolean,
    notice: String? = null,
    error: String? = null,
    secondaryError: String? = null,
    bannerNotice: Boolean = false,
    onDismissNotice: (() -> Unit)? = null,
) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (notice != null) {
        if (bannerNotice && onDismissNotice != null) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(notice, Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                IconButton(onClick = onDismissNotice) { Icon(Icons.Outlined.Close, "关闭提示") }
            }
        } else {
            Text(notice, color = MaterialTheme.colorScheme.primary)
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    secondaryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

/**
 * 待查看深链提示横幅组件。
 */
@Composable
fun PendingLinkBanner(
    pendingLink: PendingTaskLink?,
    enabled: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val link = pendingLink ?: return
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text("有待查看的${link.label}，当前输入仍保留。", style = MaterialTheme.typography.bodyMedium)
        Row {
            androidx.compose.material3.TextButton(onOpen, enabled = enabled) { Text("查看链接") }
            androidx.compose.material3.TextButton(onDismiss) { Text("暂不查看") }
        }
    }
}
