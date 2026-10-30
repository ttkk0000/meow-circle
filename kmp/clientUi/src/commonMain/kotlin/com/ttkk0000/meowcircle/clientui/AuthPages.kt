package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ttkk0000.meowcircle.client.*

@Composable fun ClientAuthDialog(store: MobileStore,s: MobileState) {
    val f = store.features; val work by f.state.collectAsState()
    val platform = LocalClientPlatform.current
    val providerState by store.providerAuth.state.collectAsState()
    if(providerState.stage != ProviderAuthStage.Idle) { ProviderAuthTask(store,providerState); return }
    var mode by rememberSessionUiState("mode", "transient:auth") { "password" }
    var identifier by rememberSessionUiState("identifier", "transient:auth") { "" }
    var password by rememberSessionUiState("password", "transient:auth") { "" }
    var code by rememberSessionUiState("code", "transient:auth") { "" }
    var username by rememberSessionUiState("username", "transient:auth") { "" }
    var agreed by rememberSessionUiState("agreed", "transient:auth", generation = platform.policies) { false }
    val terms = platform.policies.terms; val privacy = platform.policies.privacy
    val policyReady = platform.policies.ready
    val busy = s.busy || work.busy
    MobileTaskDialog(onDismissRequest = { if(!busy) store.cancelLogin() }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("M&D",fontFamily = BrandFont,style = MaterialTheme.typography.headlineLarge,fontWeight = FontWeight.ExtraBold)
                    TextButton({ store.cancelLogin() },enabled = !busy) { Text("返回") }
                }
                Text(when(mode) { "register" -> "加入 M&D"; "recover" -> "找回账号密码"; else -> "和毛孩子一起长大" },style = MaterialTheme.typography.headlineSmall)
                Text("meow & doggie",color = MaterialTheme.colorScheme.onSurfaceVariant)
                fun changeMode(value: String) { mode = value; code = ""; password = ""; agreed = false; store.editAuth(AuthForm(visible = true)) }
                if(mode != "password") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(when(mode) { "email" -> "邮箱验证码登录"; "phone" -> "已绑定手机登录"; "register" -> "邮箱注册"; else -> "邮箱恢复" },style = MaterialTheme.typography.titleMedium)
                    TextButton({ changeMode("password") }, enabled = !busy) { Text("密码登录") }
                }
                MndOutlinedField(identifier,{ identifier = it },Modifier.fillMaxWidth(),label = { Text(when(mode) { "password" -> "账号名 / 邮箱 / 手机号"; "phone" -> "已绑定手机号"; else -> "邮箱" }) },singleLine = true,enabled = !busy)
                if(mode == "register") MndOutlinedField(username,{ username = it },Modifier.fillMaxWidth(),label = { Text("唯一账号名") },singleLine = true,enabled = !busy)
                if(mode in listOf("password","register","recover")) MndOutlinedField(password,{ password = it },Modifier.fillMaxWidth(),label = { Text(if(mode == "recover") "新密码（至少10字节）" else "密码") },visualTransformation = PasswordVisualTransformation(),singleLine = true,enabled = !busy)
                if(mode != "password") {
                    val channel = if(mode == "phone") s.capabilities.sms else s.capabilities.email
                    OtpVerificationFields(
                        code = code,
                        onCodeChange = { code = it },
                        otpWait = work.otpWait,
                        sendLabel = "发送验证码",
                        enabled = !busy,
                        canSend = channel && identifier.isNotBlank() && (mode != "register" || policyReady && agreed),
                    ) {
                        f.sendOTP(email = if(mode != "phone") identifier else "",phone = if(mode == "phone") identifier else "",purpose = when(mode) { "register" -> "email_register"; "recover" -> "email_recover"; "phone" -> "login"; else -> "email_login" })
                    }
                    if(!channel) Text("当前环境尚未接通此验证码渠道，请使用已有账号密码登录。")
                }
                if(mode in listOf("password","register")) {
                    Row { TextButton({ if(!platform.openLink(terms)) f.notify("暂无法打开协议") },enabled = policyReady) { Text("用户协议") }; TextButton({ if(!platform.openLink(privacy)) f.notify("暂无法打开隐私政策") },enabled = policyReady) { Text("隐私政策") } }
                    CheckLine("我已阅读并同意用户协议与隐私政策",agreed,{ agreed = it },policyReady && !busy)
                    if(!policyReady) Text("正式协议地址尚未配置，当前环境暂不开放注册。已有账号仍可登录。")
                }
                s.auth.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }; s.capabilityError?.let { Text(it,color = MaterialTheme.colorScheme.error) }; work.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }; work.notice?.let { Text(it) }
                if(s.auth.errorCode == "identity_in_use") {
                    AccountConflictNotice()
                    NavigationRow("登录或找回原账号","先完成原账号验证") { changeMode("recover") }
                    NavigationRow("换一个邮箱","原任务仍保留") { identifier = ""; changeMode("register") }
                }
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                val enabled = !busy && identifier.isNotBlank() && when(mode) {
                    "password" -> s.capabilities.password && password.isNotBlank()
                    "phone" -> s.capabilities.sms && code.isNotBlank()
                    "email" -> s.capabilities.email && code.isNotBlank()
                    "register" -> s.capabilities.registration && s.capabilities.email && policyReady && agreed && code.isNotBlank() && username.isNotBlank() && password.encodeToByteArray().size in 10..128
                    else -> s.capabilities.email && code.isNotBlank() && password.encodeToByteArray().size in 10..128
                }
                FeaturePrimary(when(mode) { "register" -> "确认注册"; "recover" -> "确认重设密码"; else -> "登录并继续" },enabled) {
                    when(mode) {
                        "password" -> { store.editAuth(AuthForm(true,identifier,password)); store.login(); password = "" }
                        "phone" -> { store.loginPhone(identifier,code); code = "" }
                        "email" -> { store.loginEmail(EmailAuthInput(identifier,code)); code = "" }
                        "register" -> { store.loginEmail(EmailAuthInput(identifier,code,username,password,acceptTerms = agreed),true); code = ""; password = "" }
                        else -> { val input = EmailAuthInput(identifier,code,password = password); code = ""; password = ""; f.runCommand { f.api.recoverEmail(input); f.notify("密码已更新，请返回密码登录") } }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ changeMode(if(mode == "email") "phone" else "email") },enabled = !busy) { Text(if(mode == "email") "手机验证码登录" else "邮箱验证码登录") }
                    TextButton({ changeMode("recover") },enabled = !busy) { Text("找回密码") }
                }
                TextButton({ changeMode(if(mode == "register") "password" else "register") }, enabled = !busy) { Text(if(mode == "register") "已有账号，返回登录" else "还没有账号？邮箱注册") }
                HorizontalDivider()
                ProviderEntry(store,s.capabilities.providers,busy,scope = "transient:auth")
                TextButton({ store.cancelLogin();f.open(FeatureRoute(FeaturePage.Rules)) },enabled = !busy) { Text("阅读服务说明与规则草案") }
                TextButton({ store.cancelLogin() },enabled = !busy) { Text("继续以游客身份浏览") }
    }
}

