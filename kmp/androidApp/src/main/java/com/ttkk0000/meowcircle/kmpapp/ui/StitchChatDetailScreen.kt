package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.ConversationDetails
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.Message
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.util.formatConversationListTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchChatDetailScreen(
    sdk: MeowCircleSdk,
    peerId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var conversationDetails by remember { mutableStateOf<ConversationDetails?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var typedMessage by remember { mutableStateOf("") }
    var mockMode by remember { mutableStateOf(false) }
    
    val currentUser = remember { sdk.cachedUser() }
    val currentUserId = currentUser?.id ?: 0L

    // For local mock session
    var mockMessages by remember {
        mutableStateOf(
            listOf(
                Message(
                    id = 1L,
                    senderId = peerId,
                    recipientId = currentUserId,
                    content = "你好！请问这只猫猫/商品还在吗？",
                    createdAt = Instant.now().minusSeconds(300).toString()
                ),
                Message(
                    id = 2L,
                    senderId = currentUserId,
                    recipientId = peerId,
                    content = "在的，疫苗驱虫都已经做过了，性格特别温顺粘人。",
                    createdAt = Instant.now().minusSeconds(240).toString()
                ),
                Message(
                    id = 3L,
                    senderId = peerId,
                    recipientId = currentUserId,
                    content = "太好啦！周末可以约去线下看看它吗？",
                    createdAt = Instant.now().minusSeconds(180).toString()
                )
            )
        )
    }

    fun refreshChat(showLoading: Boolean = false) {
        if (mockMode) {
            conversationDetails = ConversationDetails(
                messages = mockMessages,
                peer = User(
                    id = peerId,
                    username = "md_friend",
                    nickname = "M&D 伙伴",
                    avatarUrl = "",
                    bio = "Cat-first & doggie-friendly!",
                    createdAt = "2026-05-01T00:00:00Z"
                )
            )
            return
        }

        scope.launch {
            if (showLoading) loading = true
            sdk.fetchConversationDetails(peerId)
                .onSuccess {
                    conversationDetails = it
                    errorMsg = null
                }
                .onFailure {
                    // Failover to mock mode automatically if backend is not reachable or returns error
                    errorMsg = it.message ?: "连接服务失败，已开启离线演示模式"
                    mockMode = true
                    // Sync conversationDetails with current mockMessages
                    conversationDetails = ConversationDetails(
                        messages = mockMessages,
                        peer = User(
                            id = peerId,
                            username = "md_friend",
                            nickname = "M&D 伙伴",
                            avatarUrl = "",
                            bio = "Cat-first & doggie-friendly!",
                            createdAt = "2026-05-01T00:00:00Z"
                        )
                    )
                }
            if (showLoading) loading = false
        }
    }

    // Auto refresh logic (poll every 3s)
    LaunchedEffect(mockMode, peerId) {
        refreshChat(showLoading = true)
        while (true) {
            delay(3000)
            refreshChat(showLoading = false)
        }
    }

    // Scroll to bottom when messages list size changes
    val messageList = conversationDetails?.messages.orEmpty()
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.size - 1)
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        if (mockMode) {
            val newMsg = Message(
                id = (mockMessages.maxOfOrNull { it.id } ?: 0L) + 1L,
                senderId = currentUserId,
                recipientId = peerId,
                content = content,
                createdAt = Instant.now().toString()
            )
            mockMessages = mockMessages + newMsg
            typedMessage = ""
            refreshChat()
            
            // Auto reply simulator for immersive offline review!
            scope.launch {
                delay(1500)
                val replyMsg = Message(
                    id = (mockMessages.maxOfOrNull { it.id } ?: 0L) + 2L,
                    senderId = peerId,
                    recipientId = currentUserId,
                    content = "【M&D 离线助手】收到您的消息: \"$content\"！非常感谢您的测试 🐾",
                    createdAt = Instant.now().toString()
                )
                mockMessages = mockMessages + replyMsg
                refreshChat()
            }
            return
        }

        scope.launch {
            sdk.sendMessage(peerId, content)
                .onSuccess {
                    typedMessage = ""
                    refreshChat(showLoading = false)
                }
                .onFailure {
                    errorMsg = it.message ?: "发送失败"
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val peer = conversationDetails?.peer
                        Text(
                            text = peer?.nickname?.ifBlank { peer.username } ?: "正在对话...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPalette.OnSurface
                        )
                        Text(
                            text = if (mockMode) "离线演示模式" else "在线对话 (ID: $peerId)",
                            fontSize = 11.sp,
                            color = if (mockMode) StitchPalette.Brand else StitchPalette.OnSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = StitchPalette.OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchPalette.Surface
                )
            )
        },
        containerColor = StitchPalette.Canvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Chat Message List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (loading && conversationDetails == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = StitchPalette.Brand)
                    }
                } else if (messageList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "发送一条消息，开启和 TA 的对话吧 🐾",
                            color = StitchPalette.OnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messageList) { msg ->
                            val isMine = msg.senderId == currentUserId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                            ) {
                                if (!isMine) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(StitchPalette.BrandMuted),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (peerId % 2 == 0L) "🐱" else "🐶",
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                Column(
                                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMine) 12.dp else 2.dp,
                                            bottomEnd = if (isMine) 2.dp else 12.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMine) StitchPalette.Brand else StitchPalette.Surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Text(
                                            text = msg.content,
                                            color = if (isMine) Color.White else StitchPalette.OnSurface,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = formatConversationListTime(msg.createdAt),
                                        fontSize = 10.sp,
                                        color = StitchPalette.OnSurfaceVariant
                                    )
                                }

                                if (isMine) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(StitchPalette.SecondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Send Bar
            Surface(
                color = StitchPalette.Surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = { Text("说点什么吧...", color = StitchPalette.OnSurfaceVariant, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 120.dp),
                        shape = StitchShape.field,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StitchPalette.Brand,
                            unfocusedBorderColor = StitchPalette.BorderHairline,
                            focusedContainerColor = StitchPalette.Canvas,
                            unfocusedContainerColor = StitchPalette.Canvas
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (typedMessage.isNotBlank()) {
                                    sendMessage(typedMessage.trim())
                                }
                            }
                        )
                    )

                    Button(
                        onClick = {
                            if (typedMessage.isNotBlank()) {
                                sendMessage(typedMessage.trim())
                            }
                        },
                        shape = StitchShape.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPalette.Brand,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

