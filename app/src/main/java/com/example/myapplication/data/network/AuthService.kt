package com.example.myapplication.data.network

import AuthResponse
import LoginRequest
import RegisterRequest
import com.example.myapplication.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthService {

    @POST("/auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): AuthResponse

    @POST("/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): AuthResponse

    @GET("/auth/fetchProfile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): User


}
