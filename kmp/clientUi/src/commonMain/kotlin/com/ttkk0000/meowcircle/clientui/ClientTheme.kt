package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.clientui.resources.*
import org.jetbrains.compose.resources.Font

private val Colors = lightColorScheme(
    primary = Color(0xFFC84B3D), onPrimary = Color(0xFFFFFEFC), primaryContainer = Color(0xFFFFF1EB),
    onPrimaryContainer = Color(0xFF9E382A), background = Color(0xFFF7F8FA), onBackground = Color(0xFF202329),
    surface = Color(0xFFFFFEFC), onSurface = Color(0xFF202329), onSurfaceVariant = Color(0xFF616771),
    surfaceVariant = Color(0xFFF0F2F5), outline = Color(0xFF838A94), outlineVariant = Color(0xFFE0E3E8),
    secondary = Color(0xFF9E382A), onSecondary = Color(0xFFFFFEFC),
    secondaryContainer = Color(0xFFFFF1EB), onSecondaryContainer = Color(0xFF9E382A),
    tertiary = Color(0xFF176C49), onTertiary = Color(0xFFFFFEFC),
    tertiaryContainer = Color(0xFFE8F4ED), onTertiaryContainer = Color(0xFF176C49),
    error = Color(0xFFB42332), onError = Color(0xFFFFFEFC),
    errorContainer = Color(0xFFFDEEF0), onErrorContainer = Color(0xFFB42332),
    surfaceTint = Color(0xFFFFFEFC), inverseSurface = Color(0xFF202329),
    inverseOnSurface = Color(0xFFFFFEFC), inversePrimary = Color(0xFFFFF1EB),
    surfaceBright = Color(0xFFFFFEFC), surfaceDim = Color(0xFFF0F2F5),
    surfaceContainer = Color(0xFFFFFEFC), surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainerLowest = Color(0xFFFFFEFC), surfaceContainerHigh = Color(0xFFFFFEFC),
    surfaceContainerHighest = Color(0xFFF0F2F5),
)
private val LocalBrandFont = staticCompositionLocalOf { FontFamily.SansSerif }
val BrandFont: FontFamily @Composable get() = LocalBrandFont.current
val PageHeadlineStyle: androidx.compose.ui.text.TextStyle @Composable get() = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp,lineHeight = 40.sp,fontWeight = FontWeight.Bold)
val ClientWarningSurface = Color(0xFFFFF5DE)
val ClientSuccessSurface = Color(0xFFE8F4ED)
val ClientPublishPressed = Color(0xFFB13D30)
val ClientPublishDisabled = Color(0xFFECEEF1)
val ClientFocusColor = Color(0xFF626873)
val ClientButtonBorder = Color(0xFFB85F50)

@Composable fun MobileTheme(content: @Composable () -> Unit) {
    val brand = FontFamily(Font(Res.font.manrope, FontWeight.ExtraBold))
    val body = FontFamily(Font(Res.font.noto_sans_sc, FontWeight.Normal), Font(Res.font.noto_sans_sc, FontWeight.Medium), Font(Res.font.noto_sans_sc, FontWeight.Bold))
    val t = Typography()
    val typography = Typography(
        displayLarge = t.displayLarge.copy(fontFamily = brand), displayMedium = t.displayMedium.copy(fontFamily = brand), displaySmall = t.displaySmall.copy(fontFamily = brand),
        headlineLarge = t.headlineLarge.copy(fontFamily = body), headlineMedium = t.headlineMedium.copy(fontFamily = body), headlineSmall = t.headlineSmall.copy(fontFamily = body),
        titleLarge = t.titleLarge.copy(fontFamily = body, fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontFamily = body, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), titleSmall = t.titleSmall.copy(fontFamily = body),
        bodyLarge = t.bodyLarge.copy(fontFamily = body, fontSize = 16.sp, lineHeight = 26.sp), bodyMedium = t.bodyMedium.copy(fontFamily = body), bodySmall = t.bodySmall.copy(fontFamily = body, fontSize = 12.sp, lineHeight = 20.sp),
        labelLarge = t.labelLarge.copy(fontFamily = body, fontSize = 16.sp, lineHeight = 24.sp), labelMedium = t.labelMedium.copy(fontFamily = body, fontSize = 14.sp, lineHeight = 22.sp), labelSmall = t.labelSmall.copy(fontFamily = body, fontSize = 12.sp, lineHeight = 20.sp),
    )
    CompositionLocalProvider(LocalBrandFont provides brand) {
        MaterialTheme(colorScheme = Colors, typography = typography,
            shapes = Shapes(small = RoundedCornerShape(MobileUi.cardRadius), medium = RoundedCornerShape(MobileUi.cardRadius), large = RoundedCornerShape(MobileUi.dialogRadius))) {
            ClientAssets(content)
        }
    }
}
