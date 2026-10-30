package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*

@Composable fun CityField(value: String, onChange: (String) -> Unit, label: String = "城市", enabled: Boolean = true,
    allowEmpty: Boolean = true, emptyLabel: String = "不限城市／暂不填写") {
    var opened by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var provinceMenu by remember { mutableStateOf(false) }
    val selected = mainlandCity(value)
    MndSecondaryButton("$label：${selected?.name ?: value.ifBlank { "请选择" }}", enabled) {
        query = ""; province = selected?.provinceCode.orEmpty(); opened = true
    }
    if (opened && enabled) AlertDialog(onDismissRequest = { opened = false }, title = { Text("选择大陆城市") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MndOutlinedField(query, { query = it; province = "" }, Modifier.fillMaxWidth(),
                    label = { Text("搜索城市、拼音或区划代码") }, singleLine = true)
                Box {
                    TextButton({ provinceMenu = true }) { Text(mainlandProvinces.firstOrNull { it.code == province }?.name ?: "全部省份") }
                    DropdownMenu(provinceMenu, { provinceMenu = false }, Modifier.heightIn(max = 320.dp)) {
                        DropdownMenuItem(text = { Text("全部省份") }, onClick = { province = ""; provinceMenu = false })
                        mainlandProvinces.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { province = p.code; provinceMenu = false }) }
                    }
                }
                val rows = remember(query, province) { searchMainlandCities(query, province) }
                Text("支持地级市、县级市、自治州、盟、地区及省直辖地区", style = MaterialTheme.typography.bodySmall)
                if (rows.isEmpty()) Text("没有匹配的城市，请更换关键词或省份。")
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(rows, key = { it.code }) { city ->
                        TextButton({ onChange(city.value); opened = false }, Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(city.name + if (selected?.code == city.code) " · 已选" else "")
                                Text(city.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton({ opened = false }) { Text("关闭") } },
        dismissButton = { if (allowEmpty) TextButton({ onChange(""); opened = false }) { Text(emptyLabel) } })
}

@Composable fun CityFormField(f: MobileFeatureStore, s: FeatureState, fallback: String = "", label: String = "城市", allowEmpty: Boolean = true) {
    CityField(selectedCity(f, fallback), { f.edit("cityChoice", "catalog"); f.edit("city", it) }, label,
        enabled = !s.busy && !s.loading && s.confirmation == null && !s.unconfirmedResult, allowEmpty = allowEmpty)
}
