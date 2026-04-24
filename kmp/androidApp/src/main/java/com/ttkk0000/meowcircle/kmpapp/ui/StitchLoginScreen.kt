package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import kotlinx.coroutines.launch

@Composable
fun StitchLoginScreen(
    sdk: MeowCircleSdk,
    healthHint: String?,
    onLoggedIn: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("demo") }
    var password by remember { mutableStateOf("123456") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var showPw by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(StitchPalette.Canvas),
    ) {
        Box(
            modifier =
                Modifier
                    .size(220.dp)
                    .offset((-40).dp, (-80).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(StitchPalette.Brand.copy(alpha = 0.22f), Color.Transparent),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .size(180.dp)
                    .align(Alignment.BottomEnd)
                    .offset(40.dp, 60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(StitchPalette.GoldWeak, Color.Transparent),
                        ),
                    ),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            healthHint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = StitchShadows.cardAmbientY,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = StitchShadows.cardAmbientColor,
                            spotColor = StitchShadows.cardAmbientColor,
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .padding(20.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        StitchPalette.BrandLight.copy(alpha = 0.55f),
                                        StitchPalette.SecondaryContainer.copy(alpha = 0.7f),
                                    ),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Kitty Circle",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.95f),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "MEOW",
                    style = MaterialTheme.typography.displaySmall,
                    color = StitchPalette.Brand,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    "欢迎回来",
                    style = MaterialTheme.typography.headlineMedium,
                    color = StitchPalette.OnSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    "登录以继续",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(16.dp))
                Text("手机号 / 用户名", style = MaterialTheme.typography.labelLarge, color = StitchPalette.OnSurfaceVariant)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = stitchFieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                Text("密码", style = MaterialTheme.typography.labelLarge, color = StitchPalette.OnSurfaceVariant)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPw = !showPw }) {
                            Icon(
                                imageVector = if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPw) "隐藏密码" else "显示密码",
                                tint = StitchPalette.Outline,
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = stitchFieldColors(),
                )
                err?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = StitchPalette.Error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            err = null
                            busy = true
                            sdk
                                .login(username.trim(), password)
                                .fold(
                                    onSuccess = { onLoggedIn(it) },
                                    onFailure = { e ->
                                        err = (e as? ApiException)?.message ?: e.message ?: "登录失败"
                                    },
                                )
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = StitchShadows.ctaGlowY,
                                shape = RoundedCornerShape(999.dp),
                                ambientColor = StitchShadows.ctaGlowColor,
                                spotColor = StitchShadows.ctaGlowColor,
                            )
                            .background(
                                brush =
                                    Brush.horizontalGradient(
                                        listOf(StitchPalette.Brand, StitchPalette.BrandLight),
                                    ),
                                shape = RoundedCornerShape(999.dp),
                            ),
                    shape = RoundedCornerShape(999.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Transparent,
                        ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text("登录", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "后端 ${BuildConfig.API_BASE_URL}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.Outline,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun stitchFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = StitchPalette.Brand.copy(alpha = 0.5f),
        unfocusedBorderColor = StitchPalette.OutlineVariant,
        focusedContainerColor = StitchPalette.SurfaceContainer,
        unfocusedContainerColor = StitchPalette.SurfaceContainer,
        cursorColor = StitchPalette.Brand,
    )
