package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 功能路由只包含资源标识；临时凭证不进入路由、表单快照或普通偏好。 */
enum class FeaturePage(val title: String) {
    EntryEdit("编辑动态"), Friends("好友与宠友圈"), Member("宠友资料"), ProfileEdit("编辑资料"), PetEdit("宠物档案"), PetDiary("独立图文日记"),
    Collaborators("协作与邀请"), Invitations("收到与发出的邀请"), GroupCreate("创建群聊"), Group("群聊管理"), GroupDirectory("发现宠友群"), GroupPreview("群简介与加入申请"), Content("关注与求助订阅"),
    Appointments("预约"), Appointment("预约详情"), AppointmentEdit("预约安排"),
    CaseEdit("走失与拾获发布"), CaseWork("线索与认领"), Claim("私密认领材料"),
    AdoptionWork("领养照护与协议"), ContractEdit("协议提案"), ReturnEdit("安全接回安排"),
    Offers("有偿信息"), Offer("披露详情"), OfferEdit("编辑披露"),
    Settings("通知设置"), Nearby("附近提醒"), Map("互助地图"), Reviews("核实与举报"), Review("核实事项"),
    Safety("安全与隐私求助"), DataRequests("资料与账号请求"), Security("账号安全"),
    Recovery("待核对操作"), Expressions("表情与贴纸"), Rules("规则与权益")
}
data class FeatureRoute(val page: FeaturePage, val id: Long = 0, val kind: String = "", val peer: Long = 0)
data class FeatureForm(val values: Map<String, String> = emptyMap(), val baseVersion: Long? = null)
data class FeatureConfirmation(val title: String, val summary: String)
data class RestrictedReference(val type: String, val clientRequestId: String, val id: Long)
data class FeatureState(
    val route: FeatureRoute? = null, val forms: Map<FeatureRoute, FeatureForm> = emptyMap(),
    val loading: Boolean = false, val busy: Boolean = false, val error: String? = null, val notice: String? = null,
    val conflict: Boolean = false, val confirmation: FeatureConfirmation? = null,
    val mapCases: PageState<CaseBody> = PageState(), val diary: PageState<EntryBody> = PageState(), val directory: PageState<GroupDirectoryBody> = PageState(), val groupPreview: MobileRecord<GroupDirectoryBody>? = null, val contentSubscriptions: MobileRecord<ContentSubscriptions>? = null,
    val friends: PageState<FriendBody> = PageState(), val invitations: PageState<InvitationBody> = PageState(),
    val appointments: PageState<AppointmentBody> = PageState(), val offers: PageState<OfferBody> = PageState(),
    val claims: PageState<ClaimBody> = PageState(), val leads: PageState<LeadBody> = PageState(),
    val care: PageState<CareBody> = PageState(), val reviews: PageState<ReviewBody> = PageState(),
    val entry: MobileRecord<EntryBody>? = null, val member: PublicMember? = null, val profile: MobileProfile? = null, val pet: MobileRecord<PetBody>? = null,
    val group: MobileRecord<GroupBody>? = null, val appointment: MobileRecord<AppointmentBody>? = null,
    val case: MobileRecord<CaseBody>? = null, val claim: MobileRecord<ClaimBody>? = null,
    val adoption: MobileRecord<AdoptionBody>? = null, val offer: MobileRecord<OfferBody>? = null,
    val review: MobileRecord<ReviewBody>? = null, val verifications: List<VerificationSummary> = emptyList(),
    val preference: MobileRecord<PreferenceBody>? = null, val subscription: MobileRecord<SubscriptionBody>? = null,
    val expressions: MobileRecord<ExpressionBody>? = null, val catalog: ExpressionCatalog = ExpressionCatalog(),
    val methods: IdentityMethods? = null, val eligibility: Eligibility? = null, val guardians: List<MobileRecord<GuardianBody>> = emptyList(),
    val restricted: List<RestrictedReference> = emptyList(), val lookupRequestId: String? = null, val dataStatus: DataRequestStatus? = null, val rightsStatus: RightsReceipt? = null,
    val pending: List<CommandRecovery> = emptyList(), val otpWait: Int = 0,
    val photos: Map<FeatureRoute,List<PhotoAttachment>> = emptyMap(),
    val unconfirmedResult: Boolean = false,
    val draftRestoring: Boolean = false, val draftReady: Boolean = true, val draftError: String? = null,
    val draftRoutes: List<FeatureRoute> = emptyList(),
)

