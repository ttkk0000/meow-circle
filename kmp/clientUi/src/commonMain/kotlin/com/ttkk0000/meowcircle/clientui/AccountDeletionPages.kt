package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ttkk0000.meowcircle.client.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive

internal const val DATA_REQUEST_UI_SCOPE = "transient:data-requests"

internal fun accountDeletionScope(clientRequestId: String?): String = "transient:sensitive:$clientRequestId"

internal fun isAccountDeletionSensitive(
    s: SensitiveActionState,
    common: FeatureState,
    actor: Long?,
    sessionEnd: SessionEnd?,
    rememberedCloseAccount: Boolean,
): Boolean {
    val intent = s.intent as? SensitiveIntent.DataRequest
    if (intent != null) return intent.input.type == "close_account"
    if (rememberedCloseAccount && s.stage != SensitiveStage.Idle) return true
    if (actor == null && sessionEnd?.kind == SessionEndKind.AccountRequest &&
        s.stage in listOf(SensitiveStage.Submitting, SensitiveStage.Complete, SensitiveStage.ResultUnknown, SensitiveStage.Failed)
    ) return true
    val routeType = common.forms[common.route]?.values?.get("type")
        ?: if (common.route?.kind == "close_account") "close_account" else "erase_fields"
    if (s.stage in listOf(SensitiveStage.LoadingScope, SensitiveStage.Failed) && routeType == "close_account") return true
    if (s.stage == SensitiveStage.ResultUnknown && s.clientRequestId != null &&
        common.restricted.any { it.type == "data_request" && it.clientRequestId == s.clientRequestId } &&
        (actor == null || routeType == "close_account")
    ) return true
    return false
}

internal fun accountDeletionSensitiveTitle(
    s: SensitiveActionState,
    step: String,
    attemptStage: String,
): String = when (s.stage) {
    SensitiveStage.LoadingScope -> "注销账号前"
    SensitiveStage.Review -> when (step) {
        "matters" -> "查看未完成事项"
        "verify" -> "确认本人操作"
        else -> "注销账号前"
    }
    SensitiveStage.Verifying, SensitiveStage.ReadyToSubmit -> "确认本人操作"
    SensitiveStage.Submitting, SensitiveStage.Complete -> "注销申请处理中"
    SensitiveStage.ResultUnknown -> "注销暂未完成"
    SensitiveStage.Failed -> when {
        attemptStage in listOf("verify_submitted", "retry_verify") && s.scope != null && s.intent != null -> "确认本人操作"
        attemptStage == "retry_matters" && s.scope != null && s.intent != null -> "查看未完成事项"
        attemptStage == "retry_impact" && s.scope != null && s.intent != null -> "注销账号前"
        else -> "注销暂未完成"
    }
    else -> "资料与账号请求"
}

internal fun accountDeletionStatusTitle(status: DataRequestStatus): String = when (status.progress().phase) {
    DataRequestPhase.Complete, DataRequestPhase.RetainedCopies -> "注销处理结果"
    DataRequestPhase.Processing, DataRequestPhase.Held -> "注销申请处理中"
    DataRequestPhase.NeedsReconciliation -> "注销暂未完成"
    DataRequestPhase.Assistance -> "资料与账号请求"
}

internal fun accountDeletionStatus(s: FeatureState, reference: RestrictedReference?): DataRequestStatus? {
    if (s.busy || s.error != null) return null
    val status = s.dataStatus?.takeIf { it.type == "close_account" } ?: return null
    val route = s.route
    return if (reference != null) {
        status.takeIf { s.lookupRequestId == reference.clientRequestId && (reference.id == 0L || it.id == reference.id) }
    } else {
        status.takeIf { route?.kind == "status" && route.id == it.id }
    }
}

internal fun dataFieldLabel(field: String): String = mapOf(
    "bio" to "个人介绍",
    "avatar" to "头像",
    "nickname" to "昵称",
)[field] ?: "其他资料"

internal fun dataExternalStateLabel(value: String): String = when (value) {
    "not_required" -> "无需外部处理"
    "pending_inventory" -> "待盘点外部副本"
    "pending" -> "外部副本处理中"
    "completed" -> "外部副本已处理"
    else -> "外部状态：$value"
}

