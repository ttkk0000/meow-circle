package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.ttkk0000.meowcircle.kmpapp.ui.MndAppMode

enum class StitchMainTab {
    // Community
    Feed, Circle, Compose, Messages, Profile,
    // Adoption
    AdoptHome, RescueHome, Applications,
    // Trade
    Market, Orders
}

data class NavSpec(
    val tab: StitchMainTab,
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector,
)

fun getTabsForMode(mode: MndAppMode): List<NavSpec> = when(mode) {
    MndAppMode.Community -> listOf(
        NavSpec(StitchMainTab.Feed, "动态", Icons.Outlined.Home, Icons.Filled.Home),
        NavSpec(StitchMainTab.Circle, "圈子", Icons.Outlined.Group, Icons.Filled.Group),
        NavSpec(StitchMainTab.Compose, "发布", Icons.Outlined.AddCircle, Icons.Filled.AddCircle),
        NavSpec(StitchMainTab.Messages, "消息", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
        NavSpec(StitchMainTab.Profile, "我的", Icons.Outlined.Pets, Icons.Filled.Pets),
    )
    MndAppMode.Adoption -> listOf(
        NavSpec(StitchMainTab.AdoptHome, "领养", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
        NavSpec(StitchMainTab.RescueHome, "救助", Icons.Outlined.MedicalServices, Icons.Filled.MedicalServices),
        NavSpec(StitchMainTab.Applications, "申请", Icons.Outlined.Article, Icons.Filled.Article),
        NavSpec(StitchMainTab.Messages, "消息", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
        NavSpec(StitchMainTab.Profile, "我的", Icons.Outlined.Pets, Icons.Filled.Pets),
    )
    MndAppMode.Trade -> listOf(
        NavSpec(StitchMainTab.Market, "市集", Icons.Outlined.Storefront, Icons.Filled.Storefront),
        NavSpec(StitchMainTab.Compose, "发布", Icons.Outlined.AddCircle, Icons.Filled.AddCircle),
        NavSpec(StitchMainTab.Messages, "消息", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
        NavSpec(StitchMainTab.Orders, "订单", Icons.AutoMirrored.Outlined.ListAlt, Icons.AutoMirrored.Filled.ListAlt),
        NavSpec(StitchMainTab.Profile, "我的", Icons.Outlined.Pets, Icons.Filled.Pets),
    )
}

@Composable
fun StitchBottomNav(
    mode: MndAppMode,
    selected: StitchMainTab,
    onSelect: (StitchMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(mode) { getTabsForMode(mode) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { spec ->
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
                            .padding(
                                horizontal = if (sel) 10.dp else 8.dp,
                                vertical = 4.dp
                            )
                    ) {
                        Icon(
                            imageVector = if (sel) spec.filledIcon else spec.outlinedIcon,
                            contentDescription = spec.label,
                            tint = if (sel) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(9.dp))
                        Text(
                            text = spec.label,
                            fontSize = 10.sp,
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
