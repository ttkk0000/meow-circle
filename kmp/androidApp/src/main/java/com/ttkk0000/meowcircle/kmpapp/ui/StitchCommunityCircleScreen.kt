package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchSearchField
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBar
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBarLeading
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBarTrailing

private data class CommunityCircle(
    val name: String,
    val category: String,
    val description: String,
    val members: String,
    val imageNumber: Int,
)

@Composable
internal fun CommunityCircleScreen(
    apiBase: String,
    user: User,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    onCompose: () -> Unit,
    onOpenFeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    val circles =
        remember {
            listOf(
                CommunityCircle("新手养猫互助", "日常", "从接猫、换粮到居家适应，和同城猫友一起讨论。", "12.8k 位成员", 4),
                CommunityCircle("晒猫片刻", "晒猫", "记录每一只猫的睡姿、表情和今天的小确幸。", "8.6k 位成员", 5),
                CommunityCircle("科学喂养", "健康", "饮食、体重与日常照护的经验交流。", "6.4k 位成员", 6),
                CommunityCircle("周末猫友会", "同城", "上海同城的线下活动、寄养互助和公益信息。", "3.1k 位成员", 7),
            )
        }
    val visibleCircles =
        remember(circles, selectedCategory, query) {
            circles.filter { circle ->
                (selectedCategory == "全部" || circle.category == selectedCategory) &&
                    (query.isBlank() ||
                        circle.name.contains(query, ignoreCase = true) ||
                        circle.description.contains(query, ignoreCase = true))
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(StitchPalette.Canvas),
    ) {
        StitchTopBar(
            apiBase = apiBase,
            user = user,
            title = "猫咪圈子",
            leading = StitchTopBarLeading.Menu,
            trailing = StitchTopBarTrailing.Bell,
            onAvatarPress = onAvatarPress,
            onNotifyPress = onNotifyPress,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(horizontal = 16.dp, vertical = 14.dp)),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StitchSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索圈子、话题或活动",
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("全部", "日常", "晒猫", "健康", "同城").forEach { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontWeight = FontWeight.SemiBold) },
                        shape = StitchShape.pill,
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StitchPalette.Brand,
                                selectedLabelColor = StitchPalette.Surface,
                                containerColor = StitchPalette.Surface,
                                labelColor = StitchPalette.OnSurfaceVariant,
                            ),
                    )
                }
            }

            Surface(
                color = StitchPalette.SecondaryContainer.copy(alpha = 0.35f),
                shape = StitchShape.cardFeed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = StitchPalette.Surface,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = null,
                            tint = StitchPalette.Secondary,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "本周话题：新猫到家第一周",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchPalette.OnSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "分享适应记录，向有经验的猫友提问。",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchPalette.OnSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = StitchPalette.Brand,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "热门圈子",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchPalette.OnSurface,
                )
                Text(
                    "${visibleCircles.size} 个",
                    style = MaterialTheme.typography.labelLarge,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }

            visibleCircles.forEach { circle ->
                CommunityCircleCard(
                    circle = circle,
                    apiBase = apiBase,
                    onClick = onOpenFeed,
                )
            }

            if (visibleCircles.isEmpty()) {
                Surface(
                    color = StitchPalette.Surface,
                    shape = StitchShape.cardFeed,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "暂时没有匹配的圈子，换个关键词试试。",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchPalette.OnSurfaceVariant,
                    )
                }
            }

            Surface(
                color = StitchPalette.Surface,
                shape = StitchShape.cardFeed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = StitchPalette.Brand,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "找不到合适的圈子？",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchPalette.OnSurface,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "先发布一条动态，邀请附近的猫友一起交流。",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchPalette.OnSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onCompose,
                        shape = StitchShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("发布动态", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenFeed,
                        shape = StitchShape.pill,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("查看全部动态", color = StitchPalette.OnSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityCircleCard(
    circle: CommunityCircle,
    apiBase: String,
    onClick: () -> Unit,
) {
    Surface(
        color = StitchPalette.Surface,
        shape = StitchShape.cardFeed,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = "${apiBase.removeSuffix("/")}/mock-images/mock_image_${circle.imageNumber}.png",
                contentDescription = circle.name,
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(StitchShape.cardFeed),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    circle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchPalette.OnSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    circle.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchPalette.OnSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    circle.members,
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchPalette.Brand,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = null,
                tint = StitchPalette.OnSurfaceVariant,
            )
        }
    }
}
