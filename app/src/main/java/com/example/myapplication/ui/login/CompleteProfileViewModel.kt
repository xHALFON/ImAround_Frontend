package com.example.myapplication.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.AuthService
import com.example.myapplication.data.network.AuthService.CompleteProfileRequest
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.ui.login.GoogleAuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompleteProfileViewModel : ViewModel() {

    private val authService = RetrofitClient.authService

    val completionResponse = MutableLiveData<GoogleAuthResponse?>()
    val errorMessage = MutableLiveData<String?>()
    val isCompleting = MutableLiveData<Boolean>()

    // 🆕 Form state management for Complete Profile
    private val _formState = MutableStateFlow(CompleteProfileFormState())
    val formState: StateFlow<CompleteProfileFormState> = _formState.asStateFlow()

    companion object {
        private const val TAG = "CompleteProfileViewModel"
    }

    fun completeProfile(
        context: Context,
        userId: String,
        birthDate: String,
        gender: String,
        genderInterest: String,
        about: String,
        occupation: String,
        hobbies: List<String>
    ) {
        viewModelScope.launch {
            try {
                isCompleting.value = true
                errorMessage.value = null

                Log.d(TAG, "🚀 Completing profile for user: $userId")
                Log.d(TAG, "📝 Profile data: birthDate=$birthDate, gender=$gender, genderInterest=$genderInterest, hobbies=${hobbies.size}")

                val request = AuthService.CompleteProfileRequest(
                    birthDate = birthDate,
                    gender = gender,
                    genderInterest = genderInterest,
                    about = about,
                    occupation = occupation,
                    hobbies = hobbies
                )

                val response = authService.completeGoogleProfile(userId, request)

                // Update session manager
                val sessionManager = SessionManager(context)
                sessionManager.markProfileCompleted()

                Log.d(TAG, "✅ Profile completion successful")
                sessionManager.logSessionInfo()

                completionResponse.value = response

            } catch (e: Exception) {
                Log.e(TAG, "❌ Profile completion failed", e)
                errorMessage.value = "Failed to complete profile: ${e.message}"
            } finally {
                isCompleting.value = false
            }
        }
    }

    // 🆕 Save form state before navigation
    fun saveFormState(
        dob: String,
        gender: String,
        genderInterest: String,
        aboutMe: String,
        occupation: String
    ) {
        _formState.value = CompleteProfileFormState(
            dob = dob,
            gender = gender,
            genderInterest = genderInterest,
            aboutMe = aboutMe,
            occupation = occupation
        )
        Log.d(TAG, "📝 Form state saved: dob=$dob, gender=$gender, genderInterest=$genderInterest")
    }

    // 🆕 Get saved form state
    fun getSavedFormState(): CompleteProfileFormState {
        return _formState.value
    }

    // 🆕 Clear form state after successful completion
    fun clearFormState() {
        _formState.value = CompleteProfileFormState()
    }

    // 🆕 Data class to hold form state
    data class CompleteProfileFormState(
        val dob: String = "",
        val gender: String = "",
        val genderInterest: String = "",
        val aboutMe: String = "",
        val occupation: String = ""
    )

    fun clearError() {
        errorMessage.value = null
    }
}