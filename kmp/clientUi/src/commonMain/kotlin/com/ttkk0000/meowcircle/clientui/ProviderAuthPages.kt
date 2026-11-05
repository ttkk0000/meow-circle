package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

/** 官方回跳、主动账号选择与原账号关联复用同一任务骨架，短时凭据不进入UI状态。 */
@Composable fun ProviderAuthTask(store: MobileStore, state: ProviderAuthState) {
    val flow = store.providerAuth
    val platform = LocalClientPlatform.current
    val scope = "transient:provider:${state.flow}"
    var identifier by rememberSessionUiState("identifier",scope) { "" }
    var password by rememberSessionUiState("password",scope) { "" }
    var code by rememberSessionUiState("code",scope) { "" }
    var phone by rememberSessionUiState("phone",scope) { "" }
    var channel by rememberSessionUiState("channel",scope) { "phone" }
    var sentAddress by rememberSessionUiState("sentAddress",scope) { "" }
    var sentPurpose by rememberSessionUiState("sentPurpose",scope) { "" }
    var agreed by rememberSessionUiState("agreed",scope,generation = platform.policies) { false }
    val work by store.features.state.collectAsState()
    val loginPurpose = if (channel == "email") "email_login" else "login"
    val existingOtpReady = code.isNotBlank() && sentAddress == identifier.trim() && sentPurpose == loginPurpose
    val linkOtpReady = code.isNotBlank() && sentAddress == phone.trim() && sentPurpose == "reauth"
    val capabilities = store.state.value.capabilities
    val channelAvailable = when (channel) { "email" -> capabilities.email; "phone" -> capabilities.sms; else -> capabilities.password }
    val name = when(state.provider) { "wechat" -> "微信"; "qq" -> "QQ"; "apple" -> "Apple"; else -> "第三方" }
    fun close() { if(!state.busy || state.stage == ProviderAuthStage.Authorizing) { flow.dismiss(); store.cancelLogin() } }
    val title = when(state.stage) {
            ProviderAuthStage.AccountChoice -> "继续使用原账号"
            ProviderAuthStage.CreateReview -> "欢迎使用 M&D"
            ProviderAuthStage.ExistingLogin -> "关联已有账号"
            ProviderAuthStage.LinkReview -> "验证本次关联用途"
            ProviderAuthStage.LinkReady -> "确认账号关联"
            ProviderAuthStage.ResultUnknown -> "授权结果待核对"
            ProviderAuthStage.Cancelled -> "已取消授权"
            ProviderAuthStage.Unavailable -> "暂时无法使用这种方式"
            ProviderAuthStage.Expired -> "授权已失效"
            ProviderAuthStage.Conflict -> "标识已被使用"
            else -> "通过$name 登录"
        }
    val navigationEnabled = (!state.busy && !work.busy) || state.stage == ProviderAuthStage.Authorizing
    fun back() {
        if (!navigationEnabled) return
        if (state.stage in setOf(ProviderAuthStage.ExistingLogin, ProviderAuthStage.LinkReview, ProviderAuthStage.LinkReady)) flow.returnToChoice()
        else close()
    }
    if (state.stage == ProviderAuthStage.Conflict) {
        AuthConflictTask(!navigationEnabled, store.state.value.user != null,
            onReturn = ::close, onOther = { flow.dismiss(); store.showLogin() },
            onRecovery = { flow.dismiss(); store.openAuthRecovery() })
        return
    }
    if (state.stage in setOf(ProviderAuthStage.Cancelled, ProviderAuthStage.Unavailable, ProviderAuthStage.Expired)) {
        MobileTaskDialog(::close, title = title, navigationEnabled = navigationEnabled) {
            MndScreenTitle(title)
            TaskNotice("可以继续", when (state.stage) {
                ProviderAuthStage.Cancelled -> "本次授权已取消，没有创建或关联账号。刚才正在看的页面和可保留的草稿仍在。"
                ProviderAuthStage.Unavailable -> "检查对应应用是否可用，也可以改用已绑定手机号或其他方式。"
                else -> "请重新发起授权。这次不会自动创建账号，原任务和可保留的草稿仍在。"
            })
            MndQuietButton("返回刚才的页面", navigationEnabled, onClick = ::close)
            FeaturePrimary(if (state.stage == ProviderAuthStage.Unavailable) "选择其他方式" else if (state.stage == ProviderAuthStage.Expired) "重新登录" else "返回登录",
                navigationEnabled, onClick = flow::dismiss)
        }
        return
    }
    MobileTaskDialog(::back,title = title, navigationEnabled = navigationEnabled) {
        when(state.stage) {
            ProviderAuthStage.Authorizing -> {
                Surface(shape = MaterialTheme.shapes.medium,color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 96.dp),contentAlignment = Alignment.Center) { DesignIcon("provider-${state.provider}",null) }
                }
                Text("正在等待$name 授权",style = MaterialTheme.typography.headlineLarge)
                Text("请在授权页面确认允许的信息。完成后会回到 M&D。")
            }
            ProviderAuthStage.AccountChoice -> {
                TaskNotice("新身份需要主动选择","已有关联身份会进入原账号；相同昵称、头像或返回邮箱不会自动合并账号。")
                NavigationRow("已有 M&D 账号，去关联",state.account?.let { "当前账号：${it.nickname.ifBlank { it.username }}，仍需本次用途验证" } ?: "先验证原账号，再确认关联") { flow.chooseExisting() }
                if(state.account == null) NavigationRow("继续创建新账号","确认协议与年龄范围后建立",flow::chooseCreate)
                TaskNotice("不会自动合并","相同昵称、头像或第三方返回邮箱不作为合并依据。")
            }
            ProviderAuthStage.CreateReview -> {
                    AuthFirstContent(name)
                        Text("创建账号不会自动取得交流或成年人资格；年龄与监护权限仍由服务端核验。")
                        Row {
                            TextButton({ platform.openLink(platform.policies.terms) },enabled = platform.policies.ready) { Text("用户协议") }
                            TextButton({ platform.openLink(platform.policies.privacy) },enabled = platform.policies.ready) { Text("隐私政策") }
                        }
                        CheckLine("我已阅读并同意当前协议，主动创建新账号",agreed,{ agreed = it },platform.policies.ready)
                        FeaturePrimary("确认创建新账号",agreed && platform.policies.ready) { flow.create(agreed) }
                        MndSecondaryButton("我有账号，去关联") { flow.returnToChoice(); flow.chooseExisting() }
                        TextButton(flow::returnToChoice) { Text("返回账号选择") }
            }
            ProviderAuthStage.ExistingLogin -> {
                MndScreenTitle("把资料留在原来的账号")
                TaskNotice("验证你已有的 M&D 账号","关联后继续使用原有宠物、日记和聊天，不按昵称或头像自动合并。")
                FormTextField(identifier,{ identifier = it; code = ""; sentAddress = ""; sentPurpose = "" },if(channel == "phone") "原账号手机号" else if(channel == "email") "原账号邮箱" else "账号名／邮箱／手机号", enabled = !work.busy)
                if(channel == "password") MndOutlinedField(password,{ password = it },Modifier.fillMaxWidth(),label = { Text("原账号密码") },visualTransformation = PasswordVisualTransformation(),singleLine = true, enabled = !work.busy)
                else OtpVerificationFields(code,{ code = it },work.otpWait,sendLabel = "发送本次验证码",enabled = !work.busy && channelAvailable,canSend = identifier.isNotBlank()) {
                    val address = identifier.trim(); val purpose = if(channel == "email") "email_login" else "login"
                    store.features.sendOTP(email = if(channel == "email") address else "",phone = if(channel == "phone") address else "",purpose = purpose) {
                        if (identifier.trim() == address && (if(channel == "email") "email_login" else "login") == purpose) { sentAddress = address; sentPurpose = purpose; code = "" }
                    }
                }
                listOf("password" to "使用原账号密码验证", "phone" to "使用已绑定手机验证", "email" to "使用原账号邮箱验证").filter { it.first != channel }.forEach { (method, label) ->
                    val available = when (method) { "phone" -> capabilities.sms; "email" -> capabilities.email; else -> capabilities.password }
                    MndQuietButton(label, !work.busy && available) { channel = method; password = ""; code = ""; sentAddress = ""; sentPurpose = ""; store.features.clearCommandFeedback() }
                }
                if (!channelAvailable) TaskNotice("当前方式尚未由服务启用", "请选择其他已关联的登录方式，不会自动创建或接管账号。")
                FeaturePrimary("验证原账号",!work.busy && channelAvailable && identifier.isNotBlank() && (if(channel == "password") password.isNotBlank() else existingOtpReady)) {
                    flow.loginExisting(identifier,password,code,channel); password = ""; code = ""
                }
            }
            ProviderAuthStage.LinkReview -> {
                Text("将$name关联到 ${state.account?.let { it.nickname.ifBlank { it.username } }.orEmpty()}。请验证本次指定用途，再主动确认。")
                MndOutlinedField(password,{ password = it },Modifier.fillMaxWidth(),label = { Text("当前账号密码（可用短信验证码替代）") },visualTransformation = PasswordVisualTransformation(),singleLine = true, enabled = !work.busy)
                FormTextField(phone,{ phone = it; code = ""; sentAddress = ""; sentPurpose = "" },"当前完整手机号（仅用于本次验证）", enabled = !work.busy)
                OtpVerificationFields(code,{ code = it },work.otpWait,sendLabel = "发送本次验证码",enabled = !work.busy && capabilities.sms,canSend = phone.isNotBlank()) {
                    val address = phone.trim()
                    store.features.sendOTP(phone = address,purpose = "reauth") { if (phone.trim() == address) { sentAddress = address; sentPurpose = "reauth"; code = "" } }
                }
                FeaturePrimary("验证本次关联用途",!work.busy && (password.isNotBlank() || linkOtpReady)) { flow.verifyLink(password,if (linkOtpReady) code else ""); password = ""; code = "" }
            }
            ProviderAuthStage.LinkReady -> {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MobilePhoto(store,avatarMediaId(state.account?.avatarUrl.orEmpty()),state.account?.id,"当前账号头像",Modifier.size(56.dp).clip(MaterialTheme.shapes.medium))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(state.account?.let { it.nickname.ifBlank { it.username } }.orEmpty(),style = MaterialTheme.typography.titleMedium)
                            Text(state.account?.username.orEmpty(),style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                AuthFacts(listOf("将关联的方式" to name))
                TaskNotice("只关联登录方式","不会覆盖原有宠物、日记、群和历史消息，也不改变交流资格。")
                MndQuietButton("取消关联", navigationEnabled, onClick = flow::returnToChoice)
                FeaturePrimary("确认关联", !state.busy && state.account != null) { flow.confirmLink(true) }
            }
            ProviderAuthStage.Checking, ProviderAuthStage.Verifying, ProviderAuthStage.Creating, ProviderAuthStage.Linking -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在处理本次授权，请保留当前任务。") }
            ProviderAuthStage.Cancelled -> TaskNotice("可以继续","刚才正在看的页面和可保留的草稿仍在，没有创建或关联账号。")
            ProviderAuthStage.Unavailable -> TaskNotice("可以继续","可以选择其他登录方式，当前页面和可保留的草稿仍在。")
            ProviderAuthStage.Expired -> TaskNotice("需要重新核对","本次授权已经结束，重新发起官方授权后再选择账号。原任务和可保留的草稿仍在。")
            ProviderAuthStage.Conflict -> AccountConflictNotice()
            ProviderAuthStage.ResultUnknown -> TaskNotice("不会自动重发","重新进行官方授权只用于核对当前已关联账号；新账号和关联仍需你明确选择。")
            ProviderAuthStage.Failed -> if(flow.canChoose) FeatureButton("返回账号选择",true,flow::returnToChoice)
            ProviderAuthStage.Idle -> Unit
        }
        state.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
        work.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
        if(state.stage in setOf(ProviderAuthStage.Failed,ProviderAuthStage.ResultUnknown)) FeaturePrimary("重新官方授权核对账号",state.provider in platform.providers) {
            store.startProvider(state.provider,ProviderLauncher { provider, challenge -> platform.authorizeProvider(provider,challenge) })
        }
        if(state.stage == ProviderAuthStage.AccountChoice) TaskSecondary("返回登录方式",onClick = flow::dismiss)
        else if (state.stage !in setOf(ProviderAuthStage.ExistingLogin, ProviderAuthStage.LinkReady)) MndQuietButton(if(state.stage == ProviderAuthStage.Authorizing) "取消本次授权" else "返回登录方式", navigationEnabled, onClick = flow::dismiss)
    }
}

