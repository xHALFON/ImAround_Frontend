package com.example.myapplication.data.network

import android.annotation.SuppressLint
import android.util.Log
import com.example.myapplication.data.model.MatchResponseItem
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException


class SocketManager() {

    private var socket: Socket? = null


    private var onMatchListener: ((MatchResponseItem) -> Unit)? = null
    private var onMatchSeenListener: ((String) -> Unit)? = null
    private var onConnectListener: (() -> Unit)? = null
    private var onDisconnectListener: (() -> Unit)? = null

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
            // כתובת של השרת מתוך Android Emulator
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

    // Check if socket is connected
    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }
}
