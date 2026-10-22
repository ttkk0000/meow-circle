package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttkk0000.meowcircle.client.*

// 按当前窗口与输入占用选择布局；手机、平板始终共用同一个 Store。
internal data class AndroidWindowPolicy(val rail: Boolean, val master: Boolean, val contextPane: Boolean)

@Composable
fun AndroidMobileApp(store: MobileStore) {
    val state by store.state.collectAsStateWithLifecycle()
    val features by store.features.state.collectAsStateWithLifecycle()
    val sensitive by store.features.sensitive.state.collectAsStateWithLifecycle()
    BackHandler(state.auth.visible || state.publicationConfirmation != null || features.route != null || sensitive.stage != SensitiveStage.Idle || state.destination != MobileDestination.Home || state.applying || state.confirmation != null) {
        when {
            features.busy -> Unit
            state.auth.visible -> store.cancelLogin()
            sensitive.stage != SensitiveStage.Idle -> store.features.sensitive.dismiss()
            features.route != null -> store.features.back()
            state.publicationConfirmation != null -> store.dismissPublication()
            else -> store.back()
        }
    }
    MobileTheme {
        AndroidAdaptiveWindow {
            val keyboard = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
            val policy = AndroidWindowPolicy(rail = maxWidth >= 680.dp, master = maxWidth >= 920.dp && maxHeight >= 480.dp && !keyboard,
                contextPane = maxWidth >= 1360.dp && maxHeight >= 600.dp && !keyboard)
            MobileShell(store, state, features, policy)
        }
    }
}

@Composable
private fun MobileShell(store: MobileStore, state: MobileState, features: FeatureState, policy: AndroidWindowPolicy) {
    val f = store.features
    val saved = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    fun select(tab: MobileTab) { if (!features.busy && state.publicationConfirmation == null) { f.closeRoute(); store.select(tab) } }
    val unread = state.inbox?.let { it.direct.values.sum() + it.groups.values.sum() + it.notifications.filterKeys { key -> key !in listOf("direct_messages","groups") }.values.sum() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (!policy.rail) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            MobileTab.entries.forEach { tab -> NavigationBarItem(selected = state.tab == tab,
                onClick = { select(tab) }, icon = { BadgedBox(badge = { if (tab == MobileTab.Messages && unread != null && unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) { Icon(tab.icon(), tab.label) } }, label = { Text(tab.label) }) }
        } }, contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (policy.rail) NavigationRail(containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                Text("M&D", Modifier.padding(vertical = 20.dp), fontFamily = BrandFont, fontWeight = FontWeight.ExtraBold)
                MobileTab.entries.forEach { tab -> NavigationRailItem(selected = state.tab == tab,
                    onClick = { select(tab) }, icon = { BadgedBox(badge = { if (tab == MobileTab.Messages && unread != null && unread > 0) Badge { Text(if(unread > 99) "99+" else "$unread") } }) { Icon(tab.icon(), tab.label) } }, label = { Text(tab.label) }) }
            }
            if (policy.contextPane) ContextPane(store, state, Modifier.width(240.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (features.route != null || state.destination != MobileDestination.Home) IconButton(onClick = { if (!f.back()) store.back() }, enabled = !state.busy && !features.busy) { Icon(Icons.Outlined.ArrowBack, "返回") }
                    Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text(features.route?.page?.title ?: if(state.destination == MobileDestination.Home && state.tab == MobileTab.Community) "M&D" else title(state), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (features.route == null && state.destination == MobileDestination.Home && state.tab == MobileTab.Community) Text("meow & doggie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.user == null) TextButton(onClick = store::showLogin) { Text("登录") }
                    IconButton(onClick = { f.open(FeatureRoute(FeaturePage.Safety)) }) { Icon(Icons.Outlined.HelpOutline,"安全与隐私求助") }
                    IconButton(onClick = { if(features.route != null) f.reload() else if(state.destination == MobileDestination.Home) store.refreshHome() else store.reloadDetail(); store.refreshInbox() }, enabled = !state.busy && !features.busy) { Icon(Icons.Outlined.Refresh,"重新载入") }
                }
                if (state.restoring || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.notice?.let { notice ->
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(notice, Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = store::clearNotice) { Icon(Icons.Outlined.Close,"关闭提示") }
                    }
                }
                if(state.inboxError != null) Text("未读读取失败，保留上次结果",Modifier.padding(horizontal = 16.dp),style = MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxSize()) {
                    val master = policy.master && features.route == null && state.tab in listOf(MobileTab.Adoption,MobileTab.Messages) && state.destination != MobileDestination.Home
                    if (master) {
                        Box(Modifier.width(300.dp).fillMaxHeight()) { saved.SaveableStateProvider("${state.user?.id ?: 0}-home-${state.tab}") { HomeContent(store,state) } }
                        VerticalDivider()
                    }
                    if(policy.master && features.route?.page in listOf(FeaturePage.Appointment,FeaturePage.Offer,FeaturePage.Claim,FeaturePage.Review)) {
                        Box(Modifier.width(280.dp).fillMaxHeight()) { saved.SaveableStateProvider("${state.user?.id ?: 0}-feature-master-${features.route?.page}") { AndroidFeatureMaster(store,features) } }
                        VerticalDivider()
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(),contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = 820.dp).fillMaxSize()) {
                            val key = features.route?.let { "feature-${it.page}-${it.id}-${it.kind}-${it.peer}" } ?: if(state.destination == MobileDestination.Home) "home-${state.tab}" else "detail-${state.destination}"
                            saved.SaveableStateProvider("${state.user?.id ?: 0}-$key") {
                                if(features.route != null) AndroidFeatureContent(store,features)
                                else if(state.destination == MobileDestination.Home) HomeContent(store,state) else DetailContent(store,state)
                            }
                        }
                    }
                    if(policy.master && state.tab == MobileTab.Publish && features.route == null && state.destination == MobileDestination.Home) {
                        VerticalDivider(); Column(Modifier.width(280.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("发布预览",style = MaterialTheme.typography.titleMedium); Text(state.composeTitle); Text(state.composeText); Text(audienceLabel(state.composeEntry.audience.mode))
                            state.photos.mapNotNull { it.mediaId }.forEach { MobilePhoto(store,it,state.user?.id,"发布预览",Modifier.fillMaxWidth().aspectRatio(1f)) }
                        }
                    }
                }
            }
        }
    }
    if (state.auth.visible) AndroidAuthDialog(store,state)
    FeatureDialogs(store,features)
    PublicationDialog(store,state)
    state.confirmation?.let { command ->
        MobileAlertDialog(onDismissRequest = store::dismissConfirmation, title = { Text(command.label) },
            text = { Text("${command.label}？\n当前申请：${adoptionStateLabel(command.record.state)}\n${command.note}\n提交后由服务端校验最新进度。交接需要双方各自确认。") },
            confirmButton = { TextButton(onClick = store::confirmAdoptionAction, enabled = !state.busy && !state.conflict) { Text("确认提交") } },
            dismissButton = { TextButton(onClick = store::dismissConfirmation, enabled = !state.busy) { Text("返回核对") } })
    }
}

