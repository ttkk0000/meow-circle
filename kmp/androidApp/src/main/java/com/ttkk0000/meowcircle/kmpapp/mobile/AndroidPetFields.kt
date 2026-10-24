package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

/** 选择真实账号档案及当前版本；读取列表不会自动修改已确认的表单版本。 */
@Composable internal fun PetChoiceField(f: MobileFeatureStore, s: FeatureState, key: String,
    fallback: Long = 0, versionKey: String? = null, fallbackVersion: Long = 0, optional: Boolean = false) {
    val selected = f.field(key, fallback.takeIf { it > 0 }?.toString().orEmpty()).toLongOrNull()
    var expanded by rememberSessionUiState("pet.$key.expanded") { false }
    LaunchedEffect(s.route) { if (!s.petChoices.loaded) f.loadPetChoices() }
    val record = s.petChoices.items.firstOrNull { it.id == selected }
    FeatureSection(if (optional) "关联我的宠物（选填）" else "选择宠物档案") {
        Text(record?.data?.name ?: selected?.let { "已关联宠物 #$it" } ?: "尚未选择宠物")
        if (versionKey != null && record != null) {
            val accepted = f.field(versionKey, fallbackVersion.takeIf { it > 0 }?.toString().orEmpty()).toLongOrNull()
            Text(if (accepted == record.version) "已核对档案 V${record.version}" else "档案已更新，请重新选择并核对当前 V${record.version}。",
                color = if (accepted == record.version) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
        }
        FeatureButton(if (expanded) "收起档案列表" else "从我的档案中选择", !s.busy && !s.loading && !s.unconfirmedResult) { expanded = !expanded }
        if (expanded) {
            s.petChoices.items.forEach { pet ->
                OutlinedButton(onClick = {
                    f.edit(key, pet.id.toString()); versionKey?.let { f.edit(it, pet.version.toString()) }; expanded = false
                }, enabled = !s.busy && !s.loading && !s.unconfirmedResult,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("${if (pet.id == selected) "已选 · " else ""}${pet.data.name} · ${if (pet.data.species == "cat") "猫咪" else "狗狗"}\n档案 V${pet.version}")
                }
            }
            if (s.petChoices.empty) {
                Text("可以先建立宠物档案；当前表单会保留。")
                FeatureButton("建立宠物档案", !s.busy) { f.open(FeatureRoute(FeaturePage.PetEdit)) }
            }
            PageFooter(s.petChoices, { f.loadPetChoices() }, { f.loadPetChoices(true) }, "暂无可选档案", enabled = !s.petChoiceLoading && !s.busy)
            if (s.petChoices.loaded && s.petChoices.error == null) FeatureButton("重新读取档案列表", !s.petChoiceLoading) { f.loadPetChoices() }
        }
        if (optional && selected != null) FeatureButton("不关联宠物", !s.busy && !s.loading) { f.edit(key, "") }
    }
}
