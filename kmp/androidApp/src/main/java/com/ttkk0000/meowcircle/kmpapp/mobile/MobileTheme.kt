package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Figma phone V2 default. Legacy Honey / Mint / Night / Neutral preferences are intentionally retained elsewhere.
private val DefaultColors = lightColorScheme(
    primary = Color(0xFFC84B3D), onPrimary = Color(0xFFFFFEFC), primaryContainer = Color(0xFFFFF1EB),
    onPrimaryContainer = Color(0xFF9E382A), background = Color(0xFFF7F8FA), onBackground = Color(0xFF202329),
    surface = Color(0xFFFFFEFC), onSurface = Color(0xFF202329), onSurfaceVariant = Color(0xFF616771),
    surfaceVariant = Color(0xFFF0F2F5), outline = Color(0xFF838A94), outlineVariant = Color(0xFFE1E4E8),
)

@Composable
fun MobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DefaultColors,
        shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content)
}
