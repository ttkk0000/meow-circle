package com.ttkk0000.meowcircle.client

// 只决定阶段入口；真实写入资格、授权与完成后的30天窗口仍由服务端逐次校验。
fun AdoptionBody.careStageAvailable(state: String): Boolean {
    val trial = listOf(publisherId,applicantId).all {it in trialConfirmations}
    return state == "trial_active" && completedAt == null && trial || state in listOf("handover_pending","return_requested","return_agreed") && completedAt == null && (trial || applicantId in handoverConfirmations) || state in listOf("completed","return_requested","return_agreed") && completedAt != null
}

fun AdoptionBody.currentContract(): ContractRevision? {
    if(contracts.orEmpty().any {it.status == "pending"}) return null
    return contracts.orEmpty().firstOrNull {it.number == effectiveContract && it.status == "effective" && listOf(publisherId,applicantId).all {party -> party in it.acceptedBy}}
}
fun availableAdoptionActions(record: MobileRecord<AdoptionBody>,actor: Long?): List<Pair<String,String>> {
    val body = record.data; val publisher = actor == body.publisherId
    if(actor == null || actor !in listOf(body.publisherId,body.applicantId)) return emptyList()
    return buildList {
        if(record.state in listOf("submitted","supplemented")) {
            if(publisher) {add("approve" to "同意继续沟通"); add("request_supplement" to "请对方补充材料"); add("reject" to "暂不通过本次申请")}
            else add("withdraw" to "撤回本次申请")
        }
        if(record.state == "needs_info" && !publisher) {add("supplement" to "提交补充材料");add("withdraw" to "撤回本次申请")}
        if(record.state == "withdrawn" && !publisher) add("resubmit" to "核对并重新申请本轮领养")
        if(record.state == "waitlisted") add(if(publisher) "resume_candidate" to "邀请继续核对安排" else "withdraw" to "退出候补")
        if(record.state == "trial_proposed") {
            if(actor !in body.trialConfirmations && body.currentContract() != null) add("confirm_trial" to "核对并确认试养安排")
            add("decline_trial" to "拒绝本次试养提案")
        }
        if(record.state in listOf("approved","trial_active","handover_pending") && actor !in body.handoverConfirmations && body.currentContract() != null) add("confirm_handover" to "确认我方已安全完成交接")
        if(record.state in listOf("trial_active","handover_pending","completed","return_requested","return_agreed")) {add("support" to "遇到照护问题，请求协助");add("request_return" to "协商安全接回")}
        if(!publisher && record.state in listOf("approved","trial_proposed")) add("withdraw" to "撤回本次申请")
    }
}
fun adoptionProgressTitle(record: MobileRecord<AdoptionBody>,actor: Long?): String = when(record.state) {
    "waitlisted" -> "当前为候补"
    "needs_info" -> if(actor == record.data.applicantId) "补充本次申请材料" else "等待申请人补充"
    "trial_proposed" -> if(actor in record.data.trialConfirmations) "等待另一方确认试养" else "确认收到的试养安排"
    "trial_active" -> "试养进行中"
    "handover_pending" -> if(actor in record.data.handoverConfirmations) "等待另一方确认交接" else "核对并确认实际交接"
    "completed" -> "交接已完成"
    "withdrawn" -> "本次申请已撤回"
    "rejected" -> "本次申请暂未通过"
    "return_requested","return_agreed" -> "协商安全接回"
    "returned" -> "本次接回已完成"
    else -> "领养申请进度"
}
