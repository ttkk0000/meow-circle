package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.ttkk0000.meowcircle.client.*

fun readingStatus(status: String) = when(status) { "published" -> "已发布文本";"archived" -> "历史只读文本";"proposed" -> "正在公示，未默认同意";"reference" -> "内容说明";else -> "草案预览，尚未正式生效" }
private val dataReadingTypes=setOf("privacy","inventory","sdk","providers","retention")
fun featureTitle(s: FeatureState): String {
    val route=s.route ?: return ""
    return when(route.page) {
        FeaturePage.LeadInbox -> if(s.leads.loaded && !s.leads.loading && !s.loading && s.leads.error == null && s.error == null && s.leads.items.isEmpty() && s.leads.next == 0L) "还没有新线索" else s.case?.data?.title?.let { "${it}的线索" } ?: "私密线索"
        FeaturePage.Lead -> if(leadReviewMinimumVersion(route.kind)?.let { leadAssessmentRecorded(s.lead,s.case,it) } == true) "线索处理已记录" else route.page.title
        FeaturePage.Reading -> s.readingDocument?.title ?: if(route.kind=="care-first-cat") "养宠指南" else "阅读说明"
        FeaturePage.Rules -> when(route.kind) { "history" -> "规则历史与下载";"changes" -> "规则变更与退出";"appeal" -> "处理依据与人工申诉";else -> "平台规则中心" }
        FeaturePage.Guardians -> if(route.kind=="protection") "未成年人保护设置" else s.guardians.firstOrNull{it.id==route.id}?.let{guardian -> when {
            guardian.state=="withdrawn" || !futureTime(guardian.data.validUntil) -> "授权已撤回／失效"
            guardian.state=="verified_pending_consent" -> "监护人授权确认"
            else -> "监护授权与管理"
        }} ?: "监护授权与管理"
        else -> route.page.title
    }
}

