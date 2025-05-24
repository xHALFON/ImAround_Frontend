package com.example.myapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.SocketManager

class MyApplication : Application(), DefaultLifecycleObserver {

    companion object {
        private const val TAG = "MyApplication"

        // Static flag to track app state
        var isAppInForeground = false
            private set

        // Static instance for easy access
        lateinit var instance: MyApplication
            private set
    }

    private lateinit var sessionManager: SessionManager
    private lateinit var socketManager: SocketManager

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this

        Log.d(TAG, "🔥 Application created")

        // Initialize services
        sessionManager = SessionManager(this)
        socketManager = SocketManager.getInstance()

        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        Log.d(TAG, "🔥 Lifecycle observer registered")
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        Log.d(TAG, "🔥 App moved to FOREGROUND")

        // Reconnect socket when app comes to foreground
        val userId = sessionManager.getUserId()
        if (userId != null) {
            Log.d(TAG, "🔥 User logged in, ensuring socket connection for user: $userId")

            // Initialize socket if not already done
            if (!socketManager.isConnected()) {
                Log.d(TAG, "🔥 Socket not connected, initializing and connecting...")
                socketManager.init("http://10.0.2.2:3000")
                socketManager.connect(userId)
            } else {
                Log.d(TAG, "🔥 Socket already connected, but ensuring user registration")
                // Even if connected, ensure user is registered with server
                socketManager.connect(userId) // This will just re-register the user
            }
        } else {
            Log.d(TAG, "🔥 No user logged in, skipping socket connection")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
        Log.d(TAG, "🔥 App moved to BACKGROUND")

        // Socket will naturally disconnect due to Android background limitations
        // FCM will take over for notifications
    }

    // Helper methods
    fun isUserLoggedIn(): Boolean {
        return sessionManager.getUserId() != null
    }

    fun getCurrentUserId(): String? {
        return sessionManager.getUserId()
    }
}