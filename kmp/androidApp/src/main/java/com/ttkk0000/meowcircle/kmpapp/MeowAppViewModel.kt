package com.ttkk0000.meowcircle.kmpapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.architecture.mvi.MviViewModel
import com.ttkk0000.meowcircle.kmpapp.theme.MeowTheme
import com.ttkk0000.meowcircle.kmpapp.ui.MndAppMode
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay

class MeowAppViewModel(
    private val sdk: MeowCircleSdk,
) : MviViewModel<MeowAppState, MeowAppIntent, MeowAppEffect>(
        initialState =
            MeowAppState(
                user = sdk.cachedUser(),
                appMode = sdk.sessionStore().getAppMode().toAppMode(),
                theme = sdk.getTheme().toMeowTheme(),
            ),
    ) {
    private var bootstrapped = false

    override fun accept(intent: MeowAppIntent) {
        when (intent) {
            MeowAppIntent.Bootstrap -> bootstrap()
            is MeowAppIntent.Authenticated ->
                reduce {
                    it.copy(
                        user = intent.user,
                        authDestination = AuthDestination.Login,
                    )
                }
            is MeowAppIntent.ShowAuth ->
                reduce { it.copy(authDestination = intent.destination) }
            is MeowAppIntent.SelectMode -> {
                sdk.sessionStore().setAppMode(intent.mode.name)
                reduce { it.copy(appMode = intent.mode) }
            }
            is MeowAppIntent.ChangeTheme -> {
                sdk.setTheme(intent.storedValue)
                reduce { it.copy(theme = sdk.getTheme().toMeowTheme()) }
            }
            is MeowAppIntent.OpenPost ->
                reduce { it.copy(postDetailId = intent.postId) }
            MeowAppIntent.ClosePost ->
                reduce { it.copy(postDetailId = null) }
            MeowAppIntent.OpenComposer ->
                reduce { it.copy(composeOpen = true) }
            MeowAppIntent.CloseComposer ->
                reduce { it.copy(composeOpen = false) }
            MeowAppIntent.PostPublished -> {
                reduce { it.copy(feedReloadSignal = it.feedReloadSignal + 1) }
                sendEffect(MeowAppEffect.ShowMessage("发布成功"))
            }
            MeowAppIntent.Logout -> {
                sdk.logout()
                reduce {
                    it.copy(
                        user = null,
                        authDestination = AuthDestination.Login,
                        postDetailId = null,
                        composeOpen = false,
                    )
                }
                sendEffect(MeowAppEffect.ShowMessage("已退出登录"))
            }
        }
    }

    private fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true
        launch {
            val elapsed =
                measureTimeMillis {
                    val restoredUser =
                        runCatching { sdk.restoreSession() }.getOrNull()
                            ?: sdk.cachedUser()
                    val healthHint =
                        sdk.health().fold(
                            onSuccess = { "服务 ${it.status} · ${it.store}" },
                            onFailure = { humanizeClientFailure(it, sdk.baseUrl) },
                        )
                    reduce {
                        it.copy(
                            user = restoredUser,
                            healthHint = healthHint,
                        )
                    }
                }
            val remaining = MIN_SPLASH_MS - elapsed
            if (remaining > 0) delay(remaining)
            reduce { it.copy(restoring = false) }
        }
    }

    class Factory(
        private val sdk: MeowCircleSdk,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MeowAppViewModel::class.java)) {
                "Unsupported ViewModel: ${modelClass.name}"
            }
            return MeowAppViewModel(sdk) as T
        }
    }

    private companion object {
        const val MIN_SPLASH_MS = 750L
    }
}

private fun String?.toAppMode(): MndAppMode? =
    when (this) {
        MndAppMode.Community.name -> MndAppMode.Community
        MndAppMode.Adoption.name -> MndAppMode.Adoption
        MndAppMode.Trade.name -> MndAppMode.Trade
        else -> null
    }

private fun String.toMeowTheme(): MeowTheme =
    when (lowercase()) {
        "honey", "sugar" -> MeowTheme.Honey
        "mint" -> MeowTheme.Mint
        "night" -> MeowTheme.Night
        "neutral", "system" -> MeowTheme.Neutral
        else -> MeowTheme.Honey
    }
