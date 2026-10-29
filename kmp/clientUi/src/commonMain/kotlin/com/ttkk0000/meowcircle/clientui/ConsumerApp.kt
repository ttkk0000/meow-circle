package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
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
import com.ttkk0000.meowcircle.client.*

// 按当前窗口与输入占用选择布局；手机、平板始终共用同一个 Store。
data class ClientWindowPolicy(val rail: Boolean, val master: Boolean, val contextPane: Boolean, val keyboard: Boolean = false,
    val expandedRail: Boolean = false)

fun clientWindowPolicy(width: Float, height: Float, fontScale: Float, keyboard: Boolean): ClientWindowPolicy {
    val scale = fontScale.coerceAtLeast(1f)
    val rail = width >= 680f
    val expandedRail = rail && width >= 1280f && width / scale >= 1000f
    val readableWidth = (width - if (expandedRail) 176f else if (rail) 80f else 0f) / scale
    return ClientWindowPolicy(rail, width >= 980f && readableWidth >= 900f && height >= 480f && !keyboard,
        width >= 1100f && readableWidth >= 1020f && height >= 600f && !keyboard, keyboard, expandedRail)
}

@Composable
fun ConsumerApp(store: MobileStore, pendingLink: PendingTaskLink? = null, openPendingLink: () -> Unit = {}, dismissPendingLink: () -> Unit = {}, uiSession: ClientUiSessionState? = null, platform: ClientUiPlatform) {
    val state by store.state.collectAsState()
    val features by store.features.state.collectAsState()
    val sensitive by store.features.sensitive.state.collectAsState()
    val providerAuth by store.providerAuth.state.collectAsState()
    val session = uiSession ?: remember { ClientUiSessionState() }
    val owner = state.user?.id ?: 0L
    remember(session, owner) { session.useOwner(owner) }
    SideEffect {
        session.retainTransientScopes(buildSet {
            features.route?.let { add(featureUiScope(it)) }
            if(features.route?.page == FeaturePage.Adult) add("transient:age")
            if(features.route?.page == FeaturePage.Guardians) add("transient:guardian")
            if (state.auth.visible) add("transient:auth")
            if (providerAuth.stage != ProviderAuthStage.Idle) add("transient:provider:${providerAuth.flow}")
            if (sensitive.stage != SensitiveStage.Idle) add("transient:sensitive:${sensitive.clientRequestId}")
            state.publicationConfirmation?.let { add("transient:publication:${it.clientRequestId}") }
        })
    }
    platform.BackHandler(providerAuth.stage != ProviderAuthStage.Idle || state.auth.visible || state.publicationConfirmation != null || features.route != null || sensitive.stage != SensitiveStage.Idle || state.destination != MobileDestination.Home || state.applying || state.confirmation != null) {
        when {
            features.busy || providerAuth.busy && providerAuth.stage != ProviderAuthStage.Authorizing -> Unit
            providerAuth.stage != ProviderAuthStage.Idle -> store.providerAuth.dismiss()
            state.auth.visible -> store.cancelLogin()
            sensitive.stage != SensitiveStage.Idle -> store.features.sensitive.dismiss()
            features.route != null -> store.features.back()
            state.publicationConfirmation != null -> store.dismissPublication()
            else -> store.back()
        }
    }
    CompositionLocalProvider(LocalUiSession provides session, LocalClientPlatform provides platform) { key(owner) { MobileTheme {
        PlatformWindow {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val keyboard = WindowInsets.ime.getBottom(density) > 0
            val policy = clientWindowPolicy(maxWidth.value, maxHeight.value, density.fontScale, keyboard)
            val navigationLocked = state.busy || features.busy || providerAuth.stage != ProviderAuthStage.Idle || state.auth.visible || state.publicationConfirmation != null ||
                state.confirmation != null || features.confirmation != null || sensitive.stage != SensitiveStage.Idle
            MobileShell(store, state, features, policy, navigationLocked, pendingLink, openPendingLink, dismissPendingLink)
        }
    } } }
}

