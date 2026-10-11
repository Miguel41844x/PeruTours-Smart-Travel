package com.example.perutours.ui.screens.signup

import android.util.Patterns

data class SignUpValidationResult(
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isValid: Boolean
        get() = nameError == null &&
                emailError == null &&
                phoneError == null &&
                passwordError == null &&
                confirmPasswordError == null
}

object SignUpValidator {

    fun validate(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): SignUpValidationResult {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanPass = password.trim()
        val cleanConfirm = confirmPassword.trim()

        // Expresión regular: solo letras en español (con tildes, ñ, diéresis) y espacios
        val nameRegex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")

        val nameError = when {
            cleanName.isBlank() -> "El nombre completo es obligatorio."
            cleanName.length < 3 -> "Ingresa un nombre válido (mínimo 3 caracteres)."
            !nameRegex.matches(cleanName) -> "El nombre solo puede contener letras y espacios."
            else -> null
        }

        val emailError = when {
            cleanEmail.isBlank() -> "El correo electrónico es obligatorio."
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "El formato del correo electrónico no es válido."
            else -> null
        }

        val phoneError = when {
            cleanPhone.isBlank() -> "El teléfono celular es obligatorio."
            cleanPhone.length != 9 || !cleanPhone.startsWith("9") -> "Ingresa un celular válido de 9 dígitos que empiece con 9."
            else -> null
        }

        val passwordError = when {
            cleanPass.isBlank() -> "La contraseña es obligatoria."
            cleanPass.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
            !cleanPass.any { it.isUpperCase() } -> "La contraseña debe contener al menos una letra mayúscula."
            !cleanPass.any { it.isDigit() } -> "La contraseña debe contener al menos un número."
            !cleanPass.any { !it.isLetterOrDigit() } -> "La contraseña debe contener al menos un símbolo (ej. @ # $ % & *)."
            else -> null
        }

        val confirmPasswordError = when {
            cleanConfirm.isBlank() -> "Confirma tu contraseña."
            cleanPass != cleanConfirm -> "Las contraseñas no coinciden."
            else -> null
        }

        return SignUpValidationResult(
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )
    }
}