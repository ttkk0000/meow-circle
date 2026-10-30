package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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
    var channel by rememberSessionUiState("channel",scope) { "password" }
    var agreed by rememberSessionUiState("agreed",scope,generation = platform.policies) { false }
    var confirmed by rememberSessionUiState("confirmed",scope,generation = state.stage) { false }
    val work by store.features.state.collectAsState()
    val name = when(state.provider) { "wechat" -> "微信"; "qq" -> "QQ"; "apple" -> "Apple"; else -> "第三方" }
    fun close() { if(!state.busy || state.stage == ProviderAuthStage.Authorizing) { flow.dismiss(); store.cancelLogin() } }
    val title = when(state.stage) {
            ProviderAuthStage.AccountChoice -> "继续使用原账号"
            ProviderAuthStage.CreateReview -> "继续创建新账号"
            ProviderAuthStage.ExistingLogin -> "关联已有账号"
            ProviderAuthStage.LinkReview, ProviderAuthStage.LinkReady -> "确认关联$name"
            ProviderAuthStage.ResultUnknown -> "授权结果待核对"
            ProviderAuthStage.Cancelled -> "已取消授权"
            ProviderAuthStage.Unavailable -> "暂时无法使用这种方式"
            ProviderAuthStage.Expired -> "本次授权已过期"
            ProviderAuthStage.Conflict -> "标识已被使用"
            else -> "通过$name 登录"
        }
    MobileTaskDialog(::close,title = title) {
        if(state.stage in setOf(ProviderAuthStage.Cancelled,ProviderAuthStage.Unavailable,ProviderAuthStage.Expired)) Text(title,style = MaterialTheme.typography.headlineLarge)
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
                    TaskSection("继续创建新账号") {
                        Text("创建账号不会自动取得交流或成年人资格；年龄与监护权限仍由服务端核验。")
                        Row {
                            TextButton({ platform.openLink(platform.policies.terms) },enabled = platform.policies.ready) { Text("用户协议") }
                            TextButton({ platform.openLink(platform.policies.privacy) },enabled = platform.policies.ready) { Text("隐私政策") }
                        }
                        CheckLine("我已阅读并同意当前协议，主动创建新账号",agreed,{ agreed = it },platform.policies.ready)
                        FeaturePrimary("确认创建新账号",agreed && platform.policies.ready) { flow.create(agreed) }
                        TextButton(flow::returnToChoice) { Text("返回账号选择") }
                    }
            }
            ProviderAuthStage.ExistingLogin -> {
                Text("把资料留在原来的账号",style = MaterialTheme.typography.headlineLarge)
                TaskNotice("验证你已有的 M&D 账号","关联后继续使用原有宠物、日记和聊天，不按昵称或头像自动合并。")
                SegmentedTextBar(listOf("password" to "密码","email" to "邮箱验证码","phone" to "已绑定手机"),channel,{ channel = it; password = ""; code = "" })
                FormTextField(identifier,{ identifier = it },if(channel == "phone") "已绑定手机号" else if(channel == "email") "邮箱" else "账号名／邮箱／手机号")
                if(channel == "password") MndOutlinedField(password,{ password = it },Modifier.fillMaxWidth(),label = { Text("原账号密码") },visualTransformation = PasswordVisualTransformation(),singleLine = true)
                else OtpVerificationFields(code,{ code = it },work.otpWait,sendLabel = "发送本次验证码",enabled = !work.busy,canSend = identifier.isNotBlank()) {
                    store.features.sendOTP(email = if(channel == "email") identifier else "",phone = if(channel == "phone") identifier else "",purpose = if(channel == "email") "email_login" else "login")
                }
                FeaturePrimary("验证原账号",identifier.isNotBlank() && if(channel == "password") password.isNotBlank() else code.isNotBlank()) {
                    flow.loginExisting(identifier,password,code,channel); password = ""; code = ""
                }
                Text("此步只验证原账号，不提前合并第三方身份。")
            }
            ProviderAuthStage.LinkReview -> {
                Text("将$name关联到 ${state.account?.let { it.nickname.ifBlank { it.username } }.orEmpty()}。请验证本次指定用途，再主动确认。")
                MndOutlinedField(password,{ password = it },Modifier.fillMaxWidth(),label = { Text("当前账号密码（可用短信验证码替代）") },visualTransformation = PasswordVisualTransformation(),singleLine = true)
                FormTextField(phone,{ phone = it },"当前完整手机号（仅用于本次验证）")
                OtpVerificationFields(code,{ code = it },work.otpWait,sendLabel = "发送本次验证码",enabled = !work.busy,canSend = phone.isNotBlank()) { store.features.sendOTP(phone = phone,purpose = "reauth") }
                FeaturePrimary("验证本次关联用途",password.isNotBlank() || code.isNotBlank()) { flow.verifyLink(password,code); password = ""; code = "" }
            }
            ProviderAuthStage.LinkReady -> {
                TaskSection("核对当前账号") {
                    Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        avatarMediaId(state.account?.avatarUrl.orEmpty())?.let { id -> MobilePhoto(store,id,state.account?.id,"当前账号头像",Modifier.size(56.dp).clip(MaterialTheme.shapes.medium)) }
                        Column { Text(state.account?.nickname.orEmpty(),style = MaterialTheme.typography.titleMedium); Text(state.account?.username.orEmpty(),style = MaterialTheme.typography.bodySmall) }
                    }
                }
                TaskSection("将关联的方式") { Text(name) }
                TaskNotice("只关联登录方式","不会覆盖原有宠物、日记、群和历史消息，也不改变交流资格。")
                CheckLine("我确认将本次$name身份关联到上述原账号",confirmed,{ confirmed = it })
                FeaturePrimary("确认关联",confirmed) { flow.confirmLink(confirmed) }
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
        if(state.stage in setOf(ProviderAuthStage.Cancelled,ProviderAuthStage.Unavailable,ProviderAuthStage.Expired)) TextButton(::close) { Text("返回刚才的页面") }
        if(state.stage in setOf(ProviderAuthStage.Failed,ProviderAuthStage.Expired,ProviderAuthStage.ResultUnknown,ProviderAuthStage.Cancelled)) FeatureButton("重新官方授权核对账号",state.provider in platform.providers) {
            store.startProvider(state.provider,ProviderLauncher { provider, challenge -> platform.authorizeProvider(provider,challenge) })
        }
        if(state.stage == ProviderAuthStage.AccountChoice) TaskSecondary("返回登录方式",onClick = flow::dismiss)
        else if(state.stage in setOf(ProviderAuthStage.Cancelled,ProviderAuthStage.Unavailable,ProviderAuthStage.Expired)) TaskPrimary(if(state.stage == ProviderAuthStage.Unavailable) "选择其他方式" else "返回登录",onClick = flow::dismiss)
        else TextButton({ flow.dismiss() },enabled = !state.busy || state.stage == ProviderAuthStage.Authorizing) { Text(if(state.stage == ProviderAuthStage.Authorizing) "取消本次授权" else "返回登录方式") }
    }
}

