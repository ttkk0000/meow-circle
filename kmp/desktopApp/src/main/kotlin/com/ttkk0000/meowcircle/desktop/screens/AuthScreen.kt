package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.desktop.LocalDesktopStitchColors
import com.ttkk0000.meowcircle.desktop.MndDesktopShape
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    sdk: MeowCircleSdk,
    onAuthSuccess: (User) -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    val scope = rememberCoroutineScope()
    var isRegisterMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.width(420.dp).wrapContentHeight(),
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRegisterMode) {
                    TextButton(
                        onClick = {
                            isRegisterMode = false
                            errorMsg = null
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("← Back to Login", color = colors.brand, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = if (isRegisterMode) "Create M&D Account" else "Welcome Back to M&D",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.onSurface
                )

                Text(
                    text = if (isRegisterMode) "Join the cat-first pet community today." else "Access your feed, market, and messages.",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = colors.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MndDesktopShape.field
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MndDesktopShape.field
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MndDesktopShape.field
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MndDesktopShape.field
                    )

                    OutlinedTextField(
                        value = smsCode,
                        onValueChange = { smsCode = it },
                        label = { Text("Verification Code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MndDesktopShape.field
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            errorMsg = null
                            if (isRegisterMode) {
                                sdk.register(username, password, nickname, phone, smsCode)
                                    .onSuccess { onAuthSuccess(it) }
                                    .onFailure { errorMsg = it.message ?: "Registration failed" }
                            } else {
                                sdk.login(username, password)
                                    .onSuccess { onAuthSuccess(it) }
                                    .onFailure { errorMsg = it.message ?: "Login failed" }
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MndDesktopShape.pill,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = MaterialTheme.colorScheme.onPrimary),
                    enabled = !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (isRegisterMode) "Register" else "Login", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        errorMsg = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isRegisterMode) "Already have an account? Sign In" else "New to M&D? Create Account",
                        color = colors.brand,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
