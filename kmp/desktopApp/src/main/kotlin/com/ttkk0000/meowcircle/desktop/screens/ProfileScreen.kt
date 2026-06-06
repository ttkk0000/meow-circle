package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.desktop.LocalDesktopStitchColors
import com.ttkk0000.meowcircle.desktop.MndApiClient
import com.ttkk0000.meowcircle.desktop.MndDesktopShape
import com.ttkk0000.meowcircle.desktop.UserStats
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    sdk: MeowCircleSdk,
    apiClient: MndApiClient,
    profileBg: String,
    onLogout: () -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf<User?>(null) }
    var stats by remember { mutableStateOf<UserStats?>(null) }

    var isEditing by remember { mutableStateOf(false) }
    var editNickname by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadProfile() {
        scope.launch {
            loading = true
            currentUser = sdk.cachedUser() ?: runCatching { sdk.me() }.getOrNull()
            currentUser?.let { user ->
                editNickname = user.nickname
                editBio = user.bio
                apiClient.fetchUserStats(user.id)
                    .onSuccess { stats = it }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Background Tint Card (Picnic, Desk, Arcade, Garden)
        val bgTint = when (profileBg) {
            "picnic" -> Color(0xFFFFECE0)
            "desk" -> Color(0xFFE0F2FE)
            "arcade" -> Color(0xFFF3E8FF)
            "garden" -> Color(0xFFDCFCE7)
            else -> colors.surfaceLow
        }

        Card(
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Profile Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(bgTint),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = 44.dp)
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(5.dp, colors.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐱", fontSize = 42.sp)
                    }
                }

                Spacer(Modifier.height(54.dp))

                // Profile Details
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val user = currentUser
                    if (user != null) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editNickname,
                                onValueChange = { editNickname = it },
                                label = { Text("Nickname") },
                                shape = MndDesktopShape.field,
                                modifier = Modifier.width(360.dp)
                            )
                            OutlinedTextField(
                                value = editBio,
                                onValueChange = { editBio = it },
                                label = { Text("Bio") },
                                shape = MndDesktopShape.field,
                                modifier = Modifier.width(360.dp),
                                maxLines = 4
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            loading = true
                                            sdk.updateMe(nickname = editNickname, avatarUrl = user.avatarUrl, bio = editBio)
                                                .onSuccess {
                                                    currentUser = it
                                                    isEditing = false
                                                    loadProfile()
                                                }
                                                .onFailure {
                                                    errorMsg = it.message ?: "Failed to update profile"
                                                }
                                            loading = false
                                        }
                                    },
                                    shape = MndDesktopShape.pill,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = Color.White)
                                ) {
                                    Text("Save Changes", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        editNickname = user.nickname
                                        editBio = user.bio
                                        isEditing = false
                                    },
                                    shape = MndDesktopShape.pill
                                ) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            Text(
                                text = user.nickname.ifBlank { user.username },
                                color = colors.onSurface,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "@${user.username} · ${user.bio.ifBlank { "A cat lover on M&D" }}",
                                color = colors.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { isEditing = true },
                                shape = MndDesktopShape.pill,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = Color.White),
                                modifier = Modifier.width(140.dp)
                            ) {
                                Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "Peach & Latte",
                            color = colors.onSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "@peachlatte · Documenting the daily life of two cats.",
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Login to customize your profile.",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Stats Grid
                    Row(
                        modifier = Modifier
                            .width(360.dp)
                            .clip(MndDesktopShape.card)
                            .background(colors.surfaceLow)
                            .border(1.dp, colors.outlineVariant, MndDesktopShape.card)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("128", color = colors.brand, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Posts", color = colors.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(24.dp), color = colors.outlineVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stats?.followers?.toString() ?: "42",
                                color = colors.brand,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text("Followers", color = colors.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(24.dp), color = colors.outlineVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stats?.following?.toString() ?: "2",
                                color = colors.brand,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text("Following", color = colors.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }

                    // Logout Button
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            sdk.logout()
                            onLogout()
                        },
                        shape = MndDesktopShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = Color.White),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
