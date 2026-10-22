package com.ttkk0000.meowcircle.iosui

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
import com.ttkk0000.meowcircle.client.*

// iPad uses its actual Scene width after UIKit resize; it never reads Android configuration or device orientation.
internal data class IpadWindowPolicy(val rail: Boolean, val contextPane: Boolean)

@Composable
fun IosMobileApp(store: MobileStore) {
    val state by store.state.collectAsState()
    MobileTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val policy = IpadWindowPolicy(rail = maxWidth >= 680.dp, contextPane = maxWidth >= 1100.dp)
            MobileShell(store, state, policy.rail, policy.contextPane)
        }
    }
}

@Composable
private fun MobileShell(store: MobileStore, state: MobileState, rail: Boolean, contextPane: Boolean) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (!rail) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            MobileTab.entries.forEach { tab -> NavigationBarItem(selected = state.tab == tab,
                onClick = { store.select(tab) }, icon = { Icon(tab.icon(), tab.label) }, label = { Text(tab.label) }) }
        } },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (rail) NavigationRail(containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxHeight()) {
                Text("M&D", Modifier.padding(vertical = 20.dp), fontWeight = FontWeight.ExtraBold)
                MobileTab.entries.forEach { tab -> NavigationRailItem(selected = state.tab == tab,
                    onClick = { store.select(tab) }, icon = { Icon(tab.icon(), tab.label) }, label = { Text(tab.label) }) }
            }
            if (contextPane) ContextPane(store, state, Modifier.width(240.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.destination != MobileDestination.Home) IconButton(onClick = { store.back() }, enabled = !state.busy) { Icon(Icons.Outlined.ArrowBack, "返回") }
                    Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text(if (state.destination == MobileDestination.Home && state.tab == MobileTab.Community) "M&D" else title(state), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (state.destination == MobileDestination.Home && state.tab == MobileTab.Community) Text("meow & doggie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.user == null) TextButton(onClick = store::showLogin) { Text("登录") }
                    IconButton(onClick = { if (state.destination == MobileDestination.Home) store.refreshHome() else store.reloadDetail() }, enabled = !state.busy) { Icon(Icons.Outlined.Refresh, "重新载入") }
                }
                if (state.restoring || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.notice?.let { notice ->
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(notice, Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = store::clearNotice) { Icon(Icons.Outlined.Close, "关闭提示") }
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 820.dp).fillMaxSize()) {
                        if (state.destination == MobileDestination.Home) HomeContent(store, state) else DetailContent(store, state)
                    }
                }
            }
        }
    }
    if (state.auth.visible) LoginDialog(store, state)
    state.confirmation?.let { command ->
        AlertDialog(onDismissRequest = store::dismissConfirmation,
            title = { Text(command.label) },
            text = { Text("${command.label}？\n当前申请：${adoptionStateLabel(command.record.state)}\n${command.note}\n提交后由服务端校验最新进度。交接需要双方各自确认。") },
            confirmButton = { TextButton(onClick = store::confirmAdoptionAction, enabled = !state.busy && !state.conflict) { Text("确认提交") } },
            dismissButton = { TextButton(onClick = store::dismissConfirmation, enabled = !state.busy) { Text("返回核对") } })
    }
}

