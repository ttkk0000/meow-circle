package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*
import kotlinx.serialization.json.JsonPrimitive

@Composable fun AccountFeature(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f = store.features; val actor = store.state.value.user?.id
    when (route.page) {
        FeaturePage.Publications -> PublicationsContent(store,s,route)
        FeaturePage.Rules -> RulesContent(store,s,route)
        FeaturePage.Reading -> ReadingContent(store,s)
        FeaturePage.SettingsHome -> SettingsHomeContent(store)
        FeaturePage.Age -> AgeRangeContent(store,s)
        FeaturePage.Adult -> AgeAssuranceContent(store,route)
        FeaturePage.Guardians -> if(route.kind=="protection") GuardianProtectionContent(store) else GuardiansContent(store,s,route)
        FeaturePage.Settings -> {
            val old = s.preference?.data ?: PreferenceBody()
            listOf("direct" to ("私信提醒" to old.directMessages),"groups" to ("群聊提醒" to old.groups),"applications" to ("申请与预约" to old.applications),"interactions" to ("社区互动" to old.interactions)).forEach { (key,pair) -> CheckLine(pair.first,f.field(key,pair.second.toString()).toBoolean(),{ f.edit(key,it.toString()) },!s.busy && !s.loading) }
            ChoiceField(f,s,"frequency","站内提醒频率",listOf("instant" to "即时","digest" to "汇总","off" to "关闭提醒"),old.frequency)
            CheckLine("允许系统推送", f.field("allow_push", old.pushPermission.toString()).toBoolean(), { f.edit("allow_push", it.toString()) }, !s.busy && !s.loading)
            Text("开启并保存后，已配置的设备通道会向推送供应商申请设备标识，向M&D登记；通知只显示通用提醒。关闭后停止设备推送，站内消息仍可查看。")
            NotificationPermission { permitted ->
                if (!permitted) Text("本机系统通知尚未允许；账号的推送偏好与各设备系统权限分别管理。")
                FeaturePrimary("保存通知设置",!s.busy && !s.loading && s.error == null) {
                    val input = old.copy(directMessages = f.field("direct",old.directMessages.toString()).toBoolean(),groups = f.field("groups",old.groups.toString()).toBoolean(),applications = f.field("applications",old.applications.toString()).toBoolean(),interactions = f.field("interactions",old.interactions.toString()).toBoolean(),frequency = f.field("frequency",old.frequency),pushPermission = f.field("allow_push", old.pushPermission.toString()).toBoolean())
                    val version = f.version(s.preference?.version ?: 0)
                    f.confirm("保存通知偏好","频率：${when(input.frequency) { "off" -> "关闭"; "digest" -> "汇总"; else -> "即时" }}\n关闭系统权限仍可查看站内消息。") { f.api.savePreferences(version,input); f.discardAndReload() }
                }
            }
            val push by f.api.devicePush.status.collectAsState()
            Text("${push.message}。站内未读以服务端为准。")
            FeatureButton("重新核对设备通道", !s.busy) { f.api.devicePush.refresh() }
            FeatureButton("附近提醒") { f.open(FeatureRoute(FeaturePage.Nearby)) }
        }
        FeaturePage.Map -> MapFeature(store,s,route)
        FeaturePage.Nearby -> {
            val old = s.subscription?.data
            RegionFields(f,s,old?.city.orEmpty(),withArea = false)
            if (old != null && old.locationPrecision != "city") Text("原提醒使用距离范围；本次保存将改为所选城市。原设置在你确认前继续保留。")
            ChoiceField(f,s,"species","物种",listOf("" to "全部","cat" to "猫咪","dog" to "狗狗"),old?.species ?: "")
            ChoiceField(f,s,"frequency","频率",listOf("instant" to "即时","daily" to "每日汇总","off" to "关闭提醒"),old?.frequency ?: "daily")
            CheckLine("启用该区域提醒",f.field("enabled",(old?.enabled ?: false).toString()).toBoolean(),{ f.edit("enabled",it.toString()) },!s.busy)
            FeaturePrimary("保存附近提醒",!s.busy && !s.loading && s.error == null) {
                val city = selectedCity(f,old?.city.orEmpty())
                if (city.isBlank()) f.notify("请选择或填写城市") else {
                    val input = SubscriptionInput(SubscriptionBody(city,0.0,0.0,10.0,f.field("frequency",old?.frequency ?: "daily"),f.field("enabled",(old?.enabled ?: false).toString()).toBoolean(),locationPrecision = "city", species = f.field("species",old?.species ?: "")),f.version(s.subscription?.version ?: 0))
                    f.confirm("保存同城提醒","$city · 全城范围\n${if(input.subscription.enabled) "启用" else "停用"}，按所选频率提醒。") { f.api.saveSubscription(input); f.discardAndReload() }
                }
            }
        }
        FeaturePage.Security -> SecurityFeature(store,s)
        FeaturePage.Recovery -> {
            val pendingCommands = (s.pending + store.state.value.pendingOperations).distinctBy { it.clientRequestId }
            if (pendingCommands.isEmpty()) Text("没有当前账号的待核对操作")
            pendingCommands.forEach { pending -> FeatureSection("原操作 ${pending.command.substringBefore('/')}") {
                Text("编号 ${pending.clientRequestId}\n${when(pending.phase) { "prepared" -> "已准备，尚待确认结果"; "sent", "result_unknown" -> "结果待核对"; else -> pending.phase }}")
                FeatureButton("只核对原请求",!s.busy) { store.reconcilePending(pending.clientRequestId) }
                if (pending.replayable) FeatureButton("按原编号与原内容重试",!s.busy) { store.retryPending(pending.clientRequestId) }
                else Text("此操作不可直接重放；请先核对结果，必要时回原用途重新验证。")
            } }
            if(s.draftRoutes.isNotEmpty()) FeatureSection("本机未完成表单") {
                s.draftRoutes.forEach { draft -> FeatureButton("${draft.page.title}${draft.id.takeIf { it > 0 }?.let { " #$it" }.orEmpty()}",!s.busy) { f.open(draft) } }
            }
            if(store.state.value.savedReplyDestinations.isNotEmpty()) FeatureSection("本机评论与聊天草稿") {
                store.state.value.savedReplyDestinations.forEach { destination ->
                    val label = when(destination) { is MobileDestination.Entry -> "动态 #${destination.id} 的评论"; is MobileDestination.Chat -> "${if(destination.group) "群聊" else "私信"} #${destination.id}"; else -> "原会话" }
                    FeatureButton(label,!s.busy) { f.closeRoute(); store.open(destination) }
                }
            }
            RestrictedResults(f,s)
        }
        FeaturePage.DataRequests -> {
            if (actor == null) { Text("登录后可发起资料请求。既有受限进度仍可在本设备查询。"); RestrictedResults(f,s); return }
            ChoiceField(f,s,"type","请求类型",listOf("erase_fields" to "删除指定资料／内容","close_account" to "注销账号","matters_assistance" to "未结事项协助"),"erase_fields")
            if (f.field("type","erase_fields") == "erase_fields") {
                listOf("bio" to "个人介绍","avatar" to "头像","nickname" to "昵称").forEach { (key,label) -> CheckLine(label,f.field("delete_$key","false").toBoolean(),{ f.edit("delete_$key",it.toString()) },!s.busy) }
                SelectionFields(f,s)
            } else if (f.field("type") == "matters_assistance") SelectionFields(f,s)
            FormField(f,s,"note","说明与希望获得的协助",lines = 5)
            Text("普通删除、注销与未结事项协助分开处理。分类保全、处理失败或需补充均在进度中显示，不提前显示资料已清除。")
            FeaturePrimary("核对请求范围",!s.busy) {
                val type = f.field("type","erase_fields"); val fields = listOf("bio","avatar","nickname").filter { f.field("delete_$it","false").toBoolean() }
                val selections = selectedData(f)
                val selected = if (type == "close_account") emptyList() else selections
                if (type == "erase_fields" && fields.isEmpty() && selected.isEmpty()) f.notify("请选择要删除的资料字段或具体记录")
                else if (selected.size > 200 || selected.any { it.kind !in selectionKinds(type == "matters_assistance").map { option -> option.first } }) f.notify("所选记录不适用于本次请求，请移除后重新核对")
                else f.prepareSensitive(SensitiveIntent.DataRequest(DataRequestInput(type,newRequestKey(),"",if(type == "erase_fields") fields else emptyList(),selected,f.field("note"))))
            }
            RestrictedResults(f,s)
        }
        FeaturePage.Safety -> SafetyFeature(store,s,route)
        FeaturePage.Reviews -> {
            FeatureButton("新建核实、举报或申诉",actor != null) { f.open(FeatureRoute(FeaturePage.Review)) }
            FeatureButton("免注册安全与隐私求助") { f.open(FeatureRoute(FeaturePage.Safety)) }
            s.reviews.items.forEach { review -> FeatureSection("${reviewTypeLabel(review.data.type)} · ${stateLabel(review.state)}") {
                Text(review.data.reason); FeatureButton("查看事项与补充") { f.open(FeatureRoute(FeaturePage.Review,review.id)) }
            } }
            PageFooter(s.reviews, { f.reload() }, { f.reload(true) }, "暂无核实、举报或申诉事项", enabled = !s.loading && !s.busy)
        }
        FeaturePage.Review -> ReviewFeature(store,s,route)
        FeaturePage.Expressions -> {
            Text("收藏与最近记录按账号同步。表情只插入内容，贴纸预览后主动发送。")
            val favorites = s.expressions?.data?.favorites.orEmpty()
            fun toggle(id: String, checked: Boolean) {
                val next = if(checked) (favorites + id).distinct() else favorites - id
                f.runCommand { f.api.saveExpressions(s.expressions?.version ?: 0,ExpressionBody(next,s.expressions?.data?.recent.orEmpty())); f.discardAndReload() }
            }
            s.catalog.emoji.forEach { item -> ExpressionFavoriteRow(item.label,item.id in favorites,!s.busy && !s.loading,{ toggle(item.id,it) }) { EmojiPhoto(item) } }
            s.catalog.stickers.forEach { item -> ExpressionFavoriteRow(item.label.ifBlank { item.id },item.id in favorites,!s.busy && !s.loading,{ toggle(item.id,it) }) { StickerPhoto(item.id,item.label,Modifier.size(48.dp)) } }

        }
        else -> Unit
    }
}

@Composable private fun SecurityFeature(store: MobileStore,s: FeatureState) {
    val f = store.features; val methods = s.methods ?: return
    val eligible = s.eligibility
    FeatureSection("登录方式与交流资格") {
        NavigationRow("年龄与使用范围","核对年龄、监护与成年业务") { f.open(FeatureRoute(FeaturePage.Age)) }
        Text("手机号：${methods.phone.ifBlank { "未绑定" }}\n密码：${if(methods.passwordSet) "已设置" else "未设置"}\n手机验证：${if(eligible?.phoneVerified == true) "已验证" else "未验证"}\n交流资格：${if(eligible?.interaction == true) "可交流" else "尚未满足条件"}")
        eligible?.reasons?.forEach { Text(MobileApiException(403,"eligibility required",it).mobileMessage()) }
        Text("年龄信息来自核实服务；此处不会将自行勾选成年作为验证。")
    }
    var phone by rememberSessionUiState("security.phone") { "" }; var email by rememberSessionUiState("security.email") { "" }
    var code by rememberSessionUiState("security.code") { "" }; var newPassword by rememberSessionUiState("security.newPassword") { "" }
    FeatureSection("验证当前手机号") {
        OutlinedTextField(phone,{ phone = it },label = { Text("完整当前手机号") })
        OtpVerificationFields(
            code = code,
            onCodeChange = { code = it },
            otpWait = s.otpWait,
            sendLabel = "发送当前手机号验证码",
            enabled = !s.busy,
            canSend = methods.smsAvailable && phone.isNotBlank(),
        ) { f.sendOTP(phone = phone,purpose = "verify_phone") }
        FeatureButton("验证当前手机号",!s.busy && code.isNotBlank()) { val proof = code; code = ""; f.runCommand { f.api.verifyCurrentPhone(proof); store.refreshEligibility(); f.reload() } }
    }
    FeatureSection("安全变更") {
        ChoiceField(f,s,"change","变更类型",listOf("phone" to "换绑手机号","email" to "绑定／更换邮箱","password" to "设置／更改密码"),"phone")
        val change = f.field("change","phone")
        if (change == "phone") OutlinedTextField(phone,{ phone = it },label = { Text("新手机号") })
        if (change == "email") OutlinedTextField(email,{ email = it },label = { Text("新邮箱") })
        if (change == "password") OutlinedTextField(newPassword,{ newPassword = it },label = { Text("新密码（至少10字节）") },visualTransformation = PasswordVisualTransformation())
        if (change != "password") {
            OtpVerificationFields(
                code = code,
                onCodeChange = { code = it },
                otpWait = s.otpWait,
                sendLabel = "发送新联系方式验证码",
                codeLabel = "新联系方式验证码",
                enabled = !s.busy,
                canSend = if (change == "phone") methods.smsAvailable && phone.isNotBlank() else store.state.value.capabilities.email && email.isNotBlank(),
            ) { f.sendOTP(email = if(change == "email") email else "",phone = if(change == "phone") phone else "",purpose = if(change == "email") "email_bind" else "bind_phone") }
        }
        FeatureButton("核对安全变更",!s.busy && !s.loading) {
            if (change == "password" && newPassword.encodeToByteArray().size !in 10..128 || change != "password" && code.isBlank()) f.notify("请检查新密码或验证码") else {
                val input = AccountInput(s.profile?.version ?: 0,newRequestKey(),phone = if(change == "phone") phone else "",email = if(change == "email") email else "",password = if(change == "password") newPassword else "",code = if(change != "password") code else "")
                f.prepareSensitive(SensitiveIntent.Account(change,input)); code = ""; newPassword = ""
            }
        }
    }
    FeatureSection("已关联的方式") {
        methods.linked.forEach { identity ->
            Text(identity.provider)
            FeatureButton("解绑此方式",!s.busy) { f.prepareSensitive(SensitiveIntent.Account("unlink",AccountInput(s.profile?.version ?: 0,newRequestKey(),identityId = identity.id))) }
        }
        ProviderEntry(store,methods.availableProviders,s.busy)
        Text("关联需要本次用途验证与主动确认；解绑最后一种登录方式会由服务端阻止。")
    }
    NavigationRow("监护授权与管理","邀请、独立关系核对、明确授权和撤回") { f.open(FeatureRoute(FeaturePage.Guardians)) }
    NavigationRow("未成年人保护设置","陌生人联系、屏蔽与资料求助") { f.open(FeatureRoute(FeaturePage.Guardians,kind="protection")) }
    FeatureButton("申请账号相关事项核实") { f.open(FeatureRoute(FeaturePage.Review)); f.edit("type","verification"); f.edit("check","account") }
    FeatureButton("资料删除、注销与协助") { f.open(FeatureRoute(FeaturePage.DataRequests)) }
    FeatureButton("退出全部会话",!s.busy) { f.confirm("退出全部会话","全部设备将需要重新登录。") { store.logoutAll(); f.finishRoute() } }
}

@Composable private fun SafetyFeature(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f = store.features
    var contact by rememberSessionUiState("safety.contact") { "" }; var code by rememberSessionUiState("safety.code") { "" }
    var verifiedContact by rememberSessionUiState<String?>("safety.verifiedContact") { null }
    ChoiceField(f,s,"type","事项类型",listOf("safety" to "安全求助","privacy" to "隐私报告","rights_notice" to "正式权利通知","counterstatement" to "对原通知提交声明","supplement" to "补充原事项","data_assistance" to "资料事项协助"),if(route.id > 0) "supplement" else "safety")
    val type = f.field("type",if(route.id > 0) "supplement" else "safety")
    if (route.peer > 0 && route.kind.isNotBlank()) Text("关联对象：${resourceLabel(route.kind)} #${route.peer}") else {
        ChoiceField(f,s,"targetKind","关联内容类型（可不关联）",listOf("" to "不指定","entries" to "动态","pets" to "宠物","offers" to "有偿信息","cases" to "走失求助"),"")
        FormField(f,s,"targetId","内容编号（可留空）",numeric = true)
    }
    FormField(f,s,"reason","事项经过与希望处理的问题",lines = 5)
    if (type in listOf("supplement","counterstatement")) { FormField(f,s,"related","原事项编号",route.id.takeIf { it > 0 }?.toString().orEmpty(),numeric = true); FormField(f,s,"version","原事项当前版本",numeric = true); Text("补充须使用原受限入口；声明须指向原通知链，不能新建无关联事项代替。") }
    if (type in listOf("rights_notice","counterstatement")) {
        FormField(f,s,"legalName","真实姓名或主体名称"); FormField(f,s,"rightType","主张的具体权利"); FormField(f,s,"statement","当次声明",lines = 4)
        OutlinedTextField(contact,{ contact = it; verifiedContact = null },label = { Text("用于此次通知的联系邮箱") })
        FeatureButton("发送联系邮箱验证码",!s.busy && contact.isNotBlank()) { val address = contact.trim(); verifiedContact = null; code = ""; f.runCommand { val receipt = f.api.requestSafetyContactOTP(f.safetyToken(),address); f.notify("验证码已发送，有效至 ${receipt.expiresAt}") } }
        OutlinedTextField(code,{ code = it },label = { Text("邮箱验证码") })
        FeatureButton("验证此次联系方式",!s.busy && code.isNotBlank()) { val proof = code; val address = contact.trim(); code = ""; f.runCommand { f.api.verifySafetyContact(f.safetyToken(),address,proof); verifiedContact = if(contact.trim() == address) address else null; f.notify("本次联系方式已验证，仍需核对并主动提交") } }
    }
    FormField(f,s,"evidence","材料来源与证明事项",lines = 4)
    FeaturePhotos(store,s,private = true,safety = true)
    FeaturePrimary("核对事项并继续",!s.busy && f.field("reason").isNotBlank()) {
        val related = f.field("related",route.id.takeIf { it > 0 }?.toString().orEmpty()).toLongOrNull() ?: 0
        val version = f.field("version").toLongOrNull() ?: 0
        if(type in listOf("rights_notice","counterstatement") && (verifiedContact == null || verifiedContact != contact.trim())) f.notify("请先验证当前填写的联系方式；修改后需重新验证") else if (type in listOf("supplement","counterstatement") && (related <= 0 || version <= 0)) f.notify("请从原事项载入或填写原编号与当前版本") else if (!f.photosReady()) f.notify("请先完成材料上传") else {
            val input = RightsInput(type,newRequestKey(),f.field("reason"),route.kind.ifBlank { f.field("targetKind") },route.peer.takeIf { it > 0 } ?: (f.field("targetId").toLongOrNull() ?: 0),related,version,f.field("rightType"),f.field("legalName"),f.field("statement"),f.field("evidence"),f.mediaIds())
            f.runCommand { f.prepareSensitive(SensitiveIntent.Rights(f.safetyToken(),input,store.state.value.user != null)) }
        }
    }
    RestrictedResults(f,s)
}

@Composable private fun ReviewFeature(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f = store.features
    if (route.id > 0) {
        val record = s.review ?: return
        FeatureSection("${reviewTypeLabel(record.data.type)} · ${stateLabel(record.state)}") { Text(record.data.reason); Text(record.data.summary); if(record.data.check.isNotBlank()) Text(checkLabel(record.data.check)); record.data.validUntil?.let { Text("有效至 ${displayTime(it)}") }; record.mediaIds.forEach { MobilePhoto(store,it,store.state.value.user?.id,"私密核实材料") } }
        if (record.state == "needs_info") {
            FormField(f,s,"supplement","补充说明",lines = 5); FeaturePhotos(store,s,record.mediaIds,private = true)
            FeatureButton("提交补充",!s.busy && f.field("supplement").isNotBlank()) { val input = VersionedAction(record.version,f.field("supplement"),f.mediaIds(record.mediaIds)); f.confirm("提交事项补充",input.note) { f.api.supplementReview(record.id,input); f.reload() } }
        }
        if (record.state in listOf("approved","rejected","expired")) FeatureButton("针对本结论发起申诉") { f.open(FeatureRoute(FeaturePage.Review,kind = "appeal",peer = record.id)); f.edit("type","appeal") }
        return
    }
    ChoiceField(f,s,"type","事项类型",listOf("verification" to "具体事项核实","report" to "举报","appeal" to "对原结论申诉","support" to "请求协助","recovery" to "恢复访问协助"),if(route.kind == "appeal") "appeal" else "report")
    val type = f.field("type",if(route.kind == "appeal") "appeal" else "report")
    if (type == "verification") ChoiceField(f,s,"check","核实项目",listOf("account" to "账号","publication_authority" to "发布授权","pet_recent" to "宠物近期情况","location" to "地点","medical_document" to "医疗文件","medical_source" to "医疗来源"),"publication_authority")
    if (type == "appeal") FormField(f,s,"appealOf","原结论编号",route.peer.takeIf { route.kind == "appeal" }?.toString().orEmpty(),numeric = true) else if(route.peer == 0L) {
        ChoiceField(f,s,"targetKind","关联对象",listOf("" to "不指定","pets" to "宠物","entries" to "动态","offers" to "有偿信息","cases" to "走失求助"),""); FormField(f,s,"targetId","对象编号（可留空）",numeric = true)
    } else Text("关联 ${resourceLabel(route.kind)} #${route.peer}")
    FormField(f,s,"reason","说明核实事项、举报或申诉理由",lines = 5); FeaturePhotos(store,s,private = true)
    FeaturePrimary("核对并提交事项",!s.busy && !s.loading && f.field("reason").isNotBlank()) {
        val appeal = f.field("appealOf",route.peer.takeIf { route.kind == "appeal" }?.toString().orEmpty()).toLongOrNull() ?: 0
        if (type == "appeal" && appeal <= 0) f.notify("申诉必须关联原结论") else {
            val input = ReviewInput(type,f.field("reason"),if(type == "verification") f.field("check","publication_authority") else "",newRequestKey(),if(type == "appeal") "" else route.kind.ifBlank { f.field("targetKind") },if(type == "appeal") 0 else route.peer.takeIf { it > 0 } ?: (f.field("targetId").toLongOrNull() ?: 0),appeal,f.mediaIds())
            f.confirm("提交${reviewTypeLabel(type)}",input.reason) { val result = f.api.review(input); f.completeTo(FeatureRoute(FeaturePage.Review,result.id)) }
        }
    }
}

@Composable private fun RestrictedResults(f: MobileFeatureStore,s: FeatureState) {
    FeatureSection("本设备受限进度") {
        Text("凭证保存在本机安全存储，不显示或复制到普通表单。")
        s.restricted.filter { it.type in listOf("data_request","safety_ticket") }.forEach { access -> FeatureButton(if(access.type == "data_request") "查询资料请求 #${access.id.takeIf { it > 0 } ?: "待核对"}" else "查询安全事项 #${access.id.takeIf { it > 0 } ?: "待核对"}",!s.busy) { f.lookup(access) } }
        s.dataStatus?.let { status ->
            val progress = status.progress()
            Text("请求 #${status.id} · ${dataTypeLabel(status.type)}")
            Text(progress.message, color = if (progress.reviewRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (status.progressVersion == 1 && progress.phase != DataRequestPhase.NeedsReconciliation) {
                Text("已处理 ${status.erased} · 待处理 ${status.pending} · 保全 ${status.held}")
                if (status.physicalPending > 0) Text("附件物理清理待确认 ${status.physicalPending}")
                if (status.providerPending > 0) Text("外部账号授权待处理 ${status.providerPending}")
                if (status.restrictedCopies > 0) Text("仍有 ${status.restrictedCopies} 份受限副本保留")
                if (status.pendingFields.isNotEmpty()) Text("待处理资料：${status.pendingFields.joinToString { field -> mapOf("bio" to "个人介绍", "avatar" to "头像", "nickname" to "昵称")[field] ?: "其他资料" }}")
            }
            progress.nextReviewAt?.let { Text("下次复核 ${displayTime(it)}") }
        }
        s.rightsStatus?.let { result ->
            Text("事项 #${result.id} · ${rightsTypeLabel(result.type)}\n${stateLabel(result.state)} · 版本 ${result.version}\n复核日期 ${displayTime(result.reviewDue)}")
            Text(when (result.evidenceState) {
                "erased" -> "本事项关联材料已按期限清理；这不代表所有外部副本均已删除。"
                "review_due" -> "材料已到复核期限，受控读取需重新核对。"
                "retained" -> "材料按当前事项的限定用途保留，不会公开展示。"
                else -> "材料处理状态待核对。"
            })
            val original = s.restricted.firstOrNull { it.type == "safety_ticket" && (it.id == result.id || it.id == 0L && it.clientRequestId == s.lookupRequestId) }
            if (original != null) FeatureButton("在原事项追加材料",!s.busy) { f.useSafetyAccess(original); f.open(FeatureRoute(FeaturePage.Safety,result.id)); f.edit("version",result.version.toString()) }
        }
    }
}
@Composable private fun SelectionFields(f: MobileFeatureStore,s: FeatureState) {
    val assistance = f.field("type","erase_fields") == "matters_assistance"
    val options = selectionKinds(assistance)
    val kind = f.field("selectionKind","entries").takeIf { value -> options.any { it.first == value } }
    ChoiceField(f,s,"selectionKind","从当前记录中选择",options,"entries")
    LaunchedEffect(kind,assistance,s.route) { if (kind != null) f.loadDataChoices(kind,assistance) }
    if (kind == null) Text("请先选择适用于本次请求的记录类型。")
    else if (s.dataChoiceKind == kind && s.dataChoiceAssistance == assistance) {
        s.dataChoices.items.forEach { record ->
            val item = "$kind:${record.id}"
            val title = listOf("title","name","text","message").firstNotNullOfOrNull { field ->
                (record.data[field] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } }?.take(64)
            CheckLine("${title ?: resourceLabel(kind)} · #${record.id} · ${stateLabel(record.state)}",selectedData(f).any { it.kind == kind && it.id == record.id },{ checked ->
                val current = f.field("selections").split(',').filter { it.isNotBlank() }
                if (checked && current.size >= 200) f.notify("一次最多选择200条记录") else f.edit("selections",(if (checked) (current + item).distinct() else current - item).joinToString(","))
            },!s.busy && !s.loading && !s.unconfirmedResult)
        }
        PageFooter(s.dataChoices, { f.loadDataChoices(kind,assistance) }, { f.loadDataChoices(kind,assistance,true) },
            "此类型暂无可选记录。", enabled = !s.dataChoiceLoading && !s.busy)
        if (s.dataChoices.loaded && s.dataChoices.error == null) FeatureButton("重新读取记录",!s.dataChoiceLoading) { f.loadDataChoices(kind,assistance) }
    }
    if (selectedData(f).isNotEmpty()) Text("本次已选 ${selectedData(f).size} 条，可逐条移除：")
    selectedData(f).forEach { selection -> FeatureButton("移除：${resourceLabel(selection.kind)} #${selection.id}",!s.busy) { f.edit("selections",f.field("selections").split(',').filterNot { it == "${selection.kind}:${selection.id}" }.joinToString(",")) } }
}
private fun selectionKinds(assistance: Boolean) = listOf("entries" to "动态","entry_comments" to "动态评论","pet_comments" to "领养交流评论","pets" to "宠物档案","media" to "照片","cases" to "走失求助","offers" to "有偿信息","drafts" to "草稿","subscriptions" to "附近订阅") +
    if (assistance) listOf("adoptions" to "领养事项","appointments" to "预约","reviews" to "核实事项") else emptyList()
private fun selectedData(f: MobileFeatureStore) = f.field("selections").split(',').mapNotNull { item -> val parts = item.split(':'); if(parts.size == 2) parts[1].toLongOrNull()?.let { DataSelection(parts[0],it) } else null }
private fun reviewTypeLabel(v: String) = mapOf("verification" to "核实","report" to "举报","appeal" to "申诉","support" to "协助","recovery" to "恢复")[v] ?: v
fun resourceLabel(v: String) = mapOf("pets" to "宠物档案","entries" to "动态","offers" to "有偿信息","cases" to "走失求助","media" to "照片","adoptions" to "领养事项","appointments" to "预约")[v] ?: v

@Composable private fun ExpressionFavoriteRow(label: String, selected: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit, preview: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        preview()
        Box(Modifier.weight(1f)) { CheckLine(label,selected,onChange,enabled) }
    }
}
