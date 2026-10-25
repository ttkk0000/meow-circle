package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

@Composable fun InboxScreen(store: MobileStore,s: MobileState) {
    var category by rememberSaveable(s.user?.id) { mutableStateOf(0) }
    var unreadOnly by rememberSaveable(s.user?.id) { mutableStateOf(false) }
    LaunchedEffect(category,s.user?.id) { if(category != 0) store.loadNotifications() }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ChoiceChipBar(
            options = listOf(0 to "聊天",1 to "互动",2 to "通知"),
            selected = category,
            onSelect = { category = it },
            badge = { index ->
                val count = s.inbox?.let { inbox -> if(index == 0) inbox.direct.values.sum() + inbox.groups.values.sum() else inbox.notifications.filterKeys { key -> if(index == 1) key == "interactions" else key !in listOf("interactions","direct_messages","groups") }.values.sum() }
                UnreadBadge(count)
            },
        )
        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton({ unreadOnly = !unreadOnly }) { Text(if(unreadOnly) "显示全部消息" else "仅看未读") }
            TextButton({ store.features.open(FeatureRoute(FeaturePage.Settings)) }) { Text("提醒设置") }
        }
        LazyColumn(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(8.dp),contentPadding = PaddingValues(bottom = 16.dp)) {
            if(category == 0) {
                item { TaskNotice("消息请求 ${s.inbox?.messageRequests ?: "—"}","接受后才能发送图片。待处理事项 ${s.inbox?.pending?.values?.sum() ?: "—"}，与未读分开计算。") }
                items(s.conversations.items.filter { !unreadOnly || (s.inbox?.direct?.get(it.id.toString()) ?: 0) > 0 },key = { "direct-${it.id}" }) { row ->
                    val peer = if(row.data.requesterId == s.user?.id) row.data.recipientId else row.data.requesterId
                    LaunchedEffect(peer,s.user?.id) { store.ensureMember(peer) }
                    val member = s.members[peer]
                    MessageRow(member?.nickname?.ifBlank { member.username } ?: "宠友 #$peer",if(row.state == "pending") "待接受的消息请求" else "日常交流",s.inbox?.direct?.get(row.id.toString()),store = store,avatar = member?.avatarUrl?.let(::avatarMediaId)) { store.open(MobileDestination.Chat(row.id)) }
                }
                item { PageFooter(s.conversations,{ store.refreshHome() },{ store.refreshHome(true) }) }
                items(s.groups.items.filter { !unreadOnly || (s.inbox?.groups?.get(it.id.toString()) ?: 0) > 0 },key = { "group-${it.id}" }) { row ->
                    MessageRow(row.data.name,"${row.data.members.size} 位成员${if(s.user?.id in row.data.muted) " · 免打扰" else ""}",s.inbox?.groups?.get(row.id.toString()),true) { store.open(MobileDestination.Chat(row.id,true)) }
                }
                item { PageFooter(s.groups,{ store.refreshHome() },{ store.refreshHome(true) }) }
                item { NavigationRow("发现宠友群") { store.features.open(FeatureRoute(FeaturePage.GroupDirectory)) }; NavigationRow("好友与邀请") { store.features.open(FeatureRoute(FeaturePage.Invitations)) }; NavigationRow("创建群聊") { store.features.open(FeatureRoute(FeaturePage.GroupCreate)) }; NavigationRow("预约与安排") { store.features.open(FeatureRoute(FeaturePage.Appointments)) } }
            } else {
                val rows = s.notifications.items.filter { (!unreadOnly || !it.data.read) && if(category == 1) it.data.category == "interactions" else it.data.category !in listOf("interactions","direct_messages","groups") }
                items(rows,key = { "notice-${it.id}" }) { row ->
                    TaskSection(notificationCategoryLabel(row.data.category)) { Text(row.data.text); if(!row.data.read) TextButton({ store.readNotification(row) },enabled = !s.busy) { Text("标为已读") }; TextButton({ openNotification(store,row.data) }) { Text("查看原事项") } }
                }
                item { PageFooter(s.notifications,{ store.loadNotifications() },{ store.loadNotifications(true) }); if(rows.isEmpty() && s.notifications.loaded && !s.notifications.loading) Text("此分类暂无消息") }
            }
        }
    }
}
private fun notificationCategoryLabel(value: String): String = when(value) {
    "interactions" -> "社区互动"
    "applications" -> "申请与业务进度"
    "direct_messages" -> "私信提醒"
    "groups" -> "群聊提醒"
    else -> "站内通知"
}
