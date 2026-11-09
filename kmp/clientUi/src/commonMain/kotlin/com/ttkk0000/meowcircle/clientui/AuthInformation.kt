package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class AuthInformationPage { Entry, Terms, Privacy, Providers }

/** Read-only product summaries. Reading these pages never accepts an agreement or starts SDKs. */
@Composable fun AuthInformationTask(page: AuthInformationPage, onBack: () -> Unit,
    onPage: (AuthInformationPage) -> Unit, onManage: () -> Unit, onExit: () -> Unit) {
    val platform = LocalClientPlatform.current
    var linkError by remember(page) { mutableStateOf(false) }
    fun openPolicy(url: String) { linkError = !platform.openLink(url) }
    val title = when (page) {
        AuthInformationPage.Entry -> "服务协议与隐私"
        AuthInformationPage.Terms -> "M&D 服务协议"
        AuthInformationPage.Privacy -> "M&D 隐私政策"
        AuthInformationPage.Providers -> "第三方登录说明"
    }
    MobileTaskDialog(onBack, title = title) {
        when (page) {
            AuthInformationPage.Entry -> {
                NavigationRow("服务协议", "账号、内容和社区规则") { onPage(AuthInformationPage.Terms) }
                NavigationRow("隐私与数据说明", "用途、授权和资料控制") { onPage(AuthInformationPage.Privacy) }
                NavigationRow("第三方登录说明", "微信、QQ与Apple授权范围") { onPage(AuthInformationPage.Providers) }
            }
            AuthInformationPage.Terms -> {
                MndScreenTitle("使用前先了解")
                AuthFacts(listOf("账号" to "本人使用并保护可用的登录方式", "内容与互动" to "遵守发布范围，尊重他人资料与权利", "退出与申诉" to "账号、内容和处理结果有对应入口"))
                TaskNotice("隐私与选择", "你可以在设置中管理登录方式、通知和账号数据。")
            }
            AuthInformationPage.Privacy -> {
                MndScreenTitle("管理你的资料与选择")
                AuthFacts(listOf("登录" to "用于验证身份和关联同一账号", "位置" to "附近互助单独说明用途并请求授权", "照片" to "由你选择后用于对应内容", "账号数据" to "可在设置申请查询或注销"))
                NavigationRow("第三方登录信息", "查看 SDK 与提供方范围") { onPage(AuthInformationPage.Providers) }
            }
            AuthInformationPage.Providers -> {
                AuthFacts(listOf("微信 / QQ" to "仅处理本次授权返回的账号标识和你允许的资料", "Apple" to "可在官方页面选择隐藏邮箱，昵称和头像可稍后补充", "不会顺带做的事" to "不读取通讯录，不默认公开手机号或位置"))
                NavigationRow("管理已关联方式", "登录后可在账号设置中调整", onManage)
                MndQuietButton("微信官方隐私说明") { openPolicy("https://weixin.qq.com/cgi-bin/readtemplate?lang=zh_CN&t=weixin_agreement&s=privacy") }
                MndQuietButton("QQ官方隐私说明") { openPolicy("https://wiki.connect.qq.com/qq互联sdk隐私保护声明") }
                MndQuietButton("Apple隐私说明") { openPolicy("https://www.apple.com/legal/privacy/") }
            }
        }
        val official = when (page) { AuthInformationPage.Terms -> platform.policies.terms; AuthInformationPage.Privacy -> platform.policies.privacy; else -> "" }
        if (page == AuthInformationPage.Terms || page == AuthInformationPage.Privacy) {
            Text("此页为功能与资料用途摘要，正式内容以当前政策为准。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MndQuietButton("阅读正式全文", official.isNotBlank()) { openPolicy(official) }
        }
        if (linkError) Text("暂时无法打开链接，请稍后重试。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        FeaturePrimary(if (page == AuthInformationPage.Entry || page == AuthInformationPage.Providers) "返回登录" else "返回协议入口") {
            if (page == AuthInformationPage.Entry || page == AuthInformationPage.Providers) onExit() else onPage(AuthInformationPage.Entry)
        }
    }
}

@Composable internal fun AuthFacts(facts: List<Pair<String, String>>) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            facts.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
