package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

fun invitationTitle(record: MobileRecord<InvitationBody>, actor: Long?): String {
    val scope = invitationScope(record.data.scope)
    val recipient = record.data.recipientId == actor
    if(record.data.scope !in listOf("caretaker","adoption_editor")) return "$scope · ${stateLabel(record.state)}"
    return when(record.state) {
        "accepted" -> { if(recipient) "已加入$scope" else "${scope}已授权" }
        "left" -> { if(recipient) "已退出$scope" else "对方已退出$scope" }
        "revoked" -> "${scope}授权已结束"
        "declined" -> "${scope}邀请已拒绝"
        "expired" -> "邀请已过期"
        else -> "${scope}邀请"
    }
}

@Composable
fun InvitationDetailContent(store: MobileStore, state: FeatureState, record: MobileRecord<InvitationBody>) {
    val f = store.features
    val actor = store.state.value.user?.id
    val recipient = record.data.recipientId == actor
    if(actor == null || record.ownerId != actor && !recipient) {
        TaskNotice("当前协作不可用","请使用收到邀请或发出邀请的账号重新核对；此处不展示受限资料。",warning = true)
        return
    }
    val collaboration = record.data.scope in listOf("caretaker","adoption_editor")
    if(collaboration) TaskNotice(if(record.state == "accepted") "本次有限授权" else "邀请范围",
        invitationActionDetail(record.data.scope,if(record.state == "revoked") "revoke" else "accept"))
    if(record.data.note.isNotBlank()) FeatureSection("邀请说明") { Text(record.data.note) }
    Text("事项 #${record.data.targetId} · 邀请 #${record.id} · 当前版本 ${record.version}")
    Text("${if(state.error != null || state.loading) "上次读取" else "当前状态"}：${stateLabel(record.state)}")
    if(record.state == "pending") Text("接受邀请截止 ${displayTime(record.data.expiresAt)}；截止只限制尚未接受的邀请。")
    if(record.state == "left") {
        TaskNotice("仅结束本次协作权限","当前邀请记录已退出；已有记录及原事项保留。核对某次未知请求时仍需读取原操作回执。")
        FeatureButton("核对原操作回执",!state.busy) { f.open(FeatureRoute(FeaturePage.Recovery)) }
    }
    val enabled = !state.busy && !state.loading && state.error == null && !state.conflict && !state.unconfirmedResult
    availableInvitationActions(record,actor).forEach { (action,label) ->
        FeatureButton(if(action == "leave") invitationActionTitle(record.data.scope,action) else label,enabled) {
            val input = VersionedAction(record.version,clientRequestId = newRequestKey())
            val title = if(action == "revoke" && record.state == "pending") "撤销本次邀请" else invitationActionTitle(record.data.scope,action)
            val summary = "${invitationScope(record.data.scope)} · 事项 #${record.data.targetId}\n邀请 #${record.id} · 基准版本 ${record.version}\n${invitationActionDetail(record.data.scope,action)}"
            val command: suspend () -> Unit = {
                val result = f.api.invitationAction(record.id,action,input)
                if(action == "leave") f.invitationCompleted(result) else f.reload()
                store.refreshHome()
            }
            if(action == "leave" || action == "revoke" && collaboration) {
                val confirmLabel = if(action == "leave") "确认退出" + if(record.data.scope == "caretaker") "照护协作" else "介绍协作"
                    else if(record.state == "pending") "确认撤销邀请" else if(record.data.scope == "caretaker") "确认撤销照护授权" else "确认结束介绍协作"
                val cancelLabel = if(action == "leave") "保留当前协作" else if(record.state == "pending") "保留邀请" else "保留当前授权"
                f.confirmPage(title,summary,command,confirmLabel,cancelLabel)
            } else f.confirm(title,summary,command)
        }
    }
    if(record.state == "accepted" && recipient) {
        when(record.data.scope) {
            "adoption_editor" -> FeatureButton("维护该宠物介绍",enabled) { f.open(FeatureRoute(FeaturePage.Collaborators,record.data.targetId)) }
            "caretaker" -> FeatureButton("查看授权照护近况",enabled) { f.open(FeatureRoute(FeaturePage.AdoptionWork,record.data.targetId,"care")) }
        }
    }
    if(record.state == "accepted" && record.data.scope in listOf("group_member","group_request"))
        FeatureButton("进入群聊",enabled) { f.closeRoute(); store.open(MobileDestination.Chat(record.data.targetId,true)) }
    FeatureButton("返回邀请与协作列表",!state.busy) {
        f.returnTo(if(collaboration) FeatureRoute(FeaturePage.Invitations,kind = record.data.scope,peer = record.data.targetId) else FeatureRoute(FeaturePage.Invitations))
    }
}

@Composable
fun InvitationConfirmationPage(store: MobileStore, state: FeatureState) {
    val pending = state.confirmation ?: return
    val f = store.features
    val enabled = !state.busy && !state.loading && !state.conflict && !state.unconfirmedResult &&
        state.pending.none { it.phase in listOf("sent", "result_unknown") }
    val canReturn = !state.busy && !state.loading && !state.unconfirmedResult
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(MobileUi.pagePadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TaskNotice(pending.title,pending.summary)
            state.error?.let { TaskNotice(if(state.unconfirmedResult) "结果待核对" else if(state.conflict) "邀请状态已变化" else "本次操作未完成",it,warning = true) }
            if(state.unconfirmedResult) {
                FeatureButton("核对原操作结果",!state.busy) { f.open(FeatureRoute(FeaturePage.Recovery)) }
                Text("原请求、对象与版本保持；读取状态变化不代表本次操作成功，不会自动重新提交。",style = MaterialTheme.typography.bodySmall)
            }
            if(state.conflict) Text("返回邀请列表重新核对当前权限与版本；本次确认不会自动重试。",style = MaterialTheme.typography.bodySmall)
            MndSecondaryButton(if(state.conflict) { if(state.route?.id == 0L) "返回邀请列表" else "返回当前邀请" } else pending.cancelLabel,enabled = canReturn) {
                f.dismissConfirmation()
                if(state.conflict) f.discardAndReload()
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface) {
            MndSecondaryButton(pending.confirmLabel, enabled = enabled,
                loading = state.busy, modifier = Modifier.fillMaxWidth().padding(16.dp)) { f.submitConfirmation() }
        }
    }
}

@Composable
fun InvitationOutcomePage(store: MobileStore, state: FeatureState, record: MobileRecord<InvitationBody>) {
    val title = if(record.data.scope == "caretaker") "已退出照护协作" else "已退出介绍协作"
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(MobileUi.pagePadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title,style = PageHeadlineStyle)
            TaskNotice("仅结束本次协作权限",invitationActionDetail(record.data.scope,"leave"))
            Text("邀请 #${record.id} · 事项 #${record.data.targetId} · 回执版本 ${record.version}",style = MaterialTheme.typography.bodySmall)
        }
        Surface(color = MaterialTheme.colorScheme.surface) {
            MndPrimaryButton("返回消息",enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().padding(16.dp)) { store.features.finishRoute(); store.select(MobileTab.Messages) }
        }
    }
}
