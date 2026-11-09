package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

fun securityTitle(view: SecurityView): String = when (view) {
    SecurityView.Accounts -> "账号与登录方式"
    SecurityView.Phone -> "交流前验证手机号"
    SecurityView.PhoneCode -> "验证当前手机号"
    SecurityView.PhoneSaved -> "当前手机号已验证"
    SecurityView.PhoneChange -> "更换手机号"
    SecurityView.EmailChange -> "绑定／更换邮箱"
    SecurityView.PasswordChange -> "设置新密码"
    SecurityView.Methods -> "已关联的登录方式"
    SecurityView.Eligibility -> "年龄与交流资格"
}

@Composable fun SecurityContent(store: MobileStore, s: FeatureState) {
    val f = store.features
    val root by store.state.collectAsState()
    val methods = s.methods ?: return
    val ready = !s.busy && !s.loading && s.error == null && s.readFailure == null && !s.unconfirmedResult
    var logoutMode by rememberSessionUiState("security.logout") { "" }
    when (s.securityView) {
        SecurityView.Phone, SecurityView.PhoneCode -> CurrentPhoneContent(store, s, methods, ready)
        SecurityView.PhoneSaved -> {
            MndScreenTitle("验证已由服务确认")
            TaskNotice("返回原任务继续核对", "当前手机号验证不代替年龄、监护、城市准入或业务授权；回到原任务后仍需主动确认，不自动发布或发送。", success = true)
            FeaturePrimary("返回原任务", !s.busy) { f.setSecurityView(SecurityView.Accounts); f.back() }
        }
        SecurityView.PhoneChange, SecurityView.EmailChange, SecurityView.PasswordChange -> SecurityChangeContent(store, s, methods, ready)
        SecurityView.Methods -> {
            NavigationRow("关联手机号", methods.phone.ifBlank { "核对本次新号码与原账号" }) { if (ready) f.setSecurityView(SecurityView.PhoneChange) }
            NavigationRow("关联邮箱", "核对本次邮箱与原账号") { if (ready) f.setSecurityView(SecurityView.EmailChange) }
            methods.linked.forEach { identity ->
                NavigationRow(providerLabel(identity.provider), "已关联 · 解绑前核对当前账号与影响") {
                    if (ready) f.prepareSensitive(SensitiveIntent.Account("unlink", AccountInput(s.profile?.version ?: 0, newRequestKey(), identityId = identity.id)))
                }
            }
            if (methods.linked.isEmpty()) TaskNotice("尚无已关联的快捷方式", "可以选择当前可用的官方授权；相同昵称或头像不会自动合并账号。")
            ProviderEntry(store, methods.availableProviders, !ready)
            TaskNotice("保护最后一种登录方式", "关联与解绑均需主动确认和本次用途验证，不能移除最后一种可用登录方式。")
            FeaturePrimary("返回账号设置", !s.busy) { f.setSecurityView(SecurityView.Accounts) }
        }
        SecurityView.Eligibility -> {
            val eligible = s.eligibility
            TaskNotice("登录与业务资格分别检查", "年龄信息来自服务核实；这里不会将自行勾选成年当成验证。手机号、监护授权与具体事项分别核对。")
            Text("当前手机号：${if (eligible?.contractVersion == 1 && eligible.phoneVerified) "服务已确认验证" else "尚未确认有效"}")
            Text("交流资格：${if (eligible?.contractVersion == 1 && eligible.interaction) "当前可用" else "尚未满足条件"}")
            eligible?.reasons?.forEach { Text(MobileApiException(403, "eligibility required", it).mobileMessage()) }
            NavigationRow("核对当前手机号", "保留原任务，不自动提交") { if (ready) f.setSecurityView(SecurityView.Phone) }
            NavigationRow("年龄与使用范围", "年龄、监护与成年业务") { if (ready) f.open(FeatureRoute(FeaturePage.Age)) }
            NavigationRow("监护授权与管理", "邀请、独立核对与撤回") { if (ready) f.open(FeatureRoute(FeaturePage.Guardians)) }
            NavigationRow("未成年人保护设置", "陌生人联系、屏蔽与资料求助") { if (ready) f.open(FeatureRoute(FeaturePage.Guardians, kind = "protection")) }
            FeaturePrimary("返回账号设置", !s.busy) { f.setSecurityView(SecurityView.Accounts) }
        }
        SecurityView.Accounts -> {
            val user = s.profile?.user ?: store.state.value.user
            if (user != null) Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    MobilePhoto(store, avatarMediaId(user.avatarUrl.orEmpty()), user.id, "当前账号头像", Modifier.size(56.dp).clip(MaterialTheme.shapes.medium))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(user.nickname.ifBlank { user.username }, style = MaterialTheme.typography.titleMedium)
                        Text("同一个 M&D 账号", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            NavigationRow("手机号", methods.phone.ifBlank { "尚未绑定" }) { if (ready) f.setSecurityView(SecurityView.PhoneChange) }
            NavigationRow("邮箱", "绑定或更换已验证邮箱") { if (ready) f.setSecurityView(SecurityView.EmailChange) }
            val platforms = listOf("wechat", "qq") + if (LocalClientPlatform.current.kind == ClientPlatformKind.IOS || methods.linked.any { it.provider == "apple" }) listOf("apple") else emptyList()
            platforms.forEach { provider -> NavigationRow(providerLabel(provider), if (methods.linked.any { it.provider == provider }) "已关联" else "未关联") { if (ready) f.setSecurityView(SecurityView.Methods) } }
            NavigationRow("密码与恢复", if (methods.passwordSet) "更新密码或恢复登录" else "设置密码与保留恢复方式") { if (ready) f.setSecurityView(SecurityView.PasswordChange) }
            NavigationRow("当前手机号与交流资格", "登录、年龄与交流分别核对") { if (ready) f.setSecurityView(SecurityView.Eligibility) }
            NavigationRow("资料删除、注销与协助", "先查看影响，再核对用途") { if (ready) f.open(FeatureRoute(FeaturePage.DataRequests)) }
            NavigationRow("退出当前账号", "保留公共浏览") { if (ready) logoutMode = "current" }
            MndQuietButton("退出全部会话", ready) { logoutMode = "all" }
            FeaturePrimary("返回设置", !s.busy) { f.returnTo(FeatureRoute(FeaturePage.SettingsHome)) }
        }
    }
    if (logoutMode.isNotEmpty()) {
        val logoutBusy = root.busy || s.busy
        MobileTaskDialog(onDismissRequest = { if (!logoutBusy) logoutMode = "" },
            title = if (logoutMode == "all") "退出全部会话" else "退出当前账号", navigationEnabled = !logoutBusy) {
            TaskNotice("退出后", "可继续浏览公开内容。当前账号的私密内容将从本机隐藏，已保存的草稿按账号保留。未保存的输入请先处理后退出。")
            if (logoutMode == "all") TaskNotice("全部设备", "此账号在其他设备的会话也会失效，需要重新登录。")
            MndSecondaryButton("继续使用", !logoutBusy) { logoutMode = "" }
            root.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            FeaturePrimary(if (logoutBusy) "正在退出" else "确认退出", ready && !logoutBusy) {
                if (logoutMode == "all") store.signOutAllDevices() else store.logout()
            }
        }
    }
}

private fun providerLabel(provider: String) = when (provider) { "wechat" -> "微信"; "qq" -> "QQ"; "apple" -> "Apple"; "email" -> "邮箱"; else -> "已关联的方式" }

@Composable private fun CurrentPhoneContent(store: MobileStore, s: FeatureState, methods: IdentityMethods, ready: Boolean) {
    val f = store.features
    if (ready && s.eligibility?.contractVersion == 1 && s.eligibility.phoneVerified) {
        MndScreenTitle("当前手机号已验证")
        TaskNotice("无需重复收取短信", "服务已确认当前号码验证。业务权限、年龄与监护仍需分别核对；返回后不会自动发布。")
        FeaturePrimary("返回原任务") { f.setSecurityView(SecurityView.Accounts); f.back() }
        return
    }
    AuthCurrentPhoneContent(store, s, methods, changing = false, onVerified = f::currentPhoneVerified, onCancel = { f.back() })
}

@Composable private fun SecurityChangeContent(store: MobileStore, s: FeatureState, methods: IdentityMethods, ready: Boolean) {
    val f = store.features
    val action = when (s.securityView) { SecurityView.PhoneChange -> "phone"; SecurityView.EmailChange -> "email"; else -> "password" }
    var value by rememberSessionUiState("security.$action.value") { "" }
    var code by rememberSessionUiState("security.$action.code") { "" }
    var repeated by rememberSessionUiState("security.$action.repeated") { "" }
    var currentPhoneRead by rememberSessionUiState("security.phone.currentRead", generation = s.profile?.version) { false }
    var sentValue by rememberSessionUiState("security.$action.sentValue") { "" }
    var receiptKey by rememberSessionUiState("security.$action.receipt") { "" }
    if (action == "phone" && methods.phone.isNotBlank() && !currentPhoneRead) {
        AuthCurrentPhoneContent(store, s, methods, changing = true,
            onVerified = { currentPhoneRead = true }, onCancel = { f.setSecurityView(SecurityView.Accounts) })
        return
    }
    val usableReceipt = sentValue == value.trim() && receiptKey.isNotEmpty() && receiptKey == s.otpDelivery?.key && s.otpDelivery?.expired == false
    val editable = !s.busy && !s.loading && s.readFailure == null && !s.unconfirmedResult
    if (action == "phone") MndScreenTitle("验证新手机号")
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SecurityFact("原账号", s.profile?.user?.username ?: "当前 M&D 账号")
            if (action == "phone") SecurityFact("原号码", methods.phone.ifBlank { "尚未绑定" })
            Text("保持同一账号，不创建新账号。", style = MaterialTheme.typography.bodySmall)
        }
    }
    TaskNotice("当前账号安全验证", "核对目标后仍需验证本次账号用途；普通登录态或新联系方式不能直接授权接管。")
    MndOutlinedField(value, { value = it; code = ""; sentValue = ""; receiptKey = ""; f.clearCommandFeedback() }, label = { Text(when (action) { "phone" -> "新的手机号"; "email" -> "新邮箱"; else -> "新密码" }) },
        enabled = editable, singleLine = true, visualTransformation = if (action == "password") PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = when (action) { "phone" -> KeyboardType.Phone; "email" -> KeyboardType.Email; else -> KeyboardType.Password }))
    if (action == "password") {
        MndOutlinedField(repeated, { repeated = it }, label = { Text("再次确认") }, enabled = ready, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), isError = repeated.isNotEmpty() && repeated != value)
    } else {
        OtpVerificationFields(code, { code = it.filter { c -> c in '0'..'9' }.take(6); f.clearCommandFeedback() }, s.otpWait, sendLabel = "发送新联系方式验证码", codeLabel = "验证码",
            enabled = editable, canSend = if (action == "phone") validLoginPhone(value) && methods.smsAvailable else value.isNotBlank() && store.state.value.capabilities.email) {
            val address = value.trim()
            code = ""; sentValue = ""; receiptKey = ""
            f.sendOTP(email = if (action == "email") address else "", phone = if (action == "phone") address else "", purpose = if (action == "email") "email_bind" else "bind_phone") {
                sentValue = address; receiptKey = f.state.value.otpDelivery?.key.orEmpty()
            }
        }
        if (receiptKey.isNotEmpty() && s.otpDelivery?.expired == true) TaskNotice("验证码已过期", "请重新获取新联系方式的验证码。")
    }
    if (action == "phone") TaskNotice("确认号码可用", if (methods.phone.isNotBlank()) "验证新号码后，旧号码不再用于当前账号登录。接下来仍需核对本次变更用途并主动确认。" else "验证新号码后关联到同一账号；接下来仍需核对本次变更用途并主动确认。")
    NavigationRow("原号码已无法使用", "独立恢复，不跳过核对") { if (editable) { value = ""; code = ""; repeated = ""; receiptKey = ""; store.openAuthRecovery() } }
    TaskNotice("成功后重新核对资格", "只有验证与确认后的真实回执才表示更新；失败保持原绑定与资料。旧会话将失效，请重新登录。")
    FeaturePrimary("核对本次安全变更", editable && value.isNotBlank() && if (action == "password") value.encodeToByteArray().size in 10..128 && value == repeated else usableReceipt && validAuthCode(code)) {
        val input = AccountInput(s.profile?.version ?: 0, newRequestKey(), phone = if (action == "phone") value.trim() else "",
            email = if (action == "email") value.trim() else "", password = if (action == "password") value else "", code = if (action == "password") "" else code)
        f.prepareSensitive(SensitiveIntent.Account(action, input)); currentPhoneRead = false; if (action == "password") value = ""; code = ""; repeated = ""
    }
}

@Composable private fun SecurityFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