@Composable fun ReadingContent(store: MobileStore,s: FeatureState) {
    val f=store.features
    val document=s.readingDocument ?: return
    val platform=LocalClientPlatform.current
    if(document.coverAsset=="first-cat") {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val photoSize=if(maxWidth >= 600.dp) Modifier.height(360.dp) else Modifier.aspectRatio(358f/240f)
            BundledGuideImage("files/first-cat.jpg","内容配图，非用户档案",modifier=Modifier.fillMaxWidth().then(photoSize).clip(MaterialTheme.shapes.medium),contentScale=ContentScale.Crop)
        }
    }
    if(document.type!="guide") TaskNotice(readingStatus(document.status),document.notice.ifBlank { "查看适用版本与时间后，再决定是否继续。阅读不会自动同意新规则。" })
    else if(document.notice.isNotBlank()) Text(document.notice,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    SelectionContainer { Column(verticalArrangement=Arrangement.spacedBy(MobileUi.contentGap)) {
        if(document.heading.isNotBlank()) Text(document.heading,style=if(document.type=="guide") MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium.copy(fontSize=28.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold))
        if(document.facts.isNotEmpty()) MndSection("") { document.facts.forEach { fact -> Column(verticalArrangement=Arrangement.spacedBy(6.dp)) { Text(fact.label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(fact.text,style=MaterialTheme.typography.bodyLarge) } } }
        document.paragraphs.forEach { Text(it,style=MaterialTheme.typography.bodyLarge) }
        document.changes.forEach { Text(it,style=MaterialTheme.typography.bodyLarge) }
        if(document.type!="guide") { Text("版本：${document.revision}",style=MaterialTheme.typography.bodySmall);if(document.effectiveFrom.isNotBlank()) Text("生效时间：${displayTime(document.effectiveFrom)}");if(document.effectiveUntil.isNotBlank()) Text("适用截止：${displayTime(document.effectiveUntil)}") }
    } }
    if(document.type=="guide") {
        FeatureButton("回到社区交流") {f.closeRoute();store.select(MobileTab.Community)}
        FeaturePrimary("看看大家的日常") {f.closeRoute();store.select(MobileTab.Community)}
    } else {
        NavigationRow("下载完整文本",document.revision) { if(!platform.openLink(store.api.readingDownloadURL(document.key))) f.notify("当前设备暂不能打开下载，可继续阅读原文") }
        when(document.type) {
            "teen" -> { NavigationRow("未满14岁的监护授权","查看范围与撤回") {f.open(FeatureRoute(FeaturePage.Security))};NavigationRow("年龄／监护有误","申请复核") {f.open(FeatureRoute(FeaturePage.Safety))};NavigationRow("个人资料权利","访问、更正、删除与投诉") {f.open(FeatureRoute(FeaturePage.DataRequests))} }
            "seller" -> { NavigationRow("查看合同与售后","费用、责任人与有效版本") {f.open(FeatureRoute(FeaturePage.Appointments))};NavigationRow("查看隐私与权利渠道") {f.open(FeatureRoute(FeaturePage.Safety))} }
            else -> Unit
        }
        if(document.type=="privacy") {
            NavigationRow("个人信息处理清单","用途、处理方与保存期限") {f.open(FeatureRoute(FeaturePage.Reading,kind="type-inventory"))}
            NavigationRow("访问／更正／撤回／删除","查看真实请求流程") {f.open(FeatureRoute(FeaturePage.DataRequests))}
            NavigationRow("未成年人专门规则","监护授权与保护设置") {f.open(FeatureRoute(FeaturePage.Reading,kind="type-teen"))}
            NavigationRow("第三方与数据去向","逐项查看用途与处理范围") {f.open(FeatureRoute(FeaturePage.Reading,kind="type-providers"))}
        }
        if(document.type in dataReadingTypes) {
            NavigationRow("查看与撤回授权","必要用途及影响分项说明") {f.open(FeatureRoute(FeaturePage.Security))}
            if(document.type!="sdk") NavigationRow("第三方与公开联系") {f.open(FeatureRoute(FeaturePage.Reading,kind="type-sdk"))}
            if(document.type!="retention") NavigationRow("必要留存与复核") {f.open(FeatureRoute(FeaturePage.Reading,kind="type-retention"))}
            NavigationRow("申请删除、解释或更正范围") {f.open(FeatureRoute(FeaturePage.DataRequests))}
            NavigationRow("隐私投诉","无需注册") {f.open(FeatureRoute(FeaturePage.Safety));f.edit("type","privacy")}
        }
        NavigationRow("完整规则与历史版本","支持检索与下载") {f.open(FeatureRoute(FeaturePage.Rules,kind="history"))}
        NavigationRow("表达意见或申诉","保留原结论与事项") {f.open(FeatureRoute(FeaturePage.Rules,kind="appeal"))}
        when(document.type) {
            "service" -> FeaturePrimary("返回登录") {store.showLogin()}
            "privacy" -> FeaturePrimary("返回设置") {f.returnTo(FeatureRoute(FeaturePage.SettingsHome))}
            in dataReadingTypes -> FeaturePrimary("返回隐私与数据") {f.returnTo(FeatureRoute(FeaturePage.Reading,kind="type-privacy"))}
            else -> FeaturePrimary("返回规则中心") {f.returnTo(FeatureRoute(FeaturePage.Rules))}
        }
    }
}

@Composable fun SettingsHomeContent(store: MobileStore) {
    val f=store.features
    NavigationRow("账号与登录","关联、换绑、恢复与退出") {f.open(FeatureRoute(FeaturePage.Security))}
    NavigationRow("通知偏好","私信、群聊、申请与附近提醒") {f.open(FeatureRoute(FeaturePage.Settings))}
    NavigationRow("隐私与可见范围","个人资料和圈子内容") {f.open(FeatureRoute(FeaturePage.Reading,kind="type-privacy"))}
    NavigationRow("账号数据与注销","查看影响，再验证身份") {f.open(FeatureRoute(FeaturePage.DataRequests))}
    NavigationRow("表情与贴纸","常用、收藏与输入方式") {f.open(FeatureRoute(FeaturePage.Expressions))}
    NavigationRow("未成年人保护","年龄、监护与权限") {f.open(FeatureRoute(FeaturePage.Reading,kind="type-teen"))}
    NavigationRow("平台规则","规则、历史与申诉") {f.open(FeatureRoute(FeaturePage.Rules))}
    NavigationRow("隐私与安全报告","游客也可报告与查询") {f.open(FeatureRoute(FeaturePage.Safety))}
    FeaturePrimary("返回我的") {f.closeRoute();store.select(MobileTab.Profile)}
}

@Composable fun RulesContent(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f=store.features
    val catalog=s.readingCatalog
    val platform=LocalClientPlatform.current
    var query by rememberSessionUiState("rules.search") { "" }
    when(route.kind) {
        "history" -> {
            TaskNotice("版本保存","适用期间的完整文本保留为只读；草案和正式版本分别标识，不用新规则倒改已有记录。")
            catalog?.documents?.filter {it.type!="guide"}?.forEach { document -> NavigationRow(document.title,"${document.revision} · ${readingStatus(document.status)}") {f.open(FeatureRoute(FeaturePage.Reading,kind=document.key))} }
            if(catalog?.documents?.none {it.status in listOf("published","archived")}==true) Text("尚未配置正式当前版本或历史版本；下方草案不能代替正式协议。")
            NavigationRow("查看正在公示的变更","比较差异与退出影响") {f.open(FeatureRoute(FeaturePage.Rules,kind="changes"))}
        }
        "changes" -> {
            TaskNotice("重大变化单独提示","不默认同意，不追溯剥夺已产生权益；实际公示和过渡以已发布文本为准。")
            val proposals=catalog?.documents?.filter {it.status=="proposed"}.orEmpty()
            proposals.forEach {document -> NavigationRow(document.title,"${document.revision} · ${document.effectiveFrom.ifBlank {"拟生效时间待明确"}}") {f.open(FeatureRoute(FeaturePage.Reading,kind=document.key))}}
            if(catalog!=null && proposals.isEmpty()) Text("当前没有已配置的公示变更。")
            NavigationRow("查看当前与历史规则") {f.open(FeatureRoute(FeaturePage.Rules,kind="history"))}
            NavigationRow("表达意见或申诉") {f.open(FeatureRoute(FeaturePage.Rules,kind="appeal"))}
            NavigationRow("退出或注销的影响","未完事项分别处理") {f.open(FeatureRoute(FeaturePage.DataRequests))}
        }
        "appeal" -> {
            TaskNotice("保留原记录","从原事项查看处理范围、时间、事实摘要及结论；不披露举报者和他人私密材料，不因申诉自动扩大处理。")
            NavigationRow("选择本账号的原核实／举报事项","查看实际结论后补充或申请人工复核") {f.open(FeatureRoute(FeaturePage.Reviews))}
            NavigationRow("安全、隐私或权利意见","保留原事项编号与版本") {f.open(FeatureRoute(FeaturePage.Safety))}
        }
        else -> {
            MndTextField(query,{query=it},"检索规则")
            TaskNotice("规则与权益","正式版本、生效时间和经营信息以实际配置为准；草案阅读不构成已同意。")
            NavigationRow("权益、服务与安全","主体、数据、投诉与合同") {f.open(FeatureRoute(FeaturePage.Safety))}
            catalog?.documents?.filter { it.type!="guide" && (query.isBlank() || (it.title+it.heading+it.facts.joinToString {fact->fact.text}+it.paragraphs.joinToString()).contains(query,true)) }?.forEach {document -> NavigationRow(document.title,"${document.revision} · ${readingStatus(document.status)}") {f.open(FeatureRoute(FeaturePage.Reading,kind=document.key))}}
            listOf("正式服务协议" to platform.policies.terms,"正式隐私政策" to platform.policies.privacy).forEach { (label,url) -> if(isPublicHttps(url)) NavigationRow(label) { if(!platform.openLink(url)) f.notify("此设备暂无法打开正式地址") } }
            if(catalog!=null) MndSection("平台主体") {Text(catalog.operatorName.ifBlank {"真实主体名称尚未配置"});if(catalog.operatorAddress.isNotBlank())Text(catalog.operatorAddress);if(catalog.operatorContact.isNotBlank())Text(catalog.operatorContact)}
            NavigationRow("历史版本与下载","保留适用期间完整内容") {f.open(FeatureRoute(FeaturePage.Rules,kind="history"))}
            NavigationRow("规则变更与意见","公示期间可反馈") {f.open(FeatureRoute(FeaturePage.Rules,kind="changes"))}
            NavigationRow("处理依据与人工申诉","保留原结论") {f.open(FeatureRoute(FeaturePage.Rules,kind="appeal"))}
        }
    }
    if(route.kind.isNotBlank()) FeaturePrimary("返回规则中心") {f.returnTo(FeatureRoute(FeaturePage.Rules))}
    else FeaturePrimary("返回原页面") {if(!f.back())store.select(MobileTab.Profile)}
}
