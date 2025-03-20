package com.example.myapplication.ui.login

import AuthResponse
import LoginRequest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.launch




class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    val authResponse = MutableLiveData<AuthResponse>()
    val errorMessage = MutableLiveData<String>()

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                // נניח שה־AuthService מגדיר את loginUser כ-suspend function
                val response = RetrofitClient.authService.loginUser(LoginRequest(email, password))
                // שמירת נתוני המשתמש ב-SessionManager
                sessionManager.saveAuthData(response.userId, response.token)

                authResponse.postValue(response)
            } catch (e: Exception) {
                errorMessage.postValue(e.localizedMessage)
            }
        }
    }
}
