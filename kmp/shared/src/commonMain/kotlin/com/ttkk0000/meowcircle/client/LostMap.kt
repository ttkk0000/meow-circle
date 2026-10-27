package com.ttkk0000.meowcircle.client

import kotlin.math.floor

/** 只承载公开粗点；地图适配层不能取得私密位置与认领材料。 */
data class LostMapPoint(val id: String, val title: String, val latitude: Double, val longitude: Double,
    val coordinateSystem: String = "gcj02")

fun coarseMapPoint(latitude: Double, longitude: Double): LostMapPoint? {
    if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return LostMapPoint("picked", "本次公开大致区域", coarseCoordinate(latitude), coarseCoordinate(longitude))
}

fun caseMapPoint(record: MobileRecord<CaseBody>): LostMapPoint? {
    val body = record.data
    if (body.locationPrecision == "area" || body.coordinateSystem !in listOf("gcj02", "wgs84")) return null
    if (!body.latitude.isFinite() || !body.longitude.isFinite() || body.latitude !in -90.0..90.0 || body.longitude !in -180.0..180.0) return null
    return LostMapPoint(record.id.toString(), body.title, body.latitude, body.longitude, body.coordinateSystem)
}

fun mapCityCenter(city: String): LostMapPoint = mainlandCity(city)?.let {
    LostMapPoint("center", it.name, it.latitude, it.longitude)
} ?: LostMapPoint("country-center", "中国大陆", 35.0, 105.0)

fun MobileFeatureStore.pickedCaseMapPoint(city: String, area: String): LostMapPoint? {
    if (!sameCity(field("mapCity"), city) || field("mapArea") != area) return null
    return coarseMapPoint(field("mapLatitude").toDoubleOrNull() ?: Double.NaN,
        field("mapLongitude").toDoubleOrNull() ?: Double.NaN)
}

// 与服务端中点远离0的规则一致，避免跨端默认round舍入方式不同。
private fun coarseCoordinate(value: Double): Double = if (value < 0) -floor(-value * 100 + 0.5) / 100 else floor(value * 100 + 0.5) / 100
