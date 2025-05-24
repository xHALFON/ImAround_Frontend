package com.example.myapplication.data.network

import android.annotation.SuppressLint
import android.util.Log
import com.example.myapplication.data.model.MatchResponseItem
import com.example.myapplication.data.model.MessageResponse
import com.example.myapplication.data.model.MessagesReadResponse
import com.example.myapplication.data.model.SendMessageRequest
import com.example.myapplication.data.model.TypingIndicatorResponse
import com.example.myapplication.data.model.TypingRequest
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class SocketManager() {

    companion object {
        private const val TAG = "SocketManager"
        private var instance: SocketManager? = null

        fun getInstance(): SocketManager {
            if (instance == null) {
                instance = SocketManager()
            }
            return instance!!
        }
    }

    private var socket: Socket? = null
    private val gson = Gson()
    private var serverUrl: String? = null
    private var currentUserId: String? = null
    private var isInitialized = false

    // Match listener
    private var onMatchListener: ((MatchResponseItem) -> Unit)? = null
    private var onMatchSeenListener: ((String) -> Unit)? = null
    private var onConnectListener: (() -> Unit)? = null
    private var onDisconnectListener: (() -> Unit)? = null

    // Chat listener
    private var onMessageReceivedListener: ((MessageResponse) -> Unit)? = null
    private var onMessageSentListener: ((MessageResponse) -> Unit)? = null
    private var onTypingIndicatorListener: ((TypingIndicatorResponse) -> Unit)? = null
    private var onMessageErrorListener: ((String) -> Unit)? = null
    private var onMessagesReadListener: ((MessagesReadResponse) -> Unit)? = null

    fun setOnMessagesReadListener(listener: (MessagesReadResponse) -> Unit) {
        this.onMessagesReadListener = listener
    }

    @SuppressLint("SuspiciousIndentation")
    fun init(serverUrl: String) {
        if (isInitialized && this.serverUrl == serverUrl) {
            Log.d(TAG, "🔥 Socket already initialized with same URL")
            return
        }

        try {
            Log.d(TAG, "🔥 Initializing socket with URL: $serverUrl")
            this.serverUrl = serverUrl

            // Disconnect existing socket if any
            socket?.disconnect()

            socket = IO.socket(serverUrl)
            isInitialized = true
            Log.d(TAG, "🔥 Socket initialized successfully")
            setupSocketEvents()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Socket initialization error", e)
            isInitialized = false
        }
    }

    private fun setupSocketEvents() {
        socket?.let { socket ->
            Log.d(TAG, "🔥 Setting up socket events")

            // Connection events
            socket.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "🔥 Socket connected successfully")
                onConnectListener?.invoke()
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "🔥 Socket disconnected")
                onDisconnectListener?.invoke()
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                if (args.isNotEmpty()) {
                    val error = args[0]
                    Log.e(TAG, "❌ Connection error: $error")
                    if (error is Exception) {
                        Log.e(TAG, "❌ Error details: ${error.message}")
                    }
                } else {
                    Log.e(TAG, "❌ Connection error with no details")
                }
            }

            // Match events
            socket.on("new_match") { args ->
                try {
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val matchData = args[0] as JSONObject
                        Log.d(TAG, "🔥 Received match: $matchData")
                        val match = parseMatchData(matchData)
                        onMatchListener?.invoke(match)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing match data: ${e.message}")
                }
            }

            // Chat events
            socket.on("new_message") { args ->
                try {
                    Log.d(TAG, "🔥 Received new_message: $args")
                    if (args.isNotEmpty()) {
                        try {
                            val jsonString = args[0].toString()
                            Log.d(TAG, "🔥 Message JSON: $jsonString")
                            val messageResponse = gson.fromJson(jsonString, MessageResponse::class.java)
                            Log.d(TAG, "🔥 Parsed message: $messageResponse")
                            onMessageReceivedListener?.invoke(messageResponse)
                        } catch (ex: Exception) {
                            Log.e(TAG, "❌ Error parsing message data", ex)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling received message", e)
                }
            }

            socket.on("message_sent") { args ->
                try {
                    Log.d(TAG, "🔥 Received message_sent confirmation")
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        val messageResponse = gson.fromJson(data.toString(), MessageResponse::class.java)
                        onMessageSentListener?.invoke(messageResponse)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling sent message confirmation", e)
                }
            }

            socket.on("message_error") { args ->
                try {
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        val errorMessage = data.optString("error", "Unknown error sending message")
                        Log.e(TAG, "❌ Message error: $errorMessage")
                        onMessageErrorListener?.invoke(errorMessage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling message error", e)
                }
            }

            socket.on("typing_indicator") { args ->
                try {
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        val typingResponse = gson.fromJson(data.toString(), TypingIndicatorResponse::class.java)
                        onTypingIndicatorListener?.invoke(typingResponse)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling typing indicator", e)
                }
            }

            socket.on("messages_read") { args ->
                try {
                    Log.d(TAG, "🔥 Received messages_read event")
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        val messagesReadResponse = MessagesReadResponse(
                            matchId = data.getString("matchId"),
                            readBy = data.getString("readBy")
                        )
                        onMessagesReadListener?.invoke(messagesReadResponse)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling messages read event", e)
                }
            }
        }
    }

    // Parse JSON match data to MatchResponseItem
    private fun parseMatchData(jsonData: JSONObject): MatchResponseItem {
        val matchId = jsonData.optString("_id")
        val participantsArray = jsonData.optJSONArray("participants")
        val likedArray = jsonData.optJSONArray("liked")
        val seen = jsonData.optBoolean("seen", false)

        val participants = mutableListOf<String>()
        val liked = mutableListOf<String>()

        // Parse participants array
        for (i in 0 until (participantsArray?.length() ?: 0)) {
            participants.add(participantsArray!!.optString(i))
        }

        // Parse liked array
        for (i in 0 until (likedArray?.length() ?: 0)) {
            liked.add(likedArray!!.optString(i))
        }

        return MatchResponseItem(
            _id = matchId,
            participants = participants,
            liked = liked,
            seen = seen
        )
    }

    // Connect to socket server with improved logic
    fun connect(userId: String) {
        Log.d(TAG, "🔥 Attempting to connect socket for user: $userId")

        if (!isInitialized) {
            Log.e(TAG, "❌ Socket not initialized, cannot connect")
            return
        }

        currentUserId = userId

        socket?.let { socket ->
            if (socket.connected()) {
                Log.d(TAG, "🔥 Socket already connected, but ensuring user registration")
                // Even if connected, ensure user is registered
                socket.emit("user_connected", userId)
                return
            }

            try {
                Log.d(TAG, "🔥 Connecting socket...")
                socket.connect()

                // Set up connection listener for THIS connection attempt
                socket.off(Socket.EVENT_CONNECT) // Remove old listeners
                socket.on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "🔥 Socket connected, registering user: $userId")
                    socket.emit("user_connected", userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error connecting socket", e)
            }
        } ?: run {
            Log.e(TAG, "❌ Socket is null, cannot connect")
        }
    }

    // Reconnect with current user
    fun reconnect() {
        currentUserId?.let { userId ->
            Log.d(TAG, "🔥 Reconnecting socket for user: $userId")
            connect(userId)
        } ?: run {
            Log.e(TAG, "❌ No current user ID for reconnection")
        }
    }

    // Disconnect from socket server
    fun disconnect() {
        Log.d(TAG, "🔥 Disconnecting socket")
        socket?.disconnect()
        currentUserId = null
    }

    // Check if socket is connected
    fun isConnected(): Boolean {
        val connected = socket?.connected() ?: false
        Log.d(TAG, "🔥 Socket connected status: $connected")
        return connected
    }

    // Message methods
    fun sendMessage(matchId: String, sender: String, recipient: String, content: String) {
        socket?.let { socket ->
            if (socket.connected()) {
                val request = SendMessageRequest(matchId, sender, recipient, content)
                socket.emit("send_message", JSONObject(gson.toJson(request)))
                Log.d(TAG, "🔥 Message sent: $content")
            } else {
                Log.e(TAG, "❌ Cannot send message, socket not connected")
            }
        }
    }

    fun markMessagesAsRead(chatId: String, userId: String, matchId: String) {
        socket?.let { socket ->
            if (socket.connected()) {
                val data = JSONObject().apply {
                    put("chatId", chatId)
                    put("userId", userId)
                    put("matchId", matchId)
                }
                socket.emit("mark_messages_as_read", data)
                Log.d(TAG, "🔥 Marked messages as read")
            } else {
                Log.e(TAG, "❌ Cannot mark messages as read, socket not connected")
            }
        }
    }

    fun sendTypingIndicator(matchId: String, userId: String, isTyping: Boolean) {
        socket?.let { socket ->
            if (socket.connected()) {
                val request = TypingRequest(matchId, userId)
                if (isTyping) {
                    socket.emit("typing", JSONObject(gson.toJson(request)))
                } else {
                    socket.emit("stop_typing", JSONObject(gson.toJson(request)))
                }
            }
        }
    }

    // Listener setters
    fun setOnMatchListener(listener: (MatchResponseItem) -> Unit) {
        this.onMatchListener = listener
    }

    fun setOnMatchSeenListener(listener: (String) -> Unit) {
        this.onMatchSeenListener = listener
    }

    fun setOnConnectListener(listener: () -> Unit) {
        this.onConnectListener = listener
    }

    fun setOnDisconnectListener(listener: () -> Unit) {
        this.onDisconnectListener = listener
    }

    fun setOnMessageReceivedListener(listener: (MessageResponse) -> Unit) {
        this.onMessageReceivedListener = listener
    }

    fun setOnMessageSentListener(listener: (MessageResponse) -> Unit) {
        this.onMessageSentListener = listener
    }

    fun setOnTypingIndicatorListener(listener: (TypingIndicatorResponse) -> Unit) {
        this.onTypingIndicatorListener = listener
    }

    fun setOnMessageErrorListener(listener: (String) -> Unit) {
        this.onMessageErrorListener = listener
    }
}