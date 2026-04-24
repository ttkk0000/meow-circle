package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.ttkk0000.meowcircle.kmpapp.theme.StitchLoginRef
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import kotlinx.coroutines.launch

/**
 * Matches Stitch **MOBILE** screen「登录页面」(`deviceType=MOBILE`, not the desktop-titled export):
 * open canvas, MEOW lockup + sparkle, tagline, icon fields, forgot password, CTA, social row, register footer.
 */
@Composable
fun StitchLoginScreen(
    sdk: MeowCircleSdk,
    healthHint: String?,
    onLoggedIn: (User) -> Unit,
    onNavigateRegister: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("demo") }
    var password by remember { mutableStateOf("123456") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var showPw by remember { mutableStateOf(false) }
    var hintDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val scroll = rememberScrollState()

    Box(modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(50.dp, (-100).dp)
                    .size(256.dp)
                    .clip(CircleShape)
                    .background(StitchLoginRef.PrimaryContainer.copy(alpha = 0.10f)),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset((-80).dp, 120.dp)
                    .size(288.dp)
                    .clip(CircleShape)
                    .background(StitchLoginRef.SecondaryContainer.copy(alpha = 0.10f)),
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 28.dp),
        ) {
            healthHint?.let { hint ->
                val isBad = hint.contains("超时") || hint.contains("无法连接") || hint.contains("失败")
                CompactHintLine(
                    fullText = hint,
                    color = if (isBad) StitchPalette.Error else StitchLoginRef.Outline,
                    modifier = Modifier.padding(bottom = 8.dp),
                    onTapDetail = { hintDialog = ("后端状态" to hint) },
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "MEOW",
                        fontSize = 56.sp,
                        lineHeight = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = StitchLoginRef.PrimaryContainer,
                        letterSpacing = (-1).sp,
                    )
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = StitchLoginRef.SecondaryContainer,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 28.dp, y = (-6).dp)
                                .size(28.dp),
                    )
                }
                Text(
                    "欢迎回到喵圈",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StitchLoginRef.Outline,
                )
            }

            Spacer(Modifier.height(40.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    placeholder = {
                        Text(
                            "手机号/邮箱",
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
                    shape = RoundedCornerShape(32.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                            unfocusedContainerColor = StitchLoginRef.SurfaceContainerLow,
                            focusedTextColor = StitchLoginRef.OnSurface,
                            unfocusedTextColor = StitchLoginRef.OnSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = StitchLoginRef.PrimaryContainer,
                            focusedLeadingIconColor = StitchLoginRef.PrimaryContainer,
                            unfocusedLeadingIconColor = StitchLoginRef.Outline,
                        ),
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    placeholder = {
                        Text(
                            "密码",
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
                                contentDescription = if (showPw) "隐藏密码" else "显示密码",
                                tint = StitchLoginRef.Outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(32.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                            unfocusedContainerColor = StitchLoginRef.SurfaceContainerLow,
                            focusedTextColor = StitchLoginRef.OnSurface,
                            unfocusedTextColor = StitchLoginRef.OnSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = StitchLoginRef.PrimaryContainer,
                            focusedLeadingIconColor = StitchLoginRef.PrimaryContainer,
                            unfocusedLeadingIconColor = StitchLoginRef.Outline,
                        ),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "忘记密码？",
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.2.sp,
                            ),
                        color = StitchLoginRef.PrimaryContainer,
                        modifier = Modifier.clickable(enabled = !busy) { /* Stitch placeholder */ },
                    )
                }

                err?.let { msg ->
                    CompactHintLine(
                        fullText = msg,
                        color = StitchPalette.Error,
                        modifier = Modifier.fillMaxWidth(),
                        onTapDetail = { hintDialog = ("登录失败" to msg) },
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(32.dp),
                                spotColor = StitchLoginRef.PrimaryContainer.copy(alpha = 0.25f),
                                ambientColor = StitchLoginRef.PrimaryContainer.copy(alpha = 0.12f),
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(StitchLoginRef.PrimaryContainer, StitchLoginRef.InversePrimary),
                                ),
                            )
                            .clickable(enabled = !busy) {
                                scope.launch {
                                    err = null
                                    busy = true
                                    sdk
                                        .login(username.trim(), password)
                                        .fold(
                                            onSuccess = { onLoggedIn(it) },
                                            onFailure = { e ->
                                                err =
                                                    (e as? ApiException)?.message
                                                        ?: humanizeClientFailure(e, BuildConfig.API_BASE_URL)
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
                            "登录",
                            style =
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp,
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
                    color = StitchLoginRef.SurfaceVariant,
                    thickness = 1.dp,
                )
                Text(
                    "或使用以下方式登录",
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.2.sp,
                        ),
                    color = StitchLoginRef.Outline,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center,
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = StitchLoginRef.SurfaceVariant,
                    thickness = 1.dp,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocialCircle(onClick = { /* Stitch decorative */ }) {
                    Icon(Icons.Filled.ChatBubble, null, tint = StitchLoginRef.WeChat, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.size(24.dp))
                SocialCircle(onClick = { }) {
                    Icon(Icons.Filled.Pets, null, tint = StitchLoginRef.QqBlue, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.size(24.dp))
                SocialCircle(onClick = { }) {
                    Icon(
                        Icons.Outlined.Smartphone,
                        null,
                        tint = StitchLoginRef.OnSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("还没有账号？", style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.Outline)
                Spacer(Modifier.size(4.dp))
                Text(
                    "立即注册",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = StitchLoginRef.PrimaryContainer,
                    modifier = Modifier.clickable(enabled = !busy) { onNavigateRegister() },
                )
            }

            Text(
                "后端 ${BuildConfig.API_BASE_URL}",
                style = MaterialTheme.typography.labelSmall,
                color = StitchLoginRef.Outline.copy(alpha = 0.55f),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                textAlign = TextAlign.Center,
            )
        }

        hintDialog?.let { (title, body) ->
            val dialogScroll = rememberScrollState()
            AlertDialog(
                onDismissRequest = { hintDialog = null },
                confirmButton = {
                    TextButton(onClick = { hintDialog = null }) {
                        Text("知道了")
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(StitchLoginRef.SurfaceContainerLow)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
