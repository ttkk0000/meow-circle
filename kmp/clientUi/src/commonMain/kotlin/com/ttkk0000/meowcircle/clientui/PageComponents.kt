package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

private data class TaskAction(val label: String,val enabled: Boolean,val run: () -> Unit,val secondary: Boolean = false)
private val LocalPrimaryAction = staticCompositionLocalOf<((Any,TaskAction?) -> Unit)?> { null }
val LocalTaskNavigationEnabled = staticCompositionLocalOf { true }

/** 表单正文独立滚动，主操作保持可达；不会创建第二个业务 Store。 */
@Composable fun TaskColumn(spacing: Int = 16,content: @Composable ColumnScope.() -> Unit) {
    TaskActionHost { modifier -> MndForm(modifier,gap = spacing.dp,content = content) }
}

/** Also hosts persistent recovery actions for independently scrolling lists. */
@Composable fun TaskActionHost(content: @Composable (Modifier) -> Unit) {
    val actions = remember { mutableStateListOf<Pair<Any,TaskAction>>() }
    val register = remember { { key: Any,value: TaskAction? ->
        val index = actions.indexOfFirst { it.first == key }
        if(value == null) { if(index >= 0) actions.removeAt(index) }
        else if(index < 0) actions.add(key to value)
        else if(actions[index].second != value) actions[index] = key to value
    } }
    Column(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPrimaryAction provides register) {
            content(Modifier.weight(1f).fillMaxWidth())
        }
        if(actions.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = 168.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp,vertical = 12.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { (_,action) ->
                    if(action.secondary) MndSecondaryButton(action.label,action.enabled,onClick = action.run)
                    else MndPrimaryButton(action.label,action.enabled,onClick = action.run)
                }
            }
        }
    }
}
@Composable fun TaskPrimary(label: String,enabled: Boolean = true,onClick: () -> Unit) {
    PersistentTaskAction(label,enabled,false,onClick)
}
@Composable fun TaskSecondary(label: String,enabled: Boolean = true,onClick: () -> Unit) {
    PersistentTaskAction(label,enabled,true,onClick)
}
@Composable private fun PersistentTaskAction(label: String,enabled: Boolean,secondary: Boolean,onClick: () -> Unit) {
    val register = LocalPrimaryAction.current
    val latest by rememberUpdatedState(onClick)
    val stable = remember { { latest() } }
    val action = remember(label,enabled,secondary) { TaskAction(label,enabled,stable,secondary) }
    val key = remember { Any() }
    if(register == null) {
        if(secondary) MndSecondaryButton(label,enabled,onClick = stable) else MndPrimaryButton(label,enabled,onClick = stable)
    }
    else {
        SideEffect { register(key,action) }
        DisposableEffect(register,key) { onDispose { register(key,null) } }
    }
}
@Composable fun TaskSection(title: String,content: @Composable ColumnScope.() -> Unit) {
    MndSection(title,content = content)
}
@Composable fun TaskNotice(title: String,text: String,warning: Boolean = false,success: Boolean = false) {
    Surface(color = if(warning) ClientWarningSurface else if(success) ClientSuccessSurface else MaterialTheme.colorScheme.surfaceVariant,shape = MaterialTheme.shapes.medium,modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title,fontWeight = FontWeight.Bold); Text(text,style = MaterialTheme.typography.bodySmall) }
    }
}
@Composable fun MemberSection(store: MobileStore,id: Long,compact: Boolean = false,content: @Composable ColumnScope.() -> Unit) {
    val state by store.state.collectAsState()
    LaunchedEffect(id,state.user?.id) { store.ensureMember(id) }
    val member = state.members[id]
    val name = member?.nickname?.ifBlank { member.username } ?: "宠友资料待核对"
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if(compact) 12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(compact) 8.dp else 12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                avatarMediaId(member?.avatarUrl.orEmpty())?.let { media -> MobilePhoto(store,media,state.user?.id,"$name 的头像",Modifier.size(if(compact) 32.dp else 48.dp).clip(CircleShape)) }
                Text(name,style=if(compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
@Composable fun NavigationRow(title: String,detail: String = "",onClick: () -> Unit) {
    NavigationRow(title,detail,compact = false,onClick = onClick)
}
@Composable fun NavigationRow(title: String,detail: String = "",compact: Boolean,onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = LocalTaskNavigationEnabled.current,onClick = onClick).heightIn(min = 64.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title,style = MaterialTheme.typography.bodyLarge); if(detail.isNotBlank()) Text(detail,color = MaterialTheme.colorScheme.onSurfaceVariant,style = MaterialTheme.typography.bodySmall) }
        DesignIcon("chevron-right",null)
    }
}
@Composable fun UnreadBadge(count: Int?) {
    if(count != null && count > 0) Badge { Text(if(count > 99) "99+" else count.toString()) }
}
@Composable fun MessageRow(title: String,context: String,count: Int?,group: Boolean = false,store: MobileStore? = null,avatar: Long? = null,onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 84.dp).padding(vertical = 12.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if(store != null && avatar != null) MobilePhoto(store,avatar,store.state.value.user?.id,"宠友头像",Modifier.size(48.dp).clip(CircleShape))
        else Surface(shape = MaterialTheme.shapes.medium,color = MaterialTheme.colorScheme.primaryContainer) { Box(Modifier.size(48.dp),contentAlignment = Alignment.Center) { DesignIcon(if(group) "bell" else "message-circle",null) } }
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title,fontWeight = FontWeight.Bold); Text(context,style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant) }
        UnreadBadge(count)
    }
}
@Composable fun FeedTile(store: MobileStore,s: MobileState,record: MobileRecord<EntryBody>) {
    val metrics = s.metrics[record.id]
    LaunchedEffect(record.ownerId,s.user?.id) { store.ensureMember(record.ownerId) }
    val author = s.members[record.ownerId]
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().clickable { store.open(MobileDestination.Entry(record.id)) }) { MobilePhoto(store,record.mediaIds.firstOrNull(),s.user?.id,record.data.title.ifBlank { "宠物日常照片" },Modifier.fillMaxWidth().aspectRatio(173f / 190f)) }
            Column(Modifier.fillMaxWidth().clickable { store.open(MobileDestination.Entry(record.id)) }.padding(8.dp),verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.data.title.ifBlank { record.data.text },minLines = 2,maxLines = 2,overflow = TextOverflow.Ellipsis,fontWeight = FontWeight.Bold,style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().clickable { store.features.open(FeatureRoute(FeaturePage.Member,record.ownerId)) },verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    avatarMediaId(author?.avatarUrl.orEmpty())?.let { id -> MobilePhoto(store,id,s.user?.id,"发布人头像",Modifier.size(24.dp).clip(CircleShape)) }
                    Text(author?.nickname?.ifBlank { author.username } ?: if(record.ownerId == s.user?.id) s.user?.nickname.orEmpty() else "宠友 #${record.ownerId}",maxLines = 1,style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ store.toggleReaction(record.id) },enabled = metrics != null && !s.busy,contentPadding = PaddingValues(8.dp),modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { DesignIcon("heart",if(metrics?.viewerLiked == true) "取消喜欢" else "喜欢"); Spacer(Modifier.width(8.dp)); Text(metrics?.likes?.toString() ?: "—") }
                TextButton({ store.open(MobileDestination.Entry(record.id)) },contentPadding = PaddingValues(8.dp),modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { DesignIcon("message-circle","评论"); Spacer(Modifier.width(8.dp)); Text(metrics?.comments?.toString() ?: "—") }
            }
        }
    }
}
@Composable fun <T> RecordList(page: PageState<T>,title: String,retry: () -> Unit,more: () -> Unit,emptyText: String = "暂时没有可显示的内容",header: @Composable () -> Unit = {},
    emptyActions: @Composable ColumnScope.() -> Unit = {}, failureActions: @Composable ColumnScope.() -> Unit = {}, row: @Composable (MobileRecord<T>) -> Unit) {
    Column(Modifier.fillMaxSize()) {
    PageRefresh(page)
    LazyColumn(Modifier.weight(1f),contentPadding = PaddingValues(MobileUi.pagePadding),verticalArrangement = Arrangement.spacedBy(MobileUi.sectionGap)) {
        item { Text(title,style = MaterialTheme.typography.titleLarge,fontWeight = FontWeight.Bold) };item { header() }
        items(page.items,key = { it.id }) { row(it) }
        item { PageFooter(page,retry,more,emptyText,refreshVisible = true,emptyActions = emptyActions,failureActions = failureActions) }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun <T> ChoiceChipBar(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    scrollable: Boolean = false,
    enabled: Boolean = true,
    badge: (@Composable (T) -> Unit)? = null,
) {
    val modifier = if (scrollable) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth()
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                enabled = enabled,
                label = {
                    if (badge == null) Text(label)
                    else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(label); badge(value) }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun <T> SegmentedTextBar(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    scrollable: Boolean = false,
    enabled: Boolean = true,
) {
    val modifier = if (scrollable) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()) else Modifier
    FlowRow(modifier) {
        options.forEach { (value, label) ->
            TextButton(onClick = { onSelect(value) }, enabled = enabled) {
                Text(if (selected == value) "● $label" else label)
            }
        }
    }
}

@Composable fun PhotoQueueList(
    store: MobileStore,
    photos: List<PhotoAttachment>,
    ownerId: Long?,
    busy: Boolean,
    showPreview: Boolean = true,
    compactPreview: Boolean = false,
    onRetry: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    localPreview: suspend (String) -> SelectedPhoto? = store::localPhotoPreview,
    onSaveOrder: (() -> Unit)? = null,
) {
    FeatureButton("照片选择说明") { store.features.open(FeatureRoute(FeaturePage.PhotoHelp)) }
    photos.forEachIndexed { index, photo ->
        key(photo.key) {
            var dragY by remember { mutableFloatStateOf(0f) }
            var rowHeight by remember { mutableIntStateOf(1) }
            val move by rememberUpdatedState(onMove)
            Column(Modifier.onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }) {
                MndSection("第 ${index + 1} 张${if (index == 0) " · 封面" else ""} · ${photo.name}") {
                    if (showPreview) {
                        photo.mediaId?.let { mediaId ->
                            MobilePhoto(
                                store,
                                mediaId,
                                ownerId,
                                photo.name,
                                if (compactPreview) Modifier.size(112.dp) else Modifier.fillMaxWidth().heightIn(max = 180.dp),
                            )
                        }
                        if (photo.mediaId == null) LocalPhotoPreview(photo.key, ownerId, photo.name,
                            if (compactPreview) Modifier.size(112.dp) else Modifier.fillMaxWidth().height(172.dp), localPreview)
                    }
                    if (photo.uploading) {
                        val percent = photo.uploadPercent
                        Text(if (percent == null) "等待上传进度…" else if (percent == 100) "已传输 100% · 等待服务器确认" else "上传中 $percent%")
                        if (percent == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                    } else if (photo.mediaId != null) Text("已完成 · 已加入本次草稿", style = MaterialTheme.typography.bodySmall)
                    Text("长按拖动调整顺序，第一张作为封面", Modifier.fillMaxWidth().heightIn(min = 48.dp).pointerInput(photo.key, busy) {
                        if (!busy) detectDragGesturesAfterLongPress(
                            onDragStart = { dragY = 0f }, onDragEnd = { dragY = 0f }, onDragCancel = { dragY = 0f },
                            onDrag = { change, distance ->
                                change.consume(); dragY += distance.y
                                if (kotlin.math.abs(dragY) >= rowHeight / 2f) { move(photo.key, if (dragY > 0) 1 else -1); dragY = 0f }
                            })
                    }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    photo.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        MndSecondaryButton("重试此照片", !busy) { onRetry(photo.key) }
                    }
                    Row {
                        TextButton({ onMove(photo.key, -1) }, enabled = !busy && index > 0) { Text("前移") }
                        TextButton({ onMove(photo.key, 1) }, enabled = !busy && index < photos.lastIndex) { Text("后移") }
                        TextButton({ onRemove(photo.key) }, enabled = !busy) { Text("移除") }
                    }
                }
            }
        }
    }
    if (photos.isNotEmpty() && onSaveOrder != null) MndSecondaryButton("保存顺序到本机草稿", !busy, onClick = onSaveOrder)
    val failed = photos.filter { it.mediaId == null && it.error != null && !it.uploading }
    if (failed.isNotEmpty()) {
        Text("已完成 ${photos.count { it.mediaId != null }} 张 · 未完成 ${failed.size} 张")
        MndSecondaryButton("重试失败照片", !busy) { failed.forEach { onRetry(it.key) } }
        MndSecondaryButton("移除失败照片", !busy) { failed.forEach { onRemove(it.key) } }
        Text("已完成的照片与原上传编号会保留；未完成照片不会冒充上传成功。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun OtpVerificationFields(
    code: String,
    onCodeChange: (String) -> Unit,
    otpWait: Int,
    sendLabel: String,
    codeLabel: String = "验证码",
    waitSuffix: String = "秒后可重发",
    enabled: Boolean = true,
    canSend: Boolean = true,
    sendFirst: Boolean = false,
    onSend: () -> Unit,
) {
    val sendAction: @Composable () -> Unit = {
        MndSecondaryButton(
            label = if (otpWait > 0) "$otpWait $waitSuffix" else sendLabel,
            enabled = enabled && canSend && otpWait == 0,
            onClick = onSend,
        )
    }
    val codeField: @Composable () -> Unit = {
        MndOutlinedField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(codeLabel) },
            singleLine = true,
            enabled = enabled,
        )
    }
    if (sendFirst) {
        sendAction()
        codeField()
    } else {
        codeField()
        sendAction()
    }
}

fun avatarMediaId(url: String): Long? = Regex("/media/(\\d+)/content").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
