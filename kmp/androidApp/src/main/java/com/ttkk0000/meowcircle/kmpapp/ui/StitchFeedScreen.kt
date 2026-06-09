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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.Conversation
import com.ttkk0000.meowcircle.Listing
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.Post
import com.ttkk0000.meowcircle.PostFeedItem
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import com.ttkk0000.meowcircle.kmpapp.theme.MeowStitchTheme
import com.ttkk0000.meowcircle.kmpapp.theme.MeowTheme
import com.ttkk0000.meowcircle.kmpapp.ui.components.FeedTileCard
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchBottomNav
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchMainTab
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchSearchField
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBar
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchFab
import com.ttkk0000.meowcircle.kmpapp.util.formatCompactCount
import com.ttkk0000.meowcircle.kmpapp.util.formatConversationListTime
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

private data class FeedFilter(
    val key: String,
    val labelRes: Int,
)

private data class HomeSignal(
    val label: String,
    val value: String,
    val tone: Color,
)

private data class ProfileBackgroundOption(
    val key: String,
    val labelRes: Int,
    val noteRes: Int,
)

private val FEED_FILTERS =
    listOf(
        FeedFilter("follow", R.string.feed_filter_following),
        FeedFilter("new", R.string.feed_filter_nearby),
        FeedFilter("rec", R.string.feed_filter_popular),
    )

/** M&D「发现」热门圈子，本地展示，后续可接圈子接口。 */
private val DISCOVER_CIRCLE_LABELS =
    listOf("猫猫新手村", "橘猫联盟", "黑猫部", "猫咪摄影", "领养中心")

private val PROFILE_BACKGROUND_OPTIONS =
    listOf(
        ProfileBackgroundOption("picnic", R.string.profile_bg_picnic, R.string.profile_bg_picnic_note),
        ProfileBackgroundOption("desk", R.string.profile_bg_desk, R.string.profile_bg_desk_note),
        ProfileBackgroundOption("arcade", R.string.profile_bg_arcade, R.string.profile_bg_arcade_note),
        ProfileBackgroundOption("garden", R.string.profile_bg_garden, R.string.profile_bg_garden_note),
    )

private val FEED_PAGE_PADDING = 20.dp
private val FEED_CARD_RADIUS = StitchShape.cardFeed
private val FEED_SECTION_RADIUS = StitchShape.cardFeed
private val FEED_CARD_CONTENT_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

