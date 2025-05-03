
package com.example.myapplication.data.network

import com.example.myapplication.data.model.LikeRequest
import com.example.myapplication.data.model.LikeResponse
import com.example.myapplication.data.model.MatchCheckResponse
import com.example.myapplication.data.model.MatchResponseItem
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Interface for matching service
interface MatchingService {
    @POST("/match/like")
    suspend fun likeUser(@Body request: LikeRequest): LikeResponse

    @GET("/match/ismatch/{userId}")
    suspend fun checkMatches(@Path("userId") userId: String): MatchCheckResponse

    @GET("/match/match/{matchId}")
    suspend fun getMatchById(@Path("matchId") matchId: String): MatchResponseItem
}