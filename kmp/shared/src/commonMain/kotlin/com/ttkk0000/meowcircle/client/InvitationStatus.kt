package com.ttkk0000.meowcircle.client

import kotlin.time.Clock
import kotlin.time.Instant

fun invitationActionTitle(scope: String, action: String): String = when(action) {
    "leave" -> { if(scope == "caretaker") "退出本次照护协作" else "退出本次介绍协作" }
    "revoke" -> { when(scope) { "caretaker" -> "撤销本次照护授权"; "adoption_editor" -> "结束本次介绍协作"; else -> "撤销本次邀请或授权" } }
    "accept" -> "接受本次邀请"
    "decline" -> "拒绝本次邀请"
    else -> "核对本次邀请"
}

fun invitationActionDetail(scope: String, action: String): String = when {
    action == "leave" && scope == "caretaker" -> "退出后，你将不能再通过本次授权查看或记录私密照护近况。已有记录保留，原领养、试养和交接事项继续；退出协作不代表完成交接或接回。"
    action == "leave" -> "退出后，你将不能再通过本次授权维护该宠物的介绍与照片。已有记录和原领养事项保留，宠物归属不变；公开资料仍按原可见范围展示。"
    scope == "caretaker" && action == "accept" -> "仅获得本次事项的私密照护近况权限；申请材料、协议与完整宠物档案不会开放，也不能代替双方确认试养、交接或接回。"
    scope == "adoption_editor" && action == "accept" -> "仅维护该宠物的领养介绍与照片，不获得领养决定、私密申请材料、协议或宠物归属权限。"
    action == "revoke" -> "仅撤销这份邀请或已接受授权。已有记录及原事项保留，不取消领养、试养、预约或交接。"
    else -> "仅处理这份邀请；接受截止只限制尚未接受的邀请，当前权限仍需重新核对。"
}

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
