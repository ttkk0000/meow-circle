package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

data class AuthRecoveryState(val account: String = "", val contact: String = "", val reason: String = "",
    val busy: Boolean = false, val unknown: Boolean = false, val receipt: RightsReceipt? = null, val error: String? = null)
enum class AuthRecoveryIntent { Submit, Read, Dismiss }

/** Anonymous assistance only. A receipt never authenticates an account or changes its bindings. */
class AuthRecoveryStore(private val api: MobileApi, parentScope: CoroutineScope) :
    MviStore<AuthRecoveryState, AuthRecoveryIntent, Unit>(AuthRecoveryState(), parentScope) {
    private var work: Job? = null
    private var revision = 0L
    private var owner = api.user.value?.id ?: 0L
    private var token: String? = null
    private var requestKey: String? = null

    init { scope.launch { api.user.collect { user ->
        if (owner != (user?.id ?: 0L)) { owner = user?.id ?: 0L; clear() }
    } } }
    override fun accept(intent: AuthRecoveryIntent) { when (intent) {
        AuthRecoveryIntent.Submit -> submit()
        AuthRecoveryIntent.Read -> readReceipt()
        AuthRecoveryIntent.Dismiss -> dismiss()
    } }
    private fun clear() {
        revision++; work?.cancel(); work = null; token = null; requestKey = null
        setState(AuthRecoveryState())
    }
    fun begin() { owner = api.user.value?.id ?: 0L; clear() }
    fun edit(account: String = state.value.account, contact: String = state.value.contact, reason: String = state.value.reason) {
        if (state.value.busy || state.value.unknown || state.value.receipt != null) return
        setState(state.value.copy(account = account, contact = contact, reason = reason, error = null))
    }
    private fun current(version: Long, actor: Long) = revision == version && owner == actor && (api.user.value?.id ?: 0L) == actor
    private fun validReceipt(value: RightsReceipt) = value.id > 0 && value.version > 0 && value.type == "safety"
    private fun received(value: RightsReceipt) {
        check(validReceipt(value)) { "原回执尚未核对，请继续读取" }
        setState(AuthRecoveryState(receipt = value))
    }
    fun submit() {
        val draft = state.value
        if (draft.busy || draft.unknown || draft.receipt != null || (api.user.value?.id ?: 0L) != owner) return
        val account = draft.account.trim(); val contact = draft.contact.trim(); val reason = draft.reason.trim()
        fun count(value: String) = value.count { it.code !in 0xDC00..0xDFFF }
        if (count(account) !in 1..120 || count(contact) !in 1..240 || count(reason) !in 1..2000) {
            setState(draft.copy(error = "请填写原账号、可联系的方式和情况说明（分别最多120、240、2000字）")); return
        }
        val key = newRequestKey(); val actor = owner; val version = ++revision
        requestKey = key
        setState(draft.copy(busy = true, error = null))
        work = scope.launch {
            var sent = false
            try {
                val access = token ?: api.newSafetySession().token
                if (!current(version, actor)) return@launch
                token = access
                // The existing restricted assistance queue carries this request, never a login grant.
                val input = RightsInput(type = "safety", clientRequestId = key,
                    reason = "账号访问恢复协助\n原账号：$account\n可联系的方式：$contact\n情况说明：$reason")
                sent = true
                val result = api.safetySubmitSecure(access, input, authenticated = false)
                if (current(version, actor)) received(result)
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                if (current(version, actor)) {
                    val uncertain = sent && !(e is MobileApiException && e.status in listOf(400, 401, 403, 404, 409, 422, 429))
                    setState(state.value.copy(busy = false, unknown = uncertain,
                        error = if (uncertain) "申请结果待核对，请读取原回执，不要重复提交" else "暂未提交恢复申请，请核对后重试"))
                    if (!uncertain) requestKey = null
                }
            }
        }
    }
    fun readReceipt() {
        val access = token ?: return; val key = requestKey ?: return
        if (state.value.busy) return
        val actor = owner; val version = ++revision
        setState(state.value.copy(busy = true, error = null))
        work = scope.launch {
            try {
                val result = api.findSafetyTicket(key, access)
                if (current(version, actor)) received(result)
            } catch (e: CancellationException) { throw e } catch (_: Exception) {
                if (current(version, actor)) setState(state.value.copy(busy = false, error = "暂未核对到原回执；这不表示申请失败，请稍后继续读取"))
            }
        }
    }
    fun reference(): RestrictedReference? {
        if ((api.user.value?.id ?: 0L) != owner) return null
        val receipt = state.value.receipt ?: return null
        return requestKey?.let { RestrictedReference("safety_ticket", it, receipt.id) }
    }
    fun dismiss() { if (!state.value.busy) clear() }
    override fun close() { clear(); super.close() }
}
