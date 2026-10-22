package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class MobileTab(val label: String) { Community("社区"), Adoption("领养"), Publish("发布"), Messages("消息"), Profile("我的") }

sealed interface MobileDestination {
    data object Home : MobileDestination
    data class Pet(val id: Long) : MobileDestination
    data class Entry(val id: Long) : MobileDestination
    data class Chat(val id: Long, val group: Boolean = false) : MobileDestination
    data class Case(val id: Long) : MobileDestination
    data object Help : MobileDestination
    data object Applications : MobileDestination
    data class Application(val id: Long) : MobileDestination
    data object Notifications : MobileDestination
}

data class PageState<T>(val items: List<MobileRecord<T>> = emptyList(), val next: Long = 0, val loading: Boolean = false,
    val loaded: Boolean = false, val error: String? = null)

data class AuthForm(val visible: Boolean = false, val username: String = "", val password: String = "", val error: String? = null)

data class AdoptionCommand(val record: MobileRecord<AdoptionBody>, val action: String, val label: String, val note: String)

data class MobileState(
    val restoring: Boolean = true, val user: MobileUser? = null, val tab: MobileTab = MobileTab.Community,
    val destination: MobileDestination = MobileDestination.Home, val auth: AuthForm = AuthForm(),
    val capabilities: LoginCapabilities = LoginCapabilities(), val capabilityError: String? = null,
    val eligibility: Eligibility? = null, val pendingOperations: List<CommandRecovery> = emptyList(),
    val publicationConfirmation: EntryInput? = null,
    val entries: PageState<EntryBody> = PageState(), val pets: PageState<PetBody> = PageState(),
    val myPets: PageState<PetBody> = PageState(), val applications: PageState<AdoptionBody> = PageState(),
    val cases: PageState<CaseBody> = PageState(), val conversations: PageState<ConversationBody> = PageState(),
    val groups: PageState<GroupBody> = PageState(), val messages: PageState<MessageBody> = PageState(),
    val comments: PageState<CommentBody> = PageState(), val notifications: PageState<NotificationBody> = PageState(),
    val pet: MobileRecord<PetBody>? = null, val entry: MobileRecord<EntryBody>? = null,
    val application: MobileRecord<AdoptionBody>? = null, val case: MobileRecord<CaseBody>? = null,
    val conversation: MobileRecord<ConversationBody>? = null, val profile: MobileProfile? = null,
    val metrics: Map<Long, EntryMetrics> = emptyMap(), val inbox: InboxSummary? = null,
    val emoji: List<EmojiItem> = emptyList(), val detailLoading: Boolean = false, val detailError: String? = null,
    val busy: Boolean = false, val notice: String? = null, val conflict: Boolean = false,
    val applying: Boolean = false, val applicationText: String = "", val actionNote: String = "",
    val reply: String = "", val composeTitle: String = "", val composeText: String = "",
    val photos: List<PhotoAttachment> = emptyList(), val draftLoading: Boolean = false, val draftLoaded: Boolean = false,
    val composeEdited: Boolean = false,
    val confirmation: AdoptionCommand? = null,
)

