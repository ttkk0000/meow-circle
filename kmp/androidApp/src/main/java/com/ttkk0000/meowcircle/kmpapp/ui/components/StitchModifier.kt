package com.ttkk0000.meowcircle.kmpapp.ui.components

import android.graphics.DashPathEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.toComposePathEffect

fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp) = this.drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f).toComposePathEffect()
        ),
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
    )
}
