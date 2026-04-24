package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.theme.StitchLoginRef
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import kotlinx.coroutines.launch

/** MOBILE「注册页面」：Material 3 Scaffold + Stitch 色与圆角输入（无手机号时使用用户名注册）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchRegisterScreen(
    sdk: MeowCircleSdk,
    onBack: () -> Unit,
    onRegistered: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchLoginRef.Background,
        topBar = {
            TopAppBar(
                title = { Text("注册 · Kitty Circle", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = StitchLoginRef.Background,
                        titleContentColor = StitchLoginRef.OnSurface,
                        navigationIconContentColor = StitchLoginRef.OnSurface,
                    ),
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                "创建账号",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = StitchLoginRef.PrimaryContainer,
            )
            Text(
                "加入 Kitty Circle，分享主子的日常",
                style = MaterialTheme.typography.bodyLarge,
                color = StitchLoginRef.Outline,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            Text(
                "用户名至少 3 位，密码至少 6 位；可不填昵称（将用用户名）。手机号与验证码可选。",
                style = MaterialTheme.typography.bodySmall,
                color = StitchLoginRef.Outline,
            )
            Spacer(Modifier.height(16.dp))
            RegField(
                value = username,
                onValueChange = { username = it },
                placeholder = "用户名",
                leading = Icons.Outlined.Person,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RegField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "手机号",
                leading = Icons.Outlined.Smartphone,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RegField(
                    value = smsCode,
                    onValueChange = { smsCode = it },
                    placeholder = "验证码",
                    leading = Icons.Outlined.MarkEmailRead,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "短信校验可选",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchLoginRef.Outline,
                    modifier =
                        Modifier
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            RegField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = "昵称（可选）",
                leading = Icons.Outlined.Badge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RegField(
                value = password,
                onValueChange = { password = it },
                placeholder = "密码",
                leading = Icons.Outlined.Lock,
                isPassword = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            RegField(
                value = confirm,
                onValueChange = { confirm = it },
                placeholder = "确认密码",
                leading = Icons.Outlined.Lock,
                isPassword = true,
                modifier = Modifier.fillMaxWidth(),
            )
            err?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = StitchPalette.Error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    err = null
                    if (password != confirm) {
                        err = "两次密码不一致"
                        return@Button
                    }
                    scope.launch {
                        busy = true
                        sdk
                            .register(
                                username = username.trim(),
                                password = password,
                                nickname = nickname.trim(),
                                phone = phone.trim(),
                                smsCode = smsCode.trim(),
                            )
                            .fold(
                                onSuccess = { onRegistered(it) },
                                onFailure = { e ->
                                    err =
                                        (e as? ApiException)?.message
                                            ?: humanizeClientFailure(e, BuildConfig.API_BASE_URL)
                                },
                            )
                        busy = false
                    }
                },
                enabled = !busy,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(StitchLoginRef.PrimaryContainer, StitchLoginRef.InversePrimary),
                            ),
                            RoundedCornerShape(32.dp),
                        ),
                shape = RoundedCornerShape(32.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent,
                    ),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "注册",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Text(
                "我已阅读并同意 用户协议 和 隐私政策",
                style = MaterialTheme.typography.labelSmall,
                color = StitchLoginRef.Outline,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已有账号？", style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.Outline)
                Spacer(Modifier.size(4.dp))
                Text(
                    "立即登录",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = StitchLoginRef.PrimaryContainer,
                    modifier = Modifier.clickable(enabled = !busy) { onBack() },
                )
            }
        }
    }
}

@Composable
private fun RegField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 56.dp),
        placeholder = { Text(placeholder, color = StitchLoginRef.Outline.copy(alpha = 0.6f)) },
        leadingIcon = {
            Icon(leading, contentDescription = null, tint = StitchLoginRef.Outline, modifier = Modifier.size(22.dp))
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
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
            ),
    )
}
