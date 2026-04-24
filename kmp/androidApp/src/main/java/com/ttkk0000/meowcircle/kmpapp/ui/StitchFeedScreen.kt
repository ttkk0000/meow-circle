package com.ttkk0000.meowcircle.kmpapp.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.Conversation
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.PostFeedItem
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import com.ttkk0000.meowcircle.kmpapp.ui.components.FeedTileCard
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchBottomNav
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchMainTab
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchSearchField
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBar
import com.ttkk0000.meowcircle.kmpapp.util.formatCompactCount
import com.ttkk0000.meowcircle.kmpapp.util.formatConversationListTime
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl
import kotlinx.coroutines.launch

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

/** MOBILE「发现」热门圈子 — 与 Stitch 稿文案一致（本地展示，后续可接圈子接口）。 */
private val DISCOVER_CIRCLE_LABELS =
    listOf("布偶猫舍", "新手铲屎官", "橘猫联盟", "猫咪摄影", "领养中心")

private val FEED_PAGE_PADDING = 20.dp
private val FEED_CARD_RADIUS = RoundedCornerShape(20.dp)
private val FEED_SECTION_RADIUS = RoundedCornerShape(24.dp)
private val FEED_CARD_CONTENT_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

private enum class MessageSection {
    Chats,
    LikesFavorites,
    NewFollowers,
    Notifications,
}

