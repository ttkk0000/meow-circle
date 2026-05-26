package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * M&D radius rules: cards and panels stay at 8dp, pills remain fully rounded.
 */
object StitchShape {
    val pill = RoundedCornerShape(999.dp)
    val cardFeed = RoundedCornerShape(8.dp)
    /** 与 [cardFeed] 顶部圆角一致，用于封面图 clip。 */
    val cardFeedTop = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    val container = RoundedCornerShape(8.dp)
    val field = RoundedCornerShape(8.dp)
    val chip = RoundedCornerShape(8.dp)
}