@Composable
private fun MobileShell(store: MobileStore, state: MobileState, features: FeatureState, policy: ClientWindowPolicy, navigationLocked: Boolean,
    pendingLink: PendingTaskLink?, openPendingLink: () -> Unit, dismissPendingLink: () -> Unit) {
    val f = store.features
    val ageState by f.age.state.collectAsState()
    val providerAuth by store.providerAuth.state.collectAsState()
    val saved = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    // One content instance moves between full-width and the master pane.
    val home = remember(store, saved) { movableContentOf<MobileState> { current ->
        saved.SaveableStateProvider("${current.user?.id ?: 0}-home-${current.tab}") {
            CompositionLocalProvider(LocalUiScope provides "screen:home-${current.tab}") { HomeContent(store,current) }
        }
    } }
    fun select(tab: MobileTab) { if (!navigationLocked) { f.closeRoute(); store.select(tab) } }
    val unread = state.inbox?.globalTotal
    val showMaster = policy.master && features.route == null && state.tab in listOf(MobileTab.Adoption,MobileTab.Messages) && state.destination != MobileDestination.Home
    val showFeatureMaster = policy.master && features.route?.page in listOf(FeaturePage.Appointment,FeaturePage.Offer,FeaturePage.Claim,FeaturePage.Review)
    val showComposePreview = state.publicationConfirmation == null && policy.master && state.tab == MobileTab.Publish && features.route == null && state.destination == MobileDestination.Home
    val showPublishShortcut = features.route?.page == FeaturePage.GroupDirectory ||
        features.route == null && (state.destination == MobileDestination.Notifications ||
            state.destination == MobileDestination.Home && state.tab in listOf(MobileTab.Community,MobileTab.Adoption,MobileTab.Profile))
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (!policy.rail && !policy.keyboard && features.route == null && state.destination == MobileDestination.Home && state.tab != MobileTab.Publish) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            MobileTab.entries.forEach { tab -> if (tab == MobileTab.Publish) {
                MndPublishButton(PublishButtonSize.Compact, enabled = !navigationLocked, current = state.tab == tab,
                    navigation = true, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { select(tab) }
            } else NavigationBarItem(selected = state.tab == tab, enabled = !navigationLocked,
                onClick = { select(tab) }, icon = { BadgedBox(badge = { if (tab == MobileTab.Messages && unread != null && unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) { DesignIcon(tab.designIcon(),tab.label) } }, label = { Text(tab.label) }) }
        } }, contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (policy.rail) NavigationRail(containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier
                .width(if (policy.expandedRail) 176.dp else 80.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Text("M&D", Modifier.padding(vertical = 20.dp), fontFamily = BrandFont, fontWeight = FontWeight.ExtraBold)
                MobileTab.entries.forEach { tab -> if (tab == MobileTab.Publish) {
                    MndPublishButton(if (policy.expandedRail) PublishButtonSize.Sidebar else PublishButtonSize.Compact,
                        enabled = !navigationLocked, current = state.tab == tab, navigation = true,
                        modifier = Modifier.width(if (policy.expandedRail) 152.dp else 56.dp)
                            .heightIn(min = if (policy.expandedRail) 48.dp else 56.dp)) { select(tab) }
                } else NavigationRailItem(selected = state.tab == tab, enabled = !navigationLocked,
                    onClick = { select(tab) }, icon = { BadgedBox(badge = { if (tab == MobileTab.Messages && unread != null && unread > 0) Badge { Text(if(unread > 99) "99+" else "$unread") } }) { DesignIcon(tab.designIcon(),tab.label) } }, label = { Text(tab.label) }) }
            }
            if (policy.contextPane && !showMaster && !showFeatureMaster && !showComposePreview) ContextPane(store, state, features, Modifier.width(224.dp).fillMaxHeight(), enabled = !navigationLocked)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                val sharedLink = state.pendingTaskLink?.let { PendingTaskLink(it.target.kind,it.target.id,it.owner) }
                PendingLinkBanner(pendingLink ?: sharedLink, !navigationLocked && store.canOpenNotification(),
                    if (pendingLink != null) openPendingLink else store::openPendingTaskLink,
                    if (pendingLink != null) dismissPendingLink else store::dismissTaskLink)
                PendingNotificationBanner(store, state, !navigationLocked && store.canOpenNotification())
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.publicationConfirmation != null || features.route != null || state.destination != MobileDestination.Home) IconButton(onClick = { if (!f.back()) { if (state.publicationConfirmation != null) store.dismissPublication() else store.back() } }, enabled = !state.busy && !features.busy) { Icon(Icons.Outlined.ArrowBack, "返回") }
                    Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text(if(features.route == null && state.publicationConfirmation != null) "发布前预览" else if(features.route?.page == FeaturePage.Adult) ageStageTitle(ageState.stage) else if(features.route!=null) featureTitle(features,state.user?.id) else if(state.destination == MobileDestination.Home && state.tab == MobileTab.Community) "M&D" else title(state), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (features.route == null && state.destination == MobileDestination.Home && state.tab == MobileTab.Community) Text("meow & doggie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (showPublishShortcut) MndPublishButton(PublishButtonSize.Shortcut, enabled = !navigationLocked,
                        modifier = Modifier.size(48.dp)) { select(MobileTab.Publish) }
                    if (state.user == null) TextButton(onClick = store::showLogin, enabled = !navigationLocked) { Text("登录") }
                    IconButton(onClick = { f.open(FeatureRoute(FeaturePage.Safety)) }, enabled = !navigationLocked) { Icon(Icons.Outlined.HelpOutline,"安全与隐私求助") }
                    IconButton(onClick = { if(features.route != null) f.reload() else if(state.destination == MobileDestination.Home) store.refreshHome() else store.reloadDetail(); store.refreshInbox() }, enabled = !navigationLocked) { Icon(Icons.Outlined.Refresh,"重新载入") }
                }
                MviStatusBanner(
                    loading = state.restoring || state.busy,
                    notice = state.notice,
                    bannerNotice = true,
                    onDismissNotice = store::clearNotice,
                )
                if(state.inboxError != null) Text(if (state.inboxReadAt != null) "未读暂未更新 · 上次读取 ${displayTime(state.inboxReadAt)}" else "未读暂不可读，未将失败当作零条消息", Modifier.padding(horizontal = 16.dp),style = MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxSize()) {
                    if (showMaster) {
                        Box(Modifier.width(300.dp).fillMaxHeight()) { home(state) }
                        VerticalDivider()
                    }
                    if(showFeatureMaster) {
                        Box(Modifier.width(280.dp).fillMaxHeight()) { saved.SaveableStateProvider("${state.user?.id ?: 0}-feature-master-${features.route?.page}") { CompositionLocalProvider(LocalUiScope provides "master:${features.route?.page}") { ClientFeatureMaster(store,features) } } }
                        VerticalDivider()
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(),contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = MobileUi.contentWidth).fillMaxSize()) {
                            val key = features.route?.let { "feature-${it.page}-${it.id}-${it.kind}-${it.peer}" } ?: if(state.destination == MobileDestination.Home) "home-${state.tab}" else "detail-${state.destination}"
                            if (features.route == null && state.publicationConfirmation != null) {
                                CompositionLocalProvider(LocalUiScope provides "transient:publication:${state.publicationConfirmation.clientRequestId}") { PublicationPreviewPage(store,state) }
                            } else if(features.route == null && state.destination == MobileDestination.Home) home(state)
                            else saved.SaveableStateProvider("${state.user?.id ?: 0}-$key") {
                                CompositionLocalProvider(LocalUiScope provides (features.route?.let(::featureUiScope) ?: "screen:$key")) {
                                    if(features.route != null) ClientFeatureContent(store,features) else DetailContent(store,state)
                                }
                            }
                        }
                    }
                    if(showComposePreview) {
                        VerticalDivider(); Column(Modifier.width(280.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(MobileUi.pagePadding),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("发布预览",style = MaterialTheme.typography.titleMedium); Text(state.composeTitle); Text(state.composeText); Text(audienceLabel(state.composeEntry.audience.mode))
                            state.photos.mapNotNull { it.mediaId }.forEach { MobilePhoto(store,it,state.user?.id,"发布预览",Modifier.fillMaxWidth().aspectRatio(1f)) }
                        }
                    }
                }
            }
        }
    }
    if (state.auth.visible || providerAuth.stage != ProviderAuthStage.Idle) ClientAuthDialog(store,state)
    InboxReadAllDialog(store,state)
    FeatureDialogs(store,features)
    state.confirmation?.let { command ->
        MobileAlertDialog(onDismissRequest = store::dismissConfirmation, title = { Text(command.label) },
            text = { Text("${command.label}？\n当前申请：${adoptionStateLabel(command.record.state)}\n${command.note}\n提交后由服务端校验最新进度。交接需要双方各自确认。") },
            confirmButton = { TextButton(onClick = store::confirmAdoptionAction, enabled = !state.busy && !state.conflict) { Text("确认提交") } },
            dismissButton = { TextButton(onClick = store::dismissConfirmation, enabled = !state.busy) { Text("返回核对") } })
    }
}

