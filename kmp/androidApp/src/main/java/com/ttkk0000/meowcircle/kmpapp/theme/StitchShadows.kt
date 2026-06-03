package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** M&D ambient shadows: warm crimson tint, restrained enough for dense mobile and desktop surfaces. */
object StitchShadows {
    val cardAmbientColor = Color(0x12CF2D56)
    val cardAmbientBlur: Dp = 32.dp
    val cardAmbientY: Dp = 12.dp

    val headerAmbientColor = Color(0x0DCF2D56)
    val headerAmbientBlur: Dp = 20.dp
    val headerAmbientY: Dp = 4.dp

    val ctaGlowColor = Color(0x33CF2D56)
    val ctaGlowBlur: Dp = 15.dp
    val ctaGlowY: Dp = 8.dp

    val avatarGlowColor = Color(0x1FCF2D56)
    val avatarGlowBlur: Dp = 20.dp
}
