package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette

enum class StitchMainTab {
    Feed,
    Market,
    Messages,
    Orders,
    Profile,
}

private data class NavSpec(
    val tab: StitchMainTab,
    val labelRes: Int,
    val icon: ImageVector,
)

private val NAV_ITEMS =
    listOf(
        NavSpec(StitchMainTab.Feed, R.string.nav_feed, Icons.Outlined.RssFeed),
        NavSpec(StitchMainTab.Market, R.string.nav_market, Icons.Outlined.Storefront),
        NavSpec(StitchMainTab.Messages, R.string.nav_messages, Icons.Outlined.ChatBubbleOutline),
        NavSpec(StitchMainTab.Orders, R.string.nav_orders, Icons.Outlined.ReceiptLong),
        NavSpec(StitchMainTab.Profile, R.string.nav_profile, Icons.Filled.Pets),
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
            val label = stringResource(spec.labelRes)
            NavigationBarItem(
                selected = sel,
                onClick = { onSelect(spec.tab) },
                icon = {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = label,
                    )
                },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
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