@Composable
private fun ContextPane(store: MobileStore, state: MobileState, modifier: Modifier) {
    Column(modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("和猫咪一起的日常", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("交流、领养与同城互助", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { store.open(MobileDestination.Help) }, modifier = Modifier.fillMaxWidth()) { Text("走失互助") }
        OutlinedButton(onClick = { store.open(MobileDestination.Applications) }, modifier = Modifier.fillMaxWidth()) { Text("领养申请进度") }
        OutlinedButton(onClick = { store.open(MobileDestination.Notifications) }, modifier = Modifier.fillMaxWidth()) { Text("通知") }
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
            Text("发生时间：${record.data.occurredAt}")
            Text(record.data.description)
            Text("公开位置为大致区域。认领与交接需要核对材料并由双方确认。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (record.data.closureReason.isNotBlank()) Text("结束说明：${record.data.closureReason}")
        } }
        MobileDestination.Help -> Records(s.cases, "走失互助", { store.reloadDetail() }, { store.reloadDetail(true) }) { row ->
            ActionCard(row.data.title, "${row.data.city} · ${row.data.area}\n${if (row.data.type == "lost") "走失" else "拾获"}") { store.open(MobileDestination.Case(row.id)) }
        }
        MobileDestination.Applications -> if (s.user == null) SignInPrompt(store) else Records(s.applications, "领养申请进度", { store.reloadDetail() }, { store.reloadDetail(true) }) { row ->
            ActionCard(adoptionStateLabel(row.state), row.data.message) { store.open(MobileDestination.Application(row.id)) }
        }
        MobileDestination.Notifications -> if (s.user == null) SignInPrompt(store) else Records(s.notifications, "通知", { store.reloadDetail() }, { store.reloadDetail(true) }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.data.text, fontWeight = if (row.data.read) FontWeight.Normal else FontWeight.Bold)
                    notificationDestination(row.data)?.let { target -> TextButton(onClick = { store.open(target) }) { Text("查看详情") } }
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
        item { SectionTitle(entry.data.title); Text(entry.data.text) }
        item {
            val metrics = s.metrics[entry.id]
            OutlinedButton(onClick = store::toggleLike, enabled = metrics != null && !s.busy) { Text(if (metrics?.viewerLiked == true) "已赞 ${metrics.likes}" else "赞 ${metrics?.likes ?: "—"}") }
        }
        item { SectionTitle("评论") }
        items(s.comments.items, key = { it.id }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Text(row.data.text, Modifier.fillMaxWidth().padding(16.dp)) }
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
        item { Text("消息请求 ${s.inbox?.messageRequests?.toString() ?: "—"}", fontWeight = FontWeight.Bold) }
        items(s.conversations.items, key = { "direct-${it.id}" }) { row ->
            ActionCard("私信会话", "${if (row.state == "pending") "消息请求" else "私信"} · ${s.inbox?.let { it.direct["${row.id}"] ?: 0 }?.toString() ?: "—"} 条未读") { store.open(MobileDestination.Chat(row.id)) }
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
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (s.conversation?.state == "pending") {
            Text("对方接受前，发起人仅可发送一条文字。", Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.conversation.data.recipientId == s.user?.id) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { store.answerMessageRequest("accept") }, enabled = !s.busy) { Text("接受消息请求") }
                OutlinedButton(onClick = { store.answerMessageRequest("decline") }, enabled = !s.busy) { Text("拒绝") }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(s.messages.items, key = { it.id }) { row ->
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val bubbleWidth = minOf(560.dp, maxWidth * 0.78f)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (row.ownerId == s.user?.id) Arrangement.End else Arrangement.Start) {
                        Surface(shape = RoundedCornerShape(16.dp), color = if (row.ownerId == s.user?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.widthIn(max = bubbleWidth)) {
                            Text(row.data.text.ifBlank { "贴纸（暂无法预览）" }, Modifier.padding(14.dp))
                        }
                    }
                }
            }
            item { PageFooter(s.messages, { store.loadMessages() }, { store.loadMessages(true) }) }
        }
        if (s.messages.items.isNotEmpty()) TextButton(onClick = store::markChatRead, enabled = !s.busy) { Text("将已载入消息标为已读") }
        val conversation = s.conversation
        val canSend = chat.group || conversation?.state == "accepted" || conversation?.let { it.state == "pending" && it.data.requesterId == s.user?.id && s.messages.loaded && s.messages.items.isEmpty() } == true
        if (canSend) ReplyEditor(store, s, "消息") else Text("当前会话暂不可发送", Modifier.padding(16.dp))
    }
}

