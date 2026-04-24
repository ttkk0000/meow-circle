package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val StitchScheme =
    lightColorScheme(
        primary = StitchPalette.Brand,
        onPrimary = Color.White,
        primaryContainer = StitchPalette.BrandLight,
        onPrimaryContainer = StitchPalette.PrimaryDark,
        secondary = StitchPalette.Secondary,
        onSecondary = Color.White,
        secondaryContainer = StitchPalette.SecondaryContainer,
        onSecondaryContainer = StitchPalette.Secondary,
        tertiary = StitchPalette.BrandLight,
        onTertiary = StitchPalette.PrimaryDark,
        background = StitchPalette.Canvas,
        onBackground = StitchPalette.OnSurface,
        surface = StitchPalette.Surface,
        onSurface = StitchPalette.OnSurface,
        surfaceVariant = StitchPalette.SurfaceContainer,
        onSurfaceVariant = StitchPalette.OnSurfaceVariant,
        outline = StitchPalette.Outline,
        outlineVariant = StitchPalette.OutlineVariant,
        error = StitchPalette.Error,
        onError = Color.White,
    )

/**
 * Typography = Stitch MCP `designTheme.typography`（Plus Jakarta Sans 由系统无衬线近似），
 * 与移动端工程「Kitty Circle Social」一致，而非桌面导出 HTML。
 */
private val StitchTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = (-0.96).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.6).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.35).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.25).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.15).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.1).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.12).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 28.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.14.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.12.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.1.sp,
            ),
    )

@Composable
fun MeowStitchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StitchScheme,
        typography = StitchTypography,
        content = content,
    )
}
