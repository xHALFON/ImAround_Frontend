package com.example.myapplication.ui.chat

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.UserResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// צבעים
val Purple = Color(0xFF6200EE)
val White = Color.White
val Black = Color.Black
val Gray = Color.Gray
val Green = Color(0xFF4CAF50)
val Blue = Color(0xFF2196F3)
val Amber = Color(0xFFFFC107)
val LightGray = Color(0xFFE0E0E0)
val LightGrayBackground = Color(0xFFF0F0F0)
val LightBackground = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    matchId: String,
    chatPartner: UserResponse,
    onBackClick: () -> Unit,
    sessionManager: SessionManager,
    viewModel: ChatViewModel = viewModel()
) {
    val currentUserId = sessionManager.getUserId() ?: "unknown"
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val isOtherUserTyping by viewModel.isOtherUserTyping.collectAsState(initial = false)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val activeChat by viewModel.activeChat.collectAsState(initial = null)


    val listState = rememberLazyListState()
    // Add log to track activeChat state changes
    LaunchedEffect(activeChat) {
        Log.e("ChatDetailScreen", "Active chat state changed: ${activeChat?.matchId ?: "null"}")
    }

    // Load chat when screen is displayed
    LaunchedEffect(key1 = matchId) {
        Log.e("ChatDetailScreen", "Loading chat for matchId: $matchId")
        viewModel.ensureSocketConnected()
        viewModel.loadChatByMatchId(matchId, chatPartner)
    }

    // Better cleanup when leaving screen
    DisposableEffect(key1 = Unit) {
        onDispose {
            Log.e("ChatDetailScreen", "DisposableEffect cleaning up. Current matchId: $matchId")
            viewModel.clearActiveChat(fullClear = false)
        }
    }
    // Scroll to bottom when new messages arrive
    LaunchedEffect(key1 = messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // AI Tips dialog state
    var showAITips by remember { mutableStateOf(false) }
    var tipContent by remember { mutableStateOf(getRandomTips()) }

    Scaffold(
        topBar = {
            ChatHeader(
                partner = chatPartner,
                isOnline = true,
                onBackClick = onBackClick,
                onAITipsClick = {
                    showAITips = !showAITips
                    if (showAITips) {
                        tipContent = getRandomTips()
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                onSendMessage = { message ->
                    viewModel.sendMessage(message)
                },
                onTypingChanged = { isTyping ->
                    viewModel.onTypingChanged(isTyping)
                }
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        // Messages list
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(messages) { message ->
                    MessageItem(
                        message = message,
                        isFromCurrentUser = message.sender == currentUserId,
                        partnerAvatarUrl = chatPartner.avatar
                    )
                }
            }

            // Typing indicator
            AnimatedVisibility(
                visible = isOtherUserTyping,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 4.dp),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    text = "Typing...",
                    color = Gray,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }

    // AI Tips dialog
    if (showAITips) {
        AlertDialog(
            onDismissRequest = { showAITips = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conversation Tips 💡",
                        color = Purple,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showAITips = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Text(
                    text = tipContent,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = {
                            tipContent = getRandomTips()
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("More Tips")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHeader(
    partner: UserResponse,
    isOnline: Boolean,
    onBackClick: () -> Unit,
    onAITipsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LightGray)
                ) {
                    AsyncImage(
                        model = partner.avatar.ifEmpty { null },
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Name and status
                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${partner.firstName} ${partner.lastName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        fontSize = 12.sp,
                        color = if (isOnline) Green else Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // AI Tips button
            IconButton(onClick = onAITipsClick) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "AI Tips",
                    tint = Amber,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun MessageItem(
    message: Message,
    isFromCurrentUser: Boolean,
    partnerAvatarUrl: String
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile image (only for received messages)
            if (!isFromCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LightGray)
                        .padding(end = 8.dp)
                ) {
                    AsyncImage(
                        model = partnerAvatarUrl.ifEmpty { null },
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Message bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isFromCurrentUser) 12.dp else 4.dp,
                            bottomEnd = if (isFromCurrentUser) 4.dp else 12.dp
                        )
                    )
                    .background(if (isFromCurrentUser) Purple else White)
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isFromCurrentUser) White else Black
                )
            }
        }

        // Time and read status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = timeFormat.format(message.timestamp),
                fontSize = 10.sp,
                color = Gray,
                modifier = Modifier.padding(
                    start = if (isFromCurrentUser) 0.dp else 40.dp,
                    end = 4.dp
                )
            )

            // Read status (only for sent messages)
            if (isFromCurrentUser) {
                Icon(
                    imageVector = if (message.read) Icons.Default.Done else Icons.Default.Schedule,
                    contentDescription = if (message.read) "Read" else "Sent",
                    tint = if (message.read) Blue else Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle typing indicator with debounce
    val isTyping = remember { mutableStateOf(false) }

    LaunchedEffect(messageText) {
        isTyping.value = messageText.isNotEmpty()
        onTypingChanged(isTyping.value)

        if (messageText.isEmpty()) {
            onTypingChanged(false)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Message input field
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp)
                    .onFocusChanged { state ->
                        if (!state.isFocused && isTyping.value) {
                            onTypingChanged(false)
                            isTyping.value = false
                        }
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = LightGrayBackground,
                    focusedContainerColor = LightGrayBackground,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("Message") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageText.isNotEmpty()) {
                            onSendMessage(messageText)
                            messageText = ""
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                )
            )

            // Send button
            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = {
                    if (messageText.isNotEmpty()) {
                        onSendMessage(messageText)
                        messageText = ""
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Purple
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = White
                )
            }
        }
    }
}

// Helper function for AI tips
private fun getRandomTips(): String {
    val tips = listOf(
        "Ask open-ended questions to keep the conversation flowing.",
        "Share something interesting about your day to create a connection.",
        "Find common interests by asking about hobbies and passions.",
        "Use humor to lighten the mood - a good laugh can break the ice.",
        "Show genuine interest by following up on details they share.",
        "Share a funny story about yourself to show vulnerability.",
        "Ask about their weekend plans or a recent adventure.",
        "Discuss favorite movies, books, or TV shows to find common ground.",
        "Share a unique experience or travel story to stand out.",
        "Ask what they're passionate about - people love talking about their interests."
    )

    val randomTips = tips.shuffled().take(4)
    val tipsText = StringBuilder("Here are some conversation starters:\n\n")

    randomTips.forEach { tip ->
        tipsText.append("• $tip\n\n")
    }

    return tipsText.toString().trim()
}