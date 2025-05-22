package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.model.*
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.network.SocketManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatRepository {
    private val chatService = RetrofitClient.chatService
    private val socketManager = SocketManager.getInstance()

    companion object {
        private const val TAG = "ChatRepository"

        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun getInstance(): ChatRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatRepository().also { INSTANCE = it }
            }
        }
    }

    // State flows for reactive updates
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()

    // Flows for socket events
    private val _newMessage = MutableSharedFlow<MessageResponse>(replay = 1)
    val newMessage: Flow<MessageResponse> = _newMessage

    private val _messageSent = MutableSharedFlow<MessageResponse>(replay = 1)
    val messageSent: Flow<MessageResponse> = _messageSent

    private val _typingIndicator = MutableSharedFlow<TypingIndicatorResponse>(replay = 1)
    val typingIndicator: Flow<TypingIndicatorResponse> = _typingIndicator

    private val _messagesRead = MutableSharedFlow<MessagesReadResponse>(replay = 1)
    val messagesRead: Flow<MessagesReadResponse> = _messagesRead

    private val _messageError = MutableSharedFlow<String>(replay = 1)
    val messageError: Flow<String> = _messageError

    init {
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        // Listen for new messages
        socketManager.setOnMessageReceivedListener { messageResponse ->
            Log.d(TAG, "=== NEW MESSAGE RECEIVED ===")
            Log.d(TAG, "Message: ${messageResponse.message.content}")
            Log.d(TAG, "From: ${messageResponse.message.sender}")
            _newMessage.tryEmit(messageResponse)
            updateChatWithNewMessage(messageResponse)
        }

        // Listen for message sent confirmation
        socketManager.setOnMessageSentListener { messageResponse ->
            Log.d(TAG, "=== MESSAGE SENT CONFIRMATION ===")
            Log.d(TAG, "Message: ${messageResponse.message.content}")
            Log.d(TAG, "From: ${messageResponse.message.sender}")
            Log.d(TAG, "Emitting to messageSent flow")
            _messageSent.tryEmit(messageResponse)
            updateChatWithNewMessage(messageResponse)
        }

        // Listen for typing indicators
        socketManager.setOnTypingIndicatorListener { typingIndicator ->
            Log.d(TAG, "Typing indicator: ${typingIndicator.userId} is typing: ${typingIndicator.isTyping}")
            _typingIndicator.tryEmit(typingIndicator)
        }

        // Listen for messages read events
        socketManager.setOnMessagesReadListener { messagesRead ->
            Log.d(TAG, "Messages read by: ${messagesRead.readBy}")
            _messagesRead.tryEmit(messagesRead)
            updateMessagesAsRead(messagesRead)
        }

        // Listen for message errors
        socketManager.setOnMessageErrorListener { error ->
            Log.e(TAG, "Message error: $error")
            _messageError.tryEmit(error)
        }
    }

    // API Methods
    suspend fun getUserChats(userId: String): Result<List<Chat>> {
        return try {
            val response = chatService.getUserChats(userId)
            _chats.value = response
            Log.d(TAG, "Loaded ${response.size} chats for user $userId")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user chats", e)
            Result.failure(e)
        }
    }

    suspend fun getChatByMatchId(matchId: String): Result<Chat> {
        return try {
            val chat = chatService.getChatByMatchId(matchId)
            _currentChat.value = chat
            Log.d(TAG, "Successfully loaded chat for match $matchId")
            Log.d(TAG, "Chat details - id: ${chat.id}, matchId: ${chat.matchId}, messages: ${chat.messages.size}")
            Result.success(chat)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading chat for match $matchId", e)
            Result.failure(e)
        }
    }

    suspend fun getUserDetails(userId: String): Result<UserResponse> {
        return try {
            val userDetails = chatService.getUserDetails(userId)
            Log.d(TAG, "Loaded user details for $userId: ${userDetails.firstName} ${userDetails.lastName}")
            Result.success(userDetails)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user details for $userId", e)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Boolean> {
        return try {
            chatService.markMessagesAsRead(chatId, userId)
            Log.d(TAG, "Marked messages as read for chat $chatId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    // Socket Methods
    fun connectSocket(userId: String) {
        socketManager.connect(userId)
        Log.d(TAG, "Connected to socket with userId: $userId")
    }

    fun disconnectSocket() {
        socketManager.disconnect()
        Log.d(TAG, "Disconnected from socket")
    }

    fun sendMessage(matchId: String, sender: String, recipient: String, content: String) {
        socketManager.sendMessage(matchId, sender, recipient, content)
        Log.d(TAG, "Sending message: $content")
    }

    fun sendTypingIndicator(matchId: String, userId: String, isTyping: Boolean) {
        socketManager.sendTypingIndicator(matchId, userId, isTyping)
        Log.d(TAG, "Sending typing indicator: $isTyping")
    }

    fun markMessagesAsReadSocket(chatId: String, userId: String, matchId: String) {
        socketManager.markMessagesAsRead(chatId, userId, matchId)
        Log.d(TAG, "Marking messages as read via socket")
    }

    fun isSocketConnected(): Boolean {
        return socketManager.isConnected()
    }

    private fun updateChatWithNewMessage(messageResponse: MessageResponse) {
        Log.d(TAG, "=== UPDATING CHAT WITH NEW MESSAGE ===")
        Log.d(TAG, "Message matchId: '${messageResponse.matchId}'")
        Log.d(TAG, "Message content: ${messageResponse.message.content}")
        Log.d(TAG, "Message sender: ${messageResponse.message.sender}")

        // תמיד חפש את השיחה ברשימה, לא משנה מה המצב הנוכחי
        val currentChats = _chats.value.toMutableList()
        val chatIndex = currentChats.indexOfFirst { it.matchId == messageResponse.matchId }

        if (chatIndex != -1) {
            Log.d(TAG, "Found chat in list at index $chatIndex")
            val existingChat = currentChats[chatIndex]

            // בדוק אם ההודעה כבר קיימת (למנוע כפילויות)
            val messageExists = existingChat.messages.any {
                it.content == messageResponse.message.content &&
                        it.sender == messageResponse.message.sender &&
                        it.timestamp == messageResponse.message.timestamp
            }

            if (!messageExists) {
                Log.d(TAG, "Message is new, adding to chat")
                val updatedMessages = existingChat.messages + messageResponse.message
                val updatedChat = existingChat.copy(
                    messages = updatedMessages,
                    lastActivity = messageResponse.message.timestamp
                )

                // עדכון השיחה ברשימה
                currentChats[chatIndex] = updatedChat

                // מיון לפי פעילות אחרונה (החדש ביותר בראש)
                currentChats.sortByDescending { it.lastActivity }

                // עדכון State
                _chats.value = currentChats
                Log.d(TAG, "Updated chats list, chat moved to position: ${currentChats.indexOfFirst { it.id == updatedChat.id }}")

                // אם זו השיחה הפתוחה כרגע, עדכן גם אותה
                if (_currentChat.value?.matchId == messageResponse.matchId) {
                    Log.d(TAG, "Also updating current chat")
                    _currentChat.value = updatedChat
                }
            } else {
                Log.d(TAG, "Message already exists, skipping")
            }
        } else {
            Log.d(TAG, "Chat not found in list!")
            Log.d(TAG, "Available chats: ${currentChats.map { "matchId: '${it.matchId}'" }}")

            // אם השיחה לא נמצאת ברשימה, נסה לטעון אותה מחדש
            // זה יכול לקרות אם המשתמש קיבל הודעה לשיחה שעדיין לא נטענה
            Log.d(TAG, "Chat not in list, might need to refresh chats")
        }
    }

    private fun updateChatInList(updatedChat: Chat) {
        val currentChats = _chats.value.toMutableList()
        val index = currentChats.indexOfFirst { it.id == updatedChat.id }
        if (index != -1) {
            currentChats[index] = updatedChat
            // Sort by last activity (most recent first)
            currentChats.sortByDescending { it.lastActivity }
            _chats.value = currentChats
        }
    }

    private fun updateMessagesAsRead(messagesRead: MessagesReadResponse) {
        Log.d(TAG, "=== UPDATING MESSAGES AS READ ===")
        Log.d(TAG, "Match ID: ${messagesRead.matchId}")
        Log.d(TAG, "Read by: ${messagesRead.readBy}")

        val currentChat = _currentChat.value
        if (currentChat?.matchId == messagesRead.matchId) {
            Log.d(TAG, "Found matching chat, updating messages")
            val updatedMessages = currentChat.messages.map { message ->
                if (message.sender != messagesRead.readBy) {
                    Log.d(TAG, "Marking message as read: ${message.content.take(20)}...")
                    message.copy(read = true)
                } else {
                    message
                }
            }
            val updatedChat = currentChat.copy(messages = updatedMessages)
            _currentChat.value = updatedChat
            Log.d(TAG, "Updated current chat with read status")
            updateChatInList(updatedChat)
        } else {
            Log.d(TAG, "No matching chat found for read update")
        }
    }

    // Method to clear current chat when leaving chat detail
    fun clearCurrentChat() {
        _currentChat.value = null
    }
}