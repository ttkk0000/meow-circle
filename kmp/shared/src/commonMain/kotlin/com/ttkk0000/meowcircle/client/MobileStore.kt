package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class MobileTab(val label: String) { Community("社区"), Adoption("领养"), Publish("发布"), Messages("消息"), Profile("我的") }

sealed interface MobileDestination {
    data object Home : MobileDestination
    data class Pet(val id: Long,val discussion: Boolean = false) : MobileDestination
    data class Entry(val id: Long) : MobileDestination
    data class Chat(val id: Long, val group: Boolean = false) : MobileDestination
    data class Case(val id: Long) : MobileDestination
    data object Help : MobileDestination
    data object Applications : MobileDestination
    data class Application(val id: Long) : MobileDestination
    data object Notifications : MobileDestination
}

data class PageState<T>(val items: List<MobileRecord<T>> = emptyList(), val next: Long = 0, val loading: Boolean = false,
    val loaded: Boolean = false, val error: String? = null, val appending: Boolean = false,
    val failure: ClientReadFailure? = null, val readAt: String? = null) {
    val refreshing: Boolean get() = loading && loaded && !appending
    val canLoadMore: Boolean get() = loaded && next != 0L && !loading && (error == null || appending)
    val empty: Boolean get() = loaded && items.isEmpty() && next == 0L && !loading && error == null
    val exhausted: Boolean get() = loaded && items.isNotEmpty() && next == 0L && !loading && error == null

    fun starting(more: Boolean): PageState<T> = copy(loading = true, error = null, failure = null, appending = more)
    fun failed(message: String, clear: Boolean = false, failure: ClientReadFailure? = null): PageState<T> =
        if (clear) PageState(error = message, failure = failure) else copy(loading = false, error = message, failure = failure)
    fun completed(result: MobilePage<T>, more: Boolean): PageState<T> {
        check(result.nextCursor == 0L || result.nextCursor > if (more) next else 0L) { "分页游标未前进" }
        return PageState(((if (more) items else emptyList()) + result.items).distinctBy { it.id }, result.nextCursor, loaded = true, readAt = clientReadTimestamp())
    }
}

data class AuthForm(val visible: Boolean = false, val username: String = "", val password: String = "", val error: String? = null,val errorCode: String = "")

data class AdoptionCommand(val record: MobileRecord<AdoptionBody>, val action: String, val label: String, val note: String)
data class PendingNotification(val id: Long, val owner: Long)
fun canonicalNotificationId(raw: String): Long? = raw.takeIf { it.length in 1..19 }?.toLongOrNull()?.takeIf { it > 0 && it.toString() == raw }

data class MobileState(
    val restoring: Boolean = true, val user: MobileUser? = null, val tab: MobileTab = MobileTab.Community,
    val destination: MobileDestination = MobileDestination.Home, val auth: AuthForm = AuthForm(),
    val capabilities: LoginCapabilities = LoginCapabilities(), val capabilityError: String? = null,
    val eligibility: Eligibility? = null, val pendingOperations: List<CommandRecovery> = emptyList(),
    val publicationConfirmation: EntryInput? = null,
    val publicationIssue: String? = null,
    val publicationSourceRequest: String? = null,
    val publicationSourceRecord: MobileRecord<SourceDeclarationBody>? = null,
    val publicationSourceResult: Boolean = false,
    val pendingTaskLink: PendingClientTaskLink? = null,
    val composeAudienceValid: Boolean = true,
    val entries: PageState<EntryBody> = PageState(), val pets: PageState<PetBody> = PageState(),
    val myPets: PageState<PetBody> = PageState(), val applications: PageState<AdoptionBody> = PageState(),
    val cases: PageState<CaseBody> = PageState(), val conversations: PageState<ConversationBody> = PageState(),
    val groups: PageState<GroupBody> = PageState(), val messages: PageState<MessageBody> = PageState(),
    val comments: PageState<CommentBody> = PageState(), val commentMetrics: Map<Long,CommentMetrics> = emptyMap(), val notifications: PageState<NotificationBody> = PageState(),
    val petChecks: List<VerificationSummary> = emptyList(), val pet: MobileRecord<PetBody>? = null, val entry: MobileRecord<EntryBody>? = null,
    val application: MobileRecord<AdoptionBody>? = null, val case: MobileRecord<CaseBody>? = null,
    val conversation: MobileRecord<ConversationBody>? = null, val chatLead: MobileRecord<LeadBody>? = null, val chatCase: MobileRecord<CaseBody>? = null, val group: MobileRecord<GroupBody>? = null, val profile: MobileProfile? = null,
    val members: Map<Long,PublicMember> = emptyMap(), val metrics: Map<Long, EntryMetrics> = emptyMap(), val inbox: InboxSummary? = null, val inboxError: String? = null,
    val inboxReadAt: String? = null, val inboxFailure: ClientReadFailure? = null,
    val feedSpace: String = "community", val feedKeyword: String = "", val feedCity: String = "",
    val petKeyword: String = "", val petCity: String = "", val petSpecies: String = "",
    val caseCity: String = "",
    val inboxReadAll: InboxReadAllState = InboxReadAllState(),
    val emoji: List<EmojiItem> = emptyList(), val stickers: List<StickerItem> = emptyList(), val detailLoading: Boolean = false, val detailError: String? = null,
    val detailFailure: ClientReadFailure? = null,
    val pendingNotification: PendingNotification? = null, val notificationLoading: Boolean = false,
    val notificationError: String? = null,
    val expressionState: MobileRecord<ExpressionBody>? = null,
    val busy: Boolean = false, val notice: String? = null, val conflict: Boolean = false,
    val applying: Boolean = false, val applicationText: String = "", val actionNote: String = "",
    val reply: String = "", val replyTo: Long = 0, val replySticker: String = "", val replyPhotos: List<PhotoAttachment> = emptyList(), val composeEntry: EntryBody = EntryBody(""), val composeTitle: String = "", val composeText: String = "",
    val replyDraftRestoring: Boolean = false, val replyDraftError: String? = null,
    val replyDraftReady: Boolean = true,
    val guestReplySuggestion: String? = null,
    val savedReplyDestinations: List<MobileDestination> = emptyList(),
    val photos: List<PhotoAttachment> = emptyList(), val draftLoading: Boolean = false, val draftLoaded: Boolean = false,
    val composeEdited: Boolean = false,
    val confirmation: AdoptionCommand? = null,
)

