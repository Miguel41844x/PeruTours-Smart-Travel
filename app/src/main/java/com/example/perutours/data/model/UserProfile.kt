package com.example.perutours.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dni: String = "",
    val city: String = "Lima, Perú",
    val role: String = "cliente",
    val photoUrl: String = "",
    val preferences: List<String> = emptyList()
)