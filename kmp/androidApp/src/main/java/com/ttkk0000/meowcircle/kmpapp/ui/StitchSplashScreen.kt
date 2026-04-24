package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.kmpapp.theme.StitchLoginRef
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette

/** MOBILE「启动页」：与登录稿同一奶油底 + 角上淡粉光晕，中央 MEOW。 */
@Composable
fun StitchSplashScreen(
    modifier: Modifier = Modifier,
    loading: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(StitchLoginRef.Background),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp)
                    // Negative end padding is illegal in Compose; nudge past the edge instead.
                    .offset(x = 24.dp)
                    .size(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    StitchLoginRef.PrimaryContainer.copy(alpha = 0.18f),
                                    Color.Transparent,
                                ),
                        ),
                    ),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "MEOW",
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Black,
                color = StitchLoginRef.PrimaryContainer,
                letterSpacing = (-1).sp,
            )
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = StitchLoginRef.SecondaryContainer,
                modifier =
                    Modifier
                        .padding(top = 12.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StitchLoginRef.PrimaryContainer.copy(alpha = 0.12f)),
            )
            Text(
                "开启你的吸猫之旅",
                style = MaterialTheme.typography.bodyLarge,
                color = StitchLoginRef.Outline,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp).padding(top = 20.dp),
                    color = StitchPalette.Brand,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}
