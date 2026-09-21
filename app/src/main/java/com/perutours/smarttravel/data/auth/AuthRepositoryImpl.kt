package com.perutours.smarttravel.data.auth

import com.perutours.smarttravel.domain.model.AuthUser
import com.perutours.smarttravel.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthSource: FirebaseAuthSource
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = firebaseAuthSource.currentFirebaseUser?.toDomain()

    override val authStateFlow: Flow<AuthUser?>
        get() = firebaseAuthSource.observeAuthState().map { firebaseUser ->
            firebaseUser?.toDomain()
        }

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            val user = firebaseAuthSource.login(email, password)
            Result.success(user.toDomain())
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthUser> {
        return try {
            val user = firebaseAuthSource.register(email, password, firstName, lastName)
            Result.success(user.toDomain())
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    override suspend fun sendEmailVerification() {
        firebaseAuthSource.sendEmailVerification()
    }

    override suspend fun reloadUser(): AuthUser? {
        return try {
            val user = firebaseAuthSource.reloadUser()
            user.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun logout() {
        firebaseAuthSource.logout()
    }

    private fun com.google.firebase.auth.FirebaseUser.toDomain(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email.orEmpty(),
            displayName = displayName,
            isEmailVerified = isEmailVerified
        )
    }

    private fun mapFirebaseError(e: Exception): Exception {
        val message = e.message ?: return e
        return when {
            message.contains("INVALID_LOGIN_CREDENTIALS", true) ||
            message.contains("The supplied auth credential is incorrect", true) ->
                Exception("Email o contraseña incorrectos")
            message.contains("email address is already in use", true) ->
                Exception("Este email ya está registrado")
            message.contains("There is no user record corresponding", true) ->
                Exception("No existe una cuenta con este email")
            message.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS", true) ->
                Exception("La contraseña no cumple con los requisitos de seguridad")
            message.contains("WEAK_PASSWORD", true) ->
                Exception("La contraseña es muy débil")
            message.contains("network error", true) ->
                Exception("Error de red. Verifica tu conexión a internet")
            message.contains("Too many requests", true) ->
                Exception("Demasiados intentos. Espera unos minutos e intenta de nuevo")
            message.contains("A network error", true) ->
                Exception("Error de red. Verifica tu conexión a internet")
            else -> Exception("Error de autenticación: $message")
        }
    }
}
