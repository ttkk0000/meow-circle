package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import com.ttkk0000.meowcircle.client.*

@Composable fun CoarseMap(latitude: Double, longitude: Double, coordinateSystem: String = "") {
    if (coordinateSystem !in listOf("gcj02", "wgs84")) {
        TaskNotice("此记录暂不显示地图点", "原坐标来源尚未确认，请以城市与文字区域为准。")
        return
    }
    val point = LostMapPoint("detail", "公开大致区域", latitude, longitude, coordinateSystem)
    NativeLostMap(listOf(point), point.id, point)
}

@Composable fun MapFeature(store: MobileStore, s: FeatureState, route: FeatureRoute) {
    val f = store.features
    CityFormField(f, s)
    FeatureButton("按城市查看求助", !s.loading) { f.reload() }
    val kind = f.field("mapKind", "lost")
    ChoiceChipBar(listOf("lost" to "走失", "found" to "拾获", "all" to "全部"), kind, { f.edit("mapKind", it) })
    val rows = s.mapCases.items.filter { kind == "all" || it.data.type == kind }
    val selected = f.field("selected").ifBlank { route.id.toString() }
    val points = rows.filter { it.state == "active" }.mapNotNull(::caseMapPoint)
    NativeLostMap(points, selected, points.firstOrNull { it.id == selected } ?: points.firstOrNull() ?: mapCityCenter(f.field("city")),
        onSelect = { f.edit("selected", it) })
    TaskNotice("公开位置为大致区域", "仅显示已载入且坐标来源明确的求助；圆圈表示公开粗范围，不代表实时位置。无地图点的事项仍保留在列表。")
    rows.forEach { row -> FeatureSection(row.data.title) {
        Text("${row.data.city} · ${row.data.area}")
        if (points.any { it.id == row.id.toString() }) TextButton({ f.edit("selected", row.id.toString()) }) { Text(if (selected == row.id.toString()) "已选中此区域" else "查看此大致区域") }
        else Text("仅提供文字区域，或地点来源待确认。")
        NavigationRow("查看原求助与进度") { f.closeRoute(); store.open(MobileDestination.Case(row.id)) }
    } }
    PageFooter(s.mapCases, { f.reload() }, { f.reload(true) }, enabled = !s.loading)
    if (s.mapCases.loaded && rows.isEmpty()) Text("当前条件暂无求助")
}

@Composable fun CaseMapPicker(f: MobileFeatureStore, s: FeatureState, original: CaseBody?) {
    val city = selectedCity(f, original?.city.orEmpty())
    val area = f.field("area", original?.area.orEmpty())
    val point = f.pickedCaseMapPoint(city, area)
    Text("最后见到的大致位置（可选）", style = MaterialTheme.typography.titleMedium)
    NativeLostMap(listOfNotNull(point), point?.id.orEmpty(), point ?: mapCityCenter(city), pickable = !s.busy && s.confirmation == null,
        onPick = { selected ->
            if (f.state.value.route == s.route) {
                f.edit("mapCity", city); f.edit("mapArea", area)
                f.edit("mapLatitude", selected.latitude.toString()); f.edit("mapLongitude", selected.longitude.toString())
            }
        })
    if (point != null) TextButton({ f.edit("mapLatitude", ""); f.edit("mapLongitude", ""); f.edit("keepPoint", "false") }, enabled = !s.busy) { Text("取消本次地图点，只保留文字区域") }
}
