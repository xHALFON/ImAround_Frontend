package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.model.Chat
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.MessageResponse
import com.example.myapplication.data.model.MessagesReadResponse
import com.example.myapplication.data.model.TypingIndicatorResponse
import com.example.myapplication.data.network.ChatService
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.network.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository {
    private val TAG = "ChatRepository"
    private val chatService = RetrofitClient.chatService
    private val socketManager = SocketManager.getInstance()

    // Get all chats for a user
    suspend fun getUserChats(userId: String): List<Chat> {
        return withContext(Dispatchers.IO) {
            try {
                val chats = chatService.getUserChats(userId)
                Log.d(TAG, "Retrieved ${chats.size} chats for user $userId")
                chats
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user chats", e)
                emptyList()
            }
        }
    }

    // Get chat by match ID
    suspend fun getChatByMatchId(matchId: String): Chat? {
        return withContext(Dispatchers.IO) {
            try {
                val chat = chatService.getChatByMatchId(matchId)
                Log.d(TAG, "Retrieved chat for match $matchId with ${chat.messages.size} messages")
                chat
            } catch (e: Exception) {
                Log.e(TAG, "Error getting chat by match ID", e)
                null
            }
        }
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(chatId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = chatService.markMessagesAsRead(chatId, userId)
                Log.d(TAG, "Marked messages as read for chat $chatId by user $userId")
                result["success"] == true
            } catch (e: Exception) {
                Log.e(TAG, "Error marking messages as read", e)
                false
            }
        }
    }

    // Send a message via socket
    fun sendMessage(matchId: String, senderId: String, recipientId: String, content: String) {
        socketManager.sendMessage(matchId, senderId, recipientId, content)
    }

    // Send typing indicator
    fun sendTypingIndicator(matchId: String, userId: String, isTyping: Boolean) {
        socketManager.sendTypingIndicator(matchId, userId, isTyping)
    }
    fun markMessagesAsRead(chatId: String, userId: String, matchId: String) {
        socketManager.markMessagesAsRead(chatId, userId, matchId)
    }
    // Set up listeners
    fun setupMessageListeners(
        onMessageReceived: (MessageResponse) -> Unit,
        onMessageSent: (MessageResponse) -> Unit,
        onTypingIndicator: (TypingIndicatorResponse) -> Unit,
        onMessageError: (String) -> Unit,
        onMessagesRead: (MessagesReadResponse) -> Unit
    ) {
        socketManager.setOnMessageReceivedListener(onMessageReceived)
        socketManager.setOnMessageSentListener(onMessageSent)
        socketManager.setOnTypingIndicatorListener(onTypingIndicator)
        socketManager.setOnMessageErrorListener(onMessageError)
        socketManager.setOnMessagesReadListener(onMessagesRead)
    }

    fun isSocketConnected(): Boolean {
        return socketManager.isConnected()
    }

    fun connectSocket(userId: String) {
        socketManager.connect(userId)
    }
}