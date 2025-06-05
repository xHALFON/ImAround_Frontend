package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.ui.AppNavHost
import com.example.myapplication.ui.chat.ChatViewModel
import com.example.myapplication.ui.search.SearchViewModel
import com.example.myapplication.ui.theme.SimpleLoginScreenTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🔥 MainActivity onCreate")

        // יצירת שירותים
        sessionManager = SessionManager(this)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // בדיקת מצב התחברות - מעודכן לGoogle Sign-In!
        val isUserLoggedIn = sessionManager.isLoggedIn()
        Log.d(TAG, "🔥 User login status: $isUserLoggedIn")

        // Initialize data if user is logged in
        if (isUserLoggedIn) {
            Log.d(TAG, "🔥 User is logged in, loading initial data")
            searchViewModel.loadMatches()
        }

        // טיפול בהתראות שהובילו לפתיחת האפליקציה
        handleNotificationIntent(intent)

        setContent {
            SimpleLoginScreenTheme {
                val navController = rememberNavController()

                // 🆕 לוגיקה מעודכנת ל-startDestination (זה החלק החדש!)
                val startDestination = determineStartDestination()

                Log.d(TAG, "🔥 Start destination: $startDestination")

                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    searchViewModel = searchViewModel
                )
            }
        }
    }

    // 🆕 פונקציה חדשה לקביעת איפה להתחיל
    private fun determineStartDestination(): String {
        val isLoggedIn = sessionManager.isLoggedIn()
        val needsCompletion = sessionManager.needsProfileCompletion()
        val isGoogleUser = sessionManager.isGoogleUser()

        // Debug info
        Log.d(TAG, "🔍 Navigation decision:")
        Log.d(TAG, "   - Is logged in: $isLoggedIn")
        Log.d(TAG, "   - Is Google user: $isGoogleUser")
        Log.d(TAG, "   - Needs completion: $needsCompletion")

        return when {
            !isLoggedIn -> {
                Log.d(TAG, "   → Going to: login")
                "login"
            }
            needsCompletion -> {
                Log.d(TAG, "   → Going to: complete_profile")
                "complete_profile"
            }
            else -> {
                Log.d(TAG, "   → Going to: main")
                "main"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔥 MainActivity onResume - MyApplication handles socket connection")

        // MyApplication already handles socket connection in onStart()
        // No need to duplicate socket logic here

        // Just refresh data if needed
        val userId = sessionManager.getUserId()
        if (userId != null) {
            Log.d(TAG, "🔥 Refreshing app data for user: $userId")
            // Any app-specific refresh logic can go here
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "🔥 MainActivity onPause")
        // MyApplication handles socket lifecycle
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        val notificationType = intent.getStringExtra("notification_type")
        val notificationData = intent.getStringExtra("notification_data")

        Log.d(TAG, "🔥 Notification intent: type=$notificationType, data=$notificationData")

        when (notificationType) {
            "match" -> {
                Log.d(TAG, "🔥 Opened from match notification")
                // כאן אפשר להוסיף ניווט אוטומטי למסך החיפוש אם רוצים
            }
            "message" -> {
                Log.d(TAG, "🔥 Opened from message notification")
                // כאן אפשר להוסיף ניווט אוטומטי למסך הצ'אטים אם רוצים
            }
            null -> {
                Log.d(TAG, "🔥 Normal app launch (no notification)")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔥 MainActivity onDestroy")
        // MyApplication handles socket cleanup
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SimpleLoginScreenTheme {
        AppNavHost(navController = rememberNavController())
    }
}