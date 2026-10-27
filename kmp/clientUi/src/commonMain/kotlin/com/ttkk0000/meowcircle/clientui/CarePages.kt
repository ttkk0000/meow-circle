package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

@Composable fun CareFeature(store: MobileStore, s: FeatureState, route: FeatureRoute) {
    val f = store.features; val actor = store.state.value.user?.id
    if(route.page in setOf(FeaturePage.Appointment,FeaturePage.AppointmentEdit,FeaturePage.ContractEdit,FeaturePage.ReturnEdit)) AdultRequirement(store,route,s.appointment?.version ?: s.adoption?.version ?: 0)
    if(route.page == FeaturePage.AdoptionWork && route.kind == "care") {
        Text("仅当前授权事项的私密照护近况；协议、申请材料与公开档案不在此权限中。")
        s.care.items.forEach { update -> FeatureSection(if(update.data.needsHelp) "需要协助" else "照护近况") { Text(update.data.text); update.mediaIds.forEach { MobilePhoto(store,it,actor,"授权照护照片") } } }
        PageFooter(s.care, { f.reload() }, { f.reload(true) }, "暂无私密照护近况", enabled = !s.loading && !s.busy)
        FormField(f,s,"careText","补充照护近况",lines = 5)
        CheckLine("需要协助",f.field("needsHelp","false").toBoolean(),{ f.edit("needsHelp",it.toString()) },!s.busy)
        FeaturePhotos(store,s,private = true)
        FeatureButton("提交私密近况",!s.busy && f.field("careText").isNotBlank()) { val input = CareInput(f.field("careText"),f.field("needsHelp","false").toBoolean(),newRequestKey(),f.mediaIds()); f.confirm("提交授权事项的照护近况",input.text) { f.api.care(route.id,input); f.discardAndReload() } }
        return
    }
    if(route.page == FeaturePage.AdoptionWork && s.confirmation != null) {
        AdoptionConfirmationPage(store,s)
        return
    }
    when (route.page) {
        FeaturePage.Appointments -> {
            Text("当前有效安排、待确认提案与历史版本分别显示。")
            s.appointments.items.forEach { row -> FeatureSection("预约 #${row.id} · ${stateLabel(row.state)}") {
                Text(row.data.effectiveTerms?.let { "有效 V${row.data.effectiveVersion} · ${displayTime(it.startsAt)} · ${it.place}" } ?: "双方尚未确认有效安排")
                if (row.data.pendingVersion > 0) Text("待确认提案 V${row.data.pendingVersion}")
                FeatureButton("查看安排") { f.open(FeatureRoute(FeaturePage.Appointment,row.id)) }
            } }
            PageFooter(s.appointments, { f.reload() }, { f.reload(true) }, "暂无预约，请从宠物、领养申请、互助或有偿信息中发起。", enabled = !s.loading && !s.busy)
        }
        FeaturePage.Appointment -> AppointmentDetailPage(store,s,route)
        FeaturePage.AppointmentEdit -> {
            if (route.id > 0 && s.appointment == null) return
            val original = s.appointment?.data?.let { it.pendingTerms ?: it.effectiveTerms }
            val safety = s.appointment?.state == "safety_hold"
            if(s.conflict) FeatureButton("保留原说明并采用已读取预约版本",!s.busy && !s.loading && !s.unconfirmedResult) {f.adoptAppointmentVersion()}
            if(safety) {
                TaskNotice("安全暂停尚未解除","核对具体问题、更正与披露后提出新安排；双方确认同一新版本前继续不出发。",warning = true)
                FormField(f,s,"safetyResolution","具体问题、更正与本人核对说明",lines = 4)
            }
            AppointmentFields(f,s,original)
            if (route.id == 0L) Text("从当前事项发起预约；对象与参与人由此入口绑定。")
            FeaturePrimary(if(safety) "提交新提案，继续保持暂停" else "核对预约提案",!s.busy && !s.loading && !s.unconfirmedResult && !s.conflict && (!safety || f.field("safetyResolution").isNotBlank())) {
                val terms = appointmentTerms(f,original)
                if (terms == null) f.notify("请选择时间并填写地点、接待方、宠物、全部费用及收款方。金额最多两位小数。") else {
                    val record = s.appointment
                    val context = if (record == null) "${route.kind} #${route.peer}" else "${record.data.contextKind} #${record.data.contextId}"
                    val input = record?.let { AppointmentAction(f.version(it.version),newRequestKey(),terms = terms,note = if(safety) f.field("safetyResolution") else "") }
                    val creation = if (record == null) AppointmentInput(route.kind,route.peer,f.field("peer").toLongOrNull() ?: 0,terms,newRequestKey()) else null
                    f.confirm("提交预约提案","$context\n${displayTime(terms.startsAt)} · ${terms.place}\n接待：${terms.reception}\n费用 ${money(terms.amountCents)}，到访费 ${money(terms.visitFeeCents)}\n${terms.requiredFees}\n${input?.note.orEmpty()}\n${if(safety) "安全暂停保持，双方须核对并确认同一新版本。" else "双方确认前，新提案不会覆盖原有效安排。"}") {
                        val result = if (record == null) { val request = requireNotNull(creation); f.requireAppointmentContext(request.contextKind,request.contextId,request.peerId); f.api.createAppointment(request) } else f.api.appointmentAction(record.id,if(safety) "safety_repropose" else "propose",input!!)
                        f.completeTo(FeatureRoute(FeaturePage.Appointment,result.id))
                    }
                }
            }
            if (route.id == 0L) Text("参与人由原事项绑定，提交时会重新核对；不需要先加好友。")
            FeatureButton("查看全部预约") { f.open(FeatureRoute(FeaturePage.Appointments)) }
        }
        FeaturePage.CaseEdit -> {
            if (route.id > 0 && s.case == null) return
            val old = s.case; val body = old?.data
            if (old == null) ChoiceField(f,s,"type","求助类型",listOf("lost" to "我的宠物走失","found" to "拾获，等待认领"),"lost") else Text(if (body?.type == "lost") "走失求助" else "拾获待认领")
            FormField(f,s,"title","标题",body?.title.orEmpty())
            ChoiceField(f,s,"species","物种",listOf("cat" to "猫咪","dog" to "狗狗"),body?.species ?: "cat")
            FormField(f,s,"description","特征与经过",body?.description.orEmpty(),5)
            RegionFields(f,s,body?.city.orEmpty(),body?.area.orEmpty())
            if (body != null && body.locationPrecision != "area") {
                val sameRegion = selectedCity(f,body.city) == body.city && f.field("area",body.area) == body.area
                CheckLine("保留原来主动提供的大致地点",sameRegion && f.field("keepPoint","true").toBoolean(),{ f.edit("keepPoint",it.toString()) },!s.busy && sameRegion)
                Text("取消或修改城市／区域后，本次只公开文字区域，不再沿用原地点。")
            }
            CaseMapPicker(f,s,body)
            FormField(f,s,"privateLocation","私密位置补充（仅授权处理）",body?.privateLocation.orEmpty(),2)
            DateTimeField(f,s,"occurred","发生时间",body?.occurredAt.orEmpty())
            PetChoiceField(f,s,"pet",body?.petId ?: 0,optional = true)
            FeaturePhotos(store,s,old?.mediaIds.orEmpty())
            FeaturePrimary("核对并发布求助",!s.busy && !s.loading) {
                val keepPoint = body != null && body.locationPrecision != "area" && f.field("keepPoint","true").toBoolean() &&
                    selectedCity(f,body.city) == body.city && f.field("area",body.area) == body.area
                val picked = f.pickedCaseMapPoint(selectedCity(f,body?.city.orEmpty()),f.field("area",body?.area.orEmpty()))
                val lat = picked?.latitude ?: if(keepPoint) body!!.latitude else 0.0
                val lon = picked?.longitude ?: if(keepPoint) body!!.longitude else 0.0
                val time = dateTime(f.field("occurred",body?.occurredAt.orEmpty()))
                val title = f.field("title",body?.title.orEmpty()); val text = f.field("description",body?.description.orEmpty()); val city = selectedCity(f,body?.city.orEmpty())
                if (time == null || title.isBlank() || text.isBlank() || city.isBlank() || f.field("area",body?.area.orEmpty()).isBlank()) f.notify("请检查标题、特征、城市、大致区域和发生时间") else {
                    val input = CaseInput(CaseBody(body?.type ?: f.field("type","lost"),title,f.field("species",body?.species ?: "cat"),text,city,f.field("area",body?.area.orEmpty()),lat,lon,time,
                        petId = f.field("pet",body?.petId?.toString().orEmpty()).toLongOrNull() ?: 0,privateLocation = f.field("privateLocation",body?.privateLocation.orEmpty()),locationPrecision = if(picked != null || keepPoint) "approximate" else "area",coordinateSystem = picked?.coordinateSystem ?: if(keepPoint) body!!.coordinateSystem else ""),newRequestKey(),f.version(old?.version ?: 0),f.mediaIds(old?.mediaIds.orEmpty()))
                    val locationSummary = if(picked != null) "公开本次主动选择的大致区域" else if(keepPoint) "公开位置保留原先的大致地点" else "只公开城市与文字区域，不包含地点坐标"
                    f.confirm("发布求助","$title\n$city · ${input.case.area}\n$locationSummary，私密补充和认领材料不会公开。") { val result = if (old == null) f.api.createCase(input) else f.api.editCase(old.id,input); store.reloadDetail(); f.completeTo(FeatureRoute(FeaturePage.CaseWork,result.id)) }
                }
            }
        }
        FeaturePage.CaseWork -> s.case?.let { record ->
            FeatureSection(record.data.title) { Text(stateLabel(record.state)); Text("${record.data.city} · ${record.data.area}"); Text(record.data.description) }
            if (record.ownerId == actor) {
                if (record.state == "active") FeatureButton("编辑公开求助") { f.open(FeatureRoute(FeaturePage.CaseEdit,record.id)) }
                FormField(f,s,"closeReason","结束公开求助的原因",lines = 3)
                FeatureButton("结束公开求助",!s.busy && f.field("closeReason").isNotBlank() && record.state == "active") {
                    val input = VersionedAction(f.version(record.version),f.field("closeReason"))
                    f.confirm("结束公开求助","${record.data.title}\n${input.note}\n涉及认领时仍须遵守双方交接流程。") { f.api.closeCase(record.id,input); f.reload(); store.reloadDetail() }
                }
            } else if (actor != null && record.state in listOf("active","matched")) {
                FeatureButton("提供私密目击线索",!s.busy) { f.open(FeatureRoute(FeaturePage.LeadSubmit,record.id)) }
                if (record.data.type == "found" && record.state == "active") {
                    FeaturePhotos(store,s,private = true)
                    FormField(f,s,"petName","我认领的宠物名字"); FormField(f,s,"evidence","所有权证据说明（私密）",lines = 5)
                    FeatureButton("提交认领材料",!s.busy && f.field("petName").isNotBlank() && f.field("evidence").isNotBlank()) {
                        val input = ClaimInput(f.field("petName"),f.field("evidence"),newRequestKey(),f.mediaIds())
                        f.confirm("提交认领材料","${input.petName}\n${input.evidence}\n匹配材料不等于所有权确认，交接须双方主动确认。") { val result = f.api.claim(record.id,input); f.completeTo(FeatureRoute(FeaturePage.Claim,result.id)) }
                    }
                }
            }
            Text("仅展示你有权查看的线索与认领材料。")
            if (actor != null) {
                Text("认领材料")
                s.claims.items.forEach { claim -> FeatureButton("认领 #${claim.id} · ${stateLabel(claim.state)}") { f.open(FeatureRoute(FeaturePage.Claim,claim.id)) } }
                PageFooter(s.claims, { f.reload(material = CaseMaterial.Claims) }, { f.reload(true, CaseMaterial.Claims) },
                    "暂无你有权查看的认领材料", enabled = !s.loading && !s.busy)
                Text("私密线索")
                s.leads.items.forEach { lead -> NavigationRow("${leadAssessmentLabel(lead.data.assessment)} · ${displayTime(lead.createdAt)}",lead.data.text.take(100),compact = true) { f.open(FeatureRoute(FeaturePage.Lead,lead.id)) } }
                FeatureButton("查看本案私密线索列表") { f.open(FeatureRoute(FeaturePage.LeadInbox,record.id)) }
                PageFooter(s.leads, { f.reload(material = CaseMaterial.Leads) }, { f.reload(true, CaseMaterial.Leads) },
                    "暂无你有权查看的线索", enabled = !s.loading && !s.busy)
            }
            FeatureButton("为此事项提议预约",actor != null && record.ownerId != actor) { f.open(FeatureRoute(FeaturePage.AppointmentEdit,kind = "case",peer = record.id)); f.edit("peer",record.ownerId.toString()) }
        }
        FeaturePage.LeadInbox -> LeadInboxPage(store,s,route)
        FeaturePage.Lead -> LeadDetailPage(store,s,route)
        FeaturePage.LeadSubmit -> LeadSubmissionPage(store,s,route)
        FeaturePage.Claim -> s.claim?.let { claim ->
            val publisher = s.case?.ownerId == actor; val mine = claim.ownerId == actor
            FeatureSection("${claim.data.petName} · ${stateLabel(claim.state)}") {
                Text(claim.data.evidence)
                Text("发布人确认：${if (claim.data.publisherConfirmed) "已确认" else "未确认"} · 认领人确认：${if (claim.data.claimantConfirmed) "已确认" else "未确认"}")
                claim.mediaIds.forEach { MobilePhoto(store,it,actor,"私密认领材料") }
            }
            FormField(f,s,"note","补充材料／处理说明",lines = 4)
            if (mine) FeaturePhotos(store,s,claim.mediaIds,private = true)
            val actions = buildList {
                if (publisher && claim.state == "submitted") { add("match" to "材料匹配，进入交接核对"); add("request_supplement" to "请对方补充") }
                if (publisher && claim.state in listOf("submitted","needs_info")) add("reject" to "拒绝当前认领")
                if (mine && claim.state == "needs_info") add("supplement" to "提交补充")
                if (mine && claim.state in listOf("rejected","withdrawn")) add("resubmit" to "重新提交原认领")
                if (mine && claim.state in listOf("submitted","needs_info")) add("withdraw" to "撤回认领")
                if (claim.state in listOf("matched","handover_pending")) {
                    if (publisher && !claim.data.publisherConfirmed || mine && !claim.data.claimantConfirmed) add("confirm_handover" to "确认我方已经完成交接")
                    add("dispute" to "存在争议，暂停交接")
                }
            }
            actions.forEach { (action,label) -> FeatureButton(label,!s.busy) {
                val note = f.field("note")
                if (action in listOf("request_supplement","supplement","resubmit") && note.isBlank()) f.notify("请先填写补充说明") else {
                    val input = VersionedAction(f.version(claim.version),note,if (mine) f.mediaIds(claim.mediaIds) else emptyList(),newRequestKey())
                    f.confirm(label,"认领 #${claim.id} · 版本 ${claim.version}\n$note\n单方确认不能表示双方交接完成。") { f.api.claimAction(claim.id,action,input); f.discardAndReload() }
                }
            } }
        }
        FeaturePage.AdoptionWork -> s.adoption?.let { record ->
            val body = record.data; val party = actor == body.publisherId || actor == body.applicantId
            AdoptionProgressPage(store,s,record)
            body.contracts.orEmpty().forEach { contract -> FeatureSection("协议 V${contract.number} · ${stateLabel(contract.status)}") {
                ContractSummary(contract.terms); Text("已接受：${contract.acceptedBy.joinToString { "用户 #$it" }}")
                if (party && contract.status == "pending" && actor !in contract.acceptedBy) {
                    FeatureButton("核对并接受此版协议",!s.busy) { f.prepareSensitive(SensitiveIntent.Contract(record.id,ContractInput(record.version,newRequestKey(),revision = contract.number),contractReview(contract.terms))) }
                    FeatureButton("拒绝此版提案",!s.busy) { val input = ContractInput(record.version,newRequestKey(),revision = contract.number); f.confirm("拒绝协议提案","V${contract.number}；已有有效协议不会被此操作替代。") { f.api.proposeContract(record.id,"decline",input); f.discardAndReload() } }
                }
            } }
            if (party && record.state in listOf("approved","trial_proposed","trial_active","handover_pending")) {
                FeatureButton("提出新协议") { f.open(FeatureRoute(FeaturePage.ContractEdit,record.id)) }
            }
            if (record.state in listOf("return_requested","return_agreed")) {
                if (party) FeatureButton("提出安全接回方案") { f.open(FeatureRoute(FeaturePage.ReturnEdit,record.id)) }
                body.returnPlans.orEmpty().forEach { plan -> FeatureSection("接回 V${plan.number} · ${stateLabel(plan.state)}") {
                    Text("${displayTime(plan.terms.startsAt)} · ${plan.terms.place}\n接待 ${plan.terms.reception}\n接回前照护 ${plan.terms.careUntil}\n费用 ${plan.terms.costs}\n安全 ${plan.terms.safety}")
                    val proposalValid = futureTime(plan.terms.startsAt)
                    Text("方案确认 ${plan.confirmations.size}/2 · 实际交接确认 ${plan.handover.size}/2")
                    if (plan.state == "pending") Text(if (proposalValid) "确认截止：${displayTime(plan.terms.startsAt)}；原有效安排仍保留。" else "本次未生效提案已过期，请重新协商；当前照护与求助继续。")
                    val actions = buildList {
                        if (party && plan.state == "pending") {
                            if (proposalValid && actor !in plan.confirmations) add("accept" to "确认此版接回方案")
                            if (actor == plan.proposedBy) add("withdraw" to "撤回本人未生效提案")
                            else if (proposalValid) add("decline" to "拒绝此版方案")
                        }
                        if (party && plan.state == "effective" && body.effectiveReturn == plan.number && actor !in plan.handover) add("handover" to "确认我方已完成接回交接")
                    }
                    actions.forEach { (action,label) -> FeatureButton(label,!s.busy) { val input = ReturnInput(record.version,newRequestKey(),plan.number); f.confirm(label,returnPlanReview(plan,body)) { f.api.returnPlan(record.id,action,input); f.discardAndReload(); store.reloadDetail() } } }
                } }
            }
            FeatureSection("私密照护近况") {
                s.care.items.forEach { update -> Text("${if (update.data.needsHelp) "需要协助 · " else ""}${update.data.text}"); update.mediaIds.forEach { MobilePhoto(store,it,actor,"私密照护照片") } }
                PageFooter(s.care, { f.reload() }, { f.reload(true) }, "暂无私密照护近况", enabled = !s.loading && !s.busy)
                if(actor == body.applicantId && body.careStageAvailable(record.state)) {
                FormField(f,s,"careText","近况与照护记录",lines = 5)
                CheckLine("需要协助",f.field("needsHelp","false").toBoolean(),{ f.edit("needsHelp",it.toString()) },!s.busy)
                FeaturePhotos(store,s,private = true)
                FeatureButton("提交私密照护近况",!s.busy && f.field("careText").isNotBlank()) {
                    val input = CareInput(f.field("careText"),f.field("needsHelp","false").toBoolean(),newRequestKey(),f.mediaIds())
                    f.confirm("提交照护近况",input.text) { f.api.care(record.id,input); f.discardAndReload() }
                }
            }
            }
            if (actor == body.applicantId) FeatureButton("邀请照护协作者") { f.open(FeatureRoute(FeaturePage.Collaborators,record.id,"care")) }
            FeatureButton("提议领养事项预约",party) { f.open(FeatureRoute(FeaturePage.AppointmentEdit,kind = "adoption",peer = record.id)); f.edit("peer",(if (actor == body.applicantId) body.publisherId else body.applicantId).toString()) }
        }
        FeaturePage.ContractEdit -> s.adoption?.let { record ->
            if(s.conflict || record.version > f.version(record.version)) FeatureButton("保留协议草稿并采用已读版本",!s.busy && !s.loading && !s.unconfirmedResult) {f.adoptAdoptionVersion()}
            FormField(f,s,"city","适用城市"); ChoiceField(f,s,"species","物种",listOf("cat" to "猫咪","dog" to "狗狗"),"cat")
            ChoiceField(f,s,"nature","转让性质",listOf("adoption" to "领养","sale" to "出售","paid_transfer" to "有偿转让"),"adoption")
            FormField(f,s,"responsibilities","双方照护与交接责任",lines = 4); FormField(f,s,"health","已知健康情况及未知事项",lines = 4)
            FormField(f,s,"returns","退养与安全接回条款",lines = 4); FormField(f,s,"amount","基础金额（元）","0",numeric = true)
            FormField(f,s,"payee","收款方／无款项说明"); FeeFields(f,s)
            DateTimeField(f,s,"trialUntil","试养截止（选填）",optional = true)
            FormField(f,s,"offerId","有偿信息编号（如适用）",numeric = true); FormField(f,s,"offerVersion","披露版本（如适用）",numeric = true)
            FeaturePrimary("提交协议提案",!s.busy) {
                val amount = cents(f.field("amount","0")); val fees = formFees(f); val until = f.field("trialUntil")
                if (amount == null || fees == null || until.isNotBlank() && dateTime(until) == null || listOf("city","responsibilities","health","returns","payee").any { f.field(it).isBlank() }) f.notify("请完整填写责任、健康、接回、城市及金额明细") else {
                    val terms = ContractTerms(f.field("city"),f.field("species","cat"),f.field("nature","adoption"),f.field("responsibilities"),f.field("health"),f.field("returns"),amount,fees,f.field("payee"),until.takeIf { it.isNotBlank() }?.let(::dateTime),f.field("offerId").toLongOrNull() ?: 0,f.field("offerVersion").toLongOrNull() ?: 0)
                    val input = ContractInput(f.version(record.version),newRequestKey(),terms)
                    f.confirm("提出新协议","${terms.responsibilities}\n基础金额 ${money(amount)}\n${terms.returnTerms}\n双方需分别核对并接受同一版本。") { f.api.proposeContract(record.id,"propose",input); f.completeTo(FeatureRoute(FeaturePage.AdoptionWork,record.id)) }
                }
            }
        }
        FeaturePage.ReturnEdit -> s.adoption?.let { record ->
            if(s.conflict || record.version > f.version(record.version)) FeatureButton("保留接回草稿并采用已读版本",!s.busy && !s.loading && !s.unconfirmedResult) {f.adoptAdoptionVersion()}
            DateTimeField(f,s,"starts","接回时间"); FormField(f,s,"place","接回地点"); FormField(f,s,"reception","接回人与接待方")
            FormField(f,s,"careUntil","接回前由谁照护及安排",lines = 3); FormField(f,s,"costs","费用承担",lines = 3); FormField(f,s,"safety","安全与异常联络安排",lines = 4)
            FeaturePrimary("提出安全接回方案",!s.busy) {
                val time = dateTime(f.field("starts"))
                if (time == null || listOf("place","reception","careUntil","costs","safety").any { f.field(it).isBlank() }) f.notify("请完整填写接回方案") else {
                    val terms = ReturnTerms(time,f.field("place"),f.field("reception"),f.field("careUntil"),f.field("costs"),f.field("safety")); val input = ReturnInput(f.version(record.version),newRequestKey(),terms = terms)
                    f.confirm("提出安全接回方案","${displayTime(time)} · ${terms.place}\n${terms.careUntil}\n${terms.costs}\n${terms.safety}\n双方确认方案后，仍需分别确认实际交接。") { f.api.returnPlan(record.id,"propose",input); f.completeTo(FeatureRoute(FeaturePage.AdoptionWork,record.id)) }
                }
            }
        }
        else -> Unit
    }
}
@Composable fun AppointmentSummary(t: AppointmentTerms) {
    Text("${displayTime(t.startsAt)}\n${t.city} · ${t.place}\n接待：${t.reception}\n宠物：${t.petName}\n金额 ${money(t.amountCents)} · 到访费 ${money(t.visitFeeCents)}\n必付项 ${t.requiredFees}\n收款方 ${t.payee}")
    t.snapshot?.let { Text("披露快照 #${it.offerId} V${it.offerVersion}；聊天中的变更不替代此快照。") }
}
@Composable private fun AppointmentFields(f: MobileFeatureStore,s: FeatureState,old: AppointmentTerms?) {
    DateTimeField(f,s,"starts","预约时间",old?.startsAt.orEmpty()); FormField(f,s,"city","城市",old?.city.orEmpty())
    ChoiceField(f,s,"species","物种",listOf("cat" to "猫咪","dog" to "狗狗"),old?.species?.ifBlank { "cat" } ?: "cat")
    FormPair({ FormField(f,s,"place","准确地点（仅预约参与方）",old?.place.orEmpty()) }, { FormField(f,s,"reception","接待方与关系",old?.reception.orEmpty()) })
    FormPair({ FormField(f,s,"petName","宠物名字",old?.petName.orEmpty()) }, { FormField(f,s,"amount","基础金额（元）",old?.amountCents?.let { centsText(it) } ?: "0",numeric = true) })
    FormField(f,s,"visitFee","到访费（元）",old?.visitFeeCents?.let { centsText(it) } ?: "0",numeric = true)
    FormField(f,s,"requiredFees","其他必付费用明细（没有请写无）",old?.requiredFees.orEmpty(),3); FormField(f,s,"payee","收款方／无款项说明",old?.payee.orEmpty())
}
private fun appointmentTerms(f: MobileFeatureStore,old: AppointmentTerms?): AppointmentTerms? {
    val time = dateTime(f.field("starts",old?.startsAt.orEmpty())) ?: return null
    val amount = cents(f.field("amount",old?.amountCents?.let { centsText(it) } ?: "0")) ?: return null
    val visit = cents(f.field("visitFee",old?.visitFeeCents?.let { centsText(it) } ?: "0")) ?: return null
    return AppointmentTerms(time,f.field("place",old?.place.orEmpty()),f.field("reception",old?.reception.orEmpty()),f.field("petName",old?.petName.orEmpty()),amount,visit,f.field("requiredFees",old?.requiredFees.orEmpty()),f.field("payee",old?.payee.orEmpty()),city = f.field("city",old?.city.orEmpty()),species = f.field("species",old?.species ?: "cat")).takeIf { listOf(it.place,it.reception,it.petName,it.requiredFees,it.payee,it.city).none(String::isBlank) }
}
@Composable fun ContractSummary(t: ContractTerms) { Text("${t.city} · ${if (t.species == "cat") "猫咪" else "狗狗"}\n${t.responsibilities}\n健康：${t.healthDisclosure}\n接回：${t.returnTerms}\n基础金额 ${money(t.amountCents)}\n收款方 ${t.payee}"); t.fees.forEach { Text("${it.name} ${money(it.amountCents)} · ${if (it.required) "必付" else "可选"} · ${it.paidBy} · ${it.dueWhen}") } }

fun contractReview(t: ContractTerms): String = "${t.city} · ${t.species} · ${t.nature}\n${t.responsibilities}\n健康：${t.healthDisclosure}\n接回：${t.returnTerms}\n基础金额 ${money(t.amountCents)} · 收款方 ${t.payee}\n" + t.fees.joinToString("\n") { "${it.name} ${money(it.amountCents)} · ${if(it.required) "必付" else "可选"} · ${it.paidBy} · ${it.dueWhen}" } + t.trialUntil?.let { "\n试养至 ${displayTime(it)}" }.orEmpty()
