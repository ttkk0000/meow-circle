package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class MeowTheme {
    Sugar,
    Mint,
    Night
}

private val SugarScheme =
    lightColorScheme(
        primary = Color(0xFFCF2D56),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE77E96),
        onPrimaryContainer = Color(0xFF7F2038),
        secondary = Color(0xFF2D8AA3),
        onSecondary = Color.White,
        secondaryContainer = Color(0x2E2D8AA3),
        onSecondaryContainer = Color(0xFF2D8AA3),
        tertiary = Color(0xFFC08532),
        onTertiary = Color(0xFF7F2038),
        background = Color(0xFFF7F1E8),
        onBackground = Color(0xFF2B1722),
        surface = Color(0xFFFFFCF7),
        onSurface = Color(0xFF2B1722),
        surfaceVariant = Color(0xFFF9E8EE),
        onSurfaceVariant = Color(0xCC2B1722),
        outline = Color(0x9959414D),
        outlineVariant = Color(0x2459414D),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )

private val MintScheme =
    lightColorScheme(
        primary = Color(0xFF00AECA),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF56C7FF),
        onPrimaryContainer = Color(0xFF006B54),
        secondary = Color(0xFFFF9F7A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8FFF6),
        onSecondaryContainer = Color(0xFFFF9F7A),
        tertiary = Color(0xFF56C7FF),
        onTertiary = Color(0xFF006B54),
        background = Color(0xFFEFFFF8),
        onBackground = Color(0xFF0F2A29),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F2A29),
        surfaceVariant = Color(0xFFE8FFF6),
        onSurfaceVariant = Color(0xCC0F2A29),
        outline = Color(0x994D6866),
        outlineVariant = Color(0x244D6866),
        error = Color(0xFFD94A67),
        onError = Color.White,
    )

private val NightScheme =
    darkColorScheme(
        primary = Color(0xFFC482FF),
        onPrimary = Color(0xFF1D1028),
        primaryContainer = Color(0xFF282D59),
        onPrimaryContainer = Color(0xFFFFF7FF),
        secondary = Color(0xFFFFE66F),
        onSecondary = Color(0xFF1D1028),
        secondaryContainer = Color(0xFF252A55),
        onSecondaryContainer = Color(0xFFFFE66F),
        tertiary = Color(0xFF282D59),
        onTertiary = Color(0xFFFFF7FF),
        background = Color(0xFF14162F),
        onBackground = Color(0xFFF8F0FF),
        surface = Color(0xFF1D2144),
        onSurface = Color(0xFFF8F0FF),
        surfaceVariant = Color(0xFF252A55),
        onSurfaceVariant = Color(0xCCF8F0FF),
        outline = Color(0x99BBB4DA),
        outlineVariant = Color(0x24BBB4DA),
        error = Color(0xFFFF6689),
        onError = Color.White,
    )

/**
 * Typography = M&D cute design tokens（Plus Jakarta Sans 由系统无衬线近似），
 * 与移动端 M&D 设计记忆一致，避免旧稿的紧缩字距。
 */
private val StitchTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = 0.sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
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
                letterSpacing = 0.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            ),
    )

@Composable
fun MeowStitchTheme(
    theme: MeowTheme = MeowTheme.Sugar,
    content: @Composable () -> Unit
) {
    val scheme = when (theme) {
        MeowTheme.Mint -> MintScheme
        MeowTheme.Night -> NightScheme
        MeowTheme.Sugar -> SugarScheme
    }
    val stitchColors = when (theme) {
        MeowTheme.Mint -> MintStitchColors
        MeowTheme.Night -> NightStitchColors
        MeowTheme.Sugar -> SugarStitchColors
    }
    CompositionLocalProvider(LocalStitchColors provides stitchColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = StitchTypography,
            content = content,
        )
    }
}
