package com.example.myapplication.data.network


import CloudinaryService
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
object RetrofitClient {
    private const val BASE_URL = "https://imaround-backend.onrender.com/"

    // יצירת Retrofit instance אחד
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // שירות האימות
    val authService: AuthService by lazy {
        Log.d("InfoTrack", "RetrofitClient: authService")
        retrofit.create(AuthService::class.java)
    }

    // שירות החיפוש
    val searchService: SearchService by lazy {
        retrofit.create(SearchService::class.java)
    }
    val matchingService: MatchingService by lazy {
        retrofit.create(MatchingService::class.java)
    }
    // שירות הצ'אט
    val chatService: ChatService by lazy {
        Log.d("InfoTrack", "RetrofitClient: chatService")
        retrofit.create(ChatService::class.java)
    }

    val profilePhotoAnalysisService: ProfilePhotoAnalysisService by lazy {
        retrofit.create(ProfilePhotoAnalysisService::class.java)
    }
}

object CloudinaryClient {
    private const val BASE_URL = "https://api.cloudinary.com/"

    val service: CloudinaryService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryService::class.java)
    }
}

