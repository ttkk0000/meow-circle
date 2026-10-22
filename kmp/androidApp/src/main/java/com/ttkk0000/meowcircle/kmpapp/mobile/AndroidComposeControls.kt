package com.ttkk0000.meowcircle.kmpapp.mobile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.kmpapp.R

internal val originChoices = listOf("original" to "原创／实拍","ai_generated" to "AI 生成","ai_modified" to "AI 修改","unknown" to "来源尚不确定")
internal fun originLabel(v: String) = originChoices.firstOrNull { it.first == v }?.second ?: "未提供来源声明"

@Composable internal fun PublicationDialog(store: MobileStore,s: MobileState) {
    val input = s.publicationConfirmation ?: return
    var origin by remember(input) { mutableStateOf(input.declaration.origin) }; var confirmed by remember(input) { mutableStateOf(false) }
    MobileAlertDialog(onDismissRequest = store::dismissPublication,title = { Text("核对内容与来源") },text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(input.entry.title.ifBlank { "当前发布内容" }); Text(input.entry.text)
            Text("${input.mediaIds.size} 张照片 · ${audienceLabel(input.entry.audience.mode)}")
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(input.mediaIds.size) { index -> MobilePhoto(store,input.mediaIds[index],s.user?.id,"核对发布照片 ${index + 1}",Modifier.size(96.dp)) } }
            originChoices.forEach { (id,label) -> CheckLine(label,origin == id,{ if(it) { origin = id; confirmed = false } },!s.busy) }
            CheckLine("我已核对当前内容、照片、受众和来源声明",confirmed,{ confirmed = it },!s.busy)
            Text("来源改变后会重新生成核对内容，须再次主动确认。已识别的 AI 内容不能声明为原创。",style = MaterialTheme.typography.labelSmall)
        }
    },confirmButton = { TextButton({ store.confirmPublication(origin,confirmed) },enabled = !s.busy && confirmed) { Text(if(origin == input.declaration.origin) "确认发布" else "更新来源后重新核对") } },dismissButton = { TextButton(store::dismissPublication,enabled = !s.busy) { Text("返回编辑") } })
}

@Composable internal fun ComposeAudience(store: MobileStore,s: MobileState) {
    var people by remember(s.composeEntry.audience) { mutableStateOf(s.composeEntry.audience.userIds.joinToString(",")) }
    var pet by remember(s.composeEntry.petId) { mutableStateOf(s.composeEntry.petId.takeIf { it > 0 }?.toString().orEmpty()) }
    Text("发布空间与可见范围",style = MaterialTheme.typography.titleMedium)
    Row { listOf("community" to "社区","circle" to "宠友圈","diary" to "日记").forEach { (id,label) ->
        FilterChip(s.composeEntry.space == id,{ store.editComposeOptions(id,if(id == "community") Audience() else if(id == "circle" && s.composeEntry.audience.mode == "public") Audience("friends") else s.composeEntry.audience,s.composeEntry.petId) },label = { Text(label) },modifier = Modifier.padding(end = 8.dp))
    } }
    Row { listOf("daily_share" to "日常","help" to "互助","activity" to "活动","trade" to "有偿信息讨论").forEach { (id,label) -> TextButton({ store.editComposeOptions(s.composeEntry.space,s.composeEntry.audience,s.composeEntry.petId,id) },enabled = !s.busy) { Text(if(s.composeEntry.category == id) "● $label" else label) } } }
    if(s.composeEntry.space != "community") {
        listOf("private" to "仅自己","friends" to "好友可见","selected" to "指定好友","excluded" to "排除指定好友").forEach { (mode,label) ->
            CheckLine(label,s.composeEntry.audience.mode == mode,{ checked -> if(checked) store.editComposeOptions(s.composeEntry.space,Audience(mode,if(mode in listOf("selected","excluded")) ids(people).orEmpty() else emptyList()),s.composeEntry.petId) },!s.busy)
        }
        if(s.composeEntry.audience.mode in listOf("selected","excluded")) {
            OutlinedTextField(people,{ people = it; store.editComposeAudienceList(it) },Modifier.fillMaxWidth(),label = { Text("好友编号，逗号分隔") },isError = ids(people) == null,enabled = !s.busy)
            Text("更改受众不会自动放宽为公开；选中名单由服务端校验好友关系。")
        }
    }
    if(s.composeEntry.space == "diary") OutlinedTextField(pet,{ pet = it; store.editComposeOptions("diary",s.composeEntry.audience,it.toLongOrNull() ?: 0) },Modifier.fillMaxWidth(),label = { Text("所属宠物编号") },enabled = !s.busy)
    if(s.pendingOperations.isNotEmpty()) FeatureButton("核对原发布或消息结果") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
    FeatureButton("账号资格与手机验证") { store.features.open(FeatureRoute(FeaturePage.Security)) }
}

