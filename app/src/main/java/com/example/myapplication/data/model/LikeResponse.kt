package com.example.myapplication.data.model

data class LikeResponse(
    val isMatch: Boolean,
    val message: String,
    val matchDetails: MatchResponseItem? = null
)

