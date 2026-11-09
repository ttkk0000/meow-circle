package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

@Composable fun AuthProfileTask(store: MobileStore) {
    val controller = store.profileSetup
    val s by controller.state.collectAsState()
    val user = s.profile?.user ?: store.state.value.user ?: return
    val ready = s.profile != null && !s.loading && !s.busy && !s.unknown
    val platform = LocalClientPlatform.current
    MobileTaskDialog(store::closeOptionalProfile, title = "设置你的资料", navigationEnabled = !s.blocked) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                if (s.photo != null) LocalPhotoPreview(s.photoKey, s.owner, "所选头像预览", Modifier.size(56.dp)) { key ->
                    controller.state.value.takeIf { it.owner == store.api.user.value?.id && it.photoKey == key }?.photo
                } else MobilePhoto(store, avatarMediaId(user.avatarUrl.orEmpty()), s.owner, "当前账号头像", Modifier.size(56.dp).clip(MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.nickname.ifBlank { user.nickname.ifBlank { user.username } }, style = MaterialTheme.typography.titleMedium)
                    Text("头像与昵称可随时修改", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        MndOutlinedField(s.nickname, controller::editNickname, label = { Text("昵称") }, enabled = ready && !s.picking, singleLine = true)
        platform.PhotoPicker(1, ready, { photos -> photos.firstOrNull()?.let(controller::selectPhoto) }, controller::pickerFailed) { enabled, launch ->
            SideEffect { controller.pickerBusy(ready && !enabled) }
            NavigationRow(enabled, "选择头像", "只读取你主动选择的照片", minHeight = 56.dp, onClick = launch)
        }
        if (s.photo != null) Text("所选头像将在你保存时上传。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MndQuietButton("稍后再说", !s.blocked, onClick = store::closeOptionalProfile)
        s.error?.let { TaskNotice(if (s.unknown) "保存结果待核对" else "资料尚未确认", it) }
        if (s.unknown) Text("稍后再说只退出设置，不撤销已经发出的保存；之后可在账号设置核对。", style = MaterialTheme.typography.bodySmall)
        if (s.profile == null || s.needsReload || s.unknown) MndSecondaryButton(
            if (s.loading) "正在读取资料" else if (s.unknown) "核对刚才的保存结果" else "重新读取当前资料",
            !s.loading && !s.blocked, onClick = controller::refresh)
        if (s.loading || s.blocked) LinearProgressIndicator(Modifier.fillMaxWidth())
        FeaturePrimary(if (s.busy) "正在保存" else if (s.picking) "正在准备头像" else "保存并继续", ready && !s.picking && !s.needsReload && s.nickname.isNotBlank(), onClick = controller::save)
    }
}
