package com.perutours.smarttravel.domain.usecase

import com.perutours.smarttravel.domain.repository.AuthRepository
import javax.inject.Inject

class SendEmailVerificationUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            repository.sendEmailVerification()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error al enviar el correo de verificación: ${e.message}"))
        }
    }
}