@Composable internal fun ExpressionPanel(store: MobileStore,s: MobileState,editor: TextFieldValue,onEdit: (TextFieldValue) -> Unit,stickersAllowed: Boolean) {
    var category by remember { mutableStateOf("emoji") }
    Row { listOf("emoji" to "表情","favorite" to "收藏","recent" to "最近","sticker" to "贴纸").forEach { (key,label) -> TextButton({ category = key }) { Text(if(category == key) "● $label" else label) } } }
    val saved = s.expressionState?.data ?: ExpressionBody()
    val emoji = when(category) { "favorite" -> s.emoji.filter { it.id in saved.favorites }; "recent" -> saved.recent.mapNotNull { id -> s.emoji.firstOrNull { it.id == id } }; "sticker" -> emptyList(); else -> s.emoji }
    val stickers = when(category) { "favorite" -> s.stickers.filter { it.id in saved.favorites }; "recent" -> saved.recent.mapNotNull { id -> s.stickers.firstOrNull { it.id == id } }; "sticker" -> s.stickers; else -> emptyList() }
    LazyVerticalGrid(GridCells.Adaptive(if(category == "sticker") 88.dp else 64.dp),Modifier.fillMaxWidth().heightIn(min = 80.dp,max = 260.dp)) {
        items(emoji,key = { it.id }) { item -> Column {
            TextButton({ val start = editor.selection.min; val end = editor.selection.max; val next = editor.text.replaceRange(start,end,item.unicode); val value = TextFieldValue(next,TextRange(start + item.unicode.length)); if(store.editReply(next)) { onEdit(value); store.rememberExpression(item.id) } },enabled = !s.busy) { Text(item.unicode,style = MaterialTheme.typography.titleLarge) }
            TextButton({ store.favoriteExpression(item.id) },enabled = !s.busy) { Text(if(item.id in saved.favorites) "取消收藏" else "收藏",style = MaterialTheme.typography.labelSmall) }
        } }
        items(stickers,key = { it.id }) { item -> Column {
            TextButton({ store.selectSticker(item.id); store.rememberExpression(item.id) },enabled = !s.busy && stickersAllowed) { StickerPhoto(item.id,item.label,Modifier.size(64.dp)) }
            TextButton({ store.favoriteExpression(item.id) },enabled = !s.busy) { Text(if(item.id in saved.favorites) "取消收藏" else "收藏",style = MaterialTheme.typography.labelSmall) }
        } }
        if(emoji.isEmpty() && stickers.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text("此分类暂时没有内容") }
    }
    if(!stickersAllowed) Text("消息请求被接受前仅可发送一条文字，暂不开放贴纸和照片。")
    TextButton({
        val range = editor.selection
        val start = if(range.collapsed) previousGrapheme(editor.text,range.start) else range.min
        val end = range.max
        val next = editor.text.removeRange(start,end)
        if(store.editReply(next)) onEdit(TextFieldValue(next,TextRange(start)))
    },enabled = !s.busy && (editor.selection.max > 0)) { Text("删除上一颗表情／选区") }
}
// Android ICU 按字素分段，完整删除旗帜、肤色和 ZWJ，不按 UTF-16 单元截断。
internal fun previousGrapheme(text: String,offset: Int): Int {
    val boundary = android.icu.text.BreakIterator.getCharacterInstance(java.util.Locale.ROOT)
    boundary.setText(text); return boundary.preceding(offset).takeIf { it != android.icu.text.BreakIterator.DONE } ?: 0
}
private object StickerBitmaps {
    private var atlas: android.graphics.Bitmap? = null
    private val pieces = mutableMapOf<Int,androidx.compose.ui.graphics.ImageBitmap>()
    @Synchronized fun get(resources: android.content.res.Resources,index: Int): androidx.compose.ui.graphics.ImageBitmap? {
        if(index !in 0..23) return null
        pieces[index]?.let { return it }
        val source = atlas ?: BitmapFactory.decodeResource(resources,R.drawable.mnd_cat_stickers)?.also { atlas = it } ?: return null
        return android.graphics.Bitmap.createBitmap(source,(index % 6) * (source.width / 6),(index / 6) * (source.height / 4),source.width / 6,source.height / 4).asImageBitmap().also { pieces[index] = it }
    }
}
@Composable internal fun StickerPhoto(id: String,description: String,modifier: Modifier = Modifier.size(128.dp)) {
    val resources = LocalContext.current.resources
    val index = id.removePrefix("sticker-").toIntOrNull()?.minus(1)
    val bitmap = remember(index,resources) { index?.let { StickerBitmaps.get(resources,it) } }
    if(bitmap == null) Text("贴纸暂不可用") else Image(bitmap,description,modifier)
}
internal fun openNotification(store: MobileStore,body: NotificationBody) {
    if(body.targetId <= 0) { store.pickerFailed("通知对象已失效，请重新读取通知"); return }
    val feature = when(body.targetKind) {
        "appointments" -> FeatureRoute(FeaturePage.Appointment,body.targetId)
        "invitations" -> FeatureRoute(FeaturePage.Invitations)
        "claims" -> FeatureRoute(FeaturePage.Claim,body.targetId)
        "offers" -> FeatureRoute(FeaturePage.Offer,body.targetId)
        "reviews" -> FeatureRoute(FeaturePage.Review,body.targetId)
        "guardians" -> FeatureRoute(FeaturePage.Security)
        "friendships" -> FeatureRoute(FeaturePage.Friends)
        else -> null
    }
    if(feature != null) store.features.open(feature) else when(body.targetKind) {
        "entries" -> store.open(MobileDestination.Entry(body.targetId)); "pets" -> store.open(MobileDestination.Pet(body.targetId))
        "adoptions" -> store.open(MobileDestination.Application(body.targetId)); "cases" -> store.open(MobileDestination.Case(body.targetId))
        "conversations" -> store.open(MobileDestination.Chat(body.targetId)); "groups" -> store.open(MobileDestination.Chat(body.targetId,true))
        else -> store.pickerFailed("此通知暂无可打开的有效对象，请在事项列表核对")
    }
}