private fun feedErrorMessage(
    e: Throwable,
    apiBase: String,
): String {
    val raw = (e as? ApiException)?.message ?: e.message.orEmpty()
    val isHtmlOrProxy = raw.contains("<html", ignoreCase = true) ||
            raw.contains("[Fiddler]", ignoreCase = true) ||
            raw.contains("connection refused", ignoreCase = true) ||
            raw.contains("connectionrefused", ignoreCase = true) ||
            raw.contains("积极拒绝", ignoreCase = true)
    return when {
        raw.contains("Expected JsonArray", ignoreCase = true) ||
            raw.contains("$.items", ignoreCase = true) ||
            raw.contains("JsonNull", ignoreCase = true) ->
            "动态列表暂时没有返回内容。可以重试，或先看离线演示。"
        e is ApiException && raw.isNotBlank() && !isHtmlOrProxy -> raw
        else -> humanizeClientFailure(e, apiBase)
    }
}

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
    onThemeChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var apiBase by remember { mutableStateOf(sdk.baseUrl) }
    val scope = rememberCoroutineScope()
    var profileUser by remember(user.id) { mutableStateOf(user) }
    var tab by remember { mutableStateOf(StitchMainTab.Feed) }
    var filter by remember { mutableStateOf("rec") }
    var q by remember { mutableStateOf("") }
    var selectedCircle by remember { mutableStateOf<String?>(null) }
    var messageQuery by remember { mutableStateOf("") }
    var messageSection by remember { mutableStateOf(MessageSection.Chats) }
    var profileHint by remember { mutableStateOf<String?>(null) }
    var profileBackground by remember { mutableStateOf(sdk.sessionStore().getProfileBackground()) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var profileSaving by remember { mutableStateOf(false) }
    var profileSaveErr by remember { mutableStateOf<String?>(null) }
    var rawItems by remember { mutableStateOf<List<PostFeedItem>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var conversations by remember { mutableStateOf<List<Conversation>?>(null) }
    var convErr by remember { mutableStateOf<String?>(null) }
    var convLoading by remember { mutableStateOf(false) }
    var listings by remember { mutableStateOf<List<Listing>?>(null) }
    var listingsErr by remember { mutableStateOf<String?>(null) }
    var listingsLoading by remember { mutableStateOf(false) }
    var profilePosts by remember { mutableStateOf<List<PostFeedItem>?>(null) }
    var profileLoading by remember { mutableStateOf(false) }
    var mockMode by remember { mutableStateOf(false) }
    var feedRetrySignal by remember { mutableStateOf(0) }
    var currentGlobalTheme by remember { mutableStateOf(sdk.getTheme()) }

    val activeTheme = remember(tab, currentGlobalTheme) {
        val globalThemeLower = currentGlobalTheme.lowercase()
        val isNightOrNeutral = globalThemeLower in listOf("night", "neutral", "system")
        if (isNightOrNeutral) {
            when (globalThemeLower) {
                "night" -> MeowTheme.Night
                "neutral", "system" -> MeowTheme.Neutral
                else -> MeowTheme.Neutral
            }
        } else {
            if (tab == StitchMainTab.Market) {
                MeowTheme.Mint
            } else {
                MeowTheme.Honey
            }
        }
    }

    fun createMockPosts(): List<PostFeedItem> {
        val author1 = User(1L, "peachlatte", "桃子和拿铁", "", "两只猫的日常记录员", "2026-05-27T00:00:00Z")
        val author2 = User(2L, "puff_bakery", "泡芙小店", "", "提供猫罐头和零食", "2026-05-27T00:00:00Z")
        val author3 = User(3L, "sunday_walk", "周日散步社", "", "组织小型宠物活动", "2026-05-27T00:00:00Z")
        val author4 = User(4L, "clean_corner", "干净角落", "", "二手猫用品整理中", "2026-05-27T00:00:00Z")
        val post1 = Post(
          id = 1L,
          authorId = 1L,
          title = "猫猫第一次学会开门，家里从此没有秘密",
          content = "给门把手加了保护套，顺便记录一下这个聪明小脑袋。它先观察了我们两天，然后第三天就开始自己尝试。",
          category = "daily_share",
          tags = listOf("日常", "聪明猫"),
          mediaIds = emptyList(),
          createdAt = "2026-06-02T10:00:00Z",
          lastReplyAt = "2026-06-02T10:30:00Z"
        )
        val post2 = Post(
          id = 2L,
          authorId = 2L,
          title = "猫猫新手村：接猫回家第一周需要准备什么？",
          content = "接猫回家前，猫砂盆、航空箱、幼猫粮和水碗必不可少。最重要的是给主子一个安静的角落适应新环境。",
          category = "help",
          tags = listOf("新手", "养猫技巧"),
          mediaIds = emptyList(),
          createdAt = "2026-06-02T09:00:00Z",
          lastReplyAt = "2026-06-02T09:15:00Z"
        )
        val post3 = Post(
          id = 3L,
          authorId = 3L,
          title = "周末猫猫摄影散步局，doggie 也可以来当气氛组",
          content = "小区花园集合，拍照为主，不强社交。胆小猫可以只坐航空箱里观察。",
          category = "activity",
          tags = listOf("活动", "摄影"),
          mediaIds = emptyList(),
          createdAt = "2026-06-02T08:30:00Z",
          lastReplyAt = "2026-06-02T08:45:00Z"
        )
        val post4 = Post(
          id = 4L,
          authorId = 4L,
          title = "出一个 9 成新的开放式猫砂盆，适合小户型",
          content = "已彻底清洁消毒，同城可自提，附送未拆封猫砂铲。",
          category = "trade",
          tags = listOf("二手", "猫砂盆"),
          mediaIds = emptyList(),
          createdAt = "2026-06-02T08:00:00Z",
          lastReplyAt = "2026-06-02T08:10:00Z"
        )
        return listOf(
            PostFeedItem(post1, author1, 128L, true, null),
            PostFeedItem(post2, author2, 45L, false, null),
            PostFeedItem(post3, author3, 68L, false, null),
            PostFeedItem(post4, author4, 16L, false, null)
        )
    }

    fun createMockConversations(): List<Conversation> {
        val peer1 = User(2L, "puff_bakery", "泡芙小店", "", "提供猫罐头和零食", "2026-05-27T00:00:00Z")
        val peer2 = User(3L, "xiaoman", "小满", "", "橘猫大联盟盟主", "2026-05-27T00:00:00Z")
        return listOf(
            Conversation(peer1, "地址发你啦，今晚可自提。", 2L, 1, "2026-06-02T10:00:00Z"),
            Conversation(peer2, "谢谢！", 3L, 0, "2026-06-02T09:00:00Z")
        )
    }

    fun createMockListings(): List<Listing> =
        listOf(
            Listing(
                id = 1L,
                sellerId = 2L,
                type = "product",
                title = "未拆封猫罐头 6 罐组合",
                description = "猫猫优先，适合换粮过渡；同城可自提。",
                priceCents = 6800L,
                currency = "CNY",
                createdAt = "2026-06-02T10:00:00Z",
            ),
            Listing(
                id = 2L,
                sellerId = 5L,
                type = "service",
                title = "周末上门喂猫与铲砂",
                description = "有基础照护记录，可同时帮 doggie 换水。",
                priceCents = 12000L,
                currency = "CNY",
                createdAt = "2026-06-02T10:00:00Z",
            ),
            Listing(
                id = 3L,
                sellerId = 8L,
                type = "adopt",
                title = "三个月橘猫找稳定家庭",
                description = "已驱虫，性格亲人，需要领养回访。",
                priceCents = 0L,
                currency = "CNY",
                createdAt = "2026-05-31T18:30:00Z",
            ),
        )

    val effectiveFilter =
        when (tab) {
            StitchMainTab.Market -> "new"
            StitchMainTab.Feed -> filter
            else -> filter
        }

    LaunchedEffect(effectiveFilter, tab, feedReloadSignal, mockMode, feedRetrySignal) {
        if (tab != StitchMainTab.Feed && tab != StitchMainTab.Market) return@LaunchedEffect
        if (mockMode) {
            rawItems = createMockPosts()
            err = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        err = null
        rawItems =
            sdk.feedPosts(effectiveFilter).fold(
                onSuccess = { it },
                onFailure = { e ->
                    err = feedErrorMessage(e, apiBase)
                    null
                },
            )
        loading = false
    }

    LaunchedEffect(tab, mockMode) {
        if (tab != StitchMainTab.Market) return@LaunchedEffect
        if (mockMode) {
            listings = createMockListings()
            listingsErr = null
            listingsLoading = false
            return@LaunchedEffect
        }
        listingsLoading = true
        listingsErr = null
        listings =
            sdk.listings().fold(
                onSuccess = { it },
                onFailure = { e ->
                    listingsErr =
                        (e as? ApiException)?.message
                            ?: humanizeClientFailure(e, apiBase)
                    null
                },
            )
        listingsLoading = false
    }

    LaunchedEffect(tab, feedReloadSignal, mockMode) {
        if (tab != StitchMainTab.Messages) return@LaunchedEffect
        if (mockMode) {
            conversations = createMockConversations()
            convErr = null
            convLoading = false
            return@LaunchedEffect
        }
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

    LaunchedEffect(tab, profileUser.id, feedReloadSignal, mockMode) {
        if (tab != StitchMainTab.Profile) return@LaunchedEffect
        if (mockMode) {
            profilePosts = createMockPosts().filter { it.author.id == profileUser.id }
            profileLoading = false
            return@LaunchedEffect
        }
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
            val list = rawItems ?: return@remember emptyList<PostFeedItem>()
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

    MeowStitchTheme(theme = activeTheme) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = StitchPalette.Canvas,
            bottomBar = {
                StitchBottomNav(
                    selected = tab,
                    onSelect = { t ->
                        tab = t
                    },
                )
            },
            floatingActionButton = {
                if (tab == StitchMainTab.Feed) {
                    StitchFab(
                        onClick = onCompose,
                    )
                }
            }
        ) { inner ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = inner.calculateBottomPadding()),
            ) {
                when (tab) {
                    StitchMainTab.Profile ->
                        Column(Modifier.fillMaxSize()) {
                            StitchTopBar(
                                apiBase = apiBase,
                                user = profileUser,
                                title = stringResource(R.string.feed_profile_title),
                                subtitle = profileUser.nickname.ifBlank { profileUser.username },
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
                                profileBackground = profileBackground,
                                onLogout = onLogout,
                                onOpenPost = onOpenPost,
                                onEditProfile = { showEditProfile = true },
                                onSettings = { showThemeDialog = true },
                                onProfileBackgroundChanged = {
                                    sdk.sessionStore().setProfileBackground(it)
                                    profileBackground = sdk.sessionStore().getProfileBackground()
                                },
                                hint = profileHint,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }
                    StitchMainTab.Messages ->
                        StitchMessagesScreen(
                            sdk = sdk,
                            currentUser = profileUser,
                            loading = convLoading,
                            err = convErr,
                            items = conversations,
                            mockMode = mockMode,
                            onEnableMock = { mockMode = true },
                            onOpenMarket = { tab = StitchMainTab.Market },
                            onOpenOrders = { tab = StitchMainTab.Orders },
                            modifier = Modifier.fillMaxSize(),
                        )
                    StitchMainTab.Orders -> {
                        StitchOrdersScreen(
                            sdk = sdk,
                            onOpenMarket = { tab = StitchMainTab.Market },
                            onOpenMessages = { tab = StitchMainTab.Messages },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    StitchMainTab.Market -> {
                        StitchMarketScreen(
                            sdk = sdk,
                            user = profileUser,
                            apiBase = apiBase,
                            listings = listings,
                            loading = listingsLoading,
                            err = listingsErr,
                            query = q,
                            onQueryChange = { q = it },
                            mockMode = mockMode,
                            onEnableMock = { mockMode = true },
                            onAvatarPress = { tab = StitchMainTab.Profile },
                            onNotifyPress = { tab = StitchMainTab.Messages },
                            onOpenOrders = { tab = StitchMainTab.Orders },
                            onOpenMessages = { tab = StitchMainTab.Messages },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    StitchMainTab.Feed -> {
                        Column(Modifier.fillMaxSize()) {
                            StitchTopBar(
                                apiBase = apiBase,
                                user = profileUser,
                                title = "M&D",
                                subtitle = "meow & doggie",
                                onAvatarPress = { tab = StitchMainTab.Profile },
                                onNotifyPress = {
                                    tab = StitchMainTab.Messages
                                    messageQuery = ""
                                    messageSection = MessageSection.Notifications
                                },
                            )
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = FEED_PAGE_PADDING),
                            ) {
                                HomeFeedHeader(
                                    query = q,
                                    onQueryChange = { q = it },
                                    selectedFilter = filter,
                                    onFilterChange = { filter = it },
                                    items = rawItems.orEmpty(),
                                    mockMode = mockMode,
                                    onEnableMock = { mockMode = true },
                                    onCompose = onCompose,
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            Box(Modifier.weight(1f)) {
                                when {
                                    loading && rawItems == null ->
                                        FeedLoadingPane(Modifier.fillMaxSize())
                                    err != null && rawItems == null ->
                                        FeedFailurePane(
                                            message = err!!,
                                            currentUrl = apiBase,
                                            defaultUrl = BuildConfig.API_BASE_URL,
                                            onUrlChanged = { newUrl ->
                                                sdk.sessionStore().setApiUrl(newUrl)
                                                apiBase = sdk.baseUrl
                                                mockMode = false
                                                feedRetrySignal += 1
                                            },
                                            onRetry = {
                                                mockMode = false
                                                feedRetrySignal += 1
                                            },
                                            onDemo = { mockMode = true },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    filtered.isEmpty() ->
                                        PlaceholderPane(
                                            headline = stringResource(if (q.isBlank()) R.string.feed_empty_title else R.string.feed_empty_search_title),
                                            body =
                                                if (q.isBlank()) {
                                                    stringResource(R.string.feed_empty_body)
                                                } else {
                                                    stringResource(R.string.feed_empty_search_body)
                                                },
                                            modifier = Modifier.fillMaxSize(),
                                        )
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
                                    profileHint = context.getString(R.string.profile_updated)
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
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = sdk.getTheme(),
                onDismiss = { showThemeDialog = false },
                onSelect = {
                    onThemeChanged(it)
                    showThemeDialog = false
                }
            )
        }
    }
}

@Composable
private fun HomeFeedHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    items: List<PostFeedItem>,
    mockMode: Boolean,
    onEnableMock: () -> Unit,
    onCompose: () -> Unit,
) {
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
                .border(1.dp, StitchPalette.BorderHairline, FEED_SECTION_RADIUS),
        shape = FEED_SECTION_RADIUS,
        color = StitchPalette.Surface,
    ) {
        Row(
            modifier =
                Modifier
                    .clickable(onClick = onCompose)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(StitchPalette.BrandMuted)
                        .border(1.dp, StitchPalette.BorderHairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Pets,
                    contentDescription = null,
                    tint = StitchPalette.Brand,
                    modifier = Modifier.size(24.dp),
                )
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = StitchShape.pill,
                color = StitchPalette.SurfaceLow,
                border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
            ) {
                Text(
                    stringResource(R.string.feed_share_prompt),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(StitchPalette.BrandMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.feed_add_photo),
                    tint = StitchPalette.Brand,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    FeedFilterRow(selectedFilter = selectedFilter, onFilterChange = onFilterChange)
    if (mockMode) {
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.feed_demo),
            style = MaterialTheme.typography.labelMedium,
            color = StitchPalette.Brand,
            modifier =
                Modifier
                    .clip(StitchShape.pill)
                    .background(StitchPalette.BrandMuted)
                    .clickable(onClick = onEnableMock)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    } else {
        query.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.feed_filtered_by, it),
                style = MaterialTheme.typography.labelMedium,
                color = StitchPalette.OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedFilterRow(
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FEED_FILTERS.forEach { f ->
            val selected = selectedFilter == f.key
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(f.key) },
                label = {
                    Text(
                        stringResource(f.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
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
                shape = StitchShape.pill,
            )
        }
    }
}

@Composable
private fun DiscoverFeedHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCircle: String?,
    onCircleSelect: (String) -> Unit,
    onResetCircles: () -> Unit,
    listings: List<Listing>?,
    listingsLoading: Boolean,
    listingsErr: String?,
    onEnableMock: () -> Unit,
) {
    StitchSearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "搜索圈子、活动或好物",
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "M&D 发现圈子",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchPalette.OnSurface,
            )
            Text(
                selectedCircle ?: "猫猫兴趣先行，服务和领养随后",
                style = MaterialTheme.typography.bodySmall,
                color = StitchPalette.OnSurfaceVariant,
            )
        }
        Text(
            "全部",
            style = MaterialTheme.typography.labelLarge,
            color = StitchPalette.Brand,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onResetCircles),
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
            val selected = selectedCircle == label
            Surface(
                shape = StitchShape.pill,
                color = if (selected) StitchPalette.Brand else StitchPalette.SurfaceLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchPalette.BorderHairline),
                modifier = Modifier.clickable { onCircleSelect(label) },
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else StitchPalette.OnSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FEED_SECTION_RADIUS,
        color = StitchPalette.SecondaryContainer.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Campaign, contentDescription = null, tint = StitchPalette.Secondary)
            Column(Modifier.weight(1f)) {
                Text(
                    "本周主题：猫猫新手村开放问答",
                    style = MaterialTheme.typography.titleSmall,
                    color = StitchPalette.OnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "适合第一次养猫、换粮、搬家适应等问题",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    MarketPreviewSection(
        query = query,
        listings = listings,
        loading = listingsLoading,
        err = listingsErr,
        onEnableMock = onEnableMock,
    )
}

@Composable
private fun MarketPreviewSection(
    query: String,
    listings: List<Listing>?,
    loading: Boolean,
    err: String?,
    onEnableMock: () -> Unit,
) {
    val previewItems =
        remember(listings, query) {
            val all = listings.orEmpty()
            val key = query.trim().lowercase()
            val filtered =
                if (key.isBlank()) {
                    all
                } else {
                    all.filter {
                        it.title.lowercase().contains(key) ||
                            it.description.lowercase().contains(key) ||
                            it.type.lowercase().contains(key)
                    }
                }
            filtered.take(3)
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FEED_SECTION_RADIUS,
        color = StitchPalette.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "好物市集",
                        style = MaterialTheme.typography.titleMedium,
                        color = StitchPalette.OnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "猫猫用品、领养、上门服务优先；doggie 友好内容作为伴随分支",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchPalette.OnSurfaceVariant,
                    )
                }
                Surface(
                    shape = StitchShape.pill,
                    color = StitchPalette.BrandMuted,
                ) {
                    Text(
                        "安心交易",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = StitchPalette.Brand,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            when {
                loading && listings == null ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(color = StitchPalette.Brand, modifier = Modifier.size(18.dp))
                        Text("正在加载市集...", style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
                    }
                err != null && listings == null ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "市集暂时不可用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StitchPalette.Error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = onEnableMock,
                            colors =
                                ButtonDefaults.textButtonColors(
                                    containerColor = StitchPalette.BrandMuted,
                                    contentColor = StitchPalette.Brand,
                                ),
                            shape = FEED_SECTION_RADIUS,
                        ) {
                            Text("看演示")
                        }
                    }
                previewItems.isEmpty() ->
                    Text(
                        "暂时没有匹配的好物",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchPalette.OnSurfaceVariant,
                    )
                else ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        previewItems.forEach { item ->
                            ListingPreviewRow(item)
                        }
                    }
            }
        }
    }
}

@Composable
private fun ListingPreviewRow(item: Listing) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(FEED_CARD_RADIUS)
                .background(StitchPalette.SurfaceLow)
                .border(1.dp, StitchPalette.BorderHairline, FEED_CARD_RADIUS)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = StitchShape.pill,
            color = if (item.type == "adopt") StitchPalette.SecondaryContainer else StitchPalette.BrandMuted,
        ) {
            Text(
                listingTypeLabel(item.type),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (item.type == "adopt") StitchPalette.PrimaryDark else StitchPalette.Brand,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = StitchPalette.OnSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                color = StitchPalette.OnSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            formatListingPrice(item.priceCents, item.currency),
            style = MaterialTheme.typography.labelLarge,
            color = StitchPalette.Error,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun listingTypeLabel(type: String): String =
    when (type) {
        "product" -> "商品"
        "service" -> "服务"
        "adopt" -> "领养"
        else -> type
    }

private fun formatListingPrice(
    cents: Long,
    currency: String,
): String {
    if (cents <= 0L) return "免费"
    val amount = cents / 100.0
    val symbol = if (currency.uppercase() == "CNY") "¥" else currency.uppercase()
    return "$symbol${amount.toString().trimEnd('0').trimEnd('.')}"
}

@Composable
private fun FeedLoadingPane(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = FEED_PAGE_PADDING, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) { index ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (index == 0) 176.dp else 132.dp),
                shape = FEED_SECTION_RADIUS,
                color = StitchPalette.SurfaceLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchPalette.BorderHairline),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPalette.Brand, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedFailurePane(
    message: String,
    currentUrl: String,
    defaultUrl: String,
    onUrlChanged: (String?) -> Unit,
    onRetry: () -> Unit,
    onDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showApiDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = FEED_SECTION_RADIUS,
            color = StitchPalette.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StitchPalette.BorderHairline),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "动态暂时没取到",
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchPalette.OnSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    color = StitchPalette.OnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "当前 API: $currentUrl",
                    color = StitchPalette.OnSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { showApiDialog = true }
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onRetry,
                        colors =
                            ButtonDefaults.textButtonColors(
                                containerColor = StitchPalette.SurfaceLow,
                                contentColor = StitchPalette.OnSurface,
                            ),
                        shape = FEED_SECTION_RADIUS,
                    ) {
                        Text("重试", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showApiDialog = true },
                        colors =
                            ButtonDefaults.textButtonColors(
                                containerColor = StitchPalette.SurfaceLow,
                                contentColor = StitchPalette.Brand,
                            ),
                        shape = FEED_SECTION_RADIUS,
                    ) {
                        Text("配置 API", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onDemo,
                        colors =
                            ButtonDefaults.textButtonColors(
                                containerColor = StitchPalette.BrandMuted,
                                contentColor = StitchPalette.Brand,
                            ),
                        shape = FEED_SECTION_RADIUS,
                    ) {
                        Text("看离线演示", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showApiDialog) {
            ApiBaseUrlConfigDialog(
                currentUrl = currentUrl,
                defaultUrl = defaultUrl,
                onDismiss = { showApiDialog = false },
                onSave = { newUrl ->
                    onUrlChanged(newUrl)
                    showApiDialog = false
                }
            )
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val normalizedTheme =
        when (currentTheme.lowercase()) {
            "sugar" -> "honey"
            "system" -> "neutral"
            else -> currentTheme.lowercase()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.theme_choose), style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect("honey") }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFF8A3D)))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.theme_honey), style = MaterialTheme.typography.bodyLarge, fontWeight = if (normalizedTheme == "honey") FontWeight.Bold else FontWeight.Normal, color = StitchPalette.OnSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect("mint") }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2EC4A6)))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.theme_mint), style = MaterialTheme.typography.bodyLarge, fontWeight = if (normalizedTheme == "mint") FontWeight.Bold else FontWeight.Normal, color = StitchPalette.OnSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect("night") }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.theme_night), style = MaterialTheme.typography.bodyLarge, fontWeight = if (normalizedTheme == "night") FontWeight.Bold else FontWeight.Normal, color = StitchPalette.OnSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect("neutral") }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF4B5563)))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.theme_neutral), style = MaterialTheme.typography.bodyLarge, fontWeight = if (normalizedTheme == "neutral") FontWeight.Bold else FontWeight.Normal, color = StitchPalette.OnSurface)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
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
        title = { Text(stringResource(R.string.profile_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_display_name)) },
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    minLines = 2,
                    maxLines = 4,
                    label = { Text(stringResource(R.string.profile_bio)) },
                )
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_avatar_url)) },
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
            TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.common_cancel)) }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    onSave(nickname.trim().ifBlank { user.username }, bio.trim(), avatarUrl.trim())
                },
            ) { Text(if (saving) stringResource(R.string.profile_saving) else stringResource(R.string.profile_save)) }
        },
    )
}

