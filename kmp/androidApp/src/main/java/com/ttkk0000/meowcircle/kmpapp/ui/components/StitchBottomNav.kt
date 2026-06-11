package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector,
)

private val NAV_ITEMS =
    listOf(
        NavSpec(StitchMainTab.Feed, "Feed", Icons.Outlined.Home, Icons.Filled.Home),
        NavSpec(StitchMainTab.Market, "Market", Icons.Outlined.Storefront, Icons.Filled.Storefront),
        NavSpec(StitchMainTab.Messages, "Messages", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
        NavSpec(StitchMainTab.Orders, "Orders", Icons.AutoMirrored.Outlined.ListAlt, Icons.AutoMirrored.Filled.ListAlt),
        NavSpec(StitchMainTab.Profile, "Profile", Icons.Outlined.Pets, Icons.Filled.Pets),
    )

@Composable
fun StitchBottomNav(
    selected: StitchMainTab,
    onSelect: (StitchMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NAV_ITEMS.forEach { spec ->
                val sel = spec.tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(spec.tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) StitchPalette.SurfaceLow else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = if (sel) spec.filledIcon else spec.outlinedIcon,
                            contentDescription = spec.label,
                            tint = if (sel) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = spec.label,
                            fontSize = 9.sp,
                            color = if (sel) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
                            fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

