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

val SugarStitchColors = StitchPaletteColors(
    Canvas = Color(0xFFFFF7EE),
    Surface = Color(0xFFFFFFFF),
    SurfaceLow = Color(0xFFFFFAF4),
    SurfaceContainer = Color(0xFFFFF0F6),
    SurfaceContainerHigh = Color(0xFFFFE2EF),
    OnSurface = Color(0xFF2B1722),
    OnSurfaceVariant = Color(0xCC2B1722),
    Brand = Color(0xFFFF4F93),
    BrandLight = Color(0xFFFF7EBB),
    BrandEnd = Color(0xFFFF4F93),
    BrandMuted = Color(0x21FF4F93),
    PrimaryDark = Color(0xFFA91558),
    Secondary = Color(0xFF26B8D8),
    SecondaryContainer = Color(0x3356C7FF),
    Outline = Color(0x9959414D),
    OutlineVariant = Color(0x2459414D),
    Error = Color(0xFFBA1A1A),
    Gold = Color(0xFF56C7FF),
    GoldWeak = Color(0x2E56C7FF),
    HeaderBorder = Color(0x1FFF4F93),
    BorderHairline = Color(0x222B1722),
    Stone500 = Color(0x9959414D),
)

val MintStitchColors = StitchPaletteColors(
    Canvas = Color(0xFFEFFFF8),
    Surface = Color(0xFFFFFFFF),
    SurfaceLow = Color(0xFFF7FFFC),
    SurfaceContainer = Color(0xFFE8FFF6),
    SurfaceContainerHigh = Color(0xFFD3F7EF),
    OnSurface = Color(0xFF0F2A29),
    OnSurfaceVariant = Color(0xCC0F2A29),
    Brand = Color(0xFF00AECA),
    BrandLight = Color(0xFF56C7FF),
    BrandEnd = Color(0xFF00AECA),
    BrandMuted = Color(0x2100AECA),
    PrimaryDark = Color(0xFF006B54),
    Secondary = Color(0xFFFF9F7A),
    SecondaryContainer = Color(0x33FF9F7A),
    Outline = Color(0x994D6866),
    OutlineVariant = Color(0x244D6866),
    Error = Color(0xFFD94A67),
    Gold = Color(0xFF9274FF),
    GoldWeak = Color(0x2E9274FF),
    HeaderBorder = Color(0x1F00AECA),
    BorderHairline = Color(0x220F2A29),
    Stone500 = Color(0x994D6866),
)

val NightStitchColors = StitchPaletteColors(
    Canvas = Color(0xFF14162F),
    Surface = Color(0xFF1D2144),
    SurfaceLow = Color(0xFF191D3C),
    SurfaceContainer = Color(0xFF252A55),
    SurfaceContainerHigh = Color(0xFF20234A),
    OnSurface = Color(0xFFF8F0FF),
    OnSurfaceVariant = Color(0xCCF8F0FF),
    Brand = Color(0xFFC482FF),
    BrandLight = Color(0xFF282D59),
    BrandEnd = Color(0xFFC482FF),
    BrandMuted = Color(0x21C482FF),
    PrimaryDark = Color(0xFFFFF7FF),
    Secondary = Color(0xFFFFE66F),
    SecondaryContainer = Color(0x33FFE66F),
    Outline = Color(0x99BBB4DA),
    OutlineVariant = Color(0x24BBB4DA),
    Error = Color(0xFFFF6689),
    Gold = Color(0xFF64E7FF),
    GoldWeak = Color(0x2E64E7FF),
    HeaderBorder = Color(0x1FC482FF),
    BorderHairline = Color(0x22F8F0FF),
    Stone500 = Color(0x99BBB4DA),
)

val LocalStitchColors = staticCompositionLocalOf { SugarStitchColors }

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
    val Outline: Color @Composable get() = StitchPalette.Outline
    val SurfaceContainerLow: Color @Composable get() = StitchPalette.SurfaceLow
    val SurfaceContainerLowest: Color @Composable get() = StitchPalette.Surface
    val SurfaceVariant: Color @Composable get() = StitchPalette.SurfaceContainerHigh
    val SecondaryContainer: Color @Composable get() = StitchPalette.Secondary
    val OnPrimaryButton: Color @Composable get() = Color.White
    val WeChat: Color get() = Color(0xFF07C160)
    val QqBlue: Color get() = Color(0xFF12B7F5)
}
