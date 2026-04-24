package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 圆角与 Stitch MCP 移动端 designMd 一致：主容器 ≥24dp，信息流卡片 **32dp**，全圆药丸。
 */
object StitchShape {
    val pill = RoundedCornerShape(999.dp)
    val cardFeed = RoundedCornerShape(32.dp)
    /** 与 [cardFeed] 顶部圆角一致，用于封面图 clip。 */
    val cardFeedTop = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val container = RoundedCornerShape(24.dp)
    val field = RoundedCornerShape(24.dp)
    val chip = RoundedCornerShape(20.dp)
}
