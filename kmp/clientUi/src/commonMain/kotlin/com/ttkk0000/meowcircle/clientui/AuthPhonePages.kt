package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.ttkk0000.meowcircle.client.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** A current-phone proof is not a grant to mutate the account's login methods. */
@Composable internal fun AuthCurrentPhoneContent(store: MobileStore, s: FeatureState, methods: IdentityMethods,
    changing: Boolean, onVerified: (Eligibility) -> Unit, onCancel: () -> Unit) {
    val f = store.features
    val prefix = if (changing) "security.change.current" else "security.verify.current"
    var phone by rememberSessionUiState("$prefix.phone") { "" }
    var code by rememberSessionUiState("$prefix.code") { "" }
    var sentPhone by rememberSessionUiState("$prefix.sent") { "" }
    var receiptKey by rememberSessionUiState("$prefix.receipt") { "" }
    val ready = !s.busy && !s.loading && s.readFailure == null && !s.unconfirmedResult
    val sentHere = receiptKey.isNotEmpty() && receiptKey == s.otpDelivery?.key && sentPhone == phone.trim()
    val expired = sentHere && s.otpDelivery?.expired == true
    if (changing) AuthFacts(listOf("当前号码" to methods.phone)) else MndScreenTitle("继续这次操作前验证")
    if (!changing) TaskNotice("用于本次功能", "验证不会公开你的手机号，也不会改变内容受众或开启通知。手机号、年龄与具体业务资格仍由服务分别核对。")
    MndOutlinedField(phone, { phone = it; code = ""; sentPhone = ""; receiptKey = ""; f.clearCommandFeedback() },
        label = { Text("当前完整手机号") }, enabled = ready, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
    MndOutlinedField(code, { code = it.filter { c -> c in '0'..'9' }.take(6); f.clearCommandFeedback() },
        label = { Text(if (changing) "当前号码验证码" else "验证码") }, enabled = ready && sentHere && !expired, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
    MndQuietButton(if (s.otpWait > 0) "${s.otpWait}秒后可重发" else "获取当前号码验证码",
        ready && methods.smsAvailable && validLoginPhone(phone) && s.otpWait == 0) {
        val address = phone.trim()
        code = ""; sentPhone = ""; receiptKey = ""
        f.sendOTP(phone = address, purpose = "verify_phone") {
            sentPhone = address; receiptKey = f.state.value.otpDelivery?.key.orEmpty()
        }
    }
    if (expired) TaskNotice("验证码已过期", "请重新获取当前号码验证码，填写最新验证码后再验证。")
    if (!methods.smsAvailable) TaskNotice("当前短信渠道尚未启用", "保留公共浏览；没有实际验证码不会继续验证或换号。")
    if (changing) {
        TaskNotice("只更新登录号码", "宠物、日记和聊天仍属于这个M&D账号。当前号验证不代替后续针对新号码的变更用途验证。")
        MndQuietButton("旧号码已不能使用", ready) { phone = ""; code = ""; receiptKey = ""; store.openAuthRecovery() }
    } else MndQuietButton("暂不验证", ready, onClick = onCancel)
    FeaturePrimary(if (changing) "验证并填写新号码" else "验证并回到任务", ready && sentHere && !expired && validAuthCode(code)) {
        val proof = code; code = ""
        val owner = store.state.value.user?.id
        val route = s.route
        f.runCommand {
            f.api.verifyCurrentPhone(proof)
            val actual = f.api.eligibility()
            currentCoroutineContext().ensureActive()
            if (owner == null || owner != store.state.value.user?.id || f.state.value.route != route) return@runCommand
            check(actual.contractVersion == 1 && actual.phoneVerified) { "当前手机号验证状态尚未确认，请重新读取资格" }
            phone = ""; sentPhone = ""; receiptKey = ""
            store.refreshEligibility(); onVerified(actual)
        }
    }
}
