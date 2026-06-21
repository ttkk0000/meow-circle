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
import com.ttkk0000.meowcircle.kmpapp.theme.MeowStitchTheme
import com.ttkk0000.meowcircle.kmpapp.theme.MeowTheme
import com.ttkk0000.meowcircle.kmpapp.ui.StitchComposeScreen
import com.ttkk0000.meowcircle.kmpapp.ui.AdoptionNavGraph
import com.ttkk0000.meowcircle.kmpapp.ui.CommunityNavGraph
import com.ttkk0000.meowcircle.kmpapp.ui.TradeNavGraph
import com.ttkk0000.meowcircle.kmpapp.ui.StitchLoginScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchModeSelectScreen
import com.ttkk0000.meowcircle.kmpapp.ui.MndAppMode
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
    var activeThemeStr by remember { mutableStateOf(sdk.getTheme()) }
    
    // mode routing
    var appModeStr by remember { mutableStateOf(sdk.sessionStore().getAppMode()) }
    val appMode = remember(appModeStr) {
        when(appModeStr) {
            "Community" -> MndAppMode.Community
            "Adoption" -> MndAppMode.Adoption
            "Trade" -> MndAppMode.Trade
            else -> null
        }
    }

    val activeTheme = when (activeThemeStr.lowercase()) {
        "honey", "sugar" -> MeowTheme.Honey
        "mint" -> MeowTheme.Mint
        "night" -> MeowTheme.Night
        "neutral", "system" -> MeowTheme.Neutral
        else -> MeowTheme.Honey
    }

    LaunchedEffect(Unit) {
        val elapsed =
            measureTimeMillis {
                user = runCatching { sdk.restoreSession() }.getOrNull() ?: sdk.cachedUser()
                healthHint =
                    sdk.health().fold(
                        onSuccess = { "服务 ${it.status} · ${it.store}" },
                        onFailure = { humanizeClientFailure(it, sdk.baseUrl) },
                    )
            }
        val minSplashMs = 750L
        if (elapsed < minSplashMs) delay(minSplashMs - elapsed)
        restoring = false
    }

    MeowStitchTheme(theme = activeTheme) {
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
                            onThemeChanged = {
                                sdk.setTheme(it)
                                activeThemeStr = it
                            },
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
            appMode == null -> 
                StitchModeSelectScreen(
                    apiBase = sdk.baseUrl,
                    user = user!!,
                    onModeSelected = { selectedMode -> 
                        val name = selectedMode.name
                        sdk.sessionStore().setAppMode(name)
                        appModeStr = name
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
            else -> {
                when (appMode) {
                    MndAppMode.Community -> {
                        CommunityNavGraph(
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
                            onThemeChanged = {
                                sdk.setTheme(it)
                                activeThemeStr = it
                            },
                            onChangeMode = { selectedMode ->
                                sdk.sessionStore().setAppMode(selectedMode.name)
                                appModeStr = selectedMode.name
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    MndAppMode.Adoption -> {
                        AdoptionNavGraph(
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
                            onThemeChanged = {
                                sdk.setTheme(it)
                                activeThemeStr = it
                            },
                            onChangeMode = { selectedMode ->
                                sdk.sessionStore().setAppMode(selectedMode.name)
                                appModeStr = selectedMode.name
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    MndAppMode.Trade -> {
                        TradeNavGraph(
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
                            onThemeChanged = {
                                sdk.setTheme(it)
                                activeThemeStr = it
                            },
                            
                            onChangeMode = { selectedMode ->
                                sdk.sessionStore().setAppMode(selectedMode.name)
                                appModeStr = selectedMode.name
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    null -> {}
                }
            }
        }
    }
}