@Composable
fun StitchFeedScreen(
    sdk: MeowCircleSdk,
    user: User,
    feedReloadSignal: Int = 0,
    onLogout: () -> Unit,
    onOpenPost: (Long) -> Unit = {},
    onCompose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val apiBase = BuildConfig.API_BASE_URL
    val scope = rememberCoroutineScope()
    var profileUser by remember(user.id) { mutableStateOf(user) }
    var tab by remember { mutableStateOf(StitchMainTab.Home) }
    var filter by remember { mutableStateOf("rec") }
    var q by remember { mutableStateOf("") }
    var selectedCircle by remember { mutableStateOf<String?>(null) }
    var messageQuery by remember { mutableStateOf("") }
    var messageSection by remember { mutableStateOf(MessageSection.Chats) }
    var profileHint by remember { mutableStateOf<String?>(null) }
    var showEditProfile by remember { mutableStateOf(false) }
    var profileSaving by remember { mutableStateOf(false) }
    var profileSaveErr by remember { mutableStateOf<String?>(null) }
    var rawItems by remember { mutableStateOf<List<PostFeedItem>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var conversations by remember { mutableStateOf<List<Conversation>?>(null) }
    var convErr by remember { mutableStateOf<String?>(null) }
    var convLoading by remember { mutableStateOf(false) }
    var profilePosts by remember { mutableStateOf<List<PostFeedItem>?>(null) }
    var profileLoading by remember { mutableStateOf(false) }

    val effectiveFilter =
        when (tab) {
            StitchMainTab.Discover -> "new"
            StitchMainTab.Home -> filter
            else -> filter
        }

    LaunchedEffect(effectiveFilter, tab, feedReloadSignal) {
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

    LaunchedEffect(tab, feedReloadSignal) {
        if (tab != StitchMainTab.Messages) return@LaunchedEffect
        convLoading = true
        convErr = null
        conversations =
            sdk.conversations().fold(
                onSuccess = { it },
                onFailure = { e ->
                    convErr =
                        (e as? ApiException)?.message
                            ?: humanizeClientFailure(e, apiBase)
                    null
                },
            )
        convLoading = false
    }

    LaunchedEffect(tab, profileUser.id, feedReloadSignal) {
        if (tab != StitchMainTab.Profile) return@LaunchedEffect
        profileLoading = true
        profilePosts =
            sdk.feedPosts("new").fold(
                onSuccess = { items -> items.filter { it.author.id == profileUser.id } },
                onFailure = { null },
            )
        profileLoading = false
    }

    val filtered =
        remember(rawItems, q, selectedCircle) {
            val list = rawItems ?: return@remember emptyList()
            val s = q.trim().lowercase()
            val queried =
                if (s.isEmpty()) {
                    list
                } else {
                    list.filter { item ->
                        val p = item.post
                        p.title.lowercase().contains(s) ||
                            p.content.lowercase().contains(s) ||
                            p.tags.any { it.lowercase().contains(s) }
                    }
                }
            val circle = selectedCircle
            if (circle.isNullOrBlank()) return@remember queried
            queried.filter { item ->
                val p = item.post
                p.title.contains(circle) || p.content.contains(circle) || p.tags.any { it.contains(circle) }
            }
        }

    val (barTitle, barSubtitle) =
        when (tab) {
            StitchMainTab.Discover -> "发现" to "看看新鲜事"
            StitchMainTab.Messages -> "消息" to "私信与通知"
            StitchMainTab.Profile -> "我的" to profileUser.nickname.ifBlank { profileUser.username }
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
                        onCompose()
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
                            user = profileUser,
                            title = barTitle,
                            subtitle = barSubtitle,
                            onAvatarPress = { /* already on profile */ },
                            onNotifyPress = {
                                tab = StitchMainTab.Messages
                                messageQuery = ""
                                messageSection = MessageSection.Notifications
                            },
                        )
                        ProfilePanel(
                            apiBase = apiBase,
                            user = profileUser,
                            gridPosts = profilePosts,
                            gridLoading = profileLoading,
                            onLogout = onLogout,
                            onOpenPost = onOpenPost,
                            onEditProfile = { showEditProfile = true },
                            onSettings = { profileHint = "设置面板将在后续版本接入偏好配置接口" },
                            hint = profileHint,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                StitchMainTab.Messages ->
                    Column(Modifier.fillMaxSize()) {
                        StitchTopBar(
                            apiBase = apiBase,
                            user = profileUser,
                            title = barTitle,
                            subtitle = barSubtitle,
                            onAvatarPress = { tab = StitchMainTab.Profile },
                            onNotifyPress = {
                                messageQuery = ""
                                messageSection = MessageSection.Notifications
                            },
                        )
                        MessagesPane(
                            loading = convLoading,
                            err = convErr,
                            items = conversations,
                            query = messageQuery,
                            section = messageSection,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            onShortcut = { label ->
                                messageQuery = ""
                                messageSection =
                                    when (label) {
                                        "赞和收藏" -> MessageSection.LikesFavorites
                                        "新增粉丝" -> MessageSection.NewFollowers
                                        "通知中心" -> MessageSection.Notifications
                                        else -> MessageSection.Chats
                                    }
                            },
                        )
                    }
                else ->
                    Column(Modifier.fillMaxSize()) {
                        StitchTopBar(
                            apiBase = apiBase,
                            user = profileUser,
                            title = barTitle,
                            subtitle = barSubtitle,
                            onAvatarPress = { tab = StitchMainTab.Profile },
                            onNotifyPress = {
                                tab = StitchMainTab.Messages
                                messageQuery = ""
                                messageSection = MessageSection.Notifications
                            },
                        )
                        if (tab == StitchMainTab.Home) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = FEED_PAGE_PADDING, vertical = 8.dp),
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
                                    .padding(horizontal = FEED_PAGE_PADDING),
                        ) {
                            StitchSearchField(
                                value = q,
                                onValueChange = { q = it },
                                placeholder = if (tab == StitchMainTab.Discover) "搜索发现…" else "搜索喵友动态…",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (tab == StitchMainTab.Discover) {
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "热门圈子",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPalette.PrimaryDark,
                                    )
                                    Text(
                                        "查看更多",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = StitchPalette.Brand,
                                        modifier =
                                            Modifier.clickable {
                                                selectedCircle = null
                                            },
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    DISCOVER_CIRCLE_LABELS.forEach { label ->
                                        Surface(
                                            shape = RoundedCornerShape(24.dp),
                                            color = StitchPalette.SecondaryContainer.copy(alpha = 0.65f),
                                            modifier =
                                                Modifier.clickable {
                                                    selectedCircle = if (selectedCircle == label) null else label
                                                },
                                        ) {
                                            Text(
                                                label,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = StitchPalette.Secondary,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    "发现日常",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchPalette.PrimaryDark,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (tab == StitchMainTab.Home) {
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .shadow(
                                                elevation = StitchShadows.cardAmbientY,
                                                shape = FEED_SECTION_RADIUS,
                                                ambientColor = StitchShadows.cardAmbientColor,
                                                spotColor = StitchShadows.cardAmbientColor,
                                            )
                                            .border(
                                                1.dp,
                                                StitchPalette.Brand.copy(alpha = 0.15f),
                                                FEED_SECTION_RADIUS,
                                            ),
                                    shape = FEED_SECTION_RADIUS,
                                    color = StitchPalette.SecondaryContainer.copy(alpha = 0.55f),
                                ) {
                                    Column(Modifier.padding(FEED_PAGE_PADDING)) {
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
                                        contentPadding = PaddingValues(start = FEED_PAGE_PADDING, end = FEED_PAGE_PADDING, top = 4.dp, bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        items(
                                            items = filtered,
                                            key = { it.post.id },
                                        ) { item ->
                                            FeedTileCard(
                                                apiBase = apiBase,
                                                item = item,
                                                onClick = { onOpenPost(item.post.id) },
                                            )
                                        }
                                    }
                            }
                        }
                    }
            }
        }
        if (showEditProfile) {
            EditProfileDialog(
                user = profileUser,
                saving = profileSaving,
                error = profileSaveErr,
                onDismiss = { showEditProfile = false },
                onSave = { nickname, bio, avatarUrl ->
                    profileSaveErr = null
                    scope.launch {
                        profileSaving = true
                        sdk
                            .updateMe(
                                nickname = nickname,
                                bio = bio,
                                avatarUrl = avatarUrl,
                            ).fold(
                                onSuccess = { updated ->
                                    profileUser = updated
                                    profileHint = "资料已更新"
                                    showEditProfile = false
                                },
                                onFailure = { e ->
                                    profileSaveErr =
                                        (e as? ApiException)?.message
                                            ?: humanizeClientFailure(e, apiBase)
                                },
                            )
                        profileSaving = false
                    }
                },
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    user: User,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (nickname: String, bio: String, avatarUrl: String) -> Unit,
) {
    var nickname by remember(user.id, user.nickname) { mutableStateOf(user.nickname) }
    var bio by remember(user.id, user.bio) { mutableStateOf(user.bio) }
    var avatarUrl by remember(user.id, user.avatarUrl) { mutableStateOf(user.avatarUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    singleLine = true,
                    label = { Text("昵称") },
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    minLines = 2,
                    maxLines = 4,
                    label = { Text("简介") },
                )
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    singleLine = true,
                    label = { Text("头像 URL") },
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = StitchPalette.Error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("取消") }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    onSave(nickname.trim().ifBlank { user.username }, bio.trim(), avatarUrl.trim())
                },
            ) { Text(if (saving) "保存中…" else "保存") }
        },
    )
}

@Composable
private fun ProfilePanel(
    apiBase: String,
    user: User,
    gridPosts: List<PostFeedItem>?,
    gridLoading: Boolean,
    onLogout: () -> Unit,
    onOpenPost: (Long) -> Unit,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    val avatarUrl = resolveMediaUrl(apiBase, user.avatarUrl.takeIf { it.isNotBlank() })
    val posts = gridPosts.orEmpty()
    val likesTotal = posts.sumOf { it.likeCount }
    Column(
        modifier =
            modifier
                .padding(horizontal = FEED_PAGE_PADDING, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = StitchPalette.Brand,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    user.nickname.ifBlank { user.username },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = StitchPalette.PrimaryDark,
                )
                Text(
                    user.bio.ifBlank { "分享主子的每一刻" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = StitchShadows.cardAmbientY,
                        shape = FEED_SECTION_RADIUS,
                        ambientColor = StitchShadows.cardAmbientColor,
                        spotColor = StitchShadows.cardAmbientColor,
                    )
                    .clip(FEED_SECTION_RADIUS)
                    .background(StitchPalette.Surface)
                    .border(1.dp, StitchPalette.BorderHairline, FEED_SECTION_RADIUS)
                    .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProfileStatCell(formatCompactCount(likesTotal), "获赞")
            ProfileStatCell("—", "关注")
            ProfileStatCell("—", "粉丝")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = FEED_CARD_RADIUS,
                color = StitchPalette.Brand.copy(alpha = 0.12f),
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable {
                            onEditProfile()
                        },
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "编辑资料",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchPalette.Brand,
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = StitchPalette.SurfaceContainer,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clickable {
                            onSettings()
                        },
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Settings, contentDescription = "设置", tint = StitchPalette.OnSurface)
                }
            }
        }
        if (!hint.isNullOrBlank()) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = StitchPalette.OnSurfaceVariant,
            )
        }
        Text(
            "动态",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = StitchPalette.PrimaryDark,
        )
        when {
            gridLoading ->
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPalette.Brand)
                }
            posts.isEmpty() ->
                Text(
                    "发布动态后会出现在这里",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                )
            else ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    posts.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEach { item ->
                                ProfileGridCell(
                                    apiBase = apiBase,
                                    item = item,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .clip(FEED_CARD_RADIUS)
                                            .clickable { onOpenPost(item.post.id) },
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
        }
        HorizontalDivider(color = StitchPalette.OutlineVariant)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(FEED_SECTION_RADIUS)
                    .background(StitchPalette.Surface)
                    .border(1.dp, StitchPalette.BorderHairline, FEED_SECTION_RADIUS)
                    .clickable(onClick = onLogout)
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "头像",
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, StitchPalette.BorderHairline, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StitchPalette.Brand),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user.nickname.take(1).ifBlank { user.username.take(1) }.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    user.nickname.ifBlank { user.username },
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchPalette.OnSurface,
                )
                Text(
                    "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = StitchPalette.Outline)
        }
        Text(
            "退出登录",
            style = MaterialTheme.typography.titleMedium,
            color = StitchPalette.Brand,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onLogout)
                    .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun ProfileStatCell(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = StitchPalette.OnSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = StitchPalette.OnSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileGridCell(
    apiBase: String,
    item: PostFeedItem,
    modifier: Modifier = Modifier,
) {
    val thumb =
        resolveMediaUrl(apiBase, item.firstMedia?.url)?.takeIf {
            item.firstMedia?.kind == "image" || item.firstMedia?.mime?.startsWith("image/") == true
        }
    Box(
        modifier =
            modifier
                .shadow(
                    elevation = StitchShadows.cardAmbientY,
                    shape = FEED_CARD_RADIUS,
                    ambientColor = StitchShadows.cardAmbientColor,
                    spotColor = StitchShadows.cardAmbientColor,
                )
                .background(StitchPalette.SurfaceContainer),
    ) {
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    item.post.title.take(2),
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(4.dp))
            Text(
                formatCompactCount(item.likeCount),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun MessagesPane(
    loading: Boolean,
    err: String?,
    items: List<Conversation>?,
    query: String,
    section: MessageSection,
    onShortcut: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredItems =
        remember(items, query) {
            val all = items.orEmpty()
            val key = query.trim()
            if (key.isBlank()) return@remember all
            all.filter { convo ->
                val title = convo.peer.nickname.ifBlank { convo.peer.username }
                title.contains(key) || convo.lastMessage.contains(key)
            }
        }
    when {
        loading && items == null ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StitchPalette.Brand)
            }
        err != null && items == null ->
            Column(
                modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(err, color = StitchPalette.Error, style = MaterialTheme.typography.bodyLarge)
            }
        else ->
            Column(modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FEED_PAGE_PADDING, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MessageShortcutRow(
                        icon = Icons.Filled.Favorite,
                        title = "赞和收藏",
                        subtitle = null,
                        onClick = { onShortcut("赞和收藏") },
                    )
                    MessageShortcutRow(
                        icon = Icons.Outlined.PersonAdd,
                        title = "新增粉丝",
                        subtitle = "12",
                        onClick = { onShortcut("新增粉丝") },
                    )
                    MessageShortcutRow(
                        icon = Icons.Filled.Campaign,
                        title = "通知中心",
                        subtitle = null,
                        onClick = { onShortcut("通知中心") },
                    )
                }
                Text(
                    when (section) {
                        MessageSection.Chats -> "私信内容"
                        MessageSection.LikesFavorites -> "赞和收藏"
                        MessageSection.NewFollowers -> "新增粉丝"
                        MessageSection.Notifications -> "通知中心"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchPalette.PrimaryDark,
                    modifier = Modifier.padding(horizontal = FEED_PAGE_PADDING, vertical = 4.dp),
                )
                if (section != MessageSection.Chats) {
                    val rows =
                        when (section) {
                            MessageSection.LikesFavorites ->
                                listOf(
                                    "小鱼干狂魔 赞了你的动态",
                                    "拍照达喵 收藏了你的帖子",
                                    "懒洋洋的喵 赞了你的评论",
                                )
                            MessageSection.NewFollowers ->
                                listOf(
                                    "铲屎官阿强 关注了你",
                                    "新手喵友_07 关注了你",
                                    "布偶猫舍官方 关注了你",
                                )
                            MessageSection.Notifications ->
                                listOf(
                                    "摄影大赛报名将于今晚截止",
                                    "你发布的动态已通过审核",
                                    "系统维护通知：凌晨 2:00~2:30",
                                )
                            MessageSection.Chats -> emptyList()
                        }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = FEED_PAGE_PADDING, top = 8.dp, end = FEED_PAGE_PADDING, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(rows) { row ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = FEED_CARD_RADIUS,
                                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Text(
                                    row,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = StitchPalette.OnSurface,
                                )
                            }
                        }
                    }
                } else if (filteredItems.isEmpty()) {
                    PlaceholderPane(
                        headline = if (items.isNullOrEmpty()) "暂无会话" else "未找到匹配会话",
                        body = if (items.isNullOrEmpty()) "还没有私信。有数据后列表会出现在这里。" else "尝试点击上方快捷入口或清空筛选词。",
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = FEED_PAGE_PADDING, top = 8.dp, end = FEED_PAGE_PADDING, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredItems, key = { it.peer.id }) { c ->
                            val peer = c.peer
                            val title = peer.nickname.ifBlank { peer.username }
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = { onShortcut(title) }),
                                shape = FEED_CARD_RADIUS,
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = StitchPalette.Surface,
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            c.lastMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StitchPalette.OnSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            formatConversationListTime(c.updatedAt),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = StitchPalette.OnSurfaceVariant,
                                        )
                                        if (c.unreadCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = StitchPalette.Brand,
                                                modifier = Modifier.size(22.dp),
                                            ) {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "${c.unreadCount.coerceAtMost(9)}${if (c.unreadCount > 9) "+" else ""}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
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
            }
    }
}

@Composable
private fun MessageShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(FEED_CARD_RADIUS)
                .background(StitchPalette.Surface)
                .border(1.dp, StitchPalette.BorderHairline, FEED_CARD_RADIUS)
                .clickable(onClick = onClick)
                .padding(FEED_CARD_CONTENT_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(26.dp))
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = StitchPalette.OnSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = StitchPalette.Outline,
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
