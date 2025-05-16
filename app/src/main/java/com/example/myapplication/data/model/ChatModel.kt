package com.example.myapplication.data.model


import com.google.gson.annotations.SerializedName
import java.util.Date

data class Message(
    val sender: String,
    val content: String,
    @SerializedName("timestamp") val timestamp: Date,
    val read: Boolean = false
)

data class Chat(
    @SerializedName("_id") val id: String,
    val matchId: String,
    val participants: List<String>,
    val messages: List<Message>,
    val lastActivity: Date,
    val createdAt: Date
)

// Request and Response models
data class SendMessageRequest(
    val matchId: String,
    val sender: String,
    val recipient: String,
    val content: String
)

data class TypingRequest(
    val matchId: String,
    val userId: String
)

data class MessageResponse(
    val matchId: String,
    val message: Message
)

data class TypingIndicatorResponse(
    val matchId: String,
    val userId: String,
    val isTyping: Boolean
)
data class MessagesReadResponse(
    val matchId: String,
    val readBy: String
)
data class ChatTipsResponse(
    val tips: List<String>
)