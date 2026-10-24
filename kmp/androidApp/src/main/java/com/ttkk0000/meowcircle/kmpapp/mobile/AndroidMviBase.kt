package com.ttkk0000.meowcircle.kmpapp.mobile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttkk0000.meowcircle.client.FeatureState
import com.ttkk0000.meowcircle.client.FeatureStoreEffect
import com.ttkk0000.meowcircle.client.FeatureStoreIntent
import com.ttkk0000.meowcircle.client.MobileState
import com.ttkk0000.meowcircle.client.MobileStoreEffect
import com.ttkk0000.meowcircle.client.MobileStoreIntent
import com.ttkk0000.meowcircle.client.MviStateOwner
import com.ttkk0000.meowcircle.client.SensitiveActionState
import com.ttkk0000.meowcircle.client.SensitiveStoreEffect
import com.ttkk0000.meowcircle.client.SensitiveStoreIntent
import com.ttkk0000.meowcircle.kmpapp.architecture.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/**
 * Android 手机／平板统一 MVI ViewModel 基类。
 * 继承自 [MviViewModel]，提供与 KMP 共享层 [MviStateOwner] 的状态／副作用桥接原语，
 * 使宿主 ViewModel 与多 Store 保持单一事实来源（Single Source of Truth）。
 */
abstract class MobileMviViewModel<State : Any, Intent : Any, Effect : Any>(
    initialState: State,
) : MviViewModel<State, Intent, Effect>(initialState) {

    protected fun <S : Any> bindState(
        flow: Flow<S>,
        mapper: (State, S) -> State,
    ): Job = launch {
        flow.collect { slice ->
            reduce { current -> mapper(current, slice) }
        }
    }

    protected fun <E : Any> forwardEffects(
        flow: Flow<E>,
        mapper: (E) -> Effect?,
    ): Job = launch {
        flow.collect { raw ->
            mapper(raw)?.let(::sendEffect)
        }
    }

    protected fun <O : MviStateOwner<S, I, E>, S : Any, I : Any, E : Any> bindStore(
        owner: O,
        stateMapper: (State, S) -> State,
        effectMapper: (E) -> Effect? = { null },
    ) {
        bindState(owner.state, stateMapper)
        forwardEffects(owner.effects, effectMapper)
    }
}

/**
 * 宿主级聚合 MVI 状态，将 [MobileStore]、[MobileFeatureStore]、[SensitiveActionStore] 与深链队列收敛为不可变快照。
 */
data class MobileHostState(
    val mobile: MobileState = MobileState(),
    val features: FeatureState = FeatureState(),
    val sensitive: SensitiveActionState = SensitiveActionState(),
    val pendingLink: PendingTaskLink? = null,
    val routeOwner: Long = 0L,
    val serializedRoute: String = "",
)

sealed interface MobileHostIntent {
    data class Store(val intent: MobileStoreIntent) : MobileHostIntent
    data class Feature(val intent: FeatureStoreIntent) : MobileHostIntent
    data class Sensitive(val intent: SensitiveStoreIntent) : MobileHostIntent
    data class QueueLink(val uri: Uri?) : MobileHostIntent
    data object OpenPendingLink : MobileHostIntent
    data object DismissPendingLink : MobileHostIntent
}

sealed interface MobileHostEffect {
    data class StoreEffect(val effect: MobileStoreEffect) : MobileHostEffect
    data class FeatureEffect(val effect: FeatureStoreEffect) : MobileHostEffect
    data class SensitiveEffect(val effect: SensitiveStoreEffect) : MobileHostEffect
}

/**
 * 生命周期安全的 MVI 单次副作用收集器。
 */
@Composable
internal fun <E : Any> CollectMviEffects(
    effects: Flow<E>,
    onEffect: (suspend (E) -> Unit)?,
) {
    if (onEffect == null) return
    val latestHandler by rememberUpdatedState(onEffect)
    LaunchedEffect(effects) {
        effects.collect { effect ->
            latestHandler(effect)
        }
    }
}

/**
 * 绑定 KMP [MviStateOwner] 的通用 Compose MVI 容器，统一订阅 `state` 与 `effects`。
 */
@Composable
internal fun <S : Any, I : Any, E : Any> MviStoreHost(
    owner: MviStateOwner<S, I, E>,
    onEffect: (suspend (E) -> Unit)? = null,
    content: @Composable (state: S, dispatch: (I) -> Unit) -> Unit,
) {
    val state by owner.state.collectAsStateWithLifecycle()
    CollectMviEffects(owner.effects, onEffect)
    content(state) { intent -> owner.accept(intent) }
}

/**
 * 绑定 Android [MviViewModel] 的通用 Compose MVI 容器。
 */
@Composable
internal fun <S : Any, I : Any, E : Any> MviViewModelHost(
    viewModel: MviViewModel<S, I, E>,
    onEffect: (suspend (E) -> Unit)? = null,
    content: @Composable (state: S, dispatch: (I) -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectMviEffects(viewModel.effects, onEffect)
    content(state) { intent -> viewModel.accept(intent) }
}

/**
 * 跨页面复用的 MVI 顶栏／表单状态提示条（统一处理加载进度条、可关闭横幅通知、主错误与草稿错误）。
 */
@Composable
internal fun MviStatusBanner(
    loading: Boolean,
    notice: String? = null,
    error: String? = null,
    secondaryError: String? = null,
    bannerNotice: Boolean = false,
    onDismissNotice: (() -> Unit)? = null,
) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (notice != null) {
        if (bannerNotice && onDismissNotice != null) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(notice, Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                IconButton(onClick = onDismissNotice) { Icon(Icons.Outlined.Close, "关闭提示") }
            }
        } else {
            Text(notice, color = MaterialTheme.colorScheme.primary)
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    secondaryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

/**
 * 待查看深链提示横幅组件。
 */
@Composable
internal fun PendingLinkBanner(
    pendingLink: PendingTaskLink?,
    enabled: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val link = pendingLink ?: return
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text("有待查看的${link.label}，当前输入仍保留。", style = MaterialTheme.typography.bodyMedium)
        Row {
            androidx.compose.material3.TextButton(onOpen, enabled = enabled) { Text("查看链接") }
            androidx.compose.material3.TextButton(onDismiss) { Text("暂不查看") }
        }
    }
}
