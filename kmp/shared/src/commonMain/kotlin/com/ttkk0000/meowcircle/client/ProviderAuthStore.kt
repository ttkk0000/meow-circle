package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/** 平台只返回官方授权结果；null代表用户主动取消，不能自行生成身份或成功会话。 */
fun interface ProviderLauncher {
    suspend fun authorize(provider: String, challenge: ProviderChallenge): ProviderCredential?
}

enum class ProviderAuthStage { Idle, Authorizing, Checking, AccountChoice, CreateReview, ExistingLogin, LinkReview, Verifying, LinkReady, Creating, Linking, Cancelled, Unavailable, Expired, Conflict, Failed, ResultUnknown }
data class ProviderAuthState(val stage: ProviderAuthStage = ProviderAuthStage.Idle, val provider: String = "",
    val flow: Long = 0, val account: MobileUser? = null, val error: String? = null) {
    val busy: Boolean get() = stage in setOf(ProviderAuthStage.Authorizing, ProviderAuthStage.Checking, ProviderAuthStage.Verifying, ProviderAuthStage.Creating, ProviderAuthStage.Linking)
}
enum class ProviderAuthIntent { Dismiss }

/** 短时授权凭据仅在本Owner私有内存；不进入UI状态、保险库、日志或草稿。 */
class ProviderAuthStore(private val api: MobileApi, parentScope: CoroutineScope, private val onLogin: (LoginSession) -> Unit) :
    MviStore<ProviderAuthState, ProviderAuthIntent, Unit>(ProviderAuthState(), parentScope) {
    private var work: Job? = null
    private var revision = 0L
    private var owner = api.user.value?.id
    private var expectingLogin = false
    private var resolution: String? = null
    private var challenge: String? = null
    private var grant: String? = null
    private var linkScope: PurposeScope? = null

    override fun accept(intent: ProviderAuthIntent) { if (intent == ProviderAuthIntent.Dismiss) dismiss() }
    init { scope.launch { api.user.collect { user ->
        if (owner != user?.id) {
            owner = user?.id
            if (!expectingLogin) clear()
        }
    } } }

    private fun secretsClear() { resolution = null; challenge = null; grant = null; linkScope = null }
    private fun clear() { work?.cancel(); revision++; secretsClear(); setState(ProviderAuthState(flow = revision)) }
    val canChoose: Boolean get() = resolution != null
    fun unavailable(provider: String) {
        if(state.value.busy) return
        clear()
        setState(ProviderAuthState(ProviderAuthStage.Unavailable,provider,revision,error = "当前暂无法使用此官方授权，请选择其他登录方式。"))
    }
    fun dismiss() {
        if (state.value.busy && state.value.stage != ProviderAuthStage.Authorizing) return
        val original = challenge
        clear()
        if (original != null) scope.launch { runCatching { withTimeout(5000) { api.finishProvider(ProviderFinish(original, ProviderCredential(), cancelled = true)) } } }
    }

    private suspend fun session(block: suspend () -> LoginSession): LoginSession {
        expectingLogin = true
        try {
            val result = block()
            if (api.user.value?.id != result.user.id) throw MobileApiException(401, "session changed")
            owner = result.user.id
            return result
        } finally { expectingLogin = false }
    }
    private fun complete(result: LoginSession) { secretsClear(); setState(ProviderAuthState(flow = revision)); onLogin(result) }
    private fun perform(stage: ProviderAuthStage, uncertain: Boolean = false, block: suspend () -> Unit) {
        if (state.value.busy) return
        val current = revision
        setState(state.value.copy(stage = stage, error = null))
        work = scope.launch {
            try { block() } catch (failure: TimeoutCancellationException) {
                if (current == revision) {
                    grant = null
                    setState(state.value.copy(stage = if (uncertain) ProviderAuthStage.ResultUnknown else ProviderAuthStage.Expired,
                        error = if (uncertain) "本次创建或关联结果待核对，不会自动重发。请重新官方授权核对当前账号。" else "官方授权已超时，请重新发起；当前任务仍保留。"))
                }
            } catch (failure: CancellationException) { throw failure } catch (failure: Exception) {
                if (current == revision) {
                    grant = null
                    val unknown = uncertain && !(failure is MobileApiException && failure.status in listOf(400,401,403,404,409,422,429))
                    val expired = failure is MobileApiException && failure.status == 401 && stage != ProviderAuthStage.Verifying
                    val conflict = failure is MobileApiException && failure.code == "identity_in_use"
                    if(expired) secretsClear()
                    if(conflict) secretsClear()
                    setState(state.value.copy(stage = if (unknown) ProviderAuthStage.ResultUnknown else if(expired) ProviderAuthStage.Expired else if(conflict) ProviderAuthStage.Conflict else ProviderAuthStage.Failed,
                        error = if (unknown) "本次授权处理结果待核对，请重新发起官方授权核对账号，不会自动重发创建或关联。" else failure.mobileMessage()))
                }
            }
        }
    }

    fun start(provider: String, launcher: ProviderLauncher) {
        if (state.value.busy || provider !in listOf("wechat", "qq", "apple")) return
        clear()
        setState(ProviderAuthState(provider = provider, flow = revision))
        perform(ProviderAuthStage.Authorizing) {
            val originalOwner = api.user.value?.id
            val ticket = api.startProvider(ProviderStart(provider))
            challenge = ticket.state
            val credential = withTimeout(5 * 60_000L) { launcher.authorize(provider, ticket) }
            if (api.user.value?.id != originalOwner) throw MobileApiException(401, "session changed")
            if (credential == null) {
                api.finishProvider(ProviderFinish(ticket.state, ProviderCredential(), cancelled = true))
                secretsClear(); setState(state.value.copy(stage = ProviderAuthStage.Cancelled)); return@perform
            }
            setState(state.value.copy(stage = ProviderAuthStage.Checking))
            expectingLogin = true
            val result = try { api.finishProvider(ProviderFinish(ticket.state, credential)) } finally { owner = api.user.value?.id; expectingLogin = false }
            challenge = null
            when {
                result.login != null -> complete(result.login)
                result.needsAccountChoice && result.resolutionToken.isNotBlank() -> {
                    resolution = result.resolutionToken
                    setState(state.value.copy(stage = ProviderAuthStage.AccountChoice, account = api.user.value))
                }
                else -> throw MobileApiException(409, "provider result unavailable")
            }
        }
    }
    fun chooseExisting() {
        if (state.value.stage != ProviderAuthStage.AccountChoice) return
        setState(state.value.copy(stage = if (api.user.value == null) ProviderAuthStage.ExistingLogin else ProviderAuthStage.LinkReview))
    }
    fun chooseCreate() {
        if (state.value.stage == ProviderAuthStage.AccountChoice && resolution != null && api.user.value == null)
            setState(state.value.copy(stage = ProviderAuthStage.CreateReview))
    }
    fun loginExisting(identifier: String, password: String, code: String = "", channel: String = "password") {
        if (state.value.stage != ProviderAuthStage.ExistingLogin || resolution == null || identifier.isBlank()) return
        perform(ProviderAuthStage.Verifying) {
            val result = session { when(channel) {
                "email" -> api.emailLogin(EmailAuthInput(identifier.trim(),code))
                "phone" -> api.phoneLogin(PhoneLoginInput(identifier.trim(),code))
                else -> api.password(identifier.trim(), password)
            } }
            // 此处只验证原账号，不提前关联第三方身份。
            setState(state.value.copy(stage = ProviderAuthStage.LinkReview, account = result.user))
        }
    }
    fun returnToChoice() {
        if (state.value.busy || resolution == null || state.value.stage == ProviderAuthStage.ResultUnknown) return
        grant = null; linkScope = null
        setState(state.value.copy(stage = ProviderAuthStage.AccountChoice, account = api.user.value, error = null))
    }
    fun verifyLink(password: String, code: String) {
        val token = resolution ?: return
        if (state.value.stage != ProviderAuthStage.LinkReview || api.user.value?.id != state.value.account?.id) return
        perform(ProviderAuthStage.Verifying) {
            val scope = api.providerLinkScope(token)
            val verified = api.reauthenticate(ReauthInput(scope, password, code))
            linkScope = scope; grant = verified.reauthToken
            setState(state.value.copy(stage = ProviderAuthStage.LinkReady))
        }
    }
    fun confirmLink(confirmed: Boolean) {
        val token = resolution ?: return
        val proof = grant ?: return
        if (!confirmed || state.value.stage != ProviderAuthStage.LinkReady || linkScope == null || api.user.value?.id != state.value.account?.id) return
        perform(ProviderAuthStage.Linking, uncertain = true) {
            grant = null
            complete(session { api.resolveProvider(ProviderResolve(token, reauthToken = proof)) })
        }
    }
    fun create(acceptedTerms: Boolean) {
        val token = resolution ?: return
        if (!acceptedTerms || state.value.stage != ProviderAuthStage.CreateReview || api.user.value != null) return
        perform(ProviderAuthStage.Creating, uncertain = true) {
            complete(session { api.resolveProvider(ProviderResolve(token, create = true, acceptTerms = true)) })
        }
    }
    override fun close() { secretsClear(); work?.cancel(); super.close() }
}
