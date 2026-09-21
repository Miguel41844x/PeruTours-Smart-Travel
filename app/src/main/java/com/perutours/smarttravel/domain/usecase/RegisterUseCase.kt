package com.perutours.smarttravel.domain.usecase

import com.perutours.smarttravel.domain.model.AuthUser
import com.perutours.smarttravel.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val UPPERCASE_REGEX = Regex(".*[A-Z].*")
        private val DIGIT_REGEX = Regex(".*\\d.*")

        const val MIN_PASSWORD_LENGTH = 8
    }

    data class ValidationError(val field: RegisterField, val message: String)

    enum class RegisterField {
        FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD
    }

    fun validate(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (firstName.isBlank()) {
            errors.add(ValidationError(RegisterField.FIRST_NAME, "El nombre es obligatorio"))
        }

        if (lastName.isBlank()) {
            errors.add(ValidationError(RegisterField.LAST_NAME, "El apellido es obligatorio"))
        }

        if (email.isBlank()) {
            errors.add(ValidationError(RegisterField.EMAIL, "El email es obligatorio"))
        } else if (!EMAIL_REGEX.matches(email)) {
            errors.add(ValidationError(RegisterField.EMAIL, "Formato de email inválido"))
        }

        if (password.isBlank()) {
            errors.add(ValidationError(RegisterField.PASSWORD, "La contraseña es obligatoria"))
        } else {
            if (password.length < MIN_PASSWORD_LENGTH) {
                errors.add(ValidationError(RegisterField.PASSWORD, "Mínimo $MIN_PASSWORD_LENGTH caracteres"))
            }
            if (!UPPERCASE_REGEX.matches(password)) {
                errors.add(ValidationError(RegisterField.PASSWORD, "Debe contener al menos 1 mayúscula"))
            }
            if (!DIGIT_REGEX.matches(password)) {
                errors.add(ValidationError(RegisterField.PASSWORD, "Debe contener al menos 1 número"))
            }
        }

        if (confirmPassword.isBlank()) {
            errors.add(ValidationError(RegisterField.CONFIRM_PASSWORD, "Confirma tu contraseña"))
        } else if (password != confirmPassword) {
            errors.add(ValidationError(RegisterField.CONFIRM_PASSWORD, "Las contraseñas no coinciden"))
        }

        return errors
    }

    suspend fun execute(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthUser> {
        return repository.register(email.trim(), password, firstName.trim(), lastName.trim())
    }
}
