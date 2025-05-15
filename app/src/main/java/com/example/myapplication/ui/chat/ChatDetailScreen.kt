package com.example.myapplication.ui.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.UserResponse
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    matchId: String,
    chatPartner: UserResponse,
    onBackClick: () -> Unit,
    sessionManager: SessionManager,
    viewModel: ChatViewModel
) {
    val chatDetailState by viewModel.chatDetailState.collectAsStateWithLifecycle()
    val currentUserId = sessionManager.getUserId() ?: ""

    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Load chat when screen is opened
    LaunchedEffect(matchId) {
        Log.d("ChatDetail", "Loading chat with matchId: '$matchId'")
        viewModel.loadChat(matchId)
    }

    // Mark messages as read when chat is loaded AND when new messages arrive
    LaunchedEffect(chatDetailState.chat) {
        chatDetailState.chat?.let { chat ->
            Log.d("ChatDetail", "Chat loaded, marking messages as read")
            Log.d("ChatDetail", "Chat ID: ${chat.id}")
            Log.d("ChatDetail", "Current user: $currentUserId")
            Log.d("ChatDetail", "Match ID: $matchId")
            Log.d("ChatDetail", "Unread messages count: ${chat.messages.count { !it.read && it.sender != currentUserId }}")

            // Only mark as read if there are unread messages from other users
            val hasUnreadMessages = chat.messages.any { !it.read && it.sender != currentUserId }
            if (hasUnreadMessages) {
                viewModel.markMessagesAsRead(chat.id, currentUserId, matchId)
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatDetailState.chat?.messages?.size) {
        val messageCount = chatDetailState.chat?.messages?.size ?: 0
        Log.d("ChatDetail", "Message count changed: $messageCount")
        if (messageCount > 0) {
            delay(100) // Small delay to ensure UI is updated
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    // Handle typing indicators
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty() && !isTyping) {
            isTyping = true
            viewModel.startTyping(matchId, currentUserId)
        } else if (messageText.isEmpty() && isTyping) {
            isTyping = false
            viewModel.stopTyping(matchId, currentUserId)
        }
    }

    // Clear current chat when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCurrentChat()
            if (isTyping) {
                viewModel.stopTyping(matchId, currentUserId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with image support
                        if (chatPartner.avatar.isNotEmpty()) {
                            Log.d("ChatDetail", "Loading avatar: ${chatPartner.avatar}")
                            AsyncImage(
                                model = chatPartner.avatar,
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Log.d("ChatDetail", "No avatar URL provided")
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${chatPartner.firstName} ${chatPartner.lastName}".trim()
                                    .ifEmpty { "Chat Partner" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (chatDetailState.typingUsers.isNotEmpty()) {
                                Text(
                                    text = "Typing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Type a message...",
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.trim().isNotEmpty()) {
                                    viewModel.sendMessage(
                                        matchId = matchId,
                                        sender = currentUserId,
                                        recipient = chatPartner._id,
                                        content = messageText.trim()
                                    )
                                    messageText = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (messageText.trim().isNotEmpty() && !chatDetailState.isSending) {
                                viewModel.sendMessage(
                                    matchId = matchId,
                                    sender = currentUserId,
                                    recipient = chatPartner._id,
                                    content = messageText.trim()
                                )
                                messageText = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (messageText.trim().isNotEmpty() && !chatDetailState.isSending)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        contentColor = if (messageText.trim().isNotEmpty() && !chatDetailState.isSending)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.outline
                    ) {
                        if (chatDetailState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message"
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error handling
            chatDetailState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            when {
                chatDetailState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                chatDetailState.chat?.messages?.isEmpty() == true -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "💬",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Start the conversation!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = chatDetailState.chat?.messages ?: emptyList(),
                            key = { "${it.sender}-${it.timestamp.time}-${it.content}" }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.sender == currentUserId,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    viewModel: ChatViewModel
) {
    val timeFormatted = viewModel.formatMessageTime(message.timestamp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (message.read) "✓✓" else "✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.read)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}