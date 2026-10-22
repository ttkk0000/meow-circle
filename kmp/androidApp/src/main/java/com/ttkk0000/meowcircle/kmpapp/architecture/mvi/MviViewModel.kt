package com.ttkk0000.meowcircle.kmpapp.architecture.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Android Compose 单向状态基类。
 *
 * 页面只读取不可变 [state]、通过 [accept] 派发意图；导航、Toast 等一次性动作使用
 * [effects]，避免把瞬态事件长期塞进可恢复状态。
 */
abstract class MviViewModel<State : Any, Intent : Any, Effect : Any>(
    initialState: State,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    private val effectChannel = Channel<Effect>(Channel.BUFFERED)

    val state: StateFlow<State> = mutableState.asStateFlow()
    val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    protected val currentState: State
        get() = mutableState.value

    abstract fun accept(intent: Intent)

    @Synchronized
    protected fun reduce(reducer: (State) -> State) {
        val current = mutableState.value
        val next = reducer(current)
        if (next != current) {
            mutableState.value = next
        }
    }

    protected fun sendEffect(effect: Effect) {
        effectChannel.trySend(effect)
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(block = block)

    override fun onCleared() {
        effectChannel.close()
        super.onCleared()
    }
}