@Composable
private fun ProfilePanel(
    apiBase: String,
    user: User,
    gridPosts: List<PostFeedItem>?,
    gridLoading: Boolean,
    profileBackground: String,
    onLogout: () -> Unit,
    onOpenPost: (Long) -> Unit,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onProfileBackgroundChanged: (String) -> Unit,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    val avatarUrl = resolveMediaUrl(apiBase, user.avatarUrl.takeIf { it.isNotBlank() })
    val posts = gridPosts.orEmpty()
    val likesTotal = posts.sumOf { it.likeCount }
    val activeBackground =
        PROFILE_BACKGROUND_OPTIONS.firstOrNull { it.key == profileBackground }
            ?: PROFILE_BACKGROUND_OPTIONS.first()
    val coverColor =
        when (activeBackground.key) {
            "desk" -> StitchPalette.SurfaceLow
            "arcade" -> StitchPalette.SurfaceContainer
            "garden" -> StitchPalette.GoldWeak
            else -> StitchPalette.SecondaryContainer.copy(alpha = 0.55f)
        }
    val coverBorder =
        when (activeBackground.key) {
            "arcade" -> StitchPalette.BrandLight
            "garden" -> StitchPalette.Outline
            else -> StitchPalette.BorderHairline
        }
    Column(
        modifier =
            modifier
                .padding(horizontal = FEED_PAGE_PADDING, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FEED_SECTION_RADIUS,
            color = coverColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, coverBorder),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Surface(
                    shape = StitchShape.pill,
                    color = StitchPalette.Surface.copy(alpha = 0.72f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchPalette.BorderHairline),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(activeBackground.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = StitchPalette.Brand,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(activeBackground.noteRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = StitchPalette.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.profile_background),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StitchPalette.OnSurface,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PROFILE_BACKGROUND_OPTIONS.forEach { option ->
                    val selected = option.key == activeBackground.key
                    Surface(
                        shape = StitchShape.pill,
                        color = if (selected) StitchPalette.Brand else StitchPalette.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StitchPalette.BorderHairline),
                        modifier = Modifier.clickable { onProfileBackgroundChanged(option.key) },
                    ) {
                        Text(
                            stringResource(option.labelRes),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else StitchPalette.OnSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
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
            ProfileStatCell(formatCompactCount(likesTotal), stringResource(R.string.profile_likes))
            ProfileStatCell("—", stringResource(R.string.profile_following))
            ProfileStatCell("—", stringResource(R.string.profile_followers))
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
                        stringResource(R.string.profile_edit),
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
            "Posts",
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
                    "Your posts will appear here.",
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
            "Log out",
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
    onToggleMock: () -> Unit,
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
                Text(
                    text = err,
                    color = StitchPalette.Error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onToggleMock,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = StitchPalette.Brand.copy(alpha = 0.1f),
                        contentColor = StitchPalette.Brand
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("看离线演示", fontWeight = FontWeight.Bold)
                }
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
                                    "M&D 伙伴阿强关注了你",
                                    "猫猫新手_07 关注了你",
                                    "领养中心官方关注了你",
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

@Composable
private fun MarketPane(
    sdk: MeowCircleSdk,
    listings: List<Listing>?,
    loading: Boolean,
    err: String?,
    q: String,
    onQueryChange: (String) -> Unit,
    mockMode: Boolean,
    onEnableMock: () -> Unit,
    onBuySuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf("all") }
    var orderDialogText by remember { mutableStateOf<String?>(null) }
    var buyingListingId by remember { mutableStateOf<Long?>(null) }
    var actionLoading by remember { mutableStateOf(false) }

    val filteredListings = remember(listings, q, selectedType) {
        val all = listings.orEmpty()
        val queryKey = q.trim().lowercase()
        val typeFiltered = if (selectedType == "all") {
            all
        } else {
            all.filter { it.type == selectedType }
        }
        if (queryKey.isBlank()) {
            typeFiltered
        } else {
            typeFiltered.filter {
                it.title.lowercase().contains(queryKey) ||
                    it.description.lowercase().contains(queryKey)
            }
        }
    }

    fun handleBuy(listing: Listing) {
        if (mockMode) {
            orderDialogText = "下单成功！已为您模拟创建订单，即将前往订单列表。"
            buyingListingId = listing.id
            return
        }
        actionLoading = true
        scope.launch {
            sdk.createOrder(listing.id).fold(
                onSuccess = { order ->
                    orderDialogText = "下单成功！已为您创建订单 #" + order.id + "，即将前往订单列表付款。"
                    buyingListingId = listing.id
                },
                onFailure = { e ->
                    val raw = (e as? ApiException)?.message ?: e.message.orEmpty()
                    orderDialogText = "下单失败: " + raw
                }
            )
            actionLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FEED_PAGE_PADDING)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "M&D 好物集市",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchPalette.OnSurface
                )
                TextButton(
                    onClick = onEnableMock,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (mockMode) StitchPalette.BrandMuted else Color.Transparent,
                        contentColor = StitchPalette.Brand
                    ),
                    shape = StitchShape.pill
                ) {
                    Text(if (mockMode) "演示模式" else "切换演示", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            StitchSearchField(
                value = q,
                onValueChange = onQueryChange,
                placeholder = "搜索集市商品、服务或领养...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "全部好物",
                    "product" to "闲置商品",
                    "service" to "上门服务",
                    "adopt" to "猫咪领养"
                ).forEach { (key, label) ->
                    val sel = selectedType == key
                    Surface(
                        shape = StitchShape.pill,
                        color = if (sel) StitchPalette.Brand else StitchPalette.Surface,
                        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                        modifier = Modifier.clickable { selectedType = key }
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sel) Color.White else StitchPalette.OnSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                loading && listings == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = StitchPalette.Brand)
                    }
                }
                err != null && listings == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(err, color = StitchPalette.Error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = onEnableMock) {
                            Text("看离线演示")
                        }
                    }
                }
                filteredListings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("没有找到匹配的物品", style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = FEED_PAGE_PADDING, end = FEED_PAGE_PADDING, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredListings, key = { it.id }) { listing ->
                            MarketListingItemCard(
                                listing = listing,
                                actionLoading = actionLoading,
                                onBuyClick = { handleBuy(listing) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (orderDialogText != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                orderDialogText = null
                buyingListingId = null
            },
            title = { Text("提示") },
            text = { Text(orderDialogText!!) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val isSuccess = orderDialogText?.contains("成功") == true
                        orderDialogText = null
                        buyingListingId = null
                        if (isSuccess) {
                            onBuySuccess()
                        }
                    }
                ) {
                    Text("好")
                }
            }
        )
    }
}

@Composable
private fun MarketListingItemCard(
    listing: Listing,
    actionLoading: Boolean,
    onBuyClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, StitchPalette.BorderHairline, StitchShape.cardFeed),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = StitchShape.pill,
                    color = if (listing.type == "adopt") StitchPalette.SecondaryContainer else StitchPalette.BrandMuted,
                ) {
                    Text(
                        text = listingTypeLabel(listing.type),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (listing.type == "adopt") StitchPalette.PrimaryDark else StitchPalette.Brand,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = formatListingPrice(listing.priceCents, listing.currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchPalette.Error,
                    fontWeight = FontWeight.Black
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    listing.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchPalette.OnSurface
                )
                Text(
                    listing.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchPalette.OnSurfaceVariant
                )
            }

            HorizontalDivider(color = StitchPalette.BorderHairline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBuyClick,
                    enabled = !actionLoading,
                    shape = StitchShape.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPalette.Brand,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (listing.type == "adopt") "申请领养" else "立即购买",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
