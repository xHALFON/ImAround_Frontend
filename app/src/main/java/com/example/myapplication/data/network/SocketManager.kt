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

    private var socket: Socket? = null
    private val gson = Gson()
//match listener
    private var onMatchListener: ((MatchResponseItem) -> Unit)? = null
    private var onMatchSeenListener: ((String) -> Unit)? = null
    private var onConnectListener: (() -> Unit)? = null
    private var onDisconnectListener: (() -> Unit)? = null
//chat listener
    private var onMessageReceivedListener: ((MessageResponse) -> Unit)? = null
    private var onMessageSentListener: ((MessageResponse) -> Unit)? = null
    private var onTypingIndicatorListener: ((TypingIndicatorResponse) -> Unit)? = null
    private var onMessageErrorListener: ((String) -> Unit)? = null

    private var onMessagesReadListener: ((MessagesReadResponse) -> Unit)? = null

    fun setOnMessagesReadListener(listener: (MessagesReadResponse) -> Unit) {
        this.onMessagesReadListener = listener
    }

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

    @SuppressLint("SuspiciousIndentation")
    fun init(serverUrl: String) {
        try {
            socket = IO.socket(serverUrl)
            Log.d(TAG, "Socket initialized with URL: $serverUrl")
                setupSocketEvents()
        } catch (e: Exception) {
            Log.e(TAG, "Socket initialization error", e)
        }

    }

   private fun setupSocketEvents() {
        socket?.let { socket ->
            // Connection events
            socket.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected")
                onConnectListener?.invoke()
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Socket disconnected")
                onDisconnectListener?.invoke()
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                if (args.isNotEmpty()) {
                    val error = args[0]
                    Log.e(TAG, "Connection error: $error")
                    Log.e(TAG, "Connection error: $socket")
                    if (error is Exception) {
                        Log.e(TAG, "Error details: ${error.message}")
                        error.printStackTrace()
                    }
                } else {
                    Log.e(TAG, "Connection error with no details")
                }
            }


            socket.on("new_match") { args ->
                try {
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val matchData = args[0] as JSONObject
                        Log.d(TAG, "Received match: $matchData")

                        // Parse the match data and notify listeners
                        val match = parseMatchData(matchData)
                        onMatchListener?.invoke(match)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing match data: ${e.message}")
                }
            }


            socket.on("new_message") { args ->
                try {
                    Log.d(TAG, "Received something in new_message: $args")

                    if (args.isNotEmpty()) {
                        // בדוק את הטיפוס
                        val argType = args[0]?.javaClass?.name
                        Log.d(TAG, "Argument type: $argType")

                        // נסה לטפל במידע בלי להתבסס על הטיפוס
                        try {
                            // ננסה להמיר את הארגומנט הראשון למחרוזת JSON
                            val jsonString = args[0].toString()
                            Log.d(TAG, "JSON string: $jsonString")

                            // ננסה להמיר את המחרוזת ל-MessageResponse
                            val messageResponse = gson.fromJson(jsonString, MessageResponse::class.java)
                            Log.d(TAG, "Successfully parsed message response: $messageResponse")

                            // אם הגענו לכאן, העברת ההודעה הצליחה
                            onMessageReceivedListener?.invoke(messageResponse)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error parsing message data", ex)
                        }
                    } else {
                        Log.d(TAG, "Empty args array")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling received message", e)
                }
            }

            socket.on("message_sent") { args ->
                try {
                    Log.d(TAG, "=== RECEIVED MESSAGE_SENT EVENT ===")
                    Log.d(TAG, "Args length: ${args.size}")

                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        Log.d(TAG, "Message sent confirmation: $data")

                        val messageResponse = gson.fromJson(data.toString(), MessageResponse::class.java)
                        Log.d(TAG, "Calling onMessageSentListener")
                        onMessageSentListener?.invoke(messageResponse)
                    } else {
                        Log.e(TAG, "Invalid message_sent event data")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling sent message confirmation", e)
                }
            }

            socket.on("message_error") { args ->
                try {
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        val errorMessage = data.optString("error", "Unknown error sending message")
                        Log.e(TAG, "Message error: $errorMessage")

                        onMessageErrorListener?.invoke(errorMessage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling message error", e)
                }
            }

            socket.on("typing_indicator") { args ->
                try {
                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        Log.d(TAG, "Typing indicator: $data")

                        val typingResponse = gson.fromJson(data.toString(), TypingIndicatorResponse::class.java)
                        onTypingIndicatorListener?.invoke(typingResponse)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling typing indicator", e)
                }
            }
            socket.on("messages_read") { args ->
                try {
                    Log.d(TAG, "=== RECEIVED MESSAGES_READ EVENT ===")
                    Log.d(TAG, "Args: ${args.toList()}")

                    if (args.isNotEmpty() && args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        Log.d(TAG, "Messages read data: $data")

                        val messagesReadResponse = MessagesReadResponse(
                            matchId = data.getString("matchId"),
                            readBy = data.getString("readBy")
                        )

                        Log.d(TAG, "Parsed MessagesReadResponse: $messagesReadResponse")
                        Log.d(TAG, "Calling onMessagesReadListener")
                        onMessagesReadListener?.invoke(messagesReadResponse)
                    } else {
                        Log.e(TAG, "Invalid messages_read event data")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling messages read event", e)
                }
            }

        }
    }
//
//    // Parse JSON match data to MatchResponseItem
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
    fun markMessagesAsRead(chatId: String, userId: String, matchId: String) {
        socket?.let { socket ->
            if (socket.connected()) {
                Log.d(TAG, "=== SENDING MARK AS READ ===")
                Log.d(TAG, "Chat ID: $chatId")
                Log.d(TAG, "User ID: $userId")
                Log.d(TAG, "Match ID: $matchId")

                val data = JSONObject().apply {
                    put("chatId", chatId)
                    put("userId", userId)
                    put("matchId", matchId)
                }
                socket.emit("mark_messages_as_read", data)
                Log.d(TAG, "Marking messages as read in chat $chatId")
            } else {
                Log.e(TAG, "Cannot mark messages as read, socket not connected")
            }
        }
    }
    // Connect to socket server
    fun connect(userId: String) {
        socket?.let { socket ->
            if (!socket.connected()) {
                socket.connect()

                // Register user with socket server once connected
                socket.once(Socket.EVENT_CONNECT) {
                    // Emit user_connected event with userId
                    socket.emit("user_connected", userId)
                    Log.d(TAG, "Registered $userId with socket")
                }
            }
        }
    }

    // Disconnect from socket server
    fun disconnect() {
        socket?.disconnect()
    }

    // Set listener for new match events
    fun setOnMatchListener(listener: (MatchResponseItem) -> Unit) {
        this.onMatchListener = listener
    }

    // Set listener for match seen events
    fun setOnMatchSeenListener(listener: (String) -> Unit) {
        this.onMatchSeenListener = listener
    }

    // Set listener for connection events
    fun setOnConnectListener(listener: () -> Unit) {
        this.onConnectListener = listener
    }

    // Set listener for disconnection events
    fun setOnDisconnectListener(listener: () -> Unit) {
        this.onDisconnectListener = listener
    }
    fun sendMessage(matchId: String, sender: String, recipient: String, content: String) {
        socket?.let { socket ->
            if (socket.connected()) {
                val request = SendMessageRequest(matchId, sender, recipient, content)
                socket.emit("send_message", JSONObject(gson.toJson(request)))
                Log.d(TAG, "Sending message: $content to $recipient")
            } else {
                Log.e(TAG, "Cannot send message, socket not connected")
            }
        }
    }

    // Send typing indicator
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

    // Set message received listener
    fun setOnMessageReceivedListener(listener: (MessageResponse) -> Unit) {
        this.onMessageReceivedListener = listener
    }

    // Set message sent listener
    fun setOnMessageSentListener(listener: (MessageResponse) -> Unit) {
        this.onMessageSentListener = listener
    }

    // Set typing indicator listener
    fun setOnTypingIndicatorListener(listener: (TypingIndicatorResponse) -> Unit) {
        this.onTypingIndicatorListener = listener
    }

    // Set message error listener
    fun setOnMessageErrorListener(listener: (String) -> Unit) {
        this.onMessageErrorListener = listener
    }

    // Check if socket is connected
    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }
}
