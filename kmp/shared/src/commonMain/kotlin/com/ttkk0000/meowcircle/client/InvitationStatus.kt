package com.ttkk0000.meowcircle.client

import kotlin.time.Clock
import kotlin.time.Instant

// 仅提供当前角色的界面入口；真实资源归属、阶段与资格由服务端逐次复核。
fun availableInvitationActions(record: MobileRecord<InvitationBody>, actor: Long?, now: Instant = Clock.System.now()): List<Pair<String,String>> {
    if(actor == null || actor <= 0) return emptyList()
    val body = record.data
    return buildList {
        if(record.state == "pending") {
            if(actor == body.recipientId) {
                val future = runCatching { Instant.parse(body.expiresAt) > now }.getOrDefault(false)
                if(future && body.scope in listOf("adoption_editor","caretaker","group_member","group_request")) add("accept" to "接受")
                add("decline" to "拒绝")
            } else if(actor == record.ownerId) add("revoke" to "撤销邀请")
        }
        if(record.state == "accepted") {
            if(actor == body.recipientId && body.scope in listOf("adoption_editor","caretaker")) add("leave" to "退出协作")
            if(actor == record.ownerId && body.scope in listOf("adoption_editor","caretaker","group_member")) add("revoke" to "撤销授权")
        }
    }
}
