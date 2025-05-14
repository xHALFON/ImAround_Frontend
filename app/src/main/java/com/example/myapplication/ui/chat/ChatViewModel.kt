package com.example.myapplication.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.model.Chat
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.UserResponse
import com.example.myapplication.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatViewModel"
    private val chatRepository = ChatRepository()
    private val sessionManager = SessionManager(application)

    // Current user ID
    val currentUserId: String
        get() = sessionManager.getUserId() ?: "unknown"

    // Active chat
    private val _activeChat = MutableStateFlow<Chat?>(null)
    val activeChat: StateFlow<Chat?> = _activeChat

    // All user chats
    private val _userChats = MutableStateFlow<List<Chat>>(emptyList())
    val userChats: StateFlow<List<Chat>> = _userChats

    // Typing indicator state
    private val _isOtherUserTyping = MutableStateFlow(false)
    val isOtherUserTyping: StateFlow<Boolean> = _isOtherUserTyping

    // Chat partner info
    private val _chatPartner = MutableStateFlow<UserResponse?>(null)
    val chatPartner: StateFlow<UserResponse?> = _chatPartner

    // Messages for current chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Typing indicator job
    private var typingJob: Job? = null

    // Active chat tracking
    private var _lastViewedMatchId: String? = null
    private var _lastViewedPartner: UserResponse? = null
    private var _isActiveScreenVisible = false

    init {
        setupSocketListeners()
        loadUserChats()
    }

    private fun setupSocketListeners() {
        chatRepository.setupMessageListeners(
            onMessageReceived = { messageResponse ->
                val matchId = messageResponse.matchId
                val message = messageResponse.message

                Log.d(TAG, "⚡ Message received: ${message.content} for matchId: $matchId, current active matchId: $_lastViewedMatchId, isActive: $_isActiveScreenVisible")

                // בדיקה משופרת עבור עדכון הודעות
                viewModelScope.launch {
                    if (matchId == _lastViewedMatchId && _isActiveScreenVisible) {
                        // אנחנו נמצאים במסך הצ'אט הנכון - עדכן את המסך
                        Log.d(TAG, "✅ Updating active chat with new message")
                        updateChatMessages(matchId, message)
                    }

                    // תמיד עדכן את רשימת הצ'אטים (גם אם אנחנו במסך אחר)
                    updateChatInList(matchId, message)
                }
            },
            onMessageSent = { messageResponse ->
                val matchId = messageResponse.matchId
                val message = messageResponse.message

                Log.d(TAG, "Message sent confirmation for matchId: $matchId")

                viewModelScope.launch {
                    if (matchId == _lastViewedMatchId && _isActiveScreenVisible) {
                        // Find the temporary message and replace it
                        val updatedMessages = _messages.value.map { existingMsg ->
                            if (existingMsg.content == message.content && existingMsg.sender == message.sender) {
                                message
                            } else {
                                existingMsg
                            }
                        }

                        _messages.value = updatedMessages

                        // Update the chat too
                        _activeChat.value?.let { chat ->
                            _activeChat.value = chat.copy(
                                messages = updatedMessages,
                                lastActivity = message.timestamp
                            )
                        }
                    }

                    // Update chat list
                    updateChatInList(matchId, message)
                }
            },
            onTypingIndicator = { response ->
                if (_activeChat.value?.matchId == response.matchId &&
                    response.userId != currentUserId &&
                    _isActiveScreenVisible) {
                    _isOtherUserTyping.value = response.isTyping
                    Log.d(TAG, "Typing indicator from ${response.userId}: ${response.isTyping}")
                }
            },
            onMessageError = { errorMsg ->
                _errorMessage.value = errorMsg
                Log.e(TAG, "Message error: $errorMsg")
            },
            onMessagesRead = { response ->
                val matchId = response.matchId

                viewModelScope.launch {
                    if (matchId == _lastViewedMatchId && _isActiveScreenVisible) {
                        val updatedMessages = _messages.value.map { message ->
                            if (message.sender == currentUserId && !message.read) {
                                message.copy(read = true)
                            } else {
                                message
                            }
                        }

                        _messages.value = updatedMessages

                        _activeChat.value?.let { chat ->
                            _activeChat.value = chat.copy(
                                messages = updatedMessages
                            )
                        }
                    }
                }
            }
        )
    }

    // פונקציה משופרת לעדכון הודעות במסך צ'אט פעיל
    private fun updateChatMessages(matchId: String, newMessage: Message) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "⚡ Updating chat messages for matchId: $matchId")

                // עזרה עדכון ה-messages ישירות
                val updatedMessages = _messages.value.toMutableList()

                // וודא שההודעה לא כבר קיימת (למניעת כפילויות)
                if (!updatedMessages.any { it.content == newMessage.content && it.timestamp == newMessage.timestamp }) {
                    updatedMessages.add(newMessage)

                    // עדכון גלובלי
                    Log.d(TAG, "📝 Setting new messages list, size: ${updatedMessages.size}")
                    _messages.value = updatedMessages

                    // גם עדכן את ה-activeChat כדי לשמור על סנכרון
                    _activeChat.value?.let { chat ->
                        val updatedChat = chat.copy(
                            messages = updatedMessages,
                            lastActivity = newMessage.timestamp
                        )
                        _activeChat.value = updatedChat
                    }

                    // אילוץ רענון בנוסף
                    delay(50)
                    forceRefreshMessages(updatedMessages)

                    Log.d(TAG, "✅ Messages updated successfully with new message")
                } else {
                    Log.d(TAG, "⚠️ Message already exists, skipping update")
                }

                // סמן הודעות שהתקבלו כנקראות
                if (newMessage.sender != currentUserId) {
                    markMessagesAsRead()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating chat messages", e)
            }
        }
    }

    // פונקציה לאילוץ רענון ההודעות
    fun forceRefreshMessages(messages: List<Message>) {
        viewModelScope.launch {
            try {
                // טכניקה משופרת לאילוץ רה-קומפוזיציה
                _messages.value = emptyList()
                delay(20)
                _messages.value = messages
                Log.d(TAG, "🔄 Force refreshed messages, count: ${messages.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during force refresh", e)
            }
        }
    }

    // Load all chats for current user
    fun loadUserChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val chats = chatRepository.getUserChats(currentUserId)
                _userChats.value = chats.sortedByDescending { it.lastActivity }
                Log.d(TAG, "Loaded ${chats.size} chats")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user chats", e)
                _errorMessage.value = "Failed to load chats: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load a specific chat by match ID
    fun loadChatByMatchId(matchId: String, chatPartner: UserResponse) {
        // שמירת מידע אודות הצ'אט הפעיל
        _lastViewedMatchId = matchId
        _lastViewedPartner = chatPartner
        _isActiveScreenVisible = true

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val chat = chatRepository.getChatByMatchId(matchId)

                if (chat != null) {
                    Log.d(TAG, "Loaded chat with ${chat.messages.size} messages")
                    _activeChat.value = chat
                    _messages.value = chat.messages
                    _chatPartner.value = chatPartner

                    // Mark messages as read
                    if (chat.messages.any { !it.read && it.sender != currentUserId }) {
                        markMessagesAsRead()
                    }
                } else {
                    // Create an empty chat if none exists
                    Log.d(TAG, "Creating new empty chat for matchId: $matchId")
                    _activeChat.value = Chat(
                        id = "",
                        matchId = matchId,
                        participants = listOf(currentUserId, chatPartner._id),
                        messages = emptyList(),
                        lastActivity = Date(),
                        createdAt = Date()
                    )
                    _messages.value = emptyList()
                    _chatPartner.value = chatPartner
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat", e)
                _errorMessage.value = "Failed to load chat: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Send a message
    fun sendMessage(content: String) {
        val activeChat = _activeChat.value ?: return
        val chatPartner = _chatPartner.value ?: return

        // Create a temporary message
        val tempMessage = Message(
            sender = currentUserId,
            content = content,
            timestamp = Date(),
            read = false
        )

        // Add to UI immediately
        viewModelScope.launch {
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(tempMessage)
            _messages.value = currentMessages

            Log.d(TAG, "Added temporary message to UI: $content")
        }

        // Send via socket
        chatRepository.sendMessage(
            matchId = activeChat.matchId,
            senderId = currentUserId,
            recipientId = chatPartner._id,
            content = content
        )
    }

    // Handle typing indicator
    fun onTypingChanged(isTyping: Boolean) {
        val activeChat = _activeChat.value ?: return

        if (isTyping) {
            // Cancel previous job if exists
            typingJob?.cancel()

            // Send typing indicator
            chatRepository.sendTypingIndicator(activeChat.matchId, currentUserId, true)

            // Schedule to stop after a delay
            typingJob = viewModelScope.launch {
                delay(3000) // Stop typing after 3 seconds of inactivity
                chatRepository.sendTypingIndicator(activeChat.matchId, currentUserId, false)
            }
        } else {
            // Cancel previous job if exists
            typingJob?.cancel()

            // Send stop typing
            chatRepository.sendTypingIndicator(activeChat.matchId, currentUserId, false)
        }
    }

    // Mark messages as read
    fun markMessagesAsRead() {
        val activeChat = _activeChat.value ?: return

        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(activeChat.id, currentUserId, activeChat.matchId)

                val updatedMessages = _messages.value.map { message ->
                    if (message.sender != currentUserId && !message.read) {
                        message.copy(read = true)
                    } else {
                        message
                    }
                }
                _messages.value = updatedMessages

                _activeChat.value?.let { chat ->
                    val updatedChat = chat.copy(
                        messages = updatedMessages
                    )
                    _activeChat.value = updatedChat
                }

                Log.d(TAG, "Marked messages as read")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking messages as read", e)
            }
        }
    }

    // וודא שהסוקט מחובר
    fun ensureSocketConnected() {
        viewModelScope.launch {
            if (!chatRepository.isSocketConnected()) {
                Log.d(TAG, "Connecting socket...")
                chatRepository.connectSocket(currentUserId)
                delay(300) // המתן רגע קטן אחרי החיבור
            } else {
                Log.d(TAG, "Socket already connected")
            }
        }
    }

    // Clear active chat when leaving the chat screen
    fun clearActiveChat(fullClear: Boolean = true) {
        if (fullClear) {
            _lastViewedMatchId = null
            _lastViewedPartner = null
        }

        // תמיד סמן שעזבנו את המסך
        _isActiveScreenVisible = false

        // נקה את שאר מצבי הצ'אט
        if (fullClear) {
            _activeChat.value = null
            _messages.value = emptyList()
            _chatPartner.value = null
        }

        _isOtherUserTyping.value = false

        Log.d(TAG, "Cleared active chat (fullClear=$fullClear)")
    }

    // Update chat in the list
    private fun updateChatInList(matchId: String, newMessage: Message) {
        viewModelScope.launch {
            try {
                val chats = _userChats.value.toMutableList()
                val chatIndex = chats.indexOfFirst { it.matchId == matchId }

                if (chatIndex >= 0) {
                    // Update existing chat
                    val chat = chats[chatIndex]

                    // בדוק שההודעה לא כבר קיימת
                    if (!chat.messages.any { it.content == newMessage.content && it.timestamp == newMessage.timestamp }) {
                        val updatedChat = chat.copy(
                            messages = chat.messages + newMessage,
                            lastActivity = newMessage.timestamp
                        )
                        chats[chatIndex] = updatedChat

                        // Sort chats by last activity
                        chats.sortByDescending { it.lastActivity }
                        _userChats.value = chats

                        Log.d(TAG, "Updated chat in list: $matchId")
                    }
                } else {
                    // Reload all chats if the chat isn't in the list
                    Log.d(TAG, "Chat not found in list, reloading all")
                    loadUserChats()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating chat in list", e)
            }
        }
    }
}