package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ttkk0000.meowcircle.client.*

@Composable fun GuardiansContent(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f=store.features;val flow=f.guardianInvites;val state by flow.state.collectAsState()
    val account by store.state.collectAsState()
    var agreed by rememberSessionUiState("guardian.read",scope="transient:guardian",generation=state.info?.policyVersion){false}
    var code by rememberSessionUiState("guardian.code",scope="transient:guardian"){""}
    var stop by rememberSessionUiState<GuardianInviteStatus?>("guardian.cancel",scope="transient:guardian"){null}
    LaunchedEffect(account.user?.id){flow.load()}
    val eligible=s.eligibility
    val info=state.info
    if(route.id==0L) {
    if(eligible?.ageBand=="under14" || eligible?.ageBand=="unknown")TaskNotice("请监护人授权","可以继续浏览适龄公开内容；授权完成前不能新增交流，私有草稿保留。")
    NavigationRow("查看儿童与监护专门规则","说明收集事项、用途和撤回方式"){f.open(FeatureRoute(FeaturePage.Reading,kind=info?.policyKey?.takeIf{it.isNotBlank()} ?: "type-teen"))}
    TaskNotice("监护关系需要核对","一个成年手机号不能单独证明监护关系。领取邀请也不会生成监护关系或授权。")
    if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    if(!state.loaded && !state.busy)FeatureButton("读取邀请与服务状态",onClick={flow.load()})
    if(info?.available==false)TaskNotice("邀请服务暂不可用","原任务和草稿保留。可以阅读专门规则，或通过私密事项协助获取帮助。")
    if(info?.available==true && !state.unknown) {
        CheckLine("我已阅读当前儿童专门规则，了解邀请和资料范围",agreed,{agreed=it},!state.busy)
        if(eligible?.ageBand!="adult")FeatureButton("邀请监护人",agreed && !state.busy){flow.create(agreed)}
        FeatureSection("监护人独立办理") {
            Text("请使用监护人本人的账号，先核对成年资格和当前手机号，再粘贴儿童分享的邀请口令。")
            if(eligible?.contractVersion!=1 || eligible?.adult!=true || eligible?.phoneVerified!=true) {
                NavigationRow("核对本人成年资格","不会将儿童账号的年龄改成成人"){f.open(FeatureRoute(FeaturePage.Adult,kind="account"))}
                NavigationRow("验证当前手机号","监护关系仍需分别核对"){f.open(FeatureRoute(FeaturePage.Security))}
            } else {
                OutlinedTextField(code,{code=it},Modifier.fillMaxWidth(),label={Text("短时单次邀请口令")},visualTransformation=PasswordVisualTransformation(),singleLine=true,enabled=!state.busy)
                FeatureButton("领取并等待关系核对",agreed && Regex("^[A-Za-z0-9_-]{43}$").matches(code) && !state.busy){val token=code;code="";flow.claim(token,agreed)}
            }
        }
    }
    if(state.unknown) {TaskNotice("原操作结果待核对","不会创建新邀请或重新领取。请先按原编号核对。",warning=true);TaskPrimary("核对原操作",!state.busy,flow::reconcile)}
    state.items.forEach { invite -> FeatureSection("邀请 #${invite.id}") {
        Text(guardianInviteLabel(invite.state));Text("有效期至 ${displayTime(invite.expiresAt)}")
        if(invite.shareUrl.isNotBlank()) {
            Text("仅向监护人私下分享。链接不含儿童资料，领取后仍须核对关系。")
            SelectionContainer{Text(invite.shareUrl)}
            Text("只分享末尾的 code 口令，也可在 M&D 监护管理页独立领取。",style=MaterialTheme.typography.bodySmall)
        }
        if(invite.guardianRecordId>0)NavigationRow("查看关系核对后的授权","仍需监护人主动确认范围"){f.open(FeatureRoute(FeaturePage.Guardians,invite.guardianRecordId))}
        if(invite.canCancel && !state.unknown)FeatureButton("取消本次邀请／待核对请求",!state.busy){stop=invite}
    } }
    if(state.next>0)FeatureButton("继续读取邀请记录",!state.busy){flow.load(true)}
    if(state.loaded && !state.unknown)FeatureButton("刷新邀请状态",!state.busy){flow.load();f.reload()}
    }
    val guardians=s.guardians.filter{route.id==0L || it.id==route.id}
    if(route.id>0 && guardians.isEmpty())TaskNotice("当前授权不可读","可能已失效或当前账号无权查看，请重新读取。")
    guardians.forEach { record ->
        val body=record.data
        val mine=record.ownerId==account.user?.id
        val policy=s.readingCatalog?.documents?.firstOrNull{it.sha256==body.policyVersion && it.status in listOf("published","archived")}
        FeatureSection("监护授权 #${record.id}") {
            if(record.state=="withdrawn" || !futureTime(body.validUntil))TaskNotice("新增交流已暂停","保留合法必要记录和私有草稿；不会自动公开、迁移或删除账号。需有效授权的操作仍由服务端检查。",warning=true)
            Text("授权状态：${if(record.state=="active" && futureTime(body.validUntil)) "有效" else if(!futureTime(body.validUntil)) "已到期" else if(record.state=="verified_pending_consent") "关系已核实，等待明确授权" else "已拒绝或撤回"}")
            Text("交流资格：仍需当前手机号验证。陌生人私信默认限制，定位不公开精确位置。")
            Text("必要资料处理与受保护的日常交流。\n不包含商业发布、线下签约、营销与公开定位。\n有效期至 ${displayTime(body.validUntil)}")
            TaskNotice("监护人独立办理","先核对本人成年资格及监护关系，不在儿童账号里将年龄改成成人。")
            policy?.let{NavigationRow("查看本次专门规则",it.revision){f.open(FeatureRoute(FeaturePage.Reading,kind=it.key))}}
            if(mine && record.state=="verified_pending_consent" && futureTime(body.validUntil)) {
                if(policy==null)Text("本次指定规则版本暂不可读，保留原授权记录。")
                var confirmed by rememberSessionUiState("guardian.consent.${record.id}",scope="transient:guardian",generation=record.version){false}
                CheckLine("我已阅读本次专门规则并同意上述必要授权",confirmed,{confirmed=it},policy!=null && !s.busy)
                FeatureButton("核对并确认授权",confirmed && policy!=null && !s.busy){f.prepareSensitive(SensitiveIntent.Guardian(record.id,GuardianDecision(record.version,true,newRequestKey()),"必要资料处理与受保护日常交流；不包含商业、线下签约、营销或公开定位。有效期 ${displayTime(body.validUntil)}"))}
            }
            if(mine && record.state in setOf("active","verified_pending_consent"))FeatureButton(if(record.state=="active")"撤回授权" else "拒绝授权",!s.busy){f.prepareSensitive(SensitiveIntent.Guardian(record.id,GuardianDecision(record.version,false,newRequestKey()),"停止此关系的新增授权处理，保留必要历史与独立资料权利。"))}
            if(record.state=="active" && s.eligibility?.phoneVerified!=true)FeatureButton("验证当前手机号",!s.busy){f.open(FeatureRoute(FeaturePage.Security))}
            NavigationRow("保护设置与陌生人联系","查看未成年人保护和联系限制"){f.open(FeatureRoute(FeaturePage.Guardians,kind="protection"))}
            NavigationRow("数据权利与投诉","按具体事项核对请求"){f.open(FeatureRoute(FeaturePage.DataRequests))}
        }
    }
    stop?.let{invite -> MobileAlertDialog({stop=null},title={Text("取消原邀请")},text={Text("仅取消邀请 #${invite.id} 的当前版本，保留草稿；不会撤回已生效的监护授权。")},confirmButton={TextButton({stop=null;flow.cancel(invite,true)},enabled=!state.busy){Text("确认取消")}},dismissButton={TextButton({stop=null}){Text("继续保留")}})}
    NavigationRow("需要帮助","私密提出资料事项协助"){f.open(FeatureRoute(FeaturePage.Safety));f.edit("type","data_assistance")}
    TaskSecondary("保留草稿，继续浏览",!s.busy && !state.busy){f.closeRoute()}
}
@Composable fun GuardianProtectionContent(store: MobileStore) {
    val f=store.features
    TaskSection("未成年人保护") {
        Text("未满14岁：有效监护授权与儿童专门规则。")
        Text("14–17岁：继续适用未成年人保护。")
        Text("陌生人联系：默认限制，支持屏蔽、举报与必要使用管理。")
        Text("商业与线下签约：成年人以本人账号办理，不代点儿童确认。")
    }
    NavigationRow("查看监护授权与撤回","范围、版本及有效状态"){f.open(FeatureRoute(FeaturePage.Guardians))}
    NavigationRow("屏蔽与好友管理","当前账号的联系关系"){f.open(FeatureRoute(FeaturePage.Friends))}
    NavigationRow("资料与侵害求助","未成年人私密信息优先保护"){f.open(FeatureRoute(FeaturePage.Safety));f.edit("type","privacy")}
    TaskNotice("保护与反馈","监护授权有范围和有效期；需要更正年龄、调整授权或请求删除，可通过规则与权利渠道办理。")
    TaskPrimary("返回未成年人规则"){f.open(FeatureRoute(FeaturePage.Reading,kind="type-teen"))}
}
fun guardianInviteLabel(state: String)=when(state){"invite_open"->"等待监护人领取";"relation_pending"->"已领取，等待独立关系核对";"relation_verified"->"关系已核实，仍需监护人明确授权";"invite_cancelled"->"原邀请已取消";"expired"->"邀请或请求已过期";"policy_changed"->"指定规则已变化，请取消原邀请后重新核对";else->"状态待核对"}
