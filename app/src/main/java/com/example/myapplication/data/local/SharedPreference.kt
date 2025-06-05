package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    // 🆕 NEW - Reactive session state
    private val _sessionState = MutableStateFlow(SessionState.UNKNOWN)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    enum class SessionState {
        UNKNOWN,
        LOGGED_IN,
        LOGGED_OUT,
        EXPIRED
    }

    companion object {
        private const val TAG = "SessionManager"

        // Original keys (keeping backward compatibility)
        private const val KEY_USER_ID = "USER_ID"
        private const val KEY_ACCESS_TOKEN = "ACCESS_TOKEN"
        private const val KEY_IS_LOGGED_IN = "IS_LOGGED_IN"
        private const val KEY_LOGIN_TIME = "LOGIN_TIME"

        // New keys for Google Sign-In support
        private const val KEY_REFRESH_TOKEN = "REFRESH_TOKEN"
        private const val KEY_USER_EMAIL = "USER_EMAIL"
        private const val KEY_FIRST_NAME = "FIRST_NAME"
        private const val KEY_LAST_NAME = "LAST_NAME"
        private const val KEY_IS_GOOGLE_USER = "IS_GOOGLE_USER"
        private const val KEY_NEEDS_COMPLETION = "NEEDS_COMPLETION"

        // Profile completion tracking
        private const val KEY_HAS_BIRTH_DATE = "HAS_BIRTH_DATE"
        private const val KEY_HAS_GENDER = "HAS_GENDER"
        private const val KEY_HAS_GENDER_INTEREST = "HAS_GENDER_INTEREST"
        private const val KEY_HAS_HOBBIES = "HAS_HOBBIES"
    }

    init {
        // 🆕 Initialize session state
        updateSessionState()
    }

    // 🆕 Update session state and notify observers
    private fun updateSessionState() {
        val currentState = when {
            !prefs.getBoolean(KEY_IS_LOGGED_IN, false) -> SessionState.LOGGED_OUT
            getUserId() == null -> SessionState.LOGGED_OUT
            !isSessionTimeValid() -> SessionState.EXPIRED
            else -> SessionState.LOGGED_IN
        }

        Log.d(TAG, "🔄 Session state updated: ${_sessionState.value} → $currentState")
        _sessionState.value = currentState
    }

    private fun isSessionTimeValid(): Boolean {
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val thirtyDaysInMillis = 30 * 24 * 60 * 60 * 1000L
        return (currentTime - loginTime) < thirtyDaysInMillis
    }

    // ⭐ Original function - keeping for backward compatibility
    fun saveAuthData(userId: String, token: String) {
        Log.d(TAG, "💾 Saving auth data (legacy): $userId")
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, token)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            // Default to NOT Google user for legacy logins
            putBoolean(KEY_IS_GOOGLE_USER, false)
            putBoolean(KEY_NEEDS_COMPLETION, false)
            apply()
        }
        // 🆕 Update reactive state
        updateSessionState()
    }

    // 🆕 New function for complete user session (Google + regular)
    fun saveUserSession(
        userId: String,
        accessToken: String,
        refreshToken: String = "",
        userEmail: String = "",
        firstName: String = "",
        lastName: String = "",
        isGoogleUser: Boolean = false,
        needsCompletion: Boolean = false
    ) {
        Log.d(TAG, "💾 Saving complete user session: $userEmail, isGoogle: $isGoogleUser, needsCompletion: $needsCompletion")

        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            putBoolean(KEY_IS_GOOGLE_USER, isGoogleUser)
            putBoolean(KEY_NEEDS_COMPLETION, needsCompletion)
            apply()
        }
        // 🆕 Update reactive state
        updateSessionState()
    }

    // ⭐ Original function - keeping exact same logic but adding state update
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()

        // בדיקה אם עברו יותר מ-30 ימים
        val thirtyDaysInMillis = 30 * 24 * 60 * 60 * 1000L
        val isValid = isLoggedIn && (currentTime - loginTime) < thirtyDaysInMillis

        // 🆕 Update state if needed
        val expectedState = if (isValid) SessionState.LOGGED_IN else SessionState.LOGGED_OUT
        if (_sessionState.value != expectedState) {
            updateSessionState()
        }

        return isValid
    }

    // 🆕 Google Sign-In specific functions
    fun needsProfileCompletion(): Boolean {
        val needsCompletion = prefs.getBoolean(KEY_NEEDS_COMPLETION, false)
        val isGoogleUser = prefs.getBoolean(KEY_IS_GOOGLE_USER, false)

        Log.d(TAG, "🤔 Checking profile completion: needsCompletion=$needsCompletion, isGoogleUser=$isGoogleUser")

        // Only Google users might need to complete their profile
        return isGoogleUser && needsCompletion
    }

    fun isGoogleUser(): Boolean {
        return prefs.getBoolean(KEY_IS_GOOGLE_USER, false)
    }

    fun markProfileCompleted() {
        Log.d(TAG, "✅ Marking profile as completed")
        prefs.edit().apply {
            putBoolean(KEY_NEEDS_COMPLETION, false)
            putBoolean(KEY_HAS_BIRTH_DATE, true)
            putBoolean(KEY_HAS_GENDER, true)
            putBoolean(KEY_HAS_GENDER_INTEREST, true)
            putBoolean(KEY_HAS_HOBBIES, true)
            apply()
        }
    }

    fun updateProfileCompletionStatus(
        hasBirthDate: Boolean = false,
        hasGender: Boolean = false,
        hasGenderInterest: Boolean = false,
        hasHobbies: Boolean = false
    ) {
        Log.d(TAG, "📝 Updating profile completion status")

        prefs.edit().apply {
            putBoolean(KEY_HAS_BIRTH_DATE, hasBirthDate)
            putBoolean(KEY_HAS_GENDER, hasGender)
            putBoolean(KEY_HAS_GENDER_INTEREST, hasGenderInterest)
            putBoolean(KEY_HAS_HOBBIES, hasHobbies)

            // If all required fields are complete, mark as not needing completion
            val isComplete = hasBirthDate && hasGender && hasGenderInterest && hasHobbies
            putBoolean(KEY_NEEDS_COMPLETION, !isComplete)

            apply()
        }

        Log.d(TAG, "✅ Profile completion status updated. Complete: ${!needsProfileCompletion()}")
    }

    // ⭐ Original getters - keeping exact same names
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    // 🆕 New getters for additional data
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun getUserFirstName(): String? = prefs.getString(KEY_FIRST_NAME, null)

    fun getUserLastName(): String? = prefs.getString(KEY_LAST_NAME, null)

    // ⭐ Original function - keeping exact same logic but adding state update
    fun clearSession() {
        Log.d(TAG, "🗑️ Clearing user session")
        prefs.edit().clear().apply()
        // 🆕 Update reactive state
        updateSessionState()
    }

    // 🆕 Force refresh session state (useful for debugging or manual refresh)
    fun refreshSessionState() {
        Log.d(TAG, "🔄 Manually refreshing session state")
        updateSessionState()
    }

    // 🆕 Debug helper function
    fun logSessionInfo() {
        Log.d(TAG, """
            📊 Session Info:
            - State: ${_sessionState.value}
            - Logged in: ${isLoggedIn()}
            - User ID: ${getUserId()}
            - Email: ${getUserEmail()}
            - First name: ${getUserFirstName()}
            - Google user: ${isGoogleUser()}
            - Needs completion: ${needsProfileCompletion()}
        """.trimIndent())
    }

    // 🆕 Helper function to check if this is a fresh install/new user
    fun isFirstRun(): Boolean {
        return getUserId() == null
    }
}