@Composable
internal fun ContextPane(store: MobileStore, state: MobileState, modifier: Modifier) {
    Column(modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("和猫咪一起的日常", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("交流、领养与同城互助", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { store.open(MobileDestination.Help) }, modifier = Modifier.fillMaxWidth()) { Text("走失互助") }
        OutlinedButton(onClick = { store.open(MobileDestination.Applications) }, modifier = Modifier.fillMaxWidth()) { Text("领养申请进度") }
        OutlinedButton(onClick = { store.open(MobileDestination.Notifications) }, modifier = Modifier.fillMaxWidth()) { Text("通知") }
        FeatureButton("预约与安排") { store.features.open(FeatureRoute(FeaturePage.Appointments)) }
        FeatureButton("好友与宠友圈") { store.features.open(FeatureRoute(FeaturePage.Friends)) }
        FeatureButton("有偿信息") { store.features.open(FeatureRoute(FeaturePage.Offers)) }
        val chatDestination = state.destination as? MobileDestination.Chat
        if(chatDestination != null) {
            HorizontalDivider()
            if(chatDestination.group) {
                Text(state.group?.data?.name ?: "群聊上下文")
                Text(state.group?.let { "${it.data.members.size} 位成员 · ${stateLabel(it.state)}" } ?: "等待读取当前成员资格")
                FeatureButton("群公告与成员管理") { store.features.open(FeatureRoute(FeaturePage.Group,chatDestination.id)) }
            } else state.conversation?.let { chat ->
                val peer = if(chat.data.requesterId == state.user?.id) chat.data.recipientId else chat.data.requesterId
                FeatureButton("查看对方资料") { store.features.open(FeatureRoute(FeaturePage.Member,peer)) }
                Text(stateLabel(chat.state))
            }
        }
        HorizontalDivider()
        Text(state.user?.nickname ?: "以游客身份浏览", fontWeight = FontWeight.Bold)
        Text("先了解，再决定。核实结果只说明已检查的具体事项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeContent(store: MobileStore, s: MobileState) {
    when (s.tab) {
        MobileTab.Community -> Feed(store, s)
        MobileTab.Adoption -> Records(s.pets, "先了解，再决定", { store.refreshHome() }, { store.refreshHome(true) },
            header = { Text("领养不是免费买宠。具体条件、健康未知和必要费用都应分别说明。", color = MaterialTheme.colorScheme.onSurfaceVariant) }) { row ->
            PetCard(store, s, row) { store.open(MobileDestination.Pet(row.id)) }
        }
        MobileTab.Publish -> PublishScreen(store, s)
        MobileTab.Messages -> if (s.user == null) SignInPrompt(store) else InboxScreen(store, s)
        MobileTab.Profile -> if (s.user == null) SignInPrompt(store) else ProfileScreen(store, s)
    }
}

@Composable
private fun Feed(store: MobileStore, s: MobileState) {
    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(s.feedKeyword) }
                OutlinedTextField(query,{ query = it },Modifier.fillMaxWidth(),label = { Text("搜索宠友日常与互助") },trailingIcon = { IconButton({ store.filterFeed(s.feedSpace,query) }) { Icon(Icons.Outlined.Search,"搜索") } },singleLine = true)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { listOf("community" to "社区","circle" to "宠友圈","diary" to "日记").forEach { (id,label) -> FilterChip(s.feedSpace == id,{ store.filterFeed(id) },label = { Text(label) },modifier = Modifier.padding(end = 8.dp)) } }
                FeatureButton("好友与宠友圈") { store.features.open(FeatureRoute(FeaturePage.Friends)) }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { store.select(MobileTab.Adoption) }, modifier = Modifier.weight(1f)) { Text("领养") }
                OutlinedButton(onClick = { store.open(MobileDestination.Help) }, modifier = Modifier.weight(1f)) { Text("走失互助") }
            }
        }
        items(s.entries.items, key = { it.id }) { row ->
            Column(Modifier.clickable { store.open(MobileDestination.Entry(row.id)) }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    MobilePhoto(store, row.mediaIds.firstOrNull(), s.user?.id, row.data.title.ifBlank { "社区照片" }, Modifier.fillMaxWidth().aspectRatio(0.9f))
                }
                Text(row.data.title.ifBlank { row.data.text }, Modifier.padding(top = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                s.metrics[row.id]?.let { Text("${it.likes} 赞 · ${it.comments} 评论", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { FeatureButton("有偿信息、到访与费用披露") { store.features.open(FeatureRoute(FeaturePage.Offers)) } }
        item(span = { GridItemSpan(maxLineSpan) }) { PageFooter(s.entries, { store.refreshHome() }, { store.refreshHome(true) }) }
    }
}

@Composable
private fun PetCard(store: MobileStore, s: MobileState, row: MobileRecord<PetBody>, open: () -> Unit) {
    Card(onClick = open, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp)) { MobilePhoto(store, row.mediaIds.firstOrNull(), s.user?.id, row.data.name, Modifier.size(100.dp, 116.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.data.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(listOf(row.data.species, row.data.breed, row.data.sex).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(row.data.bio, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(if (row.data.adoptionAvailable) "正在寻找领养人" else "宠物档案", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DetailContent(store: MobileStore, s: MobileState) {
    if (s.detailLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    s.detailError?.let { ErrorPanel(it, { store.reloadDetail() }); return }
    when (val destination = s.destination) {
        is MobileDestination.Pet -> PetDetail(store, s)
        is MobileDestination.Entry -> EntryDetail(store, s)
        is MobileDestination.Application -> ApplicationDetail(store, s)
        is MobileDestination.Chat -> ChatScreen(store, s, destination)
        is MobileDestination.Case -> s.case?.let { record -> ScrollContent {
            SectionTitle(record.data.title)
            Text(if (record.data.type == "lost") "走失求助" else "拾获待认领", color = MaterialTheme.colorScheme.primary)
            Photos(store, record.mediaIds, s.user?.id, record.data.title)
            Text("${record.data.city} · ${record.data.area}")
            Text("发生时间：${displayTime(record.data.occurredAt)}")
            Text(record.data.description)
            Text("公开位置为大致区域。认领与交接需要核对材料并由双方确认。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (record.data.closureReason.isNotBlank()) Text("结束说明：${record.data.closureReason}")
            FeatureButton("私密线索与认领") { store.features.open(FeatureRoute(FeaturePage.CaseWork,record.id)) }
            FeatureButton("报告安全或隐私问题") { store.features.open(FeatureRoute(FeaturePage.Safety,kind = "cases",peer = record.id)) }
        } }
        MobileDestination.Help -> Records(s.cases, "走失互助", { store.reloadDetail() }, { store.reloadDetail(true) },header = {
            FeatureButton("发布走失／拾获求助") { if(s.user == null) store.showLogin() else store.features.open(FeatureRoute(FeaturePage.CaseEdit)) }
            FeatureButton("附近提醒") { store.features.open(FeatureRoute(FeaturePage.Nearby)) }
        }) { row ->
            ActionCard(row.data.title, "${row.data.city} · ${row.data.area}\n${if (row.data.type == "lost") "走失" else "拾获"}") { store.open(MobileDestination.Case(row.id)) }
        }
        MobileDestination.Applications -> if (s.user == null) SignInPrompt(store) else Records(s.applications, "领养申请进度", { store.reloadDetail() }, { store.reloadDetail(true) }) { row ->
            ActionCard(adoptionStateLabel(row.state), row.data.message) { store.open(MobileDestination.Application(row.id)) }
        }
        MobileDestination.Notifications -> if (s.user == null) SignInPrompt(store) else Records(s.notifications, "通知", { store.reloadDetail() }, { store.reloadDetail(true) },header = {
            FeatureButton("通知与附近提醒设置") { store.features.open(FeatureRoute(FeaturePage.Settings)) }
            Text("进入本页不会清空未读；待处理事项与已读分开。")
            if(s.notifications.items.any { !it.data.read }) FeatureButton("将当前载入通知标为已读",!s.busy) {
                val snapshot = s.notifications.items.filter { !it.data.read }
                store.features.confirm("将已载入通知标为已读","仅标记当前 ${snapshot.size} 条通知，不接受申请或确认预约；新通知仍保持未读。") { snapshot.forEach { store.api.readNotification(it.id,it.version) }; store.reloadDetail(); store.refreshInbox() }
            }
        }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.data.text, fontWeight = if (row.data.read) FontWeight.Normal else FontWeight.Bold)
                    TextButton(onClick = { openNotification(store,row.data) }) { Text("查看对应事项") }
                    if (!row.data.read) TextButton(onClick = { store.readNotification(row) }, enabled = !s.busy) { Text("标为已读") }
                }
            }
        }
        MobileDestination.Home -> Unit
    }
}

@Composable
private fun PetDetail(store: MobileStore, s: MobileState) {
    val record = s.pet ?: return
    ScrollContent {
        Photos(store, record.mediaIds, s.user?.id, record.data.name)
        SectionTitle(record.data.name)
        Text(listOf(record.data.species, record.data.breed, record.data.sex).filter { it.isNotBlank() }.joinToString(" · "))
        Text(record.data.bio)
        if (s.user?.id == record.ownerId && record.data.health.isNotBlank()) Text("私密健康记录：${record.data.health}")
        Text("先核对具体条件、材料来源与照护安排。尚未提供的健康信息不能视为已确认。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if(s.petChecks.isNotEmpty()) FeatureSection("按事项核实") { s.petChecks.forEach { Text("${checkLabel(it.check)} · ${stateLabel(it.state)}\n${it.summary}\n${it.validUntil?.let { until -> "有效至 ${displayTime(until)}" }.orEmpty()}") } }
        if (record.ownerId == s.user?.id) {
            FeatureButton("编辑宠物档案") { store.features.open(FeatureRoute(FeaturePage.PetEdit,record.id)) }
            FeatureButton("介绍协作与邀请") { store.features.open(FeatureRoute(FeaturePage.Collaborators,record.id)) }
        } else if(s.user != null) FeatureButton("提议了解／到访预约") { store.features.open(FeatureRoute(FeaturePage.AppointmentEdit,kind = "visit",peer = record.id)); store.features.edit("peer",record.ownerId.toString()) }
        FeatureButton("具体事项核实／举报") { store.features.open(FeatureRoute(FeaturePage.Review,kind = "pets",peer = record.id)) }
        if (record.data.adoptionAvailable && s.user?.id != record.ownerId) {
            if (s.applying) {
                SectionTitle("介绍一下你的照护计划")
                Text("说明居住环境、照护时间和已有宠物。提交前可返回修改。")
                OutlinedTextField(s.applicationText, store::editApplication, Modifier.fillMaxWidth(), label = { Text("领养申请摘要") }, minLines = 5, enabled = !s.busy)
                Button(onClick = store::submitApplication, enabled = !s.busy && s.applicationText.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("确认并提交申请") }
            } else Button(onClick = store::beginApplication, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("申请领养") }
            OutlinedButton(onClick = store::contactPetOwner, enabled = !s.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("联系发布人") }
        }
    }
}

@Composable
private fun EntryDetail(store: MobileStore, s: MobileState) {
    val entry = s.entry ?: return
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Photos(store, entry.mediaIds, s.user?.id, entry.data.title) }
        item { SectionTitle(entry.data.title); Text(entry.data.text); Text("${audienceLabel(entry.data.audience.mode)} · ${originLabel(entry.data.contentOrigin)}",color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            FeatureButton("查看发布人") { store.features.open(FeatureRoute(FeaturePage.Member,entry.ownerId)) }
            if(entry.ownerId == s.user?.id) {
                FeatureButton("编辑动态") { store.features.open(FeatureRoute(FeaturePage.EntryEdit,entry.id)) }
                FeatureButton("删除动态",!s.busy) { store.features.confirm("删除动态",entry.data.title.ifBlank { entry.data.text }) { store.api.removeResource("entries",entry.id,entry.version); store.back(); store.refreshHome() } }
            }
            FeatureButton("举报此内容") { store.features.open(FeatureRoute(FeaturePage.Safety,kind = "entries",peer = entry.id)) }
        }
        item {
            val metrics = s.metrics[entry.id]
            OutlinedButton(onClick = store::toggleLike, enabled = metrics != null && !s.busy) { Text(if (metrics?.viewerLiked == true) "已赞 ${metrics.likes}" else "赞 ${metrics?.likes ?: "—"}") }
        }
        item { SectionTitle("评论"); if(s.comments.items.isNotEmpty()) Text("评论中的“回复此评论”会绑定原评论，不自动发送。",style = MaterialTheme.typography.labelSmall) }
        items(s.comments.items, key = { it.id }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if(row.data.replyTo > 0) Text("回复评论 #${row.data.replyTo}",style = MaterialTheme.typography.labelSmall)
                    Text(row.data.text); if(row.data.stickerId.isNotBlank()) StickerPhoto(row.data.stickerId,row.data.stickerId)
                    TextButton({ store.replyTo(row.id) }) { Text("回复此评论") }
                    if(row.ownerId == s.user?.id) TextButton({ store.features.confirm("删除评论",row.data.text) { store.api.removeResource("entry_comments",row.id,row.version); store.loadComments(); store.reloadDetail() } }) { Text("删除") }
                }
            }
        }
        item { PageFooter(s.comments, { store.loadComments() }, { store.loadComments(true) }) }
        item { ReplyEditor(store, s, "写下评论") }
    }
}

@Composable
private fun ApplicationDetail(store: MobileStore, s: MobileState) {
    val record = s.application ?: return
    ScrollContent {
        SectionTitle(adoptionStateLabel(record.state))
        FeatureButton("协议、私密照护与安全接回") { store.features.open(FeatureRoute(FeaturePage.AdoptionWork,record.id)) }
        Text("申请摘要", fontWeight = FontWeight.Bold); Text(record.data.message)
        if (record.data.supplement.isNotBlank()) { Text("补充资料", fontWeight = FontWeight.Bold); Text(record.data.supplement) }
        if (record.data.trialTerms.isNotBlank()) { Text("试养条款", fontWeight = FontWeight.Bold); Text(record.data.trialTerms); Text("截止：${record.data.trialUntil.orEmpty()}") }
        if (record.data.note.isNotBlank()) Text("最新说明：${record.data.note}")
        Text("交接由双方分别主动确认；同意申请不会自动转移宠物档案。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (s.conflict) OutlinedButton(onClick = { store.dismissConfirmation(); store.reloadDetail() }) { Text("重新载入当前进度") }
        OutlinedTextField(s.actionNote, store::editActionNote, Modifier.fillMaxWidth(), label = { Text("补充说明") }, minLines = 3, enabled = !s.busy)
        store.adoptionActions().forEach { (action, label) ->
            OutlinedButton(onClick = { store.requestAdoptionAction(action, label) }, enabled = !s.busy && !s.conflict,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
        }
    }
}

@Composable
private fun InboxScreen(store: MobileStore, s: MobileState) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ActionCard("通知", "${s.inbox?.notifications?.values?.sum()?.toString() ?: "—"} 条未读") { store.open(MobileDestination.Notifications) } }
        item {
            Text("待接受的消息请求 ${s.inbox?.messageRequests?.toString() ?: "—"}", fontWeight = FontWeight.Bold)
            Text("待处理事项 ${s.inbox?.pending?.values?.sum()?.toString() ?: "—"}，与未读分开计算")
            FeatureButton("好友与邀请") { store.features.open(FeatureRoute(FeaturePage.Invitations)) }
            FeatureButton("创建群聊") { store.features.open(FeatureRoute(FeaturePage.GroupCreate)) }
            FeatureButton("预约与安排") { store.features.open(FeatureRoute(FeaturePage.Appointments)) }
        }
        items(s.conversations.items, key = { "direct-${it.id}" }) { row ->
            ActionCard("与用户 #${if(row.data.requesterId == s.user?.id) row.data.recipientId else row.data.requesterId}", "${if (row.state == "pending") "消息请求" else "私信"} · ${s.inbox?.let { it.direct["${row.id}"] ?: 0 }?.toString() ?: "—"} 条未读") { store.open(MobileDestination.Chat(row.id)) }
        }
        item { PageFooter(s.conversations, { store.refreshHome() }, { store.refreshHome(true) }) }
        item { SectionTitle("群聊") }
        items(s.groups.items, key = { "group-${it.id}" }) { row ->
            ActionCard(row.data.name, "${row.data.members.size} 位成员 · ${s.inbox?.let { it.groups["${row.id}"] ?: 0 }?.toString() ?: "—"} 条未读") { store.open(MobileDestination.Chat(row.id, true)) }
        }
        item { PageFooter(s.groups, { store.refreshHome() }, { store.refreshHome(true) }) }
    }
}

@Composable
private fun ChatScreen(store: MobileStore, s: MobileState, chat: MobileDestination.Chat) {
    val conversation = s.conversation
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val uiScope = rememberCoroutineScope()
    val firstUnread = (if(chat.group) s.inbox?.groupFirstUnread else s.inbox?.directFirstUnread)?.get(chat.id.toString())
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if(chat.group) TextButton({ store.features.open(FeatureRoute(FeaturePage.Group,chat.id)) }) { Text("群公告、成员与管理") }
        if (conversation?.state == "pending") {
            Text("对方接受前，发起人仅可发送一条文字。", Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (conversation.data.recipientId == s.user?.id) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { store.answerMessageRequest("accept") }, enabled = !s.busy) { Text("接受消息请求") }
                OutlinedButton(onClick = { store.answerMessageRequest("decline") }, enabled = !s.busy) { Text("拒绝") }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            if(firstUnread != null) TextButton({ store.loadMessages(); uiScope.launch { listState.scrollToItem(0) } },enabled = !s.messages.loading) { Text("定位首条未读") }
            TextButton({ store.loadMessages(fromBeginning = true); uiScope.launch { listState.scrollToItem(0) } },enabled = !s.messages.loading) { Text("读取较早历史") }
        }
        LazyColumn(Modifier.weight(1f),state = listState, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(s.messages.items, key = { it.id }) { row ->
                if(row.id == firstUnread) Text("以下为未读消息",color = MaterialTheme.colorScheme.primary,style = MaterialTheme.typography.labelMedium)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val bubbleWidth = minOf(560.dp, maxWidth * 0.78f)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (row.ownerId == s.user?.id) Arrangement.End else Arrangement.Start) {
                        Surface(shape = RoundedCornerShape(16.dp), color = if (row.ownerId == s.user?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.widthIn(max = bubbleWidth)) {
                            Column(Modifier.padding(14.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if(row.data.text.isNotBlank()) Text(row.data.text)
                                if(row.data.stickerId.isNotBlank()) StickerPhoto(row.data.stickerId,row.data.stickerId)
                                row.mediaIds.forEach { MobilePhoto(store,it,s.user?.id,"消息照片",Modifier.fillMaxWidth().aspectRatio(1.25f)) }
                            }
                        }
                    }
                }
            }
            item { PageFooter(s.messages, { store.loadMessages() }, { store.loadMessages(true) }) }
        }
        if (s.messages.items.isNotEmpty()) TextButton(onClick = store::markChatRead, enabled = !s.busy) { Text("将已载入消息标为已读") }
        val canSend = (chat.group && s.group?.let { it.state == "active" && s.user?.id in it.data.members } == true) || conversation?.state == "accepted" || conversation?.let { it.state == "pending" && it.data.requesterId == s.user?.id && s.messages.loaded && s.messages.items.isEmpty() } == true
        if (canSend) ReplyEditor(store, s, "消息") else Text("当前会话暂不可发送", Modifier.padding(16.dp))
    }
}

@Composable
private fun ReplyEditor(store: MobileStore, s: MobileState, label: String) {
    var replaceSaved by remember(s.user?.id) { mutableStateOf(false) }
    if(replaceSaved) MobileAlertDialog(onDismissRequest = { replaceSaved = false },title = { Text("读取本机消息草稿") },text = { Text("放弃此账号未保存的评论与聊天输入，读取本机已保存的原文和附件。不会自动发送。") },
        confirmButton = { TextButton({ replaceSaved = false; store.replaceWithSavedReplies() }) { Text("放弃未保存输入并读取") } },dismissButton = { TextButton({ replaceSaved = false }) { Text("继续编辑") } })
    var editor by rememberPrivateEditor(s.reply,"reply-${s.user?.id}-${s.destination}")
    var emojiOpen by androidx.compose.runtime.saveable.rememberSaveable(s.destination.toString()) { mutableStateOf(false) }
    LaunchedEffect(s.reply) { if (editor.text != s.reply) editor = TextFieldValue(s.reply, TextRange(s.reply.length)) }
    val chat = s.destination as? MobileDestination.Chat
    val richAllowed = chat == null || chat.group || s.conversation?.state == "accepted"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Column(Modifier.heightIn(max = minOf(420.dp,maxHeight * 0.65f)).verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if(s.replyDraftRestoring) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在恢复本机消息草稿") }
            s.guestReplySuggestion?.let { text -> FeatureSection("登录前的评论输入") { Text(text); FeatureButton("用登录前的评论替换当前输入",!s.busy && s.replyDraftReady) { store.useGuestReply() } } }
            s.replyDraftError?.let {
                Text(it,color = MaterialTheme.colorScheme.error)
                if(!s.replyDraftReady) FeatureButton("重试读取本机消息草稿",!s.busy) { store.restoreReplyDrafts() }
                else { FeatureButton("重试保存当前消息草稿",!s.busy) { store.retryReplySave() }; FeatureButton("读取本机已保存版本",!s.busy) { replaceSaved = true } }
            }
            if(s.replyTo > 0) Row { Text("回复评论 #${s.replyTo}",Modifier.weight(1f)); TextButton({ store.replyTo(0) }) { Text("取消回复") } }
            OutlinedTextField(editor, { if (store.editReply(it.text)) editor = it }, Modifier.fillMaxWidth(), label = { Text(label) }, enabled = !s.busy, maxLines = 5)
            if(s.replySticker.isNotBlank()) { StickerPhoto(s.replySticker,s.replySticker); TextButton({ store.selectSticker("") },enabled = !s.busy) { Text("移除待发送贴纸") } }
            if(chat != null && richAllowed) PhotoPickerButton(9 - s.replyPhotos.size,!s.busy,store::addReplyPhotos,store::pickerFailed)
            s.replyPhotos.forEach { photo ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(photo.name,Modifier.weight(1f)); if(photo.uploading) CircularProgressIndicator(Modifier.size(20.dp))
                    if(photo.error != null) TextButton({ store.retryReplyPhoto(photo.key) },enabled = !s.busy) { Text("重试") }
                    TextButton({ store.removeReplyPhoto(photo.key) },enabled = !s.busy) { Text("移除") }
                }
                photo.error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { emojiOpen = !emojiOpen }) { Text(if (emojiOpen) "收起表情" else "表情与贴纸") }
                Button(onClick = store::sendReply, enabled = !s.busy && (s.reply.isNotBlank() || s.replySticker.isNotBlank() || s.replyPhotos.isNotEmpty()) && s.replyPhotos.all { it.mediaId != null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("发送") }
            }
            if (emojiOpen) ExpressionPanel(store,s,editor,{ editor = it },richAllowed)
            if(s.pendingOperations.isNotEmpty()) FeatureButton("核对原消息结果") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
        }
    }
}

@Composable
private fun PublishScreen(store: MobileStore, s: MobileState) {
    var replaceDraft by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var emojiOpen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var composer by rememberPrivateEditor(s.composeText,"compose-${s.user?.id}")
    LaunchedEffect(s.composeText) { if(composer.text != s.composeText) composer = TextFieldValue(s.composeText,TextRange(s.composeText.length)) }
    if (s.user == null) { SignInPrompt(store); return }
    ScrollContent {
        SectionTitle("分享和猫咪的日常")
        OutlinedTextField(s.composeTitle, { store.editCompose(it, s.composeText) }, Modifier.fillMaxWidth(), label = { Text("标题（选填）") }, enabled = !s.busy)
        OutlinedTextField(composer, { next -> store.editCompose(s.composeTitle,next.text); if(store.state.value.composeText == next.text) composer = next }, Modifier.fillMaxWidth(), label = { Text("今天想分享什么？") }, minLines = 6, enabled = !s.busy)
        TextButton({ emojiOpen = !emojiOpen }) { Text(if(emojiOpen) "收起表情" else "插入表情") }
        if(emojiOpen) ComposeEmojiPanel(store,s,composer) { next -> store.editCompose(s.composeTitle,next.text); if(store.state.value.composeText == next.text) composer = next }
        ComposeAudience(store,s)
        PhotoPickerButton(store, 9 - s.photos.size, !s.busy)
        s.photos.forEachIndexed { index, photo ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}. ${photo.name}")
                    photo.mediaId?.let { MobilePhoto(store, it, s.user?.id, photo.name, Modifier.size(112.dp)) }
                    if (photo.uploading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    photo.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { store.retryPhoto(photo.key) }) { Text("重试此照片") } }
                    Row {
                        TextButton(onClick = { store.movePhoto(photo.key, -1) }, enabled = !s.busy && index > 0) { Text("前移") }
                        TextButton(onClick = { store.movePhoto(photo.key, 1) }, enabled = !s.busy && index < s.photos.lastIndex) { Text("后移") }
                        TextButton(onClick = { store.removePhoto(photo.key) }, enabled = !s.busy) { Text("移除") }
                    }
                }
            }
        }
        if (s.draftLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = store::saveComposeDraft, enabled = !s.busy && s.draftLoaded && !s.draftLoading && s.photos.all { it.mediaId != null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存草稿") }
        TextButton(onClick = { replaceDraft = true }, enabled = !s.busy && !s.draftLoading) { Text("用云端草稿替换当前输入") }
        Button(onClick = store::publish, enabled = !s.busy && s.composeAudienceValid && (s.composeText.isNotBlank() || s.photos.isNotEmpty()) && s.photos.all { it.mediaId != null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("确认发布") }
    }
    if(replaceDraft) MobileAlertDialog(onDismissRequest = { replaceDraft = false },title = { Text("用云端草稿替换当前输入？") },text = { Text("当前未同步文字、受众和照片会被云端版本替换。请先核对是否需要保留本机输入。") },confirmButton = { TextButton({ replaceDraft = false; store.restoreComposeDraft(replaceInput = true) },enabled = !s.busy) { Text("确认替换") } },dismissButton = { TextButton({ replaceDraft = false }) { Text("保留本机输入") } })
}

@Composable
private fun ProfileScreen(store: MobileStore, s: MobileState) {
    Records(s.myPets, s.user?.nickname.orEmpty(), { store.refreshHome() }, { store.refreshHome(true) }, header = {
        Text(s.user?.bio.orEmpty())
        FeatureButton("编辑个人资料") { store.features.open(FeatureRoute(FeaturePage.ProfileEdit)) }
        FeatureButton("好友与宠友圈") { store.features.open(FeatureRoute(FeaturePage.Friends)) }
        FeatureButton("协作与群邀请") { store.features.open(FeatureRoute(FeaturePage.Invitations)) }
        FeatureButton("预约与到访") { store.features.open(FeatureRoute(FeaturePage.Appointments)) }
        FeatureButton("有偿信息与披露") { store.features.open(FeatureRoute(FeaturePage.Offers)) }
        FeatureButton("核实、举报与申诉") { store.features.open(FeatureRoute(FeaturePage.Reviews)) }
        FeatureButton("通知设置与附近提醒") { store.features.open(FeatureRoute(FeaturePage.Settings)) }
        FeatureButton("账号安全与手机验证") { store.features.open(FeatureRoute(FeaturePage.Security)) }
        FeatureButton("资料删除、注销与协助") { store.features.open(FeatureRoute(FeaturePage.DataRequests)) }
        FeatureButton("待核对操作与受限进度") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
        FeatureButton("新增宠物档案") { store.features.open(FeatureRoute(FeaturePage.PetEdit)) }
        ActionCard("领养申请进度", "查看申请与双方确认") { store.open(MobileDestination.Applications) }
        ActionCard("通知", "互动、申请与消息提醒") { store.open(MobileDestination.Notifications) }
        ActionCard("走失互助", "查看走失与拾获信息") { store.open(MobileDestination.Help) }
        OutlinedButton(onClick = store::logout, enabled = !s.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("退出当前账号") }
        SectionTitle("我的宠物")
    }) { row -> PetCard(store, s, row) { store.open(MobileDestination.Pet(row.id)) } }
}

@Composable private fun SignInPrompt(store: MobileStore) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("登录后继续此任务"); Button(onClick = store::showLogin) { Text("登录") } }
} }
@Composable private fun ScrollContent(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
@Composable private fun ActionCard(title: String, detail: String, action: () -> Unit) {
    Card(onClick = action, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable private fun Photos(store: MobileStore, media: List<Long>, account: Long?, description: String) {
    media.forEach { id -> Card(shape = RoundedCornerShape(16.dp)) { MobilePhoto(store, id, account, description, Modifier.fillMaxWidth().aspectRatio(1.25f)) } }
}
@Composable private fun ErrorPanel(error: String, retry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(error); OutlinedButton(onClick = retry) { Text("重新载入") }
} }
@Composable private fun <T> Records(page: PageState<T>, title: String, retry: () -> Unit, more: () -> Unit,
    header: @Composable () -> Unit = {}, row: @Composable (MobileRecord<T>) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle(title) }; item { header() }
        items(page.items, key = { it.id }) { row(it) }
        item { PageFooter(page, retry, more) }
    }
}
@Composable private fun <T> PageFooter(page: PageState<T>, retry: () -> Unit, more: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (page.loading) CircularProgressIndicator(Modifier.padding(16.dp))
        page.error?.let { Text(it); OutlinedButton(onClick = retry) { Text("重试") } }
        if (page.loaded && page.items.isEmpty() && !page.loading && page.error == null) Text("暂时没有可显示的内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (page.loaded && page.next != 0L && !page.loading) OutlinedButton(onClick = more, modifier = Modifier.heightIn(min = 48.dp)) { Text("继续加载") }
    }
}
private fun MobileTab.icon(): ImageVector = when (this) {
    MobileTab.Community -> Icons.Outlined.Home; MobileTab.Adoption -> Icons.Outlined.FavoriteBorder
    MobileTab.Publish -> Icons.Outlined.AddCircleOutline; MobileTab.Messages -> Icons.Outlined.ChatBubbleOutline; MobileTab.Profile -> Icons.Outlined.PersonOutline
}
private fun title(s: MobileState): String = when (s.destination) {
    MobileDestination.Home -> s.tab.label; is MobileDestination.Pet -> "宠物详情"; is MobileDestination.Entry -> "社区动态"
    is MobileDestination.Application, MobileDestination.Applications -> "领养申请"; is MobileDestination.Chat -> "消息"
    is MobileDestination.Case, MobileDestination.Help -> "走失互助"; MobileDestination.Notifications -> "通知"
}
private fun notificationDestination(body: NotificationBody): MobileDestination? = when (body.targetKind) {
    "entries" -> MobileDestination.Entry(body.targetId); "pets" -> MobileDestination.Pet(body.targetId)
    "adoptions" -> MobileDestination.Application(body.targetId); "cases" -> MobileDestination.Case(body.targetId)
    "conversations" -> MobileDestination.Chat(body.targetId); "groups" -> MobileDestination.Chat(body.targetId, true); else -> null
}
