package com.ttkk0000.meowcircle.kmpapp.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        containerColor = StitchPalette.Surface,
        tonalElevation = 0.dp,
    ) {
        NAV_ITEMS.forEach { spec ->
            val sel = spec.tab == selected
            NavigationBarItem(
                selected = sel,
                onClick = { onSelect(spec.tab) },
                icon = {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = spec.label,
                    )
                },
                label = { Text(spec.label, style = MaterialTheme.typography.labelMedium) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = StitchPalette.Brand,
                        selectedTextColor = StitchPalette.Brand,
                        indicatorColor = StitchPalette.BrandMuted,
                        unselectedIconColor = StitchPalette.Outline,
                        unselectedTextColor = StitchPalette.OnSurfaceVariant,
                    ),
            )
        }
    }
}