@Composable
fun ContextPane(store: MobileStore, state: MobileState, features: FeatureState, modifier: Modifier, enabled: Boolean = true) {
    CompositionLocalProvider(LocalTaskNavigationEnabled provides enabled) {
    val f = store.features
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if(features.route != null) featureTitle(features,state.user?.id) else state.tab.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        when(state.tab) {
            MobileTab.Community -> {
                NavigationRow("推荐日常") { f.closeRoute(); store.select(MobileTab.Community); store.filterFeed("community") }
                NavigationRow("宠友圈") { f.closeRoute(); store.select(MobileTab.Community); store.filterFeed("circle") }
                NavigationRow("宠物日记") { f.closeRoute(); store.select(MobileTab.Community); store.filterFeed("diary") }
                NavigationRow("好友与邀请") { f.open(FeatureRoute(FeaturePage.Friends)) }
            }
            MobileTab.Adoption -> {
                NavigationRow("领养介绍") { f.closeRoute(); store.select(MobileTab.Adoption) }
                NavigationRow("领养申请进度") { f.closeRoute(); store.open(MobileDestination.Applications) }
                NavigationRow("预约与安排") { f.open(FeatureRoute(FeaturePage.Appointments)) }
                NavigationRow("有偿信息") { f.open(FeatureRoute(FeaturePage.Offers)) }
            }
            MobileTab.Publish -> {
                NavigationRow("我的发布","维护介绍、评论与求助") {f.open(FeatureRoute(FeaturePage.Publications))}
                NavigationRow("晒晒我的宠物") { f.closeRoute(); store.select(MobileTab.Publish) }
                NavigationRow("新增宠物档案") { f.open(FeatureRoute(FeaturePage.PetEdit)) }
                NavigationRow("走失／拾获求助") { f.open(FeatureRoute(FeaturePage.CaseEdit)) }
                NavigationRow("本机草稿与原操作") { f.open(FeatureRoute(FeaturePage.Recovery)) }
            }
            MobileTab.Messages -> {
                NavigationRow("聊天与未读") { f.closeRoute(); store.select(MobileTab.Messages) }
                NavigationRow("互动与通知") { f.closeRoute(); store.open(MobileDestination.Notifications) }
                NavigationRow("协作与群邀请") { f.open(FeatureRoute(FeaturePage.Invitations)) }
                NavigationRow("创建群聊") { f.open(FeatureRoute(FeaturePage.GroupCreate)) }
                NavigationRow("提醒设置") { f.open(FeatureRoute(FeaturePage.Settings)) }
            }
            MobileTab.Profile -> {
                NavigationRow("我的资料") { f.closeRoute(); store.select(MobileTab.Profile) }
                NavigationRow("账号安全") { f.open(FeatureRoute(FeaturePage.Security)) }
                NavigationRow("资料与账号请求") { f.open(FeatureRoute(FeaturePage.DataRequests)) }
                NavigationRow("关注与求助订阅") { store.features.open(FeatureRoute(FeaturePage.Content)) }
        NavigationRow("规则与权益") { f.open(FeatureRoute(FeaturePage.Rules)) }
            }
        }
        NavigationRow("走失互助") { f.closeRoute(); store.open(MobileDestination.Help) }
        val chatDestination = state.destination as? MobileDestination.Chat
        if(chatDestination != null) {
            HorizontalDivider()
            if(chatDestination.group) {
                Text(state.group?.data?.name ?: "群聊上下文")
                Text(state.group?.let { "${it.data.members.size} 位成员 · ${stateLabel(it.state)}" } ?: "等待读取当前成员资格")
                NavigationRow("群公告与成员管理") { f.open(FeatureRoute(FeaturePage.Group,chatDestination.id)) }
            } else state.conversation?.let { chat ->
                val peer = if(chat.data.requesterId == state.user?.id) chat.data.recipientId else chat.data.requesterId
                NavigationRow("查看对方资料") { f.open(FeatureRoute(FeaturePage.Member,peer)) }
                Text(stateLabel(chat.state))
            }
        }
        HorizontalDivider()
        Text(state.user?.nickname ?: "以游客身份浏览", fontWeight = FontWeight.Bold)
        Text("先了解，再决定。核实结果只说明已检查的具体事项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

}

@Composable
private fun HomeContent(store: MobileStore, s: MobileState) {
    when (s.tab) {
        MobileTab.Community -> Feed(store, s)
        MobileTab.Adoption -> Records(s.pets, "先了解，再决定", { store.refreshHome() }, { store.refreshHome(true) },
            emptyText = "当前暂无待领养宠物", header = { AdoptionFilters(store,s);Text("领养不是免费买宠。具体条件、健康未知和必要费用都应分别说明。", color = MaterialTheme.colorScheme.onSurfaceVariant) }) { row ->
            PetCard(store, s, row) { store.open(MobileDestination.Pet(row.id)) }
        }
        MobileTab.Publish -> PublishScreen(store, s)
        MobileTab.Messages -> if (s.user == null) SignInPrompt(store) else InboxScreen(store, s)
        MobileTab.Profile -> if (s.user == null) SignInPrompt(store) else ProfileScreen(store, s)
    }
}

@Composable private fun AdoptionFilters(store: MobileStore,s: MobileState) {
    var keyword by remember(s.petKeyword) { mutableStateOf(s.petKeyword) }
    var city by remember(s.petCity) { mutableStateOf(s.petCity) }
    var species by remember(s.petSpecies) { mutableStateOf(s.petSpecies) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(keyword,{ keyword = it },Modifier.fillMaxWidth(),label = { Text("搜索名字、品种或介绍") })
        CityField(city,{ city = it })
        ChoiceChipBar(listOf("" to "全部","cat" to "猫咪","dog" to "狗狗"),species,{ species = it })
        FeatureButton("按当前条件筛选",!s.pets.loading) { store.filterPets(keyword.trim(),city.trim(),species) }
        if (s.pets.empty && (s.petKeyword.isNotBlank() || s.petCity.isNotBlank() || s.petSpecies.isNotBlank()))
            FeatureButton("清空条件", !s.pets.loading) { store.filterPets("", "", "") }
        FeatureButton("不提供定位也可以") { store.features.open(FeatureRoute(FeaturePage.LocationHelp)) }
    }
}

@Composable
private fun Feed(store: MobileStore, s: MobileState) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    LazyVerticalGrid(columns = GridCells.Fixed(if(maxWidth >= 364.dp) 2 else 1), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { PageRefresh(s.entries) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var query by rememberSessionUiState("feed.query") { s.feedKeyword }
                OutlinedTextField(query,{ query = it },Modifier.fillMaxWidth(),label = { Text("搜索宠友日常与互助") },trailingIcon = { IconButton({ store.filterFeed(s.feedSpace,query) }) { Icon(Icons.Outlined.Search,"搜索") } },singleLine = true)
                ChoiceChipBar(listOf("community" to "推荐","following" to "关注","circle" to "宠友圈","local" to "同城"),s.feedSpace,{ store.filterFeed(it) },scrollable = true)
                if(s.feedSpace == "local") CityField(s.feedCity,{ store.filterFeed("local",city = it) },"同城范围")
                NavigationRow("晒晒今天的小可爱…","照片、日记与宠友分享") { store.select(MobileTab.Publish) }
                NavigationRow("养宠指南","迎接第一只猫，先做好准备") {store.features.open(FeatureRoute(FeaturePage.Reading,kind="care-first-cat"))}
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { store.select(MobileTab.Adoption) }, modifier = Modifier.weight(1f)) { Text("领养") }
                OutlinedButton(onClick = { store.open(MobileDestination.Help) }, modifier = Modifier.weight(1f)) { Text("走失互助") }
            }
        }
        items(s.entries.items, key = { it.id }) { row -> FeedTile(store,s,row) }
        item(span = { GridItemSpan(maxLineSpan) }) { FeatureButton("有偿信息、到访与费用披露") { store.features.open(FeatureRoute(FeaturePage.Offers)) } }
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (s.entries.empty) {
                val filtered = s.feedKeyword.isNotBlank() || s.feedCity.isNotBlank() || s.feedSpace != "community"
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CatPlaceholder()
                    HonestStateCard(if (filtered) "这次还没有合适的结果" else "第一份日常，等你来分享",
                        if (filtered) "可以调整搜索或城市；不会把其它范围的内容偷偷当作当前结果。" else "这里还没有真实内容。可以先看看养宠指南，或记录你和宠物的一天。")
                    if (filtered) FeatureButton("清空条件并查看推荐") { store.filterFeed("community", "", "") }
                    FeatureButton("看看养宠指南") { store.features.open(FeatureRoute(FeaturePage.Reading, kind = "care-first-cat")) }
                    MndPrimaryButton("晒晒我的宠物") { store.select(MobileTab.Publish) }
                }
            } else PageFooter(s.entries, { store.refreshHome() }, { store.refreshHome(true) })
        }
    }
    }
}

