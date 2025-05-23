package com.example.myapplication.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.CloudinaryClient
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.InputStream

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    val userProfile = MutableLiveData<User>()
    val errorMessage = MutableLiveData<String>()
    val logoutSuccess = MutableLiveData<Boolean>()
    // Add state for delete account
    val deleteAccountSuccess = MutableLiveData<Boolean>()

    // Form state management
    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

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

    fun updateUserProfile(
        id: String,
        updatedUser: User,
        imageUri: Uri? = null,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Starting updateUserProfile")
                Log.d("ProfileViewModel", "User ID: $id")
                Log.d("ProfileViewModel", "Updated user data:")
                Log.d("ProfileViewModel", "  firstName: '${updatedUser.firstName}'")
                Log.d("ProfileViewModel", "  lastName: '${updatedUser.lastName}'")
                Log.d("ProfileViewModel", "  gender: '${updatedUser.gender}'")
                Log.d("ProfileViewModel", "  genderInterest: '${updatedUser.genderInterest}'")
                Log.d("ProfileViewModel", "  occupation: '${updatedUser.occupation}'")
                Log.d("ProfileViewModel", "  about: '${updatedUser.about}'")
                Log.d("ProfileViewModel", "  hobbies: ${updatedUser.hobbies}")

                // Handle image upload if a new image was selected
                var finalUser = updatedUser
                if (imageUri != null) {
                    try {
                        Log.d("ProfileViewModel", "Uploading image...")
                        val imageUrl = uploadImageToCloudinary(imageUri)
                        // Update the user object with the new avatar URL
                        finalUser = updatedUser.copy(avatar = imageUrl ?: updatedUser.avatar)
                        Log.d("ProfileViewModel", "Image uploaded, new URL: $imageUrl")
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Failed to upload image: ${e.message}")
                        // Continue with update even if image upload fails
                    }
                }

                Log.d("ProfileViewModel", "Final user object being sent to server:")
                Log.d("ProfileViewModel", "  gender: '${finalUser.gender}'")
                Log.d("ProfileViewModel", "  genderInterest: '${finalUser.genderInterest}'")

                // Update user profile in backend
                val response: Response<User> = RetrofitClient.authService.updateUser(id, finalUser)

                Log.d("ProfileViewModel", "Server response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val updatedUserFromServer = response.body()!!
                    Log.d("ProfileViewModel", "Server returned updated user:")
                    Log.d("ProfileViewModel", "  gender: '${updatedUserFromServer.gender}'")
                    Log.d("ProfileViewModel", "  genderInterest: '${updatedUserFromServer.genderInterest}'")

                    userProfile.postValue(updatedUserFromServer)
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ProfileViewModel", "Update failed: $errorBody")
                    Log.e("ProfileViewModel", "Response code: ${response.code()}")
                    errorMessage.postValue("Update failed")
                    onError()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Update exception: ${e.message}", e)
                errorMessage.postValue(e.localizedMessage ?: "Error updating profile")
                onError()
            }
        }
    }
    /**
     * Deletes the user account
     */
    fun deleteAccount() {
        val userId = sessionManager.getUserId()

        if (userId == null) {
            errorMessage.postValue("User ID not found in session")
            return
        }

        viewModelScope.launch {
            try {
                // Call your API to delete the user account
                val response = RetrofitClient.authService.deleteUser(userId)

                if (response.isSuccessful) {
                    // Clear local session data
                    sessionManager.clearSession()
                    Log.d("ProfileViewModel", "User account deleted and session cleared")

                    // Notify UI about successful deletion
                    deleteAccountSuccess.postValue(true)
                } else {
                    Log.e("ProfileViewModel", "Delete account failed: ${response.errorBody()?.string()}")
                    errorMessage.postValue("Failed to delete account")
                    deleteAccountSuccess.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Delete account error: ${e.message}")
                errorMessage.postValue(e.localizedMessage ?: "Error deleting account")
                deleteAccountSuccess.postValue(false)
            }
        }
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? {
        return try {
            val contentResolver = getApplication<Application>().contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(imageUri) ?: return null
            val requestBody = inputStream.readBytes()
                .toRequestBody("image/*".toMediaTypeOrNull())

            val multipart = MultipartBody.Part.createFormData(
                "file", "profile.jpg", requestBody
            )

            val uploadPreset = "IamAroundProfilePic".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = CloudinaryClient.service.uploadImage(multipart, uploadPreset)
            Log.d("ProfileViewModel", "Image uploaded to: ${response.secure_url}")

            response.secure_url
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Image upload error: ${e.message}")
            null
        }
    }

    /**
     * Saves the form state to preserve it during navigation
     */
    fun saveFormState(
        firstName: String,
        lastName: String,
        email: String,
        about: String,
        occupation: String,
        selectedImageUri: Uri?,
        genderInterest: String = ""  ,
        gender: String = ""
    ) {
        _formState.value = FormState(
            firstName = firstName,
            lastName = lastName,
            email = email,
            about = about,
            occupation = occupation,
            selectedImageUri = selectedImageUri,
            genderInterest = genderInterest ,
            gender = gender
        )
        Log.d("ProfileViewModel", "Form state saved: firstName=$firstName, lastName=$lastName, genderInterest=$genderInterest")
    }

    /**
     * Gets the saved form state
     */
    fun getSavedFormState(): FormState {
        return _formState.value
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

    /**
     * Data class to hold form state - Updated to include genderInterest
     */
    data class FormState(
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val about: String = "",
        val occupation: String = "",
        val selectedImageUri: Uri? = null,
        val genderInterest: String = ""  ,// Added genderInterest field
        val gender: String = ""
    )

    /**
     * Clears the saved form state after a successful update
     */
    fun clearFormState() {
        _formState.value = FormState()
    }
}