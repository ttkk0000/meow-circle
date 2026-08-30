package com.ttkk0000.meowcircle.desktop

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class MndDesktopThemeMode(val label: String) {
    Honey("Honey"),
    Mint("Mint"),
    Night("Night"),
    Neutral("Neutral"),
}

@Immutable
data class DesktopStitchColors(
    val canvas: Color,
    val surface: Color,
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val brand: Color,
    val brandLight: Color,
    val brandMuted: Color,
    val primaryDark: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val gold: Color,
)

val HoneyDesktopColors = DesktopStitchColors(
    canvas = Color(0xFFFFF8F2),
    surface = Color(0xFFFFFFFF),
    surfaceLow = Color(0xFFFFF1E6),
    surfaceContainer = Color(0xFFFFF8F2),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    onSurface = Color(0xFF231F20),
    onSurfaceVariant = Color(0xFF6B5E57),
    brand = Color(0xFFFF8A3D),
    brandLight = Color(0xFFF0782C),
    brandMuted = Color(0x26FF8A3D),
    primaryDark = Color(0xFF9A4600),
    secondary = Color(0xFFFFD166),
    secondaryContainer = Color(0xFFFFF2C2),
    outline = Color(0xFFCBA88F),
    outlineVariant = Color(0xFFF1D8C8),
    error = Color(0xFFEF4444),
    gold = Color(0xFFFFD166),
)

val MintDesktopColors = DesktopStitchColors(
    canvas = Color(0xFFF5FFFC),
    surface = Color(0xFFFFFFFF),
    surfaceLow = Color(0xFFEAFBF6),
    surfaceContainer = Color(0xFFCDEFE6),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12312B),
    onSurfaceVariant = Color(0xFF4D6866),
    brand = Color(0xFF2EC4A6),
    brandLight = Color(0xFF16A34A),
    brandMuted = Color(0x212EC4A6),
    primaryDark = Color(0xFF12312B),
    secondary = Color(0xFFA7F3D0),
    secondaryContainer = Color(0xFFDDFBF0),
    outline = Color(0xFF8CCFC0),
    outlineVariant = Color(0xFFCDEFE6),
    error = Color(0xFFEF4444),
    gold = Color(0xFFFF8A65),
)

val NightDesktopColors = DesktopStitchColors(
    canvas = Color(0xFF0B0D12),
    surface = Color(0xFF151821),
    surfaceLow = Color(0xFF1F2430),
    surfaceContainer = Color(0xFF252B38),
    surfaceContainerHigh = Color(0xFF151821),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    brand = Color(0xFF8B5CF6),
    brandLight = Color(0xFFA78BFA),
    brandMuted = Color(0x218B5CF6),
    primaryDark = Color(0xFFF8FAFC),
    secondary = Color(0xFFFBBF24),
    secondaryContainer = Color(0x29FBBF24),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF303644),
    error = Color(0xFFF87171),
    gold = Color(0xFF60A5FA),
)

val NeutralDesktopColors = DesktopStitchColors(
    canvas = Color(0xFFF7F7F8),
    surface = Color(0xFFFFFFFF),
    surfaceLow = Color(0xFFF3F4F6),
    surfaceContainer = Color(0xFFE5E7EB),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF4B5563),
    brand = Color(0xFF4B5563),
    brandLight = Color(0xFF374151),
    brandMuted = Color(0x1F4B5563),
    primaryDark = Color(0xFF111827),
    secondary = Color(0xFF9CA3AF),
    secondaryContainer = Color(0xFFF3F4F6),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
    gold = Color(0xFFF97316),
)

val LocalDesktopStitchColors = staticCompositionLocalOf { HoneyDesktopColors }

object MndDesktopShape {
    val card = RoundedCornerShape(8.dp)
    val panel = RoundedCornerShape(8.dp)
    val field = RoundedCornerShape(8.dp)
    val pill = RoundedCornerShape(999.dp)
}

fun colorsFor(mode: MndDesktopThemeMode): DesktopStitchColors =
    when (mode) {
        MndDesktopThemeMode.Honey -> HoneyDesktopColors
        MndDesktopThemeMode.Mint -> MintDesktopColors
        MndDesktopThemeMode.Night -> NightDesktopColors
        MndDesktopThemeMode.Neutral -> NeutralDesktopColors
    }

@Composable
fun MndDesktopTheme(
    mode: MndDesktopThemeMode,
    content: @Composable () -> Unit,
) {
    val colors = colorsFor(mode)
    val scheme =
        if (mode == MndDesktopThemeMode.Night) {
            darkColorScheme(
                primary = colors.brand,
                secondary = colors.secondary,
                background = colors.canvas,
                surface = colors.surface,
                onSurface = colors.onSurface,
                outline = colors.outline,
                error = colors.error,
            )
        } else {
            lightColorScheme(
                primary = colors.brand,
                secondary = colors.secondary,
                background = colors.canvas,
                surface = colors.surface,
                onSurface = colors.onSurface,
                outline = colors.outline,
                error = colors.error,
            )
        }

    androidx.compose.runtime.CompositionLocalProvider(LocalDesktopStitchColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
