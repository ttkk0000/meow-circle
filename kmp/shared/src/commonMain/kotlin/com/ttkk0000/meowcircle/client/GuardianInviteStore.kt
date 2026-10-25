package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class GuardianInviteInfo(val available: Boolean = false,@SerialName("policy_version") val policyVersion: String = "",
    @SerialName("policy_label") val policyLabel: String = "",@SerialName("policy_key") val policyKey: String = "")
@Serializable data class GuardianInviteStart(@SerialName("client_request_id") val clientRequestId: String,@SerialName("policy_version") val policyVersion: String,val accepted: Boolean)
@Serializable data class GuardianInviteClaim(@SerialName("client_request_id") val clientRequestId: String,val token: String,@SerialName("policy_version") val policyVersion: String,val accepted: Boolean)
@Serializable data class GuardianInviteStatus(val id: Long,val version: Long,val state: String,@SerialName("policy_version") val policyVersion: String,
    @SerialName("expires_at") val expiresAt: String,@SerialName("guardian_record_id") val guardianRecordId: Long = 0,@SerialName("share_url") val shareUrl: String = "",val mine: Boolean = false,@SerialName("can_cancel") val canCancel: Boolean = false)
@Serializable data class GuardianInvitePage(val items: List<GuardianInviteStatus> = emptyList(),@SerialName("next_cursor") val nextCursor: Long = 0)
data class GuardianInviteState(val info: GuardianInviteInfo? = null,val items: List<GuardianInviteStatus> = emptyList(),val next: Long = 0,
    val current: GuardianInviteStatus? = null,val busy: Boolean = false,val loaded: Boolean = false,val unknown: Boolean = false,val error: String? = null)
enum class GuardianInviteIntent { Refresh }

/** 邀请链接只在当前账号内存，不进入草稿；领取后只等待关系核对，不产生监护授权。 */
class GuardianInviteStore(private val api: MobileApi,parentScope: CoroutineScope) : MviStore<GuardianInviteState,GuardianInviteIntent,Unit>(GuardianInviteState(),parentScope) {
    private var owner = api.user.value?.id
    private var revision = 0L
    private var work: Job? = null
    private var originalKey: String? = null
    override fun accept(intent: GuardianInviteIntent) { load() }
    init { scope.launch { api.user.collect { user -> if(owner != user?.id) {
        owner=user?.id;revision++;work?.cancel();originalKey=null;setState(GuardianInviteState())
    } } } }
    private fun run(block: suspend () -> Unit) {
        if(state.value.busy) return
        val current=revision
        setState(state.value.copy(busy=true,error=null))
        work=scope.launch {
            try {block()} catch(e: CancellationException){throw e} catch(e: Exception){if(current==revision)setState(state.value.copy(error=e.mobileMessage()))} finally {if(current==revision)setState(state.value.copy(busy=false))}
        }
    }
    fun load(more: Boolean=false)=run {
        val info=api.guardianInviteInfo()
        val page=api.guardianInvites(if(more)state.value.next else 0)
        setState(state.value.copy(info=info,items=if(more)(state.value.items+page.items).distinctBy{it.id} else page.items,next=page.nextCursor,loaded=true))
    }
    private fun receive(value: GuardianInviteStatus) { originalKey=null;setState(state.value.copy(current=value,items=listOf(value)+state.value.items.filter{it.id!=value.id},unknown=false)) }
    private fun mutate(block: suspend (String) -> GuardianInviteStatus) {
        if(state.value.unknown || state.value.busy) return
        val key=newRequestKey();originalKey=key
        run {
            try {receive(block(key))} catch(e: TimeoutCancellationException){setState(state.value.copy(unknown=true));throw e} catch(e: CancellationException){throw e} catch(e: Exception){val known=e is MobileApiException && e.status in listOf(400,401,403,404,409,422,429);if(known)originalKey=null;setState(state.value.copy(unknown=!known));throw e}
        }
    }
    fun create(accepted: Boolean) {
        val info=state.value.info ?: return
        if(!accepted || !info.available) return
        mutate {api.createGuardianInvite(GuardianInviteStart(it,info.policyVersion,true))}
    }
    fun claim(token: String,accepted: Boolean) {
        val info=state.value.info ?: return
        if(!accepted || !info.available || !Regex("^[A-Za-z0-9_-]{43}$").matches(token))return
        mutate {api.claimGuardianInvite(GuardianInviteClaim(it,token,info.policyVersion,true))}
    }
    fun cancel(value: GuardianInviteStatus,confirmed: Boolean) {if(confirmed && value.canCancel)mutate{api.cancelGuardianInvite(value.id,VersionedAction(value.version,clientRequestId=it))} }
    fun reconcile()=run {
        val key=originalKey
        if(key==null){val page=api.guardianInvites();setState(state.value.copy(items=page.items,next=page.nextCursor));return@run}
        val receipt=api.operation(key)
        if(receipt.resultKind!="auth_challenges" || !receipt.command.startsWith("guardian.invite."))throw MobileApiException(409,"different original operation")
        receive(api.guardianInvite(receipt.resultId))
    }
}

suspend fun MobileApi.guardianInviteInfo(): GuardianInviteInfo=get("guardians/invites/config")
suspend fun MobileApi.guardianInvites(cursor: Long=0): GuardianInvitePage=get("guardians/invites",mapOf("cursor" to "$cursor"))
suspend fun MobileApi.guardianInvite(id: Long): GuardianInviteStatus=get("guardians/invites/$id")
suspend fun MobileApi.createGuardianInvite(input: GuardianInviteStart): GuardianInviteStatus=post("guardians/invites",input)
suspend fun MobileApi.claimGuardianInvite(input: GuardianInviteClaim): GuardianInviteStatus=post("guardians/invites/claim",input)
suspend fun MobileApi.cancelGuardianInvite(id: Long,input: VersionedAction): GuardianInviteStatus=post("guardians/invites/$id/cancel",input)
