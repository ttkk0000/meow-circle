package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * M&D Stitch V2 Skeleton Pulse Modifier.
 * Provides a premium, breathing animation for loading states.
 * Colors alternate between SurfaceLow and OutlineVariant.
 */
fun Modifier.stitchSkeleton(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skeleton_transition")
    val color by transition.animateColor(
        initialValue = StitchPalette.SurfaceLow,
        targetValue = StitchPalette.OutlineVariant,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_color"
    )
    this.background(color)
}
