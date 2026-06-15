package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.Conversation
import com.ttkk0000.meowcircle.ConversationDetailData
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.Message
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchSearchField
import com.ttkk0000.meowcircle.kmpapp.util.formatConversationListTime
import kotlinx.coroutines.launch

private data class MessageFilter(val key: String, val labelRes: Int)

private val MESSAGE_FILTERS =
    listOf(
        MessageFilter("all", R.string.messages_filter_all),
        MessageFilter("buying", R.string.messages_filter_buying),
        MessageFilter("selling", R.string.messages_filter_selling),
        MessageFilter("unread", R.string.messages_filter_unread),
    )

@Composable
fun StitchMessagesScreen(
    sdk: MeowCircleSdk,
    currentUser: User,
    apiBase: String,
    loading: Boolean,
    err: String?,
    items: List<Conversation>?,
    mockMode: Boolean,
    onEnableMock: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenOrders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var activeDetail by remember { mutableStateOf<ConversationDetailData?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }
    val mockPaid = stringResource(R.string.messages_mock_paid)
    val mockPaymentReceived = stringResource(R.string.messages_mock_payment_received)
    val mockShipTomorrow = stringResource(R.string.messages_mock_ship_tomorrow)

    val visible =
        remember(items, query, filter) {
            items.orEmpty()
                .filter { convo ->
                    query.isBlank() ||
                        convo.peer.nickname.contains(query, ignoreCase = true) ||
                        convo.peer.username.contains(query, ignoreCase = true) ||
                        convo.lastMessage.contains(query, ignoreCase = true)
                }
                .filter { convo ->
                    when (filter) {
                        "unread" -> convo.unreadCount > 0
                        "buying" -> convo.lastMessage.contains("order", ignoreCase = true) || convo.lastMessage.contains("paid", ignoreCase = true) || convo.lastMessage.contains("订单") || convo.lastMessage.contains("付款")
                        "selling" -> convo.lastMessage.contains("ship", ignoreCase = true) || convo.lastMessage.contains("available", ignoreCase = true) || convo.lastMessage.contains("发货") || convo.lastMessage.contains("还在")
                        else -> true
                    }
                }
        }

    LaunchedEffect(activeConversation?.peer?.id, mockMode, mockPaid, mockPaymentReceived, mockShipTomorrow) {
        val convo = activeConversation ?: return@LaunchedEffect
        detailError = null
        if (mockMode) {
            activeDetail = mockConversationDetail(convo.peer, currentUser.id, mockPaid, mockPaymentReceived, mockShipTomorrow)
            detailLoading = false
            return@LaunchedEffect
        }
        detailLoading = true
        activeDetail =
            sdk.conversationWithPeer(convo.peer.id).fold(
                onSuccess = { it },
                onFailure = { e ->
                    detailError = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                    null
                },
            )
        detailLoading = false
    }

    if (activeConversation != null) {
        ChatDetailScreen(
            sdk = sdk,
            currentUser = currentUser,
            apiBase = apiBase,
            conversation = activeConversation!!,
            detail = activeDetail,
            loading = detailLoading,
            error = detailError,
            mockMode = mockMode,
            onBack = {
                activeConversation = null
                activeDetail = null
                detailError = null
            },
            onOpenOrders = onOpenOrders,
            onDetailChanged = { activeDetail = it },
            modifier = modifier,
        )
        return
    }

    MessagesListScreen(
        loading = loading,
        err = err,
        conversations = visible,
        query = query,
        filter = filter,
        mockMode = mockMode,
        apiBase = apiBase,
        onQueryChange = { query = it },
        onFilterChange = { filter = it },
        onOpenConversation = { activeConversation = it },
        onEnableMock = onEnableMock,
        onOpenMarket = onOpenMarket,
        onOpenOrders = onOpenOrders,
        modifier = modifier,
    )
}

