package com.perutours.smarttravel.domain.repository

import com.perutours.smarttravel.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?
    val authStateFlow: Flow<AuthUser?>
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun register(email: String, password: String, firstName: String, lastName: String): Result<AuthUser>
    suspend fun sendEmailVerification()
    suspend fun reloadUser(): AuthUser?
    suspend fun logout()
}
