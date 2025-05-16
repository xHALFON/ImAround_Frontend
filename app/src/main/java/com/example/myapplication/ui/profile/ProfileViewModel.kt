package com.example.myapplication.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.model.User
import kotlinx.coroutines.launch
import retrofit2.Response


class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    val userProfile = MutableLiveData<User>()
    val errorMessage = MutableLiveData<String>()
    val logoutSuccess = MutableLiveData<Boolean>()

    private val sessionManager = SessionManager(application)

    fun loadUserProfile() {
        val userId = sessionManager.getUserId()

        if (userId == null) {
            errorMessage.postValue("User ID not found in session")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("InfoTrack", "ProfileViewModel: Sending req to Backend with userId: $userId")
                val response = RetrofitClient.authService.getUserProfile(userId)
                Log.d("InfoTrack", "ProfileViewModel: Response: $response")
                userProfile.postValue(response)
            } catch (e: Exception) {
                errorMessage.postValue(e.localizedMessage ?: "Profile loading failed")
            }
        }
    }

    fun updateUserProfile(id: String, updatedUser: User, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val response: Response<User> = RetrofitClient.authService.updateUser(id, updatedUser)

                if (response.isSuccessful && response.body() != null) {
                    userProfile.postValue(response.body())
                    onSuccess()
                } else {
                    Log.e("ProfileViewModel", "Update failed: ${response.errorBody()?.string()}")
                    errorMessage.postValue("Update failed")
                    onError()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Update exception: ${e.message}")
                errorMessage.postValue(e.localizedMessage ?: "Error updating profile")
                onError()
            }
        }
    }

    /**
     * Logs the user out by clearing session data
     */
    fun logout() {
        viewModelScope.launch {
            try {
                // Get user id before clearing session
                val userId = sessionManager.getUserId()

                // Attempt to notify server about logout if needed
                if (userId != null) {
                    try {
                        // If you have a logout endpoint, uncomment and modify this
                        // RetrofitClient.authService.logout(userId)
                        Log.d("ProfileViewModel", "Logging out user: $userId")
                    } catch (e: Exception) {
                        Log.w("ProfileViewModel", "Failed to notify server about logout: ${e.message}")
                    }
                }

                // Clear local session data
                sessionManager.clearSession()
                Log.d("ProfileViewModel", "User session cleared")

                // Notify UI about successful logout
                logoutSuccess.postValue(true)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Logout error: ${e.message}")
                errorMessage.postValue("Logout failed: ${e.message}")
                logoutSuccess.postValue(false)
            }
        }
    }
}