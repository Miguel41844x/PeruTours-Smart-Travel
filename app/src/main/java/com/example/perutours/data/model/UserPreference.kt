package com.example.perutours.data.model

data class UserPreference(
    val userId: String = "",
    val preferenceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
