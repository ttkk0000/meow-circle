package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttkk0000.meowcircle.client.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun AndroidFeatureContent(store: MobileStore, s: FeatureState) {
    val f = store.features
    val route = s.route ?: return
    var replaceSaved by remember(route) { mutableStateOf(false) }
    if(replaceSaved) MobileAlertDialog(onDismissRequest = { replaceSaved = false },title = { Text("读取本机已保存版本") },text = { Text("此账号所有表单的未保存输入将被放弃，读取本机已保存的表单和原附件。不会提交或重新上传。") },
        confirmButton = { TextButton({ replaceSaved = false; f.replaceWithSavedDrafts() }) { Text("放弃未保存输入并读取") } },dismissButton = { TextButton({ replaceSaved = false }) { Text("继续编辑") } })
    FeatureColumn {
        Text(route.page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (s.loading || s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        s.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        s.draftError?.let { Text(it,color = MaterialTheme.colorScheme.error) }
        if (!s.draftReady) {
            Text("正在读取此账号的本机私有表单；读取完成前保留原文件。")
            if (!s.draftRestoring) FeatureButton("重试读取本机表单") { f.restoreDrafts() }
            return@FeatureColumn
        }
        if (s.draftError != null) { FeatureButton("重试保存本机表单",!s.busy) { f.retryDraftSave() }; FeatureButton("读取本机已保存版本",!s.busy) { replaceSaved = true } }
        if (s.conflict) {
            Text("记录已变化。当前输入保留，先核对最新记录；放弃当前修改后才能采用新版本。")
            FeatureButton("放弃此表单修改并重新载入", !s.busy) { f.discardAndReload() }
        }
        if (s.unconfirmedResult) Text("此页保留了结果未知的操作，不能重复提交。可刷新核对实际记录；具备操作回执的请求请从待核对操作查询。",color = MaterialTheme.colorScheme.error)
        if (s.error != null) FeatureButton("重新读取", !s.busy) { f.reload() }
        if (s.pending.isNotEmpty() && route.page != FeaturePage.Recovery) FeatureButton("核对原操作结果", !s.busy) { f.open(FeatureRoute(FeaturePage.Recovery)) }
        when (route.page) {
            FeaturePage.EntryEdit, FeaturePage.Friends, FeaturePage.Member, FeaturePage.ProfileEdit, FeaturePage.PetEdit, FeaturePage.PetDiary,
            FeaturePage.Collaborators, FeaturePage.Invitations, FeaturePage.GroupCreate, FeaturePage.Group, FeaturePage.GroupDirectory, FeaturePage.GroupPreview, FeaturePage.Content -> SocialFeature(store, s, route)
            FeaturePage.Appointments, FeaturePage.Appointment, FeaturePage.AppointmentEdit, FeaturePage.CaseEdit,
            FeaturePage.CaseWork, FeaturePage.Claim, FeaturePage.AdoptionWork, FeaturePage.ContractEdit, FeaturePage.ReturnEdit -> CareFeature(store, s, route)
            FeaturePage.Offers, FeaturePage.Offer, FeaturePage.OfferEdit -> OfferFeature(store, s, route)
            else -> AccountFeature(store, s, route)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable internal fun FeatureDialogs(store: MobileStore, s: FeatureState) {
    val f = store.features
    s.confirmation?.let { pending ->
        MobileAlertDialog(onDismissRequest = f::dismissConfirmation, title = { Text(pending.title) },
            text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) { Text(pending.summary); s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { TextButton(onClick = f::submitConfirmation, enabled = !s.busy && !s.conflict && !s.unconfirmedResult && s.pending.none { it.phase in listOf("sent", "result_unknown") }) { Text(if (s.busy) "正在提交" else "确认提交") } },
            dismissButton = { TextButton(onClick = f::dismissConfirmation, enabled = !s.busy) { Text("返回修改") } })
    }
    SensitiveDialog(f)
}

@Composable private fun SensitiveDialog(f: MobileFeatureStore) {
    val s by f.sensitive.state.collectAsStateWithLifecycle()
    val common by f.state.collectAsStateWithLifecycle()
    if (s.stage == SensitiveStage.Idle) return
    var password by remember(s.clientRequestId) { mutableStateOf("") }
    var code by remember(s.clientRequestId) { mutableStateOf("") }
    var currentPhone by remember(s.clientRequestId) { mutableStateOf("") }
    var agreed by remember(s.clientRequestId) { mutableStateOf(false) }
    val busy = common.busy || s.stage in listOf(SensitiveStage.LoadingScope, SensitiveStage.Verifying, SensitiveStage.Submitting)
    MobileAlertDialog(onDismissRequest = { if (!busy) f.sensitive.dismiss() }, title = { Text("核对本次操作") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(sensitiveSummary(s.intent))
            s.scope?.let { Text("用途：${purposeLabel(it.purpose)} · 版本 ${it.resourceVersion}\n本次验证仅适用于当前内容。验证成功后仍需主动确认提交。") }
            if (s.scope != null && s.stage in listOf(SensitiveStage.Review, SensitiveStage.Failed)) {
                OutlinedTextField(password, { password = it }, label = { Text("当前密码") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(currentPhone, { currentPhone = it }, label = { Text("当前完整手机号（使用验证码时填写）") }, modifier = Modifier.fillMaxWidth())
                FeatureButton(if(common.otpWait > 0) "${common.otpWait} 秒后重发" else "发送本次用途验证码",!busy && !common.busy && common.otpWait == 0 && currentPhone.isNotBlank()) { f.sendOTP(phone = currentPhone,purpose = "reauth") }
                OutlinedTextField(code, { code = it }, label = { Text("本次用途验证码") }, modifier = Modifier.fillMaxWidth())
                FeatureButton("验证本次用途", !busy && (password.isNotBlank() || code.isNotBlank())) { f.sensitive.verify(password, code); password = ""; code = "" }
            }
            if (s.stage == SensitiveStage.ReadyToSubmit || s.stage == SensitiveStage.Review && s.scope == null) CheckLine("我已核对上述对象、范围与内容", agreed, { agreed = it })
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (s.stage == SensitiveStage.ResultUnknown) { Text("结果仍待核对，请保留原编号。暂未查到回执也不能据此重建请求。"); FeatureButton("核对原请求", true, f.sensitive::reconcile) }
            if (s.stage == SensitiveStage.Complete) Text("已提交。${s.resultId?.let { "记录 #$it" }.orEmpty()} 后续处理以服务端记录为准。")
        } },
        confirmButton = { if (s.stage in listOf(SensitiveStage.Review, SensitiveStage.ReadyToSubmit)) TextButton(onClick = { password = ""; code = ""; f.confirmSensitive(agreed) }, enabled = agreed && !busy && (s.scope == null || s.stage == SensitiveStage.ReadyToSubmit)) { Text("确认并提交") } },
        dismissButton = { TextButton(onClick = { password = ""; code = ""; f.sensitive.dismiss(); f.reload() }, enabled = !busy) { Text(if (s.stage == SensitiveStage.Complete) "完成" else "关闭") } })
}
private fun sensitiveSummary(intent: SensitiveIntent?): String = when (intent) {
    is SensitiveIntent.Account -> "账号安全：${mapOf("phone" to "更换手机号", "email" to "绑定邮箱", "password" to "更改密码", "unlink" to "解绑登录方式")[intent.action].orEmpty()}\n${intent.input.phone.ifBlank { intent.input.email }}${intent.input.identityId.takeIf { it > 0 }?.let { " · 登录方式 #$it" }.orEmpty()}\n成功后需要重新登录。"
    is SensitiveIntent.Guardian -> "监护授权 #${intent.id}：${if (intent.input.consent) "同意指定范围" else "拒绝"}。监护授权不会替代成年人资格。\n${intent.reviewSummary}"
    is SensitiveIntent.Contract -> "接受领养协议 #${intent.id} 的第 ${intent.input.revision} 版。只确认此版本，不代表双方已完成交接。\n${intent.reviewSummary}"
    is SensitiveIntent.DataRequest -> "${dataTypeLabel(intent.input.type)}\n字段：${intent.input.fields.joinToString()}\n指定记录：${intent.input.selections.joinToString { "${it.kind} #${it.id}" }}\n${intent.input.note}\n提交后可能需要人工处理或分类保全，请在受限进度中查看结果。"
    is SensitiveIntent.Rights -> "${rightsTypeLabel(intent.input.type)}\n目标 ${intent.input.targetKind} #${intent.input.targetId}\n原事项 #${intent.input.relatedCaseId}\n${intent.input.reason}\n${intent.input.statement}\n提交主体：${intent.input.legalName}\n具体权利：${intent.input.rightType}\n原事项版本：${intent.input.version}\n材料说明：${intent.input.evidenceDescription}\n材料 ${intent.input.mediaIds.size} 张，仅用于本次事项。"
    null -> "正在准备本次操作范围"
}
private fun purposeLabel(value: String) = when {
    value.startsWith("account.") -> "账号安全变更"
    value.startsWith("contract.") -> "确认指定版本协议"
    value.startsWith("guardian.") -> "监护范围确认"
    value.startsWith("data.") -> "资料与账号请求"
    else -> "${value.substringBefore('.')} 事项确认"
}
internal fun dataTypeLabel(v: String) = mapOf("erase_fields" to "删除指定资料／内容", "close_account" to "注销账号", "matters_assistance" to "未结事项协助")[v] ?: v
internal fun rightsTypeLabel(v: String) = mapOf("safety" to "安全求助", "privacy" to "隐私报告", "data_assistance" to "资料事项协助", "rights_notice" to "正式权利通知", "counterstatement" to "对原通知提交声明", "supplement" to "补充原事项")[v] ?: v
@Composable internal fun FeatureColumn(content: @Composable ColumnScope.() -> Unit) { TaskColumn(14,content) }
@Composable internal fun FeatureSection(title: String,content: @Composable ColumnScope.() -> Unit) { TaskSection(title,content) }
@Composable internal fun FeatureButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) { Text(label) }
}
@Composable internal fun FeaturePrimary(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TaskPrimary(label,enabled,onClick)
}
@Composable internal fun FormField(f: MobileFeatureStore, s: FeatureState, key: String, label: String, fallback: String = "", lines: Int = 1, numeric: Boolean = false) {
    OutlinedTextField(f.field(key, fallback), { f.edit(key, it) }, Modifier.fillMaxWidth(), label = { Text(label) }, minLines = lines,
        maxLines = if (lines == 1) 2 else 10, enabled = !s.busy && !s.loading && s.confirmation == null,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable internal fun ChoiceField(f: MobileFeatureStore, s: FeatureState, key: String, label: String, options: List<Pair<String,String>>, fallback: String) {
    Text(label, fontWeight = FontWeight.Medium)
    Column { options.forEach { (id, text) -> CheckLine(text, f.field(key, fallback) == id, { if (it) f.edit(key, id) }, !s.busy && !s.loading) } }
}
@Composable internal fun CheckLine(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked, onChecked, enabled = enabled); Text(label, Modifier.weight(1f))
    }
}
internal fun money(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString() + " 元"
internal fun cents(value: String): Long? = runCatching { BigDecimal(value.trim()).movePointRight(2).longValueExact().takeIf { it in 0..100000000 } }.getOrNull()
internal fun dateTime(value: String): String? = runCatching { OffsetDateTime.parse(value.trim()).toString() }.getOrNull()
internal fun displayTime(value: String): String = runCatching { OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx")) }.getOrDefault(value)
internal fun ids(value: String): List<Long>? { if (value.isBlank()) return emptyList(); val parts = value.split(',', '，', ' ', '\n').filter { it.isNotBlank() }; val parsed = parts.mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0 } }; return parsed.distinct().takeIf { parsed.size == parts.size } }
internal fun stateLabel(value: String): String = mapOf("active" to "有效", "pending" to "待确认", "accepted" to "已接受", "declined" to "已拒绝", "withdrawn" to "已撤回", "removed" to "已解除", "blocked" to "已拉黑", "revoked" to "已撤销", "expired" to "已过期", "dissolved" to "已解散", "effective" to "已生效", "cancelled" to "已取消", "safety_hold" to "安全暂停", "matched" to "材料匹配待交接", "disputed" to "存在争议", "completed" to "已完成", "needs_info" to "待补充", "submitted" to "已提交", "processing" to "处理中", "partially_held" to "部分保全", "held" to "保全中", "failed" to "处理失败", "approved" to "已同意", "rejected" to "未通过")[value] ?: "当前进度：$value"

@Composable internal fun FeaturePhotos(store: MobileStore, s: FeatureState, existing: List<Long> = emptyList(), private: Boolean = false, safety: Boolean = false, maxPhotos: Int = 9) {
    val f = store.features; val route = s.route ?: return
    LaunchedEffect(route, existing, s.loading) { if (!s.loading) f.initializePhotos(existing) }
    val photos = s.photos[route].orEmpty()
    Text(if (private) "材料仅用于当前私密事项" else "照片将用于当前公开资料", color = MaterialTheme.colorScheme.onSurfaceVariant)
    PhotoPickerButton(maxPhotos - photos.size, !s.busy && !s.loading, { f.addPhotos(it.take((maxPhotos - photos.size).coerceAtLeast(0)),private,safety) }, f::notify)
    photos.forEachIndexed { index, photo ->
        FeatureSection("${index + 1}. ${photo.name}") {
            if (!safety) photo.mediaId?.let { MobilePhoto(store,it,store.state.value.user?.id,photo.name,Modifier.fillMaxWidth().heightIn(max = 180.dp)) }
            if (photo.uploading) LinearProgressIndicator(Modifier.fillMaxWidth())
            photo.error?.let { Text(it, color = MaterialTheme.colorScheme.error); FeatureButton("重试此照片", !s.busy) { f.retryPhoto(route,photo.key,private,safety) } }
            Row { TextButton({ f.movePhoto(photo.key,-1) }, enabled = !s.busy && index > 0) { Text("前移") }; TextButton({ f.movePhoto(photo.key,1) }, enabled = !s.busy && index < photos.lastIndex) { Text("后移") }; TextButton({ f.removePhoto(photo.key) },enabled = !s.busy) { Text("移除") } }
        }
    }
}

@Composable internal fun AndroidFeatureMaster(store: MobileStore,s: FeatureState) {
    val f = store.features
    FeatureColumn {
        Text("同一任务列表",style = MaterialTheme.typography.titleMedium)
        when(s.route?.page) {
            FeaturePage.Appointment -> {
                s.appointments.items.forEach { row -> FeatureButton("预约 #${row.id} · ${stateLabel(row.state)}",!s.busy) { f.open(FeatureRoute(FeaturePage.Appointment,row.id)) } }
                FeatureButton("全部预约",!s.busy) { f.open(FeatureRoute(FeaturePage.Appointments)) }
            }
            FeaturePage.Offer -> {
                s.offers.items.forEach { row -> FeatureButton("${row.data.custodian} · ${money(row.data.totalCents)}",!s.busy) { f.open(FeatureRoute(FeaturePage.Offer,row.id)) } }
                FeatureButton("全部有偿信息",!s.busy) { f.open(FeatureRoute(FeaturePage.Offers)) }
            }
            FeaturePage.Claim -> {
                s.claims.items.forEach { row -> FeatureButton("${row.data.petName} · ${stateLabel(row.state)}",!s.busy) { f.open(FeatureRoute(FeaturePage.Claim,row.id)) } }
                s.case?.let { FeatureButton("返回当前求助",!s.busy) { f.open(FeatureRoute(FeaturePage.CaseWork,it.id)) } }
            }
            FeaturePage.Review -> {
                s.reviews.items.forEach { row -> FeatureButton("事项 #${row.id} · ${stateLabel(row.state)}",!s.busy) { f.open(FeatureRoute(FeaturePage.Review,row.id)) } }
                FeatureButton("全部核实与举报",!s.busy) { f.open(FeatureRoute(FeaturePage.Reviews)) }
            }
            else -> Unit
        }
    }
}
