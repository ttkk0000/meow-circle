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
import kotlinx.coroutines.launch

class MobileViewModel(api: MobileApi, drafts: LocalComposeDrafts, workspaces: LocalWorkspaceDrafts, replies: LocalWorkspaceDrafts, private val saved: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    val store = MobileStore(api, ownsApi = false, localDrafts = drafts, localWorkspaces = workspaces, localReplies = replies)
    init {
        // 系统状态只保存资源路由，不保存密码、验证码、材料或临时证明。
        val route = saved.get<String>("mobileRoute")
        if (saved.get<Long>("routeOwner") == (api.user.value?.id ?: 0L) && route != null) restoreRoute(route)
        viewModelScope.launch {
            combine(store.state,store.features.state) { state,features ->
                val feature = features.route
                val destination = when (val d = state.destination) {
                    MobileDestination.Home -> "home:0"
                    is MobileDestination.Pet -> "pets:${d.id}"
                    is MobileDestination.Entry -> "entries:${d.id}"
                    is MobileDestination.Application -> "adoptions:${d.id}"
                    is MobileDestination.Case -> "cases:${d.id}"
                    is MobileDestination.Chat -> "${if(d.group) "groups" else "conversations"}:${d.id}"
                    MobileDestination.Help -> "help:0"
                    MobileDestination.Applications -> "applications:0"
                    MobileDestination.Notifications -> "notifications:0"
                }
                Triple(state.user?.id ?: 0L,state.tab.name + "|" + destination,feature?.let { "${it.page.name}:${it.id}:${it.kind}:${it.peer}" }.orEmpty())
            }.collect { (owner,base,feature) -> saved["routeOwner"] = owner; saved["mobileRoute"] = "$base|$feature" }
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
    /** 白名单路由只读取 ID。无效／失权资源由真实 API 重新校验。 */
    fun openLink(uri: Uri?): Boolean {
        if(store.state.value.busy || store.features.state.value.busy || store.state.value.auth.visible ||
            store.state.value.publicationConfirmation != null || store.features.state.value.confirmation != null ||
            store.features.sensitive.state.value.stage != SensitiveStage.Idle) return false
        if(uri == null || uri.scheme != "mnd" || uri.query != null || uri.fragment != null) return false
        val kind = uri.host ?: return false
        if(uri.pathSegments.size > 1) return false
        val id = uri.pathSegments.firstOrNull()?.toLongOrNull() ?: return false
        if(id <= 0) return false
        baseRoute(kind,id)?.let { store.features.closeRoute(); store.open(it); return true }
        val feature = when(kind) {
            "appointments" -> FeatureRoute(FeaturePage.Appointment,id)
            "offers" -> FeatureRoute(FeaturePage.Offer,id)
            "claims" -> FeatureRoute(FeaturePage.Claim,id)
            "reviews" -> FeatureRoute(FeaturePage.Review,id)
            else -> return false
        }
        store.features.open(feature); return true
    }
    private fun baseRoute(kind: String,id: Long): MobileDestination? = when {
        kind == "home" -> MobileDestination.Home
        kind == "help" -> MobileDestination.Help
        kind == "applications" -> MobileDestination.Applications
        kind == "notifications" -> MobileDestination.Notifications
        id <= 0 -> null
        kind == "pets" -> MobileDestination.Pet(id)
        kind == "entries" -> MobileDestination.Entry(id)
        kind == "adoptions" -> MobileDestination.Application(id)
        kind == "cases" -> MobileDestination.Case(id)
        kind == "groups" -> MobileDestination.Chat(id,true)
        kind == "conversations" -> MobileDestination.Chat(id)
        else -> null
    }
    override fun onCleared() { store.close() }
    class Factory(private val api: MobileApi, private val drafts: LocalComposeDrafts, private val workspaces: LocalWorkspaceDrafts, private val replies: LocalWorkspaceDrafts) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>,extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(MobileViewModel::class.java))
            return MobileViewModel(api,drafts,workspaces,replies,extras.createSavedStateHandle()) as T
        }
    }
}
