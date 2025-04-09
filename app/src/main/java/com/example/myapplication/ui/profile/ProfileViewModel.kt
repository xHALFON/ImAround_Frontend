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
}
