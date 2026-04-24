package com.ttkk0000.meowcircle.kmpapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.ui.StitchFeedScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchLoginScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchSplashScreen
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay

@Composable
fun MeowApp(sdk: MeowCircleSdk) {
    var restoring by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<User?>(sdk.cachedUser()) }
    var healthHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val elapsed =
            measureTimeMillis {
                user = runCatching { sdk.restoreSession() }.getOrNull() ?: sdk.cachedUser()
                healthHint =
                    sdk.health().fold(
                        onSuccess = { "服务 ${it.status} · ${it.store}" },
                        onFailure = { it.message },
                    )
            }
        val minSplashMs = 750L
        if (elapsed < minSplashMs) delay(minSplashMs - elapsed)
        restoring = false
    }

    when {
        restoring -> StitchSplashScreen(Modifier.fillMaxSize(), loading = true)
        user == null ->
            StitchLoginScreen(
                sdk = sdk,
                healthHint = healthHint,
                onLoggedIn = { user = it },
                modifier = Modifier.fillMaxSize(),
            )
        else ->
            StitchFeedScreen(
                sdk = sdk,
                user = user!!,
                onLogout = {
                    sdk.logout()
                    user = null
                },
                modifier = Modifier.fillMaxSize(),
            )
    }
}
