package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBar
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.User

data class ModeOption(
    val mode: MndAppMode,
    val title: String,
    val subtitle: String,
    val navPreview: String,
    val iconStr: String,
    val isRecommended: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchModeSelectScreen(
    apiBase: String,
    user: User,
    onModeSelected: (MndAppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        ModeOption(
            mode = MndAppMode.Community,
            title = "宠物交流",
            subtitle = "日常分享、互助提问、周边活动组织。适合绝大多数养宠人和爱宠人士。",
            navPreview = "动态 / 圈子 / 发布 / 消息 / 我的",
            iconStr = "C",
            isRecommended = true
        ),
        ModeOption(
            mode = MndAppMode.Adoption,
            title = "宠物收养",
            subtitle = "面向待领养、救助进展、申请管理和救助方沟通。安全第一，无交易语义。",
            navPreview = "领养 / 救助 / 申请 / 消息 / 我的",
            iconStr = "A"
        ),
        ModeOption(
            mode = MndAppMode.Trade,
            title = "宠物交易",
            subtitle = "专门用于宠物相关的用品交易、二手市集和商业服务。",
            navPreview = "市集 / 发布 / 消息 / 订单 / 我的",
            iconStr = "T"
        )
    )

    Scaffold(
        topBar = {
            StitchTopBar(
                apiBase = apiBase,
                user = user,
                title = "选择功能主题",
                onAvatarPress = {},
                onNotifyPress = {},
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(StitchPalette.Canvas),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "M&D 拥有多个独立的功能主题，请根据您当前的需求进行选择。您可以随时在设置中切换主题。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(modes) { option ->
                ModeCard(option = option, onSelect = { onModeSelected(option.mode) })
            }
        }
    }
}

@Composable
private fun ModeCard(option: ModeOption, onSelect: () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = StitchPalette.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(StitchShape.chip)
                        .background(StitchPalette.Brand.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.iconStr,
                        color = StitchPalette.Brand,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchPalette.OnSurface
                        )
                        if (option.isRecommended) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = StitchPalette.Brand,
                                shape = StitchShape.pill
                            ) {
                                Text(
                                    text = "推荐",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = StitchPalette.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = StitchPalette.OnSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = StitchPalette.Outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "底部导航：",
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchPalette.Stone500
                )
                Text(
                    text = option.navPreview,
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchPalette.Brand,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
