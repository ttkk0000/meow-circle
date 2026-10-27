package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

@Composable fun OfferFeature(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f = store.features; val actor = store.state.value.user?.id
    if(route.page == FeaturePage.OfferEdit) AdultRequirement(store,route,s.offer?.version ?: 0)
    when (route.page) {
        FeaturePage.Offers -> {
            Text("逐项了解主体、宠物来源、地点与全部费用。M&D 不提供付款或担保。")
            FeatureButton("发布有偿信息",actor != null) { f.open(FeatureRoute(FeaturePage.OfferEdit)) }
            s.offers.items.forEach { record -> FeatureSection("${natureLabel(record.data.nature)} · ${record.data.city}") {
                Text("${record.data.custodian} · ${record.data.origin}"); Text("基础金额 ${money(record.data.priceCents)} · 含必付项 ${money(record.data.totalCents)}")
                FeatureButton("查看完整披露") { f.open(FeatureRoute(FeaturePage.Offer,record.id)) }
            } }
            PageFooter(s.offers, { f.reload() }, { f.reload(true) }, "暂无可见信息", enabled = !s.loading && !s.busy)
        }
        FeaturePage.Offer -> s.offer?.let { record ->
            val o = record.data
            FeatureSection("${natureLabel(o.nature)} · ${stateLabel(record.state)}") {
                Text("${o.city} · ${o.area}\n宠物档案 #${o.petId} V${o.petVersion}\n有效至 ${displayTime(o.validUntil)}")
                record.mediaIds.forEach { MobilePhoto(store,it,actor,"宠物与披露照片") }
            }
            FeatureSection("主体与关系") { Text("发布主体：${publisherLabel(o.publisherType)}\n发布关系：${relationshipLabel(o.relationship)}\n授权：${o.authoritySummary}\n实际照护：${o.custodian} · ${o.custodianRelationship}\n宠物来源：${o.origin}\n接待：${o.reception} · ${o.receptionRelationship}\n地点类型：${o.venueType}\n责任方：${o.responsibleParty}") }
            FeatureSection("健康、后续与费用") {
                Text("已知健康：${o.knownHealth}\n后续照护：${o.aftercare}\n基础金额 ${money(o.priceCents)}")
                o.fees.forEach { Text("${it.name} ${money(it.amountCents)} · ${if (it.required) "必付" else "可选"}\n${it.paidBy} · ${it.dueWhen}") }
                Text("基础金额与必付项总额 ${money(o.totalCents)}\n收款方：${o.payee}\n订金条款：${o.depositTerms}")
                Text("此页提供信息披露，不提供付款、托管或交易担保。")
            }
            FeatureSection("按事项核实") {
                if (s.verifications.isEmpty()) Text("暂无有效核实结果；已上传不等于已核实。")
                s.verifications.forEach { Text("${checkLabel(it.check)} · ${stateLabel(it.state)}\n${it.summary}\n对象版本 ${it.targetVersion} · 有效至 ${it.validUntil?.let(::displayTime) ?: "未提供"}") }
            }
            if (record.ownerId == actor) {
                FeatureButton("编辑披露") { f.open(FeatureRoute(FeaturePage.OfferEdit,record.id)) }
                FeatureButton("提交具体事项核实") { f.open(FeatureRoute(FeaturePage.Review,kind = "offers",peer = record.id)); f.edit("type","verification") }
                FeatureButton("撤下该信息",!s.busy) { f.confirm("撤下信息","披露 #${record.id} V${record.version}；已约定安排需在预约中分别处理。") { f.api.removeResource("offers",record.id,record.version); f.finishRoute() } }
            } else {
                FeatureButton("提议到访预约",actor != null && record.state == "published") { f.open(FeatureRoute(FeaturePage.AppointmentEdit,kind = "offer",peer = record.id)); f.edit("peer",record.ownerId.toString()) }
                FeatureButton("举报或求助") { f.open(FeatureRoute(FeaturePage.Safety,kind = "offers",peer = record.id)) }
            }
        }
        FeaturePage.OfferEdit -> {
            if (route.id > 0 && s.offer == null) return
            val old = s.offer; val o = old?.data
            PetChoiceField(f,s,"petId",o?.petId ?: 0,versionKey = "petVersion",fallbackVersion = o?.petVersion ?: 0)
            Text("宠物资料变化后须重新核对版本；发布人须具备有效处置资格。")
            ChoiceField(f,s,"nature","性质",listOf("adoption" to "领养（不得收取基础价或必付费用）","sale" to "出售","paid_transfer" to "有偿转让"),o?.nature ?: "sale")
            ChoiceField(f,s,"publisher","主体",listOf("person" to "个人","business" to "经营主体","rescue" to "救助组织"),o?.publisherType ?: "person")
            ChoiceField(f,s,"relationship","关系",listOf("direct" to "直接发布","employee" to "工作人员","agent" to "代理"),o?.relationship ?: "direct")
            FormField(f,s,"authority","处置与发布授权说明",o?.authoritySummary.orEmpty(),3)
            FormPair({ FormField(f,s,"custodian","实际照护者",o?.custodian.orEmpty()) }, { FormField(f,s,"custodianRelation","照护者与主体关系",o?.custodianRelationship.orEmpty()) })
            FormField(f,s,"origin","宠物来源",o?.origin.orEmpty(),3); CityFormField(f,s,o?.city.orEmpty(),allowEmpty = false); FormField(f,s,"area","公开区域",o?.area.orEmpty())
            FormField(f,s,"venue","地点类型",o?.venueType.orEmpty()); FormField(f,s,"privateAddress","私密准确地址（仅授权参与方）",o?.privateAddress.orEmpty())
            FormPair({ FormField(f,s,"reception","接待方",o?.reception.orEmpty()) }, { FormField(f,s,"receptionRelation","接待方与主体关系",o?.receptionRelationship.orEmpty()) })
            FormPair({ FormField(f,s,"payee","收款方",o?.payee.orEmpty()) }, { FormField(f,s,"responsible","实际责任方",o?.responsibleParty.orEmpty()) })
            FormField(f,s,"health","已知健康情况与未知项",o?.knownHealth.orEmpty(),4); FormField(f,s,"aftercare","后续照护与异常处理",o?.aftercare.orEmpty(),3)
            FormField(f,s,"price","基础金额（元）",o?.priceCents?.let { centsText(it) } ?: "0",numeric = true)
            FeeFields(f,s,o?.fees.orEmpty())
            val previewPrice = cents(f.field("price",o?.priceCents?.let { centsText(it) } ?: "0")); val previewFees = formFees(f,o?.fees.orEmpty())
            if (previewPrice != null && previewFees != null) Text("基础金额＋必付项合计 ${money(previewPrice + previewFees.filter { it.required }.sumOf { it.amountCents })}")
            FormField(f,s,"deposit","订金／退还条款（没有请说明）",o?.depositTerms.orEmpty(),3); DateTimeField(f,s,"until","有效期至（最多90天）",o?.validUntil.orEmpty())
            FeaturePhotos(store,s,old?.mediaIds.orEmpty())
            FeaturePrimary("核对并保存披露",!s.busy && !s.loading) {
                val petId = f.field("petId",o?.petId?.toString().orEmpty()).toLongOrNull(); val petVersion = f.field("petVersion",o?.petVersion?.toString().orEmpty()).toLongOrNull()
                val price = cents(f.field("price",o?.priceCents?.let { centsText(it) } ?: "0")); val fees = formFees(f,o?.fees.orEmpty()); val until = dateTime(f.field("until",o?.validUntil.orEmpty()))
                val textFields = listOf("authority" to o?.authoritySummary,"custodian" to o?.custodian,"custodianRelation" to o?.custodianRelationship,"origin" to o?.origin,"city" to o?.city,"area" to o?.area,"venue" to o?.venueType,"reception" to o?.reception,"receptionRelation" to o?.receptionRelationship,"payee" to o?.payee,"responsible" to o?.responsibleParty,"health" to o?.knownHealth,"aftercare" to o?.aftercare,"deposit" to o?.depositTerms)
                if (petId == null || petId <= 0 || petVersion == null || petVersion <= 0 || price == null || fees == null || until == null || textFields.any { f.field(it.first,it.second.orEmpty()).isBlank() }) f.notify("请完整填写披露、有效版本、金额及有效期") else {
                    val total = price + fees.filter { it.required }.sumOf { it.amountCents }
                    if (total > 100000000 || f.field("nature",o?.nature ?: "sale") == "adoption" && total > 0) f.notify("请检查费用总额。领养中的收费项目须走私密协助核对。") else {
                        val body = OfferBody(petId,petVersion,f.field("nature",o?.nature ?: "sale"),f.field("publisher",o?.publisherType ?: "person"),f.field("relationship",o?.relationship ?: "direct"),
                            f.field("authority",o?.authoritySummary.orEmpty()),f.field("custodian",o?.custodian.orEmpty()),f.field("custodianRelation",o?.custodianRelationship.orEmpty()),f.field("origin",o?.origin.orEmpty()),f.field("city",o?.city.orEmpty()),f.field("area",o?.area.orEmpty()),f.field("venue",o?.venueType.orEmpty()),f.field("reception",o?.reception.orEmpty()),f.field("receptionRelation",o?.receptionRelationship.orEmpty()),f.field("payee",o?.payee.orEmpty()),f.field("responsible",o?.responsibleParty.orEmpty()),f.field("health",o?.knownHealth.orEmpty()),f.field("aftercare",o?.aftercare.orEmpty()),price,fees = fees,totalCents = total,depositTerms = f.field("deposit",o?.depositTerms.orEmpty()),validUntil = until,privateAddress = f.field("privateAddress",o?.privateAddress.orEmpty()))
                        val input = OfferInput(body,newRequestKey(),f.version(old?.version ?: 0),f.mediaIds(old?.mediaIds.orEmpty()))
                        f.confirm("保存披露","${natureLabel(body.nature)} · ${body.city}\n${body.custodian} · ${body.relationship}\n含必付项总额 ${money(total)}\n信息变更不会直接改写已确认预约快照。") { val result = if (old == null) f.api.createOffer(input) else f.api.editOffer(old.id,input); f.completeTo(FeatureRoute(FeaturePage.Offer,result.id)) }
                    }
                }
            }
        }
        else -> Unit
    }
}
@Composable fun FeeFields(f: MobileFeatureStore,s: FeatureState,original: List<Fee> = emptyList()) {
    val count = f.field("feeCount",original.size.toString()).toIntOrNull()?.coerceIn(0,30) ?: 0
    Text("费用明细（人民币）")
    repeat(count) { index ->
        val old = original.getOrNull(index)
        FeatureSection("费用 ${index + 1}") {
            FormField(f,s,"feeName$index","名称",old?.name.orEmpty()); FormField(f,s,"feeAmount$index","金额（元）",old?.amountCents?.let { centsText(it) } ?: "0",numeric = true)
            CheckLine("此项为必付费用",f.field("feeRequired$index",(old?.required ?: true).toString()).toBoolean(),{ f.edit("feeRequired$index",it.toString()) },!s.busy)
            FormField(f,s,"feePayer$index","由谁承担",old?.paidBy.orEmpty()); FormField(f,s,"feeWhen$index","何时支付",old?.dueWhen.orEmpty())
        }
    }
    if (count < 30) FeatureButton("添加费用明细",!s.busy) { f.edit("feeCount",(count + 1).toString()) }
    if (count > 0) FeatureButton("移除最后一项费用",!s.busy) { f.edit("feeCount",(count - 1).toString()) }
}
fun formFees(f: MobileFeatureStore,original: List<Fee> = emptyList()): List<Fee>? {
    val count = f.field("feeCount",original.size.toString()).toIntOrNull()?.takeIf { it in 0..30 } ?: return null
    return (0 until count).map { index -> val old = original.getOrNull(index); val amount = cents(f.field("feeAmount$index",old?.amountCents?.let { centsText(it) } ?: "0")) ?: return null
        Fee(f.field("feeName$index",old?.name.orEmpty()),amount,f.field("feeRequired$index",(old?.required ?: true).toString()).toBoolean(),f.field("feePayer$index",old?.paidBy.orEmpty()),f.field("feeWhen$index",old?.dueWhen.orEmpty())).takeIf { listOf(it.name,it.paidBy,it.dueWhen).none(String::isBlank) } ?: return null }
}
fun checkLabel(v: String) = mapOf("account" to "账号事项","publication_authority" to "发布授权","pet_recent" to "宠物近期情况","location" to "地点","medical_document" to "医疗文件","medical_source" to "医疗来源")[v] ?: v
private fun natureLabel(v: String) = mapOf("adoption" to "领养","sale" to "出售","paid_transfer" to "有偿转让")[v] ?: v
private fun publisherLabel(v: String) = mapOf("person" to "个人","business" to "经营主体","rescue" to "救助组织")[v] ?: v
private fun relationshipLabel(v: String) = mapOf("direct" to "直接发布","employee" to "工作人员","agent" to "代理")[v] ?: v
