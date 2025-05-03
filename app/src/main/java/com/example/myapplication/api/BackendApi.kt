package com.example.myapplication.api

import com.example.myapplication.data.model.FindUsersRequest
import com.example.myapplication.data.model.UserResponse
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendApi {
    suspend fun findUsers(userIds: List<String>): List<UserResponse> {
        return withContext(Dispatchers.IO) {
            RetrofitClient.searchService.findUsers(FindUsersRequest(userIds))
        }
    }
}