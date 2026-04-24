package com.ttkk0000.meowcircle.kmpapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.ui.StitchComposeScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchFeedScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchLoginScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchPostDetailScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchRegisterScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchSplashScreen
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay

private enum class AuthScreen {
    Login,
    Register,
}

@Composable
fun MeowApp(sdk: MeowCircleSdk) {
    var restoring by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<User?>(sdk.cachedUser()) }
    var healthHint by remember { mutableStateOf<String?>(null) }
    var authScreen by remember { mutableStateOf(AuthScreen.Login) }
    var postDetailId by remember { mutableStateOf<Long?>(null) }
    var composeOpen by remember { mutableStateOf(false) }
    var feedReloadSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val elapsed =
            measureTimeMillis {
                user = runCatching { sdk.restoreSession() }.getOrNull() ?: sdk.cachedUser()
                healthHint =
                    sdk.health().fold(
                        onSuccess = { "服务 ${it.status} · ${it.store}" },
                        onFailure = { humanizeClientFailure(it, BuildConfig.API_BASE_URL) },
                    )
            }
        val minSplashMs = 750L
        if (elapsed < minSplashMs) delay(minSplashMs - elapsed)
        restoring = false
    }

    when {
        restoring -> StitchSplashScreen(Modifier.fillMaxSize(), loading = true)
        user == null ->
            when (authScreen) {
                AuthScreen.Login ->
                    StitchLoginScreen(
                        sdk = sdk,
                        healthHint = healthHint,
                        onLoggedIn = {
                            user = it
                            authScreen = AuthScreen.Login
                        },
                        onNavigateRegister = { authScreen = AuthScreen.Register },
                        modifier = Modifier.fillMaxSize(),
                    )
                AuthScreen.Register ->
                    StitchRegisterScreen(
                        sdk = sdk,
                        onBack = { authScreen = AuthScreen.Login },
                        onRegistered = {
                            user = it
                            authScreen = AuthScreen.Login
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        composeOpen ->
            StitchComposeScreen(
                sdk = sdk,
                onClose = { composeOpen = false },
                onPosted = { feedReloadSignal++ },
                modifier = Modifier.fillMaxSize(),
            )
        postDetailId != null ->
            StitchPostDetailScreen(
                sdk = sdk,
                postId = postDetailId!!,
                onBack = { postDetailId = null },
                modifier = Modifier.fillMaxSize(),
            )
        else ->
            StitchFeedScreen(
                sdk = sdk,
                user = user!!,
                feedReloadSignal = feedReloadSignal,
                onLogout = {
                    sdk.logout()
                    user = null
                    authScreen = AuthScreen.Login
                },
                onOpenPost = { postDetailId = it },
                onCompose = { composeOpen = true },
                modifier = Modifier.fillMaxSize(),
            )
    }
}
