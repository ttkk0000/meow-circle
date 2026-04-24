package com.ttkk0000.meowcircle.kmpapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.PostFeedItem
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import com.ttkk0000.meowcircle.kmpapp.ui.components.FeedTileCard
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchBottomNav
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchMainTab
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchSearchField
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBar
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl

private data class FeedFilter(
    val key: String,
    val label: String,
)

private val FEED_FILTERS =
    listOf(
        FeedFilter("rec", "推荐"),
        FeedFilter("new", "最新"),
        FeedFilter("follow", "关注"),
    )

@Composable
fun StitchFeedScreen(
    sdk: MeowCircleSdk,
    user: User,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val apiBase = BuildConfig.API_BASE_URL
    var tab by remember { mutableStateOf(StitchMainTab.Home) }
    var filter by remember { mutableStateOf("rec") }
    var q by remember { mutableStateOf("") }
    var rawItems by remember { mutableStateOf<List<PostFeedItem>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    val effectiveFilter =
        when (tab) {
            StitchMainTab.Discover -> "new"
            StitchMainTab.Home -> filter
            else -> filter
        }

    LaunchedEffect(effectiveFilter, tab) {
        if (tab != StitchMainTab.Home && tab != StitchMainTab.Discover) return@LaunchedEffect
        loading = true
        err = null
        rawItems =
            sdk.feedPosts(effectiveFilter).fold(
                onSuccess = { it },
                onFailure = { e ->
                    err = (e as? ApiException)?.message ?: e.message ?: "加载失败"
                    null
                },
            )
        loading = false
    }

    val filtered =
        remember(rawItems, q) {
            val list = rawItems ?: return@remember emptyList()
            val s = q.trim().lowercase()
            if (s.isEmpty()) return@remember list
            list.filter { item ->
                val p = item.post
                p.title.lowercase().contains(s) ||
                    p.content.lowercase().contains(s) ||
                    p.tags.any { it.lowercase().contains(s) }
            }
        }

    val (barTitle, barSubtitle) =
        when (tab) {
            StitchMainTab.Discover -> "发现" to "看看新鲜事"
            StitchMainTab.Messages -> "消息" to "私信与通知"
            StitchMainTab.Profile -> "我的" to user.nickname.ifBlank { user.username }
            else -> "Kitty Circle" to "喵友圈 · 萌友社区"
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchPalette.Canvas,
        bottomBar = {
            StitchBottomNav(
                selected = tab,
                onSelect = { t ->
                    if (t == StitchMainTab.Compose) {
                        Toast.makeText(ctx, "发布动态（演示）", Toast.LENGTH_SHORT).show()
                    } else {
                        tab = t
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(inner),
        ) {
            when (tab) {
                StitchMainTab.Profile ->
                    Column(Modifier.fillMaxSize()) {
                        StitchTopBar(
                            apiBase = apiBase,
                            user = user,
                            title = barTitle,
                            subtitle = barSubtitle,
                            onAvatarPress = { /* already on profile */ },
                            onNotifyPress = {
                                Toast.makeText(ctx, "通知中心（演示）", Toast.LENGTH_SHORT).show()
                            },
                        )
                        ProfilePanel(
                            apiBase = apiBase,
                            user = user,
                            onLogout = onLogout,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                StitchMainTab.Messages ->
                    Column(Modifier.fillMaxSize()) {
                        StitchTopBar(
                            apiBase = apiBase,
                            user = user,
                            title = barTitle,
                            subtitle = barSubtitle,
                            onAvatarPress = { tab = StitchMainTab.Profile },
                            onNotifyPress = {
                                Toast.makeText(ctx, "通知中心（演示）", Toast.LENGTH_SHORT).show()
                            },
                        )
                        PlaceholderPane(
                            headline = "暂无会话",
                            body = "消息功能接入后，会在这里显示私信列表。",
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                else ->
                    Column(Modifier.fillMaxSize()) {
                        StitchTopBar(
                            apiBase = apiBase,
                            user = user,
                            title = barTitle,
                            subtitle = barSubtitle,
                            onAvatarPress = { tab = StitchMainTab.Profile },
                            onNotifyPress = {
                                Toast.makeText(ctx, "通知中心（演示）", Toast.LENGTH_SHORT).show()
                            },
                        )
                        if (tab == StitchMainTab.Home) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FEED_FILTERS.forEach { f ->
                                    val selected = filter == f.key
                                    FilterChip(
                                        selected = selected,
                                        onClick = { filter = f.key },
                                        label = { Text(f.label) },
                                        border =
                                            FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selected,
                                                borderColor = StitchPalette.BorderHairline,
                                                selectedBorderColor = Color.Transparent,
                                            ),
                                        colors =
                                            FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = StitchPalette.Brand,
                                                selectedLabelColor = Color.White,
                                                containerColor = StitchPalette.Surface,
                                                labelColor = StitchPalette.OnSurfaceVariant,
                                            ),
                                        modifier =
                                            Modifier.then(
                                                if (selected) {
                                                    Modifier.shadow(
                                                        elevation = StitchShadows.ctaGlowY,
                                                        shape = CircleShape,
                                                        ambientColor = StitchShadows.ctaGlowColor,
                                                        spotColor = StitchShadows.ctaGlowColor,
                                                    )
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                        }
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                        ) {
                            StitchSearchField(
                                value = q,
                                onValueChange = { q = it },
                                placeholder = if (tab == StitchMainTab.Discover) "搜索发现…" else "搜索喵友动态…",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (tab == StitchMainTab.Home) {
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .shadow(
                                                elevation = StitchShadows.cardAmbientY,
                                                shape = RoundedCornerShape(24.dp),
                                                ambientColor = StitchShadows.cardAmbientColor,
                                                spotColor = StitchShadows.cardAmbientColor,
                                            )
                                            .border(
                                                1.dp,
                                                StitchPalette.Brand.copy(alpha = 0.15f),
                                                RoundedCornerShape(24.dp),
                                            ),
                                    shape = RoundedCornerShape(24.dp),
                                    color = StitchPalette.SecondaryContainer.copy(alpha = 0.55f),
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        Text(
                                            "HOT EVENT",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = StitchPalette.Brand,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "咪友圈摄影大赛",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchPalette.PrimaryDark,
                                        )
                                        Text(
                                            "晒出你的心动瞬间，赢取猫罐头大礼包",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StitchPalette.OnSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Box(Modifier.weight(1f)) {
                            when {
                                loading && rawItems == null ->
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = StitchPalette.Brand)
                                    }
                                err != null && rawItems == null ->
                                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Text(err!!, color = StitchPalette.Error, style = MaterialTheme.typography.bodyLarge)
                                    }
                                else ->
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        items(
                                            items = filtered,
                                            key = { it.post.id },
                                        ) { item ->
                                            FeedTileCard(
                                                apiBase = apiBase,
                                                item = item,
                                                onClick = {
                                                    Toast
                                                        .makeText(ctx, item.post.title, Toast.LENGTH_SHORT)
                                                        .show()
                                                },
                                            )
                                        }
                                    }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun ProfilePanel(
    apiBase: String,
    user: User,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarUrl = resolveMediaUrl(apiBase, user.avatarUrl.takeIf { it.isNotBlank() })
    Column(
        modifier =
            modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = StitchShadows.cardAmbientY,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = StitchShadows.cardAmbientColor,
                        spotColor = StitchShadows.cardAmbientColor,
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(StitchPalette.Surface)
                    .border(1.dp, StitchPalette.BorderHairline, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "头像",
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(1.dp, StitchPalette.BorderHairline, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(StitchPalette.Brand),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user.nickname.take(1).ifBlank { user.username.take(1) }.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    user.nickname.ifBlank { user.username },
                    style = MaterialTheme.typography.titleLarge,
                    color = StitchPalette.OnSurface,
                )
                Text(
                    "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = StitchPalette.Outline,
            )
        }
        if (user.bio.isNotBlank()) {
            Text(
                user.bio,
                style = MaterialTheme.typography.bodyLarge,
                color = StitchPalette.OnSurfaceVariant,
            )
        }
        HorizontalDivider(color = StitchPalette.OutlineVariant)
        Text(
            "退出登录",
            style = MaterialTheme.typography.titleMedium,
            color = StitchPalette.Brand,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onLogout)
                    .padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun PlaceholderPane(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(headline, style = MaterialTheme.typography.headlineMedium, color = StitchPalette.OnSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = StitchPalette.OnSurfaceVariant,
        )
    }
}
