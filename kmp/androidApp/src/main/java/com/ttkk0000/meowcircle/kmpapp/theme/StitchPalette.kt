package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * M&D = meow & doggie. Cat-first, doggie-friendly token set shared with web.
 */
data class StitchPaletteColors(
    val Canvas: Color,
    val Surface: Color,
    val SurfaceLow: Color,
    val SurfaceContainer: Color,
    val SurfaceContainerHigh: Color,
    val OnSurface: Color,
    val OnSurfaceVariant: Color,
    val Brand: Color,
    val BrandLight: Color,
    val BrandEnd: Color,
    val BrandMuted: Color,
    val PrimaryDark: Color,
    val Secondary: Color,
    val SecondaryContainer: Color,
    val Outline: Color,
    val OutlineVariant: Color,
    val Error: Color,
    val Gold: Color,
    val GoldWeak: Color,
    val HeaderBorder: Color,
    val BorderHairline: Color,
    val Stone500: Color,
)

val HoneyStitchColors = StitchPaletteColors(
    Canvas = Color(0xFFFFF8F2),
    Surface = Color(0xFFFFFFFF),
    SurfaceLow = Color(0xFFFFF1E6),
    SurfaceContainer = Color(0xFFFFF8F2),
    SurfaceContainerHigh = Color(0xFFFFFFFF),
    OnSurface = Color(0xFF231F20),
    OnSurfaceVariant = Color(0xFF6B5E57),
    Brand = Color(0xFFFF8A3D),
    BrandLight = Color(0xFFF0782C),
    BrandEnd = Color(0xFFFF8A3D),
    BrandMuted = Color(0x26FF8A3D),
    PrimaryDark = Color(0xFF9A4600),
    Secondary = Color(0xFFFFD166),
    SecondaryContainer = Color(0xFFFFF2C2),
    Outline = Color(0xFFCBA88F),
    OutlineVariant = Color(0xFFF1D8C8),
    Error = Color(0xFFEF4444),
    Gold = Color(0xFFFFD166),
    GoldWeak = Color(0x24F59E0B),
    HeaderBorder = Color(0x1FFF8A3D),
    BorderHairline = Color(0xFFF1D8C8),
    Stone500 = Color(0x996B5E57),
)

val MintStitchColors = StitchPaletteColors(
    Canvas = Color(0xFFF5FFFC),
    Surface = Color(0xFFFFFFFF),
    SurfaceLow = Color(0xFFEAFBF6),
    SurfaceContainer = Color(0xFFCDEFE6),
    SurfaceContainerHigh = Color(0xFFFFFFFF),
    OnSurface = Color(0xFF12312B),
    OnSurfaceVariant = Color(0xFF4D6866),
    Brand = Color(0xFF2EC4A6),
    BrandLight = Color(0xFF16A34A),
    BrandEnd = Color(0xFF2EC4A6),
    BrandMuted = Color(0x212EC4A6),
    PrimaryDark = Color(0xFF12312B),
    Secondary = Color(0xFFA7F3D0),
    SecondaryContainer = Color(0xFFDDFBF0),
    Outline = Color(0xFF8CCFC0),
    OutlineVariant = Color(0xFFCDEFE6),
    Error = Color(0xFFEF4444),
    Gold = Color(0xFFFF8A65),
    GoldWeak = Color(0x21FF8A65),
    HeaderBorder = Color(0x1F2EC4A6),
    BorderHairline = Color(0x2212312B),
    Stone500 = Color(0x994D6866),
)

val NightStitchColors = StitchPaletteColors(
    Canvas = Color(0xFF0B0D12),
    Surface = Color(0xFF151821),
    SurfaceLow = Color(0xFF1F2430),
    SurfaceContainer = Color(0xFF252B38),
    SurfaceContainerHigh = Color(0xFF151821),
    OnSurface = Color(0xFFF8FAFC),
    OnSurfaceVariant = Color(0xFFCBD5E1),
    Brand = Color(0xFF8B5CF6),
    BrandLight = Color(0xFFA78BFA),
    BrandEnd = Color(0xFF8B5CF6),
    BrandMuted = Color(0x218B5CF6),
    PrimaryDark = Color(0xFFF8FAFC),
    Secondary = Color(0xFFFBBF24),
    SecondaryContainer = Color(0x29FBBF24),
    Outline = Color(0xFF475569),
    OutlineVariant = Color(0xFF303644),
    Error = Color(0xFFF87171),
    Gold = Color(0xFF60A5FA),
    GoldWeak = Color(0x2E60A5FA),
    HeaderBorder = Color(0x1F8B5CF6),
    BorderHairline = Color(0x24F8FAFC),
    Stone500 = Color(0xFF94A3B8),
)

