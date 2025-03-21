package com.example.myapplication.data.network


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"

    // יצירת Retrofit instance אחד
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // שירות האימות
    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    // שירות החיפוש
    val searchService: SearchService by lazy {
        retrofit.create(SearchService::class.java)
    }
}
