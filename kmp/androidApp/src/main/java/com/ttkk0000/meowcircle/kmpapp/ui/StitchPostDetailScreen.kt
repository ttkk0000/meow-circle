package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.Comment
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.PostDetailData
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.theme.StitchLoginRef
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl
import com.ttkk0000.meowcircle.humanizeClientFailure
import kotlinx.coroutines.launch

/** MOBILE「帖子详情」：与 Stitch 稿一致：图集分页、作者行、关注、互动统计、评论区块。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StitchPostDetailScreen(
    sdk: MeowCircleSdk,
    postId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val apiBase = BuildConfig.API_BASE_URL
    var detail by remember { mutableStateOf<PostDetailData?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var following by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        loading = true
        err = null
        detail =
            sdk.postDetail(postId).fold(
                onSuccess = { it },
                onFailure = { e ->
                    err =
                        (e as? ApiException)?.message
                            ?: humanizeClientFailure(e, apiBase)
                    null
                },
            )
        loading = false
    }

    LaunchedEffect(detail) {
        following = detail?.followingAuthor ?: false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchLoginRef.Background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = StitchLoginRef.Background,
                        titleContentColor = StitchLoginRef.OnSurface,
                        navigationIconContentColor = StitchLoginRef.OnSurface,
                    ),
            )
        },
    ) { inner ->
        when {
            loading && detail == null ->
                StitchLoadingScreen(
                    title = "详情页加载中",
                    subtitle = "正在加载帖子内容",
                    modifier = Modifier.fillMaxSize().padding(inner),
                )
            err != null && detail == null ->
                Column(
                    Modifier.fillMaxSize().padding(inner).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(err!!, color = StitchPalette.Error, style = MaterialTheme.typography.bodyLarge)
                }
            else -> {
                val d = detail
                if (d == null) {
                    Box(Modifier.fillMaxSize().padding(inner))
                } else {
                    val author = d.author
                    val who = author.nickname.ifBlank { author.username }
                    val imageMedia =
                        d.media.filter { it.kind == "image" || it.mime.startsWith("image/") }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(inner),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (imageMedia.isNotEmpty()) {
                            item {
                                val pagerState = rememberPagerState { imageMedia.size }
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(StitchLoginRef.SurfaceContainerLow),
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                    ) { page ->
                                        val m = imageMedia[page]
                                        val u = resolveMediaUrl(apiBase, m.url)
                                        if (u != null) {
                                            AsyncImage(
                                                model = u,
                                                contentDescription = m.filename,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                    Text(
                                        "${pagerState.currentPage + 1}/${imageMedia.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.45f),
                                                    RoundedCornerShape(8.dp),
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val av = resolveMediaUrl(apiBase, author.avatarUrl.takeIf { it.isNotBlank() })
                                if (av != null) {
                                    AsyncImage(
                                        model = av,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(StitchLoginRef.PrimaryContainer.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            who.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchLoginRef.OnSurface,
                                        )
                                    }
                                }
                                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(
                                        who,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        author.bio.ifBlank { "喵圈用户" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StitchLoginRef.Outline,
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            sdk
                                                .setFollowing(author.id, !following)
                                                .fold(
                                                    onSuccess = { following = it },
                                                    onFailure = { /* ignore */ },
                                                )
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    Text(if (following) "已关注" else "关注")
                                }
                            }
                        }
                        item {
                            Text(d.post.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(d.post.content, style = MaterialTheme.typography.bodyLarge, color = StitchLoginRef.OnSurface)
                            if (d.post.tags.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    d.post.tags.joinToString(" ") { "#$it" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StitchLoginRef.Outline,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "发布于 ${d.post.createdAt.take(16).replace('T', ' ')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StitchLoginRef.Outline,
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PostStatChip(Icons.Outlined.FavoriteBorder, "${d.likeCount}")
                                PostStatChip(Icons.Outlined.ChatBubbleOutline, "${d.comments.size}")
                                PostStatChip(Icons.Outlined.StarOutline, "—")
                                PostStatChip(Icons.Outlined.Share, "分享")
                            }
                        }
                        item {
                            HorizontalDivider(color = StitchLoginRef.SurfaceVariant)
                            Text(
                                "共 ${d.comments.size} 条评论",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(d.comments, key = { it.id }) { c -> CommentRow(apiBase, c) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostStatChip(
    icon: ImageVector,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(22.dp))
        if (label.isNotBlank()) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = StitchLoginRef.OnSurface,
            )
        }
    }
}

@Composable
private fun CommentRow(
    apiBase: String,
    c: Comment,
) {
    val label =
        c.author?.let { it.nickname.ifBlank { it.username } } ?: "用户 ${c.authorId}"
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StitchLoginRef.SurfaceContainerLow)
                .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val av = c.author?.avatarUrl?.takeIf { it.isNotBlank() }?.let { resolveMediaUrl(apiBase, it) }
            if (av != null) {
                AsyncImage(
                    model = av,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(StitchPalette.SurfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label.take(1).uppercase(), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(c.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${c.createdAt.take(16).replace('T', ' ')} · 回复",
                style = MaterialTheme.typography.labelSmall,
                color = StitchLoginRef.Outline,
            )
            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = StitchLoginRef.Outline, modifier = Modifier.size(18.dp))
        }
    }
}
