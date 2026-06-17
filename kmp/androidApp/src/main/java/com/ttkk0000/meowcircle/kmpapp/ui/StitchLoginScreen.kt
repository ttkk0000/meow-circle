package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchLoginRef
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import kotlinx.coroutines.launch

/**
 * M&D mobile login: cat-first lockup, icon fields, CTA, social row, register footer.
 */
@Composable
fun StitchLoginScreen(
    sdk: MeowCircleSdk,
    healthHint: String?,
    onLoggedIn: (User) -> Unit,
    onNavigateRegister: () -> Unit = {},
    onThemeChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("demo") }
    var password by remember { mutableStateOf("123456") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var errDetail by remember { mutableStateOf<String?>(null) }
    var showPw by remember { mutableStateOf(false) }
    var hintDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showThemePicker by remember { mutableStateOf(false) }
    var currentTheme by remember { mutableStateOf(sdk.getTheme()) }
    val scroll = rememberScrollState()

    var apiBase by remember { mutableStateOf(sdk.baseUrl) }
    var showApiConfig by remember { mutableStateOf(false) }
    var localHealthHint by remember(healthHint) { mutableStateOf(healthHint) }
    val backendConnectingText = stringResource(R.string.login_backend_connecting)
    val backendServicePrefix = stringResource(R.string.login_backend_service_prefix)
    val backendStatusTitle = stringResource(R.string.login_backend_status_title)
    val forgotPasswordTitle = stringResource(R.string.login_forgot_password)
    val forgotPasswordBody = stringResource(R.string.login_forgot_password_body)
    val loginFailedTitle = stringResource(R.string.login_failed_title)
    val socialTitle = stringResource(R.string.login_social_title)
    val socialBody = stringResource(R.string.login_social_body)

    // Two-state login flow state
    var showEmailLogin by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(StitchLoginRef.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 28.dp),
        ) {
            if (!showEmailLogin) {
                // State 1: Welcome Screen (Stitch V3 Mockup Screen 1)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Logo Box (bg #FFF1E6, 16dp rounded, pets icon, "M&D")
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(StitchLoginRef.SurfaceContainerLow)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pets,
                                contentDescription = null,
                                tint = StitchLoginRef.PrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "M&D",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchLoginRef.PrimaryContainer,
                                letterSpacing = 0.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.login_welcome_title), // "Welcome to M&D"
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        ),
                        color = StitchLoginRef.OnSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        stringResource(R.string.login_welcome_subtitle), // "A warm community for pets, people, and trusted local finds."
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = StitchLoginRef.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Mockup welcome cat image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFCFCFCF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("img", style = MaterialTheme.typography.labelSmall, color = Color(0xFF333333))
                    }

                    Spacer(Modifier.height(22.dp))

                    // Continue with Email Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StitchLoginRef.PrimaryContainer)
                            .clickable { showEmailLogin = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.login_continue_email), // "Continue with Email"
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            ),
                            color = StitchLoginRef.OnPrimaryButton,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Social login row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SocialCircle(label = "WeChat", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Icon(Icons.Filled.ChatBubble, null, tint = StitchLoginRef.WeChat, modifier = Modifier.size(20.dp))
                        }
                        SocialCircle(label = "QQ", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Icon(Icons.Filled.Pets, null, tint = StitchLoginRef.QqBlue, modifier = Modifier.size(20.dp))
                        }
                        SocialCircle(label = "Google", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = StitchLoginRef.PrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                        SocialCircle(label = "Apple", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Text("A", color = StitchLoginRef.OnSurface, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Terms footer
                    Text(
                        stringResource(R.string.login_terms_footer), // "By continuing, you agree to Terms and Privacy."
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchLoginRef.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                // State 2: Email Login Screen
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top navigation bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmailLogin = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = StitchLoginRef.OnSurface
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "M&D",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchLoginRef.PrimaryContainer,
                            letterSpacing = 0.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.size(48.dp)) // Spacer for balance
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.login_back_title), // "Welcome Back" / "欢迎回来"
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = StitchLoginRef.OnSurface,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.login_subtitle), // "欢迎回到你的猫猫宇宙"
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchLoginRef.OnSurfaceVariant,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(Modifier.height(32.dp))

                    // Input Form
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            placeholder = {
                                Text(
                                    stringResource(R.string.login_account_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = StitchLoginRef.Outline.copy(alpha = 0.6f),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = StitchLoginRef.Outline,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                                unfocusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                                focusedTextColor = StitchLoginRef.OnSurface,
                                unfocusedTextColor = StitchLoginRef.OnSurface,
                                focusedBorderColor = StitchLoginRef.PrimaryContainer,
                                unfocusedBorderColor = StitchPalette.BorderHairline,
                                cursorColor = StitchLoginRef.PrimaryContainer,
                                focusedLeadingIconColor = StitchLoginRef.PrimaryContainer,
                                unfocusedLeadingIconColor = StitchLoginRef.Outline,
                            ),
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            placeholder = {
                                Text(
                                    stringResource(R.string.login_password_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = StitchLoginRef.Outline.copy(alpha = 0.6f),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = StitchLoginRef.Outline,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPw = !showPw }) {
                                    Icon(
                                        imageVector = if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (showPw) stringResource(R.string.login_hide_password) else stringResource(R.string.login_show_password),
                                        tint = StitchLoginRef.Outline.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                                unfocusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                                focusedTextColor = StitchLoginRef.OnSurface,
                                unfocusedTextColor = StitchLoginRef.OnSurface,
                                focusedBorderColor = StitchLoginRef.PrimaryContainer,
                                unfocusedBorderColor = StitchPalette.BorderHairline,
                                cursorColor = StitchLoginRef.PrimaryContainer,
                                focusedLeadingIconColor = StitchLoginRef.PrimaryContainer,
                                unfocusedLeadingIconColor = StitchLoginRef.Outline,
                            ),
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                stringResource(R.string.login_forgot_password),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    letterSpacing = 0.sp,
                                ),
                                color = StitchLoginRef.PrimaryContainer,
                                modifier = Modifier.clickable(enabled = !busy) { hintDialog = (forgotPasswordTitle to forgotPasswordBody) },
                            )
                        }

                        err?.let { msg ->
                            CompactHintLine(
                                fullText = msg,
                                color = StitchPalette.Error,
                                modifier = Modifier.fillMaxWidth(),
                                onTapDetail = { hintDialog = (loginFailedTitle to (errDetail ?: msg)) },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = StitchLoginRef.PrimaryContainer.copy(alpha = 0.2f),
                                    ambientColor = StitchLoginRef.PrimaryContainer.copy(alpha = 0.1f),
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(StitchLoginRef.PrimaryContainer)
                                .clickable(enabled = !busy) {
                                    scope.launch {
                                        err = null
                                        errDetail = null
                                        busy = true
                                        sdk
                                            .login(username.trim(), password)
                                            .fold(
                                                onSuccess = { onLoggedIn(it) },
                                                onFailure = { e ->
                                                    errDetail = humanizeClientFailure(e, apiBase)
                                                    err = loginErrorSummary(e, apiBase)
                                                },
                                            )
                                        busy = false
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    strokeWidth = 2.dp,
                                    color = StitchLoginRef.OnPrimaryButton,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.login_button),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        lineHeight = 22.sp,
                                    ),
                                    color = StitchLoginRef.OnPrimaryButton,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = StitchPalette.BorderHairline,
                            thickness = 1.dp,
                        )
                        Text(
                            stringResource(R.string.login_social_divider),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.sp,
                            ),
                            color = StitchLoginRef.OnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = StitchPalette.BorderHairline,
                            thickness = 1.dp,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SocialCircle(label = "WeChat", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Icon(Icons.Filled.ChatBubble, null, tint = StitchLoginRef.WeChat, modifier = Modifier.size(20.dp))
                        }
                        SocialCircle(label = "QQ", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Icon(Icons.Filled.Pets, null, tint = StitchLoginRef.QqBlue, modifier = Modifier.size(20.dp))
                        }
                        SocialCircle(label = "Google", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = StitchLoginRef.PrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                        SocialCircle(label = "Apple", onClick = { hintDialog = (socialTitle to socialBody) }) {
                            Text("A", color = StitchLoginRef.OnSurface, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.login_no_account), style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.OnSurfaceVariant)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            stringResource(R.string.login_register_now),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = StitchLoginRef.PrimaryContainer,
                            modifier = Modifier.clickable(enabled = !busy) { onNavigateRegister() },
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.login_backend_config, apiBase),
                style = MaterialTheme.typography.labelSmall,
                color = StitchLoginRef.Outline.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApiConfig = true }
                    .padding(top = 20.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                "${stringResource(R.string.theme_title)} · ${loginThemeLabel(currentTheme)}",
                style = MaterialTheme.typography.labelSmall,
                color = StitchLoginRef.Outline.copy(alpha = 0.68f),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showThemePicker = true }
                        .padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
            )
        }

        hintDialog?.let { (title, body) ->
            val dialogScroll = rememberScrollState()
            AlertDialog(
                onDismissRequest = { hintDialog = null },
                confirmButton = {
                    TextButton(onClick = { hintDialog = null }) {
                        Text(stringResource(R.string.common_ok))
                    }
                },
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                text = {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchLoginRef.OnSurface,
                        modifier =
                            Modifier
                                .heightIn(max = 360.dp)
                                .verticalScroll(dialogScroll),
                    )
                },
            )
        }

        if (showThemePicker) {
            LoginThemePickerDialog(
                currentTheme = currentTheme,
                onDismiss = { showThemePicker = false },
                onSelect = { theme ->
                    currentTheme = theme
                    sdk.setTheme(theme)
                    onThemeChanged(theme)
                    showThemePicker = false
                },
            )
        }

        if (showApiConfig) {
            ApiBaseUrlConfigDialog(
                currentUrl = apiBase,
                defaultUrl = BuildConfig.API_BASE_URL,
                onDismiss = { showApiConfig = false },
                onSave = { newUrl ->
                    sdk.sessionStore().setApiUrl(newUrl)
                    val resolved = sdk.baseUrl
                    apiBase = resolved
                    showApiConfig = false
                    
                    scope.launch {
                        localHealthHint = backendConnectingText
                        sdk.health().fold(
                            onSuccess = { localHealthHint = "$backendServicePrefix ${it.status} · ${it.store}" },
                            onFailure = { localHealthHint = humanizeClientFailure(it, resolved) }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun CompactHintLine(
    fullText: String,
    color: Color,
    onTapDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val oneLine = remember(fullText) { fullText.replace('\n', ' ').trim() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clickable { onTapDetail() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            oneLine,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SocialCircle(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(StitchLoginRef.SurfaceContainerLowest)
                .clickable(onClick = onClick)
                .border(BorderStroke(1.dp, StitchPalette.BorderHairline), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = StitchLoginRef.OnSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun loginThemeLabel(theme: String): String =
    when (theme.lowercase()) {
        "honey", "sugar" -> stringResource(R.string.theme_honey)
        "mint" -> stringResource(R.string.theme_mint)
        "night" -> stringResource(R.string.theme_night)
        "neutral", "system" -> stringResource(R.string.theme_neutral)
        else -> stringResource(R.string.theme_honey)
    }

private fun loginErrorSummary(
    throwable: Throwable,
    apiBaseUrl: String,
): String {
    val raw = throwable.message.orEmpty()
    val isBackendConnection =
        raw.contains("[Fiddler]", ignoreCase = true) ||
            raw.contains("<html", ignoreCase = true) ||
            raw.contains("connection refused", ignoreCase = true) ||
            raw.contains("connectionrefused", ignoreCase = true) ||
            raw.contains("failed to connect", ignoreCase = true) ||
            raw.contains("积极拒绝", ignoreCase = true)
    return when {
        isBackendConnection -> "无法连接后端，点击查看配置。"
        throwable is ApiException && raw.isNotBlank() -> raw.take(80)
        else -> humanizeClientFailure(throwable, apiBaseUrl).lineSequence().firstOrNull().orEmpty().ifBlank { "登录失败，请稍后再试。" }
    }
}

@Composable
private fun LoginThemePickerDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val normalized =
        when (currentTheme.lowercase()) {
            "honey", "sugar" -> "honey"
            "mint" -> "mint"
            "night" -> "night"
            "neutral", "system" -> "neutral"
            else -> "honey"
        }
    val options =
        listOf(
            "honey" to stringResource(R.string.theme_honey),
            "mint" to stringResource(R.string.theme_mint),
            "night" to stringResource(R.string.theme_night),
            "neutral" to stringResource(R.string.theme_neutral),
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.theme_choose),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchLoginRef.OnSurfaceVariant,
                )
                options.forEach { (key, label) ->
                    TextButton(
                        onClick = { onSelect(key) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = StitchLoginRef.OnSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (normalized == key) {
                                Text(
                                    stringResource(R.string.common_selected),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StitchLoginRef.PrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
