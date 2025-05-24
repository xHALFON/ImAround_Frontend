package com.example.myapplication.data.network

import com.example.myapplication.data.model.LikeRequest
import com.example.myapplication.data.model.LikeResponse
import com.example.myapplication.data.model.MatchCheckResponse
import com.example.myapplication.data.model.MatchResponseItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Interface for matching service
interface MatchingService {
    @POST("/match/like")
    suspend fun likeUser(@Body request: LikeRequest): LikeResponse

    @POST("/match/dislike")
    suspend fun dislikeUser(@Body request: LikeRequest): Response<Void>

    // 🔥 הוסף את זה:
    @POST("/match/save-fcm-token")
    suspend fun saveFCMToken(@Body request: SaveFCMTokenRequest): Response<Unit>
}

// 🔥 הוסף את המודל הזה (או בקובץ נפרד במודלים):
data class SaveFCMTokenRequest(
    val userId: String,
    val token: String
)