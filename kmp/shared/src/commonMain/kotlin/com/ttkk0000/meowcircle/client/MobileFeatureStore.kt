package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject

/** 功能路由只包含资源标识；临时凭证不进入路由、表单快照或普通偏好。 */
enum class FeaturePage(val title: String) {
    Publications("我的发布"), EntryEdit("编辑动态"), Friends("好友与宠友圈"), Member("宠友资料"), ProfileEdit("编辑资料"), PetEdit("宠物档案"), PetDiary("独立图文日记"),
    Collaborators("协作与邀请"), Invitations("收到与发出的邀请"), GroupCreate("创建群聊"), Group("群聊管理"), GroupDirectory("发现宠友群"), GroupPreview("群简介与加入申请"), Content("关注与求助订阅"),
    Appointments("预约"), Appointment("预约详情"), AppointmentEdit("预约安排"),
    CaseEdit("走失与拾获发布"), CaseWork("线索与认领"), LeadSubmit("私密目击线索"), LeadInbox("私密线索"), Lead("核实这条线索"), Claim("私密认领材料"),
    AdoptionWork("领养照护与协议"), ContractEdit("协议提案"), ReturnEdit("安全接回安排"),
    Offers("有偿信息"), Offer("披露详情"), OfferEdit("编辑披露"),
    SettingsHome("设置"), Settings("通知设置"), NotificationHelp("开启系统通知"), LocationHelp("不提供定位也可以"), Nearby("附近提醒"), Map("互助地图"), Reviews("核实与举报"), Review("核实事项"),
    Safety("安全与隐私求助"), DataRequests("资料与账号请求"), Security("账号安全"),
    Recovery("待核对操作"), PhotoHelp("照片选择说明"), Expressions("表情与贴纸"), Rules("规则与权益"), Reading("阅读说明"), Age("年龄与使用范围"), Adult("成年资格核对"), Guardians("监护授权与管理")
}
enum class CaseMaterial { Claims, Leads }

data class FeatureRoute(val page: FeaturePage, val id: Long = 0, val kind: String = "", val peer: Long = 0)
data class FeatureForm(val values: Map<String, String> = emptyMap(), val baseVersion: Long? = null)
data class FeatureConfirmation(val title: String, val summary: String)
data class RestrictedReference(val type: String, val clientRequestId: String, val id: Long)
data class FeatureState(
    val publishedPets: PageState<PetBody> = PageState(), val publishedEntries: PageState<EntryBody> = PageState(),
    val publishedCases: PageState<CaseBody> = PageState(), val publishedOffers: PageState<OfferBody> = PageState(),
    val readingCatalog: ReadingCatalog? = null,val readingDocument: ReadingDocument? = null,
    val route: FeatureRoute? = null, val forms: Map<FeatureRoute, FeatureForm> = emptyMap(),
    val loading: Boolean = false, val busy: Boolean = false, val error: String? = null, val notice: String? = null,
    val readFailure: ClientReadFailure? = null,
    val conflict: Boolean = false, val confirmation: FeatureConfirmation? = null, val commandErrorCode: String? = null,
    val mapCases: PageState<CaseBody> = PageState(), val diary: PageState<EntryBody> = PageState(), val directory: PageState<GroupDirectoryBody> = PageState(), val groupPreview: MobileRecord<GroupDirectoryBody>? = null, val contentSubscriptions: MobileRecord<ContentSubscriptions>? = null,
    val friends: PageState<FriendBody> = PageState(), val invitations: PageState<InvitationBody> = PageState(),
    val appointments: PageState<AppointmentBody> = PageState(), val offers: PageState<OfferBody> = PageState(),
    val claims: PageState<ClaimBody> = PageState(), val leads: PageState<LeadBody> = PageState(),
    val care: PageState<CareBody> = PageState(), val reviews: PageState<ReviewBody> = PageState(),
    val entry: MobileRecord<EntryBody>? = null, val member: PublicMember? = null, val profile: MobileProfile? = null, val pet: MobileRecord<PetBody>? = null,
    val group: MobileRecord<GroupBody>? = null, val appointment: MobileRecord<AppointmentBody>? = null,
    val case: MobileRecord<CaseBody>? = null, val lead: MobileRecord<LeadBody>? = null, val claim: MobileRecord<ClaimBody>? = null,
    val adoption: MobileRecord<AdoptionBody>? = null, val offer: MobileRecord<OfferBody>? = null,
    val review: MobileRecord<ReviewBody>? = null, val verifications: List<VerificationSummary> = emptyList(),
    val preference: MobileRecord<PreferenceBody>? = null, val subscription: MobileRecord<SubscriptionBody>? = null,
    val expressions: MobileRecord<ExpressionBody>? = null, val catalog: ExpressionCatalog = ExpressionCatalog(),
    val petChoices: PageState<PetBody> = PageState(), val petChoiceLoading: Boolean = false, val petChoiceError: String? = null,
    val dataChoices: PageState<JsonObject> = PageState(), val dataChoiceKind: String = "", val dataChoiceAssistance: Boolean = false,
    val dataChoiceLoading: Boolean = false, val dataChoiceError: String? = null,
    val peopleChoices: PageState<FriendBody> = PageState(), val people: Map<Long, PublicMember> = emptyMap(),
    val peopleUnavailable: Set<Long> = emptySet(), val peopleErrors: Set<Long> = emptySet(),
    val methods: IdentityMethods? = null, val eligibility: Eligibility? = null, val guardians: List<MobileRecord<GuardianBody>> = emptyList(),
    val restricted: List<RestrictedReference> = emptyList(), val lookupRequestId: String? = null, val dataStatus: DataRequestStatus? = null, val rightsStatus: RightsReceipt? = null,
    val pending: List<CommandRecovery> = emptyList(), val otpWait: Int = 0,
    val photos: Map<FeatureRoute,List<PhotoAttachment>> = emptyMap(),
    val unconfirmedResult: Boolean = false,
    val draftRestoring: Boolean = false, val draftReady: Boolean = true, val draftError: String? = null,
    val draftRoutes: List<FeatureRoute> = emptyList(),
) {
    val dataProgress: DataRequestProgress? get() = dataStatus?.progress()
}

