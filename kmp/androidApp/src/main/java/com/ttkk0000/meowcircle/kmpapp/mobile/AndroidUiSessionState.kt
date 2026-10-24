package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.FeaturePage
import com.ttkk0000.meowcircle.client.FeatureRoute

/** ViewModel-owned values only: no Activity, View, callback, or system Bundle. */
class AndroidUiSessionState {
    private data class Entry(val generation: Any?, val state: MutableState<*>)
    private val values = mutableMapOf<Pair<String, String>, Entry>()
    private var owner: Long? = null

    internal fun useOwner(value: Long) {
        if (owner != value) { clear(); owner = value }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> field(scope: String, key: String, generation: Any?, initial: () -> T): MutableState<T> {
        val id = scope to key
        val old = values[id]
        if (old != null && old.generation == generation) return old.state as MutableState<T>
        return mutableStateOf(initial()).also { values[id] = Entry(generation, it) }
    }

    internal fun retainTransientScopes(active: Set<String>) {
        values.keys.removeAll { (scope, _) -> scope.startsWith("transient:") && scope !in active }
    }

    internal fun clear() { values.clear() }
}

internal val LocalUiSession = staticCompositionLocalOf<AndroidUiSessionState> { error("Android UI session is required") }
internal val LocalUiScope = staticCompositionLocalOf { "screen:root" }
internal fun featureUiScope(route: FeatureRoute): String =
    "${if (route.page in setOf(FeaturePage.Security, FeaturePage.Safety)) "transient" else "screen"}:feature-${route.page}-${route.id}-${route.kind}-${route.peer}"

@Composable internal fun <T> rememberSessionUiState(key: String, scope: String? = null, generation: Any? = null, initial: () -> T): MutableState<T> {
    val session = LocalUiSession.current
    val namespace = scope ?: LocalUiScope.current
    return remember(session, namespace, key, generation) { session.field(namespace, key, generation, initial) }
}
