package com.ttkk0000.meowcircle.kmpapp.mobile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.kmpapp.R

@Composable internal fun AndroidAuthDialog(store: MobileStore,s: MobileState) {
    val f = store.features; val work by f.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mode by rememberSessionUiState("mode", "transient:auth") { "password" }
    var identifier by rememberSessionUiState("identifier", "transient:auth") { "" }
    var password by rememberSessionUiState("password", "transient:auth") { "" }
    var code by rememberSessionUiState("code", "transient:auth") { "" }
    var username by rememberSessionUiState("username", "transient:auth") { "" }
    var agreed by rememberSessionUiState("agreed", "transient:auth", generation = termsGeneration(context)) { false }
    val terms = context.getString(R.string.mnd_terms_url); val privacy = context.getString(R.string.mnd_privacy_url)
    val policyReady = listOf(terms,privacy).all { url -> runCatching { Uri.parse(url).let { it.scheme == "https" && !it.host.isNullOrBlank() } }.getOrDefault(false) }
    val busy = s.busy || work.busy
    MobileTaskDialog(onDismissRequest = { if(!busy) store.cancelLogin() }) {
                Text("M&D",fontFamily = BrandFont,style = MaterialTheme.typography.headlineSmall)
                Text(if(mode == "register") "加入 M&D" else if(mode == "recover") "找回账号密码" else "欢迎回到 M&D",style = MaterialTheme.typography.headlineSmall)
                Text("meow & doggie · 游客也能浏览公开内容",color = MaterialTheme.colorScheme.onSurfaceVariant)
                SegmentedTextBar(
                    options = listOf("password" to "密码登录","email" to "邮箱验证码","phone" to "手机验证码","register" to "注册","recover" to "找回密码"),
                    selected = mode,
                    onSelect = { value -> mode = value; code = ""; password = ""; agreed = false; store.editAuth(AuthForm(visible = true)) },
                    scrollable = true,
                    enabled = !busy,
                )
                OutlinedTextField(identifier,{ identifier = it },Modifier.fillMaxWidth(),label = { Text(when(mode) { "password" -> "账号名 / 邮箱 / 手机号"; "phone" -> "已绑定手机号"; else -> "邮箱" }) },singleLine = true,enabled = !busy)
                if(mode == "register") OutlinedTextField(username,{ username = it },Modifier.fillMaxWidth(),label = { Text("唯一账号名") },singleLine = true,enabled = !busy)
                if(mode in listOf("password","register","recover")) OutlinedTextField(password,{ password = it },Modifier.fillMaxWidth(),label = { Text(if(mode == "recover") "新密码（至少10字节）" else "密码") },visualTransformation = PasswordVisualTransformation(),singleLine = true,enabled = !busy)
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
                if(mode == "register") {
                    Row { TextButton({ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(terms))) }.onFailure { f.notify("暂无法打开协议") } },enabled = policyReady) { Text("用户协议") }; TextButton({ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(privacy))) }.onFailure { f.notify("暂无法打开隐私政策") } },enabled = policyReady) { Text("隐私政策") } }
                    CheckLine("我已阅读并同意用户协议与隐私政策",agreed,{ agreed = it },policyReady && !busy)
                    if(!policyReady) Text("正式协议地址尚未配置，当前环境暂不开放注册。已有账号仍可登录。")
                }
                s.auth.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }; s.capabilityError?.let { Text(it,color = MaterialTheme.colorScheme.error) }; work.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }; work.notice?.let { Text(it) }
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                val enabled = !busy && identifier.isNotBlank() && when(mode) {
                    "password" -> s.capabilities.password && password.isNotBlank()
                    "phone" -> s.capabilities.sms && code.isNotBlank()
                    "email" -> s.capabilities.email && code.isNotBlank()
                    "register" -> s.capabilities.registration && s.capabilities.email && policyReady && agreed && code.isNotBlank() && username.isNotBlank() && password.toByteArray().size in 10..128
                    else -> s.capabilities.email && code.isNotBlank() && password.toByteArray().size in 10..128
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
                if(s.capabilities.providers.any { it in listOf("wechat","qq") }) Text("微信／QQ 尚未配置本机官方 SDK 回跳，当前无法发起。")
                TextButton({ store.cancelLogin();f.open(FeatureRoute(FeaturePage.Rules)) },enabled = !busy) { Text("阅读服务说明与规则草案") }
                TextButton({ store.cancelLogin() },enabled = !busy) { Text("继续以游客身份浏览") }
    }
}

private fun termsGeneration(context: android.content.Context) =
    context.getString(R.string.mnd_terms_url) to context.getString(R.string.mnd_privacy_url)
