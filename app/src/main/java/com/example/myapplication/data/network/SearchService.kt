package com.example.myapplication.data.network

import FindUsersRequest
import com.example.myapplication.data.model.UserResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SearchService {
    @POST("/search/findUsers")
    suspend fun findUsers(@Body request: FindUsersRequest): List<UserResponse>
}