package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ttkk0000.meowcircle.client.*

/** A city/area is a text region, never a fake pin at an administrative centre. */
@Composable internal fun RegionFields(f: MobileFeatureStore, s: FeatureState, city: String, area: String = "", withArea: Boolean = true) {
    ChoiceField(f,s,"cityChoice","选择城市",listOf("武汉" to "武汉","上海" to "上海","other" to "其他城市"),
        if(city in listOf("武汉","上海")) city else "other")
    if (f.field("cityChoice",if(city in listOf("武汉","上海")) city else "other") == "other")
        FormField(f,s,"city","城市名称",city)
    if (withArea) FormField(f,s,"area","区县、街道或附近地标（公开）",area)
    Text(if(withArea) "仅公开你填写的大致区域。请勿填写门牌、电话或私人精确地址。" else "关注所选城市的寻宠信息；不获取或持续追踪你的位置。")
}

internal fun selectedCity(f: MobileFeatureStore, fallback: String): String {
    val choice = f.field("cityChoice",if(fallback in listOf("武汉","上海")) fallback else "other")
    return if(choice == "other") f.field("city",fallback).trim() else choice
}
