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
            apply()
        }
    }

    fun getUserId(): String? = prefs.getString("USER_ID", null)

    fun getToken(): String? = prefs.getString("ACCESS_TOKEN", null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

}
