package com.example.myapplication.data.model

data class UserMatch(
    val id: String,
    val user: UserResponse,
    val status: MatchStatus
)