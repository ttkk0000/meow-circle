package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.Conversation
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.desktop.ConversationDetails
import com.ttkk0000.meowcircle.desktop.LocalDesktopStitchColors
import com.ttkk0000.meowcircle.desktop.MndApiClient
import com.ttkk0000.meowcircle.desktop.MndDesktopShape
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    sdk: MeowCircleSdk,
    apiClient: MndApiClient,
) {
    val colors = LocalDesktopStitchColors.current
    val scope = rememberCoroutineScope()

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var activeConversation by remember { mutableStateOf<ConversationDetails?>(null) }
    var activePeerId by remember { mutableStateOf<Long?>(null) }

    var loadingList by remember { mutableStateOf(false) }
    var loadingChat by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var typedMessage by remember { mutableStateOf("") }

    fun refreshConversations() {
        scope.launch {
            loadingList = true
            sdk.conversations()
                .onSuccess {
                    conversations = it
                    if (it.isNotEmpty() && activePeerId == null) {
                        activePeerId = it.first().peer.id
                    }
                }
                .onFailure { errorMsg = it.message ?: "Failed to load messages" }
            loadingList = false
        }
    }

    fun loadActiveChat(peerId: Long, showLoading: Boolean = true) {
        scope.launch {
            if (showLoading) loadingChat = true
            apiClient.fetchConversationDetails(peerId)
                .onSuccess { activeConversation = it }
                .onFailure { errorMsg = it.message ?: "Failed to open conversation" }
            if (showLoading) loadingChat = false
        }
    }

    LaunchedEffect(Unit) {
        refreshConversations()
    }

    LaunchedEffect(activePeerId) {
        val peerId = activePeerId
        if (peerId != null) {
            loadActiveChat(peerId, showLoading = true)
            while (true) {
                kotlinx.coroutines.delay(3000)
                loadActiveChat(peerId, showLoading = false)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Conversation List
        Card(
            modifier = Modifier.width(310.dp).fillMaxHeight(),
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Conversations",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                )

                if (loadingList && conversations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.brand)
                    }
                } else if (conversations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No chats active.", color = colors.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(conversations) { c ->
                            val isActive = c.peer.id == activePeerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MndDesktopShape.panel)
                                    .background(if (isActive) colors.surfaceLow else Color.Transparent)
                                    .clickable { activePeerId = c.peer.id }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(MndDesktopShape.card).background(colors.brandMuted),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👤", fontSize = 16.sp)
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.peer.nickname.ifBlank { c.peer.username },
                                        color = colors.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = c.lastMessage,
                                        color = colors.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                if (c.unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(colors.brand),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${c.unreadCount}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Chat Bubble Panel
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                activeConversation?.let { chat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = chat.peer.nickname.ifBlank { chat.peer.username },
                                color = colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text("ID: ${chat.peer.id}", color = colors.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    Divider(color = colors.outlineVariant)
                }

                // Chat Bubbles
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    if (loadingChat) {
                        CircularProgressIndicator(
                            color = colors.brand,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        val activeChatDetails = activeConversation
                        if (activeChatDetails == null || activeChatDetails.messages.isNullOrEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Say meow to start chatting!", color = colors.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeChatDetails.messages.orEmpty()) { msg ->
                                    val isMine = msg.senderId != activePeerId
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isMine) colors.brand else colors.surfaceLow
                                            ),
                                            modifier = Modifier.widthIn(max = 280.dp)
                                        ) {
                                            Text(
                                                text = msg.content,
                                                color = if (isMine) Color.White else colors.onSurface,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Send Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = { Text("Write a message...") },
                        modifier = Modifier.weight(1f),
                        shape = MndDesktopShape.field,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brand,
                            unfocusedBorderColor = colors.outlineVariant
                        )
                    )

                    Button(
                        onClick = {
                            val peer = activePeerId
                            val content = typedMessage.trim()
                            if (peer != null && content.isNotBlank()) {
                                scope.launch {
                                    apiClient.sendMessage(peer, content)
                                        .onSuccess {
                                            typedMessage = ""
                                            loadActiveChat(peer)
                                        }
                                }
                            }
                        },
                        shape = MndDesktopShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = Color.White),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
