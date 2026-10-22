package com.ttkk0000.meowcircle.kmpapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.kmpapp.theme.MeowStitchTheme
import com.ttkk0000.meowcircle.kmpapp.ui.AdoptionNavGraph
import com.ttkk0000.meowcircle.kmpapp.ui.CommunityNavGraph
import com.ttkk0000.meowcircle.kmpapp.ui.MndAppMode
import com.ttkk0000.meowcircle.kmpapp.ui.StitchComposeScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchLoginScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchModeSelectScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchPostDetailScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchRegisterScreen
import com.ttkk0000.meowcircle.kmpapp.ui.StitchSplashScreen
import com.ttkk0000.meowcircle.kmpapp.ui.TradeNavGraph

@Composable
fun MeowApp(
    sdk: MeowCircleSdk,
    viewModel: MeowAppViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.accept(MeowAppIntent.Bootstrap)
    }
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MeowAppEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    MeowStitchTheme(theme = state.theme) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.restoring ->
                    StitchSplashScreen(
                        modifier = Modifier.fillMaxSize(),
                        loading = true,
                    )
                state.user == null ->
                    when (state.authDestination) {
                        AuthDestination.Login ->
                            StitchLoginScreen(
                                sdk = sdk,
                                healthHint = state.healthHint,
                                onLoggedIn = { viewModel.accept(MeowAppIntent.Authenticated(it)) },
                                onNavigateRegister = {
                                    viewModel.accept(MeowAppIntent.ShowAuth(AuthDestination.Register))
                                },
                                onThemeChanged = {
                                    viewModel.accept(MeowAppIntent.ChangeTheme(it))
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        AuthDestination.Register ->
                            StitchRegisterScreen(
                                sdk = sdk,
                                onBack = {
                                    viewModel.accept(MeowAppIntent.ShowAuth(AuthDestination.Login))
                                },
                                onRegistered = {
                                    viewModel.accept(MeowAppIntent.Authenticated(it))
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                    }
                state.appMode == null ->
                    StitchModeSelectScreen(
                        apiBase = sdk.baseUrl,
                        user = checkNotNull(state.user),
                        onModeSelected = { viewModel.accept(MeowAppIntent.SelectMode(it)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                state.composeOpen ->
                    StitchComposeScreen(
                        sdk = sdk,
                        onClose = { viewModel.accept(MeowAppIntent.CloseComposer) },
                        onPosted = { viewModel.accept(MeowAppIntent.PostPublished) },
                        modifier = Modifier.fillMaxSize(),
                    )
                state.postDetailId != null ->
                    StitchPostDetailScreen(
                        sdk = sdk,
                        postId = checkNotNull(state.postDetailId),
                        onBack = { viewModel.accept(MeowAppIntent.ClosePost) },
                        modifier = Modifier.fillMaxSize(),
                    )
                else ->
                    AppModeContent(
                        sdk = sdk,
                        state = state,
                        dispatch = viewModel::accept,
                    )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun AppModeContent(
    sdk: MeowCircleSdk,
    state: MeowAppState,
    dispatch: (MeowAppIntent) -> Unit,
) {
    val user = checkNotNull(state.user)
    val commonLogout = { dispatch(MeowAppIntent.Logout) }
    val openPost = { id: Long -> dispatch(MeowAppIntent.OpenPost(id)) }
    val openComposer = { dispatch(MeowAppIntent.OpenComposer) }
    val changeTheme = { value: String -> dispatch(MeowAppIntent.ChangeTheme(value)) }
    val changeMode = { mode: MndAppMode -> dispatch(MeowAppIntent.SelectMode(mode)) }

    when (checkNotNull(state.appMode)) {
        MndAppMode.Community ->
            CommunityNavGraph(
                sdk = sdk,
                user = user,
                feedReloadSignal = state.feedReloadSignal,
                onLogout = commonLogout,
                onOpenPost = openPost,
                onCompose = openComposer,
                onThemeChanged = changeTheme,
                onChangeMode = changeMode,
                modifier = Modifier.fillMaxSize(),
            )
        MndAppMode.Adoption ->
            AdoptionNavGraph(
                sdk = sdk,
                user = user,
                feedReloadSignal = state.feedReloadSignal,
                onLogout = commonLogout,
                onOpenPost = openPost,
                onCompose = openComposer,
                onThemeChanged = changeTheme,
                onChangeMode = changeMode,
                modifier = Modifier.fillMaxSize(),
            )
        MndAppMode.Trade ->
            TradeNavGraph(
                sdk = sdk,
                user = user,
                feedReloadSignal = state.feedReloadSignal,
                onLogout = commonLogout,
                onOpenPost = openPost,
                onCompose = openComposer,
                onThemeChanged = changeTheme,
                onChangeMode = changeMode,
                modifier = Modifier.fillMaxSize(),
            )
    }
}
