package com.example.myapplication.ui.register

import AuthResponse
import RegisterRequest
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    val authResponse = MutableLiveData<AuthResponse>()
    val errorMessage = MutableLiveData<String>()

    fun registerUser(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        dob: String,
        imageUri: Uri?
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
                    avatar = imageUrl
                )
                Log.d("RegisterViewModel", "Image URL to send: $imageUrl")
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
}
