package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

@Composable fun InboxReadAllDialog(store: MobileStore, s: MobileState) {
    val work = s.inboxReadAll
    if (!work.visible) return
    val locked = work.preparing || work.submitting
    MobileAlertDialog(onDismissRequest = { if(!locked) store.dismissInboxReadAll() },
        title = { Text("全部标为已读") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if(work.preparing) CircularProgressIndicator(Modifier.size(24.dp))
                work.snapshot?.let { snapshot ->
                    Text("将本次快照中的 ${snapshot.count} 条提醒标为已读，其中包含 ${snapshot.mutedCount} 条免打扰消息。")
                    Text("新到消息仍保持未读；不会接受消息请求、确认预约或完成待办。")
                    Text("确认有效至 ${displayTime(snapshot.expiresAt)}")
                }
                work.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
                if(work.resultUnknown) Text("结果待核对。重试只核对原请求，不扩大已读范围。")
                if(work.error != null && !work.resultUnknown) {
                    TextButton(store::restartInboxReadAll, enabled = !locked) { Text("重新获取范围并核对") }
                }
            }
        },
        confirmButton = {
            if(work.error != null) TextButton(store::retryInboxReadAll,enabled = !work.preparing && !work.submitting) { Text(if(work.resultUnknown) "核对原请求" else "重试") }
            else TextButton(store::confirmInboxReadAll,enabled = work.snapshot != null && !locked && !work.resultUnknown) { Text(if(work.submitting) "提交中…" else "确认全部已读") }
        },
        dismissButton = { TextButton(store::dismissInboxReadAll,enabled = !locked) { Text(if(work.resultUnknown) "稍后核对" else "返回") } })
}