val NeutralStitchColors = StitchPaletteColors(
    Canvas = Color(0xFFF7F7F8),
    Surface = Color(0xFFFFFFFF),
    SurfaceLow = Color(0xFFF3F4F6),
    SurfaceContainer = Color(0xFFE5E7EB),
    SurfaceContainerHigh = Color(0xFFFFFFFF),
    OnSurface = Color(0xFF111827),
    OnSurfaceVariant = Color(0xFF4B5563),
    Brand = Color(0xFF4B5563),
    BrandLight = Color(0xFF374151),
    BrandEnd = Color(0xFF4B5563),
    BrandMuted = Color(0x1F4B5563),
    PrimaryDark = Color(0xFF111827),
    Secondary = Color(0xFF9CA3AF),
    SecondaryContainer = Color(0xFFF3F4F6),
    Outline = Color(0xFF9CA3AF),
    OutlineVariant = Color(0xFFE5E7EB),
    Error = Color(0xFFDC2626),
    Gold = Color(0xFFF97316),
    GoldWeak = Color(0x1FF97316),
    HeaderBorder = Color(0x1C111827),
    BorderHairline = Color(0xFFE5E7EB),
    Stone500 = Color(0xFF6B7280),
)

val LocalStitchColors = staticCompositionLocalOf { HoneyStitchColors }

object StitchPalette {
    val Canvas: Color @Composable get() = LocalStitchColors.current.Canvas
    val Surface: Color @Composable get() = LocalStitchColors.current.Surface
    val SurfaceLow: Color @Composable get() = LocalStitchColors.current.SurfaceLow
    val SurfaceContainer: Color @Composable get() = LocalStitchColors.current.SurfaceContainer
    val SurfaceContainerHigh: Color @Composable get() = LocalStitchColors.current.SurfaceContainerHigh
    val OnSurface: Color @Composable get() = LocalStitchColors.current.OnSurface
    val OnSurfaceVariant: Color @Composable get() = LocalStitchColors.current.OnSurfaceVariant
    val Brand: Color @Composable get() = LocalStitchColors.current.Brand
    val BrandLight: Color @Composable get() = LocalStitchColors.current.BrandLight
    val BrandEnd: Color @Composable get() = LocalStitchColors.current.BrandEnd
    val BrandMuted: Color @Composable get() = LocalStitchColors.current.BrandMuted
    val PrimaryDark: Color @Composable get() = LocalStitchColors.current.PrimaryDark
    val Secondary: Color @Composable get() = LocalStitchColors.current.Secondary
    val SecondaryContainer: Color @Composable get() = LocalStitchColors.current.SecondaryContainer
    val Outline: Color @Composable get() = LocalStitchColors.current.Outline
    val OutlineVariant: Color @Composable get() = LocalStitchColors.current.OutlineVariant
    val Error: Color @Composable get() = LocalStitchColors.current.Error
    val Gold: Color @Composable get() = LocalStitchColors.current.Gold
    val GoldWeak: Color @Composable get() = LocalStitchColors.current.GoldWeak
    val HeaderBorder: Color @Composable get() = LocalStitchColors.current.HeaderBorder
    val BorderHairline: Color @Composable get() = LocalStitchColors.current.BorderHairline
    val Stone500: Color @Composable get() = LocalStitchColors.current.Stone500
}

/**
 * Auth screen colors retained as semantic aliases for existing screen code.
 */
object StitchLoginRef {
    val Background: Color @Composable get() = StitchPalette.Canvas
    val PrimaryContainer: Color @Composable get() = StitchPalette.Brand
    val Primary: Color @Composable get() = StitchPalette.Brand
    val InversePrimary: Color @Composable get() = StitchPalette.BrandLight
    val OnSurface: Color @Composable get() = StitchPalette.OnSurface
    val OnSurfaceVariant: Color @Composable get() = StitchPalette.OnSurfaceVariant
    val Outline: Color @Composable get() = StitchPalette.Outline
    val SurfaceContainerLow: Color @Composable get() = StitchPalette.SurfaceLow
    val SurfaceContainerLowest: Color @Composable get() = StitchPalette.Surface
    val SurfaceVariant: Color @Composable get() = StitchPalette.SurfaceContainerHigh
    val SecondaryContainer: Color @Composable get() = StitchPalette.Secondary
    val OnPrimaryButton: Color @Composable get() = Color.White
    val WeChat: Color get() = Color(0xFF07C160)
    val QqBlue: Color get() = Color(0xFF12B7F5)
}
