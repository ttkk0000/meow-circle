package com.ttkk0000.meowcircle.kmpapp.mobile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ttkk0000.meowcircle.client.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ttkk0000.meowcircle.kmpapp.BuildConfig

typealias PendingTaskLink = com.ttkk0000.meowcircle.clientui.PendingTaskLink

class MobileViewModel(api: MobileApi, drafts: LocalComposeDrafts, workspaces: LocalWorkspaceDrafts, replies: LocalWorkspaceDrafts, private val saved: SavedStateHandle = SavedStateHandle()) : MobileMviViewModel<MobileHostState, MobileHostIntent, MobileHostEffect>(MobileHostState()) {
    private val linkKinds = taskLinkKinds
    val store = MobileStore(api, ownsApi = false, localDrafts = drafts, localWorkspaces = workspaces, localReplies = replies)
    val uiSession = AndroidUiSessionState()
    private val queuedLink = MutableStateFlow<PendingTaskLink?>(null)
    val pendingLink = queuedLink.asStateFlow()
    init {
        bindStore(store,{ current,mobile -> current.copy(mobile = mobile) },{ MobileHostEffect.StoreEffect(it) })
        bindStore(store.features,{ current,features -> current.copy(features = features) },{ MobileHostEffect.FeatureEffect(it) })
        bindStore(store.features.sensitive,{ current,sensitive -> current.copy(sensitive = sensitive) },{ MobileHostEffect.SensitiveEffect(it) })
        bindState(queuedLink) { current,link -> current.copy(pendingLink = link) }
        // 系统状态只保存资源路由，不保存密码、验证码、材料或临时证明。
        val route = saved.get<String>("mobileRoute")
        if (saved.get<Long>("routeOwner") == (api.user.value?.id ?: 0L) && route != null) restoreRoute(route)
        saved.get<String>("pendingTaskLink")?.split(':')?.takeIf { it.size == 3 }?.let { parts ->
            val owner = parts[2].toLongOrNull(); val id = parts[1].toLongOrNull()
            if(owner != null && id != null && id > 0 && id.toString() == parts[1] && (owner == 0L && parts[0] in publicTaskLinkKinds || owner > 0L && owner == api.user.value?.id) && parts[0] in linkKinds)
                queuedLink.value = PendingTaskLink(parts[0],id,owner)
        }
        viewModelScope.launch {
            combine(store.state,store.features.state) { state,features ->
                val feature = features.route
                val destination = when (val d = state.destination) {
                    MobileDestination.Home -> "home:0"
                    is MobileDestination.Pet -> "${if(d.discussion) "pet-comments" else "pets"}:${d.id}"
                    is MobileDestination.Entry -> "entries:${d.id}"
                    is MobileDestination.Application -> "adoptions:${d.id}"
                    is MobileDestination.Case -> "cases:${d.id}"
                    is MobileDestination.Chat -> "${if(d.group) "groups" else "conversations"}:${d.id}"
                    MobileDestination.Help -> "help:0"
                    MobileDestination.Applications -> "applications:0"
                    MobileDestination.Notifications -> "notifications:0"
                }
                Triple(state.user?.id ?: 0L,state.tab.name + "|" + destination,feature?.let { "${it.page.name}:${it.id}:${it.kind}:${it.peer}" }.orEmpty())
            }.collect { (owner,base,feature) ->
                val serialized = "$base|$feature"
                saved["routeOwner"] = owner; saved["mobileRoute"] = serialized
                reduce { it.copy(routeOwner = owner,serializedRoute = serialized) }
                if(queuedLink.value?.owner?.let { it != 0L && it != owner } == true) dismissPendingLink()
            }
        }
    }
    override fun accept(intent: MobileHostIntent) {
        when (intent) {
            is MobileHostIntent.Store -> store.accept(intent.intent)
            is MobileHostIntent.Feature -> store.features.accept(intent.intent)
            is MobileHostIntent.Sensitive -> store.features.sensitive.accept(intent.intent)
            is MobileHostIntent.QueueLink -> openLink(intent.uri)
            MobileHostIntent.OpenPendingLink -> openPendingLink()
            MobileHostIntent.DismissPendingLink -> dismissPendingLink()
        }
    }
    private fun restoreRoute(raw: String) {
        runCatching {
            val parts = raw.split('|'); if(parts.size != 3) return
            store.select(MobileTab.valueOf(parts[0]))
            val base = parts[1].split(':'); val id = base.getOrNull(1)?.toLongOrNull() ?: 0
            baseRoute(base.first(),id)?.let(store::open)
            val feature = parts[2].split(':')
            if(feature.size == 4) store.features.open(FeatureRoute(FeaturePage.valueOf(feature[0]),feature[1].toLong(),feature[2],feature[3].toLong()))
        }
    }
    private fun linkLocked(): Boolean = !store.canOpenNotification()
    /** Queue only an allowlisted object, never external credentials or a write action. */
    fun openLink(uri: Uri?): Boolean {
        val target = parseTaskLink(uri?.toString() ?: return false, BuildConfig.API_BASE_URL) ?: return false
        val owner = if (target.isPublic) 0L else store.state.value.user?.id ?: return false
        queuedLink.value = PendingTaskLink(target.kind,target.id,owner)
        savePendingLink()
        val editing = store.state.value.applying || store.state.value.tab == MobileTab.Publish || store.features.state.value.route != null
        if(!linkLocked() && !editing && store.state.value.destination == MobileDestination.Home) openPendingLink()
        return true
    }
    private fun savePendingLink() { saved["pendingTaskLink"] = queuedLink.value?.let { "${it.kind}:${it.id}:${it.owner}" } }
    fun dismissPendingLink() { queuedLink.value = null; saved.remove<String>("pendingTaskLink") }
    fun openPendingLink() {
        val link = queuedLink.value ?: return
        if(linkLocked()) return
        if(link.owner != 0L && link.owner != store.state.value.user?.id) { dismissPendingLink(); return }
        baseRoute(link.kind,link.id)?.let { store.features.closeRoute(); store.open(it); dismissPendingLink(); return }
        val feature = taskLinkFeature(TaskLinkTarget(link.kind,link.id)) ?: run { dismissPendingLink(); return }
        store.features.open(feature); dismissPendingLink()
    }
    private fun baseRoute(kind: String,id: Long): MobileDestination? = when {
        kind == "home" -> MobileDestination.Home
        kind == "help" -> MobileDestination.Help
        kind == "applications" -> MobileDestination.Applications
        kind == "notifications" -> MobileDestination.Notifications
        id <= 0 -> null
        kind == "pets" -> MobileDestination.Pet(id)
        kind == "pet-comments" -> MobileDestination.Pet(id,true)
        kind == "entries" -> MobileDestination.Entry(id)
        kind == "adoptions" -> MobileDestination.Application(id)
        kind == "cases" -> MobileDestination.Case(id)
        kind == "groups" -> MobileDestination.Chat(id,true)
        kind == "conversations" -> MobileDestination.Chat(id)
        else -> null
    }
    override fun onCleared() { uiSession.clear(); store.close(); super.onCleared() }
    class Factory(private val api: MobileApi, private val drafts: LocalComposeDrafts, private val workspaces: LocalWorkspaceDrafts, private val replies: LocalWorkspaceDrafts) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>,extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(MobileViewModel::class.java))
            return MobileViewModel(api,drafts,workspaces,replies,extras.createSavedStateHandle()) as T
        }
    }
}
