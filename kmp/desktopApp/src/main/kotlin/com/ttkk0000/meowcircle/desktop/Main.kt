package com.ttkk0000.meowcircle.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ttkk0000.meowcircle.ActionCounts
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.desktop.screens.*

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "M&D Desktop Client",
    ) {
        DesktopApp()
    }
}

@Composable
@Preview
fun DesktopApp() {
    val sdk = remember {
        MeowCircleSdk(defaultBaseUrl = "http://127.0.0.1:8080").apply {
            if (sessionStore().getApiUrl("").contains("localhost")) {
                sessionStore().setApiUrl("http://127.0.0.1:8080")
            }
        }
    }
    var themeMode by remember { mutableStateOf(themeModeFromSession(sdk.getTheme())) }
    val apiClient = remember { MndApiClient(sdk) }
    var currentUser by remember { mutableStateOf<User?>(sdk.cachedUser()) }

    LaunchedEffect(Unit) {
        currentUser = sdk.restoreSession()
    }

    MndDesktopTheme(themeMode) {
        MndDesktopShell(
            sdk = sdk,
            apiClient = apiClient,
            currentUser = currentUser,
            onLoginSuccess = { currentUser = it },
            onLogout = { currentUser = null },
            themeMode = themeMode,
            onThemeChange = {
                sdk.setTheme(it.sessionValue)
                themeMode = it
            }
        )
    }
}

private fun themeModeFromSession(value: String): MndDesktopThemeMode =
    when (value.trim().lowercase()) {
        "mint" -> MndDesktopThemeMode.Mint
        "night" -> MndDesktopThemeMode.Night
        "neutral", "system" -> MndDesktopThemeMode.Neutral
        else -> MndDesktopThemeMode.Honey
    }

private val MndDesktopThemeMode.sessionValue: String
    get() =
        when (this) {
            MndDesktopThemeMode.Honey -> "honey"
            MndDesktopThemeMode.Mint -> "mint"
            MndDesktopThemeMode.Night -> "night"
            MndDesktopThemeMode.Neutral -> "neutral"
        }

