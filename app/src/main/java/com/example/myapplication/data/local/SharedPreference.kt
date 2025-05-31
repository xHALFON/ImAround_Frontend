package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveAuthData(userId: String, token: String) {
        prefs.edit().apply {
            putString("USER_ID", userId)
            putString("ACCESS_TOKEN", token)
            putBoolean("IS_LOGGED_IN", true)
            putLong("LOGIN_TIME", System.currentTimeMillis()) // שמירת זמן ההתחברות
            apply()
        }
    }

    // פונקציה חדשה - בדיקה אם המשתמש מחובר + תוקף זמן
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false)
        val loginTime = prefs.getLong("LOGIN_TIME", 0)
        val currentTime = System.currentTimeMillis()

        // בדיקה אם עברו יותר מ-30 ימים
        val thirtyDaysInMillis = 30 * 24 * 60 * 60 * 1000L
        return isLoggedIn && (currentTime - loginTime) < thirtyDaysInMillis
    }

    fun getUserId(): String? = prefs.getString("USER_ID", null)

    fun getToken(): String? = prefs.getString("ACCESS_TOKEN", null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}