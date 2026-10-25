package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

@Composable fun AgeRangeContent(store: MobileStore,s: FeatureState) {
    val f = store.features
    Text("为你提供适合的保护",style = PageHeadlineStyle)
    Text("只保留必要年龄区间；普通浏览不需要上传身份证。以下说明不会更改已核实的年龄结果。")
    NavigationRow("未满14岁","请监护人完成指定范围授权") { f.open(FeatureRoute(FeaturePage.Guardians)) }
    NavigationRow("14–17岁","交流需满足当前手机与年龄资格，保护陌生人联系") { f.open(FeatureRoute(FeaturePage.Reading,kind = "type-teen")) }
    NavigationRow("年满18岁","商业与线下业务仍需有效成年资格") { f.open(FeatureRoute(FeaturePage.Adult,kind = "account",peer = s.eligibility?.contractVersion?.toLong() ?: 0)) }
    TaskNotice("登录成功不等于所有权限","年龄、监护、手机号和业务角色分别检查，选择年龄说明不会授予资格。")
    s.eligibility?.let { Text("当前服务端年龄区间：${ageBandLabel(it.ageBand)}") }
    NavigationRow("年龄或监护信息有误","提交私密事项，由服务核对") { f.open(FeatureRoute(FeaturePage.Safety)); f.edit("type","data_assistance") }
    TaskSecondary("先继续浏览") { f.closeRoute() }
}
fun ageBandLabel(value: String) = when(value) { "under14" -> "未满14岁";"14_17" -> "14–17岁";"adult" -> "成年";else -> "待有效核对" }

