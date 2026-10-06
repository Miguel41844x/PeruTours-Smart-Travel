package com.example.perutours.data.model

data class Preference(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val active: Boolean = true,
    val order: Int = 0
)