@Composable internal fun ComposeEmojiPanel(store: MobileStore,s: MobileState,editor: TextFieldValue,onEdit: (TextFieldValue) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(52.dp),Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
        items(s.emoji,key = { it.id }) { item -> TextButton({
            val start = editor.selection.min; val next = editor.text.replaceRange(start,editor.selection.max,item.unicode)
            onEdit(TextFieldValue(next,TextRange(start + item.unicode.length))); store.rememberExpression(item.id)
        },enabled = !s.busy) { Text(item.unicode,style = MaterialTheme.typography.titleLarge) } }
    }
    TextButton({ val start = if(editor.selection.collapsed) previousGrapheme(editor.text,editor.selection.start) else editor.selection.min; onEdit(TextFieldValue(editor.text.removeRange(start,editor.selection.max),TextRange(start))) },enabled = !s.busy && editor.selection.max > 0) { Text("删除上一颗表情／选区") }
}

/** 输入内容属于共享内存／私有草稿；系统 Bundle 只记光标，不复制正文。 */
@Composable internal fun rememberPrivateEditor(text: String,key: String): MutableState<TextFieldValue> {
    var start by androidx.compose.runtime.saveable.rememberSaveable(key) { mutableIntStateOf(text.length) }
    var end by androidx.compose.runtime.saveable.rememberSaveable(key) { mutableIntStateOf(text.length) }
    val editor = remember(key) { mutableStateOf(TextFieldValue(text,TextRange(start.coerceIn(0,text.length),end.coerceIn(0,text.length)))) }
    SideEffect { start = editor.value.selection.start; end = editor.value.selection.end }
    return editor
}