@Composable fun AccountConflictNotice() {
    TaskNotice("保留原绑定","邮箱、手机号或第三方身份已有绑定时，不覆盖原资料、不迁移聊天，也不透露其他账号信息。请登录或找回原账号，或选择其他标识。持有新号码本身不能接管其过去关联的账号。")
}

@Composable private fun FormTextField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean = true) =
    MndOutlinedField(value,onChange,Modifier.fillMaxWidth(),label = { Text(label) },singleLine = true, enabled = enabled)

/** 登录与安全设置共用官方授权入口和本次隐私确认。 */
@Composable fun ProviderEntry(store: MobileStore, available: List<String>, busy: Boolean, scope: String? = null, heading: String = "官方授权", knownLayout: Boolean = false, onManageAccount: (() -> Unit)? = null) {
    val platform = LocalClientPlatform.current
    var selected by rememberSessionUiState("provider.consent",scope) { "" }
    var information by rememberSessionUiState("provider.information",scope) { false }
    val choices = listOf("wechat" to "微信","qq" to "QQ") + if(platform.kind == ClientPlatformKind.IOS) listOf("apple" to "Apple") else emptyList()
    Text(heading,style = if (knownLayout) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelMedium)
    val rows = if (knownLayout) choices.chunked(2) else listOf(choices)
    rows.forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { (provider,label) ->
            val apple = knownLayout && provider == "apple"
            val colors = if (apple) ButtonDefaults.outlinedButtonColors(containerColor = Color.Black, contentColor = Color.White,
                disabledContainerColor = Color.Black.copy(alpha = .5f), disabledContentColor = Color.White.copy(alpha = .5f)) else ButtonDefaults.outlinedButtonColors()
            OutlinedButton({
            if(provider !in platform.providers || provider !in available) store.providerAuth.unavailable(provider) else { selected = provider; information = false }
        },enabled = !busy,modifier = Modifier.weight(1f).heightIn(min = MobileUi.actionHeight), colors = colors) {
            DesignIcon("provider-$provider",null,modifier = if (knownLayout) Modifier.size(20.dp) else Modifier,
                colorFilter = if (apple) ColorFilter.tint(Color.White) else null)
            Spacer(Modifier.width(MobileUi.compactGap)); Text(if (apple) "通过 Apple 登录" else label,
                style = if (knownLayout) MaterialTheme.typography.labelMedium else LocalTextStyle.current)
        } }
    } }
    if (selected.isNotBlank() && information) {
        AuthInformationTask(AuthInformationPage.Providers, onBack = { information = false }, onPage = {},
            onExit = { information = false; selected = "" }, onManage = {
                information = false; selected = ""
                if (onManageAccount != null) onManageAccount() else {
                    store.cancelLogin(); store.features.open(FeatureRoute(FeaturePage.Security))
                    if (store.state.value.user == null) store.showLogin()
                }
            })
    } else if(selected.isNotBlank()) MobileTaskDialog({ if (!busy) selected = "" }, title = "本次官方授权", navigationEnabled = !busy) {
        Text("将打开官方授权页面，使用本次返回的身份完成登录。SDK会处理必要设备与网络信息；此步不会自动创建或合并账号。")
        val policy = when(selected) { "wechat" -> "https://weixin.qq.com/cgi-bin/readtemplate?lang=zh_CN&t=weixin_agreement&s=privacy"; "qq" -> "https://wiki.connect.qq.com/qq互联sdk隐私保护声明"; else -> "https://www.apple.com/legal/privacy/" }
        NavigationRow("第三方登录信息", "查看 SDK 与提供方范围") { if (!busy) information = true }
        MndQuietButton("阅读官方隐私说明", !busy) { platform.openLink(policy) }
        MndQuietButton("取消", !busy) { selected = "" }
        FeaturePrimary("同意并进行本次授权", !busy) { val provider = selected; selected = ""; store.startProvider(provider,ProviderLauncher { id, challenge -> platform.authorizeProvider(id,challenge) }) }
    }
}
