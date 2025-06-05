package com.example.myapplication.ui.profile

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.CloudinaryClient
import com.example.myapplication.data.network.ProfilePhotoAnalysisRequest
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.model.User
import com.example.myapplication.ui.login.GoogleSignInViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    val userProfile = MutableLiveData<User>()
    val errorMessage = MutableLiveData<String>()
    val logoutSuccess = MutableLiveData<Boolean>()
    val deleteAccountSuccess = MutableLiveData<Boolean>()

    // 🆕 AI Photo Analysis
    private val _photoAnalysisFeedback = MutableLiveData<String?>()
    val photoAnalysisFeedback: LiveData<String?> = _photoAnalysisFeedback

    private val _isAnalyzingPhoto = MutableLiveData<Boolean>(false)
    val isAnalyzingPhoto: LiveData<Boolean> = _isAnalyzingPhoto

    // Form state management
    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    private val sessionManager = SessionManager(application)

    companion object {
        private const val TAG = "ProfileViewModel"
    }

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

    // 🆕 AI Photo Analysis Function
    fun analyzeProfilePhoto(imageUri: Uri) {
        _isAnalyzingPhoto.value = true

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 Starting photo analysis...")

                // Convert Uri to Base64
                val base64Image = convertImageToBase64(imageUri)

                if (base64Image != null) {
                    val request = ProfilePhotoAnalysisRequest(base64Image)
                    val response = RetrofitClient.profilePhotoAnalysisService.analyzeProfilePhoto(request)

                    if (response.isSuccessful && response.body() != null) {
                        _photoAnalysisFeedback.postValue(response.body()!!.feedback)
                        Log.d(TAG, "✅ Photo analysis completed successfully")
                    } else {
                        _photoAnalysisFeedback.postValue("Could not analyze image. Please try again.")
                        Log.w(TAG, "⚠️ Photo analysis response was unsuccessful")
                    }
                } else {
                    _photoAnalysisFeedback.postValue("Failed to process image.")
                    Log.e(TAG, "❌ Failed to convert image to base64")
                }
            } catch (e: Exception) {
                _photoAnalysisFeedback.postValue("Error: ${e.localizedMessage ?: "Unknown error"}")
                Log.e(TAG, "❌ Error analyzing photo", e)
            } finally {
                _isAnalyzingPhoto.value = false
            }
        }
    }

    // 🆕 Convert image Uri to Base64 string
    private fun convertImageToBase64(uri: Uri): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
            }

            // Compress the image
            val compressedBitmap = compressBitmap(bitmap)

            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            // Convert to Base64 with Data URI prefix
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to base64", e)
            null
        }
    }

    // 🆕 Compress bitmap to reasonable size for upload
    private fun compressBitmap(originalBitmap: Bitmap): Bitmap {
        val maxWidth = 800
        val maxHeight = 800

        val width = originalBitmap.width
        val height = originalBitmap.height

        // If image is already small enough, no need to compress
        if (width <= maxWidth && height <= maxHeight) {
            return originalBitmap
        }

        // Calculate new size ratio
        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (newWidth / ratio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (newHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }

    // 🆕 Clear photo analysis feedback
    fun clearPhotoAnalysisFeedback() {
        _photoAnalysisFeedback.value = null
    }

    fun deleteAccount(
        context: Context,
        navController: NavHostController,
        googleSignInViewModel: GoogleSignInViewModel? = null
    ) {
        val userId = sessionManager.getUserId()

        if (userId == null) {
            errorMessage.postValue("User ID not found in session")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "🗑️ Starting account deletion for user: $userId")

                // Call your API to delete the user account
                val response = RetrofitClient.authService.deleteUser(userId)

                if (response.isSuccessful) {
                    Log.d("ProfileViewModel", "✅ Account deleted successfully")

                    // Perform complete logout with navigation
                    performLogoutWithNavigation(context, navController, googleSignInViewModel)

                    // Notify UI about successful deletion
                    deleteAccountSuccess.postValue(true)
                } else {
                    Log.e("ProfileViewModel", "❌ Delete account failed: ${response.errorBody()?.string()}")
                    errorMessage.postValue("Failed to delete account")
                    deleteAccountSuccess.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ Delete account error: ${e.message}")
                errorMessage.postValue(e.localizedMessage ?: "Error deleting account")
                deleteAccountSuccess.postValue(false)
            }
        }
    }

    fun afterLogout(){
        logoutSuccess.value = false
    }

    private fun performLogoutWithNavigation(
        context: Context,
        navController: NavHostController,
        googleSignInViewModel: GoogleSignInViewModel? = null
    ) {
        Log.d("ProfileViewModel", "🔄 Performing logout with navigation")

        // Check if Google user
        val isGoogleUser = sessionManager.isGoogleUser()
        Log.d("ProfileViewModel", "📱 Is Google user: $isGoogleUser")

        // Google Sign-Out (if needed)
        if (isGoogleUser && googleSignInViewModel != null) {
            Log.d("ProfileViewModel", "🔄 Performing Google Sign-Out")
            googleSignInViewModel.signOut(context)
        }

        // Clear local session data
        sessionManager.clearSession()
        Log.d("ProfileViewModel", "🗑️ Session cleared")

        // Navigate to login with complete stack clear
        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }

        Log.d("ProfileViewModel", "✅ Logout with navigation completed")
    }

    @Deprecated("Use logout(context, navController, googleSignInViewModel) instead")
    fun logout() {
        Log.w("ProfileViewModel", "⚠️ Using deprecated logout() - navigation may not work properly")
        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    Log.d("ProfileViewModel", "Logging out user: $userId")
                }
                sessionManager.clearSession()
                Log.d("ProfileViewModel", "User session cleared")
                logoutSuccess.postValue(true)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Logout error: ${e.message}")
                errorMessage.postValue("Logout failed: ${e.message}")
                logoutSuccess.postValue(false)
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

    fun saveFormState(
        firstName: String,
        lastName: String,
        email: String,
        about: String,
        occupation: String,
        selectedImageUri: Uri?,
        genderInterest: String = "",
        gender: String = ""
    ) {
        _formState.value = FormState(
            firstName = firstName,
            lastName = lastName,
            email = email,
            about = about,
            occupation = occupation,
            selectedImageUri = selectedImageUri,
            genderInterest = genderInterest,
            gender = gender
        )
        Log.d("ProfileViewModel", "Form state saved: firstName=$firstName, lastName=$lastName, genderInterest=$genderInterest")
    }

    fun getSavedFormState(): FormState {
        return _formState.value
    }

    data class FormState(
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val about: String = "",
        val occupation: String = "",
        val selectedImageUri: Uri? = null,
        val genderInterest: String = "",
        val gender: String = ""
    )

    fun clearFormState() {
        _formState.value = FormState()
    }
}