@Composable fun AgeAssuranceContent(store: MobileStore,route: FeatureRoute) {
    val f = store.features
    val flow = f.age
    val state by flow.state.collectAsState()
    val platform = LocalClientPlatform.current
    val owner = store.state.value.user
    var accepted by rememberSessionUiState("age.consent",scope = "transient:age",generation = state.stage) { false }
    LaunchedEffect(owner?.id,route) { flow.load() }
    val current = state.request
    val taskLabel = current?.taskLabel ?: state.taskLabel.takeIf { it.isNotBlank() } ?: if(route.kind == "account") "账号成年资格" else (FeaturePage.entries.firstOrNull { it.name == route.kind }?.title ?: "当前成年业务") + route.id.takeIf { it>0 }?.let { " #$it" }.orEmpty()
    if(state.busy) LinearProgressIndicator()
    when(state.stage) {
        AgeStage.Loading -> { Text("正在核对本次服务与已有请求"); if(!state.busy) FeatureButton("重新读取",onClick = flow::load) }
        AgeStage.Overview -> {
            TaskNotice("年满18岁","商业发布、预约与线下签约需有效成年资格，并继续检查角色和资料。")
            Text("年龄核对完成后，才能办理需要成年人身份的事项。")
            NavigationRow("开始年龄核对","仅返回必要结果",flow::explainPurpose)
            NavigationRow("暂不核对","继续日常浏览与交流流程") { f.back() }
            NavigationRow("核对有误或无法完成","申请私密资料事项协助") { f.open(FeatureRoute(FeaturePage.Safety)); f.edit("type","data_assistance") }
            TaskSecondary("返回原任务") { f.back() }
        }
        AgeStage.Notice -> {
            TaskNotice("只为本次成年业务核对","当前任务：$taskLabel。普通浏览无需完成此核对。")
            TaskSection("核对范围") { Text("成年资格结果、有效期与必要来源");Text("原始材料在已确认通道处理，不进入公开资料。");Text("未完成时保持成年业务受限，原任务和草稿保留。") }
            CheckLine("我已了解本次核对用途与资料范围",accepted,{ accepted=it },!state.busy)
            TaskPrimary("核对服务并继续",accepted && !state.busy) { flow.prepare(accepted,"${route.kind.ifBlank { "account" }}:${route.id}:v${route.peer}",taskLabel) }
        }
        AgeStage.Ready -> {
            TaskNotice("核对服务可用后才继续","返回应用不代表核验成功，结果由服务端查询。")
            TaskSection("本次核验") { Text(taskLabel);Text(owner?.username.orEmpty());Text(state.provider?.name.orEmpty());Text("只回传结果、有效期与必要来源。") }
            FeatureButton("阅读核验通道隐私说明",!state.busy) { platform.openLink(state.provider?.privacyUrl.orEmpty()) }
            TaskPrimary("打开本次核验",!state.busy) { flow.begin(true) }
        }
        AgeStage.Pending,AgeStage.ResultUnknown -> {
            TaskSection("成年资格核对中") { Text("请求 #${current?.id ?: "待确认"} · $taskLabel");Text(if(state.stage == AgeStage.ResultUnknown) "本次结果待核对" else "等待核验服务返回结果") }
            TaskNotice("收到结果后才更新","查询或退出等待不会确认成功；结果不明时保留原请求，不创建新的核验。")
            if(current?.launchUrl?.isNotBlank() == true) FeatureButton("打开原核验页面",!state.busy) { if(!platform.openLink(current.launchUrl)) f.notify("暂无法打开核验页面，请保留本次请求并重试") }
            TaskSecondary("查询本次结果",!state.busy,flow::query)
        }
        AgeStage.Passed -> {
            TaskNotice("成年资格结果已返回","有效期至 ${current?.validUntil?.let(::displayTime).orEmpty()}。手机号、角色、当前版本和其他条件仍须分别核对。")
            TaskSection("核对结果") { Text("${owner?.username.orEmpty()} · $taskLabel");Text("核验通道：${current?.provider.orEmpty()}");Text("有效期至 ${current?.validUntil?.let(::displayTime).orEmpty()}") }
            TaskPrimary("复核当前任务条件",!state.busy,flow::review)
        }
        AgeStage.TaskReview -> {
            TaskSection("回到原任务前再核对") { Text(taskLabel);Text("当前成年资格：${if(state.eligibility?.contractVersion == 1 && state.eligibility?.adult == true) "有效" else "待核对"}");state.eligibility?.reasons?.forEach { Text(MobileApiException(403,"eligibility",it).mobileMessage()) } }
            TaskNotice("仍需主动确认","核验成功不会自动签约、发布、确认预约或提交原草稿。")
            if(state.eligibility?.phoneVerified != true) NavigationRow("当前手机号仍未验证","保留同一任务，进入交流验证") { f.open(FeatureRoute(FeaturePage.Security)) }
            TaskSecondary("刷新原任务条件",!state.busy) { store.refreshEligibility(); f.back() }
        }
        AgeStage.Unsupported -> { TaskNotice("成年业务暂不可办理","可以继续浏览，原任务和输入保留；成年业务仍以当前有效资格为准。",warning = true);NavigationRow("需要帮助","按实际公开渠道申请协助") { f.open(FeatureRoute(FeaturePage.Safety)) } }
        AgeStage.Cancelled -> TaskNotice("已取消本次核对","不会改变年龄资格，也不会替你提交原任务。")
        AgeStage.NotEligible -> TaskNotice("本次成年业务仍受限","可以浏览与保留草稿；商业发布和线下签约由成年人用自己的账号办理。")
        AgeStage.Expired -> TaskNotice("原结果已失效","重新核对前仍保留原任务，不使用过期结果放行。",warning = true)
        AgeStage.Failed -> TaskNotice("本次未获得有效结果","原任务和输入仍在；先核对原请求，再决定是否重新发起。",warning = true)
    }
    state.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
    if(state.stage in setOf(AgeStage.Cancelled,AgeStage.NotEligible,AgeStage.Expired,AgeStage.Failed)) {
        if(state.stage != AgeStage.NotEligible && current?.state !in setOf("pending","preparing","unknown") && current != null) FeatureButton("重新阅读用途并核对",!state.busy,flow::restart)
        else if(state.stage != AgeStage.NotEligible) FeatureButton("查询原请求",!state.busy,flow::query)
        if(state.stage == AgeStage.NotEligible) NavigationRow("查看成年人办理范围","不把本账号临时改成成人") { f.open(FeatureRoute(FeaturePage.Reading,kind = "type-seller")) }
        NavigationRow("对结果有疑问","私密提出资料事项协助") { f.open(FeatureRoute(FeaturePage.Safety)); f.edit("type","data_assistance") }
    }
    if(state.stage == AgeStage.Expired) TaskPrimary("重新核对本次用途",!state.busy,flow::restart)
    else if(state.stage in setOf(AgeStage.Unsupported,AgeStage.Cancelled,AgeStage.NotEligible,AgeStage.Failed)) TaskSecondary("返回并保留任务",!state.busy) { f.back() }
    if(state.stage !in setOf(AgeStage.Overview,AgeStage.TaskReview)) FeatureButton(if(state.stage in setOf(AgeStage.Pending,AgeStage.ResultUnknown)) "退出等待，保留原请求" else "取消并返回原任务",!state.busy) { f.back() }
}

fun ageStageTitle(stage: AgeStage) = when(stage) {
    AgeStage.Overview -> "成年业务资格";AgeStage.Notice -> "成年核对前说明";AgeStage.Ready -> "准备打开核验服务"
    AgeStage.Pending -> "成年资格核对中";AgeStage.Passed -> "成年资格结果已返回";AgeStage.Cancelled -> "核对尚未完成"
    AgeStage.Failed -> "成年核对未通过";AgeStage.Unsupported -> "核对服务暂不可用";AgeStage.NotEligible -> "当前不符合成年业务范围"
    AgeStage.Expired -> "成年资格需要重新核对";AgeStage.TaskReview -> "继续原任务核对";AgeStage.ResultUnknown -> "本次结果待核对";else -> "成年资格核对"
}

@Composable fun AdultRequirement(store: MobileStore,route: FeatureRoute,version: Long) {
    val account by store.state.collectAsState()
    if(account.user != null && (account.eligibility?.contractVersion != 1 || account.eligibility?.adult != true)) NavigationRow("核对本次成年业务资格","原任务和输入保留，核验后仍需主动确认") {
        store.features.open(FeatureRoute(FeaturePage.Adult,id = route.id,kind = route.page.name,peer = version))
    }
}
