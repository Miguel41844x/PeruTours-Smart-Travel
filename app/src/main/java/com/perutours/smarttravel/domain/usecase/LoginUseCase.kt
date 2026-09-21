package com.perutours.smarttravel.domain.usecase

import com.perutours.smarttravel.domain.model.AuthUser
import com.perutours.smarttravel.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("El email no puede estar vacío"))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("La contraseña no puede estar vacía"))
        return repository.login(email.trim(), password)
    }
}
