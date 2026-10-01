package com.example.perutours.ui.screens.login

import android.util.Patterns

data class LoginValidationResult(
    val emailError: String? = null,
    val passwordError: String? = null
) {
    val isValid: Boolean
        get() = emailError == null && passwordError == null
}

object LoginValidator {

    fun validate(email: String, password: String): LoginValidationResult {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        val emailError = when {
            cleanEmail.isBlank() -> "Ingresa tu correo electrónico."
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "El formato del correo no es válido."
            else -> null
        }

        val passwordError = when {
            cleanPassword.isBlank() -> "Ingresa tu contraseña."
            else -> null
        }

        return LoginValidationResult(
            emailError = emailError,
            passwordError = passwordError
        )
    }

    fun validateEmailOnly(email: String): String? {
        val cleanEmail = email.trim()
        return when {
            cleanEmail.isBlank() -> "Por favor ingresa tu correo arriba para enviarte el enlace."
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "Por favor ingresa un correo electrónico válido."
            else -> null
        }
    }
}
