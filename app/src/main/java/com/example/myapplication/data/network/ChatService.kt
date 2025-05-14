package com.example.myapplication.data.network

import com.example.myapplication.data.model.Chat
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ChatService {
    @GET("chat/user/{userId}")
    suspend fun getUserChats(@Path("userId") userId: String): List<Chat>

    @GET("chat/match/{matchId}")
    suspend fun getChatByMatchId(@Path("matchId") matchId: String): Chat

    @PUT("chat/{chatId}/read/{userId}")
    suspend fun markMessagesAsRead(
        @Path("chatId") chatId: String,
        @Path("userId") userId: String
    ): Map<String, Boolean>

}