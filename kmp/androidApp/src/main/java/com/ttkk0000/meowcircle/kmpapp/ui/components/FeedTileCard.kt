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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Share
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
                .shadow(
                    elevation = StitchShadows.cardAmbientY,
                    shape = StitchShape.cardFeed,
                    ambientColor = StitchShadows.cardAmbientColor,
                    spotColor = StitchShadows.cardAmbientColor,
                )
                .clip(StitchShape.cardFeed)
                .background(StitchPalette.Surface)
                .border(1.dp, StitchPalette.BorderHairline, StitchShape.cardFeed)
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
        Text(
            post.content.ifBlank { post.title },
            style = MaterialTheme.typography.bodyLarge,
            color = StitchPalette.OnSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        if (thumb != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(1.18f)
                        .clip(StitchShape.cardFeed),
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
                        .padding(horizontal = 16.dp)
                        .aspectRatio(1.55f)
                        .clip(StitchShape.cardFeed)
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
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedActionIcon(Icons.Outlined.FavoriteBorder, formatCompactCount(item.likeCount), StitchPalette.Brand)
            Spacer(Modifier.width(18.dp))
            FeedActionIcon(Icons.Outlined.ChatBubbleOutline, "42", StitchPalette.OnSurfaceVariant)
            Spacer(Modifier.width(18.dp))
            FeedActionIcon(Icons.Outlined.Share, "", StitchPalette.OnSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(R.string.common_save),
                tint = StitchPalette.OnSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
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
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
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
