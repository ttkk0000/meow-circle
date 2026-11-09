package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

@Composable internal fun AuthRecoveryTask(store: MobileStore) {
    val controller = store.authRecovery
    val state by controller.state.collectAsState()
    val receipt = state.receipt
    MobileTaskDialog(store::closeAuthRecovery, title = if (receipt == null) "恢复账号访问" else "恢复申请已收到", navigationEnabled = !state.busy) {
        if (receipt == null) {
            MndOutlinedField(state.account, { controller.edit(account = it) }, label = { Text("原账号") },
                placeholder = { Text("手机号或M&D账号") }, singleLine = true, enabled = !state.busy && !state.unknown)
            MndOutlinedField(state.contact, { controller.edit(contact = it) }, label = { Text("可联系你的方式") },
                placeholder = { Text("仅用于本次恢复") }, singleLine = true, enabled = !state.busy && !state.unknown)
            MndOutlinedField(state.reason, { controller.edit(reason = it) }, label = { Text("情况说明") },
                placeholder = { Text("简要说明原号码或登录方式为什么不可用") }, enabled = !state.busy && !state.unknown)
            TaskNotice("保护你的资料", "不会因相同昵称、头像或邮箱直接合并账号。申请只进入协助核对，不直接修改原账号或登录方式。")
            if (state.unknown) {
                TaskNotice("原申请结果待核对", "本机已保留受限查询入口；离开后也可在待核对事项继续查询。")
                FeaturePrimary("读取原回执", !state.busy, controller::readReceipt)
            } else FeaturePrimary("提交恢复申请", !state.busy && state.account.isNotBlank() && state.contact.isNotBlank() && state.reason.isNotBlank(), controller::submit)
        } else {
            MndScreenTitle("请保留回执")
            val progress = mapOf("submitted" to "待核对", "in_review" to "核对中", "needs_info" to "待补充", "accepted" to "已受理", "rejected" to "未受理", "closed" to "已结束")[receipt.state] ?: "进度待核对"
            AuthFacts(listOf("回执" to "#${receipt.id}", "状态" to progress))
            TaskNotice("下一步", "可在此补充必要信息。处理前仍需核对原账号，原资料不会被新账号覆盖；收到回执不代表已恢复访问。")
            NavigationRow(!state.busy, "补充恢复信息", "只用于本次申请", 56.dp, store::supplementAuthRecovery)
            FeaturePrimary("继续公开浏览", !state.busy, store::exitAuthRecovery)
        }
        state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
