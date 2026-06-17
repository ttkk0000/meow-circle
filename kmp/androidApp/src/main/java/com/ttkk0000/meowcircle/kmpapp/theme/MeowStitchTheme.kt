package com.ttkk0000.meowcircle.kmpapp.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.ttkk0000.meowcircle.kmpapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.Bold)
)
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold)
)
enum class MeowTheme {
    Honey,
    Mint,
    Night,
    Neutral,
}
val HoneyScheme =
    lightColorScheme(
        primary = Color(0xFF9A4600),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFF8A3D),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFFFD166),
        onSecondary = Color(0xFF231F20),
        secondaryContainer = Color(0xFFFFF2C2),
        onSecondaryContainer = Color(0xFF231F20),
        tertiary = Color(0xFF7C5CFF),
        onTertiary = Color.White,
        background = Color(0xFFFFF8F2),
        onBackground = Color(0xFF231F20),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF231F20),
        surfaceVariant = Color(0xFFFFF1E6),
        onSurfaceVariant = Color(0xCC231F20),
        outline = Color(0xFFF1D8C8),
        outlineVariant = Color(0xFFF5E2D5),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )
val MintScheme =
    lightColorScheme(
        primary = Color(0xFF12312B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF2EC4A6),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFA7F3D0),
        onSecondary = Color(0xFF12312B),
        secondaryContainer = Color(0xFFDDFBF0),
        onSecondaryContainer = Color(0xFF12312B),
        tertiary = Color(0xFF0EA5E9),
        onTertiary = Color.White,
        background = Color(0xFFF5FFFC),
        onBackground = Color(0xFF12312B),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF12312B),
        surfaceVariant = Color(0xFFEAFBF6),
        onSurfaceVariant = Color(0xCC12312B),
        outline = Color(0xFF8CCFC0),
        outlineVariant = Color(0xFFCDEFE6),
        error = Color(0xFFEF4444),
        onError = Color.White,
    )
val NightScheme =
    darkColorScheme(
        primary = Color(0xFFF8FAFC),
        onPrimary = Color(0xFF151821),
        primaryContainer = Color(0xFF8B5CF6),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFFBBF24),
        onSecondary = Color(0xFF151821),
        secondaryContainer = Color(0x29FBBF24),
        onSecondaryContainer = Color(0xFFFBBF24),
        tertiary = Color(0xFF60A5FA),
        onTertiary = Color(0xFF151821),
        background = Color(0xFF0B0D12),
        onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF151821),
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF252B38),
        onSurfaceVariant = Color(0xFFCBD5E1),
        outline = Color(0xFF475569),
        outlineVariant = Color(0xFF303644),
        error = Color(0xFFF87171),
        onError = Color.White,
    )
val NeutralScheme =
    lightColorScheme(
        primary = Color(0xFF4B5563),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF4B5563),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF9CA3AF),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF3F4F6),
        onSecondaryContainer = Color(0xFF111827),
        tertiary = Color(0xFFF97316),
        onTertiary = Color.White,
        background = Color(0xFFF7F7F8),
        onBackground = Color(0xFF111827),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF111827),
        surfaceVariant = Color(0xFFE5E7EB),
        onSurfaceVariant = Color(0xFF4B5563),
        outline = Color(0xFF9CA3AF),
        outlineVariant = Color(0xFFE5E7EB),
        error = Color(0xFFDC2626),
        onError = Color.White,
    )
/**
 * Typography = M&D cute design tokens（Plus Jakarta Sans 由系统无衬线近似），
 * 与移动端 M&D 设计记忆一致，避免旧稿的紧缩字距。
 */
val StitchTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = 0.sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 28.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            ),
    )
@Composable
fun MeowStitchTheme(
    theme: MeowTheme = MeowTheme.Honey,
    content: @Composable () -> Unit
) {
    val scheme = when (theme) {
        MeowTheme.Honey -> HoneyScheme
        MeowTheme.Mint -> MintScheme
        MeowTheme.Night -> NightScheme
        MeowTheme.Neutral -> NeutralScheme
    }
    val stitchColors = when (theme) {
        MeowTheme.Honey -> HoneyStitchColors
        MeowTheme.Mint -> MintStitchColors
        MeowTheme.Night -> NightStitchColors
        MeowTheme.Neutral -> NeutralStitchColors
    }
    val darkTheme = theme == MeowTheme.Night
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is android.app.Activity) {
                    break
                }
                context = context.baseContext
            }
            if (context is android.app.Activity) {
                val window = context.window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalStitchColors provides stitchColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = StitchTypography,
            content = content,
        )
    }
}