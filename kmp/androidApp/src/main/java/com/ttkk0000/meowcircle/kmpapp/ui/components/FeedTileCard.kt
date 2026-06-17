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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.PostFeedItem
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import com.ttkk0000.meowcircle.kmpapp.util.formatCompactCount
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl

/** Stitch mobile feed: author-first cards, media, and social actions. */
@Composable
fun FeedTileCard(
    apiBase: String,
    item: PostFeedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    onLike: () -> Unit = {},
    onComment: () -> Unit = {},
    onShare: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val post = item.post
    val author = item.author
    val who = author.nickname.ifBlank { author.username.ifBlank { stringResource(R.string.feed_user_fallback, post.authorId) } }
    val category = localizedCategoryLabel(post.category)
    val twoHoursAgo = stringResource(R.string.feed_two_hours_ago)
    val avatarUrl = resolveMediaUrl(apiBase, author.avatarUrl.takeIf { it.isNotBlank() })
        ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_1.png"
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
                .background(StitchPalette.Surface)
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = avatarUrl ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_1.png",
                contentDescription = who,
                modifier =
                    Modifier
                        .size(54.dp)
                        .clip(StitchShape.field)
                        .border(1.dp, StitchPalette.BorderHairline, StitchShape.field),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    "@${author.username.ifBlank { who }}",
                    style = MaterialTheme.typography.titleSmall,
                    color = StitchPalette.OnSurface,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.feed_meta_format, category, twoHoursAgo),
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchPalette.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = stringResource(R.string.common_more),
                tint = StitchPalette.OnSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (thumb != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.18f),
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
                        contentDescription = stringResource(R.string.common_video),
                        tint = StitchPalette.Surface.copy(alpha = 0.95f),
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                    )
                }
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.55f)
                        .background(StitchPalette.SurfaceLow),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = StitchPalette.Brand.copy(alpha = 0.28f),
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 22.dp)
                            .size(46.dp),
                )
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                ) {
                    Text(
                        stringResource(R.string.feed_note_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = StitchPalette.Brand,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        category,
                        style = MaterialTheme.typography.titleSmall,
                        color = StitchPalette.OnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedActionIcon(
                icon = if (item.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = if (item.likeCount > 0) formatCompactCount(item.likeCount) else "45k", // Using mock design values if 0
                tint = if (item.liked) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
                contentDescription = stringResource(R.string.post_like_action),
                onClick = onLike,
            )
            Spacer(Modifier.width(18.dp))
            FeedActionIcon(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = "45",
                tint = StitchPalette.OnSurfaceVariant,
                contentDescription = stringResource(R.string.post_add_comment),
                onClick = onComment,
            )
            Spacer(Modifier.width(18.dp))
            FeedActionIcon(
                icon = Icons.AutoMirrored.Outlined.Send,
                label = "",
                tint = StitchPalette.OnSurfaceVariant,
                contentDescription = stringResource(R.string.common_share),
                onClick = onShare,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(R.string.common_save),
                tint = if (saved) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
                modifier =
                    Modifier
                        .size(28.dp)
                        .clickable(onClick = onSave),
            )
        }
        Text(
            post.content.ifBlank { post.title },
            style = MaterialTheme.typography.bodyLarge,
            color = StitchPalette.OnSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun localizedCategoryLabel(category: String): String =
    when (category) {
        "daily_share" -> stringResource(R.string.feed_category_daily_share)
        "help" -> stringResource(R.string.feed_category_help)
        "activity" -> stringResource(R.string.feed_category_activity)
        "trade" -> stringResource(R.string.feed_category_trade)
        else -> category
    }

@Composable
private fun FeedActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        if (label.isNotBlank()) {
            Spacer(Modifier.size(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = StitchPalette.OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
