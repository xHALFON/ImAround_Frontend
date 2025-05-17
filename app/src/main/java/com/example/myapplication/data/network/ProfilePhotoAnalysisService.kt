package com.example.myapplication.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ProfilePhotoAnalysisRequest(
    val imageBase64: String
)

data class ProfilePhotoAnalysisResponse(
    val feedback: String,
    val is_person: Boolean = true // ברירת מחדל true למקרה של שרתים ישנים שלא מחזירים את השדה הזה
)
interface ProfilePhotoAnalysisService {
    @POST("/auth/analyzeProfilePhoto")
    suspend fun analyzeProfilePhoto(@Body request: ProfilePhotoAnalysisRequest): Response<ProfilePhotoAnalysisResponse>
}