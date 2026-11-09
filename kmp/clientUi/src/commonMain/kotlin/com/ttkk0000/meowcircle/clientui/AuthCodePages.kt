package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.validAuthCode
import com.ttkk0000.meowcircle.client.validLoginPhone

/** One editable field for paste/keyboard/accessibility; six decorative cells never submit. */
@Composable internal fun AuthOtpField(value: String, onChange: (String) -> Unit, enabled: Boolean, label: String = "短信验证码，6位数字") {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(enabled) { if (enabled) focus.requestFocus() }
    val colors = MaterialTheme.colorScheme
    BasicTextField(value, { onChange(it.filter { char -> char in '0'..'9' }.take(6)) },
        modifier = Modifier.fillMaxWidth().height(56.dp).focusRequester(focus).semantics { contentDescription = label }, enabled = enabled,
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent), cursorBrush = SolidColor(Color.Transparent), interactionSource = interactions,
        decorationBox = { inner -> Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { index ->
                    val selected = enabled && focused && index == value.length.coerceAtMost(5)
                    Box(Modifier.weight(1f).fillMaxHeight().background(colors.surface, RoundedCornerShape(8.dp))
                        .border(if (selected) 2.dp else 1.dp, if (selected) ClientFocusColor else colors.outline, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center) {
                        Text(value.getOrNull(index)?.toString() ?: "·", style = MaterialTheme.typography.titleMedium,
                            color = if (enabled) colors.onSurface else colors.onSurfaceVariant)
                    }
                }
            }
            Box(Modifier.matchParentSize()) { inner() }
        } })
}

@Composable internal fun AuthPhoneErrorTask(value: String, onChange: (String) -> Unit, busy: Boolean, agreed: Boolean, onAgree: (Boolean) -> Unit,
    canSend: Boolean, wait: Int, onSend: () -> Unit, onOther: () -> Unit, onBack: () -> Unit) {
    val platform = LocalClientPlatform.current
    val valid = validLoginPhone(value)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    MobileTaskDialog(onBack, title = "请检查手机号", navigationEnabled = !busy) {
        MndScreenTitle(if (value.count { it in '0'..'9' } < 10) "手机号还没有填完整" else "请核对手机号格式")
        MndOutlinedField(value, onChange, modifier = Modifier.fillMaxWidth().focusRequester(focus), label = { Text("已绑定手机号") }, placeholder = { Text("请输入手机号") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), enabled = !busy, isError = !valid,
            supportingText = if (!valid) ({ Text("请填写可接收短信的完整手机号。") }) else null)
        if (platform.policies.ready) {
            CheckLine("我已阅读并同意服务协议与隐私政策", agreed, onAgree, !busy)
            Row {
                TextButton({ platform.openLink(platform.policies.terms) }, enabled = !busy) { Text("服务协议") }
                TextButton({ platform.openLink(platform.policies.privacy) }, enabled = !busy) { Text("隐私政策") }
            }
        }
        MndPrimaryButton(if (wait > 0) "${wait}秒后可重发" else "重新获取验证码", !busy && canSend && valid && wait == 0 && (!platform.policies.ready || agreed), onClick = onSend)
        MndQuietButton("换个登录方式", !busy, onClick = onOther)
    }
}

@Composable internal fun AuthCodeResentTask(address: String, value: String, onChange: (String) -> Unit, busy: Boolean,
    canSubmit: Boolean, onSubmit: () -> Unit, onOther: () -> Unit, onBack: () -> Unit) {
    MobileTaskDialog(onBack, title = "验证码已重发", navigationEnabled = !busy) {
        MndScreenTitle("请留意新的短信")
        Text(address, style = MaterialTheme.typography.titleMedium)
        AuthOtpField(value, onChange, !busy)
        TaskNotice("使用最新验证码", "旧验证码不再使用；不要向他人提供验证码。")
        MndQuietButton("更换登录方式", !busy, onClick = onOther)
        FeaturePrimary("验证并继续", !busy && canSubmit && validAuthCode(value), onSubmit)
    }
}

@Composable internal fun AuthCodeExpiredTask(busy: Boolean, wait: Int, canSend: Boolean, onSend: () -> Unit, onReturn: () -> Unit, onBack: () -> Unit) {
    MobileTaskDialog(onBack, title = "验证码已过期", navigationEnabled = !busy) {
        MndScreenTitle("验证码已过期")
        TaskNotice("可以继续", "重新获取后填写最新验证码，无需重新开始刚才的操作。")
        MndQuietButton("返回刚才的页面", !busy, onClick = onReturn)
        FeaturePrimary(if (wait > 0) "${wait}秒后可重发" else "重新获取", !busy && canSend && wait == 0, onSend)
    }
}

@Composable internal fun AuthSmsDelayedTask(busy: Boolean, onEdit: () -> Unit, onReturn: () -> Unit, onRecovery: () -> Unit, onBack: () -> Unit) {
    MobileTaskDialog(onBack, title = "暂时没有收到短信", navigationEnabled = !busy) {
        MndScreenTitle("暂时没有收到短信")
        TaskNotice("可以继续", "检查号码和短信收件箱，也可以返回登录选择其他已关联的方式。")
        MndQuietButton("返回刚才的页面", !busy, onClick = onReturn)
        MndQuietButton("账号恢复帮助", !busy, onClick = onRecovery)
        FeaturePrimary("返回填写", !busy, onEdit)
    }
}