/** One portable state owner. Rotation, tab layout and iPad resizing never create a second business store. */
class MobileStore(val api: MobileApi, private val ownsApi: Boolean = true) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(MobileState())
    val state = _state.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()
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
    private val uploads = Semaphore(2)

    init {
        scope.launch {
            api.user.collect { user ->
                if (_state.value.user?.id != user?.id) accountChanged(user)
            }
        }
        refreshCapabilities()
        scope.launch {
            try { val emoji = api.expressions().emoji; _state.update { it.copy(emoji = emoji) } }
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
        accountEpoch++
        jobs.filterKeys { it != "restore" && it != "capabilities" }.values.forEach { it.cancel() }
        jobs.keys.removeAll { it != "restore" && it != "capabilities" }
        history.clear()
        draftVersion = 0L
        photoBytes.clear()
        replyDrafts.clear()
        replyKeys.clear()
        detailRevision++
        _state.update { old ->
            MobileState(restoring = old.restoring, user = user, tab = old.tab,
                destination = old.destination, auth = if (user != null) AuthForm() else old.auth,
                capabilities = old.capabilities, capabilityError = old.capabilityError, emoji = old.emoji)
        }
        refreshHome()
        reloadDetail()
        if (user != null) refreshEligibility()
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
        history.clear()
        detailRevision++
        _state.update { it.copy(tab = tab, destination = MobileDestination.Home, detailError = null, applying = false, confirmation = null) }
        if (tab in listOf(MobileTab.Publish, MobileTab.Messages, MobileTab.Profile) && _state.value.user == null) showLogin()
        refreshHome()
    }

    fun open(destination: MobileDestination) {
        if (_state.value.busy || destination == _state.value.destination) return
        history.add(_state.value.destination)
        detailRevision++
        replyKey = replyKeys.getOrPut(destination) { newKey() }
        _state.update { it.copy(destination = destination, pet = null, entry = null, case = null, application = null,
            conversation = null, comments = PageState(), messages = PageState(), reply = replyDrafts[destination].orEmpty(), actionNote = "",
            applying = false, applicationText = "", detailError = null, conflict = false, confirmation = null) }
        reloadDetail()
    }

    fun back(): Boolean {
        val s = _state.value
        if (s.busy) return true
        if (s.auth.visible) { cancelLogin(); return true }
        if (s.confirmation != null) { dismissConfirmation(); return true }
        if (s.applying) { _state.update { it.copy(applying = false) }; return true }
        if (s.destination == MobileDestination.Home) return false
        detailRevision++
        val previous = history.removeLastOrNull() ?: MobileDestination.Home
        replyKey = replyKeys.getOrPut(previous) { newKey() }
        _state.update { it.copy(destination = previous,
            detailError = null, confirmation = null, applying = false, reply = replyDrafts[previous].orEmpty()) }
        reloadDetail()
        return true
    }

    fun showLogin() { _state.update { it.copy(auth = it.auth.copy(visible = true, error = null)) }; refreshCapabilities() }
    fun cancelLogin() { if (_state.value.busy) return; resumeApply = null; _state.update { it.copy(auth = AuthForm()) } }
    fun editAuth(form: AuthForm) { _state.update { it.copy(auth = form.copy(error = null)) } }
    fun clearNotice() { _state.update { it.copy(notice = null) } }
    fun editApplication(text: String) { if (withinLimit(text, 2000)) _state.update { it.copy(applicationText = text) } }
    fun editActionNote(text: String) { if (withinLimit(text, 3000)) _state.update { it.copy(actionNote = text) } }
    fun editReply(text: String): Boolean {
        if (_state.value.busy || !withinLimit(text, 2000)) return false
        if (text == _state.value.reply) return true
        replyKey = newKey()
        replyKeys[_state.value.destination] = replyKey
        replyDrafts[_state.value.destination] = text
        _state.update { it.copy(reply = text) }
        return true
    }
    fun editCompose(title: String, text: String) {
        if (withinLimit(title, 80) && withinLimit(text, 5000)) _state.update { it.copy(composeTitle = title, composeText = text, composeEdited = true) }
    }
    private fun withinLimit(text: String, max: Int): Boolean {
        // Reject the edit as a unit; never split a surrogate, emoji sequence or IME composition with take().
        val count = text.count { it.code !in 0xDC00..0xDFFF }
        if (count <= max) return true
        _state.update { it.copy(notice = "最多可输入 $max 个字符") }
        return false
    }

    fun login() {
        val form = _state.value.auth
        val guestReply = _state.value.takeIf { it.user == null }?.reply.orEmpty()
        val originalDestination = _state.value.destination
        if (_state.value.busy) return
        // Login changes the account epoch. It cannot be a cancellable resource-loading job.
        _state.update { it.copy(busy = true) }
        scope.launch {
            try {
                val session = api.password(form.username.trim(), form.password)
                if (_state.value.user?.id != session.user.id) accountChanged(session.user)
                if (guestReply.isNotEmpty() && originalDestination == _state.value.destination && originalDestination is MobileDestination.Entry) editReply(guestReply)
                _state.update { it.copy(auth = AuthForm(), notice = "登录成功，请确认后继续原任务") }
                resumeApply?.let { id ->
                    resumeApply = null
                    if (_state.value.destination == MobileDestination.Pet(id)) _state.update { it.copy(applying = true) }
                }
                reloadDetail()
                refreshHome()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(auth = it.auth.copy(error = e.mobileMessage())) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    fun logout() = command {
        api.logout()
        if (_state.value.user != null) accountChanged(null)
        _state.update { it.copy(tab = MobileTab.Community, destination = MobileDestination.Home, notice = "已退出登录") }
        refreshHome()
    }

    private fun command(block: suspend () -> Unit) {
        if (_state.value.busy || sending) return
        _state.update { it.copy(busy = true, notice = null, conflict = false) }
        scope.launch {
            val epoch = accountEpoch
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == accountEpoch) _state.update { it.copy(notice = e.mobileMessage(), conflict = e is MobileApiException && e.status == 409) }
            } finally { if (epoch == accountEpoch) _state.update { it.copy(busy = false, pendingOperations = api.recoverableCommands()) } }
        }
    }

    fun refreshHome(more: Boolean = false) {
        when (_state.value.tab) {
            MobileTab.Community -> loadPage("entries", more, { it.entries }, { s, p -> s.copy(entries = p) }, { api.entries(it) }) {
                val metrics = api.metrics(_state.value.entries.items.map { row -> row.id }).associateBy(EntryMetrics::entryId)
                _state.update { it.copy(metrics = metrics) }
            }
            MobileTab.Adoption -> loadPage("pets", more, { it.pets }, { s, p -> s.copy(pets = p) }, { api.pets(it) }, filter = { it.data.adoptionAvailable })
            MobileTab.Publish -> if (_state.value.user != null && !_state.value.draftLoaded) restoreComposeDraft()
            MobileTab.Messages -> if (_state.value.user != null) {
                loadPage("conversations", more, { it.conversations }, { s, p -> s.copy(conversations = p) }, { api.conversations(it) })
                loadPage("groups", more, { it.groups }, { s, p -> s.copy(groups = p) }, { api.groups(it) })
                task("inbox") { val inbox = api.inbox(); _state.update { it.copy(inbox = inbox) } }
            }
            MobileTab.Profile -> _state.value.user?.let { user ->
                task("profile") { val profile = api.profile(); _state.update { it.copy(profile = profile) } }
                loadPage("my-pets", more, { it.myPets }, { s, p -> s.copy(myPets = p) }, { api.pets(it, user.id) })
            }
        }
    }

    private fun <T> loadPage(key: String, more: Boolean, get: (MobileState) -> PageState<T>,
        put: (MobileState, PageState<T>) -> MobileState, fetch: suspend (Long) -> MobilePage<T>,
        filter: (MobileRecord<T>) -> Boolean = { true }, after: suspend () -> Unit = {}) {
        val old = get(_state.value)
        val epoch = accountEpoch
        if (old.loading || more && (old.next == 0L || !old.loaded)) return
        _state.update { put(it, (if (more) old else PageState()).copy(loading = true)) }
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
                _state.update { put(it, PageState(((if (more) old.items else emptyList()) + selected).distinctBy { row -> row.id }, cursor, loaded = true)) }
                after()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == accountEpoch) _state.update { put(it,
                    if (e is MobileApiException && e.status in listOf(401, 403, 404)) PageState(error = e.mobileMessage())
                    else get(it).copy(loading = false, error = e.mobileMessage())) }
            }
        }
    }

    fun reloadDetail(more: Boolean = false) {
        val s = _state.value
        val destination = s.destination
        val revision = detailRevision
        fun current() = revision == detailRevision && destination == _state.value.destination
        when (destination) {
            MobileDestination.Home -> Unit
            MobileDestination.Help -> loadPage("cases", more, { it.cases }, { state, p -> state.copy(cases = p) }, { api.cases(it) })
            MobileDestination.Applications -> if (s.user == null) showLogin() else loadPage("applications", more, { it.applications }, { state, p -> state.copy(applications = p) }, { api.adoptions(it) })
            MobileDestination.Notifications -> if (s.user == null) showLogin() else loadPage("notifications", more, { it.notifications }, { state, p -> state.copy(notifications = p) }, { api.notifications(it) })
            else -> {
                _state.update { it.copy(detailLoading = true, detailError = null, conflict = false, confirmation = null) }
                task("detail") {
                    try {
                        when (destination) {
                            is MobileDestination.Pet -> { val record = api.pet(destination.id); if (current()) _state.update { it.copy(pet = record) } }
                            is MobileDestination.Entry -> {
                                val record = api.entry(destination.id)
                                val metrics = api.metrics(listOf(destination.id))
                                if (current()) _state.update { it.copy(entry = record, metrics = it.metrics + metrics.associateBy(EntryMetrics::entryId)) }
                                if (current()) loadComments()
                            }
                            is MobileDestination.Application -> { val record = api.adoption(destination.id); if (current()) _state.update { it.copy(application = record) } }
                            is MobileDestination.Case -> { val record = api.case(destination.id); if (current()) _state.update { it.copy(case = record) } }
                            is MobileDestination.Chat -> {
                                if (!destination.group) {
                                    val record = api.conversation(destination.id)
                                    if (current()) _state.update { it.copy(conversation = record) }
                                }
                                if (current()) loadMessages(more)
                            }
                            else -> Unit
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        if (current()) _state.update { it.copy(pet = null, entry = null, application = null, case = null,
                            conversation = null, comments = PageState(), messages = PageState(), detailError = e.mobileMessage()) }
                    } finally { if (current()) _state.update { it.copy(detailLoading = false) } }
                }
            }
        }
    }

    fun loadComments(more: Boolean = false) {
        val id = (_state.value.destination as? MobileDestination.Entry)?.id ?: return
        loadPage("comments", more, { it.comments }, { s, p -> s.copy(comments = p) }, { api.comments(id, it) })
    }
    fun loadMessages(more: Boolean = false) {
        val chat = _state.value.destination as? MobileDestination.Chat ?: return
        loadPage("messages", more, { it.messages }, { s, p -> s.copy(messages = p) }, { api.messages(chat.id, chat.group, it) })
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
        val record = api.apply(pet.id, s.applicationText.trim())
        _state.update { it.copy(applying = false, applicationText = "", busy = false) }
        open(MobileDestination.Application(record.id))
    }

    fun adoptionActions(): List<Pair<String, String>> {
        val s = _state.value
        val record = s.application ?: return emptyList()
        val actor = s.user?.id ?: return emptyList()
        val publisher = actor == record.data.publisherId
        if (!publisher && actor != record.data.applicantId) return emptyList()
        return buildList {
            if (record.state in listOf("submitted", "supplemented")) {
                if (publisher) { add("approve" to "同意申请"); add("request_supplement" to "请对方补充"); add("reject" to "拒绝申请") }
                else add("withdraw" to "撤回申请")
            }
            if (record.state == "needs_info" && !publisher) { add("supplement" to "提交补充"); add("withdraw" to "撤回申请") }
            if (record.state == "trial_proposed") {
                if (actor !in record.data.trialConfirmations) add("confirm_trial" to "确认试养条款")
                add("decline_trial" to "拒绝本次试养提案")
            }
            if (record.state in listOf("approved", "trial_active", "handover_pending") && actor !in record.data.handoverConfirmations)
                add("confirm_handover" to "确认已经完成交接")
            if (record.state in listOf("trial_active", "handover_pending", "completed", "return_requested")) {
                add("support" to "请求协助"); add("request_return" to "发起退养协商")
            }
            if (!publisher && record.state in listOf("approved", "trial_proposed")) add("withdraw" to "撤回申请")
        }
    }
    fun requestAdoptionAction(action: String, label: String) {
        if (!requireInteractionContract()) return
        val s = _state.value
        val record = s.application ?: return
        if (adoptionActions().none { it.first == action }) return
        if (action in listOf("supplement", "request_supplement", "support", "request_return") && s.actionNote.isBlank()) {
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

    fun toggleLike() {
        val s = _state.value
        if (s.user == null) { showLogin(); return }
        if (!requireInteractionContract()) return
        val record = s.entry ?: return
        val metrics = s.metrics[record.id] ?: return
        command {
            api.react(record.id, !metrics.viewerLiked)
            val latest = api.metrics(listOf(record.id))
            _state.update { it.copy(metrics = it.metrics + latest.associateBy(EntryMetrics::entryId)) }
        }
    }
    fun sendReply() {
        val s = _state.value
        if (s.user == null) { showLogin(); return }
        if (!requireInteractionContract()) return
        if (s.reply.isBlank() || sending || s.busy) return
        val key = replyKey
        command {
            sending = true
            try {
                when (val destination = s.destination) {
                    is MobileDestination.Entry -> { api.comment(destination.id, s.reply.trim(), key); loadComments() }
                    is MobileDestination.Chat -> { api.send(destination.id, destination.group, s.reply.trim(), key); loadMessages() }
                    else -> return@command
                }
                replyKey = newKey()
                replyDrafts.remove(s.destination)
                replyKeys.remove(s.destination)
                _state.update { it.copy(reply = "") }
            } finally { sending = false }
        }
    }
    fun markChatRead() = command {
        val s = _state.value
        val chat = s.destination as? MobileDestination.Chat ?: return@command
        val through = s.messages.items.maxOfOrNull { it.id } ?: return@command
        api.readMessages(chat.id, chat.group, through)
        val inbox = api.inbox()
        _state.update { it.copy(inbox = inbox, notice = "已标为已读") }
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
    }
    fun publish() {
        val s = _state.value
        if (s.user == null) { showLogin(); return }
        if (!requireInteractionContract()) return
        if (s.composeText.isBlank() && s.photos.isEmpty()) return
        if (s.photos.any { it.mediaId == null }) { _state.update { it.copy(notice = "请先完成照片上传，或移除上传失败的照片") }; return }
        command {
            val input = EntryInput(EntryBody(text = s.composeText.trim(), title = s.composeTitle.trim()), mediaIds = s.photos.mapNotNull { it.mediaId }, clientRequestId = newRequestKey())
            val preview = api.previewPublication(input)
            _state.update { it.copy(publicationConfirmation = input.copy(declaration = PublicationDeclaration(preview.manifestHash, preview.origin)), notice = "请核对当前内容与照片，并确认来源声明后发布") }
        }
    }

    fun confirmPublication(origin: String, confirmed: Boolean) = command {
            if (!confirmed || !requireInteractionContract()) return@command
            val s = _state.value
            val pending = s.publicationConfirmation ?: return@command
            if (pending.entry.text != s.composeText.trim() || pending.entry.title != s.composeTitle.trim() || pending.mediaIds != s.photos.mapNotNull { it.mediaId }) {
                _state.update { it.copy(publicationConfirmation = null, notice = "内容已改变，请重新核对发布内容") }; return@command
            }
            // A changed origin invalidates the old manifest and requires a new preview.
            if (origin != pending.declaration.origin) {
                val revised = pending.copy(declaration = PublicationDeclaration(origin = origin))
                val preview = api.previewPublication(revised)
                _state.update { it.copy(publicationConfirmation = revised.copy(declaration = PublicationDeclaration(preview.manifestHash, origin)), notice = "来源已改变，请重新确认当前声明") }
                return@command
            }
            val record = api.publish(pending.copy(declaration = pending.declaration.copy(confirmed = true)))
            var notice = "已发布到社区"
            try { draftVersion = api.saveComposeDraft(draftVersion, "", "", emptyList()).version }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { notice = "已发布到社区，云端旧草稿暂未清理" }
            photoBytes.clear()
            _state.update { it.copy(composeTitle = "", composeText = "", photos = emptyList(), composeEdited = false, publicationConfirmation = null, busy = false, notice = notice) }
            select(MobileTab.Community)
            open(MobileDestination.Entry(record.id))
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
        _state.update { it.copy(eligibility = qualification, pendingOperations = api.recoverableCommands()) }
    }
    fun reconcilePending(key: String) = command {
        try {
            val receipt = api.reconcileOperation(key)
            _state.update { it.copy(pendingOperations = api.recoverableCommands(), notice = if (receipt.state == "committed") "已核对：原操作已成功，请查看原记录" else "原操作仍在处理中") }
        } catch (e: MobileApiException) {
            if (e.status == 404) _state.update { it.copy(pendingOperations = api.recoverableCommands(), notice = "暂未查到回执，结果仍待核对，请勿重复提交") } else throw e
        }
    }
    fun retryPending(key: String) = command {
        api.retryOriginal(key)
        _state.update { it.copy(pendingOperations = api.recoverableCommands(),notice = "已按原请求编号核对并重试，请查看原记录") }
    }

    fun addPhotos(selected: List<SelectedPhoto>) {
        if (_state.value.user == null) { showLogin(); return }
        val free = 9 - _state.value.photos.size
        if (selected.size > free) _state.update { it.copy(notice = "每次发布最多 9 张照片") }
        selected.take(free.coerceAtLeast(0)).forEach { photo ->
            if (photo.mime !in listOf("image/jpeg", "image/png") || photo.bytes.size !in 1..10 * 1024 * 1024) {
                pickerFailed("请选择不超过 10 MiB 的 JPEG 或 PNG 照片")
            } else {
                val key = newKey()
                photoBytes[key] = photo
                _state.update { it.copy(photos = it.photos + PhotoAttachment(key, photo.name), composeEdited = true) }
                retryPhoto(key)
            }
        }
    }

    fun retryPhoto(key: String) {
        val photo = photoBytes[key] ?: return
        if (_state.value.photos.find { it.key == key }?.uploading != false) return
        val epoch = accountEpoch
        _state.update { it.copy(photos = it.photos.map { item -> if (item.key == key) item.copy(uploading = true, error = null) else item }) }
        task("photo-$key") {
            try {
                val result = uploads.withPermit { api.upload(photo, key) }
                if (epoch == accountEpoch) {
                    photoBytes.remove(key)
                    _state.update { it.copy(photos = it.photos.map { item -> if (item.key == key) item.copy(mediaId = result.id, uploading = false) else item }) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == accountEpoch) _state.update { it.copy(photos = it.photos.map { item -> if (item.key == key) item.copy(uploading = false, error = e.mobileMessage()) else item }) }
            }
        }
    }

    fun removePhoto(key: String) {
        if (_state.value.busy) return
        jobs.remove("photo-$key")?.cancel()
        photoBytes.remove(key)
        _state.update { it.copy(photos = it.photos.filterNot { item -> item.key == key }, composeEdited = true) }
    }
    fun movePhoto(key: String, offset: Int) {
        if (_state.value.busy) return
        _state.update { s ->
            val list = s.photos.toMutableList()
            val index = list.indexOfFirst { it.key == key }
            val target = index + offset
            if (index >= 0 && target in list.indices) list.add(target, list.removeAt(index))
            s.copy(photos = list, composeEdited = true)
        }
    }

    fun saveComposeDraft() = command {
        val s = _state.value
        if (!s.draftLoaded || s.photos.any { it.mediaId == null }) {
            _state.update { it.copy(notice = "请先载入草稿并完成照片上传") }; return@command
        }
        val record = api.saveComposeDraft(draftVersion, s.composeTitle, s.composeText, s.photos.mapNotNull { it.mediaId })
        draftVersion = record.version
        _state.update { it.copy(notice = "草稿已保存", composeEdited = false) }
    }
    fun restoreComposeDraft(replaceInput: Boolean = false) {
        if (_state.value.user == null || _state.value.draftLoading || _state.value.busy) return
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
                if (content != null && (replaceInput || !_state.value.composeEdited)) {
                    _state.value.photos.forEach { jobs.remove("photo-${it.key}")?.cancel() }
                    photoBytes.clear()
                    _state.update { it.copy(composeTitle = content.title, composeText = content.text,
                        photos = record?.mediaIds.orEmpty().map { id -> PhotoAttachment("restored-$id", "已保存照片", id) }, composeEdited = false) }
                }
                _state.update { it.copy(draftLoaded = true) }
            } finally { if (epoch == accountEpoch) _state.update { it.copy(draftLoading = false) } }
        }
    }
    suspend fun media(id: Long): ByteArray = api.media(id)
    fun close() { scope.cancel(); if (ownsApi) api.close() }
    private fun newKey(): String = Random.nextBytes(16).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
}

fun adoptionStateLabel(state: String): String = when (state) {
    "submitted" -> "申请已提交"; "needs_info" -> "等待补充"; "supplemented" -> "已补充资料"
    "approved" -> "申请已同意"; "rejected" -> "申请未通过"; "withdrawn" -> "已撤回"
    "trial_proposed" -> "试养提案待确认"; "trial_active" -> "试养中"; "handover_pending" -> "等待另一方确认交接"
    "completed" -> "交接已完成"; "return_requested" -> "退养协商中"; else -> "状态待核对"
}
