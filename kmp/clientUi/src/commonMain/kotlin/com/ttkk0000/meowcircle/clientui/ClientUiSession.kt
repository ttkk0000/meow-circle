package com.ttkk0000.meowcircle.clientui

import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.FeaturePage
import com.ttkk0000.meowcircle.client.FeatureRoute

/** ViewModel-owned values only: no Activity, View, callback, or system Bundle. */
class ClientUiSessionState {
    private data class Entry(val generation: Any?, val state: MutableState<*>)
    private val values = mutableMapOf<Pair<String, String>, Entry>()
    private var owner: Long? = null

    fun useOwner(value: Long) {
        if (owner != value) { clear(); owner = value }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> field(scope: String, key: String, generation: Any?, initial: () -> T): MutableState<T> {
        val id = scope to key
        val old = values[id]
        if (old != null && old.generation == generation) return old.state as MutableState<T>
        return mutableStateOf(initial()).also { values[id] = Entry(generation, it) }
    }

    fun retainTransientScopes(active: Set<String>) {
        values.keys.removeAll { (scope, _) -> scope.startsWith("transient:") && scope !in active }
    }

    fun clear() { values.clear() }
}

val LocalUiSession = staticCompositionLocalOf<ClientUiSessionState> { error("Client UI session is required") }
val LocalUiScope = staticCompositionLocalOf { "screen:root" }
fun featureUiScope(route: FeatureRoute): String =
    "${if (route.page in setOf(FeaturePage.Security, FeaturePage.Safety)) "transient" else "screen"}:feature-${route.page}-${route.id}-${route.kind}-${route.peer}"

@Composable fun <T> rememberSessionUiState(key: String, scope: String? = null, generation: Any? = null, initial: () -> T): MutableState<T> {
    val session = LocalUiSession.current
    val namespace = scope ?: LocalUiScope.current
    return remember(session, namespace, key, generation) { session.field(namespace, key, generation, initial) }
}
