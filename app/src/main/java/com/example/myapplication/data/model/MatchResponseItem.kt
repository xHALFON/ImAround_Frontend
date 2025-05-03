package com.example.myapplication.data.model

data class MatchResponseItem(
    val _id: String,
    val participants: List<String>,
    val liked: List<String>,
    val seen: Boolean = true
)