package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

@Composable fun AppointmentDetailPage(store: MobileStore, s: FeatureState, route: FeatureRoute) {
    val record = s.appointment ?: return
    val f = store.features; val body = record.data; val actor = store.state.value.user?.id
    val held = record.state == "safety_hold"; val ended = record.state in listOf("cancelled","expired")
    val enabled = !s.busy && !s.loading && !s.conflict && !s.unconfirmedResult && actor in listOf(record.ownerId,body.peerId)
    val resultVersion = route.kind.removePrefix("cancelled-").takeIf {route.kind.startsWith("cancelled-")}?.toLongOrNull()
    if(s.conflict) FeatureButton("保留原说明并采用已读取预约版本",!s.busy && !s.loading && !s.unconfirmedResult) {f.adoptAppointmentVersion()}
    if(resultVersion != null && (record.state != "cancelled" || record.version < resultVersion)) {
        TaskNotice("取消结果仍待回读","已取得响应后仍需读取对应记录与版本，不根据旧页面显示取消完成。")
        FeatureButton("重新读取原预约",!s.busy) { f.reload() }
        return
    }
    if(f.field("appointment_stage") == "cancel" && !ended) {
        AppointmentCancelPage(store,s,record)
        return
    }
    if(record.state == "cancelled") {
        TaskNotice("本次预约已取消","本次安排已结束，原申请、会话与必要证据保留。再次见面须重新提议并由双方确认。")
    } else if(held) {
        TaskNotice(if(body.safetyProposal() == null) "先不要按原安排出发" else "新安排等待双方核对确认","安全暂停保持；修改、拒绝、撤回或到期均不能恢复出发。原原因：${body.safetyReason.ifBlank {body.reason}}",warning = true)
    } else if(record.safetyResolved()) {
        TaskNotice("新安排已确认","安全核对说明与双方确认已记录在V${body.effectiveVersion}；再次变化仍须重新核对，不复用旧确认。")
    } else TaskNotice(stateLabel(record.state),"有效安排、待确认提案与历史分别保留；未回复不等于同意。")
    FeatureSection(if(held) "暂停前的安排（不可据此出发）" else "当前有效安排") {
        Text("预约 #${record.id} · 记录版本 ${record.version}")
        body.effectiveTerms?.let { Text("V${body.effectiveVersion}"); AppointmentSummary(it) } ?: Text("双方尚未确认有效安排")
    }
    val pending = body.versions.firstOrNull {it.number == body.pendingVersion && it.status == "pending"}
    fun act(action: String,label: String,restore: Long = 0) {
        val input = AppointmentAction(f.version(record.version),newRequestKey(),restoreVersion = restore,note = f.field("note"))
        f.confirm(label,"预约 #${record.id} · 记录版本 ${record.version}\n有效V${body.effectiveVersion}；待确认V${body.pendingVersion}\n${pending?.resolutionNote.orEmpty()}\n${input.note}") {
            f.api.appointmentAction(record.id,action,input); f.discardAndReload()
        }
    }
    if(pending != null) FeatureSection("待确认新提案 V${pending.number}") {
        AppointmentSummary(pending.terms)
        Text("已确认：${pending.confirmations.joinToString {"用户 #$it"}}")
        if(held) {
            Text("本次更正与核对：${pending.resolutionNote}")
            FormField(f,s,"note","本人对问题、更正和新安排的核对说明",lines = 3)
        }
        if(record.canConfirmAppointment(actor)) FeatureButton("核对并确认V${pending.number}",enabled && (!held || f.field("note").isNotBlank())) {act("confirm","确认此新版本${if(held) "并完成本人安全核对" else ""}")}
        if(!held || pending == body.safetyProposal()) FeatureButton(if(pending.proposedBy == actor) "撤回本次提案" else "拒绝本次提案",enabled) {act(if(pending.proposedBy == actor) "withdraw_change" else "decline",if(held) "结束提案并继续安全暂停" else "结束提案，保留原有效安排")}
    }
    if(!ended) {
        FeatureButton(if(held) "重新核对与协商新安排" else "提出普通改约",enabled) {f.open(FeatureRoute(FeaturePage.AppointmentEdit,record.id,if(held) "safety" else ""))}
        if(!held) {
            FormField(f,s,"note","具体安全暂停原因",lines = 3)
            FeatureButton("因安全原因暂停",enabled && f.field("note").isNotBlank()) {act("safety_hold","安全暂停，通知双方先不要按原安排出发")}
        }
        FeatureButton("取消本次预约",enabled) {f.edit("appointment_stage","cancel")}
    }
    FeatureSection("预约版本记录") {
        body.versions.asReversed().forEach {v ->
            Text("V${v.number} · ${stateLabel(v.status)} · ${displayTime(v.terms.startsAt)} · ${v.terms.place}")
            if(v.resolutionNote.isNotBlank()) Text("安全核对记录：${v.resolutionNote}")
            if(v.status == "replaced" && !ended && !held) FeatureButton("以历史V${v.number}内容发起新提案",enabled) {act("restore","新建提案，双方重新确认；不复活历史确认",v.number)}
        }
    }
    FeatureButton("报告相关问题",!s.busy) {f.open(FeatureRoute(FeaturePage.Safety,record.id,"appointments"))}
    if(body.effectiveVersion > 0 && record.state == "confirmed") AppointmentFeedback(store,s,record)
}

@Composable private fun AppointmentCancelPage(store: MobileStore,s: FeatureState,record: MobileRecord<AppointmentBody>) {
    val f = store.features; val held = record.state == "safety_hold"
    var acknowledged by remember(record.id,s.confirmation?.summary) {mutableStateOf(false)}
    val enabled = !s.busy && !s.loading && !s.conflict && !s.unconfirmedResult
    when {
        s.unconfirmedResult -> {
            TaskNotice("取消结果待确认","查询或退出等待不代表成功。原请求、说明和版本保留；上次读取${if(held) "为安全暂停，继续不出发" else "的安排暂未确认取消"}。",warning = true)
            FeatureButton("查询本次原请求",!s.busy) {f.open(FeatureRoute(FeaturePage.Recovery))}
        }
        s.confirmation != null -> {
            TaskNotice(if(s.error != null) "取消暂未完成" else "确认取消这次预约",if(held) "取消范围仅本次预约；退出或保留仍保持安全暂停，不恢复出发。" else "仅取消本次安排；原申请、会话和证据保留。",warning = held)
            Text(s.confirmation.summary)
            CheckLine("我确认取消本次预约，了解原申请和会话仍保留",acknowledged,{acknowledged = it},enabled)
            TaskPrimary("确认取消本次预约",enabled && acknowledged,f::submitConfirmation)
            TaskSecondary("返回核对原因",!s.busy) {f.dismissConfirmation()}
        }
        else -> {
            TaskNotice("确认取消这次预约",if(held) "当前仍为安全暂停；取消前请继续不出发，取消不会自动判为无故失约。" else "取消本次安排，不结束原申请和会话。",warning = held)
            FormField(f,s,"cancel_reason","取消原因",lines = 3)
            TaskPrimary("核对取消范围",enabled && f.field("cancel_reason").isNotBlank()) {
                val input = AppointmentAction(f.version(record.version),newRequestKey(),note = f.field("cancel_reason"))
                f.confirm("确认取消本次预约","预约 #${record.id} · 记录版本 ${record.version}\n${input.note}\n仅本次安排；保留原申请、会话与必要证据。") {
                    val result = f.api.appointmentAction(record.id,"cancel",input)
                    f.completeTo(FeatureRoute(FeaturePage.Appointment,result.id,"cancelled-${result.version}"))
                }
            }
        }
    }
    if(s.unconfirmedResult) TaskSecondary("查看原预约记录",!s.busy) {f.open(FeatureRoute(FeaturePage.Appointment,record.id,"view"))}
    else TaskSecondary(if(held) "保留预约并继续暂停" else "保留当前预约",!s.busy) {f.dismissConfirmation(); f.edit("appointment_stage","")}
}

@Composable private fun AppointmentFeedback(store: MobileStore,s: FeatureState,record: MobileRecord<AppointmentBody>) {
    val f = store.features
    val ready = record.data.effectiveTerms?.startsAt?.let {runCatching {!futureTime(it)}.getOrDefault(false)} == true
    FeatureSection("到访后逐项反馈") {
        Text(if(ready) "同一有效版本仅提交一份反馈；反馈不代表身份或健康保证。" else "预约时间尚未到，暂不能提交反馈。")
        listOf("identity" to "主体身份","pet" to "宠物信息","reception" to "接待方","place" to "地点","fees" to "费用").forEach {(key,label)->ChoiceField(f,s,"feedback_$key",label,listOf("match" to "一致","mismatch" to "不一致","unknown" to "尚未核实"),"unknown")}
        FormField(f,s,"feedbackNote","到访补充",lines = 3)
        FeatureButton("提交逐项反馈",!s.busy && !s.loading && !s.unconfirmedResult && ready) {
            val input = FeedbackInput(record.id,record.data.effectiveVersion,listOf("identity","pet","reception","place","fees").associateWith {f.field("feedback_$it","unknown")},f.field("feedbackNote"))
            f.confirm("提交本次到访反馈","预约 #${record.id} · 有效V${record.data.effectiveVersion}\n${input.note}") {f.api.feedback(input); f.discardAndReload()}
        }
    }
}
