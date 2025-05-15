package com.example.myapplication.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository.getInstance()

    companion object {
        private const val TAG = "ChatViewModel"
        private const val TYPING_TIMEOUT_MS = 3000L
    }

    // UI State
    data class ChatListState(
        val chats: List<Chat> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isRefreshing: Boolean = false
    )

    data class ChatDetailState(
        val chat: Chat? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isTyping: Boolean = false,
        val typingUsers: Set<String> = emptySet(),
        val isSending: Boolean = false
    )

    // State flows
    private val _chatListState = MutableStateFlow(ChatListState())
    val chatListState: StateFlow<ChatListState> = _chatListState.asStateFlow()

    private val _chatDetailState = MutableStateFlow(ChatDetailState())
    val chatDetailState: StateFlow<ChatDetailState> = _chatDetailState.asStateFlow()

    // Typing job for debouncing
    private var typingJob: Job? = null

    init {
        Log.d(TAG, "ChatViewModel initialized, starting to observe repository events")
        observeRepositoryEvents()
    }

    private fun observeRepositoryEvents() {
        Log.d(TAG, "Setting up repository event observers")

        // Observe chats list
        viewModelScope.launch {
            repository.chats.collect { chats ->
                _chatListState.value = _chatListState.value.copy(
                    chats = chats,
                    isLoading = false
                )
            }
        }

        // Observe current chat
        viewModelScope.launch {
            repository.currentChat.collect { chat ->
                _chatDetailState.value = _chatDetailState.value.copy(
                    chat = chat,
                    isLoading = false
                )
            }
        }

        // Observe new messages
        viewModelScope.launch {
            repository.newMessage.collect { messageResponse ->
                Log.d(TAG, "Received new message in ViewModel")
                // Update UI state if needed
            }
        }

        // Observe message sent confirmations
        viewModelScope.launch {
            Log.d(TAG, "Setting up messageSent observer")
            repository.messageSent.collect { messageResponse ->
                Log.d(TAG, "=== MESSAGE SENT IN VIEWMODEL ===")
                Log.d(TAG, "Message sent confirmation received: ${messageResponse.message.content}")
                Log.d(TAG, "Current isSending state: ${_chatDetailState.value.isSending}")
                Log.d(TAG, "Setting isSending = false")
                _chatDetailState.value = _chatDetailState.value.copy(isSending = false)
                Log.d(TAG, "New isSending state: ${_chatDetailState.value.isSending}")
            }
        }

        // Observe typing indicators
        viewModelScope.launch {
            repository.typingIndicator.collect { typingIndicator ->
                updateTypingIndicator(typingIndicator)
            }
        }

        // Observe messages read events
        viewModelScope.launch {
            Log.d(TAG, "Setting up messagesRead observer")
            repository.messagesRead.collect { messagesRead ->
                Log.d(TAG, "=== MESSAGES READ EVENT IN VIEWMODEL ===")
                Log.d(TAG, "Match ID: ${messagesRead.matchId}")
                Log.d(TAG, "Read by: ${messagesRead.readBy}")
                Log.d(TAG, "Messages read by: ${messagesRead.readBy}")
            }
        }

        // Observe message errors
        viewModelScope.launch {
            repository.messageError.collect { error ->
                Log.d(TAG, "Message error received: $error")
                _chatDetailState.value = _chatDetailState.value.copy(
                    error = error,
                    isSending = false
                )
            }
        }
    }

    // Chat List Methods
    fun loadUserChats(userId: String) {
        viewModelScope.launch {
            _chatListState.value = _chatListState.value.copy(isLoading = true, error = null)

            repository.getUserChats(userId)
                .onSuccess { chats ->
                    _chatListState.value = _chatListState.value.copy(
                        chats = chats,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _chatListState.value = _chatListState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun refreshChats(userId: String) {
        viewModelScope.launch {
            _chatListState.value = _chatListState.value.copy(isRefreshing = true)

            repository.getUserChats(userId)
                .onSuccess { chats ->
                    _chatListState.value = _chatListState.value.copy(
                        chats = chats,
                        isRefreshing = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _chatListState.value = _chatListState.value.copy(
                        isRefreshing = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    // Chat Detail Methods
    fun loadChat(matchId: String) {
        viewModelScope.launch {
            _chatDetailState.value = _chatDetailState.value.copy(isLoading = true, error = null)

            repository.getChatByMatchId(matchId)
                .onSuccess { chat ->
                    _chatDetailState.value = _chatDetailState.value.copy(
                        chat = chat,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _chatDetailState.value = _chatDetailState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load chat"
                    )
                }
        }
    }

    fun sendMessage(matchId: String, sender: String, recipient: String, content: String) {
        if (content.trim().isEmpty()) return

        Log.d(TAG, "=== SENDING MESSAGE ===")
        Log.d(TAG, "Content: $content")
        Log.d(TAG, "Setting isSending = true")

        viewModelScope.launch {
            _chatDetailState.value = _chatDetailState.value.copy(isSending = true)
            repository.sendMessage(matchId, sender, recipient, content)

            // Stop typing indicator when sending message
            stopTyping(matchId, sender)

            // Add timeout to reset isSending state if no confirmation received
            delay(3000) // 3 seconds timeout
            if (_chatDetailState.value.isSending) {
                Log.d(TAG, "Message send timeout - resetting isSending to false")
                _chatDetailState.value = _chatDetailState.value.copy(isSending = false)
            }
        }
    }

    fun markMessagesAsRead(chatId: String, userId: String, matchId: String) {
        viewModelScope.launch {
            Log.d(TAG, "=== MARKING MESSAGES AS READ ===")
            Log.d(TAG, "Chat ID: $chatId")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Match ID: $matchId")

            // Only mark via socket - remove API call to avoid race condition
            repository.markMessagesAsReadSocket(chatId, userId, matchId)
            Log.d(TAG, "Sent mark as read via socket only")
        }
    }

    // Typing Methods
    fun startTyping(matchId: String, userId: String) {
        repository.sendTypingIndicator(matchId, userId, true)
        _chatDetailState.value = _chatDetailState.value.copy(isTyping = true)

        // Cancel previous typing job and start a new one
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(TYPING_TIMEOUT_MS)
            stopTyping(matchId, userId)
        }
    }

    fun stopTyping(matchId: String, userId: String) {
        typingJob?.cancel()
        repository.sendTypingIndicator(matchId, userId, false)
        _chatDetailState.value = _chatDetailState.value.copy(isTyping = false)
    }

    private fun updateTypingIndicator(typingIndicator: TypingIndicatorResponse) {
        val currentTypingUsers = _chatDetailState.value.typingUsers.toMutableSet()

        if (typingIndicator.isTyping) {
            currentTypingUsers.add(typingIndicator.userId)
        } else {
            currentTypingUsers.remove(typingIndicator.userId)
        }

        _chatDetailState.value = _chatDetailState.value.copy(
            typingUsers = currentTypingUsers
        )
    }

    // Socket Methods
    fun connectSocket(userId: String) {
        repository.connectSocket(userId)
    }

    fun disconnectSocket() {
        repository.disconnectSocket()
    }

    fun isSocketConnected(): Boolean {
        return repository.isSocketConnected()
    }

    // Helper Methods
    fun clearError() {
        _chatDetailState.value = _chatDetailState.value.copy(error = null)
        _chatListState.value = _chatListState.value.copy(error = null)
    }

    fun clearCurrentChat() {
        repository.clearCurrentChat()
        _chatDetailState.value = ChatDetailState()
    }

    // Get unread message count for a chat
    fun getUnreadMessageCount(chat: Chat, currentUserId: String): Int {
        return chat.messages.count { message ->
            message.sender != currentUserId && !message.read
        }
    }

    // Get the other participant in a chat
    fun getOtherParticipant(chat: Chat, currentUserId: String): String? {
        return chat.participants.find { it != currentUserId }
    }

    // Cache for user details to avoid repeated API calls
    private val userCache = mutableMapOf<String, UserResponse>()

    suspend fun getUserDetails(userId: String): UserResponse? {
        return try {
            // Check cache first
            userCache[userId]?.let {
                Log.d(TAG, "Using cached user details for $userId")
                return it
            }

            // Fetch from API if not in cache
            repository.getUserDetails(userId)
                .onSuccess { userDetails ->
                    userCache[userId] = userDetails
                    Log.d(TAG, "Cached user details for $userId: ${userDetails.firstName} ${userDetails.lastName}")
                    return userDetails
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to fetch user details for $userId", error)
                    return null
                }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user details for $userId", e)
            null
        }
    }

    // Get the last message text for chat list
    fun getLastMessageText(chat: Chat): String {
        return chat.messages.lastOrNull()?.content ?: "No messages yet"
    }

    // Format time for display
    fun formatMessageTime(timestamp: java.util.Date): String {
        val now = System.currentTimeMillis()
        val messageTime = timestamp.time
        val diff = now - messageTime

        return when {
            diff < 60000 -> "Just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
            diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
            else -> "${diff / 86400000}d ago" // Days ago
        }
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        repository.disconnectSocket()
    }
}