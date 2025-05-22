package com.example.myapplication.model

data class User(
    val _id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val birthDate: String,
    val avatar: String,
    val occupation: String? = null,
    val about: String? = null,
    val genderInterest: String,
    val hobbies: List<String>? = emptyList()
)
