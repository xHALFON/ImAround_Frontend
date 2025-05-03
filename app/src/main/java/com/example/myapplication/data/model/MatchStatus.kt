package com.example.myapplication.data.model

enum class MatchStatus {
    CONFIRMED,  // Both users have liked each other
    PENDING,    // You liked them, waiting for their response
    RECEIVED    // They liked you, waiting for your approval
}