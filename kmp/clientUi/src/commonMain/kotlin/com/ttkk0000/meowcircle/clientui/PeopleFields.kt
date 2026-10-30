package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

/** 同一选择状态用于手机与平板；只筛选已取回的可访问好友，不提供账号枚举。 */
@Composable fun PeopleChoiceField(f: MobileFeatureStore, s: FeatureState, title: String,
    selected: List<Long>, onSelected: (List<Long>) -> Unit, multiple: Boolean = false,
    excluded: Set<Long> = emptySet(), enabled: Boolean = true) {
    val actor = f.api.user.value?.id
    var expanded by rememberSessionUiState("people.$title.expanded") { false }
    var query by rememberSessionUiState("people.$title.query") { "" }
    val editable = enabled && !s.busy && !s.loading && !s.unconfirmedResult && s.confirmation == null
    LaunchedEffect(actor) { if (actor != null && !s.peopleChoices.loaded) f.loadPeopleChoices() }
    val relationships = s.peopleChoices.items.filter { it.state == "accepted" }.associateBy { row ->
        if (row.data.requesterId == actor) row.data.recipientId else row.data.requesterId
    }
    val candidates = relationships.keys.filter { it !in excluded && it != actor && it !in s.peopleUnavailable }
        .mapNotNull { s.people[it] }.filter { person ->
            query.isBlank() || person.nickname.ifBlank { person.username }.contains(query.trim(), ignoreCase = true)
        }
    FeatureSection(title) {
        Text(if (selected.isEmpty()) "尚未选择" else "已选 ${selected.size} 位${if (multiple) "好友" else "人员"}")
        selected.forEach { id ->
            val inaccessible = id in s.peopleUnavailable || id in excluded
            val person = s.people[id]
            val name = person?.nickname?.ifBlank { person.username }?.ifBlank { "宠友" }
            Text(when {
                inaccessible -> "已选人员已失去访问权限，请移除或重新选择"
                person != null && id in relationships -> "$name · 好友"
                else -> "已选人员待重新核对；提交前会检查当前关系"
            }, color = if (inaccessible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            FeatureButton("移除${if (inaccessible || name == null) "此选择" else name}", editable) { onSelected(selected - id) }
        }
        FeatureButton(if (expanded) "收起好友列表" else "从我的好友中选择", editable && actor != null) { expanded = !expanded }
        if (expanded) {
            MndOutlinedField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("按昵称筛选已加载好友") }, enabled = editable)
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                candidates.forEach { person ->
                    val checked = person.id in selected
                    OutlinedButton(onClick = {
                        if (checked) onSelected(selected - person.id)
                        else if (!multiple) onSelected(listOf(person.id))
                        else if (selected.size < 200) onSelected(selected + person.id)
                        else f.notify("一次最多选择 200 位好友")
                    }, enabled = editable, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Text(person.nickname.ifBlank { person.username }.take(1).ifBlank { "友" })
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(person.nickname.ifBlank { person.username }.ifBlank { "宠友" })
                                Text("好友 · ${if (checked) "已选择" else "可选择"}", style = MaterialTheme.typography.labelMedium)
                            }
                            Text(if (checked) "取消" else "选择")
                        }
                    }
                }
                if (candidates.isEmpty() && s.peopleChoices.loaded && !s.peopleChoices.loading)
                    Text(if (query.isNotBlank()) "已加载的好友中没有匹配昵称，可继续加载下一页。" else "当前没有可选择的好友，可在明确打开的宠友资料页发起好友申请。")
                if (s.peopleErrors.isNotEmpty()) {
                    Text("部分好友资料暂时加载失败，原选择仍保留。", color = MaterialTheme.colorScheme.error)
                    FeatureButton("重试读取好友资料", !s.peopleChoices.loading && editable) { f.loadPeopleChoices() }
                }
                PageFooter(s.peopleChoices, { f.loadPeopleChoices() }, { f.loadPeopleChoices(true) },
                    "还没有好友关系", enabled = !s.peopleChoices.loading && editable)
                if (s.peopleChoices.loaded && s.peopleChoices.error == null)
                    FeatureButton("刷新好友关系", !s.peopleChoices.loading && editable) { f.loadPeopleChoices() }
            }
        }
    }
}
