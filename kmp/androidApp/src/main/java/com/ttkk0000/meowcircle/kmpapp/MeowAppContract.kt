package com.ttkk0000.meowcircle.kmpapp

import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.theme.MeowTheme
import com.ttkk0000.meowcircle.kmpapp.ui.MndAppMode

enum class AuthDestination {
    Login,
    Register,
}

data class MeowAppState(
    val restoring: Boolean = true,
    val user: User? = null,
    val healthHint: String? = null,
    val authDestination: AuthDestination = AuthDestination.Login,
    val appMode: MndAppMode? = null,
    val postDetailId: Long? = null,
    val composeOpen: Boolean = false,
    val feedReloadSignal: Int = 0,
    val theme: MeowTheme = MeowTheme.Honey,
)

sealed interface MeowAppIntent {
    data object Bootstrap : MeowAppIntent

    data class Authenticated(val user: User) : MeowAppIntent

    data class ShowAuth(val destination: AuthDestination) : MeowAppIntent

    data class SelectMode(val mode: MndAppMode) : MeowAppIntent

    data class ChangeTheme(val storedValue: String) : MeowAppIntent

    data class OpenPost(val postId: Long) : MeowAppIntent

    data object ClosePost : MeowAppIntent

    data object OpenComposer : MeowAppIntent

    data object CloseComposer : MeowAppIntent

    data object PostPublished : MeowAppIntent

    data object Logout : MeowAppIntent
}

sealed interface MeowAppEffect {
    data class ShowMessage(val message: String) : MeowAppEffect
}
