package com.example.myapplication.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.AuthService
import com.example.myapplication.data.network.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

class GoogleSignInViewModel : ViewModel() {

    private val authService: AuthService = RetrofitClient.authService

    val authResponse = MutableLiveData<GoogleAuthResponse?>()
    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()

    companion object {
        private const val TAG = "GoogleSignInViewModel"
    }

    // יצירת Google Sign-In Client
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestProfile()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    // טיפול בתוצאה של Google Sign-In
    fun handleSignInResult(task: Task<GoogleSignInAccount>, context: Context) {
        try {
            isLoading.value = true
            val account = task.getResult(ApiException::class.java)
            account?.let {
                Log.d(TAG, "✅ Google Sign-In success: ${it.email}")
                authenticateWithBackend(it, context)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "❌ Google Sign-In failed: ${e.statusCode}")
            errorMessage.value = when (e.statusCode) {
                12501 -> "Sign-in was cancelled"
                12502 -> "Sign-in failed. Please try again"
                else -> "Google Sign-In failed: ${e.message}"
            }
            isLoading.value = false
        }
    }

    // שליחת פרטי Google למול הבאקאנד
    private fun authenticateWithBackend(account: GoogleSignInAccount, context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚀 Authenticating with backend...")

                val request = GoogleAuthRequest(
                    idToken = account.idToken ?: "",
                    email = account.email ?: "",
                    firstName = account.givenName ?: "",
                    lastName = account.familyName ?: "",
                    avatar = account.photoUrl?.toString() ?: ""
                )

                Log.d(TAG, "📤 Sending request: ${request.email}")

                val response = authService.googleAuth(request)

                Log.d(TAG, "📥 Response received: needsCompletion=${response.needsCompletion}")

                // שמירת נתונים ב-SessionManager
                val sessionManager = SessionManager(context)
                sessionManager.saveUserSession(
                    userId = response.id,
                    accessToken = response.accessToken ?: response.token ?: "",
                    refreshToken = response.refreshToken ?: "",
                    userEmail = response.email,
                    firstName = response.firstName,
                    lastName = response.lastName,
                    isGoogleUser = true,
                    needsCompletion = response.needsCompletion
                )

                Log.d(TAG, "💾 Session saved successfully")
                sessionManager.logSessionInfo()

                authResponse.value = response

            } catch (e: Exception) {
                Log.e(TAG, "❌ Backend authentication failed", e)
                errorMessage.value = "Authentication failed: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // Sign out
    fun signOut(context: Context) {
        val googleSignInClient = getGoogleSignInClient(context)
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "🚪 Google Sign-Out completed")
        }

        val sessionManager = SessionManager(context)
        sessionManager.clearSession()
    }

    fun clearError() {
        errorMessage.value = null
    }
}

// Data classes for Google Auth
data class GoogleAuthRequest(
    val idToken: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatar: String
)

data class GoogleAuthResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatar: String,
    val token: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean,
    val needsCompletion: Boolean = false
)