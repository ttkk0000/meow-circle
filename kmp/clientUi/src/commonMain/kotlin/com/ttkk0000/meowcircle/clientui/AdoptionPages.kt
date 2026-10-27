package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

@Composable fun AdoptionProgressPage(store: MobileStore,s: FeatureState,record: MobileRecord<AdoptionBody>) {
    val f=store.features; val body=record.data; val actor=store.state.value.user?.id
    val contract=body.currentContract(); val party=actor in listOf(body.publisherId,body.applicantId)
    val enabled=party && !s.busy && !s.loading && !s.conflict && !s.unconfirmedResult
    var acknowledged by remember(record.id,record.version) {mutableStateOf(false)}
    if(s.conflict || record.version > f.version(record.version)) FeatureButton("保留原说明并采用已读领养版本",!s.busy && !s.loading && !s.unconfirmedResult) {f.adoptAdoptionVersion()}
    if(s.commandErrorCode == "pet_placement_conflict") TaskNotice("试养状态已变化","本次安排未能确认，输入仍保留。先读取最新申请状态，不沿用旧按钮继续交接，也不展示其它家庭材料。",warning=true)
    TaskNotice(adoptionProgressTitle(record,actor),when(record.state) {
        "waitlisted" -> "申请保留为候补，不是已被拒绝；不会自动接受新条件，不披露其它家庭或申请材料。"
        "trial_proposed" -> "双方确认同一试养安排后再开始；本次确认不代替另一方。"
        "handover_pending" -> "只确认我方实际交接，双方分别确认后才进入完成状态。"
        "completed" -> "30天私密照护更新仅向本次送养人及获授权照护者共享；求助与接回入口独立保留。"
        else -> "仅当前申请双方可见；不要求无关证件或完整家庭地址。"
    })
    FeatureSection("本次申请") {
        Text("申请 #${record.id} · 宠物 #${body.petId} · 第${body.adoptionCycle}轮 · 版本${record.version}")
        Text(body.message)
        if(body.supplement.isNotBlank()) Text("补充资料：${body.supplement}")
        if(body.note.isNotBlank()) Text("最新说明：${body.note}")
    }
    if(record.state in listOf("trial_proposed","trial_active")) FeatureSection("本次试养安排") {
        Text("截止：${body.trialUntil?.let(::displayTime) ?: "待核对"}")
        Text("照护：${body.trialTerms}")
        contract?.let {Text("生效协议V${it.number} · 费用${money(it.terms.amountCents)} · ${it.terms.payee}");Text("退出与安全接回：${it.terms.returnTerms}")}
        Text("本人：${if(actor in body.trialConfirmations) "已确认" else "待确认"}；对方：${if(listOf(body.publisherId,body.applicantId).filter {it != actor}.all {it in body.trialConfirmations}) "已确认" else "待确认"}")
    }
    val actions=availableAdoptionActions(record,actor)
    if(actions.isNotEmpty()) {
        FormField(f,s,"adoptionNote","本次必要说明／补充材料",lines=3)
        if(actions.any {it.first in listOf("confirm_trial","confirm_handover")}) CheckLine("我已核对照护责任、退出安排或本人实际交接情况，不替对方确认",acknowledged,{acknowledged=it},enabled)
        actions.forEach {(action,label)->
            val specific=action in listOf("supplement","request_supplement","support","request_return","resubmit")
            FeatureButton(label,enabled && (!specific || f.field("adoptionNote").isNotBlank()) && (action !in listOf("confirm_trial","confirm_handover") || acknowledged) && (action != "confirm_trial" || body.trialUntil?.let(::futureTime) == true)) {
                val input=AdoptionActionInput(f.version(record.version),f.field("adoptionNote"),clientRequestId=newRequestKey())
                f.confirm(label,"申请 #${record.id} · 当前版本${record.version}\n${input.note}\n${if(action == "confirm_trial") "试养截止：${body.trialUntil.orEmpty()}\n${body.trialTerms}" else "只处理本次申请，不替对方同意或确认交接。"}\n${if(action in listOf("confirm_trial","confirm_handover")) contract?.terms?.let(::contractReview).orEmpty() else ""}") {
                    f.api.adoptionAction(record.id,action,input); f.discardAndReload(); store.reloadDetail()
                }
            }
        }
    }
    if(record.state in listOf("approved","trial_proposed")) {
        if(contract == null) TaskNotice("先核对同一生效协议","双方尚未接受当前同一协议，或有新协议提案待处理。不能据此开始试养或交接。")
        else if(!contract.terms.trialUntil.isNullOrBlank()) {
            FeatureSection("按生效协议提出试养") {Text("协议V${contract.number} · 截止${displayTime(contract.terms.trialUntil!!)}");Text(contract.terms.responsibilities);Text("退出安排：${contract.terms.returnTerms}")}
            FeatureButton("核对并提出协议内的试养安排",enabled && futureTime(contract.terms.trialUntil!!)) {
                val input=AdoptionActionInput(f.version(record.version),contract.terms.responsibilities,contract.terms.trialUntil,newRequestKey())
                f.confirm("核对试养安排","生效协议V${contract.number}\n${contractReview(contract.terms)}\n双方确认前尚未开始试养。") {f.api.adoptionAction(record.id,"propose_trial",input);f.discardAndReload();store.reloadDetail()}
            }
        } else Text("本协议未约定试养截止；需要试养时先提出协议新版本，并由双方接受。")
    }
}

@Composable fun AdoptionConfirmationPage(store: MobileStore,s: FeatureState) {
    val f=store.features; val pending=s.confirmation ?: return
    var acknowledged by remember(pending.summary) {mutableStateOf(false)}
    if(s.commandErrorCode == "pet_placement_conflict") TaskNotice("未能确认本次安排","当前宠物已有其它有效安排。原说明继续保留，请返回读取最新状态，不把本次处理显示为完成。",warning=true)
    if(s.unconfirmedResult) {
        TaskNotice("本次处理结果待核对","原请求、说明与基准版本保留。当前记录变化不能替代原请求回执，不重建或自动重发。",warning=true)
        TaskPrimary("查询本次原请求",!s.busy) {f.open(FeatureRoute(FeaturePage.Recovery))}
    } else {
        FeatureSection(pending.title) {Text(pending.summary)}
        CheckLine("我已核对本次事项、版本与上述内容，只确认我方行为",acknowledged,{acknowledged=it},!s.busy && !s.conflict)
        TaskPrimary("确认并提交",acknowledged && !s.busy && !s.loading && !s.conflict && s.commandErrorCode != "pet_placement_conflict",f::submitConfirmation)
        TaskSecondary("返回原事项继续核对",!s.busy) {f.dismissConfirmation();f.reload()}
    }
}

fun returnPlanReview(plan: ReturnPlan,body: AdoptionBody): String {
    fun summary(value: ReturnPlan): String = "R${value.number} · ${stateLabel(value.state)}\n时间／确认截止：${displayTime(value.terms.startsAt)}\n地点：${value.terms.place}\n接回人：${value.terms.reception}\n接回前照护：${value.terms.careUntil}\n费用：${value.terms.costs}\n安全与异常联络：${value.terms.safety}"
    val previous=body.returnPlans.orEmpty().firstOrNull {it.number == body.effectiveReturn && it.state == "effective" && it.number != plan.number}
    return (previous?.let {"原有效安排仍保留\n${summary(it)}\n\n"} ?: "") + "本次核对\n${summary(plan)}\n\n只核对本次方案，不复用其它版本的确认；接受方案与实际交接分别记录，单方确认不表示接回完成。"
}
