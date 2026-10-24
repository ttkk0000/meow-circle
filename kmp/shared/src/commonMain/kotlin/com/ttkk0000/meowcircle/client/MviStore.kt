package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 跨平台统一 MVI 状态容器契约（Android / iOS / Web / Desktop 共用语义）。
 * 旋转、分屏、主从切换不得重建业务 Store；所有状态变更通过单向 StateFlow 输出。
 */
interface MviStateOwner<State : Any, Intent : Any, Effect : Any> {
    val state: StateFlow<State>
    val effects: Flow<Effect>
    val currentState: State get() = state.value
    fun accept(intent: Intent)
    fun close()
}

/**
 * KMP 共享层可复用 MVI 基类：
 * 统一托管 [scope]、[state]、[effects] 与按名称去重的协程任务表 [jobs]，
 * 同时兼容子类保留细粒度领域方法与统一 [accept] Intent 分发。
 */
abstract class MviStore<State : Any, Intent : Any, Effect : Any>(
    initialState: State,
    parentScope: CoroutineScope? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : MviStateOwner<State, Intent, Effect> {
    protected val ownJob: Job = SupervisorJob(parentScope?.coroutineContext?.get(Job))
    protected val scope: CoroutineScope = CoroutineScope((parentScope?.coroutineContext ?: dispatcher) + ownJob)
    protected val mutable: MutableStateFlow<State> = MutableStateFlow(initialState)
    protected val _state: MutableStateFlow<State> get() = mutable
    override val state: StateFlow<State> = mutable.asStateFlow()

    private val effectFlow = MutableSharedFlow<Effect>(extraBufferCapacity = 32)
    override val effects: Flow<Effect> = effectFlow.asSharedFlow()

    protected val jobs: MutableMap<String, Job> = mutableMapOf()

    protected fun reduce(reducer: (State) -> State) {
        mutable.update(reducer)
    }

    protected fun setState(next: State) {
        mutable.value = next
    }

    protected fun sendEffect(effect: Effect) {
        effectFlow.tryEmit(effect)
    }

    protected fun cancelJob(key: String) {
        jobs.remove(key)?.cancel()
    }

    protected fun cancelJobsWhere(predicate: (String) -> Boolean) {
        val matched = jobs.keys.filter(predicate)
        matched.forEach { key -> jobs.remove(key)?.cancel() }
    }

    override fun close() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        ownJob.cancel()
    }
}

sealed interface MobileStoreIntent {
    data class SelectTab(val tab: MobileTab) : MobileStoreIntent
    data class OpenDestination(val destination: MobileDestination) : MobileStoreIntent
    data object Back : MobileStoreIntent
    data class RefreshHome(val more: Boolean = false) : MobileStoreIntent
    data class ReloadDetail(val more: Boolean = false) : MobileStoreIntent
    data object ShowLogin : MobileStoreIntent
    data object CancelLogin : MobileStoreIntent
    data object ClearNotice : MobileStoreIntent
}

sealed interface MobileStoreEffect {
    data class Notice(val message: String) : MobileStoreEffect
    data object RequireLogin : MobileStoreEffect
}

sealed interface FeatureStoreIntent {
    data class OpenRoute(val route: FeatureRoute) : FeatureStoreIntent
    data object Back : FeatureStoreIntent
    data object CloseRoute : FeatureStoreIntent
    data class Reload(val more: Boolean = false) : FeatureStoreIntent
    data class EditField(val key: String, val value: String) : FeatureStoreIntent
    data object DiscardAndReload : FeatureStoreIntent
    data object DismissConfirmation : FeatureStoreIntent
    data object SubmitConfirmation : FeatureStoreIntent
    data class Notify(val message: String) : FeatureStoreIntent
}

sealed interface FeatureStoreEffect {
    data class Notice(val message: String) : FeatureStoreEffect
    data class RouteCompleted(val route: FeatureRoute) : FeatureStoreEffect
}

sealed interface SensitiveStoreIntent {
    data class Prepare(val intent: SensitiveIntent) : SensitiveStoreIntent
    data class Verify(val password: String = "", val code: String = "") : SensitiveStoreIntent
    data class Confirm(val userConfirmed: Boolean) : SensitiveStoreIntent
    data object Reconcile : SensitiveStoreIntent
    data object Dismiss : SensitiveStoreIntent
}

sealed interface SensitiveStoreEffect {
    data class Completed(val clientRequestId: String, val resultKind: String?, val resultId: Long?) : SensitiveStoreEffect
}
