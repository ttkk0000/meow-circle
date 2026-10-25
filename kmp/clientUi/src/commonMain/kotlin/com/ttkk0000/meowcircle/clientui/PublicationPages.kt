package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

/** 预览使用服务端核对的同一快照；来源变动后必须重新获取 manifest 并再次主动确认。 */
@Composable fun PublicationPreviewPage(store: MobileStore, state: MobileState) {
    val input = state.publicationConfirmation ?: return
    val scope = "transient:publication:${input.clientRequestId}"
    var origin by rememberSessionUiState("origin", scope, input) { input.declaration.origin }
    var confirmed by rememberSessionUiState("confirmed", scope, input) { false }
    var choosing by rememberSessionUiState("choosing", scope, input) { false }
    var sourceReview by rememberSessionUiState("sourceReview", scope, input) { false }
    var rightsConfirmed by rememberSessionUiState("sourceRights", scope, origin) { false }
    val pending = state.pendingOperations.any { it.command == "entries" && it.clientRequestId == input.clientRequestId && it.phase != "rejected" }
    val enabled = !state.busy && !pending
    CompositionLocalProvider(LocalTaskNavigationEnabled provides !state.busy) {
        TaskColumn {
            when {
                state.publicationSourceRequest != null -> {
                    TaskNotice("来源声明结果待确认", "保留原草稿与本次请求标识；未取得实际回执前，不显示声明已保存或内容已发布。")
                    Text("本次请求：${state.publicationSourceRequest}",style = MaterialTheme.typography.bodySmall)
                    NavigationRow("原请求恢复", "保留原编号和原内容，重试须主动确认") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
                    NavigationRow("了解适用限制", "真实宠物材料需单独核对") { store.features.open(FeatureRoute(FeaturePage.Safety)) }
                    TaskPrimary("查询本次结果",!state.busy,store::queryPublicationSource)
                }
                state.publicationSourceResult && state.publicationSourceRecord != null -> {
                    val record = state.publicationSourceRecord
                    val matches = record.data.manifestHash == input.declaration.manifestHash && record.data.origin == input.declaration.origin
                    Text("来源声明已记录",style = MaterialTheme.typography.titleLarge,fontWeight = FontWeight.Bold)
                    TaskNotice("实际记录已返回", "来源声明已保存，内容尚未发布；这不代表素材权利或真实性已通过平台核验。")
                    TaskSection("本次记录") {
                        Text("声明 #${record.id} · ${displayTime(record.createdAt)}")
                        Text("来源：${originLabel(record.data.origin)}")
                        Text(if (matches) input.entry.title.ifBlank { "当前发布草稿" } else "当前内容尚未与本记录匹配，须重新核对来源")
                        Text("下一步：检查受众、隐私和发布资格")
                    }
                    NavigationRow("更正来源声明", "产生新记录，不覆盖已有记录") { store.hidePublicationSourceResult(); choosing = true; sourceReview = false; rightsConfirmed = false; confirmed = false }
                    TaskPrimary("回到发布预览",!state.busy,store::hidePublicationSourceResult)
                }
                pending -> {
                    TaskNotice("先不要重复提交", "原草稿与本次请求标识已保留。未取得实际回执前，不显示内容已发布。")
                    NavigationRow("核对本次结果", "通过原请求标识回源，不创建新发布") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
                    TaskPrimary("查看原操作结果", !state.busy) { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
                }
                state.publicationIssue != null -> {
                    TaskNotice("素材使用范围待核对", "材料已有 AI 来源标识，不能声明为原创。请保留原文件、重新说明来源；本次拒绝不代表草稿已发布。", warning = true)
                    NavigationRow("重新选择并说明", "不自动清除素材来源标识") { store.clearPublicationIssue(); confirmed = false; choosing = true; sourceReview = false }
                    NavigationRow("提出异议或求助", "通过安全报告补充必要说明") { store.features.open(FeatureRoute(FeaturePage.Safety)) }
                    TaskPrimary("返回草稿", !state.busy, store::dismissPublication)
                }
                choosing -> {
                    if (!sourceReview) {
                        Text("声明内容来源", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TaskNotice("帮助大家理解图片来源", "选择与你的文字、图片实际情况一致的声明；普通裁切调色不自动视为 AI 生成。")
                        NavigationRow("包含 AI 生成或合成内容", "下一步核对展示与使用范围") { origin = "ai_generated"; rightsConfirmed = false; sourceReview = true }
                        NavigationRow("未使用 AI 生成或合成内容", "确认图片来源与使用权") { origin = "original"; rightsConfirmed = false; sourceReview = true }
                        TaskNotice("隐私与标识分别处理", "照片中的 GPS 等隐私信息与适用的 AI 来源标识分别处理；保留原文件，不清除已有的来源依据。")
                        TaskPrimary("暂不选择，返回预览", enabled) { choosing = false }
                    } else {
                        val ai = origin in listOf("ai_generated","ai_modified")
                        Text(if (ai) "AI 内容声明确认" else "普通内容来源确认", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TaskSection("本次内容") {
                            Text(input.entry.title.ifBlank { "当前发布草稿" }); Text(input.entry.text)
                            Text("来源：${originLabel(origin)}")
                            if (ai) {
                                SegmentedTextBar(listOf("ai_generated" to "AI 生成","ai_modified" to "AI 修改"),origin,{ origin = it; rightsConfirmed = false },enabled)
                                Text("公开展示：内容周边显示相应 AI 提示")
                            }
                        }
                        TaskNotice(if (ai) "真实宠物材料另有要求" else "识别存在差异时", if (ai) "明确标注的 AI 示意内容不能替代待领养宠物的近期实拍、来源或医疗证明。" else "发现已有生成标识时会提示核对与求助，不直接认定故意造假。")
                        CheckLine("我确认声明与本次内容相符，并拥有必要素材权利",rightsConfirmed,{ rightsConfirmed = it },enabled)
                        NavigationRow("修改来源选择", "返回后保留草稿") { sourceReview = false; rightsConfirmed = false }
                        Text("保存声明后仍需在预览页核对受众并主动确认发布。",style = MaterialTheme.typography.bodySmall)
                        TaskPrimary("提交本次声明",enabled && rightsConfirmed) { store.recheckPublicationSource(origin,rightsConfirmed); confirmed = false; choosing = false }
                    }
                }
                else -> {
                    val user = state.user
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        avatarMediaId(user?.avatarUrl.orEmpty())?.let { MobilePhoto(store,it,user?.id,"当前发布者头像",Modifier.size(48.dp).clip(CircleShape)) }
                        Column {
                            Text(user?.let { it.nickname.ifBlank { it.username } }.orEmpty(), fontWeight = FontWeight.Bold)
                            Text(if (input.entry.petId > 0) "关联宠物 #${input.entry.petId}" else "${input.entry.space.let { if (it == "diary") "宠物日记" else if (it == "circle") "宠友圈" else "社区日常" }}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    input.mediaIds.firstOrNull()?.let { media -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                        MobilePhoto(store,media,user?.id,"本次发布封面",Modifier.fillMaxWidth().height(if (maxWidth >= 600.dp) 360.dp else 240.dp).clip(MaterialTheme.shapes.medium))
                    } }
                    if (input.mediaIds.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(input.mediaIds.size - 1) { index -> MobilePhoto(store,input.mediaIds[index + 1],user?.id,"发布照片 ${index + 2}",Modifier.size(96.dp).clip(MaterialTheme.shapes.medium)) }
                    }
                    NavigationRow("内容来源与 AI 声明", originLabel(origin)) { confirmed = false; rightsConfirmed = false; sourceReview = false; choosing = true }
                    if (input.entry.title.isNotBlank()) Text(input.entry.title,style = MaterialTheme.typography.titleLarge,fontWeight = FontWeight.Bold)
                    Text(input.entry.text,style = MaterialTheme.typography.bodyLarge)
                    Text("${audienceLabel(input.entry.audience.mode)} · ${input.mediaIds.size} 张照片",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (input.entry.audience.userIds.isNotEmpty()) Text("受众名单：${input.entry.audience.userIds.joinToString { "#$it" }}",style = MaterialTheme.typography.bodySmall)
                    if (input.entry.city.isNotBlank()) Text("城市：${input.entry.city}",style = MaterialTheme.typography.bodySmall)
                    CheckLine("我已核对当前内容、照片、受众和来源声明",confirmed,{ confirmed = it },enabled)
                    Text("来源变更会使本次确认失效。已识别的 AI 材料不能声明为原创。",style = MaterialTheme.typography.labelSmall)
                    TextButton(store::dismissPublication,enabled = enabled) { Text("返回修改，保留草稿") }
                    TaskPrimary(if (origin == input.declaration.origin) "确认发布" else "更新来源后重新核对",enabled && confirmed) { store.confirmPublication(origin,confirmed) }
                }
            }
        }
    }
}