@Composable
private fun MndDesktopShell(
    sdk: MeowCircleSdk,
    apiClient: MndApiClient,
    currentUser: User?,
    onLoginSuccess: (User) -> Unit,
    onLogout: () -> Unit,
    themeMode: MndDesktopThemeMode,
    onThemeChange: (MndDesktopThemeMode) -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    var activeScreen by remember { mutableStateOf("feed") }
    var profileBg by remember { mutableStateOf(sdk.sessionStore().getProfileBackground().ifBlank { "picnic" }) }
    var actionCounts by remember(currentUser?.id) { mutableStateOf(ActionCounts()) }

    LaunchedEffect(currentUser?.id, activeScreen) {
        if (currentUser == null) {
            actionCounts = ActionCounts()
        } else {
            sdk.actionCounts().onSuccess { actionCounts = it }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.canvas,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DesktopRail(
                activeScreen = activeScreen,
                onScreenChange = { activeScreen = it },
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                profileBg = profileBg,
                onProfileBgChange = { profileBg = it },
                currentUser = currentUser,
                actionCounts = actionCounts,
                sdk = sdk
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                when (activeScreen) {
                    "feed" -> FeedScreen(sdk = sdk, apiClient = apiClient, onNavigateToCompose = { activeScreen = if (currentUser == null) "auth" else "publish" })
                    "market" -> MarketScreen(sdk = sdk, apiClient = apiClient, onNavigateToPublish = { activeScreen = if (currentUser == null) "auth" else "publish" })
                    "messages" -> {
                        val user = currentUser
                        if (user == null) {
                            AuthScreen(sdk = sdk, onAuthSuccess = { onLoginSuccess(it); activeScreen = "messages" })
                        } else {
                            MessagesScreen(sdk = sdk, apiClient = apiClient)
                        }
                    }
                    "orders" -> {
                        val user = currentUser
                        if (user == null) {
                            AuthScreen(sdk = sdk, onAuthSuccess = { onLoginSuccess(it); activeScreen = "orders" })
                        } else {
                            OrdersScreen(sdk = sdk, apiClient = apiClient)
                        }
                    }
                    "profile" -> {
                        val user = currentUser
                        if (user == null) {
                            AuthScreen(sdk = sdk, onAuthSuccess = { onLoginSuccess(it); activeScreen = "profile" })
                        } else {
                            ProfileScreen(sdk = sdk, apiClient = apiClient, profileBg = profileBg, onLogout = onLogout)
                        }
                    }
                    "safety" -> SafetyScreen(apiClient = apiClient)
                    "publish" -> {
                        val user = currentUser
                        if (user == null) {
                            AuthScreen(sdk = sdk, onAuthSuccess = { onLoginSuccess(it); activeScreen = "publish" })
                        } else {
                            PublishScreen(sdk = sdk, apiClient = apiClient, onPublishSuccess = { activeScreen = "feed" })
                        }
                    }
                    "auth" -> {
                        AuthScreen(sdk = sdk, onAuthSuccess = { onLoginSuccess(it); activeScreen = "feed" })
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopRail(
    activeScreen: String,
    onScreenChange: (String) -> Unit,
    themeMode: MndDesktopThemeMode,
    onThemeChange: (MndDesktopThemeMode) -> Unit,
    profileBg: String,
    onProfileBgChange: (String) -> Unit,
    currentUser: User?,
    actionCounts: ActionCounts,
    sdk: MeowCircleSdk,
) {
    val colors = LocalDesktopStitchColors.current
    val messageBadge =
        if (currentUser == null) 0 else actionCounts.unreadMessages + actionCounts.unreadNotifications
    val orderBadge = if (currentUser == null) 0 else actionCounts.orderActions
    Card(
        modifier = Modifier.width(248.dp).fillMaxHeight(),
        shape = MndDesktopShape.panel,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("M&D", color = colors.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("meow & doggie", color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text(
                text = "M&D Stitch V2 Board",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Divider(color = colors.outlineVariant)

            RailItem("Feed", "cat-first stories", selected = activeScreen == "feed") {
                onScreenChange("feed")
            }
            RailItem("Market", "services and goods", selected = activeScreen == "market") {
                onScreenChange("market")
            }
            RailItem("Messages", "orders and chat", selected = activeScreen == "messages", badge = messageBadge) {
                onScreenChange("messages")
            }
            RailItem("Orders", "purchased items", selected = activeScreen == "orders", badge = orderBadge) {
                onScreenChange("orders")
            }
            if (currentUser == null) {
                RailItem("Login / Register", "sign in to account", selected = activeScreen == "auth") {
                    onScreenChange("auth")
                }
            } else {
                RailItem("Profile", "personal settings", selected = activeScreen == "profile") {
                    onScreenChange("profile")
                }
            }
            RailItem("Review", "safety queue", selected = activeScreen == "safety") {
                onScreenChange("safety")
            }

            Spacer(Modifier.weight(1f))
            Box {
                var showSettings by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MndDesktopShape.panel)
                        .background(colors.surfaceLow)
                        .border(1.dp, colors.outlineVariant, MndDesktopShape.panel)
                        .clickable { showSettings = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandDot(12.dp, colors.brand)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Style Settings", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("global theme & profile cover", color = colors.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Text("⚙️", fontSize = 14.sp)
                }

                DropdownMenu(
                    expanded = showSettings,
                    onDismissRequest = { showSettings = false },
                    modifier = Modifier.width(220.dp).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "APP THEME",
                            color = colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        MndDesktopThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) colors.brandMuted else Color.Transparent)
                                    .clickable {
                                        onThemeChange(mode)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BrandDot(10.dp, colorsFor(mode).brand)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) colors.brand else colors.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = colors.outlineVariant)

                        Text(
                            text = "PROFILE COVER",
                            color = colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        listOf("picnic" to "Picnic 🍑", "desk" to "Desk 🖥️", "arcade" to "Arcade 🕹️", "garden" to "Garden 🏡").forEach { (bgId, bgName) ->
                            val isSelected = profileBg == bgId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) colors.brandMuted else Color.Transparent)
                                    .clickable {
                                        onProfileBgChange(bgId)
                                        sdk.sessionStore().setProfileBackground(bgId)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BrandDot(10.dp, when (bgId) {
                                    "picnic" -> Color(0xFFFFECE0)
                                    "desk" -> Color(0xFFE0F2FE)
                                    "arcade" -> Color(0xFFF3E8FF)
                                    "garden" -> Color(0xFFDCFCE7)
                                    else -> colors.outline
                                })
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = bgName,
                                    color = if (isSelected) colors.brand else colors.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RailItem(
    title: String,
    subtitle: String,
    selected: Boolean = false,
    badge: Int = 0,
    onClick: () -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    val background = if (selected) colors.brandMuted else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MndDesktopShape.panel)
            .background(background)
            .border(1.dp, if (selected) colors.brand else colors.outlineVariant, MndDesktopShape.panel)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandDot(12.dp, if (selected) colors.brand else colors.outline)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.onSurfaceVariant, fontSize = 11.sp)
        }
        if (badge > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .defaultMinSize(minWidth = 22.dp)
                    .clip(MndDesktopShape.pill)
                    .background(colors.error)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badge > 99) "99+" else badge.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun ThemeChoice(
    mode: MndDesktopThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    val modeColors = colorsFor(mode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MndDesktopShape.panel)
            .clickable(onClick = onClick)
            .background(if (selected) colors.surfaceLow else Color.Transparent)
            .border(1.dp, if (selected) colors.brand else colors.outlineVariant, MndDesktopShape.panel)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandDot(14.dp, modeColors.brand)
        Spacer(Modifier.width(10.dp))
        Text(mode.label, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BrandMark(size: Dp) {
    val colors = LocalDesktopStitchColors.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.brand),
        contentAlignment = Alignment.Center,
    ) {
        Text("M&D", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BrandDot(size: Dp, color: Color) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}
