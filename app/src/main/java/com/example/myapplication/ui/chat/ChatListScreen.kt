package com.example.myapplication.ui.chat.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.model.Chat
import com.example.myapplication.data.model.UserResponse
import com.example.myapplication.ui.chat.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

// צבעים
val Purple = Color(0xFF6200EE)
val White = Color.White
val Black = Color.Black
val Gray = Color.Gray
val LightGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatSelected: (String, UserResponse) -> Unit,
    sessionManager: SessionManager,
    viewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val chats by viewModel.userChats.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val currentUserId = sessionManager.getUserId() ?: "unknown"

    // Load chats when screen is displayed
    LaunchedEffect(key1 = Unit) {
        viewModel.loadUserChats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { innerPadding ->
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (chats.isEmpty() && !isLoading) {
                // Empty state
                Text(
                    text = "No conversations yet.\nGo match with someone!",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .wrapContentSize(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = Gray,
                    fontSize = 18.sp
                )
            } else {
                // Chat list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(chats) { chat ->
                        ChatItem(
                            chat = chat,
                            currentUserId = currentUserId,
                            onClick = {
                                // Get partner user ID
                                val partnerId = chat.participants.find { it != currentUserId } ?: return@ChatItem

                                // Create temporary user (you'd fetch this from your API in a real app)
                                val partnerUser = UserResponse(
                                    _id = partnerId,
                                    firstName = "User", // Replace with actual data
                                    lastName = partnerId.take(4), // Replace with actual data
                                    email = "", // Replace with actual data
                                    avatar = "" // Replace with actual data
                                )

                                onChatSelected(chat.matchId, partnerUser)
                            }
                        )
                    }
                }
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
}

@Composable
fun ChatItem(
    chat: Chat,
    currentUserId: String,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateTimeFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    // Get partner user ID
    val partnerId = chat.participants.find { it != currentUserId } ?: return

    // Determine last message and time
    val lastMessage = chat.messages.lastOrNull()
    val lastMessageTime = lastMessage?.timestamp ?: chat.lastActivity

    // Format time
    val formattedTime = when {
        isSameDay(Calendar.getInstance(), lastMessageTime) ->
            timeFormat.format(lastMessageTime)
        isYesterday(Calendar.getInstance(), lastMessageTime) ->
            "Yesterday"
        else ->
            dateTimeFormat.format(lastMessageTime)
    }

    // Count unread messages
    val unreadCount = chat.messages.count { !it.read && it.sender != currentUserId }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            AsyncImage(
                model = "", // Replace with user avatar URL
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.iamaround_logo_new)
            )

            // Message info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Name
                    Text(
                        text = "User ${partnerId.take(4)}", // Replace with actual user name
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Time
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        color = Gray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message
                    Text(
                        text = when {
                            lastMessage != null -> {
                                if (lastMessage.sender == currentUserId) {
                                    "You: ${lastMessage.content}"
                                } else {
                                    lastMessage.content
                                }
                            }
                            else -> "Start a conversation"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread count
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Purple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                color = White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = LightGray
        )
    }
}

// Helper functions
private fun isSameDay(cal1: Calendar, date: Date): Boolean {
    val cal2 = Calendar.getInstance().apply { time = date }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(today: Calendar, date: Date): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val otherDay = Calendar.getInstance().apply { time = date }

    return yesterday.get(Calendar.YEAR) == otherDay.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == otherDay.get(Calendar.DAY_OF_YEAR)
}

// Simple color fill placeholder
class ColorFillPlaceholder(private val color: Color) : Any() {
    // This is just a placeholder that works with coil
    override fun toString(): String = "ColorFillPlaceholder(color=$color)"
}