@Composable
private fun MessagesListScreen(
    loading: Boolean,
    err: String?,
    conversations: List<Conversation>,
    query: String,
    filter: String,
    mockMode: Boolean,
    apiBase: String,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onEnableMock: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenOrders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(StitchPalette.Surface)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.messages_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = StitchPalette.OnSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEnableMock, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.messages_compose), tint = StitchPalette.Brand)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StitchSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.messages_search_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MESSAGE_FILTERS.forEach { f ->
                    val selected = filter == f.key
                    Surface(
                        shape = StitchShape.pill,
                        color = if (selected) StitchPalette.Brand else StitchPalette.Surface,
                        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                        modifier = Modifier.clickable { onFilterChange(f.key) },
                    ) {
                        Text(
                            stringResource(f.labelRes),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else StitchPalette.OnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPalette.Brand)
                }
                err != null && !mockMode -> MessagesEmptyState(
                    title = stringResource(R.string.messages_unavailable_title),
                    body = err,
                    primary = stringResource(R.string.market_view_demo),
                    secondary = null,
                    onPrimary = onEnableMock,
                    onSecondary = null,
                )
                conversations.isEmpty() -> MessagesEmptyState(
                    title = stringResource(R.string.messages_empty_title),
                    body = stringResource(R.string.messages_empty_body),
                    primary = stringResource(R.string.messages_browse_market),
                    secondary = stringResource(R.string.messages_view_orders),
                    onPrimary = onOpenMarket,
                    onSecondary = onOpenOrders,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp),
                ) {
                    items(conversations, key = { it.peer.id }) { convo ->
                        ConversationRow(convo = convo, apiBase = apiBase, onClick = { onOpenConversation(convo) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatDetailScreen(
    sdk: MeowCircleSdk,
    currentUser: User,
    apiBase: String,
    conversation: Conversation,
    detail: ConversationDetailData?,
    loading: Boolean,
    error: String?,
    mockMode: Boolean,
    onBack: () -> Unit,
    onOpenOrders: () -> Unit,
    onDetailChanged: (ConversationDetailData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var draft by remember(conversation.peer.id) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    val moreComingSoon = stringResource(R.string.messages_more_coming_soon)
    val attachComingSoon = stringResource(R.string.messages_attach_coming_soon)
    val peer = detail?.peer ?: conversation.peer
    val messages = detail?.messages.orEmpty()

    fun send() {
        val text = draft.trim()
        if (text.isBlank()) return
        if (mockMode) {
            val local =
                Message(
                    id = (messages.maxOfOrNull { it.id } ?: 0L) + 1L,
                    senderId = currentUser.id,
                    recipientId = peer.id,
                    content = text,
                    read = true,
                    createdAt = "2026-06-09T09:41:00Z",
                )
            onDetailChanged(ConversationDetailData(peer = peer, messages = messages + local))
            draft = ""
            return
        }
        sending = true
        sendError = null
        scope.launch {
            sdk.sendMessage(peer.id, text).fold(
                onSuccess = { sent ->
                    onDetailChanged(ConversationDetailData(peer = peer, messages = messages + sent))
                    draft = ""
                },
                onFailure = { e ->
                    sendError = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                },
            )
            sending = false
        }
    }

    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(StitchPalette.Surface)
                    .border(1.dp, StitchPalette.BorderHairline)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = StitchPalette.OnSurface)
            }
            AvatarBubble(peer, apiBase)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(peer.nickname.ifBlank { peer.username }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.messages_online), style = MaterialTheme.typography.labelMedium, color = StitchPalette.OnSurfaceVariant)
            }
            IconButton(onClick = { sendError = moreComingSoon }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.common_more), tint = StitchPalette.OnSurface)
            }
        }
        if (conversation.lastMessage.contains("order", ignoreCase = true) || conversation.lastMessage.contains("订单") || messages.any { it.content.contains("order", ignoreCase = true) || it.content.contains("订单") }) {
            OrderContextCard(apiBase = apiBase, onOpenOrders = onOpenOrders)
        }
        Box(Modifier.weight(1f)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPalette.Brand)
                }
                error != null -> Text(
                    error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = StitchPalette.Error,
                    textAlign = TextAlign.Center,
                )
                messages.isEmpty() -> Text(
                    stringResource(R.string.messages_empty_chat),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = StitchPalette.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Surface(shape = StitchShape.pill, color = StitchPalette.Surface) {
                            Text(stringResource(R.string.messages_today), modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = StitchPalette.OnSurfaceVariant)
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message = message, mine = message.senderId == currentUser.id)
                    }
                }
            }
        }
        sendError?.let {
            Text(it, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp), color = StitchPalette.Error, style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(StitchPalette.Surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = { sendError = attachComingSoon }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = stringResource(R.string.messages_attach), tint = StitchPalette.Brand)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.messages_type_placeholder)) },
                singleLine = true,
                shape = StitchShape.pill,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchPalette.BorderHairline,
                        unfocusedBorderColor = StitchPalette.BorderHairline,
                        cursorColor = StitchPalette.Brand,
                        focusedContainerColor = StitchPalette.Surface,
                        unfocusedContainerColor = StitchPalette.Surface,
                    ),
                modifier = Modifier.weight(1f).height(54.dp),
            )
            IconButton(
                onClick = ::send,
                enabled = !sending && draft.isNotBlank(),
                modifier = Modifier.size(48.dp).clip(CircleShape).background(StitchPalette.Brand),
            ) {
                if (sending) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.messages_send), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    convo: Conversation,
    apiBase: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(StitchPalette.Surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBubble(convo.peer, apiBase)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    convo.peer.nickname.ifBlank { convo.peer.username },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(formatConversationListTime(convo.updatedAt), style = MaterialTheme.typography.labelSmall, color = StitchPalette.OnSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (convo.lastMessage.contains("order", ignoreCase = true) || convo.lastMessage.contains("订单")) {
                    Surface(shape = StitchShape.pill, color = Color(0xFFE3F1FF)) {
                        Text(
                            stringResource(R.string.messages_order_badge),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2F7FD5),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    convo.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (convo.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(StitchPalette.Brand))
                }
            }
        }
    }
    HorizontalDivider(color = StitchPalette.BorderHairline)
}

