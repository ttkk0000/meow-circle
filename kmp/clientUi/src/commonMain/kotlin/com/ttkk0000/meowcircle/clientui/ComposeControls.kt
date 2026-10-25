package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

val originChoices = listOf("original" to "原创／实拍","ai_generated" to "AI 生成","ai_modified" to "AI 修改","unknown" to "来源尚不确定")
fun originLabel(v: String) = originChoices.firstOrNull { it.first == v }?.second ?: "未提供来源声明"

@Composable fun ComposeAudience(store: MobileStore,s: MobileState) {
    val feature by store.features.state.collectAsState()
    Text("发布空间与可见范围",style = MaterialTheme.typography.titleMedium)
    ChoiceChipBar(
        options = listOf("community" to "社区","circle" to "宠友圈","diary" to "日记"),
        selected = s.composeEntry.space,
        onSelect = { id -> store.editComposeOptions(id,if(id == "community") Audience() else if(id == "circle" && s.composeEntry.audience.mode == "public") Audience("friends") else s.composeEntry.audience,s.composeEntry.petId) },
        enabled = !s.busy,
    )
    SegmentedTextBar(
        options = listOf("daily_share" to "日常","help" to "互助","activity" to "活动","trade" to "有偿信息讨论"),
        selected = s.composeEntry.category,
        onSelect = { id -> store.editComposeOptions(s.composeEntry.space,s.composeEntry.audience,s.composeEntry.petId,id) },
        enabled = !s.busy,
    )
    if(s.composeEntry.space != "community") {
        listOf("private" to "仅自己","friends" to "好友可见","selected" to "指定好友","excluded" to "排除指定好友").forEach { (mode,label) ->
            CheckLine(label,s.composeEntry.audience.mode == mode,{ checked -> if(checked) store.editComposeOptions(s.composeEntry.space,Audience(mode,if(mode in listOf("selected","excluded")) s.composeEntry.audience.userIds else emptyList()),s.composeEntry.petId) },!s.busy)
        }
        if(s.composeEntry.audience.mode in listOf("selected","excluded")) {
            Text(if (s.composeEntry.audience.mode == "selected") "仅以下好友可见。" else "所有好友中排除以下人员；今后新增的好友也可能可见。")
            PeopleChoiceField(store.features,feature,"选择发布受众",s.composeEntry.audience.userIds,
                { chosen -> store.editComposeAudienceList(chosen.joinToString(",")) },multiple = true,enabled = !s.busy)
            Text("提交前会重新核对好友关系；读取失败或人员失权时保留名单，不会自动放宽为公开。")
        }
    }
    if(s.composeEntry.space == "diary") PetChoiceField(store.features,feature,"composePet",selection = s.composeEntry.petId,
        onSelected = { chosen -> store.editComposeOptions("diary",s.composeEntry.audience,chosen?.id ?: 0) },enabled = !s.busy)

    if(s.pendingOperations.isNotEmpty()) FeatureButton("核对原发布或消息结果") { store.features.open(FeatureRoute(FeaturePage.Recovery)) }
    FeatureButton("账号资格与手机验证") { store.features.open(FeatureRoute(FeaturePage.Security)) }
}

@Composable fun ExpressionPanel(store: MobileStore,s: MobileState,editor: TextFieldValue,onEdit: (TextFieldValue) -> Unit,stickersAllowed: Boolean) {
    var category by rememberSessionUiState("expression.category") { "emoji" }
    SegmentedTextBar(listOf("emoji" to "表情","favorite" to "收藏","recent" to "最近","sticker" to "贴纸"),category,{ category = it })
    val saved = s.expressionState?.data ?: ExpressionBody()
    val emoji = when(category) { "favorite" -> s.emoji.filter { it.id in saved.favorites }; "recent" -> saved.recent.mapNotNull { id -> s.emoji.firstOrNull { it.id == id } }; "sticker" -> emptyList(); else -> s.emoji }
    val stickers = when(category) { "favorite" -> s.stickers.filter { it.id in saved.favorites }; "recent" -> saved.recent.mapNotNull { id -> s.stickers.firstOrNull { it.id == id } }; "sticker" -> s.stickers; else -> emptyList() }
    LazyVerticalGrid(GridCells.Adaptive(if(category == "sticker") 88.dp else 64.dp),Modifier.fillMaxWidth().heightIn(min = 80.dp,max = 260.dp)) {
        items(emoji,key = { it.id }) { item -> Column {
            TextButton({ val start = editor.selection.min; val end = editor.selection.max; val next = editor.text.replaceRange(start,end,item.unicode); val value = TextFieldValue(next,TextRange(start + item.unicode.length)); if(store.editReply(next)) { onEdit(value); store.rememberExpression(item.id) } },enabled = !s.busy) { EmojiPhoto(item) }
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
fun openNotification(store: MobileStore,body: NotificationBody) {
    if (!store.openNotification(body)) store.pickerFailed("此通知暂无可打开的有效对象，请在事项列表核对")
}

@Composable fun ComposeEmojiPanel(store: MobileStore,s: MobileState,editor: TextFieldValue,onEdit: (TextFieldValue) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(52.dp),Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
        items(s.emoji,key = { it.id }) { item -> TextButton({
            val start = editor.selection.min; val next = editor.text.replaceRange(start,editor.selection.max,item.unicode)
            onEdit(TextFieldValue(next,TextRange(start + item.unicode.length))); store.rememberExpression(item.id)
        },enabled = !s.busy) { EmojiPhoto(item) } }
    }
    TextButton({ val start = if(editor.selection.collapsed) previousGrapheme(editor.text,editor.selection.start) else editor.selection.min; onEdit(TextFieldValue(editor.text.removeRange(start,editor.selection.max),TextRange(start))) },enabled = !s.busy && editor.selection.max > 0) { Text("删除上一颗表情／选区") }
}

/** 输入内容属于共享内存／私有草稿；系统 Bundle 只记光标，不复制正文。 */
@Composable fun rememberPrivateEditor(text: String,key: String): MutableState<TextFieldValue> {
    var start by androidx.compose.runtime.saveable.rememberSaveable(key) { mutableIntStateOf(text.length) }
    var end by androidx.compose.runtime.saveable.rememberSaveable(key) { mutableIntStateOf(text.length) }
    val editor = remember(key) { mutableStateOf(TextFieldValue(text,TextRange(start.coerceIn(0,text.length),end.coerceIn(0,text.length)))) }
    SideEffect { start = editor.value.selection.start; end = editor.value.selection.end }
    return editor
}