@Composable fun AccountConflictNotice() {
    TaskNotice("保留原绑定","邮箱、手机号或第三方身份已有绑定时，不覆盖原资料、不迁移聊天，也不透露其他账号信息。请登录或找回原账号，或选择其他标识。持有新号码本身不能接管其过去关联的账号。")
}

@Composable private fun FormTextField(value: String, onChange: (String) -> Unit, label: String) =
    MndOutlinedField(value,onChange,Modifier.fillMaxWidth(),label = { Text(label) },singleLine = true)

/** 登录与安全设置共用官方授权入口和本次隐私确认。 */
@Composable fun ProviderEntry(store: MobileStore, available: List<String>, busy: Boolean, scope: String? = null) {
    val platform = LocalClientPlatform.current
    var selected by rememberSessionUiState("provider.consent",scope) { "" }
    val choices = listOf("wechat" to "微信","qq" to "QQ") + if(platform.kind == ClientPlatformKind.IOS) listOf("apple" to "Apple") else emptyList()
    Text("官方授权",style = MaterialTheme.typography.labelMedium)
    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(MobileUi.contentGap)) {
        choices.forEach { (provider,label) -> OutlinedButton({
            if(provider !in platform.providers || provider !in available) store.providerAuth.unavailable(provider) else selected = provider
        },enabled = !busy,modifier = Modifier.weight(1f).heightIn(min = MobileUi.actionHeight)) { DesignIcon("provider-$provider",null); Spacer(Modifier.width(MobileUi.compactGap)); Text(label) } }
    }
    if(selected.isNotBlank()) TaskSection("本次官方授权") {
        Text("将打开官方授权页面，使用本次返回的身份完成登录。SDK会处理必要设备与网络信息；此步不会自动创建或合并账号。")
        val policy = when(selected) { "wechat" -> "https://weixin.qq.com/cgi-bin/readtemplate?lang=zh_CN&t=weixin_agreement&s=privacy"; "qq" -> "https://wiki.connect.qq.com/qq互联sdk隐私保护声明"; else -> "https://www.apple.com/legal/privacy/" }
        TextButton({ platform.openLink(policy) }) { Text("阅读官方隐私说明") }
        Button({ val provider = selected; selected = ""; store.startProvider(provider,ProviderLauncher { id, challenge -> platform.authorizeProvider(id,challenge) }) },enabled = !busy) { Text("同意并进行本次授权") }
        TextButton({ selected = "" }) { Text("取消") }
    }
}
