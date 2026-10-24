package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Typed immutable review snapshots. A changed snapshot always requires new scope verification. */
sealed interface SensitiveIntent {
    data class Account(val action: String, val input: AccountInput) : SensitiveIntent
    data class Guardian(val id: Long, val input: GuardianDecision, val reviewSummary: String = "") : SensitiveIntent
    data class Contract(val id: Long, val input: ContractInput, val reviewSummary: String = "") : SensitiveIntent
    data class DataRequest(val input: DataRequestInput) : SensitiveIntent
    data class Rights(val token: String, val input: RightsInput, val authenticated: Boolean = false) : SensitiveIntent
}

enum class SensitiveStage { Idle, LoadingScope, Review, Verifying, ReadyToSubmit, Submitting, ResultUnknown, Complete, Failed }
data class SensitiveActionState(val stage: SensitiveStage = SensitiveStage.Idle, val intent: SensitiveIntent? = null,
    val scope: PurposeScope? = null, val error: String? = null, val clientRequestId: String? = null,
    val resultId: Long? = null, val resultKind: String? = null)

/** Android and iOS own the UI; this owner supplies the same confirmation and recovery rules. */
class SensitiveActionStore(private val api: MobileApi, parentScope: CoroutineScope) :
    MviStore<SensitiveActionState, SensitiveStoreIntent, SensitiveStoreEffect>(SensitiveActionState(), parentScope) {
    private var work: Job? = null
    private var grant: String? = null // memory only, never included in state or persisted by this owner
    private var revision = 0L
    private var owner = api.user.value?.id
    private var expectedSignOut = false

    override fun accept(intent: SensitiveStoreIntent) {
        when (intent) {
            is SensitiveStoreIntent.Prepare -> prepare(intent.intent)
            is SensitiveStoreIntent.Verify -> verify(intent.password, intent.code)
            is SensitiveStoreIntent.Confirm -> confirm(intent.userConfirmed)
            SensitiveStoreIntent.Reconcile -> reconcile()
            SensitiveStoreIntent.Dismiss -> dismiss()
        }
    }

    init {
        scope.launch { api.user.collect { user ->
            if (owner != user?.id) {
                owner = user?.id
                if (user == null && expectedSignOut) {
                    expectedSignOut = false
                    grant = null
                    // The API clears this session after a committed account change or closure.
                    // Retain only a minimal progress/result reference across that expected logout.
                    if (mutable.value.stage == SensitiveStage.Submitting) {
                        mutable.value = SensitiveActionState(SensitiveStage.Submitting,clientRequestId = mutable.value.clientRequestId)
                    }
                    return@collect
                }
                expectedSignOut = false
                // API submission methods reconcile their own sent records when cancellation occurs.
                work?.cancel(); revision++; grant = null
                mutable.value = SensitiveActionState()
            }
        } }
    }

    fun prepare(intent: SensitiveIntent) {
        if (mutable.value.stage in listOf(SensitiveStage.Submitting,SensitiveStage.ResultUnknown)) return
        expectedSignOut = false
        work?.cancel(); grant = null
        val currentRevision = ++revision
        mutable.value = SensitiveActionState(SensitiveStage.LoadingScope)
        work = scope.launch {
            try {
                val normalized = normalize(intent)
                val purpose = when (normalized) {
                    is SensitiveIntent.Account -> api.accountScope(normalized.action,normalized.input)
                    is SensitiveIntent.Guardian -> if (normalized.input.consent) api.guardianScope(normalized.id) else null
                    is SensitiveIntent.Contract -> api.contractScope(normalized.id,normalized.input)
                    is SensitiveIntent.DataRequest -> if (normalized.input.type == "matters_assistance") null else api.dataRequestScope(normalized.input)
                    is SensitiveIntent.Rights -> if (normalized.authenticated && normalized.input.type in listOf("rights_notice","counterstatement")) api.rightsScope(normalized.input) else null
                }
                if (revision == currentRevision) mutable.value = SensitiveActionState(SensitiveStage.Review,normalized,purpose,clientRequestId = normalized.key())
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (revision == currentRevision) mutable.value = SensitiveActionState(SensitiveStage.Failed,error = e.mobileMessage()) }
        }
    }

    fun verify(password: String = "",code: String = "") {
        val current = mutable.value
        val purpose = current.scope ?: return
        if (current.stage !in listOf(SensitiveStage.Review,SensitiveStage.Failed)) return
        val currentRevision = revision
        grant = null
        mutable.value = current.copy(stage = SensitiveStage.Verifying,error = null)
        work = scope.launch {
            try {
                val result = api.reauthenticate(ReauthInput(purpose,password,code))
                if (revision == currentRevision) {
                    grant = result.reauthToken
                    mutable.value = current.copy(stage = SensitiveStage.ReadyToSubmit)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (revision == currentRevision) mutable.value = current.copy(stage = SensitiveStage.Failed,error = e.mobileMessage()) }
        }
    }

    /** A successful reauthentication never calls this automatically. */
    fun confirm(userConfirmed: Boolean) {
        if (!userConfirmed) return
        val current = mutable.value
        val intent = current.intent ?: return
        if (current.stage !in listOf(SensitiveStage.Review,SensitiveStage.ReadyToSubmit)) return
        if (current.scope != null && grant == null) {
            mutable.value = current.copy(error = "请先完成本次用途验证")
            return
        }
        val proof = grant.orEmpty()
        grant = null
        val currentRevision = revision
        val submittedOwner = owner
        expectedSignOut = intent is SensitiveIntent.Account || (intent is SensitiveIntent.DataRequest && intent.input.type == "close_account")
        mutable.value = current.copy(stage = SensitiveStage.Submitting,error = null)
        work = scope.launch {
            try {
                val result: Pair<String,Long?> = when (intent) {
                    is SensitiveIntent.Account -> { api.changeAccount(intent.action,intent.input.copy(reauthToken = proof)); "accounts" to null }
                    is SensitiveIntent.Guardian -> "guardians" to api.guardianDecision(intent.id,intent.input.copy(reauthToken = proof)).id
                    is SensitiveIntent.Contract -> "adoptions" to api.contract(intent.id,"accept",intent.input.copy(reauthToken = proof)).id
                    is SensitiveIntent.DataRequest -> "data_requests" to api.submitDataRequest(intent.input.copy(reauthToken = proof)).id
                    is SensitiveIntent.Rights -> "rights_cases" to api.submitSafetyTicket(intent.token,intent.input.copy(reauthToken = proof,confirmed = true),intent.authenticated).id
                }
                if (revision == currentRevision) {
                    // Drop credentials and private form data after confirmed success.
                    mutable.value = SensitiveActionState(SensitiveStage.Complete,clientRequestId = intent.key(),resultId = result.second,resultKind = result.first)
                    sendEffect(SensitiveStoreEffect.Completed(intent.key(),result.first,result.second))
                }
            } catch (e: CancellationException) { throw e }
            catch (e: OperationResultUnknown) {
                expectedSignOut = false
                if (revision == currentRevision) mutable.value = if (owner == submittedOwner) current.copy(stage = SensitiveStage.ResultUnknown,error = e.mobileMessage())
                    else SensitiveActionState(SensitiveStage.ResultUnknown,error = e.mobileMessage(),clientRequestId = intent.key())
            } catch (e: Exception) {
                expectedSignOut = false
                if (revision == currentRevision) mutable.value = if (owner == submittedOwner) current.copy(stage = SensitiveStage.Failed,error = e.mobileMessage())
                    else SensitiveActionState(SensitiveStage.Failed,error = e.mobileMessage(),clientRequestId = intent.key())
            }
        }
    }

    fun reconcile() {
        val current = mutable.value
        val intent = current.intent ?: return
        if (current.stage != SensitiveStage.ResultUnknown) return
        val currentRevision = revision
        work = scope.launch {
            try {
                val result = when (intent) {
                    is SensitiveIntent.DataRequest -> { val receipt = api.findDataRequest(intent.key(),intent.input.queryToken); "data_requests" to receipt.id }
                    is SensitiveIntent.Rights -> { val receipt = api.findSafetyTicket(intent.key(),intent.token); "rights_cases" to receipt.id }
                    else -> { val receipt = api.reconcileOperation(intent.key()); receipt.resultKind to receipt.resultId }
                }
                if (revision == currentRevision) mutable.value = SensitiveActionState(SensitiveStage.Complete,clientRequestId = intent.key(),resultId = result.second,resultKind = result.first)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // 404, timeout and temporary service errors retain the original key and unknown result.
                if (revision == currentRevision) mutable.value = current.copy(error = if (e is MobileApiException && e.status == 404) "暂未查到原请求，结果仍待核对" else e.mobileMessage())
            }
        }
    }

    fun dismiss() {
        if (mutable.value.stage == SensitiveStage.Submitting) return
        work?.cancel(); revision++; grant = null; expectedSignOut = false; mutable.value = SensitiveActionState()
    }
    override fun close() { grant = null; work?.cancel(); super.close() }

    private suspend fun normalize(intent: SensitiveIntent): SensitiveIntent = when (intent) {
        is SensitiveIntent.Account -> intent.copy(input = intent.input.copy(clientRequestId = intent.input.clientRequestId.ifEmpty(::newRequestKey),reauthToken = ""))
        is SensitiveIntent.Guardian -> intent.copy(input = intent.input.copy(clientRequestId = intent.input.clientRequestId.ifEmpty(::newRequestKey),reauthToken = ""))
        is SensitiveIntent.Contract -> intent.copy(input = intent.input.copy(clientRequestId = intent.input.clientRequestId.ifEmpty(::newRequestKey),reauthToken = ""))
        is SensitiveIntent.DataRequest -> intent.copy(input = intent.input.copy(clientRequestId = intent.input.clientRequestId.ifEmpty(::newRequestKey),
            queryToken = intent.input.queryToken.ifEmpty { api.newDataRequestCapability() },reauthToken = ""))
        is SensitiveIntent.Rights -> intent.copy(input = intent.input.copy(clientRequestId = intent.input.clientRequestId.ifEmpty(::newRequestKey),reauthToken = "",confirmed = false))
    }
}

private fun SensitiveIntent.key(): String = when (this) {
    is SensitiveIntent.Account -> input.clientRequestId
    is SensitiveIntent.Guardian -> input.clientRequestId
    is SensitiveIntent.Contract -> input.clientRequestId
    is SensitiveIntent.DataRequest -> input.clientRequestId
    is SensitiveIntent.Rights -> input.clientRequestId
}
