package com.perutours.smarttravel.domain.model

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val isEmailVerified: Boolean = false
)
