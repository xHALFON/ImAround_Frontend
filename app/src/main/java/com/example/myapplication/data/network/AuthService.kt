package com.example.myapplication.data.network

import AuthResponse
import LoginRequest
import RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): AuthResponse

    @POST("/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): AuthResponse
}
