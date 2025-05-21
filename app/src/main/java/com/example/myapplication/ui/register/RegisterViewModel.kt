package com.example.myapplication.ui.register

import AuthResponse
import RegisterRequest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import com.example.myapplication.data.network.CloudinaryClient
import com.example.myapplication.data.network.CloudinaryUploadResponse
import com.example.myapplication.data.network.ProfilePhotoAnalysisRequest
import java.io.ByteArrayOutputStream

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    val authResponse = MutableLiveData<AuthResponse>()
    val errorMessage = MutableLiveData<String>()

    // User registration data
    var firstName = mutableStateOf("")
    var lastName = mutableStateOf("")
    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var dob = mutableStateOf("")
    var imageUri = mutableStateOf<Uri?>(null)
    var aboutMe = mutableStateOf("")
    var occupation = mutableStateOf("")
    var genderInterest = mutableStateOf("") // New field for gender interest

    private val _photoAnalysisFeedback = MutableLiveData<String?>()
    val photoAnalysisFeedback: LiveData<String?> = _photoAnalysisFeedback

    private val _isAnalyzingPhoto = MutableLiveData<Boolean>(false)
    val isAnalyzingPhoto: LiveData<Boolean> = _isAnalyzingPhoto

    fun registerUser(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        dob: String,
        imageUri: Uri?,
        aboutMe: String,
        occupation: String,
        genderInterest: String, // Added parameter
        hobbies: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val imageUrl = imageUri?.let { uploadToCloudinary(it) } ?: ""

                val request = RegisterRequest(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = dob,
                    avatar = imageUrl,
                    about = aboutMe,
                    occupation = occupation,
                    genderInterest = genderInterest, // Include in request
                    hobbies = hobbies
                )
                Log.d("RegisterViewModel", "Image URL to send: $imageUrl")
                Log.d("RegisterViewModel", "Hobbies to send: $hobbies")
                Log.d("RegisterViewModel", "Gender interest to send: $genderInterest")
                val response = RetrofitClient.authService.registerUser(request)
                authResponse.postValue(response)
            } catch (e: Exception) {
                errorMessage.postValue(e.localizedMessage ?: "Registration error")
            }
        }
    }

    private suspend fun uploadToCloudinary(imageUri: Uri): String? {
        val contentResolver = getApplication<Application>().contentResolver
        val inputStream: InputStream = contentResolver.openInputStream(imageUri) ?: return null
        val requestBody = inputStream.readBytes()
            .toRequestBody("image/*".toMediaTypeOrNull())

        val multipart = MultipartBody.Part.createFormData(
            "file", "profile.jpg", requestBody
        )

        val uploadPreset = "IamAroundProfilePic".toRequestBody("text/plain".toMediaTypeOrNull())

        val response: CloudinaryUploadResponse = CloudinaryClient.service.uploadImage(multipart, uploadPreset)
        Log.d("CloudinaryUpload", "Uploaded to: ${response.secure_url}")

        return response.secure_url
    }

    fun analyzeProfilePhoto(imageUri: Uri) {
        _isAnalyzingPhoto.value = true

        viewModelScope.launch {
            try {
                // המרת ה-Uri לBase64
                val base64Image = convertImageToBase64(imageUri)

                if (base64Image != null) {
                    val request = ProfilePhotoAnalysisRequest(base64Image)
                    val response = RetrofitClient.profilePhotoAnalysisService.analyzeProfilePhoto(request)

                    if (response.isSuccessful && response.body() != null) {
                        _photoAnalysisFeedback.postValue(response.body()!!.feedback)
                    } else {
                        _photoAnalysisFeedback.postValue("Could not analyze image. Please try again.")
                    }
                } else {
                    _photoAnalysisFeedback.postValue("Failed to process image.")
                }
            } catch (e: Exception) {
                _photoAnalysisFeedback.postValue("Error: ${e.localizedMessage ?: "Unknown error"}")
                Log.e("RegisterViewModel", "Error analyzing photo", e)
            } finally {
                _isAnalyzingPhoto.value = false
            }
        }
    }

    /**
     * ממיר Uri של תמונה למחרוזת Base64
     */
    private fun convertImageToBase64(uri: Uri): String? {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
            }

            // דחיסת התמונה
            val compressedBitmap = compressBitmap(bitmap)

            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            // המרה ל-Base64 והוספת קידומת של Data URI
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            return "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            Log.e("RegisterViewModel", "Error converting image to base64", e)
            return null
        }
    }

    /**
     * דוחס את התמונה לגודל סביר לשליחה
     */
    private fun compressBitmap(originalBitmap: Bitmap): Bitmap {
        val maxWidth = 800
        val maxHeight = 800

        val width = originalBitmap.width
        val height = originalBitmap.height

        // אם התמונה כבר קטנה מספיק, אין צורך לדחוס
        if (width <= maxWidth && height <= maxHeight) {
            return originalBitmap
        }

        // חישוב יחס הגודל החדש
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

    fun clearPhotoAnalysisFeedback() {
        _photoAnalysisFeedback.value = null
    }
}