/** One portable state owner. Rotation, tab layout and iPad resizing never create a second business store. */
class MobileStore(val api: MobileApi, private val ownsApi: Boolean = true, private val localDrafts: LocalComposeDrafts? = null,
    private val localWorkspaces: LocalWorkspaceDrafts? = null, private val localReplies: LocalWorkspaceDrafts? = null) :
    MviStore<MobileState, MobileStoreIntent, MobileStoreEffect>(MobileState(replyDraftReady = localReplies == null)) {
    val features = MobileFeatureStore(api, scope, localWorkspaces)
    private var providerGuest: Pair<MobileDestination,String>? = null
    val providerAuth = ProviderAuthStore(api, scope) { session ->
        val original = providerGuest; providerGuest = null
        finishAuthentication(session,original?.second.orEmpty(),original?.first ?: _state.value.destination)
        if(features.state.value.route != null) features.reload()
    }

    private var accountEpoch = 0L
    private var detailRevision = 0L
    private val history = mutableListOf<MobileDestination>()
    private var resumeApply: Long? = null
    private var replyKey = newKey()
    private var sending = false
    private var draftVersion = 0L
    private val photoBytes = mutableMapOf<String, SelectedPhoto>()
    private val replyDrafts = mutableMapOf<MobileDestination, String>()
    private val replyKeys = mutableMapOf<MobileDestination, String>()
    private val richReplies = mutableMapOf<MobileDestination,Pair<Long,String>>()
    private val replyPhotos = mutableMapOf<MobileDestination,List<PhotoAttachment>>()
    private val replyPhotoBytes = mutableMapOf<String,SelectedPhoto>()
    private val uncertainReplies = mutableSetOf<MobileDestination>()
    private val uploads = Semaphore(2)
    private val localSaveMutex = Mutex()
    private val expressionMutex = Mutex()
    private var localRevision = 0L
    private var localReady = localDrafts == null
    private var localPublicationKey: String? = null
    private val replySaveMutex = Mutex()
    private var replyRevision = 0L
    private var replyReady = localReplies == null
    private var pendingGuestReply: Pair<MobileDestination,String>? = null

    override fun accept(intent: MobileStoreIntent) {
        when (intent) {
            is MobileStoreIntent.SelectTab -> select(intent.tab)
            is MobileStoreIntent.OpenDestination -> open(intent.destination)
            MobileStoreIntent.Back -> back()
            is MobileStoreIntent.RefreshHome -> refreshHome(intent.more)
            is MobileStoreIntent.ReloadDetail -> reloadDetail(intent.more)
            MobileStoreIntent.ShowLogin -> showLogin()
            MobileStoreIntent.CancelLogin -> cancelLogin()
            MobileStoreIntent.ClearNotice -> clearNotice()
        }
    }

    init {
        if (api.user.value == null) restoreReplyDrafts()
        scope.launch {
            api.user.collect { user ->
                if (_state.value.user?.id != user?.id) accountChanged(user)
                else if (_state.value.user != user) _state.update { it.copy(user = user) }
            }
        }
        refreshCapabilities()
        scope.launch {
            try { val catalog = api.expressions(); _state.update { it.copy(emoji = catalog.emoji, stickers = catalog.stickers) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Text input remains usable when the optional catalogue is unavailable. */ }
        }
        task("restore") {
            try {
                val profile = api.restore()
                _state.update { it.copy(profile = profile) }
            } finally { _state.update { it.copy(restoring = false) } }
        }
        refreshHome()
    }

    private fun accountChanged(user: MobileUser?) {
        if(providerAuth.state.value.stage !in setOf(ProviderAuthStage.Checking,ProviderAuthStage.Verifying,ProviderAuthStage.Creating,ProviderAuthStage.Linking)) providerGuest = null
        val previousOwner = _state.value.user?.id
        val sensitiveSignOut = user == null && previousOwner != null && features.sensitive.state.value.stage == SensitiveStage.Submitting
        accountEpoch++
        jobs.filterKeys { it != "restore" && it != "capabilities" }.values.forEach { it.cancel() }
        jobs.keys.removeAll { it != "restore" && it != "capabilities" }
        history.clear()
        draftVersion = 0L
        localRevision = 0L
        localReady = localDrafts == null
        localPublicationKey = null
        photoBytes.clear()
        replyDrafts.clear()
        replyKeys.clear(); richReplies.clear(); replyPhotos.clear(); replyPhotoBytes.clear(); uncertainReplies.clear()
        replyRevision = 0L; replyReady = localReplies == null
        pendingGuestReply = null
        detailRevision++
        _state.update { old ->
            MobileState(restoring = old.restoring, user = user, tab = old.tab,
                destination = old.destination, auth = if (user != null) AuthForm() else old.auth,
                pendingNotification = old.pendingNotification?.takeIf { user != null && (it.owner == 0L || it.owner == user.id) },
                pendingTaskLink = old.pendingTaskLink?.takeIf { it.owner == 0L || it.owner == user?.id },
                capabilities = old.capabilities, capabilityError = old.capabilityError, emoji = old.emoji, stickers = old.stickers)
        }
        if (user != null && localDrafts != null) restoreLocalComposeDraft(user.id)
        restoreReplyDrafts()
        if (sensitiveSignOut && previousOwner != null) task("clear-signed-out-drafts") {
            withContext(NonCancellable) { localDrafts?.clear(previousOwner); localWorkspaces?.clear(previousOwner); localReplies?.clear(previousOwner) }
        }
        refreshHome()
        reloadDetail()
        if (user != null) { refreshEligibility(); refreshInbox(); task("expressions") { val record = api.expressionState().items.firstOrNull(); _state.update { it.copy(expressionState = record) } } }
    }

    private fun task(key: String, block: suspend () -> Unit) {
        jobs[key]?.cancel()
        val epoch = accountEpoch
        lateinit var job: Job
        job = scope.launch(start = CoroutineStart.LAZY) {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == accountEpoch) {
                    if (e is MobileApiException && e.status == 401) _state.update { it.copy(auth = it.auth.copy(visible = true)) }
                    _state.update { it.copy(notice = e.mobileMessage(), restoring = false) }
                }
            } finally { if (jobs[key] == job) jobs.remove(key) }
        }
        jobs[key] = job
        job.start()
    }

    private fun refreshCapabilities() = task("capabilities") {
        try { val capabilities = api.capabilities(); _state.update { it.copy(capabilities = capabilities, capabilityError = null) } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _state.update { it.copy(capabilityError = e.mobileMessage()) } }
    }

    fun select(tab: MobileTab) {
        if (_state.value.busy) return
        pendingGuestReply = null
        history.clear()
        detailRevision++
        _state.update { it.copy(tab = tab, destination = MobileDestination.Home, detailError = null, detailFailure = null, applying = false, confirmation = null,guestReplySuggestion = null) }
        if (tab in listOf(MobileTab.Publish, MobileTab.Messages, MobileTab.Profile) && _state.value.user == null) showLogin()
        refreshHome()
    }

    fun open(destination: MobileDestination) {
        if (_state.value.busy || destination == _state.value.destination) return
        pendingGuestReply = null
        history.add(_state.value.destination)
        jobs.remove("comments")?.cancel(); jobs.remove("messages")?.cancel()
        detailRevision++
        replyKey = replyKeys.getOrPut(destination) { newKey() }
        _state.update { it.copy(destination = destination, pet = null, petChecks = emptyList(), entry = null, case = null, application = null,
            conversation = null, chatLead = null, chatCase = null, group = null, comments = PageState(), commentMetrics = emptyMap(), messages = PageState(), reply = replyDrafts[destination].orEmpty(),
            replyTo = richReplies[destination]?.first ?: 0, replySticker = richReplies[destination]?.second.orEmpty(), replyPhotos = replyPhotos[destination].orEmpty(), actionNote = "",
            applying = false, applicationText = "", detailError = null, detailFailure = null, conflict = false, confirmation = null,guestReplySuggestion = null) }
        reloadDetail()
    }

    fun back(): Boolean {
        val s = _state.value
        if (s.busy) return true
        if (s.auth.visible) { cancelLogin(); return true }
        if (s.confirmation != null) { dismissConfirmation(); return true }
        if (s.applying) { _state.update { it.copy(applying = false) }; return true }
        if (s.destination == MobileDestination.Home) return false
        jobs.remove("comments")?.cancel(); jobs.remove("messages")?.cancel()
        detailRevision++
        val previous = history.removeLastOrNull() ?: MobileDestination.Home
        pendingGuestReply = null
        replyKey = replyKeys.getOrPut(previous) { newKey() }
        _state.update { it.copy(destination = previous,guestReplySuggestion = null,comments = PageState(),commentMetrics = emptyMap(),
            detailError = null, detailFailure = null, confirmation = null, applying = false, reply = replyDrafts[previous].orEmpty(), replyTo = richReplies[previous]?.first ?: 0, replySticker = richReplies[previous]?.second.orEmpty(), replyPhotos = replyPhotos[previous].orEmpty()) }
        reloadDetail()
        return true
    }

    fun showLogin() { _state.update { it.copy(auth = it.auth.copy(visible = true, error = null,errorCode = "")) }; refreshCapabilities() }
    fun cancelLogin() { if (_state.value.busy) return; resumeApply = null; _state.update { it.copy(auth = AuthForm()) } }
    fun editAuth(form: AuthForm) { _state.update { it.copy(auth = form.copy(error = null,errorCode = "")) } }
    fun clearNotice() { _state.update { it.copy(notice = null) } }
    fun rememberExpression(id: String) = task("expression-$id") {
        if (_state.value.user == null) return@task
        expressionMutex.withLock {
            val record = api.expressionState().items.firstOrNull()
            val body = record?.data ?: ExpressionBody()
            val updated = api.saveExpressions(record?.version ?: 0, body.copy(recent = (listOf(id) + body.recent).distinct().take(50)))
            _state.update { it.copy(expressionState = updated) }
        }
    }
    fun favoriteExpression(id: String) = command {
        expressionMutex.withLock {
            val record = api.expressionState().items.firstOrNull()
            val body = record?.data ?: ExpressionBody()
            val updated = api.saveExpressions(record?.version ?: 0, body.copy(favorites = if (id in body.favorites) body.favorites - id else (body.favorites + id).distinct()))
            _state.update { it.copy(expressionState = updated) }
        }
    }
    fun filterFeed(space: String, keyword: String = _state.value.feedKeyword, city: String = _state.value.feedCity) {
        require(space in listOf("community", "circle", "diary", "following", "local"))
        if (keyword.encodeToByteArray().size > 120) { pickerFailed("搜索词过长，请缩短后重试"); return }
        jobs.remove("entries")?.cancel()
        _state.update { it.copy(feedSpace = space, feedKeyword = keyword, feedCity = city, entries = PageState(), metrics = emptyMap()) }
        refreshHome()
    }
    fun editComposeCity(city: String) { if(!canEditCompose() || city.length > 120) return; _state.update { it.copy(composeEntry = it.composeEntry.copy(city = city),composeEdited = true,publicationConfirmation = null) }; scheduleLocalSave() }
    fun filterCases(city: String) {
        if(city.encodeToByteArray().size > 120) { pickerFailed("请核对城市"); return }
        jobs.remove("cases")?.cancel()
        _state.update { it.copy(caseCity = normalizeCity(city),cases = PageState()) }
        reloadDetail()
    }
    fun filterPets(keyword: String, city: String, species: String) {
        if(keyword.encodeToByteArray().size > 120 || city.encodeToByteArray().size > 120 || species !in listOf("","cat","dog")) { pickerFailed("请缩短搜索词并核对城市与物种");return }
        jobs.remove("pets")?.cancel()
        _state.update { it.copy(petKeyword = keyword,petCity = city,petSpecies = species,pets = PageState()) }
        refreshHome()
    }
    fun editComposeOptions(space: String, audience: Audience, petId: Long = 0, category: String = _state.value.composeEntry.category) {
        if (!canEditCompose()) return
        require(space in listOf("community", "circle", "diary"))
        if (space == "community" && audience.mode != "public" || space == "circle" && audience.mode == "public") return
        _state.update { it.copy(composeEntry = it.composeEntry.copy(space = space, audience = audience, petId = petId, category = category), composeEdited = true, publicationConfirmation = null, composeAudienceValid = true) }
        scheduleLocalSave()
    }
    fun editComposeAudienceList(raw: String) {
        val pieces = raw.split(',', '，', ' ', '\n').filter(String::isNotBlank)
        val values = pieces.mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0 } }.distinct()
        if (values.size != pieces.size || values.size > 200) {
            _state.update { it.copy(composeAudienceValid = false, publicationConfirmation = null, notice = "好友名单格式有误，已暂停发布，请核对") }; return
        }
        val s = _state.value
        editComposeOptions(s.composeEntry.space,s.composeEntry.audience.copy(userIds = values),s.composeEntry.petId,s.composeEntry.category)
    }
    fun dismissPublication() { if (!_state.value.busy) _state.update { it.copy(publicationConfirmation = null, publicationIssue = null, publicationSourceResult = false) } }
    fun hidePublicationSourceResult() { if (!_state.value.busy) _state.update { it.copy(publicationSourceResult = false) } }
    fun clearPublicationIssue() { if (!_state.value.busy) _state.update { it.copy(publicationIssue = null) } }
    fun recheckPublicationSource(origin: String, rightsConfirmed: Boolean) = command {
        if (!rightsConfirmed || !canEditCompose() || _state.value.publicationSourceRequest != null) return@command
        val epoch = accountEpoch
        val pending = _state.value.publicationConfirmation ?: return@command
        val revised = pending.copy(declaration = PublicationDeclaration(origin = origin))
        features.requireCurrentAudience(revised.entry.audience)
        val preview = api.previewPublication(revised)
        if (epoch != accountEpoch || _state.value.publicationConfirmation != pending) return@command
        val checked = revised.copy(declaration = PublicationDeclaration(preview.manifestHash,preview.origin))
        val previous = _state.value.publicationSourceRecord?.let { prior ->
            try { api.sourceDeclaration(prior.id).takeIf { it.ownerId == _state.value.user?.id && it.state == "active" } }
            catch (e: MobileApiException) { if (e.status in listOf(403,404)) null else throw e }
        }
        if (epoch != accountEpoch) return@command
        val prepared = api.prepareSourceDeclaration(SourceDeclarationInput(newRequestKey(),checked,true,previous?.id ?: 0))
        if (epoch != accountEpoch) return@command
        val key = prepared.clientRequestId
        _state.update { it.copy(publicationConfirmation = checked, publicationSourceRequest = key, publicationIssue = null) }
        try {
            persistCompose()
            if (epoch != accountEpoch) return@command
            val record = api.declareSource(prepared)
            if (epoch == accountEpoch && _state.value.publicationSourceRequest == key) acceptPublicationSource(record)
        } catch (e: Exception) {
            if (epoch == accountEpoch && api.commandStatus(key)?.phase in listOf(null,"rejected")) {
                _state.update { it.copy(publicationSourceRequest = null) }
                withContext(NonCancellable) { persistCompose() }
            }
            throw e
        }
    }
    private suspend fun acceptPublicationSource(record: MobileRecord<SourceDeclarationBody>) {
        val epoch = accountEpoch
        var pending = _state.value.publicationConfirmation
        if (record.id <= 0 || record.ownerId != _state.value.user?.id || record.ownerId != api.user.value?.id || record.state != "active") throw MobileApiException(403,"source record is not available")
        if (pending != null && pending.declaration.origin != record.data.origin) {
            val candidate = pending.copy(declaration = PublicationDeclaration(origin = record.data.origin))
            val preview = try { api.previewPublication(candidate) }
            catch (e: CancellationException) { throw e } catch (_: Exception) { null }
            if (epoch != accountEpoch || record.ownerId != api.user.value?.id) return
            if (preview != null && preview.manifestHash == record.data.manifestHash) pending = candidate.copy(declaration = PublicationDeclaration(preview.manifestHash,preview.origin))
        }
        val matches = pending != null && pending.declaration.manifestHash == record.data.manifestHash && pending.declaration.origin == record.data.origin
        val resolved = pending
        _state.update { it.copy(publicationSourceRecord = record, publicationSourceRequest = null, publicationSourceResult = true,
            publicationConfirmation = if (matches && resolved != null) resolved.copy(declaration = resolved.declaration.copy(recordId = record.id)) else resolved,
            notice = "来源声明已记录；内容尚未发布，发布资格和受众仍需核对") }
        persistCompose()
    }
    fun queryPublicationSource() = command {
        val key = _state.value.publicationSourceRequest ?: return@command
        val epoch = accountEpoch
        try {
            val receipt = api.reconcileOperation(key)
            if (receipt.clientRequestId != key || receipt.command != "source.declare" || receipt.state != "committed" || receipt.resultKind != "source_declarations") throw OperationResultUnknown(key)
            val record = api.sourceDeclaration(receipt.resultId)
            if (epoch == accountEpoch && _state.value.publicationSourceRequest == key) acceptPublicationSource(record)
        } catch (e: MobileApiException) {
            if (e.status == 404) { if (epoch == accountEpoch && _state.value.publicationSourceRequest == key) _state.update { it.copy(notice = "暂未查到声明回执，保留原请求，请勿重复提交") } } else throw e
        }
    }
    private suspend fun finishRecoveredSource(receipt: OperationReceipt) {
        if (receipt.command != "source.declare" || receipt.state != "committed" || receipt.resultKind != "source_declarations" || receipt.clientRequestId != _state.value.publicationSourceRequest) return
        val epoch = accountEpoch
        val record = api.sourceDeclaration(receipt.resultId)
        if (epoch == accountEpoch && receipt.clientRequestId == _state.value.publicationSourceRequest) acceptPublicationSource(record)
    }
    fun queueTaskLink(target: TaskLinkTarget): Boolean {
        if (target.kind !in taskLinkKinds || target.id <= 0) return false
        val owner = if (target.isPublic) 0L else api.user.value?.id ?: return false
        _state.update { it.copy(pendingTaskLink = PendingClientTaskLink(target, owner)) }
        return true
    }
    fun dismissTaskLink() { _state.update { it.copy(pendingTaskLink = null) } }
    fun openPendingTaskLink() {
        val pending = _state.value.pendingTaskLink ?: return
        if (!canOpenNotification()) return
        if (_state.value.user?.id != api.user.value?.id) return
        if (pending.owner != 0L && pending.owner != _state.value.user?.id) { dismissTaskLink(); return }
        taskLinkDestination(pending.target)?.let { target -> features.closeRoute(); this.open(target); dismissTaskLink(); return }
        taskLinkFeature(pending.target)?.let { target -> features.open(target); dismissTaskLink() }
    }

    /** 推送只携带通知编号，不采信正文、目标类型、URL或任何写入指令。 */
    fun queueNotification(raw: String): Boolean {
        val id = canonicalNotificationId(raw) ?: return false
        val pending = PendingNotification(id, _state.value.user?.id ?: 0L)
        if (_state.value.pendingNotification == pending) return true
        jobs.remove("notification-link")?.cancel()
        _state.update { it.copy(pendingNotification = pending, notificationError = null, notificationLoading = false) }
        return true
    }
    fun queueInboxNotification() {
        jobs.remove("notification-link")?.cancel()
        _state.update { it.copy(pendingNotification = PendingNotification(0, it.user?.id ?: 0L), notificationError = null, notificationLoading = false) }
    }
    fun dismissNotification() {
        jobs.remove("notification-link")?.cancel()
        _state.update { it.copy(pendingNotification = null, notificationError = null, notificationLoading = false) }
    }
    fun canOpenNotification(): Boolean = _state.value.let { s -> features.state.value.let { f ->
        !s.restoring && !s.busy && !s.notificationLoading && !f.busy && !s.auth.visible && s.confirmation == null &&
            s.publicationConfirmation == null && f.confirmation == null && !s.inboxReadAll.visible && !features.age.state.value.busy &&
            features.sensitive.state.value.stage == SensitiveStage.Idle && providerAuth.state.value.stage == ProviderAuthStage.Idle
    } }
    fun openPendingNotification() {
        val pending = _state.value.pendingNotification ?: return
        if (!canOpenNotification()) return
        val actor = _state.value.user?.id ?: run { showLogin(); return }
        if (pending.owner != 0L && pending.owner != actor) { dismissNotification(); return }
        if (pending.id == 0L) { features.closeRoute(); select(MobileTab.Messages); dismissNotification(); return }
        val epoch = accountEpoch
        val original = _state.value
        val route = features.state.value.route
        _state.update { it.copy(notificationLoading = true, notificationError = null) }
        task("notification-link") {
            fun current() = epoch == accountEpoch && _state.value.pendingNotification == pending
            try {
                val record = api.notification(pending.id)
                if (!current()) return@task
                if (record.ownerId != actor || record.id != pending.id) throw MobileApiException(403, "notification unavailable")
                if (_state.value.destination != original.destination || _state.value.tab != original.tab || features.state.value.route != route) return@task
                _state.update { it.copy(notificationLoading = false) }
                if (!canOpenNotification()) return@task
                if (!openNotification(record.data)) throw MobileApiException(404, "notification target unavailable")
                dismissNotification()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (current()) _state.update { it.copy(notificationError = e.mobileMessage()) }
            } finally {
                if (current()) _state.update { it.copy(notificationLoading = false) }
            }
        }
    }

    /** 站内列表和系统通知共用白名单；打开事项本身不标已读、不确认或提交。 */
    fun openNotification(body: NotificationBody): Boolean {
        if (body.targetId <= 0) return false
        val feature = when (body.targetKind) {
            "appointments" -> FeatureRoute(FeaturePage.Appointment, body.targetId)
            "invitations" -> FeatureRoute(FeaturePage.Invitations)
            "claims" -> FeatureRoute(FeaturePage.Claim, body.targetId)
            "offers" -> FeatureRoute(FeaturePage.Offer, body.targetId)
            "reviews" -> FeatureRoute(FeaturePage.Review, body.targetId)
            "guardians" -> FeatureRoute(FeaturePage.Guardians, body.targetId)
            "friendships" -> FeatureRoute(FeaturePage.Friends)
            else -> null
        }
        if (feature != null) { features.open(feature); return true }
        val target = when (body.targetKind) {
            "entries" -> MobileDestination.Entry(body.targetId)
            "pets" -> MobileDestination.Pet(body.targetId)
            "adoptions" -> MobileDestination.Application(body.targetId)
            "cases" -> MobileDestination.Case(body.targetId)
            "conversations" -> MobileDestination.Chat(body.targetId)
            "groups" -> MobileDestination.Chat(body.targetId, true)
            else -> return false
        }
        features.closeRoute(); this.open(target)
        return true
    }
    fun refreshInbox() = task("inbox") {
        if (_state.value.user == null) return@task
        try { val summary = api.inbox(); api.devicePush.updateBadge(summary.globalTotal); _state.update { it.copy(inbox = summary, inboxError = null, inboxFailure = null, inboxReadAt = clientReadTimestamp()) } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _state.update { it.copy(inboxError = e.mobileMessage(), inboxFailure = e.readFailure(),
            inbox = if (e is MobileApiException && e.status in listOf(401,403,404)) null else it.inbox,
            inboxReadAt = if (e is MobileApiException && e.status in listOf(401,403,404)) null else it.inboxReadAt) } }
    }
    fun prepareInboxReadAll() {
        if (_state.value.user == null) { showLogin(); return }
        val current = _state.value.inboxReadAll
        if (current.snapshot != null || current.prepareRequestId.isNotEmpty() || current.resultUnknown) {
            _state.update { it.copy(inboxReadAll = it.inboxReadAll.copy(visible = true)) }; return
        }
        // After process death the secure command journal, rather than an empty
        // in-memory dialog, determines whether an earlier intent is unresolved.
        try {
            api.pendingInboxReadAll()?.let { pending ->
                _state.update { it.copy(inboxReadAll = InboxReadAllState(visible = true, resultUnknown = true,
                    error = "原全部已读请求待核对，请重试原请求；不会重新核销新增消息",
                    prepareRequestId = if (pending.confirmation == null) pending.requestId else "",
                    confirmRequestId = if (pending.confirmation != null) pending.requestId else "",
                    recoveredConfirmation = pending.confirmation)) }
                return
            }
        } catch (e: Exception) { pickerFailed(e.mobileMessage()); return }
        runInboxReadAll(confirm = false)
    }

    fun confirmInboxReadAll() {
        if (_state.value.inboxReadAll.resultUnknown) {
            pickerFailed("提交结果待核对，请重试原请求"); return
        }
        runInboxReadAll(confirm = true)
    }

    fun retryInboxReadAll() = runInboxReadAll(confirm = _state.value.inboxReadAll.confirmRequestId.isNotEmpty(), retry = true)

    fun dismissInboxReadAll() {
        _state.update { state -> state.copy(inboxReadAll = state.inboxReadAll.hidden()) }
    }

    fun restartInboxReadAll() {
        val current = _state.value.inboxReadAll
        if (_state.value.busy || current.preparing || current.submitting) return
        if (current.resultUnknown) { prepareInboxReadAll(); return }
        _state.update { it.copy(inboxReadAll = InboxReadAllState()) }
        prepareInboxReadAll()
    }

    private fun runInboxReadAll(confirm: Boolean, retry: Boolean = false) {
        val current = _state.value.inboxReadAll
        if (_state.value.busy || _state.value.user == null || current.preparing || current.submitting) return
        val snapshot = current.snapshot
        if (confirm && snapshot == null && current.recoveredConfirmation == null || retry && !current.canRetry) return
        val key = (if (confirm) current.confirmRequestId else current.prepareRequestId).ifEmpty { newRequestKey() }
        val epoch = accountEpoch
        command {
            _state.update { it.copy(inboxReadAll = current.copy(preparing = !confirm, submitting = confirm,
                visible = true, error = null, prepareRequestId = if (confirm) current.prepareRequestId else key,
                confirmRequestId = if (confirm) key else current.confirmRequestId)) }
            try {
                // An uncertain result is retried with its original persisted bytes
                // and request ID. A 404 receipt does not authorize a new intent.
                if (retry && current.resultUnknown && api.commandStatus(key)?.phase != "committed") api.retryOriginal(key)
                if (confirm) {
                    val input = current.recoveredConfirmation ?: InboxReadAllInput(snapshot!!.snapshotId, snapshot.version, key)
                    val result = api.confirmInboxReadAll(input)
                    if (epoch != accountEpoch) return@command
                    _state.update { it.copy(inboxReadAll = InboxReadAllState(),
                        notice = "已核销 ${result.appliedCount} 条未读${if (result.skippedCount > 0) "，${result.skippedCount} 条已不可访问" else ""}；新消息和待处理事项仍保留") }
                    // Never optimistically zero the inbox: new arrivals and muted
                    // messages are reconciled from the authoritative summary.
                    refreshInbox()
                    loadNotifications()
                } else {
                    val result = api.prepareInboxReadAll(InboxSnapshotInput(key))
                    if (epoch != accountEpoch) return@command
                    _state.update { it.copy(inboxReadAll = InboxReadAllState(snapshot = result, prepareRequestId = key, visible = it.inboxReadAll.visible)) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == accountEpoch) _state.update { state -> state.copy(inboxReadAll = state.inboxReadAll.copy(
                    preparing = false, submitting = false, error = e.mobileMessage(), resultUnknown = e is OperationResultUnknown)) }
                throw e
            }
        }
    }
    private fun canEditReply(): Boolean {
        if (!replyReady) { pickerFailed("本机消息草稿尚未恢复，请先重试读取"); return false }
        if (sending || _state.value.busy) return false
        val destination = _state.value.destination
        if (destination in uncertainReplies) { pickerFailed("原消息结果待核对，请先查询原编号，当前内容不能重复发送"); return false }
        val path = when (destination) { is MobileDestination.Chat -> "${if (destination.group) "groups" else "conversations"}/${destination.id}/messages"; is MobileDestination.Entry -> "entries/${destination.id}/comments"; is MobileDestination.Pet -> if(destination.discussion) "pets/${destination.id}/comments" else return false; else -> return false }
        if (api.recoverableCommands().any { it.command == path && it.phase in listOf("sent", "result_unknown") }) {
            pickerFailed("原消息结果待核对，输入已保留，请先到待核对操作查看原请求"); return false
        }
        return true
    }
    fun replyTo(id: Long) {
        if (!canEditReply()) return
        richReplies[_state.value.destination] = id to _state.value.replySticker
        replyKey = newKey(); replyKeys[_state.value.destination] = replyKey; _state.update { it.copy(replyTo = id) }
        scheduleReplySave()
    }
    fun selectSticker(id: String) {
        if (!canEditReply() || id.isNotBlank() && _state.value.stickers.none { it.id == id }) return
        richReplies[_state.value.destination] = _state.value.replyTo to id
        replyKey = newKey(); replyKeys[_state.value.destination] = replyKey; _state.update { it.copy(replySticker = id) }
        scheduleReplySave()
    }
    fun addReplyPhotos(selected: List<SelectedPhoto>) {
        val s = _state.value; val chat = s.destination as? MobileDestination.Chat ?: return
        if (!canEditReply() || !requireInteractionContract() || !chat.group && s.conversation?.state != "accepted") return
        replyKey = newKey(); replyKeys[chat] = replyKey
        selected.take((9 - s.replyPhotos.size).coerceAtLeast(0)).forEach { photo ->
            val key = newKey(); replyPhotoBytes[key] = photo
            replyPhotos[chat] = replyPhotos[chat].orEmpty() + PhotoAttachment(key, photo.name, mime = photo.mime)
            _state.update { it.copy(replyPhotos = replyPhotos[chat].orEmpty()) }; retryReplyPhoto(key)
        }
    }
    fun retryReplyPhoto(key: String) {
        if (!canEditReply()) return
        val chat = _state.value.destination as? MobileDestination.Chat ?: return
        val item = replyPhotos[chat].orEmpty().firstOrNull { it.key == key } ?: return
        if (item.uploading || item.mediaId != null) return
        val epoch = accountEpoch
        fun update(change: (PhotoAttachment) -> PhotoAttachment) {
            if (epoch != accountEpoch) return
            replyPhotos[chat] = replyPhotos[chat].orEmpty().map { if (it.key == key) change(it) else it }
            if (_state.value.destination == chat) _state.update { it.copy(replyPhotos = replyPhotos[chat].orEmpty()) }
        }
        update { it.copy(uploading = true,error = null,uploadPercent = null) }
        task("reply-photo-$key") {
            try {
                val photo = replyPhotoBytes[key] ?: localReplies?.photo(_state.value.user?.id ?: 0L,WorkspacePhoto(item.key,item.name,item.mime))?.also { replyPhotoBytes[key] = it }
                    ?: throw LocalDraftException("原照片不可读，请移除后重新选择")
                persistReplies()
                val result = api.uploadPrivate(photo,key) { sent, total -> update { it.withProgress(sent,total) } }
                update { it.copy(uploading = false,mediaId = result.id) }
                persistReplies()
                if (epoch == accountEpoch) replyPhotoBytes.remove(key)
            } catch (e: CancellationException) { throw e } catch (e: Exception) { update { it.copy(uploading = false,error = e.mobileMessage()) } }
        }
    }
    fun removeReplyPhoto(key: String) {
        if (!canEditReply() || _state.value.busy) return
        val destination = _state.value.destination
        jobs.remove("reply-photo-$key")?.cancel(); replyPhotoBytes.remove(key)
        replyPhotos[destination] = replyPhotos[destination].orEmpty().filterNot { it.key == key }
        replyKey = newKey(); replyKeys[destination] = replyKey; _state.update { it.copy(replyPhotos = replyPhotos[destination].orEmpty()) }
        scheduleReplySave()
    }
    fun moveReplyPhoto(key: String, offset: Int) {
        if (!canEditReply() || _state.value.busy) return
        val destination = _state.value.destination
        val photos = replyPhotos[destination].orEmpty().toMutableList()
        val index = photos.indexOfFirst { it.key == key }
        val target = index + offset
        if (index < 0 || target !in photos.indices || index == target) return
        photos.add(target, photos.removeAt(index))
        replyPhotos[destination] = photos
        replyKey = newKey(); replyKeys[destination] = replyKey
        _state.update { it.copy(replyPhotos = photos) }
        scheduleReplySave()
    }
    fun editApplication(text: String) { if (withinLimit(text, 2000)) _state.update { it.copy(applicationText = text) } }
    fun editActionNote(text: String) { if (withinLimit(text, 3000)) _state.update { it.copy(actionNote = text) } }
    fun editReply(text: String): Boolean {
        if (_state.value.busy || !canEditReply() || !withinLimit(text, 2000)) return false
        if (text == _state.value.reply) return true
        replyKey = newKey()
        replyKeys[_state.value.destination] = replyKey
        replyDrafts[_state.value.destination] = text
        _state.update { it.copy(reply = text) }
        scheduleReplySave()
        return true
    }
    fun editCompose(title: String, text: String) {
        if (!canEditCompose()) return
        if (withinLimit(title, 80) && withinLimit(text, 5000)) {
            _state.update { it.copy(composeTitle = title, composeText = text, composeEdited = true, publicationConfirmation = null) }
            scheduleLocalSave()
        }
    }

    private fun canEditCompose(): Boolean {
        val notice = when {
            !localReady -> "本地草稿尚未恢复，请解锁设备并重试载入，原文件不会被覆盖"
            localPublicationKey != null || api.recoverableCommands().any { it.command == "entries" } -> "原发布结果待核对，请先核对原请求，勿重复提交"
            else -> null
        }
        if (notice != null) _state.update { it.copy(notice = notice) }
        return notice == null
    }

    private fun pendingCommands(): List<CommandRecovery> {
        val pending = api.recoverableCommands().toMutableList()
        uncertainReplies.forEach { destination ->
            val key = replyKeys[destination] ?: return@forEach
            if (pending.none { it.clientRequestId == key }) pending.add(CommandRecovery(key,replyPath(destination),"result_unknown",replayable = true))
        }
        val key = localPublicationKey ?: return pending
        return if (pending.any { it.clientRequestId == key }) pending
            else pending + CommandRecovery(key, "entries", "result_unknown", replayable = false)
    }

    private suspend fun persistCompose() {
        val storage = localDrafts ?: return
        val owner = _state.value.user?.id ?: return
        val epoch = accountEpoch
        localSaveMutex.withLock {
            if (epoch != accountEpoch || !localReady) throw LocalDraftException("本地草稿尚未恢复，原文件未被覆盖")
            val s = _state.value
            val snapshot = LocalComposeDraft(server = api.storageScope, owner = owner,
                cloudVersion = draftVersion, cloudLoaded = s.draftLoaded, title = s.composeTitle, text = s.composeText,
                edited = s.composeEdited, photos = s.photos.map { LocalComposePhoto(it.key, it.name, it.mime, it.mediaId) },
                publicationKey = localPublicationKey, entry = s.composeEntry, sourceRequestId = s.publicationSourceRequest, sourceRecord = s.publicationSourceRecord)
            val bytes = photoBytes.toMap()
            try {
                withContext(NonCancellable) {
                    val saved = storage.save(localRevision, snapshot, bytes)
                    if (epoch == accountEpoch) localRevision = saved.revision
                }
                if (epoch != accountEpoch) throw CancellationException("Account changed")
            } catch (e: CancellationException) { throw e } catch (e: LocalDraftException) { throw e } catch (_: Exception) { throw LocalDraftException("设备草稿尚未保存，已保留当前输入，请解锁设备并重试") }
        }
    }

    private fun scheduleLocalSave() {
        if (localReady && _state.value.user != null && localDrafts != null) task("local-save") { persistCompose() }
    }
    fun saveLocalPhotoOrder() {
        if (_state.value.user == null) { showLogin(); return }
        if (!canEditCompose()) return
        if (localDrafts == null) { pickerFailed("本机草稿存储暂不可用"); return }
        task("local-save") { persistCompose(); _state.update { it.copy(notice = "照片顺序已保存到本机草稿，尚未发布") } }
    }

    private fun restoreLocalComposeDraft(owner: Long) = task("local-restore") {
        val storage = localDrafts ?: return@task
        val epoch = accountEpoch
        try {
            val draft = storage.load(owner)
            val restoredBytes = mutableMapOf<String, SelectedPhoto>()
            val restored = draft?.photos.orEmpty().map { photo ->
                var error: String? = null
                if (photo.mediaId == null) {
                    try { restoredBytes[photo.key] = storage.photo(owner, photo) } catch (e: CancellationException) { throw e } catch (_: Exception) { error = "原照片暂不可读，请重试或移除后重新选择" }
                }
                PhotoAttachment(photo.key, photo.name, photo.mediaId, error = error ?: if (photo.mediaId == null) "上传未完成，请按原编号重试" else null, mime = photo.mime)
            }
            if (epoch != accountEpoch) return@task
            photoBytes.putAll(restoredBytes)
            localRevision = draft?.revision ?: 0L
            draftVersion = draft?.cloudVersion ?: 0L
            localPublicationKey = draft?.publicationKey ?: api.recoverableCommands().filter { it.command == "entries" }.singleOrNull()?.clientRequestId
            localReady = true
            _state.update { it.copy(composeTitle = draft?.title.orEmpty(), composeText = draft?.text.orEmpty(), photos = restored,
                composeEdited = draft?.edited ?: false, draftLoaded = draft?.cloudLoaded ?: false, composeEntry = draft?.entry ?: EntryBody(""),
                publicationSourceRequest = draft?.sourceRequestId?.takeUnless { api.commandStatus(it)?.phase == "rejected" } ?: api.recoverableCommands().firstOrNull { it.command == "source_declarations" }?.clientRequestId, publicationSourceRecord = draft?.sourceRecord,
                pendingOperations = pendingCommands(), notice = if (draft != null && restored.any { photo -> photo.mediaId == null }) "本地草稿和照片已恢复，未自动重新上传或发布" else it.notice) }
            val status = localPublicationKey?.let(api::commandStatus)
            if (status?.phase == "committed") finishRecoveredPublication(status.receipt)
            else if (status?.phase == "rejected") {
                localPublicationKey = null
                persistCompose()
                _state.update { it.copy(pendingOperations = pendingCommands()) }
            }
            if (_state.value.tab == MobileTab.Publish && !_state.value.draftLoaded) restoreComposeDraft()
        } catch (e: CancellationException) { throw e } catch (e: LocalDraftException) { throw e } catch (_: Exception) { throw LocalDraftException("本地草稿暂不可读，请解锁设备后重试，原文件未被覆盖") }
    }
    private fun withinLimit(text: String, max: Int): Boolean {
        // Reject the edit as a unit; never split a surrogate, emoji sequence or IME composition with take().
        val count = text.count { it.code !in 0xDC00..0xDFFF }
        if (count <= max) return true
        _state.update { it.copy(notice = "最多可输入 $max 个字符") }
        return false
    }

    fun startProvider(provider: String, launcher: ProviderLauncher) {
        if(_state.value.busy) return
        if(providerGuest == null && _state.value.user == null) providerGuest = _state.value.destination to _state.value.reply
        providerAuth.start(provider,launcher)
    }

    private fun finishAuthentication(session: LoginSession, guestReply: String, originalDestination: MobileDestination) {
        if (_state.value.user?.id != session.user.id) accountChanged(session.user)
        if (guestReply.isNotEmpty() && originalDestination == _state.value.destination && (originalDestination is MobileDestination.Entry || originalDestination is MobileDestination.Pet)) {
            if (replyReady) _state.update { it.copy(guestReplySuggestion = guestReply) }
            else pendingGuestReply = originalDestination to guestReply
        }
        _state.update { it.copy(auth = AuthForm(), notice = "登录成功，请确认后继续原任务") }
        resumeApply?.let { id ->
            resumeApply = null
            if (_state.value.destination == MobileDestination.Pet(id)) _state.update { it.copy(applying = true) }
        }
        reloadDetail(); refreshHome()
    }

    fun login() {
        val form = _state.value.auth
        authenticate { api.password(form.username.trim(), form.password) }
    }

    fun loginEmail(input: EmailAuthInput, register: Boolean = false) {
        if (!_state.value.capabilities.email || register && (!_state.value.capabilities.registration || !input.acceptTerms)) return
        authenticate { api.emailLogin(input, register) }
    }
    fun loginPhone(phone: String, code: String) {
        if (!_state.value.capabilities.sms) return
        authenticate { api.phoneLogin(PhoneLoginInput(phone.trim(), code)) }
    }
    private fun authenticate(login: suspend () -> LoginSession) {
        val guestReply = _state.value.takeIf { it.user == null }?.reply.orEmpty()
        val originalDestination = _state.value.destination
        if (_state.value.busy) return
        // Login changes the account epoch. It cannot be a cancellable resource-loading job.
        _state.update { it.copy(busy = true, auth = it.auth.copy(password = "")) }
        scope.launch {
            try {
                finishAuthentication(login(),guestReply,originalDestination)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(auth = it.auth.copy(error = e.mobileMessage(),errorCode = (e as? MobileApiException)?.code.orEmpty())) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    fun logout() = command {
        val owner = _state.value.user?.id
        api.logout()
        if (_state.value.user != null) accountChanged(null)
        var notice = "已退出登录"
        if (owner != null) try { withContext(NonCancellable) { localDrafts?.clear(owner); localWorkspaces?.clear(owner); localReplies?.clear(owner) } }
        catch (_: Exception) { notice = "已退出登录，本地私有草稿暂未清理，请解锁设备后重试" }
        _state.update { it.copy(tab = MobileTab.Community, destination = MobileDestination.Home, notice = notice) }
        refreshHome()
    }

    suspend fun logoutAll() {
        val owner = _state.value.user?.id
        api.logout(true)
        withContext(NonCancellable) { if (owner != null) { localDrafts?.clear(owner); localWorkspaces?.clear(owner); localReplies?.clear(owner) } }
        if (_state.value.user != null) accountChanged(null)
    }

    private fun command(block: suspend () -> Unit) {
        if (_state.value.busy || sending) return
        _state.update { it.copy(busy = true, notice = null, conflict = false) }
        scope.launch {
            val epoch = accountEpoch
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                if (epoch == accountEpoch) _state.update { it.copy(notice = e.mobileMessage(), conflict = e is MobileApiException && e.status == 409,
                    publicationIssue = if (it.publicationConfirmation != null && e is MobileApiException && e.code == "known_ai_cannot_be_denied") e.code else it.publicationIssue) }
            } finally { if (epoch == accountEpoch) _state.update { it.copy(busy = false, pendingOperations = pendingCommands()) } }
        }
    }

    fun refreshHome(more: Boolean = false) {
        when (_state.value.tab) {
            MobileTab.Community -> loadPage("entries", more, { it.entries }, { s, p -> s.copy(entries = p) }, { api.feedPage(it,_state.value.feedSpace,_state.value.feedKeyword,city = _state.value.feedCity) }) {
                val epoch = accountEpoch
                val metrics = _state.value.entries.items.map { row -> row.id }.chunked(50).flatMap { api.metrics(it) }.associateBy(EntryMetrics::entryId)
                if(epoch == accountEpoch) _state.update { it.copy(metrics = metrics) }
            }
            MobileTab.Adoption -> loadPage("pets", more, { it.pets }, { s, p -> s.copy(pets = p) }, { api.adoptionPets(it,_state.value.petKeyword,_state.value.petCity,_state.value.petSpecies) }, filter = { it.data.adoptionAvailable })
            MobileTab.Publish -> if (_state.value.user != null && !_state.value.draftLoaded) restoreComposeDraft()
            MobileTab.Messages -> if (_state.value.user != null) {
                loadConversations(more)
                loadGroups(more)
                refreshInbox()
            }
            MobileTab.Profile -> _state.value.user?.let { user ->
                task("profile") { val profile = api.profile(); _state.update { it.copy(profile = profile) } }
                loadPage("my-pets", more, { it.myPets }, { s, p -> s.copy(myPets = p) }, { api.pets(it, user.id) })
            }
        }
    }

    fun loadConversations(more: Boolean = false) {
        if (_state.value.user == null) return
        loadPage("conversations", more, { it.conversations }, { s, p -> s.copy(conversations = p) }, { api.conversations(it) })
    }
    fun loadGroups(more: Boolean = false) {
        if (_state.value.user == null) return
        loadPage("groups", more, { it.groups }, { s, p -> s.copy(groups = p) }, { api.groups(it) })
    }

    private fun <T> loadPage(key: String, more: Boolean, get: (MobileState) -> PageState<T>,
        put: (MobileState, PageState<T>) -> MobileState, fetch: suspend (Long) -> MobilePage<T>,
        filter: (MobileRecord<T>) -> Boolean = { true }, after: suspend () -> Unit = {}) {
        val old = get(_state.value)
        val epoch = accountEpoch
        if (old.loading || more && !old.canLoadMore) return
        _state.update { put(it, old.starting(more)) }
        task(key) {
            try {
                var cursor = if (more) old.next else 0L
                var page: MobilePage<T>
                var selected: List<MobileRecord<T>>
                var rounds = 0
                do {
                    page = fetch(cursor)
                    selected = page.items.filter(filter)
                    check(page.nextCursor == 0L || page.nextCursor > cursor) { "Non advancing cursor" }
                    cursor = page.nextCursor
                    rounds++
                } while (selected.isEmpty() && cursor != 0L && rounds < 10)
                currentCoroutineContext().ensureActive()
                if (epoch != accountEpoch) return@task
                _state.update { put(it, PageState(((if (more) old.items else emptyList()) + selected).distinctBy { row -> row.id }, cursor, loaded = true, readAt = clientReadTimestamp())) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == accountEpoch) _state.update { put(it,
                    get(it).failed(e.mobileMessage(), e is MobileApiException && e.status in listOf(401, 403, 404), e.readFailure())) }
                return@task
            }
            // 附加统计失败不能将已成功读取的列表变成翻页失败。
            after()
        }
    }

    fun reloadDetail(more: Boolean = false) {
        val s = _state.value
        val destination = s.destination
        val revision = detailRevision
        fun current() = revision == detailRevision && destination == _state.value.destination
        when (destination) {
            MobileDestination.Home -> Unit
            MobileDestination.Help -> loadPage("cases", more, { it.cases }, { state, p -> state.copy(cases = p) }, { api.cases(it,_state.value.caseCity) })
            MobileDestination.Applications -> if (s.user == null) showLogin() else loadPage("applications", more, { it.applications }, { state, p -> state.copy(applications = p) }, { api.adoptions(it) })
            MobileDestination.Notifications -> if (s.user == null) showLogin() else loadNotifications(more)
            else -> {
                _state.update { it.copy(detailLoading = true, detailError = null, detailFailure = null, conflict = false, confirmation = null) }
                task("detail") {
                    try {
                        when (destination) {
                            is MobileDestination.Pet -> { val record = api.pet(destination.id); val checks = if(destination.discussion) emptyList() else api.petVerifications(destination.id); if (current()) { _state.update { it.copy(pet = record, petChecks = checks) };if(destination.discussion)loadComments() } }
                            is MobileDestination.Entry -> {
                                val record = api.entry(destination.id)
                                val metrics = api.metrics(listOf(destination.id))
                                if (current()) _state.update { it.copy(entry = record, metrics = it.metrics + metrics.associateBy(EntryMetrics::entryId)) }
                                if (current()) loadComments()
                            }
                            is MobileDestination.Application -> { val record = api.adoption(destination.id); if (current()) _state.update { it.copy(application = record) } }
                            is MobileDestination.Case -> { val record = api.case(destination.id); if (current()) _state.update { it.copy(case = record) } }
                            is MobileDestination.Chat -> {
                                if (destination.group) { val record = api.group(destination.id); if (current()) _state.update { it.copy(group = record) } } else {
                                    val record = api.conversation(destination.id)
                                    val lead = if (record.data.contextKind == "lead") api.leadRecord(record.data.contextId) else null
                                    val parent = lead?.let { api.case(it.referenceId) }
                                    if (current()) _state.update { it.copy(conversation = record,chatLead = lead,chatCase = parent) }
                                }
                                if (current()) loadMessages(more)
                            }
                            else -> Unit
                        }
                    } catch (e: CancellationException) { throw e } catch (e: Exception) {
                        if (current()) _state.update { it.copy(pet = null, petChecks = emptyList(), entry = null, application = null, case = null,
                            conversation = null, chatLead = null, chatCase = null, group = null, comments = PageState(), messages = PageState(), detailError = e.mobileMessage(), detailFailure = e.readFailure()) }
                    } finally { if (current()) _state.update { it.copy(detailLoading = false) } }
                }
            }
        }
    }

    fun loadNotifications(more: Boolean = false) {
        if (_state.value.user == null) return
        loadPage("notifications",more,{ it.notifications },{ s,p -> s.copy(notifications = p) },{ api.notifications(it) })
    }
    fun loadComments(more: Boolean = false) {
        val destination=_state.value.destination
        val id=when(destination) {is MobileDestination.Entry -> destination.id;is MobileDestination.Pet -> destination.id;else -> return}
        val pet=destination is MobileDestination.Pet
        if(pet && !(destination as MobileDestination.Pet).discussion) return
        val epoch=accountEpoch
        loadPage("comments", more, { it.comments }, { s, p -> s.copy(comments = p) }, { cursor ->
            val page=api.discussionComments(id,pet,cursor)
            try {
                val metrics=api.commentMetrics(if(pet) "pet_comments" else "entry_comments",page.items.map {it.id})
                if(epoch==accountEpoch && destination==_state.value.destination) _state.update {it.copy(commentMetrics = it.commentMetrics+metrics.associateBy(CommentMetrics::commentId))}
            } catch(e: CancellationException) {throw e} catch(_: Exception) { /* 评论正文仍可阅读，计数未返回不虚构点赞状态。 */ }
            page
        })
    }
    fun toggleCommentLike(id: Long) {
        val s=_state.value
        if(s.busy || !requireInteractionContract()) return
        val kind=when(s.destination) {is MobileDestination.Entry -> "entry_comments";is MobileDestination.Pet -> "pet_comments";else -> return}
        val metric=s.commentMetrics[id] ?: return
        command {
            api.reactComment(kind,id,!metric.viewerLiked)
            val metrics=api.commentMetrics(kind,listOf(id))
            if(_state.value.destination==s.destination) _state.update {it.copy(commentMetrics = it.commentMetrics+metrics.associateBy(CommentMetrics::commentId))}
        }
    }
    fun loadMessages(more: Boolean = false, fromBeginning: Boolean = false) {
        val chat = _state.value.destination as? MobileDestination.Chat ?: return
        val epoch = accountEpoch
        loadPage("messages", more, { it.messages }, { s, p -> s.copy(messages = p) }, { cursor ->
            val first = if (!more && !fromBeginning) {
                val summary = api.inbox()
                api.devicePush.updateBadge(summary.globalTotal)
                currentCoroutineContext().ensureActive()
                if (epoch != accountEpoch) throw CancellationException("Account changed")
                _state.update { it.copy(inbox = summary,inboxError = null, inboxFailure = null, inboxReadAt = clientReadTimestamp()) }
                (if(chat.group) summary.groupFirstUnread else summary.directFirstUnread)[chat.id.toString()]
            } else null
            api.messages(chat.id, chat.group, first?.takeIf { it > 0 }?.minus(1) ?: cursor)
        })
    }
    fun beginApplication() {
        val pet = _state.value.pet ?: return
        if (_state.value.user == null) { resumeApply = pet.id; showLogin() }
        else _state.update { it.copy(applying = true) }
    }
    fun submitApplication() = command {
        if (!requireInteractionContract()) return@command
        val s = _state.value
        val pet = s.pet ?: return@command
        if (s.applicationText.isBlank()) { _state.update { it.copy(notice = "请填写照护与领养计划") }; return@command }
        val record = api.apply(pet.id, s.applicationText.trim(), cycle = pet.data.adoptionCycle)
        _state.update { it.copy(applying = false, applicationText = "", busy = false) }
        open(MobileDestination.Application(record.id))
    }

    fun adoptionActions(): List<Pair<String, String>> {
        val state = _state.value
        return state.application?.let {availableAdoptionActions(it,state.user?.id)} ?: emptyList()
    }
    fun requestAdoptionAction(action: String, label: String) {
        if (!requireInteractionContract()) return
        val s = _state.value
        val record = s.application ?: return
        if (adoptionActions().none { it.first == action }) return
        if(action in listOf("confirm_trial","confirm_handover")) {
            features.open(FeatureRoute(FeaturePage.AdoptionWork,record.id))
            return
        }
        if (action in listOf("supplement", "request_supplement", "support", "request_return", "resubmit") && s.actionNote.isBlank()) {
            _state.update { it.copy(notice = "请先填写补充说明") }; return
        }
        _state.update { it.copy(confirmation = AdoptionCommand(record, action, label, s.actionNote)) }
    }
    fun dismissConfirmation() { if (!_state.value.busy) _state.update { it.copy(confirmation = null) } }
    fun confirmAdoptionAction() = command {
        if (!requireInteractionContract()) return@command
        val pending = _state.value.confirmation ?: return@command
        val record = api.adoptionAction(pending.record.id, pending.action, AdoptionActionInput(pending.record.version, pending.note))
        _state.update { it.copy(application = record, confirmation = null, actionNote = "", notice = "进度已更新") }
    }

    fun ensureMember(id: Long) {
        if(id <= 0 || id in _state.value.members || jobs["member-$id"]?.isActive == true) return
        val epoch = accountEpoch
        task("member-$id") { val member = api.member(id); if(epoch == accountEpoch) _state.update { it.copy(members = it.members + (id to member)) } }
    }
    fun toggleLike() {
        val id = _state.value.entry?.id ?: return
        toggleReaction(id)
    }
    fun toggleReaction(id: Long) {
        val s = _state.value
        if (s.user == null) { showLogin(); return }
        if (!requireInteractionContract()) return
        val metrics = s.metrics[id] ?: return
        command {
            api.react(id, !metrics.viewerLiked)
            val latest = api.metrics(listOf(id))
            _state.update { it.copy(metrics = it.metrics + latest.associateBy(EntryMetrics::entryId)) }
        }
    }
    fun sendReply() {
        val s = _state.value
        if (s.user == null) { showLogin(); return }
        if (!requireInteractionContract()) return
        if (!canEditReply() || s.reply.isBlank() && s.replySticker.isBlank() && s.replyPhotos.isEmpty() || sending || s.busy) return
        if (s.replyPhotos.any { it.mediaId == null }) { pickerFailed("请先完成消息照片上传"); return }
        val key = replyKey
        command {
            sending = true
            var sendAttempted = false
            try {
                uncertainReplies.add(s.destination)
                persistReplies()
                sendAttempted = true
                when (val destination = s.destination) {
                    is MobileDestination.Entry -> { api.richComment(destination.id, MessageBody(s.reply.trim(),s.replySticker), key,s.replyTo); loadComments() }
                    is MobileDestination.Pet -> {api.richPetComment(destination.id,MessageBody(s.reply.trim(),s.replySticker),key,s.replyTo);loadComments()}
                    is MobileDestination.Chat -> { api.richMessage(destination.id, destination.group, MessageBody(s.reply.trim(),s.replySticker),s.replyPhotos.mapNotNull { it.mediaId },key); loadMessages() }
                    else -> return@command
                }
                replyKey = newKey()
                uncertainReplies.remove(s.destination)
                replyDrafts.remove(s.destination)
                replyKeys.remove(s.destination)
                richReplies.remove(s.destination); replyPhotos.remove(s.destination)
                _state.update { it.copy(reply = "",replyTo = 0,replySticker = "",replyPhotos = emptyList()) }
                persistReplies()
            } catch (e: Exception) {
                if (!sendAttempted || e is MobileApiException && e.status in listOf(400,401,403,404,409,422,429)) uncertainReplies.remove(s.destination)
                throw e
            } finally { sending = false; scheduleReplySave() }
        }
    }
    fun markChatRead() = command {
        val s = _state.value
        val chat = s.destination as? MobileDestination.Chat ?: return@command
        val through = s.messages.items.maxOfOrNull { it.id } ?: return@command
        api.readMessages(chat.id, chat.group, through)
        val inbox = api.inbox()
        api.devicePush.updateBadge(inbox.globalTotal)
        _state.update { it.copy(inbox = inbox, inboxError = null, inboxFailure = null, inboxReadAt = clientReadTimestamp(), notice = "已标为已读") }
    }
    fun answerMessageRequest(action: String) = command {
        if (!requireInteractionContract()) return@command
        val s = _state.value
        val conversation = s.conversation ?: return@command
        if (conversation.data.recipientId != s.user?.id || conversation.state != "pending" || action !in listOf("accept", "decline")) return@command
        val record = api.conversationAction(conversation.id, conversation.version, action)
        _state.update { it.copy(conversation = record) }
    }
    fun contactPetOwner() {
        val s = _state.value
        if (s.user == null) { showLogin(); return }
        if (!requireInteractionContract()) return
        val pet = s.pet ?: return
        command {
            val conversation = api.startConversation(pet.ownerId)
            _state.update { it.copy(busy = false) }
            open(MobileDestination.Chat(conversation.id))
        }
    }
    fun readNotification(record: MobileRecord<NotificationBody>) = command {
        val updated = api.readNotification(record.id, record.version)
        _state.update { it.copy(notifications = it.notifications.copy(items = it.notifications.items.map { row -> if (row.id == updated.id) updated else row })) }
        refreshInbox()
    }
    fun publish() {
        val s = _state.value
        if (s.user == null) { showLogin(); return }
        if (!canEditCompose()) return
        if (s.composeEntry.audience.mode != "private" && !requireInteractionContract()) return
        if (!s.composeAudienceValid || s.composeEntry.audience.mode == "selected" && s.composeEntry.audience.userIds.isEmpty() || s.composeEntry.space == "diary" && s.composeEntry.petId <= 0) { pickerFailed("请核对可见好友名单及日记所属宠物"); return }
        if (s.composeText.isBlank() && s.photos.isEmpty()) return
        if (s.photos.any { it.mediaId == null }) { _state.update { it.copy(notice = "请先完成照片上传，或移除上传失败的照片") }; return }
        command {
            val epoch = accountEpoch
            var input = EntryInput(s.composeEntry.copy(text = s.composeText.trim(), title = s.composeTitle.trim()), mediaIds = s.photos.mapNotNull { it.mediaId }, clientRequestId = newRequestKey(), declaration = PublicationDeclaration(origin = s.publicationSourceRecord?.data?.origin ?: "unknown"))
            features.requireCurrentAudience(input.entry.audience)
            var issue: String? = null
            val preview = try { api.previewPublication(input) } catch (e: MobileApiException) {
                if (e.code != "known_ai_cannot_be_denied") throw e
                input = input.copy(declaration = PublicationDeclaration(origin = "unknown"))
                issue = e.code
                api.previewPublication(input)
            }
            if (epoch != accountEpoch) return@command
            val source = s.publicationSourceRecord?.takeIf { it.ownerId == s.user.id && it.data.manifestHash == preview.manifestHash && it.data.origin == preview.origin }
            _state.update { it.copy(publicationConfirmation = input.copy(declaration = PublicationDeclaration(preview.manifestHash, preview.origin, recordId = source?.id ?: 0)), publicationSourceRequest = it.publicationSourceRequest ?: api.recoverableCommands().firstOrNull { it.command == "source_declarations" }?.clientRequestId, publicationSourceResult = false, publicationIssue = issue, notice = "请核对当前内容与照片，并确认来源声明后发布") }
        }
    }

    fun confirmPublication(origin: String, confirmed: Boolean) = command {
            val epoch = accountEpoch
            if (_state.value.publicationSourceRequest != null || !canEditCompose() || !confirmed || _state.value.composeEntry.audience.mode != "private" && !requireInteractionContract()) return@command
            val s = _state.value
            val pending = s.publicationConfirmation ?: return@command
            if (pending.entry.text != s.composeText.trim() || pending.entry.title != s.composeTitle.trim() || pending.entry != s.composeEntry.copy(text = s.composeText.trim(), title = s.composeTitle.trim()) || pending.mediaIds != s.photos.mapNotNull { it.mediaId }) {
                _state.update { it.copy(publicationConfirmation = null, notice = "内容已改变，请重新核对发布内容") }; return@command
            }
            // A changed origin invalidates the old manifest and requires a new preview.
            if (origin != pending.declaration.origin) {
                val revised = pending.copy(declaration = PublicationDeclaration(origin = origin))
                features.requireCurrentAudience(revised.entry.audience)
                val preview = api.previewPublication(revised)
                if (epoch != accountEpoch) return@command
                _state.update { it.copy(publicationConfirmation = revised.copy(declaration = PublicationDeclaration(preview.manifestHash, origin)), notice = "来源已改变，请重新确认当前声明") }
                return@command
            }
            features.requireCurrentAudience(pending.entry.audience)
            val prepared = api.preparePublication(pending.copy(declaration = pending.declaration.copy(confirmed = true)))
            if (epoch != accountEpoch) return@command
            localPublicationKey = prepared.clientRequestId
            persistCompose()
            val record = try {
                api.publish(prepared)
            } catch (e: Exception) {
                if (api.commandStatus(prepared.clientRequestId)?.phase == "rejected") {
                    localPublicationKey = null
                    withContext(NonCancellable) { persistCompose() }
                }
                throw e
            }
            if (epoch != accountEpoch) return@command
            finishRecoveredPublication(OperationReceipt(prepared.clientRequestId, "entries", 0, "entries", record.id, record.version, "committed"))
            if (epoch != accountEpoch) return@command
            _state.update { it.copy(busy = false) }
            select(MobileTab.Community)
            this.open(MobileDestination.Entry(record.id))
    }

    private suspend fun finishRecoveredPublication(receipt: OperationReceipt?) {
        if (receipt == null || receipt.state != "committed" || receipt.resultKind != "entries" || receipt.clientRequestId != localPublicationKey) return
        val epoch = accountEpoch
        localPublicationKey = null
        photoBytes.clear()
        _state.update { it.copy(composeTitle = "", composeText = "", composeEntry = EntryBody(""), photos = emptyList(), composeEdited = true, publicationConfirmation = null, publicationSourceRecord = null, publicationSourceResult = false, publicationSourceRequest = null) }
        // 先持久化已确认结果后的空草稿，避免清理云端前进程退出又显示旧稿。
        try { persistCompose() } catch (e: Exception) { if (epoch == accountEpoch) localReady = false; throw e }
        var notice = "原发布已成功，本地草稿已清理"
        try {
            val cleared = api.saveComposeDraft(draftVersion, "", "", emptyList())
            if (epoch != accountEpoch) throw CancellationException("Account changed")
            draftVersion = cleared.version
            _state.update { it.copy(composeEdited = false, draftLoaded = true) }
            persistCompose()
        } catch (e: CancellationException) { throw e } catch (_: Exception) { notice = "原发布已成功，云端旧草稿暂未清理；本地不会自动恢复旧稿" }
        _state.update { it.copy(pendingOperations = pendingCommands(), notice = notice) }
    }

    fun pickerFailed(message: String) { _state.update { it.copy(notice = message) } }

    private fun requireInteractionContract(): Boolean {
        val qualification = _state.value.eligibility
        if (qualification?.contractVersion == 1 && qualification.interaction) return true
        val code = qualification?.reasons?.firstOrNull()
        _state.update { it.copy(notice = if (code == null) "交流资格尚未读取，已保留输入，请稍后重试" else MobileApiException(403, "eligibility required", code).mobileMessage()) }
        refreshEligibility()
        return false
    }

    fun refreshEligibility() = task("eligibility") {
        if (_state.value.user == null) return@task
        val qualification = api.eligibility()
        _state.update { it.copy(eligibility = qualification, pendingOperations = pendingCommands()) }
    }
    fun reconcilePending(key: String) = command {
        try {
            val receipt = api.reconcileOperation(key)
            features.operationResolved(receipt)
            finishRecoveredReply(receipt)
            finishRecoveredPublication(receipt)
            finishRecoveredSource(receipt)
            _state.update { it.copy(pendingOperations = pendingCommands(), notice = if (receipt.state == "committed") "已核对：原操作已成功，请查看原记录" else "原操作仍在处理中") }
        } catch (e: MobileApiException) {
            if (e.status == 404) _state.update { it.copy(pendingOperations = pendingCommands(), notice = "暂未查到回执，结果仍待核对，请勿重复提交") } else throw e
        }
    }
    fun retryPending(key: String) = command {
        val originalReply = replyKeys.entries.firstOrNull { it.value == key && it.key in uncertainReplies }?.key
        val known = api.commandStatus(key)
        val receipt = when {
            known?.phase == "committed" -> known.receipt ?: api.reconcileOperation(key)
            known == null && originalReply != null -> {
                // 用户主动选择原编号重试；草稿已锁定，正文、贴纸、照片顺序与原请求一致。
                val rich = richReplies[originalReply] ?: (0L to "")
                val content = MessageBody(replyDrafts[originalReply].orEmpty().trim(),rich.second)
                when (originalReply) {
                    is MobileDestination.Entry -> api.richComment(originalReply.id,content,key,rich.first)
                    is MobileDestination.Pet -> api.richPetComment(originalReply.id,content,key,rich.first)
                    is MobileDestination.Chat -> api.richMessage(originalReply.id,originalReply.group,content,replyPhotos[originalReply].orEmpty().mapNotNull { it.mediaId },key)
                    else -> error("原消息草稿不可读")
                }
                api.commandStatus(key)?.receipt ?: throw OperationResultUnknown(key)
            }
            else -> api.retryOriginal(key)
        }
        features.operationResolved(receipt)
        finishRecoveredReply(receipt)
        finishRecoveredPublication(receipt)
        finishRecoveredSource(receipt)
        _state.update { it.copy(pendingOperations = pendingCommands(),notice = "已按原请求编号核对并重试，请查看原记录") }
    }

    private fun finishRecoveredReply(receipt: OperationReceipt) {
        if (receipt.state != "committed" || receipt.resultKind !in listOf("messages", "group_messages", "entry_comments", "pet_comments")) return
        val destination = replyKeys.entries.firstOrNull { it.value == receipt.clientRequestId }?.key ?: return
        uncertainReplies.remove(destination)
        replyDrafts.remove(destination); replyKeys.remove(destination); richReplies.remove(destination); replyPhotos.remove(destination)
        scheduleReplySave()
        if (_state.value.destination == destination) {
            replyKey = newKey()
            _state.update { it.copy(reply = "", replyTo = 0, replySticker = "", replyPhotos = emptyList()) }
            reloadDetail()
        }
    }

    fun addPhotos(selected: List<SelectedPhoto>) {
        if (_state.value.user == null) { showLogin(); return }
        if (!canEditCompose()) return
        val free = 9 - _state.value.photos.size
        if (selected.size > free) _state.update { it.copy(notice = "每次发布最多 9 张照片") }
        selected.take(free.coerceAtLeast(0)).forEach { photo ->
            if (photo.mime !in listOf("image/jpeg", "image/png") || photo.bytes.size !in 1..10 * 1024 * 1024) {
                pickerFailed("请选择不超过 10 MiB 的 JPEG 或 PNG 照片")
            } else {
                val key = newKey()
                photoBytes[key] = photo
                _state.update { it.copy(photos = it.photos + PhotoAttachment(key, photo.name, mime = photo.mime), composeEdited = true, publicationConfirmation = null) }
                retryPhoto(key)
            }
        }
    }

    fun retryPhoto(key: String) {
        if (!localReady) { canEditCompose(); return }
        val item = _state.value.photos.find { it.key == key } ?: return
        if (item.mediaId != null) {
            task("photo-$key") {
                persistCompose()
                photoBytes.remove(key)
                _state.update { it.copy(photos = it.photos.map { photo -> if (photo.key == key) photo.copy(error = null) else photo }) }
            }
            return
        }
        val owner = _state.value.user?.id ?: return
        if (_state.value.photos.find { it.key == key }?.uploading != false) return
        val epoch = accountEpoch
        _state.update { it.copy(photos = it.photos.map { item -> if (item.key == key) item.copy(uploading = true, error = null, uploadPercent = null) else item }) }
        task("photo-$key") {
            try {
                val photo = photoBytes[key] ?: localDrafts?.photo(owner, LocalComposePhoto(item.key, item.name, item.mime))
                    ?: throw LocalDraftException("原照片暂不可读，请移除后重新选择")
                if (epoch != accountEpoch) return@task
                photoBytes[key] = photo
                persistCompose()
                val result = uploads.withPermit { api.upload(photo, key) { sent, total ->
                    if (epoch == accountEpoch) _state.update { state -> state.copy(photos = state.photos.map { current -> if (current.key == key) current.withProgress(sent,total) else current }) }
                } }
                if (epoch == accountEpoch) {
                    _state.update { it.copy(photos = it.photos.map { item -> if (item.key == key) item.copy(mediaId = result.id, uploading = false) else item }) }
                    persistCompose()
                    photoBytes.remove(key)
                }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                if (epoch == accountEpoch) _state.update { it.copy(photos = it.photos.map { item -> if (item.key == key) item.copy(uploading = false, error = e.mobileMessage()) else item }) }
            }
        }
    }

    suspend fun localPhotoPreview(key: String): SelectedPhoto? {
        val owner = _state.value.user?.id ?: return null
        val epoch = accountEpoch
        val compose = _state.value.photos.firstOrNull { it.key == key }
        val reply = _state.value.replyPhotos.firstOrNull { it.key == key }
        val result = when {
            compose != null -> photoBytes[key] ?: localDrafts?.photo(owner, LocalComposePhoto(key, compose.name, compose.mime))
            reply != null -> replyPhotoBytes[key] ?: localReplies?.photo(owner, WorkspacePhoto(key, reply.name, reply.mime))
            else -> null
        }
        return result.takeIf { epoch == accountEpoch }
    }

    fun removePhoto(key: String) {
        if (_state.value.busy || !canEditCompose()) return
        jobs.remove("photo-$key")?.cancel()
        photoBytes.remove(key)
        _state.update { it.copy(photos = it.photos.filterNot { item -> item.key == key }, composeEdited = true, publicationConfirmation = null) }
        scheduleLocalSave()
    }
    fun movePhoto(key: String, offset: Int) {
        if (_state.value.busy || !canEditCompose()) return
        _state.update { s ->
            val list = s.photos.toMutableList()
            val index = list.indexOfFirst { it.key == key }
            val target = index + offset
            if (index >= 0 && target in list.indices) list.add(target, list.removeAt(index))
            s.copy(photos = list, composeEdited = true, publicationConfirmation = null)
        }
        scheduleLocalSave()
    }

    fun saveComposeDraft() = command {
        if (!canEditCompose()) return@command
        val s = _state.value
        if (!s.draftLoaded || s.photos.any { it.mediaId == null }) {
            _state.update { it.copy(notice = "请先载入草稿并完成照片上传") }; return@command
        }
        val record = api.saveComposeDraft(draftVersion, s.composeTitle, s.composeText, s.photos.mapNotNull { it.mediaId }, s.composeEntry)
        draftVersion = record.version
        _state.update { it.copy(notice = "草稿已保存", composeEdited = false) }
        persistCompose()
    }
    fun restoreComposeDraft(replaceInput: Boolean = false) {
        if (_state.value.user == null || _state.value.draftLoading || _state.value.busy) return
        if (!localReady) { restoreLocalComposeDraft(_state.value.user!!.id); return }
        if (localPublicationKey != null) { canEditCompose(); return }
        val epoch = accountEpoch
        _state.update { it.copy(draftLoading = true) }
        task("draft") {
            try {
                var cursor = 0L
                var found: MobileRecord<DraftBody>? = null
                do {
                    val page = api.drafts(cursor)
                    found = page.items.firstOrNull { it.data.contextKind == "mnd.compose.community.v1" }
                    check(page.nextCursor == 0L || page.nextCursor > cursor)
                    cursor = page.nextCursor
                } while (found == null && cursor != 0L)
                val record = found
                val content = record?.let { api.json.decodeFromString<ComposeDraftContent>(it.data.text) }
                check(content == null || content.schema == 1) { "Unsupported draft schema" }
                draftVersion = record?.version ?: 0L
                if (replaceInput || !_state.value.composeEdited) {
                    _state.value.photos.forEach { jobs.remove("photo-${it.key}")?.cancel() }
                    photoBytes.clear()
                    _state.update { it.copy(composeTitle = content?.title.orEmpty(), composeText = content?.text.orEmpty(), composeEntry = content?.entry ?: EntryBody(""),
                        photos = record?.mediaIds.orEmpty().map { id -> PhotoAttachment(newKey(), "已保存照片", id) }, composeEdited = false) }
                }
                _state.update { it.copy(draftLoaded = true) }
                persistCompose()
            } finally { if (epoch == accountEpoch) _state.update { it.copy(draftLoading = false) } }
        }
    }
    private fun replyPath(destination: MobileDestination): String = when(destination) {
        is MobileDestination.Entry -> "entries/${destination.id}/comments"
        is MobileDestination.Pet -> "pets/${destination.id}/comments"
        is MobileDestination.Chat -> "${if(destination.group) "groups" else "conversations"}/${destination.id}/messages"
        else -> error("不是消息或评论入口")
    }
    private fun replyRoute(key: String): MobileDestination {
        val parts = key.split(':'); require(parts.size == 2)
        val id = parts[1].toLong(); require(id > 0)
        return when(parts[0]) { "pet" -> MobileDestination.Pet(id,true); "entry" -> MobileDestination.Entry(id); "chat" -> MobileDestination.Chat(id); "group" -> MobileDestination.Chat(id,true); else -> error("未知消息草稿") }
    }
    private fun replyWorkspace(): List<WorkspaceDraft> = (replyDrafts.keys + richReplies.keys + replyPhotos.keys + uncertainReplies).mapNotNull { destination ->
        val text = replyDrafts[destination].orEmpty(); val rich = richReplies[destination] ?: (0L to "")
        val photos = replyPhotos[destination].orEmpty()
        if (text.isEmpty() && rich.first == 0L && rich.second.isEmpty() && photos.isEmpty() && destination !in uncertainReplies) return@mapNotNull null
        val key = when(destination) { is MobileDestination.Pet -> "pet:${destination.id}"; is MobileDestination.Entry -> "entry:${destination.id}"; is MobileDestination.Chat -> "${if(destination.group) "group" else "chat"}:${destination.id}"; else -> return@mapNotNull null }
        WorkspaceDraft(key,mapOf("text" to text,"replyTo" to rich.first.toString(),"sticker" to rich.second),
            photos = photos.map { WorkspacePhoto(it.key,it.name,it.mime,it.mediaId) },uncertain = destination in uncertainReplies,
            requests = listOf(replyKeys.getOrPut(destination) { newKey() }))
    }
    private suspend fun persistReplies() {
        val storage = localReplies ?: return
        val epoch = accountEpoch; val owner = _state.value.user?.id ?: 0L
        replySaveMutex.withLock {
            if (!replyReady) throw LocalDraftException("本机消息草稿尚未恢复，原文件不会被覆盖")
            if (epoch != accountEpoch) throw CancellationException("Account changed")
            val drafts = replyWorkspace(); val bytes = replyPhotoBytes.toMap()
            withContext(NonCancellable) {
                val saved = storage.save(owner,replyRevision,drafts,bytes)
                if (epoch == accountEpoch) { replyRevision = saved.revision; _state.update { it.copy(replyDraftError = null,savedReplyDestinations = drafts.map { d -> replyRoute(d.key) },pendingOperations = pendingCommands()) } }
            }
            if (epoch != accountEpoch) throw CancellationException("Account changed")
        }
    }
    private fun scheduleReplySave() {
        _state.update { it.copy(savedReplyDestinations = replyWorkspace().map { d -> replyRoute(d.key) },pendingOperations = pendingCommands()) }
        if (localReplies == null || !replyReady) return
        task("reply-save") {
            try { persistReplies() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(replyDraftError = "本机消息草稿未保存：${e.mobileMessage()}，当前输入保留") } }
        }
    }
    fun retryReplySave() { scheduleReplySave() }
    fun useGuestReply() {
        val text = _state.value.guestReplySuggestion ?: return
        if (editReply(text)) _state.update { it.copy(guestReplySuggestion = null) }
    }
    fun replaceWithSavedReplies() {
        if (_state.value.busy || uncertainReplies.isNotEmpty()) { pickerFailed("请先核对原消息结果，再替换本机草稿"); return }
        jobs.filterKeys { it.startsWith("reply-") }.values.forEach { it.cancel() }
        replyDrafts.clear(); replyKeys.clear(); richReplies.clear(); replyPhotos.clear(); replyPhotoBytes.clear()
        replyReady = false
        _state.update { it.copy(reply = "",replyTo = 0,replySticker = "",replyPhotos = emptyList(),replyDraftReady = false,savedReplyDestinations = emptyList()) }
        restoreReplyDrafts()
    }
    fun restoreReplyDrafts() {
        val storage = localReplies ?: return
        if (replyReady || jobs["reply-restore"]?.isActive == true) return
        val epoch = accountEpoch; val owner = _state.value.user?.id ?: 0L
        _state.update { it.copy(replyDraftRestoring = true,replyDraftReady = false,replyDraftError = null) }
        task("reply-restore") {
            try {
                val saved = replySaveMutex.withLock { storage.load(owner) }
                val drafts = saved?.drafts.orEmpty().associateBy { replyRoute(it.key) }
                if (epoch != accountEpoch) return@task
                drafts.forEach { (destination,draft) ->
                    val key = draft.requests.singleOrNull() ?: newKey()
                    if (api.commandStatus(key)?.phase == "committed") return@forEach
                    replyDrafts[destination] = draft.values["text"].orEmpty()
                    richReplies[destination] = (draft.values["replyTo"]?.toLongOrNull() ?: 0L) to draft.values["sticker"].orEmpty()
                    replyKeys[destination] = key
                    if (draft.uncertain) uncertainReplies.add(destination)
                    replyPhotos[destination] = draft.photos.map { p -> PhotoAttachment(p.key,p.name,p.mediaId,error = if(p.mediaId == null) "原文件已保留，请按原编号重试上传" else null,mime = p.mime) }
                }
                replyRevision = saved?.revision ?: 0L; replyReady = true
                val destination = _state.value.destination
                replyKey = replyKeys.getOrPut(destination) { newKey() }
                _state.update { it.copy(reply = replyDrafts[destination].orEmpty(),replyTo = richReplies[destination]?.first ?: 0L,replySticker = richReplies[destination]?.second.orEmpty(),
                    replyPhotos = replyPhotos[destination].orEmpty(),replyDraftRestoring = false,replyDraftReady = true,savedReplyDestinations = replyWorkspace().map { d -> replyRoute(d.key) },pendingOperations = pendingCommands()) }
                pendingGuestReply?.takeIf { it.first == destination }?.let { (_,text) -> _state.update { it.copy(guestReplySuggestion = text) } }
                pendingGuestReply = null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (epoch == accountEpoch) _state.update { it.copy(replyDraftRestoring = false,replyDraftError = "本机消息草稿未恢复：${e.mobileMessage()}，原文件未被覆盖") } }
        }
    }
    suspend fun media(id: Long): ByteArray = api.media(id)
    override fun close() { providerAuth.close(); features.close(); super.close(); if (ownsApi) api.close() }
    private fun newKey(): String = Random.nextBytes(16).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
}

fun adoptionStateLabel(state: String): String = when (state) {
    "submitted" -> "申请已提交"; "needs_info" -> "等待补充"; "supplemented" -> "已补充资料"
    "approved" -> "申请已同意"; "rejected" -> "申请未通过"; "withdrawn" -> "已撤回"
    "trial_proposed" -> "试养提案待确认"; "trial_active" -> "试养中"; "handover_pending" -> "等待另一方确认交接"
    "waitlisted" -> "候补中，不会自动进入试养"; "closed_other_placement" -> "本轮已由其他安排完成交接"
    "reserved_trial" -> "当前已有试养安排"; "placed" -> "本轮交接已完成"
    "completed" -> "交接已完成"; "return_requested" -> "退养协商中"; "return_agreed" -> "接回安排已确认，实际接回待确认"; "returned" -> "双方已确认接回"; else -> "状态待核对"
}
