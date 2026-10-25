package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class AgeProviderInfo(val available: Boolean = false,val name: String = "",@SerialName("privacy_url") val privacyUrl: String = "")
@Serializable data class AgeAssuranceInput(@SerialName("client_request_id") val clientRequestId: String,
    @SerialName("task_key") val taskKey: String,@SerialName("task_label") val taskLabel: String,val accepted: Boolean)
@Serializable data class AgeAssuranceStatus(val id: Long,val state: String,@SerialName("task_key") val taskKey: String,
    @SerialName("task_label") val taskLabel: String,val provider: String,@SerialName("launch_url") val launchUrl: String = "",
    @SerialName("expires_at") val expiresAt: String,@SerialName("valid_until") val validUntil: String? = null)
enum class AgeStage { Loading, Overview, Notice, Ready, Pending, Passed, Cancelled, Failed, Unsupported, NotEligible, Expired, TaskReview, ResultUnknown }
data class AgeState(val stage: AgeStage = AgeStage.Loading,val busy: Boolean = false,val provider: AgeProviderInfo? = null,
    val request: AgeAssuranceStatus? = null,val taskKey: String = "",val taskLabel: String = "",val eligibility: Eligibility? = null,val error: String? = null)
enum class AgeIntent { Refresh }

/** 当前账号核验任务；外部回到应用只是返回，不据此设置成年资格或自动提交原任务。 */
class AgeAssuranceStore(private val api: MobileApi,parentScope: CoroutineScope) : MviStore<AgeState,AgeIntent,Unit>(AgeState(),parentScope) {
    private var owner = api.user.value?.id
    private var revision = 0L
    private var work: Job? = null
    private var requestKey = newRequestKey()
    override fun accept(intent: AgeIntent) { if(intent == AgeIntent.Refresh) load() }
    init { scope.launch { api.user.collect { user -> if(owner != user?.id) {
        owner = user?.id; revision++; work?.cancel(); requestKey = newRequestKey(); setState(AgeState())
    } } } }
    private fun run(action: suspend () -> Unit) {
        if(state.value.busy) return
        val current = revision
        setState(state.value.copy(busy = true,error = null))
        work = scope.launch {
            try { action() } catch(e: CancellationException) { throw e } catch(e: Exception) {
                if(current == revision) setState(state.value.copy(error = e.mobileMessage()))
            } finally { if(current == revision) setState(state.value.copy(busy = false)) }
        }
    }
    fun load() = run {
        val info = api.ageProvider()
        val current = api.currentAgeAssurance()
        setState(state.value.copy(provider = info))
        if(current == null) setState(state.value.copy(stage = AgeStage.Overview,request = null,taskKey = "",taskLabel = "")) else receive(current)
    }
    fun explainPurpose() { if(!state.value.busy && state.value.stage == AgeStage.Overview) setState(state.value.copy(stage = AgeStage.Notice)) }
    fun prepare(accepted: Boolean,taskKey: String,taskLabel: String) {
        if(!accepted || state.value.busy || state.value.stage != AgeStage.Notice) return
        setState(state.value.copy(stage = if(state.value.provider?.available == true) AgeStage.Ready else AgeStage.Unsupported,taskKey = taskKey,taskLabel = taskLabel))
    }
    fun begin(accepted: Boolean) {
        if(!accepted || state.value.stage != AgeStage.Ready || state.value.provider?.available != true || state.value.taskKey.isBlank()) return
        run {
            try { receive(api.beginAgeAssurance(AgeAssuranceInput(requestKey,state.value.taskKey,state.value.taskLabel,true))) } catch(e: TimeoutCancellationException) { setState(state.value.copy(stage = AgeStage.ResultUnknown));throw e } catch(e: CancellationException) { throw e } catch(e: Exception) { setState(state.value.copy(stage = if(e is MobileApiException && e.status in listOf(400,401,403,409,422,429,503)) AgeStage.Failed else AgeStage.ResultUnknown));throw e }
        }
    }
    private fun receive(request: AgeAssuranceStatus) {
        val stage = when(request.state) {
            "pending","preparing" -> AgeStage.Pending
            "passed" -> AgeStage.Passed
            "not_eligible" -> AgeStage.NotEligible
            "cancelled" -> AgeStage.Cancelled
            "failed" -> AgeStage.Failed
            "expired","superseded" -> AgeStage.Expired
            else -> AgeStage.ResultUnknown
        }
        setState(state.value.copy(stage = stage,request = request))
    }
    fun query() = run {
        val current = state.value.request ?: api.currentAgeAssurance()
        if(current == null) { setState(state.value.copy(stage = AgeStage.ResultUnknown,error = "暂未查到本次请求；保留原任务，不会自动重新创建核验。"));return@run }
        receive(api.queryAgeAssurance(current.id))
    }
    fun review() = run {
        val eligibility = api.eligibility()
        setState(state.value.copy(eligibility = eligibility,stage = AgeStage.TaskReview))
    }
    fun restart() {
        if(state.value.busy || state.value.stage !in setOf(AgeStage.Cancelled,AgeStage.Failed,AgeStage.Expired)) return
        if(state.value.request?.state in setOf("pending","preparing","unknown")) return
        requestKey = newRequestKey()
        setState(AgeState(stage = AgeStage.Notice,provider = state.value.provider))
    }
}
