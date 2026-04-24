package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.PostFeedItem
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import com.ttkk0000.meowcircle.kmpapp.util.categoryLabel
import com.ttkk0000.meowcircle.kmpapp.util.formatCompactCount
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl

/** Stitch 移动首页：单列卡片，大图 + 标题 + 作者与点赞。 */
@Composable
fun FeedTileCard(
    apiBase: String,
    item: PostFeedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val post = item.post
    val author = item.author
    val who = author.nickname.ifBlank { author.username.ifBlank { "用户 ${post.authorId}" } }
    val thumb =
        resolveMediaUrl(apiBase, item.firstMedia?.url)?.takeIf {
            item.firstMedia?.kind == "image" ||
                item.firstMedia?.mime?.startsWith("image/") == true ||
                item.firstMedia?.kind == "video" ||
                item.firstMedia?.mime?.startsWith("video/") == true
        }
    val isVideo =
        item.firstMedia?.kind == "video" || (item.firstMedia?.mime?.startsWith("video/") == true)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = StitchShadows.cardAmbientY,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = StitchShadows.cardAmbientColor,
                    spotColor = StitchShadows.cardAmbientColor,
                )
                .clip(RoundedCornerShape(28.dp))
                .background(StitchPalette.Surface)
                .border(1.dp, StitchPalette.BorderHairline, RoundedCornerShape(28.dp))
                .clickable(onClick = onClick),
    ) {
        if (thumb != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            ) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (isVideo) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = "视频",
                        tint = StitchPalette.Surface.copy(alpha = 0.95f),
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                categoryLabel(post.category),
                style = MaterialTheme.typography.labelLarge,
                color = StitchPalette.Brand,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchPalette.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (post.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    post.tags.take(3).joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    who,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = StitchPalette.Brand,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    formatCompactCount(item.likeCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchPalette.OnSurface,
                )
            }
        }
    }
}
