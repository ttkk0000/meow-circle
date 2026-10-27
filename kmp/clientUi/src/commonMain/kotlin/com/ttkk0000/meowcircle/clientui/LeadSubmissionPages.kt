package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

/** 同一份私有草稿切换照片、检查与失败状态，不另建表单或请求。 */
@Composable fun LeadSubmissionPage(store: MobileStore, state: FeatureState, route: FeatureRoute) {
    val f = store.features
    val parent = state.case ?: return
    val actor = store.state.value.user?.id
    val text = f.field("lead")
    val editable = !state.busy && !state.loading && !state.unconfirmedResult && !state.conflict
    val current = parent.state in listOf("active","matched") && actor != null && actor != parent.ownerId
    val prepare = {
        val snapshot = f.state.value
        val source = snapshot.case
        val description = f.field("lead")
        if(source != null && actor != null && source.id == route.id && source.state in listOf("active","matched") && source.ownerId != actor &&
            !snapshot.busy && !snapshot.loading && !snapshot.conflict && !snapshot.unconfirmedResult && description.isNotBlank() && description.length <= 2000 && f.photosReady()) {
            val input = LeadSubmissionInput(f.version(source.version),description,f.mediaIds(),newRequestKey())
            f.confirm("检查私密线索","${source.data.title}\n$description\n仅私密共享，不表示已匹配。") {
                val received = f.api.submitLead(source.id,input)
                f.completeTo(FeatureRoute(FeaturePage.Lead,received.id,"submitted-${received.version}"))
            }
        } else f.notify("请先核对当前求助、原文、照片与原操作结果，输入继续保留")
    }
    when {
        state.error != null -> {
            TaskNotice("输入未清空","未知结果先核对本次提交；保留原请求，不另发副本。",warning = true)
            FeatureButton("回到表单",!state.busy) { f.dismissConfirmation(); f.reload() }
            if(state.unconfirmedResult) TaskPrimary("核对原操作结果",!state.busy) { f.open(FeatureRoute(FeaturePage.Recovery)) }
            else if(state.conflict) TaskPrimary("重新读取最新求助",!state.busy) { f.dismissConfirmation(); f.reload() }
            else TaskPrimary("重试检查摘要",!state.busy) { f.dismissConfirmation(); prepare() }
        }
        state.confirmation != null -> {
            Text("提交给：${parent.data.title} #${parent.id}",fontWeight = FontWeight.Bold)
            TaskNotice("本次共享","$text\n已选照片 ${f.mediaIds().size} 张；仅本案当前发布人与本人可见，手动地点仅以你填写的文字为准。")
            TaskNotice("收到不等于匹配","不移动公共标记，不修改宠物状态；后续仍需核对。")
            FeatureButton("返回修改",!state.busy) { f.dismissConfirmation() }
            TaskPrimary("确认私密提交",editable && current && state.pending.none { it.phase in listOf("sent","result_unknown") }) { f.submitConfirmation() }
        }
        f.field("lead_stage") == "photos" -> {
            val photos = state.photos[route].orEmpty()
            photos.firstOrNull()?.mediaId?.let { id -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                MobilePhoto(store,id,actor,"本次目击照片",Modifier.fillMaxWidth().height(if(maxWidth >= 600.dp) 360.dp else 260.dp).clip(MaterialTheme.shapes.medium))
            } }
            TaskNotice("私密材料","不表示识别成功；仅提供你有权使用的照片，不公开精确位置。")
            FeaturePhotos(store,state,private = true)
            FeatureButton("不附照片继续",editable) { photos.forEach { f.removePhoto(it.key) }; f.edit("lead_stage","edit") }
            TaskPrimary("使用照片并返回",editable && f.photosReady()) { f.edit("lead_stage","edit") }
        }
        else -> {
            Text("提交给：${parent.data.title}",fontWeight = FontWeight.Bold)
            TaskNotice("私密目击描述","写明看到的时间、大致区域与特征。请勿公开电话或门牌，不承诺识别或找回。")
            val legacy = state.forms[FeatureRoute(FeaturePage.CaseWork,route.id)]?.values?.get("lead")
            if(text.isBlank() && !legacy.isNullOrBlank()) {
                TaskNotice("有旧版表单草稿","可主动复制原文字与已上传附件；旧表单可能还包含认领材料，请检查后再提交，原稿继续保留。")
                FeatureButton("读取旧版线索草稿",editable) { f.importLegacyLead() }
            }
            FormField(f,state,"lead","目击描述与手动地点",lines = 5)
            Text("${text.length}/2000 字",style = MaterialTheme.typography.bodySmall,color = if(text.length > 2000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            if(state.conflict) FeatureButton("保留原稿并采用已读取版本",!state.busy && !state.loading && !state.unconfirmedResult && parent.version > f.version()) { f.adoptLeadVersion() }
            FeatureButton("照片说明与选择（${f.mediaIds().size} 张已上传）",editable && current) { f.edit("lead_stage","photos") }
            if(!current) TaskNotice("当前不可新增线索","求助已停止接收，或当前账号是发布人；原稿继续保留。")
            if(state.unconfirmedResult) FeatureButton("核对原操作结果",!state.busy) { f.open(FeatureRoute(FeaturePage.Recovery)) }
            TaskPrimary("检查私密线索",editable && current && text.isNotBlank() && text.length <= 2000 && f.photosReady(),prepare)
        }
    }
}
