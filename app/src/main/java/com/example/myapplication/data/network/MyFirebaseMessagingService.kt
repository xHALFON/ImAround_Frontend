package com.example.myapplication.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.data.local.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "match_notifications"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔥 FCM Service created")
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "🔥 FCM Message received from: ${remoteMessage.from}")
        Log.d(TAG, "🔥 Message data: ${remoteMessage.data}")
        Log.d(TAG, "🔥 Message notification: ${remoteMessage.notification}")

        if (remoteMessage.data.isNotEmpty()) {
            val messageType = remoteMessage.data["type"]
            Log.d(TAG, "🔥 Message type: $messageType")

            when (messageType) {
                "new_match" -> {
                    Log.d(TAG, "🔥 Handling new match")
                    handleNewMatch(remoteMessage.data)
                }
                "new_message" -> {
                    Log.d(TAG, "🔥 Handling new message")
                    handleNewMessage(remoteMessage.data)
                }
                else -> {
                    Log.d(TAG, "🔥 Unknown message type: $messageType")
                }
            }
        } else {
            Log.d(TAG, "🔥 No data in message")
        }
    }

    private fun handleNewMatch(data: Map<String, String>) {
        Log.d(TAG, "🔥 handleNewMatch called with data: $data")
        val matchedUserName = data["userName"] ?: "Someone"
        val matchedUserId = data["userId"] ?: ""

        Log.d(TAG, "🔥 Matched user: $matchedUserName, ID: $matchedUserId")

        // Use the improved background detection
        val isBackground = !MyApplication.isAppInForeground
        Log.d(TAG, "🔥 App in background: $isBackground")

        if (isBackground) {
            Log.d(TAG, "🔥 Sending notification for match")
            sendNotification(
                title = "🎉 New Match!",
                body = "You matched with $matchedUserName! Click to open",
                type = "match",
                extraData = matchedUserId
            )
        } else {
            Log.d(TAG, "🔥 App is foreground, not sending notification (Socket handles it)")
        }
    }

    private fun handleNewMessage(data: Map<String, String>) {
        Log.d(TAG, "🔥 handleNewMessage called with data: $data")
        val senderName = data["senderName"] ?: "Someone"
        val messageContent = data["message"] ?: "sent you a message"
        val matchId = data["matchId"] ?: ""

        Log.d(TAG, "🔥 Message from: $senderName, content: $messageContent")

        // Use the improved background detection
        val isBackground = !MyApplication.isAppInForeground
        val inChatScreen = isInChatScreen(matchId)
        Log.d(TAG, "🔥 App in background: $isBackground, in chat screen: $inChatScreen")

        if (isBackground || !inChatScreen) {
            Log.d(TAG, "🔥 Sending notification for message")
            sendNotification(
                title = "New message from $senderName",
                body = messageContent,
                type = "message",
                extraData = matchId
            )
        } else {
            Log.d(TAG, "🔥 App is foreground and in chat, not sending notification (Socket handles it)")
        }
    }

    private fun isInChatScreen(matchId: String): Boolean {
        // Cannot easily check - assume not
        // If you want full accuracy, need to save state somewhere
        Log.d(TAG, "🔥 Checking if in chat screen for match: $matchId (always returns false)")
        return false
    }

    private fun sendNotification(
        title: String,
        body: String,
        type: String,
        extraData: String
    ) {
        Log.d(TAG, "🔥 Creating notification:")
        Log.d(TAG, "🔥   Title: $title")
        Log.d(TAG, "🔥   Body: $body")
        Log.d(TAG, "🔥   Type: $type")
        Log.d(TAG, "🔥   Extra Data: $extraData")

        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_type", type)
                putExtra("notification_data", extraData)
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // temporary icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()

            Log.d(TAG, "🔥 Showing notification with ID: $notificationId")
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "🔥 Notification shown successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "🔥 Error creating notification: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "🔥 Creating notification channel...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Match Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about new matches and messages"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "🔥 Notification channel created successfully")
        } else {
            Log.d(TAG, "🔥 No need to create notification channel (API < 26)")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "🔥 New FCM token received: $token")

        // Send the new token to server
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        if (userId != null) {
            Log.d(TAG, "🔥 Should update FCM token for user: $userId")
            // Here you'll add a call to server with the new token
            // You can use WorkManager or similar library for background
        } else {
            Log.d(TAG, "🔥 No user ID available, cannot update FCM token")
        }
    }
}