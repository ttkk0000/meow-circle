package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** M&D ambient shadows: primary pink tint, restrained enough for dense mobile and desktop surfaces. */
object StitchShadows {
    val cardAmbientColor = Color(0x14FF4F93)
    val cardAmbientBlur: Dp = 32.dp
    val cardAmbientY: Dp = 12.dp

    val headerAmbientColor = Color(0x0DFF4F93)
    val headerAmbientBlur: Dp = 20.dp
    val headerAmbientY: Dp = 4.dp

    val ctaGlowColor = Color(0x4DFF4F93)
    val ctaGlowBlur: Dp = 15.dp
    val ctaGlowY: Dp = 8.dp

    val avatarGlowColor = Color(0x26FF4F93)
    val avatarGlowBlur: Dp = 20.dp
}