@Composable
private fun MessageBubble(
    message: Message,
    mine: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = StitchShape.cardFeed,
            color = if (mine) StitchPalette.Brand else StitchPalette.Surface,
            shadowElevation = 0.dp,
            border = if (mine) null else BorderStroke(1.dp, StitchPalette.BorderHairline),
            modifier = Modifier.fillMaxWidth(0.78f),
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (mine) Color.White else StitchPalette.OnSurface,
            )
        }
    }
}

@Composable
private fun OrderContextCard(
    apiBase: String,
    onOpenOrders: () -> Unit,
) {
    val imageUrl = "${apiBase.removeSuffix("/")}/mock-images/mock_image_15.png"
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).clickable(onClick = onOpenOrders),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.messages_order_context_title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(stringResource(R.string.messages_order_context_id), style = MaterialTheme.typography.labelMedium, color = StitchPalette.OnSurfaceVariant)
                Text(stringResource(R.string.messages_order_context_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.messages_order_context_status), style = MaterialTheme.typography.labelSmall, color = Color(0xFF2F7FD5), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MessagesEmptyState(
    title: String,
    body: String,
    primary: String,
    secondary: String?,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            shape = StitchShape.cardFeed,
            colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
            border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(78.dp).clip(CircleShape).background(StitchPalette.BrandMuted), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                Button(onClick = onPrimary, shape = StitchShape.field, colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(primary, color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (secondary != null && onSecondary != null) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onSecondary, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                        Text(secondary, color = StitchPalette.OnSurface)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.messages_suggested_starters), style = MaterialTheme.typography.titleSmall, color = StitchPalette.OnSurfaceVariant)
        listOf(
            R.string.messages_starter_product,
            R.string.messages_starter_order,
            R.string.messages_starter_follow,
        ).forEach { starter ->
            Spacer(Modifier.height(10.dp))
            Surface(shape = StitchShape.field, color = StitchPalette.Surface, border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                Text(stringResource(starter), modifier = Modifier.fillMaxWidth().padding(13.dp), style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AvatarBubble(user: User, apiBase: String) {
    val avatarUrl = resolveMediaUrl(apiBase, user.avatarUrl.takeIf { it.isNotBlank() })
        ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_1.png"
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(StitchPalette.BrandMuted),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = user.nickname,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun mockConversationDetail(
    peer: User,
    currentUserId: Long,
    paidMessage: String,
    paymentReceivedMessage: String,
    shipTomorrowMessage: String,
): ConversationDetailData =
    ConversationDetailData(
        peer = peer,
        messages =
            listOf(
                Message(1L, peer.id, currentUserId, paidMessage, false, "2026-06-09T10:30:00Z"),
                Message(2L, currentUserId, peer.id, paymentReceivedMessage, true, "2026-06-09T10:41:00Z"),
                Message(3L, currentUserId, peer.id, shipTomorrowMessage, true, "2026-06-09T10:42:00Z"),
            ),
    )