/** 与 MobileStore 同生命周期的功能状态；所有 Android 面板共用此实例。 */
class MobileFeatureStore(val api: MobileApi, parentScope: CoroutineScope, private val localDrafts: LocalWorkspaceDrafts? = null) :
    MviStore<FeatureState, FeatureStoreIntent, FeatureStoreEffect>(FeatureState(draftRestoring = localDrafts != null,draftReady = localDrafts == null), parentScope) {
    val sensitive = SensitiveActionStore(api, scope)
    val age = AgeAssuranceStore(api,scope)
    val guardianInvites = GuardianInviteStore(api,scope)
    private val history = mutableListOf<FeatureRoute>()
    private var loading: Job? = null
    private var commandJob: Job? = null
    private var countdown: Job? = null
    private var petChoiceJob: Job? = null
    private var dataChoiceJob: Job? = null
    private var peopleChoiceJob: Job? = null
    private var dataChoiceSequence = 0L
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

    override fun accept(intent: FeatureStoreIntent) {
        when (intent) {
            is FeatureStoreIntent.OpenRoute -> open(intent.route)
            FeatureStoreIntent.Back -> back()
            FeatureStoreIntent.CloseRoute -> closeRoute()
            is FeatureStoreIntent.Reload -> reload(intent.more)
            is FeatureStoreIntent.EditField -> edit(intent.key, intent.value)
            FeatureStoreIntent.DiscardAndReload -> discardAndReload()
            FeatureStoreIntent.DismissConfirmation -> dismissConfirmation()
            FeatureStoreIntent.SubmitConfirmation -> submitConfirmation()
            is FeatureStoreIntent.Notify -> notify(intent.message)
        }
    }

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
                loading?.cancel(); commandJob?.cancel(); countdown?.cancel(); petChoiceJob?.cancel(); dataChoiceJob?.cancel(); peopleChoiceJob?.cancel()
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
    fun selectPublications(kind: String) {
        require(kind in listOf("pets","entries","cases","offers"))
        val route=mutable.value.route ?: return
        if(route.page!=FeaturePage.Publications || mutable.value.busy) return
        revision++;loading?.cancel()
        mutable.update {it.copy(route=route.copy(kind=kind),loading=false,error=null)}
        reload()
    }
    fun loadContentSubscriptions() { if(owner == null) return; val currentEpoch = epoch; scope.launch { try { val p = api.contentSubscriptions(); if(epoch == currentEpoch) mutable.update { it.copy(contentSubscriptions = p) } } catch(e: CancellationException) { throw e } catch(e: Exception) { if(epoch == currentEpoch) notify(e.message ?: "暂无法读取关注状态") } } }
    fun field(key: String, fallback: String = ""): String = mutable.value.route?.let { mutable.value.forms[it]?.values?.get(key) } ?: fallback
    fun importLegacyLead() {
        val state = mutable.value; val route = state.route ?: return
        if(route.page != FeaturePage.LeadSubmit || state.busy || state.loading || state.confirmation != null || state.unconfirmedResult || !canEditDraft()) return
        if(field("lead").isNotBlank() || state.photos[route].orEmpty().isNotEmpty()) { notify("当前草稿已有内容，不覆盖原稿"); return }
        val old = FeatureRoute(FeaturePage.CaseWork,route.id)
        val text = state.forms[old]?.values?.get("lead")?.takeIf { it.isNotBlank() } ?: return
        val photos = state.photos[old].orEmpty().filter { it.mediaId != null && !it.uploading }.map { it.copy(key = newRequestKey()) }
        edit("lead",text)
        mutable.update { it.copy(photos = it.photos + (route to photos),notice = "已复制文字和已上传附件，请核对；旧表单未删除，未上传附件仍在原表单") }
        scheduleDraftSave()
    }
    fun edit(key: String, value: String) {
        if (!canEditDraft() || mutable.value.busy || mutable.value.confirmation != null || mutable.value.route in uncertainRoutes || value.length > 10000) return
        val route = mutable.value.route ?: return
        mutable.update { s -> val form = s.forms[route] ?: FeatureForm(); s.copy(forms = s.forms + (route to form.copy(values = form.values + (key to value)))) }
        scheduleDraftSave()
    }
    fun version(fallback: Long = 0): Long = mutable.value.route?.let { mutable.value.forms[it]?.baseVersion } ?: fallback
    fun adoptLeadVersion() {
        mutable.value.case?.let {adoptReadVersion(it.id,it.version,setOf(FeaturePage.LeadSubmit),"求助")}
    }
    fun adoptAppointmentVersion() {
        mutable.value.appointment?.let {adoptReadVersion(it.id,it.version,setOf(FeaturePage.Appointment,FeaturePage.AppointmentEdit),"预约")}
    }
    fun adoptAdoptionVersion() {
        mutable.value.adoption?.let {adoptReadVersion(it.id,it.version,setOf(FeaturePage.AdoptionWork,FeaturePage.ContractEdit,FeaturePage.ReturnEdit),"领养事项")}
    }
    private fun adoptReadVersion(id: Long,version: Long,pages: Set<FeaturePage>,label: String) {
        val state=mutable.value; val route=state.route ?: return
        if(route.page !in pages || route.id != id || state.busy || state.loading || state.unconfirmedResult || route in uncertainRoutes || !canEditDraft()) return
        val form=state.forms[route] ?: return
        if(version <= (form.baseVersion ?: 0)) {notify("请先读取当前"+label+"版本，原输入继续保留");return}
        pendingAction=null
        mutable.update {it.copy(forms=it.forms+(route to form.copy(baseVersion=version)),conflict=false,confirmation=null,error=null,commandErrorCode=null,notice="已保留原输入并采用刚读取的"+label+"版本，请重新核对")}
        scheduleDraftSave()
    }
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
        mutable.update { it.copy(route = route, loading = false, error = null, readFailure = null, notice = null, conflict = false,
            readingDocument = null,groupPreview = null, mapCases = PageState(), diary = PageState(), entry = null, member = null, pet = null, group = null, appointment = null, case = null, lead = null, claim = null, adoption = null,
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
        revision++; loading?.cancel(); mutable.update { it.copy(route = previous, loading = false, error = null, readFailure = null, notice = null,readingDocument = null,groupPreview = null,mapCases = PageState(),diary = PageState(),entry = null,member = null,pet = null,group = null,appointment = null,case = null,claim = null,adoption = null,offer = null,review = null,unconfirmedResult = previous in uncertainRoutes) }
        if (previous != null) reload()
        return true
    }
    /** 阅读子页返回已有入口，避免再压入一份目录而形成循环。 */
    fun returnTo(route: FeatureRoute) {
        if(mutable.value.busy) return
        val index=history.indexOfLast {it==route}
        if(index>=0) {while(history.lastIndex>index) history.removeAt(history.lastIndex);back()}
        else {closeRoute();open(route)}
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
        sendEffect(FeatureStoreEffect.RouteCompleted(route))
        reload()
    }
    fun notify(message: String) { mutable.update { it.copy(notice = message) }; sendEffect(FeatureStoreEffect.Notice(message)) }
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
        mutable.update { it.copy(busy = true, error = null, notice = null,commandErrorCode = null) }
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
                        conflict = e is MobileApiException && e.status == 409, unconfirmedResult = unknown || it.unconfirmedResult,commandErrorCode = (e as? MobileApiException)?.code,
                        pending = api.recoverableCommands()) }
                }
            } finally { if (epoch == originalEpoch) { mutable.update { it.copy(busy = false, pending = api.recoverableCommands()) }; scheduleDraftSave() } }
        }
    }
    fun reload(more: Boolean = false, material: CaseMaterial? = null) {
        val route = mutable.value.route ?: return
        if (more && mutable.value.loading || material != null && route.page != FeaturePage.CaseWork) return
        val currentRevision = ++revision
        val currentEpoch = epoch
        loading?.cancel()
        mutable.update { it.copy(loading = true, error = null, readFailure = null,commandErrorCode = null) }
        loading = scope.launch {
            fun current() = revision == currentRevision && epoch == currentEpoch
            try {
                when (route.page) {
                    FeaturePage.Publications -> {
                        val account=owner ?: return@launch
                        val params=mapOf("owner_id" to "$account")
                        when(route.kind.ifBlank {"pets"}) {
                            "entries" -> loadFeaturePage(more,::current,{it.publishedEntries},{state,page->state.copy(publishedEntries=page)}) {api.get("entries",params+("cursor" to "$it"))}
                            "cases" -> loadFeaturePage(more,::current,{it.publishedCases},{state,page->state.copy(publishedCases=page)}) {api.get("cases",params+("cursor" to "$it"))}
                            "offers" -> loadFeaturePage(more,::current,{it.publishedOffers},{state,page->state.copy(publishedOffers=page)}) {api.get("offers",params+("cursor" to "$it"))}
                            else -> loadFeaturePage(more,::current,{it.publishedPets},{state,page->state.copy(publishedPets=page)}) {api.get("pets",params+("cursor" to "$it"))}
                        }
                    }
                    FeaturePage.EntryEdit -> { val p = api.entry(route.id); if(current()) { mutable.update { it.copy(entry = p) }; capture(p.version) } }
                    FeaturePage.PetDiary -> { loadFeaturePage(more, ::current, { it.diary }, { s, p -> s.copy(diary = p) }) { cursor -> api.get("entries",mapOf("reference_id" to route.id.toString(),"space" to "diary","cursor" to cursor.toString()),authenticated = owner != null) } }
                    FeaturePage.Friends -> { loadFeaturePage(more, ::current, { it.friends }, { s, p -> s.copy(friends = p) }) { api.friends(it) } }
                    FeaturePage.Member -> { val p = api.member(route.id); if (current()) mutable.update { it.copy(member = p) }; if(owner != null) { val content = api.contentSubscriptions(); if(current()) mutable.update { it.copy(contentSubscriptions = content) } } }
                    FeaturePage.Content -> { val p = api.contentSubscriptions(); if(current()) mutable.update { it.copy(contentSubscriptions = p) } }
                    FeaturePage.GroupDirectory -> { loadFeaturePage(more, ::current, { it.directory }, { s, p -> s.copy(directory = p) }) { api.groupDirectory(it,field("q"),field("city")) } }
                    FeaturePage.GroupPreview -> { val p = api.groupPreview(route.id); if(current()) mutable.update { it.copy(groupPreview = p) } }
                    FeaturePage.ProfileEdit -> { val p = api.profile(); if (current()) { mutable.update { it.copy(profile = p) }; capture(p.version) } }
                    FeaturePage.PetEdit, FeaturePage.Collaborators -> if (route.id > 0 && route.kind != "care") {
                        val p = api.pet(route.id); if (current()) { mutable.update { it.copy(pet = p) }; capture(p.version) }
                    }
                    FeaturePage.Invitations -> { loadFeaturePage(more, ::current, { it.invitations }, { s, p -> s.copy(invitations = p) }) { api.invitations(it) } }
                    FeaturePage.Group -> { val p = api.group(route.id); if (current()) { mutable.update { it.copy(group = p) }; capture(p.version) } }
                    FeaturePage.Appointments -> { loadFeaturePage(more, ::current, { it.appointments }, { s, p -> s.copy(appointments = p) }) { api.appointments(it) } }
                    FeaturePage.Appointment, FeaturePage.AppointmentEdit -> if (route.id > 0) {
                        val p = api.appointment(route.id); if (current()) { mutable.update { it.copy(appointment = p) }; capture(p.version) }
                    }
                    FeaturePage.CaseEdit -> if (route.id > 0) { val p = api.case(route.id); if (current()) { mutable.update { it.copy(case = p) }; capture(p.version) } }
                    FeaturePage.CaseWork -> {
                        if (material == null || mutable.value.case == null) {
                            val record = api.case(route.id)
                            if (current()) { mutable.update { it.copy(case = record) }; capture(record.version) }
                        }
                        if (owner == null) return@launch
                        // 两类私密材料独立取数、保存游标及重试，不因另一类失败而重复翻页。
                        if (material == null || material == CaseMaterial.Claims) {
                            loadFeaturePage(more, ::current, { it.claims }, { s, p -> s.copy(claims = p) }) { api.claims(route.id, it) }
                        }
                        if (material == null || material == CaseMaterial.Leads) {
                            loadFeaturePage(more, ::current, { it.leads }, { s, p -> s.copy(leads = p) }) { api.leads(route.id, it) }
                        }
                    }
                    FeaturePage.LeadSubmit -> {
                        val parent = api.case(route.id)
                        if(current()) { mutable.update { it.copy(case = parent) }; capture(parent.version) }
                    }
                    FeaturePage.LeadInbox -> {
                        if (!more || mutable.value.case == null) {
                            val p = api.case(route.id)
                            if (current()) mutable.update { it.copy(case = p) }
                        }
                        loadFeaturePage(more,::current,{it.leads},{state,page->state.copy(leads=page)}) {api.leads(route.id,it)}
                    }
                    FeaturePage.Lead -> {
                        val p = api.leadRecord(route.id); val c = api.case(p.referenceId)
                        if(current()) { mutable.update {it.copy(lead=p,case=c)}; capture(p.version) }
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
                            loadFeaturePage(more, ::current, { it.care }, { s, p -> s.copy(care = p) }) { api.careUpdates(route.id, it) }
                        }
                    }
                    FeaturePage.Offers -> { loadFeaturePage(more, ::current, { it.offers }, { s, p -> s.copy(offers = p) }) { api.offers(it) } }
                    FeaturePage.Offer, FeaturePage.OfferEdit -> if (route.id > 0) {
                        val p = api.offer(route.id); if (current()) { mutable.update { it.copy(offer = p) }; capture(p.version) }
                        val checks = api.offerVerifications(route.id); if (current()) mutable.update { it.copy(verifications = checks) }
                    }
                    FeaturePage.Settings -> { val p = api.preferences().items.firstOrNull(); if (current()) { mutable.update { it.copy(preference = p) }; capture(p?.version ?: 0) } }
                    FeaturePage.Map -> { val p: MobilePage<CaseBody> = if(route.id > 0) MobilePage(items = listOf(api.case(route.id))) else api.get("cases",buildMap { put("cursor",if(more) mutable.value.mapCases.next.toString() else "0");put("city",field("city"));put("state","active") },authenticated = owner != null);if(current()) mutable.update { it.copy(mapCases = page(it.mapCases,p,more)) } }
                    FeaturePage.Nearby -> { val p = api.subscriptions().items.firstOrNull(); if (current()) { mutable.update { it.copy(subscription = p) }; capture(p?.version ?: 0) } }
                    FeaturePage.Expressions -> {
                        val catalog = api.expressions(); val p = api.expressionState().items.firstOrNull()
                        if (current()) { mutable.update { it.copy(catalog = catalog, expressions = p) }; capture(p?.version ?: 0) }
                    }
                    FeaturePage.Reviews -> { loadFeaturePage(more, ::current, { it.reviews }, { s, p -> s.copy(reviews = p) }) { api.reviews(it) } }
                    FeaturePage.Review -> if (route.id > 0) { val p = api.reviewRecord(route.id); if (current()) { mutable.update { it.copy(review = p) }; capture(p.version) } }
                    FeaturePage.Security -> {
                        val methods = api.identityMethods(); val profile = api.profile(); val eligibility = api.eligibility()
                        if (current()) { mutable.update { it.copy(methods = methods, profile = profile, eligibility = eligibility) }; capture(profile.version) }
                    }
                    FeaturePage.Recovery -> if (current()) mutable.update { it.copy(pending = api.recoverableCommands(), restricted = restricted()) }
                    FeaturePage.DataRequests, FeaturePage.Safety -> if (current()) mutable.update { it.copy(restricted = restricted()) }
                    FeaturePage.Rules -> {val catalog=api.readingCatalog();if(current())mutable.update {it.copy(readingCatalog = catalog)}}
                    FeaturePage.Reading -> {
                        val document=if(route.kind.startsWith("type-")) api.readingCatalog().documentFor(route.kind.removePrefix("type-")) ?: throw MobileApiException(404,"not found") else api.readingDocument(route.kind)
                        if(current())mutable.update {it.copy(readingDocument = document)}
                    }
                    FeaturePage.Age, FeaturePage.Adult -> { val eligible = api.eligibility();if(current()) mutable.update { it.copy(eligibility = eligible) } }
                    FeaturePage.Guardians -> {
                        val guardians=api.guardians();val eligibility=api.eligibility();val catalog=api.readingCatalog()
                        if(current())mutable.update{it.copy(guardians=guardians,eligibility=eligibility,readingCatalog=catalog)}
                    }
                    FeaturePage.GroupCreate, FeaturePage.SettingsHome, FeaturePage.NotificationHelp, FeaturePage.LocationHelp, FeaturePage.PhotoHelp -> Unit
                }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                if (current()) mutable.update { it.copy(error = e.mobileMessage(), readFailure = e.readFailure(),
                    readingDocument = if (denied(e)) null else it.readingDocument,
                    readingCatalog = if (denied(e)) null else it.readingCatalog,
                    groupPreview = if (denied(e)) null else it.groupPreview,
                    contentSubscriptions = if (denied(e)) null else it.contentSubscriptions,
                    diary = if (denied(e)) it.diary.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.diary,
                    mapCases = if (denied(e)) it.mapCases.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.mapCases,
                    // 权限被撤销时立即清除受保护的数据。
                    entry = if (denied(e)) null else it.entry, pet = if (denied(e)) null else it.pet,
                    profile = if (denied(e)) null else it.profile, member = if (denied(e)) null else it.member,
                    case = if (denied(e)) null else it.case, lead = if (denied(e)) null else it.lead, appointment = if (denied(e)) null else it.appointment,
                    offer = if (denied(e)) null else it.offer, review = if (denied(e)) null else it.review,
                    verifications = if (denied(e)) emptyList() else it.verifications,
                    claim = if (denied(e)) null else it.claim, group = if (denied(e)) null else it.group,
                    adoption = if (denied(e)) null else it.adoption,
                    friends = if (denied(e)) it.friends.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.friends,
                    invitations = if (denied(e)) it.invitations.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.invitations,
                    appointments = if (denied(e)) it.appointments.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.appointments,
                    offers = if (denied(e)) it.offers.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.offers,
                    reviews = if (denied(e)) it.reviews.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.reviews,
                    claims = if (denied(e)) it.claims.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.claims,
                    leads = if (denied(e)) it.leads.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.leads,
                    care = if (denied(e)) it.care.failed(e.mobileMessage(), clear = true, failure = e.readFailure()) else it.care) }
            } finally { if (current()) mutable.update { it.copy(loading = false) } }
        }
    }
    private suspend fun <T> loadFeaturePage(more: Boolean, current: () -> Boolean,
        get: (FeatureState) -> PageState<T>, put: (FeatureState, PageState<T>) -> FeatureState,
        fetch: suspend (Long) -> MobilePage<T>) {
        if (!current()) return
        val old = get(mutable.value)
        if (more && !old.canLoadMore) return
        mutable.update { put(it, old.starting(more)) }
        try {
            val result = fetch(if (more) old.next else 0)
            currentCoroutineContext().ensureActive()
            if (current()) mutable.update { put(it, old.completed(result, more)) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (current()) mutable.update { put(it, get(it).failed(e.mobileMessage(), denied(e), e.readFailure())) }
            if (denied(e)) throw e
        }
    }
    private fun denied(e: Exception) = e is MobileApiException && e.status in listOf(401, 403, 404)
    /** 只从当前账号可访问的关系取得候选；不提供全站账号枚举。 */
    fun loadPeopleChoices(more: Boolean = false) {
        val account = owner ?: return
        if (api.user.value?.id != account) return
        val old = mutable.value.peopleChoices
        if (old.loading || more && !old.canLoadMore) return
        val originalEpoch = epoch
        mutable.update { it.copy(peopleChoices = old.starting(more)) }
        peopleChoiceJob = scope.launch {
            try {
                val result = api.friends(if (more) old.next else 0)
                val next = old.completed(result, more)
                val peers = result.items.filter { it.state == "accepted" }.map { row ->
                    if (row.data.requesterId == account) row.data.recipientId else row.data.requesterId
                }.distinct().filter { it > 0 && it != account }
                val profiles = mutableMapOf<Long, PublicMember>()
                val unavailable = mutableSetOf<Long>(); val errors = mutableSetOf<Long>()
                for (batch in peers.chunked(4)) coroutineScope {
                    batch.map { peer -> async {
                        try { peer to Result.success(api.member(peer)) }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { peer to Result.failure<PublicMember>(e) }
                    } }.awaitAll().forEach { (peer, profile) ->
                        profile.onSuccess { profiles[peer] = it }.onFailure {
                            if (it is MobileApiException && it.status == 401) throw it
                            if (it is MobileApiException && it.status in listOf(403, 404)) unavailable.add(peer) else errors.add(peer)
                        }
                    }
                }
                if (epoch == originalEpoch && api.user.value?.id == account) mutable.update { current -> current.copy(peopleChoices = next,
                    people = (if (more) current.people else emptyMap()) - unavailable - errors + profiles,
                    peopleUnavailable = (if (more) current.peopleUnavailable else emptySet()) + unavailable,
                    peopleErrors = (if (more) current.peopleErrors else emptySet()) + errors) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == originalEpoch) mutable.update { it.copy(peopleChoices = it.peopleChoices.failed(e.mobileMessage(), denied(e)),
                    people = if (denied(e)) emptyMap() else it.people,
                    peopleUnavailable = if (denied(e)) emptySet() else it.peopleUnavailable,
                    peopleErrors = if (denied(e)) emptySet() else it.peopleErrors) }
            }
        }
    }
    /** 预约对象来自已访问的业务事项，不要求双方先成为好友。服务端仍作最终准入检查。 */
    suspend fun requireAppointmentContext(contextKind: String, contextId: Long, peer: Long) {
        val account = owner ?: throw MobileApiException(401, "请登录后核对预约事项")
        val originalEpoch = epoch
        try {
            val expected = when (contextKind) {
                "adoption" -> api.adoption(contextId).data.let { a -> when (account) {
                    a.applicantId -> a.publisherId
                    a.publisherId -> a.applicantId
                    else -> throw MobileApiException(422, "你已不是此领养事项的参与人")
                } }
                "case" -> api.case(contextId).ownerId
                "visit" -> api.pet(contextId).ownerId
                "offer" -> api.offer(contextId).ownerId
                else -> throw MobileApiException(422, "请从有效业务事项发起预约")
            }
            if (epoch != originalEpoch || api.user.value?.id != account) throw CancellationException("Account changed")
            if (contextId <= 0 || peer <= 0 || peer == account || expected != peer)
                throw MobileApiException(422, "预约参与人已变化，请返回原事项重新核对")
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (e is MobileApiException && e.status in listOf(401, 422)) throw e
            throw MobileApiException(422, "暂时无法核对预约事项，请重试；原表单已保留")
        }
    }
    /** 选择摘要仍保留；提交前重新核对当前关系，失效不会自动删除或放宽受众。 */
    suspend fun requireCurrentPeople(ids: List<Long>): List<PublicMember> {
        val account = owner ?: throw MobileApiException(401, "请登录后核对所选人员")
        if (api.user.value?.id != account) throw CancellationException("Account changed")
        val originalEpoch = epoch
        val wanted = ids.toSet()
        if (wanted.isEmpty() || wanted.size != ids.size || wanted.size > 200 || wanted.any { it <= 0 || it == account })
            throw MobileApiException(422, "请从可访问好友中选择人员")
        val accepted = mutableSetOf<Long>()
        try {
            var cursor = 0L
            val seen = mutableSetOf<Long>()
            do {
                if (!seen.add(cursor)) throw MobileApiException(422, "好友分页暂不可核对，请重新读取")
                val page = api.friends(cursor)
                currentCoroutineContext().ensureActive()
                if (epoch != originalEpoch || api.user.value?.id != account) throw CancellationException("Account changed")
                page.items.filter { it.state == "accepted" }.forEach { row ->
                    val peer = if (row.data.requesterId == account) row.data.recipientId else row.data.requesterId
                    if (peer in wanted) accepted.add(peer)
                }
                cursor = page.nextCursor
            } while (cursor != 0L && !accepted.containsAll(wanted))
            val missing = wanted - accepted
            if (missing.isNotEmpty()) {
                mutable.update { it.copy(people = it.people - missing, peopleUnavailable = it.peopleUnavailable + missing) }
                throw MobileApiException(422, "部分所选人员已不再是可访问好友，请调整选择；原可见范围保持不变")
            }
            val profiles = mutableListOf<PublicMember>()
            for (batch in ids.chunked(4)) coroutineScope {
                batch.map { peer -> async { peer to runCatching {
                    try { api.member(peer) } catch (e: CancellationException) { throw e }
                } } }.awaitAll().forEach { (peer, result) ->
                    currentCoroutineContext().ensureActive()
                    if (epoch != originalEpoch || api.user.value?.id != account) throw CancellationException("Account changed")
                    result.getOrElse { error ->
                        if (error is MobileApiException && error.status in listOf(403, 404)) {
                            mutable.update { it.copy(people = it.people - peer, peopleUnavailable = it.peopleUnavailable + peer) }
                            throw MobileApiException(422, "所选人员的资料已不可访问，请调整选择")
                        }
                        throw error
                    }.let(profiles::add)
                }
            }
            mutable.update { it.copy(people = it.people + profiles.associateBy { p -> p.id }, peopleUnavailable = it.peopleUnavailable - wanted) }
            return profiles
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            // 此时尚未发出写请求；读取失败不能被操作恢复机制误记为提交结果未知。
            if (epoch == originalEpoch && denied(e)) mutable.update { it.copy(
                peopleChoices = it.peopleChoices.failed(e.mobileMessage(), clear = true, failure = e.readFailure()), people = emptyMap(),
                peopleUnavailable = it.peopleUnavailable + wanted, peopleErrors = emptySet()) }
            if (e is MobileApiException && e.status in listOf(401, 422)) throw e
            throw MobileApiException(422, "暂时无法核对所选人员，请重试；原选择与可见范围已保留")
        }
    }
    suspend fun requireCurrentAudience(audience: Audience) {
        if (audience.mode in listOf("selected", "excluded")) {
            if (audience.mode == "selected" && audience.userIds.isEmpty()) throw MobileApiException(422, "请至少选择一位好友")
            if (audience.userIds.isNotEmpty()) requireCurrentPeople(audience.userIds)
        }
    }
    suspend fun requireOpenedMember(id: Long): PublicMember {
        val route = mutable.value.route
        val originalEpoch = epoch
        if (route?.page != FeaturePage.Member || route.id != id || mutable.value.member?.id != id)
            throw MobileApiException(422, "请先明确打开对方资料")
        try {
            val member = api.member(id)
            if (epoch != originalEpoch || mutable.value.route != route) throw CancellationException("Context changed")
            mutable.update { it.copy(member = member) }
            return member
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (epoch == originalEpoch && denied(e)) mutable.update { it.copy(member = null, people = it.people - id) }
            throw MobileApiException(if (e is MobileApiException && e.status == 401) 401 else 422,
                "暂时不能访问对方资料，请重新核对后再发送申请")
        }
    }
    suspend fun prepareFriendRequest(id: Long, reason: String): FriendInput {
        requireOpenedMember(id)
        val account = owner ?: throw MobileApiException(401, "请先登录")
        val originalEpoch = epoch
        var cursor = 0L
        val seen = mutableSetOf<Long>()
        try {
            do {
                if (!seen.add(cursor)) throw MobileApiException(422, "好友关系暂不可核对，请重试")
                val page = api.friends(cursor)
                if (epoch != originalEpoch || api.user.value?.id != account) throw CancellationException("Account changed")
                val relationship = page.items.firstOrNull { row ->
                    row.data.requesterId == id && row.data.recipientId == account || row.data.recipientId == id && row.data.requesterId == account
                }
                if (relationship != null) {
                    if (relationship.state !in listOf("removed", "declined", "withdrawn"))
                        throw MobileApiException(422, "已有好友关系或待处理申请，请在好友列表查看当前状态")
                    return FriendInput(id, newRequestKey(), relationship.version, reason)
                }
                cursor = page.nextCursor
            } while (cursor != 0L)
            return FriendInput(id, newRequestKey(), reason = reason)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (e is MobileApiException && e.status in listOf(401, 422)) throw e
            throw MobileApiException(422, "暂时无法核对好友关系，请重试；本次申请尚未发送")
        }
    }
    fun loadPetChoices(more: Boolean = false) {
        val account = owner ?: return
        if (mutable.value.petChoiceLoading || more && !mutable.value.petChoices.canLoadMore) return
        val originalEpoch = epoch
        val old = mutable.value.petChoices
        mutable.update { it.copy(petChoiceLoading = true, petChoiceError = null, petChoices = old.starting(more)) }
        petChoiceJob = scope.launch {
            try {
                val result = api.pets(if (more) old.next else 0, owner = account)
                if (epoch == originalEpoch) mutable.update { it.copy(petChoices = old.completed(result, more)) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (epoch == originalEpoch) mutable.update { it.copy(petChoiceError = e.mobileMessage(),
                    petChoices = it.petChoices.failed(e.mobileMessage(), denied(e))) }
            } finally { if (epoch == originalEpoch) mutable.update { it.copy(petChoiceLoading = false) } }
        }
    }
    fun loadDataChoices(kind: String, assistance: Boolean, more: Boolean = false) {
        val account = owner ?: return
        require(kind in listOf("entries", "entry_comments", "pet_comments", "pets", "media", "cases", "offers", "drafts", "source_declarations", "subscriptions") ||
            assistance && kind in listOf("adoptions", "appointments", "reviews"))
        val same = mutable.value.dataChoiceKind == kind && mutable.value.dataChoiceAssistance == assistance
        if (same && (mutable.value.dataChoiceLoading || more && !mutable.value.dataChoices.canLoadMore)) return
        dataChoiceJob?.cancel()
        val request = ++dataChoiceSequence
        val originalEpoch = epoch
        val route = mutable.value.route
        val append = same && more
        val old = if (same) mutable.value.dataChoices else PageState<JsonObject>()
        mutable.update { it.copy(dataChoiceLoading = true, dataChoiceError = null, dataChoiceKind = kind,
            dataChoiceAssistance = assistance, dataChoices = old.starting(append)) }
        dataChoiceJob = scope.launch {
            fun current() = epoch == originalEpoch && request == dataChoiceSequence && mutable.value.route == route
            try {
                val result = api.get<MobilePage<JsonObject>>(kind, buildMap {
                    put("cursor", if (append) old.next.toString() else "0")
                    if (!assistance) put("owner_id", account.toString())
                })
                if (current()) mutable.update { it.copy(dataChoices = old.completed(result, append)) }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                if (current()) mutable.update { it.copy(dataChoiceError = e.mobileMessage(),
                    dataChoices = it.dataChoices.failed(e.mobileMessage(), denied(e))) }
            } finally {
                if (epoch == originalEpoch && request == dataChoiceSequence) mutable.update { it.copy(dataChoiceLoading = false) }
            }
        }
    }
    private fun <T> page(old: PageState<T>, result: MobilePage<T>, more: Boolean): PageState<T> = old.completed(result, more)
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
        update { it.copy(uploading = true,error = null,uploadPercent = null) }
        photoJobs[key] = scope.launch {
            try {
                val photo = photoBytes[key] ?: localDrafts?.photo(owner ?: 0L,WorkspacePhoto(key,item.name,item.mime,private = storedScope.first,safety = storedScope.second))?.also { photoBytes[key] = it }
                    ?: throw LocalDraftException("原照片不可读，请重新读取或移除后选择")
                persistDrafts()
                val progress: (Long, Long?) -> Unit = { sent, total -> update { it.withProgress(sent,total) } }
                val result = if (storedScope.second) api.uploadSafetyEvidence(photo,key,safetyToken(route),progress) else if (storedScope.first) api.uploadPrivate(photo,key,progress) else api.upload(photo,key,progress)
                update { it.copy(mediaId = result.id,uploading = false) }
                persistDrafts()
                if (epoch == originalEpoch) photoBytes.remove(key)
            } catch (e: CancellationException) { throw e } catch (e: Exception) { update { it.copy(uploading = false,error = e.mobileMessage()) } }
        }
    }
    suspend fun localPhotoPreview(route: FeatureRoute, key: String): SelectedPhoto? {
        if (route != mutable.value.route) return null
        val originalEpoch = epoch
        val item = mutable.value.photos[route].orEmpty().firstOrNull { it.key == key } ?: return null
        val scope = photoScopes[key] ?: (false to false)
        val photo = photoBytes[key] ?: localDrafts?.photo(owner ?: 0L, WorkspacePhoto(key,item.name,item.mime,private = scope.first,safety = scope.second))
        return photo.takeIf { epoch == originalEpoch && route == mutable.value.route }
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
    override fun close() { guardianInvites.close(); age.close(); sensitive.close(); super.close() }
}