@Composable
private fun ReplyEditor(store: MobileStore, s: MobileState, label: String) {
    var editor by remember(s.destination) { mutableStateOf(TextFieldValue(s.reply)) }
    var emojiOpen by remember(s.destination) { mutableStateOf(false) }
    LaunchedEffect(s.reply) { if (editor.text != s.reply) editor = TextFieldValue(s.reply, TextRange(s.reply.length)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(editor, { if (store.editReply(it.text)) editor = it }, Modifier.fillMaxWidth(), label = { Text(label) }, enabled = !s.busy, maxLines = 5)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { emojiOpen = !emojiOpen }) { Text(if (emojiOpen) "收起表情" else "表情") }
            Button(onClick = store::sendReply, enabled = !s.busy && s.reply.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("发送") }
        }
        if (emojiOpen) LazyVerticalGrid(GridCells.Adaptive(48.dp), Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
            items(s.emoji, key = { it.id }) { emoji ->
                TextButton(onClick = {
                    val start = editor.selection.min; val end = editor.selection.max
                    val next = editor.text.replaceRange(start, end, emoji.unicode)
                    if (store.editReply(next)) editor = TextFieldValue(next, TextRange(start + emoji.unicode.length))
                }) { Text(emoji.unicode) }
            }
        }
    }
}

@Composable
private fun PublishScreen(store: MobileStore, s: MobileState) {
    if (s.user == null) { SignInPrompt(store); return }
    ScrollContent {
        SectionTitle("分享和猫咪的日常")
        OutlinedTextField(s.composeTitle, { store.editCompose(it, s.composeText) }, Modifier.fillMaxWidth(), label = { Text("标题（选填）") }, enabled = !s.busy)
        OutlinedTextField(s.composeText, { store.editCompose(s.composeTitle, it) }, Modifier.fillMaxWidth(), label = { Text("今天想分享什么？") }, minLines = 8, enabled = !s.busy)
        Text("发布到公开社区", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        TextButton(onClick = { store.restoreComposeDraft(replaceInput = true) }, enabled = !s.busy && !s.draftLoading) { Text("用云端草稿替换当前输入") }
        Button(onClick = store::publish, enabled = !s.busy && (s.composeText.isNotBlank() || s.photos.isNotEmpty()) && s.photos.all { it.mediaId != null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("确认发布") }
    }
}

@Composable
private fun ProfileScreen(store: MobileStore, s: MobileState) {
    Records(s.myPets, s.user?.nickname.orEmpty(), { store.refreshHome() }, { store.refreshHome(true) }, header = {
        Text(s.user?.bio.orEmpty())
        ActionCard("领养申请进度", "查看申请与双方确认") { store.open(MobileDestination.Applications) }
        ActionCard("通知", "互动、申请与消息提醒") { store.open(MobileDestination.Notifications) }
        ActionCard("走失互助", "查看走失与拾获信息") { store.open(MobileDestination.Help) }
        OutlinedButton(onClick = store::logout, enabled = !s.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("退出当前账号") }
        SectionTitle("我的宠物")
    }) { row -> PetCard(store, s, row) { store.open(MobileDestination.Pet(row.id)) } }
}

@Composable
private fun LoginDialog(store: MobileStore, s: MobileState) {
    val form = s.auth
    Dialog(onDismissRequest = store::cancelLogin, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(20.dp).widthIn(max = 520.dp).fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("欢迎回到 M&D", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = store::cancelLogin, enabled = !s.busy) { Icon(Icons.Outlined.Close, "关闭登录") }
                }
                Text("暂可使用已有账号的账号名或手机号与密码登录。游客也可以浏览公开内容。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.capabilityError?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = store::showLogin) { Text("重试") } }
                OutlinedTextField(form.username, { store.editAuth(form.copy(username = it)) }, Modifier.fillMaxWidth(), label = { Text("账号名 / 手机号") }, singleLine = true, enabled = !s.busy)
                OutlinedTextField(form.password, { store.editAuth(form.copy(password = it)) }, Modifier.fillMaxWidth(), label = { Text("密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !s.busy)
                form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = store::login, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !s.busy && s.capabilities.password && form.username.isNotBlank() && form.password.isNotBlank()) { Text("登录并继续") }
            }
        }
    }
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