@Composable
internal fun ColumnScope.DataRequestProgressDetails(status: DataRequestStatus) {
    val progress = status.progress()
    Text("请求 #${status.id} · ${dataTypeLabel(status.type)}")
    Text(progress.message, color = if (progress.reviewRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    if (status.progressVersion == 1 && progress.phase != DataRequestPhase.NeedsReconciliation) {
        Text("已处理 ${status.erased} · 待处理 ${status.pending} · 保全 ${status.held}")
        if (status.physicalPending > 0) Text("附件物理清理待确认 ${status.physicalPending}")
        if (status.providerPending > 0) Text("外部账号授权待处理 ${status.providerPending}")
        if (status.restrictedCopies > 0) Text("仍有 ${status.restrictedCopies} 份受限副本保留")
        if (status.pendingFields.isNotEmpty()) {
            Text("待处理资料：${status.pendingFields.joinToString(transform = ::dataFieldLabel)}")
        }
        if (status.externalState !in listOf("not_required", "completed")) {
            Text(dataExternalStateLabel(status.externalState))
        }
    }
    progress.nextReviewAt?.let { Text("下次复核 ${displayTime(it)}") }
}

/** 账号注销五态：影响确认、用途验证、受理后退出、最终结果／必要保全及失败复用原申请。 */
@Composable
fun AccountDeletionSensitivePage(store: MobileStore, s: SensitiveActionState, common: FeatureState) {
    val f = store.features
    val scope = accountDeletionScope(s.clientRequestId)
    val owner = store.state.value.user?.id
    var rememberedClose by rememberSessionUiState("closeAccount.active", scope) { false }
    var step by rememberSessionUiState("dataStep", scope, s.intent) { "impact" }
    var attemptStage by rememberSessionUiState("closeAccount.attempt", scope, s.intent) { "" }
    var impactAgreed by rememberSessionUiState("closeAccount.impactAgreed", scope, s.intent) { false }
    var finalConfirmed by rememberSessionUiState("closeAccount.finalConfirmed", scope, s.intent) { false }
    var verifyMethod by rememberSessionUiState("closeAccount.verifyMethod", scope, s.intent) { "" }
    var password by rememberSessionUiState("password", scope) { "" }
    var currentPhone by rememberSessionUiState("currentPhone", scope) { "" }
    var code by rememberSessionUiState("code", scope) { "" }
    var sentPhone by rememberSessionUiState("closeAccount.sentPhone", scope) { "" }
    var sentReceiptKey by rememberSessionUiState("closeAccount.sentReceipt", scope) { "" }
    var matterKind by rememberSessionUiState("closeAccount.matterKind", scope) { "adoptions" }
    var loadedMethods by rememberSessionUiState<IdentityMethods?>("closeAccount.methods", scope, owner) { common.methods }
    var viewingDeleteStatus by rememberSessionUiState("closeAccount.viewingStatus", DATA_REQUEST_UI_SCOPE, common.route) { common.route?.kind == "status" }
    var lastLookupRef by rememberSessionUiState<RestrictedReference?>("closeAccount.lastLookup", DATA_REQUEST_UI_SCOPE) { null }
    var lastLookupIsCloseAccount by rememberSessionUiState("closeAccount.lastLookupIsDelete", DATA_REQUEST_UI_SCOPE) { false }

    SideEffect {
        if ((s.intent as? SensitiveIntent.DataRequest)?.input?.type == "close_account") {
            rememberedClose = true
        }
    }
    LaunchedEffect(step) {
        if (step != "verify") {
            password = ""
            code = ""
        }
    }
    LaunchedEffect(owner, common.methods) {
        if (common.methods != null) {
            loadedMethods = common.methods
        } else if (owner != null && loadedMethods == null) {
            try {
                val actual = f.api.identityMethods()
                if (store.state.value.user?.id == owner) loadedMethods = actual
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    val methods = common.methods ?: loadedMethods
    val busy = common.busy || s.stage in listOf(SensitiveStage.LoadingScope, SensitiveStage.Verifying, SensitiveStage.Submitting)
    val canRetryOriginal = s.stage == SensitiveStage.Failed && s.scope != null && s.intent != null
    val inVerifyFailure = canRetryOriginal && attemptStage in listOf("verify_submitted", "retry_verify")
    val inImpactStage = s.stage == SensitiveStage.Review || (canRetryOriginal && attemptStage in listOf("retry_impact", "retry_matters"))
    val showMatters = (s.stage == SensitiveStage.Review && step == "matters") || (canRetryOriginal && attemptStage == "retry_matters")

    fun openRestrictedLookup(ref: RestrictedReference?) {
        password = ""
        code = ""
        currentPhone = ""
        if (ref != null) {
            lastLookupRef = ref
            lastLookupIsCloseAccount = true
            viewingDeleteStatus = true
            f.sensitive.dismiss()
            f.lookup(ref)
        } else {
            f.sensitive.dismiss()
            f.open(FeatureRoute(FeaturePage.Recovery))
        }
    }

    TaskColumn(spacing = 16) {
        when {
            s.stage == SensitiveStage.LoadingScope -> {
                Text("请确认你了解这些影响", style = PageHeadlineStyle)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                TaskNotice("正在核对注销范围", "正在向服务端申请受限查询凭证并读取当前注销范围，不会提交注销申请。")
            }

            showMatters -> {
                Text("查看未完成事项", style = PageHeadlineStyle)
                TaskNotice("先了解影响，不会自动结案", "以下为当前账号可读取的真实记录；浏览或处理不会自动提交注销，也不会自动关闭事项。")
                ChoiceChipBar(
                    options = listOf(
                        "adoptions" to "领养事项",
                        "appointments" to "预约",
                        "reviews" to "核实事项",
                        "cases" to "走失求助",
                        "pets" to "宠物档案",
                    ),
                    selected = matterKind,
                    onSelect = { matterKind = it },
                    scrollable = true,
                    enabled = !busy,
                )
                LaunchedEffect(matterKind, owner) {
                    if (owner != null) f.loadDataChoices(matterKind, true)
                }
                if (common.dataChoiceKind == matterKind && common.dataChoiceAssistance) {
                    common.dataChoices.items.forEach { record ->
                        val title = listOf("title", "name", "text", "message").firstNotNullOfOrNull { field ->
                            (record.data[field] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                        }?.take(64)
                        TaskNotice(
                            "${title ?: resourceLabel(matterKind)} · #${record.id}",
                            "当前状态：${stateLabel(record.state)}",
                        )
                    }
                    PageFooter(
                        common.dataChoices,
                        { f.loadDataChoices(matterKind, true) },
                        { f.loadDataChoices(matterKind, true, true) },
                        "此类型暂无未完成或可读取记录。",
                        enabled = !common.dataChoiceLoading && !busy,
                    )
                }
                NavigationRow(!busy, "前往领养申请进度", "查看申请与双方确认") {
                    f.sensitive.dismiss()
                    f.closeRoute()
                    store.open(MobileDestination.Applications)
                }
                NavigationRow(!busy, "前往预约与安排", "查看当前与历史预约") {
                    f.sensitive.dismiss()
                    f.open(FeatureRoute(FeaturePage.Appointments))
                }
                NavigationRow(!busy, "前往协作与群邀请", "查看待处理与已加入协作") {
                    f.sensitive.dismiss()
                    f.open(FeatureRoute(FeaturePage.Invitations))
                }
                NavigationRow(!busy, "前往核实与举报", "查看待处理事项") {
                    f.sensitive.dismiss()
                    f.open(FeatureRoute(FeaturePage.Reviews))
                }
                NavigationRow(!busy, "改用未结事项协助", "协助请求不代替正式注销") {
                    f.sensitive.dismiss()
                    f.edit("type", "matters_assistance")
                }
                TaskPrimary("返回注销影响确认", !busy) {
                    if (canRetryOriginal) attemptStage = "retry_impact"
                    step = "impact"
                }
            }

            inImpactStage && !inVerifyFailure && step != "verify" -> {
                val note = (s.intent as? SensitiveIntent.DataRequest)?.input?.note.orEmpty()
                Text("请确认你了解这些影响", style = PageHeadlineStyle)
                AuthFacts(
                    listOf(
                        "宠物与日记" to "公开范围及保留方式将在处理结果中说明",
                        "未完成事项" to "申请、求助和协作关系需受控处理",
                        "必要历史" to "争议与审计所需记录按规则受限保留",
                    ),
                )
                if (note.isNotBlank()) AuthFacts(listOf("补充说明" to note))
                NavigationRow(!busy, "查看未完成事项", "先了解影响，不会自动结案") {
                    if (canRetryOriginal) attemptStage = "retry_matters"
                    step = "matters"
                }
                TaskNotice(
                    "受理与清除分开",
                    "确认并提交后将立即退出当前会话；未结事项与必要保全分别处理，不会将受理显示为全部删除。后续仅凭本设备受限凭证查询进度。",
                )
                CheckLine(
                    "我已了解注销影响，并确认提交申请",
                    impactAgreed,
                    { impactAgreed = it },
                    !busy,
                )
                MndQuietButton("返回修改请求", !busy) {
                    password = ""
                    code = ""
                    currentPhone = ""
                    f.clearCommandFeedback()
                    f.sensitive.dismiss()
                    f.reload()
                }
                TaskPrimary("继续验证身份", !busy && impactAgreed && s.scope != null) {
                    attemptStage = if (canRetryOriginal) "retry_verify" else ""
                    step = "verify"
                }
            }

            s.stage == SensitiveStage.ReadyToSubmit -> {
                Text("确认本人操作", style = PageHeadlineStyle)
                s.scope?.let {
                    Text(
                        "用途：资料与账号请求 · 版本 ${it.resourceVersion}；本次验证仅适用于当前注销申请。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TaskNotice(
                    "本人操作已验证，尚未提交",
                    "提交后会立即退出当前账号并进入受理处理；未结事项与必要保全分别处理，不会将受理显示为已全部删除。",
                    success = true,
                )
                AuthFacts(
                    listOf(
                        "操作类型" to "注销账号",
                        "请求编号" to (s.clientRequestId ?: "当前申请"),
                    ),
                )
                CheckLine(
                    "我已确认注销此账号，并了解提交后会立即退出",
                    finalConfirmed,
                    { finalConfirmed = it },
                    !busy,
                )
                MndQuietButton("取消并保留当前账号", !busy) {
                    finalConfirmed = false
                    password = ""
                    code = ""
                    currentPhone = ""
                    f.clearCommandFeedback()
                    f.sensitive.dismiss()
                    f.reload()
                }
                TaskPrimary("提交注销申请", !busy && finalConfirmed) {
                    attemptStage = "submit"
                    password = ""
                    code = ""
                    f.clearCommandFeedback()
                    f.confirmSensitive(finalConfirmed)
                }
            }

            (s.stage == SensitiveStage.Review && step == "verify") ||
                s.stage == SensitiveStage.Verifying ||
                inVerifyFailure -> {
                val phoneChannelAvailable = methods?.smsAvailable ?: store.state.value.capabilities.sms
                val activeMethod = verifyMethod.ifBlank {
                    when {
                        methods != null && methods.phone.isNotBlank() && methods.smsAvailable -> "phone"
                        methods != null && methods.passwordSet -> "password"
                        phoneChannelAvailable -> "phone"
                        else -> "password"
                    }
                }
                val sentHere = sentReceiptKey.isNotEmpty() &&
                    sentReceiptKey == common.otpDelivery?.key &&
                    sentPhone == currentPhone.trim()
                val otpExpired = sentHere && common.otpDelivery?.expired == true

                Text("确认本人操作", style = PageHeadlineStyle)
                s.scope?.let {
                    Text(
                        "用途：资料与账号请求 · 版本 ${it.resourceVersion}；本次验证仅适用于当前注销申请，验证成功后仍需主动确认提交。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (attemptStage == "retry_verify") {
                    TaskNotice(
                        "继续原注销申请",
                        "原申请编号与核对范围已保留，不重复创建申请。请重新完成本次用途验证后再确认提交。",
                    )
                }
                if (activeMethod == "phone") {
                    if (methods != null && methods.phone.isNotBlank()) {
                        AuthFacts(listOf("已验证手机号" to methods.phone))
                    } else if (methods != null) {
                        TaskNotice("当前账号尚未绑定手机号", "请改用当前账号密码验证本次注销用途。")
                    }
                    MndOutlinedField(
                        value = currentPhone,
                        onValueChange = {
                            currentPhone = it
                            code = ""
                            sentPhone = ""
                            sentReceiptKey = ""
                            f.clearCommandFeedback()
                        },
                        label = { Text("当前完整手机号（输入后发送验证码）") },
                        enabled = !busy,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                    OtpVerificationFields(
                        code = code,
                        onCodeChange = {
                            code = it.filter { c -> c in '0'..'9' }.take(6)
                            f.clearCommandFeedback()
                        },
                        otpWait = common.otpWait,
                        sendLabel = "发送本次用途验证码",
                        codeLabel = "验证码",
                        waitSuffix = "秒后可重发",
                        enabled = !busy && !common.busy,
                        canSend = phoneChannelAvailable && validLoginPhone(currentPhone),
                        sendFirst = true,
                    ) {
                        val address = currentPhone.trim()
                        code = ""
                        sentPhone = ""
                        sentReceiptKey = ""
                        f.sendOTP(phone = address, purpose = "reauth") {
                            if (currentPhone.trim() == address) {
                                sentPhone = address
                                sentReceiptKey = f.state.value.otpDelivery?.key.orEmpty()
                            }
                        }
                    }
                    if (otpExpired) {
                        TaskNotice("验证码已过期", "请重新获取本次用途验证码，填写最新验证码后再验证。", warning = true)
                    }
                    if (!phoneChannelAvailable) {
                        TaskNotice("当前短信渠道暂未启用", "请换用当前账号密码验证本次注销用途。", warning = true)
                    }
                } else {
                    if (methods != null && !methods.passwordSet) {
                        TaskNotice("当前账号尚未设置密码", "可切换已绑定手机号验证码，或返回账号设置先维护登录方式。")
                    }
                    MndOutlinedField(
                        value = password,
                        onValueChange = {
                            password = it
                            f.clearCommandFeedback()
                        },
                        label = { Text("当前账号密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !busy,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }

                MndQuietButton(
                    if (activeMethod == "phone") "换用已关联方式验证（改用账号密码）" else "换用已关联方式验证（改用手机号验证码）",
                    !busy,
                ) {
                    verifyMethod = if (activeMethod == "phone") "password" else "phone"
                    password = ""
                    code = ""
                    sentPhone = ""
                    sentReceiptKey = ""
                    f.clearCommandFeedback()
                }
                if (methods?.linked?.isNotEmpty() == true) {
                    Text(
                        "当前注销用途验证仅支持已绑定手机号验证码或账号密码；微信／QQ／Apple 及邮箱方式验证待共享与服务端合同补齐。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val canVerify = !busy && if (activeMethod == "password") {
                    password.isNotBlank()
                } else {
                    sentHere && !otpExpired && validAuthCode(code)
                }
                MndSecondaryButton(
                    label = if (s.stage == SensitiveStage.Verifying) "正在验证本次用途" else "验证本次用途",
                    enabled = canVerify,
                    loading = s.stage == SensitiveStage.Verifying,
                ) {
                    attemptStage = "verify_submitted"
                    finalConfirmed = false
                    val pwd = if (activeMethod == "password") password else ""
                    val otp = if (activeMethod == "phone") code else ""
                    password = ""
                    code = ""
                    f.clearCommandFeedback()
                    f.sensitive.verify(pwd, otp)
                }
                if (s.stage == SensitiveStage.Verifying) LinearProgressIndicator(Modifier.fillMaxWidth())
                common.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                common.error?.let { TaskNotice("验证码请求未完成", it, warning = true) }
                if (attemptStage == "verify_submitted") {
                    s.error?.let { TaskNotice("验证尚未通过", it, warning = true) }
                }
                MndQuietButton("返回核对注销影响", !busy) {
                    attemptStage = if (canRetryOriginal) "retry_impact" else ""
                    step = "impact"
                }
                TaskPrimary("提交注销申请", enabled = false) {}
            }

            s.stage == SensitiveStage.Submitting -> {
                Text("正在提交注销申请", style = PageHeadlineStyle)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                TaskNotice(
                    "正在受理中",
                    "正在向服务端确认本次注销申请，受理后会立即退出当前会话；请勿重复点击或关闭页面。",
                )
            }

            s.stage == SensitiveStage.Complete -> {
                val receiptText = s.resultId?.takeIf { it > 0 }?.let { "#$it" }
                    ?: "待核对（${s.clientRequestId.orEmpty()}）"
                val ref = common.restricted.firstOrNull {
                    it.type == "data_request" &&
                        (it.clientRequestId == s.clientRequestId || (s.resultId != null && it.id == s.resultId))
                }
                if (owner != null) {
                    TaskNotice(
                        "服务端已受理，本机会话待退出",
                        "原请求已核对受理；请查看受限进度或退出当前账号，勿重复创建新的注销申请。",
                        warning = true,
                    )
                }
                AccountDeletionProcessingContent(
                    receiptLabel = receiptText,
                    stateText = "处理中",
                    sessionText = if (owner == null) "已退出当前账号" else "待退出当前会话",
                    noticeTitle = "可在此查看结果",
                    noticeBody = "不会把尚未处理完成显示为已全部删除。后续处理与必要保全以服务端受限进度为准。",
                    actionTitle = "查看处理进度",
                    actionDetail = "保留必要回执",
                    busy = busy,
                    onViewProgress = { openRestrictedLookup(ref) },
                    onBrowsePublic = {
                        store.api.acknowledgeSessionEnd()
                        f.sensitive.dismiss()
                        f.closeRoute()
                        store.select(MobileTab.Community)
                    },
                )
            }

            s.stage == SensitiveStage.ResultUnknown -> {
                val ref = common.restricted.firstOrNull {
                    it.type == "data_request" && it.clientRequestId == s.clientRequestId
                }
                Text("你的申请没有丢失", style = PageHeadlineStyle)
                TaskNotice(
                    "提交结果待核对，未确认已受理",
                    "网络连接或服务端响应中断。原请求编号与本机受限查询凭证已保留；请先核对原回执，未查到结果前不重复创建申请。",
                    warning = true,
                )
                s.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                NavigationRow(
                    enabled = !busy,
                    title = "查看原回执",
                    detail = s.clientRequestId?.let { "原编号 $it · 核对是否已受理" } ?: "查看本设备受限进度",
                ) {
                    if (s.intent != null) f.sensitive.reconcile() else openRestrictedLookup(ref)
                }
                MndQuietButton("前往待核对操作", !busy) {
                    f.sensitive.dismiss()
                    f.open(FeatureRoute(FeaturePage.Recovery))
                }
                TaskPrimary("安全重试", !busy) {
                    if (s.intent != null) f.sensitive.reconcile() else openRestrictedLookup(ref)
                }
            }

            else -> {
                val ref = common.restricted.firstOrNull {
                    it.type == "data_request" && it.clientRequestId == s.clientRequestId
                }
                val capabilityInvalid = s.errorCode in listOf("query_capability_required", "idempotency_conflict")
                Text("你的申请没有丢失", style = PageHeadlineStyle)
                if (s.intent != null && s.scope != null) {
                    TaskNotice(
                        "请重试或查看进度",
                        if (capabilityInvalid) {
                            "本次请求查询凭证已失效或冲突；请先查看原回执确认是否已受理，避免重复创建申请。"
                        } else {
                            "当前仍在受控处理中，不重复创建申请。本次提交尚未获服务端确认，已保留原请求编号与核对范围。"
                        },
                        warning = true,
                    )
                    s.error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    NavigationRow(
                        enabled = !busy,
                        title = "查看原回执",
                        detail = ref?.let { if (it.id > 0) "#${it.id}" else "原编号 ${it.clientRequestId}" }
                            ?: (s.clientRequestId ?: "查看本设备受限进度"),
                    ) { openRestrictedLookup(ref) }
                    MndQuietButton("返回查看影响", !busy) {
                        attemptStage = "retry_impact"
                        finalConfirmed = false
                        step = "impact"
                    }
                    TaskPrimary("安全重试", !busy) {
                        if (capabilityInvalid) {
                            openRestrictedLookup(ref)
                        } else {
                            attemptStage = "retry_verify"
                            finalConfirmed = false
                            password = ""
                            code = ""
                            f.clearCommandFeedback()
                            step = "verify"
                        }
                    }
                } else {
                    val existingRef = ref ?: common.restricted.lastOrNull { it.type == "data_request" }
                    TaskNotice(
                        "请重试或查看进度",
                        "未能读取本次注销核对范围，尚未提交注销申请，原说明已保留，不重复创建申请。",
                        warning = true,
                    )
                    s.error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (existingRef != null) {
                        NavigationRow(
                            enabled = !busy,
                            title = "查看原回执",
                            detail = if (existingRef.id > 0) "#${existingRef.id}" else "原编号 ${existingRef.clientRequestId}",
                        ) { openRestrictedLookup(existingRef) }
                    }
                    MndQuietButton("返回修改请求", !busy) {
                        f.sensitive.dismiss()
                        f.reload()
                    }
                    TaskPrimary("安全重试", !busy && !common.unconfirmedResult) {
                        val note = f.field("note")
                        f.sensitive.dismiss()
                        f.prepareSensitive(
                            SensitiveIntent.DataRequest(
                                DataRequestInput("close_account", newRequestKey(), "", emptyList(), emptyList(), note),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AccountDeletionProcessingContent(
    receiptLabel: String,
    stateText: String,
    sessionText: String,
    noticeTitle: String,
    noticeBody: String,
    actionTitle: String,
    actionDetail: String,
    busy: Boolean,
    onViewProgress: () -> Unit,
    onBrowsePublic: () -> Unit,
) {
    Text("申请已受理", style = PageHeadlineStyle)
    AuthFacts(
        listOf(
            "回执" to receiptLabel,
            "当前状态" to stateText,
            "登录状态" to sessionText,
        ),
    )
    TaskNotice(noticeTitle, noticeBody)
    NavigationRow(!busy, actionTitle, actionDetail, onClick = onViewProgress)
    TaskPrimary("返回公开浏览", !busy, onClick = onBrowsePublic)
}

/** 由受限查询真实服务端 DataRequestStatus 驱动的注销结果／处理中／失败状态视图。 */
@Composable
fun AccountDeletionStatusRouter(
    store: MobileStore,
    f: MobileFeatureStore,
    s: FeatureState,
    status: DataRequestStatus,
    onShowList: () -> Unit,
) {
    val progress = status.progress()
    val ref = s.restricted.firstOrNull {
        it.type == "data_request" &&
            (it.id == status.id || (it.id == 0L && it.clientRequestId == s.lookupRequestId))
    }

    fun browsePublic() {
        store.api.acknowledgeSessionEnd()
        f.closeRoute()
        store.select(MobileTab.Community)
    }

    when (progress.phase) {
        DataRequestPhase.Complete -> {
            Text("账号注销已完成", style = PageHeadlineStyle)
            AuthFacts(
                listOf(
                    "回执" to "#${status.id}",
                    "登录方式" to "已解除账号关联",
                    "保留说明" to "必要记录仍受限，不对外公开",
                    "处理结果" to "已处理 ${status.erased} 项 · 无待处理与受限副本",
                ),
            )
            TaskNotice("今后使用", "再次使用须重新创建或登录其他账号。")
            if (ref != null) MndQuietButton("重新核对结果", !s.busy) { f.lookup(ref) }
            MndQuietButton("查看本设备其他受限进度", !s.busy, onClick = onShowList)
            FeaturePrimary("浏览公开内容", !s.busy, ::browsePublic)
        }

        DataRequestPhase.RetainedCopies -> {
            Text("注销主体已处理，仍有受限副本保留", style = PageHeadlineStyle)
            val facts = buildList {
                add("回执" to "#${status.id}")
                add("登录方式" to "已解除账号关联")
                add("保留说明" to "仍有 ${status.restrictedCopies} 份受限副本按规则保留，不对外公开")
                progress.nextReviewAt?.let { add("下次复核" to displayTime(it)) }
            }
            AuthFacts(facts)
            TaskNotice(
                "今后使用",
                "再次使用须重新创建或登录其他账号。受限副本仍受控保留，不会提前显示为全部清除。",
            )
            if (ref != null) MndQuietButton("重新核对结果", !s.busy) { f.lookup(ref) }
            MndQuietButton("查看本设备其他受限进度", !s.busy, onClick = onShowList)
            FeaturePrimary("浏览公开内容", !s.busy, ::browsePublic)
        }

        DataRequestPhase.Processing, DataRequestPhase.Held -> {
            Text(
                if (progress.phase == DataRequestPhase.Held) "部分记录必要保全，处理中" else "申请已受理",
                style = PageHeadlineStyle,
            )
            val facts = buildList {
                add("回执" to "#${status.id}")
                add("当前状态" to stateLabel(status.state))
                if (status.progressVersion == 1) {
                    add("分项进度" to "已处理 ${status.erased} · 待处理 ${status.pending} · 保全 ${status.held}")
                    if (status.physicalPending > 0) add("附件清理" to "物理清理待确认 ${status.physicalPending} 项")
                    if (status.providerPending > 0) add("登录方式" to "外部账号授权待处理 ${status.providerPending} 项")
                    if (status.restrictedCopies > 0) add("受限副本" to "仍有 ${status.restrictedCopies} 份受限副本保留")
                    if (status.pendingFields.isNotEmpty()) {
                        add("待处理资料" to status.pendingFields.joinToString("、", transform = ::dataFieldLabel))
                    }
                    if (status.externalState !in listOf("not_required", "completed")) {
                        add("外部副本" to dataExternalStateLabel(status.externalState))
                    }
                }
                progress.nextReviewAt?.let { add("下次复核" to displayTime(it)) }
            }
            AuthFacts(facts)
            TaskNotice(
                if (progress.phase == DataRequestPhase.Held) "必要保全与分项处理" else "可在此查看结果",
                if (progress.phase == DataRequestPhase.Held) {
                    "${progress.message}。争议、未结事项或合规审计所需记录按规则受限保留，不对外公开；其余资料分别处理，不会提前显示为已全部删除。"
                } else {
                    "不会把尚未处理完成显示为已全部删除。${progress.message}"
                },
                warning = progress.reviewRequired,
            )
            if (ref != null) {
                NavigationRow(!s.busy, "查看处理进度", "重新读取原回执 #${status.id}，不重复创建申请") { f.lookup(ref) }
            }
            MndQuietButton("查看本设备其他受限进度", !s.busy, onClick = onShowList)
            FeaturePrimary("返回公开浏览", !s.busy, ::browsePublic)
        }

        DataRequestPhase.NeedsReconciliation, DataRequestPhase.Assistance -> {
            Text("你的申请没有丢失", style = PageHeadlineStyle)
            TaskNotice(
                "请重试或查看进度",
                "当前仍在受控处理中或结果待核对，不重复创建申请。${progress.message}",
                warning = true,
            )
            AuthFacts(listOf("回执" to "#${status.id}", "当前状态" to stateLabel(status.state)))
            if (ref != null) {
                NavigationRow(!s.busy, "查看原回执", "#${status.id} · 重新向服务端核对") { f.lookup(ref) }
            }
            MndQuietButton("查看本设备其他受限进度", !s.busy, onClick = onShowList)
            FeaturePrimary("安全重试", !s.busy && ref != null) { ref?.let(f::lookup) }
        }
    }
}

/** 受限查询失败恢复卡（状态 5：注销暂未完成）。 */
@Composable
fun AccountDeletionLookupFailureCard(
    f: MobileFeatureStore,
    s: FeatureState,
    ref: RestrictedReference?,
    onShowList: () -> Unit,
) {
    Text("你的申请没有丢失", style = PageHeadlineStyle)
    TaskNotice(
        "请重试或查看进度",
        "暂时未能读取服务端回执，原请求与本机受限查询凭证仍保留，不重复创建申请。",
        warning = true,
    )
    s.error?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (ref != null) {
        NavigationRow(
            enabled = !s.busy,
            title = "查看原回执",
            detail = if (ref.id > 0) "#${ref.id}" else "原编号 ${ref.clientRequestId}",
        ) { f.lookup(ref) }
    }
    MndQuietButton("返回受限进度列表", !s.busy, onClick = onShowList)
    FeaturePrimary("安全重试", !s.busy && ref != null) { ref?.let(f::lookup) }
}
