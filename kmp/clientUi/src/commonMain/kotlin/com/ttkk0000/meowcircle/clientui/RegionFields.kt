package com.ttkk0000.meowcircle.clientui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ttkk0000.meowcircle.client.*

/** A city/area is a text region, never a fake pin at an administrative centre. */
@Composable fun RegionFields(f: MobileFeatureStore, s: FeatureState, city: String, area: String = "", withArea: Boolean = true) {
    CityFormField(f,s,city,allowEmpty = false)
    if (withArea) FormField(f,s,"area","区县、街道或附近地标（公开）",area)
    Text(if(withArea) "仅公开你填写的大致区域。请勿填写门牌、电话或私人精确地址。" else "关注所选城市的寻宠信息；不获取或持续追踪你的位置。")
}

fun selectedCity(f: MobileFeatureStore, fallback: String): String {
    val choice = f.field("cityChoice")
    // Preserve the old two-city draft format until the user picks a catalog entry.
    return normalizeCity(if(choice in listOf("武汉","上海")) choice else f.field("city",fallback))
}
