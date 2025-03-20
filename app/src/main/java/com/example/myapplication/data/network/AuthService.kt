package com.example.myapplication.data.network

import AuthResponse
import LoginRequest
import RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/auth/register")
    fun registerUser(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("/auth/login")
    fun loginUser(@Body request: LoginRequest): Call<AuthResponse>
}
