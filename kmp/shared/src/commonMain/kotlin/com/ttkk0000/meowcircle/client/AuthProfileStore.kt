package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class AuthProfileState(val owner: Long = 0, val profile: MobileProfile? = null,
    val nickname: String = "", val photo: SelectedPhoto? = null, val photoKey: String = "",
    val loading: Boolean = false, val busy: Boolean = false, val picking: Boolean = false, val unknown: Boolean = false,
    val needsReload: Boolean = false, val error: String? = null) {
    val blocked: Boolean get() = busy || picking
}
enum class AuthProfileIntent { Save, Refresh, Dismiss }

/** Optional setup stays in owner memory, leaving the original feature route/draft untouched. */
class AuthProfileStore(private val api: MobileApi, parentScope: CoroutineScope,
    private val onSaved: (MobileProfile) -> Unit) :
    MviStore<AuthProfileState, AuthProfileIntent, Unit>(AuthProfileState(), parentScope) {
    private var work: Job? = null
    private var revision = 0L
    private var observedOwner = api.user.value?.id
    private var uploaded: Long? = null
    private data class Pending(val input: ProfileEdit, val originalAvatar: String)
    private var pending: Pending? = null

    override fun accept(intent: AuthProfileIntent) { when (intent) {
        AuthProfileIntent.Save -> save()
        AuthProfileIntent.Refresh -> refresh()
        AuthProfileIntent.Dismiss -> dismiss()
    } }
    init { scope.launch { api.user.collect { user ->
        if (observedOwner != user?.id) { observedOwner = user?.id; clear() }
    } } }
    private fun clear() {
        revision++; work?.cancel(); work = null; uploaded = null; pending = null
        setState(AuthProfileState())
    }
    fun dismiss() { if (!state.value.blocked) clear() }
    private fun current(version: Long, owner: Long) = version == revision && owner > 0 &&
        state.value.owner == owner && api.user.value?.id == owner
    fun begin(owner: Long) {
        if (owner <= 0 || api.user.value?.id != owner || state.value.blocked) return
        observedOwner = owner
        clear(); setState(AuthProfileState(owner = owner)); refresh()
    }
    fun editNickname(value: String) {
        val s = state.value
        if (!s.blocked && !s.loading && !s.unknown && api.user.value?.id == s.owner)
            setState(s.copy(nickname = value, error = null))
    }
    fun selectPhoto(photo: SelectedPhoto) {
        val s = state.value
        if (s.busy || s.loading || s.unknown || api.user.value?.id != s.owner) return
        if (photo.mime !in listOf("image/jpeg", "image/png") || photo.bytes.size !in 1..10 * 1024 * 1024) {
            setState(s.copy(error = "请选择10MiB以内的JPEG或PNG照片")); return
        }
        uploaded = null
        setState(s.copy(photo = photo, photoKey = newRequestKey(), error = null))
    }
    fun pickerFailed(message: String) { if (!state.value.busy) setState(state.value.copy(error = message)) }
    fun pickerBusy(value: Boolean) {
        val s = state.value
        if (api.user.value?.id == s.owner && s.picking != value) setState(s.copy(picking = value))
    }
    private fun matches(profile: MobileProfile, expected: Pending): Boolean {
        val input = expected.input
        val avatar = input.avatarMediaId?.let { id ->
            Regex("/media/(\\d+)/content").find(profile.user.avatarUrl.orEmpty())?.groupValues?.getOrNull(1)?.toLongOrNull() == id
        } ?: (profile.user.avatarUrl.orEmpty() == expected.originalAvatar)
        return profile.user.id == state.value.owner && profile.version > input.version &&
            profile.user.nickname == input.nickname && profile.user.bio.orEmpty() == input.bio &&
            profile.theme == input.theme && profile.background == input.background && avatar
    }
    private fun completed(profile: MobileProfile) {
        pending = null; uploaded = null
        setState(state.value.copy(profile = profile, nickname = profile.user.nickname, photo = null,
            photoKey = "", loading = false, busy = false, unknown = false, needsReload = false, error = null))
        onSaved(profile)
    }
    fun refresh() {
        val s = state.value
        if (s.owner <= 0 || s.blocked || s.loading || api.user.value?.id != s.owner) return
        val version = revision
        setState(s.copy(loading = true, error = null))
        work = scope.launch {
            try {
                val actual = api.refreshProfileIdentity()
                if (!current(version, s.owner)) return@launch
                val expected = pending
                if (s.unknown && expected != null) {
                    if (matches(actual, expected)) completed(actual)
                    else setState(state.value.copy(loading = false, error = "尚未确认本次修改结果，请先核对当前资料；不会自动再次保存。"))
                } else setState(state.value.copy(profile = actual, loading = false, needsReload = false,
                    nickname = if (s.profile == null) actual.user.nickname else state.value.nickname, error = null))
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                if (current(version, s.owner)) setState(state.value.copy(loading = false, error = e.mobileMessage()))
            }
        }
    }
    fun save() {
        val s = state.value; val profile = s.profile ?: return
        if (s.blocked || s.loading || s.unknown || s.needsReload || api.user.value?.id != s.owner) return
        val nickname = s.nickname.trim()
        var count = 0; var index = 0
        while (index < nickname.length) {
            if (nickname[index] in '\uD800'..'\uDBFF' && index + 1 < nickname.length && nickname[index + 1] in '\uDC00'..'\uDFFF') index++
            count++; index++
        }
        if (count !in 1..40) { setState(s.copy(error = "昵称需为1至40字")); return }
        if (nickname == profile.user.nickname && s.photo == null) { completed(profile); return }
        val version = revision
        setState(s.copy(busy = true, error = null))
        work = scope.launch {
            var sent = false
            try {
                if (s.photo != null && uploaded == null) {
                    val media = api.upload(s.photo, s.photoKey)
                    if (!current(version, s.owner)) return@launch
                    check(media.id > 0 && media.ownerId == s.owner && media.data.scope == "public") { "头像上传回执尚未确认" }
                    uploaded = media.id
                }
                if (!current(version, s.owner)) return@launch
                val input = ProfileEdit(profile.version, nickname, profile.user.bio.orEmpty(), profile.theme,
                    profile.background, uploaded)
                val expected = Pending(input, profile.user.avatarUrl.orEmpty())
                pending = expected; sent = true
                val actual = api.editProfile(input)
                if (!current(version, s.owner)) return@launch
                check(matches(actual, expected)) { "资料回执与本次修改不一致，请先核对当前资料" }
                completed(actual)
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                if (current(version, s.owner)) {
                    val known = e is MobileApiException && e.status in listOf(400,401,403,404,409,422,429)
                    val unknown = sent && !known
                    if (!unknown) pending = null
                    setState(state.value.copy(busy = false, unknown = unknown,
                        needsReload = e is MobileApiException && e.status == 409,
                        error = if (unknown) "保存结果待核对，输入已保留；请读取当前资料，不会自动重发。" else e.mobileMessage()))
                }
            }
        }
    }
    override fun close() { clear(); super.close() }
}
