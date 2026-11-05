package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ttkk0000.meowcircle.client.*

private fun authAddress(value: String): String {
    val clean = value.trim()
    return if (clean.contains('@')) clean.take(1) + "***@" + clean.substringAfter('@')
        else if (clean.length >= 7) clean.take(3) + " **** " + clean.takeLast(4) else "****"
}

@Composable fun ClientAuthDialog(store: MobileStore, s: MobileState) {
    val f = store.features
    val work by f.state.collectAsState()
    val sensitive by f.sensitive.state.collectAsState()
    val platform = LocalClientPlatform.current
    val providerState by store.providerAuth.state.collectAsState()
    if (providerState.stage != ProviderAuthStage.Idle) { ProviderAuthTask(store, providerState); return }
    var mode by rememberSessionUiState("mode", "transient:auth") { if (s.user == null && f.api.rememberedIdentity != null) "known" else "password" }
    var step by rememberSessionUiState("step", "transient:auth") { "start" }
    var identifier by rememberSessionUiState("identifier", "transient:auth") { "" }
    var sentIdentifier by rememberSessionUiState("sentIdentifier", "transient:auth") { "" }
    var sentMode by rememberSessionUiState("sentMode", "transient:auth") { "" }
    var sentReceiptKey by rememberSessionUiState("sentReceiptKey", "transient:auth") { "" }
    var phoneError by rememberSessionUiState("phoneError", "transient:auth") { false }
    var resent by rememberSessionUiState("resent", "transient:auth") { false }
    var password by rememberSessionUiState("password", "transient:auth") { "" }
    var repeated by rememberSessionUiState("repeated", "transient:auth") { "" }
    var code by rememberSessionUiState("code", "transient:auth") { "" }
    var username by rememberSessionUiState("username", "transient:auth") { "" }
    var agreed by rememberSessionUiState("agreed", "transient:auth", generation = platform.policies) { false }
    var information by rememberSessionUiState("information", "transient:auth") { emptyList<AuthInformationPage>() }
    var ended by rememberSessionUiState<SessionEnd?>("sessionEnd", "transient:auth") {
        f.api.sessionEnd?.takeIf { it.kind != SessionEndKind.AccountRequest }
    }
    val busy = s.busy || work.busy || sensitive.stage == SensitiveStage.Submitting
    val completion = s.auth.completion
    if (completion != null && s.user?.id == completion.owner && f.api.user.value?.id == completion.owner) {
        if (completion.profile) AuthProfileTask(store)
        else if (completion.reviewReturn) AuthReturnTaskPage(store, busy)
        else AuthSuccessTask(store, s, completion.created, busy)
        return
    }
    if (s.auth.recovery) { AuthRecoveryTask(store); return }
    if (ended != null) {
        LaunchedEffect(sensitive.stage) {
            if (ended?.kind == SessionEndKind.SecurityChange && sensitive.stage == SensitiveStage.Complete && sensitive.resultKind == "accounts")
                f.sensitive.dismiss()
        }
        SessionEndTask(ended!!, busy, s.notice, onClose = store::cancelLogin, onLogin = {
            if (!busy) {
                f.api.acknowledgeSessionEnd(); ended = null; mode = if (f.api.rememberedIdentity != null) "known" else "password"; step = "start"
                password = ""; repeated = ""; code = ""; sentMode = ""; sentIdentifier = ""
                store.editAuth(AuthForm(visible = true)); f.clearCommandFeedback()
            }
        })
        return
    }
    if (information.isNotEmpty()) {
        AuthInformationTask(information.last(), onBack = { information = information.dropLast(1) },
            onPage = { information = if (it == AuthInformationPage.Entry) listOf(it) else information + it },
            onExit = { information = emptyList() }, onManage = {
                information = emptyList(); mode = "password"; step = "start"; password = ""; repeated = ""; code = ""; sentMode = ""; sentIdentifier = ""; agreed = false
                store.cancelLogin(); f.open(FeatureRoute(FeaturePage.Security))
                if (store.state.value.user == null) store.showLogin()
            })
        return
    }
    val policyReady = platform.policies.ready
    val known = f.api.rememberedIdentity
    if (mode == "known" && s.user == null && known != null) {
        AuthKnownTask(store, known, busy, onPhone = { mode = "phone"; step = "start" }, onOther = { mode = "password"; step = "start" })
        return
    }
    val phone = mode == "phone" || mode == "phone-recover"
    val recovery = mode == "recover" || mode == "phone-recover"
    val channel = if (phone) s.capabilities.sms else s.capabilities.email
    val requiresAgreement = mode == "register" || mode == "email"
    val consentReady = !requiresAgreement || policyReady && agreed
    val sent = sentMode == mode && sentIdentifier == identifier.trim() && sentIdentifier.isNotEmpty() &&
        sentReceiptKey.isNotEmpty() && sentReceiptKey == work.otpDelivery?.key
    val passwordReady = password.encodeToByteArray().size in 10..128 && password == repeated
    fun clearFeedback() { store.editAuth(AuthForm(visible = true)); f.clearCommandFeedback() }
    fun changeMode(value: String) {
        if (busy) return
        mode = value; step = "start"; code = ""; password = ""; repeated = ""; agreed = false
        sentMode = ""; sentIdentifier = ""; sentReceiptKey = ""; phoneError = false; resent = false; clearFeedback()
    }
    fun back() {
        if (busy) return
        when (step) {
            "create-review" -> { step = "code"; clearFeedback() }
            "code" -> { step = "start"; code = ""; clearFeedback() }
            "reset" -> { step = "code"; password = ""; repeated = ""; clearFeedback() }
            "help" -> { step = if (sent) "code" else "start"; clearFeedback() }
            "done" -> changeMode("password")
            else -> store.cancelLogin()
        }
    }
    fun sendCode() {
        if (busy || !channel || !consentReady || work.otpWait > 0 || identifier.isBlank()) return
        if (phone && !validLoginPhone(identifier)) { clearFeedback(); phoneError = true; return }
        val address = identifier.trim()
        val resending = sent && phone
        val purpose = when (mode) { "register" -> "email_register"; "recover" -> "email_recover"; "phone-recover" -> "recover"; "phone" -> "login"; else -> "email_login" }
        f.sendOTP(email = if (phone) "" else address, phone = if (phone) address else "", purpose = purpose) {
            sentIdentifier = address; sentMode = mode; sentReceiptKey = f.state.value.otpDelivery?.key.orEmpty()
            code = ""; step = "code"; phoneError = false; resent = resending
        }
    }
    val failure = s.auth.error ?: work.error
    val failureType = if (s.auth.error != null) s.auth.failure else work.commandFailure
    val errorCode = if (s.auth.error != null) s.auth.errorCode else work.commandErrorCode
    val receiptExpired = sent && work.otpDelivery?.expired == true
    val explicitExpired = errorCode in listOf("otp_expired", "code_expired")
    if (!busy && step in listOf("code", "reset", "create-review") && (receiptExpired && failure == null || explicitExpired)) {
        AuthCodeExpiredTask(busy, work.otpWait, channel && consentReady,
            onSend = { password = ""; repeated = ""; code = ""; clearFeedback(); sendCode() },
            onReturn = store::cancelLogin, onBack = ::back)
        return
    }
    if (phone && step == "help") {
        AuthSmsDelayedTask(busy, onEdit = { step = "start"; code = ""; clearFeedback() },
            onReturn = store::cancelLogin, onRecovery = store::openAuthRecovery, onBack = ::back)
        return
    }
    if (phone && step == "start" && phoneError && failure == null) {
        AuthPhoneErrorTask(identifier, { identifier = it; code = ""; sentMode = ""; clearFeedback() }, busy, agreed, { agreed = it },
            channel && consentReady, work.otpWait, ::sendCode, { changeMode("password") }, ::back)
        return
    }
    if (phone && step == "code" && resent && failure == null) {
        AuthCodeResentTask(authAddress(sentIdentifier), code, { code = it }, busy, sent && channel && consentReady,
            onSubmit = { if (recovery) { password = ""; repeated = ""; step = "reset"; clearFeedback() } else { store.loginPhone(sentIdentifier, code); code = "" } },
            onOther = { changeMode("password") }, onBack = ::back)
        return
    }
    if (failure != null && errorCode == "identity_in_use") {
        AuthConflictTask(busy, s.user != null, store::cancelLogin,
            onOther = { identifier = ""; username = ""; changeMode("password") },
            onRecovery = { clearFeedback(); store.openAuthRecovery() })
        return
    }
    val failurePage = failure != null && step !in listOf("help", "done")
    val expired = errorCode in listOf("otp_expired", "code_expired")
    val limited = failureType == ClientReadFailure.Busy
    val failureTitle = when {
        expired -> "验证码已过期"
        limited -> "操作有些频繁"
        failureType == ClientReadFailure.Connection -> "连接暂时中断"
        step in listOf("code", "reset") -> "验证码暂不可用"
        else -> "尚未完成登录"
    }
    val title = if (failurePage) failureTitle else when (step) {
        "create-review" -> "欢迎使用 M&D"
        "help" -> { if (phone) "暂时没有收到短信" else "验证码暂不可用" }
        "reset" -> "设置新密码"
        "done" -> "密码已更新"
        "code" -> { if (phone) "验证手机号" else "验证邮箱" }
        else -> when (mode) { "email" -> "邮箱登录 / 注册"; "register" -> "邮箱注册"; "phone" -> "已绑定手机登录"; "recover", "phone-recover" -> "找回密码"; else -> "密码登录" }
    }
    MobileTaskDialog(onDismissRequest = ::back, title = title, navigationEnabled = !busy) {
        if (failurePage) {
            MndScreenTitle(failureTitle)
            TaskNotice("可以继续", when {
                limited -> "请稍后再试，也可以选择其他登录方式。原来的内容会保留。"
                expired -> "重新获取后填写最新验证码，无需重新开始刚才的操作。"
                else -> "尚未确认本次登录或验证，请核对后继续。原账号、绑定与当前任务保持，不会自动提交草稿。"
            })
            Text(failure.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            MndQuietButton("返回刚才的页面", !busy) { store.cancelLogin() }
            FeaturePrimary(if (limited) "换个方式" else if (expired && work.otpWait > 0) "${work.otpWait}秒后可重发" else if (expired) "重新获取" else "重新尝试",
                !busy && (!expired || work.otpWait == 0 && channel && consentReady)) {
                when {
                    limited -> changeMode("password")
                    expired || step == "start" && mode != "password" -> { clearFeedback(); sendCode() }
                    else -> { if (step == "reset" || step == "create-review") step = "code"; clearFeedback() }
                }
            }
        } else when (step) {
            "create-review" -> {
                AuthFirstContent("邮箱 · ${authAddress(sentIdentifier)}")
                Text("本次验证码和账号信息将在确认创建时由服务核对。", style = MaterialTheme.typography.bodySmall)
                MndSecondaryButton("我有账号，去登录", !busy) { changeMode("password") }
                MndQuietButton("返回核对邮箱与验证码", !busy) { step = "code"; clearFeedback() }
                FeaturePrimary("继续并创建账号", !busy && sent && validAuthCode(code) && passwordReady && consentReady && s.capabilities.registration) {
                    store.loginEmail(EmailAuthInput(sentIdentifier, code, username.trim(), password, acceptTerms = agreed), true)
                    code = ""; password = ""; repeated = ""
                }
            }
            "done" -> {
                MndScreenTitle("原来的资料都在")
                TaskNotice("重新登录", "使用新密码或已关联的方式，回到刚才的页面；不会自动发布或提交草稿。")
                FeaturePrimary("返回登录") { changeMode("password") }
            }
            "help" -> {
                MndScreenTitle("未完成验证")
                TaskNotice("保留当前任务", "验证码错误、过期、发送频繁或网络异常时保持原账号与绑定，不会自动建号、合并账号或发送草稿。")
                NavigationRow(if (phone) "检查已绑定号码" else "检查邮箱地址", authAddress(identifier)) { step = "start"; code = ""; clearFeedback() }
                MndQuietButton("换个登录方式", !busy) { changeMode("password") }
                MndQuietButton("无法使用原标识，寻求帮助", !busy, onClick = store::openAuthRecovery)
                FeaturePrimary(if (sent) "重新填写验证码" else "返回填写", !busy) { step = if (sent) "code" else "start"; clearFeedback() }
            }
            "reset" -> {
                MndOutlinedField(password, { password = it }, label = { Text("新密码") }, enabled = !busy,
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    supportingText = { Text("使用10至128字节的密码") })
                MndOutlinedField(repeated, { repeated = it }, label = { Text("再次确认") }, enabled = !busy,
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    isError = repeated.isNotEmpty() && repeated != password,
                    supportingText = { if (repeated.isNotEmpty() && repeated != password) Text("两次密码不一致") })
                TaskNotice("下次登录", "可以继续使用已关联的快捷登录或新密码。保存时会一并验证刚才的原账号验证码。")
                if (code.isBlank() || work.error != null) NavigationRow("重新填写验证码", "保留原账号和任务") { step = "code"; clearFeedback() }
                FeaturePrimary("保存新密码", !busy && sent && validAuthCode(code) && passwordReady) {
                    val address = sentIdentifier; val otp = code; val nextPassword = password
                    password = ""; repeated = ""; code = ""
                    f.runCommand {
                        if (phone) f.api.recoverPhone(PhoneRecoveryInput(address, otp, nextPassword))
                        else f.api.recoverEmail(EmailAuthInput(address, otp, password = nextPassword))
                        step = "done"
                    }
                }
            }
            else -> {
                if (mode != "password") {
                    MndScreenTitle(if (step == "code") { if (phone) "查看手机中的验证码" else "查看邮箱中的验证码" }
                        else if (recovery) "恢复你原来的账号" else if (phone) "使用已绑定的手机号" else "使用你已验证的邮箱")
                }
                if (step == "code") {
                    Text("发送至 ${authAddress(sentIdentifier)}", style = MaterialTheme.typography.bodySmall)
                    AuthOtpField(code, { code = it; clearFeedback() }, !busy, if (phone) "短信验证码，6位数字" else "邮箱验证码，6位数字")
                    NavigationRow(if (phone) "修改手机号" else "修改邮箱", "保留原任务") { step = "start"; code = ""; clearFeedback() }
                    NavigationRow("验证码错误 / 过期 / 发送频繁", "重试不会新建账号") { step = "help"; clearFeedback() }
                    MndQuietButton(if (work.otpWait > 0) "${work.otpWait}秒后可重发" else "重新获取验证码", !busy && work.otpWait == 0, onClick = ::sendCode)
                    TaskNotice("保护原账号", if (recovery) "验证码仅用于重设原账号密码，不新建账号。下一步设置新密码，保存时由服务端核对验证码。"
                        else "验证码仅用于本次登录或明确选择的注册，不授权更换绑定或自动发布。")
                    if (mode == "register") {
                        MndOutlinedField(username, { username = it }, label = { Text("唯一账号名") }, enabled = !busy, singleLine = true)
                        MndOutlinedField(password, { password = it }, label = { Text("密码（10至128字节）") }, enabled = !busy,
                            singleLine = true, visualTransformation = PasswordVisualTransformation())
                        MndOutlinedField(repeated, { repeated = it }, label = { Text("再次确认") }, enabled = !busy,
                            singleLine = true, visualTransformation = PasswordVisualTransformation(), isError = repeated.isNotEmpty() && repeated != password)
                    }
                    FeaturePrimary(if (recovery) "继续设置新密码" else if (mode == "register") "核对创建账号" else "验证并继续",
                        !busy && channel && sent && validAuthCode(code) && consentReady && (mode != "register" || s.capabilities.registration && username.isNotBlank() && passwordReady)) {
                        when {
                            recovery -> { password = ""; repeated = ""; step = "reset"; clearFeedback() }
                            mode == "register" -> { step = "create-review"; clearFeedback() }
                            phone -> { store.loginPhone(sentIdentifier, code); code = "" }
                            else -> { store.loginEmail(EmailAuthInput(sentIdentifier, code, if (mode == "register") username else "", if (mode == "register") password else "", acceptTerms = agreed), mode == "register"); code = ""; password = ""; repeated = "" }
                        }
                    }
                } else {
                    if (mode == "password" && policyReady || requiresAgreement) {
                        CheckLine("我已阅读并同意服务协议与隐私政策", agreed, { agreed = it }, policyReady && !busy)
                        MndQuietButton("服务协议", policyReady && !busy) { if (!platform.openLink(platform.policies.terms)) f.notify("暂无法打开协议") }
                        MndQuietButton("隐私政策", policyReady && !busy) { if (!platform.openLink(platform.policies.privacy)) f.notify("暂无法打开隐私政策") }
                    }
                    MndOutlinedField(identifier, { identifier = it; code = ""; sentMode = ""; clearFeedback() },
                        label = { Text(if (mode == "password") "账号名 / 邮箱 / 手机号" else if (phone) "已绑定手机号" else "邮箱") },
                        enabled = !busy, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (mode == "password") KeyboardType.Text else if (phone) KeyboardType.Phone else KeyboardType.Email))
                    if (mode == "password") {
                        MndOutlinedField(password, { password = it; clearFeedback() }, label = { Text("密码") }, enabled = !busy,
                            singleLine = true, visualTransformation = PasswordVisualTransformation(), isError = s.auth.error != null)
                        NavigationRow("忘记密码", "验证后设置新密码") { changeMode("recover") }
                        MndQuietButton("改用邮箱验证码", !busy) { changeMode("email") }
                        FeaturePrimary("登录", !busy && s.capabilities.password && identifier.isNotBlank() && password.isNotBlank() && (!policyReady || agreed)) {
                            store.editAuth(AuthForm(true, identifier, password)); store.login(); password = ""
                        }
                    } else {
                        if (requiresAgreement) TaskNotice("已有账号不会重复创建", "登录已绑定邮箱会进入原账号；创建新账号需要你主动选择注册，不按同名昵称合并。")
                        else if (recovery) TaskNotice("保护原账号", "使用已验证的标识恢复访问。当前登录态或新号码不能直接授权接管。")
                        else TaskNotice("仅登录原账号", "手机号必须已绑定；验证码不会自动新建账号或代替交流资格。")
                        NavigationRow(if (phone) "短信未到 / 原号码不可用" else "邮件未到 / 地址填写错误", "查看恢复帮助") { step = "help"; clearFeedback() }
                        FeaturePrimary(if (work.otpWait > 0) "${work.otpWait}秒后可重发" else if (phone) "发送短信验证码" else "发送邮箱验证码",
                            !busy && channel && consentReady && identifier.isNotBlank() && work.otpWait == 0 && (mode != "register" || s.capabilities.registration), ::sendCode)
                    }
                    if (!policyReady && requiresAgreement) Text("正式协议地址尚未配置，暂不开放此流程；已有账号仍可使用密码登录。")
                    if (mode != "password" && !channel) TaskNotice("当前渠道尚未启用", "请使用已有账号密码或其他已绑定的登录方式。")
                    MndQuietButton(if (mode == "password") "邮箱注册" else "返回密码登录", !busy) { changeMode(if (mode == "password") "register" else "password") }
                    if (mode != "phone") MndQuietButton("已绑定手机登录", !busy) { changeMode("phone") }
                    if (recovery) MndQuietButton(if (phone) "邮箱找回密码" else "已绑定手机恢复", !busy) { changeMode(if (phone) "recover" else "phone-recover") }
                    ProviderEntry(store, s.capabilities.providers, busy, scope = "transient:auth", onManageAccount = {
                        changeMode("password"); store.cancelLogin(); f.open(FeatureRoute(FeaturePage.Security)); store.showLogin()
                    })
                    MndQuietButton("服务协议与隐私说明", !busy) { information = listOf(AuthInformationPage.Entry) }
                    MndQuietButton("阅读服务说明与规则草案", !busy) { store.cancelLogin(); f.open(FeatureRoute(FeaturePage.Rules)) }
                    MndQuietButton("继续以游客身份浏览", !busy) { store.cancelLogin() }
                }
            }
        }
        s.capabilityError?.let {
            TaskNotice("登录方式尚未读到", it)
            MndSecondaryButton("重新读取登录方式", !busy, onClick = store::refreshCapabilities)
        }
        work.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable internal fun AuthFirstContent(method: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("M&D", fontFamily = BrandFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp)
        Text("meow & doggie", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    MndScreenTitle("和毛孩子，记录每一天")
    AuthFacts(listOf("登录方式" to method))
    TaskNotice("首次使用", "继续将创建一个M&D账号。昵称、头像和宠物资料都可以稍后补充。")
}

@Composable private fun AuthSuccessTask(store: MobileStore, s: MobileState, created: Boolean, busy: Boolean) {
    val user = s.user ?: return
    MobileTaskDialog(store::finishLogin, title = "登录成功", navigationEnabled = !busy) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MobilePhoto(store, avatarMediaId(user.avatarUrl.orEmpty()), user.id, "当前账号头像", Modifier.size(56.dp).clip(MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(user.nickname.ifBlank { user.username }, style = MaterialTheme.typography.titleMedium)
                    Text("已登录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        MndScreenTitle("继续刚才的事情")
        TaskNotice("内容已保留", "你可以检查草稿后再发送或发布，不会自动完成上一步。")
        NavigationRow(!busy, "登录后回到哪里", store.authReturnTarget().title, 56.dp) { store.reviewAuthReturn(true) }
        if (created) NavigationRow("设置头像与昵称", "可稍后补充，不影响返回原任务") {
            if (!busy) store.openOptionalProfile()
        }
        FeaturePrimary("返回并继续", !busy, onClick = store::finishLogin)
    }
}

@Composable private fun AuthReturnTaskPage(store: MobileStore, busy: Boolean) {
    val target = store.authReturnTarget()
    MobileTaskDialog({ store.reviewAuthReturn(false) }, title = "登录后回到哪里", navigationEnabled = !busy) {
        NavigationRow(!busy, target.title, target.detail, 56.dp, store::finishLogin)
        FeaturePrimary("返回刚才的页面", !busy, store::finishLogin)
    }
}

@Composable private fun SessionEndTask(end: SessionEnd, busy: Boolean, notice: String?, onClose: () -> Unit, onLogin: () -> Unit) {
    val signedOut = end.kind == SessionEndKind.SignedOut
    val changed = end.kind == SessionEndKind.SecurityChange
    val title = when {
        signedOut -> "已退出登录"
        !changed -> "登录状态已过期"
        end.action == "phone" -> "手机号已更新"
        end.action == "email" -> "邮箱已更新"
        end.action == "password" -> "密码已更新"
        else -> "登录方式已更新"
    }
    MobileTaskDialog(onDismissRequest = onClose, title = title, navigationEnabled = !busy) {
        MndScreenTitle(when {
            signedOut -> "随时回来，继续记录"
            changed && end.maskedAddress.isNotEmpty() -> "已更新为${end.maskedAddress}"
            else -> title
        })
        if (signedOut) {
            MndSecondaryButton("重新登录", !busy, onClick = onLogin)
            notice?.takeIf { it != "已退出登录" }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        } else {
            TaskNotice(if (changed) "账号没有变" else "可以继续", if (changed)
                "设置已由服务确认。旧会话已失效，请重新登录后返回账号设置；仍可使用其他已关联的登录方式。"
                else "重新登录后返回当前任务，草稿不会自动提交。")
            if (!changed) MndQuietButton("返回刚才的页面", !busy, onClick = onClose)
        }
        FeaturePrimary(if (signedOut) "先逛逛" else if (changed) "重新登录并返回设置" else "重新登录", !busy,
            onClick = if (signedOut) onClose else onLogin)
    }
}
