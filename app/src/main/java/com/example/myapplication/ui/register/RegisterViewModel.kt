package com.example.myapplication.ui.register

import AuthResponse
import RegisterRequest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    val authResponse = MutableLiveData<AuthResponse>()
    val errorMessage = MutableLiveData<String>()

    fun registerUser(username: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.authService.registerUser(
                    RegisterRequest(username, email, password)
                )
                authResponse.postValue(response)
            } catch (e: Exception) {
                errorMessage.postValue(e.localizedMessage ?: "Registration error")
            }
        }
    }
}
