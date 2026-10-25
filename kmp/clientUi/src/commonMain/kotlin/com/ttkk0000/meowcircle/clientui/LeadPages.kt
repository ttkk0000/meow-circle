package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

@Composable fun LeadInboxPage(store: MobileStore, state: FeatureState, route: FeatureRoute) {
    val f = store.features
    val empty = state.leads.loaded && !state.leads.loading && !state.loading && state.leads.error == null && state.error == null && state.leads.items.isEmpty() && state.leads.next == 0L
    if(empty) HonestStateCard("暂无新的目击线索","可补照片、检查区域或主动分享，不能保证有人响应。")
    else {
    TaskNotice("仅本案授权人员可见", "不公开目击者原图、位置和联系信息。列表只显示当前账号有权读取的线索。")
    state.leads.items.forEach { lead -> NavigationRow("一位宠友 · ${displayTime(lead.createdAt)}", "${leadAssessmentLabel(lead.data.assessment)} · ${lead.data.text.take(100)}",compact = true) {
        f.open(FeatureRoute(FeaturePage.Lead,lead.id))
    } }
    PageFooter(state.leads,{f.reload()},{f.reload(true)},"暂无你有权查看的新线索；不会生成示例目击或热度。",enabled = !state.busy && !state.loading)
    }
    TaskSecondary("返回本次维护",!state.busy) { f.open(FeatureRoute(FeaturePage.CaseWork,route.id)) }
}

@Composable fun LeadDetailPage(store: MobileStore, state: FeatureState, route: FeatureRoute) {
    val lead = state.lead ?: return
    val parent = state.case ?: return
    val actor = store.state.value.user?.id
    val f = store.features
    val assessment = leadAssessment(lead.data.assessment)
    val minimum = leadReviewMinimumVersion(route.kind)
    val result = minimum != null && leadAssessmentRecorded(lead,parent,minimum)
    if (result) {
        TaskNotice(leadAssessmentLabel(lead.data.assessment),leadAssessmentSummary(lead.data.assessment),success = assessment == LeadAssessment.Helpful)
        Text("记录时间：${displayTime(lead.data.reviewedAt.orEmpty())}",style = MaterialTheme.typography.bodySmall)
        FeatureButton("继续核实这条线索",!state.busy) { f.open(FeatureRoute(FeaturePage.Lead,lead.id)) }
        TaskSecondary("返回本次维护",!state.busy) { f.open(FeatureRoute(FeaturePage.CaseWork,parent.id)) }
        return
    }
    Text("${parent.data.title} · 私密线索 #${lead.id}",fontWeight = FontWeight.Bold)
    lead.mediaIds.forEachIndexed { index,id -> BoxWithConstraints(Modifier.fillMaxWidth()) {
        val photoHeight = if(maxWidth >= 600.dp) 360.dp else 170.dp
        MobilePhoto(store,id,actor,"目击者提供的私密照片 ${index+1}",Modifier.fillMaxWidth().height(photoHeight).clip(MaterialTheme.shapes.medium))
    } }
    Text(lead.data.text,style = MaterialTheme.typography.bodyLarge)
    Text("收到于 ${displayTime(lead.createdAt)}",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
    TaskNotice(leadAssessmentLabel(lead.data.assessment),leadAssessmentSummary(lead.data.assessment))
    if(minimum != null && lead.version < minimum) TaskNotice("当前结果尚未回读","已收到保存回执，重新读取后再查看当前标记；不会重复提交原请求。")
    if (canAssessLead(lead,parent,actor)) {
        listOf("helpful" to "标记有帮助","not_match" to "标记不匹配").forEach { (value,label) ->
            val enabled = !state.busy && !state.loading && !state.conflict && !state.unconfirmedResult && lead.data.assessment != value
            val submit = {
                val input = LeadAssessmentInput(f.version(lead.version),newRequestKey(),value)
                f.confirm(label,"仅保存本条线索的处理标记；${leadAssessmentSummary(value)}") {
                    val recorded = f.api.assessLead(lead.id,input)
                    f.completeTo(FeatureRoute(FeaturePage.Lead,lead.id,"reviewed-${recorded.version}"))
                }
            }
            if(value == "helpful") FeatureButton(label,enabled,submit)
            else TextButton(onClick = submit,enabled = enabled,modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label,Modifier.fillMaxWidth()) }

        }
    }
    FeatureButton("报告索费或骚扰",!state.busy) { f.open(FeatureRoute(FeaturePage.Safety,kind = "cases",peer = parent.id)); f.notify("请说明私密线索 #${lead.id} 的情况与希望处理的问题") }
    FeatureButton("返回线索列表",!state.busy) { f.open(FeatureRoute(FeaturePage.LeadInbox,parent.id)) }
    if (canContactLead(lead,parent,actor)) TaskPrimary(if(actor == lead.ownerId) "联系本案发布人" else "站内联系目击者",!state.busy && !state.loading && !state.conflict && !state.unconfirmedResult) {
        val input = LeadContactInput(f.version(lead.version),newRequestKey())
        f.confirm("发起本条线索的站内联系","会话仅绑定本条私密线索。对方接受前限一条文字；请勿付费换线索。") {
            val conversation = f.api.contactLead(lead.id,input)
            f.finishRoute(); store.open(MobileDestination.Chat(conversation.id))
        }
    } else if (parent.state !in listOf("active","matched")) Text("本次求助已停止接收新联系；仍可核对你有权读取的线索历史。",style = MaterialTheme.typography.bodySmall)
}

/** 线索会话的真实求助摘要，只展示粗略区域。 */
@Composable fun LeadCaseContext(store: MobileStore, parent: MobileRecord<CaseBody>) {
    TaskSection("关联求助") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),verticalAlignment = Alignment.CenterVertically) {
            parent.mediaIds.firstOrNull()?.let { MobilePhoto(store,it,store.state.value.user?.id,"关联求助照片",Modifier.size(64.dp).clip(MaterialTheme.shapes.medium)) }
            Column { Text(parent.data.title,fontWeight = FontWeight.Bold);Text("${parent.data.city} · ${parent.data.area}",style = MaterialTheme.typography.bodySmall) }
        }
        Text("本次求助的文字区域，不代表实时位置。",style = MaterialTheme.typography.bodySmall)
        FeatureButton("查看求助") { store.features.open(FeatureRoute(FeaturePage.CaseWork,parent.id)) }
    }
}
