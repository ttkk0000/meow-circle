package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

/** 关注只订阅可读公开内容，好友和私密资料仍按原服务端权限读取。 */
@Composable fun ContentSubscriptionButton(store: MobileStore,id: Long,case: Boolean = false) {
    val state by store.features.state.collectAsState()
    val f = store.features
    val actor = store.state.value.user?.id
    LaunchedEffect(actor,id,case) { f.loadContentSubscriptions() }
    val record = state.contentSubscriptions
    if (state.route == null) {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.error != null) FeatureButton("重新读取关注状态", !state.busy) { f.loadContentSubscriptions() }
        if (state.pending.any { it.phase in listOf("sent", "result_unknown") }) FeatureButton("核对原关注操作") { f.open(FeatureRoute(FeaturePage.Recovery)) }
    }
    val old = record?.data
    val selected = if(case) old?.watchedCases?.contains(id) == true else old?.followedUsers?.contains(id) == true
    val label = if(case) if(selected) "取消关注此求助" else "关注求助进展" else if(selected) "取消关注" else "关注作者"
    FeatureButton(label,!state.busy && (actor == null || record != null)) {
        if(actor == null) store.showLogin() else if(old != null && record != null) {
            val users = if(case) old.followedUsers else if(selected) old.followedUsers - id else old.followedUsers + id
            val cases = if(!case) old.watchedCases else if(selected) old.watchedCases - id else old.watchedCases + id
            val input = ContentSubscriptionInput(record.version,newRequestKey(),users,cases)
            f.confirm(label,"${if(case) "求助" else "用户"} #$id；不改变好友关系或私密访问范围。") { f.api.saveContentSubscriptions(input); f.loadContentSubscriptions(); store.refreshHome() }
        }
    }
}
fun speciesLabel(value: String) = when(value) { "cat" -> "猫咪"; "dog" -> "狗狗"; "rabbit" -> "兔兔"; else -> "其他宠物" }
