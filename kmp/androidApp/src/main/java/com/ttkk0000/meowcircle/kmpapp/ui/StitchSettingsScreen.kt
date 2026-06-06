package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

private enum class SettingsSubScreen {
    Main,
    AccountSecurity,
    AppearanceTheme,
    Notifications,
    PrivacyPolicy,
    UserNotice
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchSettingsScreen(
    sdk: MeowCircleSdk,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onThemeChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var currentSub by remember { mutableStateOf(SettingsSubScreen.Main) }
    val currentUser = remember { sdk.cachedUser() }

    // Account & Security States
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passErrorText by remember { mutableStateOf<String?>(null) }
    var passSuccessText by remember { mutableStateOf<String?>(null) }

    // Appearance & Theme States
    val activeThemeStr = remember { sdk.getTheme() }
    var selectedTheme by remember { mutableStateOf(activeThemeStr) }

    // Notifications States
    var dmNotify by remember { mutableStateOf(true) }
    var likeNotify by remember { mutableStateOf(true) }
    var followNotify by remember { mutableStateOf(true) }
    var systemNotify by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSub) {
                            SettingsSubScreen.Main -> "设置"
                            SettingsSubScreen.AccountSecurity -> "账号与安全"
                            SettingsSubScreen.AppearanceTheme -> "外观与主题"
                            SettingsSubScreen.Notifications -> "消息通知"
                            SettingsSubScreen.PrivacyPolicy -> "隐私政策"
                            SettingsSubScreen.UserNotice -> "用户协议"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPalette.OnSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSub == SettingsSubScreen.Main) {
                                onBack()
                            } else {
                                currentSub = SettingsSubScreen.Main
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = StitchPalette.OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchPalette.Surface
                )
            )
        },
        containerColor = StitchPalette.Canvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentSub) {
                SettingsSubScreen.Main -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = StitchShape.cardFeed,
                                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                                border = BorderStroke(1.dp, StitchPalette.BorderHairline)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SettingsMenuItem(
                                        icon = Icons.Default.Person,
                                        title = "个人资料",
                                        subtitle = currentUser?.nickname?.ifBlank { currentUser.username } ?: "未设置",
                                        onClick = onEditProfile
                                    )
                                    HorizontalDivider(color = StitchPalette.BorderHairline, thickness = 0.5.dp)
                                    SettingsMenuItem(
                                        icon = Icons.Default.Security,
                                        title = "账号与安全",
                                        subtitle = "密码与手机绑定",
                                        onClick = { currentSub = SettingsSubScreen.AccountSecurity }
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                shape = StitchShape.cardFeed,
                                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                                border = BorderStroke(1.dp, StitchPalette.BorderHairline)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SettingsMenuItem(
                                        icon = Icons.Default.Palette,
                                        title = "外观与主题",
                                        subtitle = when (sdk.getTheme().lowercase()) {
                                            "honey", "sugar" -> "蜜糖橘 (Honey)"
                                            "mint" -> "薄荷绿 (Mint)"
                                            "night" -> "暗夜紫 (Night)"
                                            "neutral" -> "极简灰 (Neutral)"
                                            else -> "蜜糖橘 (Honey)"
                                        },
                                        onClick = { currentSub = SettingsSubScreen.AppearanceTheme }
                                    )
                                    HorizontalDivider(color = StitchPalette.BorderHairline, thickness = 0.5.dp)
                                    SettingsMenuItem(
                                        icon = Icons.Default.Notifications,
                                        title = "消息通知",
                                        subtitle = "推送与通知管理",
                                        onClick = { currentSub = SettingsSubScreen.Notifications }
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                shape = StitchShape.cardFeed,
                                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                                border = BorderStroke(1.dp, StitchPalette.BorderHairline)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SettingsMenuItem(
                                        icon = Icons.Default.Description,
                                        title = "用户协议",
                                        onClick = { currentSub = SettingsSubScreen.UserNotice }
                                    )
                                    HorizontalDivider(color = StitchPalette.BorderHairline, thickness = 0.5.dp)
                                    SettingsMenuItem(
                                        icon = Icons.Default.PrivacyTip,
                                        title = "隐私政策",
                                        onClick = { currentSub = SettingsSubScreen.PrivacyPolicy }
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onLogout,
                                shape = StitchShape.pill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchPalette.Error.copy(alpha = 0.1f),
                                    contentColor = StitchPalette.Error
                                ),
                                border = BorderStroke(1.dp, StitchPalette.Error.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("退出登录", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                SettingsSubScreen.AccountSecurity -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "修改密码",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchPalette.OnSurface
                        )

                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text("原密码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StitchPalette.Brand,
                                unfocusedBorderColor = StitchPalette.BorderHairline
                            )
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("新密码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StitchPalette.Brand,
                                unfocusedBorderColor = StitchPalette.BorderHairline
                            )
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("确认新密码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StitchPalette.Brand,
                                unfocusedBorderColor = StitchPalette.BorderHairline
                            )
                        )

                        if (passErrorText != null) {
                            Text(passErrorText!!, color = StitchPalette.Error, fontSize = 12.sp)
                        }

                        if (passSuccessText != null) {
                            Text(passSuccessText!!, color = StitchPalette.Brand, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (oldPassword.isBlank() || newPassword.isBlank()) {
                                    passErrorText = "密码不能为空"
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    passErrorText = "两次输入的密码不一致"
                                    return@Button
                                }
                                passErrorText = null
                                passSuccessText = "密码修改成功 (离线模拟)"
                                oldPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            },
                            shape = StitchShape.pill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StitchPalette.Brand,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("保存修改", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                SettingsSubScreen.AppearanceTheme -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "选择应用主题",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchPalette.OnSurface
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeCardItem("蜜糖橘 (Honey)", "honey", selectedTheme == "honey") { selectedTheme = "honey" }
                            ThemeCardItem("薄荷绿 (Mint)", "mint", selectedTheme == "mint") { selectedTheme = "mint" }
                            ThemeCardItem("暗夜紫 (Night)", "night", selectedTheme == "night") { selectedTheme = "night" }
                            ThemeCardItem("极简灰 (Neutral)", "neutral", selectedTheme == "neutral") { selectedTheme = "neutral" }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onThemeChanged(selectedTheme)
                            },
                            shape = StitchShape.pill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StitchPalette.Brand,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("应用主题", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                SettingsSubScreen.Notifications -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = StitchShape.cardFeed,
                            colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                            border = BorderStroke(1.dp, StitchPalette.BorderHairline)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingsSwitchItem("新私信通知", "收到好友或买家发来的新私信", dmNotify) { dmNotify = it }
                                HorizontalDivider(color = StitchPalette.BorderHairline, thickness = 0.5.dp)
                                SettingsSwitchItem("赞与收藏通知", "动态或商品被他人点赞、收藏", likeNotify) { likeNotify = it }
                                HorizontalDivider(color = StitchPalette.BorderHairline, thickness = 0.5.dp)
                                SettingsSwitchItem("关注通知", "有新伙伴关注了我的账号", followNotify) { followNotify = it }
                                HorizontalDivider(color = StitchPalette.BorderHairline, thickness = 0.5.dp)
                                SettingsSwitchItem("系统公告通知", "社区维护、服务升级等官方动态", systemNotify) { systemNotify = it }
                            }
                        }
                    }
                }

                SettingsSubScreen.PrivacyPolicy -> {
                    Card(
                        shape = StitchShape.cardFeed,
                        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                "M&D 隐私权政策说明",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StitchPalette.OnSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                "1. 个人信息收集：我们仅在您注册账号、更新个人资料以及在市集发布/购买好物时收集必要的账户基本信息和沟通日志。\n\n" +
                                        "2. 数据使用范围：收集的数据将主要用于会话私信发送、市集订单的履约和交易保护，以确保“cat-first & doggie-friendly”生态下的网络安全。\n\n" +
                                        "3. 数据安全与加密：我们绝不会私自向外部第三方出售或共享您的个人隐私数据。所有登录凭证均使用加密传输并安全存储。\n\n" +
                                        "4. 账户注销说明：如果您希望注销并清除在 M&D 产生的所有痕迹，可以在设置或联系官方客服反馈，我们将于3个工作日内响应并彻底删除。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StitchPalette.OnSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                SettingsSubScreen.UserNotice -> {
                    Card(
                        shape = StitchShape.cardFeed,
                        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                "M&D 用户服务协议条款",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StitchPalette.OnSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                "1. 行为规范：M&D 是一个以猫咪为主要情感核心、同时对狗狗友好的温暖宠物社区。用户在发布内容或留言时，必须尊重生命、严禁出现任何虐待、虐杀、恶意遗弃宠物的言论或画面。\n\n" +
                                        "2. 市集规则：发布于市集的商品、喂养服务或领养广告，必须真实有效。如有虚假宣传、诱导私下无担保交易或诈骗行为，一经核实将永久封禁账号。\n\n" +
                                        "3. 知识产权：用户发布在社区中的宠物原创照片、文章所有权归原作者所有，但 M&D 拥有在本软件内免费展示、分发的权利。\n\n" +
                                        "4. 免责声明：市集商品或服务由用户自行提供，请在线下接触或领养时注意安全，M&D 不承担由线下接触产生的意外连带责任。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StitchPalette.OnSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StitchPalette.Brand,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = StitchPalette.OnSurface
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = StitchPalette.BorderHairline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = StitchPalette.OnSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = StitchPalette.OnSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StitchPalette.Brand,
                uncheckedThumbColor = StitchPalette.OnSurfaceVariant,
                uncheckedTrackColor = StitchPalette.BorderHairline
            )
        )
    }
}

@Composable
private fun ThemeCardItem(
    label: String,
    themeValue: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = StitchShape.container,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) StitchPalette.BrandMuted else StitchPalette.Surface
        ),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (selected) StitchPalette.Brand else StitchPalette.BorderHairline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (themeValue) {
                                "honey" -> Color(0xFFFF8A3D)
                                "mint" -> Color(0xFF2EC4A6)
                                "night" -> Color(0xFF8B5CF6)
                                "neutral" -> Color(0xFF4B5563)
                                else -> Color(0xFFFF8A3D)
                            }
                        )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchPalette.OnSurface
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = StitchPalette.Brand,
                    unselectedColor = StitchPalette.BorderHairline
                )
            )
        }
    }
}
