package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette

enum class StitchMainTab {
    Home,
    Market,
    Messages,
    Orders,
    Profile,
}

private data class NavSpec(
    val tab: StitchMainTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val NAV_ITEMS =
    listOf(
        NavSpec(StitchMainTab.Home, "首页", Icons.Outlined.Home),
        NavSpec(StitchMainTab.Market, "市集", Icons.Outlined.ShoppingCart),
        NavSpec(StitchMainTab.Messages, "消息", Icons.Outlined.MailOutline),
        NavSpec(StitchMainTab.Orders, "订单", Icons.Outlined.Receipt),
        NavSpec(StitchMainTab.Profile, "我的", Icons.Outlined.AccountCircle),
    )

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StitchBottomNav(
    selected: StitchMainTab,
    onSelect: (StitchMainTab) -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<StitchMainTab, Int> = emptyMap(),
) {
    NavigationBar(
        modifier = modifier,
        containerColor = StitchPalette.Surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        NAV_ITEMS.forEach { spec ->
            val sel = spec.tab == selected
            val badgeCount = badges[spec.tab]?.coerceAtLeast(0) ?: 0
            NavigationBarItem(
                selected = sel,
                onClick = { onSelect(spec.tab) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge(
                                    containerColor = StitchPalette.Error,
                                    contentColor = Color.White,
                                ) {
                                    Text(formatBadgeCount(badgeCount), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
                    ) {
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
                        indicatorColor = StitchPalette.BrandMuted,
                        unselectedIconColor = StitchPalette.Outline,
                        unselectedTextColor = StitchPalette.OnSurfaceVariant,
                    ),
            )
        }
    }
}

private fun formatBadgeCount(value: Int): String =
    if (value > 99) "99+" else value.toString()
