package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette

enum class StitchMainTab {
    Home,
    Discover,
    Compose,
    Messages,
    Profile,
}

private data class NavSpec(
    val tab: StitchMainTab,
    val label: String,
    val icon: ImageVector,
)

private val NAV_ITEMS =
    listOf(
        NavSpec(StitchMainTab.Home, "首页", Icons.Outlined.Home),
        NavSpec(StitchMainTab.Discover, "发现", Icons.Outlined.Explore),
        NavSpec(StitchMainTab.Compose, "发布", Icons.Outlined.AddCircleOutline),
        NavSpec(StitchMainTab.Messages, "消息", Icons.Outlined.MailOutline),
        NavSpec(StitchMainTab.Profile, "我的", Icons.Outlined.AccountCircle),
    )

@Composable
fun StitchBottomNav(
    selected: StitchMainTab,
    onSelect: (StitchMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = StitchPalette.Surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        NAV_ITEMS.forEach { spec ->
            val sel = spec.tab == selected
            NavigationBarItem(
                selected = sel,
                onClick = { onSelect(spec.tab) },
                icon = {
                    if (spec.tab == StitchMainTab.Compose) {
                        Surface(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .border(1.dp, StitchPalette.Brand.copy(alpha = 0.2f), CircleShape),
                            shape = CircleShape,
                            color = if (sel) StitchPalette.Brand else StitchPalette.SurfaceLow,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.label,
                                    tint = if (sel) Color.White else StitchPalette.Brand,
                                    modifier = Modifier.size(25.dp),
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = spec.label,
                        )
                    }
                },
                label = { Text(spec.label, style = MaterialTheme.typography.labelMedium) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = StitchPalette.Brand,
                        selectedTextColor = StitchPalette.Brand,
                        indicatorColor = if (spec.tab == StitchMainTab.Compose) Color.Transparent else StitchPalette.BrandMuted,
                        unselectedIconColor = StitchPalette.Outline,
                        unselectedTextColor = StitchPalette.OnSurfaceVariant,
                    ),
            )
        }
    }
}
