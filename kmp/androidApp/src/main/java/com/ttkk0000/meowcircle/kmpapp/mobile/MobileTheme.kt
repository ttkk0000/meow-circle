package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontLoadingStrategy
import com.ttkk0000.meowcircle.kmpapp.R

// Figma phone V2 default. Legacy Honey / Mint / Night / Neutral preferences are intentionally retained elsewhere.
private val DefaultColors = lightColorScheme(
    primary = Color(0xFFC84B3D), onPrimary = Color(0xFFFFFEFC), primaryContainer = Color(0xFFFFF1EB),
    onPrimaryContainer = Color(0xFF9E382A), background = Color(0xFFF7F8FA), onBackground = Color(0xFF202329),
    surface = Color(0xFFFFFEFC), onSurface = Color(0xFF202329), onSurfaceVariant = Color(0xFF616771),
    surfaceVariant = Color(0xFFF0F2F5), outline = Color(0xFF838A94), outlineVariant = Color(0xFFE1E4E8),
)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resource: Int) = FontFamily(listOf(400,500,600,700).map { weight ->
    Font(resource,weight = FontWeight(weight),loadingStrategy = FontLoadingStrategy.Async,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
})
internal val BrandFont = variableFamily(R.font.manrope)
private val ContentFont = variableFamily(R.font.noto_sans_sc)
private val DefaultTypography = Typography().let { t -> Typography(
    displayLarge = t.displayLarge.copy(fontFamily = BrandFont),displayMedium = t.displayMedium.copy(fontFamily = BrandFont),displaySmall = t.displaySmall.copy(fontFamily = BrandFont),
    headlineLarge = t.headlineLarge.copy(fontFamily = ContentFont),headlineMedium = t.headlineMedium.copy(fontFamily = ContentFont),headlineSmall = t.headlineSmall.copy(fontFamily = ContentFont),
    titleLarge = t.titleLarge.copy(fontFamily = ContentFont,fontSize = 20.sp,lineHeight = 30.sp,fontWeight = FontWeight.Bold),titleMedium = t.titleMedium.copy(fontFamily = ContentFont,fontSize = 16.sp,lineHeight = 24.sp,fontWeight = FontWeight.Bold),titleSmall = t.titleSmall.copy(fontFamily = ContentFont),
    bodyLarge = t.bodyLarge.copy(fontFamily = ContentFont,fontSize = 16.sp,lineHeight = 26.sp),bodyMedium = t.bodyMedium.copy(fontFamily = ContentFont),bodySmall = t.bodySmall.copy(fontFamily = ContentFont,fontSize = 12.sp,lineHeight = 20.sp),
    labelLarge = t.labelLarge.copy(fontFamily = ContentFont,fontSize = 16.sp,lineHeight = 24.sp),labelMedium = t.labelMedium.copy(fontFamily = ContentFont,fontSize = 14.sp,lineHeight = 22.sp),labelSmall = t.labelSmall.copy(fontFamily = ContentFont),
) }

@Composable
fun MobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DefaultColors, typography = DefaultTypography,
        shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content)
}