/** 与 MobileStore 同生命周期的功能状态；所有 Android 面板共用此实例。 */
class MobileFeatureStore(val api: MobileApi, parentScope: CoroutineScope, private val localDrafts: LocalWorkspaceDrafts? = null) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val mutable = MutableStateFlow(FeatureState(draftRestoring = localDrafts != null,draftReady = localDrafts == null))
    val state = mutable.asStateFlow()
    val sensitive = SensitiveActionStore(api, scope)
    private val history = mutableListOf<FeatureRoute>()
    private var loading: Job? = null
    private var commandJob: Job? = null
    private var countdown: Job? = null
    private var revision = 0L
    private var epoch = 0L
    private var owner = api.user.value?.id
    private var pendingAction: (suspend () -> Unit)? = null
    private val safetyReferences = mutableMapOf<FeatureRoute,RestrictedReference>()
    private var pendingSafetyReference: RestrictedReference? = null
    private val photoBytes = mutableMapOf<String,SelectedPhoto>()
    private val photoJobs = mutableMapOf<String,Job>()
    private val photoScopes = mutableMapOf<String,Pair<Boolean,Boolean>>()
    private val uncertainRoutes = mutableSetOf<FeatureRoute?>()
    private val uncertainRequests = mutableMapOf<String,FeatureRoute?>()
    private val draftMutex = Mutex()
    private val safetyMutex = Mutex()
    private var draftRevision = 0L
    private var draftReady = localDrafts == null
    private var draftSave: Job? = null
    private var draftRestore: Job? = null

    init {
        restoreDrafts()
        scope.launch { sensitive.state.collect { current ->
            val key = current.clientRequestId ?: return@collect
            val route = uncertainRequests[key] ?: return@collect
            if (current.stage == SensitiveStage.Complete) {
                uncertainRequests.remove(key); uncertainRoutes.remove(route); clearForm(route)
                mutable.update { it.copy(unconfirmedResult = it.route in uncertainRoutes,restricted = restricted(),notice = "原事项已提交，请在受限进度或当前记录核对结果") }
                if (mutable.value.route == route) reload()
            } else if (current.stage == SensitiveStage.Failed) {
                // 写 API 已将不确定结果分类为 ResultUnknown；Failed 允许修改后重新验证。
                uncertainRequests.remove(key); uncertainRoutes.remove(route)
                mutable.update { it.copy(unconfirmedResult = it.route in uncertainRoutes) }; scheduleDraftSave()
            }
        } }
        scope.launch { api.user.collect { user ->
            if (owner != user?.id) {
                val resume = mutable.value.route
                owner = user?.id; epoch++; revision++
                loading?.cancel(); commandJob?.cancel(); countdown?.cancel()
                pendingAction = null; safetyReferences.clear(); pendingSafetyReference = null; history.clear()
                photoJobs.values.forEach { it.cancel() }; photoJobs.clear(); photoBytes.clear()
                photoScopes.clear(); draftSave?.cancel(); draftRestore?.cancel()
                draftRevision = 0L; draftReady = localDrafts == null
                uncertainRoutes.clear(); uncertainRequests.clear()
                mutable.value = FeatureState(route = resume,draftRestoring = localDrafts != null,draftReady = localDrafts == null)
                restoreDrafts()
                if (resume != null) reload()
            }
        } }
    }
    fun loadContentSubscriptions() { if(owner == null) return; val currentEpoch = epoch; scope.launch { try { val p = api.contentSubscriptions(); if(epoch == currentEpoch) mutable.update { it.copy(contentSubscriptions = p) } } catch(e: CancellationException) { throw e } catch(e: Exception) { if(epoch == currentEpoch) notify(e.message ?: "暂无法读取关注状态") } } }
    fun field(key: String, fallback: String = ""): String = mutable.value.route?.let { mutable.value.forms[it]?.values?.get(key) } ?: fallback
    fun edit(key: String, value: String) {
        if (!canEditDraft() || mutable.value.busy || mutable.value.confirmation != null || mutable.value.route in uncertainRoutes || value.length > 10000) return
        val route = mutable.value.route ?: return
        mutable.update { s -> val form = s.forms[route] ?: FeatureForm(); s.copy(forms = s.forms + (route to form.copy(values = form.values + (key to value)))) }
        scheduleDraftSave()
    }
    fun version(fallback: Long = 0): Long = mutable.value.route?.let { mutable.value.forms[it]?.baseVersion } ?: fallback
    private fun capture(version: Long) {
        if (!draftReady) return
        val route = mutable.value.route ?: return
        mutable.update { s -> val form = s.forms[route] ?: FeatureForm();
            s.copy(forms = s.forms + (route to if (form.baseVersion == null) form.copy(baseVersion = version) else form)) }
    }
    fun open(route: FeatureRoute) {
        if (mutable.value.busy) return
        mutable.value.route?.takeIf { it != route }?.let(history::add)
        revision++; loading?.cancel(); dismissConfirmation()
        mutable.update { it.copy(route = route, loading = false, error = null, notice = null, conflict = false,
            groupPreview = null, mapCases = PageState(), diary = PageState(), entry = null, member = null, pet = null, group = null, appointment = null, case = null, claim = null, adoption = null,
            offer = null, review = null, verifications = emptyList(), claims = PageState(), leads = PageState(), care = PageState(),
            unconfirmedResult = route in uncertainRoutes) }
        reload()
        pendingSafetyReference?.let { safetyReferences[route] = it; pendingSafetyReference = null; scheduleDraftSave() }
    }
    fun back(): Boolean {
        if (mutable.value.route == null) return false
        if (mutable.value.busy) return true
        if (mutable.value.confirmation != null) { dismissConfirmation(); return true }
        val previous = history.removeLastOrNull()
        revision++; loading?.cancel(); mutable.update { it.copy(route = previous, loading = false, error = null, notice = null,groupPreview = null,mapCases = PageState(),diary = PageState(),entry = null,member = null,pet = null,group = null,appointment = null,case = null,claim = null,adoption = null,offer = null,review = null,unconfirmedResult = previous in uncertainRoutes) }
        if (previous != null) reload()
        return true
    }
    fun closeRoute() { if (!mutable.value.busy) { history.clear(); revision++; loading?.cancel(); mutable.update { it.copy(route = null, loading = false) } } }
    /** 仅在写请求收到明确成功回执之后使用。 */
    fun finishRoute() { mutable.value.route?.let(::clearForm); history.clear(); revision++; loading?.cancel(); mutable.update { it.copy(route = null, loading = false, confirmation = null) }; pendingAction = null }
    private fun restricted() = api.limitedRequestAccess().map { RestrictedReference(it.type,it.clientRequestId,it.id) }
    private fun access(reference: RestrictedReference): RestrictedAccess = api.limitedRequestAccess().first { it.type == reference.type && it.clientRequestId == reference.clientRequestId }
    fun useSafetyAccess(reference: RestrictedReference) { require(reference.type == "safety_ticket"); access(reference); pendingSafetyReference = reference }
    fun discardAndReload() {
        if (!canEditDraft() || mutable.value.route in uncertainRoutes) { notify("请先核对原操作，不能放弃结果未知的草稿"); return }
        val route = mutable.value.route ?: return
        clearForm(route)
        mutable.update { it.copy(conflict = false) }; reload()
    }
    private fun clearForm(route: FeatureRoute) {
        mutable.value.photos[route].orEmpty().forEach { photo -> photoJobs.remove(photo.key)?.cancel(); photoBytes.remove(photo.key); photoScopes.remove(photo.key) }
        safetyReferences.remove(route)
        mutable.update { it.copy(forms = it.forms - route, photos = it.photos - route) }
        scheduleDraftSave()
    }
    fun completeTo(route: FeatureRoute) {
        mutable.value.route?.let { clearForm(it) }
        history.clear(); pendingAction = null
        mutable.update { it.copy(route = route, confirmation = null, conflict = false, notice = "已保存，请核对当前记录") }
        reload()
    }
    fun notify(message: String) { mutable.update { it.copy(notice = message) } }
    fun prepareSensitive(intent: SensitiveIntent) {
        if (!canEditDraft() || mutable.value.route in uncertainRoutes) { notify("请先核对原事项结果，不能另建相同操作"); return }
        sensitive.prepare(intent)
    }
    fun confirmSensitive(agreed: Boolean) {
        val current = sensitive.state.value
        if (!agreed || current.stage !in listOf(SensitiveStage.Review,SensitiveStage.ReadyToSubmit) || current.scope != null && current.stage != SensitiveStage.ReadyToSubmit || mutable.value.busy) return
        val route = mutable.value.route ?: return
        val key = current.clientRequestId ?: return
        if (route in uncertainRoutes || !canEditDraft()) return
        val originalEpoch = epoch
        mutable.update { it.copy(busy = true) }
        scope.launch {
            try {
                uncertainRoutes.add(route); uncertainRequests[key] = route
                persistDrafts()
                if (epoch != originalEpoch) return@launch
                mutable.update { it.copy(unconfirmedResult = true) }
                sensitive.confirm(true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == originalEpoch) {
                    uncertainRoutes.remove(route); uncertainRequests.remove(key)
                    mutable.update { it.copy(draftError = "原事项尚未提交：${e.mobileMessage()}，请重试保存后确认") }
                }
            } finally { if(epoch == originalEpoch) mutable.update { it.copy(busy = false) } }
        }
    }
    fun confirm(title: String, summary: String, action: suspend () -> Unit) {
        if (mutable.value.busy || mutable.value.loading || mutable.value.confirmation != null) return
        reviewPrepared(title, summary, action)
    }
    fun reviewPrepared(title: String, summary: String, action: suspend () -> Unit) {
        if (mutable.value.loading || !canEditDraft()) return
        if (mutable.value.route in uncertainRoutes) { notify("原操作结果尚不明确，请核对记录；此页不会再次提交相同操作"); return }
        if (api.recoverableCommands().any { it.phase in listOf("sent", "result_unknown") }) {
            mutable.update { it.copy(notice = "请先核对原操作结果，再提交新操作", pending = api.recoverableCommands()) }; return
        }
        if (!photosReady()) { notify("请先完成此表单的照片上传，失败照片可重试或移除"); return }
        pendingAction = action
        mutable.update { it.copy(confirmation = FeatureConfirmation(title, summary), error = null) }
    }
    fun dismissConfirmation() { if (!mutable.value.busy) { pendingAction = null; mutable.update { it.copy(confirmation = null) } } }
    fun submitConfirmation() {
        if (mutable.value.loading) return
        if (mutable.value.route in uncertainRoutes) return
        val action = pendingAction ?: return
        runCommand { action(); pendingAction = null; mutable.update { it.copy(confirmation = null) } }
    }
    fun runCommand(action: suspend () -> Unit) {
        if (mutable.value.busy) return
        val originalEpoch = epoch
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        commandJob = scope.launch {
            var actionStarted = false
            try { if (pendingAction != null) persistDrafts(); actionStarted = true; action(); if (epoch == originalEpoch) mutable.update { it.copy(notice = it.notice ?: "操作已完成，请以当前记录为准") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == originalEpoch) {
                    val unknown = actionStarted && pendingAction != null && (e is OperationResultUnknown || e !is MobileApiException || e.status >= 500)
                    if (unknown) uncertainRoutes.add(mutable.value.route)
                    if (e is OperationResultUnknown) uncertainRequests[e.clientRequestId] = mutable.value.route
                    mutable.update { it.copy(error = if (unknown) "${e.mobileMessage()}。提交结果待核对，请勿重建请求" else e.mobileMessage(),
                        conflict = e is MobileApiException && e.status == 409, unconfirmedResult = unknown || it.unconfirmedResult,
                        pending = api.recoverableCommands()) }
                }
            } finally { if (epoch == originalEpoch) { mutable.update { it.copy(busy = false, pending = api.recoverableCommands()) }; scheduleDraftSave() } }
        }
    }
    fun reload(more: Boolean = false) {
        val route = mutable.value.route ?: return
        val currentRevision = ++revision
        val currentEpoch = epoch
        loading?.cancel()
        mutable.update { it.copy(loading = true, error = null) }
        loading = scope.launch {
            fun current() = revision == currentRevision && epoch == currentEpoch
            try {
                when (route.page) {
                    FeaturePage.EntryEdit -> { val p = api.entry(route.id); if(current()) { mutable.update { it.copy(entry = p) }; capture(p.version) } }
                    FeaturePage.PetDiary -> { val p: MobilePage<EntryBody> = api.get("entries",mapOf("reference_id" to route.id.toString(),"space" to "diary","cursor" to if(more) mutable.value.diary.next.toString() else "0"),authenticated = owner != null);if(current()) mutable.update { it.copy(diary = page(it.diary,p,more)) } }
                    FeaturePage.Friends -> { val p = api.friends(if (more) mutable.value.friends.next else 0); if (current()) mutable.update { it.copy(friends = page(it.friends, p, more)) } }
                    FeaturePage.Member -> { val p = api.member(route.id); if (current()) mutable.update { it.copy(member = p) }; if(owner != null) { val content = api.contentSubscriptions(); if(current()) mutable.update { it.copy(contentSubscriptions = content) } } }
                    FeaturePage.Content -> { val p = api.contentSubscriptions(); if(current()) mutable.update { it.copy(contentSubscriptions = p) } }
                    FeaturePage.GroupDirectory -> { val p = api.groupDirectory(if(more) mutable.value.directory.next else 0,field("q"),field("city"));if(current()) mutable.update { it.copy(directory = page(it.directory,p,more)) } }
                    FeaturePage.GroupPreview -> { val p = api.groupPreview(route.id); if(current()) mutable.update { it.copy(groupPreview = p) } }
                    FeaturePage.ProfileEdit -> { val p = api.profile(); if (current()) { mutable.update { it.copy(profile = p) }; capture(p.version) } }
                    FeaturePage.PetEdit, FeaturePage.Collaborators -> if (route.id > 0 && route.kind != "care") {
                        val p = api.pet(route.id); if (current()) { mutable.update { it.copy(pet = p) }; capture(p.version) }
                    }
                    FeaturePage.Invitations -> { val p = api.invitations(if (more) mutable.value.invitations.next else 0); if (current()) mutable.update { it.copy(invitations = page(it.invitations, p, more)) } }
                    FeaturePage.Group -> { val p = api.group(route.id); if (current()) { mutable.update { it.copy(group = p) }; capture(p.version) } }
                    FeaturePage.Appointments -> { val p = api.appointments(if (more) mutable.value.appointments.next else 0); if (current()) mutable.update { it.copy(appointments = page(it.appointments, p, more)) } }
                    FeaturePage.Appointment, FeaturePage.AppointmentEdit -> if (route.id > 0) {
                        val p = api.appointment(route.id); if (current()) { mutable.update { it.copy(appointment = p) }; capture(p.version) }
                    }
                    FeaturePage.CaseEdit -> if (route.id > 0) { val p = api.case(route.id); if (current()) { mutable.update { it.copy(case = p) }; capture(p.version) } }
                    FeaturePage.CaseWork -> {
                        val record = api.case(route.id)
                        if (current()) { mutable.update { it.copy(case = record) }; capture(record.version) }
                        if (owner == null) return@launch
                        // 公开案例与私密材料各自取数，私密失败不伪装为公开材料。
                        if (!more || mutable.value.claims.next != 0L) {
                            val claims = api.claims(route.id, if (more) mutable.value.claims.next else 0)
                            if (current()) mutable.update { it.copy(claims = page(it.claims, claims, more)) }
                        }
                        if (!more || mutable.value.leads.next != 0L) {
                            val leads = api.leads(route.id, if (more) mutable.value.leads.next else 0)
                            if (current()) mutable.update { it.copy(leads = page(it.leads, leads, more)) }
                        }
                    }
                    FeaturePage.Claim -> {
                        val p = api.claimRecord(route.id); val c = api.case(p.referenceId)
                        if (current()) { mutable.update { it.copy(claim = p, case = c) }; capture(p.version) }
                        val claims = api.claims(c.id); if(current()) mutable.update { it.copy(claims = page(PageState(),claims,false)) }
                    }
                    FeaturePage.AdoptionWork, FeaturePage.ContractEdit, FeaturePage.ReturnEdit -> {
                        if (route.kind != "care") {
                            val p = api.adoption(route.id); if (current()) { mutable.update { it.copy(adoption = p) }; capture(p.version) }
                        }
                        if (route.page == FeaturePage.AdoptionWork) {
                            val updates = api.careUpdates(route.id, if (more) mutable.value.care.next else 0)
                            if (current()) mutable.update { it.copy(care = page(it.care, updates, more)) }
                        }
                    }
                    FeaturePage.Offers -> { val p = api.offers(if (more) mutable.value.offers.next else 0); if (current()) mutable.update { it.copy(offers = page(it.offers, p, more)) } }
                    FeaturePage.Offer, FeaturePage.OfferEdit -> if (route.id > 0) {
                        val p = api.offer(route.id); if (current()) { mutable.update { it.copy(offer = p) }; capture(p.version) }
                        val checks = api.offerVerifications(route.id); if (current()) mutable.update { it.copy(verifications = checks) }
                    }
                    FeaturePage.Settings -> { val p = api.preferences().items.firstOrNull(); if (current()) { mutable.update { it.copy(preference = p) }; capture(p?.version ?: 0) } }
                    FeaturePage.Map -> { val p: MobilePage<CaseBody> = if(route.id > 0) MobilePage(items = listOf(api.case(route.id))) else api.get("cases",buildMap { put("cursor",if(more) mutable.value.mapCases.next.toString() else "0");put("city",field("city"));if(field("latitude").isNotBlank() && field("longitude").isNotBlank()) { put("latitude",field("latitude"));put("longitude",field("longitude"));put("radius_km","10") } },authenticated = owner != null);if(current()) mutable.update { it.copy(mapCases = page(it.mapCases,p,more)) } }
                    FeaturePage.Nearby -> { val p = api.subscriptions().items.firstOrNull(); if (current()) { mutable.update { it.copy(subscription = p) }; capture(p?.version ?: 0) } }
                    FeaturePage.Expressions -> {
                        val catalog = api.expressions(); val p = api.expressionState().items.firstOrNull()
                        if (current()) { mutable.update { it.copy(catalog = catalog, expressions = p) }; capture(p?.version ?: 0) }
                    }
                    FeaturePage.Reviews -> { val p = api.reviews(if (more) mutable.value.reviews.next else 0); if (current()) mutable.update { it.copy(reviews = page(it.reviews, p, more)) } }
                    FeaturePage.Review -> if (route.id > 0) { val p = api.reviewRecord(route.id); if (current()) { mutable.update { it.copy(review = p) }; capture(p.version) } }
                    FeaturePage.Security -> {
                        val methods = api.identityMethods(); val profile = api.profile(); val eligibility = api.eligibility()
                        if (current()) { mutable.update { it.copy(methods = methods, profile = profile, eligibility = eligibility) }; capture(profile.version) }
                        val guardians = api.guardians(); if (current()) mutable.update { it.copy(guardians = guardians) }
                    }
                    FeaturePage.Recovery -> if (current()) mutable.update { it.copy(pending = api.recoverableCommands(), restricted = restricted()) }
                    FeaturePage.DataRequests, FeaturePage.Safety -> if (current()) mutable.update { it.copy(restricted = restricted()) }
                    FeaturePage.GroupCreate, FeaturePage.Rules -> Unit
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (current()) mutable.update { it.copy(error = e.mobileMessage(),
                    // 权限被撤销时立即清除受保护的数据。
                    entry = if (denied(e)) null else it.entry, pet = if (denied(e)) null else it.pet,
                    profile = if (denied(e)) null else it.profile, member = if (denied(e)) null else it.member,
                    case = if (denied(e)) null else it.case, appointment = if (denied(e)) null else it.appointment,
                    offer = if (denied(e)) null else it.offer, review = if (denied(e)) null else it.review,
                    verifications = if (denied(e)) emptyList() else it.verifications,
                    claim = if (denied(e)) null else it.claim, group = if (denied(e)) null else it.group,
                    adoption = if (denied(e)) null else it.adoption,
                    claims = if (denied(e)) PageState() else it.claims, leads = if (denied(e)) PageState() else it.leads,
                    care = if (denied(e)) PageState() else it.care) }
            } finally { if (current()) mutable.update { it.copy(loading = false) } }
        }
    }
    private fun denied(e: Exception) = e is MobileApiException && e.status in listOf(401, 403, 404)
    private fun <T> page(old: PageState<T>, result: MobilePage<T>, more: Boolean): PageState<T> {
        check(result.nextCursor == 0L || result.nextCursor > if (more) old.next else 0L) { "分页游标未前进" }
        return PageState(((if (more) old.items else emptyList()) + result.items).distinctBy { it.id }, result.nextCursor, loaded = true)
    }
    fun sendOTP(email: String = "", phone: String = "", purpose: String) {
        if (mutable.value.otpWait > 0) return
        runCommand {
            val receipt = if (email.isNotBlank()) api.requestEmailOTP(email.trim(), purpose) else api.requestPhoneOTP(phone.trim(), purpose)
            mutable.update { it.copy(otpWait = receipt.retryAfterSeconds.coerceAtLeast(1), notice = "验证码已发送，有效期至 ${receipt.expiresAt}") }
            countdown?.cancel(); countdown = scope.launch {
                while (mutable.value.otpWait > 0) { delay(1000); mutable.update { it.copy(otpWait = (it.otpWait - 1).coerceAtLeast(0)) } }
            }
        }
    }
    suspend fun safetyToken(): String {
        // 只有 API 响应才能创建受限访问能力，禁止用本地随机数充当凭证。
        check(draftReady) { "本机草稿尚未恢复，请先重试读取" }
        val route = mutable.value.route ?: error("请先打开安全事项")
        return safetyToken(route)
    }
    private suspend fun safetyToken(route: FeatureRoute): String = safetyMutex.withLock {
        safetyReferences[route]?.let { return@withLock access(it).token }
        check(route.id == 0L) { "请从原事项受限进度进入，不能为原材料另建访问凭证" }
        val originalEpoch = epoch
        val created = api.newSafetySession()
        currentCoroutineContext().ensureActive()
        check(epoch == originalEpoch) { "账号已变化，请重新进入当前事项" }
        val reference = api.limitedRequestAccess().first { it.type == "safety_session" && it.token == created.token }
        safetyReferences[route] = RestrictedReference(reference.type,reference.clientRequestId,reference.id)
        persistDrafts()
        created.token
    }
    fun lookup(reference: RestrictedReference) = runCommand {
        val access = access(reference)
        if (access.type == "data_request") {
            val result = if (access.id > 0) api.dataRequestStatus(access.id, access.token) else api.findDataRequest(access.clientRequestId, access.token)
            mutable.update { it.copy(dataStatus = result, lookupRequestId = reference.clientRequestId) }
        } else {
            val result = if (access.id > 0) api.safetyTicketStatus(access.id, access.token) else api.findSafetyTicket(access.clientRequestId, access.token)
            mutable.update { it.copy(rightsStatus = result, lookupRequestId = reference.clientRequestId) }
        }
        uncertainRequests.remove(reference.clientRequestId)?.let { route -> uncertainRoutes.remove(route); clearForm(route) }
        mutable.update { it.copy(unconfirmedResult = it.route in uncertainRoutes) }
    }
    fun reconcile(key: String, retry: Boolean = false) = runCommand {
        val result = if (retry) api.retryOriginal(key) else api.reconcileOperation(key)
        operationResolved(result)
        mutable.update { it.copy(pending = api.recoverableCommands(), notice = "原请求：${result.state}，结果 ${result.resultKind} #${result.resultId}") }
    }
    fun operationResolved(receipt: OperationReceipt) {
        if (receipt.state != "committed" || !uncertainRequests.containsKey(receipt.clientRequestId)) return
        val route = uncertainRequests.remove(receipt.clientRequestId)
        uncertainRoutes.remove(route)
        route?.let(::clearForm)
        if (mutable.value.route == route) { pendingAction = null; mutable.update { it.copy(confirmation = null, unconfirmedResult = false, error = null) } }
        scheduleDraftSave()
    }
    fun mediaIds(existing: List<Long> = emptyList()): List<Long> = mutable.value.route?.let { mutable.value.photos[it]?.mapNotNull { photo -> photo.mediaId } } ?: existing
    fun photosReady(): Boolean = mutable.value.route?.let { mutable.value.photos[it] }.orEmpty().all { it.mediaId != null && !it.uploading }
    fun initializePhotos(existing: List<Long>) {
        if (!draftReady) return
        val route = mutable.value.route ?: return
        if (mutable.value.photos.containsKey(route)) return
        mutable.update { it.copy(photos = it.photos + (route to existing.map { id -> PhotoAttachment(newRequestKey(), "已保存照片", id) })) }
    }
    fun addPhotos(selected: List<SelectedPhoto>, private: Boolean, safety: Boolean = false) {
        val route = mutable.value.route ?: return
        if (!canEditDraft() || mutable.value.busy || route in uncertainRoutes) return
        selected.take((9 - mutable.value.photos[route].orEmpty().size).coerceAtLeast(0)).forEach { photo ->
            val key = newRequestKey(); photoBytes[key] = photo
            photoScopes[key] = private to safety
            mutable.update { it.copy(photos = it.photos + (route to (it.photos[route].orEmpty() + PhotoAttachment(key,photo.name,mime = photo.mime)))) }
            retryPhoto(route,key,private,safety)
        }
    }
    fun retryPhoto(route: FeatureRoute, key: String, private: Boolean, safety: Boolean = false) {
        if (!canEditDraft() || route in uncertainRoutes) return
        val item = mutable.value.photos[route].orEmpty().firstOrNull { it.key == key } ?: return
        if (item.mediaId != null) return
        val storedScope = photoScopes[key] ?: (private to safety)
        photoScopes[key] = storedScope
        if (photoJobs[key]?.isActive == true) return
        val originalEpoch = epoch
        fun update(change: (PhotoAttachment) -> PhotoAttachment) {
            if (epoch == originalEpoch) mutable.update { it.copy(photos = it.photos + (route to it.photos[route].orEmpty().map { item -> if (item.key == key) change(item) else item })) }
        }
        update { it.copy(uploading = true,error = null) }
        photoJobs[key] = scope.launch {
            try {
                val photo = photoBytes[key] ?: localDrafts?.photo(owner ?: 0L,WorkspacePhoto(key,item.name,item.mime,private = storedScope.first,safety = storedScope.second))?.also { photoBytes[key] = it }
                    ?: throw LocalDraftException("原照片不可读，请重新读取或移除后选择")
                persistDrafts()
                val result = if (storedScope.second) api.uploadSafetyEvidence(photo,key,safetyToken(route)) else if (storedScope.first) api.uploadPrivate(photo,key) else api.upload(photo,key)
                update { it.copy(mediaId = result.id,uploading = false) }
                persistDrafts()
                if (epoch == originalEpoch) photoBytes.remove(key)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { update { it.copy(uploading = false,error = e.mobileMessage()) } }
        }
    }
    fun removePhoto(key: String) {
        val route = mutable.value.route ?: return
        if (!canEditDraft() || mutable.value.busy || route in uncertainRoutes) return
        photoJobs.remove(key)?.cancel(); photoBytes.remove(key)
        mutable.update { it.copy(photos = it.photos + (route to it.photos[route].orEmpty().filterNot { photo -> photo.key == key })) }
        photoScopes.remove(key); scheduleDraftSave()
    }
    fun movePhoto(key: String, offset: Int) {
        val route = mutable.value.route ?: return
        if (!canEditDraft() || mutable.value.busy || route in uncertainRoutes) return
        val photos = mutable.value.photos[route].orEmpty().toMutableList(); val from = photos.indexOfFirst { it.key == key }; val to = from + offset
        if (from >= 0 && to in photos.indices) { photos.add(to,photos.removeAt(from)); mutable.update { it.copy(photos = it.photos + (route to photos)) }; scheduleDraftSave() }
    }
    private fun routeKey(route: FeatureRoute) = "${route.page.name}:${route.id}:${route.kind}:${route.peer}"
    private fun parseRoute(key: String): FeatureRoute {
        val parts = key.split(':'); require(parts.size == 4)
        val route = FeatureRoute(FeaturePage.valueOf(parts[0]),parts[1].toLong(),parts[2],parts[3].toLong())
        require(route.id >= 0 && route.peer >= 0 && route.kind.length <= 60)
        return route
    }
    private fun workspace(): List<WorkspaceDraft> {
        val s = mutable.value
        val routes = (s.forms.keys + s.photos.keys + uncertainRoutes.filterNotNull() + safetyReferences.keys).filter { route ->
            !s.forms[route]?.values.isNullOrEmpty() || !s.photos[route].isNullOrEmpty() || route in uncertainRoutes || route in safetyReferences
        }
        return routes.map { route ->
            val form = s.forms[route] ?: FeatureForm(); val reference = safetyReferences[route]
            WorkspaceDraft(routeKey(route),form.values,form.baseVersion,s.photos[route].orEmpty().map { photo ->
                val type = photoScopes[photo.key] ?: (true to false)
                WorkspacePhoto(photo.key,photo.name,photo.mime,photo.mediaId,type.first,type.second)
            },route in uncertainRoutes,uncertainRequests.filterValues { it == route }.keys.toList(),reference?.type,reference?.clientRequestId)
        }
    }
    private fun canEditDraft(): Boolean {
        if (!draftReady) notify("本机表单尚未恢复，请重试读取，原草稿不会被覆盖")
        return draftReady
    }
    private suspend fun persistDrafts() {
        val storage = localDrafts ?: return
        val originalEpoch = epoch; val originalOwner = owner ?: 0L
        draftMutex.withLock {
            if (!draftReady) throw LocalDraftException("本机草稿尚未恢复，原文件不会被覆盖")
            if (originalEpoch != epoch) throw CancellationException("Account changed")
            val drafts = workspace(); val bytes = photoBytes.toMap()
            withContext(NonCancellable) {
                val saved = storage.save(originalOwner,draftRevision,drafts,bytes)
                if (originalEpoch == epoch) { draftRevision = saved.revision; mutable.update { it.copy(draftError = null,draftRoutes = drafts.map { d -> parseRoute(d.key) }) } }
            }
            if (originalEpoch != epoch) throw CancellationException("Account changed")
        }
    }
    private fun scheduleDraftSave() {
        mutable.update { it.copy(draftRoutes = workspace().map { d -> parseRoute(d.key) }) }
        if (localDrafts == null || !draftReady) return
        draftSave?.cancel()
        val originalEpoch = epoch
        draftSave = scope.launch {
            try { persistDrafts() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (epoch == originalEpoch) mutable.update { it.copy(draftError = "本机表单未保存：${e.mobileMessage()}。当前输入仍保留") } }
        }
    }
    fun retryDraftSave() { scheduleDraftSave() }
    /** 产品内明确选择放弃未保存输入后，重新读取跨窗口已保存版本。 */
    fun replaceWithSavedDrafts() {
        if (mutable.value.busy || uncertainRoutes.isNotEmpty()) { notify("请先核对结果未知的原操作，再替换本机草稿"); return }
        draftSave?.cancel(); photoJobs.values.forEach { it.cancel() }; photoJobs.clear(); photoBytes.clear(); photoScopes.clear()
        safetyReferences.clear(); draftReady = false
        mutable.update { it.copy(forms = emptyMap(),photos = emptyMap(),draftReady = false,draftRoutes = emptyList()) }
        restoreDrafts()
    }
    fun restoreDrafts() {
        val storage = localDrafts ?: return
        if (draftReady || draftRestore?.isActive == true) return
        val originalEpoch = epoch; val originalOwner = owner ?: 0L
        mutable.update { it.copy(draftRestoring = true,draftError = null) }
        draftRestore = scope.launch {
            try {
                val saved = draftMutex.withLock { storage.load(originalOwner) }
                val drafts = saved?.drafts.orEmpty().associateBy { parseRoute(it.key) }
                if (epoch != originalEpoch) return@launch
                mutable.update { it.copy(forms = drafts.mapValues { (_,d) -> FeatureForm(d.values,d.baseVersion) },photos = drafts.mapValues { (_,d) -> d.photos.map { p ->
                    PhotoAttachment(p.key,p.name,p.mediaId,error = if(p.mediaId == null) "原文件已保留，请按原编号重试上传" else null,mime = p.mime)
                } },draftRoutes = drafts.keys.toList()) }
                drafts.forEach { (route,draft) ->
                    if (draft.uncertain) uncertainRoutes.add(route)
                    draft.requests.forEach { uncertainRequests[it] = route }
                    draft.photos.forEach { photoScopes[it.key] = it.private to it.safety }
                    if (draft.accessType != null && draft.accessReference != null) safetyReferences[route] = RestrictedReference(draft.accessType,draft.accessReference,route.id)
                }
                draftRevision = saved?.revision ?: 0L; draftReady = true
                mutable.update { it.copy(draftRestoring = false,draftReady = true,unconfirmedResult = it.route in uncertainRoutes) }
                if (mutable.value.route != null) reload()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (epoch == originalEpoch) mutable.update { it.copy(draftRestoring = false,draftError = "本机表单未恢复：${e.mobileMessage()}。原文件未被覆盖") } }
        }
    }
    fun close() { sensitive.close(); job.cancel() }
}
