package com.perutours.smarttravel.data.auth

import com.perutours.smarttravel.domain.model.AuthUser
import com.perutours.smarttravel.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository : AuthRepository {

    private val users = mutableMapOf<String, FakeUser>()

    private data class FakeUser(
        val uid: String,
        val email: String,
        val password: String,
        val firstName: String,
        val lastName: String,
        var isEmailVerified: Boolean = false
    )

    private val _authStateFlow = MutableStateFlow<AuthUser?>(null)
    override val authStateFlow: Flow<AuthUser?> = _authStateFlow.asStateFlow()

    private var currentFakeUser: FakeUser? = null

    override val currentUser: AuthUser?
        get() = currentFakeUser?.toDomain()

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        val user = users.values.find {
            it.email.equals(email.trim(), ignoreCase = true) && it.password == password
        } ?: return Result.failure(Exception("Email o contraseña incorrectos"))

        currentFakeUser = user
        _authStateFlow.value = user.toDomain()
        return Result.success(user.toDomain())
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthUser> {
        if (users.values.any { it.email.equals(email.trim(), ignoreCase = true) }) {
            return Result.failure(Exception("Este email ya está registrado"))
        }

        val uid = "fake_${System.currentTimeMillis()}"
        val user = FakeUser(
            uid = uid,
            email = email.trim(),
            password = password,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            isEmailVerified = false
        )
        users[uid] = user
        currentFakeUser = user
        _authStateFlow.value = user.toDomain()
        return Result.success(user.toDomain())
    }

    override suspend fun sendEmailVerification() {
        currentFakeUser?.let { user ->
            user.isEmailVerified = true
            _authStateFlow.value = user.toDomain()
        }
    }

    override suspend fun reloadUser(): AuthUser? {
        return currentFakeUser?.toDomain()
    }

    override suspend fun logout() {
        currentFakeUser = null
        _authStateFlow.value = null
    }

    private fun FakeUser.toDomain(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = "$firstName $lastName".trim(),
            isEmailVerified = isEmailVerified
        )
    }
}
