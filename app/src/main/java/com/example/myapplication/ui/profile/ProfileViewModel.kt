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
}