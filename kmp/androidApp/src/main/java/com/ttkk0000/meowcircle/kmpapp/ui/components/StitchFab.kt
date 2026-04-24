package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows

@Composable
fun StitchFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "发布",
) {
    FloatingActionButton(
        onClick = onClick,
        modifier =
            modifier
                .shadow(
                    elevation = StitchShadows.ctaGlowY,
                    shape = CircleShape,
                    ambientColor = StitchShadows.ctaGlowColor,
                    spotColor = StitchShadows.ctaGlowColor,
                )
                .size(56.dp),
        shape = CircleShape,
        containerColor = StitchPalette.Brand,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = contentDescription)
    }
}
