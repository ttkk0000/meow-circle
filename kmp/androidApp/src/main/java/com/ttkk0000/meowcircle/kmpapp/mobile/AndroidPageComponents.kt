package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.svg.SvgDecoder
import com.ttkk0000.meowcircle.client.*

private object DesignImageLoader {
    @Volatile private var value: ImageLoader? = null
    @Synchronized fun get(context: Context): ImageLoader = value ?: ImageLoader.Builder(context.applicationContext)
        .components { add(SvgDecoder.Factory()) }.build().also { value = it }
}
/** 来自当前 Figma 原始 SVG；所有图标共用应用级解码器与缓存。 */
@Composable internal fun DesignIcon(name: String,description: String?,modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember(context.applicationContext) { DesignImageLoader.get(context) }
    AsyncImage("file:///android_asset/figma/$name.svg",description,imageLoader = loader,modifier = modifier.size(24.dp))
}

private data class TaskAction(val label: String,val enabled: Boolean,val run: () -> Unit)
private val LocalPrimaryAction = staticCompositionLocalOf<((Any,TaskAction?) -> Unit)?> { null }

/** 表单正文独立滚动，主操作保持可达；不会创建第二个业务 Store。 */
@Composable internal fun TaskColumn(spacing: Int = 16,content: @Composable ColumnScope.() -> Unit) {
    val actions = remember { mutableStateListOf<Pair<Any,TaskAction>>() }
    val register = remember { { key: Any,value: TaskAction? ->
        val index = actions.indexOfFirst { it.first == key }
        if(value == null) { if(index >= 0) actions.removeAt(index) }
        else if(index < 0) actions.add(key to value)
        else if(actions[index].second != value) actions[index] = key to value
    } }
    Column(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPrimaryAction provides register) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement = Arrangement.spacedBy(spacing.dp),content = content)
        }
        if(actions.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = 168.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp,vertical = 12.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { (_,primary) -> Button(primary.run,Modifier.fillMaxWidth().heightIn(min = 48.dp),enabled = primary.enabled) { Text(primary.label) } }
            }
        }
    }
}
@Composable internal fun TaskPrimary(label: String,enabled: Boolean = true,onClick: () -> Unit) {
    val register = LocalPrimaryAction.current
    val latest by rememberUpdatedState(onClick)
    val stable = remember { { latest() } }
    val action = remember(label,enabled) { TaskAction(label,enabled,stable) }
    val key = remember { Any() }
    if(register == null) Button(stable,Modifier.fillMaxWidth().heightIn(min = 48.dp),enabled = enabled) { Text(label) }
    else {
        SideEffect { register(key,action) }
        DisposableEffect(register,key) { onDispose { register(key,null) } }
    }
}
@Composable internal fun TaskSection(title: String,content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title,style = MaterialTheme.typography.titleMedium,fontWeight = FontWeight.Bold); content() }
    }
}
@Composable internal fun TaskNotice(title: String,text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant,shape = MaterialTheme.shapes.medium,modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title,fontWeight = FontWeight.Bold); Text(text,style = MaterialTheme.typography.bodySmall) }
    }
}
@Composable internal fun NavigationRow(title: String,detail: String = "",onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(vertical = 12.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title,style = MaterialTheme.typography.bodyLarge); if(detail.isNotBlank()) Text(detail,color = MaterialTheme.colorScheme.onSurfaceVariant,style = MaterialTheme.typography.bodySmall) }
        DesignIcon("chevron-right",null)
    }
}
@Composable internal fun UnreadBadge(count: Int?) {
    if(count != null && count > 0) Badge { Text(if(count > 99) "99+" else count.toString()) }
}
@Composable internal fun MessageRow(title: String,context: String,count: Int?,group: Boolean = false,store: MobileStore? = null,avatar: Long? = null,onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 84.dp).padding(vertical = 12.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if(store != null && avatar != null) MobilePhoto(store,avatar,store.state.value.user?.id,"宠友头像",Modifier.size(48.dp).clip(CircleShape))
        else Surface(shape = MaterialTheme.shapes.medium,color = MaterialTheme.colorScheme.primaryContainer) { Box(Modifier.size(48.dp),contentAlignment = Alignment.Center) { DesignIcon(if(group) "bell" else "message-circle",null) } }
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title,fontWeight = FontWeight.Bold); Text(context,style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant) }
        UnreadBadge(count)
    }
}
@Composable internal fun FeedTile(store: MobileStore,s: MobileState,record: MobileRecord<EntryBody>) {
    val metrics = s.metrics[record.id]
    LaunchedEffect(record.ownerId,s.user?.id) { store.ensureMember(record.ownerId) }
    val author = s.members[record.ownerId]
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().clickable { store.open(MobileDestination.Entry(record.id)) }) { MobilePhoto(store,record.mediaIds.firstOrNull(),s.user?.id,record.data.title.ifBlank { "宠物日常照片" },Modifier.fillMaxWidth().aspectRatio(173f / 190f)) }
            Column(Modifier.fillMaxWidth().clickable { store.open(MobileDestination.Entry(record.id)) }.padding(8.dp),verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.data.title.ifBlank { record.data.text },minLines = 2,maxLines = 2,overflow = TextOverflow.Ellipsis,fontWeight = FontWeight.Bold,style = MaterialTheme.typography.titleMedium)
                Text(author?.nickname?.ifBlank { author.username } ?: if(record.ownerId == s.user?.id) s.user?.nickname.orEmpty() else "宠友 #${record.ownerId}",maxLines = 1,style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ store.toggleReaction(record.id) },enabled = metrics != null && !s.busy,contentPadding = PaddingValues(8.dp),modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { DesignIcon("heart",if(metrics?.viewerLiked == true) "取消喜欢" else "喜欢"); Spacer(Modifier.width(8.dp)); Text(metrics?.likes?.toString() ?: "—") }
                TextButton({ store.open(MobileDestination.Entry(record.id)) },contentPadding = PaddingValues(8.dp),modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { DesignIcon("message-circle","评论"); Spacer(Modifier.width(8.dp)); Text(metrics?.comments?.toString() ?: "—") }
            }
        }
    }
}
@Composable internal fun <T> RecordList(page: PageState<T>,title: String,retry: () -> Unit,more: () -> Unit,header: @Composable () -> Unit = {},row: @Composable (MobileRecord<T>) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding = PaddingValues(16.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(title,style = MaterialTheme.typography.titleLarge,fontWeight = FontWeight.Bold) };item { header() }
        items(page.items,key = { it.id }) { row(it) }
        item { PageFooter(page,retry,more) }
    }
}

internal fun avatarMediaId(url: String): Long? = Regex("/media/(\\d+)/content").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