@Composable
fun PetCard(store: MobileStore, s: MobileState, row: MobileRecord<PetBody>, open: () -> Unit) {
    Card(onClick = open, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp)) { MobilePhoto(store, row.mediaIds.firstOrNull(), s.user?.id, row.data.name, Modifier.size(96.dp, 112.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.data.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(listOf(speciesLabel(row.data.species), row.data.breed, when(row.data.sex) { "male" -> "公"; "female" -> "母"; else -> "" }).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(row.data.bio, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if(row.data.city.isNotBlank()) Text(row.data.city,style = MaterialTheme.typography.bodySmall)
                Text(if (row.data.adoptionAvailable) "正在寻找领养人" else "宠物档案", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DetailContent(store: MobileStore, s: MobileState) {
    if (s.detailLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    s.detailError?.let { error ->
        CompositionLocalProvider(LocalUiScope provides "${LocalUiScope.current}:read-failure") { TaskColumn {
            ReadFailureContent(s.detailFailure, error, { store.reloadDetail() })
            UnavailableTaskActions(store, s.detailFailure)
        } }
        return
    }
    when (val destination = s.destination) {
        is MobileDestination.Pet -> if(destination.discussion) PetDiscussion(store,s) else PetDetail(store,s)
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
            ContentSubscriptionButton(store,record.id,true)
            NavigationRow("查看公开大致区域") { store.features.open(FeatureRoute(FeaturePage.Map,record.id)) }
            FeatureButton("私密线索与认领") { store.features.open(FeatureRoute(FeaturePage.CaseWork,record.id)) }
            FeatureButton("报告安全或隐私问题") { store.features.open(FeatureRoute(FeaturePage.Safety,kind = "cases",peer = record.id)) }
        } }
        MobileDestination.Help -> Records(s.cases, "走失互助", { store.reloadDetail() }, { store.reloadDetail(true) }, emptyText = "暂无可显示的走失或拾获信息", header = {
            CityField(s.caseCity,store::filterCases,"求助城市")
            FeatureButton("发布走失／拾获求助") { if(s.user == null) store.showLogin() else store.features.open(FeatureRoute(FeaturePage.CaseEdit)) }
            FeatureButton("互助地图") { store.features.open(FeatureRoute(FeaturePage.Map)) }
            FeatureButton("附近提醒") { store.features.open(FeatureRoute(FeaturePage.Nearby)) }
        }) { row ->
            ActionCard(row.data.title, "${row.data.city} · ${row.data.area}\n${if (row.data.type == "lost") "走失" else "拾获"}") { store.open(MobileDestination.Case(row.id)) }
        }
        MobileDestination.Applications -> if (s.user == null) SignInPrompt(store) else Records(s.applications, "领养申请进度", { store.reloadDetail() }, { store.reloadDetail(true) }, emptyText = "暂无领养申请") { row ->
            ActionCard(adoptionStateLabel(row.state), row.data.message) { store.open(MobileDestination.Application(row.id)) }
        }
        MobileDestination.Notifications -> if (s.user == null) SignInPrompt(store) else Records(s.notifications, "通知", { store.reloadDetail() }, { store.reloadDetail(true) }, emptyText = "暂无通知", header = {
            FeatureButton("全部消息与通知标为已读",!s.busy && !s.inboxReadAll.preparing && !s.inboxReadAll.submitting) { store.prepareInboxReadAll() }
            FeatureButton("设置") { store.features.open(FeatureRoute(FeaturePage.SettingsHome)) }
            Text("进入本页不会清空未读；待处理事项与已读分开。")
            if(s.notifications.items.any { !it.data.read }) FeatureButton("将当前载入通知标为已读",!s.busy) {
                val snapshot = s.notifications.items.filter { !it.data.read }
                store.features.confirm("将已载入通知标为已读","仅标记当前 ${snapshot.size} 条通知，不接受申请或确认预约；新通知仍保持未读。") { snapshot.forEach { store.api.readNotification(it.id,it.version) }; store.reloadDetail(); store.refreshInbox() }
            }
        }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(MobileUi.pagePadding), verticalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
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
    val owned = s.user?.id == record.ownerId
    ScrollContent {
        Photos(store, record.mediaIds, s.user?.id, record.data.name)
        SectionTitle(record.data.name)
        Text(listOf(speciesLabel(record.data.species), record.data.breed, record.data.sex).filter { it.isNotBlank() }.joinToString(" · "))
        Text(record.data.bio)
        if(record.data.adoptionAvailable || record.data.adoptionOpened) NavigationRow("领养交流","照护讨论与公开答疑") {store.open(MobileDestination.Pet(record.id,true))}
        NavigationRow("独立图文日记","只读取这只宠物的内容") { store.features.open(FeatureRoute(FeaturePage.PetDiary,record.id)) }
        if(owned) {
            Text("${if(record.data.missing) "暂时找不到它" else "在家"} · ${audienceLabel(record.data.audience.mode)}",color = MaterialTheme.colorScheme.primary)
            NavigationRow("当前状态",if(record.data.missing) "暂时找不到它 · 求助资料分别设置" else "在家 · 可单独更新走失状态") { store.features.open(FeatureRoute(FeaturePage.PetEdit,record.id)) }
            NavigationRow("资料可见性","与单条日记、公开求助分别设置") { store.features.open(FeatureRoute(FeaturePage.PetEdit,record.id)) }
            NavigationRow("切换或添加宠物","猫咪、狗狗和其他宠物") { store.select(MobileTab.Profile) }
            TaskNotice("自己的记录，不是认证","档案、照片和爱心数不构成真实性担保。")
            FeaturePrimary("给${record.data.name}写日记") { store.select(MobileTab.Publish); store.editComposeOptions("diary", Audience("private"), record.id) }
        }
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
                Button(onClick = store::submitApplication, enabled = !s.busy && s.applicationText.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = MobileUi.actionHeight)) { Text("确认并提交申请") }
            } else Button(onClick = store::beginApplication, modifier = Modifier.fillMaxWidth().heightIn(min = MobileUi.actionHeight)) { Text("申请领养") }
            OutlinedButton(onClick = store::contactPetOwner, enabled = !s.busy, modifier = Modifier.fillMaxWidth().heightIn(min = MobileUi.actionHeight)) { Text("联系发布人") }
        }
    }
}


@Composable private fun PetDiscussion(store: MobileStore,s: MobileState) {
    val record=s.pet ?: return
    if(!record.data.adoptionAvailable && !record.data.adoptionOpened) {
        ScrollContent {TaskNotice("目前未开放领养交流","仍可返回查看这只宠物的档案与日记。");NavigationRow("返回宠物档案") {store.back()}}
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {Text("正在讨论：${record.data.name}",style=MaterialTheme.typography.titleMedium)}
            commentItems(store,s,"pet_comments",showTitle=false)
        }
        ReplyEditor(store,s,"说点什么…")
    }
}

@Composable
private fun EntryDetail(store: MobileStore, s: MobileState) {
    val entry = s.entry ?: return
    Column(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NavigationRow(if(entry.ownerId == s.user?.id) s.user?.nickname.orEmpty() else "宠友 #${entry.ownerId}", "查看发布人资料") { store.features.open(FeatureRoute(FeaturePage.Member,entry.ownerId)) } }
        item { Photos(store, entry.mediaIds, s.user?.id, entry.data.title) }
        item { SectionTitle(entry.data.title); Text(entry.data.text); if(entry.data.city.isNotBlank()) Text(entry.data.city,style = MaterialTheme.typography.labelSmall); Text("${audienceLabel(entry.data.audience.mode)} · ${originLabel(entry.data.contentOrigin)}",color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            if(entry.ownerId == s.user?.id) {
                FeatureButton("编辑动态") { store.features.open(FeatureRoute(FeaturePage.EntryEdit,entry.id)) }
                FeatureButton("删除动态",!s.busy) { store.features.confirm("删除动态",entry.data.title.ifBlank { entry.data.text }) { store.api.removeResource("entries",entry.id,entry.version); store.back(); store.refreshHome() } }
            }
            FeatureButton("举报此内容") { store.features.open(FeatureRoute(FeaturePage.Safety,kind = "entries",peer = entry.id)) }
        }
        item {
            val metrics = s.metrics[entry.id]
            TaskSection("互动") { Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = store::toggleLike,enabled = metrics != null && !s.busy,modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { DesignIcon("heart",if(metrics?.viewerLiked == true) "取消喜欢" else "喜欢"); Spacer(Modifier.width(8.dp)); Text(metrics?.likes?.toString() ?: "—") }
                Row(Modifier.weight(1f).heightIn(min = 48.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) { DesignIcon("message-circle","评论"); Text(metrics?.comments?.toString() ?: "—") }
            } }
        }
        commentItems(store,s,"entry_comments")
    }
    ReplyEditor(store, s, "说点什么…")
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.commentItems(store: MobileStore,s: MobileState,kind: String,showTitle: Boolean = true) {
    if(showTitle) item { SectionTitle(if(kind=="pet_comments") "领养交流" else "评论") }
    items(s.comments.items,key={it.id}) {row ->
        MemberSection(store,row.ownerId,compact=true) {
            if(row.data.replyTo>0) Text("回复评论 #${row.data.replyTo}",style=MaterialTheme.typography.bodySmall)
            Text(row.data.text)
            if(row.data.stickerId.isNotBlank()) StickerPhoto(row.data.stickerId,row.data.stickerId)
            var menu by rememberSessionUiState("comment.menu.${row.id}") {false}
            val metric=s.commentMetrics[row.id]
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                TextButton({store.replyTo(row.id)},modifier=Modifier.weight(1f)) {Text("回复")}
                TextButton({store.toggleCommentLike(row.id)},enabled=metric!=null && !s.busy,
                    colors=ButtonDefaults.textButtonColors(contentColor=if(metric?.viewerLiked==true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                    DesignIcon(if(metric?.viewerLiked==true) "comment-heart-liked" else "comment-heart",if(metric?.viewerLiked==true) "取消喜欢" else "喜欢")
                    Text("${if(metric?.viewerLiked==true) "已赞" else "点赞"} ${metric?.likes ?: "—"}")
                }
                Box {
                    IconButton({menu=true},enabled=!s.busy) {DesignIcon("comment-more","评论操作")}
                    DropdownMenu(menu,{menu=false}) {
                        DropdownMenuItem(text={Text("举报此评论")},onClick={menu=false;store.features.open(FeatureRoute(FeaturePage.Review,kind=kind,peer=row.id));store.features.edit("type","report")})
                        if(row.ownerId==s.user?.id) DropdownMenuItem(text={Text("删除此评论")},onClick={menu=false;store.features.confirm("删除评论",row.data.text.ifBlank {"贴纸评论"}) {store.api.removeResource(kind,row.id,row.version);store.loadComments()}})
                    }
                }
            }
        }
    }
    if(kind=="pet_comments") item {TaskNotice("评论不能替代医疗资料","想了解具体情况可站内联系，不公开证件和家庭地址。")}
    item {PageFooter(s.comments,{store.loadComments()},{store.loadComments(true)},"还没有评论")}
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
                modifier = Modifier.fillMaxWidth().heightIn(min = MobileUi.actionHeight)) { Text(label) }
        }
    }
}

@Composable
private fun ChatScreen(store: MobileStore, s: MobileState, chat: MobileDestination.Chat) {
    val conversation = s.conversation
    val leadContext = conversation?.data?.contextKind == "lead"
    val contextWritable = !leadContext || (s.chatLead != null && s.chatCase?.state in listOf("active","matched"))
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val uiScope = rememberCoroutineScope()
    val firstUnread = (if(chat.group) s.inbox?.groupFirstUnread else s.inbox?.directFirstUnread)?.get(chat.id.toString())
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if(chat.group) {
            Text("${s.group?.data?.name.orEmpty()} · ${s.group?.data?.members?.size ?: 0} 位成员",style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
            if(!s.group?.data?.notice.isNullOrBlank()) TaskNotice("群公告",s.group!!.data.notice)
            TextButton({ store.features.open(FeatureRoute(FeaturePage.Group,chat.id)) }) { Text("群公告、成员与管理") }
        }
        if (conversation?.state == "pending") {
            Text("对方接受前，发起人仅可发送一条文字。", Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (conversation.data.recipientId == s.user?.id) Row(horizontalArrangement = Arrangement.spacedBy(MobileUi.contentGap)) {
                Button(onClick = { store.answerMessageRequest("accept") }, enabled = !s.busy && contextWritable) { Text("接受消息请求") }
                OutlinedButton(onClick = { store.answerMessageRequest("decline") }, enabled = !s.busy) { Text("拒绝") }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            if(firstUnread != null) TextButton({ store.loadMessages(); uiScope.launch { listState.scrollToItem(if(leadContext) 1 else 0) } },enabled = !s.messages.loading) { Text("定位首条未读") }
            TextButton({ store.loadMessages(fromBeginning = true); uiScope.launch { listState.scrollToItem(0) } },enabled = !s.messages.loading) { Text("读取较早历史") }
        }
        PageRefresh(s.messages)
        LazyColumn(Modifier.weight(1f),state = listState, verticalArrangement = Arrangement.spacedBy(MobileUi.contentGap), contentPadding = PaddingValues(vertical = 16.dp)) {
            if (leadContext) item(key = "lead-context") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    s.chatCase?.let { LeadCaseContext(store,it) }
                    TaskNotice("私密线索 #${conversation?.data?.contextId} · 目击者会话",if (contextWritable) "保持站内联系，不先付费换线索。" else "本次求助已停止新联系，当前仅查看私密历史。")
                }
            }
            items(s.messages.items, key = { it.id }) { row ->
                if(row.id == firstUnread) Text("以下为未读消息",color = MaterialTheme.colorScheme.primary,style = MaterialTheme.typography.labelMedium)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val bubbleWidth = minOf(560.dp, maxWidth * (300f / 358f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (row.ownerId == s.user?.id) Arrangement.End else Arrangement.Start) {
                        Surface(shape = MaterialTheme.shapes.medium, color = if (row.ownerId == s.user?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.widthIn(max = bubbleWidth)) {
                            Column(Modifier.padding(horizontal = 16.dp,vertical = 12.dp),verticalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
                                if(chat.group && row.ownerId != s.user?.id) Text(s.members[row.ownerId]?.nickname?.ifBlank { "群成员" } ?: "群成员",style = MaterialTheme.typography.labelSmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if(row.data.text.isNotBlank()) CompositionLocalProvider(LocalContentColor provides if(row.ownerId == s.user?.id) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) { RichContentText(row.data.text,s.emoji) }
                                if(row.data.stickerId.isNotBlank()) StickerPhoto(row.data.stickerId,row.data.stickerId)
                                row.mediaIds.forEach { MobilePhoto(store,it,s.user?.id,"消息照片",Modifier.fillMaxWidth().aspectRatio(1.25f)) }
                            }
                        }
                    }
                }
            }
            item { PageFooter(s.messages, { store.loadMessages() }, { store.loadMessages(true) }, "暂无消息", refreshVisible = true) }
        }
        if (s.messages.items.isNotEmpty()) TextButton(onClick = store::markChatRead, enabled = !s.busy) { Text("将已载入消息标为已读") }
        val canSend = contextWritable && ((chat.group && s.group?.let { it.state == "active" && s.user?.id in it.data.members } == true) || conversation?.state == "accepted" || conversation?.let { it.state == "pending" && it.data.requesterId == s.user?.id && s.messages.loaded && s.messages.items.isEmpty() } == true)
        if (canSend) ReplyEditor(store, s, "消息",compact = leadContext) else Text("当前会话暂不可发送", Modifier.padding(MobileUi.pagePadding))
    }
}

@Composable
private fun ReplyEditor(store: MobileStore, s: MobileState, label: String, compact: Boolean = false) {
    var replaceSaved by rememberSessionUiState("reply.replaceSaved") { false }
    if(replaceSaved) MobileAlertDialog(onDismissRequest = { replaceSaved = false },title = { Text("读取本机消息草稿") },text = { Text("放弃此账号未保存的评论与聊天输入，读取本机已保存的原文和附件。不会自动发送。") },
        confirmButton = { TextButton({ replaceSaved = false; store.replaceWithSavedReplies() }) { Text("放弃未保存输入并读取") } },dismissButton = { TextButton({ replaceSaved = false }) { Text("继续编辑") } })
    var editor by rememberPrivateEditor(s.reply,"reply-${s.user?.id}-${s.destination}")
    var emojiOpen by androidx.compose.runtime.saveable.rememberSaveable(s.destination.toString()) { mutableStateOf(false) }
    LaunchedEffect(s.reply) { if (editor.text != s.reply) editor = TextFieldValue(s.reply, TextRange(s.reply.length)) }
    val chat = s.destination as? MobileDestination.Chat
    val richAllowed = chat == null || chat.group || s.conversation?.state == "accepted"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Column(Modifier.heightIn(max = minOf(420.dp,maxHeight * 0.65f)).verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
            if(s.replyDraftRestoring) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在恢复本机消息草稿") }
            s.guestReplySuggestion?.let { text -> FeatureSection("登录前的评论输入") { Text(text); FeatureButton("用登录前的评论替换当前输入",!s.busy && s.replyDraftReady) { store.useGuestReply() } } }
            s.replyDraftError?.let {
                Text(it,color = MaterialTheme.colorScheme.error)
                if(!s.replyDraftReady) FeatureButton("重试读取本机消息草稿",!s.busy) { store.restoreReplyDrafts() }
                else { FeatureButton("重试保存当前消息草稿",!s.busy) { store.retryReplySave() }; FeatureButton("读取本机已保存版本",!s.busy) { replaceSaved = true } }
            }
            if(s.replyTo > 0) Row { Text("回复评论 #${s.replyTo}",Modifier.weight(1f)); TextButton({ store.replyTo(0) }) { Text("取消回复") } }
            if (!compact) MndRichTextField(editor, { if (store.editReply(it.text)) editor = it }, label, enabled = !s.busy, maxLines = 5)
            if(s.replySticker.isNotBlank()) { StickerPhoto(s.replySticker,s.replySticker); TextButton({ store.selectSticker("") },enabled = !s.busy) { Text("移除待发送贴纸") } }
            if(chat != null && richAllowed && (!compact || emojiOpen)) PhotoPickerButton(9 - s.replyPhotos.size,!s.busy,store::addReplyPhotos,store::pickerFailed)
            if (chat != null && richAllowed && (!compact || s.replyPhotos.isNotEmpty())) PhotoQueueList(
                store = store, photos = s.replyPhotos, ownerId = s.user?.id, busy = s.busy,
                showPreview = false, onRetry = store::retryReplyPhoto,
                onMove = store::moveReplyPhoto, onRemove = store::removeReplyPhoto,
                onSaveOrder = store::retryReplySave,
            )
            if (compact) Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp),verticalAlignment = Alignment.Bottom) {
                MndRichTextField(editor,{ if(store.editReply(it.text)) editor = it },"发送消息…",Modifier.weight(1f),enabled = !s.busy,maxLines = 5,compact = true)
                IconButton(onClick = { emojiOpen = !emojiOpen },enabled = !s.busy && editor.composition == null,modifier = Modifier.size(48.dp)) { DesignIcon("smile","表情、贴纸与照片") }
                Button(onClick = store::sendReply,enabled = !s.busy && editor.composition == null && (s.reply.isNotBlank() || s.replySticker.isNotBlank() || s.replyPhotos.isNotEmpty()) && s.replyPhotos.all { it.mediaId != null },
                    modifier = Modifier.widthIn(min = 64.dp).heightIn(min = MobileUi.actionHeight),contentPadding = PaddingValues(horizontal = 16.dp),shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("发送") }
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { emojiOpen = !emojiOpen }) { DesignIcon("smile",null); Spacer(Modifier.width(8.dp)); Text(if (emojiOpen) "收起表情" else "表情与贴纸") }
                Button(onClick = store::sendReply, enabled = !s.busy && (s.reply.isNotBlank() || s.replySticker.isNotBlank() || s.replyPhotos.isNotEmpty()) && s.replyPhotos.all { it.mediaId != null }, modifier = Modifier.heightIn(min = MobileUi.actionHeight)) { Text(if(chat == null) "发布" else "发送") }
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
        MndTextField(s.composeTitle, { store.editCompose(it, s.composeText) }, "标题（选填）", enabled = !s.busy, maxLines = Int.MAX_VALUE)
        MndRichTextField(composer, { next -> store.editCompose(s.composeTitle,next.text); if(store.state.value.composeText == next.text) composer = next }, "今天想分享什么？", minLines = 6, maxLines = Int.MAX_VALUE, enabled = !s.busy)
        TextButton({ emojiOpen = !emojiOpen }) { Text(if(emojiOpen) "收起表情" else "插入表情") }
        if(emojiOpen) ComposeEmojiPanel(store,s,composer) { next -> store.editCompose(s.composeTitle,next.text); if(store.state.value.composeText == next.text) composer = next }
        CityField(s.composeEntry.city,store::editComposeCity,"城市（选填，不含精确定位）",enabled = !s.busy)
        ComposeAudience(store,s)
        PhotoPickerButton(store, 9 - s.photos.size, !s.busy)
        PhotoQueueList(
            store = store,
            photos = s.photos,
            ownerId = s.user?.id,
            busy = s.busy,
            compactPreview = true,
            onRetry = store::retryPhoto,
            onMove = store::movePhoto,
            onRemove = store::removePhoto,
            onSaveOrder = store::saveLocalPhotoOrder,
        )
        if (s.draftLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = store::saveComposeDraft, enabled = !s.busy && s.draftLoaded && !s.draftLoading && s.photos.all { it.mediaId != null }, modifier = Modifier.fillMaxWidth().heightIn(min = MobileUi.actionHeight)) { Text("保存草稿") }
        TextButton(onClick = { replaceDraft = true }, enabled = !s.busy && !s.draftLoading) { Text("用云端草稿替换当前输入") }
        TaskPrimary("预览这篇日常",!s.busy && s.composeAudienceValid && (s.composeText.isNotBlank() || s.photos.isNotEmpty()) && s.photos.all { it.mediaId != null },store::publish)
    }
    if(replaceDraft) MobileAlertDialog(onDismissRequest = { replaceDraft = false },title = { Text("用云端草稿替换当前输入？") },text = { Text("当前未同步文字、受众和照片会被云端版本替换。请先核对是否需要保留本机输入。") },confirmButton = { TextButton({ replaceDraft = false; store.restoreComposeDraft(replaceInput = true) },enabled = !s.busy) { Text("确认替换") } },dismissButton = { TextButton({ replaceDraft = false }) { Text("保留本机输入") } })
}

@Composable
private fun ProfileScreen(store: MobileStore, s: MobileState) {
    Records(s.myPets, s.user?.nickname.orEmpty(), { store.refreshHome() }, { store.refreshHome(true) }, emptyText = "还没有宠物档案，可从上方新增", header = {
        Text(s.user?.bio.orEmpty())
        NavigationRow("规则与权益") { store.features.open(FeatureRoute(FeaturePage.Rules)) }
        NavigationRow("我的发布","维护介绍、图文与求助记录") {store.features.open(FeatureRoute(FeaturePage.Publications))}
        NavigationRow("编辑个人资料") { store.features.open(FeatureRoute(FeaturePage.ProfileEdit)) }
        NavigationRow("关注与求助订阅") { store.features.open(FeatureRoute(FeaturePage.Content)) }
        NavigationRow("好友与宠友圈") { store.features.open(FeatureRoute(FeaturePage.Friends)) }
        NavigationRow("协作与群邀请") { store.features.open(FeatureRoute(FeaturePage.Invitations)) }
        NavigationRow("预约与到访") { store.features.open(FeatureRoute(FeaturePage.Appointments)) }
        NavigationRow("有偿信息与披露") { store.features.open(FeatureRoute(FeaturePage.Offers)) }
        NavigationRow("核实、举报与申诉") { store.features.open(FeatureRoute(FeaturePage.Reviews)) }
        NavigationRow("设置与隐私") { store.features.open(FeatureRoute(FeaturePage.SettingsHome)) }
        NavigationRow("账号安全与手机验证") { store.features.open(FeatureRoute(FeaturePage.Security)) }
        NavigationRow("资料删除、注销与协助") { store.features.open(FeatureRoute(FeaturePage.DataRequests)) }
        NavigationRow("待核对操作与受限进度") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
        NavigationRow("新增宠物档案") { store.features.open(FeatureRoute(FeaturePage.PetEdit)) }
        NavigationRow("领养申请进度", "查看申请与双方确认") { store.open(MobileDestination.Applications) }
        NavigationRow("通知", "互动、申请与消息提醒") { store.open(MobileDestination.Notifications) }
        NavigationRow("走失互助", "查看走失与拾获信息") { store.open(MobileDestination.Help) }
        OutlinedButton(onClick = store::logout, enabled = !s.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("退出当前账号") }
        SectionTitle("我的宠物")
    }) { row -> PetCard(store, s, row) { store.open(MobileDestination.Pet(row.id)) } }
}

@Composable private fun SignInPrompt(store: MobileStore) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MobileUi.sectionGap)) { Text("登录后继续此任务"); Button(onClick = store::showLogin) { Text("登录") } }
} }
@Composable private fun ScrollContent(content: @Composable ColumnScope.() -> Unit) { TaskColumn(content = content) }
@Composable private fun SectionTitle(text: String) { MndScreenTitle(text) }
@Composable private fun ActionCard(title: String, detail: String, action: () -> Unit) {
    MndActionCard(title, detail, action)
}
@Composable private fun Photos(store: MobileStore, media: List<Long>, account: Long?, description: String) {
    ClientMediaGallery(store,media,account,description)
}
@Composable private fun ErrorPanel(error: String, retry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(MobileUi.sectionGap)) {
    Text(error); OutlinedButton(onClick = retry) { Text("重新载入") }
} }
@Composable private fun <T> Records(page: PageState<T>,title: String,retry: () -> Unit,more: () -> Unit,emptyText: String = "暂时没有可显示的内容",header: @Composable () -> Unit = {},row: @Composable (MobileRecord<T>) -> Unit) { RecordList(page,title,retry,more,emptyText,header,row) }
private fun MobileTab.designIcon(): String = when(this) {
    MobileTab.Community -> "house"; MobileTab.Adoption -> "heart-handshake"; MobileTab.Publish -> "circle-plus"; MobileTab.Messages -> "message-circle"; MobileTab.Profile -> "user-round"
}
private fun title(s: MobileState): String = when (val d = s.destination) {
    MobileDestination.Home -> s.tab.label; is MobileDestination.Pet -> if(d.discussion) "${s.pet?.data?.name ?: "宠物"}的领养交流" else "宠物详情"; is MobileDestination.Entry -> "社区动态"
    is MobileDestination.Application, MobileDestination.Applications -> "领养申请"; is MobileDestination.Chat -> "消息"
    is MobileDestination.Case, MobileDestination.Help -> "走失互助"; MobileDestination.Notifications -> "通知"
}
private fun notificationDestination(body: NotificationBody): MobileDestination? = when (body.targetKind) {
    "entries" -> MobileDestination.Entry(body.targetId); "pets" -> MobileDestination.Pet(body.targetId)
    "adoptions" -> MobileDestination.Application(body.targetId); "cases" -> MobileDestination.Case(body.targetId)
    "conversations" -> MobileDestination.Chat(body.targetId); "groups" -> MobileDestination.Chat(body.targetId, true); else -